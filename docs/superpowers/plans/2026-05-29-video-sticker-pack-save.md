# Video Sticker Pack Save Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Save WhatsApp-compatible video-generated sticker packs by separating static and animated stickers, improving animated previews, and blocking screen interaction during bottom FAB loading.

**Architecture:** Keep `StickerPackDraftSaver` as the reusable persistence boundary, but make mixed-media callers split drafts before saving. Add selection state and split-save orchestration in `VideoStickerPackViewModel`, then update `VideoStickerPackScreen` to render selectable static/animated previews and use the existing `PackBottomBarFab` loading affordance. Add a reusable transparent interaction blocker for bottom-FAB loading states and apply IME padding to form screens with text fields.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform Material 3, coroutines `StateFlow`, MockK, Kotlin test, Gradle Android unit tests.

---

## Files

- Modify: `composeApp/src/commonMain/kotlin/data/repository/StickerPackDraftSaver.kt`
- Modify: `composeApp/src/androidUnitTest/kotlin/data/repository/StickerPackDraftSaverTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/videostickerpack/VideoStickerPackState.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/videostickerpack/VideoStickerPackIntent.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/videostickerpack/VideoStickerPackViewModel.kt`
- Modify: `composeApp/src/androidUnitTest/kotlin/presentation/videostickerpack/VideoStickerPackViewModelTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/videostickerpack/VideoStickerPackScreen.kt`
- Create: `composeApp/src/commonMain/kotlin/presentation/components/InteractionBlocker.kt`
- Modify: bottom-FAB-loading screens that need interaction blocking: `SyncScreen.kt`, `SharePreviewScreen.kt`, `PublicPackDetailScreen.kt`, `PackDetailScreen.kt`, `ProcessingHistoryScreen.kt`, `EditorScreen.kt`, `CreatePackScreen.kt`, `AnimatedEditorScreen.kt`, and `VideoStickerPackScreen.kt`
- Modify for IME padding where text fields currently lack it: `VideoStickerPackScreen.kt`, `CreatePackScreen.kt`, `EditorScreen.kt`, `AnimatedEditorScreen.kt`, `DecorationBottomSheets.kt`, `AiGenerateStickerPackBottomSheet.kt`, `AiGenerateBottomSheet.kt`

