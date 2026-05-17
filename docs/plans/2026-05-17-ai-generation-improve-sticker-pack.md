# AI Improve And Sticker Pack Generation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add AI Improve for the sticker editor and sticker-pack editor, add Home AI sticker-pack generation, and preserve optional API text decoration output end-to-end.

**Architecture:** Refactor generation into one API result model that carries `localPath` plus optional `StickerDecoration`. Make API text assets first-class decoration metadata instead of relying on ID prefixes, centralize decoration render semantics, keep UI flows in existing MVI screens, reuse the existing generate bottom sheet style, reuse on-device grid splitting for sticker-pack grid outputs, and extract shared pack-saving logic so Home generation does not duplicate Create Pack persistence rules.

**Tech Stack:** Kotlin Multiplatform, Jetpack Compose, Koin, Ktor multipart, existing `StickerApiRepository`, existing `OnDeviceImageProcessor`, existing MVI `State`/`Intent`/`ViewModel`/`Effect` files.

---

## API Facts Confirmed From `/Users/shafiq/VsCodeProjects/stiker-api`

The OpenAPI and controller currently define these endpoints:

- `/api/v1/generate`: multipart `text` required, optional single `image`. It rejects old fields `grid`, `rows`, `cols`, `layout`, `split`, and `normalize`.
- `/api/v1/generate/sticker-pack`: multipart `text` required, optional single `image`, and `layout` or `rows` plus `cols` required by backend validator. It returns one 512x512 grid image in `data.images`.
- `/api/v1/generate/improvement`: multipart array field `images` required. One input returns one sticker image and may include `textAssetDecoration`. Multiple inputs are chunked into 4x4 grid images, max 16 inputs per output image.
- The route name in API is `/api/v1/generate/improvement`, not `/generate/improve`. Use `/api/v1/generate/improvement` unless the backend is changed.
- Image results may include `textAssetDecoration` and/or `textOutsideForeground`. Existing app only models `textOutsideForeground`, so `textAssetDecoration` must be added.

---

## Design Decisions

- Use the label `Improve` in UI because it matches API naming and is shorter than `Enhance`.
- Keep `/api/v1/generate` simple. It sends only `text` and optional `image`.
- Add a new Home bottom-bar action with `AutoAwesome` for AI pack generation. Keep the existing FAB for manual Create Pack.
- Home AI pack generation sheet collects `pack name`, `publisher`, `prompt`, optional reference image, and `layout` because backend requires layout for sticker-pack generation.
- Use `4x4` as the default Home pack layout because it matches the improve grid contract and current grid-split defaults.
- Home generated packs are saved directly after API returns and grid split finishes. The tray icon is derived from the first generated sticker.
- Create Pack improve replaces current draft stickers after a confirmation sheet, not silently. Generated sticker additions keep the existing “select results then add” behavior.
- Editor improve reuses the existing generated-result confirmation sheet and applies the improved sticker image plus optional decorations only after user confirms.
- API text decorations must not be detected by `id.startsWith("api_txt_")`. Add explicit domain fields for source/layout so future API decoration types can be added without rewriting every renderer.
- Both `textAssetDecoration` and `textOutsideForeground` must flow through one API decoration mapper and one render-spec helper, then work consistently in editor preview, read-only thumbnails, Android export compositing, and persistence.
- Do not commit during implementation unless the user explicitly asks.

---

### Task 1: Fix And Extend Remote API Contract

**Files:**

- Modify: `composeApp/src/commonMain/kotlin/data/remote/SetikerApiService.kt`
- Modify: `composeApp/src/commonMain/kotlin/data/remote/model/ApiEnvelope.kt`
- Modify: `composeApp/src/commonMain/kotlin/data/remote/model/StickerApiModels.kt`

**Step 1: Add API model for shared text decoration output**

Add a serializable model beside existing `ApiTextOutsideForeground`:

```kotlin
@Serializable
data class ApiTextAssetDecoration(
    val text: String? = null,
    val style: ApiTextOutsideForegroundStyle? = null,
    val source: String? = null
)

@Serializable
data class ApiImage(
    val id: String,
    val url: String,
    val width: Int? = null,
    val height: Int? = null,
    val textAssetDecoration: ApiTextAssetDecoration? = null,
    val textOutsideForeground: ApiTextOutsideForeground? = null
)
```

