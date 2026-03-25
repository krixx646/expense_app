# Entitlement Edge-Case QA Matrix

Last updated: 2026-02-05

Scope:
- Premium entitlement correctness (`paid OR active trial`)
- One-time trial enforcement (per account)
- Cross-device/reinstall behavior
- Trial expiry transitions while app is open/offline

## Preconditions

- App build: latest debug build with backend trial enforcement.
- Firestore rules include `users/{uid}/entitlements/trial_state` access for owner.
- Test accounts:
  - `A`: fresh account (no trial used)
  - `B`: account with consumed/expired trial
- Optional second device/emulator for cross-device checks.

## Matrix

| ID | Scenario | Steps | Expected |
|---|---|---|---|
| E1 | Fresh user starts trial online | Sign in as `A` -> Subscription -> tap `Start 7-day trial` | Trial starts; premium features unlock; ad slots hidden; `trial_state` doc created in Firestore |
| E2 | Trial start retried on same account (same device) | While active/after consumed, tap trial again | User sees already active or already used message; no second grant |
| E3 | Trial start retried on same account (other device) | Sign in as `A` on device 2 -> attempt trial | Same result as E2; no second grant |
| E4 | Logout/login same account | Start trial on `A` -> logout -> login `A` | Entitlement restores from backend; status remains correct |
| E5 | Logout/login different account | Start trial on `A` -> logout -> login `B` | Entitlement reflects `B` only; no entitlement leak from `A` |
| E6 | Reinstall + login same account | Start/consume trial on `A` -> reinstall app -> login `A` | Trial state restored from backend; one-time rule still enforced |
| E7 | Trial expires while app open (online) | Start short trial (or adjust expiry in backend) and keep app open past expiry | Premium locks automatically without app restart |
| E8 | Trial expires while app open (offline) | Start trial -> disconnect network -> wait until expiry | Local ticker locks premium at expiry time even offline |
| E9 | Offline trial claim | Disable network -> tap trial | Trial is blocked with backend verification message; no local fallback grant |
| E10 | Paid premium precedence | Create active paid subscription and expired trial | Premium access remains unlocked because paid plan is active |
| E11 | Ad gating check | Compare Reports/Dashboard as free vs trial/premium | Ads shown only on free; hidden on trial/premium |
| E12 | AI coach gating check | Open AI chat as free vs trial/premium | Free sees lock/paywall; trial/premium can chat |

## Validation Notes

- Firestore path to inspect:
  - `users/{uid}/entitlements/trial_state`
- Required fields:
  - `trialConsumed: true`
  - `trialStartedAt: int`
  - `trialExpiresAt: int`
  - `updatedAt: int`
- Local reset:
  - Sign out and clear app data (or reinstall), then sign in again

## Exit Criteria

- All scenarios `E1` through `E12` pass.
- No entitlement mismatch after auth changes, reinstall, or expiry transitions.
- No premium feature remains unlocked after trial expiry unless paid premium is active.
