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
- **SDLPoP replay invocation (Linux):** Run from `/tmp/sdlpop/` (binary copied there for execute permission). Use `SDL_VIDEODRIVER=offscreen SDL_AUDIODRIVER=dummy ./prince validate "replays/foo.p1r" seed=12345`. Output: `state_trace.bin`.
- **SDL headless mode:** Pi has no display. Use `SDL_VIDEODRIVER=offscreen` (not `dummy` — dummy hangs). Requires `xvfb` package installed but `offscreen` driver doesn't need it.
- **NTFS execute permissions:** USB drive is NTFS — `chmod +x` is silently ignored. Workaround: copy binaries to `/tmp/sdlpop/` with symlinks to data/replays/doc/SDLPoP.ini.
- **Line endings:** Source files from Windows have CRLF. Run `dos2unix *.c *.h` in SDLPoP/src/ after any file transfer. Grep/ripgrep fail silently on CRLF files.
- **C unsigned word comparisons:** `(word)x < (word)y` in C casts to unsigned 16-bit. Translate as `(x and 0xFFFF) < (y and 0xFFFF)`. Common in distance checks (seg005 `controlStanding`, `controlWithSword`). Getting this wrong produces subtle distance-check bugs.
- **Reference traces:** Regenerated all 13 on ARM64 Pi (2026-04-03). Sizes match expected frame counts. Determinism verified.
- **Build commands:** C: `cd SDLPoP/src && make -j3` (add `CPPFLAGS="-Wall -D_GNU_SOURCE=1 -DDUMP_FRAME_STATE -DUSE_REPLAY"` for instrumented build). Kotlin: `cd SDLPoP-kotlin && gradle build` / `gradle test`. Traces: `python3 tools/compare_traces.py ref.trace test.trace`.
- **Trace generation:** From `/tmp/sdlpop/`: `SDL_VIDEODRIVER=offscreen SDL_AUDIODRIVER=dummy ./prince validate "replays/foo.p1r" seed=12345` → outputs `state_trace.bin` (310 bytes/frame).

## Current Status

**Track:** A — Game Logic Translation (Build regime, autonomous)
**Module:** 11 — Layer 1: seg002 (Guard AI, room transitions) — **IN PROGRESS**
**Phase:** 11b — Guard AI & autocontrol — **PLANNED** (step breakdown below)
**Next:** Step 1 — Translate 16 autocontrol/guard AI functions + write tests
**Blocked/Broken:** None.

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

### Module 11: Layer 1 seg002 — IN PROGRESS

**Scope:** Translate seg002.c (1,237 lines, 52 functions) → Seg002.kt. Guard AI, room transitions, sword combat detection, special level events, shadow/skeleton autocontrol.

**Regime:** Build (autonomous). Validated by compilation + unit tests (replay oracle deferred to Module 13 integration).

**Dependencies consumed:** Seg006 (tile queries, character physics), Seg005 (combat helpers via stubs), Seg004 (collision), GameState, SequenceTable, ExternalStubs.

**Dependencies provided:** `autocontrol_opponent`, `leave_guard`, `check_shadow`, `enter_guard`, `exit_room`, `check_guard_fallout`, `check_sword_hurt`, `check_sword_hurting`, `check_skel`, `hurt_by_sword`, `do_auto_moves` — replace ExternalStubs entries + provide new functions for Module 12/13.

#### Phase 11a: Guard init, room management & special events (~19 functions, ~530 lines)

Functions: `do_init_shad`, `get_guard_hp`, `check_shadow`, `enter_guard`, `check_guard_fallout`, `leave_guard`, `follow_guard`, `exit_room`, `goto_other_room`, `leave_room`, `Jaffar_exit`, `level3_set_chkp`, `sword_disappears`, `meet_Jaffar`, `play_mirr_mus`

Also: move helpers `move_0_nothing` through `move_7`, `move_up_back`, `move_down_back`, `move_down_forw` (~10 trivial functions, ~60 lines)

**Tests:**
- Guard init: `do_init_shad` sets Char fields + shadow state correctly
- `enter_guard`: loads guard from level data, sets charid/skill/hp, handles skeleton vs guard
- `check_guard_fallout`: shadow clears, skeleton reappears, regular guard dies
- `leave_guard`: saves guard state back to level arrays
- `goto_other_room`: coordinate adjustments per direction (left +140, right -140, up +189, down -189)
- `leave_room`: frame-based exit direction logic, special event triggers
- Special events: `check_shadow` level 5/6/12, `check_skel` skeleton wake, `meet_Jaffar`, `play_mirr_mus`
- Move helpers: verify control variable settings

#### Phase 11b: Guard AI & autocontrol (~16 functions, ~280 lines) — PLANNED

**Scope:** All autocontrol dispatch and guard AI functions. These implement the opponent behavior loop: dispatch by charid → inactive/active guard states → distance-based tactics → probabilistic combat decisions.

**Step 1: Translate all 16 functions + write tests** (~280 lines C → Kotlin, single step)

