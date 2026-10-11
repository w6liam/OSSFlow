package app.utterdroid.a11y

object AccessibilityEnabled {
    fun listed(rawSetting: String?, packageName: String, className: String): Boolean {
        if (rawSetting.isNullOrBlank() || packageName.isEmpty() || className.isEmpty()) return false
        return rawSetting.split(':').any { entry ->
            val component = unflatten(entry.trim()) ?: return@any false
            component.first == packageName && component.second == className
        }
    }

    private fun unflatten(entry: String): Pair<String, String>? {
        val sep = entry.indexOf('/')
        if (sep <= 0 || sep + 1 >= entry.length) return null
        val pkg = entry.substring(0, sep)
        var cls = entry.substring(sep + 1)
        if (cls.startsWith('.')) cls = pkg + cls
        if (cls.isEmpty()) return null
        return pkg to cls
    }
}
