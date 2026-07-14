package presentation.legal

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import domain.model.LegalSection
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import presentation.common.ContentStateAnimations
import presentation.common.DocumentLoadPhase
import presentation.common.resolveLocal
import presentation.components.AppPrimaryButton
import presentation.components.AppTextField
import presentation.components.AppTopBar
import presentation.components.EmptyState
import presentation.components.LoadingIndicator
import presentation.components.ScaffoldBottomBarLazyColumn
import presentation.theme.AccentCoral
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
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.account_deletion_confirm_label
import setiker.composeapp.generated.resources.account_deletion_email_label
import setiker.composeapp.generated.resources.account_deletion_email_placeholder
import setiker.composeapp.generated.resources.account_deletion_form_title
import setiker.composeapp.generated.resources.account_deletion_reason_label
import setiker.composeapp.generated.resources.account_deletion_reason_placeholder
import setiker.composeapp.generated.resources.account_deletion_submit
import setiker.composeapp.generated.resources.account_deletion_submitting

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
        onIntent = viewModel::onIntent
    )
}

@Composable
fun LegalDocumentScreen(
    state: LegalDocumentState,
    onIntent: (LegalDocumentIntent) -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = state.document?.title ?: "",
                onBackClick = { onIntent(LegalDocumentIntent.NavigateBack) }
            )
        },
        containerColor = neubrutalScreenBackground()
    ) { padding ->
        val phase = when {
            state.isLoading -> DocumentLoadPhase.Loading
            state.error != null -> DocumentLoadPhase.Error
            state.document != null -> DocumentLoadPhase.Ready
            else -> DocumentLoadPhase.Loading
        }
        AnimatedContent(
            targetState = phase,
            transitionSpec = { with(ContentStateAnimations) { documentReveal() } },
            label = "legal_document_phase",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { current ->
            when (current) {
                DocumentLoadPhase.Loading -> LoadingIndicator(modifier = Modifier.fillMaxSize())
                DocumentLoadPhase.Error -> EmptyState(
                    title = state.error?.resolveLocal().orEmpty(),
                    description = "",
                    modifier = Modifier.fillMaxSize()
                )
                DocumentLoadPhase.Ready -> {
                    val doc = state.document ?: return@AnimatedContent
                    ScaffoldBottomBarLazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
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
                        if (state.showDeletionForm) {
                            item {
                                AccountDeletionRequestForm(
                                    state = state,
                                    onIntent = onIntent
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountDeletionRequestForm(
    state: LegalDocumentState,
    onIntent: (LegalDocumentIntent) -> Unit
) {
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(Res.string.account_deletion_form_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = neubrutalOnSurface()
        )
        AppTextField(
            value = state.deletionEmail,
            onValueChange = { onIntent(LegalDocumentIntent.UpdateDeletionEmail(it)) },
            label = stringResource(Res.string.account_deletion_email_label),
            placeholder = stringResource(Res.string.account_deletion_email_placeholder),
            keyboardType = KeyboardType.Email,
            enabled = !state.isSubmittingDeletion
        )
        AppTextField(
            value = state.deletionReason,
            onValueChange = { onIntent(LegalDocumentIntent.UpdateDeletionReason(it)) },
            label = stringResource(Res.string.account_deletion_reason_label),
            placeholder = stringResource(Res.string.account_deletion_reason_placeholder),
            singleLine = false,
            maxLines = 4,
            enabled = !state.isSubmittingDeletion
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !state.isSubmittingDeletion) {
                    onIntent(LegalDocumentIntent.SetDeletionConfirmed(!state.deletionConfirmed))
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = state.deletionConfirmed,
                onCheckedChange = { onIntent(LegalDocumentIntent.SetDeletionConfirmed(it)) },
                enabled = !state.isSubmittingDeletion,
                colors = CheckboxDefaults.colors(
                    checkedColor = AccentCoral,
                    checkmarkColor = neubrutalOnSurface()
                )
            )
            Text(
                text = stringResource(Res.string.account_deletion_confirm_label),
                style = MaterialTheme.typography.bodyMedium,
                color = neubrutalOnSurface(),
                modifier = Modifier.weight(1f)
            )
        }
        state.deletionSubmitError?.let { error ->
            Text(
                text = error.resolveLocal(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        state.deletionSubmitSuccessMessage?.let { message ->
            Text(
                text = message.resolveLocal(),
                style = MaterialTheme.typography.bodySmall,
                color = neubrutalOnSurface(),
                fontWeight = FontWeight.Medium
            )
        }
        AppPrimaryButton(
            text = if (state.isSubmittingDeletion) {
                stringResource(Res.string.account_deletion_submitting)
            } else {
                stringResource(Res.string.account_deletion_submit)
            },
            onClick = { onIntent(LegalDocumentIntent.SubmitDeletionRequest) },
            enabled = !state.isSubmittingDeletion
        )
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