**Step 2: Refactor `/generate` multipart body without breaking call sites yet**

Keep the current `SetikerApiService.generate` parameters temporarily so existing callers still compile, but ignore the old grid-related parameters in the request body:

```kotlin
suspend fun generate(
    prompt: String,
    grid: Boolean = false,
    layout: String? = null,
    normalize: Boolean? = null,
    inputImagePath: String? = null,
    splitGridOnServer: Boolean = false
): List<ApiImage>
```

The form body must append only `text` and optional `image`.

Expected behavior: no `grid`, `layout`, `normalize`, or `split` fields are sent.

**Step 3: Add `/generate/sticker-pack` service method**

Add:

```kotlin
suspend fun generateStickerPack(
    prompt: String,
    layout: String,
    inputImagePath: String? = null
): List<ApiImage>
```

The form body must append `text`, `layout`, and optional `image`.

**Step 4: Add `/generate/improvement` service method**

Add:

```kotlin
suspend fun improve(
    imagePaths: List<String>
): List<ApiImage>
```

The form body must append each file using key `images`.

**Step 5: Compile targeted source set**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`

Expected: BUILD SUCCESSFUL.

---

### Task 2: Centralize Scalable Text Asset Decoration Semantics

**Files:**

- Create: `composeApp/src/commonMain/kotlin/data/remote/model/GeneratedStickerFile.kt`
- Create: `composeApp/src/commonMain/kotlin/data/remote/mapper/ApiDecorationMapper.kt`
- Modify: `composeApp/src/commonMain/kotlin/domain/model/StickerDecoration.kt`
- Modify: `composeApp/src/commonMain/kotlin/domain/model/DecorationRenderSpec.kt`
- Modify: `composeApp/src/commonMain/kotlin/data/remote/mapper/TextOutsideForegroundMapper.kt` or delete after migration
- Modify: `composeApp/src/commonMain/kotlin/presentation/components/DecorationPreviewLayer.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/components/ReadOnlyDecorationOverlay.kt`
- Modify: `composeApp/src/androidMain/kotlin/data/storage/AndroidStickerFileStorage.kt`
- Test: `composeApp/src/commonTest/kotlin/data/remote/mapper/ApiDecorationMapperTest.kt`
- Test: `composeApp/src/commonTest/kotlin/domain/model/DecorationRenderSpecTest.kt`

**Step 1: Add neutral generated file model**

Create:

```kotlin
package data.remote.model

import domain.model.StickerDecoration

data class GeneratedStickerFile(
    val localPath: String,
    val decorations: List<StickerDecoration> = emptyList()
)
```

**Step 2: Add explicit text decoration metadata**

Extend `StickerDecoration.kt` with explicit source and layout enums. Defaults preserve persisted JSON compatibility for existing manual text decorations.

```kotlin
@Serializable
enum class TextDecorationSource {
    User,
    ApiTextAsset,
    ApiOutsideForeground
}

@Serializable
enum class TextDecorationLayout {
    Freeform,
    BottomCaption
}

@Serializable
@SerialName("text")
data class TextDecoration(
    override val id: String,
    val text: String,
    val font: DecorationFont,
    val fontWeight: DecorationFontWeight = DecorationFontWeight.Regular,
    val textColorArgb: Long = 0xFFFFFFFFL,
    val source: TextDecorationSource = TextDecorationSource.User,
    val layout: TextDecorationLayout = TextDecorationLayout.Freeform,
    override val centerX: Float = 0.5f,
    override val centerY: Float = 0.5f,
    override val scale: Float = 1f
) : StickerDecoration
```

**Step 3: Add central render helpers**

Add helpers in `DecorationRenderSpec.kt` so all renderers use the same rule:

```kotlin
fun TextDecoration.isBottomCaption(): Boolean =
    layout == TextDecorationLayout.BottomCaption

fun textBoxWidthPx(
    decoration: TextDecoration,
    canvasWidthPx: Float,
    minDimPx: Float,
    scale: Float
): Float = if (decoration.isBottomCaption()) {
    canvasWidthPx * (1f - 2f * API_CAPTION_HORIZONTAL_INSET_RATIO)
} else {
    minDimPx * TEXT_BOX_RATIO * scale
}

