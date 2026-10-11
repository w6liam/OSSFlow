package app.utterdroid.api

import app.utterdroid.logic.ApiUrls
import app.utterdroid.model.AppSettings
import app.utterdroid.model.SttProtocol
import java.io.File
import okhttp3.OkHttpClient

interface TranscriptionClient {
    suspend fun transcribe(file: File, settings: AppSettings, keyterms: List<String>): String
}

class TranscriptionRouter(http: OkHttpClient) : TranscriptionClient {
    private val grok = GrokSttClient(http)
    private val openAi = OpenAiTranscriptionClient(http)

    override suspend fun transcribe(file: File, settings: AppSettings, keyterms: List<String>): String {
        // OpenRouter has no /stt route. A Grok-style URL there is the marketing site.
        val client = if (settings.sttProtocol == SttProtocol.OPENAI || ApiUrls.isOpenRouter(settings.sttBaseUrl)) {
            openAi
        } else {
            grok
        }
        return client.transcribe(file, settings, keyterms)
    }
}
