package com.eetu.youtubemusicapp.service

import android.os.Looper
import android.webkit.WebView
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * A Media3 Player implementation that proxies commands and state to a WebView.
 */
@UnstableApi
class WebPlayerProxy(private val webView: WebView) : SimpleBasePlayer(Looper.getMainLooper()) {
    
    private var isMediaPlaying = false
    private var currentPosMs = 0L
    private var mediaDurationMs = 0L
    private var currentMetadata = MediaMetadata.EMPTY

    override fun getState(): State {
        val commands = Player.Commands.Builder()
            .add(Player.COMMAND_PLAY_PAUSE)
            .add(Player.COMMAND_SEEK_TO_NEXT)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS)
            .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
            .add(Player.COMMAND_GET_METADATA)
            .add(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)
            .build()

        val mediaItemData = MediaItemData.Builder("ytm-id")
            .setMediaMetadata(currentMetadata)
            .setDurationUs(if (mediaDurationMs > 0) mediaDurationMs * 1000 else 0L)
            .build()

        return State.Builder()
            .setAvailableCommands(commands)
            .setPlayWhenReady(isMediaPlaying, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(if (mediaDurationMs > 0 || isMediaPlaying) Player.STATE_READY else Player.STATE_IDLE)
            .setContentPositionMs(currentPosMs)
            .setPlaylist(listOf(mediaItemData))
            .setPlaylistMetadata(currentMetadata)
            .setCurrentMediaItemIndex(0)
            .build()
    }

    fun updateState(playing: Boolean, position: Long, duration: Long) {
        this.isMediaPlaying = playing
        this.currentPosMs = position
        this.mediaDurationMs = duration
        invalidateState()
    }

    fun updateMetadata(title: String, artist: String, album: String, artworkUrl: String) {
        this.currentMetadata = MediaMetadata.Builder()
            .setTitle(title)
            .setDisplayTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(android.net.Uri.parse(artworkUrl))
            .build()
        invalidateState()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        pauseOrPlay(playWhenReady)
        return Futures.immediateVoidFuture()
    }

    fun pauseOrPlay(play: Boolean) {
        webView.post { 
            if (play) {
                webView.evaluateJavascript("(document.querySelector('ytmusic-player-bar .play-pause-button') || document.querySelector('[aria-label=\"Play\"]'))?.click() || document.querySelector('video')?.play();", null) 
            } else {
                webView.evaluateJavascript("(document.querySelector('ytmusic-player-bar .play-pause-button') || document.querySelector('[aria-label=\"Pause\"]'))?.click() || document.querySelector('video')?.pause();", null) 
            }
        }
    }

    override fun handleSeek(
        mediaItemIndex: Int,
        positionMs: Long,
        seekCommand: Int
    ): ListenableFuture<*> {
        webView.post {
            when (seekCommand) {
                Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                    webView.evaluateJavascript("(document.querySelector('ytmusic-player-bar .next-button') || document.querySelector('[aria-label=\"Next song\"]'))?.click();", null)
                }
                Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                    webView.evaluateJavascript("(document.querySelector('ytmusic-player-bar .previous-button') || document.querySelector('[aria-label=\"Previous song\"]'))?.click();", null)
                }
                Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM -> {
                    val seconds = positionMs / 1000
                    webView.evaluateJavascript("document.querySelector('video').currentTime = $seconds;", null)
                }
            }
        }
        return Futures.immediateVoidFuture()
    }
}
