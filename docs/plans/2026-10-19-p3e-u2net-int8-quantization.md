# P3e — u2net On-Device Model Quantization (Evaluation + Adoption)

**Goal:** Decide, with measured evidence, whether a smaller/quantized variant of the on-device
`u2net.onnx` background-removal model can replace the 176 MB fp32 asset without hurting sticker
quality, and adopt the winner in the Android app (asset + versioned cache key + legacy cleanup).

**Scope:** Android only (iOS has no on-device background removal yet). Runtime unchanged
(onnxruntime-android 1.31.0, CPU EP, 320×320 input, single working image ≤512, 1 intra-op thread).

**Evaluation workspace:** `%TEMP%\opencode\setiker-eval\u2net\` (venv with onnxruntime 1.31.0 —
same version as the device AAR; `prepare*.py`, `compare.py`, `per_image.py`, `eval_pctl.py`,
`maskdiff.py`, `out/` strips + summary.json). Test set: 10 real photos from the device camera roll
+ 2 device UI screenshots, 12 total.

## Variants evaluated

| Variant | How produced | Size |
|---|---|---|
| `fp32` (baseline) | repo asset `u2net.onnx` | 176.0 MB |
| `fp16` | `onnxconverter_common.float16` (keep_io_types) | 88.1 MB |
| `int8dyn` | `quantize_dynamic` (QInt8, MatMul/Gemm/Conv) | 44.2 MB |
| `int8qdq` | `quantize_static` QDQ, per-channel, MinMax calibration, opset bumped 11→13 (`axis` attr) | 44.4 MB |
| `int8pctl` | same as `int8qdq` but **Percentile** calibration | 44.4 MB |
| `u2netp` | smaller u2net-p from rembg release | 4.6 MB |

Host metrics (8 threads, 12 images, masks normalized exactly like the app: output[0] → last 320·320
values → min-max → α = min(origα, mask·255)):

| Variant | Host latency (mean) | MAE vs fp32 | IoU@0.5 vs fp32 (mean / min) |
|---|---|---|---|
| fp32 | 400 ms | — | — |
| fp16 | 399 ms | 0.0006 | 0.999 / 0.86 |
| int8dyn | 762 ms | 0.0105 | 0.855 / **0.004** |
| int8qdq | 160 ms | 0.0137 | 0.829 / **0.058** |
| int8pctl | 169 ms | — | ~0.95 photos / 0.68 worst photo |
| u2netp | 208 ms | 0.1023 | 0.458 / 0.022 |

Per-image, `int8pctl` fixed the two worst MinMax cases (`0.55→0.68`, `0.64→0.94`) and improved the
UI screenshot case (0.887→0.944); the remaining low scores are ambiguous scenes (cluttered desk) and
screenshots — where fp32 itself produces spatially noisy soft-alpha (visual strips in `out/`).

## Device measurements (Xiaomi `8TEYJVDYVOVS5DGM`, Android 15, portrait, release-like debug build)

Method: model swapped in-place via `run-as cp` into `files/models/` (app caches the asset there),
editor "Remove background" job runs the real pipeline, `elapsedMs` from
`OnDeviceImageProcessor: removeBackground done` in logcat, output PNG pulled from
`cache/on_device_processing/`.

| Model | 1st call (incl. session init) | steady call | Notes |
|---|---|---|---|
| fp32 | 7177 ms | 5875 ms | baseline |
| **int8pctl** | 2719 ms | **1597 ms** | **3.7× faster** |
| fp16 | 579 ms | 111 ms | ⚠️ **invalid** — ORT session init fails; app silently degrades to the brightness fallback (no model) |
| int8dyn | not run on device | | host latency/quality already disqualify it |
| u2netp | not run on device | | quality disqualifies it |

Device quality check, same sticker, alpha channel of the 512×512 outputs:
`fp32` vs `int8pctl` → mean α diff **0.01/255**, p95 **0**, max 4, IoU(α>127) **1.000**; visual
output identical (pulled PNGs in `device/fp32_out`, `device/pctl_out`).

Key side finding (documented, not shipped): with an unusable model file the app does **not** fail —
it falls back to `removeBrightBackgroundFallback` (threshold heuristic). The fp16 case above proves
the degradation is silent; output differed from the real mask by ~34k pixels on the test sticker.
Out of P3e scope (behavior kept as-is).

## Decision

Adopt **int8pctl** (static QDQ, per-channel, *Percentile* calibration, opset 13) as the shipped
model: **4× smaller** asset and **3.7× faster** steady-state on device, with device-level output
equivalent to fp32 on the reference sticker and ≥0.94 IoU on 9/10 real photos host-side.
Rejected: `fp16` (session init fails on Android ORT 1.31 → silent heuristic fallback),
`int8dyn` (slower than fp32 on host, min IoU 0.004), `u2netp` (quality far below usable).

Repro (workspace): `python prepare2.py && python prepare3.py && python eval_pctl.py compare.py`.

## Implementation (Android)

1. `composeApp/src/androidMain/assets/models/u2net_int8.onnx` — the adopted model (44.4 MB, LFS);
   old `u2net.onnx` removed (LFS `git rm`).
2. `AndroidOnDeviceImageProcessor`:
   - `MODEL_FILE_NAME = "u2net_int8.onnx"` — the versioned name is the cache-busting key: existing
     installs have the old copy in `files/models`, and a different asset name forces a re-copy.
   - `LEGACY_MODEL_FILE_NAMES = ["u2net.onnx"]` — deleted from `files/models` on first model need,
     reclaiming 176 MB on upgraded devices.
3. Behavior/texts unchanged; no new dependencies.

## Verification (this phase)

- `:androidApp:assembleDebug` → APK size drops ~130 MB (see `build/p3e-assemble.txt`).
- Install over existing app (keeps old cached model) → editor remove-background → new file
  `files/models/u2net_int8.onnx` present, `u2net.onnx` deleted, `elapsedMs ≈ 1.6 s`, output visual
  equal to fp32 run.
- Full gate rerun on final SHA: `compileCommonMainKotlinMetadata`, `compileIosMainKotlinMetadata`,
  `testAndroidHostTest` (180), `assembleDebug`.
