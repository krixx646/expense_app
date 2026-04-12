import { verifyFirebaseIdentity } from "../_shared/firebaseAuth.ts";
import {
  corsPreflightResponse,
  isHttpError,
  jsonResponse,
  requirePost
} from "../_shared/http.ts";
import {
  BILLING_SOURCE,
  createSupabaseAdminClient
} from "../_shared/subscriptionStore.ts";
import {
  getFounderRewardStateByUid,
  upsertFounderRewardState
} from "../_shared/founderRewardStore.ts";

const DAY_MS = 24 * 60 * 60 * 1000;
const FOUNDER_TRIAL_DAYS = 14;

Deno.serve(async (request) => {
  if (request.method == "OPTIONS") {
    return corsPreflightResponse();
  }

  try {
    requirePost(request);
    const identity = await verifyFirebaseIdentity(request);
    const client = createSupabaseAdminClient();
    const stored = await getFounderRewardStateByUid(client, identity.uid);

    if (!stored || !stored.has_founder_reward) {
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
        message: "Founder reward not found for this account."
      });
    }

    if (stored.founder_trial_activated) {
      return jsonResponse(200, {
        hasFounderReward: true,
        productId: stored.product_id,
        purchaseTime: stored.purchase_time,
        acknowledgedAt: stored.acknowledged_at,
        purchaseTokenHash: stored.purchase_token_hash,
        founderBadgeGranted: stored.founder_badge_granted,
        founderTrialActivated: true,
        trialStartedAt: stored.founder_trial_started_at,
        trialExpiresAt: stored.founder_trial_expires_at,
        newlyActivated: false,
        source: BILLING_SOURCE,
        message: "Your Founder reward has already been activated."
      });
    }

    const now = Date.now();
    const expiresAt = now + FOUNDER_TRIAL_DAYS * DAY_MS;
    await upsertFounderRewardState(client, {
      ...stored,
      has_founder_reward: true,
      founder_badge_granted: true,
      founder_trial_activated: true,
      founder_trial_started_at: now,
      founder_trial_expires_at: expiresAt,
      source: BILLING_SOURCE,
      updated_at: now
    });

    return jsonResponse(200, {
      hasFounderReward: true,
      productId: stored.product_id,
      purchaseTime: stored.purchase_time,
      acknowledgedAt: stored.acknowledged_at,
      purchaseTokenHash: stored.purchase_token_hash,
      founderBadgeGranted: true,
      founderTrialActivated: true,
      trialStartedAt: now,
      trialExpiresAt: expiresAt,
      newlyActivated: true,
      source: BILLING_SOURCE,
      message: "Founder reward activated. Enjoy 14 days of Premium access."
    });
  } catch (error) {
    console.error("claim-founder-reward failed", error);
    if (isHttpError(error)) {
      return jsonResponse(error.status, { message: error.message });
    }
    const message = error instanceof Error ? error.message : "Unexpected error.";
    return jsonResponse(500, { message });
  }
});
