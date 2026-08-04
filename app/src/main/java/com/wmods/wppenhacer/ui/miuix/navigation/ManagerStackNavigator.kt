package com.wmods.wppenhacer.ui.miuix.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.NavDisplayTransitionEffects

/**
 * Navigation 3-backed manager stack.
 *
 * The stack is mutated synchronously when a route is requested, so predictive back is registered
 * before the new entry's transition has finished. NavDisplay then owns the enter, pop, cancellation,
 * clipping, dimming, and input-blocking behavior consistently with the other native Miuix apps.
 */
@Composable
internal fun ManagerStackNavigator(
    stack: SnapshotStateList<String>,
    onExitRoot: () -> Unit,
    content: @Composable (screen: String, push: (String) -> Unit, pop: () -> Unit) -> Unit,
) {
    fun push(screen: String) {
        if (stack.lastOrNull() != screen) stack.add(screen)
    }

    fun pop() {
        if (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
        } else {
            onExitRoot()
        }
    }

    val currentContent = rememberUpdatedState(content)
    val entries = remember {
        entryProvider<String>(
            fallback = { screen ->
                NavEntry(screen) { entryScreen ->
                    currentContent.value(entryScreen, ::push, ::pop)
                }
            }
        ) { }
    }

    NavDisplay(
        backStack = stack,
        onBack = ::pop,
        entryProvider = entries,
        transitionEffects = NavDisplayTransitionEffects(
            enableCornerClip = true,
            dimAmount = 0.5f,
            blockInputDuringTransition = true,
            popDirectionFollowsSwipeEdge = false,
        ),
    )
}
