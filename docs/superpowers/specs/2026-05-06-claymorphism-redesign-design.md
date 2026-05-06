# Setiker Claymorphism Redesign Design Spec

**Date:** 2026-05-06  
**Approach:** Pastel Playful Clay (Approach A)  
**Framework:** Kotlin Compose Multiplatform (Compose Material 3)  

---

## 1. Overview

Redesign the entire Setiker app (WhatsApp Sticker Maker) with a **playful claymorphism** visual language. The current design uses a generic WhatsApp-green Material 3 theme with flat cards, standard shadows, and no interaction feedback. The new design aims to feel soft, inflated, playful, and tactile — like squeezable clay.

---

## 2. Color Palette

### Light Mode

| Token | Hex | Usage |
|-------|-----|-------|
| `ClayBackground` | `#FFF8F0` | App scaffold background (warm cream) |
| `ClaySurface` | `#FFFFFF` | Card/dialog backgrounds |
| `ClayPrimary` | `#FF8C69` | Primary actions, FAB, active states (soft coral) |
| `ClayPrimaryLight` | `#FFE5DC` | Container/chip backgrounds |
| `ClaySecondary` | `#7DD3C0` | Secondary accents, success states (soft mint) |
| `ClayTextDark` | `#2D2D2D` | Headlines, primary text |
| `ClayTextMedium` | `#8B8680` | Descriptions, secondary text, placeholders |
| `ClayError` | `#FF6B6B` | Error states, delete actions (soft red) |
| `ClayShadow` | `rgba(255,140,105,0.12)` | Colored shadow tint (coral) |

### Dark Mode

| Token | Hex | Usage |
|-------|-----|-------|
| `ClayBackgroundDark` | `#2A2A2A` | Dark app background (soft charcoal) |
| `ClaySurfaceDark` | `#3A3A3A` | Dark card/dialog backgrounds |
| `ClayPrimaryDark` | `#FF9E85` | Brighter coral for dark mode contrast |
| `ClayTextDarkMode` | `#F0F0F0` | Light text on dark backgrounds |
| `ClayShadowDark` | `rgba(0,0,0,0.25)` | Darker, heavier shadows |

---

## 3. Typography

Continue using `FontFamily.Default` (system font) for broad compatibility across Android/iOS. Keep existing scale but adjust weights for more playful hierarchy:

- **Display titles** (screen headers): `FontWeight.Bold`, 28sp, `letterSpacing = (-0.5).sp`, color `ClayTextDark`
- **Card titles**: `FontWeight.SemiBold`, 16sp, color `ClayTextDark`
- **Body**: `FontWeight.Normal`, 14-16sp, color `ClayTextMedium`
- **Labels / Buttons**: `FontWeight.SemiBold`, 14sp, color `ClayTextDark` or white

---

## 4. Claymorphism Design System

### 4.1 Shadows (The Core of Clay)

Claymorphism requires **two layers of shadow** per elevated element:

1. **Outer Shadow** — gives the element lift off the surface:
   - Offset: `(0.dp, 6.dp)` to `(0.dp, 8.dp)`
   - Blur radius: `12.dp` to `16.dp`
   - Color: `ClayShadow` (coral tinted, never pure black)

2. **Inner Highlight** — simulates the top edge catching light (inflated look):
   - Achieved via an inset border or custom draw modifier
   - For Compose: use a top-aligned subtle gradient or a custom `Modifier` with `drawBehind`

### 4.2 Border Radius Scale

Vary radii by element size to maintain visual hierarchy:

| Element | Radius |
|---------|--------|
| Large containers / Dialogs | `28.dp` |
| Cards | `24.dp` |
| Buttons (Primary) | `20.dp` |
| Buttons (Small / Icon) | `16.dp` |
| TextFields | `16.dp` |
| Chips / Tags | `50%` (pill) |
| Image thumbnails inside cards | `16.dp` |
| Small icon buttons | `12.dp` |

### 4.3 Button States

All interactive elements must have visible feedback:

