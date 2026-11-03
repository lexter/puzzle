# Slide Puzzle — Design Spec

Date: 2026-11-03 · Status: draft for review
Screen concepts: "Slide Puzzle — Screen Concepts" design canvas (claude.ai artifact)

## 1. Goal

An offline sliding-tile puzzle (the classic 15-puzzle, played with pictures instead of numbers) for Android and iOS. One player races a stopwatch or plays relaxed. Two players on the same Wi-Fi race on identical boards, and the first to finish wins.

### Requirements (from the brief)

| Area | Requirement |
|---|---|
| Platforms | Android 10+ (minSdk 29), iOS 15+; phones, tablets, foldables / dual-screen, iPad split view |
| Tech | Kotlin + Compose Multiplatform, **100% shared Compose UI**: every screen is written once in Compose. Thin native shells only: an Android `Activity` and a SwiftUI app entry on iOS. No screen is written in SwiftUI or Android Views. |
| Look | Glass-style UI, light / dark / system appearance |
| Connectivity | Fully offline; nearby 2-player P2P race |
| Landing | Single / Peer-to-Peer, Time-based / Free play, leaderboard (top 10 per grid size) |
| Game | Settings button, Start button, grid 4×4 by default, scenic default picture, camera or gallery photo cropped and sliced into tiles, stopwatch in time-based mode, opponent mirror at 70% opacity in P2P |
| Settings | Grid size 4×4 … 10×10; appearance Light / Dark / System |

### Decisions made during brainstorming

1. **P2P is cross-platform** (Android ↔ iPhone) over local Wi-Fi or a phone hotspot: TCP sockets plus mDNS/Bonjour discovery. No internet needed.
2. **Time-based = stopwatch counting up.** Score = fastest time, with fewer moves breaking ties. Free play has no timer and isn't ranked.
3. **Leaderboard stores single-player time-based results only.** P2P races aren't ranked.
4. **The P2P host decides the match.** The host picks the grid size, image and mode. The image is sent to the guest, and both boards are shuffled from one shared seed, so they're identical. Both players get a synced 3-2-1 countdown, and the first to solve wins.
5. **Glass is drawn entirely in Compose** on every platform (frosted blur through the Haze library). No native Liquid Glass view. `GlassSurface` is one component, so native glass can be added later without touching the screens.
6. "iPhone duo" is read as foldable / dual-screen devices plus iPad split view, all handled through window size classes and Android's hinge (fold) information.

## 2. Screens (see design canvas)

1. **Landing:** title, leaderboard button (top right), board preview, Players segmented control (Single / Peer to Peer), Mode cards (Time-based / Free play), Play. Play goes to the Game screen for Single and to the Lobby for Peer to Peer.
2. **P2P Lobby:** Host / Join toggle.
   - Host: shows the match settings (grid, mode, image thumbnail) and a list of joining players with Accept. "Start race" begins the countdown.
   - Join: lists the hosts found on the network.
3. **Game:** back, stopwatch (time-based only), settings in the top bar; grid + move count; board; picture source (Scenic / Camera / Gallery, before Start only); **Start** shuffles the board and starts the clock. On a win: a sheet with time, moves, "New best!" when ranked, and Play again.
4. **Game, P2P:** same as Game plus a connection status line, the opponent mirror (their live board at 70% opacity, top-right of the board on phones, in a side panel on wide screens) and progress bars for both players. No picture picker or Start; the race starts from the host's countdown.
5. **Settings sheet:** grid size 4–10 (changing it reshuffles the board and resets the clock; only the host can change it in P2P), Appearance Light / Dark / System (default System, saved on the device).
6. **Leaderboard:** grid-size tabs 4×4…10×10, top 10 rows (rank, time, moves, date).

**Adaptive layout:** compact width uses the phone layout. Medium/expanded width puts the board on the left and a side panel on the right (timer details, opponent mirror, picture source). On Android foldables in book/tabletop posture, the board is placed on one side of the hinge.

**Visual system:** Outfit font (bundled, not downloaded), one accent (light `#2563EB`, dark `#2F66E8`). Glass panels are drawn over a blurred copy of the current puzzle picture. The stopwatch and counters use tabular (fixed-width) numbers. Touch targets are at least 44 pt.

## 3. Architecture

Single Gradle project:

