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
- **`Char.x`/`Char.y` must wrap as unsigned byte (0-255):** In C, `char_type.x` and `char_type.y` are `byte` (unsigned 8-bit). Arithmetic wraps implicitly. In Kotlin, they're `Int` — values go negative without `and 0xFF` masking. Critical in `playSeq()` (SEQ_DX, SEQ_DY), `fallSpeed()`, and `gotoOtherRoom()`. Without masking, `leave_room()`'s `chary >= 211` check fails on negative y values, blocking room transitions. Discovered in Phase 15b human-driven debug.
- **`restore_room_after_quick_load()` must run after savestate restore:** C reloads room buffers from `level.fg[]/bg[]` via `draw_game_frame()` with `different_room=1` before `draw_level_first()`. Without this, the savestate's stale `curr_room_modif[]` values cause frame-0 tile modifier divergences. Discovered in Phase 15b human-driven debug.
- **Headless replay needs sprite dimensions for collision:** `setCharCollision()` uses image width/height from `getImage()`. In headless mode, `getImage()` returns null → `charWidthHalf=0`, `charHeight=0`. This collapses the collision footprint, causing `checkSpikeBelow()` to miss spike tiles. Resolving this requires loading sprite dimension data from asset files. Identified as root cause of remaining 8/13 trace divergences.
- **`soundFlags` must include `sfDigi` for replay mode:** C reference builds always have digital sound enabled (`sound_flags & sfDigi`). This controls `lastLooseSound` tracking in `loose_shake()`'s `do-while` loop — without it, the loop iterates a different number of times, consuming extra `prandom(2)` calls and causing RNG drift. Set `soundFlags |= SoundFlags.DIGI` during replay initialization. Discovered in Phase 15a verification (4 traces had RNG drift, all fixed by this).
- **C room buffers are pointers into level data:** `curr_room_tiles`/`curr_room_modif` point directly into `level.fg`/`level.bg` in C. Kotlin uses copied room buffers, so replay-mode animated tile writes that update `currRoomModif` must also sync the matching `level.bg` entry or later room reloads can resurrect stale modifiers. Discovered in Step 15b.8 (`sword_and_level_transition` frame-0 modifier drift).
- **Enum object references:** `GameConstants.ROOMCOUNT` not `TileGeometry.ROOMCOUNT` — constants live in their defining object, not an alias. `Short == Int` comparisons need `.toInt()` on the Short side.
- **Reference traces:** Regenerated all 13 on ARM64 Pi (2026-04-03). Sizes match expected frame counts. Determinism verified.
- **Build commands:** C: `cd SDLPoP/src && make -j3` (add `CPPFLAGS="-Wall -D_GNU_SOURCE=1 -DDUMP_FRAME_STATE -DUSE_REPLAY"` for instrumented build). Kotlin: `cd SDLPoP-kotlin && gradle build` / `gradle test`. Traces: `python3 tools/compare_traces.py ref.trace test.trace`.
- **Trace generation:** From `/tmp/sdlpop/`: `SDL_VIDEODRIVER=offscreen SDL_AUDIODRIVER=dummy ./prince validate "replays/foo.p1r" seed=12345` → outputs `state_trace.bin` (310 bytes/frame).

## Current Status

**Track:** B — Android Platform (Rendering)
**Module:** 16 — Rendering (seg008/seg009/lighting → Android Canvas + asset pipeline)
**Phase:** 16a — Android project scaffold — **COMPLETE**
**Next:** Phase 16b (Asset loading pipeline — DAT decompression → Kotlin, Build regime, can run on Pi)

