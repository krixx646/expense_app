package com.chelumore.xpenze.core.billing

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
class SupabaseBillingApi @Inject constructor(
    private val authRepository: AuthRepository,
    private val json: Json
) {
    private val client = OkHttpClient()

    fun isConfigured(): Boolean = projectUrl().isNotBlank()

    suspend fun verifyPurchase(
        productId: String,
        purchaseToken: String,
        packageName: String
    ): ApiCallResult {
        val requestJson = json.encodeToString(
            VerifyPurchaseRequest(
                productId = productId,
                purchaseToken = purchaseToken,
                packageName = packageName
            )
        )
        return postFunction(
            path = VERIFY_PURCHASE_PATH,
            requestJson = requestJson
        )
    }

    suspend fun refreshPurchase(): ApiCallResult {
        return postFunction(
            path = REFRESH_PURCHASE_PATH,
            requestJson = "{}"
        )
    }

    suspend fun verifyFounderReward(
        productId: String,
        purchaseToken: String,
        packageName: String
    ): FounderRewardApiCallResult {
        val requestJson = json.encodeToString(
            VerifyPurchaseRequest(
                productId = productId,
                purchaseToken = purchaseToken,
                packageName = packageName
            )
        )
        return postFounderRewardFunction(
            path = VERIFY_FOUNDER_REWARD_PATH,
            requestJson = requestJson
        )
    }

    suspend fun refreshFounderReward(): FounderRewardApiCallResult {
        return postFounderRewardFunction(
            path = REFRESH_FOUNDER_REWARD_PATH,
            requestJson = "{}"
        )
    }

    suspend fun activateFounderReward(): FounderRewardApiCallResult {
        return postFounderRewardFunction(
            path = ACTIVATE_FOUNDER_REWARD_PATH,
            requestJson = "{}"
        )
    }

    private suspend fun postFunction(
        path: String,
        requestJson: String
    ): ApiCallResult {
        if (!isConfigured()) return ApiCallResult.NotConfigured
        val firstAttempt = executeRequest(
            path = path,
            requestJson = requestJson,
            forceRefreshToken = false
        )
        return if (firstAttempt is ApiCallResult.Failure && firstAttempt.statusCode == 401) {
            executeRequest(
                path = path,
                requestJson = requestJson,
                forceRefreshToken = true
            )
        } else {
            firstAttempt
        }
    }

    private suspend fun postFounderRewardFunction(
        path: String,
        requestJson: String
    ): FounderRewardApiCallResult {
        if (!isConfigured()) return FounderRewardApiCallResult.NotConfigured
        val firstAttempt = executeFounderRewardRequest(
            path = path,
            requestJson = requestJson,
            forceRefreshToken = false
        )
        return if (firstAttempt is FounderRewardApiCallResult.Failure && firstAttempt.statusCode == 401) {
            executeFounderRewardRequest(
                path = path,
                requestJson = requestJson,
                forceRefreshToken = true
            )
        } else {
            firstAttempt
        }
    }

    private suspend fun executeRequest(
        path: String,
        requestJson: String,
        forceRefreshToken: Boolean
    ): ApiCallResult {
        val idToken = authRepository.getIdToken(forceRefresh = forceRefreshToken)
            ?.takeIf { it.isNotBlank() }
            ?: return ApiCallResult.Failure("Sign in is required.", statusCode = 401)

        val request = Request.Builder()
            .url("${projectUrl().trimEnd('/')}/functions/v1/$path")
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
                            ?: "Premium backend request failed (${response.code}).",
                        statusCode = response.code
                    )
                }

                val payload = runCatching {
                    json.decodeFromString<BillingVerificationPayload>(rawBody)
                }.getOrElse {
                    return ApiCallResult.Failure("Premium backend returned an invalid response.")
                }
                ApiCallResult.Success(payload)
            }
        }.getOrElse { error ->
            ApiCallResult.Failure(error.message ?: "Unable to reach premium backend.")
        }
    }

    private suspend fun executeFounderRewardRequest(
        path: String,
        requestJson: String,
        forceRefreshToken: Boolean
    ): FounderRewardApiCallResult {
        val idToken = authRepository.getIdToken(forceRefresh = forceRefreshToken)
            ?.takeIf { it.isNotBlank() }
            ?: return FounderRewardApiCallResult.Failure("Sign in is required.", statusCode = 401)

        val request = Request.Builder()
            .url("${projectUrl().trimEnd('/')}/functions/v1/$path")
            .post(requestJson.toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer $idToken")
            .header("Content-Type", "application/json")
            .build()

        return runCatching {
            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return FounderRewardApiCallResult.Failure(
                        message = parseErrorMessage(rawBody)
                            ?: "Founder reward backend request failed (${response.code}).",
                        statusCode = response.code
                    )
                }

                val payload = runCatching {
                    json.decodeFromString<FounderRewardPayload>(rawBody)
                }.getOrElse {
                    return FounderRewardApiCallResult.Failure(
                        "Founder reward backend returned an invalid response."
                    )
                }
                FounderRewardApiCallResult.Success(payload)
            }
        }.getOrElse { error ->
            FounderRewardApiCallResult.Failure(
                error.message ?: "Unable to reach founder reward backend."
            )
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
    private data class VerifyPurchaseRequest(
        val productId: String,
        val purchaseToken: String,
        val packageName: String
    )

    @Serializable
    private data class ErrorPayload(
        val message: String = ""
    )

    @Serializable
    data class BillingVerificationPayload(
        val isPremium: Boolean = false,
        val productId: String = "",
        val purchaseTime: Long = 0L,
        val expiryTime: Long = 0L,
        val isAutoRenewing: Boolean = false,
        val acknowledgedAt: Long? = null,
        val purchaseTokenHash: String = "",
        val source: String = "",
        val message: String = ""
    )

    @Serializable
    data class FounderRewardPayload(
        val hasFounderReward: Boolean = false,
        val productId: String = "",
        val purchaseTime: Long = 0L,
        val acknowledgedAt: Long? = null,
        val purchaseTokenHash: String = "",
        val founderBadgeGranted: Boolean = false,
        val founderTrialActivated: Boolean = false,
        val trialStartedAt: Long = 0L,
        val trialExpiresAt: Long = 0L,
        val newlyActivated: Boolean = false,
        val source: String = "",
        val message: String = ""
    )

    sealed interface ApiCallResult {
        data object NotConfigured : ApiCallResult

        data class Success(
            val payload: BillingVerificationPayload
        ) : ApiCallResult

        data class Failure(
            val message: String,
            val statusCode: Int? = null
        ) : ApiCallResult
    }

    sealed interface FounderRewardApiCallResult {
        data object NotConfigured : FounderRewardApiCallResult

        data class Success(
            val payload: FounderRewardPayload
        ) : FounderRewardApiCallResult

        data class Failure(
            val message: String,
            val statusCode: Int? = null
        ) : FounderRewardApiCallResult
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        const val VERIFY_PURCHASE_PATH = "verify-play-subscription"
        const val REFRESH_PURCHASE_PATH = "refresh-play-subscription"
        const val VERIFY_FOUNDER_REWARD_PATH = "verify-play-founder-reward"
        const val REFRESH_FOUNDER_REWARD_PATH = "refresh-founder-reward"
        const val ACTIVATE_FOUNDER_REWARD_PATH = "claim-founder-reward"
    }
}
