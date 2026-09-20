package com.hybridtts.ui.screens

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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hybridtts.ui.theme.AccentCyan
import com.hybridtts.ui.theme.AccentEmerald
import com.hybridtts.ui.theme.BorderSubtle
import com.hybridtts.ui.theme.DarkCard
import com.hybridtts.ui.theme.DarkSurface
import com.hybridtts.ui.theme.PureBlack
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
        // MODE SELECTOR: AUTO vs MANUAL (As requested!)
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
                // Auto Mode Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isAutoMode) AccentEmerald else DarkCard,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { isAutoMode = true }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isAutoMode) PureBlack else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AUTO DETECT",
                            color = if (isAutoMode) PureBlack else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Manual Mode Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (!isAutoMode) AccentCyan else DarkCard,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { isAutoMode = false }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = if (!isAutoMode) PureBlack else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MANUAL SWAP",
                            color = if (!isAutoMode) PureBlack else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Isometric Sync Toggle Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = AccentEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "ISOMETRIC TIMELINE CONSERVER",
                            color = AccentEmerald,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Locks audio duration within ±5ms",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                Switch(
                    checked = isometricSyncEnabled,
                    onCheckedChange = { isometricSyncEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PureBlack,
                        checkedTrackColor = AccentEmerald,
                        uncheckedTrackColor = DarkSurface
                    )
                )
            }
        }

        // Speaker A Card (Clean Layout, No Collisions)
        RefinedSpeakerCard(
            speakerName = "SPEAKER A",
            language = speakerALang,
            transcript = speakerATranscript,
            accentColor = AccentEmerald
        )

        // Center Switcher (Active in Manual Mode)
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = {
                    val tempL = speakerALang
                    speakerALang = speakerBLang
                    speakerBLang = tempL

                    val tempT = speakerATranscript
                    speakerATranscript = speakerBTranscript
                    speakerBTranscript = tempT
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(DarkCard, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Swap Languages",
                    tint = if (!isAutoMode) AccentCyan else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Speaker B Card (Clean Layout, No Collisions)
        RefinedSpeakerCard(
            speakerName = "SPEAKER B",
            language = speakerBLang,
            transcript = speakerBTranscript,
            accentColor = AccentCyan
        )
    }
}

@Composable
fun RefinedSpeakerCard(
    speakerName: String,
    language: String,
    transcript: String,
    accentColor: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row with clean separation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = speakerName,
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )

                // Language Pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DarkSurface,
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Text(
                        text = language,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Clean Transcript Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = transcript,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Microphone Trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = { /* Staged for mic in Day 8 */ },
                    modifier = Modifier
                        .size(38.dp)
                        .background(accentColor, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = PureBlack,
                        modifier = Modifier.size(20.dp)
                    )
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
                Text(
                    text = "WEBRTC PEER-TO-PEER ENCLAVE",
                    color = AccentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Direct encrypted data stream. Cellular Uplink bypass enabled. Voice disguise runs locally on the Helio G85 DSP.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        IconButton(
            onClick = { /* Push to talk staged for Day 8 */ },
            modifier = Modifier
                .size(110.dp)
                .background(DarkCard, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                tint = AccentEmerald,
                modifier = Modifier.size(50.dp)
            )
        }

        Text(
            text = "HOLD TO TALK // DISGUISED",
            color = TextPrimary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Target Disguise Profile: Aura Neutral (512-dim)",
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}
