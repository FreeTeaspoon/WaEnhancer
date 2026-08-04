package com.wmods.wppenhacer.ui.miuix

import org.junit.Assert.assertEquals
import org.junit.Test

class ManagerModelsTest {
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
}
