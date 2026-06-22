package presentation.aijobs



import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.PaddingValues

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.items

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.automirrored.filled.ArrowBack

import androidx.compose.material.icons.filled.Refresh

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.runtime.collectAsState

import androidx.compose.runtime.getValue

import androidx.compose.ui.Modifier

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.unit.dp

import domain.model.aijob.AiJobStatus

import domain.model.aijob.WorkspaceDraft

import domain.model.aijob.WorkspaceDraftStatus

import org.jetbrains.compose.resources.stringResource

import org.koin.compose.viewmodel.koinViewModel
import presentation.common.resolveOrDefault
import presentation.components.AppIllustration
import presentation.components.AppTopBar

import presentation.components.DraftJobListCard

import presentation.components.EmptyState

import presentation.components.LoadingIndicator

import presentation.components.PackBottomBar

import presentation.components.PackBottomBarFab
import presentation.components.PackBottomBarIconButton

import presentation.theme.neubrutalMutedOnSurface

import presentation.theme.neubrutalOnSurface

import presentation.theme.neubrutalScreenBackground

import setiker.composeapp.generated.resources.Res

import setiker.composeapp.generated.resources.ai_jobs_empty_desc
import setiker.composeapp.generated.resources.back_content_description

import setiker.composeapp.generated.resources.ai_jobs_empty_title

import setiker.composeapp.generated.resources.ai_jobs_running_status

import setiker.composeapp.generated.resources.ai_jobs_section_cancelled

import setiker.composeapp.generated.resources.ai_jobs_section_needs_attention

import setiker.composeapp.generated.resources.ai_jobs_section_other

import setiker.composeapp.generated.resources.ai_jobs_section_ready

import setiker.composeapp.generated.resources.ai_jobs_section_running

import setiker.composeapp.generated.resources.ai_jobs_section_saved

import setiker.composeapp.generated.resources.ai_jobs_status_cancelled

import setiker.composeapp.generated.resources.ai_jobs_status_draft

import setiker.composeapp.generated.resources.ai_jobs_status_processing

import setiker.composeapp.generated.resources.ai_jobs_status_ready

import setiker.composeapp.generated.resources.ai_jobs_status_saved

import setiker.composeapp.generated.resources.ai_jobs_clear_completed
import setiker.composeapp.generated.resources.ai_jobs_title



@Composable

fun AiJobsScreenRoot(

    onBackClick: () -> Unit,

    onOpenDraft: (String, String?) -> Unit,

    onOpenPack: (String) -> Unit,

    viewModel: AiJobsViewModel = koinViewModel()

) {

    val state by viewModel.state.collectAsState()

    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                AiJobsEffect.NavigateBack -> onBackClick()
                is AiJobsEffect.NavigateToDraft -> onOpenDraft(effect.draftId, effect.originRoute)
                is AiJobsEffect.NavigateToPack -> onOpenPack(effect.packId)
                is AiJobsEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message.resolveOrDefault())
            }
        }
    }

    AiJobsScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBackClick = onBackClick,
        snackbarHostState = snackbarHostState
    )
}



@Composable

fun AiJobsScreen(

    state: AiJobsState,

    onIntent: (AiJobsIntent) -> Unit,

    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState,

    modifier: Modifier = Modifier

) {

    val grouped = groupDrafts(state.drafts, state.jobs)

    Scaffold(

        topBar = {

            AppTopBar(
                title = stringResource(Res.string.ai_jobs_title)
            )

        },

        bottomBar = {

            PackBottomBar(

                actionStatusText = if (state.activeJobCount > 0) {

                    stringResource(Res.string.ai_jobs_running_status, state.activeJobCount)

                } else {

                    null

                },

                actions = {
                    PackBottomBarIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back_content_description),
                        onClick = onBackClick
                    )
                },

                floatingActionButton = {

                    PackBottomBarFab(

                        icon = Icons.Default.Refresh,

                        contentDescription = stringResource(Res.string.ai_jobs_clear_completed),

                        onClick = { onIntent(AiJobsIntent.ClearCompleted) },

                        enabled = true,

                        isLoading = false

                    )

                }

            )

        },
        snackbarHost = { SnackbarHost(snackbarHostState) },

        containerColor = neubrutalScreenBackground(),

        modifier = modifier

    ) { padding ->

        when {

            state.isLoading -> LoadingIndicator(

                modifier = Modifier.fillMaxSize().padding(padding),
                illustration = AppIllustration.LoadingState

            )

            state.drafts.isEmpty() -> EmptyState(

                title = stringResource(Res.string.ai_jobs_empty_title),

                description = stringResource(Res.string.ai_jobs_empty_desc),
                illustration = AppIllustration.AiJobs,

                modifier = Modifier.fillMaxSize().padding(padding)

            )

            else -> LazyColumn(

                modifier = Modifier.fillMaxSize().padding(padding),

                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),

                verticalArrangement = Arrangement.spacedBy(12.dp)

            ) {
                grouped.forEach { section ->

                    item(key = "header_${section.titleKey}") {

                        Text(

                            text = section.title,

                            style = MaterialTheme.typography.titleMedium,

                            fontWeight = FontWeight.Bold,

                            color = neubrutalOnSurface(),

                            modifier = Modifier

                                .fillMaxWidth()

                                .padding(top = 4.dp, bottom = 4.dp)

                        )

                    }

                    items(

                        section.items,

                        key = { item -> "${section.titleKey}_${item.draft.id}" }

                    ) { item ->

                        val latestJob = item.latestJob

                        val showProgress = latestJob?.progress != null &&

                            latestJob.status in ACTIVE_JOB_STATUSES

                        DraftJobListCard(

                            draft = item.draft,

                            latestJob = latestJob,

                            statusLabel = statusLabel(item.draft, latestJob),

                            showProgress = showProgress,

                            canRetry = canRetry(item.draft, latestJob),

                            onOpen = { onIntent(AiJobsIntent.OpenDraft(item.draft.id)) },

                            onRetry = { onIntent(AiJobsIntent.RetryDraft(item.draft.id)) },

                            onDelete = { onIntent(AiJobsIntent.DeleteDraft(item.draft.id)) },

                            onCancel = latestJob?.id?.takeIf {

                                latestJob.status in ACTIVE_JOB_STATUSES

                            }?.let { jobId ->

                                { onIntent(AiJobsIntent.CancelJob(jobId)) }

                            }

                        )

                    }

                }

            }

        }

    }

}



