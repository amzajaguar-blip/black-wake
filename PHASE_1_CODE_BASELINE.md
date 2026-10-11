# PHASE_1_CODE_BASELINE.md

## (a) Branch and Baseline SHA
- Branch: `strike/phase-1-core-surgery`
- Baseline SHA: `2b9315760fb83886715d09955a12c7c7518394bb`

## (b) Exact Source Files (Design Docs)
The following 8 design documents are part of this baseline:
1. `PHASE_1_DESIGN_SPEC.md`
2. `BLACK_WAKE_MASTER_DESIGN.md`
3. `SYSTEM_INTERACTION_MATRIX.md`
4. `MASTER_EXPLOIT_REGISTER.md`
5. `MASTER_ACCEPTANCE_GATES.md`
6. `ENEMY_COUNTERPLAY_MATRIX.md`
7. `ROUTE_AND_CHAPTER_BLUEPRINT.md`
8. `PROGRESSION_AND_RETENTION_MODEL.md`

## (c) INPUT→STATE→SIMULATION→AI→FEEDBACK→RESET/EXPIRY Map

### Sonar
- **INPUT**: `prev.hunted` (boolean), `prev.sonar.cooldown` (float), `prev.sonar.active` (boolean), `prev.sonar.radius` (float)
- **STATE**: `SonarState(cooldown, active, radius)`
- **SIMULATION**: 
  - Cooldown decreases by `dt`.
  - If `prev.hunted && !pingActive && cooldown <= 0f` → trigger ping: set `pingActive = true`, `radius = 0f`, `cooldown = sonarCooldownSeconds(modules)`, emit tone.
  - If `pingActive`: increase `radius` by `dt * PING_SPEED`; if radius intersects a hazard's z, emit vibrate; if `radius >= PING_RANGE` → reset: `pingActive = false`, `radius = 0f`.
- **AI**: Active pursuers investigate the ping origin for 2 seconds if they are in SEARCH state (see PursuerAI.kt: investigation logic).
- **FEEDBACK**: 
  - Tone: `GameEvent.Tone(1320f, 0.22f, 0.3f)` on ping start.
  - Vibrate: `GameEvent.Vibrate(listOf(0, 30))` when ping crosses a hazard.
- **RESET/EXPIRY**: 
  - Ping active expires when `radius >= PING_RANGE` (110 units).
  - Cooldown expires naturally; radar modules can reduce cooldown to a floor of 4 seconds (`6 - min(radarLevel, 2)`).

### Flare
- **INPUT**: Player flare command (external to step, via `launchFlare` function).
- **STATE**: `FlareState(charges, timer)`
- **SIMULATION** (in `launchFlare`):
  - If `flare.charges > 0 && flare.timer <= 0f && !run.dive.submerged`:
    - Decrement charges, set `timer = FLARE_SECONDS` (5f).
    - Dazzle all pursuers in PURSUIT, INTERCEPT, or DETECTED state (set state to SEARCH).
    - Emit screen flash and noise.
- **AI**: 
  - Dazzled pursuers enter SEARCH state.
  - After dazzle, pursuers follow reacquisition rules: `reacquired = !ctx.hidden || ctx.detection > 0.6f` (PursuerAI.kt:83).
  - If not reacquired, they remain in SEARCH; if reacquired, they return to PURSUIT/INTERCEPT.
- **FEEDBACK**:
  - Screen flash: `screenFlash = 0.5f`, `flashKind = FlashKind.FLARE`.
  - Noise: `GameEvent.Noise(0.35f, 0.5f)`.
  - HUD message: "RAZZO // INSEGUITORI ACCECATI" if dazzled > 0, else "RAZZO // ACQUA ILLUMINATA".
- **RESET/EXPIRY**:
  - Flare timer decreases by `dt` each frame; when `timer <= 0f`, flare effect ends (but charges remain depleted until reload).
  - Visual effect lasts 5 seconds (timer).

### SPINTA (Emergency Power)
- **INPUT**: `throttle > 0.8f` (from `RunState.throttle`, set via `setThrottle` or input).
- **STATE**: Derived from `throttle`; no dedicated state burst/heat/Dive lock in current code.
- **SIMULATION**:
  - `targetSpeed = (0.8f + throttle * 0.4f) * boat.speed`
  - When `throttle > 0.8f`, the speed is boosted (see E6: speed = `(0.8f + throttle*0.4f)*boat.speed` = 1.2× full lever at max throttle) [GameSimulation.kt:120].
  - Detection: increased detection due to throttle (not explicitly modeled as a separate burst cost).
- **AI**: Pursuers use standard detection and pursuit logic; no specific AI response to SPINTA.
- **FEEDBACK**:
  - HUD: `Hud.kt:215` shows "SPINTA" when `throttle > 0.8f`.
  - Audio/visual: none distinct from normal high throttle.
- **RESET/EXPIRY**: 
  - No automatic reset; state lasts as long as `throttle > 0.8f`.
  - No heat lock, Dive lock, or burst duration limits.

### Damage Control
- **INPUT**: None (system absent).
- **STATE**: No repair-related fields in `RunState` (see E7: no repair field in `GameModel.kt`).
- **SIMULATION**: Hull only decreases on impact or fuel exhaustion; no recovery mechanic.
- **AI**: Pursuers unaffected by hull state.
- **FEEDBACK**: None (no repair action).
- **RESET/EXPIRY**: N/A (system not present).

