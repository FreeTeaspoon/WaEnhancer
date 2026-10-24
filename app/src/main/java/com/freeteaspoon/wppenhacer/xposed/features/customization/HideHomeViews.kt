package com.freeteaspoon.wppenhacer.xposed.features.customization

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.view.View
import com.freeteaspoon.wppenhacer.xposed.core.Feature
import com.freeteaspoon.wppenhacer.xposed.core.WppCore
import com.freeteaspoon.wppenhacer.xposed.utils.Utils
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import java.util.Collections
import java.util.WeakHashMap

private const val HIDE_FAB_KEY = "hide_home_fab"
private const val HIDE_BOTTOM_NAV_KEY = "hide_bottom_nav"

// View.VISIBILITY_MASK, used by View.setFlags to carry visibility changes.
private const val VISIBILITY_MASK = 0x0000000C

// The primary FAB (new chat / new call / camera) and its extended (labelled) variant.
private val FAB_RESOURCE_NAMES = arrayOf("fab", "fabText")

// Track each view independently: a container ID can exist in resources even when
// the active home layout uses the bar without that container.
private val BOTTOM_NAV_RESOURCE_NAMES =
    arrayOf("bottom_nav_container", "bottom_nav", "bottom_nav_divider")

/**
 * Hides the floating action button and/or the bottom navigation bar on the
 * WhatsApp home screen. WhatsApp toggles both views itself (tab changes,
 * scrolling, search mode), so the views are tracked and every later
 * visibility change is forced back to GONE.
 */
class HideHomeViews(
    classLoader: ClassLoader,
    preferences: SharedPreferences
) : Feature(classLoader, preferences) {

    private val forcedGone: MutableSet<View> = Collections.newSetFromMap(WeakHashMap())

    override fun doHook() {
        val hideFab = xprefs.getBoolean(HIDE_FAB_KEY, false)
        val hideBottomNav = xprefs.getBoolean(HIDE_BOTTOM_NAV_KEY, false)
        if (!hideFab && !hideBottomNav) return

        val targetIds = HashSet<Int>()
        if (hideFab) {
            val fabIds = resolveIds(FAB_RESOURCE_NAMES)
            if (fabIds.isEmpty()) log("Home FAB resource was not found")
            targetIds += fabIds
        }
        if (hideBottomNav) {
            val bottomNavIds = resolveIds(BOTTOM_NAV_RESOURCE_NAMES)
            if (bottomNavIds.isEmpty()) log("Bottom navigation resource was not found")
            targetIds += bottomNavIds
        }
        if (targetIds.isEmpty()) return

        View::class.resolve().firstMethod {
            name = "onAttachedToWindow"
            parameters()
        }.hook {
            after {
                val view = instance as? View ?: return@after
                if (view.id !in targetIds) return@after
                if (!isHomeActivity(view.context)) return@after
                forcedGone.add(view)
                view.visibility = View.GONE
            }
        }

        View::class.resolve().firstMethod {
            name = "onDetachedFromWindow"
            parameters()
        }.hook {
            after {
                val view = instance as? View ?: return@after
                forcedGone.remove(view)
            }
        }

        View::class.resolve().firstMethod {
            name = "setFlags"
            parameters(Int::class, Int::class)
        }.hook {
            before {
                if (forcedGone.isEmpty()) return@before
                if ((args[1] as Int) and VISIBILITY_MASK == 0) return@before
                val view = instance as? View ?: return@before
                if (view !in forcedGone) return@before
                args[0] = ((args[0] as Int) and VISIBILITY_MASK.inv()) or View.GONE
            }
        }
    }

    private fun resolveIds(names: Array<String>): List<Int> {
        return names.mapNotNull { name ->
            Utils.getID(name, "id").takeIf { id -> id > 0 }
        }
    }

    private fun isHomeActivity(context: Context?): Boolean {
        var current = context
        while (current != null) {
            if (current is Activity) return WppCore.homeActivityClass.isInstance(current)
            current = (current as? ContextWrapper)?.baseContext
        }
        return false
    }

    override fun getPluginName(): String {
        return "Hide Home Views"
    }
}
