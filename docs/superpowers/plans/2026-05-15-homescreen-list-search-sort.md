# HomeScreen List Redesign with Search & Sort Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ubah HomeScreen dari grid 2 kolom ke list vertikal dengan search bar dan sort, menggunakan design system Neubrutalism yang konsisten.

**Architecture:** MVI pattern tetap — tambah state `searchQuery` dan `sortOrder`, computed property `filteredPacks`. Filtering & sorting in-memory di ViewModel. Komponen UI baru reusable.

**Tech Stack:** Kotlin Multiplatform, Jetpack Compose, Material3, MVI, Koin

---

## File Structure

### New Files
| File | Responsibility |
|---|---|
| `domain/model/SortOrder.kt` | Enum SortOrder dengan label UI dan comparator |
| `presentation/components/StickerPackListCard.kt` | Horizontal card item untuk list pack |
| `presentation/components/NeubrutalSearchBar.kt` | Reusable search input dengan styling Neubrutal |
| `presentation/components/SortBottomSheet.kt` | BottomSheet untuk memilih urutan sort |

### Modified Files
| File | Responsibility |
|---|---|
| `presentation/home/HomeState.kt` | Tambah `searchQuery`, `sortOrder`, computed `filteredPacks` |
| `presentation/home/HomeIntent.kt` | Tambah `SearchQueryChanged`, `SortOrderChanged` |
| `presentation/home/HomeViewModel.kt` | Handle intents baru, maintain filtering logic |
| `presentation/home/HomeScreen.kt` | Replace grid dengan list + search + sort action |
| `composeApp/src/commonMain/composeResources/values/strings.xml` | String resources baru untuk search/sort |

---

## Task 1: SortOrder Enum

**Files:**
- Create: `composeApp/src/commonMain/kotlin/domain/model/SortOrder.kt`

- [ ] **Step 1: Create SortOrder enum**

```kotlin
package domain.model

enum class SortOrder(val labelRes: String) {
    NAME_ASC("sort_name_asc"),
    NAME_DESC("sort_name_desc"),
    STICKER_COUNT_ASC("sort_stickers_asc"),
    STICKER_COUNT_DESC("sort_stickers_desc"),
    NEWEST("sort_newest"),
    OLDEST("sort_oldest");

    val comparator: Comparator<StickerPack>
        get() = when (this) {
            NAME_ASC -> compareBy { it.name.lowercase() }
            NAME_DESC -> compareByDescending { it.name.lowercase() }
            STICKER_COUNT_ASC -> compareBy { it.stickers.size }
            STICKER_COUNT_DESC -> compareByDescending { it.stickers.size }
            NEWEST -> compareByDescending { it.identifier }
            OLDEST -> compareBy { it.identifier }
        }
}
```

- [ ] **Step 2: Compile check**

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/domain/model/SortOrder.kt
git commit -m "feat: add SortOrder enum with comparator"
```

---

## Task 2: String Resources

**Files:**
- Modify: `composeApp/src/commonMain/composeResources/values/strings.xml`

- [ ] **Step 1: Add string resources**

Tambahkan di dalam `<resources>` (sebaiknya setelah `home_hint`):

```xml
    <string name="search_packs_placeholder">Search packs...</string>
    <string name="sort_by">Sort by</string>
    <string name="sort_name_asc">Name (A-Z)</string>
    <string name="sort_name_desc">Name (Z-A)</string>
    <string name="sort_stickers_asc">Stickers (fewest)</string>
    <string name="sort_stickers_desc">Stickers (most)</string>
    <string name="sort_newest">Newest first</string>
    <string name="sort_oldest">Oldest first</string>
    <string name="no_search_results_title">No packs found</string>
    <string name="no_search_results_desc">Try a different search term.</string>
    <string name="sort_content_description">Sort</string>
```

- [ ] **Step 2: Sync project / compile**

Run: `./gradlew :composeApp:generateComposeResClass`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/composeResources/values/strings.xml
git commit -m "feat: add search and sort string resources"
```

---

## Task 3: StickerPackListCard Component

**Files:**
- Create: `composeApp/src/commonMain/kotlin/presentation/components/StickerPackListCard.kt`

- [ ] **Step 1: Create component file**

