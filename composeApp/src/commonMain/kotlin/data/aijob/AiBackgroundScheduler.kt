package data.aijob

expect class AiBackgroundScheduler {
    fun scheduleJob(jobId: String, requiresNetwork: Boolean)
    fun cancelJob(jobId: String)
    fun resumeQueuedJobs()
}
