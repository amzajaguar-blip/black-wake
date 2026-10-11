# BLACK WAKE — PHASE 1 DESIGN SPEC / IMPLEMENTATION CONTRACT

Status: **DESIGN APPROVED FOR GLM IMPLEMENTATION; PHASE 1 GATE NOT YET PASSED.**
Scope: native Android game at this repository revision. All numbers below are initial balance targets, not claims of measured player outcomes. GLM must return simulation, runtime, and play evidence before a PASS verdict. Phase 2 remains closed.

## 1. Current design forensics

| Requested system | Observed implementation | Player problem | Verdict |
| --- | --- | --- | --- |
| Emergency Power | No separate state, action, or button. The top throttle band is labelled `SPINTA`; speed ranges from `0.8` to `1.2 × boat.speed`, while fuel cost rises quadratically and detection rises at high throttle. | The supposed emergency option is just a continuous slider extreme. It has no distinct escape event or tactical timing. | **REBUILD** the top band as a bounded emergency burst; keep the lever. |
| Flare (`RAZZO`) | Three charges. Usable surfaced, outside its five-second visual timer. Instantly puts every DETECTED/PURSUIT/INTERCEPT pursuer in SEARCH. It lights the scene and removes hunting fog. It does not raise detection or summon investigation. | Its effect is global and unpriced in pursuit pressure. With loud throttle, SEARCH immediately reacquires on the next simulation step, so the advertised escape can be false. | **REBUILD** pursuit effect; **KEEP + POLISH** the light and limited charges. |
| Damage Control | No action, state, or HUD control. Hull is only lost to impact or fuel exhaustion; hull upgrades increase capacity/reduce impact. | Damage has no mid-run recovery decision. A repair button cannot be evaluated as current behavior because it does not exist. | **REBUILD** as a deliberate, risky field repair. Do not represent this as a bug in existing code. |
| Sonar | Automatic ping only while `run.hunted`, initially ready, then cooldown `6 - radarLevel` seconds. Ring travels 110 units at 80 units/s; it highlights hazards and vibrates on hazard crossings. No player control, detection cost, or pursuer response. | It provides warning only after pursuit begins and gives no choice about when revealing information is worth exposure. | **REBUILD** as a manual active ping; remove automatic firing. |

Code anchors: [GameSimulation.kt](app/src/main/java/com/blackwake/game/GameSimulation.kt) (throttle, flare, sonar, detection, fuel, damage); [PursuerAI.kt](app/src/main/java/com/blackwake/game/PursuerAI.kt) (SEARCH/reacquire); [Hud.kt](app/src/main/java/com/blackwake/game/Hud.kt) (actual controls); [SeaRenderer.kt](app/src/main/java/com/blackwake/game/SeaRenderer.kt) (fog and ping visibility); [GameModel.kt](app/src/main/java/com/blackwake/game/GameModel.kt) (run state). Existing unit tests cover flare transition and sonar cadence, but do not prove player comprehension or tactical value.

### Dominant strategy and fake behavior analysis

The low lever band is the likely dominant baseline: it accelerates detection decay (`0.16/s` before upgrades), uses approximately `0.5 fuel/s` at zero throttle before boat modifiers, and enables the silent Intel combo. The full band reaches only `1.2 × boat.speed` while using approximately `6.6 fuel/s` and adding detection. These are code-derived values, not a measured win-rate comparison. More time at low speed can still expose the player to hazards and attacks; GLM must test whether that offsets the benefits.

Flare has a fake escape promise: `dazzle()` sends a pursuer to SEARCH, but SEARCH returns to PURSUIT when the player is not silent/submerged or detection exceeds `0.6`. A loud player can lose the flare effect within one frame. The five-second timer controls lighting and reuse, not a five-second pursuit break. Flare also brightens hazards already rendered and removes fog; these effects overlap Sonar's visual warning. The proximity hazard tone warns without either ability. Sonar's radar upgrade changes an automatic cadence, not a player decision. Current damage feedback says impact occurred but offers no tactical response beyond continuing to steer.

### Solutions considered and rejected

1. Make high throttle simply faster/cheaper: rejected because a rational player would hold it whenever safe; this worsens the low/high binary.
2. Give Flare a longer universal stun: rejected because it would become a guaranteed reset against every pursuer.
3. Keep automatic Sonar and add a manual override: rejected because free automatic information undermines timing and creates two nearly identical warnings.
4. Add passive hull regeneration: rejected because it rewards waiting and erases the cost of navigation mistakes.

