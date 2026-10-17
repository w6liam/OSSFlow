package app.ossflow.model

import app.ossflow.logic.ApiUrls

enum class SttProtocol {
    GROK,
    OPENAI,
}

enum class WritingStyle(val instruction: String) {
    NORMAL(
        "Write in standard English: clear sentences and ordinary wording. Not formal, and not casual slang.",
    ),
    FORMAL("Write in polite, professional English."),
    INFORMAL("Write in relaxed conversational English and use contractions."),
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
    val writingStyle: WritingStyle,
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
            writingStyle = WritingStyle.NORMAL,
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
