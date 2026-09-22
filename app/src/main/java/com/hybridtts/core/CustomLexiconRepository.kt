package com.hybridtts.core

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

data class LexiconEntry(
    val id: Long = 0L,
    val originalWord: String,
    val phoneticReplacement: String,
    val ipaNotation: String = "",
    val scope: String = "GLOBAL", // GLOBAL or PROJECT
    val isEnabled: Boolean = true
)

class CustomLexiconRepository private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "hybridtts_custom_lexicon.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_LEXICON = "lexicon_entries"

        private const val COL_ID = "id"
        private const val COL_WORD = "original_word"
        private const val COL_REPLACEMENT = "phonetic_replacement"
        private const val COL_IPA = "ipa_notation"
        private const val COL_SCOPE = "scope"
        private const val COL_ENABLED = "is_enabled"

        @Volatile
        private var instance: CustomLexiconRepository? = null

        fun getInstance(context: Context): CustomLexiconRepository {
            return instance ?: synchronized(this) {
                instance ?: CustomLexiconRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableSql = """
            CREATE TABLE $TABLE_LEXICON (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_WORD TEXT UNIQUE NOT NULL,
                $COL_REPLACEMENT TEXT NOT NULL,
                $COL_IPA TEXT,
                $COL_SCOPE TEXT DEFAULT 'GLOBAL',
                $COL_ENABLED INTEGER DEFAULT 1
            )
        """.trimIndent()
        db.execSQL(createTableSql)

        // Seed common default technical pronunciations
        insertDefaultSeed(db, "ASUS", "ay-soos", "/ˈeɪ.suːs/")
        insertDefaultSeed(db, "SQL", "sequel", "/ˈsiː.kwəl/")
        insertDefaultSeed(db, "Hermione", "her-my-oh-nee", "/hɜːrˈmaɪ.ə.ni/")
        insertDefaultSeed(db, "GIF", "jif", "/dʒɪf/")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LEXICON")
        onCreate(db)
    }

    private fun insertDefaultSeed(db: SQLiteDatabase, word: String, replacement: String, ipa: String) {
        val values = ContentValues().apply {
            put(COL_WORD, word)
            put(COL_REPLACEMENT, replacement)
            put(COL_IPA, ipa)
            put(COL_SCOPE, "GLOBAL")
            put(COL_ENABLED, 1)
        }
        db.insertWithOnConflict(TABLE_LEXICON, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun getAllEntries(): List<LexiconEntry> {
        val list = mutableListOf<LexiconEntry>()
        val db = readableDatabase
        val cursor = db.query(TABLE_LEXICON, null, null, null, null, null, "$COL_WORD ASC")

        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    LexiconEntry(
                        id = it.getLong(it.getColumnIndexOrThrow(COL_ID)),
                        originalWord = it.getString(it.getColumnIndexOrThrow(COL_WORD)),
                        phoneticReplacement = it.getString(it.getColumnIndexOrThrow(COL_REPLACEMENT)),
                        ipaNotation = it.getString(it.getColumnIndexOrThrow(COL_IPA)) ?: "",
                        scope = it.getString(it.getColumnIndexOrThrow(COL_SCOPE)) ?: "GLOBAL",
                        isEnabled = it.getInt(it.getColumnIndexOrThrow(COL_ENABLED)) == 1
                    )
                )
            }
        }
        return list
    }

    fun addOrUpdateEntry(entry: LexiconEntry): Boolean {
        if (entry.originalWord.isBlank() || entry.phoneticReplacement.isBlank()) return false

        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_WORD, entry.originalWord.trim())
            put(COL_REPLACEMENT, entry.phoneticReplacement.trim())
            put(COL_IPA, entry.ipaNotation.trim())
            put(COL_SCOPE, entry.scope)
            put(COL_ENABLED, if (entry.isEnabled) 1 else 0)
        }

        val result = db.insertWithOnConflict(
            TABLE_LEXICON,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        return result != -1L
    }

    fun deleteEntry(id: Long): Boolean {
        val db = writableDatabase
        return db.delete(TABLE_LEXICON, "$COL_ID = ?", arrayOf(id.toString())) > 0
    }

    // Intercepts words in raw text and replaces them before synthesis
    fun applyLexiconOverrides(rawText: String): String {
        var processed = rawText
        val activeEntries = getAllEntries().filter { it.isEnabled }

        for (entry in activeEntries) {
            // Regex word boundary matching (case-insensitive)
            val regex = Regex("\\b(?i)${Regex.escape(entry.originalWord)}\\b")
            processed = regex.replace(processed, entry.phoneticReplacement)
        }
        return processed
    }

    fun exportToJson(): String {
        val entries = getAllEntries()
        val array = JSONArray()
        for (e in entries) {
            val obj = JSONObject().apply {
                put("word", e.originalWord)
                put("replacement", e.phoneticReplacement)
                put("ipa", e.ipaNotation)
                put("scope", e.scope)
                put("enabled", e.isEnabled)
            }
            array.put(obj)
        }
        return array.toString(2)
    }

    fun importFromJson(jsonString: String): Int {
        return try {
            val array = JSONArray(jsonString)
            var count = 0
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val entry = LexiconEntry(
                    originalWord = obj.getString("word"),
                    phoneticReplacement = obj.getString("replacement"),
                    ipaNotation = obj.optString("ipa", ""),
                    scope = obj.optString("scope", "GLOBAL"),
                    isEnabled = obj.optBoolean("enabled", true)
                )
                if (addOrUpdateEntry(entry)) count++
            }
            count
        } catch (_: Exception) {
            0
        }
    }
}
