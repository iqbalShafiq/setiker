package data.storage

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import domain.model.StickerDecoration
import kotlinx.coroutines.suspendCancellableCoroutine
import presentation.components.ReadOnlyDecorationOverlay
import presentation.theme.SetikerTheme
import kotlin.coroutines.resume

/**
 * Renders [decorations] with the same Compose stack as the editor overlay
 * ([ReadOnlyDecorationOverlay]) into a transparent ARGB bitmap.
 */
internal object AndroidDecorationBitmapRenderer {

    suspend fun render(
        context: Context,
        width: Int,
        height: Int,
        decorations: List<StickerDecoration>
    ): Bitmap {
        require(width > 0 && height > 0)
        val activity = ForegroundActivityProvider.current()
            ?: context.findActivity()
            ?: error(
                "Cannot render sticker decorations: no foreground Activity. " +
                    "Open the editor and try saving again."
            )

        val contentRoot = activity.findViewById<ViewGroup>(android.R.id.content)
        val composeView = ComposeView(activity).apply {
            visibility = View.INVISIBLE
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                // 1 px = 1 dp so normalized decoration coords map 1:1 to the output bitmap.
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    SetikerTheme(darkTheme = false) {
                        ReadOnlyDecorationOverlay(
                            decorations = decorations,
                            modifier = Modifier.size(width.dp, height.dp)
                        )
                    }
                }
            }
        }

        val layoutParams = FrameLayout.LayoutParams(width, height).apply {
            // Keep off-screen but attached so WindowRecomposer is available.
            leftMargin = -width * 2
            topMargin = -height * 2
        }
        contentRoot.addView(composeView, layoutParams)
        try {
            composeView.awaitPreDraw()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            composeView.draw(Canvas(bitmap))
            return bitmap
        } finally {
            contentRoot.removeView(composeView)
        }
    }

    private suspend fun View.awaitPreDraw() = suspendCancellableCoroutine { cont ->
        if (isLaidOut && width > 0 && height > 0) {
            cont.resume(Unit)
            return@suspendCancellableCoroutine
        }
        val observer = viewTreeObserver
        val listener = object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                observer.removeOnPreDrawListener(this)
                if (cont.isActive) cont.resume(Unit)
                return true
            }
        }
        observer.addOnPreDrawListener(listener)
        cont.invokeOnCancellation { observer.removeOnPreDrawListener(listener) }
        requestLayout()
        invalidate()
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
