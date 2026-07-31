package com.wmods.wppenhacer.utils

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy
import java.util.WeakHashMap

class SharedPreferencesListenerRegistryTest {
    @Test
    fun retainsListenerForWeakSharedPreferencesImplementations() {
        val registry = SharedPreferencesListenerRegistry()
        val preferences = WeakPreferenceStore()
        var callbackCount = 0

        registry.register(
            preferences.preferences,
            SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> callbackCount++ }
        )

        assertTrue(registry.hasRetainedListener(preferences.preferences))

        preferences.dispatchChange("hide_home_camera")

        assertEquals(1, callbackCount)
    }

    private class WeakPreferenceStore {
        private val listeners =
            WeakHashMap<SharedPreferences.OnSharedPreferenceChangeListener, Boolean>()

        lateinit var preferences: SharedPreferences

        init {
            preferences = Proxy.newProxyInstance(
                SharedPreferences::class.java.classLoader,
                arrayOf(SharedPreferences::class.java)
            ) { _, method, args ->
                when (method.name) {
                    "registerOnSharedPreferenceChangeListener" -> {
                        listeners[args!!.single() as SharedPreferences.OnSharedPreferenceChangeListener] = true
                        null
                    }

                    "unregisterOnSharedPreferenceChangeListener" -> {
                        listeners.remove(args!!.single() as SharedPreferences.OnSharedPreferenceChangeListener)
                        null
                    }

                    "hashCode" -> System.identityHashCode(preferences)
                    "equals" -> args?.singleOrNull() === preferences
                    "toString" -> "WeakPreferenceStore"
                    else -> error("Unexpected SharedPreferences method: ${method.name}")
                }
            } as SharedPreferences
        }

        fun dispatchChange(key: String) {
            listeners.keys.toList().forEach { listener ->
                listener.onSharedPreferenceChanged(preferences, key)
            }
        }
    }
}
