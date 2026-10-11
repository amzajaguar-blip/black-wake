# BLACK WAKE — PROGRESSION, RANKING AND RETENTION MODEL

Status: **PROVISIONAL forecast for Phases 6–8.** Economy and duration values are planning targets, not player evidence. Phase 1 remains governed by [its sole implementation contract](PHASE_1_DESIGN_SPEC.md).

## Two layers of Intel

**Mission Intel** is a finite set of recoverable evidence objects in a run. It may be lost or unverified before extraction. **Secured Intel** is the subset successfully transmitted and is used for mission completion, rank, and narrative acknowledgement. **Evidence Credits (EC)** are a separate meta currency awarded for finite achievements. The player must see these as distinct labels; collecting the same mission object repeatedly never creates infinite EC.

The current `intelBank += bankedIntel` model is a grind incentive and is slated for Phase 6 replacement after upstream gates. No progression currency is sold. Story chapters unlock by prior chapter completion, regardless of purchases or EC balance.

### EC source and sink forecast

| Source | Award | Anti-farm rule |
| --- | --- | --- |
| First successful clear of each of 8 chapters | 4 EC | Once per chapter, tied to stable completion record. |
| First successful extraction on each authored route | 1 EC | Once per route ID; 22 maximum; cannot duplicate through restarts. |
| First A-or-better rank per chapter | 2 EC | Once per chapter, including later improvement. |
| First S rank per chapter | 1 EC | Once per chapter. |
| First meaningful failed attempt per chapter | 1 EC | Once per chapter only if at least 25% required Intel recovered and 60s played; suicide/restart gives zero. |
| First verified seeded challenge clear for each chapter | 1 EC | Once per chapter, eight maximum across all dates/seeds; replaying or changing the device clock yields zero. Subject to Phase 8 fairness. |

Six unlockable behavior cards are the EC sinks, priced **7, 8, 9, 10, 12, 14 EC** (60 total). Three starter cards are available immediately. No card levels, consumable purchases, paid upgrade acceleration, or EC cost to respec. Vessel hulls unlock from chapter milestones and are sidegrades; they do not consume EC. Cosmetic achievements, if ever added, must not compete with gameplay clarity.

### Earnings examples

| Run example | Mission Intel / secured example | New EC | Repeat of same result |
| --- | --- | --- | --- |
| Poor run: fails after 60s with 25% objective recovered | 3 / 0 | 1 only if first meaningful failure in that chapter | 0 |
| Average first success on new route | 8 / 8, meeting threshold | 5 = 4 first clear + 1 route | 0 |
| Strong first success, first A | 11 / 10 | 7 = 4 + 1 + 2 | 0 |
| High-rank first success, first S | 13 / 12 | 8 = 4 + 1 + 2 + 1 | 0 |
| New alternate route after chapter clear | Route-dependent | 1, plus first A/S milestones if newly met | 0 for the same route/milestone |
| First verified challenge clear for that chapter | Challenge objective met | 1, once per chapter across all dates | 0 |

Mission Intel values are illustrative; chapter thresholds and route caps must be tuned to actual spawn and extraction rules. A failed run's recovered Intel is visible in debrief but does not become secured Intel. A player who repeatedly clears Chapter 01 receives zero new EC after its finite first-clear, route, and rank milestones; grinding trivial missions is never the optimal way to unlock behavior cards.

### Pacing and commitment

At launch, all three core identities are possible with one starter card each. First affordable card should arrive around Chapter 02 for an ordinary player. After eight first clears on one route each, the player has about **40 EC** before rank/failure/challenge bonuses; completing all six unlocks costs 60 EC. Target all cards after the campaign plus several A ranks or alternate routes, roughly the intended 4–6-hour mastery window. This is a pacing hypothesis to verify, not a promise of measured play time. If progression stretches because ranks are too hard, revise awards or costs; do not sell relief.

The player equips **one vessel + two behavior cards** before a run. No mid-run swaps. Respec is free between runs, including after failure. Cards are never mandatory to complete a story chapter; base vessel and starter cards must pass every mission. The briefing names the likely threat and route clues so loadout is an informed commitment, but the later route decision remains in play.

## Three build identities with nine behavior cards

Every card changes an action, telegraph, or encounter rule and carries a disadvantage. The named starter cards are examples of the target base options; final tuning awaits Phase 6.

| Identity | Card | Behavior gained | Cost / when wrong |
| --- | --- | --- | --- |
| Stealth | **Silent Propeller** (starter) | At throttle `≤0.2`, Patrol loses a clean wake track behind cover. | Turning force falls 20% while quiet; poor in mine corridors and against Interceptor. |
| Stealth | **Wake Cutter** | One lateral lane change after leaving cover leaves a false wake for 1.5s. | Next true lane change is slower; Hunter can read acoustic movement. |
| Stealth | **Deep Drift** | Releasing Dive below 30 oxygen leaves a brief decoy surface wake. | Oxygen recovery starts 1s later; mines and shallow water remain threats. |
| Intelligence | **Signal Decoder** (starter) | One route preview labels a likely enemy and one fixed Intel cache before commitment. | First manual Sonar ping after route entry has a 1s longer cooldown; not useful when route is already known. |
| Intelligence | **Bearing Lock** | Sonar contact markers retain enemy heading for 1s after the ring passes. | Ping detection cost increases by `+0.04`; bad in Hunter waters. |
| Intelligence | **Cache Reader** | First verified cache in a route identifies whether the next cache is authentic. | Cache pickup requires a slightly narrower lateral alignment; risky in dense traffic. |
| Survival/escape | **Reinforced Prow** (starter) | One frontal collision per run is converted from hull damage into a forced lateral bounce. | Ignition and collision make an extra acoustic spike; not a cure for mines from below. |
| Survival/escape | **Heat Sink** | Emergency Power heat lock can end 2s earlier if the player stays in quiet throttle for that whole interval. | During cooling, lateral steering is 15% weaker; cannot chain bursts through a threat cluster. |
| Survival/escape | **Patch Relay** | Damage Control repairs 4 additional hull if completed without interruption. | Repair takes 1s longer and adds its normal signature for that extra second. |

The first three are starters; six others map to the six EC prices in listed order from lower to higher cost after final Phase 6 tuning. These are two-slot card candidates, not nine passive upgrade levels. A mixed build is allowed, but the best two-card pair must change by chapter/route. Use paired seeded comparisons: same route and player policy, each identity must produce a distinct beneficial choice in at least two chapters and a clear liability in another.

## Transparent rank and debrief forecast

A completed chapter earns `S/A/B/C` from a visible 1,000-point ledger: **objective/extraction 300**, **secured Intel up to the announced requirement 200**, **stealth exposure and clean escapes 150**, **hull preservation 150**, **route-specific optional objective 100**, **route-adjusted pace 100**. Proposed thresholds: `S ≥900`, `A ≥750`, `B ≥600`, `C <600`. The exact formula must be published in the Phase 7 contract and shown in debrief. Extra farmed Intel beyond the announced cap adds no score; route-specific pace compares against that route's fair par rather than a universal time. No rank may depend on paid content.

Failure is labelled `MISSION FAILED`, not given a misleading letter rank. It still shows recovered/secured Intel, route, peak detection, damage, tactical mistakes, near misses, and one next objective such as “Hold Sonar until the second blind corner” or “Leave the flare lane before the Hunter returns.” A victory shows personal best and the nearest unmet rank component. The debrief must store event facts, not infer mistakes from final totals alone.

Rank needs a tactical ledger of meaningful events: tool fired and consequence, pursuit acquired/lost, cache collected/secured, route committed, collision, near miss, extraction state. A “mistake” is a contextual comparison with an available alternative, not moral judgment. If the game cannot prove the cause, show the event fact instead.

## Seeded challenges and score integrity

Challenge identity is `rulesVersion:chapterId:seed:modifierPair`. The seed controls fair spawn selection, branch cue placement, enemy schedule, and weather timing; route remains a player choice. The daily operation is derived offline from UTC date plus a published season salt and a prevalidated seed list. The same code reproduces the same operation across devices on the same rules version. Old scores remain labelled by version after balance changes. Changing the device clock may alter the displayed daily operation while offline, but cannot mint additional EC because its one-time reward is keyed to chapter, not date.

Initial modifier pool: `NO FLARE`, `BLACK STORM`, `DOUBLE INTEL / DOUBLE DAMAGE`, `HUNTER WATERS`, `LIMITED SONAR`, `LOW FUEL`, `PERFECT STEALTH BONUS`, `NO REPAIR`. The generator uses only preapproved compatible pairs: never hide mandatory mines without a readable alternative; never combine low fuel with a path that cannot be completed; never require a disabled tool for an extraction. Modifiers must alter decisions, not merely multiply score. Local best and shareable code come first; no online leaderboard dependency.

Challenge score starts from the same transparent rank ledger, then applies a published difficulty multiplier between `1.00` and `1.25` for the verified modifier pair. Rank and reward cannot be manipulated by purchases, ad views, pausing, endless low-speed stalling, extra Intel respawns, or restarts. Time par and fixed cache IDs prevent farming; only the best locally completed result for a challenge identity is retained. If a seed fails fairness validation, it is ineligible for score and daily rotation.

## Retention reasons without timers

| Horizon | Player reason to return | Product signal to observe later |
| --- | --- | --- |
| Immediate, after failure | A specific avoidable error and a nearby alternative route or tool timing. | Voluntary retry, shorter gap to retry, player can name intended change. |
| Same session | Compare route consequences or build response to the same enemy. | Meaningful route/build switches, not only repeated identical attempts. |
| Next day | A new deterministic daily operation and one unfinished mastery target. | Voluntary return without push notification; challenge starts/completions. |
| Longer term | Finish eight dossiers, S ranks, route records, and all behavior cards. | Continued play after campaign completion and diverse chosen loadouts. |

No energy timer, streak penalty, notification dependence, or ad gate. Retention is a hypothesis until Phase 11 observes actual sessions.
