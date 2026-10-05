package com.freeteaspoon.wppenhacer.xposed.features.general

import android.content.SharedPreferences
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.freeteaspoon.wppenhacer.xposed.core.Feature
import com.freeteaspoon.wppenhacer.xposed.core.devkit.Unobfuscator

class ShareLimit(classLoader: ClassLoader, xprefs: SharedPreferences) :
    Feature(classLoader, xprefs) {

    override fun doHook() {
        if (!xprefs.getBoolean("removeforwardlimit", false)) return
        val multiSelectionLimitInfoClass =
            Unobfuscator.loadMultiSelectionLimitInfoClass(classLoader)
        multiSelectionLimitInfoClass.resolve().constructor { }.hookAll {
            before {
                args[0] = Int.MAX_VALUE
            }
        }
    }

    override fun getPluginName(): String = "Share Limit"
}