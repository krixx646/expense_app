import { verifyFirebaseIdentity } from "../_shared/firebaseAuth.ts";
import {
  HttpError,
  asNonEmptyString,
  corsPreflightResponse,
  isHttpError,
  jsonResponse,
  parseJsonBody,
  requirePost,
  sha256Hex
} from "../_shared/http.ts";
import {
  configuredAllowedProducts,
  configuredPackageName,
  fetchPlaySubscriptionSnapshot
} from "../_shared/playApi.ts";
import {
  BILLING_SOURCE,
  createSupabaseAdminClient,
  getSubscriptionTokenOwner,
  upsertSubscriptionState
} from "../_shared/subscriptionStore.ts";

type VerifyPurchaseRequest = {
  productId?: string;
  purchaseToken?: string;
  packageName?: string;
};

Deno.serve(async (request) => {
  if (request.method == "OPTIONS") {
    return corsPreflightResponse();
  }

  try {
    requirePost(request);
    const identity = await verifyFirebaseIdentity(request);
    const payload = await parseJsonBody<VerifyPurchaseRequest>(request);
    const productId = asNonEmptyString(payload.productId);
    const purchaseToken = asNonEmptyString(payload.purchaseToken);
    const packageName = asNonEmptyString(payload.packageName);
    if (!productId || !purchaseToken || !packageName) {
      throw new HttpError(400, "productId, purchaseToken, and packageName are required.");
    }
    if (purchaseToken.length < 20 || purchaseToken.length > 4096) {
      throw new HttpError(400, "Purchase token is invalid.");
    }

    const configuredPackage = configuredPackageName();
    if (packageName != configuredPackage) {
      throw new HttpError(403, "Package mismatch.");
    }

    const allowedProducts = configuredAllowedProducts();
    if (!allowedProducts.has(productId)) {
      throw new HttpError(403, "Product is not allowed.");
    }

    const subscription = await fetchPlaySubscriptionSnapshot({
      packageName: configuredPackage,
      productId,
      purchaseToken
    });
    if (!subscription) {
      throw new HttpError(404, "Purchase could not be verified.");
    }

    const purchaseTokenHash = await sha256Hex(purchaseToken);
    const client = createSupabaseAdminClient();
    const existingOwner = await getSubscriptionTokenOwner(client, purchaseTokenHash);
    if (existingOwner && existingOwner != identity.uid) {
      throw new HttpError(403, "This purchase token is already linked to another account.");
    }

    const now = Date.now();
    await upsertSubscriptionState(client, {
      firebase_uid: identity.uid,
      product_id: productId,
      package_name: configuredPackage,
      purchase_token: purchaseToken,
      purchase_token_hash: purchaseTokenHash,
      order_id: subscription.orderId || null,
      purchase_time: subscription.purchaseTime,
      expiry_time: subscription.expiryTime,
      is_auto_renewing: subscription.isAutoRenewing,
      acknowledged_at: subscription.acknowledgedAt,
      is_premium: subscription.isPremium,
      source: BILLING_SOURCE,
      updated_at: now
    });

    return jsonResponse(200, {
      isPremium: subscription.isPremium,
      productId,
      purchaseTime: subscription.purchaseTime,
      expiryTime: subscription.expiryTime,
      isAutoRenewing: subscription.isAutoRenewing,
      acknowledgedAt: subscription.acknowledgedAt,
      purchaseTokenHash,
      source: BILLING_SOURCE,
      message: subscription.isPremium
        ? "Subscription verified and active."
        : "Subscription verified but not active."
    });
  } catch (error) {
    console.error("verify-play-subscription failed", error);
    if (isHttpError(error)) {
      return jsonResponse(error.status, { message: error.message });
    }
    const message = error instanceof Error ? error.message : "Unexpected error.";
    return jsonResponse(500, { message });
  }
});
