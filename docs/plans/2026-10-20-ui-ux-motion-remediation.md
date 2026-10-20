# Implementation Plan — Setiker UI/UX + Motion + Performance remediation

**Source:** `docs/plans/2026-10-19-ui-ux-motion-audit.md` (22 findings)
**Rules:** TDD (test first, watch it fail), no behaviour regressions, KMP-safe (commonMain shared with iOS).
**Status:** every phase below is complete and verified on a device. See the audit document for the
measured before/after numbers and the commit that closed each item.

## Phase 1 — HIGH: correctness & visual bugs
- [x] F1 AI jobs: active job must show progress even without a `progress` payload
- [x] F2 Home: search-no-result must show the search empty state (not first-run empty state)
- [x] F3 Disabled primary/secondary buttons must not composite over the shadow layer
- [x] F5 Paywall: show price + currency, de-duplicate token copy, retitle
- [x] F6 EmptyState: must stay scrollable so description + CTA never get squeezed out
- [x] F7 Dark mode: hard-shadow depth must remain legible

## Phase 2 — states & error copy
- [x] F12 consistent in-flight treatment for remote reads
- [x] F4 Sync: user-facing error copy instead of raw debug strings
- [x] F17 Explore error branch: its own description
- [x] F19 AI jobs: no duplicated "Processing was interrupted"

## Phase 3 — motion language
- [x] Motion tokens (duration + easing) centralised in `SetikerMotion`
- [x] F8 onboarding: one coherent page transition + animated page dots
- [x] F9 raw Material IconButtons → shared neubrutal button (no ripple)
- [x] F14 press spring: no bounce on high-frequency targets
- [x] F15 Home search: focus ring + clear transition
- [x] F16 splash: brand colour + logo motion + cross-fade

## Phase 4 — accessibility
- [x] F10 search clear button 48dp
- [x] F11 reorder buttons focusable
- [x] F13 page indicator contrast + size delta
- [x] F18 decorative thumbnails null contentDescription; sticker alt text
- [x] F20 AI jobs FAB glyph/label match
- [x] F21 profile last-row bottom padding
- [x] F22 illustration height cap

## Phase 5 — missed opportunities
- [x] Loading as brand moment (shimmer placeholder for every image)
- [x] Onboarding transition polish (dots follow the pager and animate)
- [x] Save celebration using `SetikerMotion.DelightSpring`

## Phase 6 — performance
- [x] Measured on device (`dumpsys gfxinfo` + per-stage frame analysis)
- [x] Draw-phase allocations in the neubrutal gloss moved into a `drawWithCache`
- [x] Recomposition-triggering animation read in the loading indicator moved to the draw phase
- [x] Result: janky frames 43.1 % → 13.3 %, slow draw commands 101 → 11

## Phase 7 — automated guards
- [x] Unit tests for every fixed state/copy path (219 Kotlin tests, 0 failures)
- [x] `allWarningsAsErrors = true` for every target, verified by injecting a deprecated call
- [x] Unused imports removed and import blocks normalised

## Verification (all phases)
- [x] `.\gradlew.bat :composeApp:testAndroidHostTest :composeApp:lint :androidApp:assembleDebug`
- [x] Re-run on device: screenshot + layout dump for each changed screen (light + dark)
- [x] Perf re-measure: frame timing before/after
- [x] 55-state navigation smoke test, zero crashes
