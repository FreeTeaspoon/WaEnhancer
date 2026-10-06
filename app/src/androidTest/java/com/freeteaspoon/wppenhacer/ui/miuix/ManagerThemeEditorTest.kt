package com.freeteaspoon.wppenhacer.ui.miuix

import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.preference.PreferenceManager
import androidx.test.platform.app.InstrumentationRegistry
import com.freeteaspoon.wppenhacer.R
import com.freeteaspoon.wppenhacer.preference.ThemePreference
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ManagerThemeEditorTest {
    @get:Rule val rule = createAndroidComposeRule<MiuixMainActivity>()

    // Set the appop before launching instrumentation. Changing it during a test
    // kills the app process on Android, including when changed through Settings.
    @Test fun themeCreationRendersSavesAppliesAndReopensCss() {
        assertTrue("Grant all-files access before this suite", android.os.Environment.isExternalStorageManager())
        val name = "WaEnhancer editor regression"
        val folder = File(ThemePreference.rootDirectory, name)
        check(!folder.exists())
        val css = "/* <>& \"quoted\" */\nbody { color: #123456; }"
        val prefs = PreferenceManager.getDefaultSharedPreferences(rule.activity)
        val keys = setOf("custom_filters", "folder_theme", "custom_css")
        val previous = prefs.all.filterKeys { it in keys }
        try {
            rule.runOnIdle { prefs.edit().putBoolean("custom_filters", true).commit() }
            rule.onNodeWithTag(primaryNavigationTag(1)).performClick()
            rule.onNodeWithTag("features-search", true).performTextInput("custom_filters")
            rule.onNodeWithText(PreferenceRegistry.build(rule.activity).first { it.key == "custom_filters" }.title).performClick()
            rule.onNodeWithText(text(R.string.theme_manager)).performClick()
            rule.onNodeWithText(text(R.string.create_new_theme)).performClick()
            val input = hasSetTextAction() and hasAnyAncestor(isDialog())
            for (invalid in listOf("", "../escape", " .. ")) {
                rule.onNode(input, useUnmergedTree = true).performTextReplacement(invalid)
                rule.onNodeWithText(text(R.string.manager_save)).assertIsNotEnabled()
            }
            rule.onNode(input, useUnmergedTree = true).performTextReplacement(name)
            rule.onNode(input, useUnmergedTree = true).performImeAction()
            waitForEditor()
            evaluate("document.getElementById('code').value = ${JSONObject.quote(css)}; document.getElementById('code').dispatchEvent(new Event('input', {bubbles:true}));")
            assertRendered(css)
            snapshot("theme-visible-css")
            rule.activityRule.scenario.recreate()
            waitForEditor()
            assertRendered(css)
            snapshot("theme-restored-draft")
            rule.onNodeWithContentDescription(text(R.string.manager_save), useUnmergedTree = true).performTouchInput { click() }
            rule.waitUntil(15000) { File(folder, "style.css").readText() == css }
            rule.onNodeWithContentDescription(text(R.string.manager_more), useUnmergedTree = true).performClick()
            rule.onNodeWithText(text(R.string.manager_apply)).performClick()
            rule.waitUntil(15000) { prefs.getString("folder_theme", null) == name && prefs.getString("custom_css", null) == css }
            back()
            rule.onNodeWithText(name).assertIsDisplayed()
            rule.onNodeWithText(text(R.string.manager_selected)).assertIsDisplayed()
            snapshot("theme-selected")
            rule.onNodeWithText(name).performClick()
            waitForEditor()
            assertRendered(css)
            snapshot("theme-reopened-visible-css")
            rule.onNodeWithContentDescription(text(R.string.manager_more), useUnmergedTree = true).performClick()
            rule.onNodeWithText(text(R.string.clear)).performClick()
            rule.onNodeWithText(text(android.R.string.cancel)).performClick()
            assertEquals(css, evaluateString("getTextareaContent()"))
            rule.onNodeWithContentDescription(text(R.string.manager_more), useUnmergedTree = true).performClick()
            rule.onNodeWithText(text(R.string.clear)).performClick()
            rule.onNode(hasText(text(R.string.clear)) and hasClickAction()).performClick()
            rule.waitUntil(10000) { evaluateString("getTextareaContent()").isEmpty() }
            assertEquals("", evaluateString("getTextareaContent()"))
            rule.waitUntil(10000) { evaluateString("document.querySelector('pre.prism-live code').textContent").isBlank() }
            // Clear edits the draft. The saved file remains until Save is chosen.
            assertEquals(css, File(folder, "style.css").readText())
        } finally {
            folder.deleteRecursively()
            rule.runOnIdle {
                prefs.edit().apply {
                    keys.forEach(::remove)
                    previous.forEach { (key, value) -> when (value) {
                        is Boolean -> putBoolean(key, value)
                        is String -> putString(key, value)
                    } }
                }.commit()
            }
        }
    }

    private fun text(id: Int) = rule.activity.getString(id)
    private fun back() {
        rule.onAllNodesWithContentDescription(text(R.string.manager_back), useUnmergedTree = true).onLast().performClick()
        rule.waitForIdle()
    }

    private fun findEditor(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            findEditor(view.getChildAt(index))?.let { return it }
        }
        return null
    }

    private fun evaluate(script: String): String {
        val latch = CountDownLatch(1)
        var result = ""
        rule.runOnIdle {
            val editor = findEditor(rule.activity.window.decorView) ?: error("Missing editor")
            editor.evaluateJavascript(script) { result = it; latch.countDown() }
        }
        check(latch.await(10, TimeUnit.SECONDS)) { "Editor did not respond" }
        return result
    }

    private fun evaluateString(script: String): String = JSONObject("{\"value\":${evaluate(script)}}").getString("value")

    private fun waitForEditor() {
        rule.waitForIdle()
        try {
            rule.waitUntil(30000) {
                runCatching { evaluate("!!document.querySelector('pre.prism-live code')") == "true" }.getOrDefault(false)
            }
        } catch (failure: androidx.compose.ui.test.ComposeTimeoutException) {
            snapshot("theme-editor-not-ready")
            val document = runCatching { evaluate("document.documentElement.outerHTML") }.getOrElse { it.toString() }
            throw AssertionError("Editor did not load: $document", failure)
        }
    }

    private fun assertRendered(css: String) {
        assertEquals(css, evaluateString("getTextareaContent()"))
        rule.waitUntil(10000) {
            evaluateString("document.querySelector('pre.prism-live code').textContent").trimEnd() == css.trimEnd()
        }
        assertTrue("CSS must be syntax highlighted", evaluate("document.querySelectorAll('pre.prism-live .token').length > 0") == "true")
        val visibleEditor = """
            (() => {
                const editor = document.getElementById('code');
                const bounds = editor.getBoundingClientRect();
                const style = getComputedStyle(editor);
                const highlight = getComputedStyle(document.querySelector('pre.prism-live code'));
                return bounds.width >= innerWidth * 0.9 && bounds.height >= innerHeight * 0.9
                    && style.display !== 'none' && style.visibility === 'visible'
                    && Number(style.opacity) > 0 && highlight.color !== 'rgba(0, 0, 0, 0)';
            })()
        """.trimIndent()
        try {
            rule.waitUntil(10000) { evaluate(visibleEditor) == "true" }
        } catch (failure: androidx.compose.ui.test.ComposeTimeoutException) {
            snapshot("theme-editor-geometry-failure")
            val metrics = evaluate("""
                JSON.stringify({width: innerWidth, height: innerHeight,
                    editor: document.getElementById('code').getBoundingClientRect().toJSON(),
                    style: getComputedStyle(document.getElementById('code')).cssText})
            """.trimIndent())
            throw AssertionError("CSS must be visible in a viewport-sized editor: $metrics", failure)
        }
        rule.runOnIdle {
            val editor = findEditor(rule.activity.window.decorView) ?: error("Missing editor")
            assertTrue("WebView must be visible", editor.isShown && editor.width > 0 && editor.height > 0)
        }
    }

    private fun snapshot(name: String) {
        if (InstrumentationRegistry.getArguments().getString("theme_screenshots") != "true") return
        rule.waitForIdle()
        Thread.sleep(300)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: error("No screenshot")
        val folder = File(rule.activity.getExternalFilesDir(null), "audit").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
