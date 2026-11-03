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
