import { createRemoteJWKSet, jwtVerify } from "npm:jose@5.9.6";

import { HttpError, asNonEmptyString, requiredEnv } from "./http.ts";

const FIREBASE_JWKS = createRemoteJWKSet(
  new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com")
);

export type FirebaseIdentity = {
  uid: string;
  email: string | null;
};

export async function verifyFirebaseIdentity(request: Request): Promise<FirebaseIdentity> {
  const projectId = requiredEnv("FIREBASE_PROJECT_ID");
  const authorization = request.headers.get("Authorization")?.trim() ?? "";
  if (!authorization.startsWith("Bearer ")) {
    throw new HttpError(401, "Missing Firebase ID token.");
  }

  const idToken = authorization.slice("Bearer ".length).trim();
  if (!idToken) {
    throw new HttpError(401, "Missing Firebase ID token.");
  }

  const { payload } = await jwtVerify(idToken, FIREBASE_JWKS, {
    issuer: `https://securetoken.google.com/${projectId}`,
    audience: projectId
  });

  const uid = asNonEmptyString(payload.sub);
  if (!uid) {
    throw new HttpError(401, "Firebase ID token is invalid.");
  }

  return {
    uid,
    email: asNonEmptyString(payload.email)
  };
}