- **Default**: Full elevation, full color
- **Pressed / Active**: `scale(0.97f)`, shadow reduced by ~40%, color darkened slightly
- **Disabled**: Opacity `0.5f`, no shadow, flat
- **Transitions**: `200ms` smooth animation using `animate*AsState`

### 4.4 Cards

- Background: `ClaySurface` (white)
- Outer shadow: `offsetY = 8.dp`, `blurRadius = 16.dp`, color `ClayShadow`
- Inner top border: subtle 1-2px `rgba(255,255,255,0.6)` to simulate light reflection
- No visible stroke/border on default state
- Content padding: `16.dp` to `20.dp`

### 4.5 Floating Action Button (FAB)

- Replace circular default with **super-rounded square** (`20.dp` radius) or **squircle**
- Background: gradient from `ClayPrimary` to slightly lighter coral
- Shadow: prominent `offsetY = 8.dp`, `blur = 16.dp`
- Icon: white, `24.dp`
- Pressed: `scale(0.95f)`, shadow shrinks

---

## 5. Per-Screen Design

### 5.1 HomeScreen

- **TopBar**: Remove default Material 3 bottom line. Title "My Stickers" at `28.sp`, Bold, `ClayTextDark`. Background seamless with `ClayBackground`.
- **StickerPackCards**: Full clay card (24dp radius, double shadow). Thumbnail inside card uses 16dp radius. Pack name `SemiBold`, sticker count in `ClayTextMedium`.
- **FAB**: Super-rounded square with coral gradient and prominent shadow. Add subtle scale animation on press.
- **EmptyState**: Large clay circle (80dp) in soft mint (`ClaySecondary`) bg behind icon. Title `headlineMedium`, description `bodyLarge` in `ClayTextMedium`.
- **Loading**: Replace `CircularProgressIndicator` with a custom **3-dot bouncing clay loader** using `ClayPrimary` dots.

### 5.2 PackDetailScreen

- **Pack Info Header**: Large clay card (24dp radius) spanning full width. Tray icon (96dp) with 20dp radius inside. Pack name at `headlineSmall`, Bold.
- **Action Buttons**:
  - "Add to WhatsApp": Primary clay button (gradient coral, 20dp radius, shadow)
  - "Share Pack": Secondary clay button (white bg, coral border 1dp, shadow)
- **Sticker Grid**: StickerCard uses 16dp radius clay style. White bg, subtle shadow. Delete button as tiny clay circle (24dp) with `ClayError` bg, floating at top-end with small shadow.
- **Delete Dialogs**: Clay dialog (28dp radius, large soft shadow). Buttons stacked vertically, full width.

### 5.3 CreatePackScreen

- **TextFields**: Switch from `OutlinedTextField` to **filled style**:
  - Background: `ClaySurface`
  - Radius: `16.dp`
  - No visible outline; focus indicated by inner glow / subtle coral border
  - Label above in `titleMedium`
- **Tray Icon Selector**: 96dp box with 20dp radius. Empty state: dashed border `ClayPrimary` (2dp, 8dp dash/gap), light coral bg (`ClayPrimaryLight`). Filled state: clay card with image and shadow.
- **Sticker Preview Row**: Horizontal scroll of small clay cards (64dp, 16dp radius). Each shows sticker image. Remove button: tiny clay circle (20dp) with `ClayError` bg.
- **Save/Cancel**: Full-width clay buttons at bottom.

### 5.4 EditorScreen

- **Image Preview**: Large clay frame (300dp height, 24dp radius, thick soft border or inner shadow). Background `ClaySurface`.
- **Action Icons** (Crop / Remove BG): Clay circles (56dp) with `ClayPrimaryLight` bg, icon in `ClayPrimary`. Shadow soft.
- **Emoji Chips**: Pill-shaped clay (`ClayPrimaryLight` bg, `ClayTextDark` text, pill radius). Shadow very subtle. Remove icon inside chip.
- **Add Emoji Chip**: Pill-shaped **outlined** clay (coral border 1dp, transparent bg).
- **TextField** (Accessibility): Same filled clay style as CreatePack.
- **Save/Cancel**: Full-width clay buttons.

### 5.5 CropScreen

