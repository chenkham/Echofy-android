package com.Chenkham.Echofy.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * In-app updater that downloads APK from GitHub releases and installs it.
 */
object InAppUpdater {
    
    private const val TAG = "InAppUpdater"
    private const val GITHUB_REPO = "chenkham/Echofy-android"
    private const val APK_FILENAME = "echofy-update.apk"
    
    /**
     * Download state for UI updates.
     */
    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val progress: Int, val downloadedMB: Float, val totalMB: Float) : DownloadState()
        object Installing : DownloadState()
        data class Error(val message: String) : DownloadState()
        object Complete : DownloadState()
    }
    
    /**
     * Get the APK download URL from GitHub releases.
     */
    suspend fun getApkDownloadUrl(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val assets = json.getJSONArray("assets")
            
            // Find the APK asset
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.getString("name")
                if (name.endsWith(".apk")) {
                    val downloadUrl = asset.getString("browser_download_url")
                    Log.d(TAG, "Found APK: $name at $downloadUrl")
                    return@withContext Result.success(downloadUrl)
                }
            }
            
            Result.failure(Exception("No APK found in latest release"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get download URL: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Download APK with progress updates.
     * Returns a Flow of download states.
     */
    fun downloadApk(context: Context, downloadUrl: String): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0, 0f, 0f))
        
        try {
            val apkFile = File(context.cacheDir, "updates/$APK_FILENAME")
            apkFile.parentFile?.mkdirs()
            
            // Delete old APK if exists
            if (apkFile.exists()) {
                apkFile.delete()
            }
            
            val url = URL(downloadUrl)
            val connection = withContext(Dispatchers.IO) {
                url.openConnection() as HttpURLConnection
            }
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            
            val totalBytes = connection.contentLength.toLong()
            val totalMB = totalBytes / (1024f * 1024f)
            
            withContext(Dispatchers.IO) {
                connection.inputStream.use { input ->
                    apkFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var downloadedBytes = 0L
                        var bytesRead: Int
                        var lastEmitTime = System.currentTimeMillis()
                        
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            
                            // Emit progress every 100ms to avoid overwhelming UI
                            if (System.currentTimeMillis() - lastEmitTime > 100) {
                                val progress = if (totalBytes > 0) {
                                    ((downloadedBytes * 100) / totalBytes).toInt()
                                } else 0
                                val downloadedMB = downloadedBytes / (1024f * 1024f)
                                
                                // Can't emit from withContext, so we'll update at end
                                lastEmitTime = System.currentTimeMillis()
                            }
                        }
                    }
                }
            }
            
            emit(DownloadState.Downloading(100, totalMB, totalMB))
            emit(DownloadState.Installing)
            
            // Install APK
            installApk(context, apkFile)
            
            emit(DownloadState.Complete)
            
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}")
            emit(DownloadState.Error(e.message ?: "Download failed"))
        }
    }
    
    /**
     * Install APK using PackageInstaller.
     */
    private fun installApk(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        context.startActivity(intent)
        Log.d(TAG, "Install intent started for: ${apkFile.absolutePath}")
    }
    
    /**
     * Simple download using DownloadManager (alternative method).
     * Shows download progress in system notification.
     */
    fun downloadWithManager(context: Context, downloadUrl: String) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        
        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle("Echofy Update")
            setDescription("Downloading new version...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, APK_FILENAME)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }
        
        val downloadId = downloadManager.enqueue(request)
        Log.d(TAG, "Download started with ID: $downloadId")
        
        // Register receiver to install when complete
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    
                    if (cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            val uriString = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                            val file = File(Uri.parse(uriString).path ?: return)
                            installApk(context, file)
                        }
                    }
                    cursor.close()
                    context.unregisterReceiver(this)
                }
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_EXPORTED
            )
        } else {
            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }
    }
}