```
puzzle/
  composeApp/            Kotlin Multiplatform module (commonMain, androidMain, iosMain, commonTest)
  iosApp/                Xcode project: SwiftUI app entry (`@main struct iOSApp: App`) with one view hosting the shared Compose UI
  gradle/libs.versions.toml
```

**How the iOS app hosts Compose:** on iOS, Compose Multiplatform draws into a UIKit view and is exposed as a `UIViewController` through `ComposeUIViewController { App() }`. Compose has no SwiftUI view type, so the SwiftUI app wraps that controller once in a `UIViewControllerRepresentable` (`ComposeView`) and shows it full screen. That one wrapper is the only SwiftUI code; all screens stay in shared Compose. The SwiftUI shell is also where iOS-only extras (e.g. native Liquid Glass, Game Center) could be added later without touching the shared screens. The SwiftUI `App` lifecycle needs iOS 14+, so it fits the iOS 15 minimum.

Packages in `composeApp/src/commonMain/kotlin/.../puzzle/`:

| Package | Responsibility | Depends on |
|---|---|---|
| `game` | `PuzzleState` (immutable board), `PuzzleEngine` (seeded solvable shuffle, row/column slide, solved check, tiles in place), `GameClock` | nothing (pure Kotlin) |
| `image` | `PuzzleImage`: decode bytes, center square crop, downscale to ≤1024 px, JPEG encode for P2P; bundled scenic default | Compose `ImageBitmap`, `platform` decoders |
| `glass` | `GlassSurface` composable (Haze blur + tint + hairline border), light/dark tokens | Compose, Haze |
| `ui` | Screens, navigation, theme (Light/Dark/System), adaptive layouts, `BoardView` | `game`, `image`, `glass`, view models |
| `ui/*ViewModel` | Screen state + actions (`LandingVM`, `GameVM`, `LobbyVM`, `LeaderboardVM`) | `game`, `data`, `p2p` |
| `p2p` | `Discovery` (expect/actual), `PeerConnection` (Ktor TCP), `MatchProtocol` messages, `MatchController` (host/guest state machine) | Ktor network, kotlinx.serialization |
| `data` | `ScoreRepository` (Room KMP), `SettingsRepository` (DataStore preferences) | Room, DataStore |
| `platform` | `ImagePicker` (camera / gallery), `Haptics`, `DeviceName` (expect/actual) | Android / iOS APIs |

### 3.1 Game engine (`game`)

- The board is an `IntArray` of size n², where 0 is the gap and tile `k` belongs at index `k-1`.
- **Shuffle:** create a random arrangement from `Random(seed)`, then check solvability: for odd n, the number of inversions must be even. For even n, inversions plus the gap's row counted from the bottom must be odd. If it isn't solvable, swap two non-gap tiles. Reject a result that is already solved. The same seed always produces the same board.
- **Move:** tapping tile at index `i` that shares a row or column with the gap slides every tile between them one step toward the gap (counts as one move). Other taps do nothing.
- `isSolved`, `tilesInPlace` (used for the progress bars), `moves`.
- `GameClock` uses a monotonic time source. It pauses when the app goes to the background in single-player (the board is hidden while paused) and never pauses in P2P.

### 3.2 Image pipeline (`image`)

Input is either the bundled scenic image (compose resources) or bytes from the picker. Steps:

1. Decode the bytes, applying EXIF orientation on Android.
2. Center-crop to a square.
3. Scale to at most 1024 px.

The result is one `ImageBitmap`. `BoardView` draws each tile with `drawImage(srcOffset, srcSize, dstOffset, dstSize)`, so no per-tile bitmaps exist (10×10 stays cheap). Tiles slide with a short animation. For P2P the cropped image is sent as a JPEG (quality 80, roughly 150–300 KB).

### 3.3 Glass (`glass`)

`GlassSurface(shape, content)` applies a Haze blur of the background layer (the blurred puzzle picture) plus a translucent tint (light: white 52%, dark: `#1C2436` 55%), a 1 px top-light border and a soft shadow. On older or weak devices where blur is slow or unsupported, it falls back to a solid translucent tint. All screens use only `GlassSurface`, so native iOS 26 Liquid Glass could replace it later without touching the screens.

### 3.4 Peer-to-peer (`p2p`)

- **Discovery:**
  - The host opens a Ktor `ServerSocket` on a free port and advertises `_slidepuzzle._tcp` with its player name.
  - On Android this uses `NsdManager` (plus a multicast lock while the lobby is open). On iOS it uses Bonjour (`NSNetService`, or `NWListener`/`NWBrowser` from the Network framework).
  - The guest browses for that service, resolves the host's address and connects over TCP.