Functions (in call-graph order):
1. `autocontrol_opponent` — main dispatch: decrement counters, route by charid
2. `autocontrol_mouse` — mouse: stand → clear at x≥200, or trigger seq_107 at x<166
3. `autocontrol_shadow` — shadow: dispatch to level-specific handlers (Phase 11c)
4. `autocontrol_skeleton` — skeleton: set sword drawn, delegate to guard
5. `autocontrol_Jaffar` — Jaffar: delegate to guard
6. `autocontrol_kid` — kid (demo mode): delegate to guard
7. `autocontrol_guard` — guard: dispatch to inactive (sword < 2) or active
8. `autocontrol_guard_inactive` — detect Kid, turn to face, enter fighting pose
9. `autocontrol_guard_active` — main active AI: distance-based behavior tree
10. `autocontrol_guard_kid_far` — Kid far: check floor ahead, advance or retreat
11. `guard_follows_kid_down` — follow Kid down: safety checks (wall/spike/loose/chasm)
12. `autocontrol_guard_kid_in_sight` — Kid in sight: route to armed or advance/shift
13. `autocontrol_guard_kid_armed` — Kid armed: distance-based advance/block/strike
14. `guard_advance` — probabilistic advance (advprob[guard_skill])
15. `guard_block` — probabilistic block on opponent strike frames (blockprob/impblockprob)
16. `guard_strike` — probabilistic strike/restrike (strikeprob/restrikeprob)

**Key translation notes:**
- `autocontrol_guard_inactive`: uses `(word)distance < (word)-8` → unsigned comparison: `(distance and 0xFFFF) < ((-8) and 0xFFFF)`. Same pattern for `(word)-4`.
- `autocontrol_guard_active`: complex nested branching with `can_guard_see_kid` (0/1/2 states). `frame >= 150` check uses raw int, not named constant.
- `guard_follows_kid_down`: calls `get_tile()` with `++tile_row` side effect — must increment `gs.tileRow` before use.
- Combat probability functions use `custom.advprob[guard_skill]`, etc. — `custom` is `gs.custom`, arrays indexed by `gs.guardSkill`.
- `autocontrol_shadow` just dispatches to level-specific functions that are in Phase 11c — add placeholder stubs or empty functions for now.

**Tests (~25 expected):**
- `autocontrol_opponent` dispatch: 6 tests — one per charid (kid→autocontrol_kid, mouse, skeleton, shadow, Jaffar, guard), verify counter decrements (justblocked, kidSwordStrike, guardRefrac)
- `autocontrol_mouse`: 3 tests — direction none (return), stand + x≥200 (clear), x<166 (seq trigger)
- `autocontrol_guard_inactive`: 4 tests — Kid dead (return), Kid behind + notice (turn), Kid behind no notice (return), can see Kid (move to fight), Jaffar level 13 guard_notice_timer delay
- `autocontrol_guard_active`: 3 tests — can_guard_see_kid=0 + droppedout (follows down), can_guard_see_kid=2 + close distance (retreat/advance), can_guard_see_kid=2 + far distance (kid_far)
- `guard_follows_kid_down`: 3 tests — wall ahead (don't follow), spike below (don't follow), safe floor (follow)
- `autocontrol_guard_kid_armed`: 2 tests — distance<10 (advance), distance 12-28 (block+strike)
- `guard_advance/block/strike`: 6 tests — probability-based outcomes using seeded prandom, verify move calls

**Dependencies:**
- Consumes: `Seg006.charOppDist`, `Seg006.getTileInfontofChar`, `Seg006.getTileInfontof2Char`, `Seg006.wallType`, `Seg006.tileIsFloor`, `Seg006.clearChar`, `Seg006.playSeq`, `Seg006.seqtblOffsetChar`, `Seg006.getTile`, `Seg002.prandom`, `Seg002.move*` helpers (Phase 11a)
- New stubs needed: `autocontrol_shadow_level4/5/6/12` → stub or empty (Phase 11c will implement)

#### Phase 11c: Sword combat detection & shadow autocontrol (~15 functions, ~350 lines)

Functions: `hurt_by_sword`, `check_sword_hurt`, `check_sword_hurting`, `check_hurting`, `check_skel`, `do_auto_moves`, `autocontrol_shadow_level4`, `autocontrol_shadow_level5`, `autocontrol_shadow_level6`, `autocontrol_shadow_level12`

**Tests:**
- `hurt_by_sword`: HP deduction, death sequence, skeleton immunity, edge/wall positioning
- `check_hurting`: distance + frame checks, parry detection, `actions_99_hurt` assignment
- `check_sword_hurt`: Kid vs Guard routing, refrac timer set
- `check_skel`: skeleton wake conditions, tile erasure, state setup
- `do_auto_moves`: demo_time progression, move dispatch
- Shadow level autocontrol: level 4 (mirror approach), level 5 (potion steal), level 6 (step), level 12 (unite/fight)

#### Wire-up

After all phases: replace `ExternalStubs.autocontrolOpponent` and `ExternalStubs.leaveGuard` with real Seg002 calls. Add any new stubs needed for seg002's external dependencies (seg003, seg000, seg007, seg008).
