package com.freeteaspoon.wppenhacer.ui.miuix

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.preference.PreferenceManager
import com.freeteaspoon.wppenhacer.App
import com.freeteaspoon.wppenhacer.R
import java.io.File
import top.yukonga.miuix.kmp.basic.SnackbarHostState

class MiuixMainActivity : ComponentActivity() {
    private val viewModel: ManagerViewModel by viewModels()
    private val snackbarHostState = SnackbarHostState()
    private var appliedPredictiveBack = false

    override fun onCreate(savedInstanceState: Bundle?) {
        App.changeLanguage(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            appliedPredictiveBack = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(ManagerAppearanceSettings.KEY_PREDICTIVE_BACK, false)
            App.setEnableOnBackInvokedCallback(applicationInfo, appliedPredictiveBack)
        }
        super.onCreate(savedInstanceState)
        initializeLegacyDefaults()
        File(getExternalFilesDir(null), ".nomedia").delete()
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            LaunchedEffect(state.appearance.predictiveBack) {
                applyPredictiveBack(state.appearance.predictiveBack)
            }
            WaEnhancerManagerApp(
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                onPredictiveBackChange = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ::applyPredictiveBack
                } else null,
            )
        }
    }

    private fun applyPredictiveBack(enabled: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || appliedPredictiveBack == enabled) return
        appliedPredictiveBack = enabled
        App.setEnableOnBackInvokedCallback(applicationInfo, enabled)
        recreateWithoutTransition()
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
