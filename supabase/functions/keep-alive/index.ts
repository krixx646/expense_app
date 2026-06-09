import {
  HttpError,
  corsPreflightResponse,
  isHttpError,
  jsonResponse
} from "../_shared/http.ts";
import {
  SUBSCRIPTION_STATE_TABLE,
  createSupabaseAdminClient
} from "../_shared/subscriptionStore.ts";

Deno.serve(async (request) => {
  if (request.method == "OPTIONS") {
    return corsPreflightResponse();
  }

  if (request.method != "GET" && request.method != "POST") {
    return jsonResponse(405, { ok: false, message: "Only GET and POST are allowed." });
  }

  try {
    const client = createSupabaseAdminClient();
    const result = await client
      .from(SUBSCRIPTION_STATE_TABLE)
      .select("firebase_uid")
      .limit(1);

    if (result.error) {
      throw new HttpError(500, `Keep-alive database check failed: ${result.error.message}`);
    }

    return jsonResponse(200, {
      ok: true,
      checkedAt: Date.now(),
      table: SUBSCRIPTION_STATE_TABLE,
      rowCount: result.data?.length ?? 0
    });
  } catch (error) {
    if (isHttpError(error)) {
      return jsonResponse(error.status, { ok: false, message: error.message });
    }

    const message = error instanceof Error ? error.message : "Unexpected keep-alive failure.";
    return jsonResponse(500, { ok: false, message });
  }
});
