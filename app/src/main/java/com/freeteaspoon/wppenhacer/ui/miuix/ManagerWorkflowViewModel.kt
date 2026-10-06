package com.freeteaspoon.wppenhacer.ui.miuix

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.freeteaspoon.wppenhacer.BuildConfig
import com.freeteaspoon.wppenhacer.R
import com.freeteaspoon.wppenhacer.utils.RootDiagnostics
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

internal data class ManagerLoadState<T>(
    val content: T? = null,
    val error: String? = null,
    val refreshing: Boolean = false,
)
internal data class ManagerRelease(val tag: String, val notes: String, val url: String)

internal class ManagerWorkflowViewModel(app: Application) : AndroidViewModel(app) {
    private val updateLock = Mutex()
    private val diagnosticsLock = Mutex()
    private val _release = MutableStateFlow(ManagerLoadState<ManagerRelease>())
    val release = _release.asStateFlow()
    private val _diagnostics = MutableStateFlow(ManagerLoadState<List<RootDiagnostics.LogEntry>>())
    val diagnostics = _diagnostics.asStateFlow()

    private val _rootAvailable = MutableStateFlow<Boolean?>(null)
    val rootAvailable = _rootAvailable.asStateFlow()
    private val rootLock = Mutex()
    fun checkRoot(): Job = viewModelScope.launch {
        if (!rootLock.tryLock()) return@launch
        try { _rootAvailable.value = withContext(Dispatchers.IO) { com.topjohnwu.superuser.Shell.getShell().isRoot } }
        finally { rootLock.unlock() }
    }

    private var recordingModeRequest = 0
    fun selectRecordingRootMode(useRoot: Boolean): Job? {
        val request = ++recordingModeRequest
        val app = getApplication<Application>()
        val spec = PreferenceSpec("call_recording_use_root", PreferenceSource.MEDIA, "", "", null, PreferenceKind.SWITCH)
        val controller = PreferenceController(app, androidx.preference.PreferenceManager.getDefaultSharedPreferences(app))
        if (!useRoot) {
            controller.put(spec, false)
            return null
        }
        return viewModelScope.launch { rootLock.withLock {
            val available = withContext(Dispatchers.IO) { com.topjohnwu.superuser.Shell.getShell().isRoot }
            // A completed root check must not overwrite a newer choice of non-root mode.
            if (request != recordingModeRequest) return@withLock
            _rootAvailable.value = available
            if (available) {
                controller.put(spec, true)
            } else ManagerSnackbarEvents.tryShow(app.getString(R.string.manager_root_unavailable))
        } }
    }

    private val recordingsLock = Mutex()
    private val _recordings = MutableStateFlow(ManagerLoadState<List<ManagerRecording>>())
    val recordings = _recordings.asStateFlow()
    private val _recordingOperation = MutableStateFlow(false)
    val recordingOperation = _recordingOperation.asStateFlow()

    fun loadRecordings(userRefresh: Boolean = false): Job = viewModelScope.launch {
        if (!recordingsLock.tryLock()) return@launch
        try {
            _recordings.update { it.copy(refreshing = userRefresh, error = null) }
            val result = withContext(Dispatchers.IO) {
                val app = getApplication<Application>()
                val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(app)
                scanRecordings(prefs.getString("call_recording_path", null), app.getString(R.string.manager_unknown_contact))
            }
            _recordings.value = ManagerLoadState(content = result)
        } catch (cancel: CancellationException) { throw cancel
        } catch (error: Exception) { _recordings.update { it.copy(error = error.message.orEmpty()) }
        } finally { _recordings.update { it.copy(refreshing = false) }; recordingsLock.unlock() }
    }

    fun deleteRecordings(paths: Set<String>): Job = viewModelScope.launch {
        if (!recordingsLock.tryLock()) return@launch
        _recordingOperation.value = true
        try {
            val failed = withContext(Dispatchers.IO) { paths.filter { java.io.File(it).exists() && !java.io.File(it).delete() }.toSet() }
            _recordings.update { previous -> previous.copy(content = previous.content?.filter { it.file.absolutePath !in paths || it.file.absolutePath in failed }) }
            val app = getApplication<Application>()
            ManagerSnackbarEvents.tryShow(if (failed.isEmpty()) app.getString(R.string.manager_recordings_deleted, paths.size)
                else app.getString(R.string.manager_delete_failed))
        } finally { _recordingOperation.value = false; recordingsLock.unlock() }
    }

    fun loadRelease(userRefresh: Boolean = false): Job = viewModelScope.launch {
        if (!updateLock.tryLock()) return@launch
        try {
            _release.update { it.copy(refreshing = userRefresh, error = null) }
            val result = withContext(Dispatchers.IO) {
                OkHttpClient().newCall(Request.Builder().url(BuildConfig.LATEST_RELEASE_API).build()).execute().use { response ->
                    check(response.isSuccessful) { "HTTP ${response.code}" }
                    val json = JSONObject(response.body.string())
                    ManagerRelease(json.getString("tag_name"), json.optString("body"), json.getString("html_url"))
                }
            }
            _release.value = ManagerLoadState(content = result)
        } catch (cancel: CancellationException) { throw cancel
        } catch (error: Exception) {
            _release.update { it.copy(error = error.message ?: getApplication<Application>().getString(R.string.manager_operation_failed, "")) }
        } finally {
            _release.update { it.copy(refreshing = false) }
            updateLock.unlock()
        }
    }

    fun runDiagnostics(userRefresh: Boolean = false): Job = viewModelScope.launch {
        if (!diagnosticsLock.tryLock()) return@launch
        try {
            _diagnostics.update { it.copy(refreshing = userRefresh, error = null) }
            val result = withContext(Dispatchers.IO) { RootDiagnostics.collectDiagnostics(getApplication()) }
            _diagnostics.value = ManagerLoadState(content = result)
        } catch (cancel: CancellationException) { throw cancel
        } catch (error: Exception) { _diagnostics.update { it.copy(error = error.message.orEmpty()) }
        } finally { _diagnostics.update { it.copy(refreshing = false) }; diagnosticsLock.unlock() }
    }
}
