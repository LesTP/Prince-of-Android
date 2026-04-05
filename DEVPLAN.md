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
**Phase:** 11c — Sword combat detection & shadow autocontrol — **IN PROGRESS**
**Next:** Step 1 — implement sword combat functions
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

#### Phase 11a: Guard init, room management & special events — COMPLETE
One-line: 19 functions (move helpers, guard init/state, room transitions, special events), 49 tests, 356 total. See DEVLOG.

#### Phase 11b: Guard AI & autocontrol — COMPLETE
One-line: 16 autocontrol/guard AI functions (dispatch, inactive/active states, combat probability), 30 tests, 386 total. Clean review. See DEVLOG.

#### Phase 11c: Sword combat detection & shadow autocontrol (~15 functions, ~350 lines) — IN PROGRESS

**Step 1: Sword combat functions (6 functions, ~22 tests)**
Implement `hurtBySword`, `checkSwordHurt`, `checkSwordHurting`, `checkHurting` in Seg002.kt.
- `hurtBySword`: HP deduction via `takeHp`, death sequence (not in fighting pose → instant kill), skeleton immunity, gate positioning with `fixOffscreenGuardsDisappearing`, pushed-off-ledge path, sound dispatch. Uses `getTileBehindChar`, `distanceToEdgeWeight`, `charDxForward`, `loadFramDetCol`, `incCurrRow`.
- `checkSwordHurt`: Routes Guard.action==99 → loadshad/hurt_by_sword/saveshad + refrac timer; Kid.action==99 → loadkid/hurt_by_sword/savekid.
- `checkSwordHurting`: Skip if Kid on stairs (frames 219-228), then check both sides via loadshadAndOpp/saveshadAndOpp + loadkidAndOpp/savekidAndOpp.
- `checkHurting`: Distance + frame checks (frames 153-154 poking), parry detection (frames 150/161), min_hurt_range (8 unarmed, 12 armed), `actions_99_hurt` assignment, parry counter-sequence, sword moving sound.
Tests: ~22 covering all branches.

**Step 2: Skeleton wake + auto moves (2 functions, ~10 tests)**
Implement `checkSkel`, `doAutoMoves` in Seg002.kt.
- `checkSkel`: Level/room/door conditions from custom options, tile erasure (tiles_21_skeleton → tiles_1_floor), guard setup (skeleton charid, skill, drawn sword, 3HP).
- `doAutoMoves`: demo_time increment (cap at 0xFE), index advancement, move dispatch switch (0-7 → move_N functions). Takes `Array<AutoMoveType>` parameter.
Tests: ~10 covering wake conditions, tile changes, move dispatch.

**Step 3: Shadow autocontrol functions (4 functions, ~12 tests)**
Implement `autocontrolShadowLevel4`, `autocontrolShadowLevel5`, `autocontrolShadowLevel6`, `autocontrolShadowLevel12` in Seg002.kt (replace stub bodies).
- Level 4: Mirror room approach, clearChar when x<80.
- Level 5: Door open check (modif>=80), doAutoMoves with shadDrinkMove, clearChar when x<15.
- Level 6: Kid running jump frame 43 + x<128 → shift+forward.
- Level 12: Complex — init shadow, sword drawn combat (autocontrolGuardActive or down), sword draw approach, unite (flash+addLife+unitedWithShadow=42), follow Kid.
Tests: ~12 covering each level's logic paths.

**Step 4: Wire-up + review (~4 tests)**
Replace ExternalStubs.doAutoMoves with real call. Add any new stubs needed for remaining seg002 external deps. Verify all 386+ tests still pass. Clean review pass.

#### Wire-up

After all phases: replace `ExternalStubs.autocontrolOpponent` and `ExternalStubs.leaveGuard` with real Seg002 calls. Add any new stubs needed for seg002's external dependencies (seg003, seg000, seg007, seg008).
