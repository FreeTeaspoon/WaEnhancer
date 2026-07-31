package com.wmods.wppenhacer.xposed.features.customization

import android.content.SharedPreferences
import android.view.Menu
import com.wmods.wppenhacer.xposed.core.Feature
import com.wmods.wppenhacer.xposed.core.WppCore
import com.wmods.wppenhacer.xposed.utils.Utils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

private const val PREFERENCE_KEY = "hide_home_camera"

class HideHomeCamera(
    classLoader: ClassLoader,
    preferences: SharedPreferences
) : Feature(classLoader, preferences) {

    override fun doHook() {
        if (!prefs.getBoolean(PREFERENCE_KEY, false)) return

        val cameraMenuItemId = Utils.getID("menuitem_camera", "id")
        if (cameraMenuItemId <= 0) {
            log("Home camera menu item resource was not found")
            return
        }

        XposedHelpers.findAndHookMethod(
            WppCore.homeActivityClass,
            "onPrepareOptionsMenu",
            Menu::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val menu = param.args.firstOrNull() as? Menu ?: return
                    menu.findItem(cameraMenuItemId)?.isVisible = false
                }
            }
        )
    }

    override fun getPluginName(): String {
        return "Hide Home Camera"
    }
}
