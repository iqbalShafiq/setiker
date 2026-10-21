package presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.create_pack_title
import setiker.composeapp.generated.resources.no_stickers_yet_desc
import setiker.composeapp.generated.resources.no_stickers_yet_title

/**
 * The illustration must never dominate the screen: an empty state is a message, not a poster.
 */
private val EmptyStateIllustrationHeight = 180.dp

/**
 * Whether the empty state can be scrolled.
 *
 * The empty state is rendered inside containers with **unbounded** height in several places
 * (an `AnimatedContent` measures its content with infinite constraints). A `verticalScroll` in an
 * unbounded container crashes the app with "Vertically scrollable component was measured with an
 * infinity maximum height constraints", so the scroll is only used when the height is bounded.
 */
internal enum class EmptyStateScrollMode {
    /** The container has a bounded height: the content may overflow, so it scrolls. */
    SCROLLABLE,

    /** The container is unbounded: the content is laid out at its natural size. */
    WRAP_CONTENT
}

/**
 * @param maxHeightPx the maximum height the empty state was offered, in pixels.
 */
internal fun emptyStateScrollMode(maxHeightPx: Int): EmptyStateScrollMode =
    if (maxHeightPx == Constraints.Infinity) {
        EmptyStateScrollMode.WRAP_CONTENT
    } else {
        EmptyStateScrollMode.SCROLLABLE
    }

/**
 * Full-screen "nothing here yet" state.
 *
 * The content is scrollable when the available height is bounded. When the height shrinks — the IME
 * is open, the screen is short, the font scale is large — a non-scrollable centred column used to
 * squeeze the description and the primary action out of existence while they were still
 * hit-testable. The surrounding box keeps the content centred whenever it does fit.
 */
@Composable
fun EmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    illustration: AppIllustration = AppIllustration.EmptyPack,
    action: @Composable (() -> Unit)? = null,
    /**
     * Whether this empty state reports a failure.
     *
     * A failure that looks like an empty state reads as "there is nothing here" and the user
     * waits for content that is never going to arrive. The danger treatment says what happened.
     */
    isError: Boolean = false
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val scrollMode = emptyStateScrollMode(constraints.maxHeight)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (scrollMode == EmptyStateScrollMode.SCROLLABLE) {
                        Modifier.verticalScroll(rememberScrollState())
                    } else {
                        Modifier
                    }
                )
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            AppIllustrationImage(
                illustration = illustration,
                modifier = Modifier
                    .height(EmptyStateIllustrationHeight)
                    .padding(horizontal = 6.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isError) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = if (isError) MaterialTheme.colorScheme.error else neubrutalOnSurface(),
                fontWeight = if (isError) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = neubrutalMutedOnSurface(),
                textAlign = TextAlign.Center
            )

            if (action != null) {
                Spacer(modifier = Modifier.height(28.dp))
                action()
            }
        }
    }
}

// MARK: - Previews

@Preview
@Composable
private fun EmptyStatePreview() {
    MaterialTheme {
        EmptyState(
            title = stringResource(Res.string.no_stickers_yet_title),
            description = stringResource(Res.string.no_stickers_yet_desc)
        )
    }
}

@Preview
@Composable
private fun EmptyStateWithActionPreview() {
    MaterialTheme {
        EmptyState(
            title = stringResource(Res.string.no_stickers_yet_title),
            description = stringResource(Res.string.no_stickers_yet_desc),
            action = { AppPrimaryButton(text = stringResource(Res.string.create_pack_title), onClick = {}) }
        )
    }
}
