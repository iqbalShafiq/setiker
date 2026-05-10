package util

import android.os.Build
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.request.crossfade

actual fun buildAppImageLoader(context: PlatformContext): ImageLoader =
    ImageLoader.Builder(context)
        .components {
            // ImageDecoder (API 28+) handles animated WebP, GIF, and HEIF natively
            // and is significantly faster than the legacy Movie-based GifDecoder.
            // Fall back to Movie-based GIF decoding on older devices.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                add(AnimatedImageDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .crossfade(true)
        .build()
