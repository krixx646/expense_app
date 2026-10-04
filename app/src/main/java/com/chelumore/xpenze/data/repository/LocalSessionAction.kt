package com.chelumore.xpenze.data.repository

/**
 * What should happen to local state when a user successfully authenticates.
 *
 * Extracted from the repository so the rule can be tested without a live Firebase user; getting
 * this wrong either deletes a guest's records or strands them behind email verification.
 */
enum class LocalSessionAction {
    /** Same account as before: only refresh the cached profile fields. */
    RefreshProfile,

    /** Guest signed in with an unverified email. Stay a guest so they keep access to their data. */
    DeferGuestAdoption,

    /** Guest adopting a usable account. Keep every local record and attach it to the account. */
    PromoteGuest,

    /** A genuine account switch. The previous account's records must not leak into the new one. */
    ResetForAccountSwitch
}

fun resolveLocalSessionAction(
    previousUserId: String?,
    isGuestMode: Boolean,
    newUserId: String,
    hasEmail: Boolean,
    isEmailVerified: Boolean
): LocalSessionAction = when {
    !previousUserId.isNullOrBlank() && previousUserId == newUserId -> LocalSessionAction.RefreshProfile
    !previousUserId.isNullOrBlank() -> LocalSessionAction.ResetForAccountSwitch
    !isGuestMode -> LocalSessionAction.ResetForAccountSwitch
    hasEmail && !isEmailVerified -> LocalSessionAction.DeferGuestAdoption
    else -> LocalSessionAction.PromoteGuest
}
