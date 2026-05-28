# Android Video Sticker Pack API Contract Plan

## Goal

Align the Android video-to-sticker-pack flow with the backend endpoint that now returns a sticker pack plan instead of generated image files. The Android app should keep doing video upload, trimming, frame extraction, blur/quality filtering, candidate grid composition, preview, regeneration, and local pack creation. The backend should only receive lightweight candidate grids plus metadata, then return which frames and decorations should become static or animated stickers.

## Current State

The app already has a strong first pass for this feature:

- `presentation/videostickerpack/*` contains screen, state, intent, effect, and ViewModel.
- `data/video/AndroidVideoFrameCandidateExtractor.kt` extracts frame candidates and scores brightness, sharpness, and frame difference on device.
- `data/video/AndroidCandidateGridComposer.kt` composes candidate frames into `4x4` grids.
- `domain/model/VideoStickerPackModels.kt` and `domain/util/VideoStickerPackPlanner.kt` define segment limits, candidate limits, and grid batching.
- `SetikerApiService.generateVideoStickerPack(...)` already posts multipart data to `/api/v1/generate/video-sticker-pack`.
- `StickerApiRepository.generateVideoStickerPack(...)` currently expects image output, downloads the first image, and splits it as a `4x4` grid.

The main mismatch is the API contract. The backend now expects `candidateManifest` and returns `data.plan`, not `data.images`.

## Backend Contract To Consume

Endpoint:

- `POST /api/v1/generate/video-sticker-pack`
- Authenticated multipart request.

Files:

- `candidate_grids`: one or two PNG/JPEG/WebP grid images.
- Each grid is `4x4`, with at most 16 cells.
- Total candidates are capped at 32.

Fields:

- `candidateManifest`: JSON array of candidate metadata.
- `layout`: `4x4`.
- `candidateLayout`: `4x4`.
- `selectedStartMs`: selected segment start.
- `selectedEndMs`: selected segment end.
- `sourceDurationMs`: original source video duration.
- `prompt`: optional user direction.
- `maxStaticStickers`: optional, default backend value is fine for MVP.
- `maxAnimatedStickers`: optional, default backend value is fine for MVP.

Candidate manifest item:

- `candidateId`: stable ID, for example `frame_0007`.
- `frameIndex`: extracted frame index in the candidate list.
- `gridIndex`: `0` or `1`.
- `cellId`: `A1` through `D4`.
- `timestampMs`: timestamp in original video.
- `sharpnessScore`
- `brightnessScore`
- `differenceScore`

Response:

- `data.plan.packTitle`
- `data.plan.summary`
- `data.plan.staticStickers[]`
- `data.plan.animatedStickers[]`
- `data.plan.rejectedCandidates[]`
- `data.metadata`

Static sticker item:

- `candidateId`
- `frameIndex`
- `timestampMs`
- `cellId`
- `emojis`
- `accessibilityText`
- `decorations`
- `rationale`

Animated sticker item:

- `timeline[]`, where each frame has `candidateId`, `frameIndex`, `timestampMs`, and `durationMs`.
- `fps`
- `loopCount`
- `emojis`
- `accessibilityText`
- `baseDecorations`
- `frameDecorations`
- `rationale`

Supported decorations for MVP:

- `text`: `text`, optional `style`, `centerX`, `centerY`, `scale`.
- `emoji`: `emoji`, `centerX`, `centerY`, `scale`.

## Implementation Plan

1. Update shared domain models.

- Extend `VideoFrameCandidate` with deterministic `candidateId`, `gridIndex`, and `cellId`, or create a separate `VideoStickerCandidateManifestItem` so extractor output stays platform-neutral.
- Add `VideoStickerPackPlan`, `VideoStaticStickerPlan`, `VideoAnimatedStickerPlan`, `VideoAnimatedTimelineFrame`, and decoration DTO/domain models.
- Keep API response models separate from domain models in `data.remote.model`, then map them into domain objects.

2. Build candidate manifest on Android.

- After extraction and grid composition, create a manifest from the final capped candidates.
- Assign `gridIndex` by chunk index and `cellId` by row/column inside the `4x4` grid.
- Preserve the original `timestampMs`, `sharpnessScore`, `brightnessScore`, and `differenceScore`.
- Ensure `candidateCount == manifest.size == sum(candidateGrid.frameCount)`.

3. Update `SetikerApiService.generateVideoStickerPack`.

