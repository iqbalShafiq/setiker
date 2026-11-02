# Setiker — Inventaris Motion, Micro-interaction & Transition

**Tanggal:** 30 Oktober 2026
**Commit:** `ccdafa8`
**Ruang lingkup:** seluruh `composeApp/src` (commonMain + androidMain + iosMain), `androidApp`, dan shell iOS.
**Sifat:** read-only. Tidak ada kode yang diubah. Tidak ada build/perangkat yang dijalankan.
**Melanjutkan:** `docs/plans/2026-10-19-ui-ux-motion-audit.md` dan `2026-10-20-ui-ux-motion-remediation.md` (temuan F8, F14, F15, F16 sudah ditutup — tidak diangkat lagi di sini).

---

## 0. Cara membaca laporan ini

| Istilah | Arti |
|---|---|
| **Whimsical / delight** | Gerak yang sengaja dibuat menyenangkan, muncul jarang, dan tidak menghalangi interaksi. |
| **Micro-interaction** | Umpan balik kecil atas aksi berfrekuensi tinggi: tekan, pilih, fokus, salin. |
| **Transition** | Perpindahan antar layar atau antar fase konten (loading → isi → error). |
| **Hard cut** | Perubahan state tanpa animasi sama sekali. |
| **P1 / P2 / P3** | P1 = merusak rasa aplikasi; P2 = terasa janggal; P3 = polish/utang teknis. |

Catatan: aplikasi ini **tidak memakai Lottie/Rive**. Semua motion ditulis tangan dengan Compose
animation API. Aset ilustrasi adalah PNG statis.

---

## 1. Ringkasan eksekutif

Motion Setiker sekarang punya **fondasi yang rapi tetapi belum merata**. Nilai, kurva, dan spring
sudah punya satu rumah (`SetikerMotion`), transisi navigasi sudah bisa membalik arah dengan benar,
dan ada beberapa momen khas yang benar-benar terasa: titik-titik loader yang memantul, badge centang
yang "pop", timer snackbar yang tergambar sebagai border, serta shimmer pada gambar yang sedang
dimuat.

Masalahnya bukan "tidak ada animasi", melainkan **tiga bahasa gerak yang hidup berdampingan**:

1. **Bahasa Setiker** — scale + shadow-shrink, `PressSpring` non-bouncy, fade/slide dari token.
2. **Bahasa Material bawaan** — ripple pada kartu dan grid, spinner berputar, slider, sheet, dialog.
3. **Bahasa campuran** — `neubrutalPressable` yang masih bouncy (dan dipakai di snackbar), beberapa
   durasi literal, serta `AnimatedContent` tanpa spec di bottom bar.

Selain itu, **layar yang paling sering dilihat (Home) justru hard cut** saat berpindah dari loading
ke daftar, sementara 14 layar lain beranimasi. Selection (chip, style, sticker grid) juga masih
berpindah warna secara instan.

### Peta angka

| Metrik | Nilai | Arti |
|---|---:|---|
| Route terdaftar | 31 | 29 layar unik (sharePreview & createPack punya 2 route). |
| File yang mengimpor `compose.animation` | 27 | Motion tersebar, bukan hanya di theme. |
| Token durasi | 4 | 140 / 180 / 260 / 300 ms. |
| Token kurva | 3 | `EaseOut`, `EaseInOut` (**0 pemakaian**), `EaseStandard`. |
| Spring | 2 | `PressSpring` (non-bouncy), `DelightSpring` (bouncy, 2 pemakaian). |
| Call site press feedback | 10 | Semua lewat `rememberPressFeedback` + `pressScale`. |
| Permukaan tap tanpa press feedback | 11 | Masih ripple Material atau tanpa umpan balik. |
| Layar dengan transisi fase konten | 14 | `AnimatedContent` / `ContentLoadLayout` (+1 sheet). |
| Layar dengan fase hard cut | 9 | Termasuk Home dan AiJobs. |
| File yang memakai loader brand | 22 | Termasuk definisi `LoadingIndicator`; bahasa loading utama. |
| Titik spinner Material | 6 | Bahasa loading kedua. |
| Haptic feedback | 0 | Tidak ada getaran di seluruh aplikasi. |
| Penanganan reduced-motion | 0 | Tidak ada jalur khusus. |
| Animasi item list / shared element | 0 | Tidak ada. |

---

## 2. Fondasi motion

**File:** `composeApp/src/commonMain/kotlin/presentation/theme/SetikerMotion.kt`

