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
            val detail = body.replace(Regex("\\s+"), " ").trim().take(240)
            throw ApiException(
                response.code,
                detail.ifBlank { response.message.ifBlank { "HTTP ${response.code}" } },
            )
        }
        body
    }
}
