package com.freeteaspoon.wppenhacer.ui.miuix

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import com.freeteaspoon.wppenhacer.R

class ManagerParityTest {
    @get:Rule val rule = createAndroidComposeRule<MiuixMainActivity>()
    private fun text(id: Int) = rule.activity.getString(id)
    private fun tools() { rule.onNodeWithTag(primaryNavigationTag(2), true).performClick(); rule.waitForIdle() }

    @Test fun fileImportMatchesPlatformStorageAccess() {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(rule.activity)
        val previous = prefs.all["wallpaper"] as? Boolean
        try {
            rule.runOnIdle { prefs.edit().putBoolean("wallpaper", true).commit() }
            openFilePreference("wallpaper_file")
            // Configure appops before instrumentation. Android kills this process
            // when MANAGE_EXTERNAL_STORAGE changes, including from system Settings.
            if (android.os.Environment.isExternalStorageManager()) {
                rule.onNodeWithText(text(R.string.storage_permission), true).assertDoesNotExist()
                rule.onNodeWithText(text(R.string.manager_choose_file)).assertIsEnabled()
            } else {
                rule.onNodeWithText(text(R.string.storage_permission), true).assertIsDisplayed()
                rule.onNodeWithText(text(R.string.manager_choose_file)).assertIsNotEnabled()
            }
        } finally {
            rule.runOnIdle {
                prefs.edit().apply { if (previous == null) remove("wallpaper") else putBoolean("wallpaper", previous) }.commit()
            }
        }
    }

    @Test fun xmlImportDoesNotRequirePublicStorageAccess() {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(rule.activity)
        val previous = prefs.all["bootloader_spoofer_custom"] as? Boolean
        try {
            rule.runOnIdle { prefs.edit().putBoolean("bootloader_spoofer_custom", true).commit() }
            openFilePreference("bootloader_spoofer_xml")
            rule.onNodeWithText(text(R.string.storage_permission), true).assertDoesNotExist()
            rule.onNodeWithText(text(R.string.manager_choose_file)).assertIsEnabled()
        } finally {
            rule.runOnIdle {
                prefs.edit().apply { if (previous == null) remove("bootloader_spoofer_custom") else putBoolean("bootloader_spoofer_custom", previous) }.commit()
            }
        }
    }

    private fun openFilePreference(key: String) {
        rule.onNodeWithTag(primaryNavigationTag(1), true).performClick()
        rule.onNodeWithTag("features-search", true).performTextInput(key)
        val spec = PreferenceRegistry.build(rule.activity).first { it.key == key }
        rule.onNodeWithText(spec.title, true).performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("preference-$key", true).performClick()
        rule.waitForIdle()
    }

    @Test fun featureSwitchRespondsToRowAndThumbTouches() {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(rule.activity)
        val previous = prefs.all["custom_filters"] as? Boolean
        try {
            rule.runOnIdle { prefs.edit().putBoolean("custom_filters", false).commit() }
            rule.onNodeWithTag(primaryNavigationTag(1)).performClick()
            rule.onNodeWithTag("features-search", true).performTextInput("custom_filters")
            val spec = PreferenceRegistry.build(rule.activity).first { it.key == "custom_filters" }
            rule.onNodeWithText(spec.title, true).performClick()
            rule.waitForIdle()
            val row = rule.onNodeWithTag("preference-custom_filters", true)
            row.performTouchInput { click(androidx.compose.ui.geometry.Offset(width * .3f, height * .5f)) }
            rule.waitForIdle()
            org.junit.Assert.assertTrue(prefs.getBoolean("custom_filters", false))
            row.performTouchInput { click(androidx.compose.ui.geometry.Offset(width * .87f, height * .5f)) }
            rule.waitForIdle()
            org.junit.Assert.assertFalse(prefs.getBoolean("custom_filters", true))
        } finally {
            rule.runOnIdle {
                prefs.edit().apply { if (previous == null) remove("custom_filters") else putBoolean("custom_filters", previous) }.commit()
            }
        }
    }

    @Test fun liquidGlassDragCommitsAfterReversalAndRelease() {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(rule.activity)
        val keys = listOf(ManagerAppearanceSettings.KEY_FLOATING_NAVIGATION,
            ManagerAppearanceSettings.KEY_FLOATING_STYLE, ManagerAppearanceSettings.KEY_NAVIGATION_CONTENT)
        val previous = prefs.all.filterKeys { it in keys }
        try {
            rule.runOnIdle {
                prefs.edit().putBoolean(ManagerAppearanceSettings.KEY_FLOATING_NAVIGATION, true)
                    .putString(ManagerAppearanceSettings.KEY_FLOATING_STYLE, ManagerFloatingStyle.LIQUID_GLASS.name)
                    .putString(ManagerAppearanceSettings.KEY_NAVIGATION_CONTENT, ManagerNavigationContent.ICON_AND_TEXT.name).commit()
            }
            rule.onNodeWithTag(primaryNavigationTag(0)).performClick()
            rule.waitForIdle()
            val start = rule.onNodeWithTag(primaryNavigationTag(0)).fetchSemanticsNode().boundsInRoot.center
            val end = rule.onNodeWithTag(primaryNavigationTag(2)).fetchSemanticsNode().boundsInRoot.center
            rule.onRoot().performTouchInput { down(start); moveTo(start + (end - start) * .1f, 300) }
            rule.onNodeWithTag(primaryNavigationTag(0)).assertIsSelected()
            rule.onRoot().performTouchInput { moveTo(end, 450) }
            // The native bar previews the pill while held and commits on release.
            rule.onNodeWithTag(primaryNavigationTag(0)).assertIsSelected()
            rule.onRoot().performTouchInput { moveTo(start, 450) }
            rule.onNodeWithTag(primaryNavigationTag(0)).assertIsSelected()
            rule.onRoot().performTouchInput { up() }
            rule.onNodeWithTag(primaryNavigationTag(0)).assertIsSelected()
            rule.onRoot().performTouchInput { down(start); moveTo(start + (end - start) * .1f, 600); up() }
            rule.onNodeWithTag(primaryNavigationTag(0)).assertIsSelected()
            rule.onRoot().performTouchInput { down(start); moveTo(end, 600); up() }
            rule.onNodeWithTag(primaryNavigationTag(2)).assertIsSelected()
        } finally {
            rule.runOnIdle {
                prefs.edit().apply {
                    keys.forEach(::remove)
                    previous.forEach { (key, value) -> when (value) {
                        is Boolean -> putBoolean(key, value)
                        is String -> putString(key, value)
                    } }
                }.commit()
            }
        }
    }

