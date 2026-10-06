package com.freeteaspoon.wppenhacer.ui.miuix

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.freeteaspoon.wppenhacer.R
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.preference.ArrowPreference

@Composable
internal fun ManagerSearchContent(
    state: ManagerUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onNavigate: (ManagerRoute) -> Unit,
    title: String,
    wide: Boolean,
    onBack: (() -> Unit)?,
    bottomPadding: Dp = 0.dp,
    showCategories: Boolean = false,
) {
    val results = searchPreferenceSpecs(state.specs, query)
    ManagerDetailScaffold(
        title, wide, onBack,
        bottomPadding = bottomPadding,
        pageState = if (query.isNotBlank() && results.isEmpty()) ManagerPageStateKind.EMPTY else ManagerPageStateKind.CONTENT,
        stateTitle = stringResource(R.string.manager_no_results),
        stateMessage = stringResource(R.string.manager_no_results_hint),
        fixedContent = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.testTag("features-search").padding(horizontal = 12.dp).padding(top = 12.dp, bottom = 6.dp).fillMaxWidth(),
                label = stringResource(R.string.manager_search_features),
                useLabelAsPlaceholder = true,
                singleLine = true,
                leadingIcon = { Icon(MiuixIcons.Search, null, Modifier.padding(horizontal = 12.dp)) },
            )
        },
    ) {
        if (query.isNotBlank()) {
            managerGroupedCardItems("search-results", results.map { spec ->
                ManagerCardItem("${spec.source}:${spec.key}") {
                    ArrowPreference(spec.title, summary = listOf(preferenceSourceLabel(spec.source), spec.summary.orEmpty())
                        .filter(String::isNotBlank).joinToString(", "),
                        onClick = { onNavigate(preferenceRoute(state.specs, spec)) })
                }
            })
        } else if (showCategories) {
            managerGroupedCardItems("feature-categories", PreferenceSource.entries.map { source ->
                ManagerCardItem(source.name) {
                    ArrowPreference(preferenceSourceLabel(source), summary = stringResource(when (source) {
                        PreferenceSource.GENERAL -> R.string.manager_general_summary
                        PreferenceSource.HOME_SCREEN -> R.string.manager_home_screen_summary
                        PreferenceSource.CONVERSATION -> R.string.manager_conversation_summary
                        PreferenceSource.STATUS -> R.string.manager_status_summary
                        PreferenceSource.PRIVACY -> R.string.manager_privacy_summary
                        PreferenceSource.MEDIA -> R.string.manager_media_summary
                        PreferenceSource.CUSTOMIZE -> R.string.manager_customization_summary
                    }), onClick = { onNavigate(ManagerRoute.PreferencePage(source)) })
                }
            })
        }
    }
}
