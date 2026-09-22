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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
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
import com.hybridtts.core.ApiKeyEntity
import com.hybridtts.core.CustomLexiconRepository
import com.hybridtts.core.KeyPoolManager
import com.hybridtts.core.KeyStatus
import com.hybridtts.core.LexiconEntry
import com.hybridtts.core.ModelRegistryManager
import com.hybridtts.core.StudioStateManager
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
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyPool = remember { KeyPoolManager.getInstance(context) }
    val modelRegistry = remember { ModelRegistryManager.getInstance(context) }
    val lexiconRepo = remember { CustomLexiconRepository.getInstance(context) }

    var newApiKeyInput by remember { mutableStateOf("") }
    var keyList by remember { mutableStateOf<List<ApiKeyEntity>>(emptyList()) }
    var activeModelName by remember { mutableStateOf(modelRegistry.getActiveModel()) }

    var isCheckingConnection by remember { mutableStateOf(false) }
    var connectionCheckStatus by remember { mutableStateOf("") }

    // Lexicon UI state
    var lexiconList by remember { mutableStateOf<List<LexiconEntry>>(emptyList()) }
    var newLexiconWord by remember { mutableStateOf("") }
    var newLexiconReplacement by remember { mutableStateOf("") }

    var cpuThreads by remember { mutableFloatStateOf(2f) }
    var thermalProtection by remember { mutableStateOf(true) }
    var ringBufferStreaming by remember { mutableStateOf(true) }

    val scrollState = rememberScrollState()

    fun reloadAll() {
        keyList = keyPool.getAllKeys()
        lexiconList = lexiconRepo.getAllEntries()
    }

    LaunchedEffect(Unit) {
        reloadAll()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Engine Selector Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = AccentEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PRIMARY ACOUSTIC ENGINE SELECTION",
                        color = AccentEmerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                EngineOptionRow(
                    name = "Engine 1: Direct Cloud TTS (Live)",
                    desc = "Google AI Studio High-Fi Master Audio preview",
                    isSelected = StudioStateManager.activeEngineMode == 0,
                    onSelect = { StudioStateManager.activeEngineMode = 0 }
                )
                EngineOptionRow(
                    name = "Engine 2: Cloud Hybrid (Default Production)",
                    desc = "Gemini AI Director + Prosody micro-pause stitching",
                    isSelected = StudioStateManager.activeEngineMode == 1,
                    onSelect = { StudioStateManager.activeEngineMode = 1 }
                )
                EngineOptionRow(
                    name = "Engine 3: 100% Fully Local ONNX (Offline)",
                    desc = "Offline Kokoro-82M neural engine (Airplane Mode Ready)",
                    isSelected = StudioStateManager.activeEngineMode == 2,
                    onSelect = { StudioStateManager.activeEngineMode = 2 }
                )
            }
        }

        // SQLite Custom Pronunciation Lexicon Card
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
                            imageVector = Icons.Default.Spellcheck,
                            contentDescription = null,
                            tint = AccentEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SQLITE PRONUNCIATION LEXICON",
                            color = AccentEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "${lexiconList.size} Rules Active",
                        color = AccentCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Intercepts words before local neural synthesis to enforce custom phonetic spellings.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newLexiconWord,
                        onValueChange = { newLexiconWord = it },
                        modifier = Modifier.weight(1f),
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
                        placeholder = { Text("Word (e.g. ASUS)", color = TextSecondary, fontSize = 11.sp) },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newLexiconReplacement,
                        onValueChange = { newLexiconReplacement = it },
                        modifier = Modifier.weight(1f),
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
                        placeholder = { Text("Sounds like (ay-soos)", color = TextSecondary, fontSize = 11.sp) },
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (newLexiconWord.isNotBlank() && newLexiconReplacement.isNotBlank()) {
                            lexiconRepo.addOrUpdateEntry(
                                LexiconEntry(
                                    originalWord = newLexiconWord,
                                    phoneticReplacement = newLexiconReplacement
                                )
                            )
                            newLexiconWord = ""
                            newLexiconReplacement = ""
                            reloadAll()
                            Toast.makeText(context, "Pronunciation rule added!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurface,
                        contentColor = AccentEmerald
                    ),
                    border = BorderStroke(1.dp, AccentEmerald)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ADD PRONUNCIATION RULE", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                if (lexiconList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    lexiconList.forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .background(DarkSurface, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${entry.originalWord}  ➔  ${entry.phoneticReplacement}",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (entry.ipaNotation.isNotBlank()) {
                                    Text(text = "IPA: ${entry.ipaNotation}", color = AccentCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }
                            }

                            IconButton(
                                onClick = {
                                    lexiconRepo.deleteEntry(entry.id)
                                    reloadAll()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = StatusError, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Multi-API Key Vault Card
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
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AES-256 KEY POOL VAULT",
                            color = AccentCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "${keyList.count { it.status == KeyStatus.ACTIVE }} Active / ${keyList.size} Total",
                        color = AccentEmerald,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = newApiKeyInput,
                    onValueChange = { newApiKeyInput = it },
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
                    placeholder = { Text("Paste AI Studio Key (AIzaSy...)", color = TextSecondary, fontSize = 12.sp) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (keyPool.addKey(newApiKeyInput)) {
                            newApiKeyInput = ""
                            reloadAll()
                            Toast.makeText(context, "Key added to vault!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurface,
                        contentColor = AccentCyan
                    ),
                    border = BorderStroke(1.dp, AccentCyan)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ADD KEY TO VAULT", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Helio G85 Hardware Threading & Thermals
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = StatusWarning,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "HELIO G85 CPU INFERENCE TUNING",
                        color = StatusWarning,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Inference Worker Threads", color = TextSecondary, fontSize = 12.sp)
                    Text(
                        text = "${cpuThreads.roundToInt()} Cores (Cortex-A55 Pinned)",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = cpuThreads,
                    onValueChange = { cpuThreads = it },
                    valueRange = 1f..4f,
                    steps = 2,
                    colors = SliderDefaults.colors(
                        thumbColor = StatusWarning,
                        activeTrackColor = StatusWarning,
                        inactiveTrackColor = BorderSubtle
                    )
                )
            }
        }
    }
}

@Composable
fun EngineOptionRow(
    name: String,
    desc: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Button(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) DarkSurface else PureBlack
        ),
        border = BorderStroke(1.dp, if (isSelected) AccentEmerald else BorderSubtle)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isSelected) AccentEmerald else BorderSubtle,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = name,
                    color = if (isSelected) AccentEmerald else TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = desc,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}
