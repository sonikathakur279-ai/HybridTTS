package com.hybridtts.core

import android.content.ContentValues
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed class SynthesisResult {
    data class Success(val sampleRate: Int, val durationSeconds: Double) : SynthesisResult()
    data class Error(val message: String, val isQuotaExhausted: Boolean = false) : SynthesisResult()
}

class CloudTtsEngine private constructor(private val context: Context) {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val keyPool = KeyPoolManager.getInstance(context)
    private val modelRegistry = ModelRegistryManager.getInstance(context)

    var cachedPcmData: ByteArray? = null
    var cachedDurationSeconds: Double = 0.0

    private var activeAudioTrack: AudioTrack? = null

    private val _isPlayingState = MutableStateFlow(false)
    val isPlayingState: StateFlow<Boolean> = _isPlayingState.asStateFlow()

    private val _playbackProgressFraction = MutableStateFlow(0f)
    val playbackProgressFraction: StateFlow<Float> = _playbackProgressFraction.asStateFlow()

    private val _isGeneratingState = MutableStateFlow(false)
    val isGeneratingState: StateFlow<Boolean> = _isGeneratingState.asStateFlow()

    companion object {
        const val SAMPLE_RATE_24K = 24000

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

    fun loadExternalMasterAudio(pcmData: ByteArray) {
        cachedPcmData = pcmData
        cachedDurationSeconds = pcmData.size.toDouble() / (SAMPLE_RATE_24K * 2)
        prepareAudioTrack(pcmData)
    }

    suspend fun synthesizeMaster(
        text: String,
        voiceName: String = "Charon",
        audioProfile: String = "",
        styleNote: String = "Natural",
        paceNote: String = "Natural",
        accentNote: String = "Neutral",
        temperature: Float = 1.0f
    ): SynthesisResult {
        return withContext(Dispatchers.IO) {
            _isGeneratingState.value = true

            val key = keyPool.getNextActiveKey()
            if (key == null) {
                _isGeneratingState.value = false
                return@withContext SynthesisResult.Error(
                    "No active Google AI Studio keys available. Add a key in Settings!"
                )
            }

            val model = modelRegistry.getActiveModel()
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key"

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

            // Constrain temperature to safe 0.1 to 1.2 range for speech models
            val safeTemp = (Math.round(temperature * 20.0f) / 20.0f).coerceIn(0.1f, 1.2f)

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
                    put("temperature", safeTemp.toDouble())
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
            val request = Request.Builder().url(url).post(requestBody).build()

            try {
                val response = httpClient.newCall(request).execute()

                if (response.code == 429) {
                    keyPool.markCooldown(key)
                    _isGeneratingState.value = false
                    return@withContext SynthesisResult.Error(
                        "Rate limit reached on key (${key.take(4)}...). Key moved to 24h cooldown.",
                        isQuotaExhausted = true
                    )
                }

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    _isGeneratingState.value = false
                    return@withContext SynthesisResult.Error("API Error ${response.code}: $errorBody")
                }

                val responseString = response.body?.string() ?: ""
                val rootJson = JSONObject(responseString)

                val candidates = rootJson.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    _isGeneratingState.value = false
                    return@withContext SynthesisResult.Error("No speech audio returned by cloud model.")
                }

                val content = candidates.getJSONObject(0).optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                var base64Data: String? = null
                var returnedTextReason: String? = null

                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        val inlineData = part.optJSONObject("inlineData")
                        if (inlineData != null) {
                            base64Data = inlineData.optString("data", null)
                            if (base64Data != null) break
                        }
                        if (part.has("text")) {
                            returnedTextReason = part.optString("text")
                        }
                    }
                }

                if (base64Data == null) {
                    _isGeneratingState.value = false
                    val detail = returnedTextReason ?: "Model output text instead of audio. Lower temperature to 1.00!"
                    return@withContext SynthesisResult.Error("Model Notice: $detail")
                }

                val rawAudioBytes = Base64.decode(base64Data, Base64.DEFAULT)

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

                cachedPcmData = pcmData
                cachedDurationSeconds = pcmData.size.toDouble() / (SAMPLE_RATE_24K * 2)

                prepareAudioTrack(pcmData)

