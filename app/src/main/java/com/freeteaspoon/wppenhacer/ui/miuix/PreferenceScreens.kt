package com.freeteaspoon.wppenhacer.ui.miuix

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.freeteaspoon.wppenhacer.App
import com.freeteaspoon.wppenhacer.R
import com.freeteaspoon.wppenhacer.utils.RealPathUtil
import com.freeteaspoon.wppenhacer.utils.WhatsAppContactPickerLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import java.io.File

@Composable
internal fun PreferencePageScreen(
    source: PreferenceSource,
    state: ManagerUiState,
    controller: PreferenceController,
    wide: Boolean,
    bottomPadding: Dp,
    onBack: (() -> Unit)?,
    onNavigate: (ManagerRoute) -> Unit,
    highlightKey: String? = null,
    featureKey: String? = null,
) {
    val feature = state.specs.firstOrNull { it.key == featureKey }
    val title = feature?.title ?: preferenceSourceTitle(source)
    var editing by remember { mutableStateOf<PreferenceSpec?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    val masters = state.specs.mapNotNull { it.dependency }.toSet()
    val groups = if (featureKey == null) state.groups(source).map { group ->
        group.copy(preferences = group.preferences.filter { it.dependency == null })
    }.filter { it.preferences.isNotEmpty() } else listOf(PreferenceGroup(title,
        state.specs.filter { it.key == featureKey || it.dependency == featureKey }))
    val highlightedIndex = groups.preferenceItemIndex(highlightKey)
    val listState = rememberLazyListState()
    val highlightRequester = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    var highlightPositionApplied by rememberSaveable(source, highlightKey) { mutableStateOf(false) }
    LaunchedEffect(source, highlightKey, highlightedIndex) {
        if (highlightedIndex >= 0 && !highlightPositionApplied) {
            listState.scrollToItem(highlightedIndex)
            androidx.compose.runtime.withFrameNanos { }
            highlightRequester.bringIntoView()
            highlightPositionApplied = true
        }
    }
    ManagerDetailScaffoldWithState(title, wide, onBack, listState) {
        groups.forEachIndexed { groupIndex, group ->
            if (groups.size > 1) managerSection(group.title, "$source-$groupIndex")
            managerGroupedCardItems(
                keyPrefix = "${source.name}:$groupIndex",
                items = group.preferences.map { spec ->
                    ManagerCardItem(spec.key) {
                        val enabled = controller.isEnabled(spec, state.preferences)
                        if (featureKey == null && spec.key in masters) {
                            ArrowPreference(spec.title, summary = stringResource(
                                if (state.preferences[spec.key] == true) R.string.manager_on else R.string.manager_off),
                                onClick = { onNavigate(ManagerRoute.PreferenceFeature(source, spec.key)) })
                        } else androidx.compose.foundation.layout.Box(Modifier.testTag("preference-${spec.key}")
                            .semantics(mergeDescendants = !enabled) {
                                if (!enabled) disabled()
                            }.then(
                            if (spec.key == highlightKey) Modifier.bringIntoViewRequester(highlightRequester) else Modifier
                        )) { PreferenceSpecRow(
                            spec = spec,
                            value = state.preferences[spec.key],
                            enabled = enabled,
                            highlighted = spec.key == highlightKey,
                            onEdit = { editing = spec; showEditor = true },
                            onPut = { controller.put(spec, it) },
                            onNavigate = onNavigate,
                        ) }
                    }
                },
            )
        }

    }
    editing?.let { spec ->
        PreferenceValueDialog(
            show = showEditor,
            spec = spec,
            value = state.preferences[spec.key],
            onDismiss = { showEditor = false },
            // Keep the dialog composed until its exit animation finishes.
            onDismissFinished = { editing = null },
            onSave = { controller.put(spec, it); showEditor = false },
        )
    }
}

@Composable
private fun PreferenceSpecRow(
    spec: PreferenceSpec,
    value: Any?,
    enabled: Boolean,
    highlighted: Boolean,
    onEdit: () -> Unit,
    onPut: (Any) -> Unit,
    onNavigate: (ManagerRoute) -> Unit,
) {
    val summary = preferenceSummary(spec, value)
    val title = spec.title
    val titleColor = top.yukonga.miuix.kmp.basic.BasicComponentDefaults.titleColor(
        color = if (highlighted) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceContainer)
    when (spec.kind) {
        PreferenceKind.SWITCH -> SwitchPreference(
            checked = value as? Boolean ?: spec.defaultValue.toBoolean(),
            onCheckedChange = onPut,
            title = title,
            titleColor = titleColor,
            summary = summary,
            enabled = enabled,
        )
        PreferenceKind.LIST -> {
            val selectedValue = value?.toString()
            val selectedIndex = (spec.entryValues.indexOf(selectedValue)
                .takeIf { it in spec.entries.indices }
                ?: spec.entries.indexOf(selectedValue).takeIf { it >= 0 }
                ?: 0)
            if (spec.entries.isEmpty()) {
                ArrowPreference(title, titleColor = titleColor, summary = summary, enabled = enabled, onClick = onEdit)
            } else {
                OverlayDropdownPreference(
                    title = title,
            titleColor = titleColor,
                    summary = summary,
                    items = spec.entries,
                    selectedIndex = selectedIndex,
                    enabled = enabled,
                    onSelectedIndexChange = { index ->
                        onPut(spec.entryValues.getOrNull(index) ?: spec.entries[index])
                    },
                )
            }
        }
        PreferenceKind.MULTI_LIST -> {
            val selected = (value as? Set<*>)?.filterIsInstance<String>()?.toSet().orEmpty()
            val entries = spec.entries.mapIndexed { index, label ->
                val option = spec.entryValues.getOrNull(index) ?: label
                DropdownItem(
                    text = label,
                    selected = option in selected,
                    onClick = {
                        onPut(if (option in selected) selected - option else selected + option)
                    },
                )
            }
            if (entries.isEmpty()) {
                ArrowPreference(title, titleColor = titleColor, summary = summary, enabled = enabled, onClick = onEdit)
            } else {
                OverlayDropdownPreference(
                    title = title,
            titleColor = titleColor,
                    summary = summary,
                    entry = DropdownEntry(entries),
                    enabled = enabled,
                    collapseOnSelection = false,
                )
            }
        }
        PreferenceKind.INTEGER_SLIDER, PreferenceKind.FLOAT_SLIDER -> {
            val current = (value as? Number)?.toFloat() ?: spec.defaultValue?.toFloatOrNull() ?: spec.minimum
            ArrowPreference(title = title,
            titleColor = titleColor, summary = summary, enabled = enabled, onClick = onEdit)
        }

        PreferenceKind.THEME -> ArrowPreference(title, titleColor = titleColor, summary = summary, enabled = enabled, onClick = { onNavigate(ManagerRoute.ThemeManager) })
        PreferenceKind.ACTION -> ArrowPreference(
            title,
            titleColor = titleColor,
            summary = summary,
            enabled = enabled,
            onClick = if (spec.key == "call_recording_settings") ({ onNavigate(ManagerRoute.CallRecording) }) else onEdit,
        )
        else -> ArrowPreference(title, titleColor = titleColor, summary = summary, enabled = enabled, onClick = onEdit)
    }
}

@Composable
private fun PreferenceValueDialog(
    show: Boolean,
    spec: PreferenceSpec,
    value: Any?,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
    onSave: (Any) -> Unit,
) {
    val context = LocalContext.current
    val fileModel: ManagerFileWorkflowViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { onDismiss(); fileModel.importPreference(spec, uri) }
    }
    val selectContacts = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra("contacts")?.let { onSave(it.toString()) }
        }
    }
    val resources = androidx.compose.ui.platform.LocalResources.current
    val plainInput = spec.kind !in setOf(PreferenceKind.FILE, PreferenceKind.CONTACTS)
    if (spec.kind == PreferenceKind.FILE && spec.key in setOf("download_local", "call_recording_path")) {
        ManagerTextInputDialog(show, spec.title, value?.toString().orEmpty(), onDismiss, { onSave(it) },
            summary = spec.summary, maxLength = 4096, onDismissFinished = onDismissFinished,
            validate = { path -> if (path.startsWith("/") && !path.contains('\u0000')) null
                else resources.getString(R.string.manager_invalid_value) })
    } else if (plainInput) {
        val numeric = spec.kind in setOf(PreferenceKind.INTEGER_SLIDER, PreferenceKind.FLOAT_SLIDER)
        val initial = when (spec.kind) {
            PreferenceKind.COLOR -> (value as? Number)?.toInt()?.let { "#%08X".format(it) } ?: ""
            else -> value?.toString().orEmpty()
        }
        ManagerTextInputDialog(
            show = show,
            title = spec.title,
            summary = spec.summary,
            initialValue = initial,
            onDismiss = onDismiss,
            onDismissFinished = onDismissFinished,
            keyboardType = when {
                spec.isSecretPreference() -> KeyboardType.Password
                numeric -> KeyboardType.Decimal
                else -> KeyboardType.Text
            },
            visualTransformation = if (spec.isSecretPreference()) androidx.compose.ui.text.input.PasswordVisualTransformation()
                else androidx.compose.ui.text.input.VisualTransformation.None,
            allowBlank = !numeric && spec.kind != PreferenceKind.COLOR,
            maxLength = if (spec.kind == PreferenceKind.TEXT) spec.maxLength else 256,
            // CSS/XML/text documents are editors, not a single settings value.
            singleLine = spec.key != "css_theme",
            validate = { text ->
                when {
                    spec.kind == PreferenceKind.INTEGER_SLIDER -> if (text.toIntOrNull()?.let { it.toFloat() in spec.minimum..spec.maximum } == true) null
                        else resources.getString(R.string.manager_value_range, spec.minimum.toString(), spec.maximum.toString())
                    numeric -> if (text.toFloatOrNull()?.let { it.isFinite() && it in spec.minimum..spec.maximum } == true) null
                        else resources.getString(R.string.manager_value_range, spec.minimum.toString(), spec.maximum.toString())
                    spec.kind == PreferenceKind.COLOR -> if (text.removePrefix("#").let { it.length in setOf(6, 8) && it.toLongOrNull(16) != null }) null
                        else resources.getString(R.string.manager_invalid_value)
                    else -> null
                }
            },
            onConfirm = { text ->
                val parsed: Any = when (spec.kind) {
                    PreferenceKind.INTEGER_SLIDER -> text.toInt()
                    PreferenceKind.FLOAT_SLIDER -> text.toFloat()
                    PreferenceKind.COLOR -> text.removePrefix("#").let { if (it.length == 6) "FF$it" else it }.toLong(16).toInt()
                    else -> text
                }
                onSave(parsed)
            },
        )
    } else WindowDialog(show = show, title = spec.title, summary = spec.summary,
        onDismissRequest = onDismiss, onDismissFinished = onDismissFinished) {
        if (spec.kind == PreferenceKind.FILE) {
            val storageAccess = rememberManagerStorageAccess()
            val canImport = spec.key == "bootloader_spoofer_xml" || storageAccess
            if (!canImport) ManagerStoragePermissionPreference()
            ArrowPreference(stringResource(R.string.manager_choose_file), enabled = canImport,
                modifier = Modifier.semantics(mergeDescendants = true) { if (!canImport) disabled() },
                onClick = { openFile.launch(arrayOf("*/*")) })
        } else {
            ArrowPreference(stringResource(R.string.manager_choose_contacts), onClick = {
                val installed = WhatsAppContactPickerLauncher.getInstalledWhatsAppPackages(context).firstOrNull()
                if (installed == null) ManagerSnackbarEvents.tryShow(resources.getString(R.string.manager_not_installed))
                installed?.let { packageName ->
                    val contacts = value?.toString()?.removeSurrounding("[", "]")?.split(',')?.map(String::trim)
                        ?.filter(String::isNotEmpty)?.let { ArrayList(it) } ?: arrayListOf()
                    selectContacts.launch(WhatsAppContactPickerLauncher.createPickerIntent(context, packageName, spec.key, contacts))
                }
            })
        }
    }
}
@Composable
private fun preferenceSourceTitle(source: PreferenceSource): String = when (source) {
    PreferenceSource.GENERAL -> stringResource(R.string.general)
    PreferenceSource.HOME_SCREEN -> stringResource(R.string.home_screen)
    PreferenceSource.CONVERSATION -> stringResource(R.string.conversation)
    PreferenceSource.STATUS -> stringResource(R.string.status)
    PreferenceSource.PRIVACY -> stringResource(R.string.privacy)
    PreferenceSource.MEDIA -> stringResource(R.string.media)
    PreferenceSource.CUSTOMIZE -> stringResource(R.string.manager_customize)
}

internal fun preferenceSummary(spec: PreferenceSpec, value: Any?): String? {
    if (spec.isSecretPreference()) {
        return if ((value as? String)?.isNotBlank() == true) "\u2022".repeat(8) else spec.summary
    }
    val display = when (spec.kind) {
        PreferenceKind.LIST, PreferenceKind.MULTI_LIST -> null
        PreferenceKind.COLOR -> (value as? Number)?.toInt()?.let { "#%08X".format(it) }
        PreferenceKind.TEXT, PreferenceKind.FILE, PreferenceKind.CONTACTS, PreferenceKind.INTEGER_SLIDER, PreferenceKind.FLOAT_SLIDER -> value?.toString()
        else -> null
    }
    return display?.takeIf(String::isNotBlank) ?: spec.summary
}

@Composable
private fun ManagerDetailScaffoldWithState(
    title: String,
    wide: Boolean,
    onBack: (() -> Unit)?,
    listState: LazyListState,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    // Kept separate so search routing can own and restore the target list position.
    ManagerDetailScaffold(title, wide, onBack, listState = listState, content = content)
}
