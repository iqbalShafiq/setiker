package presentation.common

object PackIdentifierSanitizer {
    fun sanitize(rawName: String, suffix: Int): String {
        val allowed = Regex("[^A-Za-z0-9_\\-.,' ]")
        val cleaned = rawName.lowercase()
            .replace(allowed, "_")
            .replace("..", "_")
            .replace(Regex("_+"), "_")
            .trim('_', ' ')
        val base = cleaned.ifBlank { "pack" }.take(110)
        return "${base}_$suffix"
    }
}
