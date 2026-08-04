package com.wmods.wppenhacer.ui.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wmods.wppenhacer.R
import com.wmods.wppenhacer.ui.miuix.animation.rememberSystemAnimationsEnabled
import com.wmods.wppenhacer.ui.miuix.liquid.IosLiquidGlassNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.NavigationRailValue
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberNavigationRailState
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Tasks
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.res.stringResource
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

@Composable
internal fun ManagerMainShell(
    state: ManagerUiState,
    onNavigate: (ManagerRoute) -> Unit,
    isBackHandlerEnabled: Boolean = true,
) {
    val destinations = listOf(
        NavigationItem(stringResource(R.string.manager_home), MiuixIcons.Home),
        NavigationItem(stringResource(R.string.manager_features), MiuixIcons.Tasks),
        NavigationItem(stringResource(R.string.manager_tools), MiuixIcons.Settings),
    )
    val pager = rememberPagerState(pageCount = { destinations.size })
    val navigation = rememberPrimaryPagerNavigationState(pager, rememberSystemAnimationsEnabled())
    LaunchedEffect(pager.currentPage) { navigation.syncPage() }
    DisposableEffect(navigation) { onDispose(navigation::dispose) }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = isBackHandlerEnabled && navigation.selectedPage != 0,
        onBackCompleted = { navigation.navigateTo(0) },
    )

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp
        val expanded = maxWidth >= 1200.dp
        val content: @Composable (Modifier, androidx.compose.ui.unit.Dp) -> Unit = { modifier, bottomPadding ->
            HorizontalPager(
                state = pager,
                modifier = modifier,
                verticalAlignment = Alignment.Top,
                key = { PrimaryDestination.entries[it].name },
            ) { page ->
                ManagerPrimaryDestinationScreen(
                    destination = PrimaryDestination.entries[page],
                    state = state,
                    wide = wide,
                    bottomPadding = bottomPadding,
                    onNavigate = onNavigate,
                    onSelectPrimary = { navigation.navigateTo(it.ordinal) },
                )
            }
        }
        if (wide) {
            val rail = rememberNavigationRailState(
                initialValue = if (expanded) NavigationRailValue.Expanded else NavigationRailValue.Collapsed,
            )
            LaunchedEffect(expanded) { if (expanded) rail.expand() else rail.collapse() }
            Row(Modifier.fillMaxSize().background(MiuixTheme.colorScheme.surface)) {
                NavigationRail(state = rail) {
                    destinations.forEachIndexed { index, item ->
                        NavigationRailItem(
                            selected = navigation.selectedPage == index,
                            onClick = { navigation.navigateTo(index) },
                            icon = item.icon,
                            label = item.label,
                            modifier = Modifier.testTag(primaryNavigationTag(index)),
                        )
                    }
                }
                content(Modifier.weight(1f), 0.dp)
            }
        } else {
            val backdrop = rememberManagerBackdrop()
            val appearance = state.appearance
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    when {
                        appearance.floatingNavigation && appearance.floatingStyle == ManagerFloatingStyle.LIQUID_GLASS ->
                            IosLiquidGlassNavigationBar(
                                items = destinations,
                                selectedIndex = navigation.selectedPage,
                                onItemClick = navigation::navigateTo,
                                backdrop = backdrop,
                                isBlurActive = backdrop != null,
                                isDark = LocalManagerDarkMode.current,
                                showLabels = appearance.navigationContent == ManagerNavigationContent.ICON_AND_TEXT,
                                itemModifier = { index -> Modifier.testTag(primaryNavigationTag(index)) },
                            )
                        appearance.floatingNavigation -> {
                            val floatingPillRadius = 50.dp
                            val shape = RoundedCornerShape(floatingPillRadius)
                            val isDark = LocalManagerDarkMode.current
                            val highlight = remember(isDark) {
                                if (isDark) Highlight.GlassStrokeMiddleDark
                                else Highlight.GlassStrokeMiddleLight
                            }
                            val modifier = if (backdrop == null) Modifier else Modifier.textureBlur(
                                backdrop = backdrop,
                                shape = shape,
                                blurRadius = 25f,
                                colors = BlurDefaults.blurColors(
                                    blendColors = listOf(BlendColorEntry(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f))),
                                ),
                                highlight = highlight,
                            )
                            FloatingNavigationBar(
                                modifier = modifier,
                                color = if (backdrop == null) MiuixTheme.colorScheme.surfaceContainer else Color.Transparent,
                                cornerRadius = floatingPillRadius,
                            ) {
                                destinations.forEachIndexed { index, item ->
                                    ManagerFloatingItem(
                                        item = item,
                                        selected = navigation.selectedPage == index,
                                        showLabel = appearance.navigationContent == ManagerNavigationContent.ICON_AND_TEXT,
                                        testTag = primaryNavigationTag(index),
                                        onClick = { navigation.navigateTo(index) },
                                    )
                                }
                            }
                        }
                        else -> ManagerBlurredBar(backdrop) {
                            NavigationBar(
                                color = managerBarColor(backdrop),
                                mode = if (appearance.navigationContent == ManagerNavigationContent.ICON_ONLY) {
                                    NavigationBarDisplayMode.IconOnly
                                } else NavigationBarDisplayMode.IconAndText,
                            ) {
                                destinations.forEachIndexed { index, item ->
                                    NavigationBarItem(
                                        selected = navigation.selectedPage == index,
                                        onClick = { navigation.navigateTo(index) },
                                        icon = item.icon,
                                        label = item.label,
                                        modifier = Modifier.testTag(primaryNavigationTag(index)),
                                    )
                                }
                            }
                        }
                    }
                },
            ) { padding ->
                content(
                    Modifier.fillMaxSize().then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
                    padding.calculateBottomPadding(),
                )
            }
        }
    }
}

@Composable
private fun ManagerFloatingItem(item: NavigationItem, selected: Boolean, showLabel: Boolean, testTag: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val base = MiuixTheme.colorScheme.onSurfaceContainer
    val tint = when {
        pressed -> base.copy(alpha = if (selected) .7f else .5f)
        selected -> base
        else -> base.copy(alpha = .6f)
    }
    Column(
        modifier = Modifier.testTag(testTag).defaultMinSize(
            minWidth = if (showLabel) 56.dp else 48.dp,
            minHeight = 48.dp,
        ).selectable(
            selected = selected,
            onClick = onClick,
            role = Role.Tab,
            interactionSource = interaction,
            indication = null,
        ).padding(horizontal = if (showLabel) 8.dp else 6.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(item.icon, if (showLabel) null else item.label, Modifier.size(22.dp), tint)
        if (showLabel) Text(item.label, color = tint, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

internal fun primaryNavigationTag(index: Int): String = "primary-nav-${PrimaryDestination.entries[index].name}"
