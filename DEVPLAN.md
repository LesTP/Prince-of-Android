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
- **`soundFlags` must include `sfDigi` for replay mode:** C reference builds always have digital sound enabled (`sound_flags & sfDigi`). This controls `lastLooseSound` tracking in `loose_shake()`'s `do-while` loop — without it, the loop iterates a different number of times, consuming extra `prandom(2)` calls and causing RNG drift. Set `soundFlags |= SoundFlags.DIGI` during replay initialization. Discovered in Phase 15a verification (4 traces had RNG drift, all fixed by this).
- **Enum object references:** `GameConstants.ROOMCOUNT` not `TileGeometry.ROOMCOUNT` — constants live in their defining object, not an alias. `Short == Int` comparisons need `.toInt()` on the Short side.
- **Reference traces:** Regenerated all 13 on ARM64 Pi (2026-04-03). Sizes match expected frame counts. Determinism verified.
- **Build commands:** C: `cd SDLPoP/src && make -j3` (add `CPPFLAGS="-Wall -D_GNU_SOURCE=1 -DDUMP_FRAME_STATE -DUSE_REPLAY"` for instrumented build). Kotlin: `cd SDLPoP-kotlin && gradle build` / `gradle test`. Traces: `python3 tools/compare_traces.py ref.trace test.trace`.
- **Trace generation:** From `/tmp/sdlpop/`: `SDL_VIDEODRIVER=offscreen SDL_AUDIODRIVER=dummy ./prince validate "replays/foo.p1r" seed=12345` → outputs `state_trace.bin` (310 bytes/frame).

## Current Status

**Track:** A → B transition — Game Loop Translation (Build regime, semi-autonomous)
**Module:** 15 — Game Loop (seg000/seg001/seg003 refactor + translate) — **IN PROGRESS**
**Phase:** 15a — seg003 translation + stub wiring — **COMPLETE (verified)**
**Phase:** 15b — seg000 frame lifecycle alignment — **Step 15b.3 ESCALATED**
**Next:** Human/orchestrator review of Step 15b.3 escalation. Replay regression remains 4/13 after the allowed two targeted fix attempts; no production code changes were retained.

**Step 15b.3 results (verified 2026-04-20):**
- Ran `gradle test --no-daemon`: passed.
- Ran `gradle layer1ReplayRegression --rerun-tasks --no-daemon`: failed with **4/13 MATCH**, preserving the Step 15b.2 match set (`falling`, `original_level2_falling_into_wall`, `original_level5_shadow_into_wall`, `original_level12_xpos_glitch`).
- Targeted fix attempt 1: added C `redraw_screen()`'s `exit_room_timer = 2` side effect to the headless redraw shim. Unit tests passed, but replay regression moved `sword_and_level_transition` to a new frame-0 `curr_room_modif[13]` divergence and changed `grab_bug_pr288` to a later `Kid.frame` divergence; reverted.
- Targeted fix attempt 2: tested C-equivalent trace timing by serializing before `headlessDrawGameFrame()`. Unit tests passed, but replay regression was unchanged at 4/13; reverted.
- No third targeted fix was attempted per Phase 15b acceptance. Superseded `HeadlessFrameLifecycle` cleanup was not performed because the phase acceptance gate is still failing.
- Remaining divergences: `basic_movement` f325 `Kid.frame` expected `103` actual `102`; `demo_suave_prince_level11` f29 `Kid.frame` expected `16` actual `1`; `falling_through_floor_pr274` f0 `curr_room_modif[17]` expected `6` actual `4`; `grab_bug_pr288` f11 `curr_room_tiles[2]` expected `0` actual `47`; `grab_bug_pr289` f16 `Kid.frame` expected `91` actual `102`; `snes_pc_set_level11` f40 `trobs_count` expected `3` actual `2`; `sword_and_level_transition` f138 `curr_room_tiles[0]` expected `52` actual `47`; `traps` f41 `Kid.frame` expected `50` actual `55`; `trick_153` f27 `Kid.y` expected `62` actual `251`.

