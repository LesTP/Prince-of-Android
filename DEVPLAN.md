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
**Module:** 14 — Replay Runner (Kotlin replay playback + trace writer) — **IN PROGRESS**
**Phase:** 14b — Non-rendering frame lifecycle reconciliation — **IN PROGRESS**
**Next:** Step 14b.3: rerun the real-trace regression workflow after the new lifecycle shim, apply no more than two targeted fixes for remaining true lifecycle/game-logic divergences, and either close Module 14 with byte-identical traces or escalate with replay/frame/field/expected/actual details and suspected Module 15 boundary.
**Blocked/Broken:** None. Step 14b.2 implemented the pinned headless lifecycle shim and `gradle test --no-daemon` passes. The exploratory `gradle test layer1ReplayRegression --rerun-tasks --no-daemon` still fails with triage-ready divergences: `basic_movement` frame 0 `curr_room_modif[10]` expected `3` actual `2`; most traces now first diverge at frame 0 `drawn_room` with actual `16`; `original_level5_shadow_into_wall` frame 0 `Guard.frame` expected `15` actual `166`; `demo_suave_prince_level11` frame 29 `Kid.frame` expected `16` actual `1`. Actual traces remain under `SDLPoP-kotlin/build/oracle/layer1-regression/workflow/real-kotlin/`.

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
One-line: Delivered the trace oracle foundation, state snapshot writer, and manifest-driven regression workflow; review accepted after one should-fix, human approval recorded on 2026-04-15, and `gradle test layer1ReplayRegression --rerun-tasks` passed. See DEVLOG §Module 13.

### Module 14: Replay Runner — IN PROGRESS
One-line: Build Kotlin replay playback through the translated game loop, produce real Kotlin state traces, and wire those traces into the Phase 13a regression harness.

#### Phase 14a: Kotlin replay playback and trace producer — ESCALATION BYPASSED
One-line: Replace the Phase 13a copy-based producer with real Kotlin trace generation from `.P1R` replay inputs, a narrow Layer 1 frame driver, replay move hooks, and 310-byte state snapshot output.

**Regime:** Build — deterministic replay I/O and state comparison are machine-verifiable.

**Scope:** Implement the minimal replay-runner surface needed by Track A. This phase may translate replay move consumption from `replay.c` and the small non-rendering frame orchestration needed to call translated Layer 1 logic, but it must not expand into full `seg000`/`seg001`/`seg003` game-loop translation, SDL/platform behavior, rendering, audio, menus, or Android integration.

**Steps:**
- **14a.1** Replay manifest and initialization — COMPLETE: mapped the 13 `Layer1RegressionManifest` ids to their `.P1R` files, parsed replay metadata through `P1RParser`, seeded `GameState` replay fields (`replaying`, `startLevel`, `randomSeed`, `numReplayTicks`, format/version/deprecation values), and tested path resolution plus initialization behavior.
- **14a.2** Replay input hooks — COMPLETE: translated the replay move consumption needed by `ExternalStubs.doReplayMove`, decoded packed per-tick control bytes into `control_x`, `control_y`, `control_shift`, handled validate seek/skipping state and end-of-replay completion, restored saved replay RNG seed, and fixed/tested the old-version `g_deprecation_number` branch used by seg007 loose-floor RNG behavior.
- **14a.3** Layer 1 frame driver — COMPLETE: added `Layer1FrameDriver` in `com.sdlpop.replay` with `playFrame`, `playKidFrame`, and `playGuardFrame` orchestration over translated Layer 1 entry points in SDLPoP order, plus focused tests for call order and deterministic replay input consumption without SDL, Android, file I/O, rendering, or audio dependencies in `com.sdlpop.game`.
- **14a.4** Real trace producer workflow — ESCALATION BYPASSED: real `.P1R` trace production is wired into `Layer1RegressionHarness`, but the forced workflow fails after two targeted fix attempts with first-frame divergences. The ordinary unit suite passes; the dedicated regression task reports triage-ready replay/frame/field/expected/actual/actual-trace details. Human/orchestrator authorization on 2026-04-15 bypassed the escalation stop and directed continuation into Phase 14b.

**Acceptance:** The dedicated Layer 1 regression workflow uses real Kotlin-produced traces under `build/oracle/layer1-regression`. Any trace divergence must report replay id, frame, field, expected value, actual value, and actual trace path. True game-logic divergences get no more than two targeted fix attempts before escalation with the replay/frame/field details.

