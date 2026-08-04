package com.wmods.wppenhacer.ui.miuix

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wmods.wppenhacer.ui.miuix.navigation.ManagerStackNavigator

private const val MANAGER_ROOT_ROUTE = "manager-root"

private val ManagerRouteStackSaver = listSaver<SnapshotStateList<String>, String>(
    save = { it.toList() },
    restore = { restored -> mutableStateListOf<String>().apply { addAll(restored.ifEmpty { listOf(MANAGER_ROOT_ROUTE) }) } },
)

@Composable
internal fun WaEnhancerManagerApp(
    viewModel: ManagerViewModel,
    onPredictiveBackChange: ((Boolean) -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val routeStack = rememberSaveable(saver = ManagerRouteStackSaver) {
        mutableStateListOf(MANAGER_ROOT_ROUTE)
    }

    ManagerTheme(state.appearance) {
        ManagerStackNavigator(routeStack, onExitRoot = { activity?.finish() }) { encoded, push, pop ->
            val navigate: (ManagerRoute) -> Unit = { route ->
                if (route.encode() != routeStack.lastOrNull()) push(route.encode())
            }
            val route = encoded.takeUnless { it == MANAGER_ROOT_ROUTE }?.let(ManagerRoute::decode)
            if (route == null) {
                ManagerMainShell(
                    state = state,
                    onNavigate = navigate,
                    isBackHandlerEnabled = routeStack.size == 1,
                )
            } else {
                ManagerRouteScreen(
                    route = route,
                    state = state,
                    controller = viewModel.controller,
                    onBack = pop,
                    onNavigate = navigate,
                    onPredictiveBackChange = onPredictiveBackChange,
                )
            }
        }
    }
}
