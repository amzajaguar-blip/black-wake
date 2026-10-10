# BLACK WAKE — FORENSIC BASELINE

Audit date: 2026-10-10  
Audited production branch: `main`  
Audited main SHA: `b5ad79d8cebe951f8b5c171b6d0b9907487d7a28`

## Executive verdict

**PLAY STORE READINESS: FAIL / RELEASE BLOCKED**

The current product does not yet justify a Play Store quality claim. The repository contains a real native Android game, but it is mixed with obsolete web/Capacitor scaffolding, player-facing debug UI, fake or unreachable systems, missing automated Android verification, and critical simulation state that was computed but not committed back into the state model.

This document distinguishes source-proven findings from runtime claims. Performance, device ergonomics and actual first-session quality remain unverified until device/runtime evidence exists.

## Ground truth architecture

The active build is native Android/Kotlin:

- `settings.gradle.kts` includes only `:app`.
- `app/build.gradle.kts` applies the Android application and Kotlin Android plugins.
- `MainActivity.kt` mounts the Compose `GameScreen`.
- gameplay lives in `GameViewModel.kt`, `GameScreen.kt`, `GameData.kt`, `PursuerAI.kt`, and `SynthAudioEngine.kt`.

The old web architecture is not present as a working second product:

- `tsconfig.json` references `client/src`, `shared`, and `server`, but those directories are absent.
- `vitest.config.ts` points tests at `server/**/*.test.ts`, but `server` is absent.
- `capacitor.config.ts` points to `dist/public`, but the active Gradle project is the root native Android app.
- `README.md` describes React/Babylon/Capacitor architecture that does not match the current tree.
- `android/` contains only a stranded `gradlew` script and is not the included Android module.

**Canonical architecture decision: native Kotlin/Compose.**

## P0 / critical correctness findings

### 1. Dive, oxygen and sonar state were computed and discarded

In `GameViewModel.updateGame()`, the code computed:

- `newSubmerged`
- `newOxygen`
- `newSonarCharges`
- `newSonarRegen`
- `newSonarActive`
- `newSonarRadius`

but the final `GameState.copy(...)` did not write those values back.

Result: the simulation and the HUD could disagree, oxygen did not advance correctly in state, and sonar activation could become stuck/incoherent.

Verdict: **REBUILD/FIX IMMEDIATELY**.

### 2. Fuel exhaustion transition contained an impossible branch

The main branch required `state.fuel == 0f` and then nested a check requiring `state.fuel > 0f` before starting drift.

That branch can never be true.

Verdict: **FIX IMMEDIATELY**.

### 3. Partial run persistence was invalid

The app persisted only pursuers while the rest of the run state was not persisted. On startup it attempted to restore pursuers into a fresh `GameState`.

This is exactly the dangerous half-restoration case: restored enemies can exist without the run that created them.

Additionally, the initialization block called `loadPursuers()` before the later-declared preference/state fields had completed normal class initialization order, creating a construction-time risk.

Verdict: **DELETE partial restoration** until a complete run snapshot exists.

### 4. New runs inherited stale subsystem state

`startGame()` reset core counters but did not reset multiple run-scoped systems including oxygen, battery, emergency power, compartments, flare state, depth/pressure state and auxiliary overlays.

A new run could therefore inherit state from the previous run.

Verdict: **FIX IMMEDIATELY**.

## P1 / player-facing product failures

### Player-facing profiler

`GameScreen` exposed a `PROFILER` button during normal product use.

Verdict: **DELETE from release-facing UI**.

### Branding incoherence

The product label is Black Wake, but the main menu still displayed `MAREA NERA`; `settings.gradle.kts` and `package.json` also retained legacy naming.

The Android `applicationId` is intentionally not changed by this strike because it may be a Play Store identity and requires release-history confirmation.

Verdict: **FIX presentation, preserve applicationId until verified**.

### HUD overload