| Token | Nilai | Dipakai untuk | Bukti |
|---|---|---|---|
| `DURATION_FAST_MS` | 140 ms | Press, exit cepat, fade badge | `PressFeedback.kt:51`, `NeubrutalSnackbar.kt:124`, `CelebrationBadge.kt:55` |
| `DURATION_BASE_MS` | 180 ms | Swap konten default, warna dot, focus ring | `ContentStateAnimations.kt:25`, `PageIndicator.kt:59`, `SetikerMotion.kt:70` |
| `DURATION_SLOW_MS` | 260 ms | Snackbar, crossfade splash → app | `NeubrutalSnackbar.kt:123`, `AndroidAppEntryPoint.kt:47` |
| `DURATION_SCREEN_MS` | 300 ms | Slide + fade antar layar | `NavigationTransitions.kt:19` |
| `EaseOut` | `cubic-bezier(0.23, 1, 0.32, 1)` | Masuk/keluar, navigasi, splash | 10 pemakaian |
| `EaseInOut` | `cubic-bezier(0.77, 0, 0.175, 1)` | Diniatkan untuk gerak di layar | **0 pemakaian** |
| `EaseStandard` | `cubic-bezier(0.2, 0, 0, 1)` | Warna/opacity | 3 pemakaian (dot, 2 focus ring) |
| `PressSpring` | `noBouncy` + `StiffnessMedium` | Tekan tombol, skala dot | 2 pemakaian |
| `DelightSpring` | `MediumBouncy` + `StiffnessMedium` | Splash logo, badge selamat | 2 pemakaian |

**Helper:**

| Helper | Lokasi | Fungsi |
|---|---|---|
| `rememberPressFeedback()` + `Modifier.pressScale()` | `components/PressFeedback.kt:39-62` | Sumber tunggal press: scale dibaca di draw phase, tanpa recompose. |
| `clickableNoRipple()` | `components/PressFeedback.kt:70-79` | Menghapus ripple Material. |
| `neubrutalPressable()` | `theme/Claymorphism.kt:324-357` | **Versi lama** yang masih bouncy dan memakai `Modifier.scale` (recompose per frame). |
| `neubrutalInteractiveCard()` | `theme/Claymorphism.kt:366-384` | **Tidak dipakai** (dead code), dan ikut mewarisi spring bouncy. |

**Catatan penting:** `EaseInOut` dan `inOut()` tidak pernah dipakai, padahal komentar di
`SetikerMotion.kt:19-22` menyatakan "movement that happens on screen uses ease-in-out". Semua
pergerakan (slide konten, navigasi) justru memakai `EaseOut`.

---

## 3. Animasi whimsical / delight

| # | Nama | Lokasi | Detail gerak | Frekuensi | Vibe |
|---|---|---|---|---|---|
| W1 | **Bouncing dots loader** | `components/LoadingIndicator.kt:79-98` | 3 dot 14 dp, scale 1 → 1.4 → 1, tween 300 ms `FastOutSlowInEasing`, stagger 150 ms, jeda 300 ms, loop selamanya | Setiap loading | Signature app; playful |
| W2 | **Shimmer gambar** | `components/NeubrutalAsyncImage.kt:100-113` | Band gradient 60% lebar, 1100 ms linear, restart; **hanya hidup saat gambar loading** | Setiap gambar remote | Halus, tidak norak |
| W3 | **Splash logo** | `splash/AppBrandedSplashScreen.kt:52-59` | Alpha 0 → 1 (180 ms), scale 0.72 → 1 dengan `DelightSpring`, caption fade menyusul | Sekali per peluncuran (Android) | First impression |
| W4 | **Crossfade splash → app** | `androidMain/.../AndroidAppEntryPoint.kt:44-50` | Crossfade 260 ms `EaseOut`, setelah splash minimal 700 ms (`SplashBranding.kt:15`) | Sekali per peluncuran | Mulus |
| W5 | **Celebration badge** | `components/CelebrationBadge.kt:46-58`, dipakai `editor/EditorScreen.kt:381` | Badge centang 56 dp: alpha 140 ms + scale `DelightSpring`, ditahan 1.2 s (`EditorScreenRoot.kt:63`) | Sekali per simpan stiker | Satu-satunya bounce untuk aksi pengguna |
| W6 | **Page dots** | `components/PageIndicator.kt:53-66` | Warna 180 ms `EaseStandard`; skala 1 ↔ 0.72 dengan `PressSpring` | Onboarding | Kecil tapi terasa |
| W7 | **Snackbar timer** | `components/NeubrutalSnackbar.kt:283-296`, `462-508` | Border tombol close "menguras" linear 10 s (normal) / 15 s (error), track tetap terlihat | Setiap snackbar | Sangat khas; jarang dipakai app lain |
| W8 | **Snackbar slide** | `components/NeubrutalSnackbar.kt:118-124`, `196-206` | Slide dari balik bottom bar sejauh tinggi kartu + 20 dp, 260 ms `EaseOut` | Setiap snackbar | Terasa seperti "push", bukan "nudge" |
| W9 | **Preview playback** | `videostickerpack/VideoStickerPackScreen.kt:599-604`; `animatededitor/AnimatedEditorViewModel.kt:283-300`; `videocrop/VideoCropViewModel.kt:152`; `videotrim/VideoTrimViewModel.kt:202` | Frame berganti sesuai durasi/fps via `delay`; bukan animasi UI, tapi gerak yang dilihat user | Saat preview | Fungsional |
| W10 | **Focus ring search** | `components/NeubrutalSearchBar.kt:62-69`; `home/HomeScreen.kt:136-143` | Border 2 dp transparan → coral, 180 ms `EaseStandard` | Setiap fokus kolom cari | Feedback yang tepat |