                _isGeneratingState.value = false
                SynthesisResult.Success(SAMPLE_RATE_24K, cachedDurationSeconds)

            } catch (e: Exception) {
                _isGeneratingState.value = false
                SynthesisResult.Error("Connection failure: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun prepareAudioTrack(pcmData: ByteArray) {
        stopPlayback()

        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE_24K,
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
                    .setSampleRate(SAMPLE_RATE_24K)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(pcmData, 0, pcmData.size)
        activeAudioTrack = track
        _playbackProgressFraction.value = 0f
    }

    fun playAudio(speed: Float = 1.0f, pitchSemitones: Float = 0.0f) {
        val pcm = cachedPcmData ?: return

        if (activeAudioTrack == null) {
            prepareAudioTrack(pcm)
        }

        val track = activeAudioTrack ?: return

        try {
            val params = PlaybackParams()
            params.speed = speed.coerceIn(0.5f, 2.0f)
            val pitchMultiplier = Math.pow(2.0, pitchSemitones.toDouble() / 12.0).toFloat()
            params.pitch = pitchMultiplier.coerceIn(0.5f, 2.0f)
            track.playbackParams = params
        } catch (_: Exception) {}

        track.play()
        _isPlayingState.value = true

        startProgressTracker()
    }

    fun pauseAudio() {
        activeAudioTrack?.let {
            if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                it.pause()
            }
        }
        _isPlayingState.value = false
    }

    fun stopPlayback() {
        try {
            activeAudioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.pause()
                }
                it.playbackHeadPosition = 0
            }
        } catch (_: Exception) {}
        _isPlayingState.value = false
        _playbackProgressFraction.value = 0f
    }

    // MediaTek Helio G85 hardware seek: pause -> reposition head -> resume
    fun seekToFraction(fraction: Float, autoResume: Boolean = true) {
        val track = activeAudioTrack ?: return
        val pcm = cachedPcmData ?: return
        val totalFrames = pcm.size / 2
        val targetFrame = (fraction.coerceIn(0f, 1f) * totalFrames).toInt()

        val wasPlaying = _isPlayingState.value

        try {
            if (wasPlaying) {
                track.pause()
            }
            track.playbackHeadPosition = targetFrame
            _playbackProgressFraction.value = fraction.coerceIn(0f, 1f)

            if (wasPlaying && autoResume) {
                track.play()
                startProgressTracker()
            }
        } catch (_: Exception) {}
    }

    private fun startProgressTracker() {
        applicationScope.launch {
            val pcm = cachedPcmData ?: return@launch
            val totalFrames = pcm.size / 2

            while (_isPlayingState.value && activeAudioTrack != null) {
                val currentFrame = activeAudioTrack?.playbackHeadPosition ?: 0
                val progress = currentFrame.toFloat() / totalFrames.toFloat()

                _playbackProgressFraction.value = progress.coerceIn(0f, 1f)

                if (currentFrame >= totalFrames) {
                    _isPlayingState.value = false
                    _playbackProgressFraction.value = 0f
                    activeAudioTrack?.playbackHeadPosition = 0
                    break
                }
                delay(50)
            }
        }
    }

    fun saveAudioToDownloads(customName: String = ""): Result<String> {
        val pcm = cachedPcmData ?: return Result.failure(Exception("No generated audio to download."))

        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = if (customName.isNotBlank()) "${customName}_$timestamp.wav" else "HybridTTS_$timestamp.wav"

            val wavHeader = createWavHeader(pcm.size, SAMPLE_RATE_24K, 1, 16)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "audio/wav")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/HybridTTS")
                }

                val uri: Uri? = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out: OutputStream ->
                        out.write(wavHeader)
                        out.write(pcm)
                    }
                    Result.success("Saved to Downloads/HybridTTS/$fileName")
                } else {
                    Result.failure(Exception("Unable to create media store record."))
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "HybridTTS")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                FileOutputStream(file).use { out ->
                    out.write(wavHeader)
                    out.write(pcm)
                }
                Result.success("Saved to ${file.absolutePath}")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createWavHeader(pcmLength: Int, sampleRate: Int, channels: Short, bitsPerSample: Short): ByteArray {
        val totalDataLen = pcmLength + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(totalDataLen)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16)
        header.putShort(1)
        header.putShort(channels)
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort(blockAlign)
        header.putShort(bitsPerSample)
        header.put("data".toByteArray())
        header.putInt(pcmLength)

        return header.array()
    }
}