#### Phase 14b: Non-rendering frame lifecycle reconciliation — IN PROGRESS
One-line: Resolve the Phase 14a replay-regression escalation by adding the smallest non-rendering game-loop/timer lifecycle slice needed for Kotlin replay traces to match the C validate runner.

**Regime:** Build — expected behavior is machine-verifiable through the 13-trace replay regression workflow.

**Scope:** Reconcile replay-runner frame lifecycle state that sits just outside the Phase 14a Layer 1 frame driver, especially timer decrement/order, per-frame sequencing, guard frame setup, and validate-mode trace timing. This phase may translate or model the minimal relevant `seg000`/`seg003` lifecycle behavior required for deterministic replay validation, but it must not expand into SDL/platform behavior, rendering, audio, menus, Android integration, or a general game-loop port.

**Steps:**
- **14b.1** Lifecycle audit and contract pinning — COMPLETE: C validate mode starts replay state, restores the savestate, resets `curr_tick`, then each `play_frame()` consumes replay input inside `play_kid()`/`control_kid()`, runs the non-rendering `seg000` frame sequence, calls `show_time()` before `dump_frame_state()`, and only then writes the 310-byte trace frame. The Kotlin runner restores the same savestate and replay input, but currently serializes immediately after a narrower Layer 1 driver. The minimum contract for 14b.2 is to add the deterministic headless lifecycle slice needed before trace serialization: timer decrement semantics from `show_time()`, the missing non-rendering `seg000`/`seg003` frame calls, and C-equivalent guard save behavior, without importing SDL/platform/render/audio/menu responsibilities.
- **14b.2** Minimal lifecycle shim — COMPLETE: expanded `Layer1FrameDriver` with the pinned non-rendering `seg000`/`seg003`/`seg008` calls, added `HeadlessFrameLifecycle` for guard visibility, bump/knock handling, sword sound boundary, HP delta application, room-transition helpers, and `show_time()` timer semantics, switched Guard save to `saveshad()`, and covered the new call order/timer/save behavior with focused tests. `gradle test --no-daemon` passes; the full real-trace regression still fails and remains Step 14b.3's closure target.
- **14b.3** Real-trace regression closure — PLANNED: rerun `gradle test layer1ReplayRegression --rerun-tasks` against all 13 real Kotlin traces, apply no more than two targeted fixes for true lifecycle/game-logic divergences, and either close Module 14 with byte-identical traces or escalate with replay/frame/field/expected/actual details and suspected Module 15 boundary.

**Lifecycle contract pinned by 14b.1:**
- C trace timing: `dump_frame_state()` is called inside `seg000.c::play_frame()` after the game-logic sequence and after `show_time()`. Therefore frame 0 in the reference trace is not the raw replay savestate; it is the post-frame state after one timer update.
- Timer scope: only the deterministic side effects of `seg008.c::show_time()` are in Phase 14b scope: blink-state toggle, conditional `rem_tick`/`rem_min` decrement, `is_show_time`, and text timer values that influence future timer display behavior. Drawing text, HP, surfaces, palettes, and sound playback remain out of scope.
- Frame sequence scope: the Kotlin frame shim must align with C `seg000.c::play_frame()` for non-rendering state calls: `do_mobs`, `process_trobs`, `check_skel`, `check_can_guard_see_kid`, Kid frame, Guard frame, sword hurt checks, `check_sword_vs_sword`, `do_delta_hp`, `exit_room`, `check_the_end`, `check_guard_fallout`, and `show_time`.
- Kid/Guard subframe scope: the existing Layer 1 driver is missing `bump_into_opponent()` and `check_knock()` in the Kid subframe, and its Guard save hook uses `saveshad_and_opp()` while C `play_guard_frame()` calls `saveshad()`. Step 14b.2 should either translate the needed `seg003` helpers or keep them as explicit no-op boundaries only if focused tests and traces prove they are not active.
- Replay input scope: `do_replay_move()` remains consumed from `Seg006.controlKid()` during the Kid subframe. The replay runner should not consume moves before `play_frame()`, because C restores RNG/validate seek state at tick 0 from inside the first Kid control path.

**Acceptance:** The ordinary Kotlin test suite remains green, and the dedicated Layer 1 replay regression workflow either passes with real Kotlin-produced traces for all 13 manifests or escalates after two targeted fixes with triage-ready divergence details. Any added lifecycle behavior must remain deterministic, headless, and free of SDL, rendering, audio, menu, or Android dependencies.
