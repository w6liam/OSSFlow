package app.utterdroid.api

import app.utterdroid.logic.ApiUrls
import app.utterdroid.model.AppSettings
import java.io.File
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject

class GrokSttClient(private val http: OkHttpClient) : TranscriptionClient {
    override suspend fun transcribe(file: File, settings: AppSettings, keyterms: List<String>): String {
        val base = ApiUrls.normalize(settings.sttBaseUrl)
        ApiUrls.requireHttps(base)
        if (settings.sttApiKey.isBlank()) error("Add your API key in Settings.")
        val body = MultipartBody.Builder().setType(MultipartBody.FORM).apply {
            addFormDataPart("model", settings.sttModel.ifBlank { AppSettings.DEFAULT_STT_MODEL })
            addFormDataPart("format", "true")
            addFormDataPart("language", settings.sttLanguage.ifBlank { AppSettings.DEFAULT_LANGUAGE })
            addFormDataPart("filler_words", "false")
            keyterms.forEach { addFormDataPart("keyterm", it) }
            addFormDataPart(
                "file",
                file.name,
                file.asRequestBody("audio/mp4".toMediaType()),
            )
        }.build()
        val request = Request.Builder()
            .url("$base/stt")
            .header("Authorization", "Bearer ${settings.sttApiKey}")
            .post(body)
            .build()
        return transcriptText(http.readBody(request))
    }
}

internal fun transcriptText(body: String): String {
    if (looksLikeHtml(body)) {
        throw IllegalStateException(
            "The transcription URL returned a web page. Check the base URL in Settings.",
        )
    }
    val text = try {
        JSONObject(body).optString("text")
    } catch (_: Exception) {
        throw IllegalStateException("Unexpected response from the transcription API.")
    }.trim()
    if (text.isEmpty()) error("Didn't catch that.")
    return text
}
