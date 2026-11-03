package app.ossflow.logic

import app.ossflow.model.WritingStyle

/**
 * One app whose built-in Smart style is formal or informal.
 *
 * [packages] are Android application ids. Several ids can be one app, such as
 * WhatsApp and WhatsApp Business. The cleanup prompt never receives these ids.
 */
data class DictationApp(
    val name: String,
    val packages: List<String>,
    val builtIn: WritingStyle,
)

/**
 * Maps the foreground app to a writing style when the setting is Smart.
 *
 * Chat apps are informal. Work apps are formal. Any other package, or no package,
 * stays [WritingStyle.OFF]. A per-app override replaces the built-in style for that
 * package only. Off, Formal, and Informal ignore the package.
 */
object AppStyles {
    val chat: List<DictationApp> = listOf(
        app("WhatsApp", WritingStyle.INFORMAL, "com.whatsapp", "com.whatsapp.w4b"),
        app(
            "Messages",
            WritingStyle.INFORMAL,
            "com.google.android.apps.messaging",
            "com.android.mms",
            "com.samsung.android.messaging",
        ),
        app("Telegram", WritingStyle.INFORMAL, "org.telegram.messenger"),
        app("Signal", WritingStyle.INFORMAL, "org.thoughtcrime.securesms"),
        app("Messenger", WritingStyle.INFORMAL, "com.facebook.orca"),
        app("Instagram", WritingStyle.INFORMAL, "com.instagram.android"),
        app("Snapchat", WritingStyle.INFORMAL, "com.snapchat.android"),
        app("Discord", WritingStyle.INFORMAL, "com.discord"),
        app("X", WritingStyle.INFORMAL, "com.twitter.android"),
    )

    val work: List<DictationApp> = listOf(
        app("Slack", WritingStyle.FORMAL, "com.Slack"),
        app("Microsoft Teams", WritingStyle.FORMAL, "com.microsoft.teams"),
        app("Gmail", WritingStyle.FORMAL, "com.google.android.gm"),
        app("Outlook", WritingStyle.FORMAL, "com.microsoft.office.outlook"),
        app("Google Docs", WritingStyle.FORMAL, "com.google.android.apps.docs.editors.docs"),
        app("LinkedIn", WritingStyle.FORMAL, "com.linkedin.android"),
        app("Notion", WritingStyle.FORMAL, "notion.id"),
        app("Jira", WritingStyle.FORMAL, "com.atlassian.android.jira.core"),
    )

    val apps: List<DictationApp> = chat + work

    /**
     * The style to put in the cleanup prompt.
     *
     * [packageName] is used only when [selected] is Smart. It is never copied into the prompt.
     */
    fun resolve(
        selected: WritingStyle,
        packageName: String?,
        overrides: Map<String, WritingStyle> = emptyMap(),
    ): WritingStyle {
        if (selected != WritingStyle.SMART) return selected
        val pkg = packageName?.trim().orEmpty()
        if (pkg.isEmpty()) return WritingStyle.OFF
        val override = overrideFor(pkg, overrides)
        if (override != null) return override
        return builtInByPackage[pkg.lowercase()] ?: WritingStyle.OFF
    }

    /** Style shown for [app], including an override stored on any of its packages. */
    fun selection(app: DictationApp, overrides: Map<String, WritingStyle>): WritingStyle {
        val chosen = app.packages.firstNotNullOfOrNull { overrideFor(it, overrides) }
        return chosen ?: app.builtIn
    }

    /** Sets [style] on every package of [app]. The built-in style clears the override. */
    fun withApp(
        overrides: Map<String, WritingStyle>,
        app: DictationApp,
        style: WritingStyle,
    ): Map<String, WritingStyle> {
        val next = overrides.toMutableMap()
        for (pkg in app.packages) {
            next.keys.filter { it.equals(pkg, ignoreCase = true) }.forEach { next.remove(it) }
        }
        if (style != app.builtIn && style != WritingStyle.SMART) {
            for (pkg in app.packages) next[pkg] = style
        }
        return next
    }

    /**
     * Sets [style] for one package.
     * A package that belongs to a known app updates that whole app.
     */
    fun withPackage(
        overrides: Map<String, WritingStyle>,
        packageName: String,
        style: WritingStyle,
    ): Map<String, WritingStyle> {
        val pkg = packageName.trim()
        val known = apps.firstOrNull { app -> app.packages.any { it.equals(pkg, ignoreCase = true) } }
        if (known != null) return withApp(overrides, known, style)
        if (style == WritingStyle.SMART || pkg.isEmpty()) return overrides
        val next = overrides.toMutableMap()
        next.keys.filter { it.equals(pkg, ignoreCase = true) }.forEach { next.remove(it) }
        next[pkg] = style
        return next
    }

    fun withoutPackage(overrides: Map<String, WritingStyle>, packageName: String): Map<String, WritingStyle> {
        val pkg = packageName.trim()
        return overrides.filterKeys { !it.equals(pkg, ignoreCase = true) }
    }

    /** Overrides whose package is not one of the built-in apps. */
    fun extras(overrides: Map<String, WritingStyle>): List<Pair<String, WritingStyle>> {
        return overrides.entries
            .filter { (pkg, style) -> style != WritingStyle.SMART && knownPackage(pkg) == null }
            .sortedBy { it.key.lowercase() }
            .map { it.key to it.value }
    }

    fun isPackageName(value: String): Boolean = PACKAGE_NAME.matches(value.trim())

    fun encode(overrides: Map<String, WritingStyle>): String =
        overrides.entries
            .mapNotNull { (pkg, style) ->
                val key = pkg.trim()
                if (key.isEmpty() || style == WritingStyle.SMART) null else key to style
            }
            .sortedBy { it.first.lowercase() }
            .joinToString("\n") { "${it.first}=${it.second.name}" }

    fun decode(raw: String?): Map<String, WritingStyle> {
        if (raw.isNullOrBlank()) return emptyMap()
        val out = linkedMapOf<String, WritingStyle>()
        for (line in raw.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val eq = trimmed.indexOf('=')
            if (eq <= 0 || eq == trimmed.lastIndex) continue
            val pkg = trimmed.substring(0, eq).trim()
            val style = WritingStyle.entries.firstOrNull { it.name == trimmed.substring(eq + 1).trim() } ?: continue
            if (pkg.isEmpty() || style == WritingStyle.SMART) continue
            out[pkg] = style
        }
        return out
    }

    private fun overrideFor(packageName: String, overrides: Map<String, WritingStyle>): WritingStyle? {
        val style = overrides[packageName]
            ?: overrides.entries.firstOrNull { it.key.equals(packageName, ignoreCase = true) }?.value
        return style?.takeIf { it != WritingStyle.SMART }
    }

    private fun knownPackage(packageName: String): String? =
        builtInByPackage[packageName.lowercase()]?.let { packageName }

    private fun app(name: String, style: WritingStyle, vararg packages: String): DictationApp =
        DictationApp(name, packages.toList(), style)

    private val builtInByPackage: Map<String, WritingStyle> = apps
        .flatMap { app -> app.packages.map { it.lowercase() to app.builtIn } }
        .toMap()

    private val PACKAGE_NAME = Regex("""^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+$""")
}