- Replace `candidateCount` field usage with `candidateManifest` JSON.
- Keep `layout`, `candidateLayout`, `selectedStartMs`, `selectedEndMs`, `sourceDurationMs`, and optional `prompt`.
- Decode `ApiSuccessEnvelope<VideoStickerPackPlanData>` instead of `ApiSuccessEnvelope<GenerateData>`.
- Use `ignoreUnknownKeys = true` so backend metadata can evolve without breaking older clients.

4. Update `StickerApiRepository.generateVideoStickerPack`.

- Stop downloading/splitting generated images for this endpoint.
- Return a domain `VideoStickerPackPlan` plus enough local candidate lookup data to preview and save.
- Resolve each static sticker candidate to its extracted local frame path.
- Resolve animated timelines to ordered local frame paths by `candidateId`.
- Validate unknown `candidateId` defensively and surface `InvalidGenerateResponse` if the backend references a candidate not sent by the app.

5. Update `VideoStickerPackViewModel`.

- Replace `generatedStickers: List<GridSplitStickerFile>` with plan-aware preview state:
  - static planned stickers with candidate frame local path and decorations.
  - animated planned stickers with timeline frame local paths, durations, fps, loop count, and decorations.
- `Generate` should extract candidates, compose grids, send manifest and grids, then display the returned plan.
- `Regenerate` should reuse the same candidates, grids, and manifest to avoid repeated frame extraction.
- `SavePack` should materialize planned stickers locally:
  - Static stickers: copy/process selected candidate frame, apply decorations if the existing save pipeline supports it, and save as normal sticker inputs.
  - Animated stickers: use the existing animated sticker model/pipeline where possible, or create a follow-up implementation if the current storage/export layer cannot yet persist generated animated sticker specs into WhatsApp-ready assets.

6. Update preview UI.

- Replace generated image grid preview with a plan preview:
  - Static section: show selected candidate frame, frame number/timestamp, emojis, and decoration overlay preview.
  - Animated section: show ordered timeline thumbnails and fps/duration summary.
- Add rejected candidates/debug detail only behind a compact optional UI, not as primary user-facing content.
- Keep progress stages: finding frames, building grids, asking AI, preparing preview, saving.

7. Handle failure and cost efficiency.

- Do not upload raw video to backend.
- Keep max segment at 60 seconds.
- Keep sample count and candidate cap at current values unless tests show UX issues.
- Reuse cached candidates on regenerate.
- Clean up unused extracted frame files after save/cancel, while preserving candidate frames referenced by the final saved pack.
- Surface backend validation errors clearly, especially manifest/grid mismatch and invalid candidate references.

8. Update navigation and entry points only if needed.

- Confirm the existing video picker entry still routes to `VideoStickerPackScreenRoot`.
- Keep current screen structure if it compiles and matches UX.
- Only touch Home/navigation if current flow is unreachable or still points to the older image-output assumption.

9. Tests to add or update.

- `VideoStickerPackPlannerTest`
  - manifest cell assignment.
  - max 32 candidates and max 2 grids.
  - candidate count consistency.
- API model serialization test
  - request includes `candidateManifest`.
  - response parses `plan.staticStickers` and `plan.animatedStickers`.
- `StickerApiRepository` test
  - maps static plan items to local candidate paths.
  - rejects unknown candidate IDs.
  - does not call image download/split for this endpoint.
- `VideoStickerPackViewModelTest`
  - generate sends manifest and shows plan preview.
  - regenerate reuses existing grids/candidates.
  - save converts planned static stickers into draft pack input.

10. Verification commands.

Run from the Android repo:

- `./gradlew :composeApp:compileDebugKotlinAndroid`
- `./gradlew :composeApp:testDebugUnitTest`
- `./gradlew :composeApp:lint`

For iOS/common safety if touched:

- `./gradlew :composeApp:compileKotlinIosSimulatorArm64`
- `./gradlew :composeApp:compileKotlinMetadata`

## Important Implementation Notes

- The old plan in `docs/superpowers/plans/2026-05-18-video-to-sticker-pack.md` assumed the backend would return generated image grids. Treat that part as outdated.
- The backend currently returns instructions, not assets. Android must render or materialize the final sticker assets locally from the candidate frames.
- Keep CPU work on Android bounded: sample frames, downscale for scoring, cap candidates, and never run heavy model inference in the API.
- Do not introduce OpenCV unless the existing sharpness/brightness/difference heuristics are visibly insufficient. The current Android extractor may be enough for MVP.
- Animated sticker persistence is the riskiest part. Before broad UI work, verify whether the existing `AnimatedStickerSpec` and save/export path can create final animated sticker files from selected candidate frame paths.
