package presentation.common

import domain.model.AiQuotaOperation
import domain.model.aijob.AiJobType

fun AiJobType.toQuotaOperation(): AiQuotaOperation? = when (this) {
    AiJobType.GENERATE_STICKERS, AiJobType.GENERATE_PACK -> AiQuotaOperation.GENERATE
    AiJobType.IMPROVE_STICKERS -> AiQuotaOperation.IMPROVE
    AiJobType.VIDEO_PACK -> AiQuotaOperation.VIDEO_STICKER_PACK
    AiJobType.GRID_SPLIT -> AiQuotaOperation.GRID_SPLIT
    AiJobType.REMOVE_BACKGROUND -> AiQuotaOperation.BACKGROUND_REMOVE
    AiJobType.ANIMATED_ENCODE -> null
}

fun AiJobType.settlesQuotaOnApi(): Boolean = when (this) {
    AiJobType.GENERATE_STICKERS,
    AiJobType.GENERATE_PACK,
    AiJobType.IMPROVE_STICKERS,
    AiJobType.VIDEO_PACK -> true
    AiJobType.GRID_SPLIT,
    AiJobType.REMOVE_BACKGROUND,
    AiJobType.ANIMATED_ENCODE -> false
}

fun AiQuotaOperation.apiName(): String = when (this) {
    AiQuotaOperation.GENERATE -> "generate"
    AiQuotaOperation.GRID_SPLIT -> "gridSplit"
    AiQuotaOperation.BACKGROUND_REMOVE -> "backgroundRemove"
    AiQuotaOperation.VIDEO_STICKER_PACK -> "videoStickerPack"
    AiQuotaOperation.IMPROVE -> "improve"
}
