package domain.model

enum class SortOrder(val labelRes: String) {
    NAME_ASC("sort_name_asc"),
    NAME_DESC("sort_name_desc"),
    STICKER_COUNT_ASC("sort_stickers_asc"),
    STICKER_COUNT_DESC("sort_stickers_desc"),
    NEWEST("sort_newest"),
    OLDEST("sort_oldest");

    val comparator: Comparator<StickerPack>
        get() = when (this) {
            NAME_ASC -> compareBy { it.name.lowercase() }
            NAME_DESC -> compareByDescending { it.name.lowercase() }
            STICKER_COUNT_ASC -> compareBy { it.stickers.size }
            STICKER_COUNT_DESC -> compareByDescending { it.stickers.size }
            NEWEST -> compareByDescending { it.identifier }
            OLDEST -> compareBy { it.identifier }
        }
}
