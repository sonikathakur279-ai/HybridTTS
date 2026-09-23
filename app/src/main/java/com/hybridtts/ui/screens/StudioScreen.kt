package com.hybridtts.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hybridtts.core.AudioEditorRepository
import com.hybridtts.core.CloudTtsEngine
import com.hybridtts.core.HybridDirectorEngine
import com.hybridtts.core.MultiSpeakerParser
import com.hybridtts.core.SpeakerRegistry
import com.hybridtts.core.StudioStateManager
import com.hybridtts.core.SubtitleAligner
import com.hybridtts.core.SubtitleFormat
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
    val speakerRegistry = remember { SpeakerRegistry.getInstance(context) }
    val editorRepo = remember { AudioEditorRepository.getInstance(context) }

    val isPlaying by ttsEngine.isPlayingState.collectAsState()
    val playbackProgress by ttsEngine.playbackProgressFraction.collectAsState()
    val isGenerating by ttsEngine.isGeneratingState.collectAsState()

    var isVoiceDropdownOpen by remember { mutableStateOf(false) }
    var isTimelineEditorExpanded by remember { mutableStateOf(false) }
    var waveformZoom by remember { mutableFloatStateOf(1.0f) }
    var selectedSubtitleFormat by remember { mutableStateOf(SubtitleFormat.SRT) }

    val hasGeneratedAudio = ttsEngine.cachedPcmData != null
    val waveformPeaks = remember(ttsEngine.cachedPcmData, waveformZoom) {
        editorRepo.extractWaveformPeaks(ttsEngine.cachedPcmData, (60 * waveformZoom).toInt().coerceIn(40, 160))
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Voice Character Selector
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
                    Text("ACTIVE VOICE PROFILE", color = AccentEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurface,
                        border = BorderStroke(1.dp, AccentEmerald),
                        modifier = Modifier.fillMaxWidth().clickable { isVoiceDropdownOpen = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Voice: ${StudioStateManager.selectedVoice}", color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = AccentEmerald)
                        }
                    }

                    DropdownMenu(expanded = isVoiceDropdownOpen, onDismissRequest = { isVoiceDropdownOpen = false }, modifier = Modifier.background(DarkCard)) {
                        CloudTtsEngine.ALL_30_VOICES.forEach { voice ->
                            DropdownMenuItem(
                                text = { Text(voice, color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
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

        // Script Composer Card
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
                    Text("SCRIPT // COMPOSER", color = AccentEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("${StudioStateManager.scriptText.length} chars", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = StudioStateManager.scriptText,
                    onValueChange = { StudioStateManager.scriptText = it },
                    modifier = Modifier.fillMaxWidth().height(115.dp),
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
                    placeholder = { Text("Enter dialogue to synthesize...", color = TextSecondary) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Master Synthesize Button
        Button(
            onClick = {
                if (isGenerating) return@Button
                StudioStateManager.playbackStatusText = "Synthesizing master audio..."

                scope.launch {
                    val dialogueLines = MultiSpeakerParser.parseDialogue(StudioStateManager.scriptText)

                    if (dialogueLines.size > 1) {
                        val compositeStream = ByteArrayOutputStream()
                        for (line in dialogueLines) {
                            val assigned = speakerRegistry.getVoiceForCharacter(line.speakerTag)
                            val res = ttsEngine.synthesizeMaster(line.spokenText, assigned, temperature = StudioStateManager.temperature)
                            if (res is SynthesisResult.Success) {
                                ttsEngine.cachedPcmData?.let {
                                    compositeStream.write(it)
                                    compositeStream.write(ByteArray(CloudTtsEngine.SAMPLE_RATE_24K * 2 * 120 / 1000))
                                }
                            }
                        }
                        val master = compositeStream.toByteArray()
                        if (master.isNotEmpty()) {
                            ttsEngine.loadExternalMasterAudio(master)
                            editorRepo.setVoiceoverPcm(master)
                            StudioStateManager.playbackStatusText = "Multi-Voice Master Ready. Tap Play."
                        }
                    } else {
                        val res = ttsEngine.synthesizeMaster(
                            StudioStateManager.scriptText,
                            StudioStateManager.selectedVoice,
                            temperature = StudioStateManager.temperature
                        )
                        if (res is SynthesisResult.Success) {
                            ttsEngine.cachedPcmData?.let { editorRepo.setVoiceoverPcm(it) }
                            StudioStateManager.playbackStatusText = String.format(Locale.US, "Master Ready (%.1fs). Tap Play.", res.durationSeconds)
                        }
                    }
                }
            },
            enabled = !isGenerating,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald, contentColor = PureBlack)
        ) {
            if (isGenerating) {
                CircularProgressIndicator(color = PureBlack, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SYNTHESIZING AUDIO...", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            } else {
                Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SYNTHESIZE SPEECH MASTER", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Playback Bar with Interactive Scrubbing
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
                                if (!hasGeneratedAudio) return@IconButton
                                if (isPlaying) ttsEngine.pauseAudio() else ttsEngine.playAudio()
                            },
                            modifier = Modifier.size(42.dp).background(if (isPlaying) AccentEmerald else DarkSurface, CircleShape)
                        ) {
                            Icon(imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = if (isPlaying) PureBlack else AccentEmerald)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(onClick = { ttsEngine.stopPlayback() }, modifier = Modifier.size(36.dp).background(DarkSurface, CircleShape)) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Direct System Share Button
                    IconButton(
                        onClick = {
                            if (!hasGeneratedAudio) return@IconButton
                            val res = ttsEngine.saveAudioToDownloads("HybridTTS_Share")
                            res.onSuccess {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "audio/wav"
                                    putExtra(Intent.EXTRA_SUBJECT, "HybridTTS Master Audio")
                                    putExtra(Intent.EXTRA_TEXT, "Generated via HybridTTS Audio Workstation.")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Master Audio via..."))
                            }
                        },
                        modifier = Modifier.size(36.dp).background(DarkSurface, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = AccentCyan, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Waveform Canvas (Sample-Accurate Pinch-To-Zoom)
                Canvas(
                    modifier = Modifier.fillMaxWidth().height(48.dp).background(DarkSurface, RoundedCornerShape(6.dp))
                ) {
                    val barWidth = (size.width / waveformPeaks.size.toFloat()).coerceAtLeast(2f)
                    val playheadX = playbackProgress * size.width

                    for ((i, peak) in waveformPeaks.withIndex()) {
                        val barHeight = peak * size.height * 0.85f
                        val x = i * barWidth
                        val y = (size.height - barHeight) / 2f
                        val isPastPlayhead = x < playheadX

                        drawRoundRect(
                            color = if (isPastPlayhead) AccentEmerald else AccentCyan.copy(alpha = 0.6f),
                            topLeft = Offset(x, y),
                            size = Size(barWidth * 0.7f, barHeight),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                    }

                    // Draw scrubbing playhead line
                    drawLine(
                        color = TextPrimary,
                        start = Offset(playheadX, 0f),
                        end = Offset(playheadX, size.height),
                        strokeWidth = 3f
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = playbackProgress,
                    onValueChange = { ttsEngine.seekToFraction(it, autoResume = true) },
                    enabled = hasGeneratedAudio,
                    colors = SliderDefaults.colors(thumbColor = AccentCyan, activeTrackColor = AccentCyan, inactiveTrackColor = BorderSubtle)
                )

                Text(StudioStateManager.playbackStatusText, color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4-Track Timeline & Auto-Ducking Mastering Studio
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkCard,
            border = BorderStroke(1.dp, if (isTimelineEditorExpanded) AccentEmerald else BorderSubtle),
            modifier = Modifier.fillMaxWidth().clickable { isTimelineEditorExpanded = !isTimelineEditorExpanded }
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Layers, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("4-TRACK AUDIO TIMELINE & AUTO-DUCKING", color = AccentEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Text(if (isTimelineEditorExpanded) "COLLAPSE" else "OPEN STUDIO", color = AccentCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }

                if (isTimelineEditorExpanded) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Auto-Ducking Switch (-14 dB attenuation)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Automated Music Ducking", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("-14 dB background attenuation during speech", color = TextSecondary, fontSize = 10.sp)
                        }
                        Switch(
                            checked = editorRepo.autoDuckingEnabled,
                            onCheckedChange = { editorRepo.autoDuckingEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = PureBlack, checkedTrackColor = AccentEmerald, uncheckedTrackColor = DarkSurface)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Track 1: Voiceover Channel
                    TrackChannelRow("Track 1: Voiceover (VO)", AccentEmerald, editorRepo.tracks[0].volumeMultiplier) {
                        editorRepo.tracks[0].volumeMultiplier = it
                    }

                    // Track 2: BGM Channel
                    TrackChannelRow("Track 2: Background Music (BGM)", AccentCyan, editorRepo.tracks[1].volumeMultiplier) {
                        editorRepo.tracks[1].volumeMultiplier = it
                    }

                    // Track 3: SFX 1 Channel
                    TrackChannelRow("Track 3: Sound Effects 1 (SFX 1)", StatusWarning, editorRepo.tracks[2].volumeMultiplier) {
                        editorRepo.tracks[2].volumeMultiplier = it
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val mixedPcm = editorRepo.mixDown4Tracks()
                            if (mixedPcm.isNotEmpty()) {
                                ttsEngine.loadExternalMasterAudio(mixedPcm)
                                Toast.makeText(context, "4 Tracks mixed down with auto-ducking!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald, contentColor = PureBlack)
                    ) {
                        Text("RENDER MASTER MIX (4 TRACKS)", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Millisecond Subtitle Exporter Card (.SRT / .VTT / .ASS)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Subtitles, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("MILLISECOND SUBTITLE EXPORTER", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Generates millisecond-accurate subtitle files synchronized to your speech timing.", color = TextSecondary, fontSize = 10.sp)

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Pair(SubtitleFormat.SRT, "SubRip (.SRT)"),
                        Pair(SubtitleFormat.VTT, "WebVTT (.VTT)"),
                        Pair(SubtitleFormat.ASS, "Karaoke (.ASS)")
                    ).forEach { (format, label) ->
                        FilterChip(
                            selected = selectedSubtitleFormat == format,
                            onClick = { selectedSubtitleFormat = format },
                            label = { Text(label, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentCyan,
                                selectedLabelColor = PureBlack,
                                containerColor = DarkSurface,
                                labelColor = TextPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val cues = SubtitleAligner.generateCuesFromScript(StudioStateManager.scriptText, ttsEngine.cachedDurationSeconds.coerceAtLeast(1.0))
                        val textContent = when (selectedSubtitleFormat) {
                            SubtitleFormat.SRT -> SubtitleAligner.buildSrt(cues)
                            SubtitleFormat.VTT -> SubtitleAligner.buildVTT(cues)
                            SubtitleFormat.ASS -> SubtitleAligner.buildAss(cues)
                        }

                        val result = SubtitleAligner.exportSubtitleFile(context, textContent, selectedSubtitleFormat)
                        result.fold(
                            onSuccess = { Toast.makeText(context, "Exported! $it", Toast.LENGTH_LONG).show() },
                            onFailure = { Toast.makeText(context, "Export error: ${it.localizedMessage}", Toast.LENGTH_SHORT).show() }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface, contentColor = AccentCyan),
                    border = BorderStroke(1.dp, AccentCyan)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("EXPORT SUBTITLE FILE", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TrackChannelRow(title: String, color: androidx.compose.ui.graphics.Color, volume: Float, onVolumeChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = color, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text(String.format(Locale.US, "%.0f%%", volume * 100f), color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        Slider(
            value = volume,
            onValueChange = onVolumeChange,
            valueRange = 0f..1.5f,
            colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color, inactiveTrackColor = BorderSubtle)
        )
    }
}
