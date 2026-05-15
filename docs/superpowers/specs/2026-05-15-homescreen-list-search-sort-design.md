# HomeScreen List Redesign with Search & Sort

**Date:** 2026-05-15
**Approach:** C — Neubrutal Card List
**Status:** Approved

---

## 1. Objective

Ubah layout `HomeScreen` dari **grid 2 kolom** ke **list vertikal** dengan tambahan:
- **Search bar** untuk filter pack berdasarkan nama
- **Sort** untuk mengurutkan pack (nama A-Z, Z-A, jumlah sticker, terbaru/terlama)

Semua komponen baru harus:
- 100% konsisten dengan design system **Neubrutalism** yang sudah ada
- Theme-aware (dark/light mode otomatis)
- Reusable untuk screen lain di masa depan

---

## 2. Architecture & Data Flow

### 2.1 State Changes (`HomeState`)

```kotlin
data class HomeState(
    val isLoading: Boolean = false,
    val packs: List<StickerPack> = emptyList(),
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.NAME_ASC,
    val error: String? = null,
    val currentUser: User? = null,
    val isSyncing: Boolean = false,
    val pendingSyncCount: Int = 0
) {
    val filteredPacks: List<StickerPack>
        get() = packs
            .filter { it.name.contains(searchQuery, ignoreCase = true) }
            .sortedWith(sortOrder.comparator)
}
```

### 2.2 SortOrder Enum

| Value | Label (UI) | Comparator |
|---|---|---|
| `NAME_ASC` | Name (A-Z) | `compareBy { it.name }` |
| `NAME_DESC` | Name (Z-A) | `compareByDescending { it.name }` |
| `STICKER_COUNT_ASC` | Stickers (fewest first) | `compareBy { it.stickers.size }` |
| `STICKER_COUNT_DESC` | Stickers (most first) | `compareByDescending { it.stickers.size }` |
| `NEWEST` | Newest first | `compareByDescending { it.identifier }` |
| `OLDEST` | Oldest first | `compareBy { it.identifier }` |

> **Note:** `NEWEST`/`OLDEST` menggunakan `identifier` sebagai proxy waktu pembuatan. Jika model `StickerPack` nantinya memiliki timestamp creation, comparator dapat diperbarui tanpa mengubah UI layer.

### 2.3 Intent Changes (`HomeIntent`)

Tambahan:
- `data class SearchQueryChanged(val query: String) : HomeIntent`
- `data class SortOrderChanged(val order: SortOrder) : HomeIntent`

### 2.4 ViewModel Behavior

- Filtering & sorting dilakukan **in-memory** setiap kali `searchQuery` atau `sortOrder` berubah
- `HomeScreen` menggunakan `state.filteredPacks` untuk render list (bukan `state.packs` langsung)
- Tidak ada network call tambahan untuk search/sort

---

## 3. New Components

### 3.1 `StickerPackListCard`

**Purpose:** Menampilkan satu StickerPack dalam layout horizontal, sebagai item list.

**Design Spec:**
- **Root:** `Row` dengan `Modifier.fillMaxWidth()`, padding 16dp
- **Tray Image (kiri):**
  - Size: 64×64dp
  - Shape: `RoundedCornerShape(12.dp)`
  - Border: 2dp, color = `neubrutalBorderColor()`
  - Shadow: `neubrutalShadow(offsetX=2.dp, offsetY=2.dp, cornerRadius=12.dp)`
  - Content: `AsyncImage` dengan `ContentScale.Crop`
- **Info (tengah, weight=1):**
  - Pack name: `titleMedium`, `FontWeight.SemiBold`, color = `neubrutalOnSurface()`
  - Meta: `bodySmall`, color = `neubrutalMutedOnSurface()`, format: "{N} stickers"
- **Chevron (kanan):**
  - Icon: `Icons.AutoMirrored.Filled.KeyboardArrowRight`
  - Tint: `neubrutalMutedOnSurface()`
- **Card wrapper:**
  - Shape: `RoundedCornerShape(16.dp)`
  - Background: `neubrutalCardSurface()`
  - Border: 2dp, color = `neubrutalBorderColor()`
  - Shadow: `neubrutalShadow(offsetX=4.dp, offsetY=4.dp, cornerRadius=16.dp)`

**Click behavior:** Seluruh card clickable, navigasi ke pack detail.

### 3.2 `NeubrutalSearchBar`

**Purpose:** Search input dengan styling Neubrutal, reusable di seluruh app.

**Design Spec:**
- **Root:** `Row` dengan `Modifier.fillMaxWidth()`, padding horizontal 16dp vertical 14dp
- **Background:** `neubrutalCardSurface()`
- **Shape:** `RoundedCornerShape(12.dp)`
- **Border:** 2dp, color = `neubrutalBorderColor()`
- **Shadow:** `neubrutalShadow(offsetX=2.dp, offsetY=2.dp, cornerRadius=12.dp)`
- **Leading icon:** Search icon, tint = `neubrutalMutedOnSurface()`
- **Input:** `BasicTextField`
  - Text color: `neubrutalOnSurface()`
  - Placeholder color: `neubrutalSubtleOnSurface()`
  - Single line
- **Trailing icon:** Clear (X) icon, muncul jika text tidak kosong. Click menghapus query.
- **Focus behavior:** Border berubah ke `MaterialTheme.colorScheme.primary` saat focused (optional, can be v2)

