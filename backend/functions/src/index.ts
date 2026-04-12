import * as admin from "firebase-admin";
import crypto from "node:crypto";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { defineSecret, defineString } from "firebase-functions/params";
import { google } from "googleapis";

admin.initializeApp();

const PLAY_SERVICE_ACCOUNT_JSON = defineSecret("PLAY_SERVICE_ACCOUNT_JSON");
const MOCHI_GEMINI_API_KEY = defineSecret("MOCHI_GEMINI_API_KEY");
const PLAY_PACKAGE_NAME = defineString("PLAY_PACKAGE_NAME");
const PLAY_ALLOWED_PRODUCTS = defineString("PLAY_ALLOWED_PRODUCTS");

const FUNCTION_REGION = "us-central1";
const ANDROID_PUBLISHER_SCOPE = "https://www.googleapis.com/auth/androidpublisher";

const ENTITLEMENTS_COLLECTION = "entitlements";
const ENTITLEMENT_DOC_SUBSCRIPTION = "subscription_state";
const TOKEN_BINDINGS_COLLECTION = "purchase_tokens";
const PRIVATE_TOKENS_COLLECTION = "server_private_play_tokens";
const SOURCE = "play_api_server";
const MOCHI_MODEL = "gemini-2.5-flash";

type VerifyPayload = {
  productId: string;
  purchaseToken: string;
  packageName: string;
};

type EntitlementResponse = {
  isPremium: boolean;
  productId: string;
  purchaseTime: number;
  expiryTime: number;
  isAutoRenewing: boolean;
  acknowledgedAt: number | null;
  purchaseTokenHash: string;
  source: string;
  message: string;
};

type MochiPromptPayload = {
  systemPrompt: string;
  userPrompt: string;
  maxOutputTokens: number;
};

type MochiPromptResponse = {
  text: string;
  finishReason: string | null;
};

export const verifyPlaySubscriptionPurchase = onCall(
  {
    region: FUNCTION_REGION,
    enforceAppCheck: true,
    timeoutSeconds: 30,
    memory: "256MiB",
    secrets: [PLAY_SERVICE_ACCOUNT_JSON]
  },
  async (request): Promise<EntitlementResponse> => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError("unauthenticated", "Sign in is required.");
    }

    const payload = parseVerifyPayload(request.data);
    const configuredPackage = PLAY_PACKAGE_NAME.value().trim();
    if (!configuredPackage) {
      throw new HttpsError("failed-precondition", "PLAY_PACKAGE_NAME is not configured.");
    }
    if (payload.packageName != configuredPackage) {
      throw new HttpsError("permission-denied", "Package mismatch.");
    }

    const allowedProducts = parseAllowedProducts(PLAY_ALLOWED_PRODUCTS.value());
    if (!allowedProducts.has(payload.productId)) {
      throw new HttpsError("permission-denied", "Product is not allowed.");
    }

    return verifyAndPersistEntitlement({
      uid,
      productId: payload.productId,
      purchaseToken: payload.purchaseToken,
      packageName: configuredPackage
    });
  }
);

export const refreshPlaySubscriptionEntitlement = onCall(
  {
    region: FUNCTION_REGION,
    enforceAppCheck: true,
    timeoutSeconds: 30,
    memory: "256MiB",
    secrets: [PLAY_SERVICE_ACCOUNT_JSON]
  },
  async (request): Promise<EntitlementResponse> => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError("unauthenticated", "Sign in is required.");
    }

    const configuredPackage = PLAY_PACKAGE_NAME.value().trim();
    if (!configuredPackage) {
      throw new HttpsError("failed-precondition", "PLAY_PACKAGE_NAME is not configured.");
    }

    const tokenDoc = await admin
      .firestore()
      .collection(PRIVATE_TOKENS_COLLECTION)
      .doc(uid)
      .get();
    if (!tokenDoc.exists) {
      const now = Date.now();
      await admin
        .firestore()
        .collection("users")
        .doc(uid)
        .collection(ENTITLEMENTS_COLLECTION)
        .doc(ENTITLEMENT_DOC_SUBSCRIPTION)
        .set(
          {
            isPremium: false,
            updatedAt: now,
            source: SOURCE
          },
          { merge: true }
        );
      return {
        isPremium: false,
        productId: "",
        purchaseTime: 0,
        expiryTime: 0,
        isAutoRenewing: false,
        acknowledgedAt: null,
        purchaseTokenHash: "",
        source: SOURCE,
        message: "No stored purchase token to refresh."
      };
    }

    const storedProductId = asString(tokenDoc.get("productId"));
    const storedPurchaseToken = asString(tokenDoc.get("purchaseToken"));
    if (!storedProductId || !storedPurchaseToken) {
      throw new HttpsError("failed-precondition", "Stored purchase token record is invalid.");
    }

    const allowedProducts = parseAllowedProducts(PLAY_ALLOWED_PRODUCTS.value());
    if (!allowedProducts.has(storedProductId)) {
      throw new HttpsError("permission-denied", "Stored product is no longer allowed.");
    }

    return verifyAndPersistEntitlement({
      uid,
      productId: storedProductId,
      purchaseToken: storedPurchaseToken,
      packageName: configuredPackage
    });
  }
);

