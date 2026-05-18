# Video To Sticker Pack Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an Android-first video-to-sticker-pack flow where the app turns a trimmed video segment into up to two `4x4` candidate grid images, sends them to a new API Agent endpoint, previews the returned `4x4` sticker grid, supports regenerate from the same candidates, and saves only after confirmation.

**Architecture:** The Android app owns video selection, max-60-second trimming, frame extraction, lightweight quality filtering, candidate-grid composition, preview, regenerate, and pack saving. The API owns multimodal AI Agent reasoning and final sticker grid generation through a new `/api/v1/generate/video-sticker-pack` endpoint. The app uploads grid images instead of raw video, then reuses existing grid-split and draft-save paths.

**Tech Stack:** Kotlin Multiplatform, Jetpack Compose, Koin, Ktor multipart, Android bitmap/media APIs, existing `StickerFileStorage`, existing `StickerApiRepository`, Express, TypeScript, Zod, Multer, OpenRouter, Sharp, Vitest.

**Execution note:** Do not commit during execution unless the user explicitly asks. The writing-plans skill normally recommends frequent commits, but this repository instruction takes precedence.

---

## File Structure

### API Repo: `C:\Users\iqbal.shafiq\VsCodeProjects\stiker-api`

- Modify: `src/utils/validators.ts`
  - Add `generateVideoStickerPackSchema` for multipart body fields.
- Create: `src/utils/video-sticker-pack.ts`
  - Normalize body values, validate selected range, define constants, normalize Agent JSON output.
- Modify: `src/controllers/generate.controller.ts`
  - Add `generateVideoStickerPack` controller method.
  - Add prompt builder helpers for video candidate-grid analysis.
- Modify: `src/services/openrouter.service.ts`
  - Add `buildVideoStickerPackPrompt` Agent helper.
- Modify: `src/app.ts`
  - Register `POST /api/v1/generate/video-sticker-pack` with `upload.array('candidate_grids', 2)` and request validation.
- Modify: `docs/openapi.json`
  - Document the new endpoint after implementation.
- Test: `tests/unit/utils/video-sticker-pack.test.ts`
- Test: `tests/unit/utils/validators-video-sticker-pack.test.ts`

### App Repo: `C:\Users\iqbal.shafiq\AndroidStudioProjects\setiker`

- Create: `composeApp/src/commonMain/kotlin/domain/model/VideoStickerPackModels.kt`
  - Shared models for selected range, candidate grid, and generation result.
- Create: `composeApp/src/commonMain/kotlin/domain/util/VideoStickerPackPlanner.kt`
  - Pure helpers for segment validation, sample timestamps, and batching candidate frames into grids.
- Create: `composeApp/src/commonMain/kotlin/data/video/VideoFrameCandidateExtractor.kt`
  - Common interface and result models.
- Create: `composeApp/src/androidMain/kotlin/data/video/AndroidVideoFrameCandidateExtractor.kt`
  - Android implementation using existing `StickerFileStorage.extractVideoFrameToFile()` and bitmap scoring.
- Create: `composeApp/src/commonMain/kotlin/data/video/CandidateGridComposer.kt`
  - Common interface for grid composition.
- Create: `composeApp/src/androidMain/kotlin/data/video/AndroidCandidateGridComposer.kt`
  - Android bitmap implementation for `1536x1536` `4x4` candidate grids with `A1`-`D4` labels.
- Modify: `composeApp/src/commonMain/kotlin/data/remote/SetikerApiService.kt`
  - Add multipart call for `/api/v1/generate/video-sticker-pack`.
- Modify: `composeApp/src/commonMain/kotlin/data/remote/StickerApiRepository.kt`
  - Add `generateVideoStickerPack(...)` that downloads output grid and splits with `4x4`.
- Create: `composeApp/src/commonMain/kotlin/presentation/videostickerpack/VideoStickerPackState.kt`
- Create: `composeApp/src/commonMain/kotlin/presentation/videostickerpack/VideoStickerPackIntent.kt`
- Create: `composeApp/src/commonMain/kotlin/presentation/videostickerpack/VideoStickerPackEffect.kt`
- Create: `composeApp/src/commonMain/kotlin/presentation/videostickerpack/VideoStickerPackViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/presentation/videostickerpack/VideoStickerPackScreen.kt`
- Create: `composeApp/src/commonMain/kotlin/presentation/videostickerpack/VideoStickerPackScreenRoot.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/navigation/AppNavigation.kt`
  - Add route and navigation handling.
- Modify: `composeApp/src/commonMain/kotlin/presentation/home/HomeIntent.kt`
  - Add video-to-pack entry intent.
- Modify: `composeApp/src/commonMain/kotlin/presentation/home/HomeEffect.kt`
  - Add navigation effect.
- Modify: `composeApp/src/commonMain/kotlin/presentation/home/HomeViewModel.kt`
  - Emit navigation effect for the new entry action.
- Modify: `composeApp/src/commonMain/kotlin/presentation/home/HomeScreen.kt`
  - Use `rememberVideoPicker` and route selected video to the new flow.
- Modify: `composeApp/src/commonMain/kotlin/presentation/components/HomeBottomBar.kt`
  - Add an additional video generation action.
- Modify: `composeApp/src/commonMain/kotlin/di/AppModule.kt`
  - Register new ViewModel and interfaces.
- Modify: `composeApp/src/androidMain/kotlin/di/AppModule.android.kt`
  - Register Android extractor and grid composer actual implementations.
- Modify: `composeApp/src/commonMain/composeResources/values/strings.xml`
  - Add labels/errors for the flow.
- Test: `composeApp/src/commonTest/kotlin/domain/util/VideoStickerPackPlannerTest.kt`
- Test: `composeApp/src/androidUnitTest/kotlin/presentation/videostickerpack/VideoStickerPackViewModelTest.kt`

---

### Task 1: API Validation And Video Pack Utilities

**Files:**

- Create: `C:\Users\iqbal.shafiq\VsCodeProjects\stiker-api\src\utils\video-sticker-pack.ts`
- Modify: `C:\Users\iqbal.shafiq\VsCodeProjects\stiker-api\src\utils\validators.ts`
- Test: `C:\Users\iqbal.shafiq\VsCodeProjects\stiker-api\tests\unit\utils\video-sticker-pack.test.ts`
- Test: `C:\Users\iqbal.shafiq\VsCodeProjects\stiker-api\tests\unit\utils\validators-video-sticker-pack.test.ts`

- [ ] **Step 1: Write utility tests**