## (d) Current Exploit Paths (from Verified Facts)
- **E2**: Sonar auto-fires while hunted → `GameSimulation.kt:131`: `if (prev.hunted && !pingActive && cooldown <= 0f) { pingActive = true … }`
- **E3**: Sonar cooldown `6f - modules.radar * 1f`, no floor → `GameSimulation.kt:52`
- **E4**: Flare dazzles every non-lost pursuer globally → `GameSimulation.kt:80` + `PursuerAI.kt:73`
- **E5**: Flare break voided on the next frame for a loud player → `PursuerAI.kt:83`: `reacquired = !ctx.hidden || ctx.detection > 0.6f`
- **E6**: No Emergency Power state; top band `throttle > 0.8f`, speed `(0.8f + throttle*0.4f)*boat.speed` (=1.2× full lever) → `Hud.kt:215` shows "SPINTA" when `throttle > 0.8f`
- **E7**: Damage Control absent: no repair field in `RunState` (`GameModel.kt`), ActionPad only RAZZO + IMMERGI (`Hud.kt`), `repair` → 0 hits in `app/src/main`
- **E8**: Low throttle code-rewarded: detection decay 0.16/s silent vs 0.02/s else (`GameSimulation.kt:145-152`), stealth `(0.3f + throttle*0.7f)` (:143), silent Intel combo (`GameSimulation.kt:300`)
- **E9**: Seeded soak harness: `SoakTest.kt` (policies CAUTIOUS/BALANCED/RECKLESS, seeds 1–5, chapters 0–7, 10 sim-minutes), `SoakAdapter.kt` LABEL "current"
- **E13**: Flare 55-unit reach no-op: `PursuerAI.kt` SPAWN_Y=118, CRUISE_Y=70, PASS_Y=97, GONE_Y=130; PLAYER_Y=85 → \|p.y−85\| ∈ [15,45], all ≤ 55

## (e) Mismatch vs `PHASE_1_DESIGN_SPEC.md`
Based on a review of the design spec, the following mismatches exist:
- **Emergency Power**: Design spec defines a bounded emergency burst with fuel cost, detection spike, heat lock, and Dive lock. Current implementation has only a visual label "SPINTA" for high throttle, with no distinct state, fuel cost, or lockouts.
- **Flare**: Design spec limits flare to affect pursuers within 55 units, adds +0.18 detection on launch, sets a 4-second investigation marker, and prevents reacquisition for 2 seconds. Current implementation dazzles all non-lost pursuers globally (E4), has no detection cost on launch, and reacquisition can occur immediately if detection > 0.6f (E5).
- **Damage Control**: Design spec adds a repair action with kits, fuel cost, time-limited repair, and detection cost. Current implementation has no repair action whatsoever (E7).
- **Sonar**: Design spec removes automatic ping, adds manual button with detection cost, propagation delay, contact snapshot, and cooldown with radar-based floor. Current implementation fires automatically while hunted (E2), has no player cost, and cooldown has no floor (E3).

## (f) Systems that are Fake/Partial/Overloaded
- **Emergency Power**: Fake – labeled as a distinct state but is merely a throttle band with no unique mechanics.
- **Flare**: Partially fake – the visual and dazzle effects are real, but the promised escape window is not reliably enforced (global effect, immediate reacquisition for loud players).
- **Damage Control**: Fake – no such system exists; hull damage is purely cumulative.
- **Sonar**: Partially fake – provides real hazard detection via ping, but automatic firing removes player agency and costs nothing, reducing tactical value.

## (g) Tests Already Covering Them (from GameSimulationTest.kt)
The following 5 test names are verified to exist in `GameSimulationTest.kt`:
1. `sonarAutoPingsWithinOneSecondOfBeingHunted`
2. `sonarRespectsCooldownWhileHunted`
3. `sonarStaysSilentWhenNotHunted`
4. `flareDazzlesActivePursuers`
5. `throttleAloneSetsTopSpeed`

## (h) Missing Tests
Based on the design spec and verified exploits, missing tests include (but are not limited to):
- Tests for manual Sonar activation (cost, cooldown, propagation).
- Tests for Flare's limited range (55 unit) and detection cost.
- Tests for Flare's reacquire grace period (2 seconds) and investigation marker (4 seconds).
- Tests for Emergency Power burst mechanics (fuel cost, speed multiplier, heat lock, Dive lock).
- Tests for Damage Control action (kit consumption, fuel cost, repair duration, hull recovery).
- Tests that automatic Sonar ping does not occur when not hunted.
- Tests that Sonar ping does not reveal entities spawned after activation.
- Tests that Flare does not affect lost pursuers.
- Tests that Emergency Power cannot be activated during heat lock or Dive lock.

## (i) Baseline Evidence from Soak Test (Pre-Change Reference)
**BLOCKED_ENVIRONMENT**: The Soak test could not be completed due to environment constraints (timeout during Gradle configuration after 10 minutes). Exact output from the attempted run:

```
Command: GRADLE_USER_HOME=/media/omar/BACKUP_BOT/GRADLE-HOME-BLACKWAKE ./gradlew :app:testDebugUnitTest --tests "com.blackwake.game.SoakTest.aSeededSoakReportsBaseline" --no-daemon
Directory: /home/locoomo/Scrivania/building factory/NEW_STUDIO_GAMES/MAREA-NERA
Timeout: 600000ms
Output before timeout:
To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.14.3/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build 
<======-------> 50% CONFIGURING [9m 39s]
> IDLE
> :app > Compiling build file 'app/build.gradle.kts'
```

Due to the timeout, no `SOAK ...` rows were captured. This baseline evidence is pending and will be provided by CI or a subsequent attempt with sufficient resources.

---
*This document captures the pre-strike tactical baseline as of commit `2b9315760fb83886715d09955a12c7c7518394bb`. No changes were made to `app/src` in this unit.*