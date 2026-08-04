package com.wmods.wppenhacer.ui.miuix

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
import top.yukonga.miuix.kmp.theme.platformDynamicColors

internal val LocalManagerBlurEnabled = staticCompositionLocalOf { true }
internal val LocalManagerDarkMode = staticCompositionLocalOf { false }

@Composable
internal fun ManagerTheme(
    appearance: ManagerAppearanceSettings,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (appearance.themeMode) {
        ManagerThemeMode.SYSTEM -> systemDark
        ManagerThemeMode.DARK -> true
        ManagerThemeMode.LIGHT -> false
    }
    val systemSeed = if (appearance.useMonet && appearance.accent == ManagerAccent.SYSTEM) {
        platformDynamicColors(dark).primary
    } else null
    val keyColor = when (appearance.accent) {
        ManagerAccent.SYSTEM -> systemSeed
        ManagerAccent.BLUE -> Color(0xFF3482FF)
        ManagerAccent.PURPLE -> Color(0xFF6750A4)
        ManagerAccent.PINK -> Color(0xFFB0006D)
        ManagerAccent.RED -> Color(0xFFBA1A1A)
        ManagerAccent.ORANGE -> Color(0xFFB65D00)
        ManagerAccent.YELLOW -> Color(0xFF7D5700)
        ManagerAccent.GREEN -> Color(0xFF4FAF50)
        ManagerAccent.TEAL -> Color(0xFF006A6A)
        ManagerAccent.CYAN -> Color(0xFF06A6B8)
    }
    val palette = when (appearance.paletteStyle) {
        ManagerPaletteStyle.TONAL_SPOT -> ThemePaletteStyle.TonalSpot
        ManagerPaletteStyle.NEUTRAL -> ThemePaletteStyle.Neutral
        ManagerPaletteStyle.VIBRANT -> ThemePaletteStyle.Vibrant
        ManagerPaletteStyle.EXPRESSIVE -> ThemePaletteStyle.Expressive
        ManagerPaletteStyle.RAINBOW -> ThemePaletteStyle.Rainbow
        ManagerPaletteStyle.FRUIT_SALAD -> ThemePaletteStyle.FruitSalad
        ManagerPaletteStyle.MONOCHROME -> ThemePaletteStyle.Monochrome
        ManagerPaletteStyle.FIDELITY -> ThemePaletteStyle.Fidelity
        ManagerPaletteStyle.CONTENT -> ThemePaletteStyle.Content
    }
    val mode = when {
        appearance.useMonet && appearance.themeMode == ManagerThemeMode.SYSTEM -> ColorSchemeMode.MonetSystem
        appearance.useMonet && appearance.themeMode == ManagerThemeMode.LIGHT -> ColorSchemeMode.MonetLight
        appearance.useMonet -> ColorSchemeMode.MonetDark
        appearance.themeMode == ManagerThemeMode.SYSTEM -> ColorSchemeMode.System
        appearance.themeMode == ManagerThemeMode.LIGHT -> ColorSchemeMode.Light
        else -> ColorSchemeMode.Dark
    }
    val controller = remember(mode, keyColor, palette, dark) {
        ThemeController(
            colorSchemeMode = mode,
            keyColor = keyColor.takeIf { appearance.useMonet || appearance.accent != ManagerAccent.SYSTEM },
            colorSpec = ThemeColorSpec.Spec2025,
            paletteStyle = palette,
            isDark = dark,
        )
    }
    val rawColors = controller.currentColors()
    val colors = remember(rawColors, dark, appearance.pureBlack, appearance.useMonet) {
        if (appearance.useMonet && dark && appearance.pureBlack) {
            rawColors.copy(background = Color.Black, surface = Color.Black)
        } else rawColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
        }
    }

    MiuixTheme(colors = colors) {
        val platformDensity = LocalDensity.current
        val density = remember(platformDensity, appearance.interfaceScale) {
            Density(
                density = platformDensity.density * normalizeScale(appearance.interfaceScale),
                fontScale = platformDensity.fontScale,
            )
        }
        CompositionLocalProvider(
            LocalManagerBlurEnabled provides appearance.blurEnabled,
            LocalManagerDarkMode provides dark,
            LocalDensity provides density,
            content = content,
        )
    }
}
