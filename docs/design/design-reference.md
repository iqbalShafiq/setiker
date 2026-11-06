# Setiker - Design Reference

The production UI uses a **neubrutal** design system implemented in Compose Multiplatform: flat
fills, one thick ink outline, and a hard offset shadow with no blur. This file records the tokens,
the rules that hold the system together, and the places where we deliberately differ from the
published canon — so that nobody "fixes" a decision that was made on purpose.

Canon reference: [neubrutalism.com](https://neubrutalism.com/).

## Source of truth (code)

- Tokens (borders, radii, shadows, spacing): `presentation/theme/Claymorphism.kt`
- Colours: `presentation/theme/NeubrutalColors.kt`
- Motion: `presentation/theme/SetikerMotion.kt`
- Icons: `presentation/icons/SetikerIcons.kt`, `presentation/icons/GlyphMorph.kt`
- Shared components: `presentation/components/`

Prefer reusing these primitives over one-off screen styling.

## Tokens

| Token | Value | Used for |
| --- | --- | --- |
| `NeubrutalBorderWidth` | 2.5 dp | Every outlined surface |
| `NeubrutalThinBorderWidth` | 1.5 dp | Secondary edges only |
| `NeubrutalSmallShadowOffset` | 3 dp | Chips, icon buttons, small controls |
| `NeubrutalShadowOffset` | 4 dp | Cards, panels, buttons |
| `NeubrutalLargeShadowOffset` | 6 dp | Dialogs, elevated overlays |
| `NeubrutalPressedShadowOffset` | 1 dp | While a control is held |
| `NeubrutalSmallRadius` | 8 dp | Chips, icon buttons |
| `NeubrutalCardRadius` | 12 dp | Cards, previews |
| `NeubrutalButtonRadius` | 16 dp | Primary and secondary buttons |
| `NeubrutalDialogRadius` | 20 dp | Dialogs and sheets |

## Rules

1. **One stroke width.** 2.5 dp nearly everywhere; deviate only to establish hierarchy.
2. **The shadow must be wider than the border.** At 2 dp against a 2.5 dp outline the copied shape
   could not be separated from the frame, and the bottom-right read as a thicker border instead of
   a surface behind it. It is 3 dp for small controls for exactly this reason.
3. **The fill stops half a pixel short of its outline** (`Modifier.neubrutalFill`). A fill that
   reaches the clipped edge leaves the last pixel half covered, and over a hard shadow it
   composites to grey — a notch in the only corner the shadow is not hidden behind.
4. **One ink.** The border and the shadow share the shadow colour. A differently coloured shadow is
   a different language (a light-cast shadow, not a stacked copy), and it breaks the dark theme,
   where the shadow inverts to a light offset.
5. **Borders carry meaning.** A border should say container, interactive, focus, selected, or
   error. Purely decorative outlines are noise. `NeubrutalBorderRole` names those states once:

   | Role | Ink | Stroke |
   | --- | --- | --- |
   | `Container` | ink | 2.5 dp |
   | `Selected` | accent | 3.75 dp (1.5×) |
   | `Error` | `ErrorRed` | 2.5 dp |
   | `Disabled` | muted ink | 2.5 dp |

   Selection changes weight as well as colour on purpose: colour alone is one channel, and a
   selected tile has to survive a greyscale screenshot. A *filled* chip does not take the
   `Selected` role — its fill already carries the state, and a second signal would be the same
   word twice. `Disabled` complements it: a control that cannot be pressed also drops its shadow,
   because a raised shadow promises a press that will never come.
6. **Press is a scale plus a shadow shrink**, never a ripple. The shadow animates in the draw phase,
   so a press does not recompose the screen.
7. **Focus is visible.** Ripple removal also removed the focus hint, so `clickableNoRipple`,
   `combinedClickableNoRipple` and `toggleableNoRipple` add `neubrutalFocusRingFor`.
8. **Motion comes from `SetikerMotion`** and always ships with reduced-motion handling.

## Documented deviations from the canon

| Canon | Ours | Why |
| --- | --- | --- |
| `border-radius: 0` | 8–20 dp | The app is full of friendly clay illustrations and warm copy. Square corners read as harsh rather than confident. |
| No inner bevel | Gloss band on the top-left | Our signature; it sells "printed on paper" at small sizes. |
| Shadow tiers 3 / 5 / 8 / 12 | 3 / 4 / 6 | Tuned for dp on a phone rather than px on a page; the hierarchy is the same. |

## States

| State | How it reads |
| --- | --- |
| Default | Ink outline, flat fill, hard shadow to the bottom-right |
| Pressed | Slightly smaller, shadow shrinks toward the control |
| Toggled | The glyph changes (fill wipe or shape morph), not the frame |
| Focused | Coral ring inside the outline — never colour alone, always a second channel |
| Disabled | Fill and ink at reduced opacity, still outlined |
## Screen Designs

### 1. Home Screen (Dashboard)
- **Layout:** Vertical scroll with sticky top app bar
- **App Bar:** Large title "My Stickers" with settings icon
- **Content:** 
  - Grid of sticker pack cards (2 columns)
  - Each card shows pack thumbnail, name, sticker count
  - "Add to WhatsApp" button on each card
- **FAB:** Plus icon to create new pack
- **Empty State:** Illustration with "Create your first pack" CTA

### 2. Sticker Pack Detail Screen
- **Layout:** Vertical scroll
- **App Bar:** Pack name with back button, actions menu
- **Content:**
  - Pack info card with thumbnail, name, publisher
  - Grid of stickers (3 columns) with 512x512 previews
  - Long press to edit/delete sticker
- **Actions:**
  - "Add to WhatsApp" primary button
  - "Edit Pack" secondary button
  - Delete pack option in menu

### 3. Create/Edit Pack Screen
- **Layout:** Form with vertical scroll
- **Fields:**
  - Pack name input (text field)
  - Publisher name input
  - Tray icon picker (image selector)
- **Sticker Grid:**
  - Horizontal scroll or grid of existing stickers
  - "Add Sticker" card with plus icon
  - Drag to reorder
- **Actions:**
  - "Save Pack" primary button
  - "Cancel" text button

### 4. Sticker Editor Screen
- **Layout:** Full screen editor
- **Image Preview:** Large preview of selected image
- **Tools:**
  - Crop button (opens crop screen)
  - Background remover button
  - Rotate/flip buttons
  - Undo/redo buttons
- **Bottom Sheet:** Emoji picker for tagging (up to 3)
- **Actions:**
  - "Save Sticker" primary button
  - "Cancel" text button

### 5. Image Crop Screen
- **Layout:** Full screen with crop overlay
- **Preview:** Image with crop frame (1:1 ratio, 512x512)
- **Tools:**
  - Aspect ratio selector (1:1 fixed for stickers)
  - Rotate left/right
  - Flip horizontal/vertical
  - Zoom slider
- **Actions:**
  - "Apply" primary button
  - "Cancel" text button

### 6. Background Remover Screen
- **Layout:** Split screen or full screen
- **Preview:** Before/after comparison or single preview
- **Tools:**
  - Brush size slider
  - Erase/restore toggle
  - Auto-remove button (AI/ML)
  - Zoom controls
- **Actions:**
  - "Apply" primary button
  - "Cancel" text button

## Platform Considerations

### Android
- Status bar: Use windowLightStatusBar for light theme
- Navigation bar: Match background color
- Edge-to-edge: Handle insets with WindowInsets

### iOS
- Status bar: Handle safe area insets
- Home indicator: Respect safe area bottom
- Navigation: Use native-like transitions

## Animation & Motion
- List items: Fade in with stagger
- Button presses: Scale down slightly
- FAB: Scale up with spring animation
- Cards: The shadow shrinks, never an elevation change

## Accessibility
- All icons have content descriptions
- Color contrast meets WCAG AA
- Touch targets minimum 48dp
- Screen reader support for all actions