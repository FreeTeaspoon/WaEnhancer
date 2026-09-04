package com.wmods.wppenhacer.ui.miuix

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wmods.wppenhacer.App
import com.wmods.wppenhacer.R
import com.wmods.wppenhacer.utils.RealPathUtil
import com.wmods.wppenhacer.utils.WhatsAppContactPickerLauncher
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
) {
    val title = preferenceSourceTitle(source)
    var editing by remember { mutableStateOf<PreferenceSpec?>(null) }
    val specs = state.specs.filter { it.source == source && !it.isManagerAppearancePreference() }
    val highlightedIndex = specs.indexOfFirst { it.key == highlightKey }
    val listState = rememberLazyListState()
    LaunchedEffect(highlightedIndex) {
        if (highlightedIndex >= 0) {
            val groupsBefore = specs.take(highlightedIndex).map { it.category }.distinct().size
            listState.animateScrollToItem((highlightedIndex + groupsBefore + 1).coerceAtLeast(0))
        }
    }
    ManagerDetailScaffoldWithState(title, wide, onBack, listState) {
        state.groups(source).forEachIndexed { groupIndex, group ->
            managerSection(group.title, "$source-$groupIndex")
            item("$source-group-$groupIndex") {
                ManagerGroupCard {
                    group.preferences.forEach { spec ->
                        PreferenceSpecRow(
                            spec = spec,
                            value = state.preferences[spec.key],
                            enabled = controller.isEnabled(spec, state.preferences),
                            highlighted = spec.key == highlightKey,
                            onEdit = { editing = spec },
                            onPut = { controller.put(spec, it) },
                            onNavigate = onNavigate,
                        )
                    }
                }
            }
        }
        item("bottom-$source") { Spacer(Modifier.height(bottomPadding + 18.dp)) }
    }
    editing?.let { spec ->
        PreferenceValueDialog(
            spec = spec,
            value = state.preferences[spec.key],
            onDismiss = { editing = null },
            onSave = { controller.put(spec, it); editing = null },
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
    val title = if (highlighted) "● ${spec.title}" else spec.title
    when (spec.kind) {
        PreferenceKind.SWITCH -> SwitchPreference(
            checked = value as? Boolean ?: spec.defaultValue.toBoolean(),
            onCheckedChange = onPut,
            title = title,
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
                ArrowPreference(title, summary = summary, enabled = enabled, onClick = null)
            } else {
                OverlayDropdownPreference(
                    title = title,
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
                ArrowPreference(title, summary = summary, enabled = enabled, onClick = null)
            } else {
                OverlayDropdownPreference(
                    title = title,
                    summary = summary,
                    entry = DropdownEntry(entries),
                    enabled = enabled,
                    collapseOnSelection = false,
                )
            }
        }
        PreferenceKind.INTEGER_SLIDER, PreferenceKind.FLOAT_SLIDER -> {
            val current = (value as? Number)?.toFloat() ?: spec.defaultValue?.toFloatOrNull() ?: spec.minimum
            var pendingValue by remember(spec.key, current) { mutableFloatStateOf(current) }
            ArrowPreference(
                title = title,
                summary = summary,
                enabled = enabled,
                onClick = onEdit,
                bottomAction = {
                    Slider(
                        value = pendingValue.coerceIn(spec.minimum, spec.maximum),
                        onValueChange = { pendingValue = it },
                        onValueChangeFinished = {
                            onPut(if (spec.kind == PreferenceKind.INTEGER_SLIDER) pendingValue.toInt() else pendingValue)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        valueRange = spec.minimum..spec.maximum,
                        showKeyPoints = false,
                        hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                    )
                },
            )
        }
        PreferenceKind.THEME -> ArrowPreference(title, summary = summary, enabled = enabled, onClick = { onNavigate(ManagerRoute.ThemeManager) })
        PreferenceKind.ACTION -> ArrowPreference(
            title,
            summary = summary,
            enabled = enabled,
            onClick = if (spec.key == "call_recording_settings") ({ onNavigate(ManagerRoute.CallRecording) }) else onEdit,
        )
        else -> ArrowPreference(title, summary = summary, enabled = enabled, onClick = onEdit)
    }
}

@Composable
private fun PreferenceValueDialog(spec: PreferenceSpec, value: Any?, onDismiss: () -> Unit, onSave: (Any) -> Unit) {
    var input by remember(spec.key, value) { mutableStateOf(value?.toString().orEmpty()) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val stored = withContext(Dispatchers.IO) {
                if (spec.key == "bootloader_spoofer_xml") {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                } else {
                    val type = context.contentResolver.getType(uri).orEmpty()
                    val extension = type.substringAfter('/', "bin").substringBefore('+')
                    val directory = File(App.waEnhancerFolder, "files").apply { mkdirs() }
                    val target = File(directory, "${spec.key}.$extension")
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        target.outputStream().use(inputStream::copyTo)
                    }
                    target.absolutePath
                }
            }
            stored?.let(onSave)
        }
    }
    val selectContacts = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra("contacts")?.let { onSave(it.toString()) }
        }
    }
    WindowDialog(
        show = true,
        title = spec.title,
        summary = spec.summary,
        onDismissRequest = onDismiss,
    ) {
        when (spec.kind) {
            PreferenceKind.FILE -> {
                Text(spec.summary.orEmpty())
                Spacer(Modifier.height(12.dp))
                TextButton(
                    text = stringResource(R.string.manager_choose_file),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = { openFile.launch(arrayOf("*/*")) },
                )
                if (spec.key == "download_local" || spec.key == "call_recording_path") {
                    TextField(value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    DialogButtons(onDismiss) { if (input.isNotBlank()) onSave(input) }
                }
            }
            PreferenceKind.CONTACTS -> {
                TextButton(
                    text = stringResource(R.string.manager_choose_contacts),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        val packages = WhatsAppContactPickerLauncher.getInstalledWhatsAppPackages(context)
                        packages.firstOrNull()?.let { packageName ->
                            val contacts = value?.toString()?.removeSurrounding("[", "]")
                                ?.split(',')?.map(String::trim)?.filter(String::isNotEmpty)?.let { ArrayList(it) } ?: arrayListOf()
                            selectContacts.launch(WhatsAppContactPickerLauncher.createPickerIntent(context, packageName, spec.key, contacts))
                        }
                    },
                )
            }
            else -> {
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (spec.kind in setOf(PreferenceKind.INTEGER_SLIDER, PreferenceKind.FLOAT_SLIDER, PreferenceKind.COLOR)) KeyboardType.Number else KeyboardType.Text,
                    ),
                    singleLine = spec.kind != PreferenceKind.TEXT,
                )
                Spacer(Modifier.height(12.dp))
                DialogButtons(onDismiss) {
                    val parsed: Any? = when (spec.kind) {
                        PreferenceKind.INTEGER_SLIDER -> input.toIntOrNull()
                        PreferenceKind.FLOAT_SLIDER -> input.toFloatOrNull()
                        PreferenceKind.COLOR -> input.removePrefix("#").toLongOrNull(16)?.toInt()
                        else -> input
                    }
                    parsed?.let(onSave)
                }
            }
        }
    }
}

@Composable
private fun DialogButtons(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(stringResource(android.R.string.cancel), onClick = onDismiss, modifier = Modifier.weight(1f))
        TextButton(stringResource(android.R.string.ok), onClick = onConfirm, modifier = Modifier.weight(1f), colors = ButtonDefaults.textButtonColorsPrimary())
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

private fun preferenceSummary(spec: PreferenceSpec, value: Any?): String? {
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
