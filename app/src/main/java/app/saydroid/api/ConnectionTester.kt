package app.saydroid.api

import app.saydroid.logic.ApiUrls
import okhttp3.OkHttpClient
import okhttp3.Request

class ConnectionTester(private val http: OkHttpClient) {
    suspend fun test(baseUrl: String, apiKey: String): String {
        val base = ApiUrls.normalize(baseUrl)
        if (apiKey.isBlank()) return "Add an API key first."
        return try {
            ApiUrls.requireHttps(base)
            val request = Request.Builder()
                .url("$base/models")
                .header("Authorization", "Bearer $apiKey")
                .get()
                .build()
            http.readBody(request)
            "Connected"
        } catch (e: ApiException) {
            "HTTP ${e.status}: ${e.message}"
        } catch (e: Exception) {
            e.message ?: "Could not connect."
        }
    }
}
