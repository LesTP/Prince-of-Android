# DEVPLAN — Prince of Persia Android Port

## Cold Start Summary

**What this is:** Android port of SDLPoP (Prince of Persia), testing autonomous AI porting methodology.

**Key constraints:**
- SDLPoP is ~25,000 lines of C with 344 global variables (tight coupling)
- Replay system (.P1R files) provides deterministic test oracle
- Target language: Kotlin (native Android, validation via replay oracle)

**Environment:** Raspberry Pi 5 (16GB), Incus container `claude-code` (Debian 12 arm64, 12GB RAM, 3 CPUs)
**Project path:** `/home/claude/workspace/PoP_port/` (NTFS USB drive, 466GB, 126GB free)
**Shell access:** Full — no Sandboxie restrictions. Claude Code can run `gradle`, `make`, `python3`, `git` directly.

**Gotchas:**
- **Kotlin integer semantics:** Signed types (Byte/Short/Int) wrap on overflow like C. Conversions are explicit (.toByte(), .toInt()). Replay oracle catches mismatches immediately.
- **Replay auto-exit:** Instrumented builds (`DUMP_FRAME_STATE`) auto-exit after replay ends. Essential for consistent trace lengths.
- **C struct sizes:** Always verify struct byte sizes against `typedef` definitions in `types.h`/`data.h`. Don't trust field counts — check each type (`byte`=1, `word`=2, `dword`=4, `short`=2). `char_type` is 16 bytes (not 17), `start_level` is `word` (2 bytes, not 4).
- **Gradle 9.x JUnit:** Add `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` to `build.gradle.kts` or test executor fails to start with "Failed to load JUnit Platform."
- **SDLPoP replay invocation (Linux):** Run from `/tmp/sdlpop/` (binary copied there for execute permission). Use `SDL_VIDEODRIVER=offscreen SDL_AUDIODRIVER=dummy ./prince validate "replays/foo.p1r" seed=12345`. Output: `state_trace.bin`.
- **SDL headless mode:** Pi has no display. Use `SDL_VIDEODRIVER=offscreen` (not `dummy` — dummy hangs). Requires `xvfb` package installed but `offscreen` driver doesn't need it.
- **NTFS execute permissions:** USB drive is NTFS — `chmod +x` is silently ignored. Workaround: copy binaries to `/tmp/sdlpop/` with symlinks to data/replays/doc/SDLPoP.ini.
- **Line endings:** Source files from Windows have CRLF. Run `dos2unix *.c *.h` in SDLPoP/src/ after any file transfer. Grep/ripgrep fail silently on CRLF files.
- **Reference traces:** Regenerated all 13 on ARM64 Pi (2026-04-03). Sizes match expected frame counts. Determinism verified.

## Current Status

**Track:** A — Game Logic Translation (Build regime, autonomous)
**Module:** 9 — Layer 1: seg004 (Collision detection) — **IN PROGRESS**
**Phase:** 9b — COMPLETE (review: no issues found). All 26 seg004 functions translated, 232 tests pass. Next: Phase completion (phase-complete).
**Blocked/Broken:** None.

### Module 9: seg004.c → Kotlin

**Scope:** 621 lines, 26 functions. Collision detection, wall bumping, chomper damage, gate pushing, edge distance.

**Dependencies:** seg006 (already translated — provides `getTile`, `wallType`, `tileIsFloor`, `charDxForward`, `distanceToEdgeWeight`, `setCharCollision`, `determineCol`, `loadFramDetCol`, `playSeq`, `takeHp`, `getTileAtChar`, `getTileInfrontofChar`, `getTileBehindChar`, `getTileDivModM7`). External stubs needed: `loadFrameToObj` (seg006 ExternalStubs — already exists), `playSound` (already stubbed), `seqtblOffsetChar` (already stubbed).

**New globals (seg004-local):** `bumpColLeftOfWall`, `bumpColRightOfWall`, `rightCheckedCol`, `leftCheckedCol`, `collTileLeftXpos` (5 variables). Constants: `wallDistFromLeft[]`, `wallDistFromRight[]` (2 lookup tables).

