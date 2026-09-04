package com.wmods.wppenhacer.ui.miuix

import android.os.Build
import kotlin.math.roundToInt

internal enum class PrimaryDestination {
    HOME,
    FEATURES,
    TOOLS,
}

internal sealed interface ManagerRoute {
    data class PreferencePage(val source: PreferenceSource, val highlightKey: String? = null) : ManagerRoute
    data object Search : ManagerRoute
    data object Appearance : ManagerRoute
    data object About : ManagerRoute
    data object Recordings : ManagerRoute
    data object Diagnostics : ManagerRoute
    data object CallRecording : ManagerRoute
    data object ThemeManager : ManagerRoute
    data class ThemeEditor(val folder: String) : ManagerRoute
    data object Configuration : ManagerRoute
    data object Updates : ManagerRoute

    fun encode(): String = when (this) {
        is PreferencePage -> "preferences:${source.name}:${highlightKey.orEmpty()}"
        Search -> "search"
        Appearance -> "appearance"
        About -> "about"
        Recordings -> "recordings"
        Diagnostics -> "diagnostics"
        CallRecording -> "call-recording"
        ThemeManager -> "theme-manager"
        is ThemeEditor -> "theme-editor:$folder"
        Configuration -> "configuration"
        Updates -> "updates"
    }

    companion object {
        fun decode(value: String): ManagerRoute? = when {
            value.startsWith("preferences:") -> value.split(':').let { parts ->
                PreferenceSource.entries.firstOrNull { it.name == parts.getOrNull(1) }?.let { source ->
                    PreferencePage(source, parts.getOrNull(2)?.takeIf(String::isNotBlank))
                }
            }
            value == "search" -> Search
            value == "appearance" -> Appearance
            value == "about" -> About
            value == "recordings" -> Recordings
            value == "diagnostics" -> Diagnostics
            value == "call-recording" -> CallRecording
            value == "theme-manager" -> ThemeManager
            value.startsWith("theme-editor:") -> ThemeEditor(value.substringAfter(':'))
            value == "configuration" -> Configuration
            value == "updates" -> Updates
            else -> null
        }
    }
}

internal enum class PreferenceSource {
    GENERAL,
    HOME_SCREEN,
    CONVERSATION,
    STATUS,
    PRIVACY,
    MEDIA,
    CUSTOMIZE,
}

internal enum class PreferenceKind {
    SWITCH,
    LIST,
    MULTI_LIST,
    TEXT,
    INTEGER_SLIDER,
    FLOAT_SLIDER,
    COLOR,
    FILE,
    CONTACTS,
    THEME,
    ACTION,
}

internal data class PreferenceSpec(
    val key: String,
    val source: PreferenceSource,
    val category: String,
    val title: String,
    val summary: String?,
    val kind: PreferenceKind,
    val entries: List<String> = emptyList(),
    val entryValues: List<String> = emptyList(),
    val defaultValue: String? = null,
    val dependency: String? = null,
    val enabledByXml: Boolean = true,
    val minimum: Float = 0f,
    val maximum: Float = 100f,
    val step: Float = 1f,
)

internal fun PreferenceSpec.isManagerAppearancePreference(): Boolean = key in setOf(
    ManagerAppearanceSettings.KEY_THEME_MODE,
    ManagerAppearanceSettings.KEY_COLOR_MODE,
    ManagerAppearanceSettings.KEY_COLOR_PRESET,
    ManagerAppearanceSettings.KEY_FORCE_ENGLISH,
)

internal data class PreferenceGroup(
    val title: String,
    val preferences: List<PreferenceSpec>,
)

internal fun searchPreferenceSpecs(
    specs: List<PreferenceSpec>,
    query: String,
): List<PreferenceSpec> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return emptyList()

    return specs.filter { spec ->
        !spec.isManagerAppearancePreference() && (
            spec.title.contains(normalizedQuery, ignoreCase = true) ||
                spec.summary.orEmpty().contains(normalizedQuery, ignoreCase = true) ||
                spec.key.contains(normalizedQuery, ignoreCase = true) ||
                spec.category.contains(normalizedQuery, ignoreCase = true) ||
                spec.source.name.replace('_', ' ').contains(normalizedQuery, ignoreCase = true)
            )
    }
}

internal enum class ManagerThemeMode { SYSTEM, DARK, LIGHT }
internal enum class ManagerPaletteStyle { TONAL_SPOT, NEUTRAL, VIBRANT, EXPRESSIVE, RAINBOW, FRUIT_SALAD, MONOCHROME, FIDELITY, CONTENT }
internal enum class ManagerAccent { SYSTEM, BLUE, PURPLE, PINK, RED, ORANGE, YELLOW, GREEN, TEAL, CYAN }
internal enum class ManagerFloatingStyle { MIUIX, LIQUID_GLASS }
internal enum class ManagerNavigationContent { ICON_AND_TEXT, ICON_ONLY }

