package com.wmods.wppenhacer.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.AtomicFile
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.WeakHashMap

internal object PreferenceSnapshotCodec {
    private const val TYPE_STRING = "String"
    private const val TYPE_BOOLEAN = "Boolean"
    private const val TYPE_INTEGER = "Integer"
    private const val TYPE_LONG = "Long"
    private const val TYPE_FLOAT = "Float"
    private const val TYPE_DOUBLE = "Double"
    private const val TYPE_ARRAY = "JSONArray"

    fun encode(values: Map<String, *>): JSONObject {
        val result = JSONObject()

        values.forEach { (key, value) ->
            val encoded = JSONObject()
            when (value) {
                is String -> {
                    encoded.put("type", TYPE_STRING)
                    encoded.put("value", value)
                }

                is Boolean -> {
                    encoded.put("type", TYPE_BOOLEAN)
                    encoded.put("value", value)
                }

                is Int -> {
                    encoded.put("type", TYPE_INTEGER)
                    encoded.put("value", value)
                }

                is Long -> {
                    encoded.put("type", TYPE_LONG)
                    encoded.put("value", value)
                }

                is Float -> {
                    encoded.put("type", TYPE_FLOAT)
                    encoded.put("value", value)
                }

                is Set<*> -> {
                    val strings = value.map { it as? String }
                    if (strings.any { it == null }) return@forEach

                    encoded.put("type", TYPE_ARRAY)
                    encoded.put("value", JSONArray(strings))
                }

                else -> return@forEach
            }
            result.put(key, encoded)
        }

        return result
    }

    fun decode(values: JSONObject): Map<String, Any> {
        val result = LinkedHashMap<String, Any>()
        val keys = values.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val encoded = values.optJSONObject(key) ?: continue
            val decoded = decodeValue(encoded) ?: continue
            result[key] = decoded
        }
        return result
    }

    private fun decodeValue(encoded: JSONObject): Any? {
        val type = encoded.optString("type")
        val value = encoded.opt("value")

        return when (type) {
            TYPE_STRING -> value as? String
            TYPE_BOOLEAN -> value as? Boolean
            TYPE_INTEGER -> (value as? Number)?.toInt()
            TYPE_LONG -> (value as? Number)?.toLong()
            TYPE_FLOAT, TYPE_DOUBLE -> (value as? Number)?.toFloat()
            TYPE_ARRAY, "HashSet", "Set" -> decodeStringSet(value)
            else -> null
        }
    }

    private fun decodeStringSet(value: Any?): Set<String>? {
        val array = value as? JSONArray ?: return null
        val result = LinkedHashSet<String>(array.length())
        for (index in 0 until array.length()) {
            val item = array.opt(index) as? String ?: return null
            result += item
        }
        return result
    }
}

internal class SharedPreferencesListenerRegistry {
    // SharedPreferences keeps change listeners weakly, so the registry must retain the listener
    // strongly while the preference instance is registered.
    private val listeners =
        WeakHashMap<SharedPreferences, SharedPreferences.OnSharedPreferenceChangeListener>()

    fun hasRetainedListener(preferences: SharedPreferences): Boolean {
        return listeners[preferences] != null
    }

    fun register(
        preferences: SharedPreferences,
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        if (hasRetainedListener(preferences)) return
        listeners[preferences] = listener
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }
}

internal object PreferenceSnapshot {
    private const val TAG = "WaEnhancerPrefs"
    private const val FILE_NAME = "wae_preferences_snapshot.json"
    private const val SCHEMA_VERSION = 1
    private const val SCHEMA_KEY = "schema_version"
    private const val INITIALIZED_KEY = "initialized"
    private const val VALUES_KEY = "values"

    private val lock = Any()
    private val registeredPreferences = SharedPreferencesListenerRegistry()

