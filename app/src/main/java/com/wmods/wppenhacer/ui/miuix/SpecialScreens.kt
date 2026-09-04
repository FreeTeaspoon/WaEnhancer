package com.wmods.wppenhacer.ui.miuix

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.media.MediaMetadataRetriever
import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.viewinterop.AndroidView
import top.yukonga.miuix.kmp.basic.TextField
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.wmods.wppenhacer.App
import com.wmods.wppenhacer.BuildConfig
import com.wmods.wppenhacer.R
import com.wmods.wppenhacer.preference.ThemePreference
import com.wmods.wppenhacer.utils.PreferenceSnapshot
import com.wmods.wppenhacer.utils.PreferenceSnapshotCodec
import com.wmods.wppenhacer.utils.RootDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import java.io.File
import java.util.zip.ZipInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Composable
internal fun ManagerAppearanceScreen(
    appearance: ManagerAppearanceSettings,
    controller: PreferenceController,
    onPredictiveBackChange: ((Boolean) -> Unit)?,
    onBack: () -> Unit,
) {
    var densityDraft by remember(appearance.interfaceScale) {
        mutableStateOf(appearance.interfaceScale * 100f)
    }
    val densityTextState = rememberTextFieldState()
    var showDensityDialog by remember { mutableStateOf(false) }
    val themeModes = ManagerThemeMode.entries
    val themeItems = listOf(
        stringResource(R.string.manager_theme_follow_system),
        stringResource(R.string.manager_theme_dark),
        stringResource(R.string.manager_theme_light),
    )
    val palettes = ManagerPaletteStyle.entries
    val paletteItems = palettes.map { it.name.readable() }
    val accents = ManagerAccent.entries.filterNot { it == ManagerAccent.CYAN }
    val accentItems = accents.map {
        if (it == ManagerAccent.SYSTEM) stringResource(R.string.manager_system_colour) else it.name.readable()
    }
    val floatingStyles = ManagerFloatingStyle.entries
    val floatingItems = listOf(
        stringResource(R.string.manager_floating_miuix),
        stringResource(R.string.manager_floating_liquid),
    )
    val navigationModes = ManagerNavigationContent.entries
    val navigationItems = listOf(
        stringResource(R.string.manager_icons_and_text),
        stringResource(R.string.manager_icons_only),
    )
    val monetSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val blurSupported = isRenderEffectSupported() && isRuntimeShaderSupported()
    val themeSection = stringResource(R.string.manager_section_theme)
    val languageSection = stringResource(R.string.manager_section_language)
    val effectsSection = stringResource(R.string.manager_section_effects)
    val navigationSection = stringResource(R.string.manager_section_navigation)

    ManagerDetailScaffold(stringResource(R.string.manager_appearance), false, onBack) {
        managerSection(themeSection, "theme")
        managerGroupedCardItems(
            keyPrefix = "appearance-theme",
            items = listOf(
                ManagerCardItem("mode") {
                    OverlayDropdownPreference(
                        title = stringResource(R.string.manager_theme_mode),
                        items = themeItems,
                        selectedIndex = appearance.themeMode.ordinal,
                        onSelectedIndexChange = { index ->
                            controller.putManagerString(
                                ManagerAppearanceSettings.KEY_THEME_MODE,
                                when (themeModes[index]) {
                                    ManagerThemeMode.DARK -> "1"
                                    ManagerThemeMode.LIGHT -> "2"
                                    ManagerThemeMode.SYSTEM -> "0"
                                },
                            )
                        },
                    )
                },
                ManagerCardItem("monet") {
                    SwitchPreference(
                        checked = appearance.useMonet,
                        onCheckedChange = {
                            controller.putManagerString(
                                ManagerAppearanceSettings.KEY_COLOR_MODE,
                                if (it) "monet" else "preset",
                            )
                        },
                        title = stringResource(R.string.manager_monet),
                        summary = stringResource(R.string.manager_monet_summary),
                        enabled = monetSupported,
                    )
                    AnimatedVisibility(
                        visible = appearance.useMonet,
                        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                    ) {
                        Column {
                            OverlayDropdownPreference(
                                title = stringResource(R.string.manager_palette_style),
                                items = paletteItems,
                                selectedIndex = palettes.indexOf(appearance.paletteStyle).coerceAtLeast(0),
                                onSelectedIndexChange = {
                                    controller.putManagerString(ManagerAppearanceSettings.KEY_PALETTE, palettes[it].name)
                                },
                            )
                            OverlayDropdownPreference(
                                title = stringResource(R.string.manager_accent_colour),
                                items = accentItems,
                                selectedIndex = accents.indexOf(appearance.accent).coerceAtLeast(0),
                                onSelectedIndexChange = {
                                    controller.putManagerString(
                                        ManagerAppearanceSettings.KEY_COLOR_PRESET,
                                        accents[it].name.lowercase(),
                                    )
                                },
                            )
                            SwitchPreference(
                                checked = appearance.pureBlack,
                                onCheckedChange = {
                                    controller.putManagerBoolean(ManagerAppearanceSettings.KEY_PURE_BLACK, it)
                                },
                                title = stringResource(R.string.manager_pure_black),
                                summary = stringResource(R.string.manager_pure_black_summary),
                            )
                        }
                    }
                },
            ),
        )

        managerSection(languageSection, "language")
        managerGroupedCardItems(
            keyPrefix = "appearance-language",
            items = listOf(
                ManagerCardItem("english") {
                    SwitchPreference(
                        checked = appearance.forceEnglish,
                        onCheckedChange = {
                            controller.putManagerBoolean(
                                ManagerAppearanceSettings.KEY_FORCE_ENGLISH,
                                it,
                                restart = true,
                            )
                        },
                        title = stringResource(R.string.manager_force_english),
                        summary = stringResource(R.string.manager_force_english_summary),
                    )
                },
            ),
        )

        managerSection(effectsSection, "effects")
        managerGroupedCardItems(
            keyPrefix = "appearance-interface",
            items = buildList {
                add(ManagerCardItem("blur") {
                    SwitchPreference(
                        checked = appearance.blurEnabled && blurSupported,
                        onCheckedChange = {
                            controller.putManagerBoolean(ManagerAppearanceSettings.KEY_BLUR, it)
                        },
                        title = stringResource(R.string.manager_blur),
                        summary = stringResource(R.string.manager_blur_summary),
                        enabled = blurSupported,
                    )
                })
                if (onPredictiveBackChange != null) add(ManagerCardItem("predictive-back") {
                    SwitchPreference(
                        checked = appearance.predictiveBack,
                        onCheckedChange = {
                            controller.putManagerBoolean(ManagerAppearanceSettings.KEY_PREDICTIVE_BACK, it)
                            onPredictiveBackChange(it)
                        },
                        title = stringResource(R.string.manager_predictive_back),
                        summary = stringResource(R.string.manager_predictive_back_summary),
                    )
                })
                add(ManagerCardItem("scale") {
                    ArrowPreference(
                        title = stringResource(R.string.manager_interface_scale),
                        summary = stringResource(R.string.manager_interface_scale_summary),
                        endActions = {
                            Text(
                                text = "${densityDraft.toInt()}%",
                                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                fontSize = MiuixTheme.textStyles.body2.fontSize,
                            )
                        },
                        bottomAction = {
                            Slider(
                                value = densityDraft,
                                onValueChange = { densityDraft = it },
                                onValueChangeFinished = {
                                    controller.putManagerFloat(ManagerAppearanceSettings.KEY_SCALE, densityDraft / 100f)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                valueRange = 80f..110f,
                                showKeyPoints = true,
                                keyPoints = listOf(80f, 90f, 100f, 110f),
                                magnetThreshold = .01f,
                                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                            )
                        },
                        onClick = {
                            densityTextState.setTextAndPlaceCursorAtEnd(densityDraft.toInt().toString())
                            showDensityDialog = true
                        },
                        holdDownState = showDensityDialog,
                    )
                })
            },
        )

        managerSection(navigationSection, "navigation")
        managerGroupedCardItems(
            keyPrefix = "appearance-navigation",
            items = listOf(
                ManagerCardItem("floating") {
                    SwitchPreference(
                        checked = appearance.floatingNavigation,
                        onCheckedChange = {
                            controller.putManagerBoolean(ManagerAppearanceSettings.KEY_FLOATING_NAVIGATION, it)
                        },
                        title = stringResource(R.string.manager_floating_navigation),
                        summary = stringResource(R.string.manager_floating_navigation_summary),
                    )
                    AnimatedVisibility(
                        visible = appearance.floatingNavigation,
                        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                    ) {
                        OverlayDropdownPreference(
                            title = stringResource(R.string.manager_floating_style),
                            items = floatingItems,
                            selectedIndex = floatingStyles.indexOf(appearance.floatingStyle).coerceAtLeast(0),
                            onSelectedIndexChange = {
                                controller.putManagerString(
                                    ManagerAppearanceSettings.KEY_FLOATING_STYLE,
                                    floatingStyles[it].name,
                                )
                            },
                        )
                    }
                },
                ManagerCardItem("mode") {
                    OverlayDropdownPreference(
                        title = stringResource(R.string.manager_navigation_content),
                        items = navigationItems,
                        selectedIndex = navigationModes.indexOf(appearance.navigationContent).coerceAtLeast(0),
                        onSelectedIndexChange = {
                            controller.putManagerString(
                                ManagerAppearanceSettings.KEY_NAVIGATION_CONTENT,
                                navigationModes[it].name,
                            )
                        },
                    )
                },
            ),
        )
        item("appearance-bottom-space") { Spacer(Modifier.height(24.dp)) }
    }

    WindowDialog(
        show = showDensityDialog,
        title = stringResource(R.string.manager_interface_scale),
        summary = stringResource(R.string.manager_interface_scale_summary),
        onDismissRequest = { showDensityDialog = false },
    ) {
        TextField(
            state = densityTextState,
            inputTransformation = DensityDigitsOnlyTransformation.maxLength(3),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            lineLimits = TextFieldLineLimits.SingleLine,
            trailingIcon = { Text("%", modifier = Modifier.padding(horizontal = 16.dp)) },
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                text = stringResource(android.R.string.cancel),
                modifier = Modifier.weight(1f),
                onClick = { showDensityDialog = false },
            )
            TextButton(
                text = stringResource(android.R.string.ok),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = {
                    val percent = densityTextState.text.toString().toIntOrNull()
                        ?.coerceIn(80, 110)
                        ?: densityDraft.toInt()
                    controller.putManagerFloat(ManagerAppearanceSettings.KEY_SCALE, percent / 100f)
                    showDensityDialog = false
                },
            )
        }
    }
}

