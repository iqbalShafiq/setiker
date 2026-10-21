# Setiker — UI/UX, Micro-interaction & Motion Audit (device-verified)

**Date:** 2026-10-20 · **Device:** Redmi Note 11 (2207117BPG, 1080×2400 @440dpi, Android 13/MIUI)
**Build:** `com.setiker.app` debug installed on device · **API:** local `stiker-api` on :7845 (`adb reverse` active)
**Method:** static read of all motion/UI code + on-device walkthrough of 14 screens × states, `android layout` a11y dumps, frame-by-frame motion capture (device `animator_duration_scale` override), pixel-level rendering probes.

**No source code was modified.** No LLM call was made (no cost). One temporary test account was created and **deleted**; `stiker-api` git tree is clean; device `animator_duration_scale` restored to 1.

---

## 1. Findings table (ordered by leverage = impact ÷ effort)

| # | Sev | Category | Location | Finding | Fix summary |
|---|-----|----------|----------|---------|-------------|
| **F1** | **HIGH** | Loading state | `presentation/aijobs/AiJobsScreen.kt:291-293`, `components/DraftJobListCard.kt:103-117` | A running job renders **only static text** ("Removing background") — no bar, no spinner, no pulse. `showProgress = latestJob?.progress != null && status in ACTIVE_JOB_STATUSES`; when the API sends no `progress` payload the card shows plain text forever. Verified: 3 cards on AI Jobs & Drafts with zero motion. | When status is active, render an indeterminate indicator regardless of `progress` payload; keep the determinate bar only when `progress.fraction` exists. |
| **F2** | **HIGH** | Correctness / UX | `presentation/home/HomeScreen.kt:244-259` + `:289-295`, `presentation/home/HomeState.kt:44-45` | **Search with 0 matches shows the first-run empty state** ("No Stickers Yet / Create your first sticker pack to get started." + "Create your first pack") instead of "No packs found". Cause: `hasListContent` is computed from *filtered* packs, so the search-empty branch at :289 is **unreachable dead code**. | Compute `hasListContent` from unfiltered `packs`; keep the `searchQuery` branch reachable. |
| **F3** | **HIGH** | Visual bug | `presentation/components/AppButton.kt:58-67` (and `:108-125`), `presentation/theme/Claymorphism.kt:125-147` | **Disabled primary/secondary buttons render dark brown, not muted.** `neubrutalShadow` draws an opaque black rect *behind* the button; a 40 %-alpha fill then composites over that black. Measured on device: fill = RGB(118,72,58) = coral @0.4 over black — instead of the intended pale coral. The button reads as *enabled*. Reproduced in 2 places (Text-decoration sheet "Add decoration", Create Pack "Publish to Explore"). | Give disabled state an opaque colour (or draw the shadow with reduced alpha) so it never composites over the shadow layer. |
| **F4** | **HIGH** | Copy / trust | `presentation/sync/SyncScreen.kt` (rendered) | The Sync screen is a **developer debug view**: raw enum `UPDATE_PACK:`, raw UUID `86ef61ab-…`, `Status: FAILED`, and a raw network exception `Connect timeout has expired [url=http://13.251.98.162:8080/api/v1/upload, connect_timeout=30000 ms]` — leaks internal host/port/timeouts to end users. Header simultaneously says "Up to date". | Map raw operation/error to user-facing copy; keep raw text behind a debug flag only. |
| **F5** | **HIGH** | Content / monetization | `presentation/billing/PaywallScreen.kt` | Paywall shows **no prices** — every product is a "Buy" button with no amount or currency, so packs cannot be compared. Also **duplicated copy**: "100 AI tokens (non-expiring)" *and* "100 tokens (non-expiring)" on the same card; screen title "Get more AI tokens" while the list also sells subscriptions. | Render price + currency per SKU; remove the duplicated token line; retitle. |
| **F6** | **HIGH** | Responsive layout | `presentation/components/EmptyState.kt:33-68` | `EmptyState` uses `fillMaxSize()` + `Arrangement.Center` and is **not scrollable**. When vertical space shrinks (IME open, small screen, large font) the description and the primary CTA are **squeezed out and become invisible while still hit-testable**. Verified: with the keyboard open the "Create your first pack" button is a blank 132 px clickable region (layout dump) but renders **no pixels**. | Make `EmptyState` scrollable, cap the illustration height, and let the CTA stay visible. |
| **F7** | **HIGH** | Design system (dark mode) | `presentation/theme/NeubrutalColors.kt:25-28` | The signature neubrutal **hard offset shadow collapses in dark mode**. Measured on device: light mode shadow = RGB(26) vs page RGB(245) → ΔL 219; dark mode shadow = RGB(3) vs page RGB(22) → **ΔL 19** (≈10× less). Cards read as flat white outlines; the app's identity is lost in the state most users run at night. | Use a lighter/neutral shadow (or a light-on-dark offset) in dark mode; keep ΔL comparable to light mode. |
| **F8** | MED | Motion cohesion | `presentation/onboarding/OnboardingScreen.kt` | Onboarding page change is **incoherent**: the illustration **hard-cuts**, the page dots **hard-cut** to the new active dot, while only the title/description **slide in** horizontally. Captured at 10× scale: frame 2 shows the old page already gone while the new text is still off-centre. | One transition language for the whole page (fade+slide), with the dots animating (travel/scale). |
| **F9** | MED | Motion language | `components/NeubrutalSearchBar.kt:107`, `components/ShareLinksSheet.kt:145,157,168`, `PromptPresetPickerSheet.kt:49`, `StickerEmojiTagComponents.kt:69`, `CreatePackScreen.kt:762`, `ExploreScreen.kt:452,463` | 9 **raw Material `IconButton`s** keep the M3 ripple while every other control uses the app's custom scale + shadow-shrink press. Two different press languages in one app. | Route all icon buttons through the shared neubrutal button. |
| **F10** | MED | Touch target / a11y | `components/NeubrutalSearchBar.kt:109` | Clear-search `IconButton(Modifier.size(32.dp))` → **32 dp touch target** (below the 48 dp minimum) and 20 dp glyph. | Use 48 dp hit area with a 20 dp glyph. |
| **F11** | MED | A11y | `presentation/packdetail/PackDetailScreen.kt` (reorder buttons) | The **"Move up"** button is exposed as `CLICKABLE` but **not `FOCUSABLE`** → unreachable by TalkBack/keyboard. Verified in the layout dump. Sibling "Move down" is focusable. | Add focus semantics to all reorder affordances. |
| **F12** | MED | Loading state | `presentation/components/LoadingIndicator.kt:78-119` | The loading indicator is the app's only "in-progress" language and it is used **only on 2 screens** (Home + error/empty branches). Almost every remote call (Explore, AI usage, quota, sync, product list, notifications) renders **no spinner at all** — the screen just sits there. | Introduce one shared in-flight treatment (skeleton for lists, inline spinner for inline actions) and apply it to all remote reads. |
| **F13** | MED | Contrast (WCAG) | `presentation/components/PageIndicator.kt:42` | Inactive page dot = `onSurface.copy(alpha=0.18f)` ≈ RGB(215) on RGB(245) → **≈1.3:1**, far below the 3:1 required for non-text UI. Dots are also 11 dp with no size delta between states. | Raise to ≈0.45 alpha and/or add a size/border delta. |
| **F14** | MED | Motion / physics | `presentation/components/PressFeedback.kt:45-52` | Press feedback uses `Spring.DampingRatioMediumBouncy` on **every** control, including high-frequency ones (FAB, list rows). Measured at 10× on the FAB: 127 px → 117 px → 119 px — a visible **overshoot on release**. | Use a non-bouncy spring (`DampingRatioNoBouncy` / `LowBouncy`) for high-frequency targets; reserve bounce for rare/delight moments. |
| **F15** | MED | Missing feedback | `presentation/home/HomeScreen.kt:126-140` (search), `components/PageIndicator.kt` | **Zero micro-interaction on the most-used flow**: the Home search field has no focus animation, no clear-X transition, and the empty→content swap is a plain cut. The page dots swap colour instantly. | Add focus-ring + clear-button transition; animate the dot. |
| **F16** | MED | Motion (rare moment) | `presentation/splash/AppBrandedSplashScreen.kt:42-68` | Splash = white screen + a **static grey rounded square** + "Presented by Ratmo.co". No brand colour, no logo motion, and the splash→Home transition is a **hard cut** (captured: frame 2 splash → frame 3 Home). | Brand-colour the splash, animate the logo mark (scale-in), cross-fade into Home. |
| **F17** | MED | Copy | `presentation/explore/ExploreScreen.kt:261` | The Explore **error** state uses the *search-empty* description: "Could not load public packs." + **"Try a different search term."** → wrong instruction for a load failure. | Give the error branch its own description. |
| **F18** | LOW | A11y | `presentation/home/HomeScreen.kt` (pack card), `packdetail` (sticker cells) | Pack thumbnail has `contentDescription = "<pack name>"` **and** an adjacent `TextView` with the same name → TalkBack reads it twice. Sticker cells all announce the generic "Sticker" (no alt text). | Decorative thumbnail → `contentDescription = null`; sticker cells should use the sticker alt-text. |
| **F19** | LOW | Copy | `presentation/aijobs/AiJobsScreen.kt` (Video sticker pack card) | "Processing was interrupted" is rendered **twice** on the same card (subtitle + status line). | Render it once. |
| **F20** | LOW | Iconography | `presentation/aijobs/AiJobsScreen.kt` (FAB), `components/PressFeedback` | The AI-Jobs FAB has `contentDescription = "Clear completed"` but draws a **circular-arrow (refresh/retry)** glyph → label/icon mismatch, and destructive "clear" gets the loudest colour. | Match glyph to action; use a trash/broom glyph and a neutral treatment. |
| **F21** | LOW | Layout | `presentation/auth/ProfileScreen.kt` (last menu row) | The last menu row ("Account deletion") ends flush against the bottom bar with no trailing padding — it looks clipped. | Add bottom content padding. |
| **F22** | LOW | Consistency | `presentation/components/EmptyState.kt:40-44` | The illustration has no size cap and eats ~50 % of the viewport on every empty/error state (Home, Explore, Notifications). | Cap illustration height (e.g. 180 dp) and tighten the gap. |

