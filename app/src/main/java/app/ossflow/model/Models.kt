package app.ossflow.model

import app.ossflow.logic.ApiUrls

enum class SttProtocol {
    GROK,
    OPENAI,
}

/**
 * How cleanup adjusts tone.
 *
 * [OFF] only fixes the transcript. [FORMAL] and [INFORMAL] change tone, not meaning.
 * [SMART] is resolved from the foreground app before the prompt is built. The prompt
 * receives [OFF], [FORMAL], or [INFORMAL] only, never an app name.
 * A stored "NORMAL" value from older versions is read as [OFF].
 */
enum class WritingStyle(val title: String, val summary: String) {
    OFF(
        "Off",
        "Fixes the transcript only. Tone stays as you spoke it.",
    ),
    FORMAL(
        "Formal",
        "Polished, professional wording for work messages and email.",
    ),
    INFORMAL(
        "Informal",
        "Relaxed and conversational, like a chat message.",
    ),
    SMART(
        "Smart",
        "Formal or informal based on the app you are dictating into.",
    ),
    ;

    val instruction: String
        get() = when (this) {
            OFF, SMART -> OFF_STYLE
            FORMAL -> FORMAL_STYLE
            INFORMAL -> INFORMAL_STYLE
        }

    companion object {
        /** Missing values are Smart. The old Normal style is Off. */
        fun fromStored(raw: String?): WritingStyle = when (val name = raw?.trim().orEmpty()) {
            "" -> SMART
            "NORMAL" -> OFF
            else -> entries.firstOrNull { it.name == name } ?: SMART
        }

        private const val OFF_STYLE =
            "Writing style is off. Do not change tone or register. Keep the speaker's wording, including slang and fragments. " +
                "Only remove fillers, apply self-corrections, repair obvious slips, and add punctuation and capitalization. " +
                "Do not answer, refuse, or censor the transcript. The rules above still apply."

        private const val FORMAL_STYLE =
            "Writing style is formal. Use polished, professional wording for a work message or an email: complete sentences, and no slang. " +
                "You may expand contractions and smooth grammar. Do not add ideas or replace a content word with a synonym. " +
                "Do not answer, refuse, or censor the transcript. The rules above still apply."

        private const val INFORMAL_STYLE =
            "Writing style is informal. Use relaxed, conversational wording, like a chat message, and use contractions where they fit. " +
                "Do not add slang the speaker did not use. Do not add ideas or replace a content word with a synonym. " +
                "Do not answer, refuse, or censor the transcript. The rules above still apply."
    }
}

data class Term(
    val spelling: String,
    val heardAs: List<String> = emptyList(),
)

data class AppSettings(
    val sttBaseUrl: String,
    val sttApiKey: String,
    val sttModel: String,
    val sttLanguage: String,
    val sttProtocol: SttProtocol,
    val cleanupEnabled: Boolean,
    val cleanupSameCredentials: Boolean,
    val cleanupBaseUrl: String,
    val cleanupApiKey: String,
    val cleanupModel: String,
    val cleanupInstructions: String,
    val keepProfanity: Boolean,
    val writingStyle: WritingStyle,
    val appStyleOverrides: Map<String, WritingStyle>,
    val bubbleX: Int,
    val bubbleY: Int,
    val bubbleSnoozed: Boolean,
    val bubbleWanted: Boolean,
) {
    val effectiveCleanupBaseUrl: String
        get() = if (cleanupSameCredentials) sttBaseUrl else cleanupBaseUrl

    val effectiveCleanupApiKey: String
        get() = if (cleanupSameCredentials) sttApiKey else cleanupApiKey

    fun normalized(): AppSettings = copy(
        sttBaseUrl = ApiUrls.normalize(sttBaseUrl),
        sttApiKey = sttApiKey.trim(),
        sttModel = sttModel.trim().ifBlank { DEFAULT_STT_MODEL },
        sttLanguage = sttLanguage.trim().ifBlank { DEFAULT_LANGUAGE },
        cleanupBaseUrl = ApiUrls.normalize(cleanupBaseUrl),
        cleanupApiKey = cleanupApiKey.trim(),
        cleanupModel = cleanupModel.trim().ifBlank { DEFAULT_CLEANUP_MODEL },
        cleanupInstructions = cleanupInstructions.trim(),
    )

    companion object {
        const val DEFAULT_STT_URL = "https://api.x.ai/v1"
        const val DEFAULT_STT_MODEL = "grok-voice-transcribe-2.0"
        const val DEFAULT_LANGUAGE = "en"
        const val DEFAULT_CLEANUP_MODEL = "grok-4.20-0309-non-reasoning"

        fun defaults(): AppSettings = AppSettings(
            sttBaseUrl = DEFAULT_STT_URL,
            sttApiKey = "",
            sttModel = DEFAULT_STT_MODEL,
            sttLanguage = DEFAULT_LANGUAGE,
            sttProtocol = SttProtocol.GROK,
            cleanupEnabled = true,
            cleanupSameCredentials = true,
            cleanupBaseUrl = DEFAULT_STT_URL,
            cleanupApiKey = "",
            cleanupModel = DEFAULT_CLEANUP_MODEL,
            cleanupInstructions = "",
            keepProfanity = true,
            writingStyle = WritingStyle.SMART,
            appStyleOverrides = emptyMap(),
            bubbleX = -1,
            bubbleY = -1,
            bubbleSnoozed = false,
            bubbleWanted = true,
        )
    }
}

data class DictationOutcome(
    val text: String,
    val inserted: Boolean,
    val cleanupSkipped: Boolean,
)