## Task 1: Stop Draft Saver From Mixing Sticker Types

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/data/repository/StickerPackDraftSaver.kt`
- Modify: `composeApp/src/androidUnitTest/kotlin/data/repository/StickerPackDraftSaverTest.kt`

- [ ] **Step 1: Replace the static-to-animated conversion test**

In `StickerPackDraftSaverTest`, replace `convertsStaticDraftsWhenPackContainsAnimatedSticker` and `convertsStaticWithoutDecorationsInAnimatedPack` with tests that assert the saver preserves each draft type instead of converting static stickers.

Use this test body for mixed decorated static plus animated:

```kotlin
@Test
fun preservesDraftTypesWhenInputContainsStaticAndAnimatedStickers() = runTest {
    val fileStorage = mockk<StickerFileStorage>()
    val decoration = mockk<StickerDecoration>()
    coEvery { fileStorage.saveTrayImage("/tmp/tray.png", "tray_pack_1710000000000.png") } returns "/saved/tray.webp"
    coEvery { fileStorage.saveStickerImage("/tmp/static.png", "sticker_pack_0_base.webp") } returns "/saved/static_base.webp"
    coEvery {
        fileStorage.saveStickerImageWithDecorations(
            sourcePath = "/saved/static_base.webp",
            fileName = "sticker_pack_0_preview.webp",
            decorations = listOf(decoration)
        )
    } returns "/saved/static_preview.webp"

    val saver = StickerPackDraftSaver(fileStorage = fileStorage, nowEpochMillis = { 1710000000000L })

    val pack = saver.buildDraftPack(
        StickerDraftInput(
            identifier = "pack",
            name = "Pack",
            publisher = "Pub",
            visibility = "PRIVATE",
            trayImagePath = "/tmp/tray.png",
            stickers = listOf(
                StickerDraftInput.StickerInput(
                    imagePath = "/tmp/static.png",
                    decorations = listOf(decoration)
                ),
                StickerDraftInput.StickerInput(
                    imagePath = "/tmp/anim.webp",
                    decorations = listOf(decoration),
                    isAnimated = true,
                    sourceVideoFile = "/tmp/video.mp4",
                    frameDecorations = mapOf(0 to listOf(decoration))
                )
            )
        )
    )

    assertTrue(pack.isAnimated)
    assertEquals("/saved/static_preview.webp", pack.stickers[0].imageFile)
    assertEquals("/saved/static_base.webp", pack.stickers[0].sourceImageFile)
    assertFalse(pack.stickers[0].isAnimated)
    assertEquals("/tmp/anim.webp", pack.stickers[1].imageFile)
    assertNull(pack.stickers[1].sourceImageFile)
    assertTrue(pack.stickers[1].isAnimated)
    assertEquals("/tmp/video.mp4", pack.stickers[1].sourceVideoFile)
    assertEquals(mapOf(0 to listOf(decoration)), pack.stickers[1].frameDecorations)

    coVerify(exactly = 0) { fileStorage.encodeSingleFrameAnimatedWebP(any(), any(), any()) }
}
```

Use this test body for a plain mixed pack:

```kotlin
@Test
fun preservesPlainStaticDraftWhenInputAlsoContainsAnimatedSticker() = runTest {
    val fileStorage = mockk<StickerFileStorage>()
    coEvery { fileStorage.saveTrayImage("/tmp/tray.png", "tray_pack_1710000000000.png") } returns "/saved/tray.webp"
    coEvery { fileStorage.saveStickerImage("/tmp/plain.png", "sticker_pack_0_base.webp") } returns "/saved/plain_base.webp"

    val saver = StickerPackDraftSaver(fileStorage = fileStorage, nowEpochMillis = { 1710000000000L })

    val pack = saver.buildDraftPack(
        StickerDraftInput(
            identifier = "pack",
            name = "Pack",
            publisher = "Pub",
            visibility = "PRIVATE",
            trayImagePath = "/tmp/tray.png",
            stickers = listOf(
                StickerDraftInput.StickerInput(imagePath = "/tmp/plain.png"),
                StickerDraftInput.StickerInput(imagePath = "/tmp/anim.webp", isAnimated = true)
            )
        )
    )

    assertTrue(pack.isAnimated)
    assertEquals("/saved/plain_base.webp", pack.stickers[0].imageFile)
    assertEquals("/saved/plain_base.webp", pack.stickers[0].sourceImageFile)
    assertFalse(pack.stickers[0].isAnimated)
    assertEquals("/tmp/anim.webp", pack.stickers[1].imageFile)
    assertTrue(pack.stickers[1].isAnimated)
    coVerify(exactly = 0) { fileStorage.encodeSingleFrameAnimatedWebP(any(), any(), any()) }
}
```

- [ ] **Step 2: Run the draft saver tests and confirm the expected failure**

Run:

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "data.repository.StickerPackDraftSaverTest"
```

Expected before implementation: failures show `encodeSingleFrameAnimatedWebP` is still called or static stickers are marked animated.

- [ ] **Step 3: Remove static conversion from `StickerPackDraftSaver`**

In `StickerPackDraftSaver.buildDraftPack`, delete the `packIsAnimated -> { ... encodeSingleFrameAnimatedWebP ... }` branch. The `when` should only special-case true animated drafts; all non-animated drafts should use the existing static base/preview path.

The resulting structure should be:

```kotlin
val packIsAnimated = input.stickers.any { it.isAnimated }

val stickers = input.stickers.mapIndexed { index, draft ->
    when {
        draft.isAnimated -> Sticker(
            imageFile = draft.imagePath,
            sourceImageFile = null,
            emojis = listOf("⭐"),
            decorations = draft.decorations,
            isAnimated = true,
            sourceVideoFile = draft.sourceVideoFile,
            frameDecorations = draft.frameDecorations
        )

        else -> {
            val baseFileName = "sticker_${input.identifier}_${index}_base.webp"
            val basePath = fileStorage.saveStickerImage(
                sourcePath = draft.imagePath,
                fileName = baseFileName
            )
            val previewPath = if (draft.decorations.isEmpty()) {
                basePath
            } else {
                fileStorage.saveStickerImageWithDecorations(
                    sourcePath = basePath,
                    fileName = "sticker_${input.identifier}_${index}_preview.webp",
                    decorations = draft.decorations
                )
            }
            Sticker(
                imageFile = previewPath,
                sourceImageFile = basePath,
                emojis = listOf("⭐"),
                decorations = draft.decorations
            )
        }
    }
}
```

