package com.Chenkham.Echofy.ai

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.Chenkham.Echofy.MainActivity
import com.Chenkham.Echofy.R
import timber.log.Timber

/**
 * Keeps the "Hey Jarvis" wake word detector alive while the app is backgrounded.
 *
 * Without this the detector only runs while the AI screen is composed, because it
 * lives in a process-scoped coroutine that Android is free to starve once the app
 * is no longer visible. A foreground service with a persistent notification is the
 * only supported way to hold the microphone in the background.
 */
class WakeWordService : Service() {

    private val aiManager by lazy { EchofyAiManager.getInstance(applicationContext) }

    /** Drives the notification wording so the user always sees the real state. */
    private var drivingMode = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Timber.d("WakeWordService stop requested")
                aiManager.setDrivingMode(false)
                aiManager.stopHeyCommandListener()
                stopSelf()
                return START_NOT_STICKY
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Never hold a microphone foreground notification we cannot honour.
            Timber.w("RECORD_AUDIO not granted; WakeWordService will not start")
            stopSelf()
            return START_NOT_STICKY
        }

        // A null intent means Android restarted us via START_STICKY; fall back to
        // the last known state instead of silently dropping driving mode.
        drivingMode = intent?.getBooleanExtra(EXTRA_DRIVING_MODE, drivingMode) ?: drivingMode
        aiManager.setDrivingMode(drivingMode)

        try {
            startAsForeground()
        } catch (e: Exception) {
            // Android 12+ throws if the app is not allowed to start a foreground
            // service from the background; fail closed rather than crash.
            Timber.e(e, "Failed to enter foreground; stopping WakeWordService")
            stopSelf()
            return START_NOT_STICKY
        }

        aiManager.startHeyCommandListener()
        Timber.d("WakeWordService started; drivingMode=$drivingMode")

        // Restart if the process is killed so detection survives memory pressure.
        return START_STICKY
    }

    private fun startAsForeground() {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, WakeWordService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (drivingMode) "Driving Mode On" else "Hey Jarvis is listening")
            .setContentText(
                if (drivingMode) {
                    "Listening for commands \u2014 no wake word needed"
                } else {
                    "Say \"Hey Jarvis\" to control playback"
                }
            )
            .setSmallIcon(R.drawable.auto_awesome)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(0, if (drivingMode) "Turn off" else "Stop", stopIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Wake Word",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shown while Echofy listens for the \"Hey Jarvis\" wake word"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        aiManager.setDrivingMode(false)
        aiManager.stopHeyCommandListener()
        Timber.d("WakeWordService destroyed")
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "echofy_wake_word"
        private const val NOTIFICATION_ID = 4711
        private const val ACTION_STOP = "com.Chenkham.Echofy.action.STOP_WAKE_WORD"
        private const val EXTRA_DRIVING_MODE = "driving_mode"

        /**
         * Starts (or updates) the service. Calling this again with a different
         * [drivingMode] re-runs onStartCommand and refreshes the notification, which
         * is how the "Driving Mode On" text stays in sync with the toggle.
         */
        fun start(context: Context, drivingMode: Boolean = false) {
            val intent = Intent(context, WakeWordService::class.java)
                .putExtra(EXTRA_DRIVING_MODE, drivingMode)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Timber.e(e, "Unable to start WakeWordService")
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, WakeWordService::class.java))
            } catch (e: Exception) {
                Timber.w(e, "Unable to stop WakeWordService")
            }
        }
    }
}