Selected design: one limited emergency burst through the existing lever, one directional escape tool, one risky repair action, and one deliberate information pulse. The four choices share detection and positioning consequences but solve different moments.

## 2. Tactical trade-off matrix

| Choice | Good when | Bad when | Visible cost | Counterplay / failure |
| --- | --- | --- | --- | --- |
| Emergency Power | An intercept is closing, a gap or extraction window requires immediate distance. | Detection is near alarm, fuel is scarce, a dive is needed soon, or a narrow hazard lane is ahead. | Rapid fuel loss, signature spike, heat lock, temporary Dive lock. | New pursuit can spawn; locked enemy ram still requires steering; overheat leaves cruise speed. |
| Flare | A visible pursuer has a firing/ram line and the player has room to change lane and quiet down. | No pursuer has line of sight, detection is high, or there is no escape lane. | One of three charges, signature spike, visible last-known flare lane. | Search/investigation returns to that lane; Hunters investigate more quickly; noisy flight reacquires. |
| Damage Control | Hull is low, a quiet open stretch exists, and fuel can be spared. | A ram is imminent, fuel is needed for extraction, or mines crowd the path. | One of two kits, 12 fuel, three seconds of reduced speed and raised signature. | Impact interrupts work; partial repair only; no invulnerability. |
| Sonar | Fog or uncertain lanes hide a mine, wreck, Intel, or pursuer approach. | The lane is already clear, detection is near a spawn threshold, or the revealed object cannot be acted upon. | Detection pulse, six-second cooldown. | Pursuers investigate the ping origin; only present contacts are revealed; new spawns are not foretold. |

## 3. Interaction matrix

| Source → target | Emergency Power | Flare | Damage Control | Sonar | Dive / enemies / environment |
| --- | --- | --- | --- | --- | --- |
| Emergency Power | Burst ends in lockout. | Can be fired after burst; its noise still exposes the boat. | Cancels repair, retaining only completed repair. | Ping is permitted, but both signatures accumulate. | Dive unavailable during burst and two seconds after; fast movement shortens hazard reaction time; distance from current pursuers grows. |
| Flare | A burst after flare can escape the marked lane but creates another signature. | No stacking while light is active. | Can buy a repair opening only after pursuit has actually been broken. | Sonar during flare is usually redundant visually; both signatures accumulate. | Surface only; visible pursuers break lock for two seconds; Hunters investigate flare lane; fog is lit for five seconds. |
| Damage Control | Repair cannot begin during burst/heat lock. | Flare may suppress a ram while repairing, but investigation still threatens. | Only one repair action at a time. | Ping can be fired, with both signature costs. | Surface only; stationary/slow work remains vulnerable to hazards and active enemies. |
| Sonar | Gives an early lane read before burst. | Helps identify whether lighting a lane is worth a charge. | May identify a safe repair corridor. | No repeat during cooldown; no stacking contact lifetimes. | Reveals currently spawned hazards, Intel, and pursuer lane through fog; acoustically alerts active pursuers. |

These are Phase 1 ability interactions. Enemy archetype identity, chapter rules, and route graphs remain Phase 3/4/2 work respectively.

## 4. System specifications

### SYSTEM: Emergency Power (`SPINTA`, top lever band)

**PLAYER PURPOSE:** Trade stealth and future mobility for immediate distance. It should solve a timed pursuit or navigation gap, then force a different next move.

**CURRENT FAILURE:** The lever's top band has no burst state, limited duration, heat, or distinct pursuit interaction. Full throttle has extreme fuel use for its speed gain. The low band also grants the silent Intel combo.

**NEW RULES:** Crossing upward into throttle `>0.8` starts one burst only if fuel `≥12`, the boat is surfaced, and heat lock is zero. During burst, effective speed is `1.70 × boat.speed`; effective lateral steering force is `0.80 × normal`; fuel drains `4.5/s × boat.fuelUse`; detection gets `+0.08` once at ignition and `+0.12/s` while active, after ordinary detection calculation. Current pursuers move 18 track units/s farther behind, capped at `y=110`; this never cancels a ram already inside its 0.6-second locked windup. The burst ends at 2.5 seconds, lever release to `≤0.8`, fuel exhaustion, or run end. Ending starts seven seconds of heat lock and two seconds of Dive lock. While lever remains high during heat lock, effective throttle is clamped to `0.5` and the HUD says `MOTORE CALDO`; the player can lower it normally. Do not retrigger until the lever has left and re-entered the top band after lock expiry. No extra button or resource bar.

