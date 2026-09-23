package com.hybridtts.core

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed class InterpreterState {
    object Idle : InterpreterState()
    data class Listening(val speakerTag: String) : InterpreterState()
    data class Translating(val recognizedText: String) : InterpreterState()
    data class Speaking(val translatedText: String) : InterpreterState()
    data class Error(val message: String) : InterpreterState()
}

class LiveSpeechInterpreterManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val ttsEngine = CloudTtsEngine.getInstance(context)
    private val keyPool = KeyPoolManager.getInstance(context)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private var speechRecognizer: SpeechRecognizer? = null

    private val _interpreterState = MutableStateFlow<InterpreterState>(InterpreterState.Idle)
    val interpreterState: StateFlow<InterpreterState> = _interpreterState.asStateFlow()

    companion object {
        val SUPPORTED_LANGUAGES = listOf(
            "English (US)", "Hindi (IN)", "Spanish (ES)", "French (FR)",
            "German (DE)", "Japanese (JP)", "Mandarin (ZH)", "Arabic (AR)",
            "Russian (RU)", "Portuguese (BR)", "Italian (IT)", "Korean (KR)"
        )

        @Volatile
        private var instance: LiveSpeechInterpreterManager? = null

        fun getInstance(context: Context): LiveSpeechInterpreterManager {
            return instance ?: synchronized(this) {
                instance ?: LiveSpeechInterpreterManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun startListening(
        speakerTag: String,
        sourceLanguage: String,
        targetLanguage: String,
        targetVoice: String,
        onRecognized: (String) -> Unit,
        onTranslated: (String) -> Unit
    ) {
        stopListening()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _interpreterState.value = InterpreterState.Error("Speech Recognition unavailable on this device.")
            return
        }

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, getLocaleTag(sourceLanguage))
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _interpreterState.value = InterpreterState.Listening(speakerTag)
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                _interpreterState.value = InterpreterState.Idle
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull() ?: ""

                if (recognizedText.isNotBlank()) {
                    onRecognized(recognizedText)
                    _interpreterState.value = InterpreterState.Translating(recognizedText)

                    scope.launch(Dispatchers.IO) {
                        val translatedText = translateText(recognizedText, sourceLanguage, targetLanguage)
                        scope.launch(Dispatchers.Main) {
                            onTranslated(translatedText)
                            _interpreterState.value = InterpreterState.Speaking(translatedText)

                            // Synthesize and speak in partner's language
                            ttsEngine.synthesizeMaster(
                                text = translatedText,
                                voiceName = targetVoice,
                                temperature = 1.0f
                            )
                            ttsEngine.playAudio()
                        }
                    }
                } else {
                    _interpreterState.value = InterpreterState.Idle
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        _interpreterState.value = InterpreterState.Idle
    }

    private suspend fun translateText(text: String, sourceLang: String, targetLang: String): String {
        val key = keyPool.getNextActiveKey() ?: return text

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$key"
        val prompt = "Translate naturally and conversationally from $sourceLang to $targetLang. Output ONLY the translated sentence:\n\"$text\""

        val json = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply { put("temperature", 0.3) })
        }

        return try {
            val req = Request.Builder().url(url).post(json.toString().toRequestBody("application/json".toMediaType())).build()
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: ""
                val root = JSONObject(body)
                root.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim()
                    .removeSurrounding("\"")
            } else {
                text
            }
        } catch (_: Exception) {
            text
        }
    }

    private fun getLocaleTag(languageName: String): String {
        return when {
            languageName.contains("Hindi") -> "hi-IN"
            languageName.contains("Spanish") -> "es-ES"
            languageName.contains("French") -> "fr-FR"
            languageName.contains("German") -> "de-DE"
            languageName.contains("Japanese") -> "ja-JP"
            languageName.contains("Mandarin") -> "zh-CN"
            languageName.contains("Arabic") -> "ar-SA"
            languageName.contains("Russian") -> "ru-RU"
            languageName.contains("Portuguese") -> "pt-BR"
            languageName.contains("Italian") -> "it-IT"
            languageName.contains("Korean") -> "ko-KR"
            else -> "en-US"
        }
    }
}
