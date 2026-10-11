package app.utterdroid.api

import app.utterdroid.logic.ApiUrls
import app.utterdroid.logic.DictionaryTerms
import app.utterdroid.model.AppSettings
import app.utterdroid.model.Term
import java.io.File
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody

class OpenAiTranscriptionClient(private val http: OkHttpClient) : TranscriptionClient {
    override suspend fun transcribe(file: File, settings: AppSettings, keyterms: List<String>): String {
        val base = ApiUrls.normalize(settings.sttBaseUrl)
        ApiUrls.requireHttps(base)
        if (settings.sttApiKey.isBlank()) error("Add your API key in Settings.")
        val hint = DictionaryTerms.whisperPrompt(keyterms.map { Term(it) })
        val body = MultipartBody.Builder().setType(MultipartBody.FORM).apply {
            addFormDataPart("model", settings.sttModel.ifBlank { "whisper-1" })
            val language = settings.sttLanguage.trim()
            if (language.isNotEmpty()) addFormDataPart("language", language)
            if (hint.isNotEmpty()) addFormDataPart("prompt", hint)
            addFormDataPart(
                "file",
                file.name,
                file.asRequestBody("audio/mp4".toMediaType()),
            )
        }.build()
        val request = Request.Builder()
            .url("$base/audio/transcriptions")
            .header("Authorization", "Bearer ${settings.sttApiKey}")
            .post(body)
            .build()
        return transcriptText(http.readBody(request))
    }
}
