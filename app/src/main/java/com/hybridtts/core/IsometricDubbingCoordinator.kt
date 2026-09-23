package com.hybridtts.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class DubbingCue(
    val cueId: String,
    val speakerTag: String,
    val startTimestampMs: Long,
    val endTimestampMs: Long,
    val sourceText: String,
    val sourceLang: String,
    val targetLang: String,
    var translatedText: String = "",
    var targetDurationSec: Double = 0.0,
    var synthesizedDurationSec: Double = 0.0,
    var warpResult: TimeWarpResult? = null
)

class IsometricDubbingCoordinator private constructor(context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val keyPool = KeyPoolManager.getInstance(context)
    private val ttsEngine = CloudTtsEngine.getInstance(context)

    companion object {
        @Volatile
        private var instance: IsometricDubbingCoordinator? = null

        fun getInstance(context: Context): IsometricDubbingCoordinator {
            return instance ?: synchronized(this) {
                instance ?: IsometricDubbingCoordinator(context.applicationContext).also { instance = it }
            }
        }
    }

    // STAGE 1: Syllable-Length Constrained Translation via Gemini Text API
    suspend fun executeStage1Translation(
        sourceText: String,
        sourceLang: String,
        targetLang: String,
        targetDurationSec: Double
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            val key = keyPool.getNextActiveKey()
                ?: return@withContext Result.failure(Exception("No active Google AI Studio keys available."))

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$key"

            // Estimate target syllable budget based on natural speech pace (~4.2 syllables/sec)
            val estimatedSyllables = (targetDurationSec * 4.2).toInt().coerceAtLeast(4)

            val prompt = """
                Translate the dialogue accurately from $sourceLang to $targetLang.
                CRITICAL CONSTRAINT: The translated text will be dubbed to match a video scene of exactly ${String.format(java.util.Locale.US, "%.2f", targetDurationSec)} seconds.
                The spoken syllable count MUST naturally fit a reading duration of ${String.format(java.util.Locale.US, "%.2f", targetDurationSec)}s (~$estimatedSyllables syllables).
                Keep the sentence natural, concise, and rhythmically matched to the original timing.
                Output ONLY the translation in $targetLang without notes or quotes.
                
                SOURCE TEXT: "$sourceText"
            """.trimIndent()

            val jsonPayload = JSONObject().apply {
                val contents = JSONArray().apply {
                    val msg = JSONObject().apply {
                        put("role", "user")
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", parts)
                    }
                    put(msg)
                }
                put("contents", contents)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.3)
                }
                put("generationConfig", generationConfig)
            }

            val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()

            try {
                val response = httpClient.newCall(request).execute()

                if (response.code == 429) {
                    keyPool.markCooldown(key)
                    return@withContext Result.failure(Exception("Rate limit reached on key. Rotated."))
                }

                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: ""
                    return@withContext Result.failure(Exception("API Error ${response.code}: $err"))
                }

                val body = response.body?.string() ?: ""
                val rootJson = JSONObject(body)
                val candidates = rootJson.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext Result.failure(Exception("Empty translation response from Gemini."))
                }

                val text = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim()
                    .removeSurrounding("\"")

                Result.success(text)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // STAGE 2: Speech Synthesis & Acoustic DSP Micro-Time Warping
    suspend fun executeStage2AcousticWarp(
        translatedText: String,
        assignedVoice: String,
        targetDurationSec: Double
    ): Result<TimeWarpResult> {
        return withContext(Dispatchers.IO) {
            val synthResult = ttsEngine.synthesizeMaster(
                text = translatedText,
                voiceName = assignedVoice,
                temperature = 1.0f
            )

            when (synthResult) {
                is SynthesisResult.Success -> {
                    val renderedDuration = synthResult.durationSeconds
                    val warpParams = SonicAudioBridge.calculateWarpParams(
                        synthesizedSeconds = renderedDuration,
                        targetSeconds = targetDurationSec
                    )
                    Result.success(warpParams)
                }
                is SynthesisResult.Error -> {
                    Result.failure(Exception(synthResult.message))
                }
            }
        }
    }
}