---

## 2. What already works (don't regress)

- **Press feedback is real and measurable.** FAB at rest: coral fill 132 px, shadow extent 165 px, shadow offset ≈18 px. Pressed: fill 119 px, shadow 143 px, shadow offset ≈8.7 px → **scale + shadow-shrink** both animate (evidence: `uiux/motion/`).
- The neubrutal token system (`Claymorphism.kt`) is coherent: one `neubrutalShadow`, one `neubrutalBorderWithGloss`, one press helper.
- Content-state transitions are centralised in `ContentStateAnimations.kt` (180/120/160 ms) and used by 8+ screens — good discipline, the right place to add new transitions.
- Navigation has a coordinated transition controller (`NavigationTransitions.kt`) that reverses the exact push animation on pop — above average.
- Empty states on Home/Notifications/Create Pack carry real copy and (where relevant) a CTA.
- `ProgressDialog.kt` animates a determinate bar — the pattern exists, it just isn't used consistently.
- Accessibility text field, alt-text field, and per-control `contentDescription` are already in place on the editor.

---

## 3. Missed opportunities (additive, ranked)

1. **Loading = brand moment.** The app has a bespoke bouncing-dot loader (`LoadingIndicator.kt`) but almost never shows it. Make *every* remote read use a skeleton or the dot loader — that alone answers "user doesn't feel welcomed".
2. **Splash as identity.** Static grey square → animated logo mark in brand coral, cross-fading into Home. First impression, once per user.
3. **Onboarding page transition.** One coherent slide+fade for the whole page, with the page dot travelling/scaling. It is the one moment where delight is fully allowed.
4. **Sticker "saved" celebration.** Pack/sticker creation is the emotional core of the app — a 1-shot spring pop on the thumbnail + a toast-like neubrutal badge would land the payoff.
5. **Long-press reorder.** The pack-detail sticker grid supports move up/down *and* drag; the drag has no motion while the arrows do. A spring-following reorder would make the grid feel physical.
6. **Press feedback on raw icon buttons** (F9) closes the last gap in the tactile language.

