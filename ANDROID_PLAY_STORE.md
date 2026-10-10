# Black Wake — Android / Google Play delivery

Black Wake is a **native Android application** built with Kotlin and Jetpack Compose.

## Current application identity

| Field | Value |
| --- | --- |
| App name | Black Wake |
| Namespace | `com.blackwake.game` |
| Application ID | `com.aistudio.mareanera.bwake` |
| Min SDK | 26 |
| Target / Compile SDK | 35 |
| Orientation | Landscape |
| Java / JVM | 21 |

The application ID is treated as a release identity. Do not rename it without first verifying the existing Google Play package history.

## Verification commands

From repository root:

```bash
gradle --no-daemon :app:assembleDebug
gradle --no-daemon :app:testDebugUnitTest
gradle --no-daemon :app:lintDebug
```

The same gate runs in GitHub Actions.

## Release gate

A green build is not a release approval.

Before generating or submitting a production AAB, Black Wake still requires:

- first-session validation on real Android hardware;
- touch-control validation on small and large landscape phones;
- low/mid-range frame-pacing evidence;
- pause/background/process-recreation behavior validation;
- chapter-strategy differentiation;
- final runtime screenshots and trailer captures;
- signed release configuration review;
- Play Console policy/data-safety review.

## Release build

The repository currently defines a release build type, but release signing credentials are not stored in source control.

When the release gate is explicitly passed, generate the AAB through the native Gradle Android pipeline. Keep keystore and credentials outside the repository.