private val DensityDigitsOnlyTransformation = InputTransformation {
    if (!asCharSequence().all { it.isDigit() }) revertAllChanges()
}

private fun String.readable() = lowercase().replace('_', ' ').replaceFirstChar(Char::titlecase)

@Composable
internal fun ManagerAboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val open: (String) -> Unit = { context.startActivity(Intent(Intent.ACTION_VIEW, it.toUri())) }
    val contributors = listOf("Dev4Mod", "frknkrc44", "mubashardev", "masbentoooredoo", "zhongerxll", "BryanGIG", "rizqi-developer", "pedroborraz", "ahmedtohamy1", "mohdafix", "maulana-kurniawan", "erzachn", "cvnertnc", "rkorossy", "StupidRepo", "Blank517", "astola-studio", "Strange-IPmart")
    val projectTitle = stringResource(R.string.manager_project_repository)
    val licenceTitle = stringResource(R.string.manager_licence)
    val contributorsTitle = stringResource(R.string.manager_contributors)
    ManagerDetailScaffold(stringResource(R.string.about), false, onBack) {
        item("version") { ManagerGroupCard { ArrowPreference(stringResource(R.string.app_name), summary = stringResource(R.string.manager_version, BuildConfig.VERSION_NAME), onClick = null) } }
        managerSection(projectTitle, "links")
        item("links") {
            ManagerGroupCard {
                ArrowPreference(projectTitle, onClick = { open(BuildConfig.GITHUB_REPOSITORY_URL) })
                ArrowPreference(stringResource(R.string.manager_support_channel), onClick = { open("https://t.me/waenhancer") })
            }
        }
        managerSection(licenceTitle, "licence")
        item("licence") { ManagerGroupCard {
            ArrowPreference(licenceTitle, summary = stringResource(R.string.manager_licence_summary), onClick = { open("https://www.gnu.org/licenses/gpl-3.0.html") })
            ArrowPreference(stringResource(R.string.manager_dependencies), summary = stringResource(R.string.manager_dependencies_summary), onClick = { open("https://github.com/compose-miuix-ui/miuix") })
        } }
        managerSection(contributorsTitle, "contributors")
        item("contributors") { ManagerGroupCard { contributors.forEach { name -> ArrowPreference(name, onClick = { open("https://github.com/$name") }) } } }
    }
}

