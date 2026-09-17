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
dump() {
  adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1
  adb exec-out cat /sdcard/ui.xml > "$OUT/$1.xml"
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
  while [ $tries -lt 8 ]; do
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
adb logcat -c
adb shell am start -W -n "$PKG/.MainActivity"
sleep 8

step 01-menu
tap "OFFICINA"; sleep 2; step 02-garage
tap "INDIETRO"; sleep 2
tap "IL RELITTO"; sleep 2; step 03-briefing
tap "INIZIA MISSIONE"; sleep 3; step 04-run-start
adb shell input swipe 1900 800 1950 800 1200
sleep 4; step 05-run-steer
if xy=$(find_node "Immergi"); then
  set -- $xy
  adb shell input swipe "$1" "$2" "$1" "$2" 2500 &
  sleep 1.2
  step 06-run-dive
  wait
fi
sleep 8; step 07-run-late
tap "Pausa"; sleep 2; step 08-pause
tap "REGISTRO DI BORDO"; sleep 2; step 09-pause-log
tap "RIPRENDI"; sleep 2
adb shell input keyevent KEYCODE_BACK; sleep 2; step 10-back-pauses
tap "RIPRENDI"; sleep 1
adb shell input keyevent KEYCODE_HOME; sleep 3
adb shell am start -W -n "$PKG/.MainActivity"; sleep 3
step 11-resume-from-background
tap "RIPRENDI"; sleep 25; step 12-run-long

adb logcat -d > "$OUT/logcat.txt"
if grep -q "FATAL EXCEPTION" "$OUT/logcat.txt"; then
  note "FATAL EXCEPTION found in logcat:"
  grep -A 30 "FATAL EXCEPTION" "$OUT/logcat.txt" | head -60 | tee -a "$OUT/summary.txt"
  FAILED=1
fi
grep -E "E AndroidRuntime|W System.err|Choreographer.*Skipped" "$OUT/logcat.txt" | grep -i "blackwake\|Skipped" | head -20 >> "$OUT/summary.txt"
rm -f "$OUT/ui-tmp.xml"
note "result: $([ $FAILED -eq 0 ] && echo PASS || echo FAIL)"
exit $FAILED
