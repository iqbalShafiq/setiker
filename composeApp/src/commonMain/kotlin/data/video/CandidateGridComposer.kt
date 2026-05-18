package data.video

import domain.model.CandidateGridImage

interface CandidateGridComposer {
    suspend fun composeGrids(candidatePaths: List<String>): List<CandidateGridImage>
}
