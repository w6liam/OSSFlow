package app.utterdroid.api

import app.utterdroid.logic.ApiUrls
import app.utterdroid.logic.CleanupPrompt
import app.utterdroid.logic.HeardAsPrompt
import app.utterdroid.model.AppSettings
import app.utterdroid.model.Term
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class CleanupClient(private val http: OkHttpClient) {
    suspend fun clean(raw: String, terms: List<Term>, settings: AppSettings): String {
        val base = ApiUrls.normalize(settings.effectiveCleanupBaseUrl)
        ApiUrls.requireHttps(base)
        val key = settings.effectiveCleanupApiKey
        if (key.isBlank()) error("Add your API key in Settings.")
        val payload = JSONObject()
            .put("model", settings.cleanupModel.ifBlank { AppSettings.DEFAULT_CLEANUP_MODEL })
            .put("temperature", 0)
            .put(
                "messages",
                JSONArray()
                    .put(message("system", CleanupPrompt.system(settings.writingStyle)))
                    .put(message("user", CleanupPrompt.user(raw, terms))),
            )
        val request = Request.Builder()
            .url("$base/chat/completions")
            .header("Authorization", "Bearer $key")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        return chatText(http.readBody(request))
    }

    suspend fun suggestMishearings(spelling: String, settings: AppSettings): String {
        val base = ApiUrls.normalize(settings.effectiveCleanupBaseUrl)
        ApiUrls.requireHttps(base)
        val key = settings.effectiveCleanupApiKey
        if (key.isBlank()) error("Add your API key in Settings.")
        val payload = JSONObject()
            .put("model", settings.cleanupModel.ifBlank { AppSettings.DEFAULT_CLEANUP_MODEL })
            .put("temperature", 0)
            .put(
                "messages",
                JSONArray()
                    .put(message("system", HeardAsPrompt.system()))
                    .put(message("user", HeardAsPrompt.user(spelling))),
            )
        val request = Request.Builder()
            .url("$base/chat/completions")
            .header("Authorization", "Bearer $key")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val content = try {
            JSONObject(http.readBody(request))
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } catch (_: Exception) {
            throw IllegalStateException("Unexpected response from the cleanup API.")
        }
        return unwrapModelText(content)
    }

    private fun message(role: String, content: String): JSONObject =
        JSONObject().put("role", role).put("content", content)
}

internal fun chatText(body: String): String {
    val content = try {
        JSONObject(body)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    } catch (_: Exception) {
        throw IllegalStateException("Unexpected response from the cleanup API.")
    }
    val text = unwrapModelText(content)
    if (text.isEmpty()) error("Cleanup returned empty text.")
    return text
}

internal fun unwrapModelText(content: String): String {
    var text = content.trim()
    if (text.startsWith("```")) {
        text = text.removePrefix("```")
        text = text.removePrefix("text").removePrefix("markdown")
        text = text.removePrefix("\n")
        if (text.endsWith("```")) text = text.removeSuffix("```")
        text = text.trim()
    }
    if (text.length >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
        text = text.substring(1, text.length - 1).trim()
    }
    return text
}
