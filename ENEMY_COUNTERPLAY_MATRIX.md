# BLACK WAKE — ENEMY COUNTERPLAY MATRIX

Status: **PROVISIONAL Phase 3 forecast.** Current code has six types with distinct movement, prediction, delay, damage, and charge-speed numbers, but a largely shared DETECTED → PURSUIT → INTERCEPT → SEARCH state machine. Phase 1's limited Flare/Sonar investigation hooks are governed only by [PHASE_1_DESIGN_SPEC.md](PHASE_1_DESIGN_SPEC.md). The identities below require Phase 1 and route evidence before implementation.

| Archetype | Visual / sonar telegraph | Hunt behavior and strength | Weakness / best response | Bad response and ability/environment interactions |
| --- | --- | --- | --- | --- |
| **Patrol** | Wide, slow white sweep; Sonar shows a broad search cone. | Acquires a continuous visible wake and calls pursuit after sustained sight. Controls open lanes, weak in cover. | Cut across a wreck shadow, then go quiet; Dive under a sweep when Intel is not in that lane. | Push straight through its cone lengthens exposure. Flare briefly breaks an acquired lock; Patrol searches the marked launch lane. Port cranes block its sight; dawn reduces cover. |
| **Interceptor** | Narrow red lane reticle, rising engine note, 0.8s commitment cue before a fast ram. | Closes distance and attacks a predicted lane. Punishes long low-throttle exposure in open water. | Let it commit, then switch lane or Dive; use Push before commitment to open distance, accepting signature cost. | Early lane weaving lets prediction track the player; Flare during committed ram does not cancel it. Narrow reef gaps limit dodge; open water helps Push. |
| **Police** | Rotating blue-white beam and short radio squawk; Sonar shows beam orientation. | Searches lanes in cycles; temporarily marks a sighted lane for other pursuers. Creates timing windows rather than raw speed. | Cross just after the beam passes, use cover, or Dive under a well-telegraphed sweep. | Sonar/Flare from inside the beam broadcasts a location; waiting in an open lane gets reacquired. Container cover blocks beam but can echo Sonar. |
| **Hunter** | Distinct twin acoustic pulses and a dotted last-known-position track; no generic red triangle only. | Investigates sound events and Flare origin; searches the last known lane, then adjacent lanes. Threatens noisy tool use and repetitive hiding. | Displace from the sound marker, quiet down, and change lane through cover; Sonar is useful only if its information outweighs the acoustic clue. | Flare then staying in place, repeated pings, or holding Dive until oxygen forces resurfacing near the marker. Storm noise can mask one clue but also weakens player Sonar. |
| **Armored** | Large dark hull with two-lane obstruction silhouette and low collision horn; Sonar marks its blocked width. | Slow, deliberate lane denial. Its danger is geometry and a high-impact ram, not relentless tracking. | Read the open lane early or Dive under the hull where depth permits; Push only after the gap is known. | Late Push cannot turn through the obstruction; Flare does not remove its physical body. Shallow island shelves can make Dive unavailable and must be signposted before commitment. |
| **Elite** | Two-stage lock: amber feint lane, then red committed lane, with distinct rhythm and Sonar glyph. | Predicts a first dodge and commits after the player's response. Threatens habitual immediate lane changes. | Hold position during feint, then change lane or Dive after red commitment; use a previously scouted gap. | Spamming lateral movement or a premature Flare wastes the escape. Dawn makes both telegraphs visible; storm may obscure them, so audio/haptic must preserve fairness. |

## Non-universal answer check

| Player strategy | Works against | Fails or costs against |
| --- | --- | --- |
| Stay quiet in cover | Patrol, some Police sweeps | Interceptor catches prolonged exposure; Hunter searches last known position; Armored blocks space. |
| Push through | Interceptor before lock, exposed timed crossings | Patrol/Police see wake; Hunter hears ignition; Armored narrows steering; fuel/heat limit repeat. |
| Dive at every threat | Patrol beam, committed surface ram, Armored if deep | Misses surface Intel; mines remain dangerous; shallow shelves and oxygen limit use. |
| Flare at first contact | Uncommitted visible pursuer if lane change follows | Hunter marks origin; committed ram survives; Armored obstruction persists; high detection can spawn another contact. |
| Ping whenever ready | Identifies hidden lane geometry | Hunter/Police investigate; clear water offers little; signature may cross pursuit threshold. |

## Encounter composition forecast

Target at least 12 tested pair types across routes/challenges: Patrol+Interceptor, Patrol+Police, Patrol+Hunter, Patrol+Armored, Interceptor+Police, Interceptor+Hunter, Interceptor+Armored, Police+Hunter, Police+Armored, Hunter+Armored, Hunter+Elite, Interceptor+Elite. Pair design must give the player a reachable escape lane and enough telegraph time for the faster threat. Elite must never be stacked with another unavoidable committed ram. Enemy count caps alone do not prove fairness.

Every archetype has one distinct teachable response and one bad response. GLM's future Phase 3 evidence must include fixed-seed A/B input traces showing the best response wins more often, a runtime capture showing the telegraph before commitment, and novice recognition observations. If players describe them only as different colors/damage values, redesign the archetype rather than increasing its stats.
