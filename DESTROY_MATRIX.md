# BLACK WAKE — DESTROY / KEEP / REBUILD MATRIX

Evidence baseline: `main@b5ad79d8cebe951f8b5c171b6d0b9907487d7a28`

| System | Actual player value | Technical / UX cost | Verdict | Replacement plan |
|---|---|---|---|---|
| Native Kotlin/Compose app | Real executable product | Large files but coherent platform | **KEEP** | Make it the only canonical architecture |
| React/Babylon/Capacitor docs/config | None in current tree; referenced source dirs are absent | Confuses build, onboarding and maintenance | **DELETE** | Native-only README and CI |
| `.gradle/` tracked cache | None | Generated binary noise and merge churn | **DELETE** | Ignore local Gradle state |
| GameViewModel monolith | Contains real game simulation | 1.2k lines, hard to test, UI/audio side effects inside simulation | **REBUILD** | Extract deterministic run rules first, not ceremonial layers |
| GameScreen monolith | Renders complete product | 2.1k lines, HUD/menu/render/effects coupled | **REBUILD** | Split by player surface after visual baseline |
| Core steering / lane inertia | Direct control and mastery | Needs device validation | **KEEP + POLISH** | Measure drag latency, overshoot, occlusion |
| Throttle / stealth / detection | Strong risk/reward candidate | UI currently competes with action | **KEEP + POLISH** | Make detection readable spatially/audio-first |
| Fuel | Creates pacing pressure | Exhaustion transition was broken | **KEEP + POLISH** | Preserve after corrected transition and test it |
| Intel objective | Gives route/reward purpose | Currently mostly numeric | **KEEP + POLISH** | Tie choices to unlocks and chapter consequences |
| Sonar | Potential information-risk tool | State writeback was broken | **REBUILD** | Verify ping lifecycle, visibility and recharge |
| Dive / oxygen | Potential stealth-risk tool | State writeback was broken; extra HUD burden | **REBUILD** | Keep only if it creates route/escape decisions in playtest |
| Battery / emergency power | Potential tradeoff | Adds another gauge and button | **SIMPLIFY** | Contextual action, not permanent instrumentation panel |
| Manual global alarm | No meaningful decision; mostly siren/UI | Button/HUD noise | **DELETE** | Trigger alarm feedback automatically from real danger |
| Flare | Situational escape/visibility candidate | Another button/resource | **SIMPLIFY** | Keep only if it solves a distinct pursuer problem |
| Damage compartments | Can create consequence | Heavy HUD/overlay cost; weak integration | **REBUILD** | Either make compartment choice tactical or collapse to hull states |
| PATCHING minigame | None currently; unreachable | Dead state/functions/overlay | **DELETE** | Reintroduce only with an actual trigger and unique decision |
| Event log during driving | Low immediate action value | Reading load; overlay not mounted | **DELETE from driving HUD** | Surface in pause/debrief |
| Tactical map | Potential route planning | Pulls eyes from action | **SIMPLIFY** | Use only when route decision is active |
| Player-facing profiler | Zero player value | Developer telemetry in product UI | **DELETE** | Keep diagnostics internal only |
| Boat selection | Looks like meaningful choice | No simulation effect; deceptive choice | **DELETE / REBUILD** | Real boat profiles or no selector |
| ENGINE upgrade | Zero current effect | Costs intel for nothing | **DELETE** | Re-add only when it transforms handling/boost |
| HULL upgrade | Real effect | Mostly numerical | **KEEP + POLISH** | Add visible/strategic consequence later |
| TANK upgrade | Real pickup effect | Mostly numerical | **KEEP + POLISH** | Validate whether it changes route risk |
| RADAR upgrade | Real sonar charge/recharge effect | Depends on sonar quality | **KEEP conditionally** | Keep only if sonar survives playtest |
| STEALTH upgrade | Real detection decay effect | Mostly numerical | **KEEP + POLISH** | Make effect perceptible |
| Pursuer AI state machine | Real chase behavior | Archetypes still share much movement logic | **REBUILD** | Preserve tested state machine; deepen behavior by archetype |
| Synth audio | Strong feedback potential | Global mutable singleton and per-SFX AudioTrack creation | **REBUILD** | Pool SFX, lifecycle-safe audio director, profile CPU/audio glitches |
| Random spawn logic | Replay variation | Uses global nondeterministic randomness | **REBUILD** | Seeded RNG for deterministic tests and reproducible runs |
| Chapters/sectors | Provides campaign framing | Strategy differentiation not proven | **REBUILD** | Each chapter must force a different decision pattern |
| Root patch/fix scripts | No runtime value | Massive repository noise | **DELETE after verification** | Git history is the archive |
| Play Store assets | Commercial surface | May be stale vs current runtime | **KEEP + REVALIDATE** | Regenerate only after gameplay identity stabilizes |

## Core-four provisional decision

Until device playtesting disproves it, Black Wake should concentrate around:

1. **Steer / route positioning**
2. **Speed versus stealth**
3. **Detection / pursuit manipulation**
4. **Information / escape tools** (sonar + dive only if they create distinct decisions)

Fuel and intel support those pillars. Any system that does not strengthen them must justify its HUD, code and learning cost.

## Release gate

**BLOCKED.** A successful build alone is not sufficient. Required: behavioral tests, runtime play session, mobile input evidence, frame pacing, first-session comprehension and final product screenshots.