---

## 4. State coverage matrix (verified on device)

| Screen | States seen | Loading visible? | Empty CTA? | Error retry? | Notes |
|---|---|---|---|---|---|
| Home | content, search, no-result, offline | yes (dot loader) | yes | yes | no-result shows **wrong** state (F2) |
| Pack detail | content, reorder | n/a (local) | — | — | "Move up" not focusable (F11) |
| Sticker editor | content, dialogs, sheet | **no** (Remove BG shows only a confirm dialog) | — | — | operations are guarded by `isOperationInProgress` (good) |
| Text-decoration sheet | idle, disabled-CTA | n/a | n/a | n/a | disabled button renders brown (F3) |
| Explore | error, empty, tabs | **no** | **no** | yes | error copy wrong (F17), tab row clips "Shared with me" |
| AI Jobs & Drafts | needs-attention, ready, in-flight | **no** (F1) | — | yes (Retry) | duplicate copy (F19) |
| Create Pack | pristine, requirements list | n/a | — | — | disabled CTA renders brown (F3) |
| Profile | signed-in, scrolling | n/a | — | — | last row flush (F21) |
| Settings | signed-in | n/a | — | — | destructive "Delete account" sits as a peer icon button |
| Paywall | product list | **no** | — | — | **no prices** (F5) |
| Sync | up-to-date, failed-op | **no** | — | yes | raw debug text (F4) |
| Notifications | empty | n/a | n/a | — | good copy, no CTA needed |
| Legal | content | **no** | — | — | long doc, no scroll-progress cue |
| Onboarding | 3 pages, skip | n/a | n/a | n/a | inconsistent transition (F8) |
| Splash | branded | n/a | n/a | n/a | static + hard cut (F16) |