@Composable
internal fun ManagerConfigurationScreen(controller: PreferenceController, onBack: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val scope = rememberCoroutineScope()
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            message = runCatching {
                val json = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() } }
                val decoded = PreferenceSnapshotCodec.decode(JSONObject(json))
                controller.replaceAll(decoded)
                resources.getString(R.string.manager_settings_imported)
            }.getOrElse { resources.getString(R.string.manager_operation_failed, it.message.orEmpty()) }
        }
    }
    val exportFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            message = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)!!.bufferedWriter().use { it.write(PreferenceSnapshotCodec.encode(prefs.all).toString(2)) }
                }
                resources.getString(R.string.manager_settings_exported)
            }.getOrElse { resources.getString(R.string.manager_operation_failed, it.message.orEmpty()) }
        }
    }
    val section = stringResource(R.string.manager_configuration)
    ManagerDetailScaffold(section, false, onBack) {
        managerSection(section, "configuration")
        item("actions") { ManagerGroupCard {
            ArrowPreference(stringResource(R.string.import_settings), summary = stringResource(R.string.manager_import_summary), onClick = { importFile.launch(arrayOf("application/json", "text/json")) })
            ArrowPreference(stringResource(R.string.export_settings), summary = stringResource(R.string.manager_export_summary), onClick = { exportFile.launch("WaEnhancer-settings.json") })
            ArrowPreference(stringResource(R.string.reset_settings), summary = stringResource(R.string.manager_reset_summary), onClick = { confirmReset = true })
        } }
        message?.let { text -> item("message") { ManagerGroupCard { ArrowPreference(text, onClick = null) } } }
    }
    WindowDialog(show = confirmReset, title = stringResource(R.string.reset_settings), summary = stringResource(R.string.manager_reset_confirm), onDismissRequest = { confirmReset = false }) {
        DialogActionButtons(onDismiss = { confirmReset = false }) {
            controller.replaceAll(emptyMap())
            confirmReset = false
        }
    }
}

