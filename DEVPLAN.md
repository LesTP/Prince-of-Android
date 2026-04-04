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
- **GameState Short fields need `.toInt()`:** `gs.tileCol`, `gs.currRoom`, and other `Short` fields cause compile errors when used in arithmetic or array indexing. Always use `.toInt()` for reads and `.toShort()` for writes (e.g., `gs.tileCol = (gs.tileCol - 1).toShort()`). Discovered in seg004 translation (35+ errors in first pass).
- **Naming conventions for constants:** FrameIds use `FID.frame_N_name` (e.g., `FID.frame_15_stand`), SeqIds use `Seq.seq_N_name` (e.g., `Seq.seq_45_bumpfall`). Fix flags in `FixesOptionsType` are `Int` not `Boolean` — check with `!= 0`.
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
**Module:** 10 — Layer 1: seg005 (Player control, sword fighting) — **IN PROGRESS**
**Phase:** 10a — COMPLETE (14 functions translated, 36 new tests, 268 total pass). Next: Phase 10b (standing control, climbing, items).
**Blocked/Broken:** None.

### Module 10: seg005.c → Kotlin

**Scope:** 1,172 lines, 38 functions (excluding `teleport` — `USE_TELEPORTS` only, not in reference build). Player control dispatch, movement, falling/landing, item pickup, sword fighting, parry.

**Dependencies:** seg006 (complete — provides tile queries, physics, `playSeq`, `charDxForward`, `distanceToEdgeWeight`, `getEdgeDistance`, `charOppDist`, `canGrab`, `releaseArrows`, `incCurrRow`, `loadFramDetCol`, `getTileDivModM7`, `backDeltaX`, `dxWeight`, `distanceToEdge`, etc.), seg004 (complete — provides `clearCollRooms`, `canBumpIntoGate`, `inWall`). External stubs needed: `playSound` (already stubbed), `checkSoundPlaying` (stubbed), `startChompers` (seg007, stubbed), `isSpikeHarmful` (seg007, stubbed), `doPickup` (seg003, needs stub), `leaveGuard` (seg002, needs stub).

**New globals (seg005-local):** `sourceModifier`, `sourceRoom`, `sourceTilepos` (3 variables — teleport only, may skip).

**`#ifdef` flags (18 unique):** `FIX_GLIDE_THROUGH_WALL`, `FIX_JUMP_THROUGH_WALL_ABOVE_GATE`, `FIX_DROP_THROUGH_TAPESTRY`, `USE_SUPER_HIGH_JUMP`, `FIX_OFFSCREEN_GUARDS_DISAPPEARING`, `FIX_LAND_AGAINST_GATE_OR_TAPESTRY`, `FIX_SAFE_LANDING_ON_SPIKES`, `ALLOW_CROUCH_AFTER_CLIMBING`, `FIX_MOVE_AFTER_DRINK`, `FIX_MOVE_AFTER_SHEATHE`, `FIX_TURN_RUN_NEAR_WALL`, `FIX_EDGE_DISTANCE_CHECK_WHEN_CLIMBING`, `FIX_JUMP_DISTANCE_AT_EDGE`, `FIX_UNINTENDED_SWORD_STRIKE`, `FIX_EXIT_DOOR` (in `up_pressed`), `USE_REPLAY` (stub-only), `USE_COPYPROT` (skip — level 15 copy protection), `USE_TELEPORTS` (skip).

**Wire-up:** `seqtblOffsetChar` and `seqtblOffsetOpp` stubs in ExternalStubs already point to `SequenceTable.seqtblOffsets[]`. When Seg005 is created, wire `ExternalStubs.control`, `ExternalStubs.drawSword`, `ExternalStubs.spiked` to Seg005's implementations.

**Steps:**

**Phase 10a — Falling, landing, movement basics (14 functions)**
- Falling/landing: `seqtblOffsetChar`, `seqtblOffsetOpp`, `doFall`, `land`, `spiked`
- Control dispatch: `control`, `controlCrouched`, `controlTurning`, `crouch`
- Movement: `forwardPressed`, `backPressed`, `controlRunning`, `controlStartrun`, `safeStep`
- Unit tests: fall/land sequences, spike handling, control dispatch by frame, movement edge cases
- **Test:** `gradle build` clean, `gradle test` all pass

**Phase 10b — Standing control, climbing, items (14 functions)**
- Standing: `controlStanding`, `upPressed`, `downPressed`, `goUpLeveldoor`
- Jumping: `standingJump`, `checkJumpUp`, `jumpUpOrGrab`, `grabUpNoFloorBehind`, `jumpUp`, `grabUpWithFloorBehind`, `runJump`
- Hanging: `controlHanging`, `canClimbUp`, `hangFall`
- Items: `checkGetItem`, `getItem`
- Unit tests: standing control branches, jump/grab logic, hanging control, item pickup
- **Test:** `gradle build` clean, `gradle test` all pass

**Phase 10c — Sword fighting (8 functions) + review**
- Sword: `drawSword`, `controlWithSword`, `swordfight`, `swordStrike`, `parry`, `backWithSword`, `forwardWithSword`
- Wire-up: connect Seg005 functions to ExternalStubs (`control`, `drawSword`, `spiked`, `doFall`)
- Code review: naming consistency, integer semantics, `#ifdef` paths
- Unit tests: sword draw/sheathe, strike/parry sequences, distance-based sword control
- **Test:** `gradle build` clean, `gradle test` all pass
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

### Module 9: Layer 1 seg004 — COMPLETE
One-line: Translated seg004.c → Kotlin (26 functions, 621 lines C → Seg004.kt, 42 new tests, 232 total pass, zero escalations). See DEVLOG §Module 9.
