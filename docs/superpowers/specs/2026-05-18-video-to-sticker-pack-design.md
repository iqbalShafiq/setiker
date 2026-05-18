# Video To Sticker Pack Design

## Goal

Add an Android-first `Video to Sticker Pack` flow that lets a user pick any local video, choose a maximum 60-second segment, generate candidate frames on-device, send those candidates to the API as one or two `4x4` grid images, and receive one AI-selected/generated `4x4` sticker-pack grid back from the API.

The feature should feel like a fast pack-generation flow, not a technical video processing tool. The user should be guided through trimming, candidate extraction, generation, preview, regeneration, and saving.

## Scope

### In Scope

- Android MVP only.
- Home entry point for `Video to Sticker Pack`.
- Android video picker reuse via existing `rememberVideoPicker` primitives.
- Trim UI that accepts long videos but enforces a selected range of at most 60 seconds.
- On-device frame sampling and lightweight candidate filtering.
- Candidate grid creation as PNG files:
  - `4x4` layout.
  - Maximum 32 candidate frames.
  - Maximum 2 input grid images.
- New API endpoint contract for AI Agent video sticker-pack generation.
- API response as one `4x4` output grid image with up to 16 sticker cells.
- App-side output grid splitting and preview before final save.
- Regenerate from the same candidate grid images without re-sampling video.
- Save generated result as a sticker pack only after user confirmation.

### Out Of Scope For MVP

- iOS implementation.
- Uploading raw video to the API.
- Server-side video decoding.
- User editing individual candidate frames before API generation.
- Re-sampling different frames during regenerate.
- Advanced semantic uniqueness using an on-device ML embedding model.
- Background queue processing after the user leaves the flow.

## Existing Project Context

The Setiker app is a Kotlin Multiplatform Compose app using MVI-style presentation state, intents, effects, Koin, Ktor multipart APIs, and existing sticker-pack draft saving utilities.

Relevant existing app pieces:

- `presentation.home.HomeScreen` already has Home bottom-bar generation actions.
- `presentation.components.rememberVideoPicker` exists in common code with Android actual implementation.
- `StickerFileStorage.getVideoDurationMs()` and `extractVideoFrameToFile()` already exist on Android.
- Android video preview/decode code already uses `MediaMetadataRetriever` and Media3 primitives.
- `StickerApiRepository.generateStickerPack()` already downloads one generated grid and splits it on-device.
- `StickerPackDraftSaver` centralizes pack creation rules.

Relevant API pieces in `C:\Users\iqbal.shafiq\VsCodeProjects\stiker-api`:

- `GenerateController` already owns AI generation endpoints.
- `OpenRouterService` already supports multimodal image inputs and agent prompt planning.
- `/api/v1/generate/improvement` already handles image inputs and uses an improvement agent.
- `GridSplitService` already handles grid splitting, text analysis, and background removal for grid cells.

## Recommended Architecture

Use a split responsibility model:

- The Android app handles video selection, trimming, frame extraction, simple quality filtering, grid composition, result preview, and local pack saving.
- The API handles AI Agent reasoning, frame selection, generation/improvement, and final output grid creation.

This keeps bandwidth small because the app uploads one or two candidate grid PNG files instead of a full video. It also keeps the API focused on AI decisions, which matches the product direction.

## User Flow

1. User taps `Video to Sticker Pack` from Home.
2. App opens Android video picker.
3. User selects a video.
4. App opens a trim screen.
5. If video duration is more than 60 seconds, the screen asks the user to choose a 60-second-or-shorter segment.
6. User confirms the segment.
7. App extracts candidate frames from that segment.
8. App shows progress while extracting and preparing candidates.
9. App builds one or two candidate grid images.
10. App sends the candidate grids to the API Agent endpoint.
11. API returns one `4x4` output grid image.
12. App splits output grid into sticker previews.
13. User sees a result preview with actions:
    - `Save Pack`
    - `Regenerate`
    - `Cancel`
