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
  getFounderRewardTokenOwner,
  upsertFounderRewardState
} from "../_shared/founderRewardStore.ts";

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

    const allowedProducts = configuredAllowedOneTimeProducts();
    if (!allowedProducts.has(productId)) {
      throw new HttpError(403, "Product is not allowed.");
    }

    const product = await fetchPlayProductSnapshot({
      packageName: configuredPackage,
      productId,
      purchaseToken
    });
    if (!product || !product.isPurchased) {
      throw new HttpError(404, "Purchase could not be verified.");
    }

    const purchaseTokenHash = await sha256Hex(purchaseToken);
    const client = createSupabaseAdminClient();
    const existingOwner = await getFounderRewardTokenOwner(client, purchaseTokenHash);
    if (existingOwner && existingOwner != identity.uid) {
      throw new HttpError(403, "This purchase token is already linked to another account.");
    }

    const stored = await getFounderRewardStateByUid(client, identity.uid);
    const now = Date.now();
    await upsertFounderRewardState(client, {
      firebase_uid: identity.uid,
      product_id: productId,
      package_name: configuredPackage,
      purchase_token: purchaseToken,
      purchase_token_hash: purchaseTokenHash,
      order_id: product.orderId || stored?.order_id || null,
      purchase_time: product.purchaseTime,
      acknowledged_at: product.acknowledgedAt,
      has_founder_reward: true,
      founder_badge_granted: true,
      founder_trial_activated: stored?.founder_trial_activated ?? false,
      founder_trial_started_at: stored?.founder_trial_started_at ?? 0,
      founder_trial_expires_at: stored?.founder_trial_expires_at ?? 0,
      source: BILLING_SOURCE,
      updated_at: now
    });

    return jsonResponse(200, {
      hasFounderReward: true,
      productId,
      purchaseTime: product.purchaseTime,
      acknowledgedAt: product.acknowledgedAt,
      purchaseTokenHash,
      founderBadgeGranted: true,
      founderTrialActivated: stored?.founder_trial_activated ?? false,
      trialStartedAt: stored?.founder_trial_started_at ?? 0,
      trialExpiresAt: stored?.founder_trial_expires_at ?? 0,
      newlyActivated: false,
      source: BILLING_SOURCE,
      message: "Founder reward verified."
    });
  } catch (error) {
    console.error("verify-play-founder-reward failed", error);
    if (isHttpError(error)) {
      return jsonResponse(error.status, { message: error.message });
    }
    const message = error instanceof Error ? error.message : "Unexpected error.";
    return jsonResponse(500, { message });
  }
});