fun textBoxHeightPx(
    decoration: TextDecoration,
    minDimPx: Float,
    scale: Float
): Float = if (decoration.isBottomCaption()) {
    minDimPx * 0.46f * scale
} else {
    minDimPx * TEXT_BOX_RATIO * scale
}

fun textSizePx(
    decoration: TextDecoration,
    minDimPx: Float,
    scale: Float
): Float = minDimPx * if (decoration.isBottomCaption()) {
    API_CAPTION_TEXT_SIZE_RATIO
} else {
    TEXT_SIZE_RATIO
} * scale

fun textMaxLines(decoration: TextDecoration): Int =
    if (decoration.isBottomCaption()) 6 else 3
```

The exact function names can be adjusted during implementation, but renderers must not duplicate the `BottomCaption` geometry or use ID-prefix checks.

**Step 4: Write failing mapper and render-spec tests**

Test both API metadata shapes and explicit layout/source:

```kotlin
@Test
fun textAssetDecorationMapsToBottomCaptionDecoration() {
    val decorations = ApiImage(
        id = "1",
        url = "/uploads/a.png",
        textAssetDecoration = ApiTextAssetDecoration(
            text = "HELLO",
            style = ApiTextOutsideForegroundStyle(
                fontFamily = "serif",
                color = "#FF0000",
                weight = "bold"
            ),
            source = "detected"
        )
    ).toStickerDecorations()

    val decoration = decorations.single() as TextDecoration
    assertEquals("HELLO", decoration.text)
    assertEquals(DecorationFont.Serif, decoration.font)
    assertEquals(DecorationFontWeight.Bold, decoration.fontWeight)
    assertEquals(0xFFFF0000L, decoration.textColorArgb)
    assertEquals(TextDecorationSource.ApiTextAsset, decoration.source)
    assertEquals(TextDecorationLayout.BottomCaption, decoration.layout)
}

@Test
fun outsideForegroundDecorationMapsToSameBottomCaptionGeometry() {
    val decorations = ApiImage(
        id = "1",
        url = "/uploads/a.png",
        textOutsideForeground = ApiTextOutsideForeground(
            text = "CAPTION",
            style = ApiTextOutsideForegroundStyle(color = "white")
        )
    ).toStickerDecorations()

    val decoration = decorations.single() as TextDecoration
    assertEquals(TextDecorationSource.ApiOutsideForeground, decoration.source)
    assertEquals(TextDecorationLayout.BottomCaption, decoration.layout)
}

@Test
fun blankTextAssetDecorationReturnsNoDecorations() {
    val decorations = ApiImage(
        id = "1",
        url = "/uploads/a.png",
        textAssetDecoration = ApiTextAssetDecoration(text = " ")
    ).toStickerDecorations()

    assertTrue(decorations.isEmpty())
}
```

Add a render spec test proving manual text and API caption text do not rely on ID:

```kotlin
@Test
fun bottomCaptionLayoutControlsRenderMetricsWithoutIdPrefix() {
    val decoration = TextDecoration(
        id = "any_future_id",
        text = "HELLO",
        font = DecorationFont.Sans,
        layout = TextDecorationLayout.BottomCaption
    )

    assertTrue(decoration.isBottomCaption())
    assertEquals(6, DecorationRenderSpec.textMaxLines(decoration))
}
```

**Step 5: Run tests and verify red**

Run: `./gradlew :composeApp:allTests`

Expected: FAIL because `TextDecorationSource`, `TextDecorationLayout`, `toStickerDecorations`, and render helpers do not exist yet.

**Step 6: Implement centralized API decoration mapper**

Create `ApiDecorationMapper.kt`:

```kotlin
package data.remote.mapper

import data.remote.model.ApiImage
import data.remote.model.ApiTextAssetDecoration
import data.remote.model.ApiTextOutsideForeground
import domain.model.StickerDecoration
import domain.model.TextDecorationSource

fun ApiImage.toStickerDecorations(): List<StickerDecoration> = listOfNotNull(
    textAssetDecoration.toTextDecoration(TextDecorationSource.ApiTextAsset)
        ?: textOutsideForeground.toTextDecoration(TextDecorationSource.ApiOutsideForeground)
)

private fun ApiTextAssetDecoration?.toTextDecoration(source: TextDecorationSource): TextDecoration? {
    val raw = this ?: return null
    return buildApiTextDecoration(text = raw.text, style = raw.style, source = source)
}

