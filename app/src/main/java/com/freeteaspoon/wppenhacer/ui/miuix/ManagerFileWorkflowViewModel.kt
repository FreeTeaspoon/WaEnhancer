package com.freeteaspoon.wppenhacer.ui.miuix

import android.app.Application
import android.net.Uri
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.freeteaspoon.wppenhacer.R
import com.freeteaspoon.wppenhacer.utils.PreferenceSnapshotCodec
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import java.io.File
import org.json.JSONObject

internal class ManagerFileWorkflowViewModel(app: Application) : AndroidViewModel(app) {
    private val lock = Mutex()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _themes = MutableStateFlow(ManagerLoadState<List<File>>())
    val themes = _themes.asStateFlow()
    private val _editors = MutableStateFlow<Map<String, ManagerLoadState<String>>>(emptyMap())
    val editors = _editors.asStateFlow()
    private val _created = MutableStateFlow<String?>(null)
    val created = _created.asStateFlow()
    private val drafts = mutableMapOf<String, String>()
    private val prefs = PreferenceManager.getDefaultSharedPreferences(app)
    private val controller = PreferenceController(app, prefs)

    private fun operation(success: Int, block: suspend () -> Unit): Job = viewModelScope.launch {
        if (!lock.tryLock()) return@launch
        _busy.value = true
        try {
            withContext(Dispatchers.IO) { block() }
            ManagerSnackbarEvents.tryShow(getApplication<Application>().getString(success))
        } catch (cancel: CancellationException) { throw cancel
        } catch (error: Exception) {
            ManagerSnackbarEvents.tryShow(getApplication<Application>().getString(R.string.manager_operation_failed, error.message.orEmpty()))
        } finally { _busy.value = false; lock.unlock() }
    }

    fun importPreference(spec: PreferenceSpec, uri: Uri) = operation(R.string.saved) {
        val app = getApplication<Application>()
        val value = (app.contentResolver.openInputStream(uri) ?: error("Cannot open document")).use { stream ->
            if (spec.key == "bootloader_spoofer_xml") {
                stream.bufferedReader().readText()
            } else {
                check(android.os.Environment.isExternalStorageManager()) { app.getString(R.string.manager_permission_denied) }
                val extension = app.contentResolver.getType(uri).orEmpty().substringAfter('/', "bin")
                    .substringBefore('+').filter(Char::isLetterOrDigit).take(16).ifBlank { "bin" }
                val directory = File(com.freeteaspoon.wppenhacer.App.waEnhancerFolder, "files")
                val target = File(directory, "${spec.key}.$extension")
                replaceImportedFile(stream, target)
                target.absolutePath
            }
        }
        controller.put(spec, value)
    }
    fun exportConfiguration(uri: Uri) = operation(R.string.manager_settings_exported) {
        val stream = getApplication<Application>().contentResolver.openOutputStream(uri) ?: error("Cannot open document")
        stream.bufferedWriter().use { it.write(PreferenceSnapshotCodec.encode(prefs.all).toString(2)) }
    }
    fun importConfiguration(uri: Uri) = operation(R.string.manager_settings_imported) {
        val stream = getApplication<Application>().contentResolver.openInputStream(uri) ?: error("Cannot open document")
        val decoded = stream.bufferedReader().use { PreferenceSnapshotCodec.decode(JSONObject(it.readText())) }
        controller.replaceAll(decoded)
    }
    fun resetConfiguration() = operation(R.string.manager_reset_complete) { controller.replaceAll(emptyMap()) }

    fun loadThemes(refresh: Boolean = false): Job = viewModelScope.launch {
        _themes.update { it.copy(refreshing = refresh) }
        try { _themes.value = ManagerLoadState(content = withContext(Dispatchers.IO) { loadThemeFolders() }) }
        catch (cancel: CancellationException) { throw cancel }
        catch (error: Exception) { _themes.update { it.copy(error = error.message.orEmpty()) } }
        finally { _themes.update { it.copy(refreshing = false) } }
    }
    fun createTheme(name: String) = operation(R.string.saved) {
        val folder = themeFolder(name.trim())
        check(folder.mkdirs())
        check(File(folder, "style.css").createNewFile())
        _themes.value = ManagerLoadState(content = loadThemeFolders())
        _created.value = folder.name
    }
    fun consumeCreated() { _created.value = null }
    fun importTheme(uri: Uri) = operation(R.string.manager_settings_imported) {
        importThemeArchive(getApplication(), uri)
        _themes.value = ManagerLoadState(content = loadThemeFolders())
    }
    fun selectDefault() {
        if (_busy.value) return
        prefs.edit { remove("folder_theme"); putString("custom_css", "") }
    }
    fun loadEditor(name: String): Job = viewModelScope.launch {
        if (_editors.value[name]?.content != null) return@launch
        try {
            val css = withContext(Dispatchers.IO) { File(themeFolder(name), "style.css").readText() }
            _editors.update { it + (name to ManagerLoadState(content = drafts[name] ?: css)) }
        } catch (cancel: CancellationException) { throw cancel }
        catch (error: Exception) { _editors.update { it + (name to ManagerLoadState(error = error.message.orEmpty())) } }
    }
    fun updateDraft(name: String, css: String) { drafts[name] = css }
    fun editorContent(name: String, fallback: String): String = drafts[name] ?: fallback
    fun saveEditor(name: String, css: String, apply: Boolean) = operation(R.string.saved) {
        val file = File(themeFolder(name), "style.css")
        val temporary = File(file.parentFile, "style.css.tmp")
        temporary.writeText(css)
        check(temporary.renameTo(file))
        if (apply || prefs.getString("folder_theme", null) == name) prefs.edit {
            putString("folder_theme", name); putString("custom_css", css)
        }
        _editors.update { it + (name to ManagerLoadState(content = css)) }
    }
    fun exportTheme(name: String, uri: Uri) = operation(R.string.exported) {
        exportThemeArchive(getApplication(), themeFolder(name), uri)
    }
}