14. If user taps `Regenerate`, app calls the same endpoint again using the same candidate grid images.
15. If user taps `Save Pack`, app persists the generated stickers as a new sticker pack and opens pack detail.

## UX Requirements

The flow should keep the existing neubrutal/clay visual language:

- Strong bordered cards and buttons.
- Clear bold headings.
- Warm screen background.
- High-contrast action states.
- Friendly copy, not technical pipeline wording.

### Home Entry

Add a distinct action labeled `Video to Sticker Pack`. It should not replace the existing manual Create Pack or AI sticker-pack prompt action.

### Trim Screen

The trim screen should communicate:

- Source video duration.
- Selected start and end time.
- Selected segment duration.
- A visible `Max 60s` rule.
- Disabled or corrective behavior when the selected segment exceeds 60 seconds.

The user may upload videos longer than 1 minute. The app only enforces the selected segment length, not the source file duration.

### Processing Screen

The processing state should show meaningful progress stages:

- `Reading video`
- `Finding clear frames`
- `Building candidate grids`
- `Asking AI Agent`
- `Preparing sticker preview`

### Result Preview

The result preview should feel like a generated pack ready for confirmation. It should show the split sticker results in a grid and provide `Save Pack`, `Regenerate`, and `Cancel` actions.

The generated pack is not saved to the library until the user taps `Save Pack`.

## Frame Sampling And Filtering

The app should sample frames uniformly across the selected segment, then filter down to at most 32 candidates.

Recommended MVP approach:

- Extract more raw samples than needed, for example 48 to 64 frames depending on selected duration.
- For each frame, compute lightweight image quality signals on Android:
  - blur/sharpness score using a simple luminance edge or Laplacian-style heuristic.
  - brightness score to reject very dark or blown-out frames.
  - near-duplicate score against the previous accepted frame using downscaled average color or block difference.
- Prefer frames that are sharp, not extreme in brightness, and not too similar to recently accepted frames.
- Stop at 32 candidates.
- If fewer than 32 pass filters, fill with the best remaining extracted frames so the flow still works.

This is intentionally heuristic. Semantic uniqueness belongs to the API Agent in this MVP.

## Candidate Grid Format

Candidate grids are app-generated PNG images.

- Layout: `4x4`.
- Cell count per grid: 16.
- Max candidate frames: 32.
- Max grid images uploaded: 2.
- Each frame should be center-cropped or fitted into a square cell consistently.
- Cells should have clear gutters or index labels if needed for API reasoning.

Recommended grid details:

- Square output image, preferably `1024x1024` or `1536x1536` so each cell remains legible to the multimodal model.
- Small visual cell numbers may be included in a corner if the API Agent will refer to selected cells in metadata.
- Do not apply sticker background removal to candidate frames before upload; the API Agent benefits from source context.

## API Endpoint Design

Add a new backend endpoint under generation:

`POST /api/v1/generate/video-sticker-pack`

Multipart request fields:

- `candidate_grids`: one or two image files, each a `4x4` candidate grid PNG.
- `layout`: expected output layout, default `4x4`.
- `candidateLayout`: input candidate grid layout, default `4x4`.
- `candidateCount`: total candidate frames represented across all grids, max `32`.
- `selectedStartMs`: selected segment start timestamp.
- `selectedEndMs`: selected segment end timestamp.
- `sourceDurationMs`: full source video duration when known.
- `prompt`: optional user style prompt. MVP may omit UI for this and send a default instruction.

Validation:

- Require at least one `candidate_grids` file.
- Allow at most two `candidate_grids` files.
- Require `candidateCount` between 1 and 32.
- Require selected range length to be more than 0 and at most 60,000 ms.
- Require output `layout` to resolve to `4x4` for MVP.

Response:

