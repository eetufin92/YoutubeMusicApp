package com.eetu.youtubemusicapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

@UnstableApi
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    companion object {
        @UnstableApi
        var playerProxy: WebPlayerProxy? = null
            set(value) {
                field = value
                instance?.updateSession()
            }
        private var instance: PlaybackService? = null
        const val CHANNEL_ID = "youtube_music_playback_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // 1. Create Notification Channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Playback Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        // 2. Build a simple initial notification to avoid ForegroundServiceDidNotStartInTimeException
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("YTM Player")
            .setContentText("Waiting for WebView...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        // 3. Immediately start foreground
        startForeground(NOTIFICATION_ID, notification)

        // 4. Initialize MediaSession if player is available
        val player = playerProxy
        if (player != null) {
            val intent = Intent(this, com.eetu.youtubemusicapp.MainActivity::class.java)
            val pendingIntent = android.app.PendingIntent.getActivity(
                this, 0, intent,
                android.app.PendingIntent.FLAG_IMMUTABLE
            )
            mediaSession = MediaSession.Builder(this, player)
                .setSessionActivity(pendingIntent)
                .build()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        if (mediaSession == null) {
            playerProxy?.let { player ->
                val intent = Intent(this, com.eetu.youtubemusicapp.MainActivity::class.java)
                val pendingIntent = android.app.PendingIntent.getActivity(
                    this, 0, intent,
                    android.app.PendingIntent.FLAG_IMMUTABLE
                )
                mediaSession = MediaSession.Builder(this, player)
                    .setSessionActivity(pendingIntent)
                    .build()
            }
        }
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player != null && !player.playWhenReady) {
            stopSelf()
        }
    }

    fun updateSession() {
        if (mediaSession == null) {
            playerProxy?.let { player ->
                val intent = Intent(this, com.eetu.youtubemusicapp.MainActivity::class.java)
                val pendingIntent = android.app.PendingIntent.getActivity(
                    this, 0, intent,
                    android.app.PendingIntent.FLAG_IMMUTABLE
                )
                mediaSession = MediaSession.Builder(this, player)
                    .setSessionActivity(pendingIntent)
                    .build()
            }
        }
    }

    override fun onDestroy() {
        instance = null
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