@Composable
internal fun ManagerUpdatesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    var state by remember { mutableStateOf(resources.getString(R.string.manager_loading)) }
    var updateUrl by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        runCatching {
            withContext(Dispatchers.IO) {
                OkHttpClient().newCall(Request.Builder().url(BuildConfig.LATEST_RELEASE_API).build()).execute().use { response ->
                    check(response.isSuccessful)
                    JSONObject(response.body.string())
                }
            }
        }.onSuccess { release ->
            state = "${release.optString("tag_name")}\n${release.optString("body")}"
            updateUrl = release.optString("html_url").takeIf(String::isNotBlank)
        }.onFailure { state = resources.getString(R.string.manager_operation_failed, it.message.orEmpty()) }
    }
    ManagerDetailScaffold(stringResource(R.string.manager_check_updates), false, onBack) {
        item("update") { ManagerGroupCard { ArrowPreference(stringResource(R.string.manager_check_updates), summary = state, onClick = updateUrl?.let { url -> ({ context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }) }) } }
    }
}

@Composable
internal fun ManagerDiagnosticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val logs = remember { mutableStateListOf<RootDiagnostics.LogEntry>() }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            RootDiagnostics.runDiagnostics(context, object : RootDiagnostics.Callback {
                override fun onLog(entry: RootDiagnostics.LogEntry) { logs += entry }
            })
        }
    }
    ManagerDetailScaffold(stringResource(R.string.manager_root_diagnostics), false, onBack) {
        item("logs") { ManagerGroupCard {
            if (logs.isEmpty()) ArrowPreference(stringResource(R.string.manager_root_checking), onClick = null)
            logs.forEachIndexed { index, log -> ArrowPreference(log.message.ifBlank { " " }, summary = log.type.name.readable(), onClick = null) }
        } }
    }
}

