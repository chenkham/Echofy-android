package com.Chenkham.Echofy.utils

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Chenkham.Echofy.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object ShareStatsTracker {
    private val memoryCache = ConcurrentHashMap<String, MutableStateFlow<Int>>()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    private const val PREFS_NAME = "echofy_share_stats"

    fun observeCount(songId: String): StateFlow<Int> {
        return memoryCache.getOrPut(songId) {
            MutableStateFlow(0)
        }.asStateFlow()
    }

    fun loadCount(context: Context, songId: String) {
        val flow = memoryCache.getOrPut(songId) { MutableStateFlow(0) }

        // Load local cached count first
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val localCount = prefs.getInt(songId, 0)
        if (localCount > flow.value) {
            flow.value = localCount
        }

        // Fetch fresh count from Cloudflare Edge asynchronously
        scope.launch {
            try {
                val baseUrl = ShareUtils.getBaseShareUrl(context)
                val request = Request.Builder()
                    .url("$baseUrl/api/stats?id=$songId")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            val remoteShares = json.optInt("shares", localCount)
                            val finalCount = maxOf(localCount, remoteShares)
                            flow.value = finalCount
                            prefs.edit().putInt(songId, finalCount).apply()
                        }
                    }
                }
            } catch (_: Exception) {
                // Keep local cached count gracefully
            }
        }
    }

    fun increment(context: Context, songId: String) {
        val flow = memoryCache.getOrPut(songId) { MutableStateFlow(0) }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val newCount = prefs.getInt(songId, flow.value) + 1
        flow.value = newCount
        prefs.edit().putInt(songId, newCount).apply()

        // Sync with Cloudflare Edge
        scope.launch {
            try {
                val baseUrl = ShareUtils.getBaseShareUrl(context)
                val request = Request.Builder()
                    .url("$baseUrl/api/share?id=$songId")
                    .post(okhttp3.RequestBody.create(null, ByteArray(0)))
                    .build()
                client.newCall(request).execute().close()
            } catch (_: Exception) {
                // Ignore sync failures silently
            }
        }
    }

    fun formatCount(count: Int): String {
        return when {
            count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fk", count / 1_000.0)
            else -> count.toString()
        }
    }
}

@Composable
fun ShareCountBadge(
    songId: String,
    modifier: Modifier = Modifier
) {
    val count by ShareStatsTracker.observeCount(songId).collectAsState()
    if (count > 0) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            modifier = modifier.padding(vertical = 2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.share),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = "${ShareStatsTracker.formatCount(count)} Shares",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