**Props:**
```kotlin
@Composable
fun NeubrutalSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search...",
    modifier: Modifier = Modifier
)
```

### 3.3 `SortBottomSheet`

**Purpose:** Bottom sheet untuk memilih urutan sorting.

**Design Spec:**
- Menggunakan Material3 `ModalBottomSheet`
- Title: "Sort by" dengan `titleMedium`, bold
- List of options menggunakan `Column` dengan `verticalArrangement = spacedBy(4.dp)`
- Each option:
  - `Row` dengan padding 16dp vertical 12dp
  - Label: `bodyLarge`, color = `neubrutalOnSurface()`
  - Check icon di kanan jika selected, color = `MaterialTheme.colorScheme.primary`
  - Background selected: `MaterialTheme.colorScheme.primaryContainer` dengan `RoundedCornerShape(12.dp)`
- Sheet background: `neubrutalCardSurface()`

**Props:**
```kotlin
@Composable
fun SortBottomSheet(
    currentSort: SortOrder,
    onSortSelected: (SortOrder) -> Unit,
    onDismiss: () -> Unit
)
```

---

## 4. Screen Layout (`HomeScreen`)

```
┌─────────────────────────────────────┐
│  ←  My Stickers            [⇅]     │  ← AppTopBar + Sort action
├─────────────────────────────────────┤
│                                     │
│  [🔍  Search packs...          ]    │  ← NeubrutalSearchBar
│                                     │
│  ┌───────────────────────────────┐  │
│  │ [🖼]  Funny Cats          >   │  │
│  │       12 stickers             │  │
│  └───────────────────────────────┘  │
│  ┌───────────────────────────────┐  │
│  │ [🖼]  Dogs                 >   │  │
│  │       5 stickers              │  │
│  └───────────────────────────────┘  │
│              ...                     │
└─────────────────────────────────────┘
         [HomeBottomBar]
```

### Layout Details
- **TopBar:** `AppTopBar` ditambah action icon sort (⇅) di kanan
- **SearchBar:** Padding horizontal 20dp, top 12dp, bottom 8dp
- **LazyColumn:**
  - `verticalArrangement = Arrangement.spacedBy(12.dp)`
  - `contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)`
- **Empty search state:**
  - Custom empty state: Icon search + "No packs found" + "Try a different search term"
- **Empty list state (no packs at all):** Existing `EmptyState` component

---

## 5. Dark / Light Mode

All new components use theme-aware helpers from `NeubrutalColors.kt`:
| Element | Light Mode | Dark Mode |
|---|---|---|
| Card background | `surface` (white) | `surface` (#242424) |
| Border | `NeubrutalBlack` | `NeubrutalWhite` |
| Text primary | `onSurface` (dark) | `onSurface` (white) |
| Text muted | `onSurface` @ 70% | `onSurface` @ 70% |
| Shadow | `NeubrutalBlack` | Black @ 85% opacity |
| Selected sort | `primaryContainer` (coral light) | `primaryContainer` (coral @ 30%) |

No hardcoded colors. All automatic via `MaterialTheme.colorScheme` and Neubrutal helper functions.

---

## 6. Edge Cases & Error Handling

| Condition | Handling |
|---|---|
| Search query empty | Show all packs sorted by current `sortOrder` |
| Search no match | Custom empty state: search icon + "No packs found" + "Try a different search term" |
| No packs at all (initial) | Existing `EmptyState`: "No stickers yet" |
| SortOrder changed while searching | Maintain current search query, re-sort filtered results |
| Rapid search typing | De-bounce 300ms in ViewModel (optional v2) |
| Long pack names | Ellipsis, max 1 line |

---

## 7. Files to Modify / Create

### Modify
| File | Changes |
|---|---|
| `HomeState.kt` | Add `searchQuery`, `sortOrder`, `filteredPacks` computed property |
| `HomeIntent.kt` | Add `SearchQueryChanged`, `SortOrderChanged` |
| `HomeViewModel.kt` | Handle new intents, expose filtered state |
| `HomeScreen.kt` | Replace grid with list, add search bar, add sort action, use `filteredPacks` |

### Create
| File | Purpose |
|---|---|
| `presentation/components/StickerPackListCard.kt` | Horizontal card item for list |
| `presentation/components/NeubrutalSearchBar.kt` | Reusable search input |
| `presentation/components/SortBottomSheet.kt` | Sort selection bottom sheet |
| `domain/model/SortOrder.kt` | SortOrder enum with comparator |

---

## 8. Migration Notes

- `StickerPackCard` (old grid card) **tidak dihapus** — tetap ada untuk backward compatibility dan screen lain yang mungkin membutuhkannya (misal: related packs di PackDetail)
- `LazyVerticalGrid` diganti `LazyColumn` hanya di `HomeScreen`
- `HomeScreenRoot` tidak perlu berubah (cocok dengan MVI pattern yang sudah ada)

---

## 9. Future Considerations

- Search debounce (300ms) dapat ditambahkan jika dataset besar
- `SortOrder.NEWEST`/`OLDEST` dapat diperbarui ke timestamp nyata jika `StickerPack` menambahkan field `createdAt`
- `NeubrutalSearchBar` dapat digunakan di screen lain (PackDetail, Editor, dll)