**GOOD USE CASE:** A pursuer closes from behind while an open lane leads to an Intel cluster or extraction.

**BAD USE CASE:** A mine is close or detection is `0.65+`; the ignition spike risks a new pursuer and the Dive lock removes an escape.

**COST:** Fuel, signature, steering precision, heat recovery, temporary Dive loss.

**COUNTERPLAY:** Existing detection thresholds still trigger pursuit/waves. A pursuer with a committed ram retains that attack. Hazards still damage on contact; rapid forward travel makes them arrive sooner.

**FAILURE STATE:** The boat overtakes a safe steering opportunity, runs dry, or exits burst into heat lock while still visible. Show which cause ended it.

**UX FEEDBACK:** Top lever band reads `SPINTA 2.5s` while active, then `CALDO Ns`. Engine audio rises on ignition and drops clearly on lockout; one short haptic on ignition, two on forced overheat. Dive button shows a numeric `BLOCCO Ns` and rejects presses without consuming oxygen.

**BALANCE PARAMETERS:** `0.8` activation threshold; `1.70×` speed; `0.80×` lateral force; `4.5 fuel/s`; `+0.08` ignition detection; `+0.12/s` active detection; `2.5s` maximum; `7s` heat lock; `2s` Dive lock; `12` minimum starting fuel. Values apply before boat modifiers where stated and are tunable only with documented comparison evidence.

**EDGE CASES:** Release during first frame still pays ignition cost. A submerged boat cannot start. Fuel reaching zero ends burst immediately. Pause freezes all timers. Death/extraction cancels effects. Holding top lever through cooldown cannot auto-start another burst. Threshold crossing with noisy touch input must not create multiple starts.

**TEST CASES:** One ignition per threshold crossing; exact maximum duration; fuel and detection deltas at fixed `dt`; current pursuer displacement; committed ram unchanged; Dive lock expiry; frame-rate invariance at 30/60/120 fps; zero-fuel and pause behavior.

**ACCEPTANCE GATE:** In runtime, at least one seeded escape scenario is easier with a well-timed burst, and one high-detection/mined scenario is worse with it. The player can see both benefit and bill before making a second choice. GLM supplies traces and video for both.

### SYSTEM: Flare (`RAZZO`)

**PLAYER PURPOSE:** Break a visible attack long enough to change lane and disappear; use a scarce charge to create an escape window.

**CURRENT FAILURE:** The global SEARCH transition does not ensure even a brief escape for a loud player and has no simulation signature cost. Five-second light can be mistaken for five-second safety.

**NEW RULES:** Retain three charges/run, surface restriction, five-second light/fog clearing, and no re-fire during lighting. At launch, affect only active visual pursuers within 55 track units of the player's row (`abs(p.y - PLAYER_Y) ≤55`), not distant or already lost contacts. Cancel an affected uncommitted intercept and set SEARCH at the player's launch lane. Affected pursuers cannot reacquire for two seconds; after that existing reacquisition rules apply. A ram already in its committed 0.6-second windup remains committed and must be steered/dove around. Add `+0.18` detection immediately; set an investigation marker at the launch lane lasting four seconds. Existing active pursuers in SEARCH steer toward that marker; Hunters do so at `1.5×` SEARCH steering rate. A new pursuer spawned from the signature uses the marker as its initial target. The marker is a temporary position cue, not a new enemy or chapter mechanic. Light does not make the player invisible.

**GOOD USE CASE:** A close pursuer is still tracking and there is a clear adjacent lane; flare, change lane, reduce throttle.

**BAD USE CASE:** The attacker is already committed, the player cannot change lane, or detection is close to `0.7`/`0.91`.

**COST:** One charge and `+0.18` detection; the launch lane becomes an enemy investigation point.

**COUNTERPLAY:** Committed ram survives; Hunters converge on the launch lane; loud throttle after the two-second grace period reacquires.

**FAILURE STATE:** The flare lights the sea but does not stop a committed ram or a newly spawned contact. Never display `INSEGUITORI ACCECATI` unless at least one pursuer actually lost an uncommitted lock.

**UX FEEDBACK:** Distinct `LOCK ROTTO 2s`, `RAM GIÀ IMPEGNATO`, or `SOLO LUCE` feedback. Show a brief amber marker at launch lane and charge count; no safety colored overlay. Audio burst and haptic confirm launch; a different low cue indicates no lock was broken.

**BALANCE PARAMETERS:** `3` charges, `5s` light, `2s` reacquire grace, `4s` investigation, `+0.18` detection, `55` track-unit reach, `1.5×` Hunter search steering.

