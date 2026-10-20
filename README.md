<p align="center">
  <img src="docs/images/logo.svg" alt="Setiker" width="96" height="96">
</p>

<h1 align="center">Setiker</h1>

<p align="center">
  Turn a photo into a WhatsApp sticker pack.
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-coral" alt="MIT License"></a>
  <a href="https://developer.android.com"><img src="https://img.shields.io/badge/Android-7.0%2B-3DDC84" alt="Android 7.0+"></a>
  <a href="https://developer.apple.com/ios/"><img src="https://img.shields.io/badge/iOS-18.2%2B-3DDC84" alt="iOS 18.2+"></a>
  <img src="https://img.shields.io/badge/Kotlin-2.4-7F52FF" alt="Kotlin 2.4">
  <img src="https://img.shields.io/badge/Compose%20Multiplatform-1.12-FF8A65" alt="Compose Multiplatform 1.12">
</p>

Setiker crops, de-backgrounds and decorates an image, then exports it as a WhatsApp sticker pack.
Packs live on the device first; sign in and they sync to the cloud, where AI can generate a whole
pack, clean up the background, or split a grid of stickers for you. One Kotlin codebase, Android and
iOS.

<p align="center">
  <img src="docs/images/hero.png" alt="Setiker on Android: the pack list, a pack of stickers, the sticker editor, and the public Explore feed" width="1000">
</p>

<p align="center">
  <img src="docs/images/flow.gif" alt="The app in motion: the pack list, a pack opening, the sticker grid, the editor, and the coral check that pops in when a sticker is saved" width="300">
</p>

## What it does

| | |
| --- | --- |
| <img src="docs/images/pack-detail.png" alt="A pack of cat stickers in a three-column grid, each sticker with a delete button and reorder arrows" width="320"> **A pack, not a folder of images.** Every sticker has a tray icon, emoji tags and alt text, and the grid reorders with drag or arrows. | <img src="docs/images/editor.png" alt="The sticker editor with a large preview, Add text, Add emoji and Add image actions, and a tag row" width="320"> **Edit in place.** Crop, remove the background, add text or emoji, drag the decoration around, and see the result on the sticker itself. |
| <img src="docs/images/ai-jobs.png" alt="The AI jobs and drafts screen, with a job that needs attention and drafts ready to review" width="320"> **AI that keeps working in the background.** Generate a pack, improve a set, or split a grid. Jobs survive a restart, report their progress, and land in a drafts list you can review. | <img src="docs/images/explore.png" alt="The Explore screen listing public sticker packs with like and save buttons" width="320"> **Publish and discover.** Share a pack with a link, browse what other people made, and save the ones you want. |
| <img src="docs/images/onboarding.png" alt="The onboarding screen introducing sticker packs" width="320"> **Three screens, then you are in.** A short onboarding explains what a pack is, and the editor is right behind it. | <img src="docs/images/profile.png" alt="The profile screen with a sticker and pack count, a daily AI quota, and a menu of account settings" width="320"> **Your quota, your account, your cloud.** The profile shows how much AI quota is left, what is signed in, and where the legal documents and account deletion live. |

## Quick start

You need **JDK 21**, the **Android SDK** (compile SDK 37), and the backend. The debug build talks to
`127.0.0.1:7845`, so the device reaches your machine over `adb reverse`.

```bash
git clone https://github.com/iqbalShafiq/setiker.git
cd setiker

# 1. the API (separate repo, see stiker-api/README.md)
git clone https://github.com/iqbalShafiq/stiker-api.git
cd stiker-api && docker compose up -d && npm run dev

# 2. let the device reach the API
adb reverse tcp:7845 tcp:7845

# 3. build and install
cd ../setiker
./gradlew :androidApp:installDebug
```

**iOS:** open `iosApp/iosApp.xcodeproj` in Xcode and run. The iOS build talks to
`http://13.251.98.162:8080`; point `ApiConfig.ios.kt` at your own server for local work.

## Configuration

| Setting | What it unlocks |
| --- | --- |
| `local.properties` → `GOOGLE_WEB_CLIENT_ID` | Google Sign-In on Android. Add the same client id to the API as `GOOGLE_CLIENT_IDS`. |
| `iosApp/iosApp/Info.plist` → `GIDClientID`, `GIDServerClientID` | Google Sign-In on iOS, plus the reversed client id in `CFBundleURLSchemes`. |
| `iosApp/iosApp/iosApp.entitlements` | Sign in with Apple (iOS only). |
| `composeApp/src/commonMain/kotlin/data/remote/ApiConfig.android.kt` | The base URL the app calls. Debug builds use `http://127.0.0.1:7845`. |
| `composeApp/src/commonMain/composeResources/font/` | The bundled text-decoration fonts. They ship with the app, so presets render offline. |

## Architecture

```mermaid
flowchart LR
  UI["composeApp\nCompose Multiplatform"] --> VM["ViewModel\nMVI state"]
  VM --> Repo["Repository"]
  Repo --> DB[("Room\ndevice store")]
  Repo --> API["stiker-api\nExpress + Prisma"]
  API --> PG[("Postgres")]
  API --> Redis[("Redis")]
  API --> AI["OpenRouter\nimage + chat models"]
  API --> Store[("Object storage")]
```

| Module | Responsibility |
| --- | --- |
| `composeApp/src/commonMain` | The whole app: screens, view models, data layer, design system. Shared by both platforms. |
| `composeApp/src/androidMain` | Android-only: Play Billing, Google Sign-In, an on-device u2net background remover (ONNX Runtime, int8), WorkManager jobs, Media3 video. |
| `composeApp/src/iosMain` | iOS-only: StoreKit 2, Apple Sign-In, and UIKit-based image and video processing. |
| `androidApp` | The Android application module. |
| `iosApp` | The Xcode project. |
| `docs/` | The design reference, the implementation plans, and the UI/UX and motion audit. |

Each feature follows the same shape: a `*State`, a `*Intent`, a `*Effect`, a `*ViewModel` and a
`*Screen`. The screen renders state and forwards intents; the view model owns the work.

### The design system

The UI is **neubrutalism**: thick black borders, hard offset shadows, flat colour, no gradients on
the chrome. It lives in `composeApp/src/commonMain/kotlin/presentation/theme/` and every screen
uses it.

Motion has one home too, `SetikerMotion`: durations, curves and springs. Press feedback is a firm
non-bouncy spring, because a button is pressed dozens of times a day and a wobble reads as
sloppiness. The one place a bounce is allowed is a celebration, such as the badge that pops when a
sticker is saved.

## Development

```bash
./gradlew :composeApp:testAndroidHostTest   # unit and view-model tests
./gradlew :composeApp:lint                   # Android lint
./gradlew :androidApp:installDebug           # build and install
```

The build treats **any compiler warning as an error** (`allWarningsAsErrors = true`), so a new
warning fails CI before it reaches a reviewer.

## Contributing

Issues and pull requests are welcome. Open a branch from `main`, keep the change focused, and say
what you ran.

The [UI/UX and motion audit](docs/plans/2026-10-19-ui-ux-motion-audit.md) is a good model for what
a change should come with: the finding, the evidence on a device, and the measurement after.

## License

Setiker is open source under the [MIT License](LICENSE).
