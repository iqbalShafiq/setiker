# Setiker - WhatsApp Sticker Maker

A Kotlin Multiplatform app for Android and iOS to create and manage WhatsApp sticker packs with image cropping and background removal features.

## Features

- Create and manage sticker packs (local-first, optional cloud sync when signed in)
- Crop, decorate, and remove backgrounds; animated sticker packs from video
- AI generation, improvement, grid split, and background jobs via `stiker-api`
- Explore public packs, share links (`shareUrl`, `deepLinkUrl`, `webFallbackUrl`)
- Auth (login/register/Google Sign-In on Android), profile, settings (legal links, AI daily usage, account deletion)
- Onboarding for first-run users
- Export sticker packs to WhatsApp
- Neubrutal Compose UI (Android + iOS)

### Sign in with Google (Android)

1. Create Web + Android OAuth clients in Google Cloud Console (package `com.setiker.app`, debug and Play App Signing SHA-1).
2. Copy [`local.properties.example`](local.properties.example) → `local.properties` and set:

```properties
GOOGLE_WEB_CLIENT_ID=your-web-client-id.apps.googleusercontent.com
```

(`sdk.dir` is usually filled by Android Studio.)

3. Set the same Web client ID (and optional Android client ID) on the API as `GOOGLE_CLIENT_IDS`.
4. See `stiker-api` README for server verification and account-linking rules.

### iOS limitations

- WhatsApp pack export and some on-device processing paths differ from Android
- Google Sign-In is not available on iOS in this phase (`GoogleSignInGateway` stub)
- Unsupported operations show localized messaging instead of failing silently
- Configure API base URL in `ApiConfig.ios.kt` for your environment

## Tech Stack

- **Kotlin Multiplatform**: Shared business logic and UI
- **Jetpack Compose**: Shared UI across platforms
- **Material3**: Modern design system
- **MVI Architecture**: Unidirectional data flow
- **Koin**: Dependency injection
- **Navigation Compose**: Type-safe navigation
- **Coil**: Image loading

## Project Structure

```
setiker/
├── composeApp/
│   ├── src/
│   │   ├── commonMain/kotlin/     # Shared code
│   │   ├── androidMain/kotlin/    # Android-specific code
│   │   └── iosMain/kotlin/        # iOS-specific code
│   └── build.gradle.kts
├── androidApp/                     # Android app module
├── iosApp/                         # iOS app module
└── docs/
    ├── design/                     # Design references
    └── plans/                      # Implementation plans
```

## API dependency

The app expects a compatible **stiker-api** backend (auth subcodes, refresh body, `/api/v1/ai/usage`, `/api/v1/legal/*`, share URL fields). Point `ApiConfig` at your server.

## Bundled fonts

Text decoration presets use these open-source fonts (see [SIL Open Font License 1.1](https://scripts.sil.org/OFL)):

| Font | Preset usage | Source |
|------|----------------|--------|
| [Bungee](https://fonts.google.com/specimen/Bungee) | Sticker Pop | Google Fonts (OFL) |
| [Luckiest Guy](https://fonts.google.com/specimen/Luckiest+Guy) | Bubble Red | Google Fonts (OFL) |
| [Fredoka](https://fonts.google.com/specimen/Fredoka) | Bubble / Neon / Sunset | Google Fonts (OFL) |
| Instrument Sans, Space Grotesk | UI / classic decoration weights | Bundled in `composeResources/font/` |

Font files live in `composeApp/src/commonMain/composeResources/font/` (Compose preview) and `composeApp/src/androidMain/assets/fonts/` (Android export).

## Verification

```bash
./gradlew :composeApp:testDebugUnitTest :composeApp:lint
```

## Getting Started

### Prerequisites

- Android Studio Arctic Fox or later
- Xcode 13 or later (for iOS)
- JDK 11 or later

### Running the App

**Android:**
```bash
./gradlew :composeApp:installDebug
```

**iOS:**
Open `iosApp/iosApp.xcodeproj` in Xcode and run.

## WhatsApp Integration

The app implements a ContentProvider to share sticker packs with WhatsApp following the [WhatsApp Stickers API](https://github.com/WhatsApp/stickers).

## Architecture

The app uses MVI (Model-View-Intent) architecture:
- **Intent**: User actions
- **ViewModel**: Business logic and state management
- **State**: Immutable UI state
- **Effect**: One-shot events (navigation, snackbars)

## License

MIT License
