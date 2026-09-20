package com.hybridtts.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.hybridtts.ui.theme.TextPrimary
import com.hybridtts.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun StudioScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ttsEngine = remember { CloudTtsEngine.getInstance(context) }

    var scriptText by remember {
        mutableStateOf(
            "Hello from HybridTTS! Your neural audio engine is now speaking through Google AI Studio on the MediaTek Helio G85."
        )
    }
    var selectedEmotion by remember { mutableIntStateOf(0) }
    val emotionTags = listOf("Neutral", "Cinematic", "Dramatic", "Whisper", "Urgent", "Conversational")

    var speechRate by remember { mutableFloatStateOf(1.0f) }
    var pitchOffset by remember { mutableFloatStateOf(0.0f) }

    var isSynthesizing by remember { mutableStateOf(false) }
    var playbackStatusText by remember { mutableStateOf("Audio Master Ready (24kHz Mono)") }
    var lastAudioDuration by remember { mutableFloatStateOf(0.0f) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
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
                        text = "SCRIPT COMPOSER",
                        color = AccentEmerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${scriptText.length} chars • ${scriptText.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size} words",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = scriptText,
                    onValueChange = { scriptText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
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
                    placeholder = { Text("Enter script or dialogue line here...", color = TextSecondary) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Emotion Directives Chip Bar
        Text(
            text = "PROSODY & EMOTION DIRECTIVES",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            emotionTags.forEachIndexed { index, tag ->
                FilterChip(
                    selected = (selectedEmotion == index),
                    onClick = { selectedEmotion = index },
                    label = { Text(tag, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentEmerald,
                        selectedLabelColor = PureBlack,
                        containerColor = DarkCard,
                        labelColor = TextPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Acoustic DSP Parameters (Speed & Pitch)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SONIC DSP SPEED & PITCH",
                        color = AccentCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Speech Rate Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Speech Rate (Speed)", color = TextSecondary, fontSize = 12.sp)
                    Text(
                        text = String.format(Locale.US, "%.2fx", speechRate),
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = speechRate,
                    onValueChange = { speechRate = it },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentEmerald,
                        activeTrackColor = AccentEmerald,
                        inactiveTrackColor = BorderSubtle
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Pitch Semitones Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Pitch Offset", color = TextSecondary, fontSize = 12.sp)
                    Text(
                        text = String.format(Locale.US, "%+.1f st", pitchOffset),
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = pitchOffset,
                    onValueChange = { pitchOffset = it },
                    valueRange = -6.0f..6.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentCyan,
                        activeTrackColor = AccentCyan,
                        inactiveTrackColor = BorderSubtle
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Synthesize Action Button (Triggers Real Cloud TTS!)
        Button(
            onClick = {
                if (isSynthesizing) return@Button
                isSynthesizing = true
                playbackStatusText = "Synthesizing audio via Google AI Studio..."

                scope.launch {
                    val result = ttsEngine.synthesizeAndPlay(
                        text = scriptText,
                        voiceName = "Aoede",
                        speed = speechRate,
                        pitchSemitones = pitchOffset
                    )

                    isSynthesizing = false

                    when (result) {
                        is SynthesisResult.Success -> {
                            lastAudioDuration = result.durationSeconds.toFloat()
                            playbackStatusText = String.format(
                                Locale.US,
                                "Playing: 24kHz Master Audio (%.1fs)",
                                result.durationSeconds
                            )
                        }
                        is SynthesisResult.Error -> {
                            playbackStatusText = "Synthesis failed. Check Settings!"
                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            enabled = !isSynthesizing,
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
            if (isSynthesizing) {
                CircularProgressIndicator(
                    color = AccentEmerald,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "SYNTHESIZING SPEECH...",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            } else {
                Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SYNTHESIZE SPEECH MASTER",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Master Audio Playback Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (ttsEngine.isPlaying()) {
                            ttsEngine.stopPlayback()
                            playbackStatusText = "Audio playback stopped."
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .background(DarkSurface, CircleShape)
                ) {
                    Icon(
                        imageVector = if (ttsEngine.isPlaying()) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = AccentEmerald
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playbackStatusText,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (lastAudioDuration > 0f) {
                            String.format(Locale.US, "Duration: %.2f sec • 16-bit PCM Linear", lastAudioDuration)
                        } else {
                            "24,000 Hz Mono • Engine 1 Active"
                        },
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(if (ttsEngine.isPlaying()) AccentEmerald else TextSecondary, CircleShape)
                )
            }
        }
    }
}
