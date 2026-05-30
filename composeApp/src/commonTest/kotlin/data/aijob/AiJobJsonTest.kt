package data.aijob

import domain.model.aijob.GenerateStickersPayload
import domain.model.aijob.WorkspaceDraftContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AiJobJsonTest {
    @Test
    fun roundTripWorkspaceDraftContext() {
        val original = WorkspaceDraftContext(
            prompt = "cute cat",
            packName = "Cats",
            publisher = "Me"
        )
        val json = AiJobJson.codec.encodeToString(original)
        val decoded = AiJobJson.codec.decodeFromString<WorkspaceDraftContext>(json)
        assertEquals(original.prompt, decoded.prompt)
        assertEquals(original.packName, decoded.packName)
    }

    @Test
    fun roundTripGenerateStickersPayload() {
        val original = GenerateStickersPayload(prompt = "dragon", inputImagePath = "/tmp/a.png")
        val json = AiJobJson.codec.encodeToString(original)
        val decoded = AiJobJson.codec.decodeFromString<GenerateStickersPayload>(json)
        assertEquals(original.prompt, decoded.prompt)
        assertEquals(original.inputImagePath, decoded.inputImagePath)
    }

    @Test
    fun failureClassifierMarksNetworkErrorsRetryable() {
        val (kind, retryable) = AiJobFailureClassifier.classify(
            IllegalStateException("connection timeout while waiting for response")
        )
        assertTrue(retryable)
        assertEquals(domain.model.aijob.AiJobFailureKind.NETWORK, kind)
    }
}
