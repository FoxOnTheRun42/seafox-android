package com.seafox.nmea_dashboard

enum class BootAutostartDecision {
    ignore,
    skipDisabled,
    scheduleDelayedLaunch,
    launchNow,
}

object BootAutostartPolicy {
    const val ACTION_AUTOSTART_INTERNAL = "com.seafox.nmea_dashboard.action.AUTOSTART_INTERNAL"

    private const val ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED"
    private const val ACTION_LOCKED_BOOT_COMPLETED = "android.intent.action.LOCKED_BOOT_COMPLETED"
    private const val ACTION_USER_UNLOCKED = "android.intent.action.USER_UNLOCKED"
    private val enabledPattern = Regex(""""bootAutostartEnabled"\s*:\s*true""")

    fun decide(action: String?, dashboardStateJson: String?): BootAutostartDecision {
        return when (action) {
            ACTION_BOOT_COMPLETED,
            ACTION_LOCKED_BOOT_COMPLETED -> {
                if (isEnabled(dashboardStateJson)) {
                    BootAutostartDecision.scheduleDelayedLaunch
                } else {
                    BootAutostartDecision.skipDisabled
                }
            }
            ACTION_USER_UNLOCKED,
            ACTION_AUTOSTART_INTERNAL -> {
                if (isEnabled(dashboardStateJson)) {
                    BootAutostartDecision.launchNow
                } else {
                    BootAutostartDecision.skipDisabled
                }
            }
            else -> BootAutostartDecision.ignore
        }
    }

    fun isEnabled(dashboardStateJson: String?): Boolean {
        if (dashboardStateJson.isNullOrBlank()) return false
        return enabledPattern.containsMatchIn(dashboardStateJson)
    }
}
