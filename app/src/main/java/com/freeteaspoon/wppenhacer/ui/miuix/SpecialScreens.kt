package com.freeteaspoon.wppenhacer.ui.miuix

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.media.MediaMetadataRetriever
import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.viewinterop.AndroidView
import top.yukonga.miuix.kmp.basic.TextField
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.freeteaspoon.wppenhacer.App
import com.freeteaspoon.wppenhacer.BuildConfig
import com.freeteaspoon.wppenhacer.R
import com.freeteaspoon.wppenhacer.preference.ThemePreference
import com.freeteaspoon.wppenhacer.utils.PreferenceSnapshot
import com.freeteaspoon.wppenhacer.utils.PreferenceSnapshotCodec
import com.freeteaspoon.wppenhacer.utils.RootDiagnostics
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
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Ok
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
    val paletteItems = stringArrayResource(R.array.manager_palette_labels).toList()
    val accents = ManagerAccent.entries.filter { it != ManagerAccent.CYAN || appearance.accent == ManagerAccent.CYAN }
    val accentLabels = stringArrayResource(R.array.manager_accent_labels)
    val accentItems = accents.map { accentLabels[it.ordinal] }
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

    ManagerDetailScaffold(stringResource(R.string.manager_appearance), false, onBack, contentMaxWidth = null, fullWidthSafeInsets = true) {
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
    }

    val resources = LocalResources.current
    ManagerTextInputDialog(
        show = showDensityDialog,
        title = stringResource(R.string.manager_interface_scale),
        summary = stringResource(R.string.manager_interface_scale_summary),
        initialValue = densityDraft.toInt().toString(),
        onDismiss = { showDensityDialog = false },
        keyboardType = KeyboardType.Number,
        maxLength = 3,
        inputFilter = { it.filter(Char::isDigit) },
        suffix = "%",
        confirmText = stringResource(R.string.manager_confirm),
        validate = { if (it.toIntOrNull() !in 80..110) resources.getString(R.string.manager_scale_range) else null },
        onConfirm = {
            controller.putManagerFloat(ManagerAppearanceSettings.KEY_SCALE, it.toInt() / 100f)
            showDensityDialog = false
        },
    )
}

@Composable
internal fun ManagerConfigurationScreen(controller: PreferenceController, onBack: () -> Unit) {
    val model: ManagerFileWorkflowViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val busy by model.busy.collectAsStateWithLifecycle()
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { importUri = it }
    val exportFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(model::exportConfiguration)
    }
    ManagerDetailScaffold(stringResource(R.string.manager_configuration), false, onBack) {
        item("tip", contentType = PageStart.Inset) { ManagerTipCard(stringResource(R.string.manager_configuration_tip)) }
        item("actions") { ManagerGroupCard {
            ArrowPreference(stringResource(R.string.manager_import), summary = stringResource(R.string.manager_import_summary), enabled = !busy,
                onClick = { importFile.launch(arrayOf("application/json", "text/json")) })
            ArrowPreference(stringResource(R.string.manager_export), summary = stringResource(R.string.manager_export_summary), enabled = !busy,
                onClick = { exportFile.launch("WaEnhancer-settings.json") })
        } }
        item("reset") { ManagerGroupCard {
            ArrowPreference(stringResource(R.string.manager_reset), summary = stringResource(R.string.manager_reset_summary), enabled = !busy,
                titleColor = top.yukonga.miuix.kmp.basic.BasicComponentDefaults.titleColor(color = MiuixTheme.colorScheme.error),
                onClick = { confirmReset = true })
        } }
    }
    WindowDialog(show = busy, title = stringResource(R.string.manager_configuration), onDismissRequest = {}) {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator()
        }
    }
    WindowDialog(show = confirmReset, title = stringResource(R.string.reset_settings), summary = stringResource(R.string.manager_reset_confirm), onDismissRequest = { confirmReset = false }) {
        ManagerDialogActions({ confirmReset = false }, stringResource(R.string.manager_reset)) {
            confirmReset = false
            model.resetConfiguration()
        }
    }
    WindowDialog(show = importUri != null, title = stringResource(R.string.import_settings), summary = stringResource(R.string.manager_import_confirm), onDismissRequest = { importUri = null }) {
        ManagerDialogActions({ importUri = null }, stringResource(R.string.manager_import)) {
            val uri = importUri ?: return@ManagerDialogActions
            importUri = null
            model.importConfiguration(uri)
        }
    }
}