- **Crop Area**: Clay frame (24dp radius, inner shadow). Background `ClaySurface`. Overlay for crop mask stays functional but uses softer dark overlay.
- **Zoom Slider**: Track with rounded caps (`ClayPrimary` active, `ClayTextMedium` inactive). Thumb is a **clay circle** (20dp) with shadow.
- **Tool Buttons** (Rotate, Flip, Reset): Clay circles (48dp) with `ClaySurface` bg, icon `ClayTextDark`. Pressed state: `ClayPrimaryLight` bg.

### 5.6 BackgroundRemoverScreen

- **Canvas Area**: Clay frame (24dp radius, subtle `ClaySecondary` / mint border 2dp). Background `ClaySurface`.
- **Brush Slider**: Same clay slider style as CropScreen.
- **Tool Buttons**: Clay circles. Selected state uses `ClayPrimary` bg with white icon. Unselected uses `ClaySurface` bg.

---

## 6. Component Redesign Details

### 6.1 AppButton (`AppPrimaryButton`, `AppSecondaryButton`)

- **Primary**:
  - Shape: `RoundedCornerShape(20.dp)`
  - Colors: gradient from `#FF8C69` to `#FFA07A` (soft coral gradient)
  - Shadow: custom `Modifier` with `offsetY = 4.dp`, `blur = 8.dp`, `ClayShadow`
  - Content padding: `vertical = 16.dp`
  - Pressed: `scale(0.97f)` + reduced shadow + darker gradient

- **Secondary**:
  - Shape: `RoundedCornerShape(20.dp)`
  - Colors: bg `ClaySurface`, content `ClayPrimary`
  - Border: `1.dp` solid `ClayPrimary.copy(alpha = 0.3f)`
  - Shadow: same as primary but slightly lighter
  - Pressed: bg shifts to `ClayPrimaryLight`

### 6.2 AppTopBar

- Remove default bottom line / elevation
- Background: seamless `ClayBackground`
- Title: `28.sp`, `FontWeight.Bold`, `ClayTextDark`
- Navigation icon: standard icon button but can add subtle press feedback

### 6.3 AppTextField

- Switch from `OutlinedTextField` to a custom **clay filled text field**:
  - Use `BasicTextField` or styled `TextField` (filled variant)
  - Background: `ClaySurface`
  - Corner radius: `16.dp`
  - Shadow: very subtle inset or none; focus shows coral border (2dp) with soft glow
  - Label positioned above the field (not floating inside) for cleaner look

### 6.4 StickerPackCard

- Replace standard `Card` with custom clay card:
  - `RoundedCornerShape(24.dp)`
  - Custom shadow modifier (outer coral-tinted)
  - Inner top highlight via a 1-2dp light overlay at top edge
  - Padding: `16.dp`
  - Thumbnail: `RoundedCornerShape(16.dp)`, aspect ratio 1:1
  - Pack name: `FontWeight.SemiBold`, `16.sp`
  - Sticker count: `bodyMedium`, `ClayTextMedium`

### 6.5 StickerCard

- `RoundedCornerShape(16.dp)`
- Background: `ClaySurface`
- Subtle shadow (lighter than pack card)
- Delete button overlay: small clay circle (24dp) with `ClayError` bg, white icon, positioned top-end with 4.dp padding

### 6.6 AppDialog

- Card radius: `28.dp`
- Shadow: large and soft (`offsetY = 12.dp`, `blur = 24.dp`)
- Background: `ClaySurface`
- Title: `headlineSmall`, Bold
- Message: `bodyMedium`, `ClayTextMedium`
- Buttons: stacked vertically, full width, clay primary / clay secondary styles

### 6.7 LoadingIndicator

- Replace `CircularProgressIndicator` with a **3-dot bouncing clay loader**:
  - 3 circles (12.dp diameter), `ClayPrimary` color
  - Arranged horizontally with 8.dp spacing
  - Bouncing animation using `animate*AsState` with staggered delays (0ms, 150ms, 300ms)
  - Each dot scales from `1.0f` to `1.4f` and back with `300ms` duration

### 6.8 EmptyState

