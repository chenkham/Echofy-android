package com.Chenkham.Echofy

import android.app.Application
import android.content.Context
import android.os.Build
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.datastore.preferences.core.edit
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.request.CachePolicy
import com.Chenkham.innertube.YouTube
import com.Chenkham.innertube.models.YouTubeLocale
import com.Chenkham.kugou.KuGou
import com.Chenkham.Echofy.constants.AccountChannelHandleKey
import com.Chenkham.Echofy.constants.AccountEmailKey
import com.Chenkham.Echofy.constants.AccountNameKey
import com.Chenkham.Echofy.constants.ContentCountryKey
import com.Chenkham.Echofy.constants.ContentLanguageKey
import com.Chenkham.Echofy.constants.CountryCodeToName
import com.Chenkham.Echofy.constants.DataSyncIdKey
import com.Chenkham.Echofy.constants.InnerTubeCookieKey
import com.Chenkham.Echofy.constants.LanguageCodeToName
import com.Chenkham.Echofy.constants.MaxImageCacheSizeKey
import com.Chenkham.Echofy.constants.ProxyEnabledKey
import com.Chenkham.Echofy.constants.ProxyTypeKey
import com.Chenkham.Echofy.constants.ProxyUrlKey
import com.Chenkham.Echofy.constants.SYSTEM_DEFAULT
import com.Chenkham.Echofy.constants.UseLoginForBrowse
import com.Chenkham.Echofy.constants.VisitorDataKey
import com.Chenkham.Echofy.extensions.toEnum
import com.Chenkham.Echofy.extensions.toInetSocketAddress
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.get
import com.Chenkham.Echofy.utils.reportException
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.Proxy
import java.util.Locale

@HiltAndroidApp
class App : Application(), ImageLoaderFactory {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        instance = this;
        Timber.plant(Timber.DebugTree())

        val locale = Locale.getDefault()
        val languageTag = locale.toLanguageTag().replace("-Hant", "") // replace zh-Hant-* to zh-*
        YouTube.locale = YouTubeLocale(
            gl = dataStore[ContentCountryKey]?.takeIf { it != SYSTEM_DEFAULT }
                ?: locale.country.takeIf { it in CountryCodeToName }
                ?: "GB",
            hl = dataStore[ContentLanguageKey]?.takeIf { it != SYSTEM_DEFAULT }
                ?: locale.language.takeIf { it in LanguageCodeToName }
                ?: languageTag.takeIf { it in LanguageCodeToName }
                ?: "en-GB"
        )
        if (languageTag == "zh-TW") {
            KuGou.useTraditionalChinese = true
        }

