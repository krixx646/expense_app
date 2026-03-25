package com.chelumore.xpenze.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chelumore.xpenze.core.common.Constants
import com.chelumore.xpenze.domain.repository.AccountCardStyle
import com.chelumore.xpenze.domain.repository.ThemeMode
import com.chelumore.xpenze.domain.repository.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = Constants.PREFERENCES_NAME
)

@Singleton
class PreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val IS_ONBOARDING_COMPLETE = booleanPreferencesKey("is_onboarding_complete")
        val APP_LANGUAGE_TAG = stringPreferencesKey("app_language_tag")
        val ACCOUNT_CARD_STYLE = stringPreferencesKey("account_card_style")
        val ACCOUNT_CARD_STYLE_OVERRIDES = stringPreferencesKey("account_card_style_overrides")
        val ARE_BALANCES_HIDDEN = booleanPreferencesKey("are_balances_hidden")
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        val DEFAULT_ACCOUNT_ID = stringPreferencesKey("default_account_id")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val BUDGET_ALERT_ENABLED = booleanPreferencesKey("budget_alert_enabled")
        val DAILY_SUMMARY_ENABLED = booleanPreferencesKey("daily_summary_enabled")
        val NOTIFICATION_PERMISSION_PROMPT_LAST_SHOWN_AT = longPreferencesKey("notification_permission_prompt_last_shown_at")
        val APP_OPEN_COUNT = intPreferencesKey("app_open_count")
        val RATE_APP_PROMPT_LAST_SHOWN_AT = longPreferencesKey("rate_app_prompt_last_shown_at")
        val HAS_RATED_ON_PLAY_STORE = booleanPreferencesKey("has_rated_on_play_store")
        val BUDGET_ALERT_ACTIVE_IDS = stringPreferencesKey("budget_alert_active_ids")
        val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_PHOTO_URL = stringPreferencesKey("user_photo_url")
        val AI_CHAT_HISTORY_JSON = stringPreferencesKey("ai_chat_history_json")
        val HAS_SEEN_AI_COACH_CTA = booleanPreferencesKey("has_seen_ai_coach_cta")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val TRIAL_STARTED_AT = longPreferencesKey("trial_started_at")
        val TRIAL_EXPIRES_AT = longPreferencesKey("trial_expires_at")
        val TRIAL_CONSUMED = booleanPreferencesKey("trial_consumed")
        val TRIAL_CONSUMED_USER_IDS = stringPreferencesKey("trial_consumed_user_ids")
        val AUTO_CAPTURE_ENABLED = booleanPreferencesKey("auto_capture_enabled")
        val AUTO_CAPTURE_STATEMENT_IMPORT_ENABLED =
            booleanPreferencesKey("auto_capture_statement_import_enabled")
        val AUTO_CAPTURE_OCR_ENABLED = booleanPreferencesKey("auto_capture_ocr_enabled")
        val AUTO_CAPTURE_NOTIFICATION_ENABLED =
            booleanPreferencesKey("auto_capture_notification_enabled")
        val AUTO_CAPTURE_MERCHANT_MEMORY_ENABLED =
            booleanPreferencesKey("auto_capture_merchant_memory_enabled")
        val AUTO_CAPTURE_RECURRING_SUGGESTIONS_ENABLED =
            booleanPreferencesKey("auto_capture_recurring_suggestions_enabled")
        val AUTO_CAPTURE_DISCLOSURE_ACCEPTED =
            booleanPreferencesKey("auto_capture_disclosure_accepted")
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val userId = prefs[Keys.USER_ID]
        val consumedUserIds = decodeConsumedUserIds(prefs[Keys.TRIAL_CONSUMED_USER_IDS].orEmpty())
        val legacyConsumed = prefs[Keys.TRIAL_CONSUMED] ?: false
        val consumedForCurrentUser = when {
            userId.isNullOrBlank() -> legacyConsumed
            else -> consumedUserIds.contains(userId)
                || (legacyConsumed && consumedUserIds.isEmpty())
        }
        UserPreferences(
            isOnboardingComplete = prefs[Keys.IS_ONBOARDING_COMPLETE] ?: false,
            appLanguageTag = prefs[Keys.APP_LANGUAGE_TAG] ?: Locale.getDefault().toLanguageTag().ifBlank { "en" },
            accountCardStyle = prefs[Keys.ACCOUNT_CARD_STYLE]
                ?.let { value -> runCatching { AccountCardStyle.valueOf(value) }.getOrNull() }
                ?: AccountCardStyle.MATTE,
            accountCardStyleOverrides = decodeAccountCardStyleOverrides(
                prefs[Keys.ACCOUNT_CARD_STYLE_OVERRIDES].orEmpty()
            ),
            areBalancesHidden = prefs[Keys.ARE_BALANCES_HIDDEN] ?: false,
            defaultCurrency = prefs[Keys.DEFAULT_CURRENCY] ?: Constants.DEFAULT_CURRENCY,
            defaultAccountId = prefs[Keys.DEFAULT_ACCOUNT_ID],
            themeMode = prefs[Keys.THEME_MODE]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.DARK,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            budgetAlertEnabled = prefs[Keys.BUDGET_ALERT_ENABLED] ?: true,
            dailySummaryEnabled = prefs[Keys.DAILY_SUMMARY_ENABLED] ?: true,
            notificationPermissionPromptLastShownAt = prefs[Keys.NOTIFICATION_PERMISSION_PROMPT_LAST_SHOWN_AT] ?: 0L,
            appOpenCount = prefs[Keys.APP_OPEN_COUNT] ?: 0,
            rateAppPromptLastShownAt = prefs[Keys.RATE_APP_PROMPT_LAST_SHOWN_AT] ?: 0L,
            hasRatedOnPlayStore = prefs[Keys.HAS_RATED_ON_PLAY_STORE] ?: false,
            lastSyncTime = prefs[Keys.LAST_SYNC_TIME] ?: 0L,
            userId = userId,
            userEmail = prefs[Keys.USER_EMAIL],
            userName = prefs[Keys.USER_NAME],
            userPhotoUrl = prefs[Keys.USER_PHOTO_URL],
            hasSeenAiCoachCta = prefs[Keys.HAS_SEEN_AI_COACH_CTA] ?: false,
            isPremium = prefs[Keys.IS_PREMIUM] ?: false,
            trialStartedAt = prefs[Keys.TRIAL_STARTED_AT] ?: 0L,
            trialExpiresAt = prefs[Keys.TRIAL_EXPIRES_AT] ?: 0L,
            trialConsumed = consumedForCurrentUser,
            autoCaptureEnabled = prefs[Keys.AUTO_CAPTURE_ENABLED] ?: false,
            autoCaptureStatementImportEnabled = prefs[Keys.AUTO_CAPTURE_STATEMENT_IMPORT_ENABLED] ?: false,
            autoCaptureOcrEnabled = prefs[Keys.AUTO_CAPTURE_OCR_ENABLED] ?: false,
            autoCaptureNotificationEnabled = prefs[Keys.AUTO_CAPTURE_NOTIFICATION_ENABLED] ?: false,
            autoCaptureMerchantMemoryEnabled = prefs[Keys.AUTO_CAPTURE_MERCHANT_MEMORY_ENABLED] ?: false,
            autoCaptureRecurringSuggestionsEnabled =
                prefs[Keys.AUTO_CAPTURE_RECURRING_SUGGESTIONS_ENABLED] ?: false,
            autoCaptureDisclosureAccepted = prefs[Keys.AUTO_CAPTURE_DISCLOSURE_ACCEPTED] ?: false
        )
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        !prefs[Keys.USER_ID].isNullOrBlank()
    }

    val isPremium: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.IS_PREMIUM] ?: false
    }

    val aiChatHistoryJson: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.AI_CHAT_HISTORY_JSON]
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_ONBOARDING_COMPLETE] = complete
        }
    }

    suspend fun setAppLanguageTag(languageTag: String) {
        val resolved = languageTag.trim().ifBlank {
            Locale.getDefault().toLanguageTag().ifBlank { "en" }
        }
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_LANGUAGE_TAG] = resolved
        }
    }

    suspend fun setAccountCardStyle(style: AccountCardStyle) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCOUNT_CARD_STYLE] = style.name
        }
    }

    suspend fun setAccountCardStyleOverride(accountId: String, style: AccountCardStyle?) {
        val trimmedAccountId = accountId.trim()
        if (trimmedAccountId.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = decodeAccountCardStyleOverrides(
                prefs[Keys.ACCOUNT_CARD_STYLE_OVERRIDES].orEmpty()
            ).toMutableMap()
            if (style == null) {
                current.remove(trimmedAccountId)
            } else {
                current[trimmedAccountId] = style
            }
            if (current.isEmpty()) {
                prefs.remove(Keys.ACCOUNT_CARD_STYLE_OVERRIDES)
            } else {
                prefs[Keys.ACCOUNT_CARD_STYLE_OVERRIDES] = encodeAccountCardStyleOverrides(current)
            }
        }
    }

    suspend fun setBalancesHidden(hidden: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ARE_BALANCES_HIDDEN] = hidden
        }
    }

    suspend fun setDefaultCurrency(currency: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_CURRENCY] = currency
        }
    }

    suspend fun setDefaultAccountId(accountId: String?) {
        context.dataStore.edit { prefs ->
            if (accountId != null) {
                prefs[Keys.DEFAULT_ACCOUNT_ID] = accountId
            } else {
                prefs.remove(Keys.DEFAULT_ACCOUNT_ID)
            }
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setBudgetAlertEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BUDGET_ALERT_ENABLED] = enabled
        }
    }

    suspend fun setDailySummaryEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DAILY_SUMMARY_ENABLED] = enabled
        }
    }

    suspend fun setNotificationPermissionPromptLastShownAt(timestampMillis: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATION_PERMISSION_PROMPT_LAST_SHOWN_AT] = timestampMillis
        }
    }

    suspend fun incrementAppOpenCount() {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.APP_OPEN_COUNT] ?: 0
            prefs[Keys.APP_OPEN_COUNT] = current + 1
        }
    }

    suspend fun setRateAppPromptLastShownAt(timestampMillis: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.RATE_APP_PROMPT_LAST_SHOWN_AT] = timestampMillis
        }
    }

    suspend fun setHasRatedOnPlayStore(rated: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAS_RATED_ON_PLAY_STORE] = rated
        }
    }

    suspend fun getBudgetAlertActiveIds(): Set<String> {
        val raw = context.dataStore.data.first()[Keys.BUDGET_ALERT_ACTIVE_IDS].orEmpty()
        return raw.split(",")
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    suspend fun setBudgetAlertActiveIds(ids: Set<String>) {
        context.dataStore.edit { prefs ->
            if (ids.isEmpty()) {
                prefs.remove(Keys.BUDGET_ALERT_ACTIVE_IDS)
            } else {
                prefs[Keys.BUDGET_ALERT_ACTIVE_IDS] = ids.joinToString(",")
            }
        }
    }

    suspend fun updateLastSyncTime(time: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SYNC_TIME] = time
        }
    }

    suspend fun setUserInfo(userId: String?, email: String?, name: String?, photoUrl: String?) {
        context.dataStore.edit { prefs ->
            val previousUserId = prefs[Keys.USER_ID]
            if (userId != null) prefs[Keys.USER_ID] = userId else prefs.remove(Keys.USER_ID)
            if (email != null) prefs[Keys.USER_EMAIL] = email else prefs.remove(Keys.USER_EMAIL)
            if (name != null) prefs[Keys.USER_NAME] = name else prefs.remove(Keys.USER_NAME)
            if (photoUrl != null) prefs[Keys.USER_PHOTO_URL] = photoUrl else prefs.remove(Keys.USER_PHOTO_URL)
            if (userId == null || (!previousUserId.isNullOrBlank() && previousUserId != userId)) {
                prefs.remove(Keys.AI_CHAT_HISTORY_JSON)
                prefs.remove(Keys.ACCOUNT_CARD_STYLE_OVERRIDES)
                prefs.remove(Keys.APP_OPEN_COUNT)
                prefs.remove(Keys.RATE_APP_PROMPT_LAST_SHOWN_AT)
                prefs.remove(Keys.HAS_RATED_ON_PLAY_STORE)
            }
        }
    }

    suspend fun setUserName(name: String?) {
        context.dataStore.edit { prefs ->
            if (name.isNullOrBlank()) {
                prefs.remove(Keys.USER_NAME)
            } else {
                prefs[Keys.USER_NAME] = name
            }
        }
    }

    suspend fun setUserPhotoUrl(photoUrl: String?) {
        context.dataStore.edit { prefs ->
            if (photoUrl.isNullOrBlank()) {
                prefs.remove(Keys.USER_PHOTO_URL)
            } else {
                prefs[Keys.USER_PHOTO_URL] = photoUrl
            }
        }
    }

    suspend fun setAiCoachCtaSeen(seen: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAS_SEEN_AI_COACH_CTA] = seen
        }
    }

    suspend fun setPremium(isPremium: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_PREMIUM] = isPremium
        }
    }

    suspend fun setAutoCaptureEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_CAPTURE_ENABLED] = enabled
        }
    }

    suspend fun setAutoCaptureStatementImportEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_CAPTURE_STATEMENT_IMPORT_ENABLED] = enabled
        }
    }

    suspend fun setAutoCaptureOcrEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_CAPTURE_OCR_ENABLED] = enabled
        }
    }

    suspend fun setAutoCaptureNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_CAPTURE_NOTIFICATION_ENABLED] = enabled
        }
    }

    suspend fun setAutoCaptureMerchantMemoryEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_CAPTURE_MERCHANT_MEMORY_ENABLED] = enabled
        }
    }

    suspend fun setAutoCaptureRecurringSuggestionsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_CAPTURE_RECURRING_SUGGESTIONS_ENABLED] = enabled
        }
    }

    suspend fun setAutoCaptureDisclosureAccepted(accepted: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_CAPTURE_DISCLOSURE_ACCEPTED] = accepted
        }
    }

    suspend fun setAiChatHistoryJson(historyJson: String?) {
        context.dataStore.edit { prefs ->
            if (historyJson.isNullOrBlank()) {
                prefs.remove(Keys.AI_CHAT_HISTORY_JSON)
            } else {
                prefs[Keys.AI_CHAT_HISTORY_JSON] = historyJson
            }
        }
    }

    suspend fun setTrialState(startedAt: Long, expiresAt: Long, consumed: Boolean) {
        context.dataStore.edit { prefs ->
            val userId = prefs[Keys.USER_ID]
            val consumedUserIds = decodeConsumedUserIds(prefs[Keys.TRIAL_CONSUMED_USER_IDS].orEmpty()).toMutableSet()
            if (consumed && !userId.isNullOrBlank()) {
                consumedUserIds.add(userId)
            }
            prefs[Keys.TRIAL_STARTED_AT] = startedAt
            prefs[Keys.TRIAL_EXPIRES_AT] = expiresAt
            prefs[Keys.TRIAL_CONSUMED] = consumed
            prefs[Keys.TRIAL_CONSUMED_USER_IDS] = encodeConsumedUserIds(consumedUserIds.toList().sorted())
        }
    }

    suspend fun clearTrialState() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.TRIAL_STARTED_AT)
            prefs.remove(Keys.TRIAL_EXPIRES_AT)
            prefs.remove(Keys.TRIAL_CONSUMED)
        }
    }

    suspend fun clearUserData() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.USER_EMAIL)
            prefs.remove(Keys.USER_NAME)
            prefs.remove(Keys.USER_PHOTO_URL)
            prefs.remove(Keys.AI_CHAT_HISTORY_JSON)
            prefs.remove(Keys.ACCOUNT_CARD_STYLE_OVERRIDES)
            prefs.remove(Keys.HAS_SEEN_AI_COACH_CTA)
            prefs.remove(Keys.IS_PREMIUM)
            prefs.remove(Keys.LAST_SYNC_TIME)
            prefs.remove(Keys.NOTIFICATION_PERMISSION_PROMPT_LAST_SHOWN_AT)
            prefs.remove(Keys.APP_OPEN_COUNT)
            prefs.remove(Keys.RATE_APP_PROMPT_LAST_SHOWN_AT)
            prefs.remove(Keys.HAS_RATED_ON_PLAY_STORE)
            prefs.remove(Keys.AUTO_CAPTURE_ENABLED)
            prefs.remove(Keys.AUTO_CAPTURE_STATEMENT_IMPORT_ENABLED)
            prefs.remove(Keys.AUTO_CAPTURE_OCR_ENABLED)
            prefs.remove(Keys.AUTO_CAPTURE_NOTIFICATION_ENABLED)
            prefs.remove(Keys.AUTO_CAPTURE_MERCHANT_MEMORY_ENABLED)
            prefs.remove(Keys.AUTO_CAPTURE_RECURRING_SUGGESTIONS_ENABLED)
            prefs.remove(Keys.AUTO_CAPTURE_DISCLOSURE_ACCEPTED)
        }
    }

    private fun decodeAccountCardStyleOverrides(raw: String): Map<String, AccountCardStyle> {
        if (raw.isBlank()) return emptyMap()
        return runCatching {
            json.decodeFromString(
                MapSerializer(String.serializer(), String.serializer()),
                raw
            )
        }.getOrElse { emptyMap() }
            .mapNotNull { (accountId, styleName) ->
                val id = accountId.trim()
                val style = runCatching { AccountCardStyle.valueOf(styleName) }.getOrNull()
                if (id.isBlank() || style == null) null else id to style
            }
            .toMap()
    }

    private fun encodeAccountCardStyleOverrides(map: Map<String, AccountCardStyle>): String {
        val serializable = map.entries
            .mapNotNull { (accountId, style) ->
                val id = accountId.trim()
                if (id.isBlank()) null else id to style.name
            }
            .toMap()
        return runCatching {
            json.encodeToString(
                MapSerializer(String.serializer(), String.serializer()),
                serializable
            )
        }.getOrElse { "{}" }
    }

    private fun decodeConsumedUserIds(raw: String): Set<String> {
        if (raw.isBlank()) return emptySet()
        return runCatching {
            json.decodeFromString(ListSerializer(String.serializer()), raw)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toSet()
        }.getOrElse { emptySet() }
    }

    private fun encodeConsumedUserIds(ids: List<String>): String {
        return runCatching {
            json.encodeToString(ListSerializer(String.serializer()), ids)
        }.getOrElse { "[]" }
    }
}