The HUD simultaneously exposes terminal feed, oxygen, fuel, intel, sonar charges, tactical map, event log, damage control, audio toggle, flare, emergency power, battery voltage/history, manual global alarm, detection telemetry, pursuer telemetry, radar, minimap, hull, dive, sonar and throttle.

This is not evidence of depth. It creates severe attention competition in a steering game.

Verdict: **SIMPLIFY after runtime capture; remove fake/developer telemetry first**.

## Fake / unreachable system findings

### Boat selection has no mechanical effect

`playerBoatType` is stored and displayed as a selection, but is not referenced by gameplay simulation. The only simulation-side occurrence is the setter itself.

Verdict: **DELETE the choice until boat profiles are real, or rebuild it as transformational loadouts**.

### ENGINE upgrade has no effect

`modules.engine` is read only by the purchase path. It does not change movement, fuel, detection, speed, boost or any other simulation rule.

Verdict: **DELETE from store/garage until implemented**.

### PATCHING system is unreachable

`GameMode.PATCHING` is checked by patching functions and UI, but the main branch contains no transition assigning `mode = GameMode.PATCHING`. `hasTriggeredPatching` is declared but not used.

Verdict: **DELETE or redesign; do not advertise it as gameplay**.

### Event log toggle does not mount its overlay

`showEventLog` toggles and `EventLogOverlay` exists, but `GameScreen` does not conditionally mount that overlay.

Verdict: **DELETE from active HUD or wire it in a non-driving context**.

## Architecture findings

- `GameViewModel.kt`: about 1,198 lines on audited main.
- `GameScreen.kt`: about 2,128 lines on audited main.
- `GameViewModel` owns simulation, input intent, AI orchestration, audio triggers, vibration, upgrades, run flow, resource systems, damage, sonar, battery, depth and UI-facing state.
- `GameScreen` owns menus, HUD, overlays, profiler, controls, canvas rendering and effects.

Verdict: **REBUILD boundaries incrementally**. Extraction is justified where it creates deterministic tests or removes frame-loop/UI coupling; do not split code only to look clean.

## Tests / CI on audited main

No `app/src/test` or `app/src/androidTest` suite existed. No GitHub Actions workflow existed.

The only root `test_audio.py` checked whether the source text contained the string `fun playExplosion()`; that is not behavioral testing.

`vitest.config.ts` pointed at a missing server tree.

Verdict: **FAIL**.

## Repository hygiene

The audited main tracked Gradle local cache state under `.gradle/` and contains a large number of one-off `patch_*.py`, `fix_*.py`, and `.sed` mutation scripts in repository root.

Verdict:

- `.gradle/`: **DELETE immediately**.
- obsolete web/Capacitor/Drizzle/Vitest scaffold: **DELETE after canonical native decision**.
- one-off patch debris: **quarantine/delete after current source is verified**.

## Content / retention findings that require runtime proof

Source inspection shows three chapters with four sectors each, mostly parameterized by density, threat, intel bias, environment and fork/extract flags. That does not prove strategic chapter differentiation.

The strike will not claim chapter variety, retention quality, control quality, frame pacing or device UX from source inspection alone.

Required evidence before release:

1. clean Android build + lint + unit tests,
2. low/mid-range device session,
3. first-session recording,
4. touch/input measurements,
5. profiler/frame pacing capture,
6. chapter strategy comparison,
7. final HUD screenshots at target aspect ratios.

## Changes started on strike branch

Branch: `strike/destroy-coding-2026-10-10`

Completed so far:

- fixed critical run-state writeback,
- fixed fuel exhaustion transition,
- removed partial pursuer-only restoration,
- reset run-scoped systems on new run,
- removed player-facing profiler,
- restored Black Wake title in menu,
- removed tracked `.gradle/` state,
- made pursuer AI JVM-testable,
- added behavioral AI unit tests,
- added Android CI build/test/lint workflow.

Release remains blocked until CI and runtime/device gates pass.