**EDGE CASES:** No target still consumes a charge because lighting/fog clearing is useful, with explicit `SOLO LUCE`. Search-only pursuers do not reset their search timer. Multiple visible pursuers may be affected, but each preserves its own search origin. Submerged or zero-charge action is disabled. Signature can cross a spawn threshold in the same step; the new contact is not dazzled by a prior launch.

**TEST CASES:** Uncommitted attack breaks for two seconds; committed ram persists; distant target unaffected; Hunter investigation; no-target charge and message; signature threshold spawn; repeated tap cannot double spend; transition after light expires.

**ACCEPTANCE GATE:** A deliberate flare-and-lane-change saves a run in a seeded case; firing in place at high throttle fails or creates a worse chase. Runtime feedback correctly distinguishes both.

### SYSTEM: Damage Control (`CONTROLLO DANNI`)

**PLAYER PURPOSE:** Trade time, fuel, and concealment for a small recovery after a navigational mistake. It creates a reason to seek a quiet corridor at low hull.

**CURRENT FAILURE:** No such action exists. Hull loss is one-way, so there is no repair decision to audit or keep.

**NEW RULES:** Add two field kits/run, shown only as `x2/x1/x0` on one action button. Repair can start surfaced when hull is at least 10 below maximum, fuel is at least 15, throttle is `≤0.3`, Emergency Power and heat lock are inactive, invulnerability is zero, and no repair is active. Starting spends one kit and 12 fuel. Repair runs three seconds, locks effective throttle to `0.2`, and adds `+0.06/s` detection. Hull recovers linearly up to 18 points, capped at max. Dive and Emergency Power cannot start while repairing. A damaging collision or ram interrupts immediately; completed partial hull stays, uncompleted repair is lost, kit/fuel remain spent. Voluntary movement of lever above `0.3` cancels with the same partial result. Steering stays available. If fuel reaches zero during repair, cancel immediately with partial progress only.

**GOOD USE CASE:** After damage, a clear low-threat stretch has enough fuel and distance for three seconds of work.

**BAD USE CASE:** An intercept is winding up, a mine is near, or fuel is just enough to reach extraction.

**COST:** One kit, 12 fuel, slower travel, `+0.18` detection across a full repair, blocked Dive/Power during work.

**COUNTERPLAY:** Pursuers continue attacks; hazard contact interrupts. The player must choose a safe time and lane. Repair has no invulnerability and does not cancel pursuit.

**FAILURE STATE:** A rushed repair restores only its completed fraction and spends the full kit. The interruption reason must appear in the feed.

**UX FEEDBACK:** Button displays kit count and a three-second progress ring; hull bar animates by actual repaired amount. Start/cancel/complete have distinct short audio cues; impact interrupt uses damage haptic. Button disable reason (`CARBURANTE`, `IN CACCIA` only if actual restriction is added, `TROPPO VELOCE`, `SCAFO INTEGRO`) must match rules; do not imply pursuit alone blocks repair.

**BALANCE PARAMETERS:** `2` kits, `15` minimum fuel to start, `12` fuel/start, `3s` duration, `18` maximum hull, `+0.06/s` detection, throttle start `≤0.3`, effective repair throttle `0.2`, minimum missing hull `10`.

**EDGE CASES:** No negative fuel, no over-heal, no kit refund on interrupt, no double tap start, death cancels repair, pause freezes progress, damage on final frame interrupts before completion, fuel exhaustion after paying start cost cannot grant free completion. If both collision and repair would complete in the same frame, resolve collision first.

**TEST CASES:** Start guards; one resource spend; linear partial heal; hit/lever cancel; max hull cap; no invulnerability; pause and frame-rate invariance; exact collision/finish ordering.

**ACCEPTANCE GATE:** A quiet corridor repair improves survival in a seeded case; the same action under active threat loses a kit or increases detection enough to hurt. In both, the player understands why.

### SYSTEM: Sonar (`SONAR`)

**PLAYER PURPOSE:** Buy a short, precise read of uncertain water before choosing a lane or timing another ability.

**CURRENT FAILURE:** It fires automatically only after pursuit and costs nothing. Hazard outlines overlap Flare light and proximity tone. Radar upgrade changes frequency but not strategy.

