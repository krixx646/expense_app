# Play Billing Server Verification Setup

This backend verifies Google Play subscription purchase tokens on the server and writes premium entitlement to Firestore.

## What is included

- Callable function: `verifyPlaySubscriptionPurchase`
- Callable function: `refreshPlaySubscriptionEntitlement`
- Server-side Firestore writes to:
  - `users/{uid}/entitlements/subscription_state`
  - `purchase_tokens/{purchaseTokenHash}`
  - `server_private_play_tokens/{uid}`

## 1) Install and build functions

From `backend/functions`:

```bash
npm install
npm run build
```

## 2) Provide credentials securely (do NOT commit secrets)

Create a Google Cloud service account key JSON with Android Publisher access, then set it as a Firebase Secret:

```bash
firebase functions:secrets:set PLAY_SERVICE_ACCOUNT_JSON --data-file "/absolute/path/to/play-service-account.json"
```

Set non-secret params:

```bash
firebase functions:params:set PLAY_PACKAGE_NAME="com.chelumore.xpenze"
firebase functions:params:set PLAY_ALLOWED_PRODUCTS="xpenze_premium_monthly,xpenze_premium_yearly"
```

## 3) Grant Play Console access to the service account

In Google Play Console:

1. Open `Users and permissions`.
2. Invite the service account email from the JSON key (`client_email`).
3. Grant subscription verification permissions (at minimum, subscriptions/orders read access needed for verification).

Also ensure Android Publisher API is enabled in the Google Cloud project connected to Play Console.

## 4) Deploy functions

From project root (or where your Firebase config lives):

```bash
firebase deploy --only functions
```

## 5) Firestore rules hardening

Merge the entitlement-specific rule blocks from:

- `backend/firestore_rules_additions.txt`

This ensures client apps can read their entitlement but cannot write premium state directly.

## 6) App Check requirement

These callable functions use `enforceAppCheck: true`.

- Release: Play Integrity provider is used by the Android app.
- Debug: debug provider is enabled; register the debug token in Firebase App Check console for local builds.

