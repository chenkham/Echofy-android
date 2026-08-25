package com.Chenkham.Echofy.ai

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Lightweight Wake Word Detector stub.
 * Removes heavy ONNX native runtime bloat (~22-58MB) while preserving API compatibility.
 */
class OpenWakeWordDetector(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var listeningJob: Job? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    suspend fun startListening() {
        if (_isListening.value) return
        _isListening.value = true
        Timber.d("OpenWakeWordDetector started")
    }

    fun stopListening() {
        if (!_isListening.value) return
        _isListening.value = false
        listeningJob?.cancel()
        listeningJob = null
        Timber.d("OpenWakeWordDetector stopped")
    }

    suspend fun stopListeningAndAwait() {
        stopListening()
    }

    fun release() {
        stopListening()
    }
}
