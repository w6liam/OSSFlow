package app.ossflow.logic

import app.ossflow.model.AppSettings
import app.ossflow.model.WritingStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStylesTest {
    @Test
    fun smartIsTheDefaultAndNormalMigratesToOff() {
        assertEquals(WritingStyle.SMART, AppSettings.defaults().writingStyle)
        assertTrue(AppSettings.defaults().appStyleOverrides.isEmpty())
        assertEquals(WritingStyle.SMART, WritingStyle.fromStored(null))
        assertEquals(WritingStyle.SMART, WritingStyle.fromStored("  "))
        assertEquals(WritingStyle.OFF, WritingStyle.fromStored("NORMAL"))
        assertEquals(WritingStyle.OFF, WritingStyle.fromStored("OFF"))
        assertEquals(WritingStyle.FORMAL, WritingStyle.fromStored("FORMAL"))
        assertEquals(WritingStyle.INFORMAL, WritingStyle.fromStored("INFORMAL"))
        assertEquals(WritingStyle.SMART, WritingStyle.fromStored("SMART"))
        assertEquals(WritingStyle.SMART, WritingStyle.fromStored("nope"))
    }

    @Test
    fun smartMapsChatAppsToInformalAndWorkAppsToFormal() {
        val informal = mapOf(
            "com.whatsapp" to "WhatsApp",
            "com.whatsapp.w4b" to "WhatsApp",
            "com.google.android.apps.messaging" to "Messages",
            "com.android.mms" to "Messages",
            "com.samsung.android.messaging" to "Messages",
            "org.telegram.messenger" to "Telegram",
            "org.thoughtcrime.securesms" to "Signal",
            "com.facebook.orca" to "Messenger",
            "com.instagram.android" to "Instagram",
            "com.snapchat.android" to "Snapchat",
            "com.discord" to "Discord",
            "com.twitter.android" to "X",
        )
        val formal = mapOf(
            "com.Slack" to "Slack",
            "com.microsoft.teams" to "Teams",
            "com.google.android.gm" to "Gmail",
            "com.microsoft.office.outlook" to "Outlook",
            "com.google.android.apps.docs.editors.docs" to "Docs",
            "com.linkedin.android" to "LinkedIn",
            "notion.id" to "Notion",
            "com.atlassian.android.jira.core" to "Jira",
        )
        for ((pkg, name) in informal) {
            assertEquals(name, WritingStyle.INFORMAL, AppStyles.resolve(WritingStyle.SMART, pkg))
        }
        for ((pkg, name) in formal) {
            assertEquals(name, WritingStyle.FORMAL, AppStyles.resolve(WritingStyle.SMART, pkg))
        }
        assertEquals(WritingStyle.FORMAL, AppStyles.resolve(WritingStyle.SMART, "com.slack"))
    }

    @Test
    fun unknownAppsAndAMissingPackageStayOff() {
        assertEquals(WritingStyle.OFF, AppStyles.resolve(WritingStyle.SMART, "com.example.notes"))
        assertEquals(WritingStyle.OFF, AppStyles.resolve(WritingStyle.SMART, null))
        assertEquals(WritingStyle.OFF, AppStyles.resolve(WritingStyle.SMART, "  "))
        assertEquals(WritingStyle.OFF, AppStyles.resolve(WritingStyle.SMART, "com.google.android.inputmethod.latin"))
    }

    @Test
    fun anExplicitStyleIgnoresTheApp() {
        assertEquals(WritingStyle.OFF, AppStyles.resolve(WritingStyle.OFF, "com.whatsapp"))
        assertEquals(WritingStyle.FORMAL, AppStyles.resolve(WritingStyle.FORMAL, "com.whatsapp"))
        assertEquals(WritingStyle.INFORMAL, AppStyles.resolve(WritingStyle.INFORMAL, "com.Slack"))
    }

    @Test
    fun aPerAppOverrideReplacesTheBuiltInStyle() {
        val overrides = mapOf("com.whatsapp" to WritingStyle.FORMAL, "com.example.notes" to WritingStyle.INFORMAL)
        assertEquals(WritingStyle.FORMAL, AppStyles.resolve(WritingStyle.SMART, "com.whatsapp", overrides))
        assertEquals(WritingStyle.FORMAL, AppStyles.resolve(WritingStyle.SMART, "com.WhatsApp", overrides))
        assertEquals(WritingStyle.INFORMAL, AppStyles.resolve(WritingStyle.SMART, "com.whatsapp.w4b", overrides))
        assertEquals(WritingStyle.INFORMAL, AppStyles.resolve(WritingStyle.SMART, "com.example.notes", overrides))
        assertEquals(WritingStyle.OFF, AppStyles.resolve(WritingStyle.OFF, "com.whatsapp", overrides))
        val prompt = CleanupPrompt.system(AppStyles.resolve(WritingStyle.SMART, "com.Slack", overrides))
        assertEquals(CleanupPrompt.system(WritingStyle.FORMAL), prompt)
        assertFalse(prompt.contains("com.Slack"))
        assertFalse(prompt.contains("Slack"))
    }

    @Test
    fun overridesRoundTripAndIgnoreSmart() {
        val stored = AppStyles.encode(
            mapOf(
                "com.whatsapp" to WritingStyle.OFF,
                "com.example.notes" to WritingStyle.FORMAL,
                "com.Slack" to WritingStyle.SMART,
                "  " to WritingStyle.INFORMAL,
            ),
        )
        val decoded = AppStyles.decode(stored)
        assertEquals(WritingStyle.OFF, decoded["com.whatsapp"])
        assertEquals(WritingStyle.FORMAL, decoded["com.example.notes"])
        assertFalse(decoded.containsKey("com.Slack"))
        assertFalse(decoded.values.contains(WritingStyle.SMART))
        assertEquals(decoded, AppStyles.decode(AppStyles.encode(decoded)))
        assertTrue(AppStyles.decode(null).isEmpty())
        assertTrue(AppStyles.decode("not a line").isEmpty())
    }

    @Test
    fun settingAnAppStyleClearsItWhenItMatchesTheBuiltInStyle() {
        val whatsapp = AppStyles.chat.first { it.name == "WhatsApp" }
        val overridden = AppStyles.withApp(emptyMap(), whatsapp, WritingStyle.FORMAL)
        assertEquals(WritingStyle.FORMAL, AppStyles.selection(whatsapp, overridden))
        assertEquals(WritingStyle.FORMAL, overridden["com.whatsapp"])
        assertEquals(WritingStyle.FORMAL, overridden["com.whatsapp.w4b"])
        val cleared = AppStyles.withApp(overridden, whatsapp, WritingStyle.INFORMAL)
        assertTrue(cleared.isEmpty())
        assertEquals(WritingStyle.INFORMAL, AppStyles.selection(whatsapp, cleared))
        val added = AppStyles.withPackage(emptyMap(), "com.example.notes", WritingStyle.OFF)
        assertEquals(listOf("com.example.notes" to WritingStyle.OFF), AppStyles.extras(added))
        assertTrue(AppStyles.extras(AppStyles.withoutPackage(added, "com.example.notes")).isEmpty())
        assertTrue(AppStyles.isPackageName("com.example.notes"))
        assertTrue(AppStyles.isPackageName("com.Slack"))
        assertTrue(AppStyles.isPackageName("notion.id"))
        assertFalse(AppStyles.isPackageName("notes"))
        assertFalse(AppStyles.isPackageName("com."))
    }
}
