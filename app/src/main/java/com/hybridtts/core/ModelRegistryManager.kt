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

        // Default stable models
        const val DEFAULT_MODEL = "gemini-2.0-flash"
        const val FALLBACK_MODEL = "gemini-1.5-flash"

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
        return prefs.getString(KEY_ACTIVE_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
    }

    fun setCustomModelOverride(customId: String) {
        prefs.edit().putString(KEY_CUSTOM_OVERRIDE, customId.trim()).apply()
    }

    fun clearCustomModelOverride() {
        prefs.edit().remove(KEY_CUSTOM_OVERRIDE).apply()
    }

    suspend fun discoverAvailableModels(apiKey: String): List<String> {
        return withContext(Dispatchers.IO) {
            val discovered = mutableListOf<String>()
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
                val request = Request.Builder().url(url).get().build()
                val response = httpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    val json = JSONObject(bodyString)
                    val modelsArray = json.optJSONArray("models")

                    if (modelsArray != null) {
                        for (i in 0 until modelsArray.length()) {
                            val modelObj = modelsArray.getJSONObject(i)
                            val name = modelObj.optString("name", "")
                            // Name format is "models/gemini-2.0-flash"
                            val cleanName = name.removePrefix("models/")
                            val supportedMethods = modelObj.optJSONArray("supportedGenerationMethods")
                            var canGenerate = false
                            if (supportedMethods != null) {
                                for (j in 0 until supportedMethods.length()) {
                                    if (supportedMethods.getString(j) == "generateContent") {
                                        canGenerate = true
                                        break
                                    }
                                }
                            }
                            if (canGenerate && cleanName.contains("gemini", ignoreCase = true)) {
                                discovered.add(cleanName)
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Return default catalog if network discovery encounters an issue
            }

            if (discovered.isEmpty()) {
                discovered.add(DEFAULT_MODEL)
                discovered.add(FALLBACK_MODEL)
                discovered.add("gemini-2.5-flash-preview-tts")
            }

            discovered
        }
    }
}
