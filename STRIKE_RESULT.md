# BLACK WAKE — DESTROY CODING STRIKE RESULT

Date: 2026-10-10  
Baseline: `main@b5ad79d8cebe951f8b5c171b6d0b9907487d7a28`  
Strike branch: `strike/destroy-coding-2026-10-10`  
Code checkpoint before this report: `d310161dc04a8771846070f8749893a6fe9d1ac2`

## Executive verdict

### Audited main

**NO — the audited main branch did not deserve a Google Play readiness claim.**

The native Android game was real, but the repository mixed obsolete architecture, fake/unreachable mechanics, incomplete persistence, missing behavioral tests, player-facing debug/telemetry UI and critical simulation bugs.

### Strike branch

**MATERIALLY IMPROVED, BUT RELEASE STILL BLOCKED.**

The strike removes proven dead weight and repairs several core systems. It does not claim physical-device input quality, frame pacing, battery performance, first-session comprehension or commercial retention from source inspection alone.

## Destruction summary

Compared with the audited main checkpoint, the code checkpoint before this report contains:

- 44 strike commits;
- 160 changed files;
- 136 removed files;
- 10 added files;
- 14 modified files;
- 1,229 added lines;
- 6,685 deleted lines.

This is a net deletion of more than 5,400 lines while adding tests and explicit systems.

## What was destroyed

### Obsolete architecture

Removed from the active tree:

- Capacitor configuration;
- TypeScript/Vitest configuration;
- Drizzle configuration/schema;
- abandoned `android/gradlew` shell;
- web/AI Studio deployment instructions;
- React/Babylon structure documentation;
- obsolete package metadata;
- tracked Gradle local cache.

Native Kotlin/Compose is now the single canonical product architecture.

### Mutation debris

Removed more than one hundred one-off mutation files:

- `patch_*.py`;
- `fix_*.py`;
- `add_*.py`;
- `.sed` scripts;
- old patch files.

Git history is the archive. The product tree no longer needs surgery scripts describing code that already changed.

### Fake or unreachable product systems

Removed:

- player-facing PROFILER button;
- vessel selector that had no simulation effect;
- ENGINE upgrade that consumed Intel but changed no gameplay;
- unreachable PATCHING mode/minigame;
- event-log button/overlay that was not mounted correctly;
- manual global-alarm button whose primary effect was UI/siren state;
- unreachable pursuer ATTACK state;
- fake radar-noise contacts;
- false `DST ... m` telemetry derived from seconds.

## Critical simulation repairs

### Dive / oxygen / sonar

The audited main computed new dive, oxygen and sonar values but failed to commit them back to `GameState`.

The strike writes back:

- submerged state;
- oxygen;
- sonar charges;
- sonar recharge timer;
- sonar active state;
- sonar radius.

### Fuel exhaustion

The audited main had an impossible branch requiring fuel to be both zero and greater than zero.

Fuel exhaustion is now a tested deterministic transition:

- fuel reaches zero;
- a finite drift window begins;
- drift counts down;
- hull punishment begins only after drift expires.

### Run state

New runs now explicitly reset run-scoped state instead of inheriting battery, oxygen, pressure, compartments, flare state and auxiliary UI from a previous run.

### Persistence

Removed pursuer-only half-restoration.

Added coherent meta-progression persistence for:

- unlocked chapters;
- Intel bank;
- best Intel;
- hull upgrade;
- tank upgrade;
- radar upgrade;
- stealth upgrade.

Runtime combat state is deliberately not half-restored.

## Architecture rebuilt where it solved a demonstrated problem

### ResourceSystem

Fuel, oxygen and battery rules now live in a deterministic JVM-testable system.

Tests cover:

- fuel exhaustion;
- drift countdown;
- oxygen depletion/regeneration;
- emergency-power oxygen tradeoff;
- battery depletion;
- battery recharge.

### ProgressionRepository

Meta-progression is separated from volatile run state.

### EnvironmentRules

Environment labels now alter rules instead of existing only as names.

Examples:

- PORT shortens sonar range and tightens lane handling;
- ISLAND strengthens wreck cover;
- EXTRACT increases detection pressure;
- STORM and DAWN define distinct profiles for future/available sector use.

Rendering uses the same environment identity instead of coloring water by chapter index alone.

### Pursuer behavior

Enemy archetypes now have distinct behavior profiles:

- detection threshold;
- reaction delay;
- search persistence;
- reacquisition duration;
- interception capability;
- interception distance.

