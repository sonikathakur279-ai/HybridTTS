package com.hybridtts.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class HybridDirectorEngine private constructor(private val context: Context) {

    private val voiceDirector = GeminiVoiceDirector.getInstance(context)
    private val cloudTtsEngine = CloudTtsEngine.getInstance(context)
    private val notificationManager = TtsNotificationManager.getInstance(context)

    companion object {
        @Volatile
        private var instance: HybridDirectorEngine? = null

        fun getInstance(context: Context): HybridDirectorEngine {
            return instance ?: synchronized(this) {
                instance ?: HybridDirectorEngine(context.applicationContext).also { instance = it }
            }
        }
    }

    suspend fun executeHybridProduction(
        script: String,
        voiceName: String,
        audioProfile: String,
        styleNote: String,
        temperature: Float,
        onStatusUpdate: (String) -> Unit
    ): SynthesisResult {
        return withContext(Dispatchers.IO) {
            onStatusUpdate("Gemini Voice Director: Analyzing phonetics & prosody...")

            val directorResult = voiceDirector.analyzeAndDirectScript(
                rawText = script,
                targetPersona = audioProfile,
                directorStyle = styleNote
            )

            if (directorResult.isFailure) {
                val err = directorResult.exceptionOrNull()?.localizedMessage ?: "Unknown director error"
                return@withContext SynthesisResult.Error("Director analysis failed: $err")
            }

            val analysis = directorResult.getOrNull()!!
            val segments = analysis.segments

            if (segments.isEmpty()) {
                return@withContext SynthesisResult.Error("Director returned zero prosody segments.")
            }

            onStatusUpdate("Director: ${analysis.detectedNarrativeTone} (${segments.size} segments). Rendering audio...")

            val compositePcmStream = ByteArrayOutputStream()

            // Synthesize and stitch each segment with micro-pause breaks
            for ((index, segment) in segments.withIndex()) {
                onStatusUpdate("Synthesizing segment ${index + 1}/${segments.size}: \"${segment.segmentText.take(24)}...\"")

                val synthResult = cloudTtsEngine.synthesizeMaster(
                    text = segment.segmentText,
                    voiceName = voiceName,
                    audioProfile = audioProfile,
                    styleNote = segment.detectedEmotion,
                    paceNote = String.format(java.util.Locale.US, "%.2fx", segment.speedRate),
                    accentNote = "Neutral",
                    temperature = temperature
                )

                when (synthResult) {
                    is SynthesisResult.Success -> {
                        val segmentPcm = cloudTtsEngine.cachedPcmData
                        if (segmentPcm != null) {
                            compositePcmStream.write(segmentPcm)

                            // Insert zero-crossing silence buffer for micro-pause
                            if (segment.pauseAfterMs > 0) {
                                val silenceBuffer = generateSilencePcm(
                                    durationMs = segment.pauseAfterMs,
                                    sampleRate = CloudTtsEngine.SAMPLE_RATE_24K
                                )
                                compositePcmStream.write(silenceBuffer)
                            }
                        }
                    }
                    is SynthesisResult.Error -> {
                        return@withContext SynthesisResult.Error("Segment ${index + 1} synthesis failed: ${synthResult.message}")
                    }
                }
            }

            val stitchedPcm = compositePcmStream.toByteArray()
            if (stitchedPcm.isEmpty()) {
                return@withContext SynthesisResult.Error("Failed to assemble composite PCM audio stream.")
            }

            // Push final master into playback engine cache cleanly
            cloudTtsEngine.loadExternalMasterAudio(stitchedPcm)

            val durationSec = cloudTtsEngine.cachedDurationSeconds
            onStatusUpdate("Hybrid Master Assembled (Duration: ${String.format(java.util.Locale.US, "%.1fs", durationSec)})")

            // Post system status bar notification
            notificationManager.notifySynthesisComplete(voiceName, durationSec)

            SynthesisResult.Success(
                sampleRate = CloudTtsEngine.SAMPLE_RATE_24K,
                durationSeconds = durationSec
            )
        }
    }

    private fun generateSilencePcm(durationMs: Int, sampleRate: Int): ByteArray {
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val totalBytes = totalSamples * 2
        return ByteArray(totalBytes)
    }
}
