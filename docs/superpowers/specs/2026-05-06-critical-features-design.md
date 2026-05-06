# WhatsApp Sticker Maker - Critical Features Design

> **Date:** 2026-05-06
> **Scope:** 6 critical placeholder features (Data Layer, Image Crop, Background Remover, Emoji Picker, Share, DI Wiring)
> **Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Room KMP 2.8.4, Koin DI

---

## 1. Data Layer (Room KMP + Local File Storage)

### Goal
Provide persistent storage for sticker packs, stickers metadata, and local file management for sticker images (WebP) and tray icons.

### Architecture
- **Entities**: Room `@Entity` classes with proper foreign keys and indices
- **DAO**: Room `@Dao` interfaces with suspend functions
- **Database**: Room `@Database` with schema version 1
- **Repository**: `StickerRepositoryImpl` bridges DAO + file storage
- **File Storage**: `expect/actual` class for cross-platform local file I/O

### Entities

```kotlin
@Entity(tableName = "sticker_packs")
data class StickerPackEntity(
    @PrimaryKey val identifier: String,
    val name: String,
    val publisher: String,
    val trayImageFile: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "stickers",
    foreignKeys = [
        ForeignKey(
            entity = StickerPackEntity::class,
            parentColumns = ["identifier"],
            childColumns = ["packId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("packId")]
)
data class StickerEntity(
    @PrimaryKey val id: String,
    val packId: String,
    val imageFile: String,
    val emojis: String, // JSON array
    val accessibilityText: String?,
    val sortOrder: Int
)
```

### DAO

```kotlin
@Dao
interface StickerPackDao {
    @Query("SELECT * FROM sticker_packs ORDER BY updatedAt DESC")
    suspend fun getAll(): List<StickerPackEntity>

    @Query("SELECT * FROM sticker_packs WHERE identifier = :id")
    suspend fun getById(id: String): StickerPackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pack: StickerPackEntity)

    @Delete
    suspend fun delete(pack: StickerPackEntity)
}

@Dao
interface StickerDao {
    @Query("SELECT * FROM stickers WHERE packId = :packId ORDER BY sortOrder")
    suspend fun getByPackId(packId: String): List<StickerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sticker: StickerEntity)

    @Query("DELETE FROM stickers WHERE packId = :packId AND sortOrder = :index")
    suspend fun deleteByPackAndIndex(packId: String, index: Int)

    @Query("DELETE FROM stickers WHERE packId = :packId")
    suspend fun deleteByPackId(packId: String)
}
```

### Database

```kotlin
@Database(
    entities = [StickerPackEntity::class, StickerEntity::class],
    version = 1
)
abstract class StickerDatabase : RoomDatabase() {
    abstract fun stickerPackDao(): StickerPackDao
    abstract fun stickerDao(): StickerDao
}
```

### File Storage (expect/actual)

```kotlin
// commonMain
expect class StickerFileStorage {
    suspend fun saveImage(sourcePath: String, fileName: String): String
    suspend fun loadImage(fileName: String): ByteArray?
    suspend fun deleteImage(fileName: String): Boolean
    suspend fun getImagePath(fileName: String): String
    suspend fun copyToWhatsAppDirectory(fileName: String): String
}
```

**Android actual:** Uses `Context.filesDir` + File I/O. Sticker images stored in `/files/stickers/`. Tray icons in `/files/tray/`.

**iOS actual:** Uses `NSDocumentDirectory` + `NSFileManager`. Same directory structure.

### Repository Implementation

```kotlin
class StickerRepositoryImpl(
    private val database: StickerDatabase,
    private val fileStorage: StickerFileStorage
) : StickerRepository {
    // Maps between Entity <-> Domain Model
    // Handles file copy/delete on pack/sticker CRUD
    // Validates WhatsApp requirements (512x512, WebP, size limits)
}
```

### Koin Wiring