**Tidak ada** animasi ambient/idle di layar mana pun (tidak ada parallax, tidak ada loop dekoratif
yang jalan terus). Ini bagus untuk baterai dan performa.

---

## 4. Micro-interaction

### 4.1 Yang sudah punya feedback (bahasa Setiker)

Pola: `scale` turun + shadow mengecil (4 dp → 2 dp, atau 2 dp → 1 dp), tanpa ripple.

| Komponen | Gesture | Gerak | Nilai |
|---|---|---|---|
| `AppPrimaryButton` / `AppSecondaryButton` | Tekan | Scale + shadow 4 → 2 dp | 0.97, `PressSpring`; `AppButton.kt:44-53`, `101-111` |
| `NeubrutalIconButton` (top bar) | Tekan | Scale + shadow 2 → 1 dp | 0.92; `AppTopBar.kt:74-84` |
| `NeubrutalIconButton` (bare) | Tekan | Scale saja | 0.92; `NeubrutalIconButton.kt:37-46` |
| `PackBottomBarIconButton` | Tekan | Scale + shadow 2 → 1 dp | 0.92; `PackBottomBar.kt:141-151` |
| `PackBottomBarFab` | Tekan | Scale + shadow | 0.95; `PackBottomBar.kt:253-269` |
| `NeubrutalSelectableChip` | Tekan | Scale + shadow 4 → 2 dp | 0.97; `NeubrutalSelectableChip.kt:33-37` |
| `ProfileMenuItem` | Tekan | Scale saja | 0.98; `ProfileMenuItem.kt:53-61` |
| `BottomSheetOptionRow` | Tekan | Scale | 0.97; `BottomSheetOptionRow.kt:47-56` |
| Tombol ikon kecil (hapus emoji, clear search, dst.) | Tekan | Scale | 0.85–0.88; beberapa call site |
| `NeubrutalSnackbar` action + close | Tekan | Scale **bouncy** | 0.94 / 0.88; `NeubrutalSnackbar.kt:374`, `417` |

**Skala tekan tidak punya token**: nilainya 0.85 / 0.86 / 0.88 / 0.92 / 0.94 / 0.95 / 0.97 / 0.98,
ditulis per call site. Shadow-shrink hanya ada di 3 komponen; sisanya scale saja.

### 4.2 Yang belum punya feedback (celah)

| Komponen | Lokasi | Yang terjadi sekarang | Dampak |
|---|---|---|---|
| `StickerCard` (grid stiker) | `components/StickerCard.kt:74-80` | `clickable` + ripple | Permukaan paling sering disentuh; bahasa sentuh beda |
| `StickerPackListCard` (list Home) | `components/StickerPackListCard.kt:74` | `clickable` + ripple | Baris utama Home |
| `DraftJobListCard` | `components/DraftJobListCard.kt:82` | `clickable` + ripple | Kartu job AI |
| `ProcessingPackListCard` | `components/ProcessingPackListCard.kt:60` | `clickable` + ripple | Kartu proses |
| `DecorationActionChip` | `components/DecorationActionChip.kt:57` | `clickable` + ripple | Aksi tambah teks/emoji/gambar di editor |
| `StylePresetChip` | `components/StylePresetChip.kt:100-111` | `clickable` + ripple; border & ketebalan ganti instan | Seleksi terasa "jepret" |
| `ReorderIconButton` | `components/ReorderIconButton.kt:37-45` | `clickable` + ripple, tanpa scale | Panah reorder |
| `NeubrutalAddTagPill` | `components/StickerEmojiTagComponents.kt:115` | `clickable` + ripple | Tambah tag emoji |
| Ikon info kuota AI | `components/AiQuotaSummary.kt:129-133` | `clickable(indication = null)` — **tanpa feedback sama sekali** | Tap terasa mati |
| Ikon mata password | `components/AppPasswordTextField.kt:148-158` | `clickable(indication = null)` — **tanpa feedback** | Tap terasa mati |
| Hapus dekorasi | `components/DecorationPreviewLayer.kt:252` | `clickable` + ripple | Overlay kecil di atas artwork |

