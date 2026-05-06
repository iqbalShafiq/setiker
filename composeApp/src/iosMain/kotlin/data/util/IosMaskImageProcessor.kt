package data.util

import androidx.compose.ui.unit.IntSize
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import presentation.backgroundremover.DrawPath
import platform.CoreGraphics.CGContextAddLineToPoint
import platform.CoreGraphics.CGContextMoveToPoint
import platform.CoreGraphics.CGContextSetLineCap
import platform.CoreGraphics.CGContextSetLineJoin
import platform.CoreGraphics.CGContextSetLineWidth
import platform.CoreGraphics.CGLineCapRound
import platform.CoreGraphics.CGLineJoinRound
import platform.CoreGraphics.CGRectMake
import platform.Foundation.writeToFile
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation

@OptIn(ExperimentalForeignApi::class)
actual suspend fun applyMaskToImage(
    imagePath: String,
    paths: List<DrawPath>,
    canvasSize: IntSize
): String = withContext(Dispatchers.IO) {
    val sourceImage = UIImage.imageWithContentsOfFile(imagePath)
        ?: throw IllegalArgumentException("Cannot load image: $imagePath")

    val sourceSize = sourceImage.size
    val scaleX = sourceSize.width / canvasSize.width.toDouble()
    val scaleY = sourceSize.height / canvasSize.height.toDouble()

    UIGraphicsBeginImageContextWithOptions(sourceSize, false, 0.0)

    val context = UIGraphicsGetCurrentContext()
        ?: throw IllegalStateException("Cannot get graphics context")

    // Draw source image
    sourceImage.drawInRect(CGRectMake(0.0, 0.0, sourceSize.width, sourceSize.height))

    // Apply mask
    paths.forEach { brushPath ->
        CGContextSetLineWidth(context, brushPath.brushSize * kotlin.math.max(scaleX, scaleY))
        CGContextSetLineCap(context, kCGLineCapRound)
        CGContextSetLineJoin(context, kCGLineJoinRound)

        if (brushPath.isErasing) {
            context.setBlendMode(platform.CoreGraphics.kCGBlendModeClear)
        } else {
            context.setBlendMode(platform.CoreGraphics.kCGBlendModeNormal)
        }

        if (brushPath.points.isNotEmpty()) {
            val first = brushPath.points.first()
            CGContextMoveToPoint(context, first.first * scaleX, first.second * scaleY)

            brushPath.points.drop(1).forEach { point ->
                CGContextAddLineToPoint(context, point.first * scaleX, point.second * scaleY)
            }
        }

        context.strokePath()
    }

    val outputImage = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()

    // Save
    val outputPath = imagePath.substringBeforeLast("/") + "/masked_${System.currentTimeMillis()}.png"
    val imageData = UIImagePNGRepresentation(outputImage)
        ?: throw IllegalStateException("Cannot encode image")

    imageData.writeToFile(outputPath, atomically = true)

    outputPath
}