**NEW RULES:** Remove automatic ping initiation. Add one manual Sonar button, usable surfaced or submerged, when cooldown is zero and no ping is active. Ping costs `+0.12` detection immediately even submerged; sound propagates for `1.375s` at current `80 units/s` to `110` range. At activation, snapshot only currently spawned entities and pursuers within range. For three seconds, show their current lane and type as compact contacts: mine, wreck, enemy, Intel, fuel, pursuer. Contacts track those entities while alive; they disappear when collected/despawned and do not include later spawns. The existing hazard haptic fires only when the wave first reaches a hazard, with a single pulse per hazard ID. Cooldown is six seconds from activation; radar levels may shorten it to a floor of four seconds (`6 - min(radarLevel,2)`), pending Phase 6 progression redesign. Active pursuers investigate the ping origin for two seconds if they are searching; pursuers already attacking do not lose attack state. A ping does not alter damage or collection rules.

**GOOD USE CASE:** Fog masks a hazard/Intel lane with enough distance remaining to steer.

**BAD USE CASE:** The approaching lanes are visually clear or detection is one ping below pursuit/wave threshold.

**COST:** `+0.12` detection, temporary enemy investigation, six-second base cooldown.

**COUNTERPLAY:** New spawns remain unknown. An existing pursuer can use the ping origin. Revealing a mine does not remove or slow it.

**FAILURE STATE:** Ping near `0.7` detection may spawn an enemy; a late ping reveals a hazard too close to evade. Feedback must identify the signature increase.

**UX FEEDBACK:** Button says `SONAR`, `PRONTO`, or remaining cooldown seconds. Ring expands, then contact glyphs persist three seconds. Immediate signature bar pulse/arrow `+12` and audio ping; hazard haptic is shorter than collision haptic. Contacts must be readable under fog and color-blind safe by shape/type symbol.

**BALANCE PARAMETERS:** `+0.12` detection, `110` range, `80 units/s` propagation, `3s` contact memory, `6s` base cooldown with `4s` minimum, `2s` search investigation.

**EDGE CASES:** Ping in empty water still incurs cost and cooldown. Starting a ping with no fuel is permitted because Sonar is electrical; no new battery bar. Pause freezes wave, contact memory, and cooldown. Multiple entities entering range after activation are not revealed. Dead/despawned contacts vanish immediately. Repeated taps before cooldown do nothing.

**TEST CASES:** Manual activation only; cost at activation; threshold-triggered spawn; snapshot excludes later spawns; glyph lifetime; hazard haptic once/ID; pursuer investigation without attack reset; surfaced/submerged use; no double activation; 30/60/120 fps equivalence.

**ACCEPTANCE GATE:** In fog, a timely ping changes a lane or Intel choice in a seeded scenario. In clear water near alarm, ping produces a visible, avoidable penalty. Video must show that the information arrives before the decision point.

## 5. Exploit scenarios GLM must defeat

| Exploit attempt | Required outcome |
| --- | --- |
| Keep lever above `0.8` through heat lock | No automatic second burst; effective cruise clamp and visible lock remain. |
| Oscillate lever across `0.8` rapidly | One ignition per valid re-entry; no duplicate bonus or free signature bypass. |
| Flare during committed ram | Ram remains; player must steer or dive. |
| Flare, stay loud in same lane | After two-second grace, pursuers investigate/reacquire; no permanent reset. |
| Fire Flare and Sonar together | Both detection spikes apply; no cap suppresses a cost. |
| Tap repair twice or cancel instantly | One kit and 12 fuel spent; no free hull. |
| Repair during invulnerability after a hit | Cannot start until invulnerability ends. |
| Repair finishing exactly as damage lands | Collision interrupts first; partial heal only. |
| Spam Sonar while submerged | Same signature cost and cooldown; submerged state is not an acoustic shield. |
| Ping before random spawn then claim foreknowledge | Later entity is never added to that ping's contact snapshot. |

## 6. BLACK WAKE — PHASE 1 IMPLEMENTATION CONTRACT

**OBJECTIVE:** Make four tactical decisions legible and situational. The player should recognize speed versus stealth, pursuit versus concealment, information versus exposure, and repair versus survival time.

**PLAYER PROBLEM:** Existing low-throttle play is heavily rewarded; Flare's escape promise is inconsistent; Sonar is automatic; hull damage has no recovery choice.

**SYSTEMS TO CHANGE:** Top throttle band, pursuit interaction hooks, Flare, Sonar, hull recovery, mission HUD, simulation state, audio/haptic event cues, and behavior tests for those rules.

