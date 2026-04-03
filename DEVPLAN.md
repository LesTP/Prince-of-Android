# DEVPLAN — Prince of Persia Android Port

## Cold Start Summary

**What this is:** Android port of SDLPoP (Prince of Persia), testing autonomous AI porting methodology.

**Key constraints:**
- SDLPoP is ~25,000 lines of C with 344 global variables (tight coupling)
- Replay system (.P1R files) provides deterministic test oracle
- Target language: Kotlin (native Android, validation via replay oracle)

**Environment:** Raspberry Pi 5 (16GB), Incus container `claude-code` (Debian 12 arm64, 12GB RAM, 3 CPUs)
**Project path:** `/home/claude/workspace/PoP port/` (NTFS USB drive, 466GB, 126GB free)
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
**Focus:** Module 6 — State Model (types.h + data.h → Kotlin)
**Blocked/Broken:** None — all toolchain verified.

**Migration from Windows (2026-04-03) — COMPLETE.** See DEVLOG §Pi Migration.

**Tracks overview:**
- **Track A (Game Logic):** C→Kotlin translation of ~7,200 lines, validated by replay oracle. **Full shell access on Pi — true autonomous mode.** Next.
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

## Module 6: State Model (types.h + data.h → Kotlin)

**Regime:** Build (autonomous)
**Source:** `types.h` (structs, enums, typedefs), `data.h`/`data.c` (344 global variables)
**Target:** `SDLPoP-kotlin/src/main/kotlin/com/sdlpop/game/` package

### Scope

Translate the C type definitions and global state into Kotlin. Only types and globals used by Layer 1 game logic (seg002/004/005/006/007) are in-scope. SDL/platform types (surfaces, textures, renderers, joystick, font, dialog) are excluded — they belong to Layers 3-4.

### Phase 6a: Core Game Types

Translate struct types used by game logic:
1. **Type aliases** — `byte`→`Int`, `sbyte`→`Int`, `word`→`Int`, `short`→`Short`, `dword`→`Long` (document masking conventions)
2. **char_type** — 16-byte character struct (Kid, Guard, Char, Opp). Data class with mutable fields.
3. **trob_type** — 3-byte animated tile state. Data class.
4. **mob_type** — 6-byte movable object state. Data class.
5. **level_type** — ~2305-byte level data (fg/bg tiles, room links, guard data). Data class.
6. **link_type** — 4-byte room link. Data class.
7. **tile_and_mod** — 2-byte tile+modifier pair.
8. **frame_type** — 5-byte animation frame descriptor.
9. **auto_move_type** — automatic movement entry.
10. **sword_table_type** — sword position entry.
11. **rect_type** — 4-short rectangle (used by rendering but also collision).

**Test:** Compile check. Verify struct sizes match C via unit test (char_type=16, trob_type=3, mob_type=6, level_type=2305).

### Phase 6b: Game Enums

Translate enum constants used by game logic:
1. **tiles** (0-30) — tile type IDs
2. **charids** — character type IDs
3. **actions** — action state IDs
4. **directions** — direction values (0, -1, 0x56)
5. **sword_status** — sword states
6. **frame_flags** — frame flag bitmasks
7. **soundflags**, **chtabs**, **blitters** — referenced by game logic for sound queueing and sprite lookup

**Test:** Compile check. Spot-check enum values match C.

### Phase 6c: Global State Object

Translate the ~344 global variables from data.h into a Kotlin singleton object:
1. **GameState object** — mutable properties for all game-logic globals
2. **Oracle-critical globals:** Kid, Guard, Char, Opp, current_level, drawn_room, rem_min, rem_tick, hitp_curr/max, guardhp_curr/max, curr_room_tiles[30], curr_room_modif[30], trobs[30], mobs[14], trobs_count, mobs_count, random_seed
3. **Control input globals:** control_x, control_y, control_shift, control_forward, control_backward, control_up, control_down, control_shift2, ctrl1_*
4. **Game logic globals:** All remaining variables from data.h used by seg002-007 (level, room_L/R/A/B/etc., collision arrays, tile state, guard state, etc.)
5. **Constant arrays:** y_land[], x_bump[], dir_front[], dir_behind[], tbl_line[], sound_interruptible[]
6. **custom_options_type** — game configuration with default values (guard skills, level tables, etc.)
7. **Exclude:** SDL surfaces, textures, renderers, window, joystick, font/text types, menu state, key scancodes

**Test:** Compile check. Unit test verifying default values for critical fields match C (e.g., custom_defaults.start_minutes_left=60, start_hitp=3, tbl_guard_hp values).

### Phase 6d: Compile & Structural Verification

1. Run `gradle build` — must compile cleanly with existing P1R parser
2. Unit tests: struct size assertions, default value spot-checks, enum value checks
3. Verify no SDL/platform dependencies leaked in

**Exit criteria:** All Kotlin types and globals compile. Unit tests pass. Ready for Module 7 (seqtbl) and Module 8 (seg006).

### Decisions Needed
- None anticipated. This is mechanical translation.
