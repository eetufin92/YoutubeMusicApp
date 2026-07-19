package com.eetu.youtubemusicapp.ui.navigation

import kotlinx.serialization.Serializable
import androidx.navigation3.runtime.NavKey

@Serializable
sealed interface Destination : NavKey {
    @Serializable
    data object Home : Destination
    
    @Serializable
    data object Settings : Destination
}
