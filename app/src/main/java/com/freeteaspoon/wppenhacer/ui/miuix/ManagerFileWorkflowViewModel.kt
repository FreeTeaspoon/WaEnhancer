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
import kotlinx.coroutines.sync.withLock
import java.io.File
import org.json.JSONObject

internal class ManagerFileWorkflowViewModel(app: Application) : AndroidViewModel(app) {
    private val lock = Mutex()
    val operation = ManagerOperation()
    val busy = operation.busy
    private val draftLock = Mutex()
    private val _template = MutableStateFlow<String?>(null)
    val template = _template.asStateFlow()
    private val _themes = MutableStateFlow(ManagerLoadState<List<File>>())
    val themes = _themes.asStateFlow()
    private val _editors = MutableStateFlow<Map<String, ManagerLoadState<String>>>(emptyMap())
    val editors = _editors.asStateFlow()
    private val _created = MutableStateFlow<String?>(null)
    val created = _created.asStateFlow()
    private val drafts = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val prefs = PreferenceManager.getDefaultSharedPreferences(app)
    private val controller = PreferenceController(app, prefs)

    private fun operation(success: Int, label: Int, block: suspend () -> Unit): Job = viewModelScope.launch {
        lock.withLock {
            operation.run(getApplication<Application>().getString(label)) {
                try {
                    withContext(Dispatchers.IO) { block() }
                    ManagerSnackbarEvents.tryShow(getApplication<Application>().getString(success))
                } catch (cancel: CancellationException) { throw cancel
                } catch (error: Exception) {
                    ManagerSnackbarEvents.tryShow(getApplication<Application>().getString(R.string.manager_operation_failed, error.message.orEmpty()))
                }
            }
        }
    }

    fun importPreference(spec: PreferenceSpec, uri: Uri) = operation(R.string.saved, R.string.manager_importing_file) {
        val app = getApplication<Application>()
        val value = (app.contentResolver.openInputStream(uri) ?: error("Cannot open document")).use { stream ->
            if (spec.key == "bootloader_spoofer_xml") {
                parsePreferenceXml(stream)
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
    fun exportConfiguration(uri: Uri) = operation(R.string.manager_settings_exported, R.string.manager_exporting_settings) {
        val stream = getApplication<Application>().contentResolver.openOutputStream(uri) ?: error("Cannot open document")
        stream.bufferedWriter().use { it.write(PreferenceSnapshotCodec.encode(prefs.all).toString(2)) }
    }
    fun importConfiguration(uri: Uri) = operation(R.string.manager_settings_imported, R.string.manager_importing_settings) {
        val stream = getApplication<Application>().contentResolver.openInputStream(uri) ?: error("Cannot open document")
        val decoded = stream.bufferedReader().use { PreferenceSnapshotCodec.decode(JSONObject(it.readText())) }
        controller.replaceAll(decoded)
    }
    fun resetConfiguration() = operation(R.string.manager_reset_complete, R.string.manager_resetting_settings) { controller.replaceAll(emptyMap()) }

    fun loadThemes(refresh: Boolean = false): Job = viewModelScope.launch { lock.withLock {
        _themes.update { it.copy(refreshing = refresh) }
        try { _themes.value = ManagerLoadState(content = withContext(Dispatchers.IO) { loadThemeFolders() }) }
        catch (cancel: CancellationException) { throw cancel }
        catch (error: Exception) { _themes.update { it.copy(error = error.message.orEmpty()) } }
        finally { _themes.update { it.copy(refreshing = false) } }
    } }
    fun createTheme(name: String) = operation(R.string.saved, R.string.manager_creating_theme) {
        val folder = themeFolder(name.trim())
        check(folder.mkdirs())
        check(File(folder, "style.css").createNewFile())
        _themes.value = ManagerLoadState(content = loadThemeFolders())
        _created.value = folder.name
    }
    fun consumeCreated() { _created.value = null }
    fun importTheme(uri: Uri) = operation(R.string.manager_settings_imported, R.string.manager_importing_theme) {
        importThemeArchive(getApplication(), uri)
        _themes.value = ManagerLoadState(content = loadThemeFolders())
    }
    fun selectDefault() {
        if (busy.value) return
        prefs.edit { remove("folder_theme"); putString("custom_css", "") }
    }
    fun loadEditor(name: String): Job = viewModelScope.launch {
        if (_editors.value[name]?.content != null) return@launch
        try {
            val css = withContext(Dispatchers.IO) {
                if (_template.value == null) _template.value = getApplication<Application>().assets.open("css_editor.html").bufferedReader().use { it.readText() }
                val draft = draftFile(name)
                if (draft.isFile) draft.readText() else File(themeFolder(name), "style.css").readText()
            }
            _editors.update { it + (name to ManagerLoadState(content = drafts[name] ?: css)) }
        } catch (cancel: CancellationException) { throw cancel }
        catch (error: Exception) { _editors.update { it + (name to ManagerLoadState(error = error.message.orEmpty())) } }
    }
    private fun draftFile(name: String): File {
        val hash = java.security.MessageDigest.getInstance("SHA-256").digest(name.toByteArray()).joinToString("") { "%02x".format(it) }
        return File(getApplication<Application>().filesDir, "editor-drafts/$hash.css")
    }
    fun updateDraft(name: String, css: String) {
        drafts[name] = css
        // Once accepted, finish the private draft write even if the route is popped.
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            withContext(NonCancellable) {
                draftLock.withLock {
                    try { withContext(Dispatchers.IO) { replaceImportedFile(css.byteInputStream(), draftFile(name)) } }
                    catch (error: Exception) {
                        ManagerSnackbarEvents.tryShow(getApplication<Application>().getString(R.string.manager_operation_failed, error.message.orEmpty()))
                    }
                }
            }
        }
    }
    fun editorContent(name: String, fallback: String): String = drafts[name] ?: fallback
    fun saveEditor(name: String, css: String, apply: Boolean) = operation(R.string.saved, R.string.manager_saving_theme) {
        val file = File(themeFolder(name), "style.css")
        val temporary = File(file.parentFile, "style.css.tmp")
        temporary.writeText(css)
        check(temporary.renameTo(file))
        if (apply || prefs.getString("folder_theme", null) == name) prefs.edit {
            putString("folder_theme", name); putString("custom_css", css)
        }
        draftLock.withLock { draftFile(name).delete() }
        _editors.update { it + (name to ManagerLoadState(content = css)) }
    }
    fun exportTheme(name: String, uri: Uri) = operation(R.string.exported, R.string.manager_exporting_theme) {
        exportThemeArchive(getApplication(), themeFolder(name), uri)
    }
    fun saveDirectory(spec: PreferenceSpec, path: String) = operation(R.string.saved, R.string.manager_checking_directory) {
        validateOutputDirectory(path)
        controller.put(spec, path)
    }
    fun importImage(name: String, fileName: String, uri: Uri) = operation(R.string.saved, R.string.manager_importing_image) {
        require(fileName.endsWith(".png", ignoreCase = true) && validThemeName(fileName)) { "Use a PNG file name" }
        val folder = themeFolder(name)
        val destination = File(folder, fileName).canonicalFile
        require(destination.parentFile == folder)
        val app = getApplication<Application>()
        val image = (app.contentResolver.openInputStream(uri) ?: error("Cannot open image")).use { android.graphics.BitmapFactory.decodeStream(it) }
            ?: error("Unable to decode image")
        try {
            val temporary = File.createTempFile("image-", ".tmp", folder)
            try {
                temporary.outputStream().use { check(image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) }
                check(temporary.renameTo(destination))
            } finally { temporary.delete() }
        } finally { image.recycle() }
    }

}
