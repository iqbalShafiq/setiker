package presentation.common

import androidx.compose.runtime.Composable

/**
 * Whether the platform asks the app to reduce motion.
 *
 * Compose already scales its own animations through the platform's animator duration scale, but
 * loops written with `delay` (the bouncing loader, autoplaying previews, the shimmer) do not know
 * about that setting. Those loops must ask this composable before they start.
 *
 * Reduced motion means fewer and gentler animations, not zero: a fade or a colour change may stay,
 * but perpetual movement and large positional transitions stop.
 */
@Composable
expect fun rememberReduceMotion(): Boolean
