package app.notype.api

import app.notype.model.AppSettings
import app.notype.model.SttProtocol
import java.io.File
import okhttp3.OkHttpClient

interface TranscriptionClient {
    suspend fun transcribe(file: File, settings: AppSettings, keyterms: List<String>): String
}

class TranscriptionRouter(http: OkHttpClient) : TranscriptionClient {
    private val grok = GrokSttClient(http)
    private val openAi = OpenAiTranscriptionClient(http)

    override suspend fun transcribe(file: File, settings: AppSettings, keyterms: List<String>): String =
        when (settings.sttProtocol) {
            SttProtocol.GROK -> grok.transcribe(file, settings, keyterms)
            SttProtocol.OPENAI -> openAi.transcribe(file, settings, keyterms)
        }
}
