package data.aijob

actual class AiBackgroundScheduler {
    actual fun scheduleJob(jobId: String, requiresNetwork: Boolean) {
        // Background AI processing is Android-first; iOS uses in-app execution fallback.
    }

    actual fun cancelJob(jobId: String) = Unit

    actual fun resumeQueuedJobs() = Unit
}