```kotlin
package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import domain.model.StickerPack
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.stickers_with_count

@Composable
fun StickerPackListCard(
    pack: StickerPack,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val border = neubrutalBorderColor()
    val surface = neubrutalCardSurface()
    val shadow = neubrutalShadowColor()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .neubrutalShadow(
                offsetX = 4.dp,
                offsetY = 4.dp,
                cornerRadius = 16.dp,
                color = shadow
            )
            .clip(RoundedCornerShape(16.dp))
            .background(surface)
            .border(
                width = 2.dp,
                color = border,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tray image
        AsyncImage(
            model = pack.trayImageFile,
            contentDescription = pack.name,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(surface)
                .border(
                    width = 2.dp,
                    color = border,
                    shape = RoundedCornerShape(12.dp)
                )
                .neubrutalShadow(
                    offsetX = 2.dp,
                    offsetY = 2.dp,
                    cornerRadius = 12.dp,
                    color = shadow
                ),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = pack.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = neubrutalOnSurface(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(Res.string.stickers_with_count, pack.stickers.size),
                style = MaterialTheme.typography.bodySmall,
                color = neubrutalMutedOnSurface()
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Chevron
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = neubrutalMutedOnSurface()
        )
    }
}

// MARK: - Preview

@Preview
@Composable
private fun StickerPackListCardPreview() {
    val mockPack = StickerPack(
        identifier = "pack_preview",
        name = "Funny Cats",
        publisher = "CatLover",
        trayImageFile = "",
        stickers = listOf(
            domain.model.Sticker(imageFile = ""),
            domain.model.Sticker(imageFile = ""),
            domain.model.Sticker(imageFile = "")
        )
    )
    MaterialTheme {
        StickerPackListCard(pack = mockPack, onClick = {})
    }
}
```

- [ ] **Step 2: Compile check**

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/presentation/components/StickerPackListCard.kt
git commit -m "feat: add StickerPackListCard component"
```

---

## Task 4: NeubrutalSearchBar Component

**Files:**
- Create: `composeApp/src/commonMain/kotlin/presentation/components/NeubrutalSearchBar.kt`

- [ ] **Step 1: Create component file**

```kotlin
package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import presentation.theme.neubrutalSubtleOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.search_packs_placeholder

@Composable
fun NeubrutalSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = stringResource(Res.string.search_packs_placeholder),
    modifier: Modifier = Modifier
) {
    val border = neubrutalBorderColor()
    val surface = neubrutalCardSurface()
    val shadow = neubrutalShadowColor()
    val onSurface = neubrutalOnSurface()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .neubrutalShadow(
                offsetX = 2.dp,
                offsetY = 2.dp,
                cornerRadius = 12.dp,
                color = shadow
            )
            .clip(RoundedCornerShape(12.dp))
            .background(surface)
            .border(
                width = 2.dp,
                color = border,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = neubrutalSubtleOnSurface()
        )

        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            singleLine = true,
            textStyle = TextStyle(
                color = onSurface,
                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                fontWeight = FontWeight.Normal
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(onSurface),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = neubrutalSubtleOnSurface()
                    )
                }
                innerTextField()
            }
        )

        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = neubrutalSubtleOnSurface()
                )
            }
        }
    }
}

// MARK: - Preview

@Preview
@Composable
private fun NeubrutalSearchBarPreview() {
    MaterialTheme {
        NeubrutalSearchBar(
            query = "",
            onQueryChange = {}
        )
    }
}

@Preview
@Composable
private fun NeubrutalSearchBarWithTextPreview() {
    MaterialTheme {
        NeubrutalSearchBar(
            query = "Cats",
            onQueryChange = {}
        )
    }
}
```

- [ ] **Step 2: Compile check**

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/presentation/components/NeubrutalSearchBar.kt
git commit -m "feat: add NeubrutalSearchBar component"
```

---

## Task 5: SortBottomSheet Component

**Files:**
- Create: `composeApp/src/commonMain/kotlin/presentation/components/SortBottomSheet.kt`

- [ ] **Step 1: Create component file**

