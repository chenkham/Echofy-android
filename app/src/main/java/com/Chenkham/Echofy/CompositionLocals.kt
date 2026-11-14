package com.Chenkham.Echofy

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.Chenkham.Echofy.db.MusicDatabase
import com.Chenkham.Echofy.playback.DownloadUtil
import com.Chenkham.Echofy.playback.PlayerConnection
import com.Chenkham.Echofy.utils.SyncUtils

val LocalDatabase = staticCompositionLocalOf<MusicDatabase> { error("No database provided") }
val LocalPlayerConnection = staticCompositionLocalOf<PlayerConnection?> { error("No PlayerConnection provided") }
val LocalPlayerAwareWindowInsets = compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }
val LocalDownloadUtil = staticCompositionLocalOf<DownloadUtil> { error("No DownloadUtil provided") }
val LocalSyncUtils = staticCompositionLocalOf<SyncUtils> { error("No SyncUtils provided") }
