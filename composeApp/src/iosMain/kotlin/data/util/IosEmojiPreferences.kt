package data.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

actual class EmojiPreferences {

    private val defaults = NSUserDefaults.standardUserDefaults

    actual suspend fun getRecentEmojis(): List<String> = withContext(Dispatchers.IO) {
        val json = defaults.stringForKey(KEY_RECENT_EMOJIS) ?: "[]"
        Json.decodeFromString(json)
    }

    actual suspend fun addRecentEmoji(emoji: String) = withContext(Dispatchers.IO) {
        val current = getRecentEmojis().toMutableList()
        current.remove(emoji)
        current.add(0, emoji)
        val limited = current.take(30)
        defaults.setObject(Json.encodeToString(limited), forKey = KEY_RECENT_EMOJIS)
    }

    companion object {
        private const val KEY_RECENT_EMOJIS = "recent_emojis"
    }
}