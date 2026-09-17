#!/usr/bin/env bash
# Emulator smoke test: install, launch and walk the main flows, capturing screenshots,
# UI hierarchy dumps and logcat. Fails if the app crashes, dies, or an expected control is missing.
set -u
APK="$1"
OUT="$2"
PKG=com.blackwake.game
mkdir -p "$OUT"
FAILED=0

note() { echo "$*" | tee -a "$OUT/summary.txt"; }
alive() { adb shell pidof "$PKG" > /dev/null 2>&1; }
# uiautomator's dumper trips over its own stale nodes while the game animates,
# so retry and fall back to the compressed hierarchy before giving up.
dump() {
  local out="$OUT/$1.xml" i
  for i in 1 2 3; do
    adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1
    adb exec-out cat /sdcard/ui.xml > "$out" 2>/dev/null
    grep -q "</hierarchy>" "$out" 2>/dev/null && return 0
    adb shell uiautomator dump --compressed /sdcard/ui.xml > /dev/null 2>&1
    adb exec-out cat /sdcard/ui.xml > "$out" 2>/dev/null
    grep -q "</hierarchy>" "$out" 2>/dev/null && return 0
    sleep 1
  done
  note "dump failed: $1"
  return 1
}
step() {
  if alive; then
    adb exec-out screencap -p > "$OUT/$1.png"
    dump "$1"
    note "ok: $1"
  else
    note "DEAD at: $1"
    FAILED=1
  fi
}

# Prints "x y" for the centre of the first node whose text or content-desc contains $1.
find_node() {
  local label="$1" tries=0 b
  local limit="${2:-8}"
  while [ $tries -lt $limit ]; do
    dump ui-tmp
    b=$(tr '>' '\n' < "$OUT/ui-tmp.xml" | grep -F "$label" | grep -o 'bounds="[^"]*"' | head -1 | grep -o '[0-9]\+' | tr '\n' ' ')
    if [ -n "$b" ]; then
      set -- $b
      echo "$(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 ))"
      return 0
    fi
    tries=$((tries + 1))
    sleep 1.5
  done
  return 1
}

tap() {
  local xy
  if xy=$(find_node "$1"); then
    adb shell input tap $xy
    note "tap '$1' at $xy"
  else
    note "NOT FOUND: '$1'"
    FAILED=1
  fi
}

adb install -r "$APK" || { note "install failed"; exit 1; }
# The system "Viewing full screen" confirmation would sit on top of the game and eat every tap.
adb shell settings put secure immersive_mode_confirmations confirmed
adb logcat -c
adb shell am start -W -n "$PKG/.MainActivity"
sleep 8
if xy=$(find_node "Got it" 2 2>/dev/null); then adb shell input tap $xy; sleep 1; fi

# Fails the run when an expected element is missing from the current screen.
expect() {
  if find_node "$1" 2 > /dev/null; then
    note "expect ok: '$1'"
  else
    note "EXPECT FAILED: '$1'"
    FAILED=1
  fi
}

step 01-menu
expect "BLACK WAKE"

tap "OFFICINA"; sleep 2; step 02-garage
expect "MODULI"
tap "INDIETRO"; sleep 2

tap "IL RELITTO"; sleep 2; step 03-briefing
tap "INIZIA MISSIONE"; sleep 3; step 04-run-start
expect "SCAFO"
expect "Pausa"

# Everything below must happen while the mission is still alive, so it runs first
# and with short waits: an unattended boat sinks in well under a minute.

# Leaving and returning must come back paused, not running blind or crashed.
adb shell input keyevent KEYCODE_HOME; sleep 3
adb shell am start -W -n "$PKG/.MainActivity"; sleep 3
step 05-resume-from-background
expect "SOSPENSIONE"
tap "RIPRENDI"; sleep 1

# The back key must pause instead of leaving the game.
adb shell input keyevent KEYCODE_BACK; sleep 2; step 06-back-pauses
expect "SOSPENSIONE"
tap "MAPPA TATTICA"; sleep 2; step 07-pause-map
tap "REGISTRO DI BORDO"; sleep 2; step 08-pause-log
tap "RIPRENDI"; sleep 1

# Controls: steer, then hold the dive button.
adb shell input swipe 700 700 1500 700 1200
sleep 2; step 09-run-steer
if xy=$(find_node "Immergi" 2); then
  set -- $xy
  adb shell input swipe "$1" "$2" "$1" "$2" 2500 &
  sleep 1.5
  step 10-run-dive
  wait
fi

# Let the mission play out; a death ends in the debrief, which must offer a retry.
sleep 30; step 11-run-late
if find_node "RIPROVA" 2 > /dev/null; then
  tap "RIPROVA"; sleep 3; step 12-retry
  expect "SCAFO"
fi

adb logcat -d > "$OUT/logcat.txt"
# Only the game's own crashes matter here; the uiautomator dumper crashes on its own.
if grep -A 2 "FATAL EXCEPTION" "$OUT/logcat.txt" | grep -q "Process: $PKG"; then
  note "FATAL EXCEPTION in $PKG:"
  grep -A 30 "FATAL EXCEPTION" "$OUT/logcat.txt" | head -60 | tee -a "$OUT/summary.txt"
  FAILED=1
fi
grep -E "E AndroidRuntime|W System.err|Choreographer.*Skipped" "$OUT/logcat.txt" | grep -i "blackwake\|Skipped" | head -20 >> "$OUT/summary.txt"
rm -f "$OUT/ui-tmp.xml"
note "result: $([ $FAILED -eq 0 ] && echo PASS || echo FAIL)"
exit $FAILED
