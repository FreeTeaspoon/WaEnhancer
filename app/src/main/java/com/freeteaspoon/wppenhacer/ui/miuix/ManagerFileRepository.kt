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
    recoverThemeImports(root)
    return checkNotNull(root.listFiles(File::isDirectory)) { "Unable to read theme directory" }
        .filter { File(it, "style.css").isFile }.sortedBy(File::getName)
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
        commitThemeFolders(root, folders.toList())
    } finally { staging.deleteRecursively() }
}

/** Journaled, same-filesystem renames. An interrupted batch rolls back on the next scan. */
internal fun commitThemeFolders(root: File, folders: List<File>, beforeInstall: (String) -> Unit = {}) {
    recoverThemeImports(root)
    val transaction = java.nio.file.Files.createTempDirectory(root.toPath(), ".waenhancer-import-").toFile()
    java.io.FileOutputStream(File(transaction, "transaction-v1")).use { out ->
        out.write("WaEnhancer theme import".toByteArray()); out.fd.sync()
    }
    val incoming = File(transaction, "new").apply { check(mkdirs()) }
    val backup = File(transaction, "old").apply { check(mkdirs()) }
    try {
        folders.forEach { check(it.copyRecursively(File(incoming, it.name))) }
        // Base64 keeps names containing newlines intact in the journal.
        val names = folders.joinToString("\n") { java.util.Base64.getEncoder().encodeToString(it.name.toByteArray(Charsets.UTF_8)) }
        java.io.FileOutputStream(File(transaction, "names")).use { out -> out.write(names.toByteArray()); out.fd.sync() }
        folders.forEach { folder ->
            val destination = File(root, folder.name)
            if (destination.exists()) check(destination.renameTo(File(backup, folder.name)))
            beforeInstall(folder.name)
            check(File(incoming, folder.name).renameTo(destination))
        }
        java.io.FileOutputStream(File(transaction, "committed")).use { it.fd.sync() }
    } finally { recoverThemeImports(root) }
}

internal fun recoverThemeImports(root: File) {
    val transactions = checkNotNull(root.listFiles { file -> file.isDirectory && file.name.startsWith(".waenhancer-import-") &&
        !File(file, "style.css").exists() && File(file, "transaction-v1").isFile &&
        File(file, "transaction-v1").readText() == "WaEnhancer theme import" }) { "Unable to read theme directory" }
    transactions.forEach { transaction ->
        val journal = File(transaction, "names")
        if (!File(transaction, "committed").exists() && journal.isFile) {
            journal.readLines().filter(String::isNotBlank).forEach { encoded ->
                val name = String(java.util.Base64.getDecoder().decode(encoded), Charsets.UTF_8)
                require(validThemeName(name))
                val destination = File(root, name)
                val old = File(transaction, "old/$name")
                val pending = File(transaction, "new/$name")
                if (old.exists()) {
                    check(pending.parentFile!!.isDirectory || pending.parentFile!!.mkdirs())
                    // Preserve a pending marker before restoring the backup, so recovery can
                    // itself be interrupted and repeated without deleting the restored theme.
                    check(!pending.exists() || pending.deleteRecursively())
                    check(!destination.exists() || destination.renameTo(pending))
                    if (!pending.exists()) check(pending.mkdirs())
                    check(old.renameTo(destination))
                } else if (!pending.exists() && destination.exists()) {
                    pending.parentFile?.mkdirs()
                    check(destination.renameTo(pending))
                }
            }
        }
        // Rename before recursive cleanup. A crash while deleting the journal must
        // never turn a committed or already restored batch back into a rollback.
        val garbage = File(root, ".waenhancer-cleanup-${java.util.UUID.randomUUID()}")
        check(transaction.renameTo(garbage)) { "Unable to finish theme transaction" }
        check(garbage.deleteRecursively()) { "Unable to clean theme transaction" }
    }
}

internal fun parsePreferenceXml(input: InputStream): String {
    val buffer = java.io.ByteArrayOutputStream()
    val chunk = ByteArray(8192)
    while (buffer.size() <= 2 * 1024 * 1024) {
        val count = input.read(chunk, 0, minOf(chunk.size, 2 * 1024 * 1024 + 1 - buffer.size()))
        if (count < 0) break
        buffer.write(chunk, 0, count)
    }
    val bytes = buffer.toByteArray()
    require(bytes.size <= 2 * 1024 * 1024) { "XML document exceeds 2 MB" }
    // Android's DOM factory does not implement every Xerces security feature.
    // Reject declarations before parsing, including UTF-16/32 byte encodings.
    val declarations = bytes.filter { it != 0.toByte() }.toByteArray().toString(Charsets.ISO_8859_1)
    if (declarations.contains("<!DOCTYPE", ignoreCase = true)) throw org.xml.sax.SAXException("Document types are not allowed")
    val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance().apply {
        isExpandEntityReferences = false
    }
    val builder = factory.newDocumentBuilder().apply {
        setEntityResolver { _, _ -> throw org.xml.sax.SAXException("External entities are not allowed") }
    }
    val document = builder.parse(bytes.inputStream())
    if (document.doctype != null) throw org.xml.sax.SAXException("Document types are not allowed")
    document.documentElement.normalize()
    val writer = java.io.StringWriter()
    javax.xml.transform.TransformerFactory.newInstance().newTransformer().transform(
        javax.xml.transform.dom.DOMSource(document), javax.xml.transform.stream.StreamResult(writer))
    return writer.toString()
}

internal fun validateOutputDirectory(path: String) {
    require(path.startsWith("/") && !path.contains('\u0000')) { "Enter an absolute directory path" }
    val directory = File(path)
    require(directory.isDirectory && directory.canRead()) { "Select an existing readable directory" }
    val probe = File.createTempFile(".waenhancer-", ".tmp", directory)
    check(probe.delete()) { "Unable to write to this directory" }
}
