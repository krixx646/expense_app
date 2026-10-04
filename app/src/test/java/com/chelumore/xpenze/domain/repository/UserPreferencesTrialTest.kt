package com.chelumore.xpenze.domain.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserPreferencesTrialTest {

    @Test
    fun `active trial requires signed-in non-guest user`() {
        val now = 10_000L
        val signedIn = UserPreferences(
            userId = "user-1",
            trialConsumed = true,
            trialExpiresAt = 20_000L
        )
        assertTrue(signedIn.isTrialActive(now))
        assertTrue(signedIn.hasPremiumAccess(now))

        val guest = signedIn.copy(userId = null, isGuestMode = true)
        assertFalse(guest.isTrialActive(now))
        assertFalse(guest.hasPremiumAccess(now))

        val signedOut = signedIn.copy(userId = null, isGuestMode = false)
        assertFalse(signedOut.isTrialActive(now))
        assertFalse(signedOut.hasPremiumAccess(now))
    }

    @Test
    fun `trial ending soon only in final two days`() {
        val now = 1_000L
        val twoDays = TRIAL_ENDING_SOON_WINDOW_MS
        val endingSoon = UserPreferences(
            userId = "user-1",
            trialConsumed = true,
            trialExpiresAt = now + twoDays
        )
        assertTrue(endingSoon.isTrialEndingSoon(now))

        val earlyTrial = endingSoon.copy(trialExpiresAt = now + twoDays + 1L)
        assertFalse(earlyTrial.isTrialEndingSoon(now))

        val expired = endingSoon.copy(trialExpiresAt = now)
        assertFalse(expired.isTrialActive(now))
        assertFalse(expired.isTrialEndingSoon(now))
    }

    @Test
    fun `guest never gets trial access even with leftover trial dates`() {
        val now = 10_000L
        val guest = UserPreferences(
            userId = null,
            isGuestMode = true,
            isPremium = false,
            trialConsumed = true,
            trialStartedAt = 1_000L,
            trialExpiresAt = 99_000L
        )
        assertFalse(guest.isTrialActive(now))
        assertFalse(guest.hasPremiumAccess(now))
        assertFalse(guest.isTrialEndingSoon(now))
    }

    @Test
    fun `paid premium remains available when not a guest`() {
        val prefs = UserPreferences(
            userId = "user-1",
            isGuestMode = false,
            isPremium = true,
            trialConsumed = true,
            trialExpiresAt = 0L
        )
        assertFalse(prefs.isTrialActive())
        assertTrue(prefs.hasPremiumAccess())
        assertEquals(true, prefs.isPremium)
    }
}