**Module 15 final results (2026-04-20):**
- Replay regression: **8/13 MATCH** (up from 1/13 Module 14 baseline)
- 573 unit tests pass
- Matching traces: `basic_movement`, `falling`, `original_level2_falling_into_wall`, `original_level5_shadow_into_wall`, `original_level12_xpos_glitch`, `snes_pc_set_level11`, `traps`, `trick_153`
- Remaining 5 divergences (root-caused, documented):
  1. `falling_through_floor_pr274` — frame 0 `curr_room_modif[17]` exp=6 act=4 (tile modifier init subtlety in `restore_room_after_quick_load` path, not game logic)
  2. `demo_suave_prince_level11` — frame 29 `Kid.frame` exp=16 act=1 (control dispatch: running stop → C chooses standing turn seq, Kotlin chooses start-run seq — likely demo-level autocontrol or input timing)
  3. `grab_bug_pr288` — frame 17 `Kid.frame` exp=91 act=40 (grab detection failure: Kid should grab ledge but continues jumping — `checkGrab()` collision bounds issue)
  4. `grab_bug_pr289` — frame 16 `Kid.frame` exp=91 act=102 (same grab detection failure class — Kid falls instead of grabbing)
  5. `sword_and_level_transition` — frame 275 `Kid.frame` exp=46 act=0 (level restart lifecycle: C respawns Kid after death, Kotlin stays dead — requires `start_game()`/`play_level()` outside headless scope)

**Phase 15b final results (human-driven debug session 2026-04-20):**
See DEVLOG §Module 15 for full details. Key fixes: `soundFlags` RNG init, `Char.x/y` byte overflow masking, `restore_room_after_quick_load()` equivalent.

**Phase 15a results:**
See DEVLOG §Module 15 for full details. Key: seg003 translation (22 functions), stub wiring, 566 tests.

**Phase 15a results:**
See DEVLOG §Module 15 for full details. Key: seg003 translation (22 functions), stub wiring, `soundFlags` fix, 566 tests.

**Phase 15b autonomous iteration results (steps 15b.1–15b.8):**
See DEVLOG §Module 15 for full step-by-step details. Summary: 8 autonomous iterations translated `draw_level_first()`, `draw_game_frame()`, `redraw_screen()` state effects into headless replay shim. Escalated 3 times at 4/13 traces. Human debug session then found 3 additional bugs (byte overflow, `restore_room_after_quick_load()`, sprite dimensions) to reach 8/13.

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

#### Phase 15b: seg000 frame lifecycle alignment — COMPLETE (5/13, remaining deferred)
One-line: Translated seg000 initialization, room-transition, and draw-frame paths. Fixed byte overflow, savestate restore, and soundFlags bugs. 5/13 traces match; remaining 8 root-caused to missing sprite dimensions in headless mode (deferred to Module 16).

**Regime:** Build (semi-autonomous) + human-driven debug session.

**Steps 15b.1–15b.8:** See step results above (autonomous iterations).

**Human-driven debug session (2026-04-20):**
- RNG diagnostic instrumentation identified `soundFlags` bug (resolved 4 RNG-drift traces)
- Per-frame Kid state diagnostic identified `Char.y` byte overflow bug (resolved room transition failures)
- C source audit identified missing `restore_room_after_quick_load()` (resolved frame-0 tile modifiers)
- Trobs diagnostic identified missing spike trob root-caused to `setCharCollision()` returning zero dimensions in headless mode (deferred — requires sprite asset loading)

**Acceptance:** 5/13 traces match. Remaining 8 divergences are root-caused and documented. The 13/13 target is blocked by headless mode lacking sprite dimensions, which is a Module 16 (Rendering) dependency. Phase 15b is complete within its scope.

**Next action:** Phase review, then decide whether to add sprite dimension loading as Phase 15c or defer to Module 16.

#### Phase 15c: Sprite dimension table for headless collision — COMPLETE (8/13)
One-line: Provided sprite width/height data via hardcoded lookup table extracted from SDLPoP PNG assets. `setCharCollision()` now computes correct collision bounds in headless mode. 8/13 traces match.

**Regime:** Build.

