package util

import coil3.ImageLoader
import coil3.PlatformContext

/**
 * Build a singleton [ImageLoader] tailored for the platform. On Android we
 * register the animated WebP / GIF decoder so animated stickers render in
 * `AsyncImage`. On other platforms the default decoder set is fine.
 */
expect fun buildAppImageLoader(context: PlatformContext): ImageLoader
