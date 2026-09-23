package com.hybridtts.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hybridtts.core.CloudTtsEngine
import com.hybridtts.core.HybridDirectorEngine
import com.hybridtts.core.MultiSpeakerParser
import com.hybridtts.core.NetworkMonitor
import com.hybridtts.core.SherpaOnnxBridge
import com.hybridtts.core.SpeakerRegistry
import com.hybridtts.core.StudioStateManager
import com.hybridtts.core.SynthesisResult
import com.hybridtts.ui.theme.AccentCyan
import com.hybridtts.ui.theme.AccentEmerald
import com.hybridtts.ui.theme.BorderSubtle
import com.hybridtts.ui.theme.DarkCard
import com.hybridtts.ui.theme.DarkSurface
import com.hybridtts.ui.theme.PureBlack
import com.hybridtts.ui.theme.StatusWarning
import com.hybridtts.ui.theme.TextPrimary
import com.hybridtts.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.Locale

@Composable
fun StudioScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ttsEngine = remember { CloudTtsEngine.getInstance(context) }
    val hybridEngine = remember { HybridDirectorEngine.getInstance(context) }
    val offlineBridge = remember { SherpaOnnxBridge.getInstance(context) }
    val networkMonitor = remember { NetworkMonitor.getInstance(context) }
    val speakerRegistry = remember { SpeakerRegistry.getInstance(context) }

    val isPlaying by ttsEngine.isPlayingState.collectAsState()
    val playbackProgress by ttsEngine.playbackProgressFraction.collectAsState()
    val isGenerating by ttsEngine.isGeneratingState.collectAsState()
    val isOnline by networkMonitor.isOnline.collectAsState()

    var isVoiceDropdownOpen by remember { mutableStateOf(false) }
    var isStyleMenuOpen by remember { mutableStateOf(false) }
    var isPaceMenuOpen by remember { mutableStateOf(false) }
    var isAccentMenuOpen by remember { mutableStateOf(false) }

    val styles = listOf("Natural", "Vocal Smile", "Newscaster", "Whisper", "Empathetic", "Promo/Hype", "Deadpan")
    val paces = listOf("Natural", "Rapid Fire", "The Drift", "Staccato")
    val accents = listOf("Neutral", "British (RP)", "British (Brixton)", "American (Gen)", "American (Valley)", "American (South)", "Transatlantic", "Australian")
    val quickAudioTags = listOf("[whisper]", "[mystery]", "[tension]", "[pause]", "[sighs]", "[laughs]", "[gasp]", "[description]")

    val scrollState = rememberScrollState()
    val hasGeneratedAudio = ttsEngine.cachedPcmData != null

    val effectiveEngineMode = if (!isOnline) 2 else StudioStateManager.activeEngineMode

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        if (!isOnline) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(8.dp),
                color = DarkCard,
                border = BorderStroke(1.dp, AccentEmerald)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.AirplanemodeActive, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AIRPLANE MODE DETECTED // ENGINE 3 ENGAGED",
                        color = AccentEmerald,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Triple-Engine Mode Selector
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = DarkCard,
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (effectiveEngineMode == 0) AccentCyan else DarkCard, RoundedCornerShape(8.dp))
                        .clickable { StudioStateManager.activeEngineMode = 0 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("E1: DIRECT", color = if (effectiveEngineMode == 0) PureBlack else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (effectiveEngineMode == 1) AccentEmerald else DarkCard, RoundedCornerShape(8.dp))
                        .clickable { StudioStateManager.activeEngineMode = 1 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("E2: HYBRID", color = if (effectiveEngineMode == 1) PureBlack else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (effectiveEngineMode == 2) AccentEmerald else DarkCard, RoundedCornerShape(8.dp))
                        .clickable { StudioStateManager.activeEngineMode = 2 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Memory, contentDescription = null, tint = if (effectiveEngineMode == 2) PureBlack else TextSecondary, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("E3: OFFLINE", color = if (effectiveEngineMode == 2) PureBlack else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Full-Width Voice Character Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (effectiveEngineMode == 2) "OFFLINE MODEL: KOKORO-82M (INT8)" else "DEFAULT NARRATOR PROFILE",
                        color = AccentEmerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurface,
                        border = BorderStroke(1.dp, AccentEmerald),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (effectiveEngineMode != 2) isVoiceDropdownOpen = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (effectiveEngineMode == 2) "Kokoro-82M int8 (Cortex-A55 Pinned)" else "Voice: ${StudioStateManager.selectedVoice}",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            if (effectiveEngineMode != 2) {
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = AccentEmerald)
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = isVoiceDropdownOpen,
                        onDismissRequest = { isVoiceDropdownOpen = false },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(DarkCard)
                    ) {
                        CloudTtsEngine.ALL_30_VOICES.forEach { voice ->
                            DropdownMenuItem(
                                text = { Text(voice, color = if (StudioStateManager.selectedVoice == voice) AccentEmerald else TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
                                onClick = {
                                    StudioStateManager.selectedVoice = voice
                                    isVoiceDropdownOpen = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Multi-Speaker Dialogue Script Composer
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MULTI-SPEAKER SCRIPT // COMPOSER",
                        color = AccentEmerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${StudioStateManager.scriptText.length} chars • ${StudioStateManager.scriptText.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size} words",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = StudioStateManager.scriptText,
                    onValueChange = { StudioStateManager.scriptText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = AccentEmerald,
                        unfocusedBorderColor = BorderSubtle,
                        cursorColor = AccentEmerald
                    ),
                    shape = RoundedCornerShape(10.dp),
                    placeholder = { Text("Use [SPEAKER]: format for multi-voice dialogue...", color = TextSecondary) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Insert Quick Character Tag:", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("[NARRATOR]:", "[ELIAS]:", "[VALERIA]:", "[GUARD 1]:").forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.clickable {
                                StudioStateManager.scriptText = "${StudioStateManager.scriptText}\n$tag "
                            }
                        ) {
                            Text(tag, color = AccentCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Synthesize Button: Supports Dynamic Multi-Speaker Rendering
        Button(
            onClick = {
                if (isGenerating) return@Button
                StudioStateManager.playbackStatusText = "Parsing dialogue lines..."

                scope.launch {
                    val dialogueLines = MultiSpeakerParser.parseDialogue(StudioStateManager.scriptText)

                    if (dialogueLines.size > 1 && effectiveEngineMode != 2) {
                        // Multi-Speaker Synthesis: Renders each character line using their assigned voice
                        StudioStateManager.playbackStatusText = "Rendering ${dialogueLines.size} multi-character lines..."
                        val compositeStream = ByteArrayOutputStream()

                        for ((idx, line) in dialogueLines.withIndex()) {
                            val assignedVoice = speakerRegistry.getVoiceForCharacter(line.speakerTag)
                            StudioStateManager.playbackStatusText = "Rendering [${line.speakerTag}] with $assignedVoice (${idx + 1}/${dialogueLines.size})..."

                            val result = ttsEngine.synthesizeMaster(
                                text = line.spokenText,
                                voiceName = assignedVoice,
                                temperature = StudioStateManager.temperature
                            )

                            if (result is SynthesisResult.Success) {
                                val pcm = ttsEngine.cachedPcmData
                                if (pcm != null) {
                                    compositeStream.write(pcm)
                                    // 120ms pause between speakers
                                    compositeStream.write(ByteArray(CloudTtsEngine.SAMPLE_RATE_24K * 2 * 120 / 1000))
                                }
                            }
                        }

                        val masterPcm = compositeStream.toByteArray()
                        if (masterPcm.isNotEmpty()) {
                            ttsEngine.loadExternalMasterAudio(masterPcm)
                            StudioStateManager.playbackStatusText = String.format(Locale.US, "Multi-Cast Master Ready (%.1fs). Tap Play.", ttsEngine.cachedDurationSeconds)
                            Toast.makeText(context, "Multi-character master assembled!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Single-Speaker or Offline Synthesis
                        val result = if (effectiveEngineMode == 2) {
                            offlineBridge.synthesizeOffline(StudioStateManager.scriptText, 2)
                        } else {
                            ttsEngine.synthesizeMaster(
                                text = StudioStateManager.scriptText,
                                voiceName = StudioStateManager.selectedVoice,
                                temperature = StudioStateManager.temperature
                            )
                        }

                        when (result) {
                            is SynthesisResult.Success -> {
                                StudioStateManager.playbackStatusText = String.format(Locale.US, "Master Ready (%.1fs). Tap Play.", result.durationSeconds)
                                Toast.makeText(context, "Synthesis complete! Tap Play.", Toast.LENGTH_SHORT).show()
                            }
                            is SynthesisResult.Error -> {
                                StudioStateManager.playbackStatusText = "Synthesis error. Check Settings!"
                                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            },
            enabled = !isGenerating,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentEmerald,
                contentColor = PureBlack,
                disabledContainerColor = DarkCard,
                disabledContentColor = TextSecondary
            )
        ) {
            if (isGenerating) {
                CircularProgressIndicator(color = AccentEmerald, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("RENDERING MULTI-SPEAKER CAST...", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            } else {
                Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (effectiveEngineMode == 2) "SYNTHESIZE OFFLINE (ENGINE 3)" else "SYNTHESIZE CAST PRODUCTION",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Playback Bar & Scrubbing Timeline
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, if (hasGeneratedAudio) AccentCyan else BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (!hasGeneratedAudio) {
                                    Toast.makeText(context, "Please synthesize audio first!", Toast.LENGTH_SHORT).show()
                                    return@IconButton
                                }
                                if (isPlaying) {
                                    ttsEngine.pauseAudio()
                                } else {
                                    ttsEngine.playAudio(StudioStateManager.speechRate, StudioStateManager.pitchOffset)
                                }
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .background(if (isPlaying) AccentEmerald else DarkSurface, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = if (isPlaying) PureBlack else (if (hasGeneratedAudio) AccentEmerald else TextSecondary)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (hasGeneratedAudio) {
                                    ttsEngine.stopPlayback()
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(DarkSurface, CircleShape)
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop", tint = if (hasGeneratedAudio) TextPrimary else TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }

                    Button(
                        onClick = {
                            if (!hasGeneratedAudio) return@Button
                            val result = ttsEngine.saveAudioToDownloads("HybridTTS_CastMaster")
                            result.fold(
                                onSuccess = { Toast.makeText(context, "Saved to Downloads/HybridTTS/", Toast.LENGTH_LONG).show() },
                                onFailure = { err -> Toast.makeText(context, "Export error: ${err.localizedMessage}", Toast.LENGTH_SHORT).show() }
                            )
                        },
                        enabled = hasGeneratedAudio,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasGeneratedAudio) AccentEmerald else DarkSurface,
                            contentColor = PureBlack,
                            disabledContainerColor = DarkSurface,
                            disabledContentColor = TextSecondary
                        ),
                        border = BorderStroke(1.dp, if (hasGeneratedAudio) AccentEmerald else BorderSubtle)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("DOWNLOAD WAV", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val currentSeconds = (playbackProgress * ttsEngine.cachedDurationSeconds).toFloat()
                val totalSeconds = ttsEngine.cachedDurationSeconds.toFloat()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = String.format(Locale.US, "%02d:%05.2f", (currentSeconds / 60).toInt(), currentSeconds % 60),
                        color = if (hasGeneratedAudio) AccentCyan else TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format(Locale.US, "%02d:%05.2f", (totalSeconds / 60).toInt(), totalSeconds % 60),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Slider(
                    value = playbackProgress,
                    onValueChange = { newFraction ->
                        if (hasGeneratedAudio) {
                            ttsEngine.seekToFraction(newFraction, autoResume = true)
                        }
                    },
                    enabled = hasGeneratedAudio,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentCyan,
                        activeTrackColor = AccentCyan,
                        inactiveTrackColor = BorderSubtle,
                        disabledThumbColor = TextSecondary,
                        disabledActiveTrackColor = BorderSubtle
                    )
                )

                Text(
                    text = StudioStateManager.playbackStatusText,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
