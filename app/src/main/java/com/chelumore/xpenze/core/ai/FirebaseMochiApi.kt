package com.chelumore.xpenze.core.ai

import android.content.Context
import com.chelumore.xpenze.BuildConfig
import com.chelumore.xpenze.core.common.await
import com.google.firebase.FirebaseApp
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseMochiApi @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun requestCompletion(
        systemPrompt: String,
        userPrompt: String,
        maxOutputTokens: Int
    ): ApiCallResult {
        val functions = functionsOrNull() ?: return ApiCallResult.NotConfigured

        val data = runCatching {
            functions
                .getHttpsCallable(FUNCTION_ASK_MOCHI)
                .call(
                    mapOf(
                        FIELD_SYSTEM_PROMPT to systemPrompt,
                        FIELD_USER_PROMPT to userPrompt,
                        FIELD_MAX_OUTPUT_TOKENS to maxOutputTokens
                    )
                )
                .await()
                .getData()
        }.getOrElse { error ->
            val message = when (error) {
                is FirebaseFunctionsException -> error.message
                else -> error.message
            } ?: "Mochi backend is unavailable."
            return ApiCallResult.Failure(message)
        }

        val payload = data as? Map<*, *> ?: return ApiCallResult.Failure(
            "Mochi backend returned an invalid response."
        )
        return ApiCallResult.Success(
            text = payload[FIELD_TEXT] as? String ?: "",
            finishReason = (payload[FIELD_FINISH_REASON] as? String)?.trim()?.ifBlank { null }
        )
    }

    private fun functionsOrNull(): FirebaseFunctions? {
        val app = runCatching { FirebaseApp.getInstance() }.getOrNull()
            ?: runCatching { FirebaseApp.initializeApp(context) }.getOrNull()
            ?: return null
        val region = BuildConfig.FIREBASE_FUNCTIONS_REGION.ifBlank { DEFAULT_FUNCTIONS_REGION }
        return runCatching { FirebaseFunctions.getInstance(app, region) }.getOrNull()
    }

    sealed interface ApiCallResult {
        data object NotConfigured : ApiCallResult

        data class Success(
            val text: String,
            val finishReason: String?
        ) : ApiCallResult

        data class Failure(
            val message: String
        ) : ApiCallResult
    }

    private companion object {
        const val DEFAULT_FUNCTIONS_REGION = "us-central1"
        const val FUNCTION_ASK_MOCHI = "askMochi"
        const val FIELD_SYSTEM_PROMPT = "systemPrompt"
        const val FIELD_USER_PROMPT = "userPrompt"
        const val FIELD_MAX_OUTPUT_TOKENS = "maxOutputTokens"
        const val FIELD_TEXT = "text"
        const val FIELD_FINISH_REASON = "finishReason"
    }
}