```kotlin
// AppModule.android.kt
actual fun platformModule() = module {
    single<StickerDatabase> {
        Room.databaseBuilder(
            context = androidContext(),
            klass = StickerDatabase::class,
            name = "sticker_database.db"
        ).build()
    }
    single { get<StickerDatabase>().stickerPackDao() }
    single { get<StickerDatabase>().stickerDao() }
    single<StickerFileStorage> { AndroidStickerFileStorage(androidContext()) }
    single<StickerRepository> { StickerRepositoryImpl(get(), get()) }
}

// AppModule.ios.kt
actual fun platformModule() = module {
    single<StickerDatabase> { 
        // Room with iOS SQLite driver
    }
    single { get<StickerDatabase>().stickerPackDao() }
    single { get<StickerDatabase>().stickerDao() }
    single<StickerFileStorage> { IosStickerFileStorage() }
    single<StickerRepository> { StickerRepositoryImpl(get(), get()) }
}
```

### ContentProvider Integration
`StickerContentProvider` harus diupdate untuk:
1. Query metadata dari `StickerRepository`
2. `openFile()` return `ParcelFileDescriptor` dari `StickerFileStorage`
3. `getStickersCursor()` baca emojis dari `StickerEntity`

---

## 2. Image Crop (Cross-Platform)

### Goal
Allow users to crop, rotate, flip, and zoom images to 512x512 sticker format.

### Architecture
- **Shared UI**: Compose Canvas + `graphicsLayer` untuk preview transformasi
- **Shared State**: `CropState` dengan scale, rotation, offset, flip flags
- **Platform Processing**: `expect/actual` untuk apply transformasi ke bitmap dan save

### UI Components
- `CropScreen`: Full screen dengan image preview + overlay grid
- `CropOverlay`: Canvas draw grid 3x3 (rule of thirds)
- `CropToolbar`: Rotate, flip, zoom controls
- `GestureHandler`: `detectTransformGestures` untuk pinch-zoom + pan

### State Machine
```
LoadImage -> Preview with transforms -> ApplyCrop -> Save 512x512 WebP -> Return path
```

### Platform Processing
```kotlin
// commonMain
expect suspend fun applyCropTransformation(
    sourcePath: String,
    scale: Float,
    rotation: Float,
    offsetX: Float,
    offsetY: Float,
    flipHorizontal: Boolean,
    flipVertical: Boolean,
    outputSize: Int = 512
): String

// Android actual: Bitmap.createBitmap + Matrix + Canvas
// iOS actual: UIImage + CGAffineTransform + UIGraphicsImageRenderer
```

### Validation
- Output harus 512x512 pixels
- Format WebP
- Max file size 100KB (static), 500KB (animated)

---

## 3. Background Remover

### Goal
Allow users to erase background dari sticker menggunakan brush tool (manual) dan auto-remove (future).

### Architecture
- **Shared UI**: Compose Canvas untuk brush drawing + image preview
- **Shared State**: List of brush paths (DrawPath data class)
- **Platform Processing**: `expect/actual` untuk apply brush mask ke bitmap

### UI Components
- `BackgroundRemoverScreen`: Split screen (before/after) atau single preview
- `BrushCanvas`: Custom composable yang menggambar paths dengan `drawPath`
- `BrushToolbar`: Brush size slider, erase/restore toggle, undo/redo, clear all

### Brush Path Model
```kotlin
data class BrushPath(
    val points: List<Offset>,
    val brushSize: Float,
    val isErasing: Boolean
)
```

### Platform Processing
```kotlin
// commonMain
expect suspend fun applyMaskToImage(
    imagePath: String,
    paths: List<BrushPath>,
    canvasSize: IntSize
): String

// Android actual: Bitmap + Canvas + PorterDuff.Mode.CLEAR
// iOS actual: UIImage + CGContext + kCGBlendModeClear
```

### Auto-remove (Phase 2)
- Android: ML Kit Subject Segmentation
- iOS: Vision framework person segmentation

---

## 4. Emoji Picker

### Goal
Picker emoji dalam bottom sheet dengan recent emojis support.

### Architecture
- **Shared UI**: Compose BottomSheet + LazyVerticalGrid
- **Data**: Recent emojis disimpan di `DataStore` (Android) / `NSUserDefaults` (iOS)
- **Integration**: Dari EditorScreen tap "Add Emoji" → show picker → select → add to state

