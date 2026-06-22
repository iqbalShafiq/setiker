package data.storage

import domain.model.DecorationRenderSpec
import domain.model.EmojiDecoration
import domain.model.ImageDecoration
import domain.model.StickerDecoration
import domain.model.TextDecoration
import domain.model.isBottomCaption
import domain.model.resolveEmojiStyle
import domain.model.resolveStyle
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSAttributedString
import platform.Foundation.NSString
import platform.Foundation.create
import platform.UIKit.NSFontAttributeName
import platform.UIKit.NSForegroundColorAttributeName
import platform.UIKit.NSStrokeColorAttributeName
import platform.UIKit.NSStrokeWidthAttributeName
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIFont
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIColor

@OptIn(ExperimentalForeignApi::class)
internal object IosDecorationCompositor {
    private const val STICKER_SIZE = 512.0

    fun compose(sourcePath: String, decorations: List<StickerDecoration>): ByteArray? {
        if (decorations.isEmpty()) return null
        val baseImage = UIImage.imageWithContentsOfFile(sourcePath) ?: return null
        val size = CGSizeMake(STICKER_SIZE, STICKER_SIZE)
        UIGraphicsBeginImageContextWithOptions(size, false, 1.0)
        baseImage.drawInRect(CGRectMake(0.0, 0.0, STICKER_SIZE, STICKER_SIZE))
        val minDim = STICKER_SIZE.toFloat()
        decorations.forEach { decoration ->
            val centerX = decoration.centerX.coerceIn(0f, 1f) * STICKER_SIZE
            val centerY = decoration.centerY.coerceIn(0f, 1f) * STICKER_SIZE
            val scale = decoration.scale.coerceIn(DecorationRenderSpec.MIN_SCALE, DecorationRenderSpec.MAX_SCALE)
            when (decoration) {
                is TextDecoration -> drawTextDecoration(decoration, centerX, centerY, minDim, scale)
                is EmojiDecoration -> drawEmojiDecoration(decoration, centerX, centerY, minDim, scale)
                is ImageDecoration -> drawImageDecoration(decoration, centerX, centerY, minDim, scale)
            }
        }
        val composed = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        val pngData = composed?.let { UIImagePNGRepresentation(it) } ?: return null
        return ByteArray(pngData.length.toInt()).apply {
            usePinned { pinned ->
                platform.posix.memcpy(pinned.addressOf(0), pngData.bytes, pngData.length)
            }
        }
    }

    private fun drawTextDecoration(
        decoration: TextDecoration,
        centerX: Double,
        centerY: Double,
        minDim: Float,
        scale: Float
    ) {
        val textSizePx = DecorationRenderSpec.textSizePx(decoration, minDim, scale).toDouble()
        val resolved = decoration.resolveStyle(textSizePx.toFloat())
        val font = UIFont.boldSystemFontOfSize(textSizePx)
        val nsText = decoration.text as NSString
        if (decoration.isBottomCaption()) {
            drawSingleLayerText(nsText, centerX, centerY, font, resolved.textColorArgb, 0.06f, 0xFFFFFFFFL)
            return
        }
        resolved.layers.forEach { layer ->
            drawSingleLayerText(
                text = nsText,
                centerX = centerX + layer.offsetXPx,
                centerY = centerY + layer.offsetYPx,
                font = font,
                fillColorArgb = layer.colorArgb,
                strokeWidthRatio = if (layer.isFill) 0f else layer.strokeWidthPx / textSizePx.toFloat(),
                strokeColorArgb = layer.colorArgb
            )
        }
    }

    private fun drawEmojiDecoration(
        decoration: EmojiDecoration,
        centerX: Double,
        centerY: Double,
        minDim: Float,
        scale: Float
    ) {
        val textSizePx = (minDim * DecorationRenderSpec.EMOJI_SIZE_RATIO * scale).toDouble()
        val resolved = decoration.resolveEmojiStyle(textSizePx.toFloat())
        val font = UIFont.systemFontOfSize(textSizePx)
        val nsText = decoration.emoji as NSString
        resolved.layers.forEach { layer ->
            drawSingleLayerText(
                text = nsText,
                centerX = centerX,
                centerY = centerY,
                font = font,
                fillColorArgb = layer.colorArgb,
                strokeWidthRatio = if (layer.isFill) 0f else layer.strokeWidthPx / textSizePx.toFloat(),
                strokeColorArgb = layer.colorArgb
            )
        }
    }

    private fun drawImageDecoration(
        decoration: ImageDecoration,
        centerX: Double,
        centerY: Double,
        minDim: Float,
        scale: Float
    ) {
        val stickerImage = UIImage.imageWithContentsOfFile(decoration.imagePath) ?: return
        val baseSize = minDim * DecorationRenderSpec.IMAGE_BASE_RATIO * scale
        val imageSize = stickerImage.size
        val aspectRatio = imageSize.useContents { width / height }
        val drawWidth: Double
        val drawHeight: Double
        if (aspectRatio >= 1.0) {
            drawWidth = baseSize.toDouble()
            drawHeight = baseSize / aspectRatio
        } else {
            drawHeight = baseSize.toDouble()
            drawWidth = baseSize * aspectRatio
        }
        stickerImage.drawInRect(
            CGRectMake(
                centerX - drawWidth / 2.0,
                centerY - drawHeight / 2.0,
                drawWidth,
                drawHeight
            )
        )
    }

    private fun drawSingleLayerText(
        text: NSString,
        centerX: Double,
        centerY: Double,
        font: UIFont,
        fillColorArgb: Long,
        strokeWidthRatio: Float,
        strokeColorArgb: Long
    ) {
        val attrs = buildAttributes(font, fillColorArgb, strokeWidthRatio, strokeColorArgb)
        val textSize = text.sizeWithAttributes(attrs)
        val x = centerX - textSize.useContents { width } / 2.0
        val y = centerY - textSize.useContents { height } / 2.0
        text.drawAtPoint(platform.CoreGraphics.CGPointMake(x, y), withAttributes = attrs)
    }

    private fun buildAttributes(
        font: UIFont,
        fillColorArgb: Long,
        strokeWidthRatio: Float,
        strokeColorArgb: Long
    ): Map<Any?, *> {
        val attrs = mutableMapOf<Any?, Any?>(
            NSFontAttributeName to font,
            NSForegroundColorAttributeName to uiColor(fillColorArgb)
        )
        if (strokeWidthRatio > 0f) {
            attrs[NSStrokeColorAttributeName] = uiColor(strokeColorArgb)
            attrs[NSStrokeWidthAttributeName] = -(font.pointSize * strokeWidthRatio)
        }
        return attrs
    }

    private fun uiColor(argb: Long): UIColor = UIColor.colorWithRed(
        red = ((argb shr 16) and 0xFF) / 255.0,
        green = ((argb shr 8) and 0xFF) / 255.0,
        blue = (argb and 0xFF) / 255.0,
        alpha = ((argb shr 24) and 0xFF) / 255.0
    )
}
