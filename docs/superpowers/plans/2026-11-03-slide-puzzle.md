# Slide Puzzle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the offline picture sliding-puzzle game for Android 10+ and iOS 15+, with every screen in shared Compose UI, single-player stopwatch/free play, a top-10 leaderboard, and an Android↔iPhone race over local Wi-Fi.

**Architecture:** One Gradle project with a `composeApp` Kotlin Multiplatform module (`commonMain`, `androidMain`, `iosMain`, plus a `jvm` target used only to run `commonTest` quickly) and a thin `iosApp` (SwiftUI `@main` app hosting `ComposeUIViewController`). Pure-Kotlin `game` and `p2p` protocol logic sit under Compose screens driven by ViewModels; platform services are `expect/actual`.

**Tech Stack:** Kotlin 2.2+, Compose Multiplatform 1.9+ (Material 3, compose resources, navigation-compose, lifecycle-viewmodel), Haze (blur), Room KMP 2.7+ with bundled SQLite driver, DataStore Preferences KMP, Ktor `ktor-network` (raw TCP), kotlinx.serialization JSON, kotlinx.coroutines (+ `-test`), androidx.window (fold info).

**Spec:** `docs/superpowers/specs/2026-11-03-slide-puzzle-design.md` (read it with this plan). Screen designs: the "Slide Puzzle — Screen Concepts" design canvas.

## Global Constraints

- Android `minSdk = 29`, `targetSdk`/`compileSdk` = latest stable; iOS deployment target `15.0`.
- Every screen is Compose in `commonMain`. Native code is limited to `MainActivity` (Android), the SwiftUI `iOSApp` + one `UIViewControllerRepresentable` `ComposeView` (iOS), and `expect/actual` services.
- No internet calls anywhere. The only network use is LAN TCP + mDNS/Bonjour, service type `_slidepuzzle._tcp`.
- Grid sizes 4…10 inclusive; default 4. Appearance LIGHT/DARK/SYSTEM; default SYSTEM.
- Leaderboard: only single-player **time-based** wins; top 10 per grid size ordered by `timeMs` then `moves`.
- P2P: host's grid, seed, mode and image win; countdown 3000 ms; ping every 2 s, peer dead after 6 s of silence; first `Solved` seen by host wins.
- Image: center square crop, max 1024 px; P2P transfer JPEG quality 80.
- Visual tokens: font Outfit (bundled), accent light `#2563EB`, dark `#2F66E8`; glass tint light white 52%, dark `#1C2436` 55%; opponent mirror alpha 0.7; touch targets ≥ 44 dp; tabular digits for timer/counters.
- Package root: `com.lexterlabra.puzzle`. App display name: "Slide Puzzle" (placeholder; spec open item).
- Every commit message ends with the session's Co-Authored-By / Claude-Session lines.

## Review Focus

1. **App sent to background mid time-based run** → stopwatch pauses and the board is hidden; on return it resumes from the same time (single-player). In P2P the clock keeps running. Test in Task 3 (`pausesOnBackground`) and Task 9 (`backgroundHidesBoardAndPausesClock`).
2. **Rapid taps while a slide animates** → every tap is applied to the engine's current board in order; the visual never shows two tiles in one cell. Test in Task 6 (`rapidTapsApplyInOrder`).
3. **Portrait / rotated (EXIF) / very large / tiny photos** → cropped square is upright and centered; photos < 64 px are rejected with the "photo too small" message; 12 MP photos decode without OOM (decode with sampling). Test in Task 6 (`cropPortraitCentersSquare`, `rejectsTinyImage`).
4. **Both players solve within milliseconds of each other** → exactly one `Result`, both devices show the same winner. Test in Task 11 (`nearSimultaneousSolveHasOneWinner`).
5. **Grid size or picture changed mid-game, or Android rotation** → board reshuffles, clock resets, nothing is written to the leaderboard; rotation keeps the board and clock. Test in Task 9 (`changingGridMidGameResetsWithoutScore`, `stateSurvivesRecreation`).

---

## File Structure

```
puzzle/
  settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml
  .gitignore
  composeApp/build.gradle.kts
  composeApp/src/
    androidMain/AndroidManifest.xml
    androidMain/kotlin/com/lexterlabra/puzzle/MainActivity.kt
    androidMain/res/xml/file_paths.xml
    androidMain/kotlin/.../platform/*.android.kt, p2p/Discovery.android.kt, data/Database.android.kt
    iosMain/kotlin/com/lexterlabra/puzzle/MainViewController.kt
    iosMain/kotlin/.../platform/*.ios.kt, p2p/Discovery.ios.kt, data/Database.ios.kt
    jvmMain/kotlin/.../  (test-only actuals: fakes for platform/data/discovery)
    commonMain/composeResources/drawable/scenic_default.jpg, font/outfit_*.ttf
    commonMain/kotlin/com/lexterlabra/puzzle/
      App.kt                       root composable: theme + nav host
      game/PuzzleState.kt          immutable board value
      game/PuzzleEngine.kt         shuffle / slide / solvable
      game/GameClock.kt            stopwatch
      image/PuzzleImage.kt         crop + scale + encode (expect decode/encode)
      glass/GlassSurface.kt        Haze-based glass panel
      ui/theme/Theme.kt            colors, typography, Appearance
      ui/board/BoardView.kt        tile drawing + slide animation + taps
      ui/landing/LandingScreen.kt, LandingViewModel.kt
      ui/leaderboard/LeaderboardScreen.kt, LeaderboardViewModel.kt
      ui/game/GameScreen.kt, GameViewModel.kt, SettingsSheet.kt, WinSheet.kt, TopBar.kt
      ui/lobby/LobbyScreen.kt, LobbyViewModel.kt
      ui/game/OpponentMirror.kt, RaceProgress.kt
      ui/layout/AdaptiveLayout.kt  size class + fold → layout mode
      data/ScoreEntity.kt, ScoreDao.kt, PuzzleDatabase.kt, ScoreRepository.kt
      data/SettingsRepository.kt
      p2p/MatchMessage.kt          sealed messages + codec
      p2p/Framing.kt               length-prefixed frames
      p2p/PeerConnection.kt        interface + KtorPeerConnection
      p2p/Discovery.kt             expect class
      p2p/MatchController.kt       host/guest state machine
      platform/ImagePicker.kt, Haptics.kt, DeviceName.kt  (expect)
    commonTest/kotlin/com/lexterlabra/puzzle/... mirrors the packages above
  iosApp/iosApp.xcodeproj, iosApp/iOSApp.swift, iosApp/ComposeView.swift, iosApp/Info.plist
  docs/superpowers/specs/…, docs/superpowers/plans/…
```