- [ ] **Step 4: Run the draft saver tests**

Run:

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "data.repository.StickerPackDraftSaverTest"
```

Expected: all tests in `StickerPackDraftSaverTest` pass.

- [ ] **Step 5: Check the diff**

Run:

```powershell
git diff -- composeApp/src/commonMain/kotlin/data/repository/StickerPackDraftSaver.kt composeApp/src/androidUnitTest/kotlin/data/repository/StickerPackDraftSaverTest.kt
```

Expected: only the conversion branch and its tests changed. Do not commit unless the user explicitly asks.

## Task 2: Add Video Sticker Selection State and Split Save

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/presentation/videostickerpack/VideoStickerPackState.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/videostickerpack/VideoStickerPackIntent.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/videostickerpack/VideoStickerPackViewModel.kt`
- Modify: `composeApp/src/androidUnitTest/kotlin/presentation/videostickerpack/VideoStickerPackViewModelTest.kt`

- [ ] **Step 1: Extend state with selected generated item keys**

In `VideoStickerPackState`, add selection sets and helper properties:

```kotlin
val selectedStaticStickerKeys: Set<String> = emptySet(),
val selectedAnimatedStickerKeys: Set<String> = emptySet(),
```

Update `canSave` to use selected counts:

```kotlin
val selectedStickerCount: Int
    get() = selectedStaticStickerKeys.size + selectedAnimatedStickerKeys.size

val isSaving: Boolean
    get() = processingStep == VideoStickerPackProcessingStep.Saving && isProcessing

val isBlockingUi: Boolean
    get() = isSaving

val canSave: Boolean
    get() = selectedStickerCount > 0 && packName.isNotBlank() && publisher.isNotBlank() && !isProcessing
```

- [ ] **Step 2: Add selection intents**

In `VideoStickerPackIntent`, add:

```kotlin
data class ToggleStaticStickerSelection(val key: String) : VideoStickerPackIntent
data class ToggleAnimatedStickerSelection(val key: String) : VideoStickerPackIntent
```

- [ ] **Step 3: Add key helpers and intent handling in the view model**

In `VideoStickerPackViewModel`, add private key helpers near the bottom:

```kotlin
private fun ResolvedVideoStaticSticker.selectionKey(): String =
    "static:${plan.candidateId}:${plan.timestampMs}:$localPath"

private fun ResolvedVideoAnimatedSticker.selectionKey(index: Int): String =
    "animated:$index:${plan.timeline.firstOrNull()?.timestampMs ?: 0}:${plan.timeline.lastOrNull()?.timestampMs ?: 0}"
```

Add imports:

```kotlin
import domain.model.ResolvedVideoAnimatedSticker
import domain.model.ResolvedVideoStaticSticker
```

Handle new intents:

```kotlin
is VideoStickerPackIntent.ToggleStaticStickerSelection -> toggleStaticSelection(intent.key)
is VideoStickerPackIntent.ToggleAnimatedStickerSelection -> toggleAnimatedSelection(intent.key)
```

Add methods:

```kotlin
private fun toggleStaticSelection(key: String) {
    _state.update { current ->
        current.copy(
            selectedStaticStickerKeys = current.selectedStaticStickerKeys.toggle(key)
        )
    }
}

private fun toggleAnimatedSelection(key: String) {
    _state.update { current ->
        current.copy(
            selectedAnimatedStickerKeys = current.selectedAnimatedStickerKeys.toggle(key)
        )
    }
}

private fun Set<String>.toggle(key: String): Set<String> =
    if (key in this) this - key else this + key
```

- [ ] **Step 4: Clear and initialize selection around generation**

When prompt/range/video changes clear selected sets alongside `generatedPlan = null`.

After `generated` is returned and before saving it into state, initialize selection to all generated stickers:

