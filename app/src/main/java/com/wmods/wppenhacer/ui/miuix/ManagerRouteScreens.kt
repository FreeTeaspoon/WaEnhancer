package com.wmods.wppenhacer.ui.miuix

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wmods.wppenhacer.R
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.preference.ArrowPreference

@Composable
internal fun ManagerRouteScreen(
    route: ManagerRoute,
    state: ManagerUiState,
    controller: PreferenceController,
    onBack: () -> Unit,
    onNavigate: (ManagerRoute) -> Unit,
    onPredictiveBackChange: ((Boolean) -> Unit)? = null,
) {
    when (route) {
        is ManagerRoute.PreferencePage -> PreferencePageScreen(
            source = route.source,
            state = state,
            controller = controller,
            wide = false,
            bottomPadding = 0.dp,
            onBack = onBack,
            onNavigate = onNavigate,
            highlightKey = route.highlightKey,
        )
        ManagerRoute.Search -> ManagerSearchScreen(state, onBack, onNavigate)
        ManagerRoute.Appearance -> ManagerAppearanceScreen(
            appearance = state.appearance,
            controller = controller,
            onPredictiveBackChange = onPredictiveBackChange,
            onBack = onBack,
        )
        ManagerRoute.About -> ManagerAboutScreen(onBack)
        ManagerRoute.Configuration -> ManagerConfigurationScreen(controller, onBack)
        ManagerRoute.Updates -> ManagerUpdatesScreen(onBack)
        ManagerRoute.Diagnostics -> ManagerDiagnosticsScreen(onBack)
        ManagerRoute.Recordings -> ManagerRecordingsScreen(onBack)
        ManagerRoute.CallRecording -> ManagerCallRecordingScreen(state, controller, onBack)
        ManagerRoute.ThemeManager -> ManagerThemeManagerScreen(onBack, onNavigate)
        is ManagerRoute.ThemeEditor -> ManagerThemeEditorScreen(route.folder, onBack)
    }
}

@Composable
private fun ManagerSearchScreen(
    state: ManagerUiState,
    onBack: () -> Unit,
    onNavigate: (ManagerRoute) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = searchPreferenceSpecs(state.specs, query)
    val resultsTitle = stringResource(R.string.manager_search_results)
    ManagerDetailScaffold(stringResource(R.string.manager_search), wide = false, onBack = onBack) {
        item("query") {
            ManagerGroupCard {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.manager_search),
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                )
            }
        }
        if (query.isNotBlank()) {
            managerSection(resultsTitle, "results")
            if (results.isEmpty()) item("empty") {
                ManagerGroupCard { ArrowPreference(stringResource(R.string.manager_no_results), onClick = null) }
            } else item("results") {
                ManagerGroupCard {
                    results.forEach { spec ->
                        ArrowPreference(
                            title = spec.title,
                            summary = "${preferenceSourceLabel(spec.source)} · ${spec.summary.orEmpty()}",
                            onClick = { onNavigate(ManagerRoute.PreferencePage(spec.source, spec.key)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun preferenceSourceLabel(source: PreferenceSource): String = when (source) {
    PreferenceSource.GENERAL -> stringResource(R.string.general)
    PreferenceSource.HOME_SCREEN -> stringResource(R.string.home_screen)
    PreferenceSource.CONVERSATION -> stringResource(R.string.conversation)
    PreferenceSource.STATUS -> stringResource(R.string.status)
    PreferenceSource.PRIVACY -> stringResource(R.string.privacy)
    PreferenceSource.MEDIA -> stringResource(R.string.media)
    PreferenceSource.CUSTOMIZE -> stringResource(R.string.manager_customize)
}