```kotlin
package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import domain.model.SortOrder
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalMutedOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.sort_by

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    currentSort: SortOrder,
    onSortSelected: (SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val options = SortOrder.entries

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = neubrutalCardSurface(),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(Res.string.sort_by),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = neubrutalOnSurface(),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
                options.forEach { option ->
                    val isSelected = option == currentSort
                    SortOptionItem(
                        option = option,
                        isSelected = isSelected,
                        onClick = { onSortSelected(option); onDismiss() }
                    )
                }
            }
        }
    }
}

@Composable
private fun SortOptionItem(
    option: SortOrder,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        androidx.compose.ui.graphics.Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(
                when (option) {
                    SortOrder.NAME_ASC -> Res.string.sort_name_asc
                    SortOrder.NAME_DESC -> Res.string.sort_name_desc
                    SortOrder.STICKER_COUNT_ASC -> Res.string.sort_stickers_asc
                    SortOrder.STICKER_COUNT_DESC -> Res.string.sort_stickers_desc
                    SortOrder.NEWEST -> Res.string.sort_newest
                    SortOrder.OLDEST -> Res.string.sort_oldest
                }
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = neubrutalOnSurface(),
            modifier = Modifier.weight(1f)
        )

        if (isSelected) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// MARK: - Preview

@Preview
@Composable
private fun SortBottomSheetPreview() {
    MaterialTheme {
        SortBottomSheet(
            currentSort = SortOrder.NAME_ASC,
            onSortSelected = {},
            onDismiss = {}
        )
    }
}
```

- [ ] **Step 2: Compile check**

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/presentation/components/SortBottomSheet.kt
git commit -m "feat: add SortBottomSheet component"
```

---

## Task 6: Update HomeState

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/presentation/home/HomeState.kt`

- [ ] **Step 1: Update HomeState**

```kotlin
package presentation.home

import domain.model.SortOrder
import domain.model.StickerPack
import domain.model.User

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

- [ ] **Step 2: Compile check**

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/presentation/home/HomeState.kt
git commit -m "feat: add search and sort state to HomeState"
```

---

## Task 7: Update HomeIntent

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/presentation/home/HomeIntent.kt`

- [ ] **Step 1: Add new intents**

```kotlin
package presentation.home

import domain.model.SortOrder

sealed interface HomeIntent {
    data object LoadPacks : HomeIntent
    data class DeletePack(val packId: String) : HomeIntent
    data class AddToWhatsApp(val packId: String) : HomeIntent
    data object CreateNewPack : HomeIntent
    data object NavigateToProfile : HomeIntent
    data object NavigateToSync : HomeIntent
    data object NavigateToLogin : HomeIntent
    data object RefreshSync : HomeIntent
    data class SearchQueryChanged(val query: String) : HomeIntent
    data class SortOrderChanged(val order: SortOrder) : HomeIntent
}
```

- [ ] **Step 2: Compile check**

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/presentation/home/HomeIntent.kt
git commit -m "feat: add SearchQueryChanged and SortOrderChanged intents"
```

---

## Task 8: Update HomeViewModel

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/presentation/home/HomeViewModel.kt`

- [ ] **Step 1: Update onIntent handler**

Tambahkan case baru di `onIntent`:

```kotlin
            is HomeIntent.SearchQueryChanged -> {
                _state.update { it.copy(searchQuery = intent.query) }
            }
            is HomeIntent.SortOrderChanged -> {
                _state.update { it.copy(sortOrder = intent.order) }
            }
```

Letakkan setelah `RefreshSync` case dan sebelum closing brace dari `when`.

File lengkap setelah modifikasi:

```kotlin
package presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.auth.AuthManager
import data.sync.SyncManager
import domain.model.StickerPack
import domain.model.SyncOperationStatus
import domain.repository.StickerRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import presentation.common.UiText
import presentation.common.toUiText
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_failed_add_pack
import setiker.composeapp.generated.resources.error_failed_delete_pack
import setiker.composeapp.generated.resources.error_pack_min_stickers_whatsapp
import setiker.composeapp.generated.resources.success_pack_added_whatsapp