**`#ifdef` flags:** `FIX_COLL_FLAGS`, `FIX_TWO_COLL_BUG`, `USE_JUMP_GRAB` (already handled in seg006), `FIX_SKELETON_CHOMPER_BLOOD`, `FIX_OFFSCREEN_GUARDS_DISAPPEARING`, `FIX_CAPED_PRINCE_SLIDING_THROUGH_GATE`, `FIX_PUSH_GUARD_INTO_WALL` (7 flags).

**Steps:**

**Phase 9a — Full translation (all 26 functions)**
Given seg004's small size (621 lines), translate all functions in one phase:
- Collision detection core (4): `checkCollisions`, `moveCollToPrev`, `getRowCollisionData`, `clearCollRooms`
- Wall position (2): `getLeftWallXpos`, `getRightWallXpos`
- Bump detection (4): `checkBumped`, `checkBumpedLookLeft`, `checkBumpedLookRight`, `isObstacleAtCol`
- Obstacle/position (2): `isObstacle`, `xposInDrawnRoom`
- Bump response (4): `bumped`, `bumpedFall`, `bumpedFloor`, `bumpedSound`
- Chomper (4): `checkChompedKid`, `chomped`, `checkChompedGuard`, `checkChompedHere`
- Gate/guard (2): `checkGatePush`, `checkGuardBumped`
- Edge/wall distance (3): `getEdgeDistance`, `distFromWallForward`, `distFromWallBehind`
- Gate helper (1): `canBumpIntoGate`
- Add seg004-local globals to GameState.kt
- Unit tests: collision setup/clear, bump detection, chomper checks, edge distance, gate push
- **Test:** `gradle build` clean, `gradle test` all pass

**Phase 9b — Review and cleanup**
- Code review: naming consistency, integer semantics, `#ifdef` paths
- DEVLOG update, DEVPLAN cleanup

**Tracks overview:**
- **Track A (Game Logic):** C→Kotlin translation of ~7,200 lines, validated by replay oracle. **Full shell access on Pi — true autonomous mode.** Current.
- **Track B (Android Platform):** Rendering, platform, audio, game loop. Requires Android Studio. **After Track A.**
- **Track C (Touch Controls):** Gesture prototype + playtesting. Requires Android Studio + phone. **Parallel, any time.**

## Phase Summary

### Phase 0: Environment Setup — COMPLETE
One-line: Built SDLPoP on Windows via MSYS2/MinGW, verified gameplay, recorded first replay. See DEVLOG §Phase-0.

### Phase 1: Replay Oracle Spike — COMPLETE
One-line: Verified replay determinism (FC: no differences). Q1 closed — oracle works. See DEVLOG §Phase-1.

### Phase 2: Target Language Decision — COMPLETE
One-line: Decision: **Kotlin**. Rationale: native Android, predictable integer semantics (signed types wrap on overflow), explicit conversions. Validation deferred to first file translation in Phase 3 — replay oracle will catch semantic mismatches immediately. Q2 closed. See DEVLOG §Phase-2.

### Phase 3: Test Infrastructure — COMPLETE
One-line: Built replay oracle toolchain: 13 reference traces, Python comparator, Kotlin P1R parser (Gradle, 9/9 tests pass). See DEVLOG §Phase-3.

**Deferred:** Game loop replay runner — build alongside first file translation.

### Module 6: State Model — COMPLETE
One-line: Translated types.h + data.h → Kotlin (4 files, 27 tests pass). See DEVLOG §Module 6.

### Module 7: Sequence Table — COMPLETE
One-line: Translated seqtbl.c → Kotlin (SequenceTable.kt + enums, 2,310-byte array, 115 offsets, 50 new tests, 77 total pass). See DEVLOG §Module 7.

### Module 8: Layer 1 seg006 — COMPLETE
One-line: Translated seg006.c → Kotlin (81 functions, 2,154 lines C → Seg006.kt + ExternalStubs.kt, 190 tests pass, zero escalations). See DEVLOG §Module 8.
