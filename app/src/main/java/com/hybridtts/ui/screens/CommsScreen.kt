package com.hybridtts.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.hybridtts.core.CloudTtsEngine
import com.hybridtts.core.InterpreterState
import com.hybridtts.core.LiveSpeechInterpreterManager
import com.hybridtts.core.VoiceDisguiseProfile
import com.hybridtts.core.VoipInterpreterService
import com.hybridtts.ui.theme.AccentCyan
import com.hybridtts.ui.theme.AccentEmerald
import com.hybridtts.ui.theme.BorderSubtle
import com.hybridtts.ui.theme.DarkCard
import com.hybridtts.ui.theme.DarkSurface
import com.hybridtts.ui.theme.PureBlack
import com.hybridtts.ui.theme.StatusError
import com.hybridtts.ui.theme.StatusWarning
import com.hybridtts.ui.theme.TextPrimary
import com.hybridtts.ui.theme.TextSecondary

@Composable
fun CommsScreen() {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Dual Interpreter", "VoIP Walkie-Talkie")

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
                            fontSize = 12.sp,
                            fontWeight = if (selectedSubTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedSubTab == 0) {
            DualInterpreterView()
        } else {
            VoipWalkieTalkieView()
        }
    }
}

@Composable
fun DualInterpreterView() {
    val context = LocalContext.current
    val interpreter = remember { LiveSpeechInterpreterManager.getInstance(context) }
    val interpreterStatus by interpreter.interpreterState.collectAsState()

    var isAutoMode by remember { mutableStateOf(true) }

    var speakerALang by remember { mutableStateOf("English (US)") }
    var speakerBLang by remember { mutableStateOf("Hindi (IN)") }
    var speakerAVoice by remember { mutableStateOf("Charon") }
    var speakerBVoice by remember { mutableStateOf("Aoede") }

    var speakerAText by remember { mutableStateOf("Press mic to speak. Voice will be recognized and translated.") }
    var speakerBText by remember { mutableStateOf("बोलने के लिए माइक दबाएं। अनुवाद यहाँ प्रदर्शित होगा।") }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasMicPermission = granted
        if (!granted) {
            Toast.makeText(context, "Microphone permission required for interpreter!", Toast.LENGTH_SHORT).show()
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mode Selector: Auto vs Manual
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

        // Speaker A Card
        LiveSpeakerCard(
            speakerTag = "SPEAKER A",
            currentLanguage = speakerALang,
            currentVoice = speakerAVoice,
            transcript = speakerAText,
            accentColor = AccentEmerald,
            isActiveListening = interpreterStatus is InterpreterState.Listening && (interpreterStatus as InterpreterState.Listening).speakerTag == "SPEAKER A",
            onLanguageChange = { speakerALang = it },
            onMicClick = {
                if (!hasMicPermission) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    return@LiveSpeakerCard
                }
                interpreter.startListening(
                    speakerTag = "SPEAKER A",
                    sourceLanguage = speakerALang,
                    targetLanguage = speakerBLang,
                    targetVoice = speakerBVoice,
                    onRecognized = { speakerAText = it },
                    onTranslated = { speakerBText = it }
                )
            }
        )

        // Center Language Swap Button
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            IconButton(
                onClick = {
                    val tempL = speakerALang
                    speakerALang = speakerBLang
                    speakerBLang = tempL

                    val tempV = speakerAVoice
                    speakerAVoice = speakerBVoice
                    speakerBVoice = tempV

                    val tempT = speakerAText
                    speakerAText = speakerBText
                    speakerBText = tempT
                },
                modifier = Modifier.size(40.dp).background(DarkCard, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, tint = if (!isAutoMode) AccentCyan else TextSecondary, modifier = Modifier.size(22.dp))
            }
        }

        // Speaker B Card
        LiveSpeakerCard(
            speakerTag = "SPEAKER B",
            currentLanguage = speakerBLang,
            currentVoice = speakerBVoice,
            transcript = speakerBText,
            accentColor = AccentCyan,
            isActiveListening = interpreterStatus is InterpreterState.Listening && (interpreterStatus as InterpreterState.Listening).speakerTag == "SPEAKER B",
            onLanguageChange = { speakerBLang = it },
            onMicClick = {
                if (!hasMicPermission) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    return@LiveSpeakerCard
                }
                interpreter.startListening(
                    speakerTag = "SPEAKER B",
                    sourceLanguage = speakerBLang,
                    targetLanguage = speakerALang,
                    targetVoice = speakerAVoice,
                    onRecognized = { speakerBText = it },
                    onTranslated = { speakerAText = it }
                )
            }
        )

        // Live Interpreter Status Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = when (interpreterStatus) {
                        is InterpreterState.Listening -> AccentEmerald
                        is InterpreterState.Translating -> AccentCyan
                        is InterpreterState.Speaking -> StatusWarning
                        else -> TextSecondary
                    },
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (val s = interpreterStatus) {
                        is InterpreterState.Listening -> "Listening to ${s.speakerTag}..."
                        is InterpreterState.Translating -> "Translating: \"${s.recognizedText.take(28)}...\""
                        is InterpreterState.Speaking -> "Synthesizing Speech Master..."
                        is InterpreterState.Error -> "Error: ${s.message}"
                        else -> "Interpreter Idle • Tap microphone to speak"
                    },
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun LiveSpeakerCard(
    speakerTag: String,
    currentLanguage: String,
    currentVoice: String,
    transcript: String,
    accentColor: androidx.compose.ui.graphics.Color,
    isActiveListening: Boolean,
    onLanguageChange: (String) -> Unit,
    onMicClick: () -> Unit
) {
    var isLangMenuOpen by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, if (isActiveListening) accentColor else BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = speakerTag, color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

                Box {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurface,
                        border = BorderStroke(1.dp, accentColor),
                        modifier = Modifier.clickable { isLangMenuOpen = true }
                    ) {
                        Text(
                            text = "$currentLanguage ($currentVoice)",
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    DropdownMenu(expanded = isLangMenuOpen, onDismissRequest = { isLangMenuOpen = false }, modifier = Modifier.background(DarkCard)) {
                        LiveSpeechInterpreterManager.SUPPORTED_LANGUAGES.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang, color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                                onClick = {
                                    onLanguageChange(lang)
                                    isLangMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(modifier = Modifier.fillMaxWidth().background(DarkSurface, RoundedCornerShape(8.dp)).padding(10.dp)) {
                Text(text = transcript, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(
                    onClick = onMicClick,
                    modifier = Modifier.size(42.dp).background(if (isActiveListening) StatusError else accentColor, CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = PureBlack, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
fun VoipWalkieTalkieView() {
    val context = LocalContext.current
    val isCallActive by VoipInterpreterService.isCallActive.collectAsState()
    val audioInputDb by VoipInterpreterService.audioInputDb.collectAsState()

    var peerIp by remember { mutableStateOf(VoipInterpreterService.targetPeerIpAddress) }
    var selectedProfile by remember { mutableStateOf(VoipInterpreterService.currentDisguiseProfile) }
    var isProfileDropdownOpen by remember { mutableStateOf(false) }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasMicPermission = granted
    }

    val disguiseProfiles = listOf(
        Pair(VoiceDisguiseProfile.TITAN_DEEP, "Titan Deep Pitch (-4 semitones)"),
        Pair(VoiceDisguiseProfile.AURA_HIGH, "Aura Elevated Pitch (+3 semitones)"),
        Pair(VoiceDisguiseProfile.CYBER_ROBOT, "Cybernetic Ring Modulator"),
        Pair(VoiceDisguiseProfile.BYPASS, "Unaltered Natural Voice")
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Enclave Security Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WEBRTC P2P ENCLAVE & DISGUISE ENGINE", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Cellular uplink bypass active. Real-time pitch & formant morphing runs on Helio G85 Cortex-A55 cluster.", color = TextSecondary, fontSize = 10.sp)
            }
        }

        // Voice Disguise Profile Selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("LIVE VOCAL DISGUISE PROFILE", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(6.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurface,
                        border = BorderStroke(1.dp, AccentEmerald),
                        modifier = Modifier.fillMaxWidth().clickable { isProfileDropdownOpen = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = disguiseProfiles.first { it.first == selectedProfile }.second,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    DropdownMenu(expanded = isProfileDropdownOpen, onDismissRequest = { isProfileDropdownOpen = false }, modifier = Modifier.background(DarkCard)) {
                        disguiseProfiles.forEach { (profile, label) ->
                            DropdownMenuItem(
                                text = { Text(label, color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                                onClick = {
                                    selectedProfile = profile
                                    VoipInterpreterService.currentDisguiseProfile = profile
                                    isProfileDropdownOpen = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Target Peer IP Input Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("PEER DEVICE IP ADDRESS (WI-FI / HOTSPOT)", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = peerIp,
                    onValueChange = {
                        peerIp = it
                        VoipInterpreterService.targetPeerIpAddress = it
                    },
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
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Large PTT (Push-To-Talk) Call Button
        IconButton(
            onClick = {
                if (!hasMicPermission) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    return@IconButton
                }

                val intent = Intent(context, VoipInterpreterService::class.java)
                if (isCallActive) {
                    intent.action = "ACTION_STOP_VOIP"
                    context.startService(intent)
                } else {
                    intent.action = "ACTION_START_VOIP"
                    context.startService(intent)
                }
            },
            modifier = Modifier
                .size(110.dp)
                .background(if (isCallActive) StatusError else AccentEmerald, CircleShape)
        ) {
            Icon(
                imageVector = if (isCallActive) Icons.Default.CallEnd else Icons.Default.Call,
                contentDescription = null,
                tint = PureBlack,
                modifier = Modifier.size(50.dp)
            )
        }

        Text(
            text = if (isCallActive) "CALL ACTIVE // TRANSMITTING DISGUISED" else "TAP TO CONNECT P2P CALL",
            color = if (isCallActive) AccentEmerald else TextPrimary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )

        // Live Audio Amplitude dB Meter
        if (isCallActive) {
            Text(
                text = String.format(java.util.Locale.US, "Mic Amplitude: %.1f dB • DSP Shifter Active", audioInputDb),
                color = AccentCyan,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
