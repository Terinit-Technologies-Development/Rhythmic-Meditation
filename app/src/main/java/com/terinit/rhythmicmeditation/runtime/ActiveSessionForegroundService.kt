package com.terinit.rhythmicmeditation.runtime

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Narrow active-session foreground service (spec 41, evidence-mandated).
 *
 * WHY THIS EXISTS: physical-device validation on Android 16 hardware showed
 * the platform killing the app process ~17 minutes into a real screen-off
 * session (midnight maintenance window). The conservative checkpoint design
 * lost only 12 seconds, but the session could not complete — so long
 * screen-off meditation needs the process kept alive.
 *
 * Scope discipline (the "narrowest possible" service):
 * - exists ONLY while a qualifying session is ACTIVE (started/stopped by the
 *   runtime state observer — see RhythmicMeditationApp);
 * - quiet ongoing notification (low importance, no sound, no vibration);
 * - contains NO timing logic: monotonic timing and the checkpoint
 *   architecture are unchanged in MeditationRuntimeController;
 * - no wake lock;
 * - stops immediately on complete/cancel/expire.
 */
class ActiveSessionForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForegroundCompat()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundCompat() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureChannel(manager: NotificationManager) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(com.terinit.rhythmicmeditation.R.string.session_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(com.terinit.rhythmicmeditation.R.string.session_channel_description)
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.terinit.rhythmicmeditation.R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(com.terinit.rhythmicmeditation.R.string.session_notification_title))
            .setContentText(getString(com.terinit.rhythmicmeditation.R.string.session_notification_text))
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "active_meditation_session"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.terinit.rhythmicmeditation.action.STOP_SESSION_SERVICE"

        /** The service exists only while a qualifying session is ACTIVE. */
        fun start(context: Context) {
            val intent = Intent(context, ActiveSessionForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /** Stops immediately on complete / cancel / expire. */
        fun stop(context: Context) {
            // stopService (not a startService with an action) — background
            // service starts are restricted on modern Android and stopping
            // must always be allowed.
            context.stopService(Intent(context, ActiveSessionForegroundService::class.java))
        }
    }
}