```kotlin
val selectedStaticKeys = generated.staticStickers.map { it.selectionKey() }.toSet()
val selectedAnimatedKeys = generated.animatedStickers.mapIndexed { index, sticker ->
    sticker.selectionKey(index)
}.toSet()

_state.update {
    it.copy(
        isProcessing = false,
        processingStep = null,
        processingProgress = 1f,
        generatedPlan = generated,
        selectedStaticStickerKeys = selectedStaticKeys,
        selectedAnimatedStickerKeys = selectedAnimatedKeys
    )
}
```

- [ ] **Step 5: Split video save into separate draft inputs**

Refactor `savePack()` so it filters selected items first:

```kotlin
val selectedStatic = generated.staticStickers.filter { it.selectionKey() in current.selectedStaticStickerKeys }
val selectedAnimated = generated.animatedStickers.filterIndexed { index, sticker ->
    sticker.selectionKey(index) in current.selectedAnimatedStickerKeys
}
if (selectedStatic.isEmpty() && selectedAnimated.isEmpty()) return@launch
val shouldSplitNames = selectedStatic.isNotEmpty() && selectedAnimated.isNotEmpty()
```

Build static inputs from `selectedStatic`, animated inputs from `selectedAnimated`, then call `draftSaver.buildDraftPack` once per non-empty group:

```kotlin
val baseIdentifier = PackIdentifierSanitizer.sanitize(current.packName, Random.nextInt(1000, 9999))
val savedPacks = mutableListOf<domain.model.StickerPack>()

if (staticInputs.isNotEmpty()) {
    val staticIdentifier = if (shouldSplitNames) "${baseIdentifier}_static" else baseIdentifier
    val staticPack = draftSaver.buildDraftPack(
        StickerDraftInput(
            identifier = staticIdentifier,
            name = if (shouldSplitNames) "${current.packName} Static" else current.packName,
            publisher = current.publisher,
            visibility = "PRIVATE",
            trayImagePath = selectedStatic.first().localPath,
            stickers = staticInputs
        )
    )
    stickerRepository.savePack(staticPack)
    savedPacks += staticPack
}

if (animatedInputs.isNotEmpty()) {
    val animatedIdentifier = if (shouldSplitNames) "${baseIdentifier}_animated" else baseIdentifier
    val animatedPack = draftSaver.buildDraftPack(
        StickerDraftInput(
            identifier = animatedIdentifier,
            name = if (shouldSplitNames) "${current.packName} Animated" else current.packName,
            publisher = current.publisher,
            visibility = "PRIVATE",
            trayImagePath = animatedInputs.first().imagePath,
            stickers = animatedInputs
        )
    )
    stickerRepository.savePack(animatedPack)
    savedPacks += animatedPack
}

val destination = savedPacks.firstOrNull()?.identifier ?: error("No selected stickers returned")
_effect.send(VideoStickerPackEffect.NavigateToPackDetail(destination))
```

Keep the existing fallback decode path for animated stickers. Only change its source list from `generated.animatedStickers` to `selectedAnimated`.

- [ ] **Step 6: Add view model tests for default selection and split save**

In `VideoStickerPackViewModelTest`, add a mixed resolved plan helper:

```kotlin
private fun mixedResolvedPlan(): ResolvedVideoStickerPackPlan {
    val static = ResolvedVideoStaticSticker(
        plan = VideoStaticStickerPlan(
            candidateId = "frame_0000",
            frameIndex = 0,
            timestampMs = 1_000L,
            cellId = "A1",
            emojis = emptyList()
        ),
        localPath = "/tmp/static.png"
    )
    val frames = listOf(
        VideoAnimatedTimelineFrame("frame_0001", 1, 2_000L, 83L),
        VideoAnimatedTimelineFrame("frame_0002", 2, 3_000L, 83L)
    )
    val animatedPlan = VideoAnimatedStickerPlan(
        timeline = frames,
        fps = 12,
        loopCount = 0,
        emojis = emptyList()
    )
    return ResolvedVideoStickerPackPlan(
        plan = VideoStickerPackPlan(
            packTitle = "Plan",
            staticStickers = listOf(static.plan),
            animatedStickers = listOf(animatedPlan)
        ),
        staticStickers = listOf(static),
        animatedStickers = listOf(
            ResolvedVideoAnimatedSticker(
                plan = animatedPlan,
                timeline = listOf(
                    ResolvedVideoAnimatedTimelineFrame(frames[0], "/tmp/a1.png"),
                    ResolvedVideoAnimatedTimelineFrame(frames[1], "/tmp/a2.png")
                )
            )
        )
    )
}
```

