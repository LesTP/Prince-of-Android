---
module: RENDERING
phase: 16e
phase_title: Rendering backend — JVM-first + level screenshot comparison
step: 0 of 6
mode: Discuss
blocked: null
regime: Build
review_done: false
---

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
**Phase:** 16e — Rendering backend (JVM-first + level screenshot comparison) — **PLANNED**
**Next:** Phase-plan action (step breakdown already in DEVPLAN, worker should proceed to Step 16e.1)

**Replay regression:** 8/13 MATCH, 642 unit tests pass. 5 remaining divergences root-caused and documented (see DEVLOG §Module 15). Matching: `basic_movement`, `falling`, `original_level2_falling_into_wall`, `original_level5_shadow_into_wall`, `original_level12_xpos_glitch`, `snes_pc_set_level11`, `traps`, `trick_153`.

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

#### Phase 16c: Render table pure logic (seg008 state functions) — COMPLETE
One-line: Translated the 30 pure `seg008.c` render-state functions into `Seg008.kt`, covering tile resolution, room/adjacent-tile loading, modifier preprocessing, object-table and dirty-rect bookkeeping, and draw/redraw orchestration behind Phase 16d render-submission hooks; review found no must-fix or should-fix issues, and `gradle test --tests com.sdlpop.game.Seg008Test --no-daemon` plus full `gradle test --no-daemon` pass with 620 tests. See DEVLOG §Module 16.

#### Phase 16d: Render table submission (seg008 mixed functions) — COMPLETE
One-line: Translated the 31 `seg008.c` render-submission functions into Kotlin render-table producers, covering table append helpers, tile/structure/overlay/people/object submissions, wall-pattern generation, wall marks, timer text, and level text; review found one blink-state compatibility must-fix, applied before closure, and full `gradle test --no-daemon` passes with 642 tests. See DEVLOG §Module 16.

#### Phase 16e: Rendering backend — JVM-first + level screenshot comparison — PENDING

**Regime:** Build (autonomous, Steps 1-4) then Refine (human-driven, Steps 5-6).

**Strategy:** Build the rendering backend as a JVM `BufferedImage` renderer first, validated via automated pixel comparison against C reference level screenshots. Defer Android `Canvas` work until blitter correctness is proven. This converts most rendering validation from human-visual (Refine) to automated-comparison (Build).

**Why JVM-first:** `BufferedImage` and `Canvas` have near-identical APIs. Asset pipeline already produces `IntArray` ARGB pixels (works on both). `gradle test` iterates in ~5s vs ~60s+ for emulator. Pixel-perfect comparison catches bugs automatically without human eyes.

**C reference:** SDLPoP's `save_level_screenshot()` (in `screenshot.c`) renders every reachable room into one composite PNG per level. Invoke via `./prince megahit N --screenshot --screenshot-level` with `SDL_VIDEODRIVER=offscreen`. Generate references for all 14 levels on the Pi.

**Blitter mode → implementation mapping:**

| C Blitter | Value | PoP Usage | Implementation |
|-----------|-------|-----------|----------------|
| `NO_TRANSP` | 0 | Opaque tiles (floor, wall base) | Overwrite all pixels |
| `OR` | 2 | Most sprites with transparency | Skip pixels where source == 0 (color key) |
| `XOR` | 3 | Shadow character | XOR source with destination pixels |
| `WHITE` | 8 | Flash effect | Draw sprite shape in solid white |
| `BLACK` | 9 | HP bars, silhouettes | Draw sprite shape in solid black |
| `TRANSP` | 0x10 | Same as OR in VGA mode | Skip color-key pixels (same as OR) |
| `MONO` | 0x40+ | Wall patterns, chomper blood | Draw sprite shape in color `blit & 0x3F` |
| hflip | bit 0x80 in midtable.blit | Left-facing characters | Mirror sprite horizontally before draw |

**Steps (Build — autonomous, Steps 1-4):**

**Step 16e.1: JVM sprite blitter**
Create `SpriteRenderer` in `com.sdlpop.render` that takes a pixel buffer target and implements `drawSprite(x, y, pixels, w, h, blitMode, hflip, clipRect?)` for all 8 blitter modes plus `drawRect(left, top, w, h, colorIndex)` for wipetable entries. Uses PoP's 16-color VGA palette for color-index resolution.
- **Files:** Create `SDLPoP-kotlin/src/main/kotlin/com/sdlpop/render/SpriteRenderer.kt`, `SpriteRendererTest.kt`
- **Test:** Unit tests: blit a known sprite with each mode, assert output pixels match expected.
- **Reference:** `SDLPoP/src/seg009.c` lines 2170-2210 (`method_6_blit_img_to_scr` blitter dispatch), `SDLPoP/src/seg008.c` lines 1048-1077 (`draw_image` dispatch)