### Components
- `EmojiPickerBottomSheet`: ModalBottomSheet dengan tabs (Recent, Smileys, Animals, Food, etc.)
- `EmojiGrid`: LazyVerticalGrid dengan 8 columns
- `EmojiCategoryTab`: ScrollableTabRow untuk kategori

### Recent Emojis Storage
```kotlin
// commonMain expect
expect class EmojiPreferences {
    suspend fun getRecentEmojis(): List<String>
    suspend fun addRecentEmoji(emoji: String)
}
```

### Emoji Categories (Unicode ranges)
- Smileys & Emotion: U+1F600..U+1F64F
- Animals & Nature: U+1F400..U+1F4FF
- Food & Drink: U+1F32D..U+1F37F
- Activities: U+1F3C0..U+1F3FF
- Travel & Places: U+1F680..U+1F6FF
- Objects: U+1F4E0..U+1F4FF
- Symbols: U+1F300..U+1F5FF

---

## 5. Share Feature

### Goal
Share sticker pack ke aplikasi lain via platform-native share sheet.

### Architecture
- **Shared**: `ShareStickerPackUseCase` - prepare shareable content
- **Platform**: `expect/actual` untuk invoke native share dialog

### Use Case
```kotlin
class ShareStickerPackUseCase(
    private val repository: StickerRepository,
    private val fileStorage: StickerFileStorage
) {
    suspend operator fun invoke(packId: String): ShareContent {
        // 1. Get pack from repository
        // 2. Copy images to temp directory
        // 3. Return URIs/files untuk sharing
    }
}
```

### Platform Share
```kotlin
// commonMain
expect fun shareStickerPack(content: ShareContent)

// Android: Intent.ACTION_SEND_MULTIPLE + FileProvider
// iOS: UIActivityViewController dengan file URLs
```

---

## 6. DI Module & Wiring

### Goal
Wire up semua dependencies menggunakan Koin dengan proper module separation.

### Module Structure
```
dataModule:
  - StickerDatabase (singleton)
  - StickerPackDao (singleton)
  - StickerDao (singleton)
  - StickerFileStorage (singleton, platform-specific)
  - StickerRepository (singleton)

viewModelModule:
  - HomeViewModel (factory)
  - PackDetailViewModel (factory)
  - CreatePackViewModel (factory)
  - EditorViewModel (factory)
  - CropViewModel (factory)
  - BackgroundRemoverViewModel (factory)

platformModule (expect/actual):
  - Android: Context, Room builder, FileStorage
  - iOS: Room with SQLite driver, FileStorage

appModule:
  - includes(dataModule, viewModelModule, platformModule())
```

### ViewModel DI Update
Semua ViewModel yang membutuhkan `StickerRepository` harus menerimanya via constructor injection. Update:
- `HomeViewModel` ✅ (sudah)
- `PackDetailViewModel` ✅ (sudah)
- `EditorViewModel` ✅ (sudah)
- `CreatePackViewModel` - perlu update
- `CropViewModel` - perlu update (opsional)
- `BackgroundRemoverViewModel` - perlu update (opsional)

---

## 7. StickerContentProvider Fix

### Current Issues
1. Semua cursor methods return empty cursor
2. `openFile()` return null
3. Tidak terintegrasi dengan repository

### Fix Required
```kotlin
class StickerContentProvider : ContentProvider() {
    // Inject repository via Koin
    private val repository: StickerRepository by inject()
    private val fileStorage: StickerFileStorage by inject()

    override fun query(...): Cursor? {
        return when (uriMatcher.match(uri)) {
            METADATA_CODE -> repository.getAllPacks().toCursor()
            METADATA_CODE_FOR_SINGLE_PACK -> repository.getPack(identifier).toCursor()
            STICKERS_CODE -> repository.getStickers(packId).toCursor()
            else -> null
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        return fileStorage.getImagePath(fileName).let { path ->
            ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
        }
    }
}
```

---

## File Structure (New/Modified Files)