Add a capturing saver that records every input:

```kotlin
private class MultiCapturingDraftSaver : StickerPackDraftSaver(fileStorage = mockk<StickerFileStorage>(relaxed = true)) {
    val inputs = mutableListOf<StickerDraftInput>()

    override suspend fun buildDraftPack(input: StickerDraftInput): StickerPack {
        inputs += input
        return StickerPack(
            identifier = input.identifier,
            name = input.name,
            publisher = input.publisher,
            trayImageFile = input.trayImagePath,
            stickers = input.stickers.map { Sticker(imageFile = it.imagePath, isAnimated = it.isAnimated) },
            isAnimated = input.stickers.any { it.isAnimated },
            visibility = input.visibility
        )
    }
}
```

Add test:

```kotlin
@Test
fun generateSelectsAllGeneratedVideoStickersByDefault() = runTest {
    val fileStorage = mockk<StickerFileStorage>()
    coEvery { fileStorage.getVideoDurationMs(any()) } returns 60_000L
    val extractor = mockk<VideoFrameCandidateExtractor>()
    coEvery { extractor.extractCandidates(any(), any(), any(), any()) } returns listOf(
        VideoFrameCandidate("/tmp/c1.png", 1_000L, 0.4, 0.5, 0.3),
        VideoFrameCandidate("/tmp/c2.png", 2_000L, 0.4, 0.5, 0.3)
    )
    val gridComposer = mockk<CandidateGridComposer>()
    coEvery { gridComposer.composeGrids(any()) } returns listOf(CandidateGridImage("/tmp/grid.png", 2))
    val apiRepository = mockk<StickerApiRepository>()
    coEvery { apiRepository.generateVideoStickerPack(any(), any(), any(), any(), any(), any(), any()) } returns mixedResolvedPlan()
    val viewModel = VideoStickerPackViewModel(fileStorage, extractor, gridComposer, apiRepository, mockk(relaxed = true), CapturingDraftSaver(fakePack()))

    viewModel.onIntent(VideoStickerPackIntent.LoadVideo("/tmp/video.mp4"))
    advanceUntilIdle()
    viewModel.onIntent(VideoStickerPackIntent.Generate)
    advanceUntilIdle()

    val state = viewModel.state.value
    assertEquals(1, state.selectedStaticStickerKeys.size)
    assertEquals(1, state.selectedAnimatedStickerKeys.size)
    assertEquals(2, state.selectedStickerCount)
}
```

Add test:

```kotlin
@Test
fun saveMixedVideoSelectionPersistsStaticAndAnimatedPacksSeparately() = runTest {
    val fileStorage = mockk<StickerFileStorage>()
    coEvery { fileStorage.getVideoDurationMs(any()) } returns 60_000L
    coEvery { fileStorage.loadImage("/tmp/a1.png") } returns byteArrayOf(1)
    coEvery { fileStorage.loadImage("/tmp/a2.png") } returns byteArrayOf(2)
    coEvery { fileStorage.saveAnimatedStickerImage(any(), any(), any(), any(), any()) } returns "/tmp/animated.webp"
    val extractor = mockk<VideoFrameCandidateExtractor>()
    coEvery { extractor.extractCandidates(any(), any(), any(), any()) } returns listOf(
        VideoFrameCandidate("/tmp/c1.png", 1_000L, 0.4, 0.5, 0.3),
        VideoFrameCandidate("/tmp/c2.png", 2_000L, 0.4, 0.5, 0.3)
    )
    val gridComposer = mockk<CandidateGridComposer>()
    coEvery { gridComposer.composeGrids(any()) } returns listOf(CandidateGridImage("/tmp/grid.png", 2))
    val apiRepository = mockk<StickerApiRepository>()
    coEvery { apiRepository.generateVideoStickerPack(any(), any(), any(), any(), any(), any(), any()) } returns mixedResolvedPlan()
    val repository = mockk<StickerRepository>(relaxed = true)
    val saver = MultiCapturingDraftSaver()
    val viewModel = VideoStickerPackViewModel(fileStorage, extractor, gridComposer, apiRepository, repository, saver)

    viewModel.onIntent(VideoStickerPackIntent.LoadVideo("/tmp/video.mp4"))
    advanceUntilIdle()
    viewModel.onIntent(VideoStickerPackIntent.Generate)
    advanceUntilIdle()
    viewModel.onIntent(VideoStickerPackIntent.UpdatePackName("Video Pack"))
    viewModel.onIntent(VideoStickerPackIntent.UpdatePublisher("Setiker"))
    viewModel.onIntent(VideoStickerPackIntent.SavePack)
    advanceUntilIdle()

    assertEquals(2, saver.inputs.size)
    assertEquals("Video Pack Static", saver.inputs[0].name)
    assertEquals("Video Pack Animated", saver.inputs[1].name)
    assertEquals(false, saver.inputs[0].stickers.single().isAnimated)
    assertEquals(true, saver.inputs[1].stickers.single().isAnimated)
    assertEquals(true, saver.inputs[0].identifier.endsWith("_static"))
    assertEquals(true, saver.inputs[1].identifier.endsWith("_animated"))
    coVerify(exactly = 2) { repository.savePack(any()) }
}
```

