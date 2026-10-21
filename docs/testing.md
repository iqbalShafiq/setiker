# Testing strategy

Five layers, each with a different job. The shortest ones run on every save; the longest one runs
when the UI changes.

| Layer | Where | Runs on | What it covers |
| --- | --- | --- | --- |
| Unit tests | `composeApp/src/commonTest` | JVM | Pure logic: mappers, planners, state helpers, error mapping. |
| View-model tests | `composeApp/src/androidHostTest` | JVM | The view models, with fakes for the repositories and the platform. |
| Screenshot and performance tests | `androidApp/src/test` | JVM (Robolectric) | The pixels of the components, and how often they are recomposed. |
| UI and accessibility tests | `androidApp/src/androidTest` | Device or emulator | The real Compose tree: semantics, the call to action of an empty state, and the accessibility checks. |
| Visual and memory regression | `tools/` | Device | The pixels of the screens, and the memory the app holds while they are used. |

## Commands

```bash
# Unit and view-model tests (no device needed)
./gradlew :composeApp:testAndroidHostTest

# The screenshot and the performance tests (no device needed)
./gradlew :androidApp:testDebugUnitTest

# Record the screenshot references, then check them
./gradlew :androidApp:recordRoborazziDebug
./gradlew :androidApp:verifyRoborazziDebug

# The instrumented UI and accessibility tests, on a connected device
./gradlew :androidApp:connectedUiTest

# Android lint
./gradlew :composeApp:lint

# The visual regression, on a connected device
python tools/screenshot_tests.py            # compare against the references
python tools/screenshot_tests.py --record   # refresh the references

# The memory of the app, over a few navigation cycles
python tools/memory_check.py

# The frame times of the app, on the sticker grid
python tools/frame_stats.py
```

The device tests need the app on the device and the API reachable from it:

```bash
adb reverse tcp:7845 tcp:7845
```

`connectedUiTest` installs both packages itself, so it is the one command to remember.

## What the UI tests pin

`androidApp/src/androidTest/.../UiRegressionTest.kt` holds the findings of the
[UI/UX and motion audit](plans/2026-10-19-ui-ux-motion-audit.md) that a test can hold on to:

- the empty state shows its title, its description **and** its action;
- the action of an empty state is reachable;
- a disabled button is not enabled;
- the page indicator says which page it is, and marks the current one as selected;
- the icon buttons are labelled and clickable;
- and the accessibility checks run over the empty state and the icon button
  (`enableAccessibilityChecks()` from `androidx.compose.ui:ui-test-junit4-accessibility`).

`androidApp/src/androidTest/.../ErrorSemanticsTest.kt` holds the two properties that a screen
reader needs and nobody sees:

- the error snackbar is a live region, so it announces itself when it appears;
- an invalid field carries the reason in the `error` semantics, so TalkBack says that the field
  is invalid and what is wrong with it.

## What the performance tests pin

`androidApp/src/test/.../RecompositionTest.kt` counts how often the sticker cell and the pack card
are composed. A change on the screen that does not touch the list must not compose the list again.
The count is the real one: the rule renders on a Robolectric device, so it is the same composition
and the same skipping the app does.

## The screenshot references

Two sets of references, for two different jobs:

- `androidApp/src/screenshots/debug/` holds the components, drawn by Robolectric with native
  graphics. They are the unit tests of the look: a colour that drifts, a corner that rounds the
  wrong way, a button that moves.
- `docs/images/` holds the screens, captured from a device. They are the unit tests of the whole
  app.

The comparison of the screens masks the system bars, because the clock and the battery change
between runs. A screen passes when at most one percent of its pixels differ; a real visual
regression fails the run.

The `hero.png` and the flow in `flow.gif` are the images the README uses; they are not part of the
comparison.

## The memory check

`tools/memory_check.py` walks through the main screens a few times and reads the object counts out
of `dumpsys meminfo` after every cycle: the live activities, the views, the app contexts, the
graphics. A leak shows up as a count that only ever goes up. The heap size on its own says
nothing, because the garbage collector takes it back.

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

## The two things that bite on a phone

- **The Compose compiler plugin has to be on every module that writes `@Composable` code.** The
  app module has no UI of its own, but both of its test source sets do. Without the plugin, the
  composable lambdas compile to the wrong signature and the test harness fails at runtime with a
  `NoSuchMethodError`.
- **Xiaomi's MIUI refuses to start an activity in the background**, and the instrumentation
  restarts the app before it runs, so a Compose test always launches its activity from the
  background. The permission has to be granted after the install and right before the run, which
  is what `:androidApp:connectedUiTest` does. On every other device the command is a no-op.
