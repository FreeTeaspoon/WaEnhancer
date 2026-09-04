package com.wmods.wppenhacer.ui.miuix

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import com.wmods.wppenhacer.App
import com.wmods.wppenhacer.BuildConfig
import com.wmods.wppenhacer.xposed.utils.Utils
import com.wmods.wppenhacer.utils.PreferenceSnapshot

internal class PreferenceController(
    private val context: Context,
    private val preferences: SharedPreferences,
) {
    fun put(spec: PreferenceSpec, value: Any) {
        preferences.edit(commit = spec.key == ManagerAppearanceSettings.KEY_FORCE_ENGLISH) {
            when (spec.kind) {
                PreferenceKind.SWITCH -> putBoolean(spec.key, value as Boolean)
                PreferenceKind.MULTI_LIST -> @Suppress("UNCHECKED_CAST") putStringSet(spec.key, value as Set<String>)
                PreferenceKind.INTEGER_SLIDER, PreferenceKind.COLOR -> putInt(spec.key, (value as Number).toInt())
                PreferenceKind.FLOAT_SLIDER -> putFloat(spec.key, (value as Number).toFloat())
                else -> putString(spec.key, value.toString())
            }
        }
        normalizeDependentValues(spec.key)
        dispatchSideEffects(spec.key)
    }

    fun putManagerBoolean(key: String, value: Boolean, restart: Boolean = false) {
        preferences.edit(commit = restart) { putBoolean(key, value) }
        if (restart) Utils.doRestart(context)
    }
    fun putManagerFloat(key: String, value: Float) = preferences.edit { putFloat(key, normalizeScale(value)) }
    fun putManagerString(key: String, value: String) = preferences.edit { putString(key, value) }

    fun replaceAll(values: Map<String, Any>) {
        preferences.edit(commit = true) {
            clear()
            values.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                    is Set<*> -> putStringSet(key, value.filterIsInstance<String>().toSet())
                }
            }
        }
        PreferenceSnapshot.syncNow(context, preferences)
        context.sendBroadcast(Intent(BuildConfig.APPLICATION_ID + ".MANUAL_RESTART"))
        (context.applicationContext as? App)?.apply {
            restartApp("com.whatsapp")
            restartApp("com.whatsapp.w4b")
        }
    }

    fun isEnabled(spec: PreferenceSpec, values: Map<String, *>): Boolean {
        if (!spec.enabledByXml) return false
        spec.dependency?.let { if (values[it] != true) return false }
        val bool: (String) -> Boolean = { values[it] as? Boolean ?: false }
        val string: (String, String) -> String = { key, fallback -> values[key] as? String ?: fallback }
        return when (spec.key) {
            "changecolor_mode" -> bool("changecolor") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            "primary_color", "background_color", "text_color" ->
                bool("changecolor") && string("changecolor_mode", "manual") != "monet"
            "wae_color_preset" -> string("wae_color_mode", "preset") != "monet"
            "oldstatus" -> !bool("igstatus")
            "verticalstatus", "channels", "status_style", "igstatus" -> !bool("oldstatus")
            "removechannel_rec" -> !bool("oldstatus") && !bool("channels")
            "show_freezeLastSeen", "showonlinetext", "dotonline" -> !bool("freezelastseen")
            "filtergroups" -> !bool("separategroups")
            "separategroups" -> !bool("filtergroups")
            "call_block_contacts" -> string("call_privacy", "0") == "3"
            "call_white_contacts" -> string("call_privacy", "0") == "4"
            else -> true
        }
    }

    private fun normalizeDependentValues(changedKey: String) {
        val editor = preferences.edit()
        var changed = false
        fun clearBoolean(key: String) {
            if (preferences.getBoolean(key, false)) {
                editor.putBoolean(key, false)
                changed = true
            }
        }
        when (changedKey) {
            "freezelastseen" -> if (preferences.getBoolean("freezelastseen", false)) {
                clearBoolean("show_freezeLastSeen")
                clearBoolean("showonlinetext")
                clearBoolean("dotonline")
            }
            "separategroups" -> if (preferences.getBoolean("separategroups", false)) clearBoolean("filtergroups")
            "filtergroups" -> if (preferences.getBoolean("filtergroups", false)) clearBoolean("separategroups")
            "igstatus" -> if (preferences.getBoolean("igstatus", false)) clearBoolean("oldstatus")
            "oldstatus" -> if (preferences.getBoolean("oldstatus", false)) {
                clearBoolean("verticalstatus")
                clearBoolean("channels")
                clearBoolean("removechannel_rec")
                clearBoolean("igstatus")
            }
            "channels" -> if (preferences.getBoolean("channels", false)) clearBoolean("removechannel_rec")
        }
        if (changed) editor.apply()
    }

    private fun dispatchSideEffects(key: String) {
        if (key == ManagerAppearanceSettings.KEY_THEME_MODE) {
            App.setThemeMode(preferences.getString(key, "0")?.toIntOrNull() ?: 0)
        }
        if (key == ManagerAppearanceSettings.KEY_FORCE_ENGLISH) {
            Utils.doRestart(context)
            return
        }
        if (!key.startsWith("manager_ui_")) {
            context.sendBroadcast(Intent(BuildConfig.APPLICATION_ID + ".MANUAL_RESTART"))
        }
    }
}
