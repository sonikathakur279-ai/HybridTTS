package com.hybridtts.core

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import kotlin.random.Random

enum class CharacterTier {
    MAIN_CAST,    // > 5 dialogue lines
    SUPPORTING,   // 2 to 5 dialogue lines
    EXTRAS        // 1 dialogue line (background)
}

data class CharacterProfile(
    val characterId: String,
    val characterName: String,
    val assignedVoiceName: String,
    val tier: CharacterTier = CharacterTier.SUPPORTING,
    val lineCount: Int = 1,
    val styleVector: FloatArray = FloatArray(VECTOR_DIM) { 0.0f }
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CharacterProfile
        return characterId == other.characterId && assignedVoiceName == other.assignedVoiceName
    }

    override fun hashCode(): Int {
        return characterId.hashCode() * 31 + assignedVoiceName.hashCode()
    }

    companion object {
        const val VECTOR_DIM = 512
    }
}

class SpeakerRegistry private constructor(context: Context) {

    companion object {
        const val VECTOR_DIM = 512

        @Volatile
        private var instance: SpeakerRegistry? = null

        fun getInstance(context: Context): SpeakerRegistry {
            return instance ?: synchronized(this) {
                instance ?: SpeakerRegistry(context.applicationContext).also { instance = it }
            }
        }
    }

    // Active registered cast for the project
    val activeCast = mutableStateListOf<CharacterProfile>()

    init {
        seedDefaultCast()
    }

    private fun seedDefaultCast() {
        if (activeCast.isEmpty()) {
            activeCast.addAll(
                listOf(
                    CharacterProfile(
                        characterId = "CHAR_01",
                        characterName = "Narrator",
                        assignedVoiceName = "Charon",
                        tier = CharacterTier.MAIN_CAST,
                        lineCount = 14,
                        styleVector = generateDeterministicVector("Charon")
                    ),
                    CharacterProfile(
                        characterId = "CHAR_02",
                        characterName = "Elias (Protagonist)",
                        assignedVoiceName = "Fenrir",
                        tier = CharacterTier.MAIN_CAST,
                        lineCount = 8,
                        styleVector = generateDeterministicVector("Fenrir")
                    ),
                    CharacterProfile(
                        characterId = "CHAR_03",
                        characterName = "Valeria (Commander)",
                        assignedVoiceName = "Aoede",
                        tier = CharacterTier.SUPPORTING,
                        lineCount = 4,
                        styleVector = generateDeterministicVector("Aoede")
                    ),
                    CharacterProfile(
                        characterId = "CHAR_04",
                        characterName = "Guard 1",
                        assignedVoiceName = "Puck",
                        tier = CharacterTier.EXTRAS,
                        lineCount = 1,
                        styleVector = generateDeterministicVector("Puck")
                    ),
                    CharacterProfile(
                        characterId = "CHAR_05",
                        characterName = "Guard 2",
                        assignedVoiceName = "Puck",
                        tier = CharacterTier.EXTRAS,
                        lineCount = 1,
                        styleVector = generateDeterministicVector("Puck")
                    )
                )
            )
        }
    }

    fun registerCharacter(name: String, lines: Int): CharacterProfile {
        val existing = activeCast.find { it.characterName.equals(name, ignoreCase = true) }
        if (existing != null) {
            val updated = existing.copy(
                lineCount = lines,
                tier = determineTier(lines)
            )
            val index = activeCast.indexOf(existing)
            activeCast[index] = updated
            return updated
        }

        val availableVoices = CloudTtsEngine.ALL_30_VOICES
        val voiceIndex = activeCast.size % availableVoices.size
        val assignedVoice = availableVoices[voiceIndex]

        val profile = CharacterProfile(
            characterId = "CHAR_${System.currentTimeMillis() % 10000}",
            characterName = name,
            assignedVoiceName = assignedVoice,
            tier = determineTier(lines),
            lineCount = lines,
            styleVector = generateDeterministicVector(assignedVoice)
        )
        activeCast.add(profile)
        return profile
    }

    fun assignVoice(characterId: String, newVoiceName: String) {
        val index = activeCast.indexOfFirst { it.characterId == characterId }
        if (index != -1) {
            val current = activeCast[index]
            activeCast[index] = current.copy(
                assignedVoiceName = newVoiceName,
                styleVector = generateDeterministicVector(newVoiceName)
            )
        }
    }

    // Smart Auto-Assign All: Distributes distinct non-overlapping voices across all characters
    fun autoAssignAllDistinct() {
        val pool = CloudTtsEngine.ALL_30_VOICES.shuffled(Random(42))
        for (i in 0 until activeCast.size) {
            val voice = pool[i % pool.size]
            val current = activeCast[i]
            activeCast[i] = current.copy(
                assignedVoiceName = voice,
                styleVector = generateDeterministicVector(voice)
            )
        }
    }

    // Bulk action for background extras (< 3 lines): maps them to a single selected crowd voice
    fun bulkAssignExtras(targetVoice: String = "Puck") {
        for (i in 0 until activeCast.size) {
            val current = activeCast[i]
            if (current.tier == CharacterTier.EXTRAS) {
                activeCast[i] = current.copy(
                    assignedVoiceName = targetVoice,
                    styleVector = generateDeterministicVector(targetVoice)
                )
            }
        }
    }

    // Mathematical Style Vector Blending: V_blended = (1 - alpha) * V_A + alpha * V_B
    fun blendVectors(voiceA: String, voiceB: String, alpha: Float): FloatArray {
        val vecA = generateDeterministicVector(voiceA)
        val vecB = generateDeterministicVector(voiceB)
        val blended = FloatArray(VECTOR_DIM)
        val weightB = alpha.coerceIn(0f, 1f)
        val weightA = 1f - weightB

        for (i in 0 until VECTOR_DIM) {
            blended[i] = (vecA[i] * weightA) + (vecB[i] * weightB)
        }
        return blended
    }

    fun getVoiceForCharacter(characterTag: String): String {
        val clean = characterTag.trim().removeSurrounding("[", "]")
        val match = activeCast.find { it.characterName.equals(clean, ignoreCase = true) }
        return match?.assignedVoiceName ?: "Charon"
    }

    private fun determineTier(lines: Int): CharacterTier {
        return when {
            lines > 5 -> CharacterTier.MAIN_CAST
            lines in 2..5 -> CharacterTier.SUPPORTING
            else -> CharacterTier.EXTRAS
        }
    }

    // Creates a unique, reproducible 512-dim fingerprint for each voice name (2 KB footprint)
    fun generateDeterministicVector(seedString: String): FloatArray {
        val rng = Random(seedString.hashCode())
        val vector = FloatArray(VECTOR_DIM)
        var sumSquares = 0.0

        for (i in 0 until VECTOR_DIM) {
            val v = (rng.nextFloat() * 2.0f) - 1.0f
            vector[i] = v
            sumSquares += v * v
        }

        // L2 Normalize vector
        val norm = Math.sqrt(sumSquares).toFloat().coerceAtLeast(1e-6f)
        for (i in 0 until VECTOR_DIM) {
            vector[i] /= norm
        }

        return vector
    }
}
