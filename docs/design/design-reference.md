# Setiker - Design Reference

The production UI uses a **neubrutal** design system implemented in Compose Multiplatform.

## Source of truth (code)

- Theme tokens: `composeApp/src/commonMain/kotlin/presentation/theme/Theme.kt`
- Colors: `presentation/theme/NeubrutalColors.kt`
- Clay accents: `presentation/theme/Claymorphism.kt`
- Shared components: `presentation/components/` (`AppTopBar`, `EmptyState`, `PackBottomBar`, `NeubrutalSearchBar`, sheets, dialogs)

Prefer reusing these primitives over one-off screen styling.

## Legacy notes (historical)

### Color Palette (older Material3 mock)
- **Primary:** #25D366 (WhatsApp Green)
- **Secondary:** #128C7E (WhatsApp Dark Green)
- **Surface:** #F0F2F5 (Light Gray)
- **Background:** #FFFFFF (White)
- **On Surface:** #111B21 (Dark Text)
- **Error:** #FF3B30

### Typography
- Display: Large, bold headlines for pack names
- Headline: Medium weight for section titles
- Body: Regular weight for descriptions
- Label: Small, medium weight for captions

### Components
- Cards: Rounded corners (16dp), subtle elevation
- Buttons: Pill-shaped primary buttons, outlined secondary
- FAB: Circular, primary color for main actions
- Chips: Rounded, for categories and filters

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
- Screen transitions: Slide from right
- List items: Fade in with stagger
- Button presses: Scale down slightly
- FAB: Scale up with spring animation
- Cards: Subtle elevation change on press

## Accessibility
- All icons have content descriptions
- Color contrast meets WCAG AA
- Touch targets minimum 48dp
- Screen reader support for all actions