---

### Task 1: Project scaffold that builds on Android, iOS and JVM

**Files:**
- Create: everything under "settings/build/gradle", `composeApp/build.gradle.kts`, `MainActivity.kt`, `MainViewController.kt`, `App.kt` (shows "Slide Puzzle" text), `iosApp/*`, `.gitignore`
- Test: `composeApp/src/commonTest/kotlin/com/lexterlabra/puzzle/SmokeTest.kt`

**Interfaces:**
- Produces: `@Composable fun App()`; `fun MainViewController(): UIViewController` (iosMain); `ComposeView: UIViewControllerRepresentable` (Swift).

- [ ] **Step 1:** `git init`, add `.gitignore` (Gradle, `.idea`, `local.properties`, `build/`, `xcuserdata`, `*.xcworkspace/xcuserdata`, `DerivedData`).
- [ ] **Step 2:** Generate the module layout matching the JetBrains KMP wizard (Android + iOS, shared UI). Look up the current stable versions on Maven Central / the Compose Multiplatform releases page and pin them in `libs.versions.toml` for every library in Tech Stack. Targets: `androidTarget()`, `iosX64()`, `iosArm64()`, `iosSimulatorArm64()` (framework `ComposeApp`, static), `jvm()`. Android `minSdk 29`; iOS deployment target 15.0 in the Xcode project.
- [ ] **Step 3:** `iOSApp.swift`: `@main struct iOSApp: App { var body: some Scene { WindowGroup { ComposeView().ignoresSafeArea(.all) } } }`; `ComposeView.swift` wraps `MainViewControllerKt.MainViewController()`.
- [ ] **Step 4: Write the smoke test** `SmokeTest.appNameIsSet()` asserting `APP_NAME == "Slide Puzzle"` (const in `App.kt`).
- [ ] **Step 5: Verify**
  - `./gradlew :composeApp:jvmTest` → BUILD SUCCESSFUL, 1 test passed
  - `./gradlew :composeApp:assembleDebug` → APK produced
  - On the Mac: `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` → success, then the iosApp scheme builds and launches in an iOS 15+ simulator showing "Slide Puzzle".
- [ ] **Step 6: Commit** `chore: scaffold Compose Multiplatform project`

### Task 2: Puzzle engine

**Files:**
- Create: `game/PuzzleState.kt`, `game/PuzzleEngine.kt`
- Test: `commonTest/.../game/PuzzleEngineTest.kt`

**Interfaces:**
- Produces:
  - `data class PuzzleState(val size: Int, val tiles: List<Int>, val moves: Int = 0)` with `val gapIndex: Int`, `val isSolved: Boolean`, `val tilesInPlace: Int` (non-gap tiles at index `tile-1`). `tiles` holds 0 for the gap; solved = `1..n²-1, 0`.
  - `object PuzzleEngine { fun solved(size: Int): PuzzleState; fun shuffled(size: Int, seed: Long): PuzzleState; fun isSolvable(size: Int, tiles: List<Int>): Boolean; fun slide(state: PuzzleState, index: Int): PuzzleState }` — `slide` returns the same instance when the tap is not in the gap's row/column; otherwise moves the tiles between `index` and the gap one step toward the gap and increments `moves` by 1.
  - `require(size in 4..10)` everywhere.

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun solvedBoardIsSolved() = assertTrue(PuzzleEngine.solved(4).isSolved)
@Test fun shuffleIsDeterministicPerSeed() =
    assertEquals(PuzzleEngine.shuffled(4, 42L).tiles, PuzzleEngine.shuffled(4, 42L).tiles)
@Test fun shufflesAreSolvableAndNotSolved() { for (n in 4..10) for (seed in 0L until 200L) {
    val s = PuzzleEngine.shuffled(n, seed)
    assertTrue(PuzzleEngine.isSolvable(n, s.tiles)); assertFalse(s.isSolved)
    assertEquals((0 until n * n).toSet(), s.tiles.toSet()) } }
@Test fun knownUnsolvableDetected() { // 14 and 15 swapped
    val t = (1..13).toList() + listOf(15, 14, 0); assertFalse(PuzzleEngine.isSolvable(4, t)) }
