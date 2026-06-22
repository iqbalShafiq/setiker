# Video Sticker Pack Save Design

## Scope

Fix WhatsApp pack compatibility and video-to-sticker save UX across the existing Kotlin Multiplatform Compose app.

## Requirements

- Never save static and animated stickers into the same WhatsApp sticker pack.
- Video-to-sticker generation may still produce both static and animated stickers in one preview session.
- Saving a mixed video-to-sticker selection creates separate packs: one static pack and one animated pack.
- Pack names and identifiers must be unique by type postfix when split, for example `My Pack` and `My Pack Animated`.
- All generated stickers are selected for saving by default.
- Users can toggle each generated sticker before saving; selected and unselected cards must be visually distinct by border treatment.
- Animated video stickers preview as one card per animated sticker, with the animation playing, not as a row of individual frames.
- Video-to-sticker save loading must show circular progress in the bottom app bar FAB.
- While any screen is in a bottom app bar FAB loading state, all interactive content in that screen must be disabled, including text fields.
- Text fields should move above the software keyboard using IME inset handling.
- Static sticker assets with decorations must follow the editor pattern: persist editable base asset separately from flattened preview asset.
- Animated sticker save should use temporary/generated animated assets and not flatten static decorations directly onto the editable image path.

## Architecture

Keep `StickerPackDraftSaver` as the reusable pack-building boundary, but remove the current behavior that converts static stickers into single-frame animated stickers when the same draft contains animated stickers. Callers that can contain mixed media must split before calling the saver.

Introduce small video-sticker selection state in `VideoStickerPackState` using stable generated item keys. Initialize selection to all generated static and animated items after generation, clear it when video range, prompt, or generated plan changes, and expose `canSave` based on selected items only.

Add save splitting inside `VideoStickerPackViewModel.savePack()`:

- Build selected static draft inputs only for the static pack.
- Build selected animated draft inputs only for the animated pack.
- Save zero, one, or two packs based on selected content.
- Use unique names and identifiers with static/animated postfixes when both types are saved.
- Navigate after successful save to the first saved pack detail, preferring static when present.

## UI Design

Update `VideoStickerPackScreen` to use the existing bottom app bar/FAB components where practical. The FAB receives `isLoading = state.processingStep == Saving` so it shows the circular progress indicator.

Wrap screen content in a reusable disabled-interaction container or apply a screen-level enabled flag to fields, sliders, buttons, and generated sticker cards. Prefer explicit `enabled = !state.isBlockingUi` for controls; use a transparent overlay only if a component cannot be disabled cleanly.

Apply IME inset support to scrollable form screens with bottom actions so focused text fields remain visible above the keyboard. Use existing Compose inset patterns if present; otherwise add `imePadding()` / content padding where the screen owns the scaffold body.

For animated previews, add or reuse a composable that cycles through resolved timeline frame paths according to duration/fps and renders a single `StickerCard`-style preview with combined base and current frame decorations.

## Error Handling

- Disable save when no selected generated sticker exists.
- If animated frame cache is incomplete, keep the existing fallback to decode frames from the source video.
- If saving any selected pack fails, show an error and do not navigate as if successful.
- Preserve existing generate/regenerate error behavior.

## Verification

- Run targeted unit tests for `StickerPackDraftSaver` and `VideoStickerPackViewModel`.
- Run an Android/KMP test task that covers the touched modules if targeted tasks are unclear.
- Inspect relevant UI code paths for enabled/loading/IME behavior.
