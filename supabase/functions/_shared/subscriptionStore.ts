import { createClient, type SupabaseClient } from "npm:@supabase/supabase-js@2.49.4";

import { HttpError, requiredEnv } from "./http.ts";

export const SUBSCRIPTION_STATE_TABLE = "play_subscription_state";
export const BILLING_SOURCE = "play_api_supabase";

export type SubscriptionStateRow = {
  firebase_uid: string;
  product_id: string;
  package_name: string;
  purchase_token: string;
  purchase_token_hash: string;
  order_id: string | null;
  purchase_time: number;
  expiry_time: number;
  is_auto_renewing: boolean;
  acknowledged_at: number | null;
  is_premium: boolean;
  source: string;
  updated_at: number;
};

export function createSupabaseAdminClient(): SupabaseClient {
  return createClient(
    requiredEnv("SUPABASE_URL"),
    requiredEnv("SUPABASE_SERVICE_ROLE_KEY"),
    {
      auth: {
        autoRefreshToken: false,
        persistSession: false
      }
    }
  );
}

export async function getSubscriptionStateByUid(
  client: SupabaseClient,
  uid: string
): Promise<SubscriptionStateRow | null> {
  const result = await client
    .from(SUBSCRIPTION_STATE_TABLE)
    .select("*")
    .eq("firebase_uid", uid)
    .maybeSingle();

  if (result.error) {
    throw new HttpError(500, `Unable to read subscription state: ${result.error.message}`);
  }

  return (result.data as SubscriptionStateRow | null) ?? null;
}

export async function getSubscriptionTokenOwner(
  client: SupabaseClient,
  purchaseTokenHash: string
): Promise<string | null> {
  const result = await client
    .from(SUBSCRIPTION_STATE_TABLE)
    .select("firebase_uid")
    .eq("purchase_token_hash", purchaseTokenHash)
    .maybeSingle();

  if (result.error) {
    throw new HttpError(500, `Unable to read purchase token ownership: ${result.error.message}`);
  }

  return (result.data as { firebase_uid?: string } | null)?.firebase_uid ?? null;
}

export async function upsertSubscriptionState(
  client: SupabaseClient,
  row: SubscriptionStateRow
): Promise<void> {
  const result = await client
    .from(SUBSCRIPTION_STATE_TABLE)
    .upsert(row, {
      onConflict: "firebase_uid"
    });

  if (result.error) {
    throw new HttpError(500, `Unable to persist subscription state: ${result.error.message}`);
  }
}