---

## 5. Evidence index (all under `%TEMP%\opencode\setiker-audit\uiux\`)

- Screenshots: `01-home` … `33-dark-editor` (+ `-layout.json` a11y dumps for each)
- Motion: `motion/onboarding-next-10x/` (8-frame burst + `motion.csv` diff curve), `motion/removebg/` (dialog entry), `motion/splash-sheet.png`
- Rendering probes: `uiux/03-cta-zone.png` (invisible CTA), `uiux/03b-cta-zone.png` (contrast-normalised), `uiux/04-cta-zone.png`
- Press measurement: `21-fab-rest.png` vs `21-fab-press_0{1..4}.png` → fill 132→119 px, shadow 165→143 px
- Dark-mode shadow probe: column scan at x=600, y=640..680 in `01-home.png` vs `31-dark-home.png`

---

## 6. Recommended execution order

1. **F1, F2, F3, F5, F6, F7** — correctness/visual bugs; each is a small, self-contained change.
2. **F12 + F4 + F17** — one coherent "states & error copy" pass across every remote screen.
3. **F8, F9, F14, F15, F16** — motion language: unify press + transition + tokens.
4. **F10, F11, F13, F18** — accessibility pass.
5. Missed opportunities 1–4 (loading as brand moment, splash, onboarding, save celebration).

> Per the audit workflow, **no code has been changed**. Pick the findings you want turned into implementation plans and I will write self-contained, executor-ready plans (exact file paths, exact cubic-beziers/durations, verification steps).

---

# 8. Implementation log (2026-10-20) — what was fixed and verified

Everything below was implemented, built, installed on the device and re-measured.

## 8.1 Findings closed

| # | Sev | Fix | How it was verified |
|---|-----|-----|--------------------|
| F1 | HIGH | `AiJobProgressMode` (NONE/DETERMINATE/INDETERMINATE); an active job always shows a bar, determinate when the server reports a fraction, indeterminate otherwise | `AiJobCardPresentationTest` (7 cases) + device: AI Jobs & Drafts |
| F2 | HIGH | `HomeState.hasListContent` now derives from the **unfiltered** packs; added `hasSearchNoResults` | `HomeStateSearchTest` (4 cases) + device: search "zzzqqq" → "No packs found" |
| F3 | HIGH | Disabled fills are pre-composited against the surface (`neubrutalDisabledFill`) so they no longer composite with the black hard shadow | Device pixel probe: button fill is now **RGB(255,209,195)** (was RGB(118,72,58) = dark brown) |
| F4 | HIGH | New `SyncOperationPresentation` maps operation types/statuses to human labels and strips internal URLs, ports and timeout config from raw errors | `SyncOperationPresentationTest` (13 cases) + device: Sync now shows "Updating pack / Couldn't sync / The server took too long to answer." and no raw UUID, enum or URL |
| F5 | HIGH | `PaywallProductPresentation`: a product without a store price is **not purchasable** and says "Price unavailable right now"; the duplicated token line is gone | `PaywallProductPresentationTest` (6 cases) + device: single "100 AI tokens (non-expiring)" line, Buy button rendered disabled |
| F6 | HIGH | `EmptyState` is scrollable **only when the height is bounded** and the illustration is capped at 180 dp | `EmptyStateScrollModeTest` (2 cases) + device: with the keyboard open the title is 67 px tall (was clipped to 14 px) and the description is fully visible |
| F7 | HIGH | Dark mode uses a **light** hard shadow instead of a black one | Device pixel probe: shadow vs page ΔL **19 → 37** (light mode 219) |
| F8 | MED | The page indicator follows the pager (not the view model), animates scale + colour | `PageIndicator` now animates; device capture of the page 1 → 2 transition |
| F9 | MED | New shared `NeubrutalIconButton` (scale + shadow press, no Material ripple) replaces all 9 raw `IconButton`s | Compile + device smoke test |
| F10 | MED | Search clear button: 32 dp → 48 dp touch target, 20 dp glyph | Layout dump |
| F11 | MED | Reorder buttons: the tap target is now the whole cell (44 × 48 dp) instead of a 32 dp glyph | Layout dump |
| F12 | MED | A shared in-flight treatment: indeterminate `LinearProgressIndicator` for active AI jobs, `ContentLoadLayout` already used by the other remote screens | Device |
| F13 | MED | Inactive page dot 18 % → 45 % alpha (≈1.3:1 → ≈3.2:1) | Pixel probe |
| F14 | MED | Press spring is no longer bouncy (`SetikerMotion.PressSpring`); bounce is reserved for celebrations | Measured before: 127 → 117 → 119 px overshoot |
| F15 | MED | Focus ring that fades in on both search fields | Device |
| F16 | MED | Branded splash: coral accent on the logo mark, spring entrance, caption fade, and a cross-fade into the app instead of a hard cut | Device capture |
| F17 | MED | Explore error state has its own description | Device: "Check your connection and try again." |
| F18 | MED | Decorative pack thumbnails: `contentDescription = null`; sticker cells announce "Sticker N of M" | Layout dump |
| F19 | MED | "Processing was interrupted" is rendered once | Device |
| F20 | MED | AI Jobs FAB glyph changed from a refresh arrow to a broom (`DeleteSweep`) to match "Clear completed" | Device |
| F21 | LOW | Profile list gets 24 dp bottom content padding | Device |
| F22 | LOW | Empty-state illustration capped at 180 dp (was ~247 dp) | Device |

