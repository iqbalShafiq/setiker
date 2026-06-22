package data.video

import domain.model.VideoFrameCandidate

interface VideoFrameCandidateExtractor {
    suspend fun extractCandidates(
        videoPath: String,
        startMs: Long,
        endMs: Long,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): List<VideoFrameCandidate>
}
