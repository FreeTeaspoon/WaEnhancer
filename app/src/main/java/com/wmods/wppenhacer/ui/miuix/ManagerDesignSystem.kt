package com.wmods.wppenhacer.ui.miuix

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wmods.wppenhacer.R
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

internal object ManagerTokens {
    val OuterMargin = 12.dp
    val CardCorner = 16.dp
    val PageItemBottomSpacing = 6.dp
    val MaxContentWidth = 880.dp
}

internal fun Modifier.managerPageItem(): Modifier = padding(
    start = ManagerTokens.OuterMargin,
    end = ManagerTokens.OuterMargin,
    bottom = ManagerTokens.PageItemBottomSpacing,
)

@Composable
internal fun rememberManagerBackdrop(): LayerBackdrop? {
    if (
        !LocalManagerBlurEnabled.current ||
        !isRenderEffectSupported() ||
        !isRuntimeShaderSupported()
    ) return null
    val surface = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surface)
        drawContent()
    }
}

@Composable
internal fun ManagerBlurredBar(
    backdrop: LayerBackdrop?,
    alpha: Float = 0.8f,
    progressive: Boolean = false,
    scrollBehavior: ScrollBehavior? = null,
    content: @Composable () -> Unit,
) {
    val modifier = if (backdrop == null || progressive) Modifier else Modifier.textureBlur(
        backdrop = backdrop,
        shape = RectangleShape,
        blurRadius = 25f,
        colors = BlurColors(
            blendColors = listOf(BlendColorEntry(MiuixTheme.colorScheme.surface.copy(alpha = alpha))),
        ),
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (backdrop != null && progressive) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        this.alpha = scrollBehavior?.state
                            ?.let { (-it.contentOffset / 48.dp.toPx()).coerceIn(0f, 1f) }
                            ?: 1f
                    }
                    .progressiveTextureBlur(
                        backdrop = backdrop,
                        shape = RectangleShape,
                        gradient = ProgressiveBlur.Top.copy(curve = 2.2f),
                        blurRadius = 10f,
                        colors = BlurColors(
                            blendColors = listOf(
                                BlendColorEntry(MiuixTheme.colorScheme.surface.copy(alpha = 0.3f)),
                            ),
                        ),
                    ),
            )
        }
        content()
    }
}

@Composable
internal fun managerBarColor(backdrop: LayerBackdrop?): Color =
    if (backdrop == null) MiuixTheme.colorScheme.surface else Color.Transparent

@Composable
internal fun ManagerGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.managerPageItem().fillMaxWidth(),
        cornerRadius = ManagerTokens.CardCorner,
        insideMargin = PaddingValues(0.dp),
        content = { content() },
    )
}

internal fun LazyListScope.managerSection(title: String, key: String) {
    item(key = "section-$key") {
        SmallTitle(text = title, modifier = Modifier.semantics { heading() })
    }
}

internal class ManagerCardItem(
    val key: String,
    val content: @Composable ColumnScope.() -> Unit,
)

@Composable
private fun ManagerCardSegment(
    isFirst: Boolean,
    isLast: Boolean,
    modifier: Modifier = Modifier,
    outerTopPadding: Dp = 0.dp,
    outerBottomPadding: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val top = if (isFirst) 16.dp else 0.dp
    val bottom = if (isLast) 16.dp else 0.dp
    val color = MiuixTheme.colorScheme.surfaceContainer
    val surface = if (top == 0.dp && bottom == 0.dp) {
        Modifier.background(color)
    } else {
        Modifier.squircleSurface(color, top, top, bottom, bottom)
    }
    CompositionLocalProvider(LocalContentColor provides MiuixTheme.colorScheme.onSurfaceContainer) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    start = 12.dp,
                    top = outerTopPadding,
                    end = 12.dp,
                    bottom = outerBottomPadding,
                )
                .then(surface),
            content = content,
        )
    }
}

internal fun LazyListScope.managerGroupedCardItems(
    keyPrefix: String,
    items: List<ManagerCardItem>,
    outerTopPadding: Dp = 0.dp,
    outerBottomPadding: Dp = 6.dp,
) {
    items.forEachIndexed { index, item ->
        item(key = "$keyPrefix:${item.key}") {
            ManagerCardSegment(
                isFirst = index == 0,
                isLast = index == items.lastIndex,
                outerTopPadding = if (index == 0) outerTopPadding else 0.dp,
                outerBottomPadding = if (index == items.lastIndex) outerBottomPadding else 0.dp,
                content = item.content,
            )
        }
    }
}

@Composable
internal fun rememberManagerListScrollBehavior(listState: LazyListState): ScrollBehavior {
    val behavior = MiuixScrollBehavior()
    val blurFadeDistance = with(LocalDensity.current) { 48.dp.toPx() }
    LaunchedEffect(listState, behavior, blurFadeDistance) {
        // Direct jumps and restored list positions do not dispatch nested scroll events.
        // Use the measured list position to recover the bar and its blur in those cases.
        snapshotFlow { listState.layoutInfo }
            .collect { layout ->
                if (layout.visibleItemsInfo.isEmpty()) return@collect
                val offset = if (listState.firstVisibleItemIndex == 0) {
                    listState.firstVisibleItemScrollOffset.toFloat()
                } else {
                    maxOf(blurFadeDistance, -behavior.state.heightOffsetLimit)
                }
                behavior.state.contentOffset = -offset
                if (listState.canScrollBackward) {
                    behavior.state.heightOffset = behavior.state.heightOffsetLimit
                }
            }
    }
    return behavior
}

@Composable
internal fun ManagerDetailScaffold(
    title: String,
    wide: Boolean,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    contentMaxWidth: Dp? = ManagerTokens.MaxContentWidth,
    fullWidthSafeInsets: Boolean = false,
    listState: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    val behavior = rememberManagerListScrollBehavior(listState)
    val backdrop = rememberManagerBackdrop()
    Scaffold(
        modifier = modifier,
        containerColor = MiuixTheme.colorScheme.surface,
        topBar = {
            ManagerBlurredBar(backdrop, progressive = true, scrollBehavior = behavior) {
                val navigation: (@Composable () -> Unit)? = onBack?.let { callback ->
                    {
                        IconButton(onClick = callback) {
                            Icon(imageVector = MiuixIcons.Back, contentDescription = stringResource(R.string.manager_back))
                        }
                    }
                }
                if (wide) {
                    SmallTopAppBar(
                        title = title,
                        color = managerBarColor(backdrop),
                        navigationIcon = { navigation?.invoke() },
                        scrollBehavior = behavior,
                    )
                } else {
                    TopAppBar(
                        title = title,
                        color = managerBarColor(backdrop),
                        navigationIcon = { navigation?.invoke() },
                        scrollBehavior = behavior,
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (contentMaxWidth != null) Modifier.widthIn(max = contentMaxWidth) else Modifier)
                    .then(
                        if (fullWidthSafeInsets) Modifier.windowInsetsPadding(
                            WindowInsets.displayCutout.union(WindowInsets.navigationBars).only(WindowInsetsSides.Horizontal),
                        ) else Modifier,
                    )
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                    .nestedScroll(behavior.nestedScrollConnection)
                    .scrollEndHaptic()
                    .overScrollVertical(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 24.dp,
                ),
                content = content,
            )
        }
    }
}
