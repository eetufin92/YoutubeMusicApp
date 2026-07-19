package com.eetu.youtubemusicapp.ui.timer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun SleepTimerDialog(
    viewModel: SleepTimerViewModel,
    onDismiss: () -> Unit
) {
    val timeLeft by viewModel.timeLeftSeconds.collectAsState()
    val isRunning by viewModel.isTimerRunning.collectAsState()
    
    var selectedMinutes by remember { mutableStateOf(30) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Timer") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isRunning) {
                    Text(
                        text = "Time remaining: ${timeLeft / 60}:${(timeLeft % 60).toString().padStart(2, '0')}",
                        style = MaterialTheme.typography.headlineMedium
                    )
                } else {
                    Text("Stop music in:")
                    Spacer(modifier = Modifier.height(16.dp))
                    Slider(
                        value = selectedMinutes.toFloat(),
                        onValueChange = { selectedMinutes = it.toInt() },
                        valueRange = 5f..120f,
                        steps = 22 // 5, 10, 15...
                    )
                    Text("$selectedMinutes minutes")
                }
            }
        },
        confirmButton = {
            if (isRunning) {
                TextButton(onClick = { 
                    viewModel.cancelTimer()
                    onDismiss()
                }) {
                    Text("Stop Timer")
                }
            } else {
                Button(onClick = { 
                    viewModel.startTimer(selectedMinutes)
                    onDismiss()
                }) {
                    Text("Start")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
