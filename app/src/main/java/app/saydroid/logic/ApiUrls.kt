package app.saydroid.logic

import java.net.URI

object ApiUrls {
    const val OPENROUTER_BASE = "https://openrouter.ai/api/v1"

    fun normalize(url: String): String {
        val trimmed = url.trim().trimEnd('/')
        return openRouterBase(trimmed) ?: trimmed
    }

    fun isOpenRouter(url: String): Boolean = openRouterBase(url.trim().trimEnd('/')) != null

    fun requireHttps(url: String) {
        if (!url.startsWith("https://")) {
            throw IllegalArgumentException("Use an https:// API URL.")
        }
    }

    /**
     * OpenRouter's website answers paths outside `/api/v1` with an HTML page.
     * `https://openrouter.ai/v1/...` is HTTP 404 and starts with `<!DOCTYPE`.
     */
    private fun openRouterBase(url: String): String? {
        val uri = try {
            URI(url)
        } catch (_: Exception) {
            return null
        }
        val host = uri.host?.lowercase() ?: return null
        if (host != "openrouter.ai" && host != "www.openrouter.ai") return null
        return OPENROUTER_BASE
    }
}
