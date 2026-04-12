package com.chelumore.xpenze.core.ai

import com.chelumore.xpenze.BuildConfig
import com.chelumore.xpenze.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class SupabaseMochiApi @Inject constructor(
    private val authRepository: AuthRepository,
    private val json: Json
) {
    private val client = OkHttpClient()

    fun isConfigured(): Boolean = projectUrl().isNotBlank()

    suspend fun requestCompletion(
        systemPrompt: String,
        userPrompt: String,
        maxOutputTokens: Int
    ): ApiCallResult {
        val requestJson = json.encodeToString(
            MochiRequest(
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                maxOutputTokens = maxOutputTokens
            )
        )
        return postFunction(requestJson)
    }

    private suspend fun postFunction(requestJson: String): ApiCallResult {
        if (!isConfigured()) return ApiCallResult.NotConfigured
        val firstAttempt = executeRequest(
            requestJson = requestJson,
            forceRefreshToken = false
        )
        return if (firstAttempt is ApiCallResult.Failure && firstAttempt.statusCode == 401) {
            executeRequest(
                requestJson = requestJson,
                forceRefreshToken = true
            )
        } else {
            firstAttempt
        }
    }

    private suspend fun executeRequest(
        requestJson: String,
        forceRefreshToken: Boolean
    ): ApiCallResult {
        val idToken = authRepository.getIdToken(forceRefresh = forceRefreshToken)
            ?.takeIf { it.isNotBlank() }
            ?: return ApiCallResult.Failure("Sign in is required.", statusCode = 401)

        val request = Request.Builder()
            .url("${projectUrl().trimEnd('/')}/functions/v1/$FUNCTION_PATH")
            .post(requestJson.toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer $idToken")
            .header("Content-Type", "application/json")
            .build()

        return runCatching {
            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return ApiCallResult.Failure(
                        message = parseErrorMessage(rawBody)
                            ?: "Mochi backend request failed (${response.code}).",
                        statusCode = response.code
                    )
                }

                val payload = runCatching {
                    json.decodeFromString<MochiResponse>(rawBody)
                }.getOrElse {
                    return ApiCallResult.Failure("Mochi backend returned an invalid response.")
                }
                ApiCallResult.Success(
                    text = payload.text,
                    finishReason = payload.finishReason
                )
            }
        }.getOrElse { error ->
            ApiCallResult.Failure(error.message ?: "Unable to reach Mochi backend.")
        }
    }

    private fun parseErrorMessage(rawBody: String): String? {
        if (rawBody.isBlank()) return null
        return runCatching {
            json.decodeFromString<ErrorPayload>(rawBody).message.trim().takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun projectUrl(): String = BuildConfig.SUPABASE_PROJECT_URL.trim()

    @Serializable
    private data class MochiRequest(
        val systemPrompt: String,
        val userPrompt: String,
        val maxOutputTokens: Int
    )

    @Serializable
    private data class MochiResponse(
        val text: String = "",
        val finishReason: String? = null
    )

    @Serializable
    private data class ErrorPayload(
        val message: String = ""
    )

    sealed interface ApiCallResult {
        data object NotConfigured : ApiCallResult

        data class Success(
            val text: String,
            val finishReason: String?
        ) : ApiCallResult

        data class Failure(
            val message: String,
            val statusCode: Int? = null
        ) : ApiCallResult
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        const val FUNCTION_PATH = "ask-mochi"
    }
}
