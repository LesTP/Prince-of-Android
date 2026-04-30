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
- **JDK 17 toolchain:** `SDLPoP-kotlin/build.gradle.kts` requests JVM toolchain 17, matching the Pi container's OpenJDK 17. The `app` module (Android) also targets Java 17. Both modules are JDK 17-compatible.
- **DAT resource payloads skip checksum byte:** SDLPoP DAT table offsets point at a one-byte checksum; image/palette payload begins at `offset + 1` and is exactly `size` bytes. Kotlin asset parsers should expose checksum-stripped payload bytes to match `seg009.c::load_from_opendats_alloc()`.
- **Keep asset decode JVM-testable:** DAT decompression, palette-to-ARGB conversion, PNG metadata parsing, and chtab catalog loading belong in `com.sdlpop.assets`; Android `Bitmap` creation stays in the app bridge. This keeps asset correctness covered by `gradle test` without Android runtime dependencies.
- **SDLPoP replay invocation (Linux):** Run from `/tmp/sdlpop/` (binary copied there for execute permission). Use `SDL_VIDEODRIVER=offscreen SDL_AUDIODRIVER=dummy ./prince validate "replays/foo.p1r" seed=12345`. Output: `state_trace.bin`.
- **SDL headless mode:** Pi has no display. Use `SDL_VIDEODRIVER=offscreen` (not `dummy` — dummy hangs). Requires `xvfb` package installed but `offscreen` driver doesn't need it.
- **NTFS execute permissions:** USB drive is NTFS — `chmod +x` is silently ignored. Workaround: copy binaries to `/tmp/sdlpop/` with symlinks to data/replays/doc/SDLPoP.ini.
- **Line endings:** NTFS Pi mount converts LF→CRLF on cross-filesystem syncs. `.gitattributes` enforces `eol=lf` for `*.sh` files. For other files: run `dos2unix *.c *.h` in SDLPoP/src/ after any file transfer. Grep/ripgrep and bash fail silently on CRLF files.
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
**Phase:** 16c — Render table pure logic — **IN PROGRESS**
**Next:** Step 16c.3 — Object table and dirty-rect bookkeeping

**Replay regression:** 8/13 MATCH, 606 unit tests pass. 5 remaining divergences root-caused and documented (see DEVLOG §Module 15). Matching: `basic_movement`, `falling`, `original_level2_falling_into_wall`, `original_level5_shadow_into_wall`, `original_level12_xpos_glitch`, `snes_pc_set_level11`, `traps`, `trick_153`.

## Phase Summary

### Phase 0: Environment Setup — COMPLETE
One-line: Built SDLPoP on Windows via MSYS2/MinGW, verified gameplay, recorded first replay. See DEVLOG_archive.md §Phase-0.

### Phase 1: Replay Oracle Spike — COMPLETE
One-line: Verified replay determinism (FC: no differences). Q1 closed — oracle works. See DEVLOG_archive.md §Phase-1.

### Phase 2: Target Language Decision — COMPLETE
One-line: Decision: **Kotlin**. Rationale: native Android, predictable integer semantics (signed types wrap on overflow), explicit conversions. Validation deferred to first file translation in Phase 3 — replay oracle will catch semantic mismatches immediately. Q2 closed. See DEVLOG_archive.md §Phase-2.

### Phase 3: Test Infrastructure — COMPLETE
One-line: Built replay oracle toolchain: 13 reference traces, Python comparator, Kotlin P1R parser (Gradle, 9/9 tests pass). See DEVLOG_archive.md §Phase-3.

**Deferred:** Game loop replay runner — build alongside first file translation.

### Module 6: State Model — COMPLETE
One-line: Translated types.h + data.h → Kotlin (4 files, 27 tests pass). See DEVLOG_archive.md §Module 6.

### Module 7: Sequence Table — COMPLETE
One-line: Translated seqtbl.c → Kotlin (SequenceTable.kt + enums, 2,310-byte array, 115 offsets, 50 new tests, 77 total pass). See DEVLOG_archive.md §Module 7.

### Module 8: Layer 1 seg006 — COMPLETE
One-line: Translated seg006.c → Kotlin (81 functions, 2,154 lines C → Seg006.kt + ExternalStubs.kt, 190 tests pass, zero escalations). See DEVLOG_archive.md §Module 8.

