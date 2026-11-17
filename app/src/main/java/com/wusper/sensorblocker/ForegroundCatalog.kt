package com.wusper.sensorblocker

import android.content.Context
import android.content.Intent
import android.view.inputmethod.InputMethodManager

/**
 * Discovers home launchers (MAIN + HOME, not every LAUNCHER app) and
 * input method packages so ForegroundTracker can treat them as transient.
 */
object ForegroundCatalog {
    fun refresh(context: Context) {
        ForegroundTracker.configure(
            selfPackage = context.packageName,
            launchers = launchers(context),
            imes = inputMethods(context)
        )
    }

    fun launchers(context: Context): Set<String> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val found = try {
            pm.queryIntentActivities(intent, 0).map { it.activityInfo.packageName }
        } catch (_: Exception) {
            emptyList()
        }
        return (found + TransientPackages.KNOWN_LAUNCHERS).toSet()
    }

    fun inputMethods(context: Context): Set<String> {
        val imm = context.getSystemService(InputMethodManager::class.java) ?: return emptySet()
        return try {
            val enabled = imm.enabledInputMethodList.orEmpty()
            val all = imm.inputMethodList.orEmpty()
            (enabled + all).map { it.packageName }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }
}