@Composable
internal fun ManagerCallRecordingScreen(state: ManagerUiState, controller: PreferenceController, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    val useRoot = state.preferences["call_recording_use_root"] as? Boolean ?: false
    val spec = remember { PreferenceSpec("call_recording_use_root", PreferenceSource.MEDIA, "", "", null, PreferenceKind.SWITCH) }
    ManagerDetailScaffold(stringResource(R.string.call_recording_title), false, onBack) {
        item("modes") { ManagerGroupCard {
            SwitchPreference(checked = !useRoot, onCheckedChange = { if (it) controller.put(spec, false) }, title = stringResource(R.string.manager_non_root_mode))
            SwitchPreference(checked = useRoot, onCheckedChange = {
                if (it) {
                    checking = true
                    scope.launch(Dispatchers.IO) {
                        val root = com.topjohnwu.superuser.Shell.getShell().isRoot
                        withContext(Dispatchers.Main) { controller.put(spec, root); checking = false }
                    }
                }
            }, title = stringResource(R.string.manager_root_mode), summary = if (checking) stringResource(R.string.manager_root_checking) else null)
        } }
    }
}

@Composable
internal fun ManagerRecordingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val scope = rememberCoroutineScope()
    var recordings by remember { mutableStateOf<List<ManagerRecording>?>(null) }
    var grouped by rememberSaveable { mutableStateOf(false) }
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedPaths by rememberSaveable { mutableStateOf(setOf<String>()) }
    var sort by rememberSaveable { mutableStateOf(RecordingSort.DATE) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        recordings = withContext(Dispatchers.IO) { scanRecordings(prefs.getString("call_recording_path", null)) }
    }
    val visible = recordings?.let { list -> when (sort) {
        RecordingSort.DATE -> list.sortedByDescending { it.date }
        RecordingSort.NAME -> list.sortedBy { it.file.name.lowercase() }
        RecordingSort.DURATION -> list.sortedByDescending { it.duration }
        RecordingSort.CONTACT -> list.sortedWith(compareBy<ManagerRecording> { it.contactName.lowercase() }.thenByDescending { it.date })
    } }
    val section = stringResource(R.string.manager_recordings)
    ManagerDetailScaffold(section, false, onBack) {
        item("mode") { ManagerGroupCard {
            SwitchPreference(grouped, { grouped = it }, stringResource(R.string.manager_group_by_contact))
            SwitchPreference(selectionMode, { enabled -> selectionMode = enabled; if (!enabled) selectedPaths = emptySet() }, stringResource(R.string.manager_select_recordings))
            OverlayDropdownPreference(
                title = stringResource(R.string.manager_sort),
                items = RecordingSort.entries.map { it.name.readable() },
                selectedIndex = sort.ordinal,
                onSelectedIndexChange = { index -> sort = RecordingSort.entries[index] },
            )
            if (selectionMode) {
                ArrowPreference(stringResource(R.string.manager_select_all), summary = resources.getString(R.string.manager_selected_count, selectedPaths.size), onClick = { selectedPaths = visible.orEmpty().map { it.file.absolutePath }.toSet() })
                ArrowPreference(stringResource(R.string.share), enabled = selectedPaths.isNotEmpty(), onClick = {
                    val uris = ArrayList(selectedPaths.map { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(it)) })
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND_MULTIPLE).setType("audio/*").putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), resources.getString(R.string.share)))
                })
                ArrowPreference(stringResource(R.string.delete), enabled = selectedPaths.isNotEmpty(), onClick = { confirmDelete = true })
            }
        } }
        when {
            recordings == null -> item("loading") { ManagerGroupCard { ArrowPreference(stringResource(R.string.manager_loading), onClick = null) } }
            recordings!!.isEmpty() -> item("empty") { ManagerGroupCard { ArrowPreference(stringResource(R.string.manager_empty), onClick = null) } }
            grouped -> visible!!.groupBy { it.contactName }.toSortedMap().forEach { (contact, items) ->
                item("contact-$contact") { ManagerGroupCard { ArrowPreference(contact, summary = resources.getQuantityString(R.plurals.manager_recording_count, items.size, items.size), onClick = null) } }
            }
            else -> item("recordings") { ManagerGroupCard { visible!!.forEach { recording ->
                if (selectionMode) {
                    SwitchPreference(
                        checked = recording.file.absolutePath in selectedPaths,
                        onCheckedChange = { checked -> selectedPaths = if (checked) selectedPaths + recording.file.absolutePath else selectedPaths - recording.file.absolutePath },
                        title = recording.contactName,
                        summary = "${recording.formattedDuration} · ${recording.formattedSize}",
                    )
                } else ArrowPreference(
                        title = recording.contactName,
                        summary = "${recording.formattedDuration} · ${recording.formattedSize}",
                        onClick = {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", recording.file)
                            context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "audio/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                        },
                    )
            } } }
        }
    }
    WindowDialog(show = confirmDelete, title = stringResource(R.string.delete), summary = stringResource(R.string.manager_delete_recordings_confirm, selectedPaths.size), onDismissRequest = { confirmDelete = false }) {
        DialogActionButtons({ confirmDelete = false }) {
            val deleting = selectedPaths
            confirmDelete = false
            scope.launch {
                withContext(Dispatchers.IO) { deleting.forEach { File(it).delete() } }
                recordings = recordings.orEmpty().filterNot { it.file.absolutePath in deleting }
                selectedPaths = emptySet()
                selectionMode = false
            }
        }
    }
}