**Step 15b.2 results (verified 2026-04-20):**
- Added `HeadlessFrameLifecycle.headlessDrawGameFrame()` and called it from `ReplayRunner.writeLayer1Trace()` after `playFrame()` and before trace serialization.
- The helper handles the state-bearing redraw flags from `draw_game_frame()`: `needFullRedraw`, `differentRoom`, and `needRedrawBecauseFlipped`; it advances `drawnRoom = nextRoom` for different-room redraws, clears `differentRoom`, and refreshes room links/current room buffers.
- Focused replay-runner tests cover full-redraw and different-room redraw handling, and `resetTraceRunState()` now clears `needFullRedraw`/`needRedrawBecauseFlipped`.
- A trial implementation that re-ran `animTileModif()`/`startChompers()` from the draw-frame hook was discarded because it duplicated `checkTheEnd()` room-entry initialization and regressed previously matching traces to frame-0 tile-modifier divergences.
- `gradle test --no-daemon` passed.
- Replay regression remains **4/13 MATCH**, preserving the Step 15b.1 match set (`falling`, `original_level2_falling_into_wall`, `original_level5_shadow_into_wall`, `original_level12_xpos_glitch`).
- Remaining divergences: `basic_movement` f325 `Kid.frame`; `demo_suave_prince_level11` f29 `Kid.frame`; `falling_through_floor_pr274` f0 `curr_room_modif[17]` expected `6` actual `4`; `grab_bug_pr288` f11 `curr_room_tiles[2]`; `grab_bug_pr289` f16 `Kid.frame`; `snes_pc_set_level11` f40 `trobs_count`; `sword_and_level_transition` f138 `curr_room_tiles[0]`; `traps` f41 `Kid.frame`; `trick_153` f27 `Kid.y`.

**Step 15b.1 results (verified 2026-04-20):**
- Added `HeadlessFrameLifecycle.headlessDrawLevelFirst()` and called it from `ReplayRunner.writeLayer1Trace()` after savestate restoration and before the first `playFrame()`.
- The helper follows C `draw_level_first()` room-entry gating: set `nextRoom = Kid.room`, run the existing `checkTheEnd()` setup when starting room differs from `drawnRoom`, otherwise refresh room links without re-randomizing same-room tile modifiers.
- `gradle test --tests com.sdlpop.replay.ReplayRunnerTest --no-daemon` passed; `gradle test --no-daemon` passed.
- Replay regression remains **4/13 MATCH**, preserving the Phase 15a match set (`falling`, `original_level2_falling_into_wall`, `original_level5_shadow_into_wall`, `original_level12_xpos_glitch`).
- `traps` moved from frame 0 `curr_room_modif[0]` to frame 41 `Kid.frame`, confirming initial different-room setup for that replay.
- `falling_through_floor_pr274` still diverges at frame 0 `curr_room_modif[17]` expected `6` actual `4`; this is now classified with same-room first-draw/redraw initialization for Step 15b.2.

**Phase 15a results (verified 2026-04-20):**
- 566 unit tests pass (up from 540 in Module 14)
- 3 compile errors fixed: `GameConstants.ROOMCOUNT` reference, `Short == Int` comparison, `tblSeamlessExit` bounds check
- `soundFlags |= SoundFlags.DIGI` fix resolved all 4 RNG-drift traces (root cause: `lastLooseSound` not tracked → `do-while` loop in `loose_shake()` consumed extra `prandom(2)` calls)
- Replay regression: **4/13 MATCH** (up from 1/13 Module 14 baseline)
  - New matches: `falling`, `original_level2_falling_into_wall`, `original_level5_shadow_into_wall`
  - Existing match: `original_level12_xpos_glitch`
- Remaining 9 divergences categorized:
  - Frame 0 tile modifiers (2): `traps`, `falling_through_floor_pr274` — missing `draw_level_first()` / `redraw_screen()` initialization
  - Mid-replay Kid.frame (4): `basic_movement` (f325), `demo_suave_prince_level11` (f29), `grab_bug_pr289` (f16), `trick_153` (f27) — frame lifecycle ordering in seg000
  - Tile/trob state (3): `grab_bug_pr288` (f11), `sword_and_level_transition` (f138), `snes_pc_set_level11` (f40) — room-transition tile initialization

**Phase 15a changes:** Wired `doPickup` to `Seg006.doPickup()`, implemented `addLife`/`featherFall`/`toggleUpside`/`expired`/`startGame` stubs, translated all 22 seg003.c functions into `Seg003.kt`, integrated `Seg003.timers()` into replay runner before each `playFrame()`, added level-specific exit events and `expired()` to frame driver, replaced `HeadlessFrameLifecycle` shims (`checkCanGuardSeeKid`, `bumpIntoOpponent`, `checkKnock`) with proper seg003 translations. Fixed `soundFlags` initialization for replay mode, added `lastLooseSound`/`soundFlags` reset to `resetTraceRunState`, added `testClassesDirs`/`classpath` to `layer1ReplayRegression` Gradle task.

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

### Module 14: Replay Runner — COMPLETE (known Layer 2 boundary limitations)
One-line: Built Kotlin replay playback pipeline with real `.P1R` trace production, Layer 1 frame driver, replay move hooks, headless lifecycle shim, and 310-byte state trace output. 1/13 traces match exactly; remaining divergences are Layer 2 game-loop behavior (seg000/seg003) deferred to Module 15. See DEVLOG §Module 14.

