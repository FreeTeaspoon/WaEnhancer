package com.wmods.wppenhacer.ui.miuix.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavCornerClipMode
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal sealed interface ManagerStackRoute : NavKey {
    data object Root : ManagerStackRoute
    data class Screen(val encoded: String) : ManagerStackRoute
}

internal const val MANAGER_ROOT_ROUTE = "manager-root"

/**
 * Navigation 3-backed manager stack.
 *
 * The stack is mutated synchronously when a route is requested, so predictive back is registered
 * before the new entry's transition has finished. NavDisplay then owns the enter, pop, cancellation,
 * clipping, dimming, and input-blocking behavior consistently with the other native Miuix apps.
 */
@Composable
internal fun ManagerStackNavigator(
    stack: NavBackStack,
    onExitRoot: () -> Unit,
    content: @Composable (screen: String, push: (String) -> Unit, pop: () -> Unit) -> Unit,
) {
    fun push(screen: String) {
        if ((stack.lastOrNull() as? ManagerStackRoute.Screen)?.encoded != screen) {
            stack.add(ManagerStackRoute.Screen(screen))
        }
    }

    fun pop() {
        if (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
        } else {
            onExitRoot()
        }
    }

    val currentContent = rememberUpdatedState(content)
    val swipeBackDirection = when (LocalLayoutDirection.current) {
        LayoutDirection.Rtl -> NavSwipeDirection.RightToLeft
        else -> NavSwipeDirection.LeftToRight
    }
    val navCornerRadius = rememberNavSystemCornerRadius()

    NavDisplay(
        backStack = stack,
        onBack = ::pop,
        transition = NavTransitions.MiuixDefault,
        effects = NavDisplayEffects(
            enableCornerClip = true,
            cornerClipRadius = navCornerRadius,
            cornerClipMode = NavCornerClipMode.Leading,
            dimAmount = 0.5f,
            blockInputDuringTransition = false,
            backdropColor = MiuixTheme.colorScheme.surface,
        ),
    ) {
        entry<ManagerStackRoute.Root>(swipeDismiss = swipeBackDirection) {
            currentContent.value(MANAGER_ROOT_ROUTE, ::push, ::pop)
        }
        entry<ManagerStackRoute.Screen>(swipeDismiss = swipeBackDirection) { entry ->
            currentContent.value(entry.encoded, ::push, ::pop)
        }
    }
}
