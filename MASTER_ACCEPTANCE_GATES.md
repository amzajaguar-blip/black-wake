# BLACK WAKE — MASTER ACCEPTANCE GATES

Status: **Phase 1 LOCKED_FOR_GLM; Phases 2–10 PROVISIONAL; Phase 11 EVIDENCE_DEPENDENT/PENDING_DEVICE; Phase 12 MONETIZATION_LOCKED.** This file forecasts evidence and pass conditions. It is not a set of implementation contracts. [Phase 1 spec](PHASE_1_DESIGN_SPEC.md) alone is authoritative for the next GLM delivery.

## Evidence standard at every gate

GLM returns the implementation diff, test commands and raw results, fixed-seed before/after traces, representative runtime capture with UI/audio, and an exploit table. GPT Design checks whether the specified decision changed in actual play. A green build or descriptive claim cannot pass a design gate. Gate verdict is exactly one of `PASS`, `PASS_WITH_FOLLOWUPS`, `FAIL_IMPLEMENTATION`, `FAIL_DESIGN`, `INSUFFICIENT_EVIDENCE`. A failed gate blocks locking the next phase. Values below are initial acceptance targets where noted; evidence can veto them.

| Phase | Gate to lock the next phase | Required evidence and likely redesign trigger |
| --- | --- | --- |
| **1 Core tools** | Each tool has a reachable good use, bad use, visible cost, enemy/environment reaction and understandable feedback. | Four paired fixed-seed good/bad scenarios, input/state traces, HUD capture, edge/exploit tests from Phase 1. Redesign if quiet-only or tool-spam dominates. |
| **2 Routes** | Two runs through one chapter on different branches have different future sectors, constraints, encounter mix, Intel opportunity, and extraction approach. | Same build and paired seed; graph trace, route ID persistence, branch-specific video, no strictly dominant branch. Pilot Chapter 01 first. |
| **3 Enemy identity** | All six archetypes are identified by telegraph and require different efficient responses; no strategy beats all. | Six A/B response comparisons, telegraph capture before commitment, 12 fair pair encounters, novice recognition notes. Redesign if types differ only numerically. |
| **4 Chapter identity** | A tester can identify a chapter from a text-free description of play rules; chapters change decisions. | Eight rule demonstrations, route fairness, extraction differences, no unavoidable geometry. Redesign any chapter that is only skin/visibility multiplier. |
| **5 Narrative runtime** | At least three meaningful reactions per chapter; each is true, timely, sparse and useful/characterful. | Event logs matching spoken/printed line, silence/cooldown tests, mixed-event priority capture. Redesign if radio masks hazard cues or states false facts. |
| **6 Progression** | At least three starter identities can clear the campaign; two builds make visibly different choices in the same mission; no universal best pair. | Paired-seed play comparisons, economy award ledger, card disadvantages, free respec flow, save migration test. Redesign stat-only/mandatory card. |
| **7 Debrief/rank** | Victory and failure each yield one specific retry goal; every rank component is visible and sums exactly; no score exploit. | Recomputed score from raw event log, representative S/A/B/C/failure captures, player explanation test. Redesign opaque or farmable component. |
| **8 Challenges** | Same code reproduces same operation; legal modifiers change decisions; seed is fair; score is bounded. | Cross-device/version reproducibility, 20 validated seeds × 8 chapters × 12 legal modifier pairs or explicit smaller pilot, anti-farm/restart tests. Redesign impossible or trivial pairs. |
| **9 First-session UX** | A newcomer can steer, collect Intel, understand signature danger and use one relevant tactic without reading a long guide. | Action-gated tutorial recordings; first 10/30/60-second observations; HUD legibility and thumb-occlusion capture on device. Redesign after repeated accidental input. |
| **10 Performance** | Pursuit, input, HUD, audio and simulation remain responsive on target devices. | Actual frame-time distribution, input-to-visual timing, audio cue timing, long dense encounter memory/GC trace, 30/60/120 Hz simulation equivalence. No desktop-only substitute. |
| **11 Device validation** | Real sessions show comprehension, retry intent, variety and stable play across 10/30/60 minutes; no fabricated data. | Protocol below, raw notes/logs, failure and boring-segment timestamps, route/build choices. Any unresolved severe usability/crash issue blocks monetization. |
| **12 Monetization** | Only after Phase 11 PASS, optional placements preserve score and player trust. | Consent, cancellation, duplicate-callback, offline/restore and no-advantage tests under then-current store rules. No Phase 12 work now. |

## Phase 5: runtime Maya and radio design forecast

Event-driven line priorities: imminent collision/intercept warning > extraction instruction > route consequence > Intel threshold > character color. Maximum one non-critical line every 20 seconds, no overlap with a critical cue, and no repeat of the same event line in one run. Subtitle/text remains available if audio is muted. A line must name a fact known to the current run state or present uncertainty honestly. Previous chapter decisions can alter one line and briefing clue, not secretly change a core rule.

| Chapter | Three minimum reaction classes tied to play |
| --- | --- |
| 01 | Route choice acknowledged; first sustained pursuit explained; extraction threshold secured. |
| 02 | Container echo after Sonar/Push; ledger damage or recovery; customs gate crossed. |
| 03 | Tide route selected; shallow Dive attempt warned; bell-cove Hunter evaded. |
| 04 | Storm blind ping uncertainty; capsule Intel recovered; calm transmission window reached/missed. |
| 05 | False radio clue contradicted by physical evidence; verified cache marked; unverified extraction explicitly identified. |
| 06 | Surveillance arc spotted; service gate timing warned; network node secured. |
| 07 | Elite feint recognized; Kross decoy chase cautioned; recording secured without chase or lost to it. |
| 08 | First uplink completed; route-specific second-window risk announced; final data secured or incomplete. |

Lines are design functions, not script text yet. Phase 5 must write concise Italian lines after underlying triggers exist. Runtime narrative is a tactical layer and character acknowledgment, never constant exposition.

## Phase 9: first-session and HUD forecast

Action-gated sequence: **move** (detect deliberate lane change) → **collect Intel** (actual pickup) → **quiet** (reduce throttle enough to lower signature for a visible interval) → **escape** (use Dive or Flare successfully against a telegraphed threat) → **manual information** (Sonar when an uncertain lane exists). No lesson advances on a fixed timer. If a player misses a pickup, the safe opportunity repeats without rewarding repeated farming. Optional hints fade after successful action and can be revisited from pause.

| Window | What the player must see/do | Device observation question |
| --- | --- | --- |
| 0–10 s | Steer toward a visible Intel case while a patrol searchlight makes lane selection matter. | Can the player move without hunting for a control? |
| 10–30 s | Observe wake/signature change, choose quiet or speed under threat, hear a short Maya reaction, see a branch and optional Sonar clue. | Can the player state why the patrol found them and what Sonar would cost? |
| 30–60 s | Resolve a real pursuit with one tool, collect or sacrifice Intel, commit to a route. | Does the player understand the consequence and one alternative? |

HUD hierarchy: **primary** lane hazards, pursuit telegraph, player position, throttle/Dive/Flare reach; **secondary** signature, fuel, hull, oxygen, repair/Sonar availability; **tertiary** chapter title, route log, rank/meta progress, debug data. Never cover the next steering gap with radio text. On landscape phones, important controls must remain clear of thumb and cutout; exact sizes/placement are **PENDING_DEVICE**.

## Phase 10: player-facing performance targets

On the agreed low and mid target Android devices, aim for a 60 Hz presentation with frame time p95 ≤20 ms and p99 ≤33 ms during dense pursuit; no visible multi-frame hitch at a tool press or spawn. Aim for input-to-visible-steering response ≤100 ms and tactical sound onset ≤100 ms from accepted action, measured rather than guessed. HUD change should appear by the next presented frame after accepted input. A 30-minute dense run should show no sustained memory growth or GC pause above one visibly skipped frame. Simulation outcomes must match within a documented tolerance across 30/60/120 Hz step schedules for fixed seeds and inputs. These are **PROVISIONAL** targets until device profiling; GLM chooses technical implementation.

## Phase 11: real-device validation protocol

Recruit at least eight first-time players for 10-minute sessions; at least six players for 30-minute sessions; and at least four for 60-minute sessions, with overlap allowed but reported. Include both a low-end supported Android device and a representative mid-range device; record model, OS, build hash, control layout, audio state, session duration, and prior familiarity. Do not fill gaps with emulator claims. Sessions should be unprompted after the first action-gated tutorial; observer notes should not coach tactics.

Record crashes/ANRs, frame pacing, first misunderstanding, accidental touches, thumb obstruction, repeated strategy, boring segments, rage quit, voluntary retry, preferred route/build, and the player's explanation after failure. Preserve timestamps and anonymous participant IDs. Observe whether 10-minute novices can explain one trade-off, whether 30-minute players choose alternatives, and whether 60-minute players voluntarily retry or master an operation. Ask for a next-day voluntary return check without push notification. A same-day session alone cannot prove next-day retention.

Provisional decision bar: zero crashes or lost progress, no repeated unavoidable death, at least 6/8 novices can state a meaningful tactical trade-off by minute one, at least half of players who fail choose a voluntary retry, and no single route/build/policy dominates all observed sessions. Small samples cannot establish population retention; use these as release-risk signals, then expand testing as needed. **MONETIZATION_GATE stays BLOCKED** until qualitative comprehension and stability pass and the next-day follow-up has actual results. Do not invent missing data.

## Phase 12: conceptual monetization boundary

The game is free to start. Possible future optional rewarded ad locations are **after a failed-run debrief** for a cosmetic dossier reveal or replay commentary, and **after a completed challenge** for a cosmetic archive item. Neither grants EC, hull, Flare, Sonar, score, rank, seed rerolls, or an in-run continuation. A Supporter/Remove Ads purchase can remove those optional prompts and add non-gameplay presentation items. A possible Premium Operations Pack may add side-story operations with the same base tools and separate ranks; it must not gate main campaign completion or competitive challenge parity.

No gameplay banner, timed interstitial, forced run-end ad, paid core upgrade, purchased rank, artificial fuel scarcity, or reward tuned to make ads attractive. All placements remain **MONETIZATION_LOCKED** until Phase 11 passes and then-current platform requirements are reviewed. A weak optional ad proposition is acceptable; weakening the game to make it appealing is not.
