package com.wmods.wppenhacer.ui.miuix

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import com.wmods.wppenhacer.ui.miuix.navigation.ManagerStackNavigator
import com.wmods.wppenhacer.ui.miuix.navigation.ManagerStackRoute
import com.wmods.wppenhacer.ui.miuix.navigation.MANAGER_ROOT_ROUTE

private val ManagerRouteStackSaver = listSaver<NavBackStack, String>(
    save = { stack ->
        stack.map { route ->
            when (route) {
                ManagerStackRoute.Root -> MANAGER_ROOT_ROUTE
                is ManagerStackRoute.Screen -> route.encoded
                else -> MANAGER_ROOT_ROUTE
            }
        }
    },
    restore = { restored ->
        mutableStateListOf<NavKey>().apply {
            addAll(
                restored.ifEmpty { listOf(MANAGER_ROOT_ROUTE) }.map { encoded ->
                    if (encoded == MANAGER_ROOT_ROUTE) {
                        ManagerStackRoute.Root
                    } else {
                        ManagerStackRoute.Screen(encoded)
                    }
                },
            )
        }
    },
)

@Stable
internal class RootTabBackState {
    val targetPage = mutableStateOf(0)
    var onBack: () -> Unit = {}
}

@Composable
internal fun WaEnhancerManagerApp(
    viewModel: ManagerViewModel,
    snackbarHostState: SnackbarHostState,
    onPredictiveBackChange: ((Boolean) -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val routeStack = rememberSaveable(saver = ManagerRouteStackSaver) {
        mutableStateListOf<NavKey>(ManagerStackRoute.Root)
    }
    val rootTabBack = androidx.compose.runtime.remember { RootTabBackState() }

    ManagerTheme(state.appearance) {
        LaunchedEffect(Unit) {
            ManagerSnackbarEvents.register()
            try {
                ManagerSnackbarEvents.events.collect { message ->
                    snackbarHostState.showSnackbar(message)
                }
            } finally {
                ManagerSnackbarEvents.unregister()
            }
        }
        Scaffold(
            snackbarHost = {
                if (routeStack.lastOrNull() != ManagerStackRoute.Root) {
                    SnackbarHost(state = snackbarHostState)
                }
            },
        ) { _ ->
            RootTabBackHandler(
                rootTabBack = rootTabBack,
                isRootRoute = routeStack.size == 1 && routeStack.lastOrNull() == ManagerStackRoute.Root,
            )
            ManagerStackNavigator(routeStack, onExitRoot = { activity?.finish() }) { encoded, push, pop ->
                val navigate: (ManagerRoute) -> Unit = { route ->
                    if ((routeStack.lastOrNull() as? ManagerStackRoute.Screen)?.encoded != route.encode()) {
                        push(route.encode())
                    }
                }
                val route = encoded.takeUnless { it == MANAGER_ROOT_ROUTE }?.let(ManagerRoute::decode)
                if (route == null) {
                    ManagerMainShell(
                        state = state,
                        onNavigate = navigate,
                        snackbarHostState = snackbarHostState,
                        rootTabBack = rootTabBack,
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
}

@Composable
private fun RootTabBackHandler(
    rootTabBack: RootTabBackState,
    isRootRoute: Boolean,
) {
    val navigationEventState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = isRootRoute && rootTabBack.targetPage.value != 0,
        onBackCompleted = { rootTabBack.onBack() },
    )
}
