package data.repository

import data.storage.StickerFileStorage
import domain.model.Sticker
import domain.model.StickerDraftInput
import domain.model.StickerPack
import kotlin.time.Clock

open class StickerPackDraftSaver(
    private val fileStorage: StickerFileStorage,
    private val nowEpochMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    open suspend fun buildDraftPack(input: StickerDraftInput): StickerPack {
        val trayPath = if (input.trayImagePath.isBlank()) {
            ""
        } else {
            val trayFileName = "tray_${input.identifier}_${nowEpochMillis()}.png"
            val savedTray = fileStorage.trySaveTrayImage(input.trayImagePath, trayFileName)
            when {
                savedTray != null -> savedTray
                input.strictTrayCompression -> throw IllegalStateException(
                    "Tray icon could not be compressed under 50 KB for WhatsApp"
                )
                else -> ""
            }
        }
        val packIsAnimated = input.stickers.any { it.isAnimated }

        val stickers = input.stickers.mapIndexed { index, draft ->
            when {
                draft.isAnimated -> Sticker(
                    imageFile = draft.imagePath,
                    sourceImageFile = null,
                    emojis = listOf("⭐"),
                    decorations = draft.decorations,
                    isAnimated = true,
                    sourceVideoFile = draft.sourceVideoFile,
                    frameDecorations = draft.frameDecorations
                )

                else -> {
                    val baseFileName = "sticker_${input.identifier}_${index}_base.webp"
                    val basePath = fileStorage.saveStickerImage(
                        sourcePath = draft.imagePath,
                        fileName = baseFileName
                    )
                    if (packIsAnimated) {
                        val animatedPath = fileStorage.encodeSingleFrameAnimatedWebP(
                            sourcePath = basePath,
                            fileName = "sticker_${input.identifier}_${index}_animated.webp",
                            decorations = draft.decorations
                        )
                        return@mapIndexed Sticker(
                            imageFile = animatedPath,
                            sourceImageFile = basePath,
                            emojis = listOf("⭐"),
                            decorations = draft.decorations,
                            isAnimated = true
                        )
                    }
                    val previewPath = if (draft.decorations.isEmpty()) {
                        basePath
                    } else {
                        fileStorage.saveStickerImageWithDecorations(
                            sourcePath = basePath,
                            fileName = "sticker_${input.identifier}_${index}_preview.webp",
                            decorations = draft.decorations
                        )
                    }
                    Sticker(
                        imageFile = previewPath,
                        sourceImageFile = basePath,
                        emojis = listOf("⭐"),
                        decorations = draft.decorations
                    )
                }
            }
        }

        return StickerPack(
            identifier = input.identifier,
            name = input.name,
            publisher = input.publisher,
            trayImageFile = trayPath,
            stickers = stickers,
            isAnimated = packIsAnimated,
            visibility = input.visibility
        )
    }
}
