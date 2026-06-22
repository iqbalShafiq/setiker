package util

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.crossfade

actual fun buildAppImageLoader(context: PlatformContext): ImageLoader =
    ImageLoader.Builder(context)
        .crossfade(true)
        .build()