    @Test fun replacingAppearanceSettingsUpdatesPlatformBackAndKeepsChildRoute() {
        if (android.os.Build.VERSION.SDK_INT < 34) return
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(rule.activity)
        val original = prefs.all.mapValues { requireNotNull(it.value) }
        val key = ManagerAppearanceSettings.KEY_PREDICTIVE_BACK
        try {
            tools()
            rule.onNodeWithText(text(R.string.manager_appearance), true).performClick()
            rule.waitForIdle()
            for (enabled in listOf(!(original[key] == true), original[key] == true)) {
                val before = rule.activity
                rule.runOnIdle { PreferenceController(before, prefs).replaceAll(original + (key to enabled)) }
                rule.waitUntil(10000) { rule.activity !== before }
                rule.waitForIdle()
                rule.onNodeWithText(text(R.string.manager_predictive_back), true).assertIsDisplayed()
                val platformValue = org.lsposed.hiddenapibypass.HiddenApiBypass.invoke(
                    android.content.pm.ApplicationInfo::class.java, rule.activity.applicationInfo,
                    "isOnBackInvokedCallbackEnabled") as Boolean
                org.junit.Assert.assertEquals(enabled, platformValue)
            }
        } finally {
            rule.runOnIdle { PreferenceController(rule.activity, prefs).replaceAll(original) }
        }
    }

    @Test fun popupBackAndChildBackKeepActivityAlive() {
        tools()
        rule.onNodeWithText(text(R.string.manager_appearance), true).performClick()
        rule.onNodeWithText(text(R.string.manager_theme_mode), true).performClick()
        rule.waitForIdle()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("input keyevent KEYCODE_BACK").close()
        rule.waitForIdle()
        rule.onNodeWithText(text(R.string.manager_theme_mode), true).assertIsDisplayed()
        rule.onNodeWithContentDescription(text(R.string.manager_back), true).performClick()
        rule.waitForIdle()
        rule.onNodeWithTag(primaryNavigationTag(2), true).assertIsDisplayed()
    }

    @Test fun featureSettingsUseChildRoutesAndReturnToFeatures() {
        rule.onNodeWithTag(primaryNavigationTag(1), true).performClick()
        rule.onNodeWithTag("features-search", true).performTextInput("wallpaper_file")
        rule.waitForIdle()
        val spec = PreferenceRegistry.build(rule.activity).first { it.key == "wallpaper_file" }
        rule.onNodeWithText(spec.title, true).performClick()
        rule.waitForIdle()
        // NavDisplay retains the shell underneath the child to preserve its state.
        rule.onNodeWithTag("preference-${spec.key}", true).assertIsDisplayed()
        rule.onNodeWithContentDescription(text(R.string.manager_back), true).performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("features-search", true).assertIsDisplayed()
    }

    @Test fun backDuringTabMotionReturnsToHome() {
        rule.mainClock.autoAdvance = false
        try {
            rule.onNodeWithTag(primaryNavigationTag(2), true).performClick()
            rule.mainClock.advanceTimeByFrame()
            systemBack()
            rule.mainClock.advanceTimeBy(1000)
            rule.onNodeWithTag(primaryNavigationTag(0), true).assertIsSelected()
        } finally {
            rule.mainClock.autoAdvance = true
        }
    }

    @Test fun aboutAnimationAllowsMenuBackAndChildBack() {
        tools()
        rule.onNodeWithText(text(R.string.about), true).performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription(text(R.string.manager_more), true).performClick()
        rule.onNodeWithText(text(R.string.manager_credits), true).assertIsDisplayed()
        systemBack()
        rule.waitForIdle()
        rule.onNodeWithContentDescription(text(R.string.manager_back), true).performClick()
        rule.waitForIdle()
        rule.onNodeWithTag(primaryNavigationTag(2), true).assertIsSelected()
    }

    @Test fun backImmediatelyAfterChildPopReturnsToHome() {
        tools()
        rule.onNodeWithText(text(R.string.manager_appearance), true).performClick()
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        try {
            rule.onNodeWithContentDescription(text(R.string.manager_back), true).performClick()
            rule.mainClock.advanceTimeByFrame()
            systemBack()
            rule.mainClock.advanceTimeBy(1000)
            rule.onNodeWithTag(primaryNavigationTag(0), true).assertIsSelected()
        } finally {
            rule.mainClock.autoAdvance = true
        }
    }

    private fun systemBack() {
        shell("input keyevent KEYCODE_BACK")
    }

    private fun shell(value: String) {
        val command = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .uiAutomation.executeShellCommand(value)
        android.os.ParcelFileDescriptor.AutoCloseInputStream(command).use { it.readBytes() }
    }
}
