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
    # Remove the previous dump first, so a failed dump can never pass off a stale file as new.
    adb shell rm -f /sdcard/ui.xml
    adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1
    adb exec-out cat /sdcard/ui.xml > "$out" 2>/dev/null
    grep -q "</hierarchy>" "$out" 2>/dev/null && return 0
    adb shell rm -f /sdcard/ui.xml
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

# Frame statistics from `dumpsys gfxinfo`. The counters run from process start and are never
# reset, so a later sample includes the earlier one. Informational only: nothing here sets FAILED.
# $1 is the label (run1 or final), $2 the saved dumpsys output.
gfx_note() {
  local label="$1" file="$2" field value frames janky p50 p90 p99
  if [ ! -s "$file" ]; then
    note "gfx $label UNEVALUABLE: $(basename "$file") is missing or empty"
    return
  fi
  for field in "Total frames rendered" "Janky frames" "50th percentile" "90th percentile" "99th percentile"; do
    value=$(tr -d '\r' < "$file" | sed -n "s/^$field: //p" | head -1)
    if [ -z "$value" ]; then
      note "gfx $label UNEVALUABLE: no '$field' line in $(basename "$file")"
      return
    fi
    case "$field" in
      "Total frames rendered") frames="$value" ;;
      "Janky frames") janky="$value" ;;
      "50th percentile") p50="$value" ;;
      "90th percentile") p90="$value" ;;
      "99th percentile") p99="$value" ;;
    esac
  done
  note "gfx $label (not gated): frames=$frames janky=$janky p50=$p50 p90=$p90 p99=$p99"
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
if ! find_node "SCAFO"; then
  note "NOT FOUND after starting mission: 'SCAFO'"
  FAILED=1
fi
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
  adb shell dumpsys gfxinfo "$PKG" > "$OUT/gfxinfo-run1.txt"
  gfx_note run1 "$OUT/gfxinfo-run1.txt"
  tap "RIPROVA"
else
  note "RETRY NOT REACHED within 120s"
  FAILED=1
  gfx_note run1 "$OUT/gfxinfo-run1.txt"
fi