@Test fun oddSizeParity() { val t = (1..23).toList() + listOf(0); val swapped = t.toMutableList().apply { this[0] = 2; this[1] = 1 }
    assertTrue(PuzzleEngine.isSolvable(5, t)); assertFalse(PuzzleEngine.isSolvable(5, swapped)) }
@Test fun slideAdjacent() { val s = PuzzleEngine.slide(PuzzleEngine.solved(4), 14)
    assertEquals(0, s.tiles[14]); assertEquals(15, s.tiles[15]); assertEquals(1, s.moves) }
@Test fun slideWholeColumn() { val s = PuzzleEngine.slide(PuzzleEngine.solved(4), 3) // gap at 15, tap top of col 3
    assertEquals(listOf(0, 4, 8, 12), listOf(s.tiles[3], s.tiles[7], s.tiles[11], s.tiles[15])); assertEquals(1, s.moves) }
@Test fun slideWholeRow() { val s = PuzzleEngine.slide(PuzzleEngine.solved(4), 12)
    assertEquals(listOf(0, 13, 14, 15), s.tiles.subList(12, 16)) }
@Test fun nonLineTapIgnored() { val b = PuzzleEngine.solved(4); assertSame(b, PuzzleEngine.slide(b, 0)) }
@Test fun gapTapIgnored() { val b = PuzzleEngine.solved(4); assertSame(b, PuzzleEngine.slide(b, 15)) }
@Test fun tilesInPlaceCounts() = assertEquals(14, PuzzleEngine.slide(PuzzleEngine.solved(4), 14).tilesInPlace)
```

- [ ] **Step 2:** `./gradlew :composeApp:jvmTest --tests '*PuzzleEngineTest*'` → FAIL (unresolved references).
- [ ] **Step 3: Implement.** `isSolvable`: count inversions over non-zero tiles; odd `size` → inversions even; even `size` → `(inversions + rowFromBottom(gap)) % 2 == 1` where `rowFromBottom = size - gapIndex / size` (1-based). `shuffled`: shuffle `0..n²-1` with `Random(seed)`; if not solvable, swap the first two non-zero tiles (this always flips solvability). If the result is solved, swap the last two non-zero tiles and then the first two (keeps it solvable, makes it unsolved). Keep `PuzzleState` immutable (`List<Int>`).
- [ ] **Step 4:** Re-run → PASS.
- [ ] **Step 5: Commit** `feat(game): seeded solvable shuffle and row/column slides`

### Task 3: Game clock

**Files:**
- Create: `game/GameClock.kt`
- Test: `commonTest/.../game/GameClockTest.kt`

**Interfaces:**
- Produces: `class GameClock(private val now: () -> Long /* monotonic ms */) { val elapsedMs: Long; val isRunning: Boolean; fun start(); fun pause(); fun resume(); fun reset() }` and `fun formatElapsed(ms: Long): String` → `"mm:ss.t"` (e.g. `107300 → "01:47.3"`, `3_600_000+` → `"60:00.0"` keeps counting minutes).

- [ ] **Step 1: Write the failing tests** with a `var t = 0L` fake: `startsAtZero`, `countsWhileRunning` (start, t=1500 → 1500), `pausesOnBackground` (start, t=1000, pause, t=5000 → 1000; resume, t=5500 → 1500), `resetStops`, `formatsTenths` (`formatElapsed(107_300) == "01:47.3"`, `formatElapsed(0) == "00:00.0"`).
- [ ] **Step 2:** Run → FAIL.
- [ ] **Step 3:** Implement; production `now` = `TimeSource.Monotonic` mark.
- [ ] **Step 4:** Run → PASS.
- [ ] **Step 5: Commit** `feat(game): stopwatch clock`

### Task 4: Settings and score storage

**Files:**
- Create: `data/SettingsRepository.kt`, `data/ScoreEntity.kt`, `data/ScoreDao.kt`, `data/PuzzleDatabase.kt`, `data/ScoreRepository.kt`, `data/Database.android.kt`, `data/Database.ios.kt`, `data/Database.jvm.kt`
- Test: `commonTest/.../data/ScoreRepositoryTest.kt`, `commonTest/.../data/SettingsRepositoryTest.kt`

**Interfaces:**
- Produces:
  - `enum class Appearance { LIGHT, DARK, SYSTEM }`
  - `class SettingsRepository(store: DataStore<Preferences>) { val appearance: Flow<Appearance>; val gridSize: Flow<Int>; val playerName: Flow<String?>; suspend fun setAppearance(a: Appearance); suspend fun setGridSize(n: Int); suspend fun setPlayerName(name: String) }` — defaults SYSTEM, 4, null (UI falls back to `DeviceName.get()`); `setGridSize` clamps to 4..10.
  - `@Entity("scores") data class ScoreEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val gridSize: Int, val timeMs: Long, val moves: Int, val playedAt: Long)`
  - `data class Score(val gridSize: Int, val timeMs: Long, val moves: Int, val playedAt: Long)`
  - `class ScoreRepository(dao: ScoreDao) { fun topTen(gridSize: Int): Flow<List<Score>>; suspend fun record(score: Score): Boolean /* true if it entered the top 10 */ }`
  - `expect fun databaseBuilder(): RoomDatabase.Builder<PuzzleDatabase>`; `expect fun createDataStore(): DataStore<Preferences>` (file `puzzle.preferences_pb`).

- [ ] **Step 1: Write the failing tests** (jvm in-memory Room + temp-file DataStore):
  - `topTenOrdersByTimeThenMoves` — insert (4×4: 60000/90, 50000/120, 50000/100) → order 50000/100, 50000/120, 60000/90.
  - `topTenLimitsToTen` — insert 12 → 10 returned, the 2 slowest missing.
  - `topTenFiltersBySize` — a 5×5 entry never appears in 4×4.
  - `recordReportsTopTenEntry` — 11th slower score returns false, faster returns true.
  - `settingsDefaults` — SYSTEM, 4; `gridSizeClamped` — set 12 → 10, set 2 → 4.
- [ ] **Step 2:** Run → FAIL.
- [ ] **Step 3:** Implement with `BundledSQLiteDriver`, `setQueryCoroutineContext(Dispatchers.IO)`; DAO query `SELECT * FROM scores WHERE gridSize = :n ORDER BY timeMs, moves LIMIT 10`.
- [ ] **Step 4:** Run → PASS.
- [ ] **Step 5: Commit** `feat(data): leaderboard and settings storage`

### Task 5: Theme and glass

**Files:**
- Create: `ui/theme/Theme.kt`, `glass/GlassSurface.kt`, fonts in `composeResources/font/`
- Test: `commonTest/.../ui/theme/ThemeTest.kt`

**Interfaces:**
- Consumes: `Appearance` (Task 4).
- Produces:
  - `@Composable fun PuzzleTheme(appearance: Appearance, content: @Composable () -> Unit)`; `fun resolveDark(appearance: Appearance, systemDark: Boolean): Boolean`
  - `object PuzzleColors` light/dark tokens (accent, text, secondaryText, glassTint, glassBorder) with the Global Constraints values.
  - `@Composable fun GlassBackdrop(image: ImageBitmap, modifier: Modifier, content: @Composable () -> Unit)` — draws the blurred picture behind the screen and provides a `HazeState` via CompositionLocal.
  - `@Composable fun GlassSurface(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(24.dp), content: @Composable BoxScope.() -> Unit)` — `hazeEffect` with the tint; when blur is unavailable (Android < 12 / `HazeDefaults` reports unsupported) draws the tint at higher alpha (light 0.82, dark 0.85) instead.

- [ ] **Step 1: Write failing tests:** `resolveDark(LIGHT, true) == false`, `(DARK, false) == true`, `(SYSTEM, true) == true`, `(SYSTEM, false) == false`.
- [ ] **Step 2:** Run → FAIL. **Step 3:** Implement (Typography uses Outfit; numerals in timer use `fontFeatureSettings = "tnum"`). **Step 4:** Run → PASS.
- [ ] **Step 5: Verify visually:** temporary preview in `App()` of two `GlassSurface`s over the scenic image in light and dark on Android emulator and iOS simulator; then remove the preview.
- [ ] **Step 6: Commit** `feat(ui): theme with light/dark/system and glass surface`

### Task 6: Picture pipeline and board view

**Files:**
- Create: `image/PuzzleImage.kt` (+ `image/ImageCodec.android.kt`, `.ios.kt`, `.jvm.kt`), `ui/board/BoardView.kt`, `composeResources/drawable/scenic_default.jpg` (placeholder landscape photo until the open item is resolved)
- Test: `commonTest/.../image/PuzzleImageTest.kt`, `commonTest/.../ui/board/BoardViewTest.kt` (Compose UI test, `runComposeUiTest`)

**Interfaces:**
- Produces:
  - `expect object ImageCodec { fun decode(bytes: ByteArray, maxSide: Int): ImageBitmap? /* applies EXIF orientation, samples large images */; fun encodeJpeg(image: ImageBitmap, quality: Int): ByteArray }`
  - `object PuzzleImage { const val MAX_SIDE = 1024; const val MIN_SIDE = 64; fun cropRect(width: Int, height: Int): IntRect; fun prepare(bytes: ByteArray): Result<ImageBitmap> /* fails with ImageTooSmall or DecodeFailed */ }`
  - `@Composable fun BoardView(state: PuzzleState, image: ImageBitmap, onTileTap: (index: Int) -> Unit, modifier: Modifier = Modifier, interactive: Boolean = true)` — square board; each tile drawn with `drawImage(image, srcOffset, srcSize, dstOffset, dstSize)` from its home cell; positions animated per tile id (`animateIntOffsetAsState`, 160 ms); tiles get `testTag("tile-$id")` and `contentDescription "Tile $id"`; gap 4 dp (2 dp when board < 200 dp), corner 8 dp (3 dp small); `interactive = false` disables taps (opponent mirror).

- [ ] **Step 1: Write failing tests:**
  - `cropLandscapeCentersSquare` — `cropRect(4000, 3000) == IntRect(500, 0, 3500, 3000)`.
  - `cropPortraitCentersSquare` — `cropRect(3000, 4000) == IntRect(0, 500, 3000, 3500)`.
  - `rejectsTinyImage` — 40×40 PNG bytes → `prepare` failure `ImageTooSmall`.
  - `rejectsGarbageBytes` — random bytes → `DecodeFailed`.
  - `rapidTapsApplyInOrder` (BoardView test) — host holds `var state` and sets `state = PuzzleEngine.slide(state, it)`; on a solved 4×4, click `tile-15` (index 14) then `tile-14` (index 13) without waiting for idle → final `state.tiles` equals `slide(slide(solved, 14), 13)` and every `tile-$id` node has distinct bounds after idle.
  - `nonInteractiveIgnoresTaps` — `interactive = false`, click `tile-15` → callback not invoked.
- [ ] **Step 2:** Run → FAIL. **Step 3:** Implement (`prepare`: decode with `maxSide = 2 * MAX_SIDE` sampling, crop, scale to ≤ `MAX_SIDE`). **Step 4:** Run → PASS.
- [ ] **Step 5: Commit** `feat(board): cropped picture tiles with animated slides`

### Task 7: Camera, gallery, haptics, device name

**Files:**
- Create: `platform/ImagePicker.kt`, `platform/Haptics.kt`, `platform/DeviceName.kt` and their `.android.kt` / `.ios.kt` / `.jvm.kt` (fakes) actuals; Android `res/xml/file_paths.xml`, manifest `FileProvider` + `CAMERA`; iOS `Info.plist` `NSCameraUsageDescription` = "Take a photo to turn into a puzzle.", `NSPhotoLibraryUsageDescription` not needed (PHPicker).

**Interfaces:**
- Produces:
  - `sealed interface PickResult { data class Picked(val bytes: ByteArray) : PickResult; data object Cancelled : PickResult; data object PermissionDenied : PickResult }`
  - `@Composable fun rememberImagePicker(onResult: (PickResult) -> Unit): ImagePicker` with `interface ImagePicker { fun pickFromGallery(); fun takePhoto(); val cameraAvailable: Boolean }`
  - `@Composable fun rememberHaptics(): Haptics` with `fun tap(); fun success()`
  - `expect object DeviceName { fun get(): String }` (Android `Settings.Global.DEVICE_NAME` fallback `Build.MODEL`; iOS `UIDevice.currentDevice.name`)
  - `fun openAppSettings()` (expect) for denied-permission links.

- [ ] **Step 1:** Implement Android: `PickVisualMedia(ImageOnly)` and `TakePicture` into a cache-dir FileProvider uri; request `CAMERA` at tap time, `PermissionDenied` on refusal; read bytes via `ContentResolver`.
- [ ] **Step 2:** Implement iOS: `PHPickerViewController` (selectionLimit 1, images) loading `public.image` data; `UIImagePickerController` `.camera` returning JPEG data; `AVCaptureDevice.authorizationStatus` → `PermissionDenied`; present from the key window's top view controller. `cameraAvailable` = `UIImagePickerController.isSourceTypeAvailable(.camera)` (false on simulator).
- [ ] **Step 3: Verify manually** on an Android emulator and iOS simulator (gallery) and one real phone of each (camera): picking returns bytes that `PuzzleImage.prepare` accepts; cancel returns `Cancelled`; denying camera returns `PermissionDenied`.
- [ ] **Step 4: Commit** `feat(platform): camera, gallery, haptics, device name`

### Task 8: Landing, leaderboard and navigation

**Files:**
- Create: `App.kt` (replace placeholder), `ui/landing/LandingScreen.kt`, `ui/landing/LandingViewModel.kt`, `ui/leaderboard/LeaderboardScreen.kt`, `ui/leaderboard/LeaderboardViewModel.kt`
- Test: `commonTest/.../ui/landing/LandingFlowTest.kt`, `commonTest/.../ui/leaderboard/LeaderboardViewModelTest.kt`

**Interfaces:**
- Consumes: Tasks 4–6.
- Produces:
  - `enum class Players { SINGLE, P2P }`, `enum class Mode { TIME_BASED, FREE }`
  - Routes (type-safe navigation, `@Serializable`): `Landing`, `Leaderboard`, `Game(mode: Mode)`, `Lobby(mode: Mode)`, `RaceGame` (no args; reads `MatchController` state).
  - `class LeaderboardViewModel(repo: ScoreRepository) { val selectedSize: StateFlow<Int>; val rows: StateFlow<List<Score>>; fun select(size: Int) }`
  - Simple manual DI: `class AppContainer(settings, scores, discoveryFactory, connectionFactory)` created per platform and passed to `App(container)`.

- [ ] **Step 1: Write failing tests:**
  - `leaderboardSwitchesSize` — fake repo; `select(5)` → rows from 5×5 only.
  - `playSingleGoesToGame` / `playP2PGoesToLobby` (UI test with `TestNavHostController` or a nav-callback lambda): select Peer to Peer + Free play, tap Play → route `Lobby(FREE)`.
  - `trophyOpensLeaderboard` — tap node with contentDescription "Leaderboard" → `Leaderboard`.
  - `emptyLeaderboardShowsHint` — no rows → text "No times yet. Finish a time-based game to set one."
- [ ] **Step 2:** Run → FAIL. **Step 3:** Implement per canvas screens 1 and 6 (grid-size tabs 4×4…10×10; rows rank/time/moves/date with `formatElapsed`). **Step 4:** Run → PASS.
- [ ] **Step 5: Commit** `feat(ui): landing, leaderboard, navigation`

### Task 9: Single-player game, settings sheet, win sheet

**Files:**
- Create: `ui/game/GameViewModel.kt`, `ui/game/GameScreen.kt`, `ui/game/TopBar.kt`, `ui/game/SettingsSheet.kt`, `ui/game/WinSheet.kt`
- Test: `commonTest/.../ui/game/GameViewModelTest.kt`, `commonTest/.../ui/game/SettingsSheetTest.kt`

**Interfaces:**
- Consumes: `PuzzleEngine`, `GameClock`, `PuzzleImage`, `ScoreRepository`, `SettingsRepository`, `ImagePicker`.
- Produces:
  - `data class GameUiState(val puzzle: PuzzleState, val image: ImageBitmap, val phase: Phase, val elapsedMs: Long, val mode: Mode, val pausedHidden: Boolean, val message: UserMessage?, val newBest: Boolean)`; `enum class Phase { READY, PLAYING, WON }`; `sealed interface UserMessage { PhotoTooSmall; PhotoUnreadable; CameraDenied }`
  - `class GameViewModel(mode: Mode, settings, scores, clock: GameClock, seedSource: () -> Long, savedState: SavedStateHandle) { val state: StateFlow<GameUiState>; fun start(); fun tap(index: Int); fun setGridSize(n: Int); fun setPicture(result: PickResult); fun useScenic(); fun onBackground(); fun onForeground(); fun playAgain() }`
  - Rules: READY shows the solved board; `start()` shuffles with `seedSource()` and starts the clock (TIME_BASED only); a solving `tap` stops the clock, sets WON, and for TIME_BASED calls `scores.record` and sets `newBest`. Picture source chips enabled only in READY.

- [ ] **Step 1: Write failing tests** (fake clock, fake repos, fixed seed):
  - `startShufflesAndRunsClock` — after `start()`, phase PLAYING, puzzle == `PuzzleEngine.shuffled(4, seed)`.
  - `solvingRecordsTimeBasedScore` — drive taps to solve (use a seed whose solution path the test computes, or inject `PuzzleEngine.slide(solved, 14)` as the "shuffle" via `seedSource`/test hook) → WON, one `Score(4, elapsed, moves, now)` recorded.
  - `freePlayNeverRecords`.
  - `changingGridMidGameResetsWithoutScore` — PLAYING, `setGridSize(6)` → READY, 6×6 solved board, clock 0, no score.
  - `backgroundHidesBoardAndPausesClock` — PLAYING, `onBackground()`, advance fake time 10 s, `onForeground()` → elapsed unchanged, `pausedHidden` false again.
  - `stateSurvivesRecreation` — new VM with the same `SavedStateHandle` restores tiles, moves, elapsed, phase.
  - `tinyPhotoShowsMessageKeepsImage` — `setPicture(Picked(tinyBytes))` → message `PhotoTooSmall`, image unchanged.
  - `settingsSheetPicksSizeAndAppearance` (UI) — tap "6×6", tap "Dark", Apply → callbacks receive 6 and DARK.
- [ ] **Step 2:** Run → FAIL. **Step 3:** Implement per canvas screens 3 and 5 (Start button label "Start"; settings icon top-right; timer pill only in TIME_BASED; win sheet "Solved!", time, moves, "New best!" when `newBest`, buttons "Play again" / "Home"). Hook lifecycle `ON_STOP`/`ON_START` to `onBackground`/`onForeground`. **Step 4:** Run → PASS.
- [ ] **Step 5: Verify manually:** play a 4×4 to completion on Android and iOS; score appears on the leaderboard; switch appearance Light/Dark/System live.
- [ ] **Step 6: Commit** `feat(game): single-player game with settings and win flow`

### Task 10: P2P messages and framing

**Files:**
- Create: `p2p/MatchMessage.kt`, `p2p/Framing.kt`
- Test: `commonTest/.../p2p/MatchMessageTest.kt`, `commonTest/.../p2p/FramingTest.kt`

**Interfaces:**
- Produces:
  - `const val PROTOCOL_VERSION = 1`
  - `@Serializable sealed interface MatchMessage` with `Hello(protocolVersion: Int, playerName: String, platform: String)`, `MatchConfig(grid: Int, seed: Long, mode: Mode, customImage: Boolean, imageBytes: Int)`, `Ready`, `Start(countdownMs: Long = 3000)`, `Progress(tiles: List<Int>, moves: Int, tilesInPlace: Int)`, `Solved(elapsedMs: Long, moves: Int)`, `Result(winnerIsHost: Boolean)`, `ImageFailed`, `Rematch`, `Leave`, `Ping`
  - `sealed interface Frame { data class Control(val message: MatchMessage); class Image(val bytes: ByteArray) }`
  - `object Framing { fun encode(frame: Frame): ByteArray; class Decoder { fun feed(chunk: ByteArray): List<Frame> } }` — wire: 4-byte big-endian length, 1-byte kind (0 = JSON control, 1 = image), payload. Max frame 8 MiB; larger → `IllegalStateException("frame too large")`.

- [ ] **Step 1: Write failing tests:** `everyMessageRoundTrips` (all variants), `decoderHandlesSplitFrames` (feed one frame 1 byte at a time → exactly one frame), `decoderHandlesMergedFrames` (two frames in one chunk → two), `imageFrameKeepsBytes`, `rejectsOversizeFrame`, `unknownMessageTypeIsIgnored` (JSON with unknown `type` → no frame, no crash; `Json { ignoreUnknownKeys = true }` and class discriminator `type`).
- [ ] **Step 2:** Run → FAIL. **Step 3:** Implement. **Step 4:** Run → PASS.
- [ ] **Step 5: Commit** `feat(p2p): match protocol and framing`

### Task 11: Match controller

**Files:**
- Create: `p2p/PeerConnection.kt` (interface only here), `p2p/MatchController.kt`
- Test: `commonTest/.../p2p/MatchControllerTest.kt` with an in-memory `FakeConnectionPair`

**Interfaces:**
- Consumes: Task 10, `PuzzleEngine`.
- Produces:
  - `interface PeerConnection { val incoming: Flow<Frame>; suspend fun send(frame: Frame); fun close(); val closed: StateFlow<Boolean> }`
  - `sealed interface MatchState { Idle; Waiting; Connected(peerName: String); Configured(config: MatchConfig, image: ImageBitmap?); Countdown(endsAtMs: Long); Racing(me: PuzzleState, opponent: PuzzleState); Finished(iWon: Boolean, myTimeMs: Long, opponentTimeMs: Long?); Lost(reason: LossReason) }`, `enum class LossReason { DISCONNECTED, VERSION_MISMATCH, IMAGE_FAILED }`
  - `class MatchController(role: Role /* HOST, GUEST */, connection: PeerConnection, myName: String, clock: () -> Long, scope: CoroutineScope) { val state: StateFlow<MatchState>; suspend fun configure(grid: Int, mode: Mode, image: ImageBitmap?) /* host */; suspend fun startRace() /* host */; suspend fun tap(index: Int); suspend fun rematch(); fun leave() }`
  - Host seed = random `Long` generated in `configure`. Opponent board in `Racing` comes from received `Progress`. Host decides the winner on the first `Solved` it observes (its own local solve or a received one) and sends `Result`. Ping every 2000 ms; no frame for 6000 ms → `Lost(DISCONNECTED)`. The guest sends `Ready` only after the image decodes. If the custom image fails to decode, the guest sends `ImageFailed` (add this message to Task 10's set) and stays connected. The host then shows "Couldn't send the photo — using the scenic picture" and re-sends `MatchConfig(customImage = false)` (spec §4). `LossReason.IMAGE_FAILED` is used only if the scenic fallback also fails.

- [ ] **Step 1: Write failing tests** (virtual time via `runTest`):
  - `handshakeConnectsBothSides` — both reach `Connected` with the other's name.
  - `versionMismatchFails` — guest Hello with version 2 → host `Lost(VERSION_MISMATCH)`.
  - `bothBoardsIdentical` — after configure + Ready + startRace, both `Racing.me.tiles` equal.
  - `countdownLasts3s` — `Countdown` then `Racing` after 3000 ms virtual time.
  - `progressUpdatesOpponentMirror` — guest tap → host `Racing.opponent` equals guest's board.
  - `firstSolverWins` — guest solves first → guest `Finished(iWon = true)`, host `Finished(false)`.
  - `nearSimultaneousSolveHasOneWinner` — host solves locally and a guest `Solved` arrives in the same virtual millisecond → exactly one `Result` sent, both sides agree.
  - `silenceMeansDisconnected` — stop guest pings, advance 6001 ms → host `Lost(DISCONNECTED)`.
  - `imageFailureFallsBackToScenic` — guest's decoder fails → guest sends `ImageFailed`, host re-sends config with `customImage = false`, both reach `Racing` with identical boards.
- [ ] **Step 2:** Run → FAIL. **Step 3:** Implement. **Step 4:** Run → PASS.
- [ ] **Step 5: Commit** `feat(p2p): host/guest match state machine`

### Task 12: Real network: TCP connection and discovery

**Files:**
- Create: `p2p/KtorPeerConnection.kt`, `p2p/Discovery.kt` (expect), `p2p/Discovery.android.kt`, `p2p/Discovery.ios.kt`, `p2p/Discovery.jvm.kt` (JmDNS-free fake: in-process registry); Android manifest permissions `INTERNET`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`; iOS `Info.plist` `NSLocalNetworkUsageDescription` = "Find nearby players on your Wi-Fi to race against.", `NSBonjourServices` = [`_slidepuzzle._tcp`]
- Test: `commonTest/.../p2p/KtorPeerConnectionTest.kt` (jvm loopback)

