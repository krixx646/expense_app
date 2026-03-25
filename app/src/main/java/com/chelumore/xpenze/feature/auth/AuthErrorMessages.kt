package com.chelumore.xpenze.feature.auth

import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

internal fun Throwable.toUserFriendlyAuthMessage(defaultMessage: String): String {
    val raw = message?.trim().orEmpty()
    return when (this) {
        is NoCredentialException -> "No Google account was available on this device. Add one or use email sign-in."
        is FirebaseAuthInvalidUserException -> "No account was found for that email address."
        is FirebaseAuthUserCollisionException -> "An account already exists with this email. Try logging in instead."
        is FirebaseAuthWeakPasswordException -> "Choose a stronger password with at least 6 characters."
        is FirebaseAuthInvalidCredentialsException -> invalidCredentialsMessage(raw)
        is FirebaseTooManyRequestsException -> "Too many attempts right now. Please wait a moment and try again."
        is FirebaseNetworkException,
        is IOException,
        is SocketException,
        is SocketTimeoutException,
        is UnknownHostException -> "Connection problem. Please check your internet and try again."
        is GetCredentialException -> {
            if (raw.contains("No credentials available", ignoreCase = true)) {
                "No Google account was available on this device. Add one or use email sign-in."
            } else {
                "Google sign-in couldn't be completed. Please try again."
            }
        }
        else -> fallbackMessage(raw, defaultMessage)
    }
}

private fun invalidCredentialsMessage(raw: String): String {
    return when {
        raw.contains("badly formatted", ignoreCase = true) ||
            raw.contains("invalid email", ignoreCase = true) -> "Enter a valid email address."
        raw.contains("password is invalid", ignoreCase = true) ||
            raw.contains("wrong password", ignoreCase = true) ||
            raw.contains("invalid login credentials", ignoreCase = true) -> "Incorrect email or password."
        else -> "Incorrect email or password."
    }
}

private fun fallbackMessage(raw: String, defaultMessage: String): String {
    return when {
        raw.contains("unexpected end of stream", ignoreCase = true) ||
            raw.contains("failed to connect", ignoreCase = true) ||
            raw.contains("connection reset", ignoreCase = true) ||
            raw.contains("timeout", ignoreCase = true) ||
            raw.contains("network error", ignoreCase = true) -> {
            "Connection problem. Please check your internet and try again."
        }
        raw.contains("there is no user record", ignoreCase = true) ||
            raw.contains("user not found", ignoreCase = true) -> {
            "No account was found for that email address."
        }
        raw.contains("email address is badly formatted", ignoreCase = true) -> {
            "Enter a valid email address."
        }
        raw.contains("password is invalid", ignoreCase = true) ||
            raw.contains("wrong password", ignoreCase = true) -> {
            "Incorrect email or password."
        }
        raw.contains("no credentials available", ignoreCase = true) -> {
            "No Google account was available on this device. Add one or use email sign-in."
        }
        raw.isNotBlank() && looksSafeForUsers(raw) -> raw
        else -> defaultMessage
    }
}

private fun looksSafeForUsers(message: String): Boolean {
    return !message.contains("okhttp", ignoreCase = true) &&
        !message.contains("exception", ignoreCase = true) &&
        !message.contains("com.android", ignoreCase = true) &&
        !message.contains("{") &&
        !message.contains("}") &&
        !message.contains("@")
}