**Step 16e.2: Render table flusher**
Create `RenderTableFlusher` that takes a `SpriteRenderer` + loaded `SpriteCatalog` map (chtabId → catalog) and implements `flushTables(gs)` in C draw order: restore peels (deferred), wipetable layer 0, backtable, midtable (with clip + hflip), wipetable layer 1, foretable. Resolves sprite via `catalogs[entry.chtabId]?.imageByFrameId(entry.id + 1)`. Position: `x = entry.xh * 8 + entry.xl`, `y = entry.y`.
- **Files:** Create `SDLPoP-kotlin/src/main/kotlin/com/sdlpop/render/RenderTableFlusher.kt`, `RenderTableFlusherTest.kt`
- **Test:** Load Level 1 state, populate tables via `Seg008.redrawRoom()`, flush to 320×200 BufferedImage, save PNG. Visual sanity check.
- **Reference:** `SDLPoP/src/seg008.c` lines 1365-1383 (`draw_tables`), lines 938-956 (`draw_back_fore`), lines 992-1045 (`draw_mid`)

**Step 16e.3: Level screenshot generator**
Create `LevelScreenshotGenerator` that loads chtab catalogs, iterates reachable rooms via BFS over room links (same algorithm as C's `save_level_screenshot`), renders each room via `Seg008.redrawRoom()` + `RenderTableFlusher`, and stitches into a composite PNG (320px × 189px per room).
- **Files:** Create `SDLPoP-kotlin/src/main/kotlin/com/sdlpop/render/LevelScreenshotGenerator.kt`, `LevelScreenshotTest.kt`
- **Test:** Generate Level 1 composite PNG from Kotlin.
- **Reference:** `SDLPoP/src/screenshot.c` lines 472-686 (`save_level_screenshot`)

**Step 16e.4: C reference screenshots + ImageMagick pixel-diff comparison**
Generate C reference screenshots for all 14 levels on the Pi. Compare against Kotlin output using ImageMagick `compare -metric AE ref.png test.png diff.png` (returns count of differing pixels, produces highlighted diff image). No custom tooling needed — `compare` is already on the Pi (Debian 12).
- **Files:** `SDLPoP/screenshots/` reference PNGs
- **Commands:** `for level in $(seq 1 14); do SDL_VIDEODRIVER=offscreen SDL_AUDIODRIVER=dummy ./prince megahit $level --screenshot --screenshot-level; done`
- **Test:** Run comparison on Level 1. Triage differences. Iterate on blitter/sprite bugs.

**Steps (Refine — human-driven, Steps 5-6):**

**Step 16e.5: Android Canvas bridge**
Create `AndroidRenderer` wrapping `android.graphics.Canvas` with the same primitives as JVM `SpriteRenderer`. Wire `drawBackForeHook`/`drawMidHook`/`drawWipeHook` in `Seg008`. Add game loop thread to `GameSurfaceView`: lock Canvas → flush render tables to 320×200 offscreen Bitmap → scale to device resolution → unlock and post.
- **Files:** Create `app/.../AndroidRenderer.kt`, modify `GameSurfaceView.kt`, `GameActivity.kt`, `Seg008.kt` hooks

**Step 16e.6: Visual debugging iteration**
Compare all 14 level screenshots (Kotlin vs C). Fix systematic issues (wrong blitter, missing sprites, draw order). Handle edge cases: palace wall colors, torch flames, gate animations, text rendering. Lighting deferred to a separate step if needed.

**Acceptance:** All 14 level screenshots match C reference within tolerance (≥95% pixel match). Level 1 renders visually correct on Android emulator. Tiles, sprites, gates, potions, torches, and wall patterns are recognizable and correctly layered.

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
| 16e | Rendering backend (JVM blitter + screenshot comparison + Android bridge) | Build (1-4) + Refine (5-6) | Review steps 1-4, visual debug steps 5-6 | 4 iters + 2 sessions |
| 16f | Real sprites → replay regression | Build + human check | Run regression, verify | 1 iter + 1 verify |

**Build/Refine boundary:** Render tables (`backtable[]`, `midtable[]`, `foretable[]`, `wipetable[]`, `objtable[]`) are the clean split. Everything producing table entries is Build. Everything consuming them to put pixels on screen is Refine.