**Interfaces:**
- Produces:
  - `class KtorPeerConnection(socket: Socket) : PeerConnection`; `object PeerSockets { suspend fun listen(): Pair<ServerSocket, Int /* port */>; suspend fun accept(server: ServerSocket): PeerConnection; suspend fun connect(host: String, port: Int): PeerConnection }`
  - `data class FoundHost(val id: String, val name: String, val host: String, val port: Int)`
  - `expect class Discovery { fun advertise(name: String, port: Int); fun stopAdvertising(); fun browse(): Flow<List<FoundHost>>; fun stopBrowsing() }` — service type `_slidepuzzle._tcp`.

- [ ] **Step 1: Write failing test** `loopbackExchangesFrames` — listen, connect on 127.0.0.1, send Control(Hello) + Image(200 KB) both ways → received equal; `closeIsObserved` → peer `closed` becomes true.
- [ ] **Step 2:** Run → FAIL. **Step 3:** Implement Ktor sockets (`SelectorManager(Dispatchers.IO)`), feeding reads into `Framing.Decoder`. Android `Discovery`: `NsdManager.registerService` / `discoverServices` + `resolveService` (use `registerServiceInfoCallback` on API 34+), hold `WifiManager.MulticastLock` while browsing. iOS `Discovery`: `NSNetService` publish/`NSNetServiceBrowser` + resolve on the main run loop (`platform.Foundation`). **Step 4:** Run → PASS.
- [ ] **Step 5: Verify manually:** Android phone and iPhone on the same Wi-Fi each see the other's host within 10 s; then repeat with one phone's hotspot; iOS shows the local-network prompt on first browse.
- [ ] **Step 6: Commit** `feat(p2p): LAN sockets and mDNS/Bonjour discovery`

