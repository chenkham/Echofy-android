package com.Chenkham.Echofy.appwrite

import android.content.Context
import io.appwrite.services.Account
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Manages the single anonymous Appwrite session used by the Together feature.
 * Replaces FirebaseShardAuthManager — no Firebase Auth dependency needed.
 */
object AppwriteSessionAuth {

    private val inFlight = AtomicBoolean(false)
    private val pendingCallbacks = CopyOnWriteArrayList<(String?) -> Unit>()

    @Volatile private var cachedUserId: String? = null
    @Volatile private var lastFailureMessage: String? = null

    /**
     * Ensures an anonymous Appwrite session exists, then calls [onReady] with the userId.
     * Safe to call multiple times — reuses the existing session.
     */
    fun ensureSession(
        context: Context,
        scope: CoroutineScope,
        onReady: (String?) -> Unit,
    ) {
        cachedUserId?.let { onReady(it); return }
        pendingCallbacks += onReady
        if (!inFlight.compareAndSet(false, true)) return

        scope.launch(Dispatchers.IO) {
            val account = AppwriteClientProvider.account(context)
            val uid = getOrCreateSession(account)
            cachedUserId = uid
            if (uid != null) lastFailureMessage = null
            inFlight.set(false)
            val callbacks = pendingCallbacks.toList()
            pendingCallbacks.clear()
            callbacks.forEach { callback ->
                runCatching { callback(uid) }
                    .onFailure { Timber.tag(TAG).w(it, "Appwrite auth ready callback failed") }
            }
        }
    }

    fun lastFailureMessage(): String? = lastFailureMessage

    private suspend fun getOrCreateSession(account: Account): String? {
        // Try reusing an existing session first
        runCatching {
            return retryNetworkFailures("fetch existing session") { account.get().id }
        }.onFailure { Timber.tag(TAG).d("No existing session: %s", it.message) }

        // Create a fresh anonymous session
        return runCatching {
            retryNetworkFailures("create anonymous session") {
                account.createAnonymousSession()
                account.get().id
            }
        }.onFailure {
            lastFailureMessage = buildFailureMessage(it)
            Timber.tag(TAG).w(it, "Anonymous session creation failed")
        }
            .getOrNull()
    }

    private suspend fun <T> retryNetworkFailures(
        action: String,
        block: suspend () -> T,
    ): T {
        var lastError: Throwable? = null
        repeat(MAX_NETWORK_ATTEMPTS) { attempt ->
            try {
                return block()
            } catch (error: Throwable) {
                lastError = error
                val canRetry = isRetryableNetworkFailure(error) && attempt < MAX_NETWORK_ATTEMPTS - 1
                if (!canRetry) throw error
                Timber.tag(TAG).w(
                    error,
                    "Appwrite %s failed, retrying (%d/%d)",
                    action,
                    attempt + 1,
                    MAX_NETWORK_ATTEMPTS,
                )
                delay(NETWORK_RETRY_DELAY_MS * (attempt + 1))
            }
        }
        throw lastError ?: IllegalStateException("Appwrite $action failed")
    }

    private fun isRetryableNetworkFailure(error: Throwable?): Boolean {
        var current = error
        while (current != null) {
            if (
                current is UnknownHostException ||
                current is SocketTimeoutException ||
                current is ConnectException
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private fun buildFailureMessage(error: Throwable): String {
        val endpointHost = runCatching { URI(AppwriteConfig.ENDPOINT).host }.getOrNull()
        var current: Throwable? = error
        while (current != null) {
            when (current) {
                is UnknownHostException -> {
                    val host = endpointHost ?: "Appwrite"
                    return "Unable to reach $host. Check internet or DNS on this device and try again."
                }
                is SocketTimeoutException, is ConnectException -> {
                    return "Unable to reach Appwrite right now. Check the network on this device and try again."
                }
            }
            current = current.cause
        }
        return error.message ?: "Unable to authenticate with Appwrite"
    }

    /** Call on sign-out / session teardown so the next Together session gets a fresh auth. */
    fun clearCachedSession() {
        cachedUserId = null
        lastFailureMessage = null
    }

    private const val TAG = "AppwriteSessionAuth"
    private const val MAX_NETWORK_ATTEMPTS = 3
    private const val NETWORK_RETRY_DELAY_MS = 1_000L
}
