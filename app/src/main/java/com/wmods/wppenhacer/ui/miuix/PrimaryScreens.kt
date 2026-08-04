package com.wmods.wppenhacer.ui.miuix

import android.annotation.SuppressLint
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.wmods.wppenhacer.BuildConfig
import com.wmods.wppenhacer.R
import com.wmods.wppenhacer.activities.MainActivity
import com.wmods.wppenhacer.App
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.CallRecording
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.icon.extended.RecordingTape
import top.yukonga.miuix.kmp.icon.extended.SearchDevice
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.WorldClock

private data class DestinationRow(
    val title: String,
    val summary: String,
    val route: ManagerRoute,
    val icon: ImageVector,
)

@Composable
internal fun ManagerPrimaryDestinationScreen(
    destination: PrimaryDestination,
    state: ManagerUiState,
    wide: Boolean,
    bottomPadding: Dp,
    onNavigate: (ManagerRoute) -> Unit,
    onSelectPrimary: (PrimaryDestination) -> Unit,
) {
    when (destination) {
        PrimaryDestination.HOME -> ManagerHomeScreen(
            state,
            wide,
            bottomPadding,
            onOpenFeatures = { onSelectPrimary(PrimaryDestination.FEATURES) },
        )
        PrimaryDestination.FEATURES -> ManagerFeaturesScreen(state, wide, bottomPadding, onNavigate)
        PrimaryDestination.TOOLS -> ManagerToolsScreen(wide, bottomPadding, onNavigate)
    }
}

@SuppressLint("BatteryLife")
@Composable
private fun ManagerHomeScreen(
    state: ManagerUiState,
    wide: Boolean,
    bottomPadding: Dp,
    onOpenFeatures: () -> Unit,
) {
    val context = LocalContext.current
    var runningVersions by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                val packageName = intent.getStringExtra("PKG") ?: return
                runningVersions = runningVersions + (packageName to intent.getStringExtra("VERSION").orEmpty())
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(BuildConfig.APPLICATION_ID + ".RECEIVER_WPP"),
            ContextCompat.RECEIVER_EXPORTED,
        )
        context.sendBroadcast(Intent(BuildConfig.APPLICATION_ID + ".CHECK_WPP"))
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    val featureSwitches = state.specs.filter { it.kind == PreferenceKind.SWITCH }
    val enabledFeatures = featureSwitches.count { state.preferences[it.key] == true }
    ManagerDetailScaffold(title = stringResource(R.string.app_name), wide = wide, onBack = null) {
        item("wekit-dashboard") {
            WeKitStyleHomeDashboard(
                moduleActive = MainActivity.isXposedEnabled(),
                enabledCount = enabledFeatures,
                totalCount = featureSwitches.size,
                whatsappVersion = runningVersions["com.whatsapp"],
                businessVersion = runningVersions["com.whatsapp.w4b"],
                onOpenFeatures = onOpenFeatures,
            )
        }
        item("home-bottom-${state.specs.size}") { androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(bottomPadding)) }
    }
}

@Composable
private fun ManagerFeaturesScreen(
    state: ManagerUiState,
    wide: Boolean,
    bottomPadding: Dp,
    onNavigate: (ManagerRoute) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val rows = listOf(
        DestinationRow(stringResource(R.string.general), stringResource(R.string.manager_general_summary), ManagerRoute.PreferencePage(PreferenceSource.GENERAL), MiuixIcons.Settings),
        DestinationRow(stringResource(R.string.home_screen), stringResource(R.string.manager_home_screen_summary), ManagerRoute.PreferencePage(PreferenceSource.HOME_SCREEN), MiuixIcons.Home),
        DestinationRow(stringResource(R.string.conversation), stringResource(R.string.manager_conversation_summary), ManagerRoute.PreferencePage(PreferenceSource.CONVERSATION), MiuixIcons.Messages),
        DestinationRow(stringResource(R.string.status), stringResource(R.string.manager_status_summary), ManagerRoute.PreferencePage(PreferenceSource.STATUS), MiuixIcons.WorldClock),
        DestinationRow(stringResource(R.string.privacy), stringResource(R.string.manager_privacy_summary), ManagerRoute.PreferencePage(PreferenceSource.PRIVACY), MiuixIcons.Contacts),
        DestinationRow(stringResource(R.string.media), stringResource(R.string.manager_media_summary), ManagerRoute.PreferencePage(PreferenceSource.MEDIA), MiuixIcons.RecordingTape),
        DestinationRow(stringResource(R.string.manager_customize), stringResource(R.string.manager_customization_summary), ManagerRoute.PreferencePage(PreferenceSource.CUSTOMIZE), MiuixIcons.Theme),
    )
    val filteredRows = rows.filter { query.isBlank() || it.title.contains(query, true) || it.summary.contains(query, true) }
    val filteredPreferences = searchPreferenceSpecs(state.specs, query)
    val resultsTitle = stringResource(R.string.manager_search_results)
    val sourceLabels = mapOf(
        PreferenceSource.GENERAL to stringResource(R.string.general),
        PreferenceSource.HOME_SCREEN to stringResource(R.string.home_screen),
        PreferenceSource.CONVERSATION to stringResource(R.string.conversation),
        PreferenceSource.STATUS to stringResource(R.string.status),
        PreferenceSource.PRIVACY to stringResource(R.string.privacy),
        PreferenceSource.MEDIA to stringResource(R.string.media),
        PreferenceSource.CUSTOMIZE to stringResource(R.string.manager_customize),
    )
    BackHandler(enabled = query.isNotBlank()) { query = "" }
    ManagerDetailScaffold(title = stringResource(R.string.manager_features), wide = wide, onBack = null) {
        item("feature-search") {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .testTag("features-search")
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                label = stringResource(R.string.manager_search_features),
                leadingIcon = {
                    Icon(
                        imageVector = MiuixIcons.Search,
                        contentDescription = null,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                },
            )
        }
        if (query.isNotBlank()) {
            managerSection(resultsTitle, "feature-results")
            if (filteredPreferences.isEmpty()) {
                item("feature-empty") {
                    ManagerGroupCard {
                        ArrowPreference(stringResource(R.string.manager_no_results), onClick = null)
                    }
                }
            } else {
                managerGroupedCardItems(
                    keyPrefix = "feature-results",
                    items = filteredPreferences.map { spec ->
                        ManagerCardItem("${spec.source.name}:${spec.key}") {
                            ArrowPreference(
                                title = spec.title,
                                summary = listOfNotNull(
                                    sourceLabels[spec.source],
                                    spec.summary?.takeIf(String::isNotBlank),
                                ).joinToString(" · "),
                                onClick = { onNavigate(ManagerRoute.PreferencePage(spec.source, spec.key)) },
                            )
                        }
                    },
                )
            }
        } else if (filteredRows.isEmpty()) {
            item("feature-empty") {
                ManagerGroupCard {
                    ArrowPreference(stringResource(R.string.manager_no_results), onClick = null)
                }
            }
        } else {
            managerGroupedCardItems(
                keyPrefix = "feature-groups",
                items = filteredRows.map { row ->
                    ManagerCardItem(row.route.encode()) {
                        ArrowPreference(
                            title = row.title,
                            summary = row.summary,
                            startAction = { Icon(row.icon, null, modifier = Modifier.padding(end = 6.dp)) },
                            onClick = { onNavigate(row.route) },
                        )
                    }
                },
                outerTopPadding = 12.dp,
            )
        }
        item("feature-bottom") { Spacer(Modifier.height(bottomPadding)) }
    }
}

