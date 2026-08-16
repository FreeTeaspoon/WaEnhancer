package com.wmods.wppenhacer.ui.miuix

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                        isBackHandlerEnabled = routeStack.size == 1,
                        snackbarHostState = snackbarHostState,
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
