package com.freeteaspoon.wppenhacer.ui.miuix

import android.app.Application
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.freeteaspoon.wppenhacer.R
import com.freeteaspoon.wppenhacer.utils.StickerRestore
import com.freeteaspoon.wppenhacer.utils.StickerRestore.StickerTarget
import com.freeteaspoon.wppenhacer.utils.WhatsAppContactPickerLauncher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference

internal sealed interface StickerRestoreStep {
    data class ChooseApp(val apps: List<List<StickerTarget>>) : StickerRestoreStep
    data class ChooseAccount(val accounts: List<StickerTarget>) : StickerRestoreStep
    data class Confirm(val target: StickerTarget, val preview: StickerRestore.Preview) : StickerRestoreStep
    data class Done(val target: StickerTarget, val count: Int) : StickerRestoreStep
    data class Message(@param:StringRes val text: Int, val detail: String? = null) : StickerRestoreStep
}

internal class StickerRestoreViewModel(private val app: Application) : AndroidViewModel(app) {
    private val _step = MutableStateFlow<StickerRestoreStep?>(null)
    val step = _step.asStateFlow()
    val operation = ManagerOperation()
    private var task: Job? = null

    fun start() = runTask(R.string.restore_stickers_loading, {
        if (StickerRestore.isRootAvailable) StickerRestore.findTargets() else null
    }) { targets ->
        when {
            targets == null -> _step.value = StickerRestoreStep.Message(R.string.restore_stickers_no_root)
            targets.isEmpty() -> _step.value = StickerRestoreStep.Message(R.string.restore_stickers_not_found)
            else -> {
                val apps = targets.groupBy { it.packageName to it.userId }.values.toList()
                if (apps.size == 1) chooseApp(apps.single())
                else _step.value = StickerRestoreStep.ChooseApp(apps)
            }
        }
    }

    fun chooseApp(accounts: List<StickerTarget>) {
        if (accounts.size == 1) preview(accounts.single())
        else _step.value = StickerRestoreStep.ChooseAccount(accounts)
    }

    fun preview(target: StickerTarget) = runTask(R.string.restore_stickers_loading, {
        StickerRestore.preview(app, target)
    }) { preview ->
        _step.value = if (preview.toRestore == 0) StickerRestoreStep.Message(R.string.restore_stickers_nothing)
        else StickerRestoreStep.Confirm(target, preview)
    }

    fun restore(target: StickerTarget) = runTask(R.string.restore_stickers, {
        StickerRestore.restore(app, target)
    }) { count -> _step.value = StickerRestoreStep.Done(target, count) }

    fun dismiss() { _step.value = null }

    private fun <T> runTask(@StringRes label: Int, block: () -> T, onResult: (T) -> Unit) {
        if (task?.isActive == true) return
        dismiss()
        task = viewModelScope.launch {
            val result = try {
                operation.run(app.getString(label)) { withContext(Dispatchers.IO) { block() } }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _step.value = StickerRestoreStep.Message(R.string.restore_stickers_failed, error.localizedMessage.orEmpty())
                return@launch
            }
            task = null
            onResult(result)
        }
    }
}

@Composable
internal fun StickerRestoreDialogs(model: StickerRestoreViewModel) {
    val step by model.step.collectAsStateWithLifecycle()
    val context = LocalContext.current
    ManagerOperationHost(model.operation)
    val current = step
    val title = when (current) {
        is StickerRestoreStep.ChooseApp -> R.string.select_whatsapp_app
        is StickerRestoreStep.ChooseAccount -> R.string.restore_stickers_choose_account
        else -> R.string.restore_stickers
    }
    val summary = when (current) {
        is StickerRestoreStep.Confirm -> with(current.preview) {
            stringResource(R.string.restore_stickers_preview, toRestore, total, starred) +
                "\n\n" + stringResource(R.string.restore_stickers_warning)
        }
        is StickerRestoreStep.Done -> stringResource(R.string.restore_stickers_success, current.count)
        is StickerRestoreStep.Message -> current.detail?.let { stringResource(current.text, it) } ?: stringResource(current.text)
        else -> null
    }
    OverlayDialog(show = current != null, title = stringResource(title), summary = summary,
        onDismissRequest = model::dismiss) {
        when (current) {
            is StickerRestoreStep.ChooseApp -> current.apps.forEach { accounts ->
                val target = accounts.first()
                val label = WhatsAppContactPickerLauncher.getPackageLabel(target.packageName)
                val profile = if (target.userId == 0) null else stringResource(R.string.restore_stickers_user_suffix, target.userId)
                ArrowPreference(title = listOfNotNull(label, profile).joinToString(" "), onClick = { model.chooseApp(accounts) })
            }
            is StickerRestoreStep.ChooseAccount -> current.accounts.forEach { target ->
                val name = target.name ?: target.accountId?.let { stringResource(R.string.restore_stickers_account, it) }
                    ?: stringResource(R.string.restore_stickers_main_account)
                val active = if (target.isActive) stringResource(R.string.restore_stickers_active) else null
                ArrowPreference(title = listOfNotNull(name, active).joinToString(" "), summary = target.phone,
                    onClick = { model.preview(target) })
            }
            is StickerRestoreStep.Confirm -> ManagerDialogActions(model::dismiss, stringResource(R.string.restore_stickers_confirm)) {
                model.restore(current.target)
            }
            is StickerRestoreStep.Done -> {
                val launch = if (current.target.userId == 0) {
                    context.packageManager.getLaunchIntentForPackage(current.target.packageName)
                } else null
                if (launch == null) TextButton(stringResource(android.R.string.ok), model::dismiss, Modifier.fillMaxWidth())
                else ManagerDialogActions(model::dismiss, stringResource(R.string.restore_stickers_open_whatsapp)) {
                    model.dismiss()
                    runCatching { context.startActivity(launch) }
                }
            }
            is StickerRestoreStep.Message -> TextButton(stringResource(android.R.string.ok), model::dismiss, Modifier.fillMaxWidth())
            null -> Spacer(Modifier.height(0.dp))
        }
    }
}
