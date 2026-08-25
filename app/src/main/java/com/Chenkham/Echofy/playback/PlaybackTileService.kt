package com.Chenkham.Echofy.playback

import android.annotation.SuppressLint
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.QuickSettingsTileEnabledKey
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.get
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

/**
 * Play/pause tile for the notification shade so playback can be controlled without
 * opening the app. The tile is a no-op when the user disables it in settings.
 */
@RequiresApi(Build.VERSION_CODES.N)
class PlaybackTileService : TileService() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateTile()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        connectController()
    }

    override fun onStopListening() {
        super.onStopListening()
        releaseController()
    }

    override fun onClick() {
        super.onClick()

        if (!isTileEnabled()) return

        controller?.let { mediaController ->
            if (mediaController.isPlaying) {
                mediaController.pause()
            } else {
                mediaController.play()
            }
            updateTile()
        }
    }

    private fun isTileEnabled(): Boolean =
        dataStore[QuickSettingsTileEnabledKey] ?: true

    private fun connectController() {
        val token = SessionToken(this, ComponentName(this, MusicService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener({
            controller = try {
                future.get().also { it.addListener(playerListener) }
            } catch (e: Exception) {
                null
            }
            updateTile()
        }, MoreExecutors.directExecutor())
    }

    private fun releaseController() {
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
    }

    @SuppressLint("WrongConstant")
    private fun updateTile() {
        val tile = qsTile ?: return

        if (!isTileEnabled()) {
            tile.state = Tile.STATE_UNAVAILABLE
            tile.label = getString(R.string.app_name)
            tile.updateTile()
            return
        }

        val isPlaying = controller?.isPlaying == true
        tile.state = if (isPlaying) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = controller?.mediaMetadata?.title?.toString()
            ?: getString(R.string.app_name)
        tile.icon = Icon.createWithResource(
            this,
            if (isPlaying) R.drawable.pause else R.drawable.play,
        )
        tile.updateTile()
    }
}
