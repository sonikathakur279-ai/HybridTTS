package com.hybridtts.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.hybridtts.core.IsometricDubbingCoordinator
import com.hybridtts.core.TimeWarpResult
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
fun CommsScreen() {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Video Dubbing", "Dual Interpreter", "VoIP Walkie-Talkie")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(16.dp)
    ) {
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = DarkCard,
            contentColor = AccentEmerald
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSubTab == index,
                    onClick = { selectedSubTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (selectedSubTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (selectedSubTab) {
            0 -> IsometricVideoDubbingView()
            1 -> DualInterpreterView()
            2 -> VoipWalkieTalkieView()
        }
    }
}

@Composable
fun IsometricVideoDubbingView() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val coordinator = remember { IsometricDubbingCoordinator.getInstance(context) }
    val ttsEngine = remember { CloudTtsEngine.getInstance(context) }

    var sourceDialogue by remember {
        mutableStateOf("We have to leave the facility immediately, the reactor is unstable!")
    }
    var translatedDialogue by remember { mutableStateOf("") }
    var sourceLanguage by remember { mutableStateOf("English (US)") }
    var targetLanguage by remember { mutableStateOf("Hindi (Devanagari)") }

    var startTimestampMs by remember { mutableLongStateOf(1200L) }
    var endTimestampMs by remember { mutableLongStateOf(4620L) }
    val targetDurationSec = (endTimestampMs - startTimestampMs) / 1000.0

    var isTranslating by remember { mutableStateOf(false) }
    var isWarperProcessing by remember { mutableStateOf(false) }
    var warpResult by remember { mutableStateOf<TimeWarpResult?>(null) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Video Scene Cue & Timecode Header Card
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
                    Text(
                        text = "VIDEO CUE TIMECODE",
                        color = AccentCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurface,
                        border = BorderStroke(1.dp, AccentCyan)
                    ) {
                        Text(
                            text = String.format(Locale.US, "TARGET: %.3fs", targetDurationSec),
                            color = AccentCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("In: 00:01.200", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("Out: 00:04.620", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("Speaker: Elias (Lead)", color = AccentEmerald, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Source Dialogue Input Card
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
                    Text("SOURCE DIALOGUE", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(sourceLanguage, color = AccentEmerald, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = sourceDialogue,
                    onValueChange = { sourceDialogue = it },
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
                    singleLine = false
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Stage 1 Trigger Button
                Button(
                    onClick = {
                        isTranslating = true
                        scope.launch {
                            val result = coordinator.executeStage1Translation(
                                sourceText = sourceDialogue,
                                sourceLang = sourceLanguage,
                                targetLang = targetLanguage,
                                targetDurationSec = targetDurationSec
                            )
                            isTranslating = false
                            result.fold(
                                onSuccess = { translatedText ->
                                    translatedDialogue = translatedText
                                    Toast.makeText(context, "Stage 1 Syllable Match Complete!", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { err ->
                                    Toast.makeText(context, "Translation error: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    enabled = !isTranslating,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald, contentColor = PureBlack)
                ) {
                    if (isTranslating) {
                        CircularProgressIndicator(color = PureBlack, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("STAGE 1: MATCHING SYLLABLES...", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("STAGE 1: ISOMETRIC TRANSLATION", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Stage 2: Translated Script & Acoustic Time-Warper Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, if (translatedDialogue.isNotBlank()) AccentCyan else BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ISOMETRIC DUBBED SCRIPT", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(targetLanguage, color = AccentCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = translatedDialogue,
                    onValueChange = { translatedDialogue = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = BorderSubtle,
                        cursorColor = AccentCyan
                    ),
                    placeholder = { Text("Run Stage 1 to generate syllable-matched translation...", color = TextSecondary, fontSize = 11.sp) },
                    singleLine = false
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Stage 2 Trigger Button
                Button(
                    onClick = {
                        if (translatedDialogue.isBlank()) {
                            Toast.makeText(context, "Please run Stage 1 translation first!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isWarperProcessing = true
                        scope.launch {
                            val result = coordinator.executeStage2AcousticWarp(
                                translatedText = translatedDialogue,
                                assignedVoice = "Charon",
                                targetDurationSec = targetDurationSec
                            )
                            isWarperProcessing = false
                            result.fold(
                                onSuccess = { warp ->
                                    warpResult = warp
                                    Toast.makeText(context, "Locked to timeline within ±${warp.deltaMilliseconds}ms!", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { err ->
                                    Toast.makeText(context, "Warp error: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    enabled = !isWarperProcessing && translatedDialogue.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentCyan,
                        contentColor = PureBlack,
                        disabledContainerColor = DarkSurface,
                        disabledContentColor = TextSecondary
                    )
                ) {
                    if (isWarperProcessing) {
                        CircularProgressIndicator(color = PureBlack, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("STAGE 2: TIME-WARPING...", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("STAGE 2: SYNTHESIZE & SONIC WARP", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Timeline Duration Conserver Card (Visual Comparison & Lock Badge)
        warpResult?.let { warp ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = BorderStroke(1.dp, AccentEmerald)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TIMELINE LOCKED (±${warp.deltaMilliseconds}ms DRIFT)",
                                color = AccentEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, AccentCyan)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.2fx SONIC DSP", warp.speedMultiplier),
                                color = AccentCyan,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Original Video Target: ${String.format(Locale.US, "%.3fs", warp.originalTargetSeconds)}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Unwarped Synthesis: ${String.format(Locale.US, "%.3fs", warp.synthesizedSeconds)}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Final Dubbed Output: ${String.format(Locale.US, "%.3fs", warp.finalWarpedSeconds)}",
                        color = AccentEmerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                ttsEngine.playAudio(speed = warp.speedMultiplier, pitchSemitones = 0.0f)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald, contentColor = PureBlack)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PLAY SYNC DUB", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val result = ttsEngine.saveAudioToDownloads("Dubbed_Sync_Cue")
                                result.fold(
                                    onSuccess = { Toast.makeText(context, "Saved to Downloads/HybridTTS!", Toast.LENGTH_LONG).show() },
                                    onFailure = { err -> Toast.makeText(context, err.localizedMessage, Toast.LENGTH_SHORT).show() }
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface, contentColor = AccentCyan),
                            border = BorderStroke(1.dp, AccentCyan)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("DOWNLOAD WAV", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DualInterpreterView() {
    val scrollState = rememberScrollState()
    var isometricSyncEnabled by remember { mutableStateOf(true) }
    var isAutoMode by remember { mutableStateOf(true) }

    var speakerALang by remember { mutableStateOf("English (US)") }
    var speakerBLang by remember { mutableStateOf("Hindi (IN)") }

    var speakerATranscript by remember {
        mutableStateOf("Tap the microphone or speak. Speech will be auto-detected and translated.")
    }
    var speakerBTranscript by remember {
        mutableStateOf("बोलने के लिए माइक दबाएं। रीयल-टाइम अनुवाद यहाँ उत्पन्न होगा।")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (isAutoMode) AccentEmerald else DarkCard, RoundedCornerShape(8.dp))
                        .clickable { isAutoMode = true }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = if (isAutoMode) PureBlack else TextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AUTO DETECT", color = if (isAutoMode) PureBlack else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (!isAutoMode) AccentCyan else DarkCard, RoundedCornerShape(8.dp))
                        .clickable { isAutoMode = false }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.TouchApp, contentDescription = null, tint = if (!isAutoMode) PureBlack else TextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("MANUAL SWAP", color = if (!isAutoMode) PureBlack else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        RefinedSpeakerCard(speakerName = "SPEAKER A", language = speakerALang, transcript = speakerATranscript, accentColor = AccentEmerald)

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            IconButton(
                onClick = {
                    val tempL = speakerALang
                    speakerALang = speakerBLang
                    speakerBLang = tempL

                    val tempT = speakerATranscript
                    speakerATranscript = speakerBTranscript
                    speakerBTranscript = tempT
                },
                modifier = Modifier.size(40.dp).background(DarkCard, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, tint = if (!isAutoMode) AccentCyan else TextSecondary, modifier = Modifier.size(22.dp))
            }
        }

        RefinedSpeakerCard(speakerName = "SPEAKER B", language = speakerBLang, transcript = speakerBTranscript, accentColor = AccentCyan)
    }
}

@Composable
fun RefinedSpeakerCard(speakerName: String, language: String, transcript: String, accentColor: androidx.compose.ui.graphics.Color) {
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
                Text(text = speakerName, color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Surface(shape = RoundedCornerShape(6.dp), color = DarkSurface, border = BorderStroke(1.dp, BorderSubtle)) {
                    Text(text = language, color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().background(DarkSurface, RoundedCornerShape(8.dp)).padding(10.dp)) {
                Text(text = transcript, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = {}, modifier = Modifier.size(38.dp).background(accentColor, CircleShape)) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = PureBlack, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun VoipWalkieTalkieView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("WEBRTC PEER-TO-PEER ENCLAVE", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Encrypted voice stream. Cellular Uplink bypass enabled. Real-time voice disguise active.", color = TextSecondary, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        IconButton(onClick = {}, modifier = Modifier.size(110.dp).background(DarkCard, CircleShape)) {
            Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(50.dp))
        }

        Text("HOLD TO TALK // DISGUISED", color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Text("Target Profile: Aura Neutral (512-dim)", color = TextSecondary, fontSize = 11.sp)
    }
}
