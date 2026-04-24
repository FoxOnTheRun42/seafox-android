package com.seafox.nmea_dashboard

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        val action = intent?.action
        if (action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.i(LOG_TAG, "Package replaced; app updated.")
            return
        }

        when (BootAutostartPolicy.decide(action, readDashboardStateJson(context))) {
            BootAutostartDecision.scheduleDelayedLaunch -> {
                scheduleAppLaunch(context)
            }
            BootAutostartDecision.launchNow -> {
                launchApp(context)
            }
            BootAutostartDecision.skipDisabled -> {
                Log.i(LOG_TAG, "Boot autostart skipped; disabled by dashboard privacy settings.")
            }
            BootAutostartDecision.ignore -> return
        }
    }

    private fun readDashboardStateJson(context: Context): String? {
        return try {
            context
                .getSharedPreferences(PREF_NAME_DASHBOARD_STATE, Context.MODE_PRIVATE)
                .getString(KEY_DASHBOARD_STATE_JSON, null)
        } catch (_: Exception) {
            null
        }
    }

    private fun launchApp(context: Context) {
        try {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(launchIntent)
            Log.i(LOG_TAG, "Started MainActivity.")
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Could not start MainActivity from boot path.", e)
        }
    }

    private fun scheduleAppLaunch(context: Context) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: run {
            Log.w(LOG_TAG, "AlarmManager not available; fallback to direct launch.")
            launchApp(context)
            return
        }
        val launchReceiverIntent = Intent(context, BootCompletedReceiver::class.java).apply {
            action = BootAutostartPolicy.ACTION_AUTOSTART_INTERNAL
        }
        val pendingLaunch = PendingIntent.getBroadcast(
            context,
            AUTO_START_REQUEST_CODE,
            launchReceiverIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        manager.cancel(pendingLaunch)
        val triggerAt = SystemClock.elapsedRealtime() + BOOT_LAUNCH_DELAY_MS
        manager.set(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            triggerAt,
            pendingLaunch,
        )
        Log.i(LOG_TAG, "Scheduled delayed app launch in ${BOOT_LAUNCH_DELAY_MS}ms.")
    }

    companion object {
        private const val LOG_TAG = "BootCompletedReceiver"
        private const val PREF_NAME_DASHBOARD_STATE = "dashboard_state"
        private const val KEY_DASHBOARD_STATE_JSON = "dashboard_state_json"
        private const val AUTO_START_REQUEST_CODE = 42004
        private const val BOOT_LAUNCH_DELAY_MS = 12_000L
    }
}