**SYSTEMS NOT TO TOUCH:** Sector graph/fork rewards, chapter content, enemy roster/stat tuning beyond listed investigation hooks, Intel economy/progression, store/billing/ads, menu/debrief, and tutorial sequencing. Existing tests that assert old automatic Sonar behavior must be revised to assert the new contract.

**REMOVE:** Automatic Sonar trigger, global no-cost Flare reset, and the implication that a five-second flare light is five seconds of safety. Do not add a separate Emergency Power button.

**KEEP:** Three-lane steering, baseline throttle below top band, Dive oxygen rules except explicit temporary locks, three Flare charges, existing flare lighting, pursuit state machine, existing fog and hazard audio as secondary cues.

**NEW BEHAVIOR:** The four system sections above are normative. A visible cost must be applied in simulation at the instant of the action. No UI-only cost or undocumented automatic effect.

**STATE MODEL:** Add burst duration/heat/re-entry latch/Dive lock; per-pursuer flare reacquire grace and investigation marker; repair kits/progress/active flag; Sonar contact snapshot IDs and expiry. All state belongs to `RunState`/`Pursuer` and resets on a new run. Use deterministic simulation time, not wall-clock timers.

**RULES:** On each frame: validate requested actions → apply one-time costs → compute movement/detection and enemy updates → resolve collisions → update repair progress if still valid → resolve death/sector completion → publish feedback. Signature spikes must participate in detection thresholds in the same frame; collision wins ties against repair completion. Document any existing event-order conflict explicitly in the implementation report.

**BALANCE CONSTANTS:** Use values in the four system sections. Keep them named, centralized, and logged in the evidence. Changes require a reason tied to outcome comparisons, not feel alone.

**EDGE CASES:** Use all listed per-system cases. Actions never apply to an already ended run. Pause freezes timers. UI enablement and simulation validation must agree. Simultaneous touch input must have deterministic priority and never duplicate costs.

**UX REQUIREMENTS:** No more than two new circular controls: Repair and Sonar; top lever remains Emergency Power. Show immediate, distinct good/bad feedback and numeric cost where relevant. At 30 seconds, a new player should be able to identify quiet, burst, and at least one tactical choice without opening a menu; this requires runtime observation rather than a code assertion.

**AUDIO/HAPTIC REQUIREMENTS:** Distinguishable confirmation for burst, flare, repair, and ping; danger cue when an action creates pursuit; no long haptic for information. Muted audio must preserve visual feedback. Respect existing mute and haptic handling.

**TEST MATRIX:** Each ability needs one beneficial and one harmful fixed-seed scenario, guard tests, cost tests, collision/threshold interaction tests, pause/reset tests, and 30/60/120 fps consistency checks. Capture input sequence, initial state, key state transitions, final hull/fuel/detection/Intel, and a short runtime video or screenshots of HUD states.

**FAILURE TESTS:** Near-alarm burst/ping, Flare into committed ram, repair into damage, empty-water Sonar, no-target Flare, zero-fuel burst, and action at run end.

**EXPLOIT TESTS:** The ten attempts in section 5, including simultaneous costs and threshold-edge presses.

**ACCEPTANCE CRITERIA:** All four systems satisfy their individual gates; UI and simulation agree; beneficial and bad use cases differ measurably; no ability is free or efficient in every tested situation; failure feedback identifies the missed choice. A green build alone is insufficient.

**FILES LIKELY INVOLVED:** `GameSimulation.kt`, `GameModel.kt`, `PursuerAI.kt`, `GameViewModel.kt`, `Hud.kt`, `SeaRenderer.kt`, `SynthAudioEngine.kt` or audio event routing, and relevant simulation/UI tests. File list is guidance, not license for unrelated refactoring.

**EXPECTED PLAYER-FACING RESULT:** Burst creates a tense escape with a painful aftermath; Flare buys a real but brief break; Repair is a risky recovery choice; Sonar reveals actionable uncertainty at a detection price. After failure, the player can point to a specific mistimed choice.

**RELEASE / MONETIZATION EFFECT:** Phase 1 is a design and gameplay gate only. It does not authorize monetization or release readiness. Monetization remains blocked pending later device evidence.

### Evidence required from GLM for review

Return the implementation diff, exact build/test commands and results, raw fixed-seed traces for every good/bad case, screenshots or a short capture of all four HUD states, and any value changed from this contract with a reason. Include an exploit table with observed outcomes. GPT Design will return exactly one verdict: `PASS`, `PASS_WITH_FOLLOWUPS`, `FAIL_IMPLEMENTATION`, `FAIL_DESIGN`, or `INSUFFICIENT_EVIDENCE`.
