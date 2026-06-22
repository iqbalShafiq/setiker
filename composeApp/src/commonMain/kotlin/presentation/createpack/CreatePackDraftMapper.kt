package presentation.createpack

import domain.model.Sticker
import domain.model.StickerPack

fun Sticker.toDraftSticker(): DraftSticker = DraftSticker(
    imagePath = if (isAnimated) imageFile else (sourceImageFile ?: imageFile),
    decorations = decorations,
    isAnimated = isAnimated,
    sourceVideoFile = sourceVideoFile,
    frameDecorations = frameDecorations,
)

fun StickerPack.toDraftStickers(): List<DraftSticker> = stickers.map { it.toDraftSticker() }

/**
 * Merges stickers loaded from persistence with any in-session drafts that are not yet saved
 * (e.g. gallery picks still on the create screen).
 */
fun mergeDraftStickersFromPack(
    persisted: List<DraftSticker>,
    inSession: List<DraftSticker>,
): List<DraftSticker> {
    val persistedPaths = persisted.map { it.imagePath }.toSet()
    return buildList {
        addAll(persisted)
        for (local in inSession) {
            if (local.imagePath.isNotBlank() &&
                local.imagePath !in persistedPaths &&
                none { it.imagePath == local.imagePath }
            ) {
                add(local)
            }
        }
    }
}
