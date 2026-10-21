# AGENTS.md

Guidance for coding agents working in this repository.

## What this is

Setiker is a Kotlin Multiplatform app for Android and iOS that turns a photo into a WhatsApp
sticker pack. The shared app lives in `composeApp`; the Android shell is `androidApp`; the backend
is a separate repository, [stiker-api](https://github.com/iqbalShafiq/stiker-api).

## Before you change anything

Read [`docs/testing.md`](docs/testing.md) first. It says which layer a test belongs in and which
command runs it.

- `./gradlew :composeApp:testAndroidHostTest` is the fast loop: unit and view-model tests, no
  device.
- `./gradlew :androidApp:testDebugUnitTest` holds the screenshot and the performance tests, also
  without a device.
- `./gradlew :composeApp:lint` must stay clean.
- **Any compiler warning fails the build.** `allWarningsAsErrors` is on for both modules.
- The Kotlin compiler does not report unused imports. Keep the import blocks sorted and remove what
  you do not use.
- **Every module that writes `@Composable` code needs the Compose compiler plugin.** The app module
  has no UI of its own, but its test source sets do, and without the plugin the composable lambdas
  compile to the wrong signature and the test harness fails with a `NoSuchMethodError`.

## Rules that are not obvious from the code

- **The design system is neubrutalism**: thick black borders, hard offset shadows, flat colour. The
  tokens are in `composeApp/src/commonMain/kotlin/presentation/theme/`. Do not add a one-off colour
  or shadow in a screen.
- **Motion has one home**: `SetikerMotion`. Durations, curves and springs come from there. Press
  feedback is a firm, non-bouncy spring; a bounce is reserved for a celebration.
- **Screens are dumb.** They render state and forward intents. The work belongs in the view model.
  State that the view model owns does not belong in a `remember`.
- **Anything the user can tap has to say what it does.** Either a visible label or a content
  description. The accessibility checks in the instrumented tests will tell you when you forget.
- **Never commit an API key, a client id or a `.env` file.** `local.properties` and `.env` are
  gitignored; `.env.example` and `local.properties.example` are the templates.

## Working on a device

```bash
./gradlew :androidApp:installDebug
adb reverse tcp:7845 tcp:7845     # the debug build talks to 127.0.0.1:7845
```

Some Xiaomi and MIUI builds block installing a new package over USB until the prompt on the phone
is accepted. An update of an installed package goes through without a prompt.

## Before you push

- [ ] `./gradlew :composeApp:testAndroidHostTest :composeApp:lint` is green
- [ ] `./gradlew :androidApp:testDebugUnitTest` is green (the screenshot and the performance tests)
- [ ] `./gradlew :androidApp:connectedUiTest` passes, if you touched a screen
- [ ] `python tools/screenshot_tests.py` matches, if you touched a screen's appearance
- [ ] The README still describes what the app does