- [ ] **Step 7: Run view model tests**

Run:

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "presentation.videostickerpack.VideoStickerPackViewModelTest"
```

Expected: all tests in `VideoStickerPackViewModelTest` pass.

## Task 3: Update Video Generator UI Preview, Selection, FAB Loading, and IME Padding

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/presentation/videostickerpack/VideoStickerPackScreen.kt`

- [ ] **Step 1: Move save action to bottom app bar FAB**

Import `Icons.Filled.Check`, `PackBottomBar`, `PackBottomBarFab`, and `PackBottomBarIconButton` if needed. Use `Scaffold(bottomBar = { ... })` with the save action in `PackBottomBarFab`.

Use this structure:

```kotlin
bottomBar = {
    PackBottomBar(
        actionStatusText = if (state.isSaving) stringResource(Res.string.video_pack_save) else null,
        actions = {
            PackBottomBarIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.back),
                onClick = onBackClick,
                enabled = !state.isProcessing
            )
        },
        floatingActionButton = {
            PackBottomBarFab(
                icon = Icons.Filled.Check,
                contentDescription = stringResource(Res.string.video_pack_save),
                onClick = { onIntent(VideoStickerPackIntent.SavePack) },
                enabled = state.canSave,
                isLoading = state.isSaving
            )
        }
    )
}
```

Remove the inline save `AppPrimaryButton`. Keep generate/regenerate as an inline primary button, disabled during processing.

- [ ] **Step 2: Disable text fields, slider, and generated cards while blocked**

Set a local flag:

```kotlin
val contentEnabled = !state.isBlockingUi
```

Pass `enabled = contentEnabled` to all `AppTextField` calls and `RangeSlider`. For generated card selection clicks, only dispatch toggle intents when `contentEnabled` is true.

- [ ] **Step 3: Add IME padding to the scrollable form**

Import `androidx.compose.foundation.layout.imePadding`. Apply it to the root scrollable column:

```kotlin
.padding(innerPadding)
.imePadding()
.padding(horizontal = 20.dp, vertical = 16.dp)
.verticalScroll(rememberScrollState())
```

- [ ] **Step 4: Render selectable static cards**

For each static sticker, compute the same key format as the view model and selected state:

```kotlin
val key = "static:${generated.plan.candidateId}:${generated.plan.timestampMs}:${generated.localPath}"
val isSelected = key in state.selectedStaticStickerKeys
```

Wrap `StickerCard` in a bordered clickable container. Use a thick accent border when selected and a muted border when unselected. Dispatch:

```kotlin
onIntent(VideoStickerPackIntent.ToggleStaticStickerSelection(key))
```

- [ ] **Step 5: Replace animated frame grid with one animated preview card per sticker**

Add a private composable in `VideoStickerPackScreen.kt`:

