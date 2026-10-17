package app.ossflow.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.SystemClock
import java.io.File

data class Recording(val file: File, val durationMs: Long)

class Recorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAt: Long = 0L

    val isRecording: Boolean
        get() = recorder != null

    fun start() {
        cancel()
        val out = File(context.cacheDir, "dictate-${System.currentTimeMillis()}.m4a")
        val created = MediaRecorder(context)
        try {
            created.setAudioSource(MediaRecorder.AudioSource.MIC)
            created.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            created.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            created.setAudioChannels(1)
            created.setAudioSamplingRate(44100)
            created.setAudioEncodingBitRate(96_000)
            created.setOutputFile(out.absolutePath)
            created.prepare()
            created.start()
        } catch (e: Exception) {
            created.release()
            out.delete()
            throw e
        }
        recorder = created
        file = out
        startedAt = SystemClock.elapsedRealtime()
    }

    fun stop(): Recording {
        val active = recorder ?: error("Not recording.")
        val out = file ?: error("Not recording.")
        val durationMs = (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0L)
        recorder = null
        file = null
        startedAt = 0L
        try {
            active.stop()
        } catch (_: RuntimeException) {
            active.release()
            out.delete()
            throw IllegalStateException("Recording was too short.")
        }
        active.release()
        if (!out.exists() || out.length() == 0L) {
            out.delete()
            throw IllegalStateException("Recording was empty.")
        }
        return Recording(out, durationMs)
    }

    fun cancel() {
        val active = recorder
        val out = file
        recorder = null
        file = null
        startedAt = 0L
        if (active != null) {
            try {
                active.stop()
            } catch (_: RuntimeException) {
                // stop() throws when the clip has no samples yet.
            }
            active.release()
        }
        out?.delete()
    }

    fun deleteLeftovers() {
        context.cacheDir.listFiles { candidate ->
            candidate.name.startsWith("dictate-") && candidate.name.endsWith(".m4a")
        }?.forEach { it.delete() }
    }
}
