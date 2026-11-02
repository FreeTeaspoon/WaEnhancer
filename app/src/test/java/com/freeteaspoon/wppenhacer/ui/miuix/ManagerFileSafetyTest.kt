package com.freeteaspoon.wppenhacer.ui.miuix

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files

class ManagerFileSafetyTest {
    @Test fun interruptedFileImportPreservesExistingFile() {
        val directory = Files.createTempDirectory("manager-import").toFile()
        try {
            val target = File(directory, "wallpaper_file.png").apply { writeText("existing wallpaper") }
            val failingInput = object : InputStream() {
                var reads = 0
                override fun read(): Int = if (reads++ < 4) 'x'.code else throw IOException("Interrupted")
            }
            assertThrows(IOException::class.java) { replaceImportedFile(failingInput, target) }
            assertEquals("existing wallpaper", target.readText())
            assertEquals(listOf(target.name), directory.list()?.toList())
            replaceImportedFile("new wallpaper".byteInputStream(), target)
            assertEquals("new wallpaper", target.readText())
        } finally { directory.deleteRecursively() }
    }
    @Test fun archiveCannotEscapeThemeDirectory() {
        listOf("../style.css", "/tmp/theme/style.css", "Green/../../style.css", "C:/theme/style.css", "Green\\style.css", "Green/./style.css").forEach {
            assertFalse(it, safeArchivePath(it))
        }
        assertTrue(safeArchivePath("Green/assets/icon.png"))
        assertTrue(safeArchivePath("Green/style.css"))
    }
    @Test fun themeNamesKeepExistingNamesAndRejectPaths() {
        listOf("Green theme", "a.b", "主题", "a".repeat(100)).forEach { assertTrue(validThemeName(it)) }
        listOf("", " ", ".", "..", " .. ", " . ", "../theme", "theme/file", "theme\u0000").forEach { assertFalse(it, validThemeName(it)) }
    }
    @Test fun searchOpensDependentFeaturesWithoutChangingTheirStoredValues() {
        val master = PreferenceSpec("wallpaper", PreferenceSource.CUSTOMIZE, "Custom", "Wallpaper", null, PreferenceKind.SWITCH)
        val child = master.copy(key = "wallpaper_file", kind = PreferenceKind.FILE, dependency = master.key)
        val specs = listOf(master, child)
        assertEquals(ManagerRoute.PreferenceFeature(PreferenceSource.CUSTOMIZE, "wallpaper", "wallpaper_file"), preferenceRoute(specs, child))
        assertEquals(ManagerRoute.PreferenceFeature(PreferenceSource.CUSTOMIZE, "wallpaper", "wallpaper"), preferenceRoute(specs, master))
    }
    @Test fun failedThemeBatchRestoresEveryExistingTheme() {
        val root = Files.createTempDirectory("theme-root").toFile()
        val staging = Files.createTempDirectory("theme-stage").toFile()
        try {
            val existing = File(root, "Green").apply { mkdirs(); File(this, "style.css").writeText("old") }
            val replacement = File(staging, "Green").apply { mkdirs(); File(this, "style.css").writeText("new") }
            val added = File(staging, "Blue").apply { mkdirs(); File(this, "style.css").writeText("blue") }
            assertThrows(IOException::class.java) {
                commitThemeFolders(root, listOf(replacement, added)) { if (it == "Blue") throw IOException("disk full") }
            }
            assertEquals("old", File(existing, "style.css").readText())
            assertFalse(File(root, "Blue").exists())
            assertEquals(listOf("Green"), root.list()?.toList())
            commitThemeFolders(root, listOf(replacement, added))
            assertEquals("new", File(existing, "style.css").readText())
            assertEquals("blue", File(root, "Blue/style.css").readText())
        } finally { root.deleteRecursively(); staging.deleteRecursively() }
    }

    @Test fun scanRecoveryRollsBackAnInterruptedThemeRename() {
        val root = Files.createTempDirectory("theme-recovery").toFile()
        try {
            val transaction = File(root, ".waenhancer-import-interrupted").apply { mkdirs() }
            File(transaction, "transaction-v1").writeText("WaEnhancer theme import")
            File(transaction, "old/Green").apply { mkdirs(); File(this, "style.css").writeText("saved") }
            File(root, "Green").apply { mkdirs(); File(this, "style.css").writeText("partial") }
            File(transaction, "names").writeText(java.util.Base64.getEncoder().encodeToString("Green".toByteArray()))
            recoverThemeImports(root)
            assertEquals("saved", File(root, "Green/style.css").readText())
            assertFalse(transaction.exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun repeatedRecoveryKeepsAnAlreadyRestoredTheme() {
        val root = Files.createTempDirectory("theme-repeated-recovery").toFile()
        try {
            val transaction = File(root, ".waenhancer-import-interrupted").apply { mkdirs() }
            File(transaction, "transaction-v1").writeText("WaEnhancer theme import")
            File(transaction, "names").writeText(java.util.Base64.getEncoder().encodeToString("Green".toByteArray()))
            File(transaction, "new/Green").mkdirs()
            File(root, "Green").apply { mkdirs(); File(this, "style.css").writeText("restored") }
            recoverThemeImports(root)
            recoverThemeImports(root)
            assertEquals("restored", File(root, "Green/style.css").readText())
        } finally { root.deleteRecursively() }
    }

    @Test fun recoveryPreservesUserThemesWithTransactionLikeNames() {
        val root = Files.createTempDirectory("theme-names").toFile()
        try {
            listOf(".import-green", ".waenhancer-import-blue").forEach { name ->
                File(root, name).apply { mkdirs(); File(this, "style.css").writeText(name) }
            }
            recoverThemeImports(root)
            assertEquals(2, root.list()?.size)
            assertEquals(".import-green", File(root, ".import-green/style.css").readText())
        } finally { root.deleteRecursively() }
    }

    @Test fun xmlImportRejectsMalformedDocumentsAndExternalEntities() {
        assertTrue(parsePreferenceXml("<map><string name=\"device\">test</string></map>".byteInputStream()).contains("test"))
        assertThrows(org.xml.sax.SAXException::class.java) { parsePreferenceXml("<map>".byteInputStream()) }
        assertThrows(org.xml.sax.SAXException::class.java) {
            parsePreferenceXml("<!DOCTYPE map [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><map>&x;</map>".byteInputStream())
        }
    }

    @Test fun xmlImportRejectsWideEncodedDeclarationsAndOversizedDocuments() {
        val malicious = "<!DOCTYPE map [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><map>&x;</map>"
        listOf(Charsets.UTF_16, Charsets.UTF_16LE, Charsets.UTF_16BE).forEach { encoding ->
            assertThrows(org.xml.sax.SAXException::class.java) { parsePreferenceXml(malicious.byteInputStream(encoding)) }
        }
        assertThrows(IllegalArgumentException::class.java) { parsePreferenceXml(ByteArray(2 * 1024 * 1024 + 1).inputStream()) }
    }

    @Test fun directoriesMustExistAndBeWritable() {
        val root = Files.createTempDirectory("output-path").toFile()
        try {
            validateOutputDirectory(root.absolutePath)
            assertEquals(0, root.list()?.size)
            assertThrows(IllegalArgumentException::class.java) { validateOutputDirectory(File(root, "missing").absolutePath) }
            assertThrows(IllegalArgumentException::class.java) { validateOutputDirectory("relative/path") }
        } finally { root.deleteRecursively() }
    }

}