private enum class RecordingSort { DATE, NAME, DURATION, CONTACT }

internal data class ManagerRecording(
    val file: File,
    val contactName: String,
    val duration: Long,
    val date: Long,
    val size: Long,
) {
    val formattedDuration: String get() {
        val totalSeconds = duration / 1000
        return if (totalSeconds >= 3600) "%d:%02d:%02d".format(totalSeconds / 3600, totalSeconds / 60 % 60, totalSeconds % 60)
        else "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }
    val formattedSize: String get() = when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "%.1f KB".format(size / 1024.0)
        else -> "%.1f MB".format(size / (1024.0 * 1024.0))
    }
}

private val recordingNamePattern = Regex("Call_([+\\w\\s]+)_\\d{8}_\\d{6}\\.(wav|m4a|mp3|aac)", RegexOption.IGNORE_CASE)

private fun scanRecordings(configuredPath: String?): List<ManagerRecording> {
    val roots = linkedSetOf(
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "WA Call Recordings"),
        File(Environment.getExternalStorageDirectory(), "WA Call Recordings"),
        File(Environment.getExternalStorageDirectory(), "Android/data/com.whatsapp/files/Recordings"),
        File(Environment.getExternalStorageDirectory(), "Android/data/com.whatsapp.w4b/files/Recordings"),
        File(Environment.getExternalStorageDirectory(), "Music/WaEnhancer/Recordings"),
    )
    configuredPath?.takeIf(String::isNotBlank)?.let { roots += File(it, "WA Call Recordings") }
    return roots.flatMap { root -> root.walkTopDown().filter { it.isFile && it.extension.lowercase() in setOf("wav", "mp3", "aac", "m4a") }.toList() }
        .distinctBy { runCatching { it.canonicalPath }.getOrDefault(it.absolutePath) }
        .map { file ->
            val contact = recordingNamePattern.matchEntire(file.name)?.groupValues?.getOrNull(1)?.takeIf(String::isNotBlank) ?: "Unknown"
            val duration = runCatching {
                MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(file.absolutePath)
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                }
            }.getOrDefault(0L)
            ManagerRecording(file, contact, duration, file.lastModified(), file.length())
        }.sortedByDescending { it.date }
}

