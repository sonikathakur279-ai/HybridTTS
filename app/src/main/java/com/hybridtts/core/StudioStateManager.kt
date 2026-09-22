package com.hybridtts.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object StudioStateManager {
    var activeEngineMode by mutableIntStateOf(1) // 0 = Direct, 1 = Hybrid

    var scriptText by mutableStateOf(
        "[mystery] The old house stood at the edge of the moor, its windows dark save for a single candle. [tension] As Elias crept closer, the heavy wooden door creaked open."
    )

    var selectedVoice by mutableStateOf("Charon")
    var audioProfileText by mutableStateOf("A deep, resonant narrator of mysteries.")

    var selectedStyle by mutableStateOf("Whisper")
    var selectedPace by mutableStateOf("The Drift")
    var selectedAccent by mutableStateOf("British (RP)")

    // Safe Gemini TTS temperature default
    var temperature by mutableFloatStateOf(1.0f)
    var speechRate by mutableFloatStateOf(1.0f)
    var pitchOffset by mutableFloatStateOf(0.0f)

    var playbackStatusText by mutableStateOf("Ready to synthesize")
    var directorAnalysisSummary by mutableStateOf("Director Idle • Ready to analyze phonetics & homographs")
}
