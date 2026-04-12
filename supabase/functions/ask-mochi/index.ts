import { verifyFirebaseIdentity } from "../_shared/firebaseAuth.ts";
import {
  HttpError,
  asNonEmptyString,
  corsPreflightResponse,
  isHttpError,
  jsonResponse,
  parseJsonBody,
  requirePost,
  requiredEnv
} from "../_shared/http.ts";

type MochiRequest = {
  systemPrompt?: string;
  userPrompt?: string;
  maxOutputTokens?: number;
};

type GeminiGenerateContentResponse = {
  candidates?: GeminiCandidate[];
};

type GeminiCandidate = {
  content?: {
    parts?: Array<{
      text?: string | null;
    }>;
  };
  finishReason?: string | null;
};

Deno.serve(async (request) => {
  if (request.method == "OPTIONS") {
    return corsPreflightResponse();
  }

  try {
    requirePost(request);
    await verifyFirebaseIdentity(request);

    const payload = await parseJsonBody<MochiRequest>(request);
    const systemPrompt = asNonEmptyString(payload.systemPrompt);
    const userPrompt = asNonEmptyString(payload.userPrompt);
    const maxOutputTokens = typeof payload.maxOutputTokens == "number"
      ? Math.floor(payload.maxOutputTokens)
      : NaN;

    if (!systemPrompt || !userPrompt || !Number.isFinite(maxOutputTokens)) {
      throw new HttpError(400, "systemPrompt, userPrompt, and maxOutputTokens are required.");
    }
    if (systemPrompt.length > 40_000 || userPrompt.length > 40_000) {
      throw new HttpError(400, "Prompt is too large.");
    }
    if (maxOutputTokens < 128 || maxOutputTokens > 8_192) {
      throw new HttpError(400, "maxOutputTokens is out of range.");
    }

    const apiKey = requiredEnv("MOCHI_GEMINI_API_KEY");
    const response = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${apiKey}`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          systemInstruction: {
            parts: [{ text: systemPrompt }]
          },
          contents: [
            {
              parts: [{ text: userPrompt }]
            }
          ],
          generationConfig: {
            temperature: 0.4,
            maxOutputTokens
          }
        })
      }
    );

    const rawBody = await response.text();
    if (!response.ok) {
      throw new HttpError(
        502,
        parseGeminiError(rawBody) ?? `Gemini request failed with HTTP ${response.status}.`
      );
    }

    let parsed: GeminiGenerateContentResponse;
    try {
      parsed = JSON.parse(rawBody) as GeminiGenerateContentResponse;
    } catch {
      throw new HttpError(502, "Unable to parse Gemini response.");
    }

    const candidates = parsed.candidates ?? [];
    const bestCandidate = candidates.reduce<GeminiCandidate | null>((best, candidate) => {
      const currentLength = candidate.content?.parts
        ?.map((part) => part.text?.trim() ?? "")
        .join("\n")
        .trim()
        .length ?? 0;
      const bestLength = best?.content?.parts
        ?.map((part) => part.text?.trim() ?? "")
        .join("\n")
        .trim()
        .length ?? 0;
      return currentLength > bestLength ? candidate : best;
    }, null);

    const text = bestCandidate?.content?.parts
      ?.map((part) => part.text?.trim() ?? "")
      .join("\n")
      .trim() ?? "";

    return jsonResponse(200, {
      text,
      finishReason: asNonEmptyString(bestCandidate?.finishReason) ?? null
    });
  } catch (error) {
    console.error("ask-mochi failed", error);
    if (isHttpError(error)) {
      return jsonResponse(error.status, { message: error.message });
    }
    const message = error instanceof Error ? error.message : "Unexpected error.";
    return jsonResponse(500, { message });
  }
});

function parseGeminiError(raw: string): string | null {
  if (!raw.trim()) return null;
  try {
    const parsed = JSON.parse(raw) as {
      error?: {
        message?: unknown;
      };
    };
    return asNonEmptyString(parsed.error?.message) ?? null;
  } catch {
    return null;
  }
}
