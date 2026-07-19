package com.eetu.youtubemusicapp

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.media3.common.util.UnstableApi
import com.eetu.youtubemusicapp.service.PlaybackService
import com.eetu.youtubemusicapp.ui.navigation.NavigationRoot
import com.eetu.youtubemusicapp.ui.theme.YoutubeMusicAppTheme

class MainActivity : ComponentActivity() {
    
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install Splash Screen
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        setContent {
            YoutubeMusicAppTheme {
                RequestNotificationPermission()
                NavigationRoot()
            }
        }
        
        // Start the PlaybackService to ensure it's ready
        startPlaybackServiceWithOptIn()
    }

    @OptIn(UnstableApi::class)
    private fun startPlaybackServiceWithOptIn() {
        val intent = Intent(this, PlaybackService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}

@Composable
fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { _ ->
            // Handle permission result if needed
        }
        LaunchedEffect(Unit) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