Hunter and Elite no longer behave as cosmetic stat variants of Patrol.

## First-session rebuild

The original tutorial was explicitly described in code as an “invisible tutorial”.

It was also temporally impossible to finish in its intended first sector: the final tutorial trigger occurred after the normal duration of that sector.

The strike:

- extends the opening tutorial sector;
- makes all tutorial events reachable before sector end;
- adds visible contextual coachmarks;
- stages steering, fuel, Intel, stealth and escape-tool teaching;
- stops repeating the tutorial after later chapters are unlocked.

## HUD / UX destruction

Removed from live driving:

- scrolling terminal feed;
- player-facing debug profiler;
- audio settings;
- battery voltage readout;
- battery history graph;
- detailed per-pursuer developer telemetry;
- fake radar noise;
- always-visible damage-control action when no breach exists.

Changed:

- damage control appears only when a breach exists;
- audio toggle lives in pause;
- battery is a compact actionable gauge;
- pursuers are summarized by count;
- map timing no longer pretends seconds are metres.

Input targets proven from source:

- DIV: 64 dp;
- SNR: 64 dp;
- throttle width: 48 dp;
- game canvas ignores touch events already consumed by controls.

Physical grip, finger occlusion, drag latency and overshoot remain unverified.

## Game feel / audio

The audited audio engine exposed dynamic properties such as RPM, detection, pursuer count, oxygen and hull state, but the game never supplied those values.

The strike connects audio to actual simulation state.

Also:

- SFX reuse one coroutine scope instead of creating a new scope per sound;
- stream audio stops in pause/background;
- game resumes audio only when the player resumes play;
- siren feedback now follows actual high detection / critical hull rather than a manual alarm button.

## Mobile lifecycle

Added:

- immersive fullscreen system-bar hiding;
- transient system bars available by swipe;
- explicit auto-pause when the activity loses foreground;
- no attempt to fake full run restoration after process death.

## Determinism and state safety

Added a per-run seed and a single seeded RNG for gameplay randomness.

Removed:

- scattered `Math.random()`;
- per-damage `java.util.Random()`;
- default unseeded list `.random()`.

Entity and pursuer lists are deep-copied before frame mutation so older `GameState` snapshots are not mutated by a later simulation frame.

## Tests and CI

Added JVM behavioral suites for:

- pursuer AI;
- resource rules;
- environment rules.

Added Android instrumentation smoke coverage that launches `MainActivity` on an emulator.

GitHub Actions gates:

1. `:app:assembleDebug`;
2. `:app:testDebugUnitTest`;
3. `:app:lintDebug`;
4. emulator `:app:connectedDebugAndroidTest`.

The workflow cancels obsolete strike runs so only the newest revision matters.

## Destroy / keep outcome

### KEEP

- native Kotlin/Compose product;
- core steering/route positioning;
- throttle versus stealth;
- detection/pursuit;
- Intel objective;
- fuel;
- hull;
- tested pursuer state machine foundation.

### KEEP + POLISH

- sonar;
- dive/oxygen;
- flare;
- tactical map;
- four real upgrades;
- damage control.

These survive because they have or can support a distinct decision, but still require runtime playtesting.

### REBUILD

- chapter strategy;
- audio implementation quality;
- progression feel;
- first-session pacing;
- rendering/performance hotspots.

The strike improved each of these, but none receives a final PASS without runtime evidence.

### DELETE

Executed deletion for:

- obsolete web architecture;
- fake vessel selection;
- fake engine upgrade;
- unreachable patching minigame;
- broken event-log surface;
- manual alarm control;
- developer profiler in product UI;
- fake radar contacts;
- repository patch debris.

## Commercial reality

The audited main lacked enough proof to say it could compete seriously on Google Play.

The strike creates a stronger engineering and product foundation, but the commercial verdict remains:

**NOT YET PROVEN.**

Before claiming store readiness, Black Wake still needs evidence for:

- first 10 / 30 / 60 seconds with a true new player;
- meaningful strategy differences across all three chapters;
- low/mid-range Android frame pacing;
- 60/90/120 Hz input feel;
- small and large landscape phone ergonomics;
- real audio listening test;
- retention reason after first clear;
- store icon/screenshots/trailer based on the final runtime;
- signed release configuration and Play policy review.

## Release gate

**BLOCKED.**

Do not generate a production AAB from this strike merely because the build is green.

A green automated gate means the branch is technically reviewable. It does not prove physical-device UX, performance or commercial quality.
