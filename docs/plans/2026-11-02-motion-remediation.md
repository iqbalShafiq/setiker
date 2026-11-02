# Setiker — Motion remediation

**Tanggal:** 2 November 2026
**Mengacu pada:** `2026-10-30-ui-motion-inventory.md`
**Status:** implemented, verified on a physical device (Xiaomi 2207117BPG, Android 14).

## Yang ditutup

| Temuan | Perubahan |
|---|---|
| **M1** ripple pada permukaan tap | `StickerCard`, `StickerPackListCard`, `DraftJobListCard`, `ProcessingPackListCard`, `DecorationActionChip`, `StylePresetChip`, `ReorderIconButton`, `NeubrutalAddTagPill`, ikon kuota AI, ikon mata password, dan tombol hapus dekorasi sekarang memakai `rememberPressFeedback` + `pressScale` + `pressShadow`. |
| **M2** `neubrutalPressable` bouncy | Helper lama dihapus. Snackbar memakai `PressSpring` yang non-bouncy. |
| **M3** Home hard cut | Fase loading/error/empty/content dibungkus `AnimatedContent` + `ContentStateAnimations.listLoad()`. Loading memakai skeleton + brand dots. |
| **M4** selection hard cut | Chip, style preset, sticker grid, border dekorasi, dan baris sheet meng-animasikan warna/ukuran dengan `EaseStandard` 140 ms. |
| **M5** bottom bar size animation | `AnimatedContent` memakai `ContentStateAnimations.fadeOnly()` (`noSizeAnimation`). |
| **M6** sheet/dialog/overlay | Semua sheet memakai `rememberAppSheetState()` (`skipPartiallyExpanded = true`). Isi sheet punya reveal + stagger. `AppDialog` mendapat fade/scale. `OfflineBanner` mendapat enter/exit. |
| **M7** reduced motion | `rememberReduceMotion()` (Android: animator duration scale; iOS: `UIAccessibilityIsReduceMotionEnabled`). Loader, shimmer, skeleton, progress indeterminate, dan autoplay preview berhenti saat aktif. |
| **M8** haptic | `rememberAppHaptics()` dipakai pada selection, long-press, save/publish, like/follow, delete dekorasi, dan AI job selesai. |
| **M9** iOS splash | `IosAppEntryPoint` menampilkan splash bermerek + crossfade yang sama dengan Android. |
| **M10** celebration | Celebration dipakai saat save pack, publish, save animated sticker, dan AI job selesai. |
| **M11** token drift | Semua durasi/curve/skala tekan memakai `SetikerMotion`; `EaseInOut` yang tidak terpakai dihapus; literal durasi di Home, document reveal, progress dialog, loader, dan shimmer dihilangkan. |
| **M12** dua `NeubrutalIconButton` | Satu implementasi surface (`NeubrutalIconButton`), satu bare (`NeubrutalBareIconButton`), dan satu surface internal yang dipakai bottom bar. Dead code dihapus. |
| **M13** dua bahasa loading | Semua `CircularProgressIndicator`/`LinearProgressIndicator` diganti brand dots atau `NeubrutalProgress`. |

## Peluang yang ditambahkan

| # | Perubahan |
|---|---|
| **O1** | Like/save di Explore dan like/follow di Public Pack Detail memakai pop + color animation + haptic. Follow FAB juga morph. |
| **O2** | `animateItem()` dipakai di Home, Explore, Pack Detail, Public Pack Detail, Creator Profile, History, Purchase History, Blocked Users, dan AI Jobs. |
| **O3** | Item yang di-drag di Pack Detail naik ke scale 1.06. |
| **O4** | Sama dengan M10. |
| **O5** | `SkeletonPackList` dipakai di Home, Explore, dan Processing History; satu shimmer animation untuk seluruh list. |
| **O6** | `BottomSheetScrollColumn` reveal dan `BottomSheetOptionRow` stagger 40 ms per baris. |
| **O7** | `NeubrutalProgress` menampilkan persentase yang ikut beranimasi; dipakai di AI job, processing card, progress dialog, video pack, dan grid split. |

## Phase 2 — de-Materialize the remaining chrome

Audit lanjutan menemukan state/kontrol yang masih terasa seperti aplikasi Material biasa. Semua
sudah diganti:

