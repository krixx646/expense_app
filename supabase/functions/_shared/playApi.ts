import { importPKCS8, SignJWT } from "npm:jose@5.9.6";

import { HttpError, asNonEmptyString, requiredEnv } from "./http.ts";

const ANDROID_PUBLISHER_SCOPE = "https://www.googleapis.com/auth/androidpublisher";
const GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";

type ServiceAccountCredentials = {
  client_email?: string;
  private_key?: string;
  token_uri?: string;
};

export type PlaySubscriptionSnapshot = {
  orderId: string;
  purchaseTime: number;
  expiryTime: number;
  isAutoRenewing: boolean;
  acknowledgedAt: number | null;
  isPremium: boolean;
};

let cachedAccessToken: { token: string; expiresAt: number } | null = null;

export function configuredPackageName(): string {
  return requiredEnv("PLAY_PACKAGE_NAME");
}

export function configuredAllowedProducts(): Set<string> {
  return new Set(
    requiredEnv("PLAY_ALLOWED_PRODUCTS")
      .split(",")
      .map((value) => value.trim())
      .filter((value) => value.length > 0)
  );
}

export async function fetchPlaySubscriptionSnapshot(params: {
  packageName: string;
  productId: string;
  purchaseToken: string;
}): Promise<PlaySubscriptionSnapshot | null> {
  const accessToken = await getGoogleAccessToken();
  const endpoint =
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(params.packageName)}` +
    `/purchases/subscriptions/${encodeURIComponent(params.productId)}/tokens/${encodeURIComponent(params.purchaseToken)}`;

  const response = await fetch(endpoint, {
    headers: {
      Authorization: `Bearer ${accessToken}`
    }
  });

  if (response.status == 404) {
    return null;
  }
  if (!response.ok) {
    const errorText = await response.text();
    console.error("Google Play subscription verification failed", {
      status: response.status,
      body: errorText,
      productId: params.productId,
      packageName: params.packageName
    });
    throw new HttpError(
      502,
      userFacingVerificationMessage(response.status)
    );
  }

  const payload = await response.json() as {
    expiryTimeMillis?: string | number;
    startTimeMillis?: string | number;
    autoRenewing?: boolean;
    acknowledgementState?: string | number;
    orderId?: string;
  };

  const now = Date.now();
  const expiryTime = asNumber(payload.expiryTimeMillis) ?? 0;
  const purchaseTime = asNumber(payload.startTimeMillis) ?? now;
  const isAcknowledged = asNumber(payload.acknowledgementState) == 1;

  return {
    orderId: asNonEmptyString(payload.orderId) ?? "",
    purchaseTime,
    expiryTime,
    isAutoRenewing: Boolean(payload.autoRenewing ?? false),
    acknowledgedAt: isAcknowledged ? now : null,
    isPremium: expiryTime > now
  };
}

async function getGoogleAccessToken(): Promise<string> {
  if (cachedAccessToken && cachedAccessToken.expiresAt > Date.now() + 60_000) {
    return cachedAccessToken.token;
  }

  const credentials = parseServiceAccountJson();
  const tokenUri = credentials.token_uri ?? GOOGLE_TOKEN_URL;
  const nowSeconds = Math.floor(Date.now() / 1000);
  const privateKey = await importPKCS8(credentials.private_key, "RS256");
  const assertion = await new SignJWT({
    scope: ANDROID_PUBLISHER_SCOPE
  })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuer(credentials.client_email)
    .setSubject(credentials.client_email)
    .setAudience(tokenUri)
    .setIssuedAt(nowSeconds)
    .setExpirationTime(nowSeconds + 3600)
    .sign(privateKey);

  const response = await fetch(tokenUri, {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded"
    },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion
    })
  });

  if (!response.ok) {
    const errorText = await response.text();
    console.error("Google Play access token request failed", {
      status: response.status,
      body: errorText
    });
    throw new HttpError(
      502,
      "Purchase verification is temporarily unavailable. Please try again in a moment."
    );
  }

  const tokenPayload = await response.json() as {
    access_token?: string;
    expires_in?: number;
  };
  const accessToken = asNonEmptyString(tokenPayload.access_token);
  if (!accessToken) {
    throw new HttpError(502, "Google access token response was invalid.");
  }

  const expiresInSeconds = tokenPayload.expires_in ?? 3600;
  cachedAccessToken = {
    token: accessToken,
    expiresAt: Date.now() + expiresInSeconds * 1000
  };
  return accessToken;
}

function parseServiceAccountJson(): Required<Pick<ServiceAccountCredentials, "client_email" | "private_key">> &
  ServiceAccountCredentials {
  const raw = requiredEnv("PLAY_SERVICE_ACCOUNT_JSON");

  let parsed: ServiceAccountCredentials;
  try {
    parsed = JSON.parse(raw) as ServiceAccountCredentials;
  } catch {
    throw new HttpError(500, "PLAY_SERVICE_ACCOUNT_JSON is not valid JSON.");
  }

  const clientEmail = asNonEmptyString(parsed.client_email);
  const privateKey = asNonEmptyString(parsed.private_key);
  if (!clientEmail || !privateKey) {
    throw new HttpError(500, "PLAY_SERVICE_ACCOUNT_JSON is missing required fields.");
  }

  return {
    ...parsed,
    client_email: clientEmail,
    private_key: privateKey
  };
}

function asNumber(value: unknown): number | null {
  if (typeof value == "number" && Number.isFinite(value)) {
    return value;
  }
  if (typeof value == "string") {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) {
      return parsed;
    }
  }
  return null;
}

function userFacingVerificationMessage(status: number): string {
  switch (status) {
    case 401:
    case 403:
      return "We couldn't verify your Google Play purchase right now. If you already subscribed, tap Restore Purchases in a moment."
    default:
      return "We couldn't verify your Google Play purchase right now. Please try again in a moment."
  }
}
