package com.hybridtts.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ModelRegistryManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_REGISTRY, Context.MODE_PRIVATE)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val PREFS_REGISTRY = "hybrid_tts_model_registry"
        private const val KEY_ACTIVE_MODEL = "key_active_model"
        private const val KEY_CUSTOM_OVERRIDE = "key_custom_override"

        // Official Google AI Studio Text-to-Speech Preview Models
        const val MODEL_GEMINI_3_1_FLASH_TTS = "gemini-3.1-flash-tts-preview"
        const val MODEL_GEMINI_2_5_FLASH_TTS = "gemini-2.5-flash-preview-tts"
        const val MODEL_GEMINI_2_5_PRO_TTS = "gemini-2.5-pro-preview-tts"

        val SUPPORTED_TTS_MODELS = listOf(
            MODEL_GEMINI_3_1_FLASH_TTS,
            MODEL_GEMINI_2_5_FLASH_TTS,
            MODEL_GEMINI_2_5_PRO_TTS
        )

        @Volatile
        private var instance: ModelRegistryManager? = null

        fun getInstance(context: Context): ModelRegistryManager {
            return instance ?: synchronized(this) {
                instance ?: ModelRegistryManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun getActiveModel(): String {
        val custom = prefs.getString(KEY_CUSTOM_OVERRIDE, "") ?: ""
        if (custom.isNotBlank()) return custom.trim()
        return prefs.getString(KEY_ACTIVE_MODEL, MODEL_GEMINI_3_1_FLASH_TTS) ?: MODEL_GEMINI_3_1_FLASH_TTS
    }

    fun setActiveModel(modelId: String) {
        prefs.edit().putString(KEY_ACTIVE_MODEL, modelId.trim()).apply()
    }

    fun setCustomModelOverride(customId: String) {
        prefs.edit().putString(KEY_CUSTOM_OVERRIDE, customId.trim()).apply()
    }

    fun clearCustomModelOverride() {
        prefs.edit().remove(KEY_CUSTOM_OVERRIDE).apply()
    }

    suspend fun testConnection(apiKey: String, modelId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId?key=$apiKey"
                val request = Request.Builder().url(url).get().build()
                val response = httpClient.newCall(request).execute()
                val latency = System.currentTimeMillis() - startTime

                if (response.isSuccessful) {
                    Result.success("HTTP 200 OK • Model Ready (${latency}ms)")
                } else {
                    val code = response.code
                    val errorBody = response.body?.string() ?: ""
                    Result.failure(Exception("Error $code: $errorBody"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
