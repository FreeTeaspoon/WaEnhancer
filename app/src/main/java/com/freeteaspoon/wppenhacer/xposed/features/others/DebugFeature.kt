package com.freeteaspoon.wppenhacer.xposed.features.others

import android.content.SharedPreferences
import com.freeteaspoon.wppenhacer.xposed.core.Feature

class DebugFeature(classLoader: ClassLoader, preferences: SharedPreferences) :
    Feature(classLoader, preferences) {

    override fun doHook() {
    }


    override fun getPluginName(): String {
        return "Debug Feature"
    }
}