### Module 9: Layer 1 seg004 — COMPLETE
One-line: Translated seg004.c → Kotlin (26 functions, 621 lines C → Seg004.kt, 42 new tests, 232 total pass, zero escalations). See DEVLOG_archive.md §Module 9.

### Module 10: Layer 1 seg005 — COMPLETE
One-line: Translated seg005.c → Kotlin (38 functions, 1,172 lines C → Seg005.kt, 75 new tests, 307 total pass, zero escalations). See DEVLOG_archive.md §Module 10.

### Module 11: Layer 1 seg002 — COMPLETE
One-line: Translated seg002.c → Seg002.kt across phases 11a-11c, including guard AI, room transitions, sword combat detection, skeleton/shadow logic, and stub wire-up; review clean. See DEVLOG_archive.md §Module 11.

### Module 12: Layer 1 seg007 — COMPLETE
Human audit approved on 2026-04-15; proceed to Module 13.

#### Phase 12a: Trob core, redraw helpers, trap/button animation — COMPLETE
One-line: Translated trob pipeline, animated-tile state machines (torch/potion/sword/chomper/spike/button/gate/leveldoor), trigger plumbing, and 6 ExternalStubs wire-ups into Seg007.kt (3 steps, 70 tests, 519 total pass, zero escalations). See DEVLOG_archive.md §Module 12.

#### Phase 12b: Loose-floor mobs and remaining seg007 functions — COMPLETE
One-line: Translated loose-floor fall initiation, falling-object simulation, Kid collision/death handling, row/room transitions, redraw bookkeeping, and mob object-table writes into Seg007.kt (3 steps, 21 new tests, 540 total pass, zero escalations). See DEVLOG_archive.md §Module 12.

### Module 13: Layer 1 integration test — COMPLETE
One-line: Built the Kotlin replay-regression harness around the translated Layer 1 game logic: 310-byte trace parsing/comparison, `GameState` snapshot serialization, all-13-trace manifest coverage, Gradle workflow, triage-ready divergence reports, and explicit Module 14 handoff for real replay playback. See DEVLOG §Module 13.

#### Phase 13a: Layer 1 replay regression harness — COMPLETE
One-line: Delivered the trace oracle foundation, state snapshot writer, and manifest-driven regression workflow; review accepted after one should-fix, human approval recorded on 2026-04-15, and `gradle test layer1ReplayRegression --rerun-tasks` passed. See DEVLOG §Module 13.

### Module 14: Replay Runner — COMPLETE
One-line: Built Kotlin replay playback pipeline with real `.P1R` trace production, Layer 1 frame driver, replay move hooks, headless lifecycle shim, and 310-byte state trace output. 1/13 traces match; remaining divergences resolved in Module 15. See DEVLOG §Module 14.

#### Phase 14a: Kotlin replay playback and trace producer — COMPLETE (ESCALATION BYPASSED)
One-line: Real `.P1R` trace production wired into regression harness; escalation bypassed into Phase 14b for lifecycle reconciliation. See DEVLOG §Module 14.

#### Phase 14b: Non-rendering frame lifecycle reconciliation — COMPLETE
One-line: Added headless non-rendering frame lifecycle shim (timer, guard visibility, bump/knock, HP delta, room transitions); 1/13 traces match, remaining deferred to Module 15. See DEVLOG §Module 14.

### Module 15: Game Loop — COMPLETE (8/13 traces)
One-line: Translated seg000/seg001/seg003 game loop, resolved replay regression from 1/13 to 8/13 traces via seg003 translation, byte overflow fixes, savestate restore, soundFlags RNG fix, and sprite dimension table. See DEVLOG §Module 15.

#### Phase 15a: seg003 translation + stub wiring — COMPLETE
One-line: Translated 22 seg003 functions, wired stubs, fixed `soundFlags` RNG bug. 4/13 traces match (up from 1/13). 566 tests pass. See DEVLOG §Module 15.

#### Phase 15b: seg000 frame lifecycle alignment — COMPLETE (5/13)
One-line: Translated seg000 initialization, room-transition, and draw-frame paths. Fixed byte overflow, savestate restore, and soundFlags bugs via human-driven debug session. 5/13 traces match; remaining 8 root-caused to missing sprite dimensions. See DEVLOG §Module 15.

