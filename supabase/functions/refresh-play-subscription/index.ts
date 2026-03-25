import { verifyFirebaseIdentity } from "../_shared/firebaseAuth.ts";
import {
  HttpError,
  corsPreflightResponse,
  isHttpError,
  jsonResponse,
  requirePost
} from "../_shared/http.ts";
import {
  configuredAllowedProducts,
  configuredPackageName,
  fetchPlaySubscriptionSnapshot
} from "../_shared/playApi.ts";
import {
  BILLING_SOURCE,
  createSupabaseAdminClient,
  getSubscriptionStateByUid,
  upsertSubscriptionState
} from "../_shared/subscriptionStore.ts";

Deno.serve(async (request) => {
  if (request.method == "OPTIONS") {
    return corsPreflightResponse();
  }

  try {
    requirePost(request);
    const identity = await verifyFirebaseIdentity(request);
    const client = createSupabaseAdminClient();
    const stored = await getSubscriptionStateByUid(client, identity.uid);
    if (!stored) {
      return jsonResponse(200, {
        isPremium: false,
        productId: "",
        purchaseTime: 0,
        expiryTime: 0,
        isAutoRenewing: false,
        acknowledgedAt: null,
        purchaseTokenHash: "",
        source: BILLING_SOURCE,
        message: "No stored purchase token to refresh."
      });
    }

    const allowedProducts = configuredAllowedProducts();
    if (!allowedProducts.has(stored.product_id)) {
      throw new HttpError(403, "Stored product is no longer allowed.");
    }

    const configuredPackage = configuredPackageName();
    const subscription = await fetchPlaySubscriptionSnapshot({
      packageName: configuredPackage,
      productId: stored.product_id,
      purchaseToken: stored.purchase_token
    });

    const now = Date.now();
    if (!subscription) {
      await upsertSubscriptionState(client, {
        ...stored,
        package_name: configuredPackage,
        expiry_time: 0,
        is_auto_renewing: false,
        is_premium: false,
        source: BILLING_SOURCE,
        updated_at: now
      });

      return jsonResponse(200, {
        isPremium: false,
        productId: stored.product_id,
        purchaseTime: stored.purchase_time,
        expiryTime: 0,
        isAutoRenewing: false,
        acknowledgedAt: stored.acknowledged_at,
        purchaseTokenHash: stored.purchase_token_hash,
        source: BILLING_SOURCE,
        message: "Stored purchase token is no longer valid."
      });
    }

    await upsertSubscriptionState(client, {
      firebase_uid: stored.firebase_uid,
      product_id: stored.product_id,
      package_name: configuredPackage,
      purchase_token: stored.purchase_token,
      purchase_token_hash: stored.purchase_token_hash,
      order_id: subscription.orderId || stored.order_id,
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
      productId: stored.product_id,
      purchaseTime: subscription.purchaseTime,
      expiryTime: subscription.expiryTime,
      isAutoRenewing: subscription.isAutoRenewing,
      acknowledgedAt: subscription.acknowledgedAt,
      purchaseTokenHash: stored.purchase_token_hash,
      source: BILLING_SOURCE,
      message: subscription.isPremium
        ? "Subscription verified and active."
        : "Subscription verified but not active."
    });
  } catch (error) {
    console.error("refresh-play-subscription failed", error);
    if (isHttpError(error)) {
      return jsonResponse(error.status, { message: error.message });
    }
    const message = error instanceof Error ? error.message : "Unexpected error.";
    return jsonResponse(500, { message });
  }
});
