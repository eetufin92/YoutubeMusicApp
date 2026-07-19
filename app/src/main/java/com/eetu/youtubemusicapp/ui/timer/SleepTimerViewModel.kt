package com.eetu.youtubemusicapp.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.media3.common.util.UnstableApi
import com.eetu.youtubemusicapp.service.PlaybackService

@UnstableApi
class SleepTimerViewModel : ViewModel() {

    private val _timeLeftSeconds = MutableStateFlow(0)
    val timeLeftSeconds: StateFlow<Int> = _timeLeftSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private var timerJob: Job? = null

    fun startTimer(minutes: Int) {
        timerJob?.cancel()
        _timeLeftSeconds.value = minutes * 60
        _isTimerRunning.value = true
        
        timerJob = viewModelScope.launch {
            while (_timeLeftSeconds.value > 0) {
                delay(1000)
                _timeLeftSeconds.value -= 1
            }
            stopPlayback()
            _isTimerRunning.value = false
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        _timeLeftSeconds.value = 0
        _isTimerRunning.value = false
    }

    private fun stopPlayback() {
        // Use the public method to pause
        PlaybackService.playerProxy?.pauseOrPlay(false)
    }

    fun formatTimeLeft(): String {
        val seconds = _timeLeftSeconds.value
        val mins = seconds / 60
        val secs = seconds % 60
        return if (mins > 0) {
            "${mins}m"
        } else {
            "${secs}s"
        }
    }
}
