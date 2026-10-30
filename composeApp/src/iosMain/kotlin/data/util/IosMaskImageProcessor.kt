package data.util

import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGBlendMode
import platform.CoreGraphics.CGContextAddLineToPoint
import platform.CoreGraphics.CGContextMoveToPoint
import platform.CoreGraphics.CGContextSetBlendMode
import platform.CoreGraphics.CGContextSetLineCap
import platform.CoreGraphics.CGContextSetLineJoin
import platform.CoreGraphics.CGContextSetLineWidth
import platform.CoreGraphics.CGContextStrokePath
import platform.CoreGraphics.CGLineCap
import platform.CoreGraphics.CGLineJoin
import platform.CoreGraphics.CGRectMake
import platform.Foundation.writeToFile
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.time

@OptIn(ExperimentalForeignApi::class)
actual suspend fun applyMaskToImage(
    imagePath: String,
    paths: List<DrawPath>,
    canvasSize: IntSize
): String = withContext(Dispatchers.IO) {
    val sourceImage = UIImage.imageWithContentsOfFile(imagePath)
        ?: throw IllegalArgumentException("Cannot load image: $imagePath")

    val sourceSize = sourceImage.size
    val sourceWidth = sourceSize.useContents { width }
    val sourceHeight = sourceSize.useContents { height }
    val scaleX = sourceWidth / canvasSize.width.toDouble()
    val scaleY = sourceHeight / canvasSize.height.toDouble()

    UIGraphicsBeginImageContextWithOptions(sourceSize, false, 0.0)

    val context = UIGraphicsGetCurrentContext()
        ?: throw IllegalStateException("Cannot get graphics context")

    // Draw source image
    sourceImage.drawInRect(CGRectMake(0.0, 0.0, sourceWidth, sourceHeight))

    // Apply mask
    paths.forEach { brushPath ->
        CGContextSetLineWidth(context, brushPath.brushSize * max(scaleX, scaleY))
        CGContextSetLineCap(context, CGLineCap.kCGLineCapRound)
        CGContextSetLineJoin(context, CGLineJoin.kCGLineJoinRound)

        if (brushPath.isErasing) {
            CGContextSetBlendMode(context, CGBlendMode.kCGBlendModeClear)
        } else {
            CGContextSetBlendMode(context, CGBlendMode.kCGBlendModeNormal)
        }

        if (brushPath.points.isNotEmpty()) {
            val first = brushPath.points.first()
            CGContextMoveToPoint(context, first.first * scaleX, first.second * scaleY)

            brushPath.points.drop(1).forEach { point ->
                CGContextAddLineToPoint(context, point.first * scaleX, point.second * scaleY)
            }
        }

        CGContextStrokePath(context)
    }

    val outputImage = UIGraphicsGetImageFromCurrentImageContext()
        ?: throw IllegalStateException("Cannot encode image")
    UIGraphicsEndImageContext()

    // Save
    val outputPath = imagePath.substringBeforeLast("/") + "/masked_${time(null) * 1000}.png"
    val imageData = UIImagePNGRepresentation(outputImage)
        ?: throw IllegalStateException("Cannot encode image")

    imageData.writeToFile(outputPath, atomically = true)

    outputPath
}