- Reuse existing success envelope shape.
- `data.images[0]` should be one output grid image.
- `data.metadata` should include:
  - `mode: "video-sticker-pack"`
  - `candidateGridCount`
  - `candidateCount`
  - `inputLayout: "4x4"`
  - `outputLayout: "4x4"`
  - `maxOutputCells: 16`
  - `agentModel`
  - `imageGenerationModel`
  - optional `selectionReasoning`

## API Agent Behavior

The API Agent should analyze the candidate grid images and choose frames that are:

- visually clear,
- not blurry,
- expressive,
- distinct from each other,
- suitable as standalone WhatsApp stickers,
- diverse across the selected segment.

The image generation step should produce one cohesive `4x4` sticker grid with up to 16 final sticker concepts. The generated stickers should have safe margins, strong subject separation, readable captions where appropriate, and sticker-ready composition.

For MVP, the app does not need to parse the Agent's selected cell IDs. The final output grid is the source of truth.

## Mobile Data Flow

Suggested app modules/classes:

- `VideoStickerPackState`, `VideoStickerPackIntent`, `VideoStickerPackEffect`.
- `VideoStickerPackViewModel` to orchestrate trim, extraction, upload, preview, regeneration, and save.
- `VideoFrameCandidateExtractor` common interface with Android implementation.
- `CandidateGridComposer` common interface with Android implementation if bitmap APIs are platform-specific.
- `StickerApiRepository.generateVideoStickerPack(...)` to call the new API endpoint and split output grid.

Data flow:

1. Picker returns local video path.
2. ViewModel loads duration through `StickerFileStorage.getVideoDurationMs()`.
3. User confirms selected segment.
4. Extractor returns candidate frame file paths and scores.
5. Grid composer returns one or two grid image paths.
6. Repository uploads grid images to API.
7. Repository downloads output grid and splits via existing `splitGridOnDevice(rawGridPath, "4x4")`.
8. ViewModel maps split files to preview draft stickers.
9. Save action uses `StickerPackDraftSaver` and `StickerRepository.savePack()`.

## Regenerate Behavior

Regenerate must reuse the same candidate grid image files generated from the first extraction.

It should not:

- reopen the video picker,
- re-trim the video,
- re-extract frames,
- recompose candidate grids.

This keeps regenerate fast and predictable. A future V2 can add `Pick Different Frames` to re-sample.

## Error Handling

Handle these user-facing failures:

- Video cannot be read.
- Video duration cannot be detected.
- Selected segment is longer than 60 seconds.
- No usable frames can be extracted.
- Candidate grid composition fails.
- API request fails.
- API response contains no output grid.
- Output grid split returns no stickers.
- Pack save fails.

Use existing `UiText` and `toUiText` patterns where possible. Keep error copy specific and actionable.

## Testing Strategy

Unit tests should cover:

- selected segment validation (`<= 60_000 ms`).
- candidate count capping at 32.
- grid batching into one or two `4x4` grids.
- regenerate reuses existing candidate grids.
- ViewModel save flow does not persist until `Save Pack`.
- API repository multipart request includes the expected fields and files if test infrastructure allows.

Backend tests should cover:

- validator rejects missing candidate grids.
- validator rejects more than 2 candidate grids.
- validator rejects candidate count greater than 32.
- validator rejects selected range longer than 60 seconds.
- controller returns one generated grid image and metadata.
- OpenRouter agent prompt includes frame-selection and uniqueness criteria.

## Implementation Decisions

- Candidate grid size is `1536x1536` so every `4x4` cell remains legible to the multimodal model.
- Candidate cells include subtle labels from `A1` to `D4`. The labels are small, high-contrast, and placed in a corner with enough padding to avoid covering the main subject.
- MVP does not expose a user style prompt field. The app sends a default prompt focused on frame diversity, clarity, expression, and WhatsApp sticker readiness.
- Android frame extraction starts from the existing `StickerFileStorage.extractVideoFrameToFile()` primitive. Media3 upgrade is allowed during implementation only if the extractor needs newer public APIs; it is not a prerequisite for the MVP design.