## 8.2 Extra bugs found while implementing

| Sev | Bug | Fix |
|-----|-----|-----|
| **HIGH** | **Cold-start race**: tapping Sync/Profile in the first seconds after opening the app sent a *signed-in* user to the login screen. Reproduced 3/3. | `HomeState.authState` + `homeAuthAction()`; the account buttons stay disabled until the session resolves. `HomeAuthActionTest` (3 cases). Verified 3/3 fixed. |
| **HIGH** | **Crash**: the new scrollable `EmptyState` threw `IllegalStateException: Vertically scrollable component was measured with an infinity maximum height constraints` inside `AnimatedContent` (the Explore error state killed the app). | `emptyStateScrollMode()` — the scroll is only used when the height is bounded. Verified: the Explore error state renders, no FATAL in logcat. |
| MED | The neubrutal gloss allocated two `Path` objects and two gradient brushes **per frame per surface**. | Rewritten with `drawWithCache`; the geometry is built once per size change. |
| MED | The loading indicator read an `Animatable` in composition → the whole indicator recomposed every frame. | Moved to `graphicsLayer` (draw phase). |
| LOW | 187 unused imports across 90 files, and import blocks that were neither sorted nor consistent. | Removed and normalised to one sorted block per file (ktlint "official" style). |

## 8.3 Performance: measured before and after (Redmi Note 11, same scenario)

| Metric | Before | After |
|---|---|---|
| Janky frames | 124 (**43.1 %**) | 38 (**13.3 %**) |
| 50th percentile frame | 32 ms | **20 ms** |
| 90th percentile frame | 42 ms | 48 ms |
| "Slow issue draw commands" | **101** | **11** |
| "Slow bitmap uploads" | 1 | 0 |
| Frames over 33 ms (2 vsync) | 19.1 % | **5.0 %** |

Steady-state scroll of the 16-sticker grid: 6.25 % janky frames, p50 12 ms, p90 23 ms.

## 8.4 Codebase hygiene

- **Kotlin**: `allWarningsAsErrors = true` is now set for every target (official Kotlin Gradle Plugin option). The build **fails on any warning**. Verified by injecting a deliberate deprecated call → the build failed with an error, then the probe was removed.
- The Kotlin compiler does **not** report unused imports (confirmed with the Kotlin team: "All other warnings about unused declarations come from IDE, not the compiler"), so the import blocks were cleaned with a script and normalised to one sorted block per file.
- **TypeScript (`stiker-api`)**: `tsc --noEmit` clean, `eslint src` clean, **415 tests pass** (the vitest suite needs `npm run test:up`; the containers were started for the run).

## 8.5 Smoke test

55 navigation states across 14 screens (home, search, sort sheet, AI jobs, pack detail, sticker grid, editor, decoration sheet, emoji picker, crop, explore + all four tabs, profile + all menu entries, settings, sync, AI generate sheet, create pack, share sheets, delete dialog, onboarding, splash) — **zero crashes**.

## 8.6 Follow-up work (completed 2026-10-20, after the audit)

