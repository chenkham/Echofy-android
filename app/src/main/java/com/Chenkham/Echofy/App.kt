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
import com.Chenkham.Echofy.constants.VisitorDataTimestampKey
import com.Chenkham.Echofy.extensions.toEnum
import com.Chenkham.Echofy.extensions.toInetSocketAddress
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.get
import kotlinx.coroutines.flow.first
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
import javax.inject.Inject
import com.Chenkham.Echofy.ads.AdManager
import com.Chenkham.Echofy.ads.SubscriptionManager

@HiltAndroidApp
class App : Application(), ImageLoaderFactory {
    
    @Inject
    lateinit var adManager: AdManager
    
    @Inject
    lateinit var subscriptionManager: SubscriptionManager

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        androidx.multidex.MultiDex.install(this)
    }
    
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        instance = this;
        Timber.plant(Timber.DebugTree())
        
        // PERFORMANCE: Set locale immediately with system defaults (no IO blocking)
        val locale = Locale.getDefault()
        val systemCountry = locale.country.takeIf { it in CountryCodeToName } ?: "US"
        val languageTag = locale.toLanguageTag().replace("-Hant", "")
        val systemLanguage = locale.language.takeIf { it in LanguageCodeToName }
            ?: languageTag.takeIf { it in LanguageCodeToName }
            ?: "en"

        YouTube.locale = YouTubeLocale(gl = systemCountry, hl = systemLanguage)
        
        if (languageTag == "zh-TW") {
            KuGou.useTraditionalChinese = true
        }

        // PERFORMANCE: Phase 1 - Critical initialization (< 100ms delay)
        GlobalScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(50)
            initializeVisitorData(systemCountry, systemLanguage)
        }

        // PERFORMANCE: Phase 2 - User preferences (200ms delay)
        GlobalScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(200)
            applyUserPreferences(systemCountry, systemLanguage)
        }

        // PERFORMANCE: Phase 3 - Auth/cookies (500ms delay)
        GlobalScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(500)
            initializeAuthState()
        }

        // PERFORMANCE: Phase 4 - AdMob initialization (3 seconds delay)
        GlobalScope.launch(Dispatchers.Main) {
            kotlinx.coroutines.delay(3000)
            adManager.initialize()
        }
        
        // PERFORMANCE: Phase 4.5 - Subscription Manager (3.5 seconds delay)
        GlobalScope.launch(Dispatchers.Main) {
            kotlinx.coroutines.delay(3500)
            subscriptionManager.initialize()
        }

        // PERFORMANCE: Phase 5 - FCM topics (5 seconds delay)
        GlobalScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(5000)
            subscribeToFcmTopics()
        }

        // PERFORMANCE: Phase 6 - Firebase In-App Messaging (4 seconds delay)
        GlobalScope.launch(Dispatchers.Main) {
            kotlinx.coroutines.delay(4000)
            initializeFirebaseInAppMessaging()
        }

        // PERFORMANCE: Phase 7 - Welcome notification (6 seconds delay)
        GlobalScope.launch(Dispatchers.Main) {
            kotlinx.coroutines.delay(6000)
            showWelcomeNotificationIfFirstLaunch()
        }
    }

    /**
     * Initialize visitorData for YouTube API calls.
     * - If stored data is fresh (<6h), use it immediately.
     * - Always schedules a periodic refresh every 6 hours.
     * - Retries the fetch up to 3 times with exponential backoff on failure.
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun initializeVisitorData(systemCountry: String, systemLanguage: String) {
        GlobalScope.launch(Dispatchers.IO) {
            // Observe store changes (e.g. after login/logout) and apply to YouTube
            dataStore.data
                .map { it[VisitorDataKey] }
                .distinctUntilChanged()
                .collect { stored ->
                    YouTube.visitorData = stored?.takeIf { it.isNotBlank() && it != "null" }
                }
        }

        GlobalScope.launch(Dispatchers.IO) {
            // On startup: refresh only if stale (>6 hours old) or missing
            val stored = dataStore.data.first()[VisitorDataKey]
            val timestamp = dataStore.data.first()[VisitorDataTimestampKey] ?: 0L
            val ageMs = System.currentTimeMillis() - timestamp
            val sixHoursMs = 6 * 60 * 60 * 1000L

            if (stored.isNullOrBlank() || stored == "null" || ageMs > sixHoursMs) {
                android.util.Log.d("App", "VisitorData is stale (${ageMs / 1000}s old) or missing — refreshing")
                fetchAndSaveVisitorData()
            } else {
                android.util.Log.d("App", "VisitorData is fresh (${ageMs / 1000}s old) — skipping startup refresh")
                YouTube.visitorData = stored
            }

            // Periodic refresh: every 6 hours
            while (true) {
                kotlinx.coroutines.delay(sixHoursMs)
                android.util.Log.d("App", "Periodic visitorData refresh triggered")
                fetchAndSaveVisitorData()
            }
        }
    }

    /**
     * Fetch fresh visitor data from YouTube with exponential backoff retry.
     * Saves result to DataStore and applies it to [YouTube.visitorData] immediately.
     */
    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun fetchAndSaveVisitorData() {
        val maxRetries = 3
        val backoffDelaysMs = listOf(2_000L, 4_000L, 8_000L)

        for (attempt in 1..maxRetries) {
            android.util.Log.d("App", "Fetching visitorData (attempt $attempt/$maxRetries)")
            val result = YouTube.visitorData()
            if (result.isSuccess) {
                val newData = result.getOrNull() ?: continue
                android.util.Log.d("App", "VisitorData refreshed successfully: ${newData.take(20)}...")
                YouTube.visitorData = newData
                dataStore.edit { prefs ->
                    prefs[VisitorDataKey] = newData
                    prefs[VisitorDataTimestampKey] = System.currentTimeMillis()
                }
                return
            } else {
                val error = result.exceptionOrNull()
                android.util.Log.w("App", "VisitorData fetch failed (attempt $attempt): ${error?.message}")
                reportException(error ?: Exception("Unknown visitorData fetch error"))
                if (attempt < maxRetries) {
                    kotlinx.coroutines.delay(backoffDelaysMs[attempt - 1])
                }
            }
        }
        android.util.Log.e("App", "All $maxRetries visitorData fetch attempts failed")
    }

    /**
     * Apply user preferences for proxy, locale, etc.
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun applyUserPreferences(systemCountry: String, systemLanguage: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val userCountry = dataStore[ContentCountryKey]?.takeIf { it != SYSTEM_DEFAULT }
            val userLanguage = dataStore[ContentLanguageKey]?.takeIf { it != SYSTEM_DEFAULT }

            if (userCountry != null || userLanguage != null) {
                YouTube.locale = YouTubeLocale(
                    gl = userCountry ?: systemCountry,
                    hl = userLanguage ?: systemLanguage
                )
            }

            if (dataStore[ProxyEnabledKey] == true) {
                try {
                    YouTube.proxy = Proxy(
                        dataStore[ProxyTypeKey].toEnum(defaultValue = Proxy.Type.HTTP),
                        dataStore[ProxyUrlKey]!!.toInetSocketAddress()
                    )
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@App, "Failed to parse proxy url.", LENGTH_SHORT).show()
                    }
                    reportException(e)
                }
            }

            if (dataStore[UseLoginForBrowse] != false) {
                YouTube.useLoginForBrowse = true
            }
        }
    }

    /**
     * Initialize authentication state (cookies, dataSyncId)
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun initializeAuthState() {
        // DataSyncId
        GlobalScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it[DataSyncIdKey] }
                .distinctUntilChanged()
                .collect { dataSyncId ->
                    YouTube.dataSyncId = dataSyncId?.let {
                        it.takeIf { !it.contains("||") }
                            ?: it.takeIf { it.endsWith("||") }?.substringBefore("||")
                            ?: it.substringAfter("||")
                    }
                }
        }

        // Cookie
        GlobalScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .collect { cookie ->
                    try {
                        YouTube.cookie = cookie
                    } catch (e: Exception) {
                        Timber.e("Could not parse cookie. Clearing existing cookie. %s", e.message)
                        forgetAccount(this@App)
                    }
                }
        }
    }
    
    /**
     * Called when the app is terminating. Clear cache if auto-clear preference is enabled.
     */
    @OptIn(DelicateCoroutinesApi::class)
    override fun onTerminate() {
        super.onTerminate()
        
        // Check if auto-clear cache is enabled
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val autoClearEnabled = dataStore[com.Chenkham.Echofy.constants.AutoClearCacheOnCloseKey] == true
                if (autoClearEnabled) {
                    Timber.d("Auto-clearing cache on app close")
                    clearAllCaches()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to auto-clear cache")
            }
        }
    }
    
    /**
     * Clears all app caches (image cache and song cache).
     */
    private fun clearAllCaches() {
        try {
            // Clear image cache (Coil)
            val imageCacheDir = cacheDir.resolve("coil")
            imageCacheDir.deleteRecursively()
            Timber.d("Image cache cleared")
            
            // Clear ExoPlayer cache
            val playerCacheDir = cacheDir.resolve("exoplayer")
            playerCacheDir.deleteRecursively()
            Timber.d("Player cache cleared")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear caches")
        }
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

        // PERFORMANCE: Professional-grade image loading like Spotify/YT Music
        // Key optimizations:
        // 1. Larger memory cache (30%) for instant scrolling - commercial apps use 25-35%
        // 2. Hardware bitmaps for GPU rendering (Android P+)
        // 3. Aggressive caching policies - never re-validate cached images
        // 4. NO crossfade for cached images - instant display
        // 5. OkHttp with connection pooling for faster network loads
        // 6. Weak references disabled - keep images longer in memory
        // 7. Larger disk cache default (1GB) for offline performance

        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .connectionPool(okhttp3.ConnectionPool(15, 5, java.util.concurrent.TimeUnit.MINUTES))
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        // will crash app if you set to 0 after cache starts being used
        if (cacheSize == 0) {
            return ImageLoader.Builder(this)
                .crossfade(false) // No crossfade for instant display
                .respectCacheHeaders(false)
                .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                .diskCachePolicy(CachePolicy.DISABLED)
                .okHttpClient(okHttpClient)
                .build()
        }

        return ImageLoader.Builder(this)
            .crossfade(false) // NO crossfade for instant display from cache
            .respectCacheHeaders(false) // Don't re-validate cached images
            .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) // GPU rendering
            .okHttpClient(okHttpClient)
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(0.30) // 30% of app memory for instant scrolling
                    .strongReferencesEnabled(true) // Keep frequently used images
                    .weakReferencesEnabled(false) // Don't use weak refs - keep images longer
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil"))
                    .maxSizeBytes((cacheSize ?: 1024) * 1024 * 1024L) // 1GB default cache
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(cacheSize?.let { if (it > 0) CachePolicy.ENABLED else CachePolicy.DISABLED } ?: CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED) // Cache network responses
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