Create `tests/unit/utils/video-sticker-pack.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import {
  VIDEO_STICKER_PACK_INPUT_LAYOUT,
  VIDEO_STICKER_PACK_MAX_CANDIDATES,
  VIDEO_STICKER_PACK_MAX_GRIDS,
  VIDEO_STICKER_PACK_MAX_SEGMENT_MS,
  normalizeVideoStickerPackAgentPlan,
  validateVideoStickerPackRequestShape,
} from '../../../src/utils/video-sticker-pack';

describe('video sticker pack utils', () => {
  it('defines MVP limits', () => {
    expect(VIDEO_STICKER_PACK_INPUT_LAYOUT).toBe('4x4');
    expect(VIDEO_STICKER_PACK_MAX_CANDIDATES).toBe(32);
    expect(VIDEO_STICKER_PACK_MAX_GRIDS).toBe(2);
    expect(VIDEO_STICKER_PACK_MAX_SEGMENT_MS).toBe(60_000);
  });

  it('accepts one or two candidate grids with a 60 second segment', () => {
    expect(() =>
      validateVideoStickerPackRequestShape({
        candidateGridCount: 2,
        candidateCount: 32,
        selectedStartMs: 15_000,
        selectedEndMs: 75_000,
      })
    ).not.toThrow();
  });

  it('rejects more than two candidate grids', () => {
    expect(() =>
      validateVideoStickerPackRequestShape({
        candidateGridCount: 3,
        candidateCount: 32,
        selectedStartMs: 0,
        selectedEndMs: 60_000,
      })
    ).toThrow('At most 2 candidate grid images are allowed');
  });

  it('rejects more than 32 candidates', () => {
    expect(() =>
      validateVideoStickerPackRequestShape({
        candidateGridCount: 2,
        candidateCount: 33,
        selectedStartMs: 0,
        selectedEndMs: 60_000,
      })
    ).toThrow('candidateCount must be between 1 and 32');
  });

  it('rejects selected segment longer than 60 seconds', () => {
    expect(() =>
      validateVideoStickerPackRequestShape({
        candidateGridCount: 1,
        candidateCount: 16,
        selectedStartMs: 0,
        selectedEndMs: 60_001,
      })
    ).toThrow('Selected video segment must be at most 60000 ms');
  });

  it('normalizes agent JSON with selected cell ids and reasoning', () => {
    const plan = normalizeVideoStickerPackAgentPlan(
      JSON.stringify({
        generationPrompt: 'Create a cohesive expressive reaction sticker pack.',
        selectedCells: ['A1', 'B2', 'D4'],
        selectionReasoning: 'Picked sharp, expressive, visually distinct frames.',
      })
    );

    expect(plan).toEqual({
      generationPrompt: 'Create a cohesive expressive reaction sticker pack.',
      selectedCells: ['A1', 'B2', 'D4'],
      selectionReasoning: 'Picked sharp, expressive, visually distinct frames.',
    });
  });
});
```

- [ ] **Step 2: Write validator tests**

Create `tests/unit/utils/validators-video-sticker-pack.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import { generateVideoStickerPackSchema } from '../../../src/utils/validators';

describe('generateVideoStickerPackSchema', () => {
  it('accepts valid multipart body fields', () => {
    const out = generateVideoStickerPackSchema.parse({
      layout: '4x4',
      candidateLayout: '4x4',
      candidateCount: '32',
      selectedStartMs: '12000',
      selectedEndMs: '72000',
      sourceDurationMs: '180000',
      prompt: 'Make expressive WhatsApp stickers',
    });

    expect(out.candidateCount).toBe(32);
    expect(out.selectedStartMs).toBe(12_000);
    expect(out.selectedEndMs).toBe(72_000);
    expect(out.layout).toBe('4x4');
  });

  it('rejects non-4x4 output layout for MVP', () => {
    expect(() =>
      generateVideoStickerPackSchema.parse({
        layout: '5x5',
        candidateLayout: '4x4',
        candidateCount: '25',
        selectedStartMs: '0',
        selectedEndMs: '30000',
      })
    ).toThrow();
  });

  it('rejects non-4x4 candidate layout for MVP', () => {
    expect(() =>
      generateVideoStickerPackSchema.parse({
        layout: '4x4',
        candidateLayout: '5x5',
        candidateCount: '25',
        selectedStartMs: '0',
        selectedEndMs: '30000',
      })
    ).toThrow();
  });
});
```

- [ ] **Step 3: Run tests and verify they fail**

Run in `C:\Users\iqbal.shafiq\VsCodeProjects\stiker-api`:

`npm test -- tests/unit/utils/video-sticker-pack.test.ts tests/unit/utils/validators-video-sticker-pack.test.ts`

Expected: FAIL because `video-sticker-pack.ts` and `generateVideoStickerPackSchema` do not exist.

- [ ] **Step 4: Add video sticker pack utility module**

Create `src/utils/video-sticker-pack.ts`:

