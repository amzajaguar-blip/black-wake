# BLACK WAKE — ROUTE AND CHAPTER BLUEPRINT

Status: **PROVISIONAL, Phases 2 and 4.** No route/chapter behavior in this document is approved for implementation until Phase 1 passes and the next phase is locked. The current game has eight linear chapters and a fork pickup that changes an immediate stat before all lanes rejoin the same next sector; this blueprint replaces that structure.

## Route graph contract forecast

Each chapter has `ENTRY → DECISION → BRANCH A/B[/C] → BRANCH CONSEQUENCE → EXTRACTION`. The chosen route ID persists until debrief. The two branch nodes are gameplay spaces with route-specific geometry, spawn tables, threat rules, Intel placement, and time pressure; they cannot be one reward pickup followed by the same sector. A merge at extraction is allowed only if the extraction approach and objective remember the route. One branch lasts roughly 2–3 minutes; the full chapter aims at 5–7 minutes after dead travel is removed.

The branch preview must disclose one advantage, one cost, one known enemy or environmental warning, and an uncertain element Sonar can help resolve. Intel opportunity is a fixed set of marked cache IDs plus fair random filler; fixed caches cannot respawn. The safe path is slower or offers less Intel, while the fastest path exposes pursuit or a difficult extraction. A route is selected by committing to a navigable lane/approach; no menu selection may override an already crossed gate.

Route IDs and chapter IDs must be stable for seed identity, narrative reactions, rank comparison, and bug reproduction. A selected branch changes at least **two subsequent encounter groups** and **one extraction condition or approach**. Route duration differences target no more than about 30% for the same chapter, unless a clearly signposted risk/reward justifies more.

## Chapter rule signatures

| Chapter | Play rule that should identify it without art | Extraction consequence |
| --- | --- | --- |
| 01 The Wreck | Searchlights make the wake readable; wreck cover cuts visibility but narrows steering. Introduces evidence versus silence. | Transmit one secured case while choosing whether to risk an optional second case. |
| 02 The Smuggler | Container corridors alternate between acoustic shelter and echo chambers; crossing open slips exposes the boat. | Reach an uplink lane while preserving the smuggler ledger, which can be damaged by a collision. |
| 03 Dead Island | Cross-current shifts the apparent lane approach; shallow shelves limit Dive near some Intel. | Leave the bay through one of two tide windows; route determines which is open. |
| 04 Hunt in the Storm | Gust cycles move safe lanes and obscure mines; Sonar may return a noisy/partial read, visibly labelled uncertain. | Align a short transmission with a calm interval while avoiding an exposed storm crest. |
| 05 Betrayal | Radio instructions can conflict with physical evidence. The player verifies a clue by navigating to a signal source, not by guessing a dialogue answer. | Transmit only verified evidence; an unverified cache counts as recovered but not secured. |
| 06 Black Port | Rotating surveillance arcs and closing service gates make waiting, Push, and quiet crossing useful at different times. | Open a brief service gate, then cross its watched lane; prior route changes gate timing. |
| 07 Kross | Dawn removes darkness; enemy lock is easier to acquire but silhouettes reveal attack setup earlier. | Keep distance from Kross's decoy while securing a recording; chasing the boat is a trap. |
| 08 Black Wake | A final storm combines known rules with one predictable variation each, not an untelegraphed pile of hazards. | Secure two sequential uplink windows; route controls which window is safer and which carries more evidence. |

These chapter rules are forecasts. Phase 4 must prove each creates different decisions in play. Visual skin or detection multiplier alone does not count.

## Route variants: 22 authored paths

Legend: `T` = target chapter duration on a successful first clear, including entry and extraction. Enemy names indicate intended emphasis, not guaranteed simultaneous spawn. Each route row specifies a second-sector consequence (`→`) and a later extraction/narrative consequence. All values are provisional.