**Known limitations (deferred to Module 15):**
Remaining 12 trace divergences are caused by missing Layer 2 lifecycle behavior, not Layer 1 translation bugs:
- `do_pickup` (seg003) — unimplemented, crashes `original_level5_shadow_into_wall` at frame 48
- `do_delta_hp` (seg003) — guard HP not applied, causes `original_level2_falling_into_wall` guardhp divergence
- Guard spawn/room-transition lifecycle in seg000 — causes RNG drift in `basic_movement`, `falling`
- Savestate initialization sequence — causes frame-0 room buffer divergences in `falling_through_floor_pr274`, `grab_bug_pr288`
- Frame lifecycle ordering — causes Kid.frame divergences in `demo_suave_prince_level11`, `grab_bug_pr289`
Full triage details in DEVLOG §Step 14b.3. Actual traces under `SDLPoP-kotlin/build/oracle/layer1-regression/workflow/real-kotlin/`.

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

#### Phase 14b: Non-rendering frame lifecycle reconciliation — COMPLETE (remaining divergences deferred to Module 15)
One-line: Resolve the Phase 14a replay-regression escalation by adding the smallest non-rendering game-loop/timer lifecycle slice needed for Kotlin replay traces to match the C validate runner.

**Regime:** Build — expected behavior is machine-verifiable through the 13-trace replay regression workflow.

**Scope:** Reconcile replay-runner frame lifecycle state that sits just outside the Phase 14a Layer 1 frame driver, especially timer decrement/order, per-frame sequencing, guard frame setup, and validate-mode trace timing. This phase may translate or model the minimal relevant `seg000`/`seg003` lifecycle behavior required for deterministic replay validation, but it must not expand into SDL/platform behavior, rendering, audio, menus, Android integration, or a general game-loop port.

**Steps:**
- **14b.1** Lifecycle audit and contract pinning — COMPLETE: C validate mode starts replay state, restores the savestate, resets `curr_tick`, then each `play_frame()` consumes replay input inside `play_kid()`/`control_kid()`, runs the non-rendering `seg000` frame sequence, calls `show_time()` before `dump_frame_state()`, and only then writes the 310-byte trace frame. The Kotlin runner restores the same savestate and replay input, but currently serializes immediately after a narrower Layer 1 driver. The minimum contract for 14b.2 is to add the deterministic headless lifecycle slice needed before trace serialization: timer decrement semantics from `show_time()`, the missing non-rendering `seg000`/`seg003` frame calls, and C-equivalent guard save behavior, without importing SDL/platform/render/audio/menu responsibilities.
- **14b.2** Minimal lifecycle shim — COMPLETE: expanded `Layer1FrameDriver` with the pinned non-rendering `seg000`/`seg003`/`seg008` calls, added `HeadlessFrameLifecycle` for guard visibility, bump/knock handling, sword sound boundary, HP delta application, room-transition helpers, and `show_time()` timer semantics, switched Guard save to `saveshad()`, and covered the new call order/timer/save behavior with focused tests. `gradle test --no-daemon` passes; the full real-trace regression still fails and remains Step 14b.3's closure target.
- **14b.3** Real-trace regression closure — ESCALATED: reran `gradle test layer1ReplayRegression --rerun-tasks --no-daemon`, applied the two allowed targeted fixes (per-replay singleton reset and replay-only current-room buffer preservation/sync), and stopped after the workflow still failed. One trace now matches exactly (`original_level12_xpos_glitch`), several first-frame contamination failures moved later, and `original_level5_shadow_into_wall` now matches frames 0-47 before entering unimplemented `seg003::do_pickup` at frame 48. No third targeted fix was attempted; see Current Status and DEVLOG Step 14b.3 for replay/frame/field/expected/actual details.

**Lifecycle contract pinned by 14b.1:**
- C trace timing: `dump_frame_state()` is called inside `seg000.c::play_frame()` after the game-logic sequence and after `show_time()`. Therefore frame 0 in the reference trace is not the raw replay savestate; it is the post-frame state after one timer update.
- Timer scope: only the deterministic side effects of `seg008.c::show_time()` are in Phase 14b scope: blink-state toggle, conditional `rem_tick`/`rem_min` decrement, `is_show_time`, and text timer values that influence future timer display behavior. Drawing text, HP, surfaces, palettes, and sound playback remain out of scope.
- Frame sequence scope: the Kotlin frame shim must align with C `seg000.c::play_frame()` for non-rendering state calls: `do_mobs`, `process_trobs`, `check_skel`, `check_can_guard_see_kid`, Kid frame, Guard frame, sword hurt checks, `check_sword_vs_sword`, `do_delta_hp`, `exit_room`, `check_the_end`, `check_guard_fallout`, and `show_time`.
- Kid/Guard subframe scope: the existing Layer 1 driver is missing `bump_into_opponent()` and `check_knock()` in the Kid subframe, and its Guard save hook uses `saveshad_and_opp()` while C `play_guard_frame()` calls `saveshad()`. Step 14b.2 should either translate the needed `seg003` helpers or keep them as explicit no-op boundaries only if focused tests and traces prove they are not active.
- Replay input scope: `do_replay_move()` remains consumed from `Seg006.controlKid()` during the Kid subframe. The replay runner should not consume moves before `play_frame()`, because C restores RNG/validate seek state at tick 0 from inside the first Kid control path.

