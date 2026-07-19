package com.eetu.youtubemusicapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.eetu.youtubemusicapp.ui.timer.SleepTimerViewModel
import com.eetu.youtubemusicapp.ui.components.YouTubeWebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.eetu.youtubemusicapp.ui.timer.SleepTimerDialog
import com.eetu.youtubemusicapp.ui.playback.PlaybackViewModel

@UnstableApi
@Composable
fun NavigationRoot() {
    val backStack = rememberNavBackStack(Destination.Home as NavKey)
    val sleepTimerViewModel: SleepTimerViewModel = viewModel()
    val context = LocalContext.current
    val playbackViewModel: PlaybackViewModel = viewModel(
        factory = remember { 
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return PlaybackViewModel(context) as T
                }
            }
        }
    )

    NavDisplay(
        backStack = backStack,
        onBack = { 
            if (backStack.isNotEmpty()) {
                backStack.removeAt(backStack.size - 1)
            }
        },
        entryProvider = { key ->
            when (key) {
                is Destination.Home -> NavEntry(key) {
                    HomeScreen(sleepTimerViewModel, playbackViewModel)
                }
                is Destination.Settings -> NavEntry(key) {
                    Text("Settings Screen")
                }
                else -> error("Unknown key: $key")
            }
        }
    )
}

@UnstableApi
@Composable
fun HomeScreen(
    sleepTimerViewModel: SleepTimerViewModel,
    playbackViewModel: PlaybackViewModel
) {
    var showTimerDialog by remember { mutableStateOf(false) }
    val isTimerRunning by sleepTimerViewModel.isTimerRunning.collectAsState()
    val isPlaying by playbackViewModel.isPlaying.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // Account for system bars (status and navigation) to ensure WebView content isn't obscured
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            YouTubeWebView(
                modifier = Modifier.fillMaxSize()
            )
            
            // Sleep Timer button is only visible when playback is active
            if (isPlaying) {
                ExtendedFloatingActionButton(
                    onClick = { showTimerDialog = true },
                    icon = { Icon(Icons.Default.Timer, contentDescription = "Sleep Timer") },
                    text = {
                        if (isTimerRunning) {
                            Text(sleepTimerViewModel.formatTimeLeft())
                        } else {
                            Text("Sleep Timer")
                        }
                    },
                    expanded = isTimerRunning,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 72.dp, end = 16.dp)
                )
            }
            
            if (showTimerDialog) {
                SleepTimerDialog(
                    viewModel = sleepTimerViewModel,
                    onDismiss = { showTimerDialog = false }
                )
            }
        }
    }
}
