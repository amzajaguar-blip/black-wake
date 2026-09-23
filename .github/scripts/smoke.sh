#!/usr/bin/env bash
# Emulator smoke test: install, launch and walk the main flows, capturing screenshots,
# UI hierarchy dumps and logcat. Fails if the app crashes, dies, or an expected control is missing.
set -u
APK="$1"
OUT="$2"
PKG=com.frenzy_rush
# The activity keeps the source namespace, which is not the applicationId,
# so the component name has to be spelled out rather than written "$PKG/.MainActivity".
ACTIVITY=com.blackwake.game.MainActivity
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
# Screenshot only, for moments while the game runs: a uiautomator dump would stall it.
shot() {
  if alive; then
    adb exec-out screencap -p > "$OUT/$1.png"
    note "ok: $1 (screenshot only)"
  else
    note "DEAD at: $1"
    FAILED=1
  fi
}

# Prints "x y" for the centre of the first node in the existing dump $1 whose text or
# content-desc contains $2, without dumping again.
xy_in_dump() {
  local b
  b=$(tr '>' '\n' 2>/dev/null < "$1" | grep -F "$2" | grep -o 'bounds="[^"]*"' | head -1 | grep -o '[0-9]\+' | tr '\n' ' ')
  [ -n "$b" ] || return 1
  set -- $b
  echo "$(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 ))"
}

# Prints "x y" for the centre of the first node on screen whose text or content-desc contains $1.
find_node() {
  local label="$1" tries=0
  local limit="${2:-8}"
  while [ $tries -lt $limit ]; do
    dump ui-tmp
    if xy_in_dump "$OUT/ui-tmp.xml" "$label"; then
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
adb shell am start -W -n "$PKG/$ACTIVITY"
sleep 8
# Intentionally optional: the system "Got it" dialog may not appear at all. This is the only
# find_node call whose miss does not fail the run.
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

# Same check against a dump already taken, so no new dump stalls the running game.
expect_in() {
  if xy_in_dump "$OUT/$1.xml" "$2" > /dev/null; then
    note "expect ok: '$2'"
  else
    note "EXPECT FAILED: '$2'"
    FAILED=1
  fi
}

step 01-menu
expect "BLACK WAKE"

tap "OFFICINA"; sleep 2; step 02-garage
expect "MODULI"
tap "INDIETRO"; sleep 2

tap "IL RELITTO"; sleep 2; step 03-briefing
# Run 1: pause, background and back. The game runs unattended here and a boat sinks in
# about 22 s, so only one dump is taken while it runs; everything after HOME is paused.
tap "INIZIA MISSIONE"; sleep 2
step 04-run-start
expect_in 04-run-start "SCAFO"
expect_in 04-run-start "Pausa"
DIVE_XY=$(xy_in_dump "$OUT/04-run-start.xml" "Immergi")
if [ -z "$DIVE_XY" ]; then
  note "NOT FOUND: dive control 'Immergi'"
  FAILED=1
fi

# Leaving and returning must come back paused, not running blind or crashed.
adb shell input keyevent KEYCODE_HOME; sleep 3
adb shell am start -W -n "$PKG/$ACTIVITY"; sleep 3
step 05-resume-from-background
expect "SOSPENSIONE"
tap "RIPRENDI"; sleep 1

# The back key must pause instead of leaving the game.
adb shell input keyevent KEYCODE_BACK; sleep 1; step 06-back-pauses
expect "SOSPENSIONE"
tap "MAPPA TATTICA"; sleep 2; step 07-pause-map
tap "REGISTRO DI BORDO"; sleep 2; step 08-pause-log
tap "RIPRENDI"; sleep 1

# Let the mission play out; a death ends in the debrief, which must offer a retry.
RETRY_START=$SECONDS
RETRY_REACHED=0
while [ $((SECONDS - RETRY_START)) -lt 120 ]; do
  if find_node "RIPROVA" 1 > /dev/null; then
    RETRY_REACHED=1
    break
  fi
  sleep 5
done
if [ $RETRY_REACHED -eq 1 ]; then
  step 11-run-late
  note "retry reached after $((SECONDS - RETRY_START))s"
  tap "RIPROVA"
else
  note "RETRY NOT REACHED within 120s"
  FAILED=1
fi

# Run 2: steer and a proven dive, all within about 9 s of the fresh run's clock.
if [ $RETRY_REACHED -ne 1 ]; then
  note "run 2 skipped: the retry was never reached"
  FAILED=1
elif [ -z "$DIVE_XY" ]; then
  note "run 2 skipped: no dive control coordinates"
  FAILED=1
else
  sleep 1
  adb shell input swipe 700 700 1500 700 1200
  shot 09-run-steer
  # Held longer than the 4 s of oxygen, so the game must log that the oxygen ran out.
  set -- $DIVE_XY
  adb shell input swipe "$1" "$2" "$1" "$2" 6000 &
  sleep 1.5
  shot 10-run-dive
  wait
  adb shell input keyevent KEYCODE_BACK
  tap "REGISTRO DI BORDO"; sleep 1
  step 10b-dive-log
  if ! grep -q "</hierarchy>" "$OUT/10b-dive-log.xml" 2>/dev/null; then
    note "DIVE NOT VERIFIED: no usable 10b-dive-log.xml"
    FAILED=1
  elif grep -qF "OSSIGENO ESAURITO" "$OUT/10b-dive-log.xml"; then
    note "expect ok: dive engaged (log)"
  else
    note "DIVE NOT ENGAGED"
    FAILED=1
  fi
  tap "RIPRENDI"
  step 12-retry
  expect "SCAFO"
fi

adb logcat -d > "$OUT/logcat.txt"
if ! grep -q "BlackWake.*background: pausing" "$OUT/logcat.txt"; then
  note "the app never reported going to the background"
  FAILED=1
fi

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
