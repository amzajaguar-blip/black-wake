# Black Wake

Black Wake is a native Android arcade stealth/chase game built with Kotlin and Jetpack Compose.

## Canonical product architecture

There is one active application architecture:

- root Gradle project
- `:app` Android application module
- Kotlin gameplay/state
- Jetpack Compose UI and Canvas rendering

The previous React/Babylon/Capacitor scaffold is obsolete and is intentionally not part of the canonical product.

## Current status

**Release gate: BLOCKED.**

The game is under a destroy/rebuild quality strike. A successful APK build is necessary but is not sufficient for release.

Current forensic reports:

- `FORENSIC_BASELINE.md`
- `DESTROY_MATRIX.md`

## Build

Requirements:

- JDK 21
- Gradle 8.6
- Android SDK compatible with compile/target SDK 35

Commands:

```bash
gradle --no-daemon :app:assembleDebug
gradle --no-daemon :app:testDebugUnitTest
gradle --no-daemon :app:lintDebug
```

GitHub Actions runs the same build/test/lint gate on strike branches and pull requests to `main`.

## Runtime entry point

`app/src/main/java/com/blackwake/game/MainActivity.kt`

Main product files:

```text
app/src/main/java/com/blackwake/game/
├── GameData.kt
├── GameScreen.kt
├── GameViewModel.kt
├── MainActivity.kt
├── PursuerAI.kt
└── SynthAudioEngine.kt
```

## Identity

- Store-facing app name: **Black Wake**
- Android namespace: `com.blackwake.game`
- Current applicationId: `com.aistudio.mareanera.bwake`

Do not change the applicationId casually. It may be tied to an existing Play Store app identity and must be verified against release history before migration.

## Quality rule

Do not claim Play Store readiness from source inspection or a green build alone.

Before release, Black Wake still requires:

- physical-device input validation,
- first-session comprehension testing,
- frame pacing/performance capture,
- low/mid-range Android validation,
- chapter strategy differentiation,
- HUD/readability captures across target aspect ratios,
- progression persistence verification,
- final Play Store media generated from the real current runtime.

## Repository hygiene

One-off patch scripts, generated Gradle state and obsolete web-stack configuration are not part of the product. Git history is the archive; the working tree should describe the game that actually ships.