**Selection hard cut** (tidak ada animasi warna/ukuran):

| Komponen | Lokasi | Perubahan |
|---|---|---|
| `NeubrutalSelectableChip` | `NeubrutalSelectableChip.kt:49-55` | Background langsung coral ↔ surface |
| `StylePresetChip` | `StylePresetChip.kt:106-110` | Border langsung 2.5 dp hitam ↔ 3.75 dp primer |
| `SelectableStickerGrid` | `SelectableStickerGrid.kt:73-76` | Border langsung 2.5 dp ↔ 3 dp coral |
| Dekorasi terpilih | `DecorationPreviewLayer.kt:184-192` | Border coral langsung muncul/hilang |

### 4.3 Motion warisan Material (tidak di-token)

| Kontrol | Lokasi | Motion |
|---|---|---|
| `LinearProgressIndicator` | `DraftJobListCard.kt:106,121`; `ProcessingPackListCard.kt:82`; `ProgressDialog.kt:121` | Animasi progress + loop indeterminate bawaan M3 |
| `CircularProgressIndicator` | `ExploreScreen.kt:313`; `SyncScreen.kt:269`; `PurchaseHistoryScreen.kt:119`; `BlockedUsersScreen.kt:214`; `PromptPresetPickerSheet.kt:98`; `PackBottomBar.kt:295` | Spinner berputar bawaan M3 |
| `Slider` / `RangeSlider` | `AnimatedEditorScreen.kt:352`; `CropScreen.kt:204`; `VideoCropScreen.kt:205`; `VideoTrimScreen.kt:223`; `VideoStickerPackScreen.kt:324,341`; `DecorationBottomSheets.kt:231,259,482,570` | Thumb/track M3 |
| `Checkbox` | `LegalDocumentScreen.kt:214` | Centang M3 |
| `SecondaryTabRow` | `EmojiPickerBottomSheet.kt:116` | Indicator geser M3 |
| `ModalBottomSheet` | 14 file (lihat §5.4) | Slide + scrim M3, bukan `SetikerMotion` |
| `Dialog` | `AppDialog.kt:83` | Animasi window platform |

---

## 5. Transition

### 5.1 Navigasi antar layar

**File:** `navigation/NavigationTransitions.kt`, dipasang di `navigation/AppNavigation.kt:169-175`.

| Aspek | Nilai |
|---|---|
| Gaya default | `BOTTOM_UP`: layar baru masuk dari bawah (full height), layar lama keluar ke atas; keduanya + fade |
| Gaya alternatif | `TOP_DOWN`: masuk dari atas; dipakai 2 pemanggilan — `navTopDown("aiJobs")` dari top bar (`AppNavigation.kt:239`) dan `notifications` (`:394`) |
| Durasi | 300 ms `EaseOut` untuk slide dan fade |
| Pop | Gaya disimpan per back-stack entry (`saveEntryTransitionStyle`), lalu dibalik persis: `popEnter`/`popExit` |
| Pemakaian | 75 pemanggilan `navBottomUp`, 2 `navTopDown` |

**Konsekuensi:** semua layar — termasuk layar utilitas seperti Settings dan Paywall — memakai slide
vertikal penuh. Tidak ada shared-axis horizontal, tidak ada fade-through, tidak ada shared element.
Untuk aplikasi mobile ini konsisten dan aman, tetapi juga membuat setiap perpindahan terasa
"seberat" push.

### 5.2 Spesifikasi transisi konten

**File:** `common/ContentStateAnimations.kt`

