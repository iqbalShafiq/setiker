"""
Visual regression for the app's key screens.

The Compose screenshot artifacts (Robolectric + Roborazzi) do not line up with the Compose 1.12
test harness yet, and the unit-test path cannot host a Compose Activity in this multiplatform
module. The device can, so the reference images are captured from a real device and compared here.

Usage (from the repository root, with a device connected):

    python tools/screenshot_tests.py            # compare against the references
    python tools/screenshot_tests.py --record   # refresh the references

The references live in docs/images/. A screen passes when at most `--tolerance` percent of its
pixels differ; the default is deliberately tight, so a real visual regression fails the run.
"""
import argparse
import os
import subprocess
import sys
import tempfile
import time

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REFERENCE_DIR = os.path.join(ROOT, "docs", "images")
ADB = os.path.join(os.path.expanduser("~"), "AppData", "Local", "Android", "Sdk", "platform-tools", "adb.exe")
PACKAGE = "com.setiker.app"
ACTIVITY = f"{PACKAGE}/com.setiker.app.MainActivity"

# name -> the taps that get the device to that screen
SCREENS = {
    "home": [],
    "pack-detail": [(540, 516)],
    "editor": [(540, 516), (187, 1030)],
    "explore": [(105, 2160)],
    "ai-jobs": [(981, 181)],
    "profile": [(254, 2160)],
}


def adb(*args, capture=False):
    result = subprocess.run(
        [ADB, "shell", *args], capture_output=capture, text=True, encoding="utf-8", errors="replace"
    )
    return result.stdout if capture else ""


def screencap(path):
    remote = "/sdcard/__shot.png"
    adb("screencap", "-p", remote)
    subprocess.run([ADB, "pull", remote, path], capture_output=True)
    adb("rm", "-f", remote)


def open_app():
    adb("am", "force-stop", PACKAGE)
    time.sleep(0.6)
    adb("am", "start", ACTIVITY)
    time.sleep(4.0)


def navigate(taps):
    for x, y in taps:
        adb("input", "tap", str(x), str(y))
        time.sleep(2.0)


def diff_percent(reference, actual):
    from PIL import Image, ImageChops

    a = Image.open(reference).convert("RGB")
    b = Image.open(actual).convert("RGB")
    if a.size != b.size:
        return 100.0
    # The system bars carry the clock and the battery, which change between runs. Mask them out.
    width, height = a.size
    top = int(height * 0.045)
    bottom = int(height * 0.055)
    a = a.crop((0, top, width, height - bottom))
    b = b.crop((0, top, width, height - bottom))
    delta = ImageChops.difference(a, b)
    changed = sum(1 for pixel in delta.getdata() if max(pixel) > 12)
    return (changed / float(a.width * a.height)) * 100.0


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--record", action="store_true", help="refresh the reference images")
    parser.add_argument("--tolerance", type=float, default=1.0, help="percent of pixels allowed to differ")
    parser.add_argument("--only", help="only check the named screen")
    args = parser.parse_args()

    if not os.path.exists(REFERENCE_DIR):
        print(f"missing reference directory: {REFERENCE_DIR}")
        return 1

    screens = {k: v for k, v in SCREENS.items() if not args.only or k == args.only}
    failures = []

    with tempfile.TemporaryDirectory() as tmp:
        for name, taps in screens.items():
            reference = os.path.join(REFERENCE_DIR, f"{name}.png")
            if not os.path.exists(reference):
                print(f"  {name:<14} no reference, skipped")
                continue

            open_app()
            navigate(taps)
            actual = os.path.join(tmp, f"{name}.png")
            screencap(actual)

            if args.record:
                subprocess.run(["copy", "/Y", actual, reference], shell=True)
                print(f"  {name:<14} reference recorded")
                continue

            percent = diff_percent(reference, actual)
            status = "ok" if percent <= args.tolerance else "CHANGED"
            print(f"  {name:<14} {percent:6.2f}% different  {status}")
            if percent > args.tolerance:
                failures.append((name, percent))

    if failures:
        print("\nvisual differences beyond the tolerance:")
        for name, percent in failures:
            print(f"  {name}: {percent:.2f}%")
        return 1

    print("\nall screens match their reference")
    return 0


if __name__ == "__main__":
    sys.exit(main())
