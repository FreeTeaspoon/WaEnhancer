package com.freeteaspoon.wppenhacer.ui.miuix

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
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.ime
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
import com.freeteaspoon.wppenhacer.R
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
    alpha: Float = 0.82f,
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
        pressFeedbackType = top.yukonga.miuix.kmp.utils.PressFeedbackType.None,
        content = { content() },
    )
}

internal fun LazyListScope.managerSection(title: String, key: String) {
    item(key = "section-$key", contentType = PageStart.Heading) {
        SmallTitle(text = title, modifier = Modifier.semantics { heading() })
    }
}

internal class ManagerCardItem(
    val key: String,
    val content: @Composable ColumnScope.() -> Unit,
)

internal fun LazyListScope.managerGroupedCardItems(
    keyPrefix: String,
    items: List<ManagerCardItem>,
    outerTopPadding: Dp = 0.dp,
    outerBottomPadding: Dp = 6.dp,
) {
    if (items.isEmpty()) return
    item(key = keyPrefix) {
        ManagerGroupCard(Modifier.padding(top = outerTopPadding).animateItem()) {
            Column { items.forEach { row -> androidx.compose.runtime.key(row.key) { row.content(this) } } }
        }
    }
}

@Composable
internal fun rememberManagerListScrollBehavior(listState: LazyListState): ScrollBehavior {
    val behavior = MiuixScrollBehavior()
    val blurFadeDistance = with(LocalDensity.current) { 48.dp.toPx() }
    LaunchedEffect(listState, behavior, blurFadeDistance) {
        var previous = emptyMap<Any, Int>()
        var initialized = false
        snapshotFlow { listState.layoutInfo to listState.isScrollInProgress }
            .collect { (layout, scrolling) ->
                if (layout.visibleItemsInfo.isEmpty()) return@collect
                val positions = layout.visibleItemsInfo.associate { it.key to it.offset }
                val common = positions.keys.firstOrNull { it in previous }
                val displacement = common?.let { positions.getValue(it) - previous.getValue(it) } ?: 0
                // Content-size changes do not send nested scroll. Preserve the collapsed bar.
                if (initialized && !scrolling && displacement != 0) {
                    behavior.state.contentOffset += displacement
                }
                val offset = if (listState.firstVisibleItemIndex == 0) {
                    listState.firstVisibleItemScrollOffset.toFloat()
                } else maxOf(blurFadeDistance, -behavior.state.heightOffsetLimit)
                // Restore/direct jumps also bypass nested scroll, including a jump to the top.
                if (!initialized || common == null || !scrolling) {
                    if (listState.canScrollBackward) {
                        behavior.state.contentOffset = minOf(behavior.state.contentOffset, -offset)
                        behavior.state.heightOffset = behavior.state.heightOffsetLimit
                    } else if (displacement == 0 || !initialized) {
                        behavior.state.contentOffset = 0f
                        behavior.state.heightOffset = 0f
                    }
                }
                initialized = true
                previous = positions
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
    bottomPadding: Dp = 0.dp,
    isRefreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    pageState: ManagerPageStateKind = ManagerPageStateKind.CONTENT,
    stateTitle: String = stringResource(if (pageState == ManagerPageStateKind.ERROR) R.string.manager_load_failed else R.string.manager_loading),
    stateMessage: String = "",
    onRetry: (() -> Unit)? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    fixedContent: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
    val pageContent = MishkaPageContent(content)
    val behavior = rememberManagerListScrollBehavior(listState)
    val backdrop = rememberManagerBackdrop()
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.fillMaxSize()) {
        val useSmallBar = wide || maxWidth >= 600.dp
        val sideGutter = contentMaxWidth?.let { ((maxWidth - it) / 2).coerceAtLeast(0.dp) } ?: 0.dp
        val horizontalInsets = WindowInsets.displayCutout.union(WindowInsets.navigationBars).only(WindowInsetsSides.Horizontal)
        Scaffold(
            containerColor = MiuixTheme.colorScheme.surface,
            topBar = {
                ManagerBlurredBar(backdrop, progressive = true, scrollBehavior = behavior) {
                    val navigation: @Composable () -> Unit = {
                        onBack?.let { callback ->
                            IconButton(onClick = callback) {
                                Icon(MiuixIcons.Back, stringResource(R.string.manager_back),
                                    Modifier.graphicsLayer { scaleX = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl) -1f else 1f })
                            }
                        }
                    }
                    if (useSmallBar) {
                        SmallTopAppBar(
                            title = title,
                            color = managerBarColor(backdrop),
                            navigationIcon = navigation,
                            scrollBehavior = behavior,
                            actions = actions,
                        )
                    } else {
                        TopAppBar(
                            title = title,
                            color = managerBarColor(backdrop),
                            navigationIcon = navigation,
                            scrollBehavior = behavior,
                            actions = actions,
                        )
                    }
                }
            },
        ) { padding ->
            Column(
                Modifier.fillMaxSize()
                    .windowInsetsPadding(horizontalInsets)
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
            ) {
                if (fixedContent != null) {
                    Box(Modifier.padding(top = padding.calculateTopPadding(), start = sideGutter, end = sideGutter)) {
                        fixedContent()
                    }
                }
                val listTopPadding = if (fixedContent == null) padding.calculateTopPadding() else 0.dp
                val safeBottom = maxOf(padding.calculateBottomPadding(), bottomPadding)
                val density = LocalDensity.current
                val stateBottom = maxOf(safeBottom, with(density) {
                    maxOf(WindowInsets.ime.getBottom(this), WindowInsets.captionBar.getBottom(this)).toDp()
                })
                val list: @Composable () -> Unit = {
                    ManagerPageStateHost(
                        state = pageState,
                        title = stateTitle,
                        message = stateMessage,
                        onRetry = onRetry,
                        modifier = Modifier.fillMaxSize(),
                        stateModifier = Modifier.padding(top = listTopPadding, bottom = stateBottom),
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                                .scrollEndHaptic()
                                .overScrollVertical()
                                .nestedScroll(behavior.nestedScrollConnection),
                            overscrollEffect = null,
                            contentPadding = PaddingValues(
                                start = sideGutter,
                                end = sideGutter,
                                top = listTopPadding + pageContent.topPadding,
                                bottom = safeBottom + 24.dp,
                            ),
                            content = pageContent.content,
                        )
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (onRefresh != null) {
                        top.yukonga.miuix.kmp.basic.PullToRefresh(
                            isRefreshing = isRefreshing,
                            onRefresh = onRefresh,
                            topAppBarScrollBehavior = behavior,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = listTopPadding + 12.dp),
                        ) { list() }
                    } else list()
                }
            }
        }
    }
}