**Acceptance:** The ordinary Kotlin test suite remains green, and the dedicated Layer 1 replay regression workflow either passes with real Kotlin-produced traces for all 13 manifests or escalates after two targeted fixes with triage-ready divergence details. Any added lifecycle behavior must remain deterministic, headless, and free of SDL, rendering, audio, menu, or Android dependencies.

### Module 15: Game Loop — IN PROGRESS
One-line: Refactor and translate seg000/seg001/seg003 (Layer 2 game loop), resolving replay regression divergences by implementing the full non-rendering frame lifecycle.

**Regime:** Build (semi-autonomous). seg000 is heavily entangled with SDL (~95 calls) and requires manual refactoring to separate game logic from platform calls before translation. seg003 helper functions are closer to autonomous translation.

**Scope:** Translate the deterministic game-loop and helper functions in seg000.c (~2,200 lines), seg001.c (~800 lines), and seg003.c (~500 lines). Refactor to remove SDL dependencies, replacing platform calls with stubs or the existing headless shim pattern. The existing `Layer1FrameDriver` and `HeadlessFrameLifecycle` should be absorbed or replaced by the full game-loop translation.

**Primary acceptance test:** All 13 replay regression traces must match. The regression suite (`gradle layer1ReplayRegression --rerun-tasks`) is the acceptance gate.

**Current baseline:** 4/13 traces match after Phase 15a.

**Depends on:** Modules 6-14 (all Layer 1 game logic + replay runner pipeline)

#### Phase 15a: seg003 translation + stub wiring — COMPLETE
One-line: Translated 22 seg003 functions, wired stubs, fixed `soundFlags` RNG bug. 4/13 traces match (up from 1/13). 566 tests pass. See DEVLOG §Module 15.

#### Phase 15b: seg000 frame lifecycle alignment — IN PROGRESS
One-line: Translate seg000 initialization and room-transition paths to resolve remaining 9 trace divergences, targeting 13/13.

**Regime:** Build (semi-autonomous).

**Remaining divergences to resolve (post-15a):**
- Frame 0 tile modifiers (`falling_through_floor_pr274`): same-room first-draw/redraw initialization still missing after Step 15b.1
- Mid-replay Kid.frame/position divergences (`basic_movement` f325, `demo_suave_prince_level11` f29, `grab_bug_pr289` f16, `trick_153` f27, `traps` f41): frame lifecycle ordering — likely `need_full_redraw` / `redraw_screen()` path in `draw_game_frame()` that initializes animated tiles on room entry
- Tile/trob state divergences (`grab_bug_pr288` f11, `sword_and_level_transition` f138, `snes_pc_set_level11` f40): `draw_game_frame()` → `redraw_screen()` → `anim_tile_modif()` on room changes during gameplay

**Root cause:** Both groups share the same mechanistic root — C calls `anim_tile_modif()` from two paths: (1) `check_the_end()` on game-logic room transitions (already in Kotlin), and (2) `draw_game_frame()` → `redraw_screen()` on rendering room transitions including the initial `draw_level_first()` call (missing from Kotlin).

**Steps:**
- **15b.1** Initial room setup (`draw_level_first` equivalent) — COMPLETE: Added `headlessDrawLevelFirst()` to `ReplayRunner.writeLayer1Trace()` after savestate restoration but before first `play_frame()`. It runs `checkTheEnd()` for different starting rooms and refreshes room links for same-room starts. `traps` moved past its frame-0 tile-modifier divergence; `falling_through_floor_pr274` still needs same-room draw-frame initialization.
- **15b.2** Per-frame room-transition redraw bookkeeping (`draw_game_frame` equivalent) — COMPLETE: Added `headlessDrawGameFrame()` after `playFrame()` but before trace serialization. It handles `different_room`, `need_full_redraw`, and flipped redraw flags without duplicating `checkTheEnd()` animated-tile/chomper initialization. Regression remains 4/13.
- **15b.3** Regression verification and cleanup — ESCALATED: Full unit suite passes, but the 13-trace regression remains 4/13 after two targeted fix attempts. No production code changes were retained; see Current Status and DEVLOG Step 15b.3 for divergence details.

**Acceptance:** All 566+ unit tests pass, 13/13 replay regression traces match. No new SDL, rendering, audio, or Android dependencies. Escalate after two targeted fixes with triage-ready divergence details.

**Next action:** Implement Step 15b.3.
