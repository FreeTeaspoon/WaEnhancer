package com.wmods.wppenhacer.xposed.features.general

import android.app.Activity
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.wmods.wppenhacer.R
import com.wmods.wppenhacer.xposed.core.Feature
import com.wmods.wppenhacer.xposed.core.devkit.Unobfuscator
import com.wmods.wppenhacer.xposed.features.customization.SeparateGroup
import com.wmods.wppenhacer.xposed.features.others.MenuHome
import com.wmods.wppenhacer.xposed.utils.DesignUtils
import com.wmods.wppenhacer.xposed.utils.Utils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

private const val PREFERENCE_KEY = "calls_button"

// Tab id WhatsApp assigns to the Calls tab (see tabshide_values in arrays.xml).
private const val CALLS_TAB_ID = 400

// Icons from WhatsApp's own resources, in order of preference.
private val CALLS_ICON_NAMES = arrayOf("wa_ic_call", "ic_action_audio_call", "vec_ic_phone_receiver")
private val CHATS_ICON_NAMES = arrayOf("vec_ic_chat", "home_tab_chats_selector")

/**
 * Adds a "Calls" entry to the home screen options menu that opens the Calls
 * tab. While the Calls tab is showing the entry becomes "Chats" and switches
 * back, since WhatsApp exits on Back from the Calls tab and the bottom bar may
 * be hidden. WhatsApp rebuilds the options menu on every tab change, so the
 * label is decided each time the menu is created.
 */
class CallsButton(
    classLoader: ClassLoader,
    preferences: SharedPreferences
) : Feature(classLoader, preferences) {

    @Volatile
    private var tabs: List<Int>? = null

    override fun doHook() {
        if (!prefs.getBoolean(PREFERENCE_KEY, false)) return
        val action = prefs.getBoolean("buttonaction", true)

        val bottomNavId = Utils.getID("bottom_nav", "id")
        val pagerId = Utils.getID("pager", "id")

        // Keep a reference to the live tab list so pager indexes can be
        // mapped to tab ids.
        try {
            XposedBridge.hookMethod(
                Unobfuscator.loadTabListMethod(classLoader),
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        @Suppress("UNCHECKED_CAST")
                        tabs = param.result as? List<Int>
                    }
                })
        } catch (e: Throwable) {
            log(e)
        }

        MenuHome.addMenuItem { menu, activity ->
            insertOption(menu, activity, action, bottomNavId, pagerId)
        }
    }

    private fun insertOption(
        menu: Menu,
        activity: Activity,
        newSettings: Boolean,
        bottomNavId: Int,
        pagerId: Int
    ) {
        val onCallsTab = currentTabId(activity, pagerId) == CALLS_TAB_ID
        val targetTab = if (onCallsTab) SeparateGroup.CHATS else CALLS_TAB_ID
        val title = if (onCallsTab) R.string.chats else R.string.calls
        val iconNames = if (onCallsTab) CHATS_ICON_NAMES else CALLS_ICON_NAMES

        val item = menu.add(0, 0, 0, title)
        loadIcon(activity, iconNames)?.let { drawable ->
            item.icon = DesignUtils.createHomeMenuIcon(drawable, newSettings)
        }
        if (newSettings) {
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        item.setOnMenuItemClickListener {
            openTab(activity, targetTab, bottomNavId, pagerId)
            true
        }
    }

    // Resolve through the activity so theme attributes inside the vector
    // (fill colours) are honoured; the application theme leaves them
    // transparent and the tint then has nothing to paint.
    private fun loadIcon(activity: Activity, names: Array<String>): Drawable? {
        return names.firstNotNullOfOrNull { name ->
            val iconId = Utils.getID(name, "drawable")
            if (iconId > 0) activity.getDrawable(iconId)?.mutate() else null
        }
    }

    private fun currentTabId(activity: Activity, pagerId: Int): Int? {
        return try {
            if (pagerId <= 0) return null
            val pager = activity.findViewById<View>(pagerId) ?: return null
            val index = XposedHelpers.callMethod(pager, "getCurrentItem") as Int
            tabs?.getOrNull(index)
        } catch (e: Throwable) {
            logDebug(e)
            null
        }
    }

    private fun openTab(activity: Activity, tabId: Int, bottomNavId: Int, pagerId: Int) {
        try {
            // Preferred: behave exactly like a tap on the tab item. The bottom
            // bar item views carry the tab id, and performClick works even
            // when the bar is hidden.
            if (bottomNavId > 0) {
                val tabItem = activity.findViewById<View>(bottomNavId)
                    ?.findViewById<View>(tabId)
                if (tabItem != null && tabItem.performClick()) return
            }

            // Fallback: move the pager directly.
            val index = tabs?.indexOf(tabId) ?: -1
            if (pagerId <= 0 || index < 0) {
                log("Tab $tabId could not be resolved")
                return
            }
            val pager = activity.findViewById<View>(pagerId) ?: return
            XposedHelpers.callMethod(pager, "setCurrentItem", index)
        } catch (e: Throwable) {
            log(e)
        }
    }

    override fun getPluginName(): String {
        return "Calls Button"
    }
}
