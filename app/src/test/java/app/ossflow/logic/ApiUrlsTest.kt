package app.ossflow.logic

import app.ossflow.api.errorDetail
import app.ossflow.api.transcriptText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiUrlsTest {
    @Test
    fun openRouterWebsitePathsUseTheApiBase() {
        assertEquals(ApiUrls.OPENROUTER_BASE, ApiUrls.normalize("https://openrouter.ai"))
        assertEquals(ApiUrls.OPENROUTER_BASE, ApiUrls.normalize("https://openrouter.ai/"))
        assertEquals(ApiUrls.OPENROUTER_BASE, ApiUrls.normalize("https://openrouter.ai/v1"))
        assertEquals(ApiUrls.OPENROUTER_BASE, ApiUrls.normalize("https://openrouter.ai/v1/"))
        assertEquals(
            ApiUrls.OPENROUTER_BASE,
            ApiUrls.normalize("https://openrouter.ai/v1/audio/transcriptions"),
        )
        assertEquals(
            ApiUrls.OPENROUTER_BASE,
            ApiUrls.normalize("https://www.openrouter.ai/api/v1/chat/completions"),
        )
        assertEquals(ApiUrls.OPENROUTER_BASE, ApiUrls.normalize("  https://openrouter.ai/api/v1/ "))
    }

    @Test
    fun otherHostsStayPut() {
        assertEquals("https://api.x.ai/v1", ApiUrls.normalize("https://api.x.ai/v1/"))
        assertEquals("https://api.openai.com/v1", ApiUrls.normalize("https://api.openai.com/v1"))
        assertFalse(ApiUrls.isOpenRouter("https://api.x.ai/v1"))
        assertTrue(ApiUrls.isOpenRouter("https://openrouter.ai/v1"))
    }

    @Test
    fun htmlErrorIsNotShownRaw() {
        val page = "<!DOCTYPE html><html><body>Not found</body></html>"
        assertEquals(
            "The API URL returned a web page (HTTP 404). Check the base URL in Settings.",
            errorDetail(404, page, "Not Found"),
        )
        assertEquals(
            """{"error":{"message":"Not Found","code":404}}""",
            errorDetail(404, """{"error":{"message":"Not Found","code":404}}""", "Not Found"),
        )
    }

    @Test
    fun htmlTranscriptionBodyExplainsTheUrl() {
        val error = runCatching {
            transcriptText("<!DOCTYPE html><html></html>")
        }.exceptionOrNull()
        assertEquals(
            "The transcription URL returned a web page. Check the base URL in Settings.",
            error?.message,
        )
    }
}
