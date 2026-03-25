package com.Chenkham.Echofy.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import com.Chenkham.Echofy.MainActivity
import com.Chenkham.Echofy.auth.AuthRepository
import com.Chenkham.Echofy.constants.VoiceControlEnabledKey
import com.Chenkham.Echofy.utils.dataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class VoiceControlReceiver : BroadcastReceiver() {

    @Inject
    lateinit var authRepository: AuthRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val ACTION_VOICE_COMMAND = "com.Chenkham.Echofy.ACTION_VOICE_COMMAND"
        const val EXTRA_JSON_DATA = "com.Chenkham.Echofy.EXTRA_JSON_DATA"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_VOICE_COMMAND) {
            val pendingResult = goAsync()
            
            scope.launch {
                try {
                    // 1. Check if Feature is Enabled in Settings
                    val isEnabled = context.dataStore.data.first()[VoiceControlEnabledKey] ?: false
                    
                    if (!isEnabled) {
                        Log.w("VoiceControlReceiver", "Ignoring command: Enabled=$isEnabled")
                        pendingResult.finish()
                        return@launch
                    }

                    val jsonString = intent.getStringExtra(EXTRA_JSON_DATA) ?: return@launch
                    try {
                        val json = JSONObject(jsonString)
                        val action = json.optString("action")
                        val query = json.optString("query")

                        Log.d("VoiceControlReceiver", "Processing action: $action, query: $query")

                        when (action.lowercase()) {
                            "play" -> {
                                if (query.isNotEmpty()) {
                                    // Delegate "Play [song]" to MainActivity's existing handler
                                    val playIntent = Intent(context, MainActivity::class.java).apply {
                                        this.action = android.provider.MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH
                                        putExtra(android.app.SearchManager.QUERY, query)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    }
                                    context.startActivity(playIntent)
                                } else {
                                    // Just "Play" -> Resume playback via MediaButton
                                    sendMediaButton(context, KeyEvent.KEYCODE_MEDIA_PLAY)
                                }
                            }
                            "pause" -> sendMediaButton(context, KeyEvent.KEYCODE_MEDIA_PAUSE)
                            "stop" -> sendMediaButton(context, KeyEvent.KEYCODE_MEDIA_STOP)
                            "next", "skip" -> sendMediaButton(context, KeyEvent.KEYCODE_MEDIA_NEXT)
                            "previous", "prev" -> sendMediaButton(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                        }
                    } catch (e: Exception) {
                        Log.e("VoiceControlReceiver", "Error parsing JSON command", e)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun sendMediaButton(context: Context, keycode: Int) {
        val event = KeyEvent(KeyEvent.ACTION_DOWN, keycode)
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            putExtra(Intent.EXTRA_KEY_EVENT, event)
            // Explicitly target the MediaButtonReceiver to avoid ambiguity
            component = android.content.ComponentName(context, androidx.media3.session.MediaButtonReceiver::class.java)
        }
        context.sendBroadcast(intent)
        
        // Send UP event immediately after for some receivers that expect both
        val eventUp = KeyEvent(KeyEvent.ACTION_UP, keycode)
        val intentUp = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            putExtra(Intent.EXTRA_KEY_EVENT, eventUp)
            component = android.content.ComponentName(context, androidx.media3.session.MediaButtonReceiver::class.java)
        }
        context.sendBroadcast(intentUp)
    }
}