| Spec | Dipakai | Detail |
|---|---|---|
| `listLoad()` | Paywall, Blocked, Purchase History, Notifications, Processing History | Masuk: fade 180 + slide `it/16`; keluar: fade 120 + slide `it/20`; konten → konten: fade saja |
| `detailReveal()` | Profile, Creator, Pack Detail, Public Pack Detail | Sama seperti `listLoad`, untuk shell layar penuh |
| `exploreBody()` | Explore | Sama, untuk body di bawah chrome |
| `formReveal()` | Edit Profile | Spinner → form: fade + slide |
| `documentReveal()` | Legal | Fade 220 ms + slide `it/24` — **durasi literal** |
| `fadeOnly()` | Sync | Fade 180/120 |
| `noSizeAnimation()` | Semua di atas | `SizeTransform` di-snap agar list tidak di-measure ulang tiap frame |

### 5.3 Layar dengan transisi fase vs hard cut

| Layar | Transisi fase | Bukti |
|---|---|---|
| Home | **Hard cut** (hanya top bar yang animasi) | `HomeScreen.kt:124-132` (top bar), `:251-328` (fase) |
| Explore | `exploreBody()` | `ExploreScreen.kt:234-239` |
| Pack Detail | `detailReveal()` | `PackDetailScreen.kt:508-515` |
| Public Pack Detail | `detailReveal()` | `PublicPackDetailScreen.kt:207-214` |
| Creator Profile | `detailReveal()` + fade empty state | `CreatorProfileScreen.kt:172-179`, `250-254` |
| Profile | `detailReveal()` | `ProfileScreen.kt:242-249` |
| Edit Profile | `formReveal()` | `EditProfileScreen.kt:72-77` |
| Legal | `documentReveal()` | `LegalDocumentScreen.kt:104-111` |
| Sync | `fadeOnly()` | `SyncScreen.kt:214-219` |
| Processing History | `listLoad()` | `ProcessingHistoryScreen.kt:187-194` |
| Paywall | `ContentLoadLayout` → `listLoad()` | `PaywallScreen.kt:111`; `ContentLoadLayout.kt:39-43` |
| Purchase History | `ContentLoadLayout` | `PurchaseHistoryScreen.kt:87` |
| Blocked Users | `ContentLoadLayout` | `BlockedUsersScreen.kt:125` |
| Notifications | `ContentLoadLayout` | `NotificationsScreen.kt:73` |
| Onboarding | Pager + dot animasi | `OnboardingScreen.kt:87-96`, `140-147` |
| **Ai Jobs** | **Hard cut** (`when` langsung) | `AiJobsScreen.kt:185-205` |
| **Create Pack** | **Hard cut** saat `isLoading` | `CreatePackScreen.kt:641-642` |
| **Share Preview** | **Hard cut** | `SharePreviewScreen.kt:78` |
| **Editor** | **Hard cut** loader → editor | `EditorScreen.kt:325` |
| **Animated Editor** | **Hard cut** loader → editor | `AnimatedEditorScreen.kt:283` |
| **Video Trim** | **Hard cut** | `VideoTrimScreen.kt:111,183` |
| **Video Crop** | **Hard cut** | `VideoCropScreen.kt:149,172` |
| **Video Sticker Pack** | **Hard cut** | `VideoStickerPackScreen.kt:393` |
| Settings | Tidak ada fase; hanya dialog | — |
| Share Links (sheet) | Fade 180/120 | `ShareLinksSheet.kt:91-100` |

### 5.4 Sheet, dialog, dan overlay

| Permukaan | Motion | Catatan |
|---|---|---|
| `ModalBottomSheet` (14 file) | Bawaan M3 | `skipPartiallyExpanded = true` di hampir semua; **`EmojiPickerBottomSheet.kt:78` memakai default (bisa partial)** |
| `AppDialog` dan dialog konfirmasi | Animasi window platform | Tidak ada token; tidak ada animasi konten |
| `NeubrutalSnackbarHost` | Slide 260 ms + timer border | Satu-satunya overlay dengan motion khas |
| `OfflineBanner` | **Muncul/hilang instan** | `OfflineBanner.kt:27-36`; tidak ada `AnimatedVisibility` |
| `InteractionBlockedBox` | Tidak ada indikator visual | Hanya memblokir pointer (`InteractionBlockedBox.kt:20-34`) |
| `ProgressDialog` | Bar 180 ms (literal) | `ProgressDialog.kt:65-69` |

---

## 6. Tabel per halaman

Kolom "Masuk" = transisi navigasi. "Di dalam" = motion khas layar. "Feedback" = micro-interaction
utama.

### 6.1 Layar utama & konten

