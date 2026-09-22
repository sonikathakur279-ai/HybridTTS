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

data class ProsodySegment(
    val segmentText: String,
    val ipaNotation: String,
    val speedRate: Float,
    val pitchSemitones: Float,
    val pauseAfterMs: Int,
    val detectedEmotion: String
)

data class DirectorAnalysisResult(
    val detectedNarrativeTone: String,
    val homographsResolved: List<String>,
    val segments: List<ProsodySegment>,
    val fullMarkupPreview: String
)

class GeminiVoiceDirector private constructor(context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val keyPool = KeyPoolManager.getInstance(context)

    companion object {
        // High-speed text endpoint (~1,500 requests/day per key on the free tier)
        private const val DIRECTOR_MODEL = "gemini-1.5-flash"

        @Volatile
        private var instance: GeminiVoiceDirector? = null

        fun getInstance(context: Context): GeminiVoiceDirector {
            return instance ?: synchronized(this) {
                instance ?: GeminiVoiceDirector(context.applicationContext).also { instance = it }
            }
        }
    }

    suspend fun analyzeAndDirectScript(
        rawText: String,
        targetPersona: String,
        directorStyle: String
    ): Result<DirectorAnalysisResult> {
        return withContext(Dispatchers.IO) {
            val key = keyPool.getNextActiveKey()
                ?: return@withContext Result.failure(
                    Exception("No active API keys found. Please add a key in Settings.")
                )

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$DIRECTOR_MODEL:generateContent?key=$key"

            val systemInstruction = """
                You are an expert Acoustic Voice Director and Phonetics Engineer.
                Analyze the input text for expressive speech synthesis.
                Persona: $targetPersona
                Style: $directorStyle

                Tasks:
                1. Resolve any ambiguous homographs (e.g., read, wound, tear, lead) using phonetic context.
                2. Divide the text into natural prosody segments.
                3. For each segment, output:
                   - speed_rate: 0.85 to 1.25 (1.0 = normal)
                   - pitch_semitones: -3.0 to +3.0 (0.0 = normal)
                   - pause_after_ms: 100 to 600 ms (based on narrative punctuation and breath points)
                   - detected_emotion: word describing the emotion
                   - ipa_notation: phonetic notation for complex or homograph words
                4. Output strictly valid JSON matching this structure:
                {
                  "narrative_tone": "Suspenseful and deliberate",
                  "homographs_resolved": ["wound -> /wuːnd/ (injury)"],
                  "segments": [
                    {
                      "text": "The wind howled outside,",
                      "ipa": "ðə wɪnd haʊld aʊtˈsaɪd",
                      "speed": 0.95,
                      "pitch": -0.5,
                      "pause_ms": 250,
                      "emotion": "eerie"
                    }
                  ]
                }
            """.trimIndent()

            val jsonPayload = JSONObject().apply {
                val contents = JSONArray().apply {
                    val msg = JSONObject().apply {
                        put("role", "user")
                        val parts = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "$systemInstruction\n\nINPUT SCRIPT:\n\"$rawText\"")
                            })
                        }
                        put("parts", parts)
                    }
                    put(msg)
                }
                put("contents", contents)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                }
                put("generationConfig", generationConfig)
            }

            val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()

            try {
                val response = httpClient.newCall(request).execute()

                if (response.code == 429) {
                    keyPool.markCooldown(key)
                    return@withContext Result.failure(
                        Exception("Gemini Text rate limit reached. Key rotated to cooldown.")
                    )
                }

                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: ""
                    return@withContext Result.failure(Exception("Director API Error ${response.code}: $err"))
                }

                val responseBody = response.body?.string() ?: ""
                val rootJson = JSONObject(responseBody)
                val candidates = rootJson.optJSONArray("candidates")

                if (candidates == null || candidates.length() == 0) {
                    return@withContext Result.failure(Exception("Empty response from AI Voice Director."))
                }

                val textContent = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                val parsedJson = JSONObject(textContent)
                val tone = parsedJson.optString("narrative_tone", "Neutral Expressive")

                val homographsArray = parsedJson.optJSONArray("homographs_resolved")
                val homographs = mutableListOf<String>()
                if (homographsArray != null) {
                    for (i in 0 until homographsArray.length()) {
                        homographs.add(homographsArray.getString(i))
                    }
                }

                val segmentsArray = parsedJson.optJSONArray("segments")
                val segments = mutableListOf<ProsodySegment>()
                val markupBuilder = StringBuilder()

                if (segmentsArray != null) {
                    for (i in 0 until segmentsArray.length()) {
                        val seg = segmentsArray.getJSONObject(i)
                        val segText = seg.optString("text", "")
                        val ipa = seg.optString("ipa", "")
                        val spd = seg.optDouble("speed", 1.0).toFloat()
                        val pit = seg.optDouble("pitch", 0.0).toFloat()
                        val pause = seg.optInt("pause_ms", 200)
                        val emo = seg.optString("emotion", "neutral")

                        segments.add(
                            ProsodySegment(
                                segmentText = segText,
                                ipaNotation = ipa,
                                speedRate = spd,
                                pitchSemitones = pit,
                                pauseAfterMs = pause,
                                detectedEmotion = emo
                            )
                        )

                        markupBuilder.append("<prosody pitch=\"${String.format(java.util.Locale.US, "%+.1f", pit)}st\" rate=\"${String.format(java.util.Locale.US, "%.2f", spd)}\">")
                        markupBuilder.append(segText)
                        markupBuilder.append("</prosody>")
                        if (pause > 0) {
                            markupBuilder.append("<break time=\"${pause}ms\"/> ")
                        }
                    }
                }

                Result.success(
                    DirectorAnalysisResult(
                        detectedNarrativeTone = tone,
                        homographsResolved = homographs,
                        segments = segments,
                        fullMarkupPreview = markupBuilder.toString()
                    )
                )

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
