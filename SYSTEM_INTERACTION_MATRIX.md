# BLACK WAKE — SYSTEM INTERACTION MATRIX

Status: **PROVISIONAL forecast** except the four tactical tool rules cited in [Phase 1](PHASE_1_DESIGN_SPEC.md). This matrix names intended interactions; it does not authorize later-phase implementation.

This is a complete upper-triangle matrix for the 17 requested systems: each unordered pair appears once. `SY` = synergy (one enables/rewards the other); `CT` = counterplay (one can answer the other); `CF` = conflict/trade-off (using one makes the other harder or more costly); `NI` = intentionally no direct interaction; `PV` = provisional relationship requiring upstream evidence. The diagonal is omitted. A `PV` cell is a design question, not permission to invent a mechanic. Interactions can be asymmetric in play; the code records the pair's main player decision.

| | TP | ST | SO | FL | DC | IN | FU | DI | EN | PA | IC | PO | HU | AR | EL | RO | BU |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| **TP** Throttle/Push | — | CF | SY | CF | CF | CF | CF | CF | CF | CT | CT | CT | CF | CF | CT | CF | SY |
| **ST** Stealth |  | — | CF | CF | CF | SY | SY | SY | SY | CT | CF | CT | CT | NI | CF | SY | SY |
| **SO** Sonar |  |  | — | CF | SY | SY | NI | SY | CT | CT | CT | CT | CF | CT | CF | SY | SY |
| **FL** Flare |  |  |  | — | SY | NI | NI | CF | SY | CT | CT | CT | CF | NI | CF | CF | SY |
| **DC** Damage Control |  |  |  |  | — | CF | CF | CF | CT | CF | CF | CF | CF | CF | CF | SY | SY |
| **IN** Intel |  |  |  |  |  | — | CF | CF | CF | CF | CF | CF | CF | CF | CF | SY | SY |
| **FU** Fuel |  |  |  |  |  |  | — | NI | CF | NI | NI | NI | NI | NI | NI | SY | SY |
| **DI** Dive |  |  |  |  |  |  |  | — | CT | CT | CT | CT | CF | CT | CT | CF | SY |
| **EN** Environment |  |  |  |  |  |  |  |  | — | SY | SY | SY | SY | SY | SY | SY | SY |
| **PA** Patrol |  |  |  |  |  |  |  |  |  | — | PV | PV | PV | PV | PV | SY | CT |
| **IC** Interceptor |  |  |  |  |  |  |  |  |  |  | — | PV | PV | PV | PV | SY | CT |
| **PO** Police |  |  |  |  |  |  |  |  |  |  |  | — | PV | PV | PV | SY | CT |
| **HU** Hunter |  |  |  |  |  |  |  |  |  |  |  |  | — | PV | PV | SY | CT |
| **AR** Armored |  |  |  |  |  |  |  |  |  |  |  |  |  | — | PV | SY | CT |
| **EL** Elite |  |  |  |  |  |  |  |  |  |  |  |  |  |  | — | SY | CT |
| **RO** Route Choice |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  | — | SY |
| **BU** Progression Build |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  | — |

## High-value connections and boundaries

- **TP–ST–FU–IN:** Push reduces time in exposed water but consumes fuel and signature; quiet travel lowers signature and supports Intel recovery but leaves more time for a pursuer to close. A route's geometry must make both policies situational. Phase 2 cannot merely attach static rewards to lanes.
- **SO–FL–DC:** Sonar gives information and an acoustic cost; Flare buys a brief break and marks a lane; Damage Control uses a quiet window and can be interrupted. A player should sometimes ping before crossing, flare only once pursued, and repair only after securing space. Simultaneous use stacks detection costs.
- **DI–IN–EN:** Dive answers surface threats but prevents collecting surface Intel. Mines and certain route constraints remain dangerous, so holding Dive never solves the entire run. Storm/port rules may modify visibility or safe depth, after Phase 4 evidence.
- **SO–EN–RO:** Sonar matters when environment or route hides a meaningful lane distinction. It may reveal current contacts, not future random spawns or a universally correct route.
- **FL–HU and SO–HU:** The Hunter investigates acoustic/flare cues. Its exact strength and telegraph are Phase 3 forecasts. Phase 1 only specifies the limited investigation hooks in its contract.
- **FU–enemy = NI:** Fuel does not directly damage, stun, or alter any archetype. It matters indirectly through movement and repairs. Direct fuel-enemy modifiers would add complexity without a distinct choice.
- **PA/IC/PO/HU/AR/EL pairs = PV:** Enemy combinations are intentionally not assigned mutual buffs. Their encounter composition and fairness must be established in Phase 3. A two-enemy encounter should create a novel lane/timing choice, not stack unavoidable damage.
- **RO–BU:** A route previews enough constraints to make loadout choice meaningful, but a build cannot make any route trivial. The route is chosen during play; the build is committed before launch.

## Matrix review rules

At each phase gate, update `PV` cells affected by observed behavior to `SY`, `CT`, `CF`, or `NI` with a one-line rationale in the review record. If any tactical tool ends up with only synergies and no counter/conflict in reachable play, the design gate fails. If an `NI` cell accidentally acquires a hidden gameplay effect, either document and test it or remove that effect. Preserve this as a relationship audit, not a substitute for the exact Phase 1 contract.
