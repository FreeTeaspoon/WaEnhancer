package com.wmods.wppenhacer.ui.miuix

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class ManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(application)
    private val specs = PreferenceRegistry.build(application)
    val controller = PreferenceController(application, preferences)

    private val mutableState = MutableStateFlow(buildState())
    val state: StateFlow<ManagerUiState> = mutableState.asStateFlow()

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        mutableState.value = buildState()
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    private fun buildState(): ManagerUiState {
        val values = preferences.all.toMap()
        return ManagerUiState(
            preferences = values,
            specs = specs,
            appearance = ManagerAppearanceSettings.from(values),
        )
    }

    override fun onCleared() {
        preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        super.onCleared()
    }
}