@SuppressLint("BatteryLife")
@Composable
private fun ManagerToolsScreen(wide: Boolean, bottomPadding: Dp, onNavigate: (ManagerRoute) -> Unit) {
    val context = LocalContext.current
    val powerManager = context.getSystemService(android.os.PowerManager::class.java)
    val batteryIgnored = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    val rows = listOf(
        DestinationRow(stringResource(R.string.manager_recordings), stringResource(R.string.manager_recordings_summary), ManagerRoute.Recordings, MiuixIcons.RecordingTape),
        DestinationRow(stringResource(R.string.call_recording_title), stringResource(R.string.manager_call_recording_summary), ManagerRoute.CallRecording, MiuixIcons.CallRecording),
        DestinationRow(stringResource(R.string.manager_root_diagnostics), stringResource(R.string.manager_diagnostics_summary), ManagerRoute.Diagnostics, MiuixIcons.SearchDevice),
        DestinationRow(stringResource(R.string.manager_appearance), stringResource(R.string.manager_appearance_summary), ManagerRoute.Appearance, MiuixIcons.Theme),
        DestinationRow(stringResource(R.string.about), stringResource(R.string.manager_about_summary), ManagerRoute.About, MiuixIcons.Info),
    )
    ManagerDetailScaffold(title = stringResource(R.string.manager_tools), wide = wide, onBack = null) {
        managerGroupedCardItems(
            keyPrefix = "tools-destinations",
            items = rows.map { row ->
                ManagerCardItem(row.route.encode()) {
                    ArrowPreference(
                        title = row.title,
                        summary = row.summary,
                        startAction = { Icon(row.icon, null, modifier = Modifier.padding(end = 6.dp)) },
                        onClick = { onNavigate(row.route) },
                    )
                }
            },
            outerTopPadding = 12.dp,
        )
        item("app-actions") {
            ManagerGroupCard {
                listOf("com.whatsapp" to R.string.manager_whatsapp, "com.whatsapp.w4b" to R.string.manager_whatsapp_business)
                    .forEach { (packageName, title) ->
                        val installed = runCatching { context.packageManager.getPackageInfo(packageName, 0) }.isSuccess
                        ArrowPreference(
                            title = stringResource(title),
                            summary = if (installed) stringResource(R.string.manager_restart_summary)
                            else stringResource(R.string.manager_not_installed),
                            enabled = installed,
                            onClick = { (context.applicationContext as? App)?.restartApp(packageName) },
                        )
                    }
            }
        }
        item("configuration") {
            ManagerGroupCard {
                ArrowPreference(
                    title = stringResource(R.string.manager_configuration),
                    summary = stringResource(R.string.manager_configuration_summary),
                    onClick = { onNavigate(ManagerRoute.Configuration) },
                )
            }
        }
        item("maintenance") {
            ManagerGroupCard {
                ArrowPreference(
                    title = stringResource(R.string.manager_check_updates),
                    onClick = { onNavigate(ManagerRoute.Updates) },
                )
                SwitchPreference(
                    checked = batteryIgnored,
                    onCheckedChange = {
                        if (!it) return@SwitchPreference
                        context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = "package:${context.packageName}".toUri()
                        })
                    },
                    title = stringResource(R.string.manager_battery_optimization),
                    summary = stringResource(R.string.manager_battery_optimization_summary),
                )
            }
        }
        item("bottom") { androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(bottomPadding)) }
    }
}
