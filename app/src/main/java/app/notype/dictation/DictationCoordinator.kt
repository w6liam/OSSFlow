package app.notype.dictation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import app.notype.a11y.NoTypeAccessibilityService
import app.notype.api.CleanupClient
import app.notype.api.TranscriptionClient
import app.notype.data.AppDatabase
import app.notype.data.SettingsRepository
import app.notype.data.TranscriptEntity
import app.notype.logic.CleanupText
import app.notype.logic.DictionaryTerms
import app.notype.logic.TimeSaved
import app.notype.model.DictationOutcome
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
                text = cleaned.ifBlank { raw }
            } catch (_: Exception) {
                text = raw
                cleanupSkipped = true
            }
        }

        withContext(Dispatchers.Main) {
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            clipboard.setPrimaryClip(ClipData.newPlainText("noType", text))
        }
        val inserted = withContext(Dispatchers.Main) {
            NoTypeAccessibilityService.insert(text)
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
