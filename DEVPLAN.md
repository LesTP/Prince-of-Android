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
- **CharType fields are all `Int`:** Unlike GameState Short fields, CharType fields (`charid`, `x`, `y`, `direction`, `sword`, etc.) are all `Int`. No `.toByte()` or `.toShort()` needed for comparisons or assignments. Constants like `Dir.LEFT`, `CID.KID`, `Sword.DRAWN` are also `Int`. Discovered in seg005 Phase 10a (multiple compile errors from unnecessary conversions).
- **Naming conventions for constants:** FrameIds use `FID.frame_N_name` (e.g., `FID.frame_15_stand`), SeqIds use `Seq.seq_N_name` (e.g., `Seq.seq_45_bumpfall`). Fix flags in `FixesOptionsType` are `Int` not `Boolean` — check with `!= 0`.
- **Replay auto-exit:** Instrumented builds (`DUMP_FRAME_STATE`) auto-exit after replay ends. Essential for consistent trace lengths.
- **C struct sizes:** Always verify struct byte sizes against `typedef` definitions in `types.h`/`data.h`. Don't trust field counts — check each type (`byte`=1, `word`=2, `dword`=4, `short`=2). `char_type` is 16 bytes (not 17), `start_level` is `word` (2 bytes, not 4).
- **Gradle 9.x JUnit:** Add `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` to `build.gradle.kts` or test executor fails to start with "Failed to load JUnit Platform."
- **Gradle native services on this container:** Fresh `gradle` runs currently fail before task execution with `Failed to load native library 'libnative-platform.so' for Linux aarch64`. Existing test artifacts remain reviewable, but new Gradle verification is blocked until the environment is repaired or a working Gradle distribution is provided.
- **SDLPoP replay invocation (Linux):** Run from `/tmp/sdlpop/` (binary copied there for execute permission). Use `SDL_VIDEODRIVER=offscreen SDL_AUDIODRIVER=dummy ./prince validate "replays/foo.p1r" seed=12345`. Output: `state_trace.bin`.
- **SDL headless mode:** Pi has no display. Use `SDL_VIDEODRIVER=offscreen` (not `dummy` — dummy hangs). Requires `xvfb` package installed but `offscreen` driver doesn't need it.
- **NTFS execute permissions:** USB drive is NTFS — `chmod +x` is silently ignored. Workaround: copy binaries to `/tmp/sdlpop/` with symlinks to data/replays/doc/SDLPoP.ini.
- **Line endings:** Source files from Windows have CRLF. Run `dos2unix *.c *.h` in SDLPoP/src/ after any file transfer. Grep/ripgrep fail silently on CRLF files.
- **C unsigned word comparisons:** `(word)x < (word)y` in C casts to unsigned 16-bit. Translate as `(x and 0xFFFF) < (y and 0xFFFF)`. Common in distance checks (seg005 `controlStanding`, `controlWithSword`). Getting this wrong produces subtle distance-check bugs.
- **`getTile()`-driven tests must seed level data, not room buffers:** `getTile()` calls `getRoomAddress()`, which reloads `currRoomTiles[]`/`currRoomModif[]` from `gs.level.fg[]`/`gs.level.bg[]`. In tests like `checkSkel`, write fixture tiles to `level` arrays or the setup will be overwritten.
- **Test `@BeforeTest` must not zero shared arrays globally:** Resetting shared arrays like `soundInterruptible` by filling with zeros corrupts default values that other test suites (e.g., `TypesTest`) validate. Only reset the specific entries modified by each test.
- **Seg007 RNG tests must restore seed init state:** Loose-floor shaking consumes RNG and can set `GameState.seedWasInit`; restore it after focused seg007 tests because `TypesTest` validates singleton defaults.
- **Reference traces:** Regenerated all 13 on ARM64 Pi (2026-04-03). Sizes match expected frame counts. Determinism verified.
- **Build commands:** C: `cd SDLPoP/src && make -j3` (add `CPPFLAGS="-Wall -D_GNU_SOURCE=1 -DDUMP_FRAME_STATE -DUSE_REPLAY"` for instrumented build). Kotlin: `cd SDLPoP-kotlin && gradle build` / `gradle test`. Traces: `python3 tools/compare_traces.py ref.trace test.trace`.
- **Trace generation:** From `/tmp/sdlpop/`: `SDL_VIDEODRIVER=offscreen SDL_AUDIODRIVER=dummy ./prince validate "replays/foo.p1r" seed=12345` → outputs `state_trace.bin` (310 bytes/frame).

## Current Status

**Track:** A — Game Logic Translation (Build regime, autonomous)
**Module:** 14 — Replay Runner (Kotlin replay playback + trace writer) — **PENDING**
**Phase:** None active
**Next:** Phase Plan for Module 14: define the Kotlin replay playback path through the translated game loop and replace the Phase 13a trace-copy producer hook with real Kotlin trace output.
**Blocked/Broken:** None. Fresh `gradle test layer1ReplayRegression --rerun-tasks` passed in `SDLPoP-kotlin` on 2026-04-15 during Phase 13a completion.

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

### Module 10: Layer 1 seg005 — COMPLETE
One-line: Translated seg005.c → Kotlin (38 functions, 1,172 lines C → Seg005.kt, 75 new tests, 307 total pass, zero escalations). See DEVLOG §Module 10.

### Module 11: Layer 1 seg002 — COMPLETE
One-line: Translated seg002.c → Seg002.kt across phases 11a-11c, including guard AI, room transitions, sword combat detection, skeleton/shadow logic, and stub wire-up; review clean. See DEVLOG §Module 11.

### Module 12: Layer 1 seg007 — COMPLETE
Human audit approved on 2026-04-15; proceed to Module 13.

#### Phase 12a: Trob core, redraw helpers, trap/button animation — COMPLETE
One-line: Translated trob pipeline, animated-tile state machines (torch/potion/sword/chomper/spike/button/gate/leveldoor), trigger plumbing, and 6 ExternalStubs wire-ups into Seg007.kt (3 steps, 70 tests, 519 total pass, zero escalations). See DEVLOG §Module 12.

#### Phase 12b: Loose-floor mobs and remaining seg007 functions — COMPLETE
One-line: Translated loose-floor fall initiation, falling-object simulation, Kid collision/death handling, row/room transitions, redraw bookkeeping, and mob object-table writes into Seg007.kt (3 steps, 21 new tests, 540 total pass, zero escalations). See DEVLOG §Module 12.

### Module 13: Layer 1 integration test — COMPLETE
One-line: Built the Kotlin replay-regression harness around the translated Layer 1 game logic: 310-byte trace parsing/comparison, `GameState` snapshot serialization, all-13-trace manifest coverage, Gradle workflow, triage-ready divergence reports, and explicit Module 14 handoff for real replay playback. See DEVLOG §Module 13.

#### Phase 13a: Layer 1 replay regression harness — COMPLETE
One-line: Delivered the trace oracle foundation, state snapshot writer, and manifest-driven regression workflow; review accepted after one should-fix, and `gradle test layer1ReplayRegression --rerun-tasks` passed. See DEVLOG §Module 13.

### Module 14: Replay Runner — PENDING
One-line: Build Kotlin replay playback through the translated game loop, produce real Kotlin state traces, and wire those traces into the Phase 13a regression harness.