# Run 2: steer and a proven dive, early in the fresh run, well before the boat sinks.
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
  # 12 s of wall time, not 6: each frame advances the game by at most 0.05 s
  # (BlackWakeApp.kt MAX_STEP_SECONDS), so on the slow emulator the game clock runs at
  # 0.62x wall time or less. The RELITTO tutorial line then also lands in the log; that is expected.
  set -- $DIVE_XY
  DIVE_T0=$(date +%s.%N)
  adb shell input swipe "$1" "$2" "$1" "$2" 12000 &
  sleep 1.5
  shot 10-run-dive
  wait
  DIVE_T1=$(date +%s.%N)
  adb shell input keyevent KEYCODE_BACK
  note "dive hold wall=$(awk -v a="$DIVE_T0" -v b="$DIVE_T1" 'BEGIN { printf "%.2f", b - a }')s"
  tap "REGISTRO DI BORDO"; sleep 1
  step 10b-dive-log
  if ! grep -q "</hierarchy>" "$OUT/10b-dive-log.xml" 2>/dev/null; then
    note "DIVE NOT VERIFIED: no usable 10b-dive-log.xml"
    FAILED=1
  elif grep -qF "OSSIGENO ESAURITO" "$OUT/10b-dive-log.xml"; then
    note "expect ok: dive engaged (log)"
  else
    note "DIVE NOT ENGAGED"
    # The log's T+ stamps are game time, so this shows how far the game clock got.
    note "diag: feed=$(tr '>' '\n' < "$OUT/10b-dive-log.xml" | grep -o 'text="[^"]*"' | sed 's/^text="//; s/"$//' | sed -n '/ VOCI$/,$p' | sed 1d | grep -v '^$' | head -12 | paste -sd ' ' -)"
    FAILED=1
  fi
  tap "RIPRENDI"
  step 12-retry
  expect "SCAFO"
fi

adb shell dumpsys gfxinfo "$PKG" > "$OUT/gfxinfo.txt"
gfx_note final "$OUT/gfxinfo.txt"

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
# Runtime errors from the app's own processes only.
APP_PIDS=$(grep -oE "Start proc [0-9]+:com\.frenzy_rush/" "$OUT/logcat.txt" | grep -oE "[0-9]+" | sort -u | paste -sd '|' -)
if [ -n "$APP_PIDS" ]; then
  grep -E "^[0-9-]+ [0-9:.]+ +($APP_PIDS) +[0-9]+ [EW] (AndroidRuntime|System\.err)" "$OUT/logcat.txt" | head -20 >> "$OUT/summary.txt"
fi

# Main-thread stalls of the app, reported by rule S v2 (startup window vs the rest of the run).
# Reporting only: limits and verdicts over a pair of runs are applied in review, not here.
python3 --version
if ! S2_NOTES=$(python3 - "$OUT/logcat.txt" "$OUT/s2.json" <<'S2_PY'
import json
import re
import sys
from datetime import datetime

LINE = re.compile(r"^(\d\d-\d\d) (\d\d:\d\d:\d\d\.\d{3})\s+(\d+)\s+(\d+)\s+[VDIWEF]\s+(\S+)\s*:\s?(.*)$")
START = re.compile(r"Start proc (\d+):com\.frenzy_rush/")
# Android prints the startup time as +812ms, +1s2ms or +1m2s3ms; absent parts count as 0.
DISPLAYED = re.compile(r"Displayed com\.frenzy_rush/\S+ .*\+(?:(\d+)m)?(?:(\d+)s)?(\d+)ms")
SKIPPED = re.compile(r"Skipped (\d+) frames")
STARTUP_WINDOW_S = 12.0


def stamp(day, clock):
    return datetime.strptime(f"2000-{day} {clock}", "%Y-%m-%d %H:%M:%S.%f")


logcat_path, json_path = sys.argv[1], sys.argv[2]
parsed = []
starts = {}
start_lines = 0
start_count = 0
unparsed_start = False
displayed_ms = None
with open(logcat_path, encoding="utf-8", errors="replace") as logcat:
    for raw in logcat:
        line = raw.rstrip("\n")
        m = LINE.match(line)
        if m:
            parsed.append(m)
        s = START.search(line)
        if s:
            start_lines += 1
            if not m:
                unparsed_start = True
            else:
                start_count += 1
                if s.group(1) not in starts:
                    starts[s.group(1)] = f"{m.group(1)} {m.group(2)}"
        if displayed_ms is None:
            d = DISPLAYED.search(line)
            if d:
                minutes, seconds, millis = (int(g) if g else 0 for g in d.groups())
                displayed_ms = minutes * 60000 + seconds * 1000 + millis

reasons = []
if start_lines == 0:
    reasons.append("no Start proc")
if displayed_ms is None:
    reasons.append("no Displayed com.frenzy_rush")
if not parsed:
    reasons.append("no line matches the S2-1 regex")

events = []
for m in parsed:
    pid = m.group(3)
    if pid not in starts or m.group(5) != "Choreographer":
        continue
    k = SKIPPED.search(m.group(6))
    if not k:
        continue
    after = (stamp(m.group(1), m.group(2)) - stamp(*starts[pid].split(" "))).total_seconds()
    window = "W1" if 0.0 <= after <= STARTUP_WINDOW_S else "W2"
    events.append({"pid": int(pid), "seconds_after_a0": round(after, 3), "frames": int(k.group(1)), "window": window})

# A start line the parser cannot read would silently lose its anchor, and the fixed
# year makes offsets go negative across New Year: neither run can be judged.
if unparsed_start:
    reasons.append("unparsed Start proc")
if any(e["seconds_after_a0"] < 0 for e in events):
    reasons.append("negative offset")

summary = {"unevaluable": reasons, "a0": starts, "displayed_ms": displayed_ms, "process_starts": start_count, "events": events}
with open(json_path, "w") as out:
    json.dump(summary, out, indent=2)

if reasons:
    for reason in reasons:
        print(f"S2 UNEVALUABLE: {reason}")
else:
    print(f"S2 anchor: pids={','.join(starts)} A0={','.join(starts.values())} D={displayed_ms} P={start_count}")
    for window, label in (("W1", "W1(startup 12s)"), ("W2", "W2(run)")):
        chosen = [e for e in events if e["window"] == window]
        largest = max((e["frames"] for e in chosen), default=0)
        listed = ", ".join(f"{e['frames']}@+{e['seconds_after_a0']:.3f}" for e in chosen)
        print(f"S2 {label}: E={len(chosen)} M={largest} events=[{listed}]")
S2_PY
); then
  note "S2 report failed: the python3 parser exited with an error"
fi
while IFS= read -r line; do
  [ -n "$line" ] && note "$line"
done <<< "$S2_NOTES"
rm -f "$OUT/ui-tmp.xml"
note "result: $([ $FAILED -eq 0 ] && echo PASS || echo FAIL)"
exit $FAILED
