package domain.model.aijob

import domain.model.ResolvedVideoAnimatedSticker
import domain.model.ResolvedVideoStaticSticker
import domain.model.ResolvedVideoStickerPackPlan
import domain.model.VideoAnimatedStickerPlan
import domain.model.VideoAnimatedTimelineFrame
import domain.model.VideoStickerPackPlan
import domain.model.VideoStaticStickerPlan
import kotlinx.serialization.Serializable

@Serializable
data class StoredVideoPackPlan(
    val plan: VideoStickerPackPlanSnapshot,
    val staticStickers: List<StoredResolvedStaticSticker> = emptyList(),
    val animatedStickers: List<StoredResolvedAnimatedSticker> = emptyList()
)

@Serializable
data class VideoStickerPackPlanSnapshot(
    val packTitle: String,
    val summary: String? = null
)

@Serializable
data class StoredResolvedStaticSticker(
    val plan: VideoStaticStickerPlanSnapshot,
    val localPath: String
)

@Serializable
data class VideoStaticStickerPlanSnapshot(
    val candidateId: String,
    val frameIndex: Int,
    val timestampMs: Long,
    val cellId: String,
    val emojis: List<String> = emptyList(),
    val accessibilityText: String? = null
)

@Serializable
data class StoredResolvedAnimatedSticker(
    val plan: VideoAnimatedStickerPlanSnapshot,
    val timeline: List<StoredResolvedAnimatedTimelineFrame>
)

@Serializable
data class VideoAnimatedStickerPlanSnapshot(
    val fps: Int,
    val loopCount: Int,
    val emojis: List<String> = emptyList(),
    val accessibilityText: String? = null
)

@Serializable
data class StoredResolvedAnimatedTimelineFrame(
    val candidateId: String?,
    val frameIndex: Int,
    val timestampMs: Long,
    val durationMs: Long,
    val localPath: String
)

fun ResolvedVideoStickerPackPlan.toStored(): StoredVideoPackPlan = StoredVideoPackPlan(
    plan = VideoStickerPackPlanSnapshot(
        packTitle = plan.packTitle,
        summary = plan.summary
    ),
    staticStickers = staticStickers.map { sticker ->
        StoredResolvedStaticSticker(
            plan = VideoStaticStickerPlanSnapshot(
                candidateId = sticker.plan.candidateId,
                frameIndex = sticker.plan.frameIndex,
                timestampMs = sticker.plan.timestampMs,
                cellId = sticker.plan.cellId,
                emojis = sticker.plan.emojis,
                accessibilityText = sticker.plan.accessibilityText
            ),
            localPath = sticker.localPath
        )
    },
    animatedStickers = animatedStickers.map { sticker ->
        StoredResolvedAnimatedSticker(
            plan = VideoAnimatedStickerPlanSnapshot(
                fps = sticker.plan.fps,
                loopCount = sticker.plan.loopCount,
                emojis = sticker.plan.emojis,
                accessibilityText = sticker.plan.accessibilityText
            ),
            timeline = sticker.timeline.map { frame ->
                StoredResolvedAnimatedTimelineFrame(
                    candidateId = frame.frame.candidateId,
                    frameIndex = frame.frame.frameIndex,
                    timestampMs = frame.frame.timestampMs,
                    durationMs = frame.frame.durationMs,
                    localPath = frame.localPath
                )
            }
        )
    }
)

fun StoredVideoPackPlan.toResolved(): ResolvedVideoStickerPackPlan {
    val static = staticStickers.map { stored ->
        ResolvedVideoStaticSticker(
            plan = VideoStaticStickerPlan(
                candidateId = stored.plan.candidateId,
                frameIndex = stored.plan.frameIndex,
                timestampMs = stored.plan.timestampMs,
                cellId = stored.plan.cellId,
                emojis = stored.plan.emojis,
                accessibilityText = stored.plan.accessibilityText
            ),
            localPath = stored.localPath
        )
    }
    val animated = animatedStickers.map { stored ->
        ResolvedVideoAnimatedSticker(
            plan = VideoAnimatedStickerPlan(
                timeline = stored.timeline.map {
                    VideoAnimatedTimelineFrame(
                        candidateId = it.candidateId,
                        frameIndex = it.frameIndex,
                        timestampMs = it.timestampMs,
                        durationMs = it.durationMs
                    )
                },
                fps = stored.plan.fps,
                loopCount = stored.plan.loopCount,
                emojis = stored.plan.emojis,
                accessibilityText = stored.plan.accessibilityText
            ),
            timeline = stored.timeline.map {
                domain.model.ResolvedVideoAnimatedTimelineFrame(
                    frame = VideoAnimatedTimelineFrame(
                        candidateId = it.candidateId,
                        frameIndex = it.frameIndex,
                        timestampMs = it.timestampMs,
                        durationMs = it.durationMs
                    ),
                    localPath = it.localPath
                )
            }
        )
    }
    return ResolvedVideoStickerPackPlan(
        plan = VideoStickerPackPlan(
            packTitle = plan.packTitle,
            summary = plan.summary,
            staticStickers = static.map { it.plan },
            animatedStickers = animated.map { it.plan }
        ),
        staticStickers = static,
        animatedStickers = animated
    )
}