| Area | Pengganti |
|---|---|
| Pull-to-refresh Explore/Notifications | `NeubrutalPullToRefreshBox`: pil hard-shadow dengan brand dots; konten bergeser agar indikator duduk di celah, bukan menimpa search bar. |
| Dialog import/error Public Pack | `AppDialog` neubrutal, bukan `AlertDialog` + `TextButton`. |
| Slider/RangeSlider di editor, crop, video, dekorasi | `NeubrutalSlider`/`NeubrutalRangeSlider` dengan track hard-shadow dan thumb bordered; gesture/aksesibilitas tetap dari Material. |
| FilterChip di editor/AI/video/dekorasi | `NeubrutalSelectableChip`/`NeubrutalFilterChip`. |
| Checkbox Legal | `NeubrutalCheckbox` + row `toggleable`. |
| Tab emoji | Baris `NeubrutalSelectableChip` yang scrollable. |
| TextButton/OutlinedButton/Button | `AppTextLink`, `AppPrimaryButton`, `AppSecondaryButton`. |
| Card Sync | `NeubrutalCard`. |
| Chrome bottom sheet | `AppModalBottomSheet`: shape 24 dp, handle hitam dengan inti coral, surface/inset seragam. |
| Drag reorder | Spring displacement non-bouncy, lift shadow 8 dp, scale 1.06, haptic mulai/selesai. |
| Drag/resize dekorasi | Lift scale 1.03 + haptic mulai. |
| Tool crop | Ikon rotate/flip/reset yang benar + press feedback + haptic. |
| Blocked state | Garis coral indeterminate tipis di `InteractionBlockedBox`. |

README sekarang memakai screenshot dan `flow.gif` baru dari build ini; `docs/images/` juga menjadi
reference visual regression yang baru.

## Phase 3 — sheet insets & form IME

- Semua sheet sekarang memakai `contentWindowInsets = WindowInsets.navigationBars` (lewat
  `AppModalBottomSheet`), jadi isi sheet dan chip prompt-preset tidak lagi tertutup system
  navigation bar.
- `AppTextField` dan `AppPasswordTextField` sekarang punya `KeyboardActions` terpusat:
  `Next` memindahkan fokus ke kontrol form berikutnya, termasuk kontrol non-teks yang focusable
  (dan menutup keyboard bila tidak ada), `Done` menutup keyboard lalu menjalankan aksi form, dan
  `Search` menutup keyboard.
- Semua form multi-field sudah diberi `ImeAction.Next` di field antara dan `ImeAction.Done` di
  field terakhir; field terakhir yang merupakan aksi utama memanggil submit/save/confirm lewat
  `onDone`. Kalau setelah field terakhir masih ada kontrol wajib non-teks, field itu memakai
  `Next` agar fokus berpindah ke kontrol tersebut (contoh: checkbox konfirmasi di form hapus akun).
- Semua text field sekarang satu baris; placeholder juga `maxLines = 1` dengan ellipsis supaya
  placeholder panjang tidak membuat field tumbuh dua baris. Override `singleLine = false` pada
  editor, report, dan legal dihapus.
- Search field Home dan `NeubrutalSearchBar` memakai `ImeAction.Search`.

## Verifikasi

- `:composeApp:testAndroidHostTest` — lulus.
- `:androidApp:testDebugUnitTest` — lulus, termasuk screenshot baru `selected_chip.png`, `progress.png`, `slider.png`, `range_slider.png`, dan `checked_checkbox.png`.
- `:androidApp:verifyRoborazziDebug` — lulus.
- `:composeApp:lint` — lulus.
- `:androidApp:connectedUiTest` — 20 test instrumented lulus.
- `MotionIntegrationTest` — 9 test lulus (loader, progress, selection, sheet row, list phase, skeleton accessibility, IME Next ke field berikutnya, IME Next ke checkbox non-teks, IME Done menjalankan aksi).
- `python tools/motion_e2e.py` — journey Home → Explore → public pack → Like → Unlike → back lulus di device.
- Verifikasi manual di device: Home list, press feedback, sort sheet + handle baru, AppDialog import, pull-to-refresh, prompt sheet di atas navigation bar, prompt satu baris, reorder drag lift (shadow 8 dp), like/follow, save pack, reduced-motion mode.
- `python tools/screenshot_tests.py` — semua layar cocok dengan reference baru.

## Catatan / batasan

- iOS tidak bisa dikompilasi di Windows; perubahan iOS (splash + reduced motion) baru diverifikasi secara statis.
- Motion sheet tetap memakai slide/scrim platform Material; chrome (shape dan handle), isi, dan state di dalamnya memakai bahasa aplikasi. Ini keputusan sadar agar tidak bertabrakan dengan gesture sistem.
- `InteractionBlockedBox` memakai garis coral tipis, bukan scrim penuh, supaya tidak menutupi indikator loading yang sudah ada di permukaan.