### Task 13: Lobby, race screen, adaptive layouts

**Files:**
- Create: `ui/lobby/LobbyViewModel.kt`, `ui/lobby/LobbyScreen.kt`, `ui/game/RaceGameScreen.kt`, `ui/game/OpponentMirror.kt`, `ui/game/RaceProgress.kt`, `ui/layout/AdaptiveLayout.kt` (+ `FoldInfo.android.kt` via `androidx.window`, `.ios.kt`/`.jvm.kt` return no fold)
- Modify: `ui/game/GameScreen.kt` to use `AdaptiveLayout`
- Test: `commonTest/.../ui/lobby/LobbyViewModelTest.kt`, `commonTest/.../ui/layout/AdaptiveLayoutTest.kt`, `commonTest/.../ui/game/RaceGameScreenTest.kt`

**Interfaces:**
- Consumes: Tasks 9, 11, 12.
- Produces:
  - `enum class LayoutMode { COMPACT, SIDE_PANEL }`; `data class FoldInfo(val hingeBoundsDp: Rect?, val isSeparating: Boolean)`; `fun layoutModeFor(widthDp: Int, heightDp: Int, fold: FoldInfo?): LayoutMode` — SIDE_PANEL when width ≥ 600 dp and width > height, or when a separating vertical hinge exists; `fun boardSide(fold: FoldInfo?): BoardSide` (`START` | `ANYWHERE`).
  - `class LobbyViewModel(discovery: Discovery, sockets: PeerSockets, settings, mode: Mode) { val tab: StateFlow<LobbyTab /* HOST, JOIN */>; val hosts: StateFlow<List<FoundHost>>; val joinRequest: StateFlow<String?>; val noHostsHint: StateFlow<Boolean> /* true after 10 s with none */; fun host(); fun join(host: FoundHost); fun accept(); suspend fun startRace(): MatchController }`
  - `@Composable fun OpponentMirror(state: PuzzleState, image: ImageBitmap, name: String, modifier: Modifier)` — `BoardView(interactive = false)`, `alpha(0.7f)`, label "$name · ${tilesInPlace}/${n²-1}".