    fun initialize(context: Context, preferences: SharedPreferences) {
        val applicationContext = context.applicationContext
        synchronized(lock) {
            if (registeredPreferences.hasRetainedListener(preferences)) return

            val snapshotFile = snapshotFile(applicationContext)
            val currentValues = readCurrentValues(preferences) ?: return
            when (val stored = readSnapshot(snapshotFile)) {
                SnapshotReadResult.Missing -> {
                    writeSnapshot(snapshotFile, currentValues)
                }

                SnapshotReadResult.Invalid -> {
                    if (currentValues.isNotEmpty()) {
                        writeSnapshot(snapshotFile, currentValues)
                    }
                }

                is SnapshotReadResult.Valid -> {
                    if (currentValues.isEmpty() && stored.snapshot.initialized) {
                        if (restore(preferences, stored.snapshot.values)) {
                            writeSnapshot(
                                snapshotFile,
                                readCurrentValues(preferences) ?: emptyMap<String, Any>()
                            )
                        }
                    } else {
                        writeSnapshot(snapshotFile, currentValues)
                    }
                }
            }

            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                syncNow(applicationContext, preferences)
            }
            registeredPreferences.register(preferences, listener)
        }
    }

    @JvmStatic
    fun syncNow(context: Context, preferences: SharedPreferences) {
        val values = readCurrentValues(preferences) ?: return
        synchronized(lock) {
            writeSnapshot(snapshotFile(context.applicationContext), values)
        }
    }

    private fun snapshotFile(context: Context): File {
        return File(context.noBackupFilesDir, FILE_NAME)
    }

    private fun readCurrentValues(preferences: SharedPreferences): Map<String, *>? {
        return try {
            preferences.all.toMap()
        } catch (error: Throwable) {
            Log.w(TAG, "Unable to read active preferences", error)
            null
        }
    }

    @SuppressLint("ApplySharedPref", "UseKtx")
    private fun restore(preferences: SharedPreferences, values: Map<String, Any>): Boolean {
        return try {
            val editor = preferences.edit().clear()
            values.forEach { (key, value) ->
                when (value) {
                    is String -> editor.putString(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Set<*> -> {
                        val stringSet = value.mapNotNull { it as? String }.toSet()
                        if (stringSet.size == value.size) {
                            editor.putStringSet(key, stringSet)
                        }
                    }
                }
            }
            editor.commit()
        } catch (error: Throwable) {
            Log.w(TAG, "Unable to restore settings snapshot", error)
            false
        }
    }

    private fun readSnapshot(file: File): SnapshotReadResult {
        if (!file.exists()) return SnapshotReadResult.Missing

        return try {
            val root = file.inputStream().bufferedReader().use { JSONObject(it.readText()) }
            if (root.optInt(SCHEMA_KEY, -1) != SCHEMA_VERSION) {
                return SnapshotReadResult.Invalid
            }
            val values = root.optJSONObject(VALUES_KEY) ?: return SnapshotReadResult.Invalid
            SnapshotReadResult.Valid(
                Snapshot(
                    initialized = root.optBoolean(INITIALIZED_KEY, false),
                    values = PreferenceSnapshotCodec.decode(values)
                )
            )
        } catch (error: Throwable) {
            Log.w(TAG, "Unable to read settings snapshot", error)
            SnapshotReadResult.Invalid
        }
    }

    @Suppress("DEPRECATION")
    private fun writeSnapshot(file: File, values: Map<String, *>) {
        try {
            file.parentFile?.mkdirs()
            val root = JSONObject()
                .put(SCHEMA_KEY, SCHEMA_VERSION)
                .put(INITIALIZED_KEY, true)
                .put(VALUES_KEY, PreferenceSnapshotCodec.encode(values))
            val atomicFile = AtomicFile(file)
            var output: FileOutputStream? = null
            try {
                output = atomicFile.startWrite()
                output.write(root.toString().toByteArray(Charsets.UTF_8))
                output.fd.sync()
                atomicFile.finishWrite(output)
                output = null
            } catch (error: Throwable) {
                output?.let { atomicFile.failWrite(it) }
                throw error
            }
        } catch (error: Throwable) {
            Log.w(TAG, "Unable to write settings snapshot", error)
        }
    }

    private data class Snapshot(
        val initialized: Boolean,
        val values: Map<String, Any>
    )

    private sealed interface SnapshotReadResult {
        data object Missing : SnapshotReadResult
        data object Invalid : SnapshotReadResult
        data class Valid(val snapshot: Snapshot) : SnapshotReadResult
    }
}