private data class DraftSection(

    val titleKey: String,

    val title: String,

    val items: List<DraftJobItem>

)



private data class DraftJobItem(

    val draft: WorkspaceDraft,

    val latestJob: domain.model.aijob.AiJob?

)



private val ACTIVE_JOB_STATUSES = setOf(

    AiJobStatus.QUEUED,

    AiJobStatus.RUNNING,

    AiJobStatus.WAITING_FOR_NETWORK,

    AiJobStatus.CHECKPOINTED,

    AiJobStatus.CANCEL_REQUESTED

)



@Composable

private fun statusLabel(draft: WorkspaceDraft, job: domain.model.aijob.AiJob?): String {

    job?.progress?.stepLabel?.takeIf { it.isNotBlank() }?.let { return it }

    return when (draft.status) {

        WorkspaceDraftStatus.PROCESSING -> stringResource(Res.string.ai_jobs_status_processing)

        WorkspaceDraftStatus.READY_TO_REVIEW -> stringResource(Res.string.ai_jobs_status_ready)

        WorkspaceDraftStatus.NEEDS_ATTENTION -> job?.failureMessage

            ?: stringResource(Res.string.ai_jobs_section_needs_attention)

        WorkspaceDraftStatus.APPLIED -> stringResource(Res.string.ai_jobs_status_saved)

        WorkspaceDraftStatus.CANCELLED -> stringResource(Res.string.ai_jobs_status_cancelled)

        WorkspaceDraftStatus.ACTIVE -> stringResource(Res.string.ai_jobs_status_draft)

    }

}



private fun canRetry(draft: WorkspaceDraft, job: domain.model.aijob.AiJob?): Boolean {

    return draft.status == WorkspaceDraftStatus.NEEDS_ATTENTION ||

        job?.status == AiJobStatus.FAILED_RETRYABLE ||

        job?.status == AiJobStatus.FAILED_FINAL

}



@Composable

private fun groupDrafts(

    drafts: List<WorkspaceDraft>,

    jobs: List<domain.model.aijob.AiJob>

): List<DraftSection> {

    val jobsByDraft = jobs.groupBy { it.workspaceDraftId }

    fun latestJob(draftId: String) = jobsByDraft[draftId]?.maxByOrNull { it.updatedAt }

    val items = drafts.map { DraftJobItem(it, latestJob(it.id)) }

    val assigned = mutableSetOf<String>()



    fun take(predicate: (DraftJobItem) -> Boolean): List<DraftJobItem> =

        items.filter { predicate(it) && assigned.add(it.draft.id) }



    val activeJobStatuses = ACTIVE_JOB_STATUSES

    val failedJobStatuses = setOf(AiJobStatus.FAILED_FINAL, AiJobStatus.FAILED_RETRYABLE)



    val running = take {

        it.draft.status == WorkspaceDraftStatus.PROCESSING ||

            it.latestJob?.status in activeJobStatuses

    }

    val needsAttention = take {

        it.draft.status == WorkspaceDraftStatus.NEEDS_ATTENTION ||

            it.latestJob?.status in failedJobStatuses

    }

    val ready = take { it.draft.status == WorkspaceDraftStatus.READY_TO_REVIEW }

    val saved = take { it.draft.status == WorkspaceDraftStatus.APPLIED }

    val cancelled = take {

        it.draft.status == WorkspaceDraftStatus.CANCELLED ||

            it.latestJob?.status == AiJobStatus.CANCELLED

    }

    val other = take { true }



    return buildList {

        if (running.isNotEmpty()) {

            add(

                DraftSection(

                    titleKey = "running",

                    title = stringResource(Res.string.ai_jobs_section_running),

                    items = running

                )

            )

        }

        if (needsAttention.isNotEmpty()) {

            add(

                DraftSection(

                    titleKey = "needs_attention",

                    title = stringResource(Res.string.ai_jobs_section_needs_attention),

                    items = needsAttention

                )

            )

        }

        if (ready.isNotEmpty()) {

            add(

                DraftSection(

                    titleKey = "ready",

                    title = stringResource(Res.string.ai_jobs_section_ready),

                    items = ready

                )

            )

        }

        if (saved.isNotEmpty()) {

            add(

                DraftSection(

                    titleKey = "saved",

                    title = stringResource(Res.string.ai_jobs_section_saved),

                    items = saved

                )

            )

        }

        if (cancelled.isNotEmpty()) {

            add(

                DraftSection(

                    titleKey = "cancelled",

                    title = stringResource(Res.string.ai_jobs_section_cancelled),

                    items = cancelled

                )

            )

        }

        if (other.isNotEmpty()) {

            add(

                DraftSection(

                    titleKey = "other",

                    title = stringResource(Res.string.ai_jobs_section_other),

                    items = other

                )

            )

        }

    }

}
