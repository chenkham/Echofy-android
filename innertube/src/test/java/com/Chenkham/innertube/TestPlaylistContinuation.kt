package com.arturo254.opentune.innertube

import com.arturo254.opentune.innertube.YouTube

import kotlinx.coroutines.runBlocking
import org.junit.Test

class TestPlaylistContinuation {
    @Test
    fun testContinuation() = runBlocking {
        val browseId = "VLPLw-VjHDlEOgs658kAHR_LAaILBXb-s9Q5"
        println("Testing YouTube.playlist for $browseId")
        val result = YouTube.playlist(browseId)
        val page = result.getOrNull()
        if (page == null) {
            println("Failed to fetch initial playlist: ${result.exceptionOrNull()}")
            result.exceptionOrNull()?.printStackTrace()
            return@runBlocking
        }
        println("Initial Songs: ${page.songs.size}")
        println("Continuation: ${page.songsContinuation}")
        
        if (page.songsContinuation != null) {
            val contResult = YouTube.playlistContinuation(page.songsContinuation)
            val contPage = contResult.getOrNull()
            if (contPage == null) {
                println("Failed to fetch continuation: ${contResult.exceptionOrNull()}")
                contResult.exceptionOrNull()?.printStackTrace()
            } else {
                println("Continuation Songs: ${contPage.songs.size}")
                println("Next Continuation: ${contPage.continuation}")
            }
        }
    }
}
