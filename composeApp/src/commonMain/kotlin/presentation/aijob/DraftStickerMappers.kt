package presentation.aijob

import domain.model.aijob.DraftStickerSnapshot
import presentation.createpack.DraftSticker

fun DraftSticker.toSnapshot(): DraftStickerSnapshot = DraftStickerSnapshot(
    imagePath = imagePath,
    decorations = decorations,
    isAnimated = isAnimated,
    sourceVideoFile = sourceVideoFile,
    frameDecorations = frameDecorations
)

fun DraftStickerSnapshot.toDraftSticker(): DraftSticker = DraftSticker(
    imagePath = imagePath,
    decorations = decorations,
    isAnimated = isAnimated,
    sourceVideoFile = sourceVideoFile,
    frameDecorations = frameDecorations
)