@Composable
internal fun ManagerThemeManagerScreen(onBack: () -> Unit, onNavigate: (ManagerRoute) -> Unit) {
    val context = LocalContext.current
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    var themes by remember { mutableStateOf<List<File>>(emptyList()) }
    var importing by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var themeName by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    fun refresh() {
        scope.launch {
            themes = withContext(Dispatchers.IO) {
                ThemePreference.rootDirectory.mkdirs()
                ThemePreference.rootDirectory.listFiles(File::isDirectory)
                    ?.filter { File(it, "style.css").exists() }
                    .orEmpty()
                    .sortedBy(File::getName)
            }
        }
    }
    LaunchedEffect(Unit) { refresh() }
    val importTheme = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            importing = true
            withContext(Dispatchers.IO) { importThemeArchive(context, uri) }
            refresh(); importing = false
        }
    }
    val title = stringResource(R.string.theme_manager)
    ManagerDetailScaffold(title, false, onBack) {
        item("actions") { ManagerGroupCard {
            ArrowPreference(stringResource(R.string.create_new_theme), onClick = { creating = true })
            ArrowPreference(stringResource(R.string.import_theme), summary = if (importing) stringResource(R.string.manager_loading) else null, onClick = { importTheme.launch(arrayOf("application/zip")) })
        } }
        item("default") { ManagerGroupCard { ArrowPreference(stringResource(R.string.manager_default_theme), onClick = { prefs.edit { putString("folder_theme", null); putString("custom_css", "") } }) } }
        themes.forEach { folder -> item("theme-${folder.name}") { ManagerGroupCard {
            ArrowPreference(folder.name, summary = if (prefs.getString("folder_theme", null) == folder.name) stringResource(R.string.manager_selected) else null, onClick = {
                scope.launch {
                    val css = withContext(Dispatchers.IO) { File(folder, "style.css").readText() }
                    prefs.edit { putString("folder_theme", folder.name); putString("custom_css", css) }
                }
            })
            ArrowPreference(stringResource(R.string.manager_edit_theme, folder.name), onClick = { onNavigate(ManagerRoute.ThemeEditor(folder.name)) })
        } } }
    }
    WindowDialog(show = creating, title = stringResource(R.string.new_theme_name), onDismissRequest = { creating = false }) {
        TextField(themeName, { themeName = it }, Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(12.dp))
        DialogActionButtons({ creating = false }) {
            val safe = themeName.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_")
            if (safe.isNotBlank()) {
                creating = false
                scope.launch {
                    withContext(Dispatchers.IO) {
                        val folder = File(ThemePreference.rootDirectory, safe).apply { mkdirs() }
                        File(folder, "style.css").apply { if (!exists()) createNewFile() }
                    }
                    refresh()
                    onNavigate(ManagerRoute.ThemeEditor(safe))
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun ManagerThemeEditorScreen(folderName: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val folder = remember(folderName) { File(ThemePreference.rootDirectory, folderName) }
    val cssFile = remember(folder) { File(folder, "style.css") }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { exportThemeArchive(context, folder, uri) } }
                .onSuccess { message = resources.getString(R.string.exported) }
                .onFailure { message = resources.getString(R.string.manager_operation_failed, it.message.orEmpty()) }
        }
    }
    ManagerDetailScaffold(folderName, false, onBack, contentMaxWidth = null) {
        item("actions") { ManagerGroupCard {
            ArrowPreference(stringResource(R.string.saved), summary = message, onClick = {
                webView?.evaluateJavascript("getTextareaContent();") { encoded ->
                    val decoded = JSONObject("{\"value\":$encoded}").optString("value")
                    scope.launch {
                        withContext(Dispatchers.IO) { folder.mkdirs(); cssFile.writeText(decoded) }
                        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                        if (prefs.getString("folder_theme", null) == folderName) prefs.edit { putString("custom_css", decoded) }
                        message = resources.getString(R.string.saved)
                    }
                }
            })
            ArrowPreference(stringResource(R.string.clear), onClick = { webView?.evaluateJavascript("document.querySelector('textarea').value = '';", null) })
            ArrowPreference(stringResource(R.string.export_as_zip), onClick = { export.launch("$folderName.zip") })
        } }
        item("editor") {
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(640.dp),
                factory = { activityContext ->
                    WebView(activityContext).apply {
                        settings.javaScriptEnabled = true
                        settings.allowContentAccess = true
                        settings.domStorageEnabled = true
                        webViewClient = WebViewClient()
                        webChromeClient = WebChromeClient()
                        val template = activityContext.assets.open("css_editor.html").bufferedReader().use { it.readText() }
                        val css = runCatching { cssFile.readText() }.getOrDefault("")
                        loadDataWithBaseURL("file:///android_asset/", template.replace("{{content}}", css), "text/html", "UTF-8", null)
                        webView = this
                    }
                },
            )
        }
    }
}

