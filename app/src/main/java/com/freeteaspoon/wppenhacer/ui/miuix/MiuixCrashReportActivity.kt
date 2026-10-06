package com.freeteaspoon.wppenhacer.ui.miuix

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.extended.Copy
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.freeteaspoon.wppenhacer.R
import com.freeteaspoon.wppenhacer.activities.CrashReportActivity
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference

class MiuixCrashReportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val info = intent.getStringExtra(CrashReportActivity.EXTRA_CRASH_INFO).orEmpty()
        val trace = intent.getStringExtra(CrashReportActivity.EXTRA_CRASH_TRACE).orEmpty()
        val report = "$info\n\n$trace"
        val appearance = ManagerAppearanceSettings.from(PreferenceManager.getDefaultSharedPreferences(this).all)
        setContent {
            val host = androidx.compose.runtime.remember { top.yukonga.miuix.kmp.basic.SnackbarHostState() }
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            ManagerTheme(appearance) {
                top.yukonga.miuix.kmp.basic.Scaffold(snackbarHost = { top.yukonga.miuix.kmp.basic.SnackbarHost(state = host) }) { _ ->
                ManagerDetailScaffold(getString(R.string.manager_crash_report), false, onBack = ::finish) {
                    item("summary") { ManagerGroupCard { ArrowPreference(getString(R.string.manager_crash_summary), onClick = null) } }
                    item("actions") { ManagerGroupCard {
                        top.yukonga.miuix.kmp.basic.BasicComponent(title = getString(R.string.copy), endActions = {
                            top.yukonga.miuix.kmp.basic.Icon(top.yukonga.miuix.kmp.icon.MiuixIcons.Copy, null)
                        }, onClick = {
                            getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(getString(R.string.manager_crash_report), report))
                            scope.launch { host.showSnackbar(getString(R.string.manager_copied)) }
                        })
                        ArrowPreference(getString(R.string.share), onClick = {
                            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, report), getString(R.string.share)))
                        })
                    } }
                    item("report") { ManagerGroupCard { SelectionContainer { Text(report, modifier = Modifier.padding(16.dp)) } } }
                }
                }
            }
        }
    }
}
