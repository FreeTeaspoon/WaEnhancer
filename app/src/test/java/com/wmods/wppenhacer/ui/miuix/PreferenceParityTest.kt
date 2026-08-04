package com.wmods.wppenhacer.ui.miuix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class PreferenceParityTest {
    private val files = listOf(
        "preference_general_home.xml",
        "preference_general_homescreen.xml",
        "preference_general_conversation.xml",
        "fragment_general.xml",
        "fragment_privacy.xml",
        "fragment_media.xml",
        "fragment_customization.xml",
    )

    @Test
    fun everyUpstreamKeyHasAKnownMiuixControlType() {
        val supported = setOf(
            "MaterialSwitchPreference", "MultiSelectListPreference", "ListPreference",
            "LimitedEditTextPreference", "EditTextPreference", "FloatSeekBarPreference",
            "SeekBarPreference", "ColorPreferenceCompat", "FileSelectPreference",
            "FileReaderPreference", "ContactPickerPreference", "ThemePreference", "Preference",
        )
        val keyed = preferenceElements()
        assertEquals("Update the Miuix registry when upstream adds or removes settings", 140, keyed.size)
        keyed.forEach { element ->
            val shortTag = element.tagName.substringAfterLast('.')
            assertTrue("${element.xmlKey()} uses unsupported upstream control $shortTag", shortTag in supported)
            assertFalse("Upstream keys must not use the manager-only namespace", element.xmlKey().startsWith("manager_ui_"))
            validateDefaultRepresentation(shortTag, element)
        }
        assertEquals(keyed.size, keyed.map { it.xmlKey() }.toSet().size)
    }

    @Test
    fun managerOnlyKeysAreExplicitlyApproved() {
        assertEquals(
            setOf(
                "manager_ui_palette_style", "manager_ui_pure_black", "manager_ui_blur",
                "manager_ui_predictive_back", "manager_ui_scale", "manager_ui_floating_navigation",
                "manager_ui_floating_style", "manager_ui_navigation_content",
            ),
            ManagerAppearanceSettings.managerKeys,
        )
    }

    private fun preferenceElements(): List<Element> = files.flatMap { name ->
        val file = sequenceOf(
            File("src/main/res/xml/$name"),
            File("app/src/main/res/xml/$name"),
        ).firstOrNull(File::isFile) ?: error("Cannot find $name")
        val document = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder().parse(file)
        (0 until document.getElementsByTagName("*").length).mapNotNull { index ->
            (document.getElementsByTagName("*").item(index) as? Element)?.takeIf { it.xmlKey().isNotBlank() }
        }
    }

    private fun validateDefaultRepresentation(tag: String, element: Element) {
        val raw = element.getAttributeNS(ANDROID_NS, "defaultValue").ifBlank {
            element.getAttributeNS(AUTO_NS, "defaultValue")
        }
        if (raw.isBlank() || raw.startsWith('@')) return
        when (tag) {
            "MaterialSwitchPreference" -> assertTrue("${element.xmlKey()} must keep a Boolean default", raw == "true" || raw == "false")
            "SeekBarPreference" -> assertTrue("${element.xmlKey()} must keep an integer default", raw.toIntOrNull() != null)
            "FloatSeekBarPreference" -> assertTrue("${element.xmlKey()} must keep a numeric default", raw.toFloatOrNull() != null)
        }
    }

    private fun Element.xmlKey(): String = getAttributeNS(AUTO_NS, "key").ifBlank { getAttributeNS(ANDROID_NS, "key") }

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
        const val AUTO_NS = "http://schemas.android.com/apk/res-auto"
    }
}
