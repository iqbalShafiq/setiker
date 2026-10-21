package presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.remote.ExploreApiRepository
import domain.error.detailText
import domain.error.onFailure
import domain.error.onSuccess
import domain.model.PromptPreset
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalSubtleOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.prompt_presets
import setiker.composeapp.generated.resources.prompt_presets_empty
import setiker.composeapp.generated.resources.prompt_presets_open

@Composable
fun PromptPresetsTrailingIcon(
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    NeubrutalIconButton(
        icon = Icons.AutoMirrored.Filled.MenuBook,
        contentDescription = stringResource(Res.string.prompt_presets_open),
        onClick = onClick,
        enabled = enabled,
        iconSize = 20.dp,
        tint = neubrutalSubtleOnSurface(),
        pressedScale = 0.88f,
        label = "prompt_presets_scale"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptPresetPickerSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onPresetSelected: (PromptPreset) -> Unit,
    category: String? = null,
    exploreApiRepository: ExploreApiRepository = koinInject()
) {
    if (!visible) return
    var presets by remember(visible, category) { mutableStateOf<List<PromptPreset>>(emptyList()) }
    var isLoading by remember(visible, category) { mutableStateOf(true) }
    var loadError by remember(visible, category) { mutableStateOf<String?>(null) }

    LaunchedEffect(visible, category) {
        if (!visible) return@LaunchedEffect
        isLoading = true
        loadError = null
        exploreApiRepository.getPromptPresets(category)
            .onSuccess { loaded ->
                presets = loaded
                isLoading = false
            }
            .onFailure { error ->
                presets = emptyList()
                loadError = error.detailText()
                isLoading = false
            }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(Res.string.prompt_presets),
                fontWeight = FontWeight.Bold
            )
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                loadError != null -> {
                    Text(
                        text = loadError ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = neubrutalMutedOnSurface()
                    )
                }
                presets.isEmpty() -> {
                    Text(
                        text = stringResource(Res.string.prompt_presets_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = neubrutalMutedOnSurface()
                    )
                }
                else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presets, key = { it.id }) { preset ->
                        NeubrutalSelectableChip(
                            label = preset.title,
                            selected = false,
                            onClick = {
                                onPresetSelected(preset)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}
