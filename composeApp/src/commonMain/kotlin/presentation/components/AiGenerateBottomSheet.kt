package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.stringResource
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.neubrutalSubtleOnSurface
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalCardRadius
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.cancel
import setiker.composeapp.generated.resources.close
import setiker.composeapp.generated.resources.generate
import setiker.composeapp.generated.resources.generate_ai_sheet_title
import setiker.composeapp.generated.resources.generate_clear_image
import setiker.composeapp.generated.resources.generate_grid_hint
import setiker.composeapp.generated.resources.generate_input_image_content_description
import setiker.composeapp.generated.resources.generate_input_image_default_hint
import setiker.composeapp.generated.resources.generate_input_image_hint
import setiker.composeapp.generated.resources.generate_input_image_label
import setiker.composeapp.generated.resources.generate_pick_image
import setiker.composeapp.generated.resources.generate_replace_image
import setiker.composeapp.generated.resources.generate_tip
import setiker.composeapp.generated.resources.generating
import setiker.composeapp.generated.resources.prompt_label
import setiker.composeapp.generated.resources.prompt_placeholder

/**
 * Shared bottom sheet for hitting `/api/v1/generate`. Used by:
 *   - Pack editor (`CreatePackScreen`): bulk generate stickers, optional reference image.
 *   - Single sticker editor (`EditorScreen`): generate a replacement for one sticker, default
 *     reference image is the sticker being edited.
 *
 * The same state shape lives in both screens to keep the contract identical:
 *   - `prompt` maps to API text input.
 *   - `inputImagePath` is the optional `image` multipart field. The callers decide the default.
 *
 * @param hasContextualDefault true when the input image already represents something meaningful
 *   to the user (e.g. the current sticker). When true, the hint text emphasises that the
 *   default is the related image and a "remove" button is shown; when false, the slot reads as
 *   a generic optional reference uploader. Pack editor passes `false`, sticker editor passes
 *   `true`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiGenerateBottomSheet(
    prompt: String,
    onPromptChange: (String) -> Unit,
    inputImagePath: String?,
    onPickInputImage: () -> Unit,
    onClearInputImage: () -> Unit,
    hasContextualDefault: Boolean,
    isGenerating: Boolean,
    onGenerate: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = neubrutalScreenBackground(),
        scrimColor = Color.Black.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(Res.string.generate_ai_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = neubrutalOnSurface()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Optional reference image. Default value (sticker image vs none) is decided by the
            // caller; this composable just renders whatever is in `inputImagePath`.
            Text(
                text = stringResource(Res.string.generate_input_image_label),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = neubrutalOnSurface()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(NeubrutalCardRadius))
                        .background(neubrutalCardSurface())
                        .border(NeubrutalBorderWidth, neubrutalBorderColor(), RoundedCornerShape(NeubrutalCardRadius))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val path = inputImagePath
                    if (!path.isNullOrBlank()) {
                        AsyncImage(
                            model = path,
                            contentDescription = stringResource(
                                Res.string.generate_input_image_content_description
                            ),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = null,
                            tint = neubrutalSubtleOnSurface()
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(
                            if (hasContextualDefault) {
                                Res.string.generate_input_image_hint
                            } else {
                                Res.string.generate_input_image_default_hint
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = neubrutalMutedOnSurface(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NeubrutalIconButton(
                            icon = Icons.Filled.Edit,
                            contentDescription = stringResource(
                                if (!inputImagePath.isNullOrBlank()) {
                                    Res.string.generate_replace_image
                                } else {
                                    Res.string.generate_pick_image
                                }
                            ),
                            onClick = onPickInputImage
                        )
                        if (!inputImagePath.isNullOrBlank()) {
                            NeubrutalIconButton(
                                icon = Icons.Filled.Close,
                                contentDescription = stringResource(Res.string.generate_clear_image),
                                onClick = onClearInputImage
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AppTextField(
                value = prompt,
                onValueChange = onPromptChange,
                label = stringResource(Res.string.prompt_label),
                placeholder = stringResource(Res.string.prompt_placeholder)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.generate_tip),
                style = MaterialTheme.typography.bodySmall,
                color = neubrutalMutedOnSurface()
            )
            Spacer(modifier = Modifier.height(20.dp))
            AppPrimaryButton(
                text = stringResource(if (isGenerating) Res.string.generating else Res.string.generate),
                enabled = !isGenerating && prompt.isNotBlank(),
                onClick = onGenerate
            )
            Spacer(modifier = Modifier.height(8.dp))
            AppSecondaryButton(
                text = stringResource(Res.string.close),
                onClick = onDismiss
            )
        }
    }
}

// MARK: - Previews

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun AiGenerateBottomSheetPreview() {
    MaterialTheme {
        AiGenerateBottomSheet(
            prompt = "A cute cat sticker",
            onPromptChange = {},
            inputImagePath = null,
            onPickInputImage = {},
            onClearInputImage = {},
            hasContextualDefault = false,
            isGenerating = false,
            onGenerate = {},
            onDismiss = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun AiGenerateBottomSheetGridPreview() {
    MaterialTheme {
        AiGenerateBottomSheet(
            prompt = "A cute cat sticker",
            onPromptChange = {},
            inputImagePath = null,
            onPickInputImage = {},
            onClearInputImage = {},
            hasContextualDefault = false,
            isGenerating = false,
            onGenerate = {},
            onDismiss = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun AiGenerateBottomSheetGeneratingPreview() {
    MaterialTheme {
        AiGenerateBottomSheet(
            prompt = "A cute cat sticker",
            onPromptChange = {},
            inputImagePath = null,
            onPickInputImage = {},
            onClearInputImage = {},
            hasContextualDefault = false,
            isGenerating = true,
            onGenerate = {},
            onDismiss = {}
        )
    }
}
