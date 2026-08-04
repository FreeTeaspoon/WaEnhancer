package com.wmods.wppenhacer.ui.miuix

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.wmods.wppenhacer.R
import com.wmods.wppenhacer.activities.CrashReportActivity
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference

class MiuixCrashReportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val info = intent.getStringExtra(CrashReportActivity.EXTRA_CRASH_INFO).orEmpty()
        val trace = intent.getStringExtra(CrashReportActivity.EXTRA_CRASH_TRACE).orEmpty()
        val report = "$info\n\n$trace"
        val appearance = ManagerAppearanceSettings.from(PreferenceManager.getDefaultSharedPreferences(this).all)
        setContent {
            ManagerTheme(appearance) {
                ManagerDetailScaffold(getString(R.string.manager_crash_report), false, onBack = ::finish) {
                    item("summary") { ManagerGroupCard { ArrowPreference(getString(R.string.manager_crash_summary), onClick = null) } }
                    item("actions") { ManagerGroupCard {
                        ArrowPreference(getString(R.string.copy), onClick = {
                            getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(getString(R.string.manager_crash_report), report))
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
