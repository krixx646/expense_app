import { verifyFirebaseIdentity } from "../_shared/firebaseAuth.ts";
import {
  HttpError,
  corsPreflightResponse,
  isHttpError,
  jsonResponse,
  requirePost
} from "../_shared/http.ts";
import {
  configuredAllowedOneTimeProducts,
  configuredPackageName,
  fetchPlayProductSnapshot
} from "../_shared/playApi.ts";
import {
  BILLING_SOURCE,
  createSupabaseAdminClient
} from "../_shared/subscriptionStore.ts";
import {
  getFounderRewardStateByUid,
  upsertFounderRewardState
} from "../_shared/founderRewardStore.ts";

Deno.serve(async (request) => {
  if (request.method == "OPTIONS") {
    return corsPreflightResponse();
  }

  try {
    requirePost(request);
    const identity = await verifyFirebaseIdentity(request);
    const client = createSupabaseAdminClient();
    const stored = await getFounderRewardStateByUid(client, identity.uid);
    if (!stored) {
      return jsonResponse(200, {
        hasFounderReward: false,
        productId: "",
        purchaseTime: 0,
        acknowledgedAt: null,
        purchaseTokenHash: "",
        founderBadgeGranted: false,
        founderTrialActivated: false,
        trialStartedAt: 0,
        trialExpiresAt: 0,
        newlyActivated: false,
        source: BILLING_SOURCE,
        message: "No founder reward found."
      });
    }

    const allowedProducts = configuredAllowedOneTimeProducts();
    if (!allowedProducts.has(stored.product_id)) {
      throw new HttpError(403, "Stored product is no longer allowed.");
    }

    const configuredPackage = configuredPackageName();
    const product = await fetchPlayProductSnapshot({
      packageName: configuredPackage,
      productId: stored.product_id,
      purchaseToken: stored.purchase_token
    });

    const now = Date.now();
    if (!product || !product.isPurchased) {
      await upsertFounderRewardState(client, {
        ...stored,
        package_name: configuredPackage,
        acknowledged_at: null,
        has_founder_reward: false,
        source: BILLING_SOURCE,
        updated_at: now
      });

      return jsonResponse(200, {
        hasFounderReward: false,
        productId: stored.product_id,
        purchaseTime: stored.purchase_time,
        acknowledgedAt: null,
        purchaseTokenHash: stored.purchase_token_hash,
        founderBadgeGranted: stored.founder_badge_granted,
        founderTrialActivated: stored.founder_trial_activated,
        trialStartedAt: stored.founder_trial_started_at,
        trialExpiresAt: stored.founder_trial_expires_at,
        newlyActivated: false,
        source: BILLING_SOURCE,
        message: "Stored founder reward token is no longer valid."
      });
    }

    await upsertFounderRewardState(client, {
      firebase_uid: stored.firebase_uid,
      product_id: stored.product_id,
      package_name: configuredPackage,
      purchase_token: stored.purchase_token,
      purchase_token_hash: stored.purchase_token_hash,
      order_id: product.orderId || stored.order_id,
      purchase_time: product.purchaseTime,
      acknowledged_at: product.acknowledgedAt,
      has_founder_reward: true,
      founder_badge_granted: true,
      founder_trial_activated: stored.founder_trial_activated,
      founder_trial_started_at: stored.founder_trial_started_at,
      founder_trial_expires_at: stored.founder_trial_expires_at,
      source: BILLING_SOURCE,
      updated_at: now
    });

    return jsonResponse(200, {
      hasFounderReward: true,
      productId: stored.product_id,
      purchaseTime: product.purchaseTime,
      acknowledgedAt: product.acknowledgedAt,
      purchaseTokenHash: stored.purchase_token_hash,
      founderBadgeGranted: true,
      founderTrialActivated: stored.founder_trial_activated,
      trialStartedAt: stored.founder_trial_started_at,
      trialExpiresAt: stored.founder_trial_expires_at,
      newlyActivated: false,
      source: BILLING_SOURCE,
      message: stored.founder_trial_activated
        ? "Founder reward is active on this account."
        : "Founder reward is available on this account."
    });
  } catch (error) {
    console.error("refresh-founder-reward failed", error);
    if (isHttpError(error)) {
      return jsonResponse(error.status, { message: error.message });
    }
    const message = error instanceof Error ? error.message : "Unexpected error.";
    return jsonResponse(500, { message });
  }
});
