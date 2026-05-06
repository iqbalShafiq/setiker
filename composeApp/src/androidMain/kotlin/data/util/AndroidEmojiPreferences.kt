package data.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

actual class EmojiPreferences(private val context: Context) {

    private val prefs: SharedPreferences
        get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    actual suspend fun getRecentEmojis(): List<String> = withContext(Dispatchers.IO) {
        val json = prefs.getString(KEY_RECENT_EMOJIS, "[]") ?: "[]"
        Json.decodeFromString(json)
    }

    actual suspend fun addRecentEmoji(emoji: String) = withContext(Dispatchers.IO) {
        val current = getRecentEmojis().toMutableList()
        current.remove(emoji)
        current.add(0, emoji)
        val limited = current.take(30)
        prefs.edit().putString(KEY_RECENT_EMOJIS, Json.encodeToString(limited)).apply()
    }

    companion object {
        private const val PREFS_NAME = "emoji_preferences"
        private const val KEY_RECENT_EMOJIS = "recent_emojis"
    }
}