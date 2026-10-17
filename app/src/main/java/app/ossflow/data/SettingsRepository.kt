package app.ossflow.data

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import app.ossflow.model.AppSettings
import app.ossflow.model.SttProtocol
import app.ossflow.model.WritingStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private val Context.settingsStore by preferencesDataStore("ossflow_settings")

data class TimeSavedTotals(val words: Long, val speakingMs: Long)

class SettingsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val store = appContext.settingsStore
    private val secrets = openSecrets(appContext)
    private val revision = MutableStateFlow(0)
    private val mutex = Mutex()

    val flow: Flow<AppSettings> = combine(store.data, revision) { prefs, _ ->
        read(prefs)
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        mutex.withLock {
            val next = transform(flow.first()).normalized()
            store.edit { prefs -> prefs.write(next) }
            withContext(Dispatchers.IO) {
                secrets.edit()
                    .putString(KEY_STT, next.sttApiKey)
                    .putString(KEY_CLEANUP, next.cleanupApiKey)
                    .commit()
            }
            revision.value = revision.value + 1
        }
    }

    suspend fun setBubblePosition(x: Int, y: Int) {
        store.edit { prefs ->
            prefs[BUBBLE_X] = x
            prefs[BUBBLE_Y] = y
        }
    }

    suspend fun setBubbleSnoozed(snoozed: Boolean) {
        store.edit { prefs -> prefs[BUBBLE_SNOOZED] = snoozed }
    }

    suspend fun setBubbleWanted(wanted: Boolean) {
        store.edit { prefs -> prefs[BUBBLE_WANTED] = wanted }
    }

    val timeSaved: Flow<TimeSavedTotals> = store.data.map { prefs ->
        TimeSavedTotals(
            words = prefs[LIFETIME_WORDS] ?: 0L,
            speakingMs = prefs[LIFETIME_SPEAKING_MS] ?: 0L,
        )
    }

    suspend fun addDictationStats(words: Int, speakingMs: Long) {
        if (words <= 0) return
        store.edit { prefs ->
            prefs[LIFETIME_WORDS] = (prefs[LIFETIME_WORDS] ?: 0L) + words
            prefs[LIFETIME_SPEAKING_MS] = (prefs[LIFETIME_SPEAKING_MS] ?: 0L) + speakingMs.coerceAtLeast(0L)
        }
    }

    suspend fun resetTimeSaved() {
        store.edit { prefs ->
            prefs[LIFETIME_WORDS] = 0L
            prefs[LIFETIME_SPEAKING_MS] = 0L
        }
    }

    private fun read(prefs: androidx.datastore.preferences.core.Preferences): AppSettings {
        val defaults = AppSettings.defaults()
        return AppSettings(
            sttBaseUrl = prefs[STT_URL] ?: defaults.sttBaseUrl,
            sttApiKey = secrets.getString(KEY_STT, "") ?: "",
            sttModel = prefs[STT_MODEL] ?: defaults.sttModel,
            sttLanguage = prefs[STT_LANGUAGE] ?: defaults.sttLanguage,
            sttProtocol = protocolOf(prefs[STT_PROTOCOL]),
            cleanupEnabled = prefs[CLEANUP_ENABLED] ?: defaults.cleanupEnabled,
            cleanupSameCredentials = prefs[CLEANUP_SAME] ?: defaults.cleanupSameCredentials,
            cleanupBaseUrl = prefs[CLEANUP_URL] ?: defaults.cleanupBaseUrl,
            cleanupApiKey = secrets.getString(KEY_CLEANUP, "") ?: "",
            cleanupModel = prefs[CLEANUP_MODEL] ?: defaults.cleanupModel,
            cleanupInstructions = prefs[CLEANUP_INSTRUCTIONS] ?: defaults.cleanupInstructions,
            writingStyle = styleOf(prefs[STYLE]),
            bubbleX = prefs[BUBBLE_X] ?: -1,
            bubbleY = prefs[BUBBLE_Y] ?: -1,
            bubbleSnoozed = prefs[BUBBLE_SNOOZED] ?: false,
            bubbleWanted = prefs[BUBBLE_WANTED] ?: true,
        )
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.write(settings: AppSettings) {
        this[STT_URL] = settings.sttBaseUrl
        this[STT_MODEL] = settings.sttModel
        this[STT_LANGUAGE] = settings.sttLanguage
        this[STT_PROTOCOL] = settings.sttProtocol.name
        this[CLEANUP_ENABLED] = settings.cleanupEnabled
        this[CLEANUP_SAME] = settings.cleanupSameCredentials
        this[CLEANUP_URL] = settings.cleanupBaseUrl
        this[CLEANUP_MODEL] = settings.cleanupModel
        this[CLEANUP_INSTRUCTIONS] = settings.cleanupInstructions
        this[STYLE] = settings.writingStyle.name
        this[BUBBLE_SNOOZED] = settings.bubbleSnoozed
        this[BUBBLE_WANTED] = settings.bubbleWanted
    }

    private fun protocolOf(raw: String?): SttProtocol =
        SttProtocol.entries.firstOrNull { it.name == raw } ?: SttProtocol.GROK

    private fun styleOf(raw: String?): WritingStyle =
        WritingStyle.entries.firstOrNull { it.name == raw } ?: WritingStyle.NORMAL

    companion object {
        private val STT_URL = stringPreferencesKey("stt_url")
        private val STT_MODEL = stringPreferencesKey("stt_model")
        private val STT_LANGUAGE = stringPreferencesKey("stt_language")
        private val STT_PROTOCOL = stringPreferencesKey("stt_protocol")
        private val CLEANUP_ENABLED = booleanPreferencesKey("cleanup_enabled")
        private val CLEANUP_SAME = booleanPreferencesKey("cleanup_same")
        private val CLEANUP_URL = stringPreferencesKey("cleanup_url")
        private val CLEANUP_MODEL = stringPreferencesKey("cleanup_model")
        private val CLEANUP_INSTRUCTIONS = stringPreferencesKey("cleanup_instructions")
        private val STYLE = stringPreferencesKey("writing_style")
        private val BUBBLE_X = intPreferencesKey("bubble_x")
        private val BUBBLE_Y = intPreferencesKey("bubble_y")
        private val BUBBLE_SNOOZED = booleanPreferencesKey("bubble_snoozed")
        private val BUBBLE_WANTED = booleanPreferencesKey("bubble_wanted")
        private val LIFETIME_WORDS = longPreferencesKey("lifetime_words")
        private val LIFETIME_SPEAKING_MS = longPreferencesKey("lifetime_speaking_ms")
        private const val KEY_STT = "stt_api_key"
        private const val KEY_CLEANUP = "cleanup_api_key"

        private fun openSecrets(context: Context): SharedPreferences {
            return try {
                val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                EncryptedSharedPreferences.create(
                    "ossflow_secrets",
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            } catch (_: Exception) {
                context.getSharedPreferences("ossflow_secrets_plain", Context.MODE_PRIVATE)
            }
        }
    }
}