export const askMochi = onCall(
  {
    region: FUNCTION_REGION,
    enforceAppCheck: true,
    timeoutSeconds: 60,
    memory: "512MiB",
    secrets: [MOCHI_GEMINI_API_KEY]
  },
  async (request): Promise<MochiPromptResponse> => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError("unauthenticated", "Sign in is required.");
    }

    const payload = parseMochiPayload(request.data);
    const apiKey = MOCHI_GEMINI_API_KEY.value().trim();
    if (!apiKey) {
      throw new HttpsError("failed-precondition", "MOCHI_GEMINI_API_KEY is not configured.");
    }

    const response = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/${MOCHI_MODEL}:generateContent?key=${apiKey}`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          systemInstruction: {
            parts: [{ text: payload.systemPrompt }]
          },
          contents: [
            {
              parts: [{ text: payload.userPrompt }]
            }
          ],
          generationConfig: {
            temperature: 0.4,
            maxOutputTokens: payload.maxOutputTokens
          }
        })
      }
    );

    const rawBody = await response.text();
    if (!response.ok) {
      throw new HttpsError(
        "internal",
        parseGeminiError(rawBody) ?? `Gemini request failed with HTTP ${response.status}.`
      );
    }

    let parsed: GeminiGenerateContentResponse;
    try {
      parsed = JSON.parse(rawBody) as GeminiGenerateContentResponse;
    } catch {
      throw new HttpsError("internal", "Unable to parse Gemini response.");
    }

    const candidates = parsed.candidates ?? [];
    const bestCandidate = candidates.reduce<GeminiCandidate | null>((best, candidate) => {
      const currentLength = candidate.content?.parts
        ?.map((part) => part.text?.trim() ?? "")
        .join("\n")
        .trim()
        .length ?? 0;
      const bestLength = best?.content?.parts
        ?.map((part) => part.text?.trim() ?? "")
        .join("\n")
        .trim()
        .length ?? 0;
      return currentLength > bestLength ? candidate : best;
    }, null);

    const text = bestCandidate?.content?.parts
      ?.map((part) => part.text?.trim() ?? "")
      .join("\n")
      .trim() ?? "";

    return {
      text,
      finishReason: asString(bestCandidate?.finishReason) ?? null
    };
  }
);

async function verifyAndPersistEntitlement(params: {
  uid: string;
  productId: string;
  purchaseToken: string;
  packageName: string;
}): Promise<EntitlementResponse> {
  const now = Date.now();
  const publisher = buildPublisherClient();
  const response = await publisher.purchases.subscriptions.get({
    packageName: params.packageName,
    subscriptionId: params.productId,
    token: params.purchaseToken
  });

  const play = response.data;
  const expiryTime = asNumber(play.expiryTimeMillis) ?? 0;
  const purchaseTime = asNumber(play.startTimeMillis) ?? now;
  const isAutoRenewing = Boolean(play.autoRenewing ?? false);
  const isAcknowledged = asNumber(play.acknowledgementState) == 1;
  const orderId = asString(play.orderId) ?? "";
  const isPremium = expiryTime > now;
  const purchaseTokenHash = sha256(params.purchaseToken);
  const acknowledgedAt = isAcknowledged ? now : null;
  const message = isPremium
    ? "Subscription verified and active."
    : "Subscription verified but not active.";

  const tokenRef = admin.firestore().collection(TOKEN_BINDINGS_COLLECTION).doc(purchaseTokenHash);
  const privateTokenRef = admin.firestore().collection(PRIVATE_TOKENS_COLLECTION).doc(params.uid);
  const entitlementRef = admin
    .firestore()
    .collection("users")
    .doc(params.uid)
    .collection(ENTITLEMENTS_COLLECTION)
    .doc(ENTITLEMENT_DOC_SUBSCRIPTION);

  await admin.firestore().runTransaction(async (tx) => {
    const existingToken = await tx.get(tokenRef);
    if (existingToken.exists) {
      const existingUid = asString(existingToken.get("uid"));
      if (existingUid && existingUid != params.uid) {
        throw new HttpsError(
          "permission-denied",
          "This purchase token is already linked to another account."
        );
      }
    }

    tx.set(
      tokenRef,
      {
        uid: params.uid,
        productId: params.productId,
        packageName: params.packageName,
        orderId,
        updatedAt: now,
        source: SOURCE
      },
      { merge: true }
    );

    tx.set(
      privateTokenRef,
      {
        uid: params.uid,
        productId: params.productId,
        purchaseToken: params.purchaseToken,
        packageName: params.packageName,
        updatedAt: now,
        source: SOURCE
      },
      { merge: true }
    );

    tx.set(
      entitlementRef,
      {
        isPremium,
        productId: params.productId,
        purchaseTokenHash,
        purchaseTime,
        expiryTime,
        isAutoRenewing,
        acknowledgedAt,
        updatedAt: now,
        source: SOURCE
      },
      { merge: true }
    );
  });

  return {
    isPremium,
    productId: params.productId,
    purchaseTime,
    expiryTime,
    isAutoRenewing,
    acknowledgedAt,
    purchaseTokenHash,
    source: SOURCE,
    message
  };
}

function buildPublisherClient() {
  const credentialsRaw = PLAY_SERVICE_ACCOUNT_JSON.value().trim();
  if (!credentialsRaw) {
    throw new HttpsError("failed-precondition", "Missing PLAY_SERVICE_ACCOUNT_JSON secret.");
  }

  let credentials: { client_email?: string; private_key?: string };
  try {
    credentials = JSON.parse(credentialsRaw) as { client_email?: string; private_key?: string };
  } catch {
    throw new HttpsError("failed-precondition", "PLAY_SERVICE_ACCOUNT_JSON is not valid JSON.");
  }

  const auth = new google.auth.GoogleAuth({
    credentials,
    scopes: [ANDROID_PUBLISHER_SCOPE]
  });

  return google.androidpublisher({
    version: "v3",
    auth
  });
}

function parseVerifyPayload(raw: unknown): VerifyPayload {
  if (typeof raw != "object" || raw == null) {
    throw new HttpsError("invalid-argument", "Request payload is invalid.");
  }

  const obj = raw as Record<string, unknown>;
  const productId = asString(obj.productId);
  const purchaseToken = asString(obj.purchaseToken);
  const packageName = asString(obj.packageName);

  if (!productId || !purchaseToken || !packageName) {
    throw new HttpsError("invalid-argument", "productId, purchaseToken, and packageName are required.");
  }

  if (purchaseToken.length < 20 || purchaseToken.length > 4096) {
    throw new HttpsError("invalid-argument", "Purchase token is invalid.");
  }

  return { productId, purchaseToken, packageName };
}

function parseMochiPayload(raw: unknown): MochiPromptPayload {
  if (typeof raw != "object" || raw == null) {
    throw new HttpsError("invalid-argument", "Request payload is invalid.");
  }

  const obj = raw as Record<string, unknown>;
  const systemPrompt = asString(obj.systemPrompt);
  const userPrompt = asString(obj.userPrompt);
  const maxOutputTokens = asNumber(obj.maxOutputTokens);
  if (!systemPrompt || !userPrompt || maxOutputTokens == null) {
    throw new HttpsError(
      "invalid-argument",
      "systemPrompt, userPrompt, and maxOutputTokens are required."
    );
  }
  if (systemPrompt.length > 40_000 || userPrompt.length > 40_000) {
    throw new HttpsError("invalid-argument", "Prompt is too large.");
  }
  if (maxOutputTokens < 128 || maxOutputTokens > 8_192) {
    throw new HttpsError("invalid-argument", "maxOutputTokens is out of range.");
  }
  return {
    systemPrompt,
    userPrompt,
    maxOutputTokens
  };
}

function parseAllowedProducts(raw: string): Set<string> {
  return new Set(
    raw
      .split(",")
      .map((it) => it.trim())
      .filter((it) => it.length > 0)
  );
}

function asString(value: unknown): string | null {
  if (typeof value != "string") return null;
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

function asNumber(value: unknown): number | null {
  if (typeof value == "number" && Number.isFinite(value)) {
    return value;
  }
  if (typeof value == "string") {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) return parsed;
  }
  return null;
}

function sha256(raw: string): string {
  return crypto.createHash("sha256").update(raw).digest("hex");
}

function parseGeminiError(raw: string): string | null {
  if (!raw.trim()) return null;
  try {
    const parsed = JSON.parse(raw) as {
      error?: {
        message?: unknown;
      };
    };
    return asString(parsed.error?.message) ?? null;
  } catch {
    return null;
  }
}

type GeminiGenerateContentResponse = {
  candidates?: GeminiCandidate[];
};

type GeminiCandidate = {
  content?: {
    parts?: Array<{
      text?: string | null;
    }>;
  };
  finishReason?: string | null;
};

