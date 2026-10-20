# Implementation Plan — Setiker UI/UX + Motion + Performance remediation

**Source:** `docs/plans/2026-10-19-ui-ux-motion-audit.md` (22 findings)
**Rules:** TDD (test first, watch it fail), no behaviour regressions, KMP-safe (commonMain shared with iOS).

## Phase 1 — HIGH: correctness & visual bugs
- [ ] F1 AI jobs: active job must show progress even without a `progress` payload
- [ ] F2 Home: search-no-result must show the search empty state (not first-run empty state)
- [ ] F3 Disabled primary/secondary buttons must not composite over the shadow layer
- [ ] F5 Paywall: show price + currency, de-duplicate token copy, retitle
- [ ] F6 EmptyState: must stay scrollable so description + CTA never get squeezed out
- [ ] F7 Dark mode: hard-shadow depth must remain legible

## Phase 2 — states & error copy
- [ ] F12 consistent in-flight treatment for remote reads
- [ ] F4 Sync: user-facing error copy instead of raw debug strings
- [ ] F17 Explore error branch: its own description
- [ ] F19 AI jobs: no duplicated "Processing was interrupted"

## Phase 3 — motion language
- [ ] Motion tokens (duration + easing) centralised
- [ ] F8 onboarding: one coherent page transition + animated page dots
- [ ] F9 raw Material IconButtons → shared neubrutal button (no ripple)
- [ ] F14 press spring: no bounce on high-frequency targets
- [ ] F15 Home search: focus ring + clear transition
- [ ] F16 splash: brand colour + logo motion + cross-fade

## Phase 4 — accessibility
- [ ] F10 search clear button 48dp
- [ ] F11 reorder buttons focusable
- [ ] F13 page indicator contrast + size delta
- [ ] F18 decorative thumbnails null contentDescription; sticker alt text
- [ ] F20 AI jobs FAB glyph/label match
- [ ] F21 profile last-row bottom padding
- [ ] F22 illustration height cap

## Phase 5 — missed opportunities
- [ ] Loading as brand moment (skeletons for lists)
- [ ] Onboarding transition polish
- [ ] Save/pack creation celebration

## Phase 6 — performance
- [ ] Measure on device (frame metrics, recomposition, draw work)
- [ ] Fix: infinite animations that keep running off-screen
- [ ] Fix: recomposition-triggering animation reads
- [ ] Fix: draw-phase allocations in Claymorphism

## Phase 7 — automated guards
- [ ] Unit tests for every fixed state/copy path
- [ ] Compose UI tests for the regression-critical screens

## Verification (all phases)
- [ ] `.\gradlew.bat :composeApp:testDebugUnitTest :composeApp:lint`
- [ ] Re-run on device: screenshot + layout dump for each changed screen (light + dark)
- [ ] Perf re-measure: frame timing before/after
