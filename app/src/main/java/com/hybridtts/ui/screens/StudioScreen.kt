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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
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
import androidx.compose.runtime.mutableFloatStateOf
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
import java.util.Locale

@Composable
fun StudioScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ttsEngine = remember { CloudTtsEngine.getInstance(context) }

    val isPlaying by ttsEngine.isPlayingState.collectAsState()
    val playbackProgress by ttsEngine.playbackProgressFraction.collectAsState()
    val isGenerating by ttsEngine.isGeneratingState.collectAsState()

    var scriptText by remember {
        mutableStateOf(
            "[mystery] The old house stood at the edge of the moor, its windows dark save for a single candle. [tension] As Elias crept closer, the heavy wooden door creaked open."
        )
    }

    var selectedVoice by remember { mutableStateOf("Charon") }
    var isVoiceDropdownOpen by remember { mutableStateOf(false) }

    var audioProfileText by remember {
        mutableStateOf("A deep, resonant narrator of mysteries.")
    }

    val styles = listOf("Natural", "Vocal Smile", "Newscaster", "Whisper", "Empathetic", "Promo/Hype", "Deadpan")
    var selectedStyle by remember { mutableStateOf("Whisper") }
    var isStyleMenuOpen by remember { mutableStateOf(false) }

    val paces = listOf("Natural", "Rapid Fire", "The Drift", "Staccato")
    var selectedPace by remember { mutableStateOf("The Drift") }
    var isPaceMenuOpen by remember { mutableStateOf(false) }

    val accents = listOf("Neutral", "British (RP)", "British (Brixton)", "American (Gen)", "American (Valley)", "American (South)", "Transatlantic", "Australian")
    var selectedAccent by remember { mutableStateOf("British (RP)") }
    var isAccentMenuOpen by remember { mutableStateOf(false) }

    // Temperature strictly stepping by 0.05
    var temperature by remember { mutableFloatStateOf(1.0f) }
    var speechRate by remember { mutableFloatStateOf(1.0f) }
    var pitchOffset by remember { mutableFloatStateOf(0.0f) }

    var playbackStatusText by remember { mutableStateOf("Google AI Studio TTS Engine Ready") }
    var hasGeneratedAudio by remember { mutableStateOf(ttsEngine.cachedPcmData != null) }

    val quickAudioTags = listOf("[whisper]", "[mystery]", "[tension]", "[pause]", "[sighs]", "[laughs]", "[gasp]", "[description]")

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Voice Selection Dropdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = AccentEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "VOICE CHARACTER (30 VOICES)",
                            color = AccentEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, AccentEmerald),
                            modifier = Modifier.clickable { isVoiceDropdownOpen = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedVoice,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = AccentEmerald
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isVoiceDropdownOpen,
                            onDismissRequest = { isVoiceDropdownOpen = false },
                            modifier = Modifier.background(DarkCard)
                        ) {
                            CloudTtsEngine.ALL_30_VOICES.forEach { voice ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = voice,
                                            color = if (selectedVoice == voice) AccentEmerald else TextPrimary,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    },
                                    onClick = {
                                        selectedVoice = voice
                                        isVoiceDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Audio Profile Persona:",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = audioProfileText,
                    onValueChange = { audioProfileText = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = AccentEmerald,
                        unfocusedBorderColor = BorderSubtle,
                        cursorColor = AccentEmerald
                    ),
                    placeholder = { Text("Describe character voice persona...", color = TextSecondary, fontSize = 11.sp) },
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Director's Note Controls Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "DIRECTOR'S NOTE (EXPRESSION STEERING)",
                    color = AccentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        DirectorNotePill(label = "Style", value = selectedStyle) { isStyleMenuOpen = true }
                        DropdownMenu(expanded = isStyleMenuOpen, onDismissRequest = { isStyleMenuOpen = false }, modifier = Modifier.background(DarkCard)) {
                            styles.forEach { st ->
                                DropdownMenuItem(text = { Text(st, color = TextPrimary, fontSize = 11.sp) }, onClick = { selectedStyle = st; isStyleMenuOpen = false })
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        DirectorNotePill(label = "Pace", value = selectedPace) { isPaceMenuOpen = true }
                        DropdownMenu(expanded = isPaceMenuOpen, onDismissRequest = { isPaceMenuOpen = false }, modifier = Modifier.background(DarkCard)) {
                            paces.forEach { pc ->
                                DropdownMenuItem(text = { Text(pc, color = TextPrimary, fontSize = 11.sp) }, onClick = { selectedPace = pc; isPaceMenuOpen = false })
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        DirectorNotePill(label = "Accent", value = selectedAccent) { isAccentMenuOpen = true }
                        DropdownMenu(expanded = isAccentMenuOpen, onDismissRequest = { isAccentMenuOpen = false }, modifier = Modifier.background(DarkCard)) {
                            accents.forEach { ac ->
                                DropdownMenuItem(text = { Text(ac, color = TextPrimary, fontSize = 11.sp) }, onClick = { selectedAccent = ac; isAccentMenuOpen = false })
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Script Input Card
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
                        text = "SCRIPT // COMPOSER",
                        color = AccentEmerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${scriptText.length} chars • ${scriptText.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size} words",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = scriptText,
                    onValueChange = { scriptText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
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
                    placeholder = { Text("Enter script or dialogue...", color = TextSecondary) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Insert Expressive Tags:",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickAudioTags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.clickable {
                                scriptText = if (scriptText.endsWith(" ") || scriptText.isEmpty()) {
                                    "$scriptText$tag "
                                } else {
                                    "$scriptText $tag "
                                }
                            }
                        ) {
                            Text(
                                text = tag,
                                color = AccentCyan,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Model Settings & Temperature (0.05 step grid)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MODEL TEMPERATURE & PLAYBACK DSP",
                        color = StatusWarning,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Temperature Slider with 0.05 increments
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Model Temperature (0.05 steps)", color = TextSecondary, fontSize = 12.sp)
                    Text(String.format(Locale.US, "%.2f", temperature), color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = temperature,
                    onValueChange = {
                        // Snap exactly to 0.05 increments
                        temperature = (Math.round(it * 20.0f) / 20.0f).coerceIn(0.05f, 2.0f)
                    },
                    valueRange = 0.05f..2.0f,
                    steps = 38,
                    colors = SliderDefaults.colors(thumbColor = StatusWarning, activeTrackColor = StatusWarning, inactiveTrackColor = BorderSubtle)
                )

                // Speech Rate Slider
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Local Playback Speed", color = TextSecondary, fontSize = 12.sp)
                    Text(String.format(Locale.US, "%.2fx", speechRate), color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = speechRate,
                    onValueChange = { speechRate = it },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(thumbColor = AccentEmerald, activeTrackColor = AccentEmerald, inactiveTrackColor = BorderSubtle)
                )

                // Pitch Offset Slider
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Local Pitch Shift", color = TextSecondary, fontSize = 12.sp)
                    Text(String.format(Locale.US, "%+.1f st", pitchOffset), color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = pitchOffset,
                    onValueChange = { pitchOffset = it },
                    valueRange = -6.0f..6.0f,
                    colors = SliderDefaults.colors(thumbColor = AccentCyan, activeTrackColor = AccentCyan, inactiveTrackColor = BorderSubtle)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Synthesize Button: Generates and buffers audio (NO AUTO-PLAY)
        Button(
            onClick = {
                if (isGenerating) return@Button
                playbackStatusText = "Synthesizing audio via Google AI Studio ($selectedVoice)..."

                scope.launch {
                    val result = ttsEngine.synthesizeMaster(
                        text = scriptText,
                        voiceName = selectedVoice,
                        audioProfile = audioProfileText,
                        styleNote = selectedStyle,
                        paceNote = selectedPace,
                        accentNote = selectedAccent,
                        temperature = temperature
                    )

                    when (result) {
                        is SynthesisResult.Success -> {
                            hasGeneratedAudio = true
                            playbackStatusText = String.format(
                                Locale.US,
                                "Audio Master Ready (%.1fs). Tap Play below.",
                                result.durationSeconds
                            )
                            Toast.makeText(context, "Audio ready! Tap Play to listen.", Toast.LENGTH_SHORT).show()
                        }
                        is SynthesisResult.Error -> {
                            playbackStatusText = "Synthesis failed. Check Settings!"
                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
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
                Text("GENERATING AUDIO IN CLOUD...", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            } else {
                Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SYNTHESIZE SPEECH MASTER", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Audio Master Player & Interactive Scrubbing Timeline
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
                        // Play / Pause Toggle Button
                        IconButton(
                            onClick = {
                                if (!hasGeneratedAudio) {
                                    Toast.makeText(context, "Please synthesize audio first!", Toast.LENGTH_SHORT).show()
                                    return@IconButton
                                }
                                if (isPlaying) {
                                    ttsEngine.pauseAudio()
                                } else {
                                    ttsEngine.playAudio(speechRate, pitchOffset)
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

                        // Stop Button (Rewinds to start)
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
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop",
                                tint = if (hasGeneratedAudio) TextPrimary else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Download Button: Lights up in Emerald when audio is ready!
                    Button(
                        onClick = {
                            if (!hasGeneratedAudio) return@Button
                            val result = ttsEngine.saveAudioToDownloads("HybridTTS_${selectedVoice}")
                            result.fold(
                                onSuccess = { path ->
                                    Toast.makeText(context, "Downloaded! $path", Toast.LENGTH_LONG).show()
                                },
                                onFailure = { err ->
                                    Toast.makeText(context, "Export error: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
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
                        Icon(imageVector = Icons.Default.Download, contentDescription = "Download WAV", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "DOWNLOAD WAV",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Interactive Audio Scrubbing Timeline Slider
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
                            ttsEngine.seekToFraction(newFraction)
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
                    text = playbackStatusText,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun DirectorNotePill(label: String, value: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(label, color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text(value, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}
