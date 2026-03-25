# Premium Roadmap TODO

Last updated: 2026-02-05

## Phase 1 - Premium Feature Buildout

- [x] Add Gemini AI client integration
  - [x] Add secure config for Gemini API key via `BuildConfig` (local/env driven)
  - [x] Create AI service layer for finance Q&A prompts
  - [x] Add error handling and user-friendly fallback messages
- [x] Build AI chat experience
  - [x] Create `AI Chat` screen with message history and input/send flow
  - [x] Add finance-context prompt builder (transactions, budgets, trends)
  - [x] Add loading states and retry support
- [x] Add AI entry points
  - [x] Add navigation route for AI chat
  - [x] Add `Open AI Chat` action from Reports
- [x] Add free-tier ad scaffolding (premium removes ads)
  - [x] Add reusable ad slot component
  - [x] Show ad slots in selected free-tier screens
  - [x] Hide ad slots when premium/trial active

## Phase 2 - Strict Premium Or Trial Gating

- [x] Add centralized entitlement model (`hasPremiumAccess = isPremium || trialActive`)
- [x] Refactor premium checks to use centralized entitlement
- [x] Lock AI Coach and AI Chat behind premium/trial entitlement
- [x] Lock advanced reports and premium insight blocks behind entitlement

## Phase 3 - Paywall Lock States For Free Users

- [x] Add locked-state cards with clear value proposition
- [x] Add CTA paths (`Start 7-day trial`, `Upgrade to Premium`)
- [x] Add contextual paywall states in all premium surfaces

## Phase 4 - One-Time 7-Day Trial Enforcement

- [x] Add persistent trial metadata
  - [x] `trialStartedAt`
  - [x] `trialExpiresAt`
  - [x] `trialConsumed` (one-time use)
- [x] Enforce one-time trial start rule per account
- [x] Auto-expire trial and re-lock premium features (live entitlement ticker while app is open)
- [x] Keep UI labels accurate (`Premium`, `Trial active`, `Free`)

## Phase 5 - QA And Hardening

- [~] Validate entitlement edge cases (logout/login, reinstall, expiry transitions)
  - [x] Added executable QA matrix: `entitlement_qa_matrix.md`
- [x] Add backend/server validation for per-account trial enforcement (Firestore transaction + startup sync)
- [ ] Validate AI behavior with empty/noisy datasets
- [ ] Validate ad visibility logic for free vs premium/trial
- [ ] Add/extend tests for entitlement and trial state transitions
- [x] Run full project checks (`build`, `test`, and lint)
