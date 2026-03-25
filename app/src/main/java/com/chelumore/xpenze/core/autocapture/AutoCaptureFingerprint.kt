package com.chelumore.xpenze.core.autocapture

import java.security.MessageDigest
import java.text.Normalizer

internal fun normalizeCaptureText(value: String): String {
    val normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
    return normalized
        .lowercase()
        .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

internal fun captureFingerprint(vararg parts: String?): String {
    val input = parts
        .mapNotNull { it?.trim()?.takeIf { value -> value.isNotEmpty() } }
        .joinToString("||")
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(input.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { byte -> "%02x".format(byte) }
}