class HomeViewModel(
    private val repository: StickerRepository,
    private val authManager: AuthManager,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        observeAuthState()
        observeSyncState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authManager.currentUser.collect { user ->
                _state.update { it.copy(currentUser = user) }
            }
        }
    }

    private fun observeSyncState() {
        viewModelScope.launch {
            syncManager.isSyncing.collect { isSyncing ->
                _state.update { it.copy(isSyncing = isSyncing) }
            }
        }
        viewModelScope.launch {
            syncManager.operationsFlow.collect { operations ->
                val pendingCount = operations.count { it.status == SyncOperationStatus.PENDING }
                _state.update { it.copy(pendingSyncCount = pendingCount) }
            }
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.LoadPacks -> loadPacks()
            is HomeIntent.DeletePack -> deletePack(intent.packId)
            is HomeIntent.AddToWhatsApp -> addToWhatsApp(intent.packId)
            is HomeIntent.CreateNewPack -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToCreatePack)
                }
            }
            is HomeIntent.NavigateToProfile -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToProfile)
                }
            }
            is HomeIntent.NavigateToSync -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToSync)
                }
            }
            is HomeIntent.NavigateToLogin -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToLogin)
                }
            }
            is HomeIntent.RefreshSync -> {
                viewModelScope.launch {
                    syncManager.sync()
                }
            }
            is HomeIntent.SearchQueryChanged -> {
                _state.update { it.copy(searchQuery = intent.query) }
            }
            is HomeIntent.SortOrderChanged -> {
                _state.update { it.copy(sortOrder = intent.order) }
            }
        }
    }

    private fun loadPacks() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val packs = repository.getAllPacks()
                _state.update { it.copy(isLoading = false, packs = packs) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun deletePack(packId: String) {
        viewModelScope.launch {
            try {
                repository.deletePack(packId)
                loadPacks()
            } catch (e: Exception) {
                _effect.send(
                    HomeEffect.ShowError(
                        e.toUiText(Res.string.error_failed_delete_pack)
                    )
                )
            }
        }
    }

    private fun addToWhatsApp(packId: String) {
        viewModelScope.launch {
            try {
                val pack = repository.getPack(packId)
                if (pack.stickers.size < StickerPack.MIN_STICKERS) {
                    _effect.send(
                        HomeEffect.ShowError(
                            UiText.StringRes(
                                Res.string.error_pack_min_stickers_whatsapp,
                                listOf(StickerPack.MIN_STICKERS)
                            )
                        )
                    )
                    return@launch
                }
                _effect.send(HomeEffect.ShowSuccess(UiText.StringRes(Res.string.success_pack_added_whatsapp)))
            } catch (e: Exception) {
                _effect.send(
                    HomeEffect.ShowError(
                        e.toUiText(Res.string.error_failed_add_pack)
                    )
                )
            }
        }
    }
}
```

- [ ] **Step 2: Compile check**

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/presentation/home/HomeViewModel.kt
git commit -m "feat: handle search and sort intents in HomeViewModel"
```

---

## Task 9: Update HomeScreen (Main UI)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/presentation/home/HomeScreen.kt`

- [ ] **Step 1: Rewrite HomeScreen**

Replace seluruh konten file dengan:

