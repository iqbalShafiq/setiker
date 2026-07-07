package presentation.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import domain.model.LegalSection
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import presentation.common.resolveLocal
import presentation.components.AppTopBar
import presentation.components.EmptyState
import presentation.components.LoadingIndicator
import presentation.components.ScaffoldBottomBarLazyColumn
import presentation.theme.NeubrutalCardRadius
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import presentation.theme.NeubrutalSmallShadowOffset

@Composable
fun LegalDocumentScreenRoot(
    docType: String,
    onBack: () -> Unit,
    viewModel: LegalDocumentViewModel = koinViewModel { parametersOf(docType) }
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                LegalDocumentEffect.NavigateBack -> onBack()
            }
        }
    }
    LegalDocumentScreen(
        state = state,
        onBack = { viewModel.onIntent(LegalDocumentIntent.NavigateBack) }
    )
}

@Composable
fun LegalDocumentScreen(
    state: LegalDocumentState,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = state.document?.title ?: "",
                onBackClick = onBack
            )
        },
        containerColor = neubrutalScreenBackground()
    ) { padding ->
        when {
            state.isLoading -> LoadingIndicator(modifier = Modifier.fillMaxSize().padding(padding))
            state.error != null -> EmptyState(
                title = state.error?.resolveLocal().orEmpty(),
                description = "",
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            state.document != null -> {
                val doc = state.document!!
                ScaffoldBottomBarLazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        doc.summary?.let { summary ->
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = neubrutalMutedOnSurface()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    items(doc.sections.size) { index ->
                        LegalSectionCard(section = doc.sections[index])
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun LegalSectionCard(section: LegalSection) {
    val border = neubrutalBorderColor()
    val shadow = neubrutalShadowColor()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .neubrutalShadow(
                offsetX = NeubrutalSmallShadowOffset,
                offsetY = NeubrutalSmallShadowOffset,
                cornerRadius = NeubrutalCardRadius,
                color = shadow
            )
            .background(neubrutalCardSurface())
            .neubrutalBorderWithGloss(
                color = border,
                cornerRadius = NeubrutalCardRadius,
                highlightColor = neubrutalGlossyHighlightColor()
            )
            .padding(16.dp)
    ) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = neubrutalOnSurface()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = section.body,
            style = MaterialTheme.typography.bodyMedium,
            color = neubrutalMutedOnSurface()
        )
    }
}