- [ ] **Step 1: Write failing tests:**
  - `layoutCompactOnPhonePortrait` — `(390, 844, null) → COMPACT`; `layoutSidePanelOnTabletLandscape` — `(1180, 820, null) → SIDE_PANEL`; `layoutSidePanelOnUnfoldedFoldable` — `(840, 900, verticalSeparatingHinge) → SIDE_PANEL`.
  - `noHostsHintAfter10s` — fake discovery emits nothing, advance 10 000 ms → true.
  - `mirrorIsSeventyPercentAndNotTappable` (UI) — node `opponent-mirror` has alpha 0.7; clicking its tiles changes nothing.
  - `connectionLostDialogOffersSoloOrLobby` (UI) — controller state `Lost(DISCONNECTED)` → dialog "Connection lost" with "Keep playing solo" and "Back to lobby"; solo continues the board unranked.
- [ ] **Step 2:** Run → FAIL. **Step 3:** Implement per canvas screens 2, 4, 7 (lobby Host/Join toggle, match summary card, joining player row with Accept, "Start race · 3-2-1"; race screen with status line "Connected · {name}", mirror top-right on COMPACT and in the side panel on SIDE_PANEL, both progress bars; big 3-2-1 overlay during `Countdown`; win/lose sheet "You win!" / "{name} won" with Rematch / Leave). Guest's settings sheet shows grid size disabled with "Only the host can change this". **Step 4:** Run → PASS.
- [ ] **Step 5: Verify manually (device matrix from spec §5):** Android ↔ iPhone race on same Wi-Fi and on hotspot, with scenic and with a camera photo; tablet landscape; foldable unfolded (or Android resizable emulator with hinge); iOS 15 simulator; kill Wi-Fi mid-race → "Connection lost" within ~6 s on both.
- [ ] **Step 6: Commit** `feat(p2p): lobby, race screen with opponent mirror, adaptive layout`

### Task 14: Release polish

**Files:**
- Modify: app icon placeholders (Android adaptive icon, iOS AppIcon), `AndroidManifest.xml` (`android:label`, portrait+landscape allowed, `resizeableActivity=true`), iOS `Info.plist` (all orientations on iPad, supports multiple windows off), `README.md` (build/run steps for Android Studio and Xcode)

- [ ] **Step 1:** Run the full suite: `./gradlew :composeApp:jvmTest :composeApp:testDebugUnitTest` → all green.
- [ ] **Step 2:** `./gradlew :composeApp:assembleRelease` (debug signing is fine for now) and archive-build the iOS app in Xcode with no warnings about missing usage descriptions.
- [ ] **Step 3:** Grep check that nothing reaches the internet: `grep -rn "http" composeApp/src --include=*.kt` → only none or comments.
- [ ] **Step 4: Commit** `chore: release polish and README`