#### Phase 15c: Sprite dimension table for headless collision — COMPLETE (8/13)
One-line: Hardcoded sprite width/height lookup table for headless collision. `setCharCollision()` now computes correct bounds. 8/13 traces match. See DEVLOG §Module 15.

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
One-line: Multi-module Gradle project with Android `app` module (SDK 24–34, `GameActivity` + `GameSurfaceView`), conditional inclusion for Pi compatibility, SDLPoP assets packaged. See DEVLOG §Module 16.

#### Phase 16b: Asset loading pipeline — COMPLETE
One-line: Ported the `seg009.c` DAT/PNG asset pipeline into JVM-testable Kotlin plus Android bridges: DAT table parsing, checksum-stripped resource reads, RLE/LZG decompression, packed-pixel expansion, palette-to-ARGB decode, source-neutral asset lookup, chtab sprite catalogs, and Bitmap creation handoff. KID chtab 2 (219 sprites) and GUARD chtab 5 (34 sprites) match `SpriteDimensions.kt`; full `gradle test` passes with 586 tests. See DEVLOG §Module 16.

Human audit approved on 2026-04-30; proceed to Phase 16c planning.

#### Phase 16c: Render table pure logic (seg008 state functions) — IN PROGRESS

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

**Phase plan (2026-04-30):**

1. **16c.1 — Seg008 render-state scaffold and tile helpers — COMPLETE (2026-04-30)**
   - Add the Kotlin `Seg008` foundation needed by later rendering phases without drawing pixels: translated tile-table constants/data models as needed, render-state globals missing from `GameState`, `calc_screen_x_coord` replacing the current identity stub, and the small pure helpers `can_see_bottomleft`, `get_spike_frame`, `get_loose_frame`, and `calc_gate_pos`.
   - Translate `get_tile_to_draw` with focused fixtures for fake tiles, room-0 edges, button states, loose-floor modifiers, and current/neighbor room lookups.
   - Wire `ExternalStubs.calcScreenXCoord` to the real helper while preserving test reset behavior.
   - Verification: `gradle test --tests com.sdlpop.game.Seg008Test`; full `gradle test` (596 tests).

2. **16c.2 — Room links, adjacent tiles, and modifier preprocessing — COMPLETE (2026-04-30)**
   - Translate `load_room_links`, `load_leftroom`, `load_rowbelow`, and `load_curr_and_left_tile`, reusing the existing `ExternalStubs.loadRoomAddress` room-buffer loader where it already matches C behavior.
   - Translate `alter_mods_allrm` and `load_alter_mod`, including gate, loose-floor, potion, wall, torch, and retained wall-modifier behavior.
   - Add tests that seed `level.fg[]`/`level.bg[]` rather than only room buffers, covering neighbor-room links, above/below/left edge cases, and all major modifier transformations.
   - Verification: `gradle test --tests com.sdlpop.game.Seg008Test`; full `gradle test` (606 tests).

3. **16c.3 — Object table and dirty-rect bookkeeping**
   - Translate `add_objtable`, `add_kid_to_objtable`, `add_guard_to_objtable`, `load_obj_from_objtable`, `mark_obj_tile_redraw`, `sort_curr_objs`, `compare_curr_objs`, `load_frame_to_obj`, and `add_drect`.
   - Preserve C-equivalent byte/sbyte wrapping for object x/y/id fields and draw-order comparisons.
   - Add tests for Kid/Guard object insertion, frame-to-object coordinate calculation, sort order, tile redraw marking, and dirty-rect merge/intersection behavior.
   - Verification target: focused `Seg008Test` plus existing game/replay tests.

4. **16c.4 — Pure orchestration with render-submission hooks**
   - Translate `draw_room`, `draw_tile`, `draw_tile_aboveroom`, `redraw_needed`, `redraw_needed_above`, `redraw_needed_tiles`, and `draw_moving`.
   - Keep Phase 16d submission functions behind no-op or test-capturable render hooks so this step establishes C-equivalent traversal/order without appending real render-table entries yet.
   - Add call-order and state-mutation tests for room traversal, above-room traversal, redraw counters, tile-object redraw clearing, and moving-object orchestration.
   - Verification target: full `gradle test`; if the environment hits the known native-platform Gradle failure, record the blocker and rely on focused compile/test artifacts available from the working distribution.

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
