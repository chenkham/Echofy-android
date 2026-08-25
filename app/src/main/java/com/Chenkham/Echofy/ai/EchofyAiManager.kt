package com.Chenkham.Echofy.ai

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.Chenkham.Echofy.db.InternalDatabase
import com.Chenkham.Echofy.db.entities.PlaylistEntity
import com.Chenkham.Echofy.db.entities.PlaylistSongMap
import com.Chenkham.Echofy.playback.PlayerConnection
import com.Chenkham.Echofy.playback.queues.YouTubeQueue
import com.Chenkham.Echofy.models.toMediaMetadata
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.SongItem
import com.arturo254.opentune.innertube.models.WatchEndpoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale
import java.util.UUID
import timber.log.Timber

enum class InputSource { TEXT, VOICE, HEY_COMMAND }

enum class AiCallState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED_IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

enum class AiModelOption(
    val displayName: String,
    val modelId: String,
    val contextWindow: String,
    val description: String
) {
    DEFAULT("Default", "default", "128K", "Free & Unlimited Cloud Agent"),
    FAST("Fast", "openai", "128K", "Fast conversational model"),
    LLAMA("Llama", "llama", "128K", "High precision music reasoning")
}

enum class AiModelStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    READY
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class AgentAction(
    val speakResponse: String,
    val actionType: String,
    val query: String = "",
    val playlistName: String = "",
    val volumeLevel: Int = 70
)