| # | Halaman | Masuk | Di dalam | Feedback | Catatan |
|---|---|---|---|---|---|
| 1 | Splash (Android) | Sistem splash 300 ms → logo | Logo fade + spring, caption fade, crossfade 260 ms | — | Hanya Android |
| 2 | Onboarding | Slide 300 ms | Pager swipe + dots (warna 180, skala spring) | Tombol 0.97 | Motion paling "lengkap" |
| 3 | Home | Slide 300 ms | Top bar title ↔ search (180/120), focus ring 180 | FAB/top bar press; **kartu pack ripple** | Fase konten hard cut |
| 4 | Explore | Slide 300 ms | `exploreBody()`; focus ring; spinner inline | Chip press 0.86 | Error/empty ikut animasi |
| 5 | Pack Detail | Slide 300 ms | `detailReveal()`; sheet M3 | Tombol press; **grid ripple** | Reorder pakai ripple |
| 6 | Public Pack Detail | Slide 300 ms | `detailReveal()` | Like/follow **instan** | Tidak ada animasi like |
| 7 | Creator Profile | Slide 300 ms | `detailReveal()`, empty fade | Tombol press | Follow instan |
| 8 | Profile | Slide 300 ms | `detailReveal()` | Menu 0.98 | — |
| 9 | Edit Profile | Slide 300 ms | `formReveal()` | FAB press | Field tanpa focus ring |
| 10 | Settings | Slide 300 ms | Dialog M3 | Tombol press | Tidak ada fase |
| 11 | Paywall | Slide 300 ms | `ContentLoadLayout` | Tombol press | — |
| 12 | Purchase History | Slide 300 ms | `ContentLoadLayout` | Tombol press | Spinner M3 |
| 13 | Blocked Users | Slide 300 ms | `ContentLoadLayout` | Tombol press | Spinner M3 |
| 14 | Notifications | **Slide dari atas** | `ContentLoadLayout` | Tombol press | Satu dari 2 `TOP_DOWN` |
| 15 | Ai Jobs | Slide (bisa dari atas) | **Hard cut**; progress bar M3 | **Kartu ripple** | Fase tidak dianimasikan |
| 16 | Processing History | Slide 300 ms | `listLoad()` | Tombol press | — |
| 17 | Sync | Slide 300 ms | `fadeOnly()`; spinner M3 | Tombol press | — |
| 18 | Legal | Slide 300 ms | `documentReveal()` 220 ms | Checkbox M3 | Durasi literal |

### 6.2 Alur pembuatan & editor

| # | Halaman | Masuk | Di dalam | Feedback | Catatan |
|---|---|---|---|---|---|
| 19 | Create Pack | Slide 300 ms | **Hard cut** loading; sheet M3 | Chip/tombol press; **grid ripple** | Tanpa celebration saat publish |
| 20 | Editor | Slide 300 ms | **Hard cut** loader; celebration badge; drag dekorasi langsung | Tombol press; **chip ripple** | Satu-satunya delight saat simpan |
| 21 | Animated Editor | Slide 300 ms | **Hard cut** loader; playback frame; slider M3 | Tombol press | Tidak ada celebration |
| 22 | Crop | Slide 300 ms | Rotate/flip/reset **instan**; pinch/pan langsung | Tombol press | Transformasi tanpa easing |
| 23 | Video Trim | Slide 300 ms | **Hard cut**; playback; `RangeSlider` M3 | Tombol press | — |
| 24 | Video Crop | Slide 300 ms | **Hard cut**; playback; transform langsung; progress dialog | Tombol press | — |
| 25 | Video Sticker Pack | Slide 300 ms | **Hard cut**; autoplay preview loop | Tombol press; seleksi instan | — |
| 26 | Share Preview | Slide 300 ms | **Hard cut** | Tombol press | — |

### 6.3 Autentikasi

| # | Halaman | Masuk | Di dalam | Feedback | Catatan |
|---|---|---|---|---|---|
| 27 | Login | Slide 300 ms | Form statis | Tombol press; snackbar slide | Field tanpa focus ring |
| 28 | Register | Slide 300 ms | Form statis | Tombol press; snackbar slide | — |
| 29 | Forgot Password | Slide 300 ms | Form statis | Tombol press | — |
| 30 | Reset Password | Slide 300 ms | Form statis | Tombol press | — |

> Total: 30 baris = 29 layar unik + splash. Ada 31 route karena `createPack` dan `sharePreview`
> masing-masing punya dua route yang dirender layar yang sama.

---

## 7. Temuan & prioritas

