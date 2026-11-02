package com.freeteaspoon.wppenhacer.ui.miuix

import android.app.Application
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import com.freeteaspoon.wppenhacer.App
import com.freeteaspoon.wppenhacer.R
import com.freeteaspoon.wppenhacer.utils.WhatsAppContactPickerLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.overlay.OverlayDialog

internal class ManagerInstalledApps(app: Application) : AndroidViewModel(app) {
    private val _packages = MutableStateFlow<List<String>>(emptyList())
    val packages = _packages.asStateFlow()
    fun refresh() = viewModelScope.launch {
        _packages.value = withContext(Dispatchers.IO) { WhatsAppContactPickerLauncher.getInstalledWhatsAppPackages(getApplication()) }
    }
    fun restart(packages: List<String>) { packages.forEach { (getApplication<Application>() as App).restartApp(it) } }
}

@Composable
internal fun ManagerRestartAction() {
    val model: ManagerInstalledApps = androidx.lifecycle.viewmodel.compose.viewModel()
    val packages by model.packages.collectAsStateWithLifecycle()
    var show by remember { mutableStateOf(false) }
    var affected by remember { mutableStateOf<List<String>>(emptyList()) }
    var confirmed by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.refresh() }
    if (packages.isNotEmpty()) IconButton(onClick = { affected = packages; show = true }) {
        Icon(MiuixIcons.Refresh, stringResource(R.string.manager_restart_apps))
    }
    OverlayDialog(show = show, title = stringResource(R.string.manager_restart_apps),
        summary = stringResource(R.string.manager_restart_confirm, affected.joinToString(", ") { WhatsAppContactPickerLauncher.getPackageLabel(it) }),
        onDismissRequest = { show = false }, onDismissFinished = {
            if (confirmed) model.restart(affected)
            confirmed = false
            affected = emptyList()
        }) {
        ManagerDialogActions({ show = false }, stringResource(R.string.manager_restart)) { confirmed = true; show = false }
    }
}
