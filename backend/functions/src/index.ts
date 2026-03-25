import * as admin from "firebase-admin";
import crypto from "node:crypto";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { defineSecret, defineString } from "firebase-functions/params";
import { google } from "googleapis";

admin.initializeApp();

const PLAY_SERVICE_ACCOUNT_JSON = defineSecret("PLAY_SERVICE_ACCOUNT_JSON");
const PLAY_PACKAGE_NAME = defineString("PLAY_PACKAGE_NAME");
const PLAY_ALLOWED_PRODUCTS = defineString("PLAY_ALLOWED_PRODUCTS");

const FUNCTION_REGION = "us-central1";
const ANDROID_PUBLISHER_SCOPE = "https://www.googleapis.com/auth/androidpublisher";

const ENTITLEMENTS_COLLECTION = "entitlements";
const ENTITLEMENT_DOC_SUBSCRIPTION = "subscription_state";
const TOKEN_BINDINGS_COLLECTION = "purchase_tokens";
const PRIVATE_TOKENS_COLLECTION = "server_private_play_tokens";
const SOURCE = "play_api_server";

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