```kotlin
@Composable
private fun AnimatedVideoStickerPreviewCard(
    animated: ResolvedVideoAnimatedSticker,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var frameIndex by remember(animated) { mutableStateOf(0) }
    LaunchedEffect(animated, frameIndex) {
        val duration = animated.timeline.getOrNull(frameIndex)?.frame?.durationMs ?: 83L
        kotlinx.coroutines.delay(duration.coerceAtLeast(16L))
        frameIndex = if (animated.timeline.isEmpty()) 0 else (frameIndex + 1) % animated.timeline.size
    }
    val frame = animated.timeline.getOrNull(frameIndex) ?: return
    val decorations = animated.plan.baseDecorations + animated.plan.frameDecorations[frameIndex].orEmpty()

    SelectableGeneratedStickerCard(
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
    ) {
        StickerCard(
            sticker = Sticker(
                imageFile = frame.localPath,
                decorations = decorations,
                accessibilityText = animated.plan.accessibilityText
            ),
            onClick = {},
            showDecorations = true,
            modifier = Modifier.size(108.dp)
        )
    }
}
```

Also add `SelectableGeneratedStickerCard` so static and animated cards share border behavior:

```kotlin
@Composable
private fun SelectableGeneratedStickerCard(
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val borderColor = if (selected) presentation.theme.AccentCoral else neubrutalBorderColor().copy(alpha = 0.35f)
    Box(
        modifier = modifier
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(4.dp)
    ) {
        content()
    }
}
```

- [ ] **Step 6: Render animated cards in `FlowRow`**

Replace the existing `animated.timeline.forEachIndexed` frame grid with one card per animated sticker:

```kotlin
FlowRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
) {
    generatedPlan.animatedStickers.forEachIndexed { index, animated ->
        val key = "animated:$index:${animated.plan.timeline.firstOrNull()?.timestampMs ?: 0}:${animated.plan.timeline.lastOrNull()?.timestampMs ?: 0}"
        val selected = key in state.selectedAnimatedStickerKeys
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AnimatedVideoStickerPreviewCard(
                animated = animated,
                selected = selected,
                enabled = contentEnabled,
                onClick = { onIntent(VideoStickerPackIntent.ToggleAnimatedStickerSelection(key)) }
            )
            Text(
                text = "Loop ${index + 1}: ${animated.timeline.size} frames at ${animated.plan.fps} fps",
                style = MaterialTheme.typography.labelSmall,
                color = neubrutalSubtleOnSurface()
            )
        }
    }
}
```

- [ ] **Step 7: Run compile for UI changes**

Run:

```powershell
.\gradlew.bat :composeApp:compileDebugKotlinAndroid
```

Expected: Kotlin compilation succeeds.

## Task 4: Add Reusable Transparent Interaction Blocker and Apply to Bottom FAB Loading Screens

**Files:**
- Create: `composeApp/src/commonMain/kotlin/presentation/components/InteractionBlocker.kt`
- Modify: `SyncScreen.kt`, `SharePreviewScreen.kt`, `PublicPackDetailScreen.kt`, `PackDetailScreen.kt`, `ProcessingHistoryScreen.kt`, `EditorScreen.kt`, `CreatePackScreen.kt`, `AnimatedEditorScreen.kt`, `VideoStickerPackScreen.kt`

- [ ] **Step 1: Create the reusable blocker**

Create `InteractionBlocker.kt`:

```kotlin
package presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics

@Composable
fun InteractionBlockedBox(
    blocked: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        content()
        if (blocked) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .semantics { disabled() }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
            )
        }
    }
}
```

- [ ] **Step 2: Wrap content areas in screens whose bottom FAB has loading**

For each screen, wrap the scaffold body content, not the bottom bar, so the loading FAB remains visible:

```kotlin
InteractionBlockedBox(
    blocked = state.isSaving,
    modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
) {
    // existing screen body content
}
```

Use the correct loading flag per screen:

- `SyncScreen.kt`: `state.isSyncing`
- `SharePreviewScreen.kt`: `state.isAccepting`
- `PublicPackDetailScreen.kt`: `state.isActionLoading`
- `PackDetailScreen.kt`: `state.cloudShareLinksLoading`
- `ProcessingHistoryScreen.kt`: `state.isClearing`
- `EditorScreen.kt`: `state.isSaving`
- `CreatePackScreen.kt`: `state.isSaving`
- `AnimatedEditorScreen.kt`: `state.isSaving`
- `VideoStickerPackScreen.kt`: `state.isSaving`

