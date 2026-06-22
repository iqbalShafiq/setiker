package domain.model.aijob

import kotlinx.serialization.Serializable

@Serializable
data class AnimatedFrameSnapshot(
    val filePath: String,
    val durationMs: Long
)
