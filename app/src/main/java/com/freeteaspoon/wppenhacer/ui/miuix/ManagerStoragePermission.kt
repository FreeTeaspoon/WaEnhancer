package com.freeteaspoon.wppenhacer.ui.miuix

import android.content.Intent
import android.os.Environment
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.freeteaspoon.wppenhacer.R
import top.yukonga.miuix.kmp.preference.ArrowPreference

@Composable
internal fun rememberManagerStorageAccess(): Boolean {
    var granted by remember { mutableStateOf(Environment.isExternalStorageManager()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = Environment.isExternalStorageManager() }
    return granted
}

@Composable
internal fun ManagerStoragePermissionRow() {
    ManagerGroupCard(Modifier.padding(top = 12.dp)) {
        ManagerStoragePermissionPreference()
    }
}

@Composable
internal fun ManagerStoragePermissionPreference() {
    val context = LocalContext.current
    ArrowPreference(stringResource(R.string.storage_permission), summary = stringResource(R.string.manager_permission_needed), onClick = {
        context.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            "package:${context.packageName}".toUri()))
    })
}
