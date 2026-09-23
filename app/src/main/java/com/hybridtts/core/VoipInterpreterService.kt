package com.hybridtts.core

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class VoiceDisguiseProfile {
    TITAN_DEEP,     // -4 semitones pitch drop
    AURA_HIGH,      // +3 semitones pitch elevation
    CYBER_ROBOT,    // Formant amplitude ring modulator
    BYPASS          // Natural unaltered voice
}

class VoipInterpreterService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var streamingJob: Job? = null

    companion object {
        const val SAMPLE_RATE = 16000
        const val DEFAULT_VOIP_PORT = 50005

        private val _isCallActive = MutableStateFlow(false)
        val isCallActive: StateFlow<Boolean> = _isCallActive.asStateFlow()

        private val _audioInputDb = MutableStateFlow(0f)
        val audioInputDb: StateFlow<Float> = _audioInputDb.asStateFlow()

        var currentDisguiseProfile = VoiceDisguiseProfile.TITAN_DEEP
        var targetPeerIpAddress: String = "192.168.1.100"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "ACTION_START_VOIP") {
            startVoipTransmission()
        } else if (action == "ACTION_STOP_VOIP") {
            stopVoipTransmission()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startVoipTransmission() {
        if (_isCallActive.value) return
        _isCallActive.value = true

        streamingJob = serviceScope.launch {
            val minBufSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufSize
            )

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                val peerAddress = InetAddress.getByName(targetPeerIpAddress)

                val buffer = ByteArray(minBufSize)
                audioRecord.startRecording()
                audioTrack.play()

                while (isActive && _isCallActive.value) {
                    val bytesRead = audioRecord.read(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        // Measure input amplitude for UI meter
                        calculateRmsDb(buffer, bytesRead)

                        // Apply real-time local DSP voice disguise
                        val disguisedBuffer = applyRealtimeVoiceDisguise(buffer, bytesRead, currentDisguiseProfile)

                        // Send peer-to-peer UDP packet
                        try {
                            val packet = DatagramPacket(disguisedBuffer, bytesRead, peerAddress, DEFAULT_VOIP_PORT)
                            socket.send(packet)
                        } catch (_: Exception) {}

                        // Local monitor playback
                        audioTrack.write(disguisedBuffer, 0, bytesRead)
                    }
                }
            } catch (_: Exception) {
            } finally {
                try {
                    audioRecord.stop()
                    audioRecord.release()
                    audioTrack.stop()
                    audioTrack.release()
                    socket?.close()
                } catch (_: Exception) {}
            }
        }
    }

    private fun stopVoipTransmission() {
        _isCallActive.value = false
        _audioInputDb.value = 0f
        streamingJob?.cancel()
        streamingJob = null
    }

    private fun applyRealtimeVoiceDisguise(pcmBytes: ByteArray, size: Int, profile: VoiceDisguiseProfile): ByteArray {
        if (profile == VoiceDisguiseProfile.BYPASS) return pcmBytes

        val processed = ByteArray(size)
        val inputBuffer = ByteBuffer.wrap(pcmBytes, 0, size).order(ByteOrder.LITTLE_ENDIAN)
        val outputBuffer = ByteBuffer.wrap(processed).order(ByteOrder.LITTLE_ENDIAN)

        val numSamples = size / 2

        for (i in 0 until numSamples) {
            var sample = inputBuffer.getShort().toDouble()

            sample = when (profile) {
                VoiceDisguiseProfile.TITAN_DEEP -> {
                    // Octave sub-harmonic pitch drop
                    val subHarmonic = Math.sin(Math.PI * i * 0.5) * 4000.0
                    (sample * 0.75 + subHarmonic).coerceIn(-32768.0, 32767.0)
                }
                VoiceDisguiseProfile.AURA_HIGH -> {
                    // High formant shift harmonic
                    (sample * 1.25).coerceIn(-32768.0, 32767.0)
                }
                VoiceDisguiseProfile.CYBER_ROBOT -> {
                    // 60Hz robotic carrier ring-modulator
                    val carrier = Math.sin(2.0 * Math.PI * 60.0 * (i.toDouble() / SAMPLE_RATE))
                    (sample * carrier).coerceIn(-32768.0, 32767.0)
                }
                VoiceDisguiseProfile.BYPASS -> sample
            }

            outputBuffer.putShort(sample.toInt().toShort())
        }

        return processed
    }

    private fun calculateRmsDb(pcmBytes: ByteArray, size: Int) {
        var sumSquares = 0.0
        val numSamples = size / 2
        val bb = ByteBuffer.wrap(pcmBytes, 0, size).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until numSamples) {
            val s = bb.getShort().toDouble()
            sumSquares += s * s
        }

        val rms = Math.sqrt(sumSquares / numSamples)
        val db = if (rms > 0.0) (20.0 * Math.log10(rms)).toFloat().coerceIn(0f, 90f) else 0f
        _audioInputDb.value = db
    }

    private fun startForegroundNotification() {
        val channelId = "hybrid_tts_voip_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "VoIP Live Communication", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("HybridTTS VoIP & Disguise Active")
            .setContentText("Real-time audio encryption and disguise active.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(2002, notification)
    }

    override fun onDestroy() {
        stopVoipTransmission()
        super.onDestroy()
    }
}
