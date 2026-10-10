# Black Wake — Production Memory

## Product identity

Black Wake is a native Android arcade stealth/chase game. The player escapes Black Tide across hostile waters while balancing route position, speed, detection and limited escape tools.

Current canonical architecture: **Kotlin + Jetpack Compose + Canvas**.

The former React/Babylon/Capacitor implementation is obsolete and is not part of the current product architecture.

## Core loop

The intended loop is:

**read the route → choose risk → steer → manage signature → survive pursuit → collect intel → extract**

The current strike treats four mechanics as provisional core pillars:

1. route positioning / steering;
2. speed versus stealth;
3. detection and pursuit manipulation;
4. information / escape tools.

Fuel and Intel support those pillars. Extra systems must justify their code, HUD and learning cost through observable player value.

## Current quality policy

Do not infer quality from feature count.

Do not claim Play Store readiness from:

- compilation alone;
- source inspection alone;
- particle effects;
- upgrade count;
- AI state enums;
- chapter labels.

The executable game and measured device behavior are the authority.

## Strike status

The 2026-10-10 destroy-coding strike established `main@b5ad79d8cebe951f8b5c171b6d0b9907487d7a28` as the forensic baseline.

See:

- `FORENSIC_BASELINE.md`
- `DESTROY_MATRIX.md`

The strike branch removes dead web architecture and fake choices, repairs critical simulation state, introduces behavioral tests, simplifies the live HUD, and preserves only coherent meta-progression.

## Release status

**BLOCKED** until physical-device UX, runtime performance and first-session comprehension gates are completed.
