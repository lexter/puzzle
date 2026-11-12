# Slide Puzzle

Offline picture sliding puzzle (4×4 up to 10×10) for Android 10+ and iOS 15+, built with
Kotlin + Compose Multiplatform. Every screen is shared Compose UI. Two players on the same
Wi-Fi (or a phone hotspot) can race on identical boards, Android ↔ iPhone included.

- Design spec: `docs/superpowers/specs/2026-11-03-slide-puzzle-design.md`
- Implementation plan: `docs/superpowers/plans/2026-11-03-slide-puzzle.md`

## Requirements

- JDK 17+ (Android Studio's bundled JDK works)
- Android Studio (Ladybug or newer) with Android SDK 36
- Xcode 15+ for iOS (deployment target iOS 15.0)

## Run the tests

```bash
./gradlew :composeApp:jvmTest
```

All shared logic and Compose UI tests run on the desktop JVM target (it exists only for tests).

## Run on Android

Open the project in Android Studio and run the `composeApp` configuration, or:

```bash
./gradlew :composeApp:installDebug
```

## Run on iOS

1. Open `iosApp/iosApp.xcodeproj` in Xcode.
2. For a real device, set `TEAM_ID` in `iosApp/Configuration/Config.xcconfig`.
3. Pick a simulator or device and press Run. The "Compile Kotlin Framework" build phase runs
   `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode` for you.

## Project layout

```
composeApp/src/commonMain/kotlin/com/lexterlabra/puzzle/
  game/      board, shuffle, slides, stopwatch (pure Kotlin)
  image/     crop / scale / JPEG for photos
  glass/     GlassSurface + blurred backdrop
  data/      settings + leaderboard (DataStore)
  p2p/       framing, messages, match state machine, TCP + mDNS/Bonjour
  platform/  camera, gallery, haptics, device name (expect/actual)
  ui/        screens, navigation, theme, adaptive layout
iosApp/      SwiftUI app entry hosting the Compose UI (no SwiftUI screens)
```

## Peer-to-peer checklist

- Both devices on the same Wi-Fi, or one device joined to the other's hotspot.
- iOS asks for Local Network access the first time you host or join; allow it.
- Service type: `_slidepuzzle._tcp` (declared in `Info.plist` → `NSBonjourServices`).

## Open items

- Replace `composeApp/src/commonMain/composeResources/drawable/scenic_default.jpg` with the
  final scenic photo (square, ~1024–2048 px).
- App name and icon are placeholders ("Slide Puzzle", tile-grid icon).
- Outfit font isn't bundled yet: drop the TTFs into `composeResources/font/` and set the
  `FontFamily` in `ui/theme/Theme.kt`.

## Git hooks

```bash
scripts/install-hooks.sh
```

- **pre-commit**: ktlint (`./gradlew :composeApp:ktlintCheck`, rules in `.editorconfig`) when Kotlin files are staged, SwiftLint (`--strict`, `.swiftlint.yml`) on staged Swift files, and `scripts/privacy-guard.sh`. Kotlin files with both staged and unstaged changes must be fully staged or stashed first. SwiftLint needs `brew install swiftlint`.
- **pre-push**: compiles the Android app, the shared code for iOS (on macOS) and every test source set.

Skip once with `--no-verify`.

## CI (`.github/workflows/ci.yml`)

Runs on pushes to `main`, pull requests and manual dispatch.

1. **Code integrity review**: gitleaks over the full history, `scripts/privacy-guard.sh` (blocks the device name, tracking identifiers, logging, cleartext traffic, ATS exceptions and external storage, and checks the iOS privacy manifest), mobsfscan (OWASP MASVS rules; justified exceptions in `.mobsf`), ktlint, and Android lint with its security checks promoted to errors.
2. **Shared tests (JVM) and Android build**: `jvmTest` (shared logic and Compose UI tests) and `assembleDebug`.
3. **Android on-device tests**: `connectedDebugAndroidTest` (`composeApp/src/androidInstrumentedTest`) on the newest Google APIs x86_64 emulator image (`EMULATOR_API_LEVEL`).
4. **iOS**: SwiftLint, the shared tests on the newest iPhone simulator (`iosSimulatorArm64Test -PiosSimulator=<UDID>`, picked by `scripts/ci/latest-simulator.sh`), and an `xcodebuild` of `iosApp`.
5. **Firebase App Distribution**: on `main` and manual runs, after 1–4 pass: a signed release APK and an ad-hoc IPA, build number = run number. Each platform is skipped with a notice until configured.

| Name | Kind | Value |
| --- | --- | --- |
| `ANDROID_KEYSTORE_BASE64` | secret | Release keystore (.jks), base64-encoded. Keep the same one forever: testers can only update over builds signed with it |
| `ANDROID_KEYSTORE_PASSWORD` | secret | Keystore password |
| `ANDROID_KEY_ALIAS` | secret | Key alias |
| `ANDROID_KEY_PASSWORD` | secret | Key password |
| `IOS_DIST_CERT_P12_BASE64` | secret | Apple Distribution certificate + key as .p12, base64-encoded (needs a paid developer account) |
| `IOS_DIST_CERT_PASSWORD` | secret | Password of that .p12 |
| `IOS_ADHOC_PROFILE_BASE64` | secret | Ad Hoc provisioning profile for `com.lexterlabra.puzzle`, base64-encoded |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | secret | Google service account key with the Firebase App Distribution Admin role |
| `FIREBASE_ANDROID_APP_ID` | variable | Firebase Android app ID (`1:…:android:…`) |
| `FIREBASE_IOS_APP_ID` | variable | Firebase iOS app ID (`1:…:ios:…`) |
| `FIREBASE_TESTER_GROUPS` | variable | Optional; comma-separated group aliases (default `testers`) |

The iOS team ID and profile name are read from the profile, so `TEAM_ID` in `Config.xcconfig` can stay empty.