```ts
export const VIDEO_STICKER_PACK_INPUT_LAYOUT = '4x4';
export const VIDEO_STICKER_PACK_OUTPUT_LAYOUT = '4x4';
export const VIDEO_STICKER_PACK_MAX_GRIDS = 2;
export const VIDEO_STICKER_PACK_MAX_CANDIDATES = 32;
export const VIDEO_STICKER_PACK_MAX_SEGMENT_MS = 60_000;

export interface VideoStickerPackRequestShape {
  candidateGridCount: number;
  candidateCount: number;
  selectedStartMs: number;
  selectedEndMs: number;
}

export interface VideoStickerPackAgentPlan {
  generationPrompt: string;
  selectedCells: string[];
  selectionReasoning?: string;
}

export function validateVideoStickerPackRequestShape(input: VideoStickerPackRequestShape): void {
  if (input.candidateGridCount < 1) {
    throw new Error('At least one candidate grid image is required');
  }
  if (input.candidateGridCount > VIDEO_STICKER_PACK_MAX_GRIDS) {
    throw new Error('At most 2 candidate grid images are allowed');
  }
  if (input.candidateCount < 1 || input.candidateCount > VIDEO_STICKER_PACK_MAX_CANDIDATES) {
    throw new Error('candidateCount must be between 1 and 32');
  }
  const segmentMs = input.selectedEndMs - input.selectedStartMs;
  if (segmentMs <= 0) {
    throw new Error('Selected video segment must be greater than 0 ms');
  }
  if (segmentMs > VIDEO_STICKER_PACK_MAX_SEGMENT_MS) {
    throw new Error('Selected video segment must be at most 60000 ms');
  }
}

export function normalizeVideoStickerPackAgentPlan(content: string): VideoStickerPackAgentPlan {
  const parsed = parseJsonObject(content);
  const generationPrompt = typeof parsed.generationPrompt === 'string'
    ? parsed.generationPrompt.trim()
    : '';
  if (!generationPrompt) {
    throw new Error('Video sticker pack agent did not return generationPrompt');
  }
  const selectedCells = Array.isArray(parsed.selectedCells)
    ? parsed.selectedCells
        .filter((value): value is string => typeof value === 'string')
        .map(value => value.trim().toUpperCase())
        .filter(Boolean)
    : [];
  const selectionReasoning = typeof parsed.selectionReasoning === 'string'
    ? parsed.selectionReasoning.trim()
    : undefined;
  return {
    generationPrompt,
    selectedCells,
    ...(selectionReasoning ? { selectionReasoning } : {}),
  };
}

function parseJsonObject(content: string): Record<string, unknown> {
  const trimmed = content.trim();
  const withoutFence = trimmed
    .replace(/^```(?:json)?\s*/i, '')
    .replace(/\s*```$/i, '')
    .trim();

  return JSON.parse(withoutFence) as Record<string, unknown>;
}
```

- [ ] **Step 5: Add request schema**

Modify `src/utils/validators.ts`:

```ts
import { z } from 'zod';
import { tryParseGridLayout } from './grid-layout';
import {
  VIDEO_STICKER_PACK_INPUT_LAYOUT,
  VIDEO_STICKER_PACK_MAX_CANDIDATES,
  VIDEO_STICKER_PACK_MAX_SEGMENT_MS,
  VIDEO_STICKER_PACK_OUTPUT_LAYOUT,
} from './video-sticker-pack';
```

Add below `generateStickerPackSchema`:

```ts
const optionalNonNegativeInt = z.preprocess((val: unknown) => {
  if (val === '' || val === null || val === undefined) {
    return undefined;
  }
  return val;
}, z.coerce.number().int().nonnegative().optional());

export const generateVideoStickerPackSchema = z
  .object({
    layout: z.string().max(32).default(VIDEO_STICKER_PACK_OUTPUT_LAYOUT),
    candidateLayout: z.string().max(32).default(VIDEO_STICKER_PACK_INPUT_LAYOUT),
    candidateCount: z.coerce.number().int().min(1).max(VIDEO_STICKER_PACK_MAX_CANDIDATES),
    selectedStartMs: z.coerce.number().int().nonnegative(),
    selectedEndMs: z.coerce.number().int().positive(),
    sourceDurationMs: optionalNonNegativeInt,
    prompt: z.string().max(2000).optional(),
  })
  .superRefine((data, ctx) => {
    if (data.layout !== VIDEO_STICKER_PACK_OUTPUT_LAYOUT) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: 'Video sticker pack output layout must be 4x4 for MVP',
        path: ['layout'],
      });
    }
    if (data.candidateLayout !== VIDEO_STICKER_PACK_INPUT_LAYOUT) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: 'Video sticker pack candidate layout must be 4x4 for MVP',
        path: ['candidateLayout'],
      });
    }
    if (data.selectedEndMs <= data.selectedStartMs) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: 'selectedEndMs must be greater than selectedStartMs',
        path: ['selectedEndMs'],
      });
    }
    if (data.selectedEndMs - data.selectedStartMs > VIDEO_STICKER_PACK_MAX_SEGMENT_MS) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: 'Selected video segment must be at most 60000 ms',
        path: ['selectedEndMs'],
      });
    }
  });

export type GenerateVideoStickerPackInput = z.infer<typeof generateVideoStickerPackSchema>;
```

- [ ] **Step 6: Run tests and verify they pass**

Run in `C:\Users\iqbal.shafiq\VsCodeProjects\stiker-api`:

`npm test -- tests/unit/utils/video-sticker-pack.test.ts tests/unit/utils/validators-video-sticker-pack.test.ts`

Expected: PASS.

---

### Task 2: API Agent Endpoint

**Files:**

- Modify: `C:\Users\iqbal.shafiq\VsCodeProjects\stiker-api\src\services\openrouter.service.ts`
- Modify: `C:\Users\iqbal.shafiq\VsCodeProjects\stiker-api\src\controllers\generate.controller.ts`
- Modify: `C:\Users\iqbal.shafiq\VsCodeProjects\stiker-api\src\app.ts`
- Modify: `C:\Users\iqbal.shafiq\VsCodeProjects\stiker-api\docs\openapi.json`

- [ ] **Step 1: Add OpenRouter video pack agent method**

Modify `src/services/openrouter.service.ts` imports:

```ts
import {
  normalizeVideoStickerPackAgentPlan,
  type VideoStickerPackAgentPlan,
} from '../utils/video-sticker-pack';
```

Add method inside `OpenRouterService` after `buildImprovementPrompt`:

```ts
  async buildVideoStickerPackPrompt(input: {
    candidateGrids: ImageGenerationInput[];
    candidateCount: number;
    selectedStartMs: number;
    selectedEndMs: number;
    userPrompt?: string;
  }): Promise<{
    plan: VideoStickerPackAgentPlan;
    generationId: string;
    metadata: {
      tokensPrompt?: number;
      tokensCompletion?: number;
      cost?: number;
      latencyMs?: number;
    };
  }> {
    const content: Array<{ type: string; text?: string; image_url?: { url: string } }> = [
      {
        type: 'text',
        text: this.buildVideoStickerPackAgentPrompt(input),
      },
    ];

    for (const image of input.candidateGrids) {
      const mime = image.mimeType && /^image\/[a-z0-9.+-]+$/i.test(image.mimeType)
        ? image.mimeType
        : 'image/png';
      content.push({
        type: 'image_url',
        image_url: { url: `data:${mime};base64,${image.buffer.toString('base64')}` },
      });
    }

    const { content: rawContent, generationId, metadata } = await this.chatCompletion({
      model: config.models.improvementAgent,
      messages: [
        {
          role: 'system',
          content: 'You are a senior sticker art director and video-frame curation agent. Return ONLY JSON. Do not include markdown.',
        },
        {
          role: 'user',
          content,
        },
      ],
      responseFormat: { type: 'json_object' },
      timeoutMs: 45_000,
    });

    return {
      plan: normalizeVideoStickerPackAgentPlan(rawContent),
      generationId,
      metadata,
    };
  }

  private buildVideoStickerPackAgentPrompt(input: {
    candidateCount: number;
    selectedStartMs: number;
    selectedEndMs: number;
    userPrompt?: string;
  }): string {
    const userPrompt = input.userPrompt?.trim();
    return `Analyze the uploaded candidate frame grid image(s) from one user-selected video segment.

The candidate grids use a 4x4 layout with subtle cell labels A1-D4. There are ${input.candidateCount} candidate frames total. The selected video segment is ${input.selectedStartMs} ms to ${input.selectedEndMs} ms.

${userPrompt ? `User style note: ${userPrompt}` : 'User style note: make the final result expressive, clean, and WhatsApp-sticker-ready.'}

Return exact JSON:
{
  "generationPrompt": "string",
  "selectedCells": ["A1", "B2"],
  "selectionReasoning": "string"
}

Rules:
- Pick up to 16 frames that are sharp, expressive, visually distinct, and suitable as standalone WhatsApp stickers.
- Avoid near-duplicates, blurry frames, awkward motion frames, closed-eye frames unless funny, and frames with unreadable subjects.
- Prefer emotional range, gesture variety, face/body clarity, and frames that communicate a reaction.
- The generationPrompt must ask the image model to create one cohesive 4x4 square sticker grid using the selected frame concepts.
- The final grid should contain at most 16 stickers, safe margins, strong subject separation, clean sticker composition, and readable short captions where helpful.
- Do not ask for separate files. Ask for exactly one square 4x4 grid image.`;
  }
```

- [ ] **Step 2: Add controller helper prompt**

Modify `src/controllers/generate.controller.ts` imports:

```ts
import {
  VIDEO_STICKER_PACK_INPUT_LAYOUT,
  VIDEO_STICKER_PACK_MAX_CANDIDATES,
  VIDEO_STICKER_PACK_MAX_GRIDS,
  VIDEO_STICKER_PACK_OUTPUT_LAYOUT,
  validateVideoStickerPackRequestShape,
} from '../utils/video-sticker-pack';
```

Add helper near existing prompt builders:

```ts
function buildVideoStickerPackGenerationPrompt(agentPrompt: string): string {
  return `${agentPrompt}

Hard output requirements:
- Output exactly one square 4x4 grid image.
- Use only the selected video frame concepts from the candidate grids.
- Generate at most 16 stickers.
- Keep each sticker fully inside its cell with safe margins.
- Keep clear gutters or visual separation between cells.
- Make every used cell expressive, readable, and sticker-ready.
- Avoid blurry subjects, cropped faces, and rectangular backdrops.
- Use a cohesive visual style across the grid.`;
}
```

- [ ] **Step 3: Add controller method**

Add method inside `GenerateController` before `requireUserId`:

```ts
  async generateVideoStickerPack(req: AuthRequest, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = this.requireUserId(req);
      const body = req.body as Record<string, unknown>;
      const files = Array.isArray(req.files) ? req.files : [];
      const candidateCount = Number(body.candidateCount);
      const selectedStartMs = Number(body.selectedStartMs);
      const selectedEndMs = Number(body.selectedEndMs);
      const sourceDurationMs = body.sourceDurationMs == null ? undefined : Number(body.sourceDurationMs);
      const prompt = typeof body.prompt === 'string' ? body.prompt.trim() : undefined;

      validateVideoStickerPackRequestShape({
        candidateGridCount: files.length,
        candidateCount,
        selectedStartMs,
        selectedEndMs,
      });

      const candidateGrids = files.map(file => this.fileToImageInput(file));
      const requestTimestamp = Date.now();
      const agent = await this.openRouterService.buildVideoStickerPackPrompt({
        candidateGrids,
        candidateCount,
        selectedStartMs,
        selectedEndMs,
        userPrompt: prompt,
      });

      const { imageBuffer, metadata: aiMetadata } =
        await this.openRouterService.generateImageWithInputs(
          buildVideoStickerPackGenerationPrompt(agent.plan.generationPrompt),
          this.toBase64Inputs(candidateGrids),
          config.models.imageGeneration,
          false
        );

      const imageResult = await this.saveGeneratedImage({
        imageBuffer,
        userId,
        subDir: `generate-video-sticker-pack/${requestTimestamp}`,
        baseName: 'generated-video-sticker-pack',
      });
      imageResult.textAssetDecoration = buildEmptyTextAssetDecoration(
        'detected',
        'Captions embedded per selected video sticker cell'
      );

      await this.recordGenerateHistory(userId, {
        inputData: {
          mode: 'video-sticker-pack',
          candidateGridCount: files.length,
          candidateCount,
          selectedStartMs,
          selectedEndMs,
          sourceDurationMs,
          inputLayout: VIDEO_STICKER_PACK_INPUT_LAYOUT,
          outputLayout: VIDEO_STICKER_PACK_OUTPUT_LAYOUT,
          maxCandidates: VIDEO_STICKER_PACK_MAX_CANDIDATES,
          maxCandidateGrids: VIDEO_STICKER_PACK_MAX_GRIDS,
          selectedCells: agent.plan.selectedCells,
        },
        images: [imageResult],
      });

      const metadata: GenerationMetadata = {
        model: config.models.imageGeneration,
        improvementAgentModel: config.models.improvementAgent,
        ...mergeAiMetadata([agent.metadata, aiMetadata]),
        mode: 'video-sticker-pack',
        inputCount: files.length,
        outputCount: 1,
        gridLayout: VIDEO_STICKER_PACK_OUTPUT_LAYOUT,
        cellCount: 16,
        outputSize: '512x512',
        backgroundRemoved: false,
        backgroundRemovalMethod: 'none',
        selectedCells: agent.plan.selectedCells,
        selectionReasoning: agent.plan.selectionReasoning,
      };

      res.status(200).json(buildSuccessResponse({ images: [imageResult], metadata }));
    } catch (error) {
      next(error);
    }
  }
```

- [ ] **Step 4: Register route**

Modify `src/app.ts` imports:

```ts
import { generateImageSchema, generateStickerPackSchema, generateVideoStickerPackSchema } from './utils/validators';
```

Add route after `/api/v1/generate/improvement`:

```ts
// eslint-disable-next-line @typescript-eslint/no-misused-promises
app.post(
  '/api/v1/generate/video-sticker-pack',
  // eslint-disable-next-line @typescript-eslint/no-misused-promises
  authenticateToken,
  upload.array('candidate_grids', 2),
  validateRequest(generateVideoStickerPackSchema),
  asyncHandler((req, res, next) => generateController.generateVideoStickerPack(req, res, next))
);
```

- [ ] **Step 5: Run API verification**

Run in `C:\Users\iqbal.shafiq\VsCodeProjects\stiker-api`:

`npm test -- tests/unit/utils/video-sticker-pack.test.ts tests/unit/utils/validators-video-sticker-pack.test.ts`

Expected: PASS.

Run:

`npm run typecheck`

Expected: PASS.

- [ ] **Step 6: Update OpenAPI document**

Modify `docs/openapi.json` by adding path `/api/v1/generate/video-sticker-pack` under `paths`. Use the existing `/api/v1/generate/improvement` style and document multipart fields `candidate_grids`, `layout`, `candidateLayout`, `candidateCount`, `selectedStartMs`, `selectedEndMs`, `sourceDurationMs`, and optional `prompt`.

---

### Task 3: App Pure Planning Models And Tests

**Files:**

- Create: `composeApp/src/commonMain/kotlin/domain/model/VideoStickerPackModels.kt`
- Create: `composeApp/src/commonMain/kotlin/domain/util/VideoStickerPackPlanner.kt`
- Test: `composeApp/src/commonTest/kotlin/domain/util/VideoStickerPackPlannerTest.kt`

- [ ] **Step 1: Write planner tests**

Create `composeApp/src/commonTest/kotlin/domain/util/VideoStickerPackPlannerTest.kt`:

```kotlin
package domain.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class VideoStickerPackPlannerTest {
    @Test
    fun validateRangeAcceptsSixtySeconds() {
        VideoStickerPackPlanner.validateSelectedRange(10_000L, 70_000L)
    }

    @Test
    fun validateRangeRejectsLongerThanSixtySeconds() {
        assertFailsWith<IllegalArgumentException> {
            VideoStickerPackPlanner.validateSelectedRange(0L, 60_001L)
        }
    }

    @Test
    fun sampleTimestampsAreUniformAndInsideRange() {
        val timestamps = VideoStickerPackPlanner.buildSampleTimestamps(
            startMs = 1_000L,
            endMs = 61_000L,
            sampleCount = 5
        )

        assertEquals(listOf(1_000L, 16_000L, 31_000L, 46_000L, 61_000L), timestamps)
    }

    @Test
    fun candidatePathsAreCappedAtThirtyTwo() {
        val paths = (1..40).map { "/tmp/$it.png" }
        assertEquals(32, VideoStickerPackPlanner.capCandidatePaths(paths).size)
    }

    @Test
    fun candidatesBatchIntoTwoFourByFourGrids() {
        val paths = (1..32).map { "/tmp/$it.png" }
        val batches = VideoStickerPackPlanner.batchCandidatePathsForGrids(paths)

        assertEquals(2, batches.size)
        assertTrue(batches.all { it.size == 16 })
    }
}
```

- [ ] **Step 2: Run test and verify it fails**

Run in app repo:

`./gradlew :composeApp:allTests --tests domain.util.VideoStickerPackPlannerTest`

Expected: FAIL because planner does not exist.

- [ ] **Step 3: Add shared models**

Create `composeApp/src/commonMain/kotlin/domain/model/VideoStickerPackModels.kt`:

```kotlin
package domain.model

data class VideoStickerPackRange(
    val startMs: Long,
    val endMs: Long,
    val sourceDurationMs: Long
) {
    val durationMs: Long get() = endMs - startMs
}

data class VideoFrameCandidate(
    val filePath: String,
    val timestampMs: Long,
    val sharpnessScore: Double,
    val brightnessScore: Double,
    val differenceScore: Double
)

data class CandidateGridImage(
    val filePath: String,
    val frameCount: Int,
    val layout: String = "4x4"
)
```

- [ ] **Step 4: Add planner**

Create `composeApp/src/commonMain/kotlin/domain/util/VideoStickerPackPlanner.kt`:

```kotlin
package domain.util

object VideoStickerPackPlanner {
    const val MAX_SEGMENT_MS: Long = 60_000L
    const val MAX_CANDIDATES: Int = 32
    const val GRID_CELL_COUNT: Int = 16
    const val GRID_LAYOUT: String = "4x4"
    const val DEFAULT_RAW_SAMPLE_COUNT: Int = 64

    fun validateSelectedRange(startMs: Long, endMs: Long) {
        require(startMs >= 0L) { "Selected start must be non-negative" }
        require(endMs > startMs) { "Selected end must be greater than start" }
        require(endMs - startMs <= MAX_SEGMENT_MS) { "Selected video segment must be at most 60000 ms" }
    }

    fun buildSampleTimestamps(
        startMs: Long,
        endMs: Long,
        sampleCount: Int = DEFAULT_RAW_SAMPLE_COUNT
    ): List<Long> {
        validateSelectedRange(startMs, endMs)
        val count = sampleCount.coerceAtLeast(1)
        if (count == 1) return listOf(startMs)
        val span = endMs - startMs
        return (0 until count).map { index ->
            startMs + index.toLong() * span / (count - 1)
        }
    }

    fun capCandidatePaths(paths: List<String>): List<String> = paths.take(MAX_CANDIDATES)

    fun batchCandidatePathsForGrids(paths: List<String>): List<List<String>> =
        capCandidatePaths(paths)
            .chunked(GRID_CELL_COUNT)
            .take(2)
}
```

- [ ] **Step 5: Run planner tests**

Run:

`./gradlew :composeApp:allTests --tests domain.util.VideoStickerPackPlannerTest`

Expected: PASS.

---

### Task 4: Android Candidate Extraction And Grid Composition

**Files:**

- Create: `composeApp/src/commonMain/kotlin/data/video/VideoFrameCandidateExtractor.kt`
- Create: `composeApp/src/commonMain/kotlin/data/video/CandidateGridComposer.kt`
- Create: `composeApp/src/androidMain/kotlin/data/video/AndroidVideoFrameCandidateExtractor.kt`
- Create: `composeApp/src/androidMain/kotlin/data/video/AndroidCandidateGridComposer.kt`
- Modify: `composeApp/src/commonMain/kotlin/di/AppModule.kt`
- Modify: `composeApp/src/androidMain/kotlin/di/AppModule.android.kt`

- [ ] **Step 1: Add common interfaces**

Create `composeApp/src/commonMain/kotlin/data/video/VideoFrameCandidateExtractor.kt`:

```kotlin
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
```

Create `composeApp/src/commonMain/kotlin/data/video/CandidateGridComposer.kt`:

```kotlin
package data.video

import domain.model.CandidateGridImage

interface CandidateGridComposer {
    suspend fun composeGrids(candidatePaths: List<String>): List<CandidateGridImage>
}
```

- [ ] **Step 2: Add Android extractor**

Create `composeApp/src/androidMain/kotlin/data/video/AndroidVideoFrameCandidateExtractor.kt`:

```kotlin
package data.video

import android.graphics.BitmapFactory
import data.storage.StickerFileStorage
import domain.model.VideoFrameCandidate
import domain.util.VideoStickerPackPlanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.time.Clock

class AndroidVideoFrameCandidateExtractor(
    private val fileStorage: StickerFileStorage
) : VideoFrameCandidateExtractor {
    override suspend fun extractCandidates(
        videoPath: String,
        startMs: Long,
        endMs: Long,
        onProgress: (current: Int, total: Int) -> Unit
    ): List<VideoFrameCandidate> = withContext(Dispatchers.IO) {
        val timestamps = VideoStickerPackPlanner.buildSampleTimestamps(startMs, endMs)
        val extracted = mutableListOf<VideoFrameCandidate>()
        var previousBrightness: Double? = null
        timestamps.forEachIndexed { index, timestamp ->
            val name = "video_pack_candidate_${Clock.System.now().toEpochMilliseconds()}_$index.png"
            val path = fileStorage.extractVideoFrameToFile(videoPath, timestamp, name)
            if (path != null) {
                val bitmap = BitmapFactory.decodeFile(path)
                if (bitmap != null) {
                    val brightness = scoreBrightness(bitmap)
                    val sharpness = scoreSharpness(bitmap)
                    val difference = previousBrightness?.let { abs(brightness - it) } ?: 1.0
                    previousBrightness = brightness
                    extracted += VideoFrameCandidate(
                        filePath = path,
                        timestampMs = timestamp,
                        sharpnessScore = sharpness,
                        brightnessScore = brightness,
                        differenceScore = difference
                    )
                    bitmap.recycle()
                }
            }
            onProgress(index + 1, timestamps.size)
        }
        rankCandidates(extracted)
    }

    private fun rankCandidates(candidates: List<VideoFrameCandidate>): List<VideoFrameCandidate> {
        val preferred = candidates
            .filter { it.brightnessScore in 0.12..0.92 }
            .filter { it.sharpnessScore >= 0.015 }
            .filter { it.differenceScore >= 0.02 }
            .sortedByDescending { it.sharpnessScore + it.differenceScore }
        val fallback = candidates.sortedByDescending { it.sharpnessScore + it.differenceScore }
        return (preferred + fallback)
            .distinctBy { it.filePath }
            .take(VideoStickerPackPlanner.MAX_CANDIDATES)
            .sortedBy { it.timestampMs }
    }

    private fun scoreBrightness(bitmap: android.graphics.Bitmap): Double {
        val stepX = (bitmap.width / 24).coerceAtLeast(1)
        val stepY = (bitmap.height / 24).coerceAtLeast(1)
        var total = 0.0
        var count = 0
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val color = bitmap.getPixel(x, y)
                val r = android.graphics.Color.red(color)
                val g = android.graphics.Color.green(color)
                val b = android.graphics.Color.blue(color)
                total += (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
                count += 1
                x += stepX
            }
            y += stepY
        }
        return if (count == 0) 0.0 else total / count
    }

    private fun scoreSharpness(bitmap: android.graphics.Bitmap): Double {
        val stepX = (bitmap.width / 24).coerceAtLeast(1)
        val stepY = (bitmap.height / 24).coerceAtLeast(1)
        var totalDiff = 0.0
        var count = 0
        var y = 0
        while (y < bitmap.height - stepY) {
            var x = 0
            while (x < bitmap.width - stepX) {
                val current = luminance(bitmap.getPixel(x, y))
                val right = luminance(bitmap.getPixel(x + stepX, y))
                val down = luminance(bitmap.getPixel(x, y + stepY))
                totalDiff += abs(current - right) + abs(current - down)
                count += 2
                x += stepX
            }
            y += stepY
        }
        return if (count == 0) 0.0 else totalDiff / count
    }

    private fun luminance(color: Int): Double =
        (0.299 * android.graphics.Color.red(color) +
            0.587 * android.graphics.Color.green(color) +
            0.114 * android.graphics.Color.blue(color)) / 255.0
}
```

- [ ] **Step 3: Add Android grid composer**

Create `composeApp/src/androidMain/kotlin/data/video/AndroidCandidateGridComposer.kt`:

```kotlin
package data.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import domain.model.CandidateGridImage
import domain.util.VideoStickerPackPlanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.time.Clock

class AndroidCandidateGridComposer(
    private val context: Context
) : CandidateGridComposer {
    override suspend fun composeGrids(candidatePaths: List<String>): List<CandidateGridImage> = withContext(Dispatchers.IO) {
        VideoStickerPackPlanner.batchCandidatePathsForGrids(candidatePaths).mapIndexed { gridIndex, batch ->
            val bitmap = Bitmap.createBitmap(GRID_SIZE, GRID_SIZE, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            val gutter = 12f
            val cellSize = (GRID_SIZE - gutter * 5) / 4f
            batch.forEachIndexed { index, path ->
                val row = index / 4
                val col = index % 4
                val left = gutter + col * (cellSize + gutter)
                val top = gutter + row * (cellSize + gutter)
                drawCell(canvas, path, RectF(left, top, left + cellSize, top + cellSize), labelFor(index))
            }
            val outDir = File(context.cacheDir, "video_pack_candidate_grids").apply { mkdirs() }
            val outFile = File(outDir, "candidate_grid_${Clock.System.now().toEpochMilliseconds()}_$gridIndex.png")
            FileOutputStream(outFile).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            CandidateGridImage(filePath = outFile.absolutePath, frameCount = batch.size)
        }
    }

    private fun drawCell(canvas: Canvas, imagePath: String, dest: RectF, label: String) {
        val bitmap = BitmapFactory.decodeFile(imagePath) ?: return
        val source = centerCropSource(bitmap)
        canvas.drawBitmap(bitmap, source, dest, null)
        bitmap.recycle()

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRect(dest, borderPaint)

        val labelBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(210, 0, 0, 0) }
        val labelText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 34f
            fakeBoldText = true
        }
        val labelRect = RectF(dest.left + 10f, dest.top + 10f, dest.left + 78f, dest.top + 54f)
        canvas.drawRoundRect(labelRect, 8f, 8f, labelBg)
        canvas.drawText(label, labelRect.left + 10f, labelRect.bottom - 12f, labelText)
    }

    private fun centerCropSource(bitmap: Bitmap): Rect {
        val size = minOf(bitmap.width, bitmap.height)
        val left = (bitmap.width - size) / 2
        val top = (bitmap.height - size) / 2
        return Rect(left, top, left + size, top + size)
    }

    private fun labelFor(index: Int): String {
        val row = listOf('A', 'B', 'C', 'D')[index / 4]
        val col = index % 4 + 1
        return "$row$col"
    }

    private companion object {
        const val GRID_SIZE = 1536
    }
}
```

- [ ] **Step 4: Register dependencies**

Modify `composeApp/src/commonMain/kotlin/di/AppModule.kt` imports:

```kotlin
import presentation.videostickerpack.VideoStickerPackViewModel
```

Add in ViewModels block:

```kotlin
    viewModelOf(::VideoStickerPackViewModel)
```

Modify `composeApp/src/androidMain/kotlin/di/AppModule.android.kt` to provide:

```kotlin
import data.video.AndroidCandidateGridComposer
import data.video.AndroidVideoFrameCandidateExtractor
import data.video.CandidateGridComposer
import data.video.VideoFrameCandidateExtractor
```

Inside the Android Koin module add:

```kotlin
single<VideoFrameCandidateExtractor> { AndroidVideoFrameCandidateExtractor(fileStorage = get()) }
single<CandidateGridComposer> { AndroidCandidateGridComposer(context = get()) }
```

- [ ] **Step 5: Compile Android source**

Run:

`./gradlew :composeApp:compileDebugKotlinAndroid`

Expected: build reaches unresolved `VideoStickerPackViewModel` until Task 6 creates it, or passes if Task 6 has already been implemented.

---

### Task 5: App API Repository Method

**Files:**

- Modify: `composeApp/src/commonMain/kotlin/data/remote/SetikerApiService.kt`
- Modify: `composeApp/src/commonMain/kotlin/data/remote/StickerApiRepository.kt`

- [ ] **Step 1: Add service method**

Modify `SetikerApiService.kt` with a new public method after `generateStickerPack`:

```kotlin
    suspend fun generateVideoStickerPack(
        candidateGridPaths: List<String>,
        candidateCount: Int,
        selectedStartMs: Long,
        selectedEndMs: Long,
        sourceDurationMs: Long,
        prompt: String? = null
    ): List<ApiImage> {
        val response = withAuthRetry { authHeader ->
            client.post("/api/v1/generate/video-sticker-pack") {
                authHeader?.let { header(HttpHeaders.Authorization, it) }
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("layout", "4x4")
                            append("candidateLayout", "4x4")
                            append("candidateCount", candidateCount.toString())
                            append("selectedStartMs", selectedStartMs.toString())
                            append("selectedEndMs", selectedEndMs.toString())
                            append("sourceDurationMs", sourceDurationMs.toString())
                            if (!prompt.isNullOrBlank()) append("prompt", prompt)
                            candidateGridPaths.forEachIndexed { index, path ->
                                appendImageFile(
                                    key = "candidate_grids",
                                    path = path,
                                    filename = "candidate_grid_${index + 1}.png"
                                )
                            }
                        }
                    )
                )
            }
        }
        return parseGenerateImages(response)
    }
```

- [ ] **Step 2: Add repository method**

Modify `StickerApiRepository.kt` with:

```kotlin
    suspend fun generateVideoStickerPack(
        candidateGridPaths: List<String>,
        candidateCount: Int,
        selectedStartMs: Long,
        selectedEndMs: Long,
        sourceDurationMs: Long,
        prompt: String? = null
    ): List<GridSplitStickerFile> {
        val images = api.generateVideoStickerPack(
            candidateGridPaths = candidateGridPaths,
            candidateCount = candidateCount,
            selectedStartMs = selectedStartMs,
            selectedEndMs = selectedEndMs,
            sourceDurationMs = sourceDurationMs,
            prompt = prompt
        )
        val rawGridPath = images.firstOrNull()?.let { downloadAndPersist(it) }
            ?: return emptyList()
        return splitGridOnDevice(rawGridPath, "4x4")
    }
```

- [ ] **Step 3: Compile API call changes**

Run:

`./gradlew :composeApp:compileDebugKotlinAndroid`

Expected: compilation passes once Task 6 ViewModel dependencies exist.

---

### Task 6: Video Sticker Pack MVI Flow And Navigation

**Files:**

- Create: `composeApp/src/commonMain/kotlin/presentation/videostickerpack/VideoStickerPackState.kt`
- Create: `composeApp/src/commonMain/kotlin/presentation/videostickerpack/VideoStickerPackIntent.kt`
- Create: `composeApp/src/commonMain/kotlin/presentation/videostickerpack/VideoStickerPackEffect.kt`
- Create: `composeApp/src/commonMain/kotlin/presentation/videostickerpack/VideoStickerPackViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/presentation/videostickerpack/VideoStickerPackScreen.kt`
- Create: `composeApp/src/commonMain/kotlin/presentation/videostickerpack/VideoStickerPackScreenRoot.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/navigation/AppNavigation.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/home/HomeIntent.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/home/HomeEffect.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/home/HomeViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/home/HomeScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/presentation/components/HomeBottomBar.kt`
- Modify: `composeApp/src/commonMain/composeResources/values/strings.xml`
- Test: `composeApp/src/androidUnitTest/kotlin/presentation/videostickerpack/VideoStickerPackViewModelTest.kt`

- [ ] **Step 1: Add state, intent, and effect**

Create `VideoStickerPackState.kt`:

```kotlin
package presentation.videostickerpack

import data.remote.model.GridSplitStickerFile
import domain.model.CandidateGridImage
import domain.model.VideoFrameCandidate

data class VideoStickerPackState(
    val videoPath: String = "",
    val sourceDurationMs: Long = 0L,
    val selectedStartMs: Long = 0L,
    val selectedEndMs: Long = 0L,
    val packName: String = "",
    val publisher: String = "",
    val isLoadingVideo: Boolean = false,
    val isProcessing: Boolean = false,
    val processingLabel: String? = null,
    val processingProgress: Float = 0f,
    val candidates: List<VideoFrameCandidate> = emptyList(),
    val candidateGrids: List<CandidateGridImage> = emptyList(),
    val generatedStickers: List<GridSplitStickerFile> = emptyList(),
    val errorMessage: String? = null
) {
    val selectedDurationMs: Long get() = selectedEndMs - selectedStartMs
    val canGenerate: Boolean get() = videoPath.isNotBlank() && selectedDurationMs in 1L..60_000L && !isProcessing
    val canSave: Boolean get() = generatedStickers.isNotEmpty() && packName.isNotBlank() && publisher.isNotBlank() && !isProcessing
}
```

Create `VideoStickerPackIntent.kt`:

```kotlin
package presentation.videostickerpack

sealed interface VideoStickerPackIntent {
    data class LoadVideo(val path: String) : VideoStickerPackIntent
    data class UpdateStart(val ms: Long) : VideoStickerPackIntent
    data class UpdateEnd(val ms: Long) : VideoStickerPackIntent
    data class UpdatePackName(val value: String) : VideoStickerPackIntent
    data class UpdatePublisher(val value: String) : VideoStickerPackIntent
    data object Generate : VideoStickerPackIntent
    data object Regenerate : VideoStickerPackIntent
    data object SavePack : VideoStickerPackIntent
    data object Cancel : VideoStickerPackIntent
}
```

Create `VideoStickerPackEffect.kt`:

```kotlin
package presentation.videostickerpack

import presentation.common.UiText

sealed interface VideoStickerPackEffect {
    data class NavigateToPackDetail(val packId: String) : VideoStickerPackEffect
    data object NavigateBack : VideoStickerPackEffect
    data class ShowError(val message: UiText) : VideoStickerPackEffect
}
```

- [ ] **Step 2: Add ViewModel**

Create `VideoStickerPackViewModel.kt` with orchestration:

```kotlin
package presentation.videostickerpack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.remote.StickerApiRepository
import data.repository.StickerPackDraftSaver
import data.storage.StickerFileStorage
import data.video.CandidateGridComposer
import data.video.VideoFrameCandidateExtractor
import domain.model.StickerDraftInput
import domain.repository.StickerRepository
import domain.util.VideoStickerPackPlanner
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import presentation.common.PackIdentifierSanitizer
import presentation.common.UiText
import kotlin.random.Random

class VideoStickerPackViewModel(
    private val fileStorage: StickerFileStorage,
    private val extractor: VideoFrameCandidateExtractor,
    private val gridComposer: CandidateGridComposer,
    private val apiRepository: StickerApiRepository,
    private val stickerRepository: StickerRepository,
    private val draftSaver: StickerPackDraftSaver
) : ViewModel() {
    private val _state = MutableStateFlow(VideoStickerPackState())
    val state: StateFlow<VideoStickerPackState> = _state.asStateFlow()

    private val _effect = Channel<VideoStickerPackEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: VideoStickerPackIntent) {
        when (intent) {
            is VideoStickerPackIntent.LoadVideo -> loadVideo(intent.path)
            is VideoStickerPackIntent.UpdateStart -> updateRange(startMs = intent.ms)
            is VideoStickerPackIntent.UpdateEnd -> updateRange(endMs = intent.ms)
            is VideoStickerPackIntent.UpdatePackName -> _state.update { it.copy(packName = intent.value) }
            is VideoStickerPackIntent.UpdatePublisher -> _state.update { it.copy(publisher = intent.value) }
            VideoStickerPackIntent.Generate -> generate(extractFreshCandidates = true)
            VideoStickerPackIntent.Regenerate -> generate(extractFreshCandidates = false)
            VideoStickerPackIntent.SavePack -> savePack()
            VideoStickerPackIntent.Cancel -> viewModelScope.launch { _effect.send(VideoStickerPackEffect.NavigateBack) }
        }
    }

    private fun loadVideo(path: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingVideo = true, videoPath = path, errorMessage = null) }
            val duration = fileStorage.getVideoDurationMs(path).takeIf { it > 0L } ?: 60_000L
            _state.update {
                it.copy(
                    isLoadingVideo = false,
                    sourceDurationMs = duration,
                    selectedStartMs = 0L,
                    selectedEndMs = duration.coerceAtMost(VideoStickerPackPlanner.MAX_SEGMENT_MS)
                )
            }
        }
    }

    private fun updateRange(startMs: Long? = null, endMs: Long? = null) {
        _state.update { current ->
            val source = current.sourceDurationMs.coerceAtLeast(1L)
            val newStart = (startMs ?: current.selectedStartMs).coerceIn(0L, source - 1L)
            val requestedEnd = (endMs ?: current.selectedEndMs).coerceIn(newStart + 1L, source)
            val newEnd = requestedEnd.coerceAtMost(newStart + VideoStickerPackPlanner.MAX_SEGMENT_MS).coerceAtMost(source)
            current.copy(selectedStartMs = newStart, selectedEndMs = newEnd, generatedStickers = emptyList())
        }
    }

    private fun generate(extractFreshCandidates: Boolean) {
        viewModelScope.launch {
            val current = _state.value
            if (current.isProcessing) return@launch
            runCatching { VideoStickerPackPlanner.validateSelectedRange(current.selectedStartMs, current.selectedEndMs) }
                .onFailure {
                    _effect.send(VideoStickerPackEffect.ShowError(UiText.DynamicString("Choose a segment up to 60 seconds.")))
                    return@launch
                }

            _state.update { it.copy(isProcessing = true, processingLabel = "Finding clear frames", processingProgress = 0f, errorMessage = null) }
            try {
                val grids = if (extractFreshCandidates || current.candidateGrids.isEmpty()) {
                    val candidates = extractor.extractCandidates(
                        videoPath = current.videoPath,
                        startMs = current.selectedStartMs,
                        endMs = current.selectedEndMs,
                        onProgress = { done, total ->
                            _state.update { it.copy(processingProgress = done.toFloat() / total.coerceAtLeast(1)) }
                        }
                    )
                    if (candidates.isEmpty()) error("No usable frames extracted")
                    _state.update { it.copy(candidates = candidates, processingLabel = "Building candidate grids") }
                    gridComposer.composeGrids(candidates.map { it.filePath }).also { composed ->
                        _state.update { it.copy(candidateGrids = composed) }
                    }
                } else {
                    current.candidateGrids
                }

                _state.update { it.copy(processingLabel = "Asking AI Agent", processingProgress = 0f) }
                val generated = apiRepository.generateVideoStickerPack(
                    candidateGridPaths = grids.map { it.filePath },
                    candidateCount = grids.sumOf { it.frameCount },
                    selectedStartMs = current.selectedStartMs,
                    selectedEndMs = current.selectedEndMs,
                    sourceDurationMs = current.sourceDurationMs
                )
                if (generated.isEmpty()) error("No generated stickers returned")
                _state.update {
                    it.copy(
                        isProcessing = false,
                        processingLabel = null,
                        processingProgress = 1f,
                        generatedStickers = generated
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isProcessing = false, processingLabel = null, errorMessage = e.message) }
                _effect.send(VideoStickerPackEffect.ShowError(UiText.DynamicString(e.message ?: "Failed to generate video sticker pack.")))
            }
        }
    }

    private fun savePack() {
        viewModelScope.launch {
            val current = _state.value
            if (!current.canSave) return@launch
            try {
                val identifier = PackIdentifierSanitizer.sanitize(current.packName, Random.nextInt(1000, 9999))
                val tray = current.generatedStickers.first().localPath
                val pack = draftSaver.buildDraftPack(
                    StickerDraftInput(
                        identifier = identifier,
                        name = current.packName,
                        publisher = current.publisher,
                        visibility = "PRIVATE",
                        trayImagePath = tray,
                        stickers = current.generatedStickers.map { file ->
                            StickerDraftInput.StickerInput(
                                imagePath = file.localPath,
                                decorations = file.decorations
                            )
                        }
                    )
                )
                stickerRepository.savePack(pack)
                _effect.send(VideoStickerPackEffect.NavigateToPackDetail(pack.identifier))
            } catch (e: Exception) {
                _effect.send(VideoStickerPackEffect.ShowError(UiText.DynamicString(e.message ?: "Failed to save pack.")))
            }
        }
    }
}
```

- [ ] **Step 3: Add minimal screen and root**

Create `VideoStickerPackScreenRoot.kt` using existing Koin/ViewModel collection patterns from other screen roots. Create `VideoStickerPackScreen.kt` with:

```kotlin
@Composable
fun VideoStickerPackScreen(
    state: VideoStickerPackState,
    onIntent: (VideoStickerPackIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = { AppTopBar(title = "Video to Sticker Pack") },
        containerColor = neubrutalScreenBackground()
    ) { innerPadding ->
        Column(
            modifier = modifier.fillMaxSize().padding(innerPadding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Choose up to 60 seconds", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Long videos are allowed. Pick the best minute, then AI will turn clear frames into a sticker pack.")
            // Add two sliders matching existing trim visual style. Disable Generate while state.canGenerate is false.
            // Add pack name and publisher fields before Save Pack.
            // Show generatedStickers grid after generation using existing image preview components.
        }
    }
}
```

Use existing components where available: `AppTopBar`, `LoadingIndicator`, `NeubrutalButton`/bottom bar button equivalents, and sticker preview image components. Keep styling consistent with Home/Create Pack screens.

- [ ] **Step 4: Add Home entry and picker**

Modify `HomeIntent.kt`:

```kotlin
data object PickVideoForStickerPack : HomeIntent
data class StartVideoStickerPack(val videoPath: String) : HomeIntent
```

Modify `HomeEffect.kt`:

```kotlin
data class NavigateToVideoStickerPack(val videoPath: String) : HomeEffect
```

Modify `HomeViewModel.onIntent`:

```kotlin
is HomeIntent.StartVideoStickerPack -> {
    viewModelScope.launch { _effect.send(HomeEffect.NavigateToVideoStickerPack(intent.videoPath)) }
}
```

Modify `HomeScreen.kt`:

```kotlin
val videoPicker = rememberVideoPicker { path ->
    path?.let { onIntent(HomeIntent.StartVideoStickerPack(it)) }
}
```

Pass a new `onVideoPackClick = { videoPicker.launch() }` into `HomeBottomBar`.

- [ ] **Step 5: Add navigation route**

Modify `AppNavigation.kt` by adding a serializable route following existing route style:

```kotlin
@Serializable
data class VideoStickerPackRoute(val videoPath: String)
```

Handle `HomeEffect.NavigateToVideoStickerPack(videoPath)` by navigating to that route. Add a `composable<VideoStickerPackRoute>` that calls `VideoStickerPackScreenRoot(videoPath = route.videoPath, ...)`.

- [ ] **Step 6: Add strings**

Add to `strings.xml`:

```xml
<string name="video_to_sticker_pack">Video to Sticker Pack</string>
<string name="video_pack_title">Video to Sticker Pack</string>
<string name="video_pack_subtitle">Pick the best minute. AI will choose clear, unique frames and turn them into a sticker pack.</string>
<string name="video_pack_max_duration">Max selected duration: 60s</string>
<string name="video_pack_find_frames">Finding clear frames</string>
<string name="video_pack_build_grids">Building candidate grids</string>
<string name="video_pack_ask_ai">Asking AI Agent</string>
<string name="video_pack_save">Save Pack</string>
<string name="video_pack_regenerate">Regenerate</string>
<string name="error_failed_generate_video_sticker_pack">Failed to generate video sticker pack.</string>
```

- [ ] **Step 7: Add ViewModel tests**

Create `VideoStickerPackViewModelTest.kt` with tests for:

```kotlin
@Test
fun regenerateReusesExistingCandidateGrids() = runTest { /* mock extractor and composer; call Generate then Regenerate; verify extractor called once, api called twice */ }

@Test
fun savePackDoesNotRunBeforeGeneratedPreviewExists() = runTest { /* call SavePack before Generate; verify repository.savePack not called */ }

@Test
fun savePackPersistsGeneratedStickersAfterPreview() = runTest { /* seed state through generate, set name/publisher, save, verify draft saver input */ }
```

Follow the mocking style from `HomeViewModelGeneratePackTest.kt` with `mockk`, `StandardTestDispatcher`, `Dispatchers.setMain`, and `advanceUntilIdle()`.

- [ ] **Step 8: Run Android tests and compile**

Run:

`./gradlew :composeApp:testDebugUnitTest --tests presentation.videostickerpack.VideoStickerPackViewModelTest`

Expected: PASS.

Run:

`./gradlew :composeApp:compileDebugKotlinAndroid`

Expected: BUILD SUCCESSFUL.

---

### Task 7: End-To-End Verification And Polish

**Files:**

- Modify files touched in Tasks 1-6 only as needed for compile/test fixes.

- [ ] **Step 1: Run API verification**

Run in `C:\Users\iqbal.shafiq\VsCodeProjects\stiker-api`:

`npm test -- tests/unit/utils/video-sticker-pack.test.ts tests/unit/utils/validators-video-sticker-pack.test.ts`

Expected: PASS.

Run:

`npm run typecheck`

Expected: PASS.

- [ ] **Step 2: Run app verification**

Run in `C:\Users\iqbal.shafiq\AndroidStudioProjects\setiker`:

`./gradlew :composeApp:allTests --tests domain.util.VideoStickerPackPlannerTest`

Expected: PASS.

Run:

`./gradlew :composeApp:testDebugUnitTest --tests presentation.videostickerpack.VideoStickerPackViewModelTest`

Expected: PASS.

Run:

`./gradlew :composeApp:compileDebugKotlinAndroid`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Manual smoke test on Android**

Install/run app, then verify:

- Home shows `Video to Sticker Pack` action.
- Tapping action opens video picker.
- Selecting a video longer than 60 seconds opens trim screen and selected duration is capped to 60 seconds.
- Generate shows progress stages.
- Candidate grids are created in app cache.
- API receives one or two `candidate_grids` files.
- Result preview appears before save.
- `Regenerate` calls API again without re-opening picker or re-extracting frames.
- `Save Pack` persists and navigates to pack detail.

---

## Self-Review

- Spec coverage: The plan covers Android-only entry, max-60-second trim, candidate extraction/filtering, two `4x4` candidate grids, new API Agent endpoint, one `4x4` output grid, preview, regenerate from same candidates, and save-after-confirmation.
- Placeholder scan: No `TBD` or intentionally undefined requirements remain. UI implementation is intentionally minimal but names exact files and required behavior.
- Type consistency: API constants, endpoint path, multipart field `candidate_grids`, layout `4x4`, candidate cap `32`, and app method names are consistent across tasks.
