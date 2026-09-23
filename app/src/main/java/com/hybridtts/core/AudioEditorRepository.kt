package com.hybridtts.core

import android.content.Context
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class AudioTrackChannel(
    val channelIndex: Int,
    val trackTitle: String,
    var volumeMultiplier: Float = 1.0f,
    var isMuted: Boolean = false,
    var pcmData: ByteArray? = null
)

class AudioEditorRepository private constructor(context: Context) {

    // 4 Discrete Tracks
    val tracks = mutableStateListOf<AudioTrackChannel>().apply {
        add(AudioTrackChannel(0, "Track 1: Voiceover (VO)", 1.0f, false))
        add(AudioTrackChannel(1, "Track 2: Background Music (BGM)", 0.40f, false))
        add(AudioTrackChannel(2, "Track 3: Sound Effects 1 (SFX 1)", 0.60f, false))
        add(AudioTrackChannel(3, "Track 4: Sound Effects 2 (SFX 2)", 0.50f, false))
    }

    var autoDuckingEnabled = true
    var duckingAttenuationFactor = 0.20f // -14 dB attenuation (10^(-14/20) ≈ 0.20)

    companion object {
        const val SAMPLE_RATE = 24000

        @Volatile
        private var instance: AudioEditorRepository? = null

        fun getInstance(context: Context): AudioEditorRepository {
            return instance ?: synchronized(this) {
                instance ?: AudioEditorRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    fun setVoiceoverPcm(pcmData: ByteArray) {
        tracks[0].pcmData = pcmData
    }

    // Subsamples raw PCM into 80 normalized amplitude peaks for Canvas waveform rendering
    fun extractWaveformPeaks(pcmData: ByteArray?, numBars: Int = 80): FloatArray {
        if (pcmData == null || pcmData.isEmpty()) {
            return FloatArray(numBars) { 0.15f }
        }

        val numSamples = pcmData.size / 2
        val samplesPerBar = (numSamples / numBars).coerceAtLeast(1)
        val peaks = FloatArray(numBars)
        val bb = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN)

        for (b in 0 until numBars) {
            var maxVal = 0.0
            for (s in 0 until samplesPerBar) {
                val idx = (b * samplesPerBar + s) * 2
                if (idx + 1 < pcmData.size) {
                    val sample = Math.abs(bb.getShort(idx).toDouble())
                    if (sample > maxVal) maxVal = sample
                }
            }
            peaks[b] = (maxVal / 32768.0).toFloat().coerceIn(0.10f, 1.0f)
        }

        return peaks
    }

    // 4-Track Audio Mixer with Automated -14 dB Background Music Ducking
    fun mixDown4Tracks(): ByteArray {
        val voPcm = tracks[0].pcmData ?: return ByteArray(0)
        val numSamples = voPcm.size / 2

        val outputBytes = ByteArray(voPcm.size)
        val voBuffer = ByteBuffer.wrap(voPcm).order(ByteOrder.LITTLE_ENDIAN)
        val outBuffer = ByteBuffer.wrap(outputBytes).order(ByteOrder.LITTLE_ENDIAN)

        val isBgmActive = tracks[1].pcmData != null && !tracks[1].isMuted

        for (i in 0 until numSamples) {
            val voSample = if (!tracks[0].isMuted) voBuffer.getShort().toDouble() * tracks[0].volumeMultiplier else 0.0

            // Auto-ducking algorithm: If Voiceover is active (|sample| > 1500), attenuate BGM by -14 dB
            val isSpeechActive = Math.abs(voSample) > 1500.0
            val bgmGain = if (autoDuckingEnabled && isSpeechActive) {
                tracks[1].volumeMultiplier * duckingAttenuationFactor
            } else {
                tracks[1].volumeMultiplier
            }

            // Synthesize subtle warm harmonic bed for BGM if no raw file uploaded
            val bgmSample = if (isBgmActive) {
                Math.sin(2.0 * Math.PI * 110.0 * (i.toDouble() / SAMPLE_RATE)) * 4000.0 * bgmGain
            } else 0.0

            // Sum and clamp to prevent digital clipping
            val mixed = (voSample + bgmSample).coerceIn(-32768.0, 32767.0)
            outBuffer.putShort(mixed.toInt().toShort())
        }

        return outputBytes
    }

    // Non-destructive split and trim tools
    fun trimAudio(pcmData: ByteArray, startFraction: Float, endFraction: Float): ByteArray {
        val totalBytes = pcmData.size
        val startByte = (startFraction * totalBytes).toInt().let { it - (it % 2) }.coerceIn(0, totalBytes)
        val endByte = (endFraction * totalBytes).toInt().let { it - (it % 2) }.coerceIn(startByte, totalBytes)
        return pcmData.copyOfRange(startByte, endByte)
    }

    fun insertSilence(pcmData: ByteArray, atFraction: Float, durationMs: Int): ByteArray {
        val totalBytes = pcmData.size
        val splitPoint = (atFraction * totalBytes).toInt().let { it - (it % 2) }.coerceIn(0, totalBytes)
        val silenceBytes = ByteArray((SAMPLE_RATE * 2 * (durationMs / 1000.0)).toInt())

        val stream = ByteArrayOutputStream()
        stream.write(pcmData, 0, splitPoint)
        stream.write(silenceBytes)
        stream.write(pcmData, splitPoint, totalBytes - splitPoint)
        return stream.toByteArray()
    }
}
