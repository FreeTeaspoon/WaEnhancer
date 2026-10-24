package com.freeteaspoon.wppenhacer

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.highcapable.yukihookapi.YukiHook
import com.highcapable.yukihookapi.hook.factory.preferences
import com.highcapable.yukihookapi.hook.xposed.preference.YukiHookPreferences
import java.util.concurrent.Executors

/** Mirrors AndroidX settings into the LibXposed remote preference store. */
class RemotePreferencesSync(context: Context) {

    private val appContext = context.applicationContext
    private val localPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)
    private val executor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "WAE-RemotePreferences").apply { isDaemon = true }
    }

    @Volatile
    private var serviceAvailable = false
    // Accessed only by the sync executor. A failed key write needs a full retry.
    private var fullSyncRequired = true

    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { preferences, key ->
            enqueue {
                if (serviceAvailable) {
                    if (key == null || fullSyncRequired) syncAll() else syncKey(preferences, key)
                } else false
            }
        }

    fun start() {
        localPreferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        YukiHook.Status.observeFrameworkService { available ->
            serviceAvailable = available
            enqueue { if (available) syncAll() else false }
        }
    }

    private fun syncAll(): Boolean {
        val values = localPreferences.all
        val remotePreferences = appContext.preferences(PREFERENCES_NAME)
        val staleKeys = remotePreferences.all().keys - values.keys

        val saved = remotePreferences.edit().apply {
            staleKeys.forEach(::remove)
            values.forEach { (key, value) -> putValue(key, value) }
        }.commit()
        if (saved) Log.i(TAG, "Mirrored ${values.size} preferences to LibXposed")
        return saved
    }

    private fun syncKey(preferences: SharedPreferences, key: String): Boolean {
        val remotePreferences = appContext.preferences(PREFERENCES_NAME)
        val editor = remotePreferences.edit()
        if (preferences.contains(key)) {
            val value = preferences.all[key]
            editor.putValue(key, value)
        } else {
            editor.remove(key)
        }
        return editor.commit()
    }

    private fun enqueue(task: () -> Boolean) {
        executor.execute {
            val saved = runCatching(task).onFailure { error ->
                Log.e(TAG, "Failed to mirror preferences to LibXposed", error)
            }.getOrDefault(false)
            fullSyncRequired = !saved
            // The hooked app caches a snapshot; refresh it once the remote store holds the new values.
            appContext.sendBroadcast(Intent(BuildConfig.APPLICATION_ID + ".PREFS_CHANGED").apply {
                putExtra(EXTRA_REMOTE_SYNCED, saved)
            })
        }
    }

    private fun YukiHookPreferences.Editor.putValue(key: String, value: Any?) {
        when (value) {
            is String -> putString(key, value)
            is Boolean -> putBoolean(key, value)
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            is Float -> putFloat(key, value)
            is Set<*> -> putStringSet(key, HashSet(value.filterIsInstance<String>()))
            else -> {
                Log.w(TAG, "Skipping unsupported preference value for key '$key'")
                remove(key)
            }
        }
    }

    companion object {
        const val EXTRA_REMOTE_SYNCED = "remote_synced"
        private const val TAG = "WAE-RemotePrefs"
        private const val PREFERENCES_NAME = BuildConfig.APPLICATION_ID + "_preferences"
    }
}