- **Framing:** a 4-byte length followed by a payload. Control messages are JSON (kotlinx.serialization, versioned); the image is sent raw.
- **Messages:**

| Message | From | Purpose |
|---|---|---|
| `Hello(protocolVersion, playerName, platform)` | both | handshake; on a version mismatch, show an "Update the app" error |
| `MatchConfig(grid, seed, mode, imageKind, imageBytesLength)` | host | match setup |
| `Image(bytes)` | host | only when a custom photo is used |
| `Ready` | guest | image decoded, board built |
| `Start(countdownMs = 3000)` | host | both start the countdown when it arrives (LAN latency is negligible) |
| `Progress(board, moves, tilesInPlace)` | both | sent after every move; drives the 70% mirror |
| `Solved(elapsedMs, moves)` | both | finish |
| `Result(winner)` | host | first `Solved` the host sees (its own or the guest's) wins |
| `Rematch` / `Leave` / `Ping` | both | Ping every 2 s, connection treated as dead after 6 s of silence |

- `MatchController` is one state machine (`Idle → Advertising/Browsing → Connected → Configured → Countdown → Racing → Finished`) and exposes a `StateFlow` to the view models.

### 3.5 Persistence (`data`)

- **Room KMP** `scores` table: `id`, `gridSize`, `timeMs`, `moves`, `playedAt`. `topTen(gridSize)` returns rows ordered by `timeMs`, then `moves`. Only single-player time-based wins are inserted.
- **DataStore** keys: `appearance` (LIGHT/DARK/SYSTEM, default SYSTEM), `gridSize` (default 4), `playerName` (defaults to the device name).

### 3.6 Platform services (`platform`)

| Service | Android | iOS |
|---|---|---|
| Gallery | Photo Picker (`PickVisualMedia`, no permission) | `PHPickerViewController` |
| Camera | `TakePicture` into a FileProvider uri, `CAMERA` permission | `UIImagePickerController(.camera)`, `NSCameraUsageDescription` |
| Discovery | `NsdManager`, `CHANGE_WIFI_MULTICAST_STATE` | Bonjour, `NSLocalNetworkUsageDescription`, `NSBonjourServices = _slidepuzzle._tcp` |
| Sockets | `INTERNET` permission (required for LAN sockets; the app makes no internet calls) | none |
| Haptics | `HapticFeedback` / Vibrator | `UIImpactFeedbackGenerator` |

## 4. Error handling

| Situation | Behaviour |
|---|---|
| Photo can't be decoded or is too small | Message, then the board stays on the current/scenic image |
| Camera permission denied | Explanation with a link to the system Settings; gallery still available |
| No hosts found after 10 s | Hint: "Same Wi-Fi or hotspot? Local network allowed?" plus Retry |
| iOS local-network permission denied | Explanation with a link to Settings |
| Peer disconnects mid-race | "Connection lost" dialog: keep playing solo (not ranked) or back to Lobby |
| Protocol version mismatch | "Both players need the same app version" |
| Image transfer fails | Host falls back to the scenic image and resends `MatchConfig` |

## 5. Testing

- **Unit tests (`commonTest`):**
  - Shuffles are always solvable for every grid size 4–10 (property test over many seeds), the same seed gives the same board, and a shuffle is never already solved.
  - Row/column slide results, non-adjacent taps ignored, `isSolved`, `tilesInPlace`.
  - `GameClock` with a fake time source.
  - `MatchProtocol` encode/decode round-trip and framing of split/merged packets.
  - `MatchController` transitions with a fake connection, including disconnects and who wins.
  - `ScoreRepository` top-10 ordering on an in-memory database.
- **UI tests:** Compose UI tests for Landing → Game → win sheet, and for the Settings sheet changing grid size and appearance.
- **Manual device matrix:** Android phone ↔ iPhone race over the same Wi-Fi and over a phone hotspot; tablet landscape; foldable unfolded; iOS 15 device or simulator; light/dark/system switching.

## 6. Out of scope (for now)

Online play, accounts, more than two players, native iOS Liquid Glass, sound effects, number hints on tiles, ranking P2P results.

## 7. Open items

- Pick the bundled scenic photo (royalty-free, about 2048 px). The canvas uses a vector placeholder.
- App name and icon (the canvas uses "Slide Puzzle").
