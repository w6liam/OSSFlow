package app.notype.logic

object ApiUrls {
    fun normalize(url: String): String = url.trim().trimEnd('/')

    fun requireHttps(url: String) {
        if (!url.startsWith("https://")) {
            throw IllegalArgumentException("Use an https:// API URL.")
        }
    }
}
