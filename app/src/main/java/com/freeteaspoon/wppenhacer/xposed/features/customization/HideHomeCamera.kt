package com.freeteaspoon.wppenhacer.xposed.features.customization

import android.content.SharedPreferences
import android.view.Menu
import com.freeteaspoon.wppenhacer.xposed.core.Feature
import com.freeteaspoon.wppenhacer.xposed.core.WppCore
import com.freeteaspoon.wppenhacer.xposed.utils.Utils
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook

private const val PREFERENCE_KEY = "hide_home_camera"

class HideHomeCamera(
    classLoader: ClassLoader,
    preferences: SharedPreferences
) : Feature(classLoader, preferences) {

    override fun doHook() {
        if (!xprefs.getBoolean(PREFERENCE_KEY, false)) return

        val cameraMenuItemId = Utils.getID("menuitem_camera", "id")
        if (cameraMenuItemId <= 0) {
            log("Home camera menu item resource was not found")
            return
        }

        WppCore.homeActivityClass.resolve().firstMethod {
            name = "onPrepareOptionsMenu"
            parameters(Menu::class.java)
            superclass()
        }.hook {
            after {
                val menu = args.firstOrNull() as? Menu ?: return@after
                menu.findItem(cameraMenuItemId)?.isVisible = false
            }
        }
    }

    override fun getPluginName(): String {
        return "Hide Home Camera"
    }
}
