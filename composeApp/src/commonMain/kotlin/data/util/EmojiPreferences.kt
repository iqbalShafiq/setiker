package data.util

expect class EmojiPreferences {
    suspend fun getRecentEmojis(): List<String>
    suspend fun addRecentEmoji(emoji: String)
}