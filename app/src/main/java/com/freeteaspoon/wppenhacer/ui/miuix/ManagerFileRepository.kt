package com.freeteaspoon.wppenhacer.ui.miuix

import android.content.Context
import android.net.Uri
import com.freeteaspoon.wppenhacer.preference.ThemePreference
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal fun replaceImportedFile(input: InputStream, target: File) {
    val directory = requireNotNull(target.parentFile)
    check(directory.isDirectory || directory.mkdirs())
    val temporary = File.createTempFile("import-", ".tmp", directory)
    try {
        temporary.outputStream().use(input::copyTo)
        check(temporary.renameTo(target)) { "Unable to replace the imported file" }
    } finally {
        temporary.delete()
    }
}

internal fun validThemeName(name: String): Boolean = name.isNotBlank() &&
    name.trim() !in setOf(".", "..") && name.none { it.isISOControl() || it in "\\/:*?\"<>|" }

internal fun themeFolder(name: String): File {
    require(validThemeName(name))
    val root = ThemePreference.rootDirectory.canonicalFile
    return File(root, name).canonicalFile.also { require(it.parentFile == root) }
}

internal fun loadThemeFolders(): List<File> {
    val root = ThemePreference.rootDirectory
    check(root.isDirectory || root.mkdirs())
    return root.listFiles(File::isDirectory)?.filter { File(it, "style.css").isFile }
        .orEmpty().sortedBy(File::getName)
}

internal fun exportThemeArchive(context: Context, folder: File, uri: Uri) {
    val root = folder.canonicalFile
    val stream = context.contentResolver.openOutputStream(uri) ?: error("Unable to open the selected document")
    stream.use { output -> ZipOutputStream(output).use { zip ->
        root.walkTopDown().filter(File::isFile).forEach { file ->
            require(file.canonicalPath.startsWith(root.path + File.separator))
            zip.putNextEntry(ZipEntry("${folder.name}/${file.relativeTo(root).invariantSeparatorsPath}"))
            file.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
        }
    } }
}

internal fun safeArchivePath(name: String): Boolean = name.isNotBlank() &&
    !name.startsWith('/') && !name.contains('\\') && !name.contains('\u0000') &&
    name.split('/').none { it == ".." || it == "." } && !name.contains(':')

internal fun importThemeArchive(context: Context, uri: Uri) {
    val root = ThemePreference.rootDirectory.canonicalFile.apply { mkdirs() }
    val staging = File(context.cacheDir, "theme-import-${System.nanoTime()}").apply { mkdirs() }
    val fallback = "imported_theme_${System.currentTimeMillis()}"
    try {
        val stream = context.contentResolver.openInputStream(uri) ?: error("Unable to open the selected document")
        var totalBytes = 0L
        var count = 0
        stream.use { input -> ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                require(++count <= 1000 && safeArchivePath(entry.name)) { "Invalid theme archive" }
                val relative = if ('/' in entry.name) entry.name else "$fallback/${entry.name}"
                val target = File(staging, relative).canonicalFile
                require(target.path.startsWith(staging.canonicalPath + File.separator))
                if (entry.isDirectory) target.mkdirs() else {
                    target.parentFile?.mkdirs()
                    target.outputStream().use { out ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            val bytes = zip.read(buffer)
                            if (bytes < 0) break
                            totalBytes += bytes
                            require(totalBytes <= 32L * 1024 * 1024) { "Theme archive exceeds 32 MB" }
                            out.write(buffer, 0, bytes)
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        } }
        val folders = staging.listFiles(File::isDirectory).orEmpty()
        require(folders.isNotEmpty() && folders.all { validThemeName(it.name) && File(it, "style.css").isFile }) { "No valid theme in archive" }
        // Validate the complete archive before overwriting any installed theme.
        folders.forEach { folder ->
            val destination = themeFolder(folder.name)
            check(folder.copyRecursively(destination, overwrite = true))
        }
    } finally { staging.deleteRecursively() }
}
