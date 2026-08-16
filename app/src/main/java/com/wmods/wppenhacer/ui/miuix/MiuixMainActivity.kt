package com.wmods.wppenhacer.ui.miuix

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.preference.PreferenceManager
import com.wmods.wppenhacer.App
import com.wmods.wppenhacer.R
import java.io.File
import top.yukonga.miuix.kmp.basic.SnackbarHostState

class MiuixMainActivity : ComponentActivity() {
    private val viewModel: ManagerViewModel by viewModels()
    private val snackbarHostState = SnackbarHostState()

    override fun onCreate(savedInstanceState: Bundle?) {
        App.changeLanguage(this)
        super.onCreate(savedInstanceState)
        initializeLegacyDefaults()
        File(getExternalFilesDir(null), ".nomedia").delete()
        enableEdgeToEdge()
        setContent {
            WaEnhancerManagerApp(
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                onPredictiveBackChange = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    { enabled ->
                        App.setEnableOnBackInvokedCallback(applicationInfo, enabled)
                        recreateWithoutTransition()
                    }
                } else null,
            )
        }
    }

    private fun initializeLegacyDefaults() {
        listOf(
            R.xml.preference_general_home,
            R.xml.preference_general_homescreen,
            R.xml.preference_general_conversation,
            R.xml.fragment_general,
            R.xml.fragment_privacy,
            R.xml.fragment_media,
            R.xml.fragment_customization,
        ).forEach { PreferenceManager.setDefaultValues(this, it, false) }
    }

    @Suppress("DEPRECATION")
    private fun recreateWithoutTransition() {
        overridePendingTransition(0, 0)
        recreate()
        overridePendingTransition(0, 0)
    }
}