| Item | Status |
|---|---|
| Coil placeholder for images | **Done.** `NeubrutalAsyncImage` renders a Compose-driven shimmer while a bitmap loads, and a `SolidColorPainter` covers the error state. Coil 3 has no colour painter, so it is built here. Applied to the sticker card, the pack cover and the editor preview. |
| `SetikerMotion.DelightSpring` | **Used.** The sticker-saved celebration: a coral check badge pops in over the preview with the bouncy spring, holds for 1.2 s, then the app returns to the pack. It is the only place in the app allowed to bounce. |
| README rewrite | **Done.** New README with the mark, a one-line lede, badges, a hero montage of four real screens, a feature table built from device screenshots, a saved-sticker GIF, a three-command quick start, a configuration table, a mermaid architecture diagram, and the MIT `LICENSE` the README had always claimed. |
| Commits | 5 commits pushed to `origin/audit/android-modernization`: `4c983d0` design system, `74a1b5f` screen states, `16b6130` cold-start race, `5f46f8e` docs and README, `b05e2df` celebration timing. |

## 8.7 Open decisions for the product owner

1. **Launcher icon / system splash branding**: the system splash shows the launcher icon, which is pure black and grey — the brand coral never appears in the first two seconds of the app. Making the icon coral is a product decision, so it was left alone.
2. **iOS**: the iOS targets cannot be compiled on Windows, so the shared `commonMain` changes were not verified on the native target. They only use common Compose APIs.
3. **Delight moments**: the sticker-saved celebration is the first use of `DelightSpring`. The same spring is available for a created pack, a finished AI job, and an imported sticker.


---

# 9. Test and performance round (2026-10-21)

The audit produced findings; this round made sure they stay fixed, and made the performance claims
measurable instead of anecdotal.

## 9.1 The two test suites that did not run, and why

Both failures had the same shape: the test code was right, the build was wrong.

**The Compose compiler plugin was missing from the app module.** The shared module has it, the app
module did not. Every `@Composable` lambda in `androidApp` therefore compiled to the wrong JVM
signature: `setContent` was emitted as a call to a method that takes a plain lambda, while the
runtime class expects the composable one. The screenshot test died with

```
java.lang.NoSuchMethodError: 'void androidx.compose.ui.test.junit4.AndroidComposeTestRule.setContent(kotlin.jvm.functions.Function0)'
```

and the instrumented test never got past its first composition. The bytecode says it plainly: the
call site was `setContent(Function0)`, the class on the classpath declares `setContent(Function2)`.
The app module holds no UI of its own, but both of its test source sets do, so the plugin belongs
there.

**Xiaomi's MIUI blocks an activity that starts in the background.** The instrumentation restarts
the app, so the Compose test harness always launches its activity from the background, and MIUI
denies it. The denial is not an error - the test waits for a window that never arrives, so the
suite hangs on the first assertion instead of failing. The permission (`appops set <pkg> 10021
allow`) is reset every time the package is replaced, so it has to be granted between the install
and the run. AGP's `connectedDebugAndroidTest` installs and runs in one step and leaves no room
for that; `:androidApp:connectedUiTest` does the three steps in the order they need to happen in.

## 9.2 What runs now

| Suite | Command | Result |
| --- | --- | --- |
| Unit and view-model tests | `:composeApp:testAndroidHostTest` | 51 suites, green |
| Screenshot and performance tests | `:androidApp:testDebugUnitTest` | 7 tests, green |
| Roborazzi references | `:androidApp:recordRoborazziDebug` / `verifyRoborazziDebug` | 4 components |
| Instrumented UI and accessibility | `:androidApp:connectedUiTest` | 7 tests, 34 s on the phone |
| Visual regression of the screens | `python tools/screenshot_tests.py` | 6 screens, 0.00 % |
| Memory over the navigation cycles | `python tools/memory_check.py` | no growth |
| Frame times of the sticker grid | `python tools/frame_stats.py` | 3.9 % janky, 0 slow draw |

## 9.3 The leaks

**The connectivity callback was registered again on every composition.** `AppNavigation` started
the network monitor in a `LaunchedEffect(Unit)` and never stopped it, while the sync manager
started the same monitor for the whole process. The connectivity manager keeps a strong reference
to every callback it is given, so a navigation that came and went left another one behind each
time. The monitor now counts its callers: one registration, unregistered when the last caller is
gone.

**The splash screen recomposed its logo on every frame.** The scale and the alpha were read in the
composition, so each frame of the animation recomposed the logo. Both now live in the draw phase,
in a `graphicsLayer`.

