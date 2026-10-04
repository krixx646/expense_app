package com.chelumore.xpenze.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalSessionActionTest {

    @Test
    fun `guest signing in with a verified google account is promoted, not wiped`() {
        val action = resolveLocalSessionAction(
            previousUserId = null,
            isGuestMode = true,
            newUserId = "uid-1",
            hasEmail = true,
            isEmailVerified = true
        )

        assertEquals(LocalSessionAction.PromoteGuest, action)
    }

    @Test
    fun `guest signing up with an unverified email keeps guest access instead of being stranded`() {
        val action = resolveLocalSessionAction(
            previousUserId = null,
            isGuestMode = true,
            newUserId = "uid-1",
            hasEmail = true,
            isEmailVerified = false
        )

        assertEquals(LocalSessionAction.DeferGuestAdoption, action)
    }

    @Test
    fun `guest with an account that has no email address is promoted`() {
        val action = resolveLocalSessionAction(
            previousUserId = null,
            isGuestMode = true,
            newUserId = "uid-1",
            hasEmail = false,
            isEmailVerified = false
        )

        assertEquals(LocalSessionAction.PromoteGuest, action)
    }

    @Test
    fun `first sign-in without a guest session still resets local state`() {
        val action = resolveLocalSessionAction(
            previousUserId = null,
            isGuestMode = false,
            newUserId = "uid-1",
            hasEmail = true,
            isEmailVerified = true
        )

        assertEquals(LocalSessionAction.ResetForAccountSwitch, action)
    }

    @Test
    fun `switching to a different account wipes the previous account's records`() {
        val action = resolveLocalSessionAction(
            previousUserId = "uid-1",
            isGuestMode = false,
            newUserId = "uid-2",
            hasEmail = true,
            isEmailVerified = true
        )

        assertEquals(LocalSessionAction.ResetForAccountSwitch, action)
    }

    @Test
    fun `re-authenticating as the same account only refreshes the profile`() {
        val action = resolveLocalSessionAction(
            previousUserId = "uid-1",
            isGuestMode = false,
            newUserId = "uid-1",
            hasEmail = true,
            isEmailVerified = true
        )

        assertEquals(LocalSessionAction.RefreshProfile, action)
    }

    @Test
    fun `a stale guest flag never overrides an existing signed-in account`() {
        val action = resolveLocalSessionAction(
            previousUserId = "uid-1",
            isGuestMode = true,
            newUserId = "uid-2",
            hasEmail = true,
            isEmailVerified = true
        )

        assertEquals(LocalSessionAction.ResetForAccountSwitch, action)
    }
}
