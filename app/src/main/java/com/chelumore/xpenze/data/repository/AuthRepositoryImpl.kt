package com.chelumore.xpenze.data.repository

import android.content.Context
import com.chelumore.xpenze.core.common.await
import com.chelumore.xpenze.core.common.Constants
import com.chelumore.xpenze.core.database.AppDatabase
import com.chelumore.xpenze.core.datastore.PreferencesDataStore
import com.chelumore.xpenze.domain.repository.AuthRepository
import com.chelumore.xpenze.domain.repository.AuthUser
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesDataStore: PreferencesDataStore,
    private val appDatabase: AppDatabase
) : AuthRepository {

    private fun firebaseAppOrNull(): FirebaseApp? {
        // Prefer existing default app (auto-initialized via google-services plugin),
        // fallback to manual initialization (useful in some test setups).
        return runCatching { FirebaseApp.getInstance() }.getOrNull()
            ?: runCatching { FirebaseApp.initializeApp(context) }.getOrNull()
    }

    private fun authOrNull(): FirebaseAuth? = firebaseAppOrNull()?.let { FirebaseAuth.getInstance(it) }

    private fun authOrThrow(): FirebaseAuth {
        return authOrNull()
            ?: throw IllegalStateException(
                "Firebase is not configured. Ensure `expense_app/app/google-services.json` exists, rebuild, and enable Email/Password in Firebase Console."
            )
    }

    override fun observeAuthState(): Flow<AuthUser?> = callbackFlow {
        val auth = authOrNull()
        if (auth == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        fun emit() {
            trySend(auth.currentUser?.toDomain())
        }

        val authListener = FirebaseAuth.AuthStateListener { emit() }
        val tokenListener = FirebaseAuth.IdTokenListener { emit() }

        auth.addAuthStateListener(authListener)
        auth.addIdTokenListener(tokenListener)

        // Initial value
        emit()

        awaitClose {
            auth.removeAuthStateListener(authListener)
            auth.removeIdTokenListener(tokenListener)
        }
    }.distinctUntilChanged()

    override suspend fun signUp(email: String, password: String, displayName: String?): AuthUser {
        require(email.isNotBlank()) { "Email is required" }
        require(password.length >= 6) { "Password must be at least 6 characters" }

        val auth = authOrThrow()
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: throw IllegalStateException("Sign up failed")
        val cleanedDisplayName = displayName
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        if (!cleanedDisplayName.isNullOrBlank()) {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(cleanedDisplayName)
                .build()
            runCatching { user.updateProfile(profileUpdates).await() }
            runCatching { user.reload().await() }
        }
        // Trigger email verification
        runCatching { user.sendEmailVerification().await() }

        establishLocalSession(user, displayName = cleanedDisplayName)

        return user.toDomain()
    }

    override suspend fun signIn(email: String, password: String): AuthUser {
        require(email.isNotBlank()) { "Email is required" }
        require(password.isNotBlank()) { "Password is required" }

        val auth = authOrThrow()
        val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: throw IllegalStateException("Sign in failed")
        establishLocalSession(user)

        return user.toDomain()
    }

    override suspend fun sendPasswordReset(email: String) {
        require(email.isNotBlank()) { "Email is required" }
        val auth = authOrThrow()
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    override suspend fun sendEmailVerification() {
        val auth = authOrThrow()
        val user = auth.currentUser ?: throw IllegalStateException("No signed-in user")
        user.sendEmailVerification().await()
    }

    override suspend fun reloadCurrentUser(): AuthUser? {
        val auth = authOrNull() ?: return null
        val user = auth.currentUser ?: return null
        user.reload().await()
        return auth.currentUser?.toDomain()
    }

    override fun getCurrentUser(): AuthUser? {
        val user = authOrNull()?.currentUser ?: return null
        return user.toDomain()
    }

    override suspend fun getIdToken(forceRefresh: Boolean): String? {
        val user = authOrNull()?.currentUser ?: return null
        return user.getIdToken(forceRefresh).await().token
    }

    override suspend fun signInWithGoogle(idToken: String): AuthUser {
        require(idToken.isNotBlank()) { "Missing Google ID token" }

        val auth = authOrThrow()
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val user = result.user ?: throw IllegalStateException("Google sign in failed")
        establishLocalSession(user)

        return user.toDomain()
    }

    override suspend fun deleteAccount() {
        val auth = authOrThrow()
        val user = auth.currentUser ?: throw IllegalStateException("No signed-in user")

        // This may fail if Firebase requires recent sign-in.
        user.delete().await()

        runCatching { auth.signOut() }
        withContext(Dispatchers.IO) {
            appDatabase.clearAllTables()
        }
        preferencesDataStore.clearUserData()
        preferencesDataStore.clearTrialState()
        preferencesDataStore.setOnboardingComplete(false)
        preferencesDataStore.setDefaultAccountId(null)
        preferencesDataStore.setDefaultCurrency(Constants.DEFAULT_CURRENCY)
        preferencesDataStore.updateLastSyncTime(0L)
    }

    override suspend fun signOut() {
        runCatching { authOrNull()?.signOut() }
        withContext(Dispatchers.IO) {
            appDatabase.clearAllTables()
        }
        preferencesDataStore.clearUserData()
        preferencesDataStore.clearTrialState()
        preferencesDataStore.setOnboardingComplete(false)
        preferencesDataStore.setDefaultAccountId(null)
        preferencesDataStore.setDefaultCurrency(Constants.DEFAULT_CURRENCY)
        preferencesDataStore.updateLastSyncTime(0L)
    }

    /**
     * Establishes the local session for [user] after a successful authentication.
     *
     * Three cases, in order:
     *  - Re-authenticating as the same account: refresh the stored profile only.
     *  - A guest adopting an account: keep every local record, because it belongs to this same
     *    person and [com.chelumore.xpenze.domain.repository.SyncRepository.fullSync] pushes it up
     *    to the new account. Adoption is deferred while an email address is unverified, so an
     *    unverified sign-up never strands the guest behind the verification wall.
     *  - Anything else (a genuine account switch): never keep the prior account's records.
     */
    private suspend fun establishLocalSession(user: FirebaseUser, displayName: String? = null) {
        val currentPrefs = preferencesDataStore.userPreferences.first()
        val resolvedName = displayName ?: user.displayName

        val action = resolveLocalSessionAction(
            previousUserId = currentPrefs.userId,
            isGuestMode = currentPrefs.isGuestMode,
            newUserId = user.uid,
            hasEmail = user.email != null,
            isEmailVerified = user.isEmailVerified
        )

        when (action) {
            LocalSessionAction.DeferGuestAdoption -> return

            LocalSessionAction.PromoteGuest -> {
                preferencesDataStore.promoteGuestToUser(
                    userId = user.uid,
                    email = user.email,
                    name = resolvedName,
                    photoUrl = user.photoUrl?.toString()
                )
                return
            }

            LocalSessionAction.ResetForAccountSwitch -> {
                withContext(Dispatchers.IO) {
                    appDatabase.clearAllTables()
                }
                preferencesDataStore.setOnboardingComplete(false)
                preferencesDataStore.setDefaultAccountId(null)
                preferencesDataStore.setDefaultCurrency(Constants.DEFAULT_CURRENCY)
                preferencesDataStore.updateLastSyncTime(0L)
                preferencesDataStore.setPremium(false)
                preferencesDataStore.clearTrialState()
            }

            LocalSessionAction.RefreshProfile -> Unit
        }

        preferencesDataStore.setUserInfo(
            userId = user.uid,
            email = user.email,
            name = resolvedName,
            photoUrl = user.photoUrl?.toString()
        )
    }

    private fun FirebaseUser.toDomain(): AuthUser {
        return AuthUser(
            uid = uid,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl?.toString(),
            isEmailVerified = isEmailVerified
        )
    }
}


