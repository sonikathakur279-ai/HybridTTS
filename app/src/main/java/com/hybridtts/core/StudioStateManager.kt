package com.hybridtts.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object StudioStateManager {
    // 0 = Engine 1 Direct, 1 = Engine 2 Hybrid, 2 = Engine 3 Local Offline
    var activeEngineMode by mutableIntStateOf(2)

    var scriptText by mutableStateOf(
        "Welcome to Engine 3 on the MediaTek Helio G85. ASUS and SQL pronunciation rules are applied offline with zero network connection."
    )

    var selectedVoice by mutableStateOf("Charon")
    var audioProfileText by mutableStateOf("A deep, resonant narrator of mysteries.")

    var selectedStyle by mutableStateOf("Whisper")
    var selectedPace by mutableStateOf("The Drift")
    var selectedAccent by mutableStateOf("British (RP)")

    var temperature by mutableFloatStateOf(1.0f)
    var speechRate by mutableFloatStateOf(1.0f)
    var pitchOffset by mutableFloatStateOf(0.0f)

    var playbackStatusText by mutableStateOf("Engine 3 Offline Neural Ready")
    var directorAnalysisSummary by mutableStateOf("Offline Engine Active • Cortex-A55 Pinned")
}