| # | Sev | Kategori | Temuan | Bukti | Rekomendasi | Effort |
|---|---|---|---|---|---|---|
| **M1** | **P1** | Micro-interaction | Permukaan tap paling sering — grid stiker dan kartu pack di Home — masih memakai ripple Material tanpa scale/shadow. Dua bahasa sentuh hidup berdampingan. | `StickerCard.kt:74-80`, `StickerPackListCard.kt:74`, `DraftJobListCard.kt:82`, `ProcessingPackListCard.kt:60`, `DecorationActionChip.kt:57`, `StylePresetChip.kt:111`, `ReorderIconButton.kt:45` | Route ke `rememberPressFeedback` + `pressScale`; tambahkan shadow-shrink. Satu helper, 7 call site. | S |
| **M2** | **P1** | Cohesion | `neubrutalPressable` masih `DampingRatioMediumBouncy` dan dipakai snackbar, sementara aturan repo menyebut press non-bouncy. | `Claymorphism.kt:324-357`; dipakai `NeubrutalSnackbar.kt:374,417` | Ganti ke `SetikerMotion.PressSpring`; hapus/arahkan helper lama. | S |
| **M3** | **P1** | Transition | Home — layar yang paling sering dilihat — berpindah loading/error/empty/daftar secara hard cut, sementara 14 layar lain beranimasi. | `HomeScreen.kt:251-328` | Bungkus dengan `AnimatedContent` + `ContentStateAnimations.listLoad()` (ukuran sudah aman karena `noSizeAnimation`). | S |
| **M4** | **P2** | Micro-interaction | Selection berpindah warna/ukuran secara instan: chip, style preset, sticker grid, border dekorasi. | `NeubrutalSelectableChip.kt:49-55`, `StylePresetChip.kt:106-110`, `SelectableStickerGrid.kt:73-76` | `animateColorAsState` + `animateDpAsState` 140 ms (`EaseStandard`), konsisten dengan page dot. | S |
| **M5** | **P2** | Transition / perf | `AnimatedContent` di bottom bar tanpa `transitionSpec` → size transform default ikut aktif, persis masalah yang membuat `noSizeAnimation()` dibuat. | `PackBottomBar.kt:88-101`; alasan di `ContentStateAnimations.kt:31-39` | Pakai `ContentStateAnimations.contentTransform(...)` atau `noSizeAnimation()`. | S |
| **M6** | **P2** | Cohesion | Sheet & dialog memakai motion Material; satu sheet (`EmojiPicker`) memakai state partial sementara yang lain `skipPartiallyExpanded`. | 14 file sheet; `EmojiPickerBottomSheet.kt:78` | Standarkan state sheet; dokumentasikan motion sheet sebagai "platform default" atau bungkus dengan spec sendiri bila M3 memungkinkan. | M |
| **M7** | **P2** | Accessibility | Tidak ada jalur reduced-motion. Loop berbasis `delay` (dot loader, playback) tetap berjalan saat animasi sistem dimatikan. | `LoadingIndicator.kt:85-98`, `AnimatedEditorViewModel.kt:289-298`, `VideoStickerPackScreen.kt:600-603` | Hormati skala durasi animasi platform; matikan shimmer/loop saat reduced motion aktif. | M |
| **M8** | **P2** | Micro-interaction | Tidak ada haptic feedback di seluruh app — tidak ada konfirmasi taktil untuk simpan, seleksi, atau long-press. | 0 hasil pencarian `Haptic` di seluruh `setiker/` | Tambahkan haptic pada simpan/celebrate, long-press, dan selection; verifikasi dukungan iOS. | M |
| **M9** | **P2** | Cross-platform | iOS langsung `App()`: tidak ada splash bermerek maupun crossfade; motion pembuka hanya ada di Android. | `iosMain/MainViewController.kt:5`; `AndroidAppEntryPoint.kt:44-50` | Pindahkan entry point splash ke `commonMain` atau tambahkan padanannya di SwiftUI. | M |
| **M10** | **P3** | Delight | Celebration hanya di Editor statis. Simpan animated sticker, publish pack, dan AI job selesai tidak punya momen payoff. | `CelebrationBadge` hanya dipakai `EditorScreen.kt:381` | Pakai ulang badge/`DelightSpring` untuk 2–3 momen bernilai tinggi. | S |
| **M11** | **P3** | Token drift | 6 nilai durasi literal di 5 tempat (Home 180/120, dokumen 220, progress 180, dot 300, shimmer 1100), `EaseInOut` tidak dipakai, skala tekan 0.85–0.98 tanpa token. | `HomeScreen.kt:128-129`, `ContentStateAnimations.kt:96-97`, `ProgressDialog.kt:67`, `LoadingIndicator.kt:90` | Tambah token dokumentasi/shimmer; hapus `EaseInOut` atau pakai; satukan skala tekan. | M |
| **M12** | **P3** | Utang teknis | Dua implementasi `NeubrutalIconButton` dengan motion berbeda; `neubrutalInteractiveCard` dead code dan bouncy. | `AppTopBar.kt:66-118` vs `NeubrutalIconButton.kt:25-61`; `Claymorphism.kt:366-384` | Satukan implementasi; hapus dead code. | S |
| **M13** | **P3** | Loading language | Dua bahasa loading: bouncing dots (22 file) dan spinner Material (6 titik). | `ExploreScreen.kt:313`, `SyncScreen.kt:269`, dll. | Pilih satu untuk konteks inline; pertahankan dots untuk layar penuh. | S |

