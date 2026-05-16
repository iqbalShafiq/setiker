package data.util

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.providers.NNAPIFlags
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.util.Log
import data.remote.model.GridSplitStickerFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer
import java.util.EnumSet
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class AndroidOnDeviceImageProcessor(
    private val context: Context
) : OnDeviceImageProcessor {

    private val inferenceMutex = Mutex()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var runner: BackgroundRemovalRunner? = null
    private var runnerUnavailable = false
    private var runnerUsesNnapi = false

    override suspend fun removeBackground(imagePath: String): String = withContext(Dispatchers.Default) {
        val source = decodeArgb(imagePath)
        try {
            val processed = removeBackgroundBitmap(source)
            savePng(processed, "removed_bg")
        } finally {
            source.recycle()
        }
    }

    override suspend fun splitGrid(
        imagePath: String,
        rows: Int,
        cols: Int
    ): List<GridSplitStickerFile> = withContext(Dispatchers.Default) {
        splitGridRawCells(imagePath, rows, cols).map { rawCellPath ->
            GridSplitStickerFile(
                localPath = removeBackground(rawCellPath),
                rawCellPath = rawCellPath
            )
        }
    }

    override suspend fun splitGridRawCells(
        imagePath: String,
        rows: Int,
        cols: Int
    ): List<String> = withContext(Dispatchers.Default) {
        val safeRows = rows.coerceAtLeast(1)
        val safeCols = cols.coerceAtLeast(1)
        val source = decodeArgb(imagePath)
        val results = mutableListOf<String>()

        try {
            for (row in 0 until safeRows) {
                for (col in 0 until safeCols) {
                    val left = (col * source.width.toFloat() / safeCols).roundToInt()
                    val top = (row * source.height.toFloat() / safeRows).roundToInt()
                    val right = (((col + 1) * source.width.toFloat() / safeCols).roundToInt())
                        .coerceAtLeast(left + 1)
                        .coerceAtMost(source.width)
                    val bottom = (((row + 1) * source.height.toFloat() / safeRows).roundToInt())
                        .coerceAtLeast(top + 1)
                        .coerceAtMost(source.height)

                    val cell = Bitmap.createBitmap(source, left, top, right - left, bottom - top)
                    try {
                        results += savePng(cell.copy(Bitmap.Config.ARGB_8888, false), "grid_raw_${row}_${col}")
                    } finally {
                        cell.recycle()
                    }
                }
            }
        } finally {
            source.recycle()
        }

        results
    }

    private suspend fun removeBackgroundBitmap(source: Bitmap): Bitmap {
        val mask = runCatching { predictMaskWithFallback(source) }
            .onFailure { error ->
                Log.w(TAG, "ONNX background removal failed; using local threshold fallback", error)
            }
            .getOrNull()

        val transparent = if (mask != null) {
            applyMask(source, mask)
        } else {
            removeBrightBackgroundFallback(source)
        }

        return resizeToSquareContain(transparent, OUTPUT_SIZE).also {
            if (it !== transparent) {
                transparent.recycle()
            }
        }
    }

    private suspend fun predictMaskWithFallback(source: Bitmap): FloatArray? = inferenceMutex.withLock {
        val activeRunner = getRunnerLocked() ?: return@withLock null
        try {
            return@withLock activeRunner.predictMask(source)
        } catch (error: Throwable) {
            if (!runnerUsesNnapi) {
                throw error
            }
            Log.w(TAG, "NNAPI inference failed; retrying with CPU", error)
            val modelFile = copyAssetModelToFiles()
            val environment = OrtEnvironment.getEnvironment()
            runner = BackgroundRemovalRunner(environment, modelFile, ExecutionProvider.CPU)
            runnerUsesNnapi = false
            return@withLock runner?.predictMask(source)
        }
    }

    private fun getRunnerLocked(): BackgroundRemovalRunner? {
        if (runnerUnavailable) return null
        runner?.let { return it }

        val modelFile = runCatching { copyAssetModelToFiles() }
            .onFailure { error ->
                runnerUnavailable = true
                Log.w(TAG, "U2Net model asset is unavailable; using fallback", error)
            }
            .getOrNull() ?: return null

        val environment = OrtEnvironment.getEnvironment()
        runner = runCatching {
            runnerUsesNnapi = true
            BackgroundRemovalRunner(environment, modelFile, ExecutionProvider.NNAPI)
        }.recoverCatching { error ->
            Log.w(TAG, "NNAPI session init failed; falling back to CPU", error)
            runnerUsesNnapi = false
            BackgroundRemovalRunner(environment, modelFile, ExecutionProvider.CPU)
        }.onFailure { error ->
            runnerUnavailable = true
            Log.w(TAG, "ONNX session init failed; using fallback", error)
        }.getOrNull()

        return runner
    }

    private fun copyAssetModelToFiles(): File {
        val modelDir = File(context.filesDir, "models").apply { mkdirs() }
        val output = File(modelDir, MODEL_FILE_NAME)
        if (output.exists() && output.length() > 0) {
            return output
        }

        context.assets.open("models/$MODEL_FILE_NAME").use { input ->
            FileOutputStream(output).use { outputStream ->
                input.copyTo(outputStream)
            }
        }
        return output
    }

    private fun decodeArgb(path: String): Bitmap {
        val decoded = BitmapFactory.decodeFile(path)
            ?: throw IllegalArgumentException("Cannot decode image: $path")
        return if (decoded.config == Bitmap.Config.ARGB_8888) {
            decoded
        } else {
            val converted = decoded.copy(Bitmap.Config.ARGB_8888, false)
            decoded.recycle()
            converted
        }
    }

    private fun applyMask(source: Bitmap, mask: FloatArray): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(source.width * source.height)
        source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)

        for (y in 0 until source.height) {
            val maskY = (y * MODEL_INPUT_SIZE / source.height).coerceIn(0, MODEL_INPUT_SIZE - 1)
            for (x in 0 until source.width) {
                val maskX = (x * MODEL_INPUT_SIZE / source.width).coerceIn(0, MODEL_INPUT_SIZE - 1)
                val index = y * source.width + x
                val pixel = pixels[index]
                val originalAlpha = Color.alpha(pixel)
                val matteAlpha = (mask[maskY * MODEL_INPUT_SIZE + maskX] * 255f)
                    .roundToInt()
                    .coerceIn(0, 255)
                pixels[index] = Color.argb(
                    min(originalAlpha, matteAlpha),
                    Color.red(pixel),
                    Color.green(pixel),
                    Color.blue(pixel)
                )
            }
        }

        output.setPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
        return output
    }

    private fun removeBrightBackgroundFallback(source: Bitmap): Bitmap {
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(output.width * output.height)
        output.getPixels(pixels, 0, output.width, 0, 0, output.width, output.height)

        for (index in pixels.indices) {
            val pixel = pixels[index]
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            if (r > 240 && g > 240 && b > 240) {
                pixels[index] = Color.argb(0, r, g, b)
            }
        }

        output.setPixels(pixels, 0, output.width, 0, 0, output.width, output.height)
        return output
    }

    private fun resizeToSquareContain(source: Bitmap, size: Int): Bitmap {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.TRANSPARENT)

        val scale = size.toFloat() / max(source.width, source.height).coerceAtLeast(1)
        val width = source.width * scale
        val height = source.height * scale
        val left = (size - width) / 2f
        val top = (size - height) / 2f

        canvas.drawBitmap(source, null, RectF(left, top, left + width, top + height), paint)
        return output
    }

    private fun savePng(bitmap: Bitmap, prefix: String): String {
        val dir = File(context.cacheDir, "on_device_processing").apply { mkdirs() }
        val output = File(dir, "${prefix}_${System.currentTimeMillis()}.png")
        FileOutputStream(output).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        bitmap.recycle()
        return output.absolutePath
    }

    private class BackgroundRemovalRunner(
        environment: OrtEnvironment,
        modelFile: File,
        provider: ExecutionProvider
    ) {
        private val options = OrtSession.SessionOptions().apply {
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            setIntraOpNumThreads(1)
            if (provider == ExecutionProvider.NNAPI && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                addNnapi(EnumSet.of(NNAPIFlags.CPU_DISABLED, NNAPIFlags.USE_FP16))
            }
        }
        private val session = environment.createSession(modelFile.absolutePath, options)
        private val inputName = session.inputNames.first()

        fun predictMask(source: Bitmap): FloatArray {
            val scaled = Bitmap.createScaledBitmap(source, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, true)
            val input = FloatArray(3 * MODEL_INPUT_SIZE * MODEL_INPUT_SIZE)
            val pixels = IntArray(MODEL_INPUT_SIZE * MODEL_INPUT_SIZE)
            scaled.getPixels(pixels, 0, MODEL_INPUT_SIZE, 0, 0, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE)
            scaled.recycle()

            for (y in 0 until MODEL_INPUT_SIZE) {
                for (x in 0 until MODEL_INPUT_SIZE) {
                    val pixel = pixels[y * MODEL_INPUT_SIZE + x]
                    val plane = y * MODEL_INPUT_SIZE + x
                    input[plane] = ((Color.red(pixel) / 255f) - 0.485f) / 0.229f
                    input[MODEL_INPUT_SIZE * MODEL_INPUT_SIZE + plane] =
                        ((Color.green(pixel) / 255f) - 0.456f) / 0.224f
                    input[2 * MODEL_INPUT_SIZE * MODEL_INPUT_SIZE + plane] =
                        ((Color.blue(pixel) / 255f) - 0.406f) / 0.225f
                }
            }

            OnnxTensor.createTensor(
                OrtEnvironment.getEnvironment(),
                FloatBuffer.wrap(input),
                longArrayOf(1, 3, MODEL_INPUT_SIZE.toLong(), MODEL_INPUT_SIZE.toLong())
            ).use { tensor ->
                session.run(mapOf(inputName to tensor)).use { output ->
                    val first = output[0].value
                    val raw = flattenOutput(first)
                    return normalizeMask(raw)
                }
            }
        }

        private fun flattenOutput(value: Any): FloatArray {
            return when (value) {
                is Array<*> -> flattenArray(value)
                is FloatArray -> value
                else -> error("Unsupported ONNX output type: ${value::class}")
            }
        }

        private fun flattenArray(value: Array<*>): FloatArray {
            val values = ArrayList<Float>(MODEL_INPUT_SIZE * MODEL_INPUT_SIZE)
            fun visit(node: Any?) {
                when (node) {
                    is Float -> values += node
                    is FloatArray -> node.forEach { values += it }
                    is Array<*> -> node.forEach(::visit)
                }
            }
            visit(value)
            return values.takeLast(MODEL_INPUT_SIZE * MODEL_INPUT_SIZE).toFloatArray()
        }

        private fun normalizeMask(raw: FloatArray): FloatArray {
            val mask = if (raw.size == MODEL_INPUT_SIZE * MODEL_INPUT_SIZE) {
                raw
            } else {
                raw.copyOfRange(0, min(raw.size, MODEL_INPUT_SIZE * MODEL_INPUT_SIZE))
                    .copyOf(MODEL_INPUT_SIZE * MODEL_INPUT_SIZE)
            }
            val minValue = mask.minOrNull() ?: 0f
            val maxValue = mask.maxOrNull() ?: 1f
            val range = (maxValue - minValue).takeIf { it > 1e-6f } ?: 1f
            return FloatArray(mask.size) { index ->
                ((mask[index] - minValue) / range).coerceIn(0f, 1f)
            }
        }
    }

    companion object {
        private const val TAG = "OnDeviceImageProcessor"
        private const val MODEL_FILE_NAME = "u2net.onnx"
        private const val MODEL_INPUT_SIZE = 320
        private const val OUTPUT_SIZE = 512
    }

    private enum class ExecutionProvider {
        NNAPI,
        CPU
    }
}
