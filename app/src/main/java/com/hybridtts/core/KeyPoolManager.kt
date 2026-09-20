package com.hybridtts.core

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

enum class KeyStatus {
    ACTIVE,
    COOLDOWN,
    DEPLETED
}

data class ApiKeyEntity(
    val key: String,
    val status: KeyStatus = KeyStatus.ACTIVE,
    val cooldownUntilMs: Long = 0L,
    val requestCount: Int = 0
) {
    val maskedKey: String
        get() = if (key.length > 8) {
            "${key.take(6)}...${key.takeLast(4)}"
        } else {
            "****"
        }
}

class KeyPoolManager private constructor(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILENAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback to mode private if hardware keystore is locked during device startup
        context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
    }

    private val lock = Any()
    private var lastUsedIndex = -1

    companion object {
        private const val PREFS_FILENAME = "hybrid_tts_vault_keys"
        private const val KEY_LIST_JSON = "key_list_json"

        @Volatile
        private var instance: KeyPoolManager? = null

        fun getInstance(context: Context): KeyPoolManager {
            return instance ?: synchronized(this) {
                instance ?: KeyPoolManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun getAllKeys(): List<ApiKeyEntity> {
        synchronized(lock) {
            val jsonString = prefs.getString(KEY_LIST_JSON, "[]") ?: "[]"
            val array = JSONArray(jsonString)
            val now = System.currentTimeMillis()
            val list = mutableListOf<ApiKeyEntity>()

            var hasUpdates = false

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val key = obj.getString("key")
                var status = KeyStatus.valueOf(obj.optString("status", KeyStatus.ACTIVE.name))
                val cooldown = obj.optLong("cooldownUntilMs", 0L)
                val count = obj.optInt("requestCount", 0)

                // Check if cooldown has expired
                if (status == KeyStatus.COOLDOWN && now >= cooldown) {
                    status = KeyStatus.ACTIVE
                    hasUpdates = true
                }

                list.add(ApiKeyEntity(key, status, cooldown, count))
            }

            if (hasUpdates) {
                saveKeys(list)
            }

            return list
        }
    }

    fun addKey(rawKey: String): Boolean {
        val trimmed = rawKey.trim()
        if (trimmed.isEmpty() || trimmed.length < 10) return false

        synchronized(lock) {
            val current = getAllKeys().toMutableList()
            if (current.any { it.key == trimmed }) return false

            current.add(ApiKeyEntity(key = trimmed, status = KeyStatus.ACTIVE))
            saveKeys(current)
            return true
        }
    }

    fun removeKey(keyToRemove: String) {
        synchronized(lock) {
            val current = getAllKeys().toMutableList()
            current.removeAll { it.key == keyToRemove }
            saveKeys(current)
        }
    }

    fun getNextActiveKey(): String? {
        synchronized(lock) {
            val keys = getAllKeys()
            val activeKeys = keys.filter { it.status == KeyStatus.ACTIVE }

            if (activeKeys.isEmpty()) {
                return null
            }

            lastUsedIndex = (lastUsedIndex + 1) % activeKeys.size
            val selected = activeKeys[lastUsedIndex]

            // Increment usage count
            val updated = keys.map {
                if (it.key == selected.key) {
                    it.copy(requestCount = it.requestCount + 1)
                } else it
            }
            saveKeys(updated)

            return selected.key
        }
    }

    fun markCooldown(rateLimitedKey: String) {
        synchronized(lock) {
            val keys = getAllKeys()
            val now = System.currentTimeMillis()
            // 24-hour reset cooldown: 24 * 60 * 60 * 1000
            val cooldownDuration = 24L * 60L * 60L * 1000L

            val updated = keys.map {
                if (it.key == rateLimitedKey) {
                    it.copy(status = KeyStatus.COOLDOWN, cooldownUntilMs = now + cooldownDuration)
                } else it
            }
            saveKeys(updated)
        }
    }

    private fun saveKeys(keys: List<ApiKeyEntity>) {
        val array = JSONArray()
        for (item in keys) {
            val obj = JSONObject()
            obj.put("key", item.key)
            obj.put("status", item.status.name)
            obj.put("cooldownUntilMs", item.cooldownUntilMs)
            obj.put("requestCount", item.requestCount)
            array.put(obj)
        }
        prefs.edit().putString(KEY_LIST_JSON, array.toString()).apply()
    }
}