private fun ApiTextOutsideForeground?.toTextDecoration(source: TextDecorationSource): TextDecoration? {
    val raw = this ?: return null
    return buildApiTextDecoration(text = raw.text, style = raw.style, source = source)
}
```

Move the existing color/font/weight parsing into this mapper or keep it internal and imported from `TextOutsideForegroundMapper.kt`, but there must be one API-to-domain mapping entry point: `ApiImage.toStickerDecorations()`.

All API-mapped text decorations must set:

```kotlin
id = "api_text_${source.name}_${image.id}",
centerX = 0.5f,
centerY = 0.88f,
scale = 0.58f,
layout = TextDecorationLayout.BottomCaption,
source = source
```

Use image ID in the decoration ID for traceability, but do not use the ID format for rendering decisions.

**Step 7: Update all renderers to use render helpers**

Replace all `decoration.id.startsWith("api_txt_")` checks in:

- `DecorationPreviewLayer.kt`
- `ReadOnlyDecorationOverlay.kt`
- `AndroidStickerFileStorage.kt`

with `decoration.isBottomCaption()` or `DecorationRenderSpec` helper calls.

This is required so editor preview, generated result grid previews, pack detail thumbnails, create-pack thumbnails, Android exported WebP, and WhatsApp content-provider fallback all render API text assets consistently.

**Step 8: Keep backward compatibility for already persisted API captions**

Existing saved decorations may still have `id = "api_txt_..."` and no `layout` field. Add a normalization helper used after JSON decode:

```kotlin
fun StickerDecoration.normalizedForCurrentSchema(): StickerDecoration = when (this) {
    is TextDecoration -> if (layout == TextDecorationLayout.Freeform && id.startsWith("api_txt_")) {
        copy(
            source = TextDecorationSource.ApiOutsideForeground,
            layout = TextDecorationLayout.BottomCaption
        )
    } else this
    else -> this
}
```

Apply it in `StickerRepositoryImpl.parseDecorations` and `parseFrameDecorations` only. New code must not create ID-prefix-based API captions.

**Step 9: Run tests and verify green**

Run: `./gradlew :composeApp:allTests`

Expected: PASS.

---

### Task 3: Refactor `StickerApiRepository` Around Generated Files

**Files:**

- Modify: `composeApp/src/commonMain/kotlin/data/remote/StickerApiRepository.kt`
- Modify: `composeApp/src/commonMain/kotlin/data/remote/model/GridSplitStickerFile.kt` only if shared naming/docs need clarification
- Test: `composeApp/src/commonTest/kotlin/data/remote/GeneratedStickerFileMapperTest.kt`

**Step 1: Add pure mapping helper for downloaded API images**

Add a private or internal helper that combines downloaded local path with API decoration metadata:

```kotlin
private suspend fun downloadAndPersistGenerated(image: ApiImage): GeneratedStickerFile {
    val path = downloadAndPersist(image)
    return GeneratedStickerFile(
        localPath = path,
        decorations = image.toStickerDecorations()
    )
}
```

Import `data.remote.mapper.toStickerDecorations`. Do not inspect `textAssetDecoration` or `textOutsideForeground` directly outside the mapper.

**Step 2: Add decorated single-generate repository method without breaking old callers**

Add:

```kotlin
suspend fun generateStickers(
    prompt: String,
    inputImagePath: String? = null
): List<GeneratedStickerFile>
```

It calls `api.generate(prompt, inputImagePath)` and maps with `downloadAndPersistGenerated`.

Keep the existing `generate(...): List<String>` method temporarily and implement it through `generateStickers(...).map { it.localPath }` so the project stays compiling until UI state is migrated in Task 4.

**Step 3: Add sticker-pack grid repository method**

Add:

```kotlin
suspend fun generateStickerPack(
    prompt: String,
    layout: String,
    inputImagePath: String? = null
): List<GridSplitStickerFile>
```

It calls `api.generateStickerPack`, downloads the first returned grid image, then calls `splitGridOnDevice(rawGridPath, layout)`.

**Step 4: Add improve repository method with single and grid output handling**

Add:

```kotlin
suspend fun improve(
    imagePaths: List<String>
): List<GeneratedStickerFile>
```

If one image is passed, map the single API image with `downloadAndPersistGenerated`.

If multiple images are passed, backend returns 4x4 grid image chunks. For each returned grid image, download it, call `splitGridOnDevice(rawGridPath, "4x4")`, and keep only the number of cells represented by that chunk.

The chunk sizes must be calculated with `imagePaths.chunked(16).map { it.size }` so an 18-sticker pack keeps 16 cells from the first output grid and 2 cells from the second output grid.

**Step 5: Compile targeted source set**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`

