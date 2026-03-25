package com.chelumore.xpenze.domain.repository

import kotlinx.coroutines.flow.Flow

data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val isEmailVerified: Boolean = false
)

interface AuthRepository {
    /**
     * Emits the latest signed-in user (or null). This updates on sign-in/out and token refresh.
     */
    fun observeAuthState(): Flow<AuthUser?>

    /**
     * Email/password sign up. Typically followed by email verification.
     */
    suspend fun signUp(email: String, password: String, displayName: String? = null): AuthUser

    /**
     * Email/password sign in.
     */
    suspend fun signIn(email: String, password: String): AuthUser

    /**
     * Sends password reset email.
     */
    suspend fun sendPasswordReset(email: String)

    /**
     * Sends an email verification link to the currently signed-in user.
     */
    suspend fun sendEmailVerification()

    /**
     * Reloads the current Firebase user and returns the latest state.
     */
    suspend fun reloadCurrentUser(): AuthUser?

    fun getCurrentUser(): AuthUser?

    suspend fun getIdToken(forceRefresh: Boolean = false): String?

    /**
     * Signs in/up with Google ID token (Credential Manager / GoogleID) and FirebaseAuth.
     */
    suspend fun signInWithGoogle(idToken: String): AuthUser

    /**
     * Deletes the currently signed-in Firebase user.
     *
     * Note: Firebase may require a recent login; callers should handle failures and prompt re-auth.
     */
    suspend fun deleteAccount()

    suspend fun signOut()
}


