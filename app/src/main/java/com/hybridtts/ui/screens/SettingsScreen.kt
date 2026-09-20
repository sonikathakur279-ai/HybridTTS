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
import androidx.compose.material.icons.filled.NetworkCheck
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
import com.hybridtts.core.KeyPoolManager
import com.hybridtts.core.KeyStatus
import com.hybridtts.core.ModelRegistryManager
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

    var selectedEngineIndex by remember { mutableIntStateOf(0) }
    var newApiKeyInput by remember { mutableStateOf("") }
    var keyList by remember { mutableStateOf<List<ApiKeyEntity>>(emptyList()) }
    var activeModelName by remember { mutableStateOf(modelRegistry.getActiveModel()) }

    var isCheckingConnection by remember { mutableStateOf(false) }
    var connectionCheckStatus by remember { mutableStateOf("") }

    var cpuThreads by remember { mutableFloatStateOf(2f) }
    var thermalProtection by remember { mutableStateOf(true) }
    var ringBufferStreaming by remember { mutableStateOf(true) }

    val scrollState = rememberScrollState()

    fun reloadKeys() {
        keyList = keyPool.getAllKeys()
    }

    LaunchedEffect(Unit) {
        reloadKeys()
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
                    name = "Engine 1: Direct Cloud TTS (Live Today)",
                    desc = "Google AI Studio High-Fi Master Audio streaming",
                    isSelected = selectedEngineIndex == 0,
                    onSelect = { selectedEngineIndex = 0 }
                )
                EngineOptionRow(
                    name = "Engine 2: Cloud Hybrid (Staged Day 4)",
                    desc = "Gemini AI Director + Local ONNX speech synthesis",
                    isSelected = selectedEngineIndex == 1,
                    onSelect = { selectedEngineIndex = 1 }
                )
                EngineOptionRow(
                    name = "Engine 3: 100% Fully Local ONNX (Staged Day 5)",
                    desc = "Offline Kokoro-82M int8 engine (Zero cloud, unlimited)",
                    isSelected = selectedEngineIndex == 2,
                    onSelect = { selectedEngineIndex = 2 }
                )
            }
        }

        // Dedicated Google AI Studio TTS Model Selector Card
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
                        text = "GOOGLE AI STUDIO TTS MODEL",
                        color = AccentCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurface,
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Text(
                            text = "3 PREVIEW MODELS",
                            color = AccentEmerald,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                ModelRegistryManager.SUPPORTED_TTS_MODELS.forEach { modelId ->
                    val isSelected = activeModelName == modelId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .background(
                                if (isSelected) DarkSurface else PureBlack,
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) AccentCyan else BorderSubtle,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                activeModelName = modelId
                                modelRegistry.setActiveModel(modelId)
                                connectionCheckStatus = ""
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(if (isSelected) AccentCyan else BorderSubtle, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = when (modelId) {
                                    ModelRegistryManager.MODEL_GEMINI_3_1_FLASH_TTS -> "Gemini 3.1 Flash TTS Preview (Recommended)"
                                    ModelRegistryManager.MODEL_GEMINI_2_5_FLASH_TTS -> "Gemini 2.5 Flash TTS Preview"
                                    ModelRegistryManager.MODEL_GEMINI_2_5_PRO_TTS -> "Gemini 2.5 Pro TTS Preview"
                                    else -> modelId
                                },
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = modelId,
                                color = if (isSelected) AccentCyan else TextSecondary,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Connection & Auto-Checker Test Button
                Button(
                    onClick = {
                        val activeKey = keyPool.getNextActiveKey()
                        if (activeKey == null) {
                            Toast.makeText(context, "Please add an API key first!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isCheckingConnection = true
                        connectionCheckStatus = "Testing handshake with Google AI Studio..."

                        scope.launch {
                            val result = modelRegistry.testConnection(activeKey, activeModelName)
                            isCheckingConnection = false
                            connectionCheckStatus = result.fold(
                                onSuccess = { it },
                                onFailure = { "Handshake Failed: ${it.localizedMessage}" }
                            )
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
                    if (isCheckingConnection) {
                        CircularProgressIndicator(
                            color = AccentEmerald,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(imageVector = Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = "TEST API KEY & MODEL CONNECTION",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (connectionCheckStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = connectionCheckStatus,
                        color = if (connectionCheckStatus.startsWith("HTTP 200")) AccentEmerald else StatusError,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
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

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Add free Google AI Studio keys. The dispatcher automatically rotates keys and sets 24-hour cooldowns on HTTP 429 quota exhaustion.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

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
                            reloadKeys()
                            Toast.makeText(context, "Key securely added to vault!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Invalid or duplicate key", Toast.LENGTH_SHORT).show()
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
                    Text("ADD KEY TO ENCRYPTED VAULT", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                if (keyList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "ENCRYPTED KEY POOL ROTATION LIST",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    keyList.forEach { entity ->
                        KeyRowItem(
                            keyEntity = entity,
                            onDelete = {
                                keyPool.removeKey(entity.key)
                                reloadKeys()
                            }
                        )
                    }
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

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Thermal Guard Watchdog", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("Auto-pause synthesis at 42°C, resume at 38°C", color = TextSecondary, fontSize = 10.sp)
                    }
                    Switch(
                        checked = thermalProtection,
                        onCheckedChange = { thermalProtection = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PureBlack,
                            checkedTrackColor = StatusWarning,
                            uncheckedTrackColor = DarkSurface
                        )
                    )
                }
            }
        }

        // Storage Pipeline Switch
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "STORAGE WRITE PIPELINE",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (ringBufferStreaming) "Option A: 64KB Ring-Buffer (Flash Safe)" else "Option B: Modular Discrete Chunks",
                        color = AccentEmerald,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Switch(
                    checked = ringBufferStreaming,
                    onCheckedChange = { ringBufferStreaming = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PureBlack,
                        checkedTrackColor = AccentEmerald,
                        uncheckedTrackColor = DarkSurface
                    )
                )
            }
        }
    }
}

@Composable
fun KeyRowItem(keyEntity: ApiKeyEntity, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(DarkSurface, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            when (keyEntity.status) {
                                KeyStatus.ACTIVE -> AccentEmerald
                                KeyStatus.COOLDOWN -> StatusWarning
                                KeyStatus.DEPLETED -> StatusError
                            },
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = keyEntity.maskedKey,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "Requests: ${keyEntity.requestCount} • ${keyEntity.status.name}",
                color = TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete key",
                tint = StatusError,
                modifier = Modifier.size(16.dp)
            )
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
