package com.chelumore.xpenze.feature.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

sealed interface GoogleSignInOutcome {
    data class Success(val idToken: String) : GoogleSignInOutcome
    data object Cancelled : GoogleSignInOutcome
    data class Error(val message: String, val throwable: Throwable? = null) : GoogleSignInOutcome
}

/**
 * google-services plugin generates `default_web_client_id` when `google-services.json` is present.
 * We look it up dynamically so the project still compiles if Firebase config isn't added yet.
 */
fun Context.googleWebClientIdOrNull(): String? {
    val resId = resources.getIdentifier("default_web_client_id", "string", packageName)
    if (resId == 0) return null
    return getString(resId).takeIf { it.isNotBlank() }
}

/**
 * Starts the Google sign-in flow using Credential Manager and returns an ID token.
 *
 * Note: Pass an Activity context (not Application context) so the account chooser UI can render.
 */
suspend fun requestGoogleIdToken(
    activity: Activity,
    serverClientId: String
): GoogleSignInOutcome {
    if (serverClientId.isBlank()) {
        return GoogleSignInOutcome.Error("Missing Web client ID (serverClientId).")
    }

    val credentialManager = CredentialManager.create(activity)
    val googleIdOption = GetGoogleIdOption.Builder()
        .setServerClientId(serverClientId)
        .setFilterByAuthorizedAccounts(false)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    return try {
        val result = credentialManager.getCredential(
            context = activity,
            request = request
        )

        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val tokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            GoogleSignInOutcome.Success(tokenCredential.idToken)
        } else {
            GoogleSignInOutcome.Error("Unexpected credential type.")
        }
    } catch (_: GetCredentialCancellationException) {
        GoogleSignInOutcome.Cancelled
    } catch (e: GetCredentialException) {
        GoogleSignInOutcome.Error(
            e.toUserFriendlyAuthMessage("Google sign-in couldn't be completed. Please try again."),
            e
        )
    } catch (e: Exception) {
        GoogleSignInOutcome.Error(
            e.toUserFriendlyAuthMessage("Google sign-in couldn't be completed. Please try again."),
            e
        )
    }
}