@SuppressLint("Recycle")
private fun exportThemeArchive(context: android.content.Context, folder: File, uri: Uri) {
    context.contentResolver.openOutputStream(uri)?.use { output ->
        ZipOutputStream(output).use { zip ->
            folder.walkTopDown().filter(File::isFile).forEach { file ->
                zip.putNextEntry(ZipEntry("${folder.name}/${file.relativeTo(folder).invariantSeparatorsPath}"))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }
}

@SuppressLint("Recycle")
private fun importThemeArchive(context: android.content.Context, uri: Uri) {
    val targetRoot = ThemePreference.rootDirectory.apply { mkdirs() }
    val fallback = "imported_theme_${System.currentTimeMillis()}"
    context.contentResolver.openInputStream(uri)?.use { input ->
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val safeName = entry.name.replace('\\', '/').trimStart('/').takeIf { !it.split('/').contains("..") }
                if (safeName != null) {
                    val relative = if ('/' in safeName) safeName else "$fallback/$safeName"
                    val output = File(targetRoot, relative)
                    if (entry.isDirectory) output.mkdirs() else { output.parentFile?.mkdirs(); output.outputStream().use(zip::copyTo) }
                }
                zip.closeEntry(); entry = zip.nextEntry
            }
        }
    }
}

@Composable
private fun DialogActionButtons(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(stringResource(android.R.string.cancel), onDismiss, Modifier.weight(1f))
        TextButton(stringResource(android.R.string.ok), onConfirm, Modifier.weight(1f), colors = ButtonDefaults.textButtonColorsPrimary())
    }
}