        if (dataStore[ProxyEnabledKey] == true) {
            try {
                YouTube.proxy = Proxy(
                    dataStore[ProxyTypeKey].toEnum(defaultValue = Proxy.Type.HTTP),
                    dataStore[ProxyUrlKey]!!.toInetSocketAddress()
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to parse proxy url.", LENGTH_SHORT).show()
                reportException(e)
            }
        }

        if (dataStore[UseLoginForBrowse] != false) {
            YouTube.useLoginForBrowse = true
        }

        GlobalScope.launch {
            dataStore.data
                .map { it[VisitorDataKey] }
                .distinctUntilChanged()
                .collect { visitorData ->
                    YouTube.visitorData = visitorData
                        ?.takeIf { it != "null" } // Previously visitorData was sometimes saved as "null" due to a bug
                        ?: YouTube.visitorData().onFailure {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@App, "Failed to get visitorData.", LENGTH_SHORT)
                                    .show()
                            }
                            reportException(it)
                        }.getOrNull()?.also { newVisitorData ->
                            dataStore.edit { settings ->
                                settings[VisitorDataKey] = newVisitorData
                            }
                        }
                }
        }
        GlobalScope.launch {
            dataStore.data
                .map { it[DataSyncIdKey] }
                .distinctUntilChanged()
                .collect { dataSyncId ->
                    YouTube.dataSyncId = dataSyncId?.let {
                        /*
                         * Workaround to avoid breaking older installations that have a dataSyncId
                         * that contains "||" in it.
                         * If the dataSyncId ends with "||" and contains only one id, then keep the
                         * id before the "||".
                         * If the dataSyncId contains "||" and is not at the end, then keep the
                         * second id.
                         * This is needed to keep using the same account as before.
                         */
                        it.takeIf { !it.contains("||") }
                            ?: it.takeIf { it.endsWith("||") }?.substringBefore("||")
                            ?: it.substringAfter("||")
                    }
                }
        }
        GlobalScope.launch {
            dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .collect { cookie ->
                    try {
                        YouTube.cookie = cookie
                    } catch (e: Exception) {
                        // we now allow user input now, here be the demons. This serves as a last ditch effort to avoid a crash loop
                        Timber.e("Could not parse cookie. Clearing existing cookie. %s", e.message)
                        forgetAccount(this@App)
                    }
                }
        }
        
        // Subscribe to FCM topics for push notifications
        subscribeToFcmTopics()
        
        // Initialize Firebase In-App Messaging at application level
        // THIS IS CRITICAL - FIAM must be initialized before any Activity starts
        initializeFirebaseInAppMessaging()
        
        // Show welcome notification for first-time users
        showWelcomeNotificationIfFirstLaunch()
    }
    
    /**
     * Initialize Firebase In-App Messaging at application startup.
     * This is critical for FIAM to work on every app open, not just first install.
     * 
     * Key points:
     * - Must be initialized at Application level (before Activities)
     * - Must enable automatic data collection
     * - Must NOT suppress messages
     * - Firebase Analytics must be enabled for FIAM to work
     * - Clear FIAM cache for real-time sync (like Spotify/Flipkart)
     */
    private fun initializeFirebaseInAppMessaging() {
        try {
            // CRITICAL: Clear FIAM cache files to enable real-time campaign sync
            // This allows new campaigns to show immediately without reinstall
            clearFiamCacheForRealTimeSync()
            
            // Initialize Firebase Analytics first (required for FIAM)
            val analytics = com.google.firebase.analytics.FirebaseAnalytics.getInstance(this)
            analytics.setAnalyticsCollectionEnabled(true)
            
            // Get Firebase In-App Messaging instance
            val fiam = com.google.firebase.inappmessaging.FirebaseInAppMessaging.getInstance()
            
            // CRITICAL: Enable automatic data collection (required for images and campaigns)
            fiam.isAutomaticDataCollectionEnabled = true
            
            // CRITICAL: Ensure messages are NOT suppressed
            fiam.setMessagesSuppressed(false)
            
            // Add a click listener to log when messages are displayed
            fiam.addClickListener { inAppMessage, action ->
                Timber.d("Firebase FIAM: Message clicked - Campaign: ${inAppMessage.campaignMetadata?.campaignName}")
            }
            
            // Log that FIAM is ready
            Timber.d("Firebase In-App Messaging initialized successfully")
            Timber.d("FIAM auto data collection: ${fiam.isAutomaticDataCollectionEnabled}")
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Firebase In-App Messaging")
        }
    }
    
    /**
     * Clear Firebase In-App Messaging cache files to enable real-time campaign sync.
     * 
     * Firebase FIAM stores these files locally:
     * - fiam_impressions_store_file: Tracks which messages have been shown
     * - rate_limit_store_file: Rate limiting info
     * - fiam_eligible_campaigns_cache_file: Cached campaigns
     * 
     * Clearing these allows new campaigns to be fetched and displayed 
     * on every app open, just like commercial apps (Spotify, Flipkart, Meesho).
     */
    private fun clearFiamCacheForRealTimeSync() {
        try {
            val cacheFiles = listOf(
                "fiam_impressions_store_file",
                "rate_limit_store_file",
                "fiam_eligible_campaigns_cache_file"
            )
            
            cacheFiles.forEach { fileName ->
                val file = java.io.File(filesDir, fileName)
                if (file.exists()) {
                    val deleted = file.delete()
                    Timber.d("FIAM cache cleared: $fileName = $deleted")
                }
            }
            
            Timber.d("FIAM cache cleared for real-time sync")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear FIAM cache")
        }
    }
    
    /**
     * Subscribe to default FCM topics for receiving notifications.
     */
    private fun subscribeToFcmTopics() {
        val topics = listOf("all_users", "updates", "listen_together")
        topics.forEach { topic ->
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance()
                    .subscribeToTopic(topic)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Timber.d("Subscribed to FCM topic: $topic")
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Failed to subscribe to FCM topic: $topic")
            }
        }
    }
    
    /**
     * Show welcome notification on first app launch.
     * Shows in system notification bar AND saves to in-app history.
     */
    private fun showWelcomeNotificationIfFirstLaunch() {
        val prefs = getSharedPreferences("echofy_app", android.content.Context.MODE_PRIVATE)
        val hasShownWelcome = prefs.getBoolean("has_shown_welcome", false)
        
        if (hasShownWelcome) return
        
        // Mark as shown
        prefs.edit().putBoolean("has_shown_welcome", true).apply()
        
        // Welcome message content
        val title = "Welcome to Echofy! 🎵"
        val body = "No ads. No limits. Welcome to Echofy—download your favorites and play offline"
        
        // Show system notification
        try {
            val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            
            // Create channel for Android 8.0+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    "echofy_welcome",
                    "Welcome",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationManager.createNotificationChannel(channel)
            }
            
            val intent = android.content.Intent(this, MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            
            val pendingIntent = android.app.PendingIntent.getActivity(
                this, 0, intent,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
            
            val notification = androidx.core.app.NotificationCompat.Builder(this, "echofy_welcome")
                .setSmallIcon(R.drawable.notification_on)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            
            notificationManager.notify(1, notification)
            Timber.d("Showed welcome notification")
        } catch (e: Exception) {
            Timber.e(e, "Failed to show welcome notification")
        }
        
        // Also save to in-app notification history
        try {
            val notifPrefs = getSharedPreferences("echofy_notifications", android.content.Context.MODE_PRIVATE)
            val notifications = notifPrefs.getStringSet("notifications", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            notifications.add("${System.currentTimeMillis()}|$title|$body|welcome|false")
            notifPrefs.edit().putStringSet("notifications", notifications).apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save welcome to history")
        }
    }

    override fun newImageLoader(): ImageLoader {
        val cacheSize = dataStore[MaxImageCacheSizeKey]

        // will crash app if you set to 0 after cache starts being used
        if (cacheSize == 0) {
            return ImageLoader.Builder(this)
                .crossfade(true)
                .respectCacheHeaders(false)
                .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                .diskCachePolicy(CachePolicy.DISABLED)
                .build()
        }

        return ImageLoader.Builder(this)
            .crossfade(true)
            .respectCacheHeaders(false)
            .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            .diskCache(
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil"))
                    .maxSizeBytes((cacheSize ?: 512) * 1024 * 1024L)
                    .build()
            )
            .build()
    }

    companion object {
        lateinit var instance: App
            private set

        fun forgetAccount(context: Context) {
            runBlocking {
                context.dataStore.edit { settings ->
                    settings.remove(InnerTubeCookieKey)
                    settings.remove(VisitorDataKey)
                    settings.remove(DataSyncIdKey)
                    settings.remove(AccountNameKey)
                    settings.remove(AccountEmailKey)
                    settings.remove(AccountChannelHandleKey)
                }
            }
        }
    }
}