Expected: BUILD SUCCESSFUL.

---

### Task 4: Update Existing Generate Result Flows To Preserve Decorations

**Files:**

- Modify: `composeApp/src/commonMain/kotlin/presentation/createpack/CreatePackState.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/createpack/CreatePackViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/createpack/CreatePackScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/editor/EditorState.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/editor/EditorIntent.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/editor/EditorViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/editor/EditorScreen.kt`

**Step 1: Change generated previews from path strings to drafts**

Update:

```kotlin
val generatedPreview: List<DraftSticker> = emptyList()
```

in both `CreatePackState` and `EditorState`.

**Step 2: Update Create Pack generation mapping**

In `CreatePackViewModel.generateStickers`, call the new decorated repository method and map each item:

```kotlin
val generated = apiRepository.generateStickers(
    prompt = currentState.generatePrompt,
    inputImagePath = currentState.generateInputImage
).map { file ->
    DraftSticker(
        imagePath = file.localPath,
        decorations = file.decorations
    )
}
```

**Step 3: Update Create Pack add-selected behavior**

In `addSelectedGeneratedToPack`, stop wrapping strings. Use the selected `DraftSticker` objects directly.

**Step 4: Update Editor apply intent**

Change `EditorIntent.ApplyGeneratedImage(val path: String)` to:

```kotlin
data class ApplyGeneratedSticker(val draft: DraftSticker) : EditorIntent
```

Use `presentation.createpack.DraftSticker` unless a more neutral draft model is extracted later.

**Step 5: Update Editor generation mapping and apply behavior**

Call `apiRepository.generateStickers(...)` and map repository output to drafts. When applying, set `imagePath = draft.imagePath`, `decorations = draft.decorations`, `selectedDecorationId = null`, and `generatedPreview = emptyList()`.

**Step 6: Update UI preview grids**

Remove `.map { DraftSticker(it) }` in generated preview sheets because state already contains `DraftSticker`.

**Step 7: Compile and lint targeted flow**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:lintDebug`

Expected: BUILD SUCCESSFUL or only warnings unrelated to this change.

**Step 8: Remove obsolete repository generate overload parameters**

After all call sites use `generateStickers(...)`, delete the old `StickerApiRepository.generate(...): List<String>` method if nothing else references it. Then simplify `SetikerApiService.generate` to:

```kotlin
suspend fun generate(
    prompt: String,
    inputImagePath: String? = null
): List<ApiImage>
```

Run: `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:lintDebug`

Expected: BUILD SUCCESSFUL.

---

### Task 5: Add Improve To Sticker Editor

**Files:**

- Modify: `composeApp/src/commonMain/kotlin/presentation/editor/EditorIntent.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/editor/EditorState.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/editor/EditorViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/editor/EditorScreen.kt`
- Modify: `composeApp/src/commonMain/composeResources/values/strings.xml`

**Step 1: Add intent and state support**

Add:

```kotlin
data object ImproveSticker : EditorIntent
```

Use existing `isApiLoading` and `generatedPreview` for loading and result confirmation.

**Step 2: Implement ViewModel handler**

Add `improveSticker()`:

```kotlin
private fun improveSticker() {
    viewModelScope.launch {
        val currentState = _state.value
        if (currentState.imagePath.isBlank()) {
            _effect.send(EditorEffect.ShowError(UiText.StringRes(Res.string.error_select_image)))
            return@launch
        }

        _state.update { it.copy(isApiLoading = true) }
        try {
            val improved = apiRepository.improve(listOf(currentState.imagePath)).map { file ->
                DraftSticker(imagePath = file.localPath, decorations = file.decorations)
            }
            _state.update { it.copy(isApiLoading = false, generatedPreview = improved) }
        } catch (e: Exception) {
            _state.update { it.copy(isApiLoading = false) }
            _effect.send(EditorEffect.ShowError(e.toUiText(Res.string.error_failed_improve_sticker)))
        }
    }
}
```

**Step 3: Add bottom-bar action**

In `EditorScreen`, add a `PackBottomBarIconButton` near `Generate AI`:

```kotlin
PackBottomBarIconButton(
    icon = Icons.Filled.AutoFixHigh,
    contentDescription = stringResource(Res.string.improve_sticker),
    onClick = { onIntent(EditorIntent.ImproveSticker) },
    enabled = state.imagePath.isNotBlank() && !isOperationInProgress
)
```

If `AutoFixHigh` is unavailable in the current material-icons dependency, use `Icons.Filled.AutoAwesome` and keep the content description distinct.

**Step 4: Add string resources**

Add:

```xml
<string name="improve_sticker">Improve</string>
<string name="error_failed_improve_sticker">Failed to improve sticker.</string>
```

**Step 5: Verify**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:lintDebug`

Expected: BUILD SUCCESSFUL.

---

### Task 6: Add Improve To Edit Sticker Pack

**Files:**

- Modify: `composeApp/src/commonMain/kotlin/presentation/createpack/CreatePackIntent.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/createpack/CreatePackState.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/createpack/CreatePackViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/createpack/CreatePackScreen.kt`
- Modify: `composeApp/src/commonMain/composeResources/values/strings.xml`

**Step 1: Add result mode to distinguish add vs replace**

Add an enum:

```kotlin
enum class GeneratedPreviewMode {
    AddToPack,
    ReplacePack
}
```

Add to `CreatePackState`:

```kotlin
val generatedPreviewMode: GeneratedPreviewMode = GeneratedPreviewMode.AddToPack
```

**Step 2: Add improve intent**

Add:

```kotlin
data object ImprovePackStickers : CreatePackIntent
data object ReplacePackWithGenerated : CreatePackIntent
```

**Step 3: Implement improve in ViewModel**

Collect non-blank, non-animated draft image paths:

```kotlin
val sourceDrafts = currentState.stickers.filter { !it.isAnimated && it.imagePath.isNotBlank() }
```

If empty, show a new error string `error_no_static_stickers_to_improve`.

Call:

```kotlin
val improved = apiRepository.improve(sourceDrafts.map { it.imagePath }).map { file ->
    DraftSticker(imagePath = file.localPath, decorations = file.decorations)
}
```

Set `generatedPreview = improved`, select all generated results, and set `generatedPreviewMode = ReplacePack`.

**Step 4: Replace draft stickers after confirmation**

Add `replacePackWithGenerated()` that replaces current non-animated draft stickers with selected improved drafts in order.

Keep animated stickers unchanged and appended in their original relative order unless product direction later says animated stickers should block improve entirely.

**Step 5: Update generated confirmation sheet**

When `generatedPreviewMode == AddToPack`, keep existing title and button.

When `generatedPreviewMode == ReplacePack`, use strings like `Confirm improved stickers` and primary button `Replace stickers`.

**Step 6: Add bottom-bar action**

In `CreatePackScreen`, add an Improve button near Generate AI:

```kotlin
PackBottomBarIconButton(
    icon = Icons.Filled.AutoFixHigh,
    contentDescription = stringResource(Res.string.improve_stickers),
    onClick = { onIntent(CreatePackIntent.ImprovePackStickers) },
    enabled = state.stickers.any { !it.isAnimated && it.imagePath.isNotBlank() } && !isOperationInProgress
)
```

Use `Icons.Filled.AutoAwesome` if `AutoFixHigh` is unavailable.

**Step 7: Verify pack limit and empty-cell behavior**

The repository improve method already trims multi-image 4x4 grid chunks to the input count. The confirmation sheet should therefore show exactly the number of improved source stickers, not all 16 grid cells.

**Step 8: Verify**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:lintDebug`

Expected: BUILD SUCCESSFUL.

---

### Task 7: Extract Shared Draft Pack Saving Logic

**Files:**

- Create: `composeApp/src/commonMain/kotlin/domain/model/StickerDraftInput.kt`
- Create: `composeApp/src/commonMain/kotlin/data/repository/StickerPackDraftSaver.kt`
- Modify: `composeApp/src/commonMain/kotlin/di/AppModule.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/createpack/CreatePackViewModel.kt`
- Test: `composeApp/src/commonTest/kotlin/data/repository/StickerPackDraftSaverTest.kt` if feasible with fake storage/repository

**Step 1: Add neutral draft input model**

Create:

```kotlin
package domain.model

data class StickerDraftInput(
    val imagePath: String,
    val decorations: List<StickerDecoration> = emptyList(),
    val isAnimated: Boolean = false,
    val sourceVideoFile: String? = null,
    val frameDecorations: Map<Int, List<StickerDecoration>> = emptyMap()
)
```

**Step 2: Add saver class**

Move the pack save logic currently inside `CreatePackViewModel.savePack()` into `StickerPackDraftSaver`.

The saver should handle tray save, static sticker WebP save, decoration preview save, animated sticker preservation, static-to-animated conversion for mixed animated packs, and `StickerPack` construction.

**Step 3: Keep Create Pack behavior unchanged**

`CreatePackViewModel.savePack()` should validate UI fields, create the identifier, map `DraftSticker` to `StickerDraftInput`, call the saver, save to repository, and emit `PackSaved` as before.

**Step 4: Register saver in Koin**

Add:

```kotlin
single { StickerPackDraftSaver(fileStorage = get()) }
```

Inject it into `CreatePackViewModel` and later `HomeViewModel`.

**Step 5: Verify no behavior regression**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:lintDebug`

Expected: BUILD SUCCESSFUL.

---

### Task 8: Add Home Generate Sticker Pack Flow

**Files:**

- Modify: `composeApp/src/commonMain/kotlin/presentation/home/HomeState.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/home/HomeIntent.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/home/HomeViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/home/HomeScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/components/HomeBottomBar.kt`
- Create: `composeApp/src/commonMain/kotlin/presentation/components/AiGenerateStickerPackBottomSheet.kt`
- Modify: `composeApp/src/commonMain/composeResources/values/strings.xml`
- Modify: `composeApp/src/commonMain/kotlin/di/AppModule.kt`

**Step 1: Add Home state fields**

Add:

```kotlin
val generatePackSheetOpen: Boolean = false,
val generatePackPrompt: String = "",
val generatePackName: String = "",
val generatePackPublisher: String = "",
val generatePackLayout: String = "4x4",
val generatePackInputImage: String? = null,
val isGeneratingPack: Boolean = false
```

**Step 2: Add Home intents**

Add:

```kotlin
data object OpenGeneratePackSheet : HomeIntent
data object CloseGeneratePackSheet : HomeIntent
data class UpdateGeneratePackPrompt(val prompt: String) : HomeIntent
data class UpdateGeneratePackName(val name: String) : HomeIntent
data class UpdateGeneratePackPublisher(val publisher: String) : HomeIntent
data class UpdateGeneratePackLayout(val layout: String) : HomeIntent
data class UpdateGeneratePackInputImage(val path: String?) : HomeIntent
data object GenerateStickerPack : HomeIntent
```

**Step 3: Add bottom bar Generate action**

Change `HomeBottomBar` signature to include `onGeneratePackClick`. Add an `AutoAwesome` action in `PackBottomBar.actions`. Keep existing FAB for Create Pack.

**Step 4: Create Home generate bottom sheet**

Use the same visual pattern as `AiGenerateBottomSheet`, but fields are `pack name`, `publisher`, `prompt`, optional reference image, and layout chips `2x2`, `3x3`, `4x4`.

The primary button is disabled if `prompt`, `pack name`, or `publisher` is blank.

**Step 5: Implement Home ViewModel generation**

Inject `StickerApiRepository`, `StickerFileStorage`, and `StickerPackDraftSaver` into `HomeViewModel`.

Implement:

