import type { SupabaseClient } from "npm:@supabase/supabase-js@2.49.4";

import { HttpError } from "./http.ts";

export const FOUNDER_REWARD_STATE_TABLE = "play_founder_reward_state";

export type FounderRewardStateRow = {
  firebase_uid: string;
  product_id: string;
  package_name: string;
  purchase_token: string;
  purchase_token_hash: string;
  order_id: string | null;
  purchase_time: number;
  acknowledged_at: number | null;
  has_founder_reward: boolean;
  founder_badge_granted: boolean;
  founder_trial_activated: boolean;
  founder_trial_started_at: number;
  founder_trial_expires_at: number;
  source: string;
  updated_at: number;
};

export async function getFounderRewardStateByUid(
  client: SupabaseClient,
  uid: string
): Promise<FounderRewardStateRow | null> {
  const result = await client
    .from(FOUNDER_REWARD_STATE_TABLE)
    .select("*")
    .eq("firebase_uid", uid)
    .maybeSingle();

  if (result.error) {
    throw new HttpError(500, `Unable to read founder reward state: ${result.error.message}`);
  }

  return (result.data as FounderRewardStateRow | null) ?? null;
}

export async function getFounderRewardTokenOwner(
  client: SupabaseClient,
  purchaseTokenHash: string
): Promise<string | null> {
  const result = await client
    .from(FOUNDER_REWARD_STATE_TABLE)
    .select("firebase_uid")
    .eq("purchase_token_hash", purchaseTokenHash)
    .maybeSingle();

  if (result.error) {
    throw new HttpError(500, `Unable to read founder reward token ownership: ${result.error.message}`);
  }

  return (result.data as { firebase_uid?: string } | null)?.firebase_uid ?? null;
}

export async function upsertFounderRewardState(
  client: SupabaseClient,
  row: FounderRewardStateRow
): Promise<void> {
  const result = await client
    .from(FOUNDER_REWARD_STATE_TABLE)
    .upsert(row, {
      onConflict: "firebase_uid"
    });

  if (result.error) {
    throw new HttpError(500, `Unable to persist founder reward state: ${result.error.message}`);
  }
}
