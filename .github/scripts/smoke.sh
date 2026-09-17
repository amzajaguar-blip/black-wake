#!/usr/bin/env bash
# Emulator smoke test: install, launch, walk menu -> briefing -> mission -> pause,
# capture screenshots, UI dumps and logcat. Fails if the app crashes or dies.
set -u
APK="$1"
OUT="$2"
PKG=com.blackwake.game
mkdir -p "$OUT"
FAILED=0

shot() { adb exec-out screencap -p > "$OUT/$1.png"; }
dump() {
  adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1
  adb exec-out cat /sdcard/ui.xml > "$OUT/$1.xml"
}
alive() { adb shell pidof "$PKG" > /dev/null 2>&1; }
note() { echo "$*" | tee -a "$OUT/summary.txt"; }

# Tap the centre of the first node whose text contains $1. Retries while the UI settles.
tap_text() {
  local label="$1" tries=0 b
  while [ $tries -lt 8 ]; do
    dump ui-tmp
    b=$(tr '>' '\n' < "$OUT/ui-tmp.xml" | grep -F "text=\"$label" | head -1 | grep -o 'bounds="[^"]*"' | grep -o '[0-9]\+' | tr '\n' ' ')
    if [ -n "$b" ]; then
      set -- $b
      local x=$(( ($1 + $3) / 2 )) y=$(( ($2 + $4) / 2 ))
      adb shell input tap "$x" "$y"
      note "tap '$label' at $x,$y"
      return 0
    fi
    tries=$((tries + 1))
    sleep 2
  done
  note "NOT FOUND: '$label'"
  return 1
}

adb install -r "$APK" || { note "install failed"; exit 1; }
adb logcat -c
adb shell am start -W -n "$PKG/.MainActivity"
sleep 8

step() {
  local name="$1"
  if alive; then shot "$name"; dump "$name"; note "ok: $name"; else note "DEAD at: $name"; FAILED=1; fi
}

step 01-menu
if [ $FAILED -eq 0 ]; then
  tap_text "IL RELITTO" || FAILED=1
  sleep 3
  step 02-briefing
fi
if [ $FAILED -eq 0 ]; then
  tap_text "INIZIA MISSIONE" || FAILED=1
  sleep 4
  step 03-run-early
  # Hold the right side of the screen to steer, then release.
  adb shell input swipe 1900 700 1950 700 1500
  sleep 6
  step 04-run-mid
  sleep 10
  step 05-run-late
fi
if [ $FAILED -eq 0 ]; then
  tap_text "PAUSA" && sleep 2 && step 06-pause
fi

adb logcat -d > "$OUT/logcat.txt"
if grep -q "FATAL EXCEPTION" "$OUT/logcat.txt"; then
  note "FATAL EXCEPTION found in logcat:"
  grep -A 25 "FATAL EXCEPTION" "$OUT/logcat.txt" | head -60 | tee -a "$OUT/summary.txt"
  FAILED=1
fi
rm -f "$OUT/ui-tmp.xml"
note "result: $([ $FAILED -eq 0 ] && echo PASS || echo FAIL)"
exit $FAILED