internal data class ManagerAppearanceSettings(
    val themeMode: ManagerThemeMode = ManagerThemeMode.SYSTEM,
    val forceEnglish: Boolean = false,
    val useMonet: Boolean = false,
    val paletteStyle: ManagerPaletteStyle = ManagerPaletteStyle.TONAL_SPOT,
    val accent: ManagerAccent = ManagerAccent.GREEN,
    val pureBlack: Boolean = false,
    val blurEnabled: Boolean = true,
    val predictiveBack: Boolean = false,
    val interfaceScale: Float = 1f,
    val floatingNavigation: Boolean = false,
    val floatingStyle: ManagerFloatingStyle = ManagerFloatingStyle.MIUIX,
    val navigationContent: ManagerNavigationContent = ManagerNavigationContent.ICON_AND_TEXT,
) {
    companion object {
        const val KEY_THEME_MODE = "thememode"
        const val KEY_FORCE_ENGLISH = "force_english"
        const val KEY_COLOR_MODE = "wae_color_mode"
        const val KEY_COLOR_PRESET = "wae_color_preset"
        const val KEY_PALETTE = "manager_ui_palette_style"
        const val KEY_PURE_BLACK = "manager_ui_pure_black"
        const val KEY_BLUR = "manager_ui_blur"
        const val KEY_PREDICTIVE_BACK = "manager_ui_predictive_back"
        const val KEY_SCALE = "manager_ui_scale"
        const val KEY_FLOATING_NAVIGATION = "manager_ui_floating_navigation"
        const val KEY_FLOATING_STYLE = "manager_ui_floating_style"
        const val KEY_NAVIGATION_CONTENT = "manager_ui_navigation_content"

        val managerKeys = setOf(
            KEY_PALETTE,
            KEY_PURE_BLACK,
            KEY_BLUR,
            KEY_PREDICTIVE_BACK,
            KEY_SCALE,
            KEY_FLOATING_NAVIGATION,
            KEY_FLOATING_STYLE,
            KEY_NAVIGATION_CONTENT,
        )

        fun from(values: Map<String, *>): ManagerAppearanceSettings {
            val themeMode = when (values[KEY_THEME_MODE] as? String) {
                "1" -> ManagerThemeMode.DARK
                "2" -> ManagerThemeMode.LIGHT
                else -> ManagerThemeMode.SYSTEM
            }
            val useMonet = values[KEY_COLOR_MODE] == "monet" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            val accent = when ((values[KEY_COLOR_PRESET] as? String).orEmpty()) {
                "system" -> ManagerAccent.SYSTEM
                "blue" -> ManagerAccent.BLUE
                "purple" -> ManagerAccent.PURPLE
                "pink" -> ManagerAccent.PINK
                "red" -> ManagerAccent.RED
                "orange" -> ManagerAccent.ORANGE
                "yellow" -> ManagerAccent.YELLOW
                "teal" -> ManagerAccent.TEAL
                "cyan" -> ManagerAccent.CYAN
                else -> ManagerAccent.GREEN
            }
            return ManagerAppearanceSettings(
                themeMode = themeMode,
                forceEnglish = values[KEY_FORCE_ENGLISH] as? Boolean ?: false,
                useMonet = useMonet,
                paletteStyle = enumValue(values[KEY_PALETTE], ManagerPaletteStyle.TONAL_SPOT),
                accent = accent,
                pureBlack = values[KEY_PURE_BLACK] as? Boolean ?: false,
                blurEnabled = values[KEY_BLUR] as? Boolean ?: true,
                predictiveBack = values[KEY_PREDICTIVE_BACK] as? Boolean ?: false,
                interfaceScale = normalizeScale((values[KEY_SCALE] as? Number)?.toFloat() ?: 1f),
                floatingNavigation = values[KEY_FLOATING_NAVIGATION] as? Boolean ?: false,
                floatingStyle = enumValue(values[KEY_FLOATING_STYLE], ManagerFloatingStyle.MIUIX),
                navigationContent = enumValue(values[KEY_NAVIGATION_CONTENT], ManagerNavigationContent.ICON_AND_TEXT),
            )
        }

        private inline fun <reified T : Enum<T>> enumValue(value: Any?, fallback: T): T =
            enumValues<T>().firstOrNull { it.name == value?.toString() } ?: fallback
    }
}

internal fun normalizeScale(value: Float): Float {
    if (!value.isFinite()) return 1f
    return (value.coerceIn(0.8f, 1.1f) * 100f).roundToInt() / 100f
}

internal data class ManagerUiState(
    val preferences: Map<String, Any?> = emptyMap(),
    val specs: List<PreferenceSpec> = emptyList(),
    val appearance: ManagerAppearanceSettings = ManagerAppearanceSettings(),
) {
    fun groups(source: PreferenceSource): List<PreferenceGroup> = specs
        .filter { it.source == source && !it.isManagerAppearancePreference() }
        .groupBy { it.category }
        .map { (title, items) -> PreferenceGroup(title, items) }
}
