package data.sync

fun normalizePackVisibilityForStorage(visibility: String): String = when (visibility.trim().uppercase()) {
    "PUBLIC" -> "PUBLIC"
    "UNLISTED" -> "UNLISTED"
    else -> "PRIVATE"
}

fun normalizePackVisibilityForApi(visibility: String): String = when (normalizePackVisibilityForStorage(visibility)) {
    "PUBLIC" -> "public"
    "UNLISTED" -> "unlisted"
    else -> "private"
}
