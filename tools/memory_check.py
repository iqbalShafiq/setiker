"""
Measure the app's memory over a few navigation cycles.

A leak shows up as a number that only ever goes up: the count of live Activities, of View
contexts, of AppContexts. The heap size alone is not a leak signal, because the garbage
collector takes it back; the object counts are.

Usage:

    python tools/memory_check.py
    python tools/memory_check.py --cycles 8
"""
import argparse
import re
import subprocess
import sys
import time

ADB = r"C:\Users\iqbal.shafiq\AppData\Local\Android\Sdk\platform-tools\adb.exe"
PACKAGE = "com.setiker.app"
ACTIVITY = f"{PACKAGE}/com.setiker.app.MainActivity"

# The main screens, with the taps that get there.
ROUTES = [
    ("home", []),
    ("pack", [(540, 516)]),
    ("editor", [(540, 516), (187, 1030)]),
    ("explore", [(105, 2160)]),
    ("profile", [(254, 2160)]),
]

FIELDS = {
    "Activities": re.compile(r"Activities:\s+(\d+)"),
    "AppContexts": re.compile(r"AppContexts:\s+(\d+)"),
    "Views": re.compile(r"Views:\s+(\d+)"),
    "ViewRoots": re.compile(r"ViewRootImpl:\s+(\d+)"),
    "PssTotal": re.compile(r"TOTAL\s+(\d+)"),
    "JavaHeap": re.compile(r"Java Heap:\s+(\d+)"),
    "NativeHeap": re.compile(r"Native Heap:\s+(\d+)"),
    "Graphics": re.compile(r"Graphics:\s+(\d+)"),
}


def adb(*args, capture=True):
    return subprocess.run(
        [ADB, *args], capture_output=capture, text=True, encoding="utf-8", errors="replace"
    ).stdout


def sample():
    raw = adb("shell", "dumpsys", "meminfo", PACKAGE)
    values = {}
    for name, pattern in FIELDS.items():
        match = pattern.search(raw)
        values[name] = int(match.group(1)) if match else 0
    return values


def navigate(name, taps):
    adb("shell", "am", "start", ACTIVITY)
    time.sleep(2.0)
    for x, y in taps:
        adb("shell", "input", "tap", str(x), str(y))
        time.sleep(1.8)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--cycles", type=int, default=4)
    args = parser.parse_args()

    adb("shell", "am", "force-stop", PACKAGE)
    time.sleep(1.0)
    adb("shell", "am", "start", ACTIVITY)
    time.sleep(4.0)

    history = []
    for cycle in range(1, args.cycles + 1):
        for name, taps in ROUTES:
            navigate(name, taps)
        values = sample()
        history.append(values)
        print(
            f"cycle {cycle}: "
            + "  ".join(f"{key}={values[key]}" for key in ("Activities", "AppContexts", "Views", "ViewRoots", "PssTotal", "Graphics"))
        )

    print()
    first, last = history[0], history[-1]
    leaked = []
    for key in ("Activities", "AppContexts", "Views", "ViewRoots"):
        if last[key] > first[key]:
            leaked.append(f"{key} {first[key]} -> {last[key]}")

    if leaked:
        print("the object counts grew over the run:")
        for line in leaked:
            print(f"  {line}")
        return 1

    print("no growth in the live object counts over the run")
    print(
        f"pss {first['PssTotal']} kB -> {last['PssTotal']} kB, "
        f"graphics {first['Graphics']} kB -> {last['Graphics']} kB"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