```kotlin
package presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import domain.model.Sticker
import domain.model.StickerPack
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import presentation.components.AppTopBar
import presentation.components.EmptyState
import presentation.components.HomeBottomBar
import presentation.components.LoadingIndicator
import presentation.components.NeubrutalSearchBar
import presentation.components.SortBottomSheet
import presentation.components.StickerPackListCard
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalScreenBackground
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.home_hint
import setiker.composeapp.generated.resources.my_stickers_title
import setiker.composeapp.generated.resources.no_search_results_desc
import setiker.composeapp.generated.resources.no_search_results_title
import setiker.composeapp.generated.resources.no_stickers_yet_desc
import setiker.composeapp.generated.resources.no_stickers_yet_title
import setiker.composeapp.generated.resources.sort_content_description

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    onPackClick: (String) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        onIntent(HomeIntent.LoadPacks)
    }

    var showSortSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.my_stickers_title),
                actions = {
                    IconButton(onClick = { showSortSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = stringResource(Res.string.sort_content_description),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        bottomBar = {
            HomeBottomBar(
                currentUser = state.currentUser,
                pendingSyncCount = state.pendingSyncCount,
                isSyncing = state.isSyncing,
                onProfileClick = {
                    if (state.currentUser != null) {
                        onIntent(HomeIntent.NavigateToProfile)
                    } else {
                        onIntent(HomeIntent.NavigateToLogin)
                    }
                },
                onSyncClick = {
                    if (state.currentUser != null) {
                        onIntent(HomeIntent.NavigateToSync)
                    } else {
                        onIntent(HomeIntent.NavigateToLogin)
                    }
                },
                onAddPackClick = { onIntent(HomeIntent.CreateNewPack) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = neubrutalScreenBackground()
    ) { innerPadding ->
        when {
            state.isLoading -> {
                LoadingIndicator(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            state.packs.isEmpty() -> {
                EmptyState(
                    title = stringResource(Res.string.no_stickers_yet_title),
                    description = stringResource(Res.string.no_stickers_yet_desc),
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            else -> {
                androidx.compose.foundation.layout.Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Text(
                        text = stringResource(Res.string.home_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = neubrutalMutedOnSurface(),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )

                    NeubrutalSearchBar(
                        query = state.searchQuery,
                        onQueryChange = { onIntent(HomeIntent.SearchQueryChanged(it)) },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    val packsToShow = state.filteredPacks

                    if (packsToShow.isEmpty() && state.searchQuery.isNotEmpty()) {
                        EmptyState(
                            title = stringResource(Res.string.no_search_results_title),
                            description = stringResource(Res.string.no_search_results_desc),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                top = 12.dp,
                                bottom = 20.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = packsToShow,
                                key = { it.identifier }
                            ) { pack ->
                                StickerPackListCard(
                                    pack = pack,
                                    onClick = { onPackClick(pack.identifier) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showSortSheet) {
            SortBottomSheet(
                currentSort = state.sortOrder,
                onSortSelected = { onIntent(HomeIntent.SortOrderChanged(it)) },
                onDismiss = { showSortSheet = false }
            )
        }
    }
}

// MARK: - Previews

private val mockPacks = listOf(
    StickerPack(
        identifier = "pack_1",
        name = "Funny Cats",
        publisher = "CatLover",
        trayImageFile = "",
        stickers = listOf(Sticker(imageFile = ""), Sticker(imageFile = ""), Sticker(imageFile = ""))
    ),
    StickerPack(
        identifier = "pack_2",
        name = "Dogs",
        publisher = "DogLover",
        trayImageFile = "",
        stickers = listOf(Sticker(imageFile = ""))
    ),
    StickerPack(
        identifier = "pack_3",
        name = "Reactions",
        publisher = "MemeMaster",
        trayImageFile = "",
        stickers = listOf(Sticker(imageFile = ""), Sticker(imageFile = ""))
    )
)

@Preview
@Composable
private fun HomeScreenLoadingPreview() {
    MaterialTheme {
        HomeScreen(
            state = HomeState(isLoading = true),
            onIntent = {},
            onPackClick = {}
        )
    }
}

@Preview
@Composable
private fun HomeScreenEmptyPreview() {
    MaterialTheme {
        HomeScreen(
            state = HomeState(),
            onIntent = {},
            onPackClick = {}
        )
    }
}

@Preview
@Composable
private fun HomeScreenWithPacksPreview() {
    MaterialTheme {
        HomeScreen(
            state = HomeState(packs = mockPacks),
            onIntent = {},
            onPackClick = {}
        )
    }
}

@Preview
@Composable
private fun HomeScreenSearchNoResultsPreview() {
    MaterialTheme {
        HomeScreen(
            state = HomeState(
                packs = mockPacks,
                searchQuery = "xyz"
            ),
            onIntent = {},
            onPackClick = {}
        )
    }
}
```

- [ ] **Step 2: Compile check**

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/presentation/home/HomeScreen.kt
git commit -m "feat: redesign HomeScreen as list with search and sort"
```

---

## Task 10: Final Verification

- [ ] **Step 1: Full compilation**

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL (no errors)

- [ ] **Step 2: Android build**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: iOS build check**

Run: `./gradlew :composeApp:compileKotlinIosArm64`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Lint**

Run: `./gradlew :composeApp:lint`
Expected: BUILD SUCCESSFUL (no lint errors)

- [ ] **Step 5: Final commit**

```bash
git log --oneline -5
```

Expected: Melihat 10 commit baru yang berurutan.

---

## Spec Coverage Checklist

| Spec Requirement | Task |
|---|---|
| SortOrder enum dengan 6 opsi dan comparator | Task 1 |
| String resources baru | Task 2 |
| StickerPackListCard horizontal Neubrutal card | Task 3 |
| NeubrutalSearchBar reusable | Task 4 |
| SortBottomSheet dengan selected state | Task 5 |
| HomeState dengan searchQuery, sortOrder, filteredPacks | Task 6 |
| HomeIntent dengan SearchQueryChanged, SortOrderChanged | Task 7 |
| HomeViewModel handle intents baru | Task 8 |
| HomeScreen list layout dengan search & sort | Task 9 |
| Dark/light mode otomatis | Semua komponen pakai neubrutal* helpers |
| Empty state untuk search no results | Task 9 |
| Compile & lint clean | Task 10 |

---

## Placeholder Scan

- [x] Tidak ada "TBD", "TODO", "implement later"
- [x] Semua step memiliki code block lengkap
- [x] Semua file path exact dan benar
- [x] Type names konsisten di seluruh plan
- [x] Tidak ada reference ke types/fungsi yang belum didefinisikan
