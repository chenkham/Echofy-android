package com.Chenkham.Echofy.jam

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.Chenkham.Echofy.utils.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

interface JamConfigRepository {
    val registry: StateFlow<JamRegistry>

    suspend fun refresh(forceRefresh: Boolean = false)

    suspend fun allocateRoom(): Result<JamRoomAllocation?>
}

class BootstrapJamConfigRepository(
    context: Context,
) : JamConfigRepository {
    private val appContext = context.applicationContext
    private val _registry = MutableStateFlow(JamBootstrapConfig.defaultRegistry(appContext))

    override val registry: StateFlow<JamRegistry> = _registry.asStateFlow()

    override suspend fun refresh(forceRefresh: Boolean) {
        _registry.value = JamBootstrapConfig.defaultRegistry(appContext)
    }

    override suspend fun allocateRoom(): Result<JamRoomAllocation?> = Result.success(null)
}

class JamRemoteConfigRepository(
    context: Context,
) : JamConfigRepository {
    private val appContext = context.applicationContext
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val bootstrapRegistry = JamBootstrapConfig.defaultRegistry(appContext)
    private val cacheFile = File(appContext.filesDir, "jam/registry-cache.json")
    private val _registry = MutableStateFlow(loadCachedRegistry() ?: bootstrapRegistry)

    override val registry: StateFlow<JamRegistry> = _registry.asStateFlow()

    override suspend fun refresh(forceRefresh: Boolean) {
        val current = _registry.value
        // Bootstrap already has valid shards — don't block startup with a network call
        if (!forceRefresh && current.shards.isNotEmpty()) {
            Timber.tag(TAG).d("Registry has ${current.shards.size} shards, skipping remote fetch")
            return
        }

        val registryUrl = resolveRegistryUrl()
        if (registryUrl.isNullOrBlank()) {
            if (current.shards.isEmpty()) {
                _registry.value = loadCachedRegistry() ?: bootstrapRegistry
            }
            return
        }

        val remoteRegistry = runCatching {
            fetchRemoteRegistry(registryUrl)
        }.onFailure { throwable ->
            Timber.tag(TAG).w(throwable, "Failed to fetch Jam registry from control plane")
        }.getOrNull()

        when {
            remoteRegistry != null && remoteRegistry.shards.isNotEmpty() -> {
                val resolvedRegistry = applyInviteBaseOverride(remoteRegistry)
                _registry.value = resolvedRegistry
                persistRegistry(resolvedRegistry)
            }
            current.shards.isNotEmpty() -> Unit
            else -> {
                _registry.value = applyInviteBaseOverride(loadCachedRegistry() ?: bootstrapRegistry)
            }
        }
    }

    override suspend fun allocateRoom(): Result<JamRoomAllocation?> {
        val allocateUrl = resolveAllocationUrl() ?: return Result.success(null)
        return runCatching {
            fetchRoomAllocation(allocateUrl)
        }.onFailure { throwable ->
            Timber.tag(TAG).w(throwable, "Failed to allocate Jam room from control plane")
        }
    }

    private suspend fun fetchRemoteRegistry(registryUrl: String): JamRegistry = withContext(Dispatchers.IO) {
        val connection = (URL(registryUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 5_000
            setRequestProperty("Accept", "application/json")
        }

        try {
            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                error("Jam registry request failed with HTTP $statusCode")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString(JamRegistry.serializer(), body)
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun fetchRoomAllocation(allocateUrl: String): JamRoomAllocation? = withContext(Dispatchers.IO) {
        val connection = (URL(allocateUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 5_000
            readTimeout = 5_000
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }

        try {
            val requestBody = json.encodeToString(
                mapOf(
                    "registryVersion" to registry.value.version.toString(),
                ),
            )
            connection.outputStream.bufferedWriter().use { writer ->
                writer.write(requestBody)
            }

            val statusCode = connection.responseCode
            if (statusCode == HttpURLConnection.HTTP_NOT_FOUND || statusCode == 405) {
                return@withContext null
            }
            if (statusCode !in 200..299) {
                error("Jam allocation request failed with HTTP $statusCode")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString(JamRoomAllocation.serializer(), body)
        } finally {
            connection.disconnect()
        }
    }

    private fun persistRegistry(registry: JamRegistry) {
        runCatching {
            cacheFile.parentFile?.mkdirs()
            cacheFile.writeText(json.encodeToString(JamRegistry.serializer(), registry))
        }.onFailure { throwable ->
            Timber.tag(TAG).w(throwable, "Failed to cache Jam registry")
        }
    }

    private fun loadCachedRegistry(): JamRegistry? {
        if (!cacheFile.exists()) return null
        return runCatching {
            val content = cacheFile.readText()
            json.decodeFromString(JamRegistry.serializer(), content)
        }.onFailure { throwable ->
            Timber.tag(TAG).w(throwable, "Failed to read cached Jam registry")
        }.getOrNull()
    }

    private suspend fun resolveRegistryUrl(): String? {
        val override = appContext.dataStore.data.first()[JamRegistryUrlKey]
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        if (override != null) return override

        return appContext.optionalString("jam_registry_url")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: null
    }

    private suspend fun resolveAllocationUrl(): String? {
        val override = appContext.dataStore.data.first()[JamAllocationUrlKey]
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        if (override != null) return override

        appContext.optionalString("jam_room_allocate_url")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        val registryUrl = resolveRegistryUrl() ?: return null
        return deriveAllocationUrl(registryUrl)
    }

    private suspend fun applyInviteBaseOverride(registry: JamRegistry): JamRegistry {
        val inviteBaseOverride = appContext.dataStore.data.first()[JamInviteBaseUrlKey]
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: appContext.optionalString("jam_invite_base_url")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        if (inviteBaseOverride.isNullOrBlank()) return registry
        return registry.copy(inviteBaseUrl = inviteBaseOverride)
    }

    private fun Context.optionalString(name: String): String? {
        val identifier = resources.getIdentifier(name, "string", packageName)
        if (identifier == 0) return null
        return getString(identifier).takeIf { it.isNotBlank() }
    }

    companion object {
        val JamRegistryUrlKey = stringPreferencesKey("jam_registry_url_override")
        val JamInviteBaseUrlKey = stringPreferencesKey("jam_invite_base_url_override")
        val JamAllocationUrlKey = stringPreferencesKey("jam_room_allocate_url_override")
        private const val TAG = "JamRemoteConfig"

        private fun deriveAllocationUrl(registryUrl: String): String? {
            return runCatching {
                val url = URL(registryUrl)
                "${url.protocol}://${url.authority}/v1/rooms/allocate"
            }.getOrNull()
        }
    }
}
