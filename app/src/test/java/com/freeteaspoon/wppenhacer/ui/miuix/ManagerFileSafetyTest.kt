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
}