class EchofyAiManager private constructor(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _callState = MutableStateFlow(AiCallState.DISCONNECTED)
    val callState: StateFlow<AiCallState> = _callState.asStateFlow()

    private val _selectedModel = MutableStateFlow(AiModelOption.DEFAULT)
    val selectedModel: StateFlow<AiModelOption> = _selectedModel.asStateFlow()

    private val _modelStatus = MutableStateFlow(AiModelStatus.READY)
    val modelStatus: StateFlow<AiModelStatus> = _modelStatus.asStateFlow()

    private val _downloadProgress = MutableStateFlow(1.0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _lastSpokenText = MutableStateFlow("")
    val lastSpokenText: StateFlow<String> = _lastSpokenText.asStateFlow()

    private val _aiResponseText = MutableStateFlow("")
    val aiResponseText: StateFlow<String> = _aiResponseText.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _isAiActiveVisual = MutableStateFlow(false)
    val isAiActiveVisual: StateFlow<Boolean> = _isAiActiveVisual.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isTtsInitialized = false
    private var isCallActive = false

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val database = InternalDatabase.newInstance(context)

    init {
        initTts()
        // Chat history lives in SharedPreferences and is parsed as JSON, which StrictMode
        // measured at ~1.5s on the main thread during MainActivity.onCreate. It grows with
        // every conversation, so the freeze got worse the more the user chatted. Loading it
        // on the IO dispatcher lets the UI draw immediately; _chatHistory starts empty and
        // the screen updates through the StateFlow once the read finishes.
        scope.launch(Dispatchers.IO) {
            loadChatHistoryFromDisk()
        }
        scope.launch {
            _callState.collect { state ->
                _isAiActiveVisual.value = state == AiCallState.LISTENING || state == AiCallState.THINKING || state == AiCallState.SPEAKING
            }
        }
    }

    fun selectModel(model: AiModelOption) {
        _selectedModel.value = model
        _modelStatus.value = AiModelStatus.READY
    }

    fun clearChat() {
        _chatHistory.value = emptyList()
        saveChatHistoryToDisk(emptyList())
    }

    private fun saveChatHistoryToDisk(history: List<ChatMessage>) {
        runCatching {
            val prefs = context.getSharedPreferences("echofy_ai_chat_prefs", Context.MODE_PRIVATE)
            val arr = JSONArray()
            history.forEach { msg ->
                arr.put(JSONObject().apply {
                    put("id", msg.id)
                    put("text", msg.text)
                    put("isUser", msg.isUser)
                    put("timestamp", msg.timestamp)
                })
            }
            prefs.edit().putString("chat_history_json", arr.toString()).apply()
        }
    }

    private fun loadChatHistoryFromDisk() {
        runCatching {
            val prefs = context.getSharedPreferences("echofy_ai_chat_prefs", Context.MODE_PRIVATE)
            val rawStr = prefs.getString("chat_history_json", null) ?: return
            val arr = JSONArray(rawStr)
            val list = mutableListOf<ChatMessage>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ChatMessage(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        text = obj.optString("text", ""),
                        isUser = obj.optBoolean("isUser", false),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            _chatHistory.value = list
        }
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
                isTtsInitialized = true
            }
        }
    }

    fun startAiCall() {
        if (isCallActive) return
        isCallActive = true
        _callState.value = AiCallState.CONNECTING

        scope.launch {
            val greeting = "Hey! I'm here. What's up?"
            _aiResponseText.value = greeting
            addAiMessage(greeting)
            speakHumanVoice(greeting) {
                if (isCallActive) {
                    startListeningLoop()
                }
            }
        }
    }

    fun endAiCall() {
        isCallActive = false
        stopListening()
        stopAudioPlayback()
        _callState.value = AiCallState.DISCONNECTED
        _aiResponseText.value = ""
        _lastSpokenText.value = ""
    }

    // ── Hey Jarvis OpenWakeWord Detector ─────────────────────────
    private var openWakeWordDetector: OpenWakeWordDetector? = null
    private val _isHeyListening = MutableStateFlow(false)
    val isHeyListening: StateFlow<Boolean> = _isHeyListening.asStateFlow()

    private var recognitionJob: Job? = null

    /** Drives the always-on recognition loop while driving mode is active. */
    private var drivingModeJob: Job? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val heyListenerLock = kotlinx.coroutines.sync.Mutex()

    /** Latest PlayerConnection, kept current across screen recompositions. */
    private var activePlayerConnection: PlayerConnection? = null

    /** True between the wake word firing and the command being captured. */
    private val _isAwaitingCommand = MutableStateFlow(false)
    val isAwaitingCommand: StateFlow<Boolean> = _isAwaitingCommand.asStateFlow()

    /** Words recognised so far, streamed live into the assistant overlay. */
    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    /**
     * True while hands-free driving mode is active. Exposed so the in-app banner
     * and the AI screen can reflect the same state as the system notification.
     */
    private val _isDrivingMode = MutableStateFlow(false)
    val isDrivingMode: StateFlow<Boolean> = _isDrivingMode.asStateFlow()

    /**
     * Switches between the two input models.
     *
     * Hey Jarvis mode idles on the ONNX wake word detector and only opens the
     * speech recognizer once the wake word fires. Driving mode skips the wake word
     * entirely and keeps the recognizer cycling, so a driver never has to preface
     * a command or touch the screen.
     */
    fun setDrivingMode(enabled: Boolean) {
        if (_isDrivingMode.value == enabled) return
        _isDrivingMode.value = enabled

        if (enabled) {
            // The detector and the recognizer cannot share the microphone.
            scope.launch(Dispatchers.Main) {
                openWakeWordDetector?.stopListeningAndAwait()
                startDrivingModeLoop()
            }
        } else {
            stopDrivingModeLoop()
            // Fall back to wake word listening if the assistant is still enabled.
            scope.launch(Dispatchers.Main) {
                if (_isHeyListening.value) {
                    openWakeWordDetector?.startListening()
                }
            }
        }
    }

    /**
     * Continuously captures commands with no wake word. Each recognition pass is
     * one-shot, so the loop immediately starts the next pass when one completes.
     */
    private fun startDrivingModeLoop() {
        if (drivingModeJob?.isActive == true) return

        if (!hasRecordAudioPermission()) {
            Timber.w("Cannot start driving mode: RECORD_AUDIO not granted")
            return
        }

        drivingModeJob = scope.launch(Dispatchers.Main) {
            Timber.d("Driving mode listening loop started")
            while (isActive && _isDrivingMode.value) {
                _liveTranscript.value = ""
                _isAwaitingCommand.value = true
                _callState.value = AiCallState.LISTENING

                val spoken = suspendCancellableCoroutine<String> { cont ->
                    startSingleListening { text ->
                        if (cont.isActive) cont.resume(text) {}
                    }
                }

                if (!isActive || !_isDrivingMode.value) break

                if (spoken.isNotBlank()) {
                    _liveTranscript.value = spoken
                    // Driving mode always speaks its reply; the driver cannot look.
                    processUserVoiceInput(spoken, activePlayerConnection, InputSource.HEY_COMMAND)
                    // Let the response play out before reopening the microphone,
                    // otherwise the recognizer transcribes our own TTS.
                    kotlinx.coroutines.delay(DRIVING_MODE_RESPONSE_GRACE_MS)
                } else {
                    // Silence is normal while driving; just cycle again.
                    kotlinx.coroutines.delay(DRIVING_MODE_IDLE_GAP_MS)
                }
            }

            _isAwaitingCommand.value = false
            _liveTranscript.value = ""
            Timber.d("Driving mode listening loop ended")
        }
    }

    private fun stopDrivingModeLoop() {
        drivingModeJob?.cancel()
        drivingModeJob = null
        _isAwaitingCommand.value = false
        _liveTranscript.value = ""
        Timber.d("Driving mode listening loop stopped")
    }

    private fun playWakeAcknowledgementTone() {
        try {
            val toneGen = android.media.ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            toneGen.startTone(android.media.ToneGenerator.TONE_CDMA_PIP, 150)
            // ToneGenerator holds a native AudioTrack; release it once the tone ends.
            scope.launch {
                kotlinx.coroutines.delay(300)
                runCatching { toneGen.release() }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to play wake acknowledgement tone")
        }
    }

    private fun requestAudioFocusForVoice(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAcceptsDelayedFocusGain(false)
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                            Timber.d("Audio focus lost for Hey Jarvis listener")
                        }
                    }
                }
                .build()

            val result = audioManager.requestAudioFocus(audioFocusRequest!!)
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                            Timber.d("Audio focus lost for Hey Jarvis listener")
                        }
                    }
                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocusForVoice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    /**
     * Points the assistant at the live player without touching listener state.
     * Used by UI that owns a PlayerConnection so wake word commands can control
     * playback even though the listener itself is started elsewhere.
     */
    fun attachPlayerConnection(playerConnection: PlayerConnection?) {
        if (playerConnection != null) {
            activePlayerConnection = playerConnection
        }
    }

    /** True when RECORD_AUDIO has been granted; the mic cannot be opened without it. */
    private fun hasRecordAudioPermission(): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    fun startHeyCommandListener(playerConnection: PlayerConnection? = null) {
        scope.launch(Dispatchers.Main) {
            heyListenerLock.withLock {
                // Refresh the player reference even when already listening, so the
                // screen can hand over a live connection to an in-flight service.
                if (playerConnection != null) {
                    activePlayerConnection = playerConnection
                }

                if (_isHeyListening.value) {
                    Timber.d("Hey Jarvis listener already running")
                    return@launch
                }

                if (!hasRecordAudioPermission()) {
                    // Without this guard AudioRecord fails silently and the detector
                    // looks "on" while never hearing anything.
                    Timber.w("Cannot start Hey Jarvis listener: RECORD_AUDIO not granted")
                    return@launch
                }

                try {
                    // Only overwrite the cached connection when a real one is supplied.
                    // WakeWordService starts the listener with no argument, and clearing
                    // it here would leave playback commands pointing at a null player.
                    if (playerConnection != null) {
                        activePlayerConnection = playerConnection
                    }

                    if (openWakeWordDetector == null) {
                        openWakeWordDetector = OpenWakeWordDetector(context) {
                            Timber.i("Hey Jarvis wake word triggered via ONNX engine")

                            scope.launch(Dispatchers.Main) {
                                // The detector owns the microphone. SpeechRecognizer
                                // cannot capture anything until it is fully released,
                                // so wait for teardown before listening for a command.
                                openWakeWordDetector?.stopListeningAndAwait()

                                // Duck music only for the command utterance, not while
                                // passively waiting for the wake word.
                                requestAudioFocusForVoice()

                                // Show the assistant overlay and beep so the user knows
                                // exactly when to speak their command.
                                _liveTranscript.value = ""
                                _isAwaitingCommand.value = true
                                _callState.value = AiCallState.LISTENING
                                playWakeAcknowledgementTone()

                                // Let the tone finish and AudioFlinger actually free the
                                // input. Starting the recognizer immediately makes it hear
                                // the beep as speech onset, or get no audio at all.
                                kotlinx.coroutines.delay(MIC_SETTLE_DELAY_MS)

                                startSingleListening { spokenText ->
                                    abandonAudioFocusForVoice()

                                    if (spokenText.isNotBlank()) {
                                        // Keep the final text on screen briefly so the
                                        // user can read what was understood.
                                        _liveTranscript.value = spokenText
                                        scope.launch {
                                            kotlinx.coroutines.delay(1200)
                                            _isAwaitingCommand.value = false
                                            _liveTranscript.value = ""
                                        }
                                        processUserVoiceInput(
                                            spokenText,
                                            activePlayerConnection,
                                            InputSource.HEY_COMMAND
                                        )
                                    } else {
                                        Timber.d("No command heard after wake word")
                                        _isAwaitingCommand.value = false
                                        _liveTranscript.value = ""
                                        _callState.value = AiCallState.CONNECTED_IDLE
                                    }

                                    // Resume wake word listening after command completes
                                    scope.launch {
                                        kotlinx.coroutines.delay(1000)
                                        if (_isHeyListening.value && !_isDrivingMode.value) {
                                            openWakeWordDetector?.startListening()
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // In driving mode the recognizer owns the microphone continuously,
                    // so the wake word detector must stay off to avoid contention.
                    if (_isDrivingMode.value) {
                        startDrivingModeLoop()
                    } else {
                        openWakeWordDetector?.startListening()
                    }
                    _isHeyListening.value = true
                    Timber.d("Voice assistant started (drivingMode=${_isDrivingMode.value})")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to start Hey Jarvis OpenWakeWord detector")
                    _isHeyListening.value = false
                    abandonAudioFocusForVoice()
                }
            }
        }
    }

    fun stopHeyCommandListener() {
        scope.launch(Dispatchers.Main) {
            heyListenerLock.withLock {
                Timber.d("Stopping Hey Jarvis listener")
                _isHeyListening.value = false
                _isAwaitingCommand.value = false
                // Driving mode rides on top of the assistant, so it cannot outlive it.
                _isDrivingMode.value = false
                stopDrivingModeLoop()
                recognitionJob?.cancel()
                recognitionJob = null
                openWakeWordDetector?.stopListeningAndAwait()
                abandonAudioFocusForVoice()
            }
        }
    }

    fun startSingleListening(onResult: (String) -> Unit) {
        scope.launch(Dispatchers.Main) {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                // No recognition service (common on de-Googled ROMs / some emulators).
                Timber.e("No speech recognition service available on this device")
                _lastSpokenText.value = "Speech recognition unavailable on this device"
                onResult("")
                return@launch
            }

            try {
                val singleRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    // Required by several recognizer implementations; without it they
                    // reject the request outright with ERROR_CLIENT.
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    // Stream words to the UI as they are recognised, like Assistant.
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    // Give the user a real window to start talking. The previous
                    // 1.2 s budget expired before many users said their first word,
                    // which surfaced as ERROR_SPEECH_TIMEOUT and a dropped command.
                    // Note: these extras are advisory and some recognizers clamp them.
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000L)
                }

                // Guarantees onResult runs exactly once, whichever path completes first.
                var delivered = false
                var lastPartial = ""
                // Audio diagnostics: distinguishes "mic delivered no audio" from
                // "mic worked but the recognizer rejected the speech".
                var peakRms = Float.NEGATIVE_INFINITY
                var sawSpeech = false
                val deliver: (String) -> Unit = { text ->
                    if (!delivered) {
                        delivered = true
                        runCatching { singleRecognizer.destroy() }
                        onResult(text)
                    }
                }

                singleRecognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Timber.d("Recognizer ready; waiting for command")
                    }

                    override fun onBeginningOfSpeech() {
                        sawSpeech = true
                        Timber.d("Command speech started")
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        if (rmsdB > peakRms) peakRms = rmsdB
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        Timber.d("Command speech ended")
                    }

                    override fun onError(error: Int) {
                        val reason = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "client side error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "missing RECORD_AUDIO permission"
                            SpeechRecognizer.ERROR_NETWORK -> "network error"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "no match"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "recognizer busy"
                            SpeechRecognizer.ERROR_SERVER -> "server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "no speech detected"
                            else -> "unknown error $error"
                        }
                        Timber.w(
                            "Command recognition failed: %s (code %d) | peakRms=%.1f dB, speechDetected=%b, partial=\"%s\"",
                            reason, error, peakRms, sawSpeech, lastPartial
                        )
                        if (!sawSpeech && peakRms < 0f) {
                            // Strong signal that the mic never actually delivered audio,
                            // rather than the user simply staying silent.
                            Timber.w("Recognizer received no usable audio; microphone may still be held elsewhere")
                        }
                        // Partial text is often correct even when the final pass
                        // reports NO_MATCH, so prefer it over discarding the command.
                        deliver(lastPartial)
                    }

                    override fun onResults(results: Bundle?) {
                        val text = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            .orEmpty()
                            .ifBlank { lastPartial }
                        Timber.d("Command recognised: \"$text\"")
                        deliver(text)
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val text = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            .orEmpty()
                        if (text.isNotBlank()) {
                            lastPartial = text
                            _liveTranscript.value = text
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                singleRecognizer.startListening(intent)

                // Some recognizers never call back at all; don't strand the detector.
                scope.launch {
                    kotlinx.coroutines.delay(COMMAND_TIMEOUT_MS)
                    withContext(Dispatchers.Main) {
                        if (!delivered) {
                            Timber.w("Command recognition timed out after ${COMMAND_TIMEOUT_MS}ms")
                            runCatching { singleRecognizer.stopListening() }
                            deliver(lastPartial)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to start command listener")
                onResult("")
            }
        }
    }

    private fun startListeningLoop() {
        if (!isCallActive) return
        _callState.value = AiCallState.LISTENING

        scope.launch(Dispatchers.Main) {
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _callState.value = AiCallState.THINKING
                    }

                    override fun onError(error: Int) {
                        if (isCallActive) {
                            scope.launch {
                                kotlinx.coroutines.delay(1000)
                                if (isCallActive && _callState.value != AiCallState.SPEAKING) {
                                    startListeningLoop()
                                }
                            }
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val spokenText = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        if (!spokenText.isNullOrEmpty()) {
                            _lastSpokenText.value = spokenText
                            processUserVoiceInput(spokenText)
                        } else if (isCallActive) {
                            startListeningLoop()
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                _callState.value = AiCallState.CONNECTED_IDLE
            }
        }
    }

    private fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) { Timber.w(e, "Ignored exception") }
    }

    /**
     * Called when app goes to background - releases audio focus but keeps state
     * for quick resume when app returns to foreground
     */
    fun onAppBackground() {
        if (_isHeyListening.value) {
            Timber.d("App backgrounded - pausing hey listener")
            abandonAudioFocusForVoice()
        }
    }

    /**
     * Called when app returns to foreground - re-requests audio focus
     * and resumes listening if it was active
     */
    fun onAppForeground(playerConnection: PlayerConnection?) {
        if (_isHeyListening.value) {
            Timber.d("App foregrounded - resuming hey listener")
            // Will automatically re-request audio focus on next restart cycle
        }
    }

    fun sendTextMessage(textInput: String, playerConnection: PlayerConnection? = null) {
        if (textInput.isBlank()) return
        processUserVoiceInput(textInput, playerConnection, InputSource.TEXT)
    }

    fun processUserVoiceInput(userInput: String, playerConnection: PlayerConnection? = null, source: InputSource = InputSource.VOICE) {
        // Voice commands arriving from WakeWordService carry no player, so fall back
        // to the connection MainActivity attached. Without this, PLAY_SONG and the
        // other playback actions silently no-op on a null player.
        val player = playerConnection ?: activePlayerConnection
        if (playerConnection != null) activePlayerConnection = playerConnection

        val cleanInput = userInput
            .replace(Regex("(?i)^hey\\s+jarvis\\b[,.]?\\s*"), "")
            .trim()
            .ifEmpty { userInput }
        _callState.value = AiCallState.THINKING
        _lastSpokenText.value = cleanInput

        // Wake word commands are shown in chat too, so the user gets visible
        // confirmation of what was heard and what the assistant did.
        addUserMessage(cleanInput)

        scope.launch {
            _aiResponseText.value = "Thinking..."

            val userContext = withContext(Dispatchers.IO) { buildUserContext(player) }

            val agentAction = withContext(Dispatchers.IO) {
                queryAiAgent(cleanInput, _chatHistory.value, userContext, source)
            }

            executeAgentAction(agentAction, player, source)
        }
    }

    private suspend fun buildUserContext(playerConnection: PlayerConnection?): String {
        val sb = StringBuilder()

        try {
            val metadata = withContext(Dispatchers.Main) { playerConnection?.mediaMetadata?.value }
            if (metadata != null) {
                val artistNames = metadata.artists.joinToString(", ") { it.name }
                sb.appendLine("[Now Playing] \"${metadata.title}\" by $artistNames")
            }
        } catch (e: Exception) { Timber.w(e, "Ignored exception") }

        try {
            val events = database.events().first().take(6)
            if (events.isNotEmpty()) {
                sb.appendLine("[History]")
                events.forEach { ev ->
                    sb.appendLine("- \"${ev.song.song.title}\" (${ev.song.artists.firstOrNull()?.name.orEmpty()})")
                }
            }
        } catch (e: Exception) { Timber.w(e, "Ignored exception") }

        try {
            val liked = database.likedSongsByRowIdAsc().first().take(6)
            if (liked.isNotEmpty()) {
                sb.appendLine("[Liked]")
                liked.forEach { s ->
                    sb.appendLine("- \"${s.song.title}\"")
                }
            }
        } catch (e: Exception) { Timber.w(e, "Ignored exception") }

        return sb.toString().take(1000)
    }

    private val aiSettingsStore = AiSettingsDataStore(context)
    private val universalAiEngine = UniversalAiEngine()

    private suspend fun queryAiAgent(
        userPrompt: String,
        history: List<ChatMessage>,
        userContext: String,
        source: InputSource = InputSource.VOICE
    ): AgentAction {
        val rawLower = userPrompt.lowercase(Locale.getDefault()).trim()
        // Only strip a leading wake word, not any word starting with "hey", so
        // requests like "play Hey Jude" keep their song title intact.
        val cmd = rawLower.replace(Regex("^hey\\s+jarvis\\b[,.]?\\s*"), "")
            .replace(Regex("^hey\\b[,.]?\\s*"), "")
            .trim()

        if (source == InputSource.HEY_COMMAND && cmd.isEmpty()) {
            return AgentAction("", "WAKE_ACK")
        }

        // 1. FAST LOCAL RULE INTERCEPTOR (0ms Latency - Direct execution for all playback commands)
        // Bare transport controls must be matched exactly, before the "play <song>"
        // prefix rules, so "play" alone resumes instead of searching for a song.
        if (cmd == "play" || cmd == "resume" || cmd == "unpause" ||
            cmd == "continue" || cmd == "start" || cmd == "play music" ||
            cmd == "resume music" || cmd == "continue playing" || cmd.contains("unpause")
        ) {
            return AgentAction("Resuming playback!", "RESUME")
        }
        if (cmd.contains("pause") || cmd.contains("stop") || cmd.contains("hold on")) {
            return AgentAction("Paused playback!", "PAUSE")
        }
        // Strip an optional leading verb plus filler words ("play the song X").
        val playMatch = Regex(
            "^(?:play|put on|start|listen to)\\s+(?:me\\s+)?(?:the\\s+)?(?:song\\s+|track\\s+|music\\s+)?(.+)$"
        ).find(cmd)
        if (playMatch != null) {
            val songName = playMatch.groupValues[1]
                .replace(Regex("\\s+(?:please|now|for me)$"), "")
                .trim()
            if (songName.isNotBlank()) {
                return AgentAction("Playing $songName for you!", "PLAY_SONG", query = songName)
            }
        }
        if (cmd.contains("next") || cmd.contains("skip")) {
            return AgentAction("Skipping to next track!", "NEXT")
        }
        if (cmd.contains("previous") || cmd.contains("go back") || cmd == "back") {
            return AgentAction("Going back to previous track!", "PREVIOUS")
        }
        if (cmd.contains("volume")) {
            val volMatch = Regex("\\d+").find(cmd)
            val level = volMatch?.value?.toIntOrNull()?.coerceIn(0, 100) ?: 70
            return AgentAction("Setting volume to $level%", "SET_VOLUME", volumeLevel = level)
        }
        if (cmd.contains("shuffle") || cmd.contains("mix it up")) {
            return AgentAction("Shuffle enabled! 🔀", "SHUFFLE")
        }
        if (cmd.contains("repeat") || cmd.contains("loop")) {
            return AgentAction("Repeat mode toggled! 🔁", "REPEAT")
        }
        if (cmd.contains("what's playing") || cmd.contains("whats playing") || cmd.contains("what song") || cmd.contains("current song")) {
            return AgentAction("", "WHATS_PLAYING")
        }
        if (cmd.contains("like")) {
            return AgentAction("Added to your liked songs! ❤️", "LIKE_SONG")
        }

        // 2. UNIVERSAL LLM ENGINE WITH MULTI-PROFILE SUPPORT & DEEP APP CONTEXT
        val profiles = aiSettingsStore.profiles.first()
        val activeId = aiSettingsStore.activeProfileId.first()
        val activeProfile = profiles.find { it.id == activeId } ?: profiles.firstOrNull()

        if (activeProfile == null) {
            return AgentAction(
                speakResponse = "Please configure your AI Provider Profile in Settings to chat with Echofy Buddy.",
                actionType = "CHAT"
            )
        }

        val keysList = aiSettingsStore.parseKeysList(activeProfile.apiKeysRaw)

        if (keysList.isEmpty() && activeProfile.baseUrl != "http://localhost:11434/v1") {
            return AgentAction(
                speakResponse = "Please add your API key for profile \"${activeProfile.name}\" in AI Settings.",
                actionType = "CHAT"
            )
        }

        val result = universalAiEngine.query(
            baseUrl = activeProfile.baseUrl,
            apiKeys = if (keysList.isNotEmpty()) keysList else listOf("local"),
            modelName = activeProfile.modelName,
            userPrompt = userPrompt,
            chatHistory = history,
            userContext = userContext
        )

        return AgentAction(
            speakResponse = result.speakResponse,
            actionType = result.actionType,
            query = result.query,
            playlistName = result.playlistName
        )
    }

    private fun fuzzyMatch(s1: String, s2: String): Float {
        val a = s1.lowercase(Locale.getDefault()).trim()
        val b = s2.lowercase(Locale.getDefault()).trim()
        if (a == b) return 1.0f
        if (a.contains(b) || b.contains(a)) return 0.85f

        val costs = IntArray(b.length + 1)
        for (j in 0..b.length) costs[j] = j
        for (i in 1..a.length) {
            costs[0] = i
            var nw = i - 1
            for (j in 1..b.length) {
                val cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), if (a[i - 1] == b[j - 1]) nw else nw + 1)
                nw = costs[j]
                costs[j] = cj
            }
        }
        val maxLen = Math.max(a.length, b.length)
        if (maxLen == 0) return 1.0f
        return 1.0f - (costs[b.length].toFloat() / maxLen.toFloat())
    }

    private fun fallbackLocalIntentClassifier(userInput: String): AgentAction {
        val text = userInput.lowercase(Locale.getDefault())
        return when {
            text.contains("pause") || text.contains("stop music") -> AgentAction("Paused playback!", "PAUSE")
            text.contains("resume") || text.contains("unpause") -> AgentAction("Resuming playback!", "RESUME")
            text.contains("next") || text.contains("skip") -> AgentAction("Skipping to next track!", "NEXT")
            text.contains("previous") || text.contains("go back") -> AgentAction("Going back!", "PREVIOUS")

            text.contains("hey") || text.contains("hi") || text.contains("hello") ->
                AgentAction("Hey there! How's your day going? Want to listen to some music or chat?", "CHAT")

            text.contains("suggest") || text.contains("recommend") ->
                AgentAction("I can suggest songs based on your mood! Tell me what genre or artist you like.", "CHAT")

            else -> AgentAction("I'm here! Tell me what song or artist you'd like to play, or ask any question.", "CHAT")
        }
    }

    private suspend fun executeAgentAction(action: AgentAction, playerConnection: PlayerConnection?, source: InputSource = InputSource.TEXT) {
        when (action.actionType) {
            "WAKE_ACK" -> {
                // Reached when the wake word fired but the follow-up utterance was
                // just "hey" with no command. The capture pass already happened, so
                // simply re-arm the detector instead of starting another recognizer.
                withContext(Dispatchers.Main) {
                    _callState.value = AiCallState.CONNECTED_IDLE
                    if (_isHeyListening.value && !_isDrivingMode.value) {
                        openWakeWordDetector?.startListening()
                    }
                }
            }
            "PAUSE" -> {
                withContext(Dispatchers.Main) { playerConnection?.player?.pause() }
                respondAndContinue(action.speakResponse, source)
            }
            "RESUME" -> {
                withContext(Dispatchers.Main) { playerConnection?.player?.play() }
                respondAndContinue(action.speakResponse, source)
            }
            "NEXT" -> {
                withContext(Dispatchers.Main) { playerConnection?.player?.seekToNext() }
                respondAndContinue(action.speakResponse, source)
            }
            "PREVIOUS" -> {
                withContext(Dispatchers.Main) { playerConnection?.player?.seekToPrevious() }
                respondAndContinue(action.speakResponse, source)
            }
            "SET_VOLUME" -> {
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetVol = (maxVol * (action.volumeLevel / 100f)).toInt().coerceIn(0, maxVol)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                respondAndContinue(action.speakResponse, source)
            }
            "SHUFFLE" -> {
                withContext(Dispatchers.Main) {
                    playerConnection?.player?.let {
                        it.shuffleModeEnabled = !it.shuffleModeEnabled
                    }
                }
                respondAndContinue(action.speakResponse, source)
            }
            "REPEAT" -> {
                withContext(Dispatchers.Main) {
                    playerConnection?.player?.let {
                        it.repeatMode = when (it.repeatMode) {
                            androidx.media3.common.Player.REPEAT_MODE_OFF -> androidx.media3.common.Player.REPEAT_MODE_ALL
                            androidx.media3.common.Player.REPEAT_MODE_ALL -> androidx.media3.common.Player.REPEAT_MODE_ONE
                            else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                        }
                    }
                }
                respondAndContinue(action.speakResponse, source)
            }
            "WHATS_PLAYING" -> {
                val metadata = withContext(Dispatchers.Main) { playerConnection?.mediaMetadata?.value }
                if (metadata != null) {
                    val artistNames = metadata.artists.joinToString(", ") { it.name }
                    respondAndContinue("Now playing: \"${metadata.title}\" by $artistNames 🎵", source)
                } else {
                    respondAndContinue("Nothing is playing right now. Ask me to play something!", source)
                }
            }
            "LIKE_SONG" -> {
                try {
                    val metadata = withContext(Dispatchers.Main) { playerConnection?.mediaMetadata?.value }
                    if (metadata != null) {
                        withContext(Dispatchers.Main) {
                            playerConnection?.toggleLike()
                        }
                        respondAndContinue("Toggled like for \"${metadata.title}\"! ❤️", source)
                    } else {
                        respondAndContinue("No song is currently playing to like.", source)
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to like current song")
                    respondAndContinue("Couldn't like the song. Try again?", source)
                }
            }
            "CREATE_PLAYLIST" -> {
                val playlistTitle = action.playlistName.ifEmpty { "AI Mix" }
                val songIds = mutableListOf<String>()
                
                // Search for songs if query is provided
                if (action.query.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        runCatching {
                            val searchResult = YouTube.search(action.query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                            searchResult?.items?.filterIsInstance<SongItem>()?.take(15)?.forEach { songItem ->
                                runCatching {
                                    database.query {
                                        insert(songItem.toMediaMetadata().toSongEntity())
                                    }
                                }
                                songIds.add(songItem.id)
                            }
                        }
                    }
                }
                
                // Fall back to history/liked if no songs found from search
                if (songIds.isEmpty()) {
                    withContext(Dispatchers.IO) {
                        runCatching {
                            val events = database.events().first().take(10)
                            events.forEach { songIds.add(it.song.song.id) }
                        }
                        runCatching {
                            val liked = database.likedSongsByRowIdAsc().first().take(10)
                            liked.forEach { if (!songIds.contains(it.song.id)) songIds.add(it.song.id) }
                        }
                    }
                }
                
                // Actually create the playlist with songs
                withContext(Dispatchers.IO) {
                    runCatching {
                        val newPlaylist = PlaylistEntity(name = playlistTitle)
                        database.query {
                            insert(newPlaylist)
                            songIds.distinct().forEachIndexed { idx, sId ->
                                runCatching {
                                    insert(PlaylistSongMap(playlistId = newPlaylist.id, songId = sId, position = idx))
                                }
                            }
                        }
                    }
                }
                
                val count = songIds.distinct().size
                val finalMessage = if (action.speakResponse.isNotBlank() && !action.speakResponse.startsWith("Created playlist")) {
                    "${action.speakResponse}\n\nCreated your playlist \"$playlistTitle\" with $count songs in your Library!"
                } else {
                    "Created your playlist \"$playlistTitle\" with $count songs! Check your Library to see it."
                }
                respondAndContinue(finalMessage, source)
            }
            "PLAY_SONG" -> {
                if (action.query.isNotEmpty()) {
                    executePlaySongAction(action.query, action.speakResponse, playerConnection, source)
                } else {
                    respondAndContinue(action.speakResponse, source)
                }
            }
            else -> {
                respondAndContinue(action.speakResponse, source)
            }
        }
    }

    private suspend fun executePlaySongAction(query: String, speakText: String, playerConnection: PlayerConnection?, source: InputSource = InputSource.TEXT) {
        val songResult = withContext(Dispatchers.IO) {
            val songSearch = runCatching {
                YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items?.firstOrNull() as? SongItem
            }.getOrNull()

            if (songSearch != null) {
                songSearch
            } else {
                runCatching {
                    YouTube.searchSummary(query).getOrNull()?.summaries
                        ?.firstOrNull()?.items?.firstOrNull()
                }.getOrNull()
            }
        }

        if (songResult != null) {
            val videoId = songResult.id
            val title = songResult.title

            withContext(Dispatchers.Main) {
                playerConnection?.playQueue(
                    YouTubeQueue(WatchEndpoint(videoId = videoId))
                )
            }
            respondAndContinue("Playing \"$title\" on Echofy now! 🎵", source)
        } else {
            respondAndContinue("Couldn't find \"$query\" right now. Try another song?", source)
        }
    }

    private fun addUserMessage(text: String) {
        val updated = _chatHistory.value.toMutableList().apply {
            add(ChatMessage(text = text, isUser = true))
        }
        val list = updated.takeLast(50)
        _chatHistory.value = list
        saveChatHistoryToDisk(list)
    }

    private fun addAiMessage(text: String) {
        val updated = _chatHistory.value.toMutableList().apply {
            add(ChatMessage(text = text, isUser = false))
        }
        val list = updated.takeLast(50)
        _chatHistory.value = list
        saveChatHistoryToDisk(list)
    }

    private fun respondAndContinue(aiResponse: String, source: InputSource = InputSource.TEXT) {
        _aiResponseText.value = aiResponse
        addAiMessage(aiResponse)

        _callState.value = AiCallState.SPEAKING

        speakHumanVoice(aiResponse) {
            if (isCallActive) {
                startListeningLoop()
            } else {
                _callState.value = AiCallState.CONNECTED_IDLE
            }
        }
    }

    private fun speakHumanVoice(text: String, onComplete: () -> Unit) {
        scope.launch(Dispatchers.Main) {
            var completed = false
            val safeOnComplete = {
                if (!completed) {
                    completed = true
                    scope.launch(Dispatchers.Main) { onComplete() }
                }
            }

            val isTtsEnabled = aiSettingsStore.isTtsEnabled.first()
            if (!isTtsEnabled) {
                safeOnComplete()
                return@launch
            }

            val cleanText = text.replace(Regex("<.*?>|\\{.*?\\}"), "").trim()
            if (cleanText.isBlank()) {
                safeOnComplete()
                return@launch
            }

            if (isTtsInitialized && textToSpeech != null) {
                val utteranceId = "ECHO_TTS_${System.currentTimeMillis()}"
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        safeOnComplete()
                    }
                    override fun onError(utteranceId: String?) {
                        safeOnComplete()
                    }
                })
                val result = textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                if (result == TextToSpeech.ERROR) {
                    safeOnComplete()
                }
            } else {
                kotlinx.coroutines.delay((cleanText.length * 60L).coerceIn(1500L, 4000L))
                safeOnComplete()
            }
        }
    }

    private fun stopAudioPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) { Timber.w(e, "Ignored exception") }
    }

    companion object {
        /** Safety net for recognizers that never invoke any callback. */
        private const val COMMAND_TIMEOUT_MS = 10000L

        /**
         * Time for the wake tone to finish and AudioFlinger to release the input
         * before the speech recognizer opens it.
         */
        private const val MIC_SETTLE_DELAY_MS = 350L

        /** Pause after a spoken reply so the recognizer does not hear our own TTS. */
        private const val DRIVING_MODE_RESPONSE_GRACE_MS = 2500L

        /** Short breather between empty recognition passes in driving mode. */
        private const val DRIVING_MODE_IDLE_GAP_MS = 400L

        @Volatile
        private var INSTANCE: EchofyAiManager? = null

        fun getInstance(context: Context): EchofyAiManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EchofyAiManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