---

## 8. Peluang yang belum ada

| # | Peluang | Di mana | Kenapa terasa | Effort |
|---|---|---|---|---|
| O1 | Animasi like/follow (heart pop / tombol morph) | Public Pack Detail, Creator Profile | Aksi emosional saat ini hanya ganti ikon | S |
| O2 | `animateItem()` untuk grid & list | Home, Pack Detail, sticker grid | Item muncul/hilang tanpa konteks gerak | S |
| O3 | Lift + scale saat drag reorder | Pack Detail | Drag terasa "menempel", bukan fisik | M |
| O4 | Celebration untuk publish pack / AI job selesai | Create Pack, Ai Jobs | Payoff emosional hanya di satu jalur | S |
| O5 | Skeleton untuk list panjang | Home, Explore, History | Dot loader bagus untuk layar penuh, kurang cocok untuk list | M |
| O6 | Stagger halus saat sheet terbuka | Semua bottom sheet | Isi sheet muncul serentak | S |
| O7 | Animasi angka progress (bukan bar Material) | Draft job, Processing pack | Bar M3 keluar dari bahasa visual app | S |

---

## 9. Yang sudah bagus (jangan di-regress)

- **Satu rumah untuk nilai gerak.** Durasi, kurva, dan spring terkumpul di `SetikerMotion`, dengan
  alasan tertulis di komentar — termasuk kenapa press tidak boleh bounce.
- **Navigasi sadar arah.** Gaya push disimpan per entry, sehingga pop membalik animasi yang benar,
  bukan sekadar kebalikannya secara kebetulan.
- **Sadar performa.** `noSizeAnimation()` menghindari re-measure list tiap frame; nilai animasi
  dibaca di `graphicsLayer`/draw phase sehingga tidak memicu recompose; shimmer hanya hidup saat
  loading; tidak ada animasi idle.
- **Page dot** menggabungkan warna, skala, kontras yang cukup, dan semantics `selected`.
- **Snackbar** punya identitas: slide dari balik bottom bar, timer yang tergambar sebagai border,
  `liveRegion`, dan durasi berbeda untuk error.
- **DelightSpring** hanya dipakai di dua momen yang layak (logo splash dan simpan stiker) — sesuai
  aturan yang didokumentasikan.
- **Loader brand** dipakai luas (22 file), jadi loading bukan sekadar spinner generik.

---

## 10. Metode & batasan

- **Metode:** pembacaan statis seluruh file Kotlin yang mengimpor `androidx.compose.animation`,
  `foundation.pager`, atau `gestures`; penelusuran seluruh call site `tween`, `spring`,
  `animate*AsState`, `AnimatedContent`, `AnimatedVisibility`, `Crossfade`, `Animatable`,
  `graphicsLayer`, `clickable`, dan `ModalBottomSheet`; pembacaan dokumen audit sebelumnya untuk
  menghormati keputusan yang sudah selesai.
- **Tidak** menjalankan aplikasi, tidak mengambil screenshot, tidak mengukur frame time. Klaim
  performa mengacu pada komentar kode dan audit 2026-10-20, bukan pengukuran baru.
- **iOS** tidak dapat dikompilasi di Windows; analisis iOS murni dari kode.
- Penilaian "rasa" (mis. apakah 260 ms snackbar terlalu lambat) memerlukan feel-check di perangkat
  dengan `animator_duration_scale` 10×. Laporan ini menandai titik yang perlu diuji, bukan
  menyimpulkan rasa akhirnya.
