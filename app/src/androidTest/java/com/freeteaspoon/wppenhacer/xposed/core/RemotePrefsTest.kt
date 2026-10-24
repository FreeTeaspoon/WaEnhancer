package com.freeteaspoon.wppenhacer.xposed.core

import androidx.preference.PreferenceManager
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemotePrefsTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val local by lazy { PreferenceManager.getDefaultSharedPreferences(context) }
    private val prefix = "migration_test_"

    @Test
    fun providerFallbackPreservesEveryPreferenceTypeAndRefreshesRemovals() {
        val keys = listOf("boolean", "int", "long", "float", "string", "set").map { prefix + it }
        try {
            assertTrue(local.edit()
                .putBoolean(keys[0], true)
                .putInt(keys[1], 42)
                .putLong(keys[2], Long.MAX_VALUE)
                .putFloat(keys[3], 1.5f)
                .putString(keys[4], "settings")
                .putStringSet(keys[5], setOf("300", "400"))
                .commit())
            val remote = RemotePrefs(context)
            remote.invalidate(remoteSynced = false)
            assertTrue(remote.getBoolean(keys[0], false))
            assertEquals(42, remote.getInt(keys[1], 0))
            assertEquals(Long.MAX_VALUE, remote.getLong(keys[2], 0))
            assertEquals(1.5f, remote.getFloat(keys[3], 0f))
            assertEquals("settings", remote.getString(keys[4], null))
            assertEquals(setOf("300", "400"), remote.getStringSet(keys[5], null))

            assertTrue(local.edit().putBoolean(keys[0], false).remove(keys[4]).commit())
            remote.invalidate(remoteSynced = false)
            assertFalse(remote.getBoolean(keys[0], true))
            assertFalse(remote.contains(keys[4]))
            assertEquals("default", remote.getString(keys[4], "default"))

            @Suppress("UNCHECKED_CAST")
            val copy = remote.getStringSet(keys[5], null) as MutableSet<String>
            copy.clear()
            assertEquals(setOf("300", "400"), remote.getStringSet(keys[5], null))
        } finally {
            local.edit().apply { keys.forEach(::remove) }.commit()
        }
    }

    @Test(expected = UnsupportedOperationException::class)
    fun moduleSettingsAreReadOnlyInTheHost() {
        RemotePrefs(context).edit()
    }
}
