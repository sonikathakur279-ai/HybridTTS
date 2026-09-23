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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.hybridtts.core.CharacterProfile
import com.hybridtts.core.CharacterTier
import com.hybridtts.core.CloudTtsEngine
import com.hybridtts.core.SpeakerRegistry
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
fun VoicesScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val speakerRegistry = remember { SpeakerRegistry.getInstance(context) }
    val ttsEngine = remember { CloudTtsEngine.getInstance(context) }

    var selectedTierIndex by remember { mutableIntStateOf(0) }
    val tierTabs = listOf("All Roles", "Main Cast", "Supporting", "Extras")

    var searchQuery by remember { mutableStateOf("") }
    var isBlendingConsoleExpanded by remember { mutableStateOf(false) }

    // Vector Blending Studio state: V_blended = (1 - alpha) * V_A + alpha * V_B
    var blendVoiceA by remember { mutableStateOf("Charon") }
    var blendVoiceB by remember { mutableStateOf("Aoede") }
    var blendAlpha by remember { mutableFloatStateOf(0.5f) }

    val cast = speakerRegistry.activeCast

    val filteredCast = cast.filter { character ->
        val matchesTier = when (selectedTierIndex) {
            1 -> character.tier == CharacterTier.MAIN_CAST
            2 -> character.tier == CharacterTier.SUPPORTING
            3 -> character.tier == CharacterTier.EXTRAS
            else -> true
        }
        val matchesSearch = character.characterName.contains(searchQuery, ignoreCase = true) ||
                character.assignedVoiceName.contains(searchQuery, ignoreCase = true)
        matchesTier && matchesSearch
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(16.dp)
    ) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = DarkCard,
                unfocusedContainerColor = DarkCard,
                focusedBorderColor = AccentEmerald,
                unfocusedBorderColor = BorderSubtle,
                cursorColor = AccentEmerald
            ),
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextSecondary)
            },
            placeholder = { Text("Search characters & assigned voices...", color = TextSecondary, fontSize = 12.sp) },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Dialogue Hierarchy Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tierTabs.forEachIndexed { index, title ->
                FilterChip(
                    selected = (selectedTierIndex == index),
                    onClick = { selectedTierIndex = index },
                    label = { Text(title, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentEmerald,
                        selectedLabelColor = PureBlack,
                        containerColor = DarkCard,
                        labelColor = TextPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Toolbar: Auto-Assign & Vector Blending Studio Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    speakerRegistry.autoAssignAllDistinct()
                    Toast.makeText(context, "Assigned 30 distinct voices across cast!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkCard,
                    contentColor = AccentEmerald
                ),
                border = BorderStroke(1.dp, AccentEmerald)
            ) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("AUTO-ASSIGN ALL", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }

            Button(
                onClick = {
                    speakerRegistry.bulkAssignExtras("Puck")
                    Toast.makeText(context, "All minor roles mapped to ambient profile!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkCard,
                    contentColor = AccentCyan
                ),
                border = BorderStroke(1.dp, AccentCyan)
            ) {
                Icon(imageVector = Icons.Default.Group, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("BULK EXTRAS", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Style Vector Blending Console Toggle Card
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkCard,
            border = BorderStroke(1.dp, if (isBlendingConsoleExpanded) AccentEmerald else BorderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isBlendingConsoleExpanded = !isBlendingConsoleExpanded }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "STYLE VECTOR BLENDING STUDIO (METHOD 3)",
                            color = AccentEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = if (isBlendingConsoleExpanded) "COLLAPSE" else "OPEN STUDIO",
                        color = AccentCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (isBlendingConsoleExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Mathematically interpolates two 512-dim style vectors into a new vocal identity with zero training overhead.",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VoicePickerDropdown(label = "Voice A", current = blendVoiceA, modifier = Modifier.weight(1f)) {
                            blendVoiceA = it
                        }
                        VoicePickerDropdown(label = "Voice B", current = blendVoiceB, modifier = Modifier.weight(1f)) {
                            blendVoiceB = it
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Interpolation Alpha (α)", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            String.format(Locale.US, "%.0f%% %s / %.0f%% %s", (1f - blendAlpha) * 100f, blendVoiceA, blendAlpha * 100f, blendVoiceB),
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Slider(
                        value = blendAlpha,
                        onValueChange = { blendAlpha = it },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentEmerald,
                            activeTrackColor = AccentEmerald,
                            inactiveTrackColor = BorderSubtle
                        )
                    )

                    Button(
                        onClick = {
                            val blendedVector = speakerRegistry.blendVectors(blendVoiceA, blendVoiceB, blendAlpha)
                            speakerRegistry.activeCast.add(
                                CharacterProfile(
                                    characterId = "HYBRID_${System.currentTimeMillis() % 1000}",
                                    characterName = "Custom Blend (${blendVoiceA.take(3)}+${blendVoiceB.take(3)})",
                                    assignedVoiceName = blendVoiceA,
                                    tier = CharacterTier.SUPPORTING,
                                    lineCount = 3,
                                    styleVector = blendedVector
                                )
                            )
                            Toast.makeText(context, "Blended 2 KB identity added to cast!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald, contentColor = PureBlack)
                    ) {
                        Text("SAVE BLENDED VOICE AS NEW CHARACTER", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Virtualized Character Deck List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filteredCast) { character ->
                CharacterDeckCard(
                    character = character,
                    onVoiceChange = { newVoice ->
                        speakerRegistry.assignVoice(character.characterId, newVoice)
                    },
                    onAudition = {
                        scope.launch {
                            ttsEngine.synthesizeMaster(
                                text = "Speaking as ${character.characterName} using neural profile ${character.assignedVoiceName}.",
                                voiceName = character.assignedVoiceName
                            )
                            ttsEngine.playAudio()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun CharacterDeckCard(
    character: CharacterProfile,
    onVoiceChange: (String) -> Unit,
    onAudition: () -> Unit
) {
    var isVoiceDropdownOpen by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = character.characterName,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (character.tier) {
                            CharacterTier.MAIN_CAST -> StatusWarning.copy(alpha = 0.2f)
                            CharacterTier.SUPPORTING -> AccentCyan.copy(alpha = 0.2f)
                            CharacterTier.EXTRAS -> DarkSurface
                        },
                        border = BorderStroke(
                            1.dp,
                            when (character.tier) {
                                CharacterTier.MAIN_CAST -> StatusWarning
                                CharacterTier.SUPPORTING -> AccentCyan
                                CharacterTier.EXTRAS -> BorderSubtle
                            }
                        )
                    ) {
                        Text(
                            text = when (character.tier) {
                                CharacterTier.MAIN_CAST -> "MAIN CAST"
                                CharacterTier.SUPPORTING -> "SUPPORTING"
                                CharacterTier.EXTRAS -> "EXTRA"
                            },
                            color = when (character.tier) {
                                CharacterTier.MAIN_CAST -> StatusWarning
                                CharacterTier.SUPPORTING -> AccentCyan
                                CharacterTier.EXTRAS -> TextSecondary
                            },
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${character.lineCount} lines • 2.0 KB Vector",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Box {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, AccentEmerald),
                            modifier = Modifier.clickable { isVoiceDropdownOpen = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = character.assignedVoiceName,
                                    color = AccentEmerald,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
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
                                    text = { Text(voice, color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                                    onClick = {
                                        onVoiceChange(voice)
                                        isVoiceDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            IconButton(
                onClick = onAudition,
                modifier = Modifier
                    .size(36.dp)
                    .background(DarkSurface, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Audition", tint = AccentEmerald, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun VoicePickerDropdown(label: String, current: String, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = DarkSurface,
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Text(label, color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text(current, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(DarkCard)) {
            CloudTtsEngine.ALL_30_VOICES.forEach { voice ->
                DropdownMenuItem(
                    text = { Text(voice, color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    onClick = {
                        onSelect(voice)
                        expanded = false
                    }
                )
            }
        }
    }
}
