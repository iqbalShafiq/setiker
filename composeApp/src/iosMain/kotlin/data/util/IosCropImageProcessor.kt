package data.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGAffineTransformMakeRotation
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.CoreGraphics.CGAffineTransformMakeTranslation
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.writeToFile
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation

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

    val context = platform.CoreGraphics.UIGraphicsGetCurrentContext()
        ?: throw IllegalStateException("Cannot get graphics context")

    // Apply transformations
    val transform = CGAffineTransformMakeTranslation(
        (outputSize / 2f + offsetX).toDouble(),
        (outputSize / 2f + offsetY).toDouble()
    )
    context.concatenateWithTransform(transform)

    context.rotateByAngle(rotation.toDouble() * kotlin.math.PI / 180.0)

    val scaleX = if (flipHorizontal) -scale.toDouble() else scale.toDouble()
    val scaleY = if (flipVertical) -scale.toDouble() else scale.toDouble()
    context.scaleBy(x = scaleX, y = scaleY)

    // Draw centered
    val sourceSize = sourceImage.size
    sourceImage.drawInRect(
        CGRectMake(
            -sourceSize.width / 2.0,
            -sourceSize.height / 2.0,
            sourceSize.width,
            sourceSize.height
        )
    )

    val outputImage = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()

    // Save to file
    val outputPath = sourcePath.substringBeforeLast("/") + "/cropped_${System.currentTimeMillis()}.png"
    val imageData = UIImagePNGRepresentation(outputImage)
        ?: throw IllegalStateException("Cannot encode image")

    imageData.writeToFile(outputPath, atomically = true)

    outputPath
}