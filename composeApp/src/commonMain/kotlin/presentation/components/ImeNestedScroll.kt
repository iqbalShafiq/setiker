package presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Nested-scroll hand-off between the soft keyboard and the scrollable container.
 *
 * `Modifier.imeNestedScroll()` exists only on Android. On the other targets the keyboard does not
 * participate in nested scroll, so the actual implementation is the identity modifier.
 */
@Composable
expect fun Modifier.imeNestedScrollCompat(): Modifier