**What was done:** Created `SpriteDimensions.kt` with hardcoded (width, height) arrays for chtab 2 (KID, 219 sprites) and chtab 5 (GUARD, 34 sprites), extracted from PNG headers in `SDLPoP/data/KID/` and `SDLPoP/data/GUARD/`. Wired `ExternalStubs.getImage()` to return dimensions from the table instead of null. This gives `setCharCollision()` correct `charWidthHalf` and `charHeight` values, enabling proper collision footprint calculation for `checkSpikeBelow()`, `checkGrab()`, and other collision-dependent functions.

**Impact:** 3 additional traces now match (`basic_movement`, `snes_pc_set_level11`, `trick_153`, `traps` — 4 total new matches from Phase 15b's 5/13 → wait, from 5/13 to 8/13 = 3 new). Actually: Phase 15b ended at 5/13 with `falling_through_floor_pr274` matching. Phase 15c brought `basic_movement`, `snes_pc_set_level11`, `traps`, `trick_153` = 4 new matches but `falling_through_floor_pr274` regressed back to diverging. Net: 5 - 1 + 4 = 8/13.

**Remaining 5 divergences — entry criteria for Module 16:**
- `falling_through_floor_pr274` f0: tile modifier init — may resolve when full `restore_room_after_quick_load()` with `draw_room()` tile reload is implemented
- `demo_suave_prince_level11` f29: control dispatch — needs demo-level autocontrol input investigation
- `grab_bug_pr288` f17 + `grab_bug_pr289` f16: grab detection — `checkGrab()` fails to detect ledge. May be a collision bounds issue with sword-overlay sprite dimensions or a `checkGrabRunJump()` translation bug
- `sword_and_level_transition` f275: level restart — fundamentally outside headless replay scope; requires `start_game()`/`play_level()` lifecycle

### Module 16: Rendering — IN PROGRESS
One-line: Translate seg008.c + lighting.c rendering to Android Canvas, load real sprite assets from DAT/PNG files, and get level 1 visually rendering on an Android emulator.

**Environment:** Windows + Android Studio. Replaces Pi headless environment for rendering work. Replay regression suite continues to validate game logic correctness.

**Entry criteria from Module 15:**
- 8/13 replay traces match with headless shim + sprite dimension table
- 5 remaining divergences documented with root causes (see Module 15 final results)
- When Module 16 loads real sprite images from DAT/PNG assets, the `SpriteDimensions` hardcoded table should be replaced with actual loaded image dimensions, which may resolve the 2 grab-detection divergences (`grab_bug_pr288`, `grab_bug_pr289`)
- The `sword_and_level_transition` divergence requires level restart lifecycle (`start_game()`/`play_level()`) — this belongs to full game loop integration (Module 20), not rendering
- The `falling_through_floor_pr274` tile modifier init and `demo_suave_prince_level11` control dispatch divergences may resolve when the full rendering + game loop pipeline replaces the headless shim

**seg008.c decomposition (2,069 lines, 75 functions):**

| Category | Count | Description |
|----------|-------|-------------|
| Pure state logic | 30 | Zero SDL calls. Tile resolution, object tables, room loading, geometry, orchestration. |
| Render table submission (mixed) | 31 | Compute what to draw and append to `backtable[]`/`foretable[]`/`midtable[]`. No direct SDL, but in the render pipeline. |
| SDL rendering (direct) | 11 | `draw_tables`, `draw_back_fore`, `draw_mid`, `hflip`, `draw_image`, `draw_wipe`, `restore_peels`, `free_peels`, `display_text_bottom`, `erase_bottom_text`, `add_peel`. |
| Asset loading | 1 | `get_image` — bridges chtab → image reference. |

**lighting.c decomposition (120 lines, 3 functions):** All 3 functions (`init_lighting`, `redraw_lighting`, `update_lighting`) touch SDL directly. Entirely Refine.

**seg009.c asset pipeline (relevant subset):**
- 5 decompressors (`decompress_rle_lr/ud`, `decompress_lzg_lr/ud`) — 100% portable pure C
- `conv_to_8bpp`, `decompr_img`, `calc_stride` — portable
- `decode_image` — decompression portable, final step creates `SDL_Surface` → replace with `Bitmap`
- `load_image` — DAT path portable, PNG path replaces `IMG_Load_RW` with `BitmapFactory`
- `open_dat`, `load_from_opendats_*` — replace `fopen` with `AssetManager`

**Build/Refine boundary:** The render tables (`backtable[]`, `midtable[]`, `foretable[]`, `wipetable[]`, `objtable[]`) are the clean split. Everything that produces table entries is Build (autonomous). Everything that consumes them to put pixels on screen is Refine (human-assisted).

**Depends on:** Modules 6–15 (all game logic + game loop + replay pipeline)

#### Phase 16a: Android project scaffold — COMPLETE

**Regime:** Refine (human-driven).

**Scope:** Create the Android Studio project, wire existing Kotlin game logic modules as a dependency, configure Gradle for Android, set up a minimal `Activity` + `SurfaceView`, package SDLPoP `data/` directory as Android assets.

**What was done:**
- Root multi-module Gradle project with conditional `app` inclusion (Pi compatibility preserved)
- Android app module: SDK 24–34, Kotlin 1.9.22, depends on `:SDLPoP-kotlin`
- `GameActivity` (fullscreen landscape) + `GameSurfaceView` (SurfaceHolder.Callback, test frame)
- All SDLPoP `data/` assets packaged under `app/src/main/assets/`
- `SDLPoP-kotlin/build.gradle.kts` adjusted: JVM toolchain 21, root-relative system property paths

**Acceptance:** Android project builds and deploys to emulator. Existing Kotlin game logic compiles as part of the Android app. Verified.

#### Phase 16b: Asset loading pipeline — PENDING

**Regime:** Build (autonomous) with human visual verification at end.

**Scope:** Port the DAT file decompression and image decode chain from seg009.c to Kotlin. Replace `SDL_Surface` output with Android `Bitmap` (ARGB_8888). Replace `fopen` DAT/PNG loading with Android `AssetManager`.

**Functions to translate (autonomous — pure C, zero SDL):**
- `decompress_rle_lr` — RLE decompressor, left-to-right scan
- `decompress_rle_ud` — RLE decompressor, top-to-bottom scan
- `decompress_lzg_lr` — LZG (LZ77-variant) decompressor, left-to-right
- `decompress_lzg_ud` — LZG decompressor, column-major
- `decompr_img` — dispatcher: routes to correct decompressor by `cmeth` flag
- `conv_to_8bpp` — expands packed pixels (1/2/4bpp) to 1-byte-per-pixel
- `calc_stride` — bytes per row calculation

**Functions to translate (Refine — SDL→Android bridge):**
- `decode_image` — keep decompression; replace `SDL_CreateRGBSurface` + `SDL_SetPaletteColors` with `Bitmap.createBitmap()` (ARGB_8888) and manual palette application (VGA 6-bit → 8-bit via `<< 2`, color 0 transparent)
- `load_image` — DAT path uses portable decompressor; PNG path replaces `IMG_Load_RW()` with `BitmapFactory.decodeStream()` via `AssetManager`
- `open_dat` / `load_from_opendats_alloc` / `load_from_opendats_to_area` / `load_from_opendats_metadata` — replace `fopen`/`fread` with `AssetManager.open()`

**Test oracle:** Decode known sprites, verify pixel dimensions match `SpriteDimensions.kt` hardcoded values. For decompressors: round-trip or golden-output tests against known DAT resource bytes.

**Human work:** Visually verify decoded sprites look correct (palette, transparency, orientation). ~1 session.

**Acceptance:** `chtab_addrs[2]` (KID, 219 sprites) and `chtab_addrs[5]` (GUARD, 34 sprites) load from DAT/PNG assets with correct dimensions. Unit tests pass for all decompressors.

#### Phase 16c: Render table pure logic (seg008 state functions) — PENDING

**Regime:** Build (autonomous).

**Scope:** Translate the 30 pure-state functions from seg008.c. Zero SDL calls. Pure array/struct manipulation identical in character to Modules 8–12.

**Functions (grouped by subsystem):**

Room/tile loading:
- `load_room_links` (already done as `loadRoomAddress` — verify/extend)
- `load_leftroom`, `load_rowbelow`, `load_curr_and_left_tile`

Tile resolution:
- `get_tile_to_draw` — 104 lines of branching logic for fake tiles, button states, loose floors
- `can_see_bottomleft` — single tile-type comparison
- `get_spike_frame`, `get_loose_frame` — modifier-to-frame converters
- `calc_gate_pos` — gate geometry arithmetic

Object table management:
- `add_objtable`, `add_kid_to_objtable`, `add_guard_to_objtable`
- `load_obj_from_objtable`, `mark_obj_tile_redraw`
- `sort_curr_objs`, `compare_curr_objs` — object draw-order sort
- `load_frame_to_obj` — computes object position from frame data

Tile preprocessing:
- `alter_mods_allrm` — iterates all rooms preprocessing tile modifiers
- `load_alter_mod` — 143-line per-tile modifier logic (gates, loose, potions, walls, torches)

Geometry:
- `calc_screen_x_coord` — `x * 320 / 280` (fix existing identity stub)
- `add_drect` — dirty-rect intersection/union tracking

Orchestrators:
- `draw_room`, `draw_tile`, `draw_tile_aboveroom`
- `redraw_needed`, `redraw_needed_above`, `redraw_needed_tiles`
- `draw_moving`

**Test oracle:** Unit tests comparing computed render tables and object tables against expected values for known room layouts. Same pattern as Modules 8–12.

**Human work:** None during translation. Review at phase boundary.

**Acceptance:** All 30 functions translated. Existing game logic tests still pass. New unit tests cover tile resolution, object table population, and modifier preprocessing.

#### Phase 16d: Render table submission (seg008 mixed functions) — PENDING

**Regime:** Build (autonomous).

**Scope:** Translate the 31 functions that compute render commands and append to `backtable[]`/`foretable[]`/`midtable[]`/`wipetable[]`. These functions don't draw pixels — they populate render queues consumed downstream by Phase 16e.

**Functions (grouped by subsystem):**

Tile rendering submissions:
- `draw_tile_floorright`, `draw_tile_topright`, `draw_tile_anim_topright`
- `draw_tile_right`, `draw_tile_anim_right`
- `draw_tile_bottom`, `draw_tile_base`
- `draw_tile_anim`, `draw_tile_fore`
- `draw_loose`, `draw_tile2`, `draw_tile_wipe`

Table append helpers:
- `add_backtable`, `add_foretable`, `add_midtable`, `add_wipetable`

Structures:
- `draw_gate_back`, `draw_gate_fore`, `draw_leveldoor`
- `draw_floor_overlay`, `draw_other_overlay`

Characters/objects:
- `draw_people`, `draw_kid`, `draw_guard`
- `draw_objtable_items_at_tile`, `draw_objtable_item`

Wall generation:
- `wall_pattern` — 111-line PRNG-deterministic wall stone algorithm
- `draw_left_mark`, `draw_right_mark` — brick decal placement

Timer/text:
- `show_time` — 84-line timer countdown + text string building
- `show_level` — level number formatting

**Test oracle:** Unit tests verifying render table entries (sprite id, position, blitter mode) for known tile configurations.

**Human work:** None during translation. Review at phase boundary.

**Acceptance:** All 31 functions translated. Render tables populate correctly for test room configurations. Existing tests still pass.

#### Phase 16e: Android rendering backend — PENDING

**Regime:** Refine (human-driven).

**Scope:** Replace the SDL draw pipeline with Android `Canvas` drawing. This is the core visual implementation phase.

**Functions to replace (11 SDL rendering functions → Android Canvas):**
- `draw_tables` → iterate `backtable[]`/`midtable[]`/`foretable[]`, draw `Bitmap` sprites via `Canvas.drawBitmap()`
- `draw_back_fore` → resolve sprite from chtab, draw to Canvas
- `draw_mid` → horizontal flip via `Matrix.preScale(-1, 1)`, clip rect, peel save, draw
- `draw_image` → dispatcher to appropriate Canvas draw call
- `hflip` → `Matrix` transform or pre-flipped Bitmap cache
- `draw_wipe` / `draw_wipes` → `Canvas.drawRect()` with `Paint`
- `restore_peels` / `add_peel` / `free_peels` → `Canvas.save()`/`Canvas.restore()` or manual Bitmap snapshots
- `display_text_bottom` / `erase_bottom_text` → `Canvas.drawText()` with `Paint`

**Lighting (3 functions):**
- `init_lighting` → load `light.png` via `BitmapFactory`, create overlay Bitmap
- `redraw_lighting` → fill overlay with ambient color, stamp light mask at torch positions
- `update_lighting` → composite overlay onto frame buffer with `PorterDuff.Mode.MULTIPLY`

**Screen present pipeline:**
- `SurfaceView.surfaceCreated` → get `Canvas` from `SurfaceHolder`
- Per frame: lock Canvas → draw 320×200 offscreen buffer scaled to screen → unlock and post
- Frame pacing: `Choreographer` callback or `Thread.sleep` targeting 60fps

**Why Refine:** No replay oracle for rendering correctness. Visual output requires human eyes. Layer ordering, transparency, clipping, palette correctness — all need visual inspection.

**Human work:**
1. Implement `Canvas`-based `draw_tables` flush loop
2. Wire frame loop: game tick → render table population (Phase 16c/d) → Canvas flush → present
3. Debug layer ordering, transparency, clipping issues visually
4. Verify: level 1 room 1 renders correctly on emulator
5. Iterate on visual issues until rooms look right

**Acceptance:** Level 1 renders visually correct on Android emulator. Tiles, sprites, gates, potions, torches, and wall patterns are recognizable and correctly layered. Timer text displays. Guard and Kid sprites render in correct positions with correct frames.

#### Phase 16f: Real sprites → replay regression — PENDING

**Regime:** Build (autonomous) with human verification.

**Scope:** Replace `SpriteDimensions.kt` hardcoded lookup table with real image dimensions from the Phase 16b asset pipeline. Wire `ExternalStubs.getImage()` to return actual loaded `Bitmap` dimensions. Re-run 13-trace replay regression to check for improvements.

**Expected impact:** Loading real sprite images (including sword-overlay sprites in additional chtabs) may resolve the 2 grab-detection divergences (`grab_bug_pr288` f17, `grab_bug_pr289` f16) where `checkGrab()` fails to detect ledges, possibly due to missing sword-overlay sprite dimensions in the hardcoded table.

**Human work:** Run replay regression, verify match count. Triage any new divergences.

**Acceptance:** `SpriteDimensions.kt` hardcoded table replaced. `ExternalStubs.getImage()` returns dimensions from loaded assets. Replay regression re-run with results documented. Target: ≥8/13 (no regressions), ideally 10/13 if grab bugs resolve.

---

**Module 16 summary:**

| Phase | Scope | Regime | Human Assistance | Est. Effort |
|-------|-------|--------|-----------------|-------------|
| 16a | Android project scaffold | Refine | Create project, configure SDK, verify build | Half day |
| 16b | Asset loading pipeline | Build + human check | Visually verify decoded sprites | 2–3 iters + 1 session |
| 16c | Render table pure logic (30 fn) | Build | Review at boundary | 3–4 iters |
| 16d | Render table submission (31 fn) | Build | Review at boundary | 3–4 iters |
| 16e | Android rendering backend | Refine | Core visual debugging | 2–3 sessions |
| 16f | Real sprites → replay regression | Build + human check | Run regression, verify | 1 iter + 1 verify |

**Build/Refine boundary:** Render tables (`backtable[]`, `midtable[]`, `foretable[]`, `wipetable[]`, `objtable[]`) are the clean split. Everything producing table entries is Build. Everything consuming them to put pixels on screen is Refine.
