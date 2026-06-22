package data.video

import domain.model.CandidateGridImage
import domain.model.VideoFrameCandidate

class UnsupportedVideoFrameCandidateExtractor(
    private val message: String = "Video sticker pack generation is only available on Android."
) : VideoFrameCandidateExtractor {
    override suspend fun extractCandidates(
        videoPath: String,
        startMs: Long,
        endMs: Long,
        onProgress: (current: Int, total: Int) -> Unit
    ): List<VideoFrameCandidate> {
        error(message)
    }
}

class UnsupportedCandidateGridComposer(
    private val message: String = "Video sticker pack generation is only available on Android."
) : CandidateGridComposer {
    override suspend fun composeGrids(candidatePaths: List<String>): List<CandidateGridImage> {
        error(message)
    }
}
