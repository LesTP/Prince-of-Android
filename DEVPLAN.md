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
**Module:** 8 — Layer 1: seg006 (Character physics, tile queries) — **IN PROGRESS**
**Phase:** 8a — Constants, frame tables, tile/room queries, state management
**Blocked/Broken:** None.

### Module 8: seg006.c → Kotlin

**Scope:** 2,154 lines, 81 functions. Most-called file in Layer 1 — all other modules (seg004, seg005, seg002, seg007) depend on it.

**Architecture decision:** seg006 calls functions from other segments (seg000, seg002, seg003, seg005, seg007, seg008, replay). These external dependencies will be handled via a stub/interface pattern: define callable function references that throw `NotImplementedError` until the corresponding module is translated. This keeps seg006 compilable and testable in isolation.

**Feature flags:** SDLPoP has ~12 `#ifdef` bug-fix flags (FIX_CORNER_GRAB, USE_REPLAY, etc.). Translation includes the fix-enabled code paths (matching the reference build configuration). Conditional compilation becomes runtime checks against `GameState.custom` / `GameState.fixes`.

**Steps:**

**Phase 8a — Constants, frame tables, tile/room queries, state management**
- Frame data tables: `frame_table_kid[]`, `frame_tbl_guard[]`, `frame_tbl_cuts[]` (FrameType arrays)
- Constants: `SEQTBL_BASE` (0x196E), direction/position macros
- Tile/room query functions (16): `get_tile`, `find_room_of_tile`, `get_tilepos`, `get_tilepos_nominus`, `get_tile_div_mod_m7`, `get_tile_div_mod`, `y_to_row_mod4`, `get_tile_at_char`, `get_tile_above_char`, `get_tile_behind_char`, `get_tile_infrontof_char`, `get_tile_infrontof2_char`, `get_tile_behind_above_char`, `get_tile_front_above_char`, `wall_type`, `tile_is_floor`
- Character state save/load (16): `loadkid`, `savekid`, `loadshad`, `saveshad`, `loadkid_and_opp`, `savekid_and_opp`, `loadshad_and_opp`, `saveshad_and_opp`, `clear_char`, `reset_obj_clip`, `save_obj`, `load_obj`, `save_ctrl_1`, `rest_ctrl_1`, `clear_saved_ctrl`, `read_user_control`
- External dependency stubs file (functions from seg000/002/003/005/007/008/replay)
- Unit tests: tile query correctness, state save/restore round-trip
- **Test:** `gradle test` — all new + existing tests pass, `gradle build` compiles

**Phase 8b — Frame/animation loading, physics, collision**
- Frame loading (5): `load_frame`, `load_fram_det_col`, `determine_col`, `get_frame_internal`, `play_seq`
- Physics/distance (8): `dx_weight`, `char_dx_forward`, `obj_dx_forward`, `x_to_xh_and_xl`, `fall_accel`, `fall_speed`, `distance_to_edge`, `distance_to_edge_weight`, `back_delta_x`
- Collision/floor (6): `set_char_collision`, `check_on_floor`, `in_wall`, `can_grab_front_above`, `can_grab`, `clip_char`, `stuck_lower`
- Unit tests: frame loading with seqtbl data, distance calculations, collision checks
- **Test:** `gradle test` — all tests pass

**Phase 8c — Falling, grabbing, damage, objects**
- Falling/grabbing (5): `start_fall`, `fell_out`, `check_grab`, `check_grab_run_jump`, `check_action`
- Damage/health (6): `take_hp`, `check_spiked`, `check_spike_below`, `is_dead`, `play_death_music`, `draw_hurt_splash`
- Objects/items (7): `do_pickup`, `check_press`, `proc_get_object`, `set_objtile_at_char`, `add_sword_to_objtable`, `check_killed_shadow`, `on_guard_killed`
- Unit tests: fall physics, damage mechanics, object interaction
- **Test:** `gradle test` — all tests pass

**Phase 8d — Player/guard control, integration**
- Player control (7): `play_kid`, `control_kid`, `user_control`, `flip_control_x`, `release_arrows`, `do_demo`, `save_ctrl_1`/`rest_ctrl_1` (if not done in 8a)
- Guard control (3): `play_guard`, `control_guard_inactive`, `char_opp_dist`
- Remaining (2): `inc_curr_row`, `clear_char` (if not done in 8a)
- Full seg006 compilation verification — all 81 functions present
- Unit tests: control flow, guard behavior
- **Test:** `gradle build` clean, `gradle test` all pass (target: 77 existing + ~40-60 new)

**Phase 8e — Review and cleanup**
- Code review: naming consistency, integer semantics audit, dead code removal
- Verify all `#ifdef` paths translated correctly
- DEVLOG update, DEVPLAN cleanup

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

### Module 6: State Model — COMPLETE
One-line: Translated types.h + data.h → Kotlin (4 files, 27 tests pass). See DEVLOG §Module 6.

### Module 7: Sequence Table — COMPLETE
One-line: Translated seqtbl.c → Kotlin (SequenceTable.kt + enums, 2,310-byte array, 115 offsets, 50 new tests, 77 total pass). See DEVLOG §Module 7.
