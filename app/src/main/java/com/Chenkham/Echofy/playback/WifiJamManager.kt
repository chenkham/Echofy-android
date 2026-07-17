package com.Chenkham.Echofy.playback

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

@Serializable
data class WifiJamEvent(
    val type: String,          // "SYNC", "PLAY", "PAUSE", "NEXT", "PREV", "SEEK"
    val mediaId: String = "",  // Current videoId/mediaId
    val positionMs: Long = 0L, // Current playback position
    val playbackSpeed: Float = 1f,
)

/**
 * Handles dependency-free local network listening together using raw TCP sockets.
 */
class WifiJamManager(private val scope: CoroutineScope) {

    private val SERVER_PORT = 50505

    // State exposing connection info
    private val _isHost = MutableStateFlow(false)
    val isHost = _isHost.asStateFlow()

    private val _isGuest = MutableStateFlow(false)
    val isGuest = _isGuest.asStateFlow()

    private val _connectedClientsCount = MutableStateFlow(0)
    val connectedClientsCount = _connectedClientsCount.asStateFlow()

    // Expose incoming events for MusicService to consume
    private val _incomingEvents = MutableStateFlow<WifiJamEvent?>(null)
    val incomingEvents = _incomingEvents.asStateFlow()

    // Host variables
    private var serverSocket: ServerSocket? = null
    private val clientWriters = mutableListOf<PrintWriter>()
    private var hostJob: Job? = null

    // Guest variables
    private var guestSocket: Socket? = null
    private var guestWriter: PrintWriter? = null
    private var guestJob: Job? = null

    /**
     * Start hosting a Jam session.
     */
    fun startHosting() {
        if (_isHost.value || _isGuest.value) return
        stopAll()

        _isHost.value = true
        _connectedClientsCount.value = 0

        hostJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(SERVER_PORT)
                Timber.tag("WifiJam").d("Host server started on port $SERVER_PORT")

                while (isActive) {
                    val client = serverSocket?.accept() ?: break
                    Timber.tag("WifiJam").d("Client connected: ${client.inetAddress.hostAddress}")
                    handleClientConnection(client)
                }
            } catch (e: Exception) {
                if (e !is java.net.SocketException) {
                    Timber.tag("WifiJam").e(e, "Host server error")
                }
            } finally {
                _isHost.value = false
            }
        }
    }

    private fun handleClientConnection(client: Socket) {
        scope.launch(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(client.inputStream))
                val writer = PrintWriter(client.outputStream, true)
                
                synchronized(clientWriters) {
                    clientWriters.add(writer)
                    _connectedClientsCount.value = clientWriters.size
                }

                // Wait, if we want Guests to send commands (like PLAY/PAUSE/SEEK), 
                // we'd read from `reader` here and broadcast it.
                // The user requested: "pause,next,previous... restricted". "restricted" to host? 
                // Wait, the user specifically answered "1. restricted" which means Guest CANNOT control playback.
                // So the host ignores any read lines from the guest, except maybe a "PING" or just waits for disconnect.

                while (isActive) {
                    val line = reader.readLine()
                    if (line == null) {
                        // Client disconnected
                        break
                    }
                    // Guest commands ignored based on user preference (restricted mode).
                }

            } catch (e: Exception) {
                Timber.tag("WifiJam").w(e, "Client disconnected abruptly")
            } finally {
                synchronized(clientWriters) {
                    clientWriters.removeIf { it.checkError() }
                    try { client.close() } catch (ignored: Exception) {}
                    _connectedClientsCount.value = clientWriters.size
                }
            }
        }
    }

    /**
     * Join a Jam session via IP address.
     */
    fun joinJam(hostIp: String) {
        if (_isHost.value || _isGuest.value) return
        stopAll()

        _isGuest.value = true

        guestJob = scope.launch(Dispatchers.IO) {
            try {
                guestSocket = Socket(hostIp, SERVER_PORT)
                guestWriter = PrintWriter(guestSocket!!.outputStream, true)
                val reader = BufferedReader(InputStreamReader(guestSocket!!.inputStream))
                
                Timber.tag("WifiJam").d("Connected to host at $hostIp")

                while (isActive) {
                    val message = reader.readLine()
                    if (message == null) {
                        Timber.tag("WifiJam").d("Host disconnected.")
                        break
                    }
                    
                    try {
                        val event = Json.decodeFromString<WifiJamEvent>(message)
                        _incomingEvents.value = event
                    } catch (e: Exception) {
                        Timber.tag("WifiJam").e(e, "Failed to parse incoming Jam Event: $message")
                    }
                }
            } catch (e: Exception) {
                Timber.tag("WifiJam").e(e, "Failed to connect to host")
            } finally {
                _isGuest.value = false
                try { guestSocket?.close() } catch (ignored: Exception) {}
            }
        }
    }

    /**
     * Broadcaster triggered by the Host's MusicService when state changes.
     */
    fun broadcastEvent(event: WifiJamEvent) {
        if (!_isHost.value) return
        
        scope.launch(Dispatchers.IO) {
            val message = try {
                Json.encodeToString(event)
            } catch (e: Exception) {
                return@launch
            }

            synchronized(clientWriters) {
                val iterator = clientWriters.iterator()
                while (iterator.hasNext()) {
                    val writer = iterator.next()
                    writer.println(message)
                    writer.flush()
                    if (writer.checkError()) {
                        iterator.remove()
                    }
                }
                _connectedClientsCount.value = clientWriters.size
            }
        }
    }

    fun stopAll() {
        _isHost.value = false
        _isGuest.value = false
        _connectedClientsCount.value = 0

        hostJob?.cancel()
        guestJob?.cancel()

        try { serverSocket?.close() } catch (ignored: Exception) {}
        try { guestSocket?.close() } catch (ignored: Exception) {}
        
        synchronized(clientWriters) {
            clientWriters.forEach { try { it.close() } catch (ignored: Exception){} }
            clientWriters.clear()
        }
    }
}
