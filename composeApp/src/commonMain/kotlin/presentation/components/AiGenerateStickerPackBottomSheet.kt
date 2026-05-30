package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalCardRadius
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.neubrutalSubtleOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.close
import setiker.composeapp.generated.resources.generate_clear_image
import setiker.composeapp.generated.resources.generate_input_image_content_description
import setiker.composeapp.generated.resources.generate_input_image_default_hint
import setiker.composeapp.generated.resources.generate_input_image_label
import setiker.composeapp.generated.resources.generate_pick_image
import setiker.composeapp.generated.resources.generate_replace_image
import setiker.composeapp.generated.resources.generate_sticker_pack
import setiker.composeapp.generated.resources.generate_pack_sheet_title
import setiker.composeapp.generated.resources.generating
import setiker.composeapp.generated.resources.pack_layout
import setiker.composeapp.generated.resources.pack_name_label
import setiker.composeapp.generated.resources.pack_name_placeholder
import setiker.composeapp.generated.resources.prompt_label
import setiker.composeapp.generated.resources.prompt_placeholder
import setiker.composeapp.generated.resources.publisher_label
import setiker.composeapp.generated.resources.publisher_placeholder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiGenerateStickerPackBottomSheet(
    packName: String,
    onPackNameChange: (String) -> Unit,
    publisher: String,
    onPublisherChange: (String) -> Unit,
    prompt: String,
    onPromptChange: (String) -> Unit,
    layout: String,
    onLayoutChange: (String) -> Unit,
    inputImagePath: String?,
    onPickInputImage: () -> Unit,
    onClearInputImage: () -> Unit,
    isGenerating: Boolean,
    onGenerate: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = neubrutalScreenBackground(),
        scrimColor = Color.Black.copy(alpha = 0.45f),
        contentWindowInsets = { zeroBottomSheetWindowInsets() }
    ) {
        BottomSheetScrollColumn {
            Text(
                text = stringResource(Res.string.generate_pack_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = neubrutalOnSurface()
            )
            Spacer(modifier = Modifier.height(16.dp))

            AppTextField(
                value = packName,
                onValueChange = onPackNameChange,
                label = stringResource(Res.string.pack_name_label),
                placeholder = stringResource(Res.string.pack_name_placeholder)
            )
            Spacer(modifier = Modifier.height(12.dp))

            AppTextField(
                value = publisher,
                onValueChange = onPublisherChange,
                label = stringResource(Res.string.publisher_label),
                placeholder = stringResource(Res.string.publisher_placeholder)
            )
            Spacer(modifier = Modifier.height(12.dp))

            AppTextField(
                value = prompt,
                onValueChange = onPromptChange,
                label = stringResource(Res.string.prompt_label),
                placeholder = stringResource(Res.string.prompt_placeholder)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(Res.string.pack_layout),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = neubrutalOnSurface()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("2x2", "3x3", "4x4").forEach { option ->
                    FilterChip(
                        selected = layout == option,
                        onClick = { onLayoutChange(option) },
                        label = { Text(option) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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
                    if (!inputImagePath.isNullOrBlank()) {
                        AsyncImage(
                            model = inputImagePath,
                            contentDescription = stringResource(Res.string.generate_input_image_content_description),
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
                        text = stringResource(Res.string.generate_input_image_default_hint),
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
                                if (!inputImagePath.isNullOrBlank()) Res.string.generate_replace_image
                                else Res.string.generate_pick_image
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

            Spacer(modifier = Modifier.height(20.dp))
            AppPrimaryButton(
                text = stringResource(if (isGenerating) Res.string.generating else Res.string.generate_sticker_pack),
                enabled = !isGenerating && packName.isNotBlank() && publisher.isNotBlank() && prompt.isNotBlank(),
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

