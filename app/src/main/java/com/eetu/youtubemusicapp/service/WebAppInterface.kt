package com.eetu.youtubemusicapp.service

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import androidx.media3.common.util.UnstableApi

/**
 * Interface that receives events from the YouTube Music web page.
 */
@UnstableApi
class WebAppInterface(private val playerProxy: WebPlayerProxy) {

    private val handler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onPlaybackUpdate(isPlaying: Boolean, position: Double, duration: Double) {
        handler.post {
            playerProxy.updateState(isPlaying, (position * 1000).toLong(), (duration * 1000).toLong())
        }
    }

    @JavascriptInterface
    fun onMetadataUpdate(title: String, artist: String, album: String, artworkUrl: String) {
        handler.post {
            playerProxy.updateMetadata(title, artist, album, artworkUrl)
        }
    }
}
