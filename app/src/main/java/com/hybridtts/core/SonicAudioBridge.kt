package com.hybridtts.core

import android.media.AudioTrack
import android.media.PlaybackParams

data class TimeWarpResult(
    val originalTargetSeconds: Double,
    val synthesizedSeconds: Double,
    val finalWarpedSeconds: Double,
    val speedMultiplier: Float,
    val deltaMilliseconds: Long,
    val isTimelineLocked: Boolean
)

object SonicAudioBridge {

    const val MIN_NATURAL_SPEED = 0.85f
    const val MAX_NATURAL_SPEED = 1.15f
    const val ACCEPTABLE_DRIFT_MS = 5L // Strict 5ms tolerance

    // Calculates the exact speed adjustment needed to match video timestamps
    fun calculateWarpParams(synthesizedSeconds: Double, targetSeconds: Double): TimeWarpResult {
        if (targetSeconds <= 0.0 || synthesizedSeconds <= 0.0) {
            return TimeWarpResult(targetSeconds, synthesizedSeconds, synthesizedSeconds, 1.0f, 0L, true)
        }

        val rawRatio = (synthesizedSeconds / targetSeconds).toFloat()
        // Clamp to natural human vocal inflection range (0.85x to 1.15x)
        val clampedSpeed = rawRatio.coerceIn(MIN_NATURAL_SPEED, MAX_NATURAL_SPEED)

        val finalDuration = synthesizedSeconds / clampedSpeed
        val deltaMs = Math.abs((finalDuration - targetSeconds) * 1000.0).toLong()

        return TimeWarpResult(
            originalTargetSeconds = targetSeconds,
            synthesizedSeconds = synthesizedSeconds,
            finalWarpedSeconds = finalDuration,
            speedMultiplier = clampedSpeed,
            deltaMilliseconds = deltaMs,
            isTimelineLocked = deltaMs <= ACCEPTABLE_DRIFT_MS
        )
    }

    // Applies the calculated micro-warp speed to an active AudioTrack without altering pitch
    fun applyWarpToTrack(track: AudioTrack?, speed: Float) {
        if (track == null) return
        try {
            val params = PlaybackParams()
            params.speed = speed.coerceIn(MIN_NATURAL_SPEED, MAX_NATURAL_SPEED)
            params.pitch = 1.0f // Strict zero-pitch distortion lock
            track.playbackParams = params
        } catch (_: Exception) {}
    }
}