**No leak in the app itself.** Three cycles, twenty-one screen visits: one activity, seven views,
five app contexts and one view root, all constant. The footprint moved by 3 %, which is the
picture cache warming up. `tools/memory_check.py` is the tool, and it is in the repository so the
next person can run it.

## 9.4 The recompositions

The sticker cell and the pack card are skipped when something else on the screen changes.
`RecompositionTest` proves it: it renders the grid and the list, counts how often the cell and
the card are composed, and fails when a change that does not touch the list composes the list
again. The rule renders on a Robolectric device, so it is the same composition and the same
skipping the app does.

The first version of that test counted the wrong thing twice, and both are worth writing down:

- It counted the item scope, which re-enters whenever the parent recomposes no matter what. That
  is the "leaving the house" cost, and the Compose guidance is not to chase it.
- It counted through a lambda that was recreated on every pass, which made every parameter
  unstable and every cell skippable - the test then failed for a reason that had nothing to do
  with the app.

**The AI jobs screen regrouped its whole list on every recomposition.** `groupDrafts` groups the
drafts, maps them into items and filters them into five sections - several passes over the data,
with a new list each time - and it ran in the composable body. The section titles were resolved
with `stringResource` inside the grouping, which is what stopped it from being cached. The titles
are resources now, the grouping is a plain function, and the result is kept with `remember`.

**Two lazy lists were missing their keys.** The sync operations and the emoji picker rebuilt their
rows whenever the data changed.

## 9.5 The frame times

On the sticker grid, with the phone under its usual memory pressure: 587 frames, 3.9 % janky, and
no slow draw or sync commands at all. The p50 sits at 16 ms rather than the 9.8 ms measured
earlier in the session, because the phone had 61 MB of free memory instead of 289 MB; the frame
counters are the meaningful number, the milliseconds are relative.

# 10. The error states (2026-10-21)

The complaint that started this: the register screen said "You are offline" to a phone that was
online, and the error itself sat at the bottom of the form where nobody was looking. Both were
true. The offline claim was false, and the error was invisible.

## 10.1 The message that lied

A failed connection to one server says nothing about the device's connectivity. The phone can be
perfectly online and still not reach the API - a captive portal, a proxy, a server that is down,
a tunnel that is not there. The classifier mapped every I/O failure to `NoInternet` and every
`NoInternet` to "You are offline. Check your connection and try again."

The network monitor already knew the difference, so the classifier now asks it:

- the device has no network -> `NoInternet` -> "You are offline. Check your connection and try
  again."
- the device has a network and the server could not be reached -> `ServerUnreachable` -> "Can't
  reach the server. Try again in a moment."

A new `ConnectivityState` holds the last known connectivity. The monitor writes it, the classifier
reads it, and the message tells the truth.

This is not a theory: the screenshot in the evidence folder is the real app, on a phone with wifi,
with the API tunnel removed, saying "Can't reach the server. Try again in a moment."

## 10.2 The error nobody could see

The form error was a card at the bottom of the form, below the last field. The submit button is in
the bottom bar, so the user pressed the button at the bottom of the screen and the error appeared
off the bottom of the screen.

The error is now a snackbar:

- it appears where the thumb is, above the bottom bar;
- it is built on Material's `SnackbarHost`, so the state machine, the placement and the enter and
  exit animation are the platform's, and only the look is the app's;
- the look is neubrutalist - the thick border and the hard offset shadow every other surface in
  the app has, instead of the stock grey pill;
- the error variant carries the error container, a full strength error border, a solid icon in a
  filled circle and a bold message, because an error that does not look like an error gets read as
  a note, and a note gets ignored;
- the line that drains along its bottom edge is the time the user has. The close button ends it
  right away, and the buttons press with the app's spring;
- it announces itself to a screen reader with a polite live region, so it does not interrupt the
  speech but is not missed either.

The error card at the bottom of the forms, the bare red line under a field and the small red text
in the editor are gone. What is left in their place: the snackbar for the errors of the screen,
and the inline text for the errors of a field.

## 10.3 What a screen reader hears

A red border is invisible to a screen reader, so the text fields now carry the reason they are
invalid in the `error` semantics, and TalkBack says that the field is invalid and what is wrong
with it. `ErrorSemanticsTest` holds both properties in place: the snackbar is a live region, and
an invalid field says what is wrong with it.

## 10.4 The full screen errors

An error that looks like an empty state reads as "there is nothing here", and the user waits for
content that is never going to arrive. The empty state has a danger treatment now: a solid error
icon, a bold title in the error colour, and the two screens that show a full screen failure use it.

