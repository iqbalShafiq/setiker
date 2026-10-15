package presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import domain.model.ShareLinkUi
import domain.model.preferredShareText
import org.jetbrains.compose.resources.stringResource
import presentation.common.ContentStateAnimations
import presentation.common.ShareLinksBodyPhase
import presentation.common.rememberShareTextAction
import presentation.theme.NeubrutalCardRadius
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.cloud_links_copy
import setiker.composeapp.generated.resources.cloud_links_create
import setiker.composeapp.generated.resources.cloud_links_empty
import setiker.composeapp.generated.resources.cloud_links_loading
import setiker.composeapp.generated.resources.cloud_links_refresh
import setiker.composeapp.generated.resources.cloud_links_revoke
import setiker.composeapp.generated.resources.cloud_links_share
import setiker.composeapp.generated.resources.cloud_links_title

@Composable
fun ShareLinksSheet(
    isLoading: Boolean,
    links: List<ShareLinkUi>,
    enabled: Boolean,
    onRefresh: () -> Unit,
    onCreate: () -> Unit,
    onRevoke: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(Res.string.cloud_links_title)
) {
    val clipboard = LocalClipboardManager.current
    val shareText = rememberShareTextAction()
    val bodyPhase = when {
        isLoading -> ShareLinksBodyPhase.Loading
        links.isEmpty() -> ShareLinksBodyPhase.Empty
        else -> ShareLinksBodyPhase.Links
    }
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = neubrutalOnSurface()
        )
        Spacer(modifier = Modifier.height(12.dp))
        AppPrimaryButton(
            text = stringResource(Res.string.cloud_links_create),
            onClick = onCreate,
            enabled = enabled
        )
        Spacer(modifier = Modifier.height(8.dp))
        AppSecondaryButton(
            text = stringResource(Res.string.cloud_links_refresh),
            onClick = onRefresh,
            enabled = enabled
        )
        Spacer(modifier = Modifier.height(12.dp))
        // Keep actions stable; only the body swaps (sheet-specific, not full-screen list load).
        AnimatedContent(
            targetState = bodyPhase,
            transitionSpec = {
                fadeIn(animationSpec = tween(ContentStateAnimations.SHEET_MS)) togetherWith
                    fadeOut(animationSpec = tween(ContentStateAnimations.EXIT_MS))
            },
            label = "share_links_body"
        ) { phase ->
            when (phase) {
                ShareLinksBodyPhase.Loading -> {
                    LoadingIndicator(
                        label = stringResource(Res.string.cloud_links_loading),
                        illustration = AppIllustration.LoadingState
                    )
                }
                ShareLinksBodyPhase.Empty -> {
                    Column {
                        AppIllustrationImage(
                            illustration = AppIllustration.SuccessSync,
                            modifier = Modifier.height(140.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(Res.string.cloud_links_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = neubrutalMutedOnSurface()
                        )
                    }
                }
                ShareLinksBodyPhase.Links -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        links.forEach { link ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(NeubrutalCardRadius))
                                    .background(neubrutalCardSurface())
                                    .neubrutalBorderWithGloss(
                                        color = neubrutalBorderColor(),
                                        cornerRadius = NeubrutalCardRadius,
                                        highlightColor = neubrutalGlossyHighlightColor()
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = link.shareUrl ?: link.token,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = neubrutalOnSurface(),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        val value = link.preferredShareText()
                                        clipboard.setText(AnnotatedString(value))
                                    },
                                    enabled = enabled
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = stringResource(Res.string.cloud_links_copy)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        shareText(link.preferredShareText())
                                    },
                                    enabled = enabled
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = stringResource(Res.string.cloud_links_share)
                                    )
                                }
                                IconButton(
                                    onClick = { onRevoke(link.id) },
                                    enabled = enabled
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(Res.string.cloud_links_revoke)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
