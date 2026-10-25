package app.ossflow.dictation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import app.ossflow.a11y.OSSFlowAccessibilityService
import app.ossflow.api.CleanupClient
import app.ossflow.api.TranscriptionClient
import app.ossflow.data.AppDatabase
import app.ossflow.data.SettingsRepository
import app.ossflow.data.TranscriptEntity
import app.ossflow.logic.CleanupText
import app.ossflow.logic.DictionaryGuard
import app.ossflow.logic.DictionaryTerms
import app.ossflow.logic.TimeSaved
import app.ossflow.model.DictationOutcome
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class DictationCoordinator(
    private val context: Context,
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository,
    private val transcription: TranscriptionClient,
    private val cleanup: CleanupClient,
) {
    suspend fun process(file: File, durationMs: Long): DictationOutcome {
        if (!file.exists() || file.length() == 0L) error("Recording was empty.")
        val settings = settingsRepository.flow.first()
        val terms = database.dictionary().all().map { it.toTerm() }
        val raw = transcription.transcribe(file, settings, DictionaryTerms.keyterms(terms)).trim()
        if (raw.isEmpty()) error("Didn't catch that.")

        var text = raw
        var cleanupSkipped = false
        if (settings.cleanupEnabled) {
            try {
                val cleaned = CleanupText.withoutLabel(cleanup.clean(raw, terms, settings), raw)
                val guarded = if (cleaned.isBlank()) raw else DictionaryGuard.apply(raw, cleaned, terms)
                text = guarded.ifBlank { raw }
            } catch (_: Exception) {
                text = raw
                cleanupSkipped = true
            }
        }

        withContext(Dispatchers.Main) {
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            clipboard.setPrimaryClip(ClipData.newPlainText("OSSFlow", text))
        }
        val inserted = withContext(Dispatchers.Main) {
            OSSFlowAccessibilityService.insert(text)
        }
        val words = TimeSaved.wordCount(text)
        settingsRepository.addDictationStats(words, TimeSaved.speakingMs(words, durationMs))
        database.transcripts().insert(
            TranscriptEntity(
                text = text,
                rawText = raw,
                createdAt = System.currentTimeMillis(),
                cleanupSkipped = cleanupSkipped,
                inserted = inserted,
            ),
        )
        return DictationOutcome(text = text, inserted = inserted, cleanupSkipped = cleanupSkipped)
    }
}
