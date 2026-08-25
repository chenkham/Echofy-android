package com.Chenkham.Echofy.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.edit
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.Chenkham.Echofy.MainActivity
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.RELEASE_RADAR_SEEN_LIMIT
import com.Chenkham.Echofy.constants.ReleaseRadarEnabledKey
import com.Chenkham.Echofy.constants.ReleaseRadarSeenIdsKey
import com.Chenkham.Echofy.di.ReleaseRadarEntryPoint
import com.arturo254.opentune.innertube.YouTube
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class ReleaseRadarWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val enabled = context.dataStore.data.first()[ReleaseRadarEnabledKey] ?: false
        if (!enabled) return Result.success()

        val database = EntryPointAccessors
            .fromApplication(context, ReleaseRadarEntryPoint::class.java)
            .database()

        val followed = database.bookmarkedArtistNames()
        if (followed.isEmpty()) return Result.success()

        val albums = YouTube.newReleaseAlbums().getOrElse {
            reportException(it)
            return Result.retry()
        }

        val followedKeys = followed.map { it.lowercase() }.toSet()
        val matches = albums.filter { album ->
            album.artists.orEmpty().any { it.name.lowercase() in followedKeys }
        }
        if (matches.isEmpty()) return Result.success()

        val seen = context.dataStore.data.first()[ReleaseRadarSeenIdsKey]
            ?.split(',')
            ?.filter { it.isNotBlank() }
            .orEmpty()
        val seenSet = seen.toSet()

        val fresh = matches.filter { it.id !in seenSet }
        if (fresh.isEmpty()) return Result.success()

        notify(fresh.map { album ->
            album.title to album.artists.orEmpty().joinToString(", ") { it.name }
        })

        val merged = (fresh.map { it.id } + seen).take(RELEASE_RADAR_SEEN_LIMIT)
        context.dataStore.edit { prefs ->
            prefs[ReleaseRadarSeenIdsKey] = merged.joinToString(",")
        }

        return Result.success()
    }

    private fun notify(releases: List<Pair<String, String>>) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.release_radar),
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            )
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val title = context.resources.getQuantityString(
            R.plurals.release_radar_new_releases,
            releases.size,
            releases.size,
        )
        val lines = releases.map { (album, artist) -> "$artist — $album" }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_on)
            .setContentTitle(title)
            .setContentText(lines.first())
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (lines.size > 1) {
            builder.setStyle(
                NotificationCompat.InboxStyle().also { style ->
                    lines.take(MAX_INBOX_LINES).forEach(style::addLine)
                }
            )
        }

        manager.notify(NOTIFICATION_ID, builder.build())
    }

    companion object {
        private const val CHANNEL_ID = "echofy_release_radar"
        private const val NOTIFICATION_ID = 4711
        private const val MAX_INBOX_LINES = 5
        const val WORK_NAME = "release_radar"

        /**
         * Registers the daily check. Uses [ExistingPeriodicWorkPolicy.KEEP] so an already
         * scheduled run keeps its place in the queue instead of restarting its 24h window
         * every time the app launches.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReleaseRadarWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
