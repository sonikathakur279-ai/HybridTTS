package com.hybridtts.core

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.PlaybackParams
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class SynthesisResult {
    data class Success(val sampleRate: Int, val durationSeconds: Double) : SynthesisResult()
    data class Error(val message: String, val isQuotaExhausted: Boolean = false) : SynthesisResult()
}

class CloudTtsEngine private constructor(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val keyPool = KeyPoolManager.getInstance(context)
    private val modelRegistry = ModelRegistryManager.getInstance(context)

    private var activeAudioTrack: AudioTrack? = null

    companion object {
        private const val SAMPLE_RATE_24K = 24000

        // Complete 30 Google AI Studio Voice Roster
        val ALL_30_VOICES = listOf(
            "Achernar", "Achird", "Algenib", "Algieba", "Alnilam",
            "Aoede", "Autonoe", "Callirrhoe", "Charon", "Despina",
            "Enceladus", "Erinome", "Fenrir", "Gacrux", "Iapetus",
            "Kore", "Laomedeia", "Leda", "Orus", "Puck",
            "Pulcherrima", "Rasalgethi", "Sadachbia", "Sadaltager", "Schedar",
            "Sulafat", "Umbriel", "Vindemiatrix", "Zephyr", "Zubenelgenubi"
        )

        @Volatile
        private var instance: CloudTtsEngine? = null

        fun getInstance(context: Context): CloudTtsEngine {
            return instance ?: synchronized(this) {
                instance ?: CloudTtsEngine(context.applicationContext).also { instance = it }
            }
        }
    }

    suspend fun synthesizeAndPlay(
        text: String,
        voiceName: String = "Charon",
        audioProfile: String = "",
        styleNote: String = "Natural",
        paceNote: String = "Natural",
        accentNote: String = "Neutral",
        temperature: Float = 1.0f,
        speed: Float = 1.0f,
        pitchSemitones: Float = 0.0f
    ): SynthesisResult {
        return withContext(Dispatchers.IO) {
            val key = keyPool.getNextActiveKey()
                ?: return@withContext SynthesisResult.Error(
                    "No active Google AI Studio keys available. Add a key in Settings!"
                )

            val model = modelRegistry.getActiveModel()
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key"

            // Construct steerable context prompt based on Google AI Studio's Director's Note
            val contextDirectives = StringBuilder()
            if (audioProfile.isNotBlank()) {
                contextDirectives.append("Audio Profile: ${audioProfile.trim()}\n")
            }
            if (styleNote != "Natural") {
                contextDirectives.append("Director's Note Style: $styleNote. ")
            }
            if (paceNote != "Natural") {
                contextDirectives.append("Pace: $paceNote. ")
            }
            if (accentNote != "Neutral") {
                contextDirectives.append("Accent: $accentNote. ")
            }
            if (contextDirectives.isNotBlank()) {
                contextDirectives.append("\n\nDeliver the following text according to the directions above:\n")
            }

            val finalPromptText = "${contextDirectives}$text"

            // Construct JSON audio payload
            val jsonPayload = JSONObject().apply {
                val contents = JSONArray().apply {
                    val message = JSONObject().apply {
                        put("role", "user")
                        val parts = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", finalPromptText)
                            })
                        }
                        put("parts", parts)
                    }
                    put(message)
                }
                put("contents", contents)

                val generationConfig = JSONObject().apply {
                    put("temperature", temperature.toDouble())
                    val modalities = JSONArray().apply {
                        put("AUDIO")
                    }
                    put("responseModalities", modalities)

                    val speechConfig = JSONObject().apply {
                        val voiceConfig = JSONObject().apply {
                            val prebuiltVoiceConfig = JSONObject().apply {
                                put("voiceName", voiceName)
                            }
                            put("prebuiltVoiceConfig", prebuiltVoiceConfig)
                        }
                        put("voiceConfig", voiceConfig)
                    }
                    put("speechConfig", speechConfig)
                }
                put("generationConfig", generationConfig)
            }

            val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            try {
                val response = httpClient.newCall(request).execute()

                // Intercept HTTP 429 Quota Rate Limits
                if (response.code == 429) {
                    keyPool.markCooldown(key)
                    return@withContext SynthesisResult.Error(
                        "Rate limit reached on key (${key.take(4)}...). Key moved to 24h cooldown. Retrying next key...",
                        isQuotaExhausted = true
                    )
                }

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    return@withContext SynthesisResult.Error("API Error ${response.code}: $errorBody")
                }

                val responseString = response.body?.string() ?: ""
                val rootJson = JSONObject(responseString)

                val candidates = rootJson.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext SynthesisResult.Error("No speech audio returned by cloud model.")
                }

                val content = candidates.getJSONObject(0).optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                var base64Data: String? = null

                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        val inlineData = part.optJSONObject("inlineData")
                        if (inlineData != null) {
                            base64Data = inlineData.optString("data", null)
                            if (base64Data != null) break
                        }
                    }
                }

                if (base64Data == null) {
                    return@withContext SynthesisResult.Error("Audio payload missing in response structure.")
                }

                val rawAudioBytes = Base64.decode(base64Data, Base64.DEFAULT)

                // Skip 44-byte WAV/RIFF header if present to extract pure linear PCM
                val pcmData = if (rawAudioBytes.size > 44 &&
                    rawAudioBytes[0] == 'R'.code.toByte() &&
                    rawAudioBytes[1] == 'I'.code.toByte() &&
                    rawAudioBytes[2] == 'F'.code.toByte() &&
                    rawAudioBytes[3] == 'F'.code.toByte()
                ) {
                    rawAudioBytes.copyOfRange(44, rawAudioBytes.size)
                } else {
                    rawAudioBytes
                }

                playPcmStream(pcmData, SAMPLE_RATE_24K, speed, pitchSemitones)

                val duration = pcmData.size.toDouble() / (SAMPLE_RATE_24K * 2) // 16-bit mono = 2 bytes/sample
                SynthesisResult.Success(SAMPLE_RATE_24K, duration)

            } catch (e: Exception) {
                SynthesisResult.Error("Connection failure: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    private fun playPcmStream(
        pcmData: ByteArray,
        sampleRate: Int,
        speed: Float,
        pitchSemitones: Float
    ) {
        stopPlayback()

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val bufferSize = maxOf(minBufferSize, pcmData.size)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(pcmData, 0, pcmData.size)

        try {
            val params = PlaybackParams()
            params.speed = speed.coerceIn(0.5f, 2.0f)
            val pitchMultiplier = Math.pow(2.0, pitchSemitones.toDouble() / 12.0).toFloat()
            params.pitch = pitchMultiplier.coerceIn(0.5f, 2.0f)
            track.playbackParams = params
        } catch (_: Exception) {}

        track.play()
        activeAudioTrack = track
    }

    fun stopPlayback() {
        try {
            activeAudioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
        } catch (_: Exception) {}
        activeAudioTrack = null
    }

    fun isPlaying(): Boolean {
        return try {
            activeAudioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING
        } catch (_: Exception) {
            false
        }
    }
}