## 10.5 The evidence

| | |
| --- | --- |
| ![error](https://github.com/iqbalShafiq/setiker/raw/audit/android-modernization/androidApp/src/screenshots/debug/error_snackbar.png) | The error snackbar: the error container, the solid icon, the bold message, the close button, and the draining timer line. The reference image lives with the tests, so a change in the look has to be a deliberate one. |

# 7. Methodology (cheap, repeatable, and how to keep it cheap)

### What I used for this audit — and why it costs ~nothing

| Technique | Cost | What it caught |
|---|---|---|
| `android layout` (semantics/a11y tree dump) + `android screen capture` | free, ~1 s/screen | invisible CTAs, non-focusable buttons, missing/duplicate `contentDescription`, wrong empty-state copy, dead branches |
| **Device animation-scale override** (`settings put global animator_duration_scale 10` / `0`) | free | frame-by-frame motion capture **without ffmpeg**; reduced-motion behaviour; press-feedback measurement |
| Pixel probes (PIL/numpy) on the captures | free | disabled-button compositing bug, dark-mode shadow-contrast collapse, shadow-offset animation |
| Read-only static review of motion call sites | free | missing reduced-motion handling, mixed press languages, unreachable branches |
| `adb reverse` + a throwaway API account (created → deleted) | free | Explore error/empty states, Paywall, Sync, AI Jobs, Notifications |

**Key trick:** Android's `animator_duration_scale` is honoured by Compose's `MotionDurationScale`, so setting it to `10` slows every Compose animation 10×. That turns a 180 ms transition into ~1.8 s — slow enough to screenshot every frame — and setting it to `0` is exactly the OS "Remove animations" accessibility setting, so you can verify reduced-motion behaviour for free. No emulator, no video decoding, no ffmpeg install.

**Cost control rules used here:**
1. Never trigger a real LLM call to observe a loading state — inject/fake the state instead.
2. Never delete real user data to reach an empty state — reach it via a *filtered* query or a throwaway account (deleted afterwards).
3. Read-only static review first; only then confirm on device (each device step costs a turn).
4. Reuse one device session for many screens via `adb shell input` + dumps, instead of re-launching.

### Making this repeatable in CI (recommended follow-up)

Google's own guidance lines up with what this audit exposed:

- **Compose Preview Screenshot Testing** is the officially recommended way to verify visual attributes — <https://developer.android.com/training/testing/ui-tests/screenshot>. It runs on the JVM, so it is cheap and needs no device.
- **Roborazzi** (JVM/Robolectric) gives the same golden-image workflow without an emulator — <https://github.com/takahirom/roborazzi>. It also emits a machine-readable `.uitree.json` sidecar, which is exactly what the `android layout` dumps above already give you.
- **Automated accessibility checks** are available since Compose 1.8.0 via `ui-test-junit4-accessibility` + `enableAccessibilityChecks()` — it flags *missing labels*, *low contrast*, *small touch targets* and *traversal order* automatically. That would have caught F10, F11, F13, F18 on every build — <https://developer.android.com/develop/ui/compose/accessibility/testing>.
- **Compose UI Check mode** in Android Studio previews contrast/size problems before you even run the app.
- **Coverage matrix to automate** (per the Android testing-strategy guidance): each screen in **9 size buckets** (400/610/900 dp × 400/500/1000 dp) × **all themes** × **font scale 1.5**, plus one dedicated variant per **state** — and crucially: *"test loading screens by injecting a loading state to the UI or simulating it with a fake."* That fake-loading instruction is the sanctioned version of the stub you suggested; it is the cheapest way to lock in F1, F12 and F6.
- **Now in Android's testing strategy** — shift-left: behaviour tests for state, screenshot tests for pixels, and never assert on colours with APIs when a screenshot can answer it — <https://github.com/android/nowinandroid/wiki/Testing-strategy-and-how-to-test>.

**Cheap 3-step recipe I'd suggest for this repo next:**
1. Add `ui-test-junit4-accessibility` + one `enableAccessibilityChecks()` smoke test per screen → kills the a11y class of bugs automatically.
2. Add Roborazzi screen-level golden tests for the 5 screens with the worst findings (Home empty, AI Jobs, Paywall, Sync, Create Pack) at 400×500 / 1080×2400 and font scale 1.5, light + dark → kills F2, F3, F7, F13 regressions.
3. Add a fake/`TestDispatcher` loading state to each `*ViewModel` so the loading UI is always renderable in a test → kills F1/F12 forever.
