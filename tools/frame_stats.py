"""
Measure the frame times of the app on the device.

The numbers are relative: the phone this runs on is often under memory pressure, so the absolute
milliseconds mean less than the comparison between two runs of the same build.

Usage:

    python tools/frame_stats.py
    python tools/frame_stats.py --scrolls 6
"""
import argparse
import re
import subprocess
import sys
import time

ADB = r"C:\Users\iqbal.shafiq\AppData\Local\Android\Sdk\platform-tools\adb.exe"
PACKAGE = "com.setiker.app"
ACTIVITY = f"{PACKAGE}/com.setiker.app.MainActivity"


def adb(*args):
    return subprocess.run(
        [ADB, *args], capture_output=True, text=True, encoding="utf-8", errors="replace"
    ).stdout


def frame_stats():
    raw = adb("shell", "dumpsys", "gfxinfo", PACKAGE)
    stats = {}
    for key, pattern in {
        "total": r"Total frames rendered:\s+(\d+)",
        "janky": r"Janky frames:\s+(\d+)",
        "p50": r"50th percentile:\s+(\d+)ms",
        "p90": r"90th percentile:\s+(\d+)ms",
        "p95": r"95th percentile:\s+(\d+)ms",
        "p99": r"99th percentile:\s+(\d+)ms",
        "slowDraw": r"Number of slow issue draw commands:\s+(\d+)",
        "slowSync": r"Number of slow issue sync commands:\s+(\d+)",
        "slowIssue": r"Number of slow issue draw commands:\s+(\d+)",
    }.items():
        match = re.search(pattern, raw)
        stats[key] = int(match.group(1)) if match else 0
    return stats


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--scrolls", type=int, default=5)
    args = parser.parse_args()

    adb("shell", "am", "force-stop", PACKAGE)
    time.sleep(1.0)
    adb("shell", "dumpsys", "gfxinfo", PACKAGE, "reset")
    adb("shell", "am", "start", ACTIVITY)
    time.sleep(4.0)

    # Open the first pack, which is the screen with the sticker grid.
    adb("shell", "input", "tap", "540", "516")
    time.sleep(3.0)

    for _ in range(args.scrolls):
        adb("shell", "input", "swipe", "540", "1400", "540", "700", "260")
        time.sleep(0.9)
        adb("shell", "input", "swipe", "540", "700", "540", "1400", "260")
        time.sleep(0.9)

    stats = frame_stats()
    frames = stats["total"]
    janky = stats["janky"]
    share = (janky / frames * 100) if frames else 0

    print(f"frames: {frames}")
    print(f"janky:  {janky} ({share:.1f} %)")
    print(f"p50: {stats['p50']} ms   p90: {stats['p90']} ms   p95: {stats['p95']} ms   p99: {stats['p99']} ms")
    print(f"slow draw commands: {stats['slowDraw']}   slow sync commands: {stats['slowSync']}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
