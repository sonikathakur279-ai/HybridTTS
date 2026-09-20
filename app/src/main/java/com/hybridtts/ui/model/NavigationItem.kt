package com.hybridtts.ui.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Studio : NavigationItem("studio", "Studio", Icons.Default.GraphicEq)
    object Voices : NavigationItem("voices", "Voices", Icons.Default.RecordVoiceOver)
    object Comms : NavigationItem("comms", "Comms", Icons.Default.HeadsetMic)
    object Settings : NavigationItem("settings", "Settings", Icons.Default.Settings)

    companion object {
        val items = listOf(Studio, Voices, Comms, Settings)
    }
}