Do not wrap initial full-screen loading states that do not use bottom app bar FAB loading.

- [ ] **Step 3: Keep explicit enabled flags where already present**

Do not remove existing `enabled = !isOperationInProgress` checks. The overlay is a safety layer for controls that do not expose `enabled` or were missed.

- [ ] **Step 4: Compile after blocker application**

Run:

```powershell
.\gradlew.bat :composeApp:compileDebugKotlinAndroid
```

Expected: Kotlin compilation succeeds.

## Task 5: Apply IME Padding to Remaining Text-Input Surfaces

**Files:**
- Modify: `VideoStickerPackScreen.kt`, `CreatePackScreen.kt`, `EditorScreen.kt`, `AnimatedEditorScreen.kt`
- Modify: `DecorationBottomSheets.kt`, `AiGenerateStickerPackBottomSheet.kt`, `AiGenerateBottomSheet.kt`

- [ ] **Step 1: Add `imePadding()` to screen scroll containers with `AppTextField`**

For each screen body column that contains `AppTextField` and lacks `imePadding()`, import:

```kotlin
import androidx.compose.foundation.layout.imePadding
```

Apply:

```kotlin
modifier = modifier
    .fillMaxSize()
    .padding(innerPadding)
    .imePadding()
    .padding(horizontal = 20.dp, vertical = 16.dp)
```

Target screen bodies:

- `VideoStickerPackScreen.kt`
- `CreatePackScreen.kt`
- `EditorScreen.kt`
- `AnimatedEditorScreen.kt`

- [ ] **Step 2: Add `imePadding()` to bottom sheet content with text fields**

For bottom sheet column/root containers with `AppTextField`, add:

```kotlin
.imePadding()
```

Target files:

- `DecorationBottomSheets.kt`
- `AiGenerateStickerPackBottomSheet.kt`
- `AiGenerateBottomSheet.kt`

- [ ] **Step 3: Compile after IME changes**

Run:

```powershell
.\gradlew.bat :composeApp:compileDebugKotlinAndroid
```

Expected: Kotlin compilation succeeds.

## Task 6: Final Verification

**Files:**
- Read-only verification across modified files

- [ ] **Step 1: Run targeted unit tests**

Run:

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "data.repository.StickerPackDraftSaverTest" --tests "presentation.videostickerpack.VideoStickerPackViewModelTest"
```

Expected: both targeted test classes pass.

- [ ] **Step 2: Run Android debug Kotlin compilation**

Run:

```powershell
.\gradlew.bat :composeApp:compileDebugKotlinAndroid
```

Expected: task finishes successfully.

- [ ] **Step 3: Inspect git diff**

Run:

```powershell
git diff --stat
git diff -- docs/superpowers/specs/2026-05-29-video-sticker-pack-save-design.md docs/superpowers/plans/2026-05-29-video-sticker-pack-save.md composeApp/src/commonMain/kotlin/data/repository/StickerPackDraftSaver.kt composeApp/src/commonMain/kotlin/presentation/videostickerpack composeApp/src/androidUnitTest/kotlin/data/repository/StickerPackDraftSaverTest.kt composeApp/src/androidUnitTest/kotlin/presentation/videostickerpack/VideoStickerPackViewModelTest.kt composeApp/src/commonMain/kotlin/presentation/components/InteractionBlocker.kt
```

Expected: diff only contains the planned changes. Do not commit unless the user explicitly asks.

- [ ] **Step 4: Manual behavior checklist**

Verify by code inspection or emulator if available:

- Video generator defaults every generated static and animated sticker to selected.
- Tapping a generated card toggles selection and changes border.
- Saving only static creates one static pack.
- Saving only animated creates one animated pack.
- Saving mixed content creates two packs with `Static` and `Animated` postfixes.
- Animated preview shows one running preview card per animated sticker.
- FAB shows circular progress during save.
- Screen content is blocked during bottom FAB loading.
- Text fields remain visible above the keyboard on video generator and create/edit text-entry surfaces.
