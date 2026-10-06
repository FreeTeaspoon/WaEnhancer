/* About composition adapted from Language Selector 1220669f, Apache-2.0,
 * and Mishka e855709c, GPL-3.0. Product branding and dependencies belong to WaEnhancer. */
package com.freeteaspoon.wppenhacer.ui.miuix

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import com.freeteaspoon.wppenhacer.BuildConfig
import com.freeteaspoon.wppenhacer.R

data class AboutDependency(
    val name: String,
    val summary: String,
    val url: String?
)

data class AboutUiState(
    val title: String,
    val appName: String,
    val versionName: String,
    val versionCode: Int,
    val sourceUrl: String,
    val dependencies: List<AboutDependency>
)

data class AboutScreenActions(
    val onBack: () -> Unit,
    val onOpenUrl: (String) -> Unit,
    val onCredits: () -> Unit
)

@Composable
internal fun ManagerAboutScreen(navigateBack: () -> Unit, onCredits: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val state = AboutUiState(
        title = stringResource(R.string.about),
        appName = stringResource(R.string.app_name),
        versionName = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE,
        sourceUrl = BuildConfig.GITHUB_REPOSITORY_URL,
        dependencies = remember { staticDependencies() }
    )

    AboutScreenContent(
        state = state,
        actions = AboutScreenActions(
            onBack = navigateBack,
            onOpenUrl = { url -> runCatching { uriHandler.openUri(url) } },
            onCredits = onCredits
        )
    )
}

