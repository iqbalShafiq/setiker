#!/usr/bin/env python3
"""End-to-end smoke journey for the motion surfaces on a connected device.

The journey exercises the real application (Koin, API, navigation, Compose tree):

1. launch the app and reach Home;
2. open Explore;
3. open the first public pack;
4. like it and wait for the "Unlike" state;
5. restore the like and go back.

It uses the installed `android` CLI for the layout tree and `adb` for input, so it does not need
an instrumented test APK. Run it while the device is signed in and the API is reachable:

    python tools/motion_e2e.py

Screenshots from each step are written to `build/motion-e2e/`.
"""

from __future__ import annotations

import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import time
from pathlib import Path

PACKAGE = "com.setiker.app"
ACTIVITY = f"{PACKAGE}/.MainActivity"
OUT_DIR = Path("build/motion-e2e")


def repo_root() -> Path:
    return Path(__file__).resolve().parent.parent


def sdk_adb() -> str:
    local = repo_root() / "local.properties"
    if local.exists():
        for line in local.read_text(encoding="utf-8").splitlines():
            if line.startswith("sdk.dir="):
                binary = "adb.exe" if os.name == "nt" else "adb"
                sdk = line.split("=", 1)[1].replace("\\\\", "\\").replace("\\:", ":")
                return str(Path(sdk) / "platform-tools" / binary)
    found = shutil.which("adb")
    if found:
        return found
    raise SystemExit("adb was not found; set ADB or sdk.dir in local.properties")


ADB = os.environ.get("ADB", sdk_adb())


def run(args: list[str], check: bool = True) -> str:
    result = subprocess.run(args, capture_output=True, text=True)
    if check and result.returncode != 0:
        raise RuntimeError(f"{' '.join(args)} failed:\n{result.stdout}\n{result.stderr}")
    return result.stdout


def adb(*args: str, check: bool = True) -> str:
    return run([ADB, "-s", SERIAL, *args], check=check)


def connected_serial() -> str:
    output = run([ADB, "devices"])
    for line in output.splitlines()[1:]:
        if line.endswith("\tdevice"):
            return line.split("\t")[0]
    raise SystemExit("No device is connected.")


def layout() -> list[dict]:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    path = Path(tempfile.gettempdir()) / "setiker-motion-e2e-layout.json"
    subprocess.run(
        ["android", "layout", "--device", SERIAL, "--pretty", "--output", str(path)],
        check=True,
        capture_output=True,
        text=True,
    )
    windows = json.loads(path.read_text(encoding="utf-8"))
    application = next((window for window in windows if window.get("type") == "APPLICATION"), None)
    if application is None:
        return []
    return list(flatten(application.get("content", [])))


def flatten(nodes: list[dict]):
    for node in nodes:
        yield node
        yield from flatten(node.get("children", []))


def find(name: str, key: str = "text") -> dict | None:
    for node in layout():
        if node.get(key) == name:
            return node
    return None


def wait_for(name: str, key: str = "text", timeout_s: float = 12.0) -> dict:
    deadline = time.time() + timeout_s
    while time.time() < deadline:
        node = find(name, key)
        if node is not None:
            return node
        time.sleep(0.35)
    raise AssertionError(f"Timed out waiting for {key}={name!r}")


def tap(node: dict) -> None:
    match = re.match(r"\[(\d+),(\d+)\]", node.get("center", ""))
    if not match:
        raise AssertionError(f"Node has no center: {node}")
    x, y = match.groups()
    adb("shell", "input", "tap", x, y)
    time.sleep(0.6)


def screenshot(name: str) -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    target = OUT_DIR / f"{name}.png"
    subprocess.run(
        ["android", "screen", "capture", "--device", SERIAL, "--output", str(target)],
        check=True,
        capture_output=True,
        text=True,
    )


def skip_onboarding_if_needed() -> None:
    for label in ("Skip", "Next"):
        node = find(label)
        if node is not None:
            tap(node)
            time.sleep(0.4)
            return


def main() -> int:
    global SERIAL
    SERIAL = os.environ.get("ANDROID_SERIAL", "") or connected_serial()
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    adb("shell", "am", "force-stop", PACKAGE, check=False)
    adb("shell", "am", "start", "-n", ACTIVITY)
    time.sleep(1.5)
    skip_onboarding_if_needed()

    if find("Sign in") is not None:
        raise AssertionError(
            "The device is signed out. Sign in once, then run the journey again."
        )

    wait_for("My Stickers", timeout_s=20)
    screenshot("01-home")

    tap(wait_for("Explore", key="content-desc"))
    wait_for("Explore Packs")
    screenshot("02-explore")

    pack = (
        find("Local Gen Test", key="content-desc")
        or find("Local Gen Test")
        or find("Local test", key="content-desc")
    )
    if pack is None:
        raise AssertionError("No public pack was found in Explore.")
    tap(pack)
    wait_for("Stickers", timeout_s=20)
    screenshot("03-public-pack")

    # The journey is idempotent: if a previous run left the pack liked, clear it first.
    if find("Like", key="content-desc") is None and find("Unlike", key="content-desc") is not None:
        tap(find("Unlike", key="content-desc"))
        wait_for("Like", key="content-desc")

    tap(wait_for("Like", key="content-desc"))
    wait_for("Unlike", key="content-desc")
    screenshot("04-liked")

    tap(wait_for("Unlike", key="content-desc"))
    adb("shell", "input", "keyevent", "4")
    time.sleep(0.8)
    adb("shell", "input", "keyevent", "4")
    time.sleep(0.8)

    print(f"Motion E2E passed. Screenshots: {OUT_DIR.resolve()}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as error:  # noqa: BLE001 - the script is a test harness
        print(f"Motion E2E failed: {error}", file=sys.stderr)
        raise SystemExit(1)