@Composable
internal fun ManagerUpdatesScreen(onBack: () -> Unit) {
    val model: ManagerWorkflowViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val state by model.release.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(model) { if (state.content == null) model.loadRelease() }
    ManagerDetailScaffold(
        stringResource(R.string.manager_check_updates), false, onBack,
        isRefreshing = state.refreshing,
        onRefresh = { model.loadRelease(true) },
        pageState = when {
            state.content != null -> ManagerPageStateKind.CONTENT
            state.error != null -> ManagerPageStateKind.ERROR
            else -> ManagerPageStateKind.LOADING
        },
        stateTitle = if (state.error != null) stringResource(R.string.manager_check_updates) else stringResource(R.string.manager_loading),
        stateMessage = state.error.orEmpty(),
        onRetry = { model.loadRelease() },
    ) {
        state.content?.let { release ->
            item("release") { ManagerGroupCard {
                ArrowPreference(release.tag, summary = release.notes, onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, release.url.toUri()))
                })
            } }
        }
    }
    LaunchedEffect(state.error) {
        if (state.content != null) state.error?.let(ManagerSnackbarEvents::tryShow)
    }
}

@Composable
internal fun ManagerDiagnosticsScreen(onBack: () -> Unit) {
    val model: ManagerWorkflowViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val state by model.diagnostics.collectAsStateWithLifecycle()
    LaunchedEffect(model) { if (state.content == null) model.runDiagnostics() }
    ManagerDetailScaffold(
        stringResource(R.string.manager_root_diagnostics), false, onBack,
        isRefreshing = state.refreshing,
        onRefresh = { model.runDiagnostics(true) },
        pageState = when {
            state.content != null -> ManagerPageStateKind.CONTENT
            state.error != null -> ManagerPageStateKind.ERROR
            else -> ManagerPageStateKind.LOADING
        },
        stateTitle = stringResource(R.string.manager_root_checking),
        stateMessage = state.error.orEmpty(),
        onRetry = { model.runDiagnostics() },
    ) {
        state.content?.let { logs ->
            item("result", contentType = PageStart.Viewport) {
                Column(Modifier.fillParentMaxSize().padding(horizontal = 28.dp, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Text(stringResource(if (logs.any { it.type == RootDiagnostics.LogType.ERROR }) R.string.manager_diagnostics_issues
                        else R.string.manager_diagnostics_ok), style = MiuixTheme.textStyles.title3,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    logs.forEach { log ->
                        Text(log.message, modifier = Modifier.padding(top = 6.dp),
                            style = MiuixTheme.textStyles.body1,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                    Spacer(Modifier.height(12.dp))
                    TextButton(stringResource(R.string.manager_back), onBack,
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp),
                        colors = ButtonDefaults.textButtonColorsPrimary())
                }
            }
        }
    }
}

@Composable
internal fun ManagerCallRecordingScreen(state: ManagerUiState, controller: PreferenceController, onBack: () -> Unit) {
    val model: ManagerWorkflowViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val rootAvailable by model.rootAvailable.collectAsStateWithLifecycle()
    val useRoot = state.preferences["call_recording_use_root"] as? Boolean ?: false
    val spec = remember { PreferenceSpec("call_recording_use_root", PreferenceSource.MEDIA, "", "", null, PreferenceKind.SWITCH) }
    LaunchedEffect(Unit) { if (rootAvailable == null) model.checkRoot() }
    ManagerDetailScaffold(stringResource(R.string.call_recording_title), false, onBack) {
        item("modes") { ManagerGroupCard {
            OverlayDropdownPreference(
                title = stringResource(R.string.call_recording_title),
                items = listOf(stringResource(R.string.manager_non_root_mode), stringResource(R.string.manager_root_mode)),
                selectedIndex = if (useRoot) 1 else 0,
                onSelectedIndexChange = { index ->
                    model.selectRecordingRootMode(index == 1)
                },
                summary = when (rootAvailable) {
                    null -> stringResource(R.string.manager_root_checking)
                    false -> stringResource(R.string.manager_root_unavailable)
                    true -> null
                },
            )
        } }
    }
}

@Composable
internal fun ManagerRecordingsScreen(onBack: () -> Unit, onNavigate: (ManagerRoute) -> Unit, contact: String? = null) {
    val storageGranted = rememberManagerStorageAccess()
    val context = LocalContext.current
    val resources = LocalResources.current
    val model: ManagerWorkflowViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val state by model.recordings.collectAsStateWithLifecycle()
    val busy by model.recordingOperation.collectAsStateWithLifecycle()
    var grouped by rememberSaveable { mutableStateOf(false) }
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedPaths by rememberSaveable { mutableStateOf(setOf<String>()) }
    var sort by rememberSaveable { mutableStateOf(RecordingSort.DATE) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(model, storageGranted) { if (storageGranted) model.loadRecordings() }
    val recordings = state.content.orEmpty().filter { contact == null || it.contactName == contact }
    val visible = when (sort) {
        RecordingSort.DATE -> recordings.sortedByDescending { it.date }
        RecordingSort.NAME -> recordings.sortedBy { it.file.name.lowercase() }
        RecordingSort.DURATION -> recordings.sortedByDescending { it.duration }
        RecordingSort.CONTACT -> recordings.sortedWith(compareBy<ManagerRecording> { it.contactName.lowercase() }.thenByDescending { it.date })
    }
    LaunchedEffect(recordings) { selectedPaths = selectedPaths.intersect(recordings.map { it.file.absolutePath }.toSet()) }
    val pageTitle = contact ?: stringResource(R.string.manager_recordings)
    ManagerDetailScaffold(pageTitle, false, onBack,
        fixedContent = if (!storageGranted) ({ ManagerStoragePermissionRow() }) else null,
        isRefreshing = state.refreshing, onRefresh = if (storageGranted) ({ model.loadRecordings(true); Unit }) else null,
        pageState = when {
            !storageGranted -> ManagerPageStateKind.EMPTY
            state.content == null && state.error != null -> ManagerPageStateKind.ERROR
            state.content == null -> ManagerPageStateKind.LOADING
            visible.isEmpty() -> ManagerPageStateKind.EMPTY
            else -> ManagerPageStateKind.CONTENT
        },
        stateTitle = stringResource(when {
            !storageGranted -> R.string.storage_permission
            state.error != null && state.content == null -> R.string.manager_load_failed
            state.content == null -> R.string.manager_loading
            else -> R.string.manager_empty
        }),
        stateMessage = state.error.orEmpty(), onRetry = { model.loadRecordings() },
        actions = {
            if (busy) top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator()
            else if (visible.isNotEmpty()) {
                if (selectedPaths.isNotEmpty()) {
                    top.yukonga.miuix.kmp.basic.IconButton(onClick = {
                        val uris = ArrayList(selectedPaths.map { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(it)) })
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND_MULTIPLE).setType("audio/*")
                            .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), resources.getString(R.string.share)))
                    }) { top.yukonga.miuix.kmp.basic.Icon(top.yukonga.miuix.kmp.icon.MiuixIcons.Share, stringResource(R.string.share)) }
                }
                top.yukonga.miuix.kmp.basic.IconButton(onClick = { confirmDelete = true }) {
                    top.yukonga.miuix.kmp.basic.Icon(top.yukonga.miuix.kmp.icon.MiuixIcons.Delete, stringResource(R.string.delete))
                }
                val items = RecordingSort.entries.map { mode -> top.yukonga.miuix.kmp.basic.DropdownItem(
                    resources.getString(when (mode) {
                        RecordingSort.DATE -> R.string.manager_sort_date
                        RecordingSort.NAME -> R.string.manager_sort_name
                        RecordingSort.DURATION -> R.string.manager_sort_duration
                        RecordingSort.CONTACT -> R.string.manager_sort_contact
                    }), selected = mode == sort, onClick = { sort = mode })
                } + listOf(
                    top.yukonga.miuix.kmp.basic.DropdownItem(stringResource(R.string.manager_select_recordings), selected = selectionMode, onClick = { selectionMode = !selectionMode; selectedPaths = emptySet() }),
                    top.yukonga.miuix.kmp.basic.DropdownItem(stringResource(R.string.manager_select_all), onClick = { selectionMode = true; selectedPaths = visible.map { it.file.absolutePath }.toSet() }),
                ) + if (contact == null) listOf(top.yukonga.miuix.kmp.basic.DropdownItem(stringResource(R.string.manager_group_by_contact), selected = grouped, onClick = { grouped = !grouped })) else emptyList()
                top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu(entry = top.yukonga.miuix.kmp.basic.DropdownEntry(items)) {
                    top.yukonga.miuix.kmp.basic.Icon(top.yukonga.miuix.kmp.icon.MiuixIcons.More, stringResource(R.string.manager_sort))
                }
            }
        },
    ) {
        state.error?.let { message -> item("refresh-error", contentType = PageStart.Inset) { ManagerTipCard(message) } }
        if (grouped && !selectionMode) {
            managerGroupedCardItems("recording-contacts", visible.groupBy { it.contactName }.map { (name, items) ->
                ManagerCardItem(name) { ArrowPreference(name, summary = resources.getQuantityString(R.plurals.manager_recording_count, items.size, items.size),
                    enabled = !busy, onClick = { onNavigate(ManagerRoute.RecordingContact(name)) }) }
            })
        } else managerGroupedCardItems("recordings", visible.map { recording ->
            ManagerCardItem(recording.file.absolutePath) {
                val summary = stringResource(R.string.manager_recording_summary, recording.formattedDuration, recording.formattedSize,
                    java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(java.util.Date(recording.date)))
                if (selectionMode) top.yukonga.miuix.kmp.preference.CheckboxPreference(
                    checked = recording.file.absolutePath in selectedPaths,
                    onCheckedChange = { checked -> selectedPaths = if (checked) selectedPaths + recording.file.absolutePath else selectedPaths - recording.file.absolutePath },
                    title = recording.contactName, summary = summary, enabled = !busy,
                ) else ArrowPreference(recording.contactName, summary = summary, enabled = !busy, onClick = {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", recording.file)
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "audio/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                        .onFailure { ManagerSnackbarEvents.tryShow(resources.getString(R.string.manager_operation_failed, it.message.orEmpty())) }
                })
            }
        })
    }
    val deleting = if (selectedPaths.isEmpty()) visible.map { it.file.absolutePath }.toSet() else selectedPaths
    WindowDialog(show = confirmDelete, title = stringResource(R.string.delete), summary = stringResource(R.string.manager_delete_recordings_confirm, deleting.size), onDismissRequest = { confirmDelete = false }) {
        ManagerDialogActions({ confirmDelete = false }, stringResource(R.string.delete)) {
            confirmDelete = false
            model.deleteRecordings(deleting)
        }
    }
}