```kotlin
private fun generateStickerPack() {
    viewModelScope.launch {
        val current = _state.value
        if (current.generatePackPrompt.isBlank()) { /* show prompt error */ return@launch }
        if (current.generatePackName.isBlank()) { /* show pack name error */ return@launch }
        if (current.generatePackPublisher.isBlank()) { /* show publisher error */ return@launch }

        _state.update { it.copy(isGeneratingPack = true, error = null) }
        try {
            val splitFiles = apiRepository.generateStickerPack(
                prompt = current.generatePackPrompt,
                layout = current.generatePackLayout,
                inputImagePath = current.generatePackInputImage
            )
            val drafts = splitFiles.map { file ->
                StickerDraftInput(imagePath = file.localPath, decorations = file.decorations)
            }.take(StickerPack.MAX_STICKERS)
            val traySource = drafts.firstOrNull()?.imagePath ?: throw IllegalStateException("No generated stickers")
            val identifier = sanitizePackIdentifier(current.generatePackName, Random.nextInt(1000, 9999))
            val pack = draftSaver.buildStickerPack(
                identifier = identifier,
                name = current.generatePackName,
                publisher = current.generatePackPublisher,
                visibility = "PRIVATE",
                trayImagePath = traySource,
                drafts = drafts
            )
            repository.savePack(pack)
            loadPacks()
            _state.update { it.copy(isGeneratingPack = false, generatePackSheetOpen = false) }
            _effect.send(HomeEffect.NavigateToPackDetail(identifier))
        } catch (e: Exception) {
            _state.update { it.copy(isGeneratingPack = false) }
            _effect.send(HomeEffect.ShowError(e.toUiText(Res.string.error_failed_generate_sticker_pack)))
        }
    }
}
```

Move `sanitizePackIdentifier` into a reusable helper instead of duplicating it from `CreatePackViewModel`.

**Step 6: Add strings**

Add strings for `generate_sticker_pack`, `generate_pack_sheet_title`, `pack_layout`, `error_failed_generate_sticker_pack`, and any missing content descriptions.

**Step 7: Verify Home flow compile and lint**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:lintDebug`

Expected: BUILD SUCCESSFUL.

---

### Task 9: Final Verification

**Files:**

- No new files unless failures require fixes

**Step 1: Search for stale API fields on `/generate` path**

Run a content search for `grid =`, `gridLayout`, `normalize`, and `splitGridOnServer` in generation call sites.

Expected: no stale `/api/v1/generate` request code appends old fields.

**Step 2: Run full Android compile and lint**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:lintDebug`

Expected: BUILD SUCCESSFUL.

Existing project warnings about AGP/KMP/deprecated APIs may remain, but there must be no compile/lint errors.

**Step 3: Run common tests**

Run: `./gradlew :composeApp:allTests`

Expected: all tests pass.

**Step 4: Manual QA checklist**

- Open sticker editor, tap Improve, confirm improved preview appears, apply it, and verify image plus text decoration overlays persist.
- Open sticker editor, tap Generate AI, confirm generated preview still works after `/generate` refactor.
- Open Create Pack or Edit Pack, tap Improve, confirm selected replacement flow shows exactly the improved sticker count.
- Open Create Pack, tap Generate AI, confirm generated stickers with optional text decorations can be added.
- Open Home, tap Generate, enter pack data and prompt, confirm a new pack is saved and opened.
- Confirm generated Home pack stickers are split from the returned grid and text decorations appear like grid split results.

---

## Risks And Watch Points

- Endpoint naming mismatch: user said `/generate/improve`, API currently exposes `/api/v1/generate/improvement`.
- Backend validator requires `layout` for `/generate/sticker-pack` even though OpenAPI only marks `text` as required.
- Multiple-image improve returns 4x4 grid chunks. The client must trim split cells to input count per chunk to avoid empty cells becoming stickers.
- Existing `CreatePackViewModel.savePack()` contains important animated-pack conversion behavior. Extract it carefully to avoid breaking static plus animated mixed packs.
- Text decoration output uses `textAssetDecoration` for improve and `textOutsideForeground` for grid-derived cells. Both must map through `ApiImage.toStickerDecorations()` so API decoration semantics stay consistent across all screens.
- Current code uses `api_txt_` ID prefixes to select caption geometry. The plan must replace this with explicit `TextDecoration.source` and `TextDecoration.layout`; keep prefix handling only as persisted-data migration/normalization.
- Android export compositing currently duplicates caption geometry separately from Compose preview. The plan must centralize render metrics in `DecorationRenderSpec` and update both Compose renderers plus `AndroidStickerFileStorage`.
- iOS storage currently ignores decorations when saving composed stickers. This is existing behavior; do not block Android feature delivery, but document that iOS export will not flatten API text decorations until native compositing exists.
- Direct Home pack saving needs a tray icon. Use the first generated sticker as tray source unless product direction changes.

---

## Execution Notes

Implement with TDD where practical for pure mapping and helper logic. For ViewModel/UI integration, compile/lint verification is mandatory after each major task. Do not commit unless the user explicitly asks.
