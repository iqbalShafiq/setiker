package presentation.aijob

import data.aijob.AiJobManager
import domain.repository.AiJobRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf

object TestAiJobDependencies {
    fun manager(): AiJobManager = mockk(relaxed = true) {
        every { observeActiveJobCount() } returns flowOf(0)
        coEvery { upsertDraft(any()) } returns Unit
        coEvery { enqueue(any(), any(), any(), any(), any(), any(), any()) } returns mockk(relaxed = true)
    }

    fun enqueueHelper(manager: AiJobManager = manager()): AiJobEnqueueHelper =
        AiJobEnqueueHelper(manager)

    fun draftResultApplier(
        jobRepository: AiJobRepository = mockk(relaxed = true) {
            every { observeByDraft(any()) } returns flowOf(emptyList())
        }
    ): DraftResultApplier = DraftResultApplier(
        draftRepository = mockk(relaxed = true),
        jobRepository = jobRepository
    )
}
