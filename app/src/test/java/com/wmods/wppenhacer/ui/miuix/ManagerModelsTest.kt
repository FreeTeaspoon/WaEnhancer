package com.wmods.wppenhacer.ui.miuix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManagerModelsTest {
    @Test
    fun searchTargetsFollowRenderedGroupsIncludingRepeatedCategories() {
        fun spec(key: String, category: String) = PreferenceSpec(
            key = key,
            source = PreferenceSource.GENERAL,
            category = category,
            title = key,
            summary = null,
            kind = PreferenceKind.SWITCH,
        )
        val state = ManagerUiState(specs = listOf(
            spec("first", "One"),
            spec(ManagerAppearanceSettings.KEY_THEME_MODE, "One"),
            spec("other_group", "Two"),
            spec("same_group", "One"),
            spec("last", "Two"),
        ))
        val groups = state.groups(PreferenceSource.GENERAL)

        assertEquals(1, groups.preferenceItemIndex("first"))
        assertEquals(2, groups.preferenceItemIndex("same_group"))
        assertEquals(4, groups.preferenceItemIndex("other_group"))
        assertEquals(5, groups.preferenceItemIndex("last"))
        assertEquals(-1, groups.preferenceItemIndex(ManagerAppearanceSettings.KEY_THEME_MODE))
        assertEquals(-1, groups.preferenceItemIndex("missing"))
        assertEquals(-1, groups.preferenceItemIndex(null))
    }

    @Test
    fun routesRoundTripIncludingSearchHighlight() {
        val routes = listOf<ManagerRoute>(
            ManagerRoute.PreferencePage(PreferenceSource.PRIVACY, "call_privacy"),
            ManagerRoute.Search,
            ManagerRoute.Appearance,
            ManagerRoute.About,
            ManagerRoute.Recordings,
            ManagerRoute.Diagnostics,
            ManagerRoute.CallRecording,
            ManagerRoute.ThemeManager,
            ManagerRoute.ThemeEditor("Green theme"),
            ManagerRoute.Configuration,
            ManagerRoute.Updates,
        )
        routes.forEach { assertEquals(it, ManagerRoute.decode(it.encode())) }
    }

    @Test
    fun interfaceScaleIsFiniteClampedAndRounded() {
        assertEquals(.8f, normalizeScale(.1f))
        assertEquals(1.1f, normalizeScale(2f))
        assertEquals(1f, normalizeScale(Float.NaN))
        assertEquals(.93f, normalizeScale(.934f))
    }

    @Test
    fun featureSearchIncludesOptionsFromEveryPreferenceSource() {
        val specs = listOf(
            PreferenceSpec(
                key = "privacy_feature",
                source = PreferenceSource.PRIVACY,
                category = "Privacy",
                title = "Call blocker",
                summary = "Block unwanted calls",
                kind = PreferenceKind.SWITCH,
            ),
            PreferenceSpec(
                key = "home_wallpaper",
                source = PreferenceSource.HOME_SCREEN,
                category = "Appearance",
                title = "Wallpaper",
                summary = null,
                kind = PreferenceKind.FILE,
            ),
        )

        assertEquals(listOf("privacy_feature"), searchPreferenceSpecs(specs, "block").map { it.key })
        assertEquals(listOf("home_wallpaper"), searchPreferenceSpecs(specs, "home screen").map { it.key })
        assertEquals(listOf("home_wallpaper"), searchPreferenceSpecs(specs, "wallpaper").map { it.key })
    }

    @Test
    fun managerAppearanceOptionsDoNotAppearAsWhatsAppFeatures() {
        val specs = listOf(
            PreferenceSpec(
                key = ManagerAppearanceSettings.KEY_THEME_MODE,
                source = PreferenceSource.GENERAL,
                category = "General",
                title = "Dark Theme Wa Enhancer",
                summary = "Force a theme mode in Wa Enhancer",
                kind = PreferenceKind.LIST,
            ),
            PreferenceSpec(
                key = "whatsapp_feature",
                source = PreferenceSource.GENERAL,
                category = "General",
                title = "Hide online status",
                summary = null,
                kind = PreferenceKind.SWITCH,
            ),
        )

        assertTrue(searchPreferenceSpecs(specs, "theme").isEmpty())
        assertEquals(listOf("whatsapp_feature"), searchPreferenceSpecs(specs, "general").map { it.key })
        assertEquals(listOf("whatsapp_feature"), searchPreferenceSpecs(specs, "status").map { it.key })
    }
}
