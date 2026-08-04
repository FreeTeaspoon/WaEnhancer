package com.wmods.wppenhacer.ui.miuix

import android.content.Context
import android.content.res.Resources
import android.content.res.XmlResourceParser
import com.wmods.wppenhacer.R
import org.xmlpull.v1.XmlPullParser

internal object PreferenceRegistry {
    private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    private const val AUTO_NS = "http://schemas.android.com/apk/res-auto"

    private data class SourceFile(
        val source: PreferenceSource,
        val resource: Int,
        val fallbackCategory: Int,
    )

    private val sourceFiles = listOf(
        SourceFile(PreferenceSource.GENERAL, R.xml.preference_general_home, R.string.general),
        SourceFile(PreferenceSource.HOME_SCREEN, R.xml.preference_general_homescreen, R.string.home_screen),
        SourceFile(PreferenceSource.CONVERSATION, R.xml.preference_general_conversation, R.string.conversation),
        SourceFile(PreferenceSource.STATUS, R.xml.fragment_general, R.string.status),
        SourceFile(PreferenceSource.PRIVACY, R.xml.fragment_privacy, R.string.privacy),
        SourceFile(PreferenceSource.MEDIA, R.xml.fragment_media, R.string.media),
        SourceFile(PreferenceSource.CUSTOMIZE, R.xml.fragment_customization, R.string.perso),
    )

    fun build(context: Context): List<PreferenceSpec> = sourceFiles.flatMap { file ->
        parse(context, file)
    }

    fun upstreamXmlResources(): List<Int> = sourceFiles.map(SourceFile::resource)

    private fun parse(context: Context, sourceFile: SourceFile): List<PreferenceSpec> {
        val parser = context.resources.getXml(sourceFile.resource)
        val result = mutableListOf<PreferenceSpec>()
        var category = context.getString(sourceFile.fallbackCategory)
        try {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    val tag = parser.name.substringAfterLast('.')
                    if (tag == "PreferenceCategory") {
                        category = parser.textAttribute(context, "title") ?: category
                    } else if (tag != "PreferenceScreen") {
                        val key = parser.rawAttribute("key")
                        if (!key.isNullOrBlank()) {
                            result += PreferenceSpec(
                                key = key,
                                source = sourceFile.source,
                                category = category,
                                title = parser.textAttribute(context, "title") ?: key,
                                summary = parser.textAttribute(context, "summary")?.takeUnless { it == "%s" },
                                kind = kindFor(parser.name),
                                entries = parser.textArray(context.resources, "entries"),
                                entryValues = parser.textArray(context.resources, "entryValues"),
                                defaultValue = parser.rawAttribute("defaultValue"),
                                dependency = parser.rawAttribute("dependency"),
                                enabledByXml = parser.rawAttribute("enabled")?.toBooleanStrictOrNull() ?: true,
                                minimum = parser.rawAttribute("minValue")?.toFloatOrNull()
                                    ?: parser.rawAttribute("min")?.toFloatOrNull() ?: 0f,
                                maximum = parser.rawAttribute("maxValue")?.toFloatOrNull()
                                    ?: parser.rawAttribute("max")?.toFloatOrNull() ?: 100f,
                                step = parser.rawAttribute("valueSpacing")?.toFloatOrNull() ?: 1f,
                            )
                        }
                    }
                }
                event = parser.next()
            }
        } finally {
            parser.close()
        }
        return result
    }

    private fun kindFor(fullTag: String): PreferenceKind = when {
        fullTag.endsWith("MaterialSwitchPreference") -> PreferenceKind.SWITCH
        fullTag.endsWith("MultiSelectListPreference") -> PreferenceKind.MULTI_LIST
        fullTag.endsWith("ListPreference") -> PreferenceKind.LIST
        fullTag.endsWith("LimitedEditTextPreference") || fullTag.endsWith("EditTextPreference") -> PreferenceKind.TEXT
        fullTag.endsWith("FloatSeekBarPreference") -> PreferenceKind.FLOAT_SLIDER
        fullTag.endsWith("SeekBarPreference") -> PreferenceKind.INTEGER_SLIDER
        fullTag.endsWith("ColorPreferenceCompat") -> PreferenceKind.COLOR
        fullTag.endsWith("FileSelectPreference") || fullTag.endsWith("FileReaderPreference") -> PreferenceKind.FILE
        fullTag.endsWith("ContactPickerPreference") -> PreferenceKind.CONTACTS
        fullTag.endsWith("ThemePreference") -> PreferenceKind.THEME
        else -> PreferenceKind.ACTION
    }

    private fun XmlResourceParser.rawAttribute(name: String): String? {
        val value = getAttributeValue(AUTO_NS, name) ?: getAttributeValue(ANDROID_NS, name) ?: return null
        if (!value.startsWith("@")) return value
        return null
    }

    private fun XmlResourceParser.resourceAttribute(name: String): Int =
        getAttributeResourceValue(AUTO_NS, name, 0).takeIf { it != 0 }
            ?: getAttributeResourceValue(ANDROID_NS, name, 0)

    private fun XmlResourceParser.textAttribute(context: Context, name: String): String? {
        val resource = resourceAttribute(name)
        if (resource != 0) return runCatching { context.getString(resource) }.getOrNull()
        return rawAttribute(name)
    }

    private fun XmlResourceParser.textArray(resources: Resources, name: String): List<String> {
        val resource = resourceAttribute(name)
        if (resource == 0) return emptyList()
        return runCatching { resources.getTextArray(resource).map(CharSequence::toString) }.getOrDefault(emptyList())
    }
}
