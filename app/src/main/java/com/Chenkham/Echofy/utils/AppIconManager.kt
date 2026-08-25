package com.Chenkham.Echofy.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.Chenkham.Echofy.constants.AppIcon
import timber.log.Timber

/**
 * Switches the launcher icon by enabling exactly one `activity-alias` and disabling
 * the rest. Android reads the enabled alias to decide which icon the launcher shows.
 */
object AppIconManager {

    fun currentIcon(context: Context): AppIcon {
        val pm = context.packageManager
        return AppIcon.entries.firstOrNull { icon ->
            pm.getComponentEnabledSetting(icon.component(context)) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } ?: AppIcon.DEFAULT
    }

    /**
     * Enables [icon] and disables every other alias. The target alias is enabled before
     * the others are disabled so the launcher never sees zero enabled entry points,
     * which on some OEM launchers drops the icon entirely.
     */
    fun applyIcon(context: Context, icon: AppIcon) {
        val pm = context.packageManager
        runCatching {
            pm.setComponentEnabledSetting(
                icon.component(context),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
            AppIcon.entries.filter { it != icon }.forEach { other ->
                pm.setComponentEnabledSetting(
                    other.component(context),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }.onFailure { Timber.e(it, "Failed to switch launcher icon to $icon") }
    }

    private fun AppIcon.component(context: Context) =
        ComponentName(context.packageName, aliasName)
}
