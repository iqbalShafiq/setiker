# Testing strategy

Four layers, each with a different job. The shortest ones run on every save; the longest one runs
when the UI changes.

| Layer | Where | Runs on | What it covers |
| --- | --- | --- | --- |
| Unit tests | `composeApp/src/commonTest` | JVM | Pure logic: mappers, planners, state helpers, error mapping. |
| View-model tests | `composeApp/src/androidHostTest` | JVM | The view models, with fakes for the repositories and the platform. |
| UI tests | `androidApp/src/androidTest` | Device or emulator | The real Compose tree: semantics, the call to action of an empty state, and the accessibility checks. |
| Screenshot regression | `tools/screenshot_tests.py` | Device | The pixels of the key screens against the references in `docs/images/`. |

## Commands

```bash
# Unit and view-model tests (no device needed)
./gradlew :composeApp:testAndroidHostTest

# Android lint
./gradlew :composeApp:lint

# The instrumented UI tests, on a connected device or an emulator
./gradlew :androidApp:connectedDebugAndroidTest

# The visual regression, on a connected device
python tools/screenshot_tests.py            # compare against the references
python tools/screenshot_tests.py --record   # refresh the references
```

The app has to be installed on the device for the last two:

```bash
./gradlew :androidApp:installDebug
adb reverse tcp:7845 tcp:7845     # so the device can reach the API on the host
```

## What the UI tests pin

`androidApp/src/androidTest/.../UiRegressionTest.kt` holds the findings of the
[UI/UX and motion audit](plans/2026-10-19-ui-ux-motion-audit.md) that a test can hold on to:

- the empty state shows its title, its description **and** its action;
- the action of an empty state is reachable;
- a disabled button is not enabled;
- the page indicator announces the current page;
- the icon buttons are labelled and clickable;
- and the accessibility checks run over the empty state and the icon button
  (`enableAccessibilityChecks()` from `androidx.compose.ui:ui-test-junit4-accessibility`).

## The screenshot references

`docs/images/` holds one PNG per screen:

| File | Screen |
| --- | --- |
| `home.png` | The pack list. |
| `pack-detail.png` | A pack and its stickers. |
| `editor.png` | The sticker editor. |
| `explore.png` | The public feed. |
| `ai-jobs.png` | The AI jobs and drafts. |
| `profile.png` | The profile. |

The comparison masks the system bars, because the clock and the battery change between runs. A
screen passes when at most one percent of its pixels differ; a real visual regression fails the
run.

The `hero.png` and the flow in `flow.gif` are the images the README uses; they are not part of the
comparison.

## Warnings are errors

Both the shared module and the app module set `allWarningsAsErrors = true`, so a new compiler
warning fails the build instead of quietly accumulating. The Kotlin compiler does not report unused
imports, so the import blocks are kept clean by review; a stray import shows up in the diff.

## Writing a test

- Keep the test next to the behaviour it describes: a state helper goes to `commonTest`, a view model
  goes to `androidHostTest`, a screen goes to `androidTest`.
- Prefer a fake over a mock. A fake is a small in-memory implementation of the interface; it breaks
  less and reads better than a stack of stubs.
- Every test needs a name that says what it protects. `shows the title, the description and the
  action` beats `testEmptyState`.
- When a test protects a finding from the audit, say so in the class comment. That is the link
  between the code and the reason it exists.