- Icon container: large clay circle (80dp) with `ClaySecondary` bg at 20% opacity, icon in `ClaySecondary`
- Title: `headlineMedium`, `ClayTextDark`
- Description: `bodyLarge`, `ClayTextMedium`
- Action button (if provided): clay primary button

### 6.9 EmojiPickerBottomSheet

- Sheet top corners: `28.dp`
- Sheet background: `ClayBackground`
- Title: `titleLarge`, padding 16.dp
- TabRow: pill indicator style or underline in `ClayPrimary`
- Emoji grid items: small clay circles (40dp) on hover/press with `ClayPrimaryLight` bg

---

## 7. Dark Mode Adaptation

All claymorphism principles apply to dark mode with these adjustments:

- Background: `ClayBackgroundDark` (`#2A2A2A`)
- Cards/Surfaces: `ClaySurfaceDark` (`#3A3A3A`)
- Primary: `ClayPrimaryDark` (`#FF9E85`) — brighter coral for contrast
- Shadows: heavier and darker (`rgba(0,0,0,0.25)`)
- Inner highlights: more prominent (simulates light catching on dark clay)
- Text: `ClayTextDarkMode` (`#F0F0F0`)

---

## 8. Animation & Motion

All transitions and interactions should feel **springy and physical**:

- **Button press**: `scale(0.97f)` with `spring()` animation
- **Card press**: slight scale down (`0.98f`) + shadow reduction
- **Screen transitions**: standard Compose navigation, but consider adding fade + slight scale on enter
- **Loading dots**: bouncing spring animation
- **Fab press**: `scale(0.95f)` + shadow compress
- **General**: use `animate*AsState` with `spring(dampingRatio = Spring.DampingRatioMediumBouncy)` for playful feel

---

## 9. Implementation Files Checklist

- [ ] `presentation/theme/Color.kt` — Add all clay color tokens
- [ ] `presentation/theme/Theme.kt` — Update `LightColorScheme` and `DarkColorScheme`
- [ ] `presentation/theme/Type.kt` — Optional: adjust weights if needed
- [ ] `presentation/components/AppButton.kt` — Full claymorphism redesign
- [ ] `presentation/components/AppTopBar.kt` — Seamless cream bg, bold title
- [ ] `presentation/components/AppTextField.kt` — Filled clay text field
- [ ] `presentation/components/StickerPackCard.kt` — Clay card with double shadow
- [ ] `presentation/components/StickerCard.kt` — Clay card, clay delete button
- [ ] `presentation/components/AppDialog.kt` — Large radius, heavy shadow, clay buttons
- [ ] `presentation/components/LoadingIndicator.kt` — 3-dot bouncing clay loader
- [ ] `presentation/components/EmptyState.kt` — Clay circle icon, updated text colors
- [ ] `presentation/components/EmojiPickerBottomSheet.kt` — Clay sheet, clay emoji items
- [ ] `presentation/home/HomeScreen.kt` — Update FAB, spacing, colors
- [ ] `presentation/packdetail/PackDetailScreen.kt` — Clay header card, clay buttons
- [ ] `presentation/createpack/CreatePackScreen.kt` — Clay inputs, clay tray selector
- [ ] `presentation/editor/EditorScreen.kt` — Clay frame, clay chips, clay buttons
- [ ] `presentation/crop/CropScreen.kt` — Clay frame, clay slider, clay tool buttons
- [ ] `presentation/backgroundremover/BackgroundRemoverScreen.kt` — Clay frame, clay tools

---

## 10. Design Principles Summary

1. **Soft & Inflated**: Every elevated element must feel like soft clay — round, puffy, with inner light.
2. **Playful Colors**: No harsh corporate greens. Use warm coral, soft mint, and cream.
3. **Tactile Feedback**: Every press, tap, and interaction must have physical feedback (scale, shadow shift).
4. **Consistent Lighting**: All shadows drop downward; all inner highlights catch light from the top.
5. **Generous Radii**: The larger the container, the rounder the corners. Never sharp edges on elevated surfaces.

---

*Spec written and ready for implementation.*