| Chapter / route ID | Route and future graph | Risk, enemies, Intel, duration | Environment and consequence |
| --- | --- | --- | --- |
| 01 `01-cover` | Wreck cover → dark channel | Patrol search weak; 2 fixed caches; T≈6m | Narrow lateral gap; Maya notes disciplined silence; optional second case lies outside cover at extraction. |
| 01 `01-wake` | Open wake → patrol screen | Patrol + Interceptor; 3 caches; T≈5m | Fast open lanes; easier uplink approach if pursuit was broken, harder if still hunted; Maya notes the visible wake. |
| 01 `01-salvage` | Salvage field → submerged cut | Mines + Patrol; 4 caches; T≈6m | Dive helps against wrecks but misses surface Intel; extraction case requires a final exposed lane change; Maya identifies extra evidence. |
| 02 `02-cranes` | Crane shadow → service quay | Police searchlights; 2 caches; T≈6m | Shelter alternates with open slips; ledger safer from impact; Maya trusts the cautious entry. |
| 02 `02-ledger` | Container core → registry barge | Police + Interceptor; 4 caches; T≈7m | Echo corridor reveals noisy tools; ledger is richer but impact can damage one cache; Maya comments on recovered names. |
| 02 `02-channel` | Customs channel → floodgate | Interceptor; 3 caches; T≈5m | Fast, broad water, little cover; extraction gate is watched if detection remains high; Maya warns about open channel. |
| 03 `03-reef` | Reef edge → tide pocket | Patrol + mines; 2 caches; T≈7m | Cross-current and shallow shelves; late tide window is stable; Maya notes slow route. |
| 03 `03-cove` | Bell cove → smuggler beach | Hunter introduced only when telegraphed; 4 caches; T≈6m | Shelter hides wake but limits turn space; narrow early tide window; Maya hears bells after route selection. |
| 03 `03-deep` | Deep water → island lee | Interceptor + Hunter cue; 3 caches; T≈5m | Dive usable but acoustically exposed surface stretches; late extraction requires a fast lane transfer. |
| 04 `04-lee` | Storm lee → calm seam | Patrol + weather mines; 2 caches; T≈7m | Safer gust timing, poor Sonar certainty; extraction calm interval is long. |
| 04 `04-capsule` | Capsule drift → debris wake | Interceptor + mines; 4 caches; T≈6m | Repeated moving lane gaps; richer transmission but short calm interval; Maya confirms capsule identity. |
| 04 `04-crest` | Wave crest → lightning cut | Police + Interceptor; 3 caches; T≈5m | Visibility pulses and Push timing; extraction point is exposed under lightning. |
| 05 `05-physical` | Wreck clue → verified beacon | Patrol + Hunter; 2 verified caches; T≈7m | Slow proof, reliable extraction; Maya acknowledges verification. |
| 05 `05-radio` | False call → relay source | Police + Hunter; 4 possible caches; T≈6m | One cache requires physical verification before securing; an unverified one yields no secured value; Maya catches the false voice. |
| 05 `05-chase` | Open pursuit → decoy buoy | Interceptor + Hunter; 3 caches; T≈5m | Fast escape but little safe repair space; extraction beacon may be a decoy unless Sonar/visual clue was read. |
| 06 `06-service` | Service trench → blind crane | Police + Armored; 2 caches; T≈7m | Gate cycles and cover; longer extraction gate window; Maya recognizes the old service map. |
| 06 `06-server` | Server barge → watched quay | Armored + Hunter; 4 caches; T≈6m | High evidence, narrow path; gate opens briefly; Maya receives network node. |
| 06 `06-slip` | Outer slip → security arc | Police + Interceptor; 3 caches; T≈5m | Open, quick; extraction requires crossing rotating arc at speed or under cover. |
| 07 `07-shadow` | Silhouette shadow → decoy edge | Hunter + Elite telegraph; 2 caches; T≈7m | Shadow pockets are small; safer recording lane; Maya warns against pursuing Kross. |
| 07 `07-record` | Broadcast lane → Kross wake | Elite + Interceptor; 4 caches; T≈6m | Recording richer but pursuit stronger; extraction penalizes chasing decoy. |
| 08 `08-shelter` | Wreck shelter → lower uplink | Armored + Hunter; 3 caches; T≈7m | Safer first window, difficult second; Maya notes the complete network path. |
| 08 `08-signal` | Open signal → upper uplink | Elite + Interceptor; 5 caches; T≈6m | Dangerous first window, easier second if pursuit was broken; Maya confirms public broadcast. |

## Route fairness and branch proof

Route evaluation must compare the same chapter on at least two branches using the same vessel/build and a paired seed. The trace should show different sector IDs, geometry/constraint flags, enemy composition, fixed-cache locations, at least one extraction condition, and the correct Maya reaction. A route with more Intel must expose a measurable countervailing risk; a safe route must still be winnable at base loadout. No route may be strictly faster, safer, and richer at once. If a branch is only a pickup/reward change, Phase 2 fails.

The first route implementation should be piloted in Chapter 01 and validated before copying architecture across eight chapters. If the graph proves too costly or unreadable on device, reduce to two meaningful routes where necessary; never simulate variety with cosmetic labels. Chapter 08 deliberately has two routes so its ending stays focused.
