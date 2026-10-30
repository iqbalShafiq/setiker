package data.util

import kotlin.math.PI
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGAffineTransformMakeTranslation
import platform.CoreGraphics.CGContextConcatCTM
import platform.CoreGraphics.CGContextRotateCTM
import platform.CoreGraphics.CGContextScaleCTM
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.writeToFile
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.time

@OptIn(ExperimentalForeignApi::class)
actual suspend fun applyCropTransformation(
    sourcePath: String,
    scale: Float,
    rotation: Float,
    offsetX: Float,
    offsetY: Float,
    flipHorizontal: Boolean,
    flipVertical: Boolean,
    outputSize: Int
): String = withContext(Dispatchers.IO) {
    val sourceImage = UIImage.imageWithContentsOfFile(sourcePath)
        ?: throw IllegalArgumentException("Cannot load image: $sourcePath")

    val size = CGSizeMake(outputSize.toDouble(), outputSize.toDouble())
    UIGraphicsBeginImageContextWithOptions(size, false, 0.0)

    val context = UIGraphicsGetCurrentContext()
        ?: throw IllegalStateException("Cannot get graphics context")

    val sourceSize = sourceImage.size
    val sourceWidth = sourceSize.useContents { width }
    val sourceHeight = sourceSize.useContents { height }
    val maxDim = maxOf(sourceWidth, sourceHeight).coerceAtLeast(1.0)
    val fitScale = outputSize.toDouble() / maxDim
    val finalScale = scale.coerceIn(0.5f, 4f).toDouble() * fitScale

    // Mirror CropScreen: aspect-fit into the square output, then apply user
    // transform and normalized offsets.
    val transform = CGAffineTransformMakeTranslation(
        outputSize / 2.0 + offsetX.coerceIn(-1f, 1f).toDouble() * outputSize,
        outputSize / 2.0 + offsetY.coerceIn(-1f, 1f).toDouble() * outputSize
    )
    CGContextConcatCTM(context, transform)

    CGContextRotateCTM(context, rotation.toDouble() * PI / 180.0)

    val scaleX = if (flipHorizontal) -finalScale else finalScale
    val scaleY = if (flipVertical) -finalScale else finalScale
    CGContextScaleCTM(context, scaleX, scaleY)

    // Draw centered
    sourceImage.drawInRect(
        CGRectMake(
            -sourceWidth / 2.0,
            -sourceHeight / 2.0,
            sourceWidth,
            sourceHeight
        )
    )

    val outputImage = UIGraphicsGetImageFromCurrentImageContext()
        ?: throw IllegalStateException("Cannot encode image")
    UIGraphicsEndImageContext()

    // Save to file
    val outputPath = sourcePath.substringBeforeLast("/") + "/cropped_${time(null) * 1000}.png"
    val imageData = UIImagePNGRepresentation(outputImage)
        ?: throw IllegalStateException("Cannot encode image")

    imageData.writeToFile(outputPath, atomically = true)

    outputPath
}