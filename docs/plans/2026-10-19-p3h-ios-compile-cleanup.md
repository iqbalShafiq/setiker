# P3h — iOS Target Compile Cleanup (type-level)

**Goal:** Make the iOS target of `:composeApp` type-check cleanly so the shared module stays
honestly multiplatform — without implementing or running the iOS app (deferred by the user;
no macOS host available in this environment).

**Starting point:** `5e1dda3` had already removed every `android.*` import from `commonMain`
(`AppLog`, `ImeNestedScroll`/`ProcessingMemory` expect-actual, emoji picker, editor), and
`:composeApp:compileCommonMainKotlinMetadata` was green. What remained was **95 errors, all in
`iosMain`** (12 files), captured in `build/p3h-ios-errors-clean.txt`.

## What was broken (patterns) and the fix

| Pattern | Fix |
|---|---|
| `fun f(...) -> Unit) {` double-arrow typos (Apple/Google sign-in) | removed the stray `-> Unit)` |
| `App()` unresolved in `MainViewController.kt` | `import com.setiker.app.App` |
| `Dispatchers.IO` unresolved (4 files) | `import kotlinx.coroutines.IO` (Native extension) |
| `kotlinx.cinterop.memcpy` in `FileBridge.ios` | `platform.posix.memcpy` + explicit import |
| `URLsForDirectory(...).firstOrNull()?.path` — element is `platform.darwin.NSObject` | `(…firstOrNull() as? NSURL)?.path` (3 files) |
| `NSData.create(...)` "unresolved" | missing `import platform.Foundation.create` + `@OptIn(…, BetaInteropApi::class)` |
| `String as NSString` ("cast can never succeed") | `NSString.create(string = …)` (draws text/emoji) |
| `CValue<CGSize>.width` direct access | `size.useContents { width }` / `{ height }` |
| `UIGraphicsGetCurrentContext()` referenced from CoreGraphics | it lives in `platform.UIKit` |
| `context.concatenateWithTransform/rotateByAngle/scaleBy/strokePath/setBlendMode` | real C API: `CGContextConcatCTM`, `CGContextRotateCTM`, `CGContextScaleCTM`, `CGContextStrokePath`, `CGContextSetBlendMode` |
| CG constants (`kCGLineCapRound`, `kCGBlendModeClear`, …) | entries of CoreGraphics enum classes: `CGLineCap.kCGLineCapRound`, `CGBlendMode.kCGBlendModeClear`, … |
| `NSString.sizeWithAttributes/drawAtPoint` unresolved | extensions live in `platform.UIKit` — imported (verified against `klib dump-metadata` of the 2.4.21 platform libs) |
| `System.currentTimeMillis()` on Native | `platform.posix.time(null) * 1000` |
| `UIGraphicsGetImageFromCurrentImageContext()` treated as non-null | null-checked before PNG encoding |
| `loadObjectOfClass(UIImage.class(), …)` type mismatch | `loadDataRepresentationForTypeIdentifier(UTTypeImage.identifier, …)` + `UIImage.imageWithData` |
| `NetworkMonitor.ios` redundant `?`; ktor `io.ktor.utils.io.errors.IOException` deprecated | nullable alias without extra `?`; `kotlinx.io.IOException` |
| `@file:OptIn` not honored for ObjC factories | declaration-level `@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)` |

API names were confirmed by dumping the actual platform-lib metadata
(`klib dump-metadata …/klib/platform/ios_arm64/org.jetbrains.kotlin.native.platform.{CoreGraphics,UIKit,Foundation}`)
instead of guessing.

## Verification

- `:composeApp:compileIosMainKotlinMetadata` — `build/p3h-ios-metadata-1.txt` (95 errors) →
  `…-2.txt` (2) → `…-3.txt` (0 errors) → `…-4.txt` (opt-in warnings) → `…-5.txt`:
  **0 errors, 0 warnings, BUILD SUCCESSFUL**.
- Unchanged and rerun on the same tree: `:composeApp:compileCommonMainKotlinMetadata`,
  `:composeApp:testAndroidHostTest` (180/0/0), `:androidApp:assembleDebug`.
- **Limits:** type-level verification only. No simulator/device run (no macOS). iOS runtime
  behavior (auth handlers, stores, drawing) is intentionally out of scope for this phase and stays
  on the backlog for a Mac-backed pass.
