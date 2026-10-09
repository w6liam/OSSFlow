package app.notype.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class ApiException(val status: Int, message: String) : Exception(message)

suspend fun OkHttpClient.readBody(request: Request): String = withContext(Dispatchers.IO) {
    newCall(request).execute().use { response ->
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            throw ApiException(
                response.code,
                errorDetail(response.code, body, response.message),
            )
        }
        body
    }
}

internal fun looksLikeHtml(body: String): Boolean {
    val start = body.trimStart()
    return start.startsWith("<!DOCTYPE", ignoreCase = true) ||
        start.startsWith("<html", ignoreCase = true)
}

internal fun errorDetail(status: Int, body: String, fallback: String): String {
    if (looksLikeHtml(body)) {
        return "The API URL returned a web page (HTTP $status). Check the base URL in Settings."
    }
    val detail = body.replace(Regex("\\s+"), " ").trim().take(240)
    return detail.ifBlank { fallback.ifBlank { "HTTP $status" } }
}