@Composable
private fun AboutScreenContent(
    state: AboutUiState,
    actions: AboutScreenActions
) {
    val layoutDirection = LocalLayoutDirection.current
    val scrollBehavior = MiuixScrollBehavior()
    val lazyListState = rememberLazyListState()
    val scrollProgressState = remember {
        derivedStateOf {
            when {
                lazyListState.firstVisibleItemIndex > 0 -> 1f
                else -> {
                    val spacer = lazyListState.layoutInfo.visibleItemsInfo
                        .firstOrNull { it.key == "logoSpacer" }
                    if (spacer != null && spacer.size > 0) {
                        (lazyListState.firstVisibleItemScrollOffset.toFloat() / spacer.size)
                            .coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                }
            }
        }
    }
    val heroCollapsed by remember { derivedStateOf { scrollProgressState.value == 1f } }
    val titleAlpha by remember {
        derivedStateOf { ((scrollProgressState.value - 0.35f) / 0.65f).coerceIn(0f, 1f) }
    }
    val barBackdrop = rememberManagerBackdrop()
    val barColor = if (barBackdrop != null) {
        Color.Transparent
    } else if (heroCollapsed) {
        MiuixTheme.colorScheme.surface
    } else {
        Color.Transparent
    }

    Scaffold(
        topBar = {
            ManagerBlurredBar(
                backdrop = barBackdrop,
                progressive = true,
                scrollBehavior = scrollBehavior
            ) {
                SmallTopAppBar(
                    modifier = Modifier.windowInsetsPadding(
                        WindowInsets.displayCutout.union(WindowInsets.navigationBars).only(WindowInsetsSides.Horizontal),
                    ),
                    title = state.title,
                    color = barColor,
                    titleColor = MiuixTheme.colorScheme.onSurface.copy(alpha = titleAlpha),
                    navigationIcon = { top.yukonga.miuix.kmp.basic.IconButton(onClick = actions.onBack) {
                        top.yukonga.miuix.kmp.basic.Icon(top.yukonga.miuix.kmp.icon.MiuixIcons.Back, stringResource(R.string.manager_back),
                            Modifier.graphicsLayer { scaleX = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl) -1f else 1f })
                    } },
                    scrollBehavior = scrollBehavior,
                    actions = {
                        top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu(entry = top.yukonga.miuix.kmp.basic.DropdownEntry(listOf(
                            top.yukonga.miuix.kmp.basic.DropdownItem(stringResource(R.string.manager_credits), onClick = actions.onCredits)
                        ))) { top.yukonga.miuix.kmp.basic.Icon(MiuixIcons.More, stringResource(R.string.manager_more)) }
                    },
                    defaultWindowInsetsPadding = false
                )
            }
        },
        contentWindowInsets = WindowInsets.systemBars
            .union(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Box(modifier = if (barBackdrop != null) Modifier.layerBackdrop(barBackdrop) else Modifier) {
            AboutContent(
                state = state,
                actions = actions,
                innerPadding = innerPadding,
                scrollBehavior = scrollBehavior,
                lazyListState = lazyListState,
                scrollProgress = { scrollProgressState.value }
            )
        }
    }
}

@Composable
private fun AboutContent(
    state: AboutUiState,
    actions: AboutScreenActions,
    innerPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
    lazyListState: LazyListState,
    scrollProgress: () -> Float
) {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val contentBackdrop = rememberLayerBackdrop()
    val isDark = LocalManagerDarkMode.current
    val blurEnabled = LocalManagerBlurEnabled.current && isRuntimeShaderSupported() && isRenderEffectSupported()
    val cardBlendColors = remember(isDark) {
        if (isDark) {
            listOf(
                BlendColorEntry(Color(0x4DA9A9A9), BlurBlendMode.Luminosity),
                BlendColorEntry(Color(0x1A9C9C9C), BlurBlendMode.PlusDarker)
            )
        } else {
            listOf(
                BlendColorEntry(Color(0x340034F9), BlurBlendMode.Overlay),
                BlendColorEntry(Color(0xB3FFFFFF.toInt()), BlurBlendMode.HardLight)
            )
        }
    }
    val logoBlendColors = remember(isDark) {
        if (isDark) {
            listOf(
                BlendColorEntry(Color(0xE6A1A1A1.toInt()), BlurBlendMode.ColorDodge),
                BlendColorEntry(Color(0x4DE6E6E6), BlurBlendMode.LinearLight),
                BlendColorEntry(Color(0xFF1AF500.toInt()), BlurBlendMode.Lab)
            )
        } else {
            listOf(
                BlendColorEntry(Color(0xCC4A4A4A.toInt()), BlurBlendMode.ColorBurn),
                BlendColorEntry(Color(0xFF4F4F4F.toInt()), BlurBlendMode.LinearLight),
                BlendColorEntry(Color(0xFF1AF200.toInt()), BlurBlendMode.Lab)
            )
        }
    }
    var logoHeight by remember { mutableStateOf(300.dp) }
    val versionProgress = { ((scrollProgress() - 0.05f) / 0.15f).coerceIn(0f, 1f) }
    val projectNameProgress = { ((scrollProgress() - 0.20f) / 0.15f).coerceIn(0f, 1f) }
    val iconProgress = { ((scrollProgress() - 0.35f) / 0.15f).coerceIn(0f, 1f) }
    val scrollPadding = PaddingValues(
        top = innerPadding.calculateTopPadding(),
        start = innerPadding.calculateStartPadding(layoutDirection),
        end = innerPadding.calculateEndPadding(layoutDirection)
    )
    val logoPadding = PaddingValues(
        top = innerPadding.calculateTopPadding() + 40.dp,
        start = innerPadding.calculateStartPadding(layoutDirection),
        end = innerPadding.calculateEndPadding(layoutDirection)
    )
    val projects = remember(state.appName, state.sourceUrl, state.dependencies) {
        listOf(
            AboutDependency(
                name = state.appName,
                summary = state.sourceUrl.removePrefix("https://"),
                url = state.sourceUrl
            )
        ) + state.dependencies
    }

    HyperOsAboutBackground(
        modifier = Modifier.fillMaxSize(),
        backdropModifier = Modifier.layerBackdrop(contentBackdrop),
        dynamicBackground = blurEnabled,
        alpha = { 1f - scrollProgress() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = logoPadding.calculateTopPadding() + 52.dp,
                    start = logoPadding.calculateStartPadding(layoutDirection),
                    end = logoPadding.calculateEndPadding(layoutDirection)
                )
                .onSizeChanged { size ->
                    with(density) { logoHeight = size.height.toDp() }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .clipToBounds()
                    .graphicsLayer {
                        val progress = iconProgress()
                        alpha = 1f - progress
                        scaleX = 1f - progress * 0.05f
                        scaleY = 1f - progress * 0.05f
                    }
            ) {
                Image(
                    modifier = Modifier
                        .requiredSize(100.dp)
                        .then(
                            if (blurEnabled) {
                                Modifier.textureBlur(
                                    backdrop = contentBackdrop,
                                    shape = RoundedCornerShape(0.dp),
                                    blurRadius = 150f,
                                    colors = BlurColors(blendColors = logoBlendColors),
                                    contentBlendMode = BlendMode.DstIn,
                                    enabled = true
                                )
                            } else {
                                Modifier
                            }
                        ),
                    painter = painterResource(R.drawable.manager_about_mark),
                    colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onBackground),
                    contentDescription = state.appName
                )
            }
            Text(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 5.dp)
                    .graphicsLayer {
                        val progress = projectNameProgress()
                        alpha = 1f - progress
                        scaleX = 1f - progress * 0.05f
                        scaleY = 1f - progress * 0.05f
                    }
                    .then(
                        if (blurEnabled) {
                            Modifier.textureBlur(
                                backdrop = contentBackdrop,
                                shape = RoundedCornerShape(16.dp),
                                blurRadius = 150f,
                                colors = BlurColors(blendColors = logoBlendColors),
                                contentBlendMode = BlendMode.DstIn,
                                enabled = true
                            )
                        } else {
                            Modifier
                        }
                    ),
                text = state.appName,
                color = MiuixTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 35.sp
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val progress = versionProgress()
                        alpha = 1f - progress
                        scaleX = 1f - progress * 0.05f
                        scaleY = 1f - progress * 0.05f
                    },
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                text = "v${state.versionName} (${state.versionCode})",
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = scrollPadding.calculateTopPadding(),
                start = scrollPadding.calculateStartPadding(layoutDirection),
                end = scrollPadding.calculateEndPadding(layoutDirection)
            ),
            overscrollEffect = null
        ) {
            item(key = "logoSpacer") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            logoHeight + 52.dp + logoPadding.calculateTopPadding() -
                                scrollPadding.calculateTopPadding() + 126.dp
                        ),
                    contentAlignment = Alignment.TopCenter,
                    content = {}
                )
            }
            item(key = "about") {
                Box {
                    Spacer(Modifier.fillParentMaxHeight())
                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                        SmallTitle(text = stringResource(R.string.manager_open_source), modifier = Modifier.semantics { heading() })
                        AboutCard(
                            blurEnabled = blurEnabled,
                            backdrop = contentBackdrop,
                            blendColors = cardBlendColors
                        ) {
                            projects.forEach { project ->
                                ArrowPreference(
                                    title = project.name,
                                    summary = project.summary,
                                    enabled = project.url != null,
                                    onClick = { project.url?.let(actions.onOpenUrl) }
                                )
                            }
                        }
                        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutCard(
    blurEnabled: Boolean,
    backdrop: LayerBackdrop,
    blendColors: List<BlendColorEntry>,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp)
            .then(
                if (blurEnabled) {
                    Modifier.textureBlur(
                        backdrop = backdrop,
                        shape = RoundedCornerShape(16.dp),
                        blurRadius = 60f,
                        colors = BlurColors(blendColors = blendColors),
                        enabled = true
                    )
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.defaultColors(
            color = if (blurEnabled) Color.Transparent else MiuixTheme.colorScheme.surfaceContainer,
            contentColor = Color.Transparent
        ),
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(0.dp),
        pressFeedbackType = top.yukonga.miuix.kmp.utils.PressFeedbackType.None,
        content = content
    )
}

private fun staticDependencies(): List<AboutDependency> = listOf(
    "Miuix" to "https://github.com/compose-miuix-ui/miuix",
    "YukiHookAPI" to "https://github.com/HighCapable/YukiHookAPI",
    "KavaRef" to "https://github.com/HighCapable/KavaRef",
    "libsu" to "https://github.com/topjohnwu/libsu",
    "DexKit" to "https://github.com/LuckyPray/DexKit",
    "OkHttp" to "https://github.com/square/okhttp",
    "RemotePreferences" to "https://github.com/apsun/RemotePreferences",
    "AndroidX" to "https://github.com/androidx/androidx",
    "Kotlin" to "https://github.com/JetBrains/kotlin",
    "Prism" to "https://github.com/PrismJS/prism",
    "Prism Live" to "https://github.com/PrismJS/live",
    "Bliss" to "https://github.com/LeaVerou/bliss",
    "libopus" to "https://github.com/xiph/opus",
    "libopusenc" to "https://github.com/xiph/libopusenc",
    "libogg" to "https://github.com/xiph/ogg",
).map { (name, url) -> AboutDependency(name, url.removePrefix("https://"), url) }