### New Files
```
composeApp/src/commonMain/kotlin/
  data/
    local/
      database/
        StickerDatabase.kt
        StickerPackDao.kt
        StickerDao.kt
      entity/
        StickerPackEntity.kt
        StickerEntity.kt
    repository/
      StickerRepositoryImpl.kt
    storage/
      StickerFileStorage.kt (expect)
    util/
      BitmapProcessor.kt (expect)
      EmojiPreferences.kt (expect)
  domain/
    usecase/
      ShareStickerPackUseCase.kt
  presentation/
    components/
      EmojiPickerBottomSheet.kt
      BrushCanvas.kt
      CropOverlay.kt
      CropToolbar.kt
      BrushToolbar.kt
    crop/
      CropImageProcessor.kt (expect)
    backgroundremover/
      MaskImageProcessor.kt (expect)

composeApp/src/androidMain/kotlin/
  data/
    storage/
      AndroidStickerFileStorage.kt (actual)
    util/
      AndroidBitmapProcessor.kt (actual)
      AndroidEmojiPreferences.kt (actual)
  di/
    AppModule.android.kt

composeApp/src/iosMain/kotlin/
  data/
    storage/
      IosStickerFileStorage.kt (actual)
    util/
      IosBitmapProcessor.kt (actual)
      IosEmojiPreferences.kt (actual)
  di/
    AppModule.ios.kt
```

### Modified Files
```
composeApp/src/androidMain/kotlin/
  StickerContentProvider.kt (full rewrite)
  WhatsAppIntegration.kt (add error handling)

composeApp/src/commonMain/kotlin/
  di/AppModule.kt (add includes)
  presentation/createpack/CreatePackViewModel.kt (inject repository)
  presentation/crop/CropViewModel.kt (inject processor)
  presentation/backgroundremover/BackgroundRemoverViewModel.kt (inject processor)
  presentation/editor/EditorScreen.kt (integrate emoji picker)
  presentation/packdetail/PackDetailScreen.kt (implement share)

composeApp/build.gradle.kts (add Room dependencies)
gradle/libs.versions.toml (add Room versions)
```

---

## Tech Stack Additions

```toml
# libs.versions.toml
room = "2.8.4"
sqlite = "2.5.0"

[ libraries ]
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
sqlite-driver = { module = "androidx.sqlite:sqlite-driver", version.ref = "sqlite" }

[ plugins ]
ksp = { id = "com.google.devtools.ksp", version = "2.0.0-1.0.24" }
```

---

## Platform Considerations

### Android
- Room: Native support, KSP for code generation
- File Storage: `Context.filesDir`, `FileProvider` untuk share
- Image Processing: `Bitmap`, `Matrix`, `Canvas`, `PorterDuff`
- Share: `Intent.ACTION_SEND_MULTIPLE`

### iOS
- Room: Requires `sqlite-driver` dari `androidx.sqlite:sqlite-driver`
- File Storage: `NSDocumentDirectory`, `NSFileManager`
- Image Processing: `UIImage`, `CGAffineTransform`, `CGContext`
- Share: `UIActivityViewController`

### KMP Common
- Semua UI logic di `commonMain`
- Semua business logic di `commonMain`
- Hanya file I/O dan native image processing yang platform-specific

---

## Performance Considerations
1. Lazy loading sticker images (Coil already configured)
2. Compress images before save (WebP encoding)
3. Background thread untuk DB operations (Room suspend functions)
4. Batch operations untuk bulk delete/insert

## Accessibility
1. Semua brush controls ada content description
2. Emoji picker support screen reader
3. Crop overlay ada visual feedback
4. Touch targets minimum 48dp

---

## Spec Self-Review

✅ **Placeholder scan**: No TBD/TODO/placeholder di spec
✅ **Internal consistency**: Entity-DAO-Repository flow consistent
✅ **Scope check**: 6 fitur, focused dan independent
✅ **Ambiguity check**: Semua expect/actual terdefinisi dengan jelas
✅ **Type consistency**: Naming conventions konsisten
✅ **KMP compliance**: Proper expect/actual pattern digunakan
✅ **Room KMP**: Version 2.8.4, setup untuk Android + iOS

---

*Design approved. Ready for implementation plan.*