private enum class RecordingSort { DATE, NAME, DURATION, CONTACT }

@Composable
internal fun ManagerThemeManagerScreen(state: ManagerUiState, onBack: () -> Unit, onNavigate: (ManagerRoute) -> Unit) {
    val storageGranted = rememberManagerStorageAccess()
    val context = LocalContext.current
    val resources = LocalResources.current
    val model: ManagerFileWorkflowViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val loaded by model.themes.collectAsStateWithLifecycle()
    val importing by model.busy.collectAsStateWithLifecycle()
    val created by model.created.collectAsStateWithLifecycle()
    val themes = loaded.content
    val error = loaded.error
    val refreshing = loaded.refreshing
    var creating by remember { mutableStateOf(false) }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    LaunchedEffect(storageGranted) { if (storageGranted) model.loadThemes() }
    LaunchedEffect(created) { created?.let { model.consumeCreated(); onNavigate(ManagerRoute.ThemeEditor(it)) } }
    val importTheme = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { importUri = it }
    ManagerDetailScaffold(stringResource(R.string.theme_manager), false, onBack,
        fixedContent = if (!storageGranted) ({ ManagerStoragePermissionRow() }) else null,
        isRefreshing = refreshing, onRefresh = if (storageGranted) ({ model.loadThemes(true); Unit }) else null,
        pageState = if (!storageGranted) ManagerPageStateKind.EMPTY else if (themes != null) ManagerPageStateKind.CONTENT else if (error == null) ManagerPageStateKind.LOADING else ManagerPageStateKind.ERROR,
        stateTitle = stringResource(when {
            !storageGranted -> R.string.storage_permission
            themes == null && error != null -> R.string.manager_load_failed
            else -> R.string.manager_loading
        }),
        stateMessage = error.orEmpty(), onRetry = { model.loadThemes() },
    ) {
        error?.let { message -> item("refresh-error", contentType = PageStart.Inset) { ManagerTipCard(message) } }
        item("tip", contentType = PageStart.Inset) { ManagerTipCard(stringResource(R.string.manager_theme_tip)) }
        item("actions") { ManagerGroupCard {
            ArrowPreference(stringResource(R.string.create_new_theme), enabled = !importing, onClick = { creating = true })
            ArrowPreference(stringResource(R.string.import_theme), enabled = !importing, onClick = { importTheme.launch(arrayOf("application/zip")) })
        } }
        item("themes") { ManagerGroupCard {
            ArrowPreference(stringResource(R.string.manager_default_theme), summary = if (state.preferences["folder_theme"] == null) stringResource(R.string.manager_selected) else null,
                enabled = !importing, onClick = model::selectDefault)
            themes.orEmpty().forEach { folder ->
                ArrowPreference(folder.name, summary = if (state.preferences["folder_theme"] == folder.name) stringResource(R.string.manager_selected) else null,
                    enabled = !importing, onClick = { onNavigate(ManagerRoute.ThemeEditor(folder.name)) })
            }
        } }
    }
    ManagerTextInputDialog(
        show = creating, title = stringResource(R.string.new_theme_name), initialValue = "",
        onDismiss = { creating = false }, maxLength = 64,
        validate = { name ->
            if (!validThemeName(name) || File(ThemePreference.rootDirectory, name.trim()).exists())
                resources.getString(R.string.manager_theme_name_invalid) else null
        },
        onConfirm = { name -> creating = false; model.createTheme(name) },
    )
    WindowDialog(show = importUri != null, title = stringResource(R.string.import_theme), summary = stringResource(R.string.manager_theme_import_confirm), onDismissRequest = { importUri = null }) {
        ManagerDialogActions({ importUri = null }, stringResource(R.string.manager_import)) {
            val uri = importUri ?: return@ManagerDialogActions
            importUri = null
            model.importTheme(uri)
        }
    }
    WindowDialog(show = importing, title = stringResource(R.string.theme_manager), onDismissRequest = {}) {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator() }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun ManagerThemeEditorScreen(folderName: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val model: ManagerFileWorkflowViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val editors by model.editors.collectAsStateWithLifecycle()
    val loaded = editors[folderName] ?: ManagerLoadState()
    val css = loaded.content
    val error = loaded.error
    val busy by model.busy.collectAsStateWithLifecycle()
    var webView by remember { mutableStateOf<WebView?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    LaunchedEffect(folderName) { model.loadEditor(folderName) }
    fun save(apply: Boolean = false) {
        if (busy) return
        webView?.evaluateJavascript("getTextareaContent();") { encoded ->
            try {
                val decoded = JSONObject("{\"value\":$encoded}").getString("value")
                model.saveEditor(folderName, decoded, apply)
            } catch (failure: Exception) {
                ManagerSnackbarEvents.tryShow(resources.getString(R.string.manager_operation_failed, failure.message.orEmpty()))
            }
        }
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { model.exportTheme(folderName, it) }
    }
    ManagerDetailScaffold(folderName, false, onBack, contentMaxWidth = null,
        pageState = if (css != null) ManagerPageStateKind.CONTENT else if (error == null) ManagerPageStateKind.LOADING else ManagerPageStateKind.ERROR,
        stateMessage = error.orEmpty(),
        actions = {
            if (busy) top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator()
            else if (webView != null) {
                top.yukonga.miuix.kmp.basic.IconButton(onClick = { save() }) {
                    top.yukonga.miuix.kmp.basic.Icon(top.yukonga.miuix.kmp.icon.MiuixIcons.Ok, stringResource(R.string.manager_save))
                }
                top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu(entry = top.yukonga.miuix.kmp.basic.DropdownEntry(listOf(
                    top.yukonga.miuix.kmp.basic.DropdownItem(stringResource(R.string.manager_apply), onClick = { save(true) }),
                    top.yukonga.miuix.kmp.basic.DropdownItem(stringResource(R.string.clear), onClick = { confirmClear = true }),
                    top.yukonga.miuix.kmp.basic.DropdownItem(stringResource(R.string.export_as_zip), onClick = { export.launch("$folderName.zip") }),
                ))) { top.yukonga.miuix.kmp.basic.Icon(top.yukonga.miuix.kmp.icon.MiuixIcons.More, stringResource(R.string.manager_more)) }
            }
        },
    ) {
        css?.let { initial -> item("editor", contentType = PageStart.Viewport) {
            AndroidView(
                modifier = Modifier.fillParentMaxSize(),
                factory = { activityContext ->
                    WebView(activityContext).apply {
                        // Chromium forces a zero CSS layout height for WRAP_CONTENT,
                        // even when Compose measures the native view to fill its parent.
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        settings.javaScriptEnabled = true
                        settings.allowContentAccess = false
                        settings.allowFileAccess = false
                        settings.domStorageEnabled = false
                        webViewClient = ManagerEditorAssets(activityContext)
                        addJavascriptInterface(ManagerEditorBridge { model.updateDraft(folderName, it) }, "ManagerEditor")
                        webChromeClient = WebChromeClient()
                        val template = activityContext.assets.open("css_editor.html").bufferedReader().use { it.readText() }
                        val escaped = android.text.TextUtils.htmlEncode(model.editorContent(folderName, initial))
                        loadDataWithBaseURL(MANAGER_EDITOR_BASE_URL, template.replace("{{content}}", escaped), "text/html", "UTF-8", null)
                        webView = this
                    }
                },
                onRelease = { view -> webView = null; view.stopLoading(); view.destroy() },
            )
        } }
    }
    WindowDialog(show = confirmClear, title = stringResource(R.string.clear), summary = stringResource(R.string.manager_clear_editor_confirm), onDismissRequest = { confirmClear = false }) {
        ManagerDialogActions({ confirmClear = false }, stringResource(R.string.clear)) {
            webView?.evaluateJavascript("document.getElementById('code').value = ''; document.getElementById('code').dispatchEvent(new Event('input', {bubbles:true}));", null)
            model.updateDraft(folderName, "")
            confirmClear = false
        }
    }
}

private class ManagerEditorBridge(private val onChanged: (String) -> Unit) {
    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    @android.webkit.JavascriptInterface
    fun changed(css: String) { main.post { onChanged(css) } }
}
