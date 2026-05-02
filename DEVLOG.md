# DEVLOG — Prince of Persia Android Port

## Module 16: Rendering

### 2026-05-02 — Step 16e.4: C reference screenshots + ImageMagick pixel-diff comparison

**Mode:** Code | **Outcome:** Complete — C reference screenshots and automated diff workflow established
**Contract changes:** `LevelScreenshotGenerator` now matches SDLPoP level-map geometry by rendering 320x200 room surfaces with a 189-pixel vertical stride. Added `com.sdlpop.MainKt` screenshot CLI and `tools/render_screenshot_compare.sh` for repeatable C/Kotlin screenshot generation and ImageMagick AE diffs. No Android Canvas or game-state producer contract changed.

Generated C reference level-map screenshots for all 14 levels through the documented `/tmp` executable workaround, copied them to `SDLPoP/screenshots/reference/level_NN_c.png`, and added a repeatable comparison script. The script prepares a temporary executable SDLPoP runtime, runs `./prince megahit N --screenshot --screenshot-level` with `SDL_VIDEODRIVER=offscreen` and dummy audio, generates Kotlin screenshots through `gradle run --args="render-level-screenshots ..."`, then writes ImageMagick `compare -metric AE` results and diff PNGs under `SDLPoP-kotlin/build/render/diff/`.

The first C-vs-Kotlin comparison exposed a geometry mismatch from Step 16e.3: C blits each 320x200 `onscreen_surface_` into the map with 189-pixel room spacing, so adjacent rooms overlap by 11 pixels and the final image height is `roomsHigh * 189 + 11`. Updated the JVM generator and test expectations to match that C geometry before recording the baseline diffs.

Baseline AE counts remain high, which is expected for the first backend comparison and gives the next visual/debug phase concrete artifacts: level 01 has 876,752 differing pixels, and all level summaries are in `SDLPoP-kotlin/build/render/diff/summary.csv`.

Verification: `gradle test --tests com.sdlpop.render.LevelScreenshotTest --no-daemon` passed; `bash tools/render_screenshot_compare.sh` completed for all 14 levels; full `gradle test --no-daemon` passed in `SDLPoP-kotlin` with 654 tests.

### 2026-05-02 — Step 16e.3: Level screenshot generator

**Mode:** Code | **Outcome:** Complete — JVM Level 1 composite PNG generation works
**Contract changes:** Added `com.sdlpop.render.LevelScreenshotGenerator` as the JVM level-map renderer; `RenderTableFlusher` now accepts directory-backed PNG sprites as well as DAT-decoded sprites. No Android Canvas or game-state producer contract changed.

Created `LevelScreenshotGenerator`, which loads SDLPoP level resources through the existing asset repository, parses the packed `level_type` byte layout, applies the translated `Seg008.alterModsAllrm()` preprocessing, maps reachable rooms with the same left/right/up/down BFS direction order as `screenshot.c::save_level_screenshot()`, renders each room through `Seg008.drawRoom()` and `RenderTableFlusher`, and stitches room images into a `BufferedImage` composite at 320x189 pixels per room.

Extended `RenderTableFlusher` to decode `PngDecodedImage` resources via `ImageIO`, which is required for the packaged directory-backed KID, environment, sword, and potion/flame assets. Added focused coverage for the room-map BFS and a Level 1 generator test that writes `SDLPoP-kotlin/build/render/level_01_kotlin.png`.

Verification: `gradle test --tests com.sdlpop.render.LevelScreenshotTest --tests com.sdlpop.render.RenderTableFlusherTest --no-daemon` passed; full `gradle test --no-daemon` passed in `SDLPoP-kotlin` with 654 tests.

### 2026-05-02 — Step 16e.2: Render table flusher

**Mode:** Code | **Outcome:** Complete — render tables flush through the JVM sprite renderer
**Contract changes:** Added `com.sdlpop.render.RenderTableFlusher` as the JVM consumer for Phase 16d render tables; no Android Canvas or game-state producer contract changed.

Created `RenderTableFlusher`, which consumes `GameState` render tables and loaded `SpriteCatalog` instances, resolves zero-based table image ids through one-based catalog frame ids, and draws via `SpriteRenderer` in SDLPoP draw order: deferred peel restore, wipe layer 0, backtable, midtable, wipe layer 1, and foretable. The midtable path handles the `0x80` hflip bit, `chtabFlipClip` clipping, sword-specific x-coordinate behavior, and C-style hflip positioning before drawing.

Added focused coverage for table draw order, layer-specific wipes, back/fore catalog id resolution, midtable clipping, hflip behavior, and dirty-rect bookkeeping.

Verification: `gradle test --tests com.sdlpop.render.RenderTableFlusherTest --no-daemon` passed; full `gradle test --no-daemon` passed in `SDLPoP-kotlin` with 652 tests.

### 2026-05-02 — Step 16e.1: JVM sprite blitter

**Mode:** Code | **Outcome:** Complete — pixel-buffer sprite blitter implemented and tested
**Contract changes:** Added `com.sdlpop.render.SpriteRenderer` as the JVM rendering primitive for later Phase 16e table flushing; no Android Canvas or game-state contract changed.

Created a pure JVM `SpriteRenderer` over an ARGB `IntArray` target, plus a `ClipRect` value type. The renderer supports `drawSprite()` with screen/explicit clipping, optional horizontal flip including the `0x80` render-table bit, and the SDLPoP blitter modes needed by Phase 16e: no-transparency overwrite, OR/TRANSP color-key transparency, XOR shadow blending, WHITE, BLACK, and MONO palette-color shape drawing. Added `drawRect()` for future wipetable flushes and embedded the default 16-color PoP VGA palette from `VGA_PALETTE_DEFAULT`.

Added focused unit coverage for overwrite semantics, transparent sprite draws, XOR, white/black/mono palette rendering, hflip, sprite clipping, and rectangle clipping.

Verification: `gradle test --tests com.sdlpop.render.SpriteRendererTest --no-daemon` passed; full `gradle test --no-daemon` passed in `SDLPoP-kotlin` with 649 tests.

### 2026-05-02 — Phase 16d complete

**Mode:** Complete | **Outcome:** Phase complete — render table submission ready for human audit
**Contract changes:** DEVPLAN.md, ARCHITECTURE.md, DECISIONS.md — phase status and completion metadata only; no source behavior changes.

Completed Phase 16d closure after review. The phase delivered the full Build-regime `seg008.c` render-submission slice: render-table data and append helpers, tile submissions, gates/level doors/overlays, people and object-table flushing, wall-pattern generation, wall marks, timer text, and level text state. The one review must-fix for `FIX_ONE_HP_STOPS_BLINKING` blink-state compatibility was applied before closure.

No new DEVPLAN gotchas were promoted from the phase learning review. The only review issue was a direct compatibility fix, not a repeated trial-and-error pattern, and the Build/Refine boundary remained unchanged: Phase 16d only populates render queues/text state and leaves Android Canvas drawing, table flushing, peels, and lighting to Phase 16e.

DEVLOG is above 500 lines, but the excess is historical completed-module context already kept here by prior Module 16 completion entries; no archive split was performed in this completion step to avoid churn outside the active phase.

Verification: full `gradle test --no-daemon` passed in `SDLPoP-kotlin`.

### 2026-05-02 — Phase 16d Review

**Mode:** Review | **Outcome:** Complete — one must-fix applied
**Contract changes:** Added `GameState.globalBlinkState` to preserve SDLPoP's `FIX_ONE_HP_STOPS_BLINKING` timer/HP rendering contract. No Android/SDL pixel drawing contract added.

Reviewed the Phase 16d render-submission implementation against the `seg008.c` mixed render-command functions and the Module 16 Build/Refine boundary. The translated code populates `backtable[]`, `foretable[]`, `midtable[]`, `wipetable[]`, object-table render entries, and text state, while leaving actual table flushing, Canvas drawing, peel restoration, and lighting in Phase 16e.

Review findings:
- Must fix: `show_time()`/`draw_hp()` omitted the C `global_blink_state` path used when `fix_one_hp_stops_blinking` is enabled. Applied by adding `GameState.globalBlinkState`, toggling it in `Seg008.showTime()`, and making HP blink rendering use it when the fix flag is set.
- Should fix: update the `Seg008.kt` phase header to reflect both Phase 16c and Phase 16d responsibilities.
- Optional: none applied.

Verification: `gradle test --tests com.sdlpop.game.Seg008Test --no-daemon` passed; full `gradle test --no-daemon` passed in `SDLPoP-kotlin` with 642 tests.

### 2026-05-02 — Step 16d.4: Wall pattern, marks, timer text, and phase integration

**Mode:** Code | **Outcome:** Complete — final Phase 16d render-submission functions translated
**Contract changes:** `Seg008` wall-pattern hook now defaults to real render-table submissions; timer/level text functions now update shared game text state and dispatch through the existing text stub. No Android/SDL pixel drawing contract added.

Translated the remaining `seg008.c` render-submission/text slice into `Seg008.kt`: `wall_pattern`, `draw_left_mark`, `draw_right_mark`, `show_time`, and `show_level`. The wall-pattern port preserves the C table-switching behavior for back/fore submissions, deterministic seed derivation from room/row/column, PRNG save/restore, dungeon divider/decal branches, palace VGA wipe-color blocks, and palace wall decal submissions. The Kotlin implementation also guards the existing `Seg002.prandom()` seed-init wrapper by temporarily marking the deterministic wall seed initialized, then restoring both seed fields.

Ported timer and level text state updates behind `ExternalStubs.displayTextBottom`, preserving minute rollover, one-minute second countdown messages, expired-time text, level 13 display remapping, seamless-level suppression, and text duration state. `resetRenderHooks()` now restores the real wall-pattern implementation.

Expanded `Seg008Test` with focused coverage for timer/level text messages, dungeon wall-pattern determinism and PRNG restoration, palace wall wipe/foretable submissions, and left/right wall mark placement.

Verification: `gradle test --tests com.sdlpop.game.Seg008Test --no-daemon` passed; full `gradle test --no-daemon` passed in `SDLPoP-kotlin` with 641 tests.

### 2026-05-02 — Step 16d.3: Structures, overlays, people, and object-table flushing

**Mode:** Code | **Outcome:** Complete — structure/overlay/people render command producers translated
**Contract changes:** `Seg008` structure, overlay, people, and object-table hooks now default to real render-table submissions; wall-pattern and timer/level text callbacks remain Phase 16d.4 boundaries.

Translated the next `seg008.c` render-submission slice into `Seg008.kt`: `draw_gate_back`, `draw_gate_fore`, `draw_leveldoor`, `draw_floor_overlay`, `draw_other_overlay`, `draw_people`, `draw_kid`, `draw_guard`, `draw_objtable_items_at_tile`, and `draw_objtable_item`. The implementation preserves gate openness/slice submission, level-door wipe and sliding segment generation, overlay table switching through `addTable`, object-table tile filtering with byte-style sentinel handling, C object sort order, shadow OR/XOR double-submit behavior, loose-floor three-piece output, and Kid/Guard object production through the existing frame/object pipeline.

Also corrected `drawTile2()` to include `drawLoose(0)`, matching the C helper used by overlay redraws. `resetRenderHooks()` now restores real defaults for the translated structure, overlay, object, and people paths while leaving the Phase 16e pixel-flush hooks as no-ops.

Expanded `Seg008Test` with focused coverage for gate back/fore command sequences, level-door stairs/wipe/segments, floor and other overlay table switching, object flush sorting and tile filtering, shadow blend output and end-level sound, and loose-floor object pieces.

Verification: `gradle test --tests com.sdlpop.game.Seg008Test --no-daemon` passed; full `gradle test --no-daemon` passed in `SDLPoP-kotlin` with 637 tests.

### 2026-05-02 — Step 16d.2: Tile render submissions

**Mode:** Code | **Outcome:** Complete — tile render command producers translated
**Contract changes:** `Seg008` tile rendering hooks now default to real render-table submissions; gate/level-door/overlay/object/wall-pattern callbacks remain as Phase 16d.3/16d.4 boundaries.

Translated the tile-render submission slice from `seg008.c` into `Seg008.kt`: `draw_tile_floorright`, `draw_tile_topright`, `draw_tile_anim_topright`, `draw_tile_right`, `draw_tile_anim_right`, `draw_tile_bottom`, `draw_loose`, `draw_tile_base`, `draw_tile_anim`, `draw_tile_fore`, `draw_tile2`, and `draw_tile_wipe`. Added the C `piece tile_table[31]` data and frame lookup tables used by floor, wall, spike, loose-floor, potion, torch, sword, chomper, lattice, and doortop submissions.

Kept the later Phase 16d work separated through explicit hooks: gate/level-door structures, wall pattern generation, overlays, people, and object-table flushing still have their own callbacks and steps. The existing traversal hooks remain overrideable for tests, but `resetRenderHooks()` now restores real tile submission defaults.

Expanded `Seg008Test` with focused render-table assertions for top/right/floor/wall submissions, animated spike/loose/torch paths, loose floor back/fore output, potion bubbles and foreground pots, sword midtable output, chomper blood overlays, wall-pattern dispatch boundaries, and wipe-table geometry. While adding coverage, reset `GameState.currentLevel` in the Seg008 test fixture to avoid cross-test level-type leakage.

Verification: `gradle test --tests com.sdlpop.game.Seg008Test --no-daemon` passed; full `gradle test --no-daemon` passed in `SDLPoP-kotlin` with 630 tests.

### 2026-05-02 — Step 16d.1: Render table data model and append helpers

**Mode:** Code | **Outcome:** Complete — render table queues and append helpers implemented
**Contract changes:** Added Kotlin render-table state/types consumed by later Phase 16d submission functions and Phase 16e draw flushing; no Android/SDL drawing contract added.

Added Kotlin equivalents for `back_table_type`, `midtable_type`, and `wipetable_type`, plus `GameState` storage for `backtable[]`, `foretable[]`, `midtable[]`, and `wipetable[]` with the C table limits. Translated `add_backtable`, `add_foretable`, `add_midtable`, and `add_wipetable` into `Seg008`, preserving zero-id rejection, table-limit rejection, `id - 1` storage, signed byte-style coordinate truncation, image-height-derived top Y, midtable clipping/peel capture, right-facing flip blit adjustment, and draw-mode deferral through no-op hooks for Phase 16e.

Added focused `Seg008Test` coverage for successful back/fore/mid/wipe appends, count changes, field mapping, table limits, missing images, midtable clip fields, and draw-mode hook dispatch. The helpers still do not draw pixels; they only populate render queues.

Verification: `gradle test --tests com.sdlpop.game.Seg008Test --no-daemon` passed; full `gradle test --no-daemon` passed in `SDLPoP-kotlin`.

### 2026-05-02 — Phase 16d plan: Render table submission

**Mode:** Discuss | **Outcome:** Phase planned
**Contract changes:** DEVPLAN.md, DECISIONS.md — phase plan only; no source behavior changes.

Planned Phase 16d as the Build-regime translation of `seg008.c` render-table submission functions. The phase will replace the Phase 16c no-op render hooks with deterministic command producers for `backtable[]`, `foretable[]`, `midtable[]`, and `wipetable[]`, while keeping actual pixel drawing, Android Canvas, peel restoration, lighting, and visual judgment deferred to Phase 16e.

Phase 16d is split into four steps:
- **16d.1** Add render-table data/state and translate append helpers.
- **16d.2** Translate tile render-submission functions.
- **16d.3** Translate structures, overlays, people, and object-table flushing.
- **16d.4** Translate wall pattern/marks, timer/level text state, and integration tests.

Planning decision recorded in `DECISIONS.md` D-25. No source code changed in this iteration.

### 2026-05-02 — Phase 16c approval

**Mode:** Approval | **Outcome:** Human audit accepted; proceed to Phase 16d planning
**Contract changes:** DEVPLAN.md — current status advanced to audited/complete; no source behavior changes.

Recorded human approval for Phase 16c after the completed review and phase-close entry. Module 16 remains in progress, and Phase 16d is now the active next step.

### 2026-05-02 — Phase 16c complete

**Mode:** Complete | **Outcome:** Phase complete — render table pure logic ready for human audit
**Contract changes:** DEVPLAN.md, ARCHITECTURE.md, DECISIONS.md — phase status and completion metadata only; no source behavior changes.

Completed Phase 16c closure after review. The phase delivered the pure `seg008.c` render-state translation in `Seg008.kt`: tile resolution, room-link and adjacent-tile loading, modifier preprocessing, object table insertion/sorting/loading, dirty-rect merging, and draw/redraw orchestration behind Phase 16d render-submission hooks.

No new gotchas were promoted from the phase learning review; the main C/Kotlin room-buffer pointer issue was already captured in DEVPLAN during Step 16c.2. DEVLOG is above 500 lines but remains an active Module 16 log, so no archive split was performed in this completion step.

Verification: `gradle test --tests com.sdlpop.game.Seg008Test --no-daemon` passed; full `gradle test --no-daemon` passed in `SDLPoP-kotlin` with 620 tests.

### 2026-05-02 — Phase 16c review

**Mode:** Review | **Outcome:** Complete — no must-fix or should-fix findings
**Contract changes:** DEVPLAN.md, DECISIONS.md — review status only; no source behavior changes.

Reviewed the Phase 16c render-state implementation against the `seg008.c` pure-state contract. The translated `Seg008.kt` slice keeps Android/SDL pixel drawing out of scope, leaves Phase 16d render-table submission behind hooks, and covers tile resolution, adjacent-room loading, modifier preprocessing, object-table state, dirty-rect bookkeeping, and draw/redraw traversal with focused tests.

Verification: `gradle test --tests com.sdlpop.game.Seg008Test --no-daemon` passed; full `gradle test --no-daemon` passed in `SDLPoP-kotlin` with 620 tests.

### 2026-04-30 — Step 16c.4: Pure orchestration with render-submission hooks

**Mode:** Code | **Outcome:** Complete — draw/redraw traversal translated behind no-op/test-capturable render hooks
**Contract changes:** `Seg008` now exposes render-submission hook callbacks for Phase 16d functions while keeping the actual render-table appenders unimplemented.

Translated the pure orchestration slice from `seg008.c` into `Seg008.kt`: `draw_room`, `draw_tile`, `draw_tile_aboveroom`, `redraw_needed`, `redraw_needed_above`, `redraw_needed_tiles`, and `draw_moving`. The implementation preserves the C traversal order for visible rows, above-room traversal, current-room restoration, draw Y temporaries, redraw counter decrement order, tile-object redraw clearing, and the compile-time `FIX_ABOVE_GATE` / `FIX_BIGPILLAR_JUMP_UP` behavior used by the reference build.

Kept Phase 16d render submission functions behind no-op hook callbacks so this step can validate orchestration without creating real `backtable[]`/`foretable[]`/`midtable[]`/`wipetable[]` entries yet. `draw_moving()` reuses the translated `Seg007.drawMobs()` for loose-floor object production, then calls a hook for the not-yet-translated people renderer before processing redraw tiles.

Expanded `Seg008Test` with call-order and state-mutation coverage for full tile draw order, above-room tile order, redraw counter priority/fallback behavior, object-redraw sentinel handling, above-room redraw behavior, current/above-room traversal, sentinel object-table flushes, and moving-object orchestration.

Verification: `gradle test --tests com.sdlpop.game.Seg008Test` passed; full `gradle test` passed in `SDLPoP-kotlin` with 620 tests.

### 2026-04-30 — Step 16c.3: Object table and dirty-rect bookkeeping

**Mode:** Code | **Outcome:** Complete — object-table state, draw-order sorting, frame-to-object conversion, and dirty-rect merging translated and verified
**Contract changes:** `GameState` now includes the `drects[30]` dirty-rectangle table; `ExternalStubs.addObjtable` now defaults to the translated `Seg008.addObjtable()` implementation instead of a rendering no-op.

Translated the next pure `seg008.c` render-state slice into `Seg008.kt`: `add_drect`, `sort_curr_objs`, `compare_curr_objs`, `load_obj_from_objtable`, `add_kid_to_objtable`, `add_guard_to_objtable`, `add_objtable`, `mark_obj_tile_redraw`, and `load_frame_to_obj`. The implementation preserves the C table-count behavior, object x/y/id byte and sbyte wrapping, shadow/guard object type selection, C draw-order comparisons, tile-object redraw marking, and dirty-rect union when the expanded source rectangle intersects an existing dirty rectangle.

Wired the shared `ExternalStubs.addObjtable` callback to the translated render-state implementation so existing sword, hurt-splash, mirror-image, and loose-floor object producers append to the real object table by default.

Expanded `Seg008Test` with focused coverage for direct object-table insertion, object temporary reload, mixed object sorting, loose-floor sorting, Kid/Guard insertion through the translated character pipeline, frame-to-object coordinate calculation, and dirty-rect append/merge behavior.

Verification: `gradle test --tests com.sdlpop.game.Seg008Test` passed; full `gradle test` passed in `SDLPoP-kotlin` with 613 tests.

### 2026-04-30 — Step 16c.2: Room links, adjacent tiles, and modifier preprocessing

**Mode:** Code | **Outcome:** Complete — room-link loading and modifier preprocessing translated and verified
**Contract changes:** `GameState` now includes `torchColors` render state for colored torch modifier preprocessing; `Seg008` now owns room-link/adjacent-tile loading and C-equivalent modifier preprocessing for render setup.

Translated the next pure `seg008.c` slice into `Seg008.kt`: `load_room_links`, `load_leftroom`, `load_rowbelow`, `load_curr_and_left_tile`, `alter_mods_allrm`, and `load_alter_mod`. The implementation reuses `ExternalStubs.loadRoomAddress()` for room-buffer loading, computes direct and diagonal room neighbors, resolves current/left/below tiles through the existing `getTileToDraw()` rules, clamps `level.usedRooms` to 24, and preprocesses gate, loose-floor, potion, wall, fake-wall, CGA/Herc wall, and colored-torch modifiers.

Because Kotlin room buffers are copied rather than C pointers into `level.fg[]`/`level.bg[]`, `loadAlterMod()` writes modifier changes to both `currRoomModif[]` and the corresponding `level.bg[]` slot for the loaded room. This preserves the C behavior where later room reloads see the preprocessed modifiers.

Expanded `Seg008Test` with level-backed fixtures for room links, diagonal neighbor fallback, current/left/below tile loading, top/left edge tiles, all major modifier transformations, wall connection bits across neighbor rooms, fake wall connection modifiers, CGA/Herc wall behavior, torch color capture, and level-buffer sync.

Verification: `gradle test --tests com.sdlpop.game.Seg008Test` passed; full `gradle test` passed in `SDLPoP-kotlin` with 606 tests.

### 2026-04-30 — Step 16c.1: Seg008 render-state scaffold and tile helpers

**Mode:** Code | **Outcome:** Complete — pure Seg008 helper scaffold translated and verified
**Contract changes:** `GameState` now includes render-state globals needed by pure `seg008.c` helpers; `ExternalStubs.calcScreenXCoord` now defaults to the translated `calc_screen_x_coord` behavior.

Added the initial `Seg008` Kotlin module for pure render-state logic without pixel drawing or render-table submission. The step translated `calc_screen_x_coord`, `can_see_bottomleft`, `get_spike_frame`, `get_loose_frame`, `calc_gate_pos`, and `get_tile_to_draw`, including pressed-button doorlink handling, fake tile display rules, room-0 edge tiles, left-room lookups, current room-buffer lookup, and the `fix_loose_left_of_potion` gate.

Extended `GameState` with the missing render temporaries used by these helpers (`drawnCol`, `drawnRow`, `tileLeft`, `modifierLeft`, draw Y positions, and gate geometry fields), and wired `ExternalStubs.calcScreenXCoord` to `Seg008.calcScreenXCoord` by default while preserving the existing callable override pattern used by focused tests.

Added `Seg008Test` coverage for screen-X scaling, bottom-left visibility, spike/loose frame conversion, gate geometry, room-buffer/left-room/room-0 tile resolution, pressed opener/closer conversion, fake tile transforms, and loose-floor fix behavior.

Verification: `gradle test --tests com.sdlpop.game.Seg008Test` passed; full `gradle test` passed in `SDLPoP-kotlin` with 596 tests.

### 2026-04-30 — Phase 16c Plan: Render table pure logic

**Mode:** Discuss | **Outcome:** Phase planned
**Contract changes:** DEVPLAN.md, DECISIONS.md — phase step breakdown and planning decision only; no source behavior changes.

Planned Phase 16c as the Build-regime translation path for the pure `seg008.c` render-state functions. The phase keeps the rendering boundary at table production: no Android Canvas work, no SDL drawing, and no real render-table append implementation from Phase 16d.

Phase 16c is split into four steps:
- **16c.1** Add the `Seg008` scaffold, missing render-state support, tile helper functions, `get_tile_to_draw`, and real `calc_screen_x_coord` wiring.
- **16c.2** Translate room-link/adjacent-tile loading and `alter_mods_allrm` / `load_alter_mod` modifier preprocessing.
- **16c.3** Translate object-table, draw-order sorting, frame-to-object, tile-redraw marking, and dirty-rect bookkeeping.
- **16c.4** Translate pure draw/redraw orchestration behind no-op or test-capturable render-submission hooks, leaving real table appenders to Phase 16d.

Planning decision recorded in `DECISIONS.md` D-21. No source code changed in this iteration.

### 2026-04-30 — Phase 16b Approval

**Mode:** Review | **Outcome:** Reviewed and approved
**Contract changes:** DEVPLAN.md, DECISIONS.md — approval status only; no interface or behavior contract changes

Human approval recorded for Phase 16b after the completed review and phase-close entry. Module 16 remains in progress, and Phase 16c is now unblocked for planning.

### 2026-04-30 — Phase 16b complete

**Mode:** Complete | **Outcome:** Phase complete — asset loading pipeline accepted for human audit
**Contract changes:** DEVPLAN now records Phase 16b as complete and promotes the DAT checksum-byte boundary plus JVM-testable asset decode split as gotchas for future asset/render work.

Completed the Phase 16b closure after review. The phase delivered the Android-independent `com.sdlpop.assets` pipeline for DAT table parsing, checksum-stripped resource reads, RLE/LZG decompression, packed-pixel expansion, palette-to-ARGB image decode, source-neutral DAT/PNG lookup, and chtab sprite catalog loading. The Android app bridge owns `AssetManager` byte access and `Bitmap` creation.

Acceptance remains satisfied: KID chtab 2 loads 219 sprites, GUARD chtab 5 loads 34 sprites, both catalogs match the existing `SpriteDimensions.kt` dimensions, and all decompressor/image tests are covered by the JVM suite. Human visual verification of decoded palette, transparency, orientation, and alignment remains the audit item before relying on these assets in the renderer.

Verification: `gradle test` passed from the repo root.

### 2026-04-30 — Phase 16b review

- **Mode:** Review
- **Outcome:** complete — one should-fix applied
- **Contract changes:** `AssetRepository.loadFromOpenDatsMetadata` removed.

Reviewed all phase 16b output (`com.sdlpop.assets` package: `AssetModels.kt`, `AssetParsers.kt`, `AssetCodecs.kt`, `AssetRepository.kt`, `AssetImages.kt`, `AssetParsersTest.kt`, plus `app/.../AndroidAssetSource.kt` and `app/.../BitmapImageDecoder.kt`) against the phase contract and the SDLPoP `seg009.c` source. Decompressors, header parsers, palette conversion, repository layering, decoded image models, and Android bridges line up with the C reference behavior.

Should-fix applied: removed `AssetRepository.loadFromOpenDatsMetadata`, which was a delegating duplicate of `loadFromOpenDatsAlloc` whose name implied SDLPoP's metadata-only contract while the implementation actually returned full bytes. The single test caller now uses `loadFromOpenDatsAlloc`. The Kotlin port intentionally collapses the C `metadata`/`alloc` split into one `LoadedAssetResource`-returning entry point because Kotlin does not expose raw `FILE*` handles, so a separate metadata-only function adds no value.

Optional findings logged but skipped per autonomous review remit:
- `AssetImages.decodeDatImage` throws on zero-height DAT images instead of returning `null` like `seg009.c::decode_image()`. The path is unreachable in practice because `loadFromOpenDatsAlloc` already filters PNG-extension DAT entries with `metadata.size <= 2` (the SDLPoP empty-image marker), and no real fixture produces a non-marker zero-height resource.
- `AssetCodecs.decompressLzgUd` keeps `destEnd` as a Kotlin `Int`, while C truncates `dest_end` to `short`. The discrepancy is observable only for LZG-UD images whose decompressed size exceeds 32,767 bytes; no SDLPoP DAT fixture in the asset tree triggers this.

Verification: `gradle test` passes (full project) after the fix.

### 2026-04-30 — Step 16b.4: Bitmap decode and sprite catalog integration

**Mode:** Code | **Outcome:** Complete — DAT palette decode, Android Bitmap bridge, and KID/GUARD sprite catalogs verified
**Contract changes:** `com.sdlpop.assets` now exposes decoded image models, `AssetImages.loadImage()`, and `AssetImages.loadSpriteCatalog()` for chtab-style sprite loading; the Android app module adds `BitmapImageDecoder` to convert decoded DAT pixels or PNG bytes into `Bitmap` instances.

Translated the remaining `seg009.c` asset image boundary into Kotlin: DAT images now decompress through the existing codecs, expand packed pixels to 8bpp indices, apply the DAT VGA palette into ARGB_8888 pixels, and preserve color 0 as transparent. Directory PNG resources remain source-backed and carry parsed dimensions plus original bytes so Android can decode them through `BitmapFactory`.

Added a sprite catalog model matching `load_sprites_from_file()` semantics: a `.pal` resource provides the image count and palette, then resources `pal_resource + 1` through `pal_resource + n_images` are loaded as the chtab image list. The catalog keeps SDLPoP's zero-based image storage but exposes one-based frame-ID lookups for integration with the translated game/render code.

Added JVM tests for DAT ARGB palette conversion, KID chtab 2 loading from directory PNG assets, and GUARD chtab 5 loading from GUARD.DAT with the GUARD1 palette. The KID 219-sprite and GUARD 34-sprite catalogs now match every dimension in `SpriteDimensions.kt`, which provides the handoff point for replacing the headless hardcoded table with loaded asset dimensions later.

Human visual verification handoff: palette correctness, color-0 transparency, PNG/DAT orientation, and apparent sprite alignment still need inspection on Android once the renderer consumes these catalogs.

Verification: `gradle test` passed with 586 tests.

### 2026-04-30 — Step 16b.3: DAT and PNG resource loading

**Mode:** Code | **Outcome:** Complete — DAT/directory asset lookup ported and verified
**Contract changes:** `com.sdlpop.assets` now includes a source-neutral `AssetByteSource` plus `AssetRepository` APIs corresponding to `open_dat`, `load_from_opendats_metadata`, `load_from_opendats_alloc`, and `load_from_opendats_to_area`.

Added a JVM-testable asset loading boundary that mirrors the relevant `seg009.c` DAT chain behavior: opened archives are searched newest-first, DAT table entries take precedence, checksum bytes are skipped before payload reads, empty DAT PNG markers can fall through to directory resources, and missing `.DAT` files can still resolve through sibling asset directories such as `KID/`.

Added `AndroidAssetSource` in the app module as the Android bridge from `AssetManager` to the shared byte-source interface. The JVM tests use the same interface against the packaged `app/src/main/assets` tree, proving KID directory-backed palette/PNG resources and GUARD.DAT-backed image resources are discoverable without SDL file APIs.

Verification: `gradle test` passed with 583 tests.

### 2026-04-30 — Step 16b.2: Pure decompression and pixel expansion

**Mode:** Code | **Outcome:** Complete — pure DAT image codecs translated and verified
**Contract changes:** `AssetParsers.readResourceBytes()` now returns the checksum-stripped DAT resource payload, matching `seg009.c::load_from_opendats_alloc()`; callers receive bytes beginning at the resource's actual image/palette header.

Translated the portable `seg009.c` asset codec slice into JVM-testable Kotlin in `AssetCodecs.kt`: RLE left-to-right, RLE up-to-down, LZG left-to-right, LZG up-to-down, the compression-method dispatcher, stride calculation, and packed 1/2/4bpp pixel expansion to one byte per pixel. The implementation keeps C-equivalent unsigned-byte handling for compressed stream bytes, LZG copy windows, packed nibbles, and column-major up/down writes.

While wiring real DAT fixtures, corrected the Step 16b.1 DAT payload boundary: SDLPoP reads one checksum byte at the resource offset, then reads `size` bytes of payload. The Kotlin parser now validates `offset + 1 + size` and returns bytes after the checksum, so `ImageDataHeader` parsing sees the real little-endian `height`, `width`, and `flags`.

Added golden-output tests covering a synthetic RLE left-to-right packet, GUARD.DAT raw/RLE/LZG resource fixtures for compression methods 0, 2, 3, and 4, decompressed byte prefixes and checksums, packed pixel expansion prefixes and checksums, and explicit 1/2/4bpp expansion behavior.

Verification: `gradle test --tests com.sdlpop.assets.AssetParsersTest` passed. Full `gradle test` passed with 579 tests, 0 failures, 0 errors.

### 2026-04-28 — Step 16b.1: Asset codec contract and golden fixtures

**Mode:** Code | **Outcome:** Complete — JVM asset metadata contract and golden fixtures added
**Contract changes:** `SDLPoP-kotlin/src/main/kotlin/com/sdlpop/assets/` — new Android-independent asset metadata boundary for DAT archives, sprite palette resources, image-data headers, PNG metadata, and VGA palette color conversion.

Audited the relevant `seg009.c` asset path (`load_sprites_from_file`, DAT table lookup, `image_data_type`, `dat_pal_type`, `decode_image`, and decompressor dispatch) and added a pure Kotlin `com.sdlpop.assets` package that keeps byte parsing and metadata separate from later Android `Bitmap` creation. The contract models DAT resource locations, DAT archive tables, packed image headers (`height`, `width`, `flags`, depth, compression method, stride), sprite palette resources, 6-bit VGA palette entries, and PNG IHDR metadata.

Added golden fixture tests against the packaged Android assets under `app/src/main/assets`: KID palette `res400.pal` verifies 219 images and VGA palette parsing, KID/GUARD/VDUNGEON PNG fixtures verify dimensions and indexed-color metadata, and `GUARD.DAT` verifies DAT table parsing plus compressed image header metadata for resource 751. These tests establish the byte-level oracle that Step 16b.2 can use when translating decompression and pixel expansion.

Verification: attempted `gradle test --tests 'com.sdlpop.assets.AssetParsersTest'` from `SDLPoP-kotlin/`, but Gradle failed during project configuration because `SDLPoP-kotlin/build.gradle.kts` now requests a JDK 21 toolchain and this container only has OpenJDK 17. No test code executed.

### 2026-04-28 — Phase 16a: Android project scaffold

**Mode:** Code (human-driven) | **Outcome:** Complete — Android app builds and deploys, game logic module linked
**Contract changes:** `settings.gradle.kts` — new root-level multi-module build replacing `SDLPoP-kotlin/settings.gradle.kts`. `SDLPoP-kotlin/build.gradle.kts` — JVM toolchain 21, system property paths adjusted for root-relative layout.

Created Android project scaffold as a multi-module Gradle project. Root `settings.gradle.kts` conditionally includes the `app` module only when `local.properties` exists (Android SDK present), so the Pi can still run `gradle build`/`gradle test` on `:SDLPoP-kotlin` without AGP or the Android SDK.

Android `app` module: `com.sdlpop.android` namespace, SDK 24–34, Kotlin 1.9.22, depends on `:SDLPoP-kotlin` for game logic. `GameActivity` sets up fullscreen landscape mode. `GameSurfaceView` implements `SurfaceHolder.Callback` and draws a test frame confirming the rendering pipeline is wired. All SDLPoP `data/` assets (KID/GUARD/SHADOW/SKEL/VIZIER sprites, LEVELS, VDUNGEON/VPALACE tilesets, sound DATs, font, music) packaged under `app/src/main/assets/`.

Decisions: `SurfaceView` + `Canvas` chosen over `GLSurfaceView` for the 320×200 tile-based 2D rendering (no 3D needed). Min SDK 24 (Android 7.0, covers 98%+ of devices). Conditional module inclusion pattern keeps Pi and Windows builds independent.

## Module 15: Game Loop

### 2026-04-20 — Phase 15c: Sprite dimension table for headless collision

**Mode:** Code (human-driven) | **Outcome:** 8/13 traces match (up from 5/13)
**Contract changes:** `SpriteDimensions.kt` — new file with hardcoded sprite (width, height) arrays. `ExternalStubs.kt` — `getImage` now returns dimensions from `SpriteDimensions` instead of null.

Created `SpriteDimensions.kt` with hardcoded width/height arrays for chtab 2 (KID, 219 sprites) and chtab 5 (GUARD, 34 sprites), extracted from PNG headers in `SDLPoP/data/KID/` and `SDLPoP/data/GUARD/`. Wired `ExternalStubs.getImage()` to return `SpriteDimensions.getImageDimensions(chtab, imageId)`. This gives `setCharCollision()` correct `charWidthHalf` and `charHeight` values so collision-dependent functions (`checkSpikeBelow()`, `checkGrab()`, `checkPress()`, etc.) compute correct column ranges.

Impact: 3 new trace matches (`basic_movement`, `snes_pc_set_level11`, `traps`, `trick_153` — 4 traces newly matching, but `falling_through_floor_pr274` regressed from Phase 15b's match back to diverging). Net: 5/13 → 8/13.

Quick triage of remaining 5 divergences:
- `falling_through_floor_pr274` f0: tile modifier init subtlety — Kid state matches but `curr_room_modif[17]` differs (exp=6 act=4)
- `demo_suave_prince_level11` f29: control dispatch — frame 28 matches (both have frame=50, running), then C transitions to standing turn (frame 16) while Kotlin starts a new run (frame 1)
- `grab_bug_pr288` f17 + `grab_bug_pr289` f16: grab detection failure — Kid should grab a ledge but doesn't. C goes to frame 91 (hang), Kotlin continues jumping/falling
- `sword_and_level_transition` f275: level restart — C respawns the Kid in room 5 after death, Kotlin stays dead in room 9. Requires `start_game()`/`play_level()` lifecycle

### 2026-04-20 — Phase 15b human-driven debug session: 3 bugs found, 5/13 traces

**Mode:** Code (human-driven) | **Outcome:** 5/13 traces match, remaining 8 root-caused to missing sprite dimensions
**Contract changes:** `Seg006.kt` — byte masking on `Char.x`/`Char.y` arithmetic. `Seg002.kt` — byte masking on `gotoOtherRoom()` x/y. `ReplayRunner.kt` — `restore_room_after_quick_load()` equivalent, `soundFlags |= DIGI`, `lastLooseSound`/`soundFlags` reset. `Layer1FrameDriver.kt` — `loadRoomLinks()` visibility changed to internal. `build.gradle.kts` — `testClassesDirs`/`classpath` for `layer1ReplayRegression` task.

Human-driven investigation after Step 15b.8 autonomous escalation. Used targeted per-frame state instrumentation (RNG call logging, Kid state comparison against reference trace bytes) to identify three bugs:

**Bug 1 — `soundFlags` not initialized for replay mode:** C reference builds have `sound_flags & sfDigi` set, enabling `lastLooseSound` tracking in `loose_shake()`'s `do-while` loop. Without it, the loop consumed extra `prandom(2)` calls, causing RNG drift. Diagnostic: added `prandom()` call counter + per-call log, compared frame-by-frame `random_seed` against reference trace. Frame 19 of `trick_153` had 2 extra `prandom(2)` calls from the untracked `do-while` loop. Fix: `soundFlags |= SoundFlags.DIGI` in `initializeReplayState()`. Impact: fixed `falling`, resolved RNG drift in `trick_153`/`snes_pc_set_level11`/`basic_movement` (divergence points moved much later).

**Bug 2 — `Char.x`/`Char.y` byte overflow:** C `char_type.x` and `char_type.y` are `byte` (unsigned 8-bit). Arithmetic wraps to 0-255 implicitly. Kotlin stores them as `Int`, so values go negative. Diagnostic: per-frame Kid state dump showed `Kid.y=-5` (Kotlin) vs `Kid.y=62=251-189` (C) at frame 27 of `trick_153`. The climbing sequence applied `dy(-63)` to `y=55`, giving `-8` in Kotlin vs `248` in C. Then `leave_room()` checked `chary >= 211`: C had `248 >= 211` (true, downward exit) while Kotlin had `-8 >= 211` (false, no transition). Fix: `and 0xFF` masking at 6 critical sites — `SEQ_DX`/`SEQ_DY` in `playSeq()`, `fallSpeed()` for both x and y, and `gotoOtherRoom()` for left/right/up/down.

**Bug 3 — Missing `restore_room_after_quick_load()`:** C calls `restore_room_after_quick_load()` after savestate restore, which sets `different_room=1`, `drawn_room=Kid.room`, runs `draw_game_frame()` (reloading room buffers from `level.fg[]/bg[]`), then resets `exit_room_timer=0`. The Kotlin replay runner skipped this, using the savestate's stale `curr_room_modif[]`. Fix: added the equivalent initialization sequence in `writeLayer1Trace()`. Impact: fixed `falling_through_floor_pr274`, resolved `trick_153` frame-0 tile modifier divergence.

**Root cause of remaining 8 divergences:** `setCharCollision()` in headless mode receives `null` from `getImage()` (no chtab sprites loaded), setting `charWidthHalf=0` and `charHeight=0`. This collapses the collision footprint, causing `checkSpikeBelow()` to miss spike tiles at adjacent columns → wrong trob count → cascading RNG and Kid.frame divergences. Confirmed by trob diagnostic showing C has a spike trob at room 6 tilepos 23 (tile type 2) that Kotlin misses despite identical Kid state. Resolving requires loading sprite dimension data from chtab asset files.

### 2026-04-20 — Step 15b.8: Final regression verification and cleanup

**Mode:** Code | **Outcome:** Escalated — replay regression still 4/13 after two targeted fix attempts
**Contract changes:** None.

Ran the ordinary Kotlin suite and the dedicated 13-trace replay regression for the final Phase 15b closure step. Initial verification preserved the Step 15b.7 result: `gradle test --no-daemon` passed, while `gradle layer1ReplayRegression --rerun-tasks --no-daemon` failed with **4/13 MATCH** (`falling`, `original_level2_falling_into_wall`, `original_level5_shadow_into_wall`, `original_level12_xpos_glitch`).

Two targeted fixes were attempted. First, same-room `draw_level_first()` animated tile startup was tested by running `animTileModif()` after `loadRoomLinks()` in the same-room first-draw path. Focused replay-runner coverage passed, but replay regression moved many traces to new frame-0 tile-modifier divergences, so the change was reverted. Second, `Seg007.animateTile()` was corrected to write the updated `currRoomModif[trob.tilepos]` value back into `level.bg`, matching C pointer semantics where `curr_room_modif` points directly into `level.bg`. This retained fix removed the `sword_and_level_transition` frame-0 tile-modifier divergence, shifting that replay back to its prior frame 275 `Kid.frame` divergence, but did not improve the total match count.

Final verification: `gradle test --tests com.sdlpop.game.Seg007Test --tests com.sdlpop.replay.ReplayRunnerTest --no-daemon` passed; `gradle test --no-daemon` passed; `gradle layer1ReplayRegression --rerun-tasks --no-daemon` still failed with **4/13 MATCH**. No third targeted fix was attempted per Phase 15b acceptance. Superseded shim cleanup was not performed because the replay acceptance gate is still failing and the remaining lifecycle boundary is unresolved.

Remaining divergences are triage-ready: `basic_movement` frame 325 `Kid.frame` expected `103` actual `102`; `demo_suave_prince_level11` frame 29 `Kid.frame` expected `16` actual `1`; `falling_through_floor_pr274` frame 0 `curr_room_modif[17]` expected `6` actual `4`; `grab_bug_pr288` frame 17 `Kid.frame` expected `91` actual `40`; `grab_bug_pr289` frame 16 `Kid.frame` expected `91` actual `102`; `snes_pc_set_level11` frame 40 `trobs_count` expected `3` actual `2`; `sword_and_level_transition` frame 275 `Kid.frame` expected `46` actual `0`; `traps` frame 41 `Kid.frame` expected `50` actual `55`; and `trick_153` frame 27 `Kid.y` expected `62` actual `251`.

### 2026-04-20 — Step 15b.7: Orchestrator-diagnosed fixes

**Mode:** Code | **Outcome:** Complete — diagnosed fixes applied, replay regression remains 4/13
**Contract changes:** None.

Applied the two fixes identified by the orchestrator C-source audit. `HeadlessFrameLifecycle.headlessDrawLevelFirst()` now always runs the translated `redrawScreen(false)` tail after first-room setup, matching `draw_level_first()`'s unconditional `redraw_screen(0)` call and setting `exitRoomTimer = 2` while clearing `differentRoom`. `Layer1FrameDriver.playKidFrame()` now sets `needRedrawBecauseFlipped` when upside-down mode expires instead of incorrectly forcing `needFullRedraw`.

Added focused replay-runner coverage for both corrected behaviors: first-draw setup now asserts the room flag is cleared and the exit-room timer is initialized, and upside-down expiry now asserts the flipped redraw flag is selected without setting the full-redraw flag.

Verification: `gradle test --no-daemon` passed with 573 tests. `gradle layer1ReplayRegression --rerun-tasks --no-daemon` still failed with **4/13 MATCH** (`falling`, `original_level2_falling_into_wall`, `original_level5_shadow_into_wall`, `original_level12_xpos_glitch`). The remaining divergences are unchanged from Step 15b.6: `basic_movement` frame 325 `Kid.frame` expected `103` actual `102`; `demo_suave_prince_level11` frame 29 `Kid.frame` expected `16` actual `1`; `falling_through_floor_pr274` frame 0 `curr_room_modif[17]` expected `6` actual `4`; `grab_bug_pr288` frame 17 `Kid.frame` expected `91` actual `40`; `grab_bug_pr289` frame 16 `Kid.frame` expected `91` actual `102`; `snes_pc_set_level11` frame 40 `trobs_count` expected `3` actual `2`; `sword_and_level_transition` frame 275 `Kid.frame` expected `46` actual `0`; `traps` frame 41 `Kid.frame` expected `50` actual `55`; and `trick_153` frame 27 `Kid.y` expected `62` actual `251`.

### 2026-04-20 — Step 15b.6: Regression verification and targeted fixes

**Mode:** Code | **Outcome:** Escalated — replay regression still 4/13 after two targeted fix attempts
**Contract changes:** None.

Ran the ordinary Kotlin suite and the dedicated 13-trace replay regression for the Phase 15b closure step. `gradle test --no-daemon` passed. `gradle layer1ReplayRegression --rerun-tasks --no-daemon` still failed with the same 4 exact matches as Step 15b.5: `falling`, `original_level2_falling_into_wall`, `original_level5_shadow_into_wall`, and `original_level12_xpos_glitch`.

Two targeted fixes were tested and rejected. First, `headlessDrawLevelFirst()` was changed to include the C `draw_level_first()` tail state: palace wall color generation and `redraw_screen(0)`. Focused replay-runner tests passed, but the replay regression remained 4/13 and moved `trick_153` from frame 27 to a new frame-0 `curr_room_modif[3]` divergence, so the change was reverted. Second, the headless draw-frame hook was tested with translated `Seg003.checkMirror()`, because C reaches `check_mirror()` through `draw_people()` during `draw_game_frame()`. Focused replay-runner tests passed, but the replay regression was unchanged at 4/13, so that change was also reverted.

Per Phase 15b acceptance, no third targeted fix was attempted. No production code changes are retained from this step, and superseded shim cleanup was not performed because the replay acceptance gate is still failing.

Remaining divergences are triage-ready: `basic_movement` frame 325 `Kid.frame` expected `103` actual `102`; `demo_suave_prince_level11` frame 29 `Kid.frame` expected `16` actual `1`; `falling_through_floor_pr274` frame 0 `curr_room_modif[17]` expected `6` actual `4`; `grab_bug_pr288` frame 17 `Kid.frame` expected `91` actual `40`; `grab_bug_pr289` frame 16 `Kid.frame` expected `91` actual `102`; `snes_pc_set_level11` frame 40 `trobs_count` expected `3` actual `2`; `sword_and_level_transition` frame 275 `Kid.frame` expected `46` actual `0`; `traps` frame 41 `Kid.frame` expected `50` actual `55`; and `trick_153` frame 27 `Kid.y` expected `62` actual `251`.

### 2026-04-20 — Step 15b.5: draw_game_frame state effects

**Mode:** Code | **Outcome:** Complete — draw-frame branching translated, replay regression remains 4/13
**Contract changes:** None.

Translated the deterministic state effects of `seg000.c::draw_game_frame()` into the headless replay lifecycle: full-redraw, different-room, and flipped-redraw branching; palace wall color generation with RNG preservation; `play_next_sound()` queue drain semantics; and text timer countdown/expiry handling including restart text. The replay runner now serializes the trace immediately after `playFrame()` and then applies the headless draw-frame hook, matching the C validate build where `dump_frame_state()` runs at the end of `play_frame()` before the later draw-frame call.

While implementing this step, direct C-source inspection corrected the stale Phase 15b assumption that `redraw_screen()` runs animated tile startup. In SDLPoP, `redraw_screen()` calls `redraw_room()` and sets `exit_room_timer = 2`; animated tile/chomper startup belongs to `check_the_end()`. The headless `redrawScreen()` helper was narrowed accordingly, and tests were updated so draw-frame redraws cover room-link/timer state while `checkTheEnd()` owns chomper startup.

Verification: `gradle test --no-daemon` passed. `gradle layer1ReplayRegression --rerun-tasks --no-daemon` still fails at **4/13 MATCH** (`falling`, `original_level2_falling_into_wall`, `original_level5_shadow_into_wall`, `original_level12_xpos_glitch`). Remaining divergences are triage-ready: `basic_movement` frame 325 `Kid.frame` expected `103` actual `102`; `demo_suave_prince_level11` frame 29 `Kid.frame` expected `16` actual `1`; `falling_through_floor_pr274` frame 0 `curr_room_modif[17]` expected `6` actual `4`; `grab_bug_pr288` frame 17 `Kid.frame` expected `91` actual `40`; `grab_bug_pr289` frame 16 `Kid.frame` expected `91` actual `102`; `snes_pc_set_level11` frame 40 `trobs_count` expected `3` actual `2`; `sword_and_level_transition` frame 275 `Kid.frame` expected `46` actual `0`; `traps` frame 41 `Kid.frame` expected `50` actual `55`; and `trick_153` frame 27 `Kid.y` expected `62` actual `251`.

### 2026-04-20 — Step 15b.4: redraw_screen state effects

**Mode:** Code | **Outcome:** Complete — headless redraw now runs room tile initialization state effects
**Contract changes:** None.

Replaced the thin `HeadlessFrameLifecycle.redrawScreen()` shim with the deterministic state-bearing slice of the C redraw path: clear `differentRoom`, reload the current room links/buffers, run animated tile initialization with `animTileModif()`, start chomper trobs via `Seg007.startChompers()`, and set `exitRoomTimer = 2`. Pure rendering, palette, keyboard-buffer, screen-copy, and blind-mode drawing behavior remains out of scope for the headless replay path.

Updated focused replay-runner tests so full-redraw and different-room redraw paths now expect potion/sword tile animations to create trobs and set the room-exit timer. Added coverage that redraw startup initializes chompers for the current character row, and reset `exitRoomTimer` in the test fixture to avoid singleton leakage.

Verification: `gradle test --tests com.sdlpop.replay.ReplayRunnerTest --no-daemon` passed, and full `gradle test --no-daemon` passed. The 13-trace replay regression was not run in this step; Step 15b.5 owns the `draw_game_frame()` branching translation and replay-regression check.

### 2026-04-20 — Step 15b.3: Regression verification and cleanup

**Mode:** Code | **Outcome:** Escalated — replay regression still 4/13 after two targeted fix attempts
**Contract changes:** None.

Ran the full ordinary Kotlin suite and the dedicated 13-trace replay regression for the Phase 15b closure step. `gradle test --no-daemon` passed. `gradle layer1ReplayRegression --rerun-tasks --no-daemon` still failed with the same 4 exact matches as Step 15b.2: `falling`, `original_level2_falling_into_wall`, `original_level5_shadow_into_wall`, and `original_level12_xpos_glitch`.

Two targeted fixes were tested and rejected. First, the headless redraw shim was given C `redraw_screen()`'s `exit_room_timer = 2` side effect. Unit tests passed, but the replay regression introduced a new frame-0 divergence in `sword_and_level_transition` and shifted `grab_bug_pr288` to a different later failure, so the change was reverted. Second, the replay runner was tested with C-like trace timing by serializing before `headlessDrawGameFrame()`. Unit tests passed, but the replay regression remained unchanged at 4/13, so that change was also reverted.

Per Phase 15b acceptance, no third targeted fix was attempted. Because the trace acceptance gate is still failing, the requested cleanup of superseded `HeadlessFrameLifecycle` code was not performed; no production code changes are retained from this step.

Remaining divergences are triage-ready: `basic_movement` frame 325 `Kid.frame` expected `103` actual `102`; `demo_suave_prince_level11` frame 29 `Kid.frame` expected `16` actual `1`; `falling_through_floor_pr274` frame 0 `curr_room_modif[17]` expected `6` actual `4`; `grab_bug_pr288` frame 11 `curr_room_tiles[2]` expected `0` actual `47`; `grab_bug_pr289` frame 16 `Kid.frame` expected `91` actual `102`; `snes_pc_set_level11` frame 40 `trobs_count` expected `3` actual `2`; `sword_and_level_transition` frame 138 `curr_room_tiles[0]` expected `52` actual `47`; `traps` frame 41 `Kid.frame` expected `50` actual `55`; and `trick_153` frame 27 `Kid.y` expected `62` actual `251`.

### 2026-04-20 — Step 15b.2: Per-frame draw-game-frame hook

**Mode:** Code | **Outcome:** Complete — headless draw-frame flag handling wired before trace serialization
**Contract changes:** None.

Added `HeadlessFrameLifecycle.headlessDrawGameFrame()` and call it from `ReplayRunner.writeLayer1Trace()` immediately after `Layer1FrameDriver.playFrame()` and before `StateTraceFormat.serializeFrameBytes()`. The helper mirrors the deterministic state-bearing part of `seg000.c::draw_game_frame()`: handle `needFullRedraw`, `differentRoom`, and `needRedrawBecauseFlipped`; move `drawnRoom` to `nextRoom` on room redraws; clear `differentRoom`; and refresh room links/current room buffers through the existing headless room loader.

The implementation deliberately does not re-run `animTileModif()` or `startChompers()` from the draw-frame hook. Trial runs showed that doing so duplicates the room-entry initialization already performed by `checkTheEnd()` during `playFrame()` and regresses previously matching traces to frame-0 tile-modifier divergences. The retained hook is therefore limited to the non-rendering redraw bookkeeping that does not duplicate game-logic tile initialization.

Added focused replay-runner coverage for full-redraw and different-room redraw flag handling, plus reset coverage for `needFullRedraw` and `needRedrawBecauseFlipped` so replay manifests do not leak singleton redraw state between runs.

Verification: `gradle test --no-daemon` passed. The dedicated replay regression still fails but preserves the 4/13 exact-match baseline (`falling`, `original_level2_falling_into_wall`, `original_level5_shadow_into_wall`, `original_level12_xpos_glitch`). Current divergences remain triage-ready: `basic_movement` frame 325 `Kid.frame`, `demo_suave_prince_level11` frame 29 `Kid.frame`, `falling_through_floor_pr274` frame 0 `curr_room_modif[17]`, `grab_bug_pr288` frame 11 `curr_room_tiles[2]`, `grab_bug_pr289` frame 16 `Kid.frame`, `snes_pc_set_level11` frame 40 `trobs_count`, `sword_and_level_transition` frame 138 `curr_room_tiles[0]`, `traps` frame 41 `Kid.frame`, and `trick_153` frame 27 `Kid.y`.

### 2026-04-20 — Step 15b.1: Initial room setup

**Mode:** Code | **Outcome:** Complete — headless first-room setup wired before replay frame 0
**Contract changes:** None.

Added `HeadlessFrameLifecycle.headlessDrawLevelFirst()` and call it from `ReplayRunner.writeLayer1Trace()` immediately after replay savestate restoration and before installing the replay input hook / entering the first `playFrame()`. The helper follows the C `draw_level_first()` room-entry gate: set `nextRoom = Kid.room`, run the existing `checkTheEnd()` animated-tile/chomper setup only when the starting room differs from `drawnRoom`, and otherwise refresh room links without re-randomizing already-initialized same-room tile modifiers.

Added focused replay-runner coverage for first-room link loading and animated-tile startup, and tightened that test class's singleton fixture reset for level room links, tile arrays, and trobs.

Verification: `gradle test --tests com.sdlpop.replay.ReplayRunnerTest --no-daemon` passed, and full `gradle test --no-daemon` passed. The dedicated replay regression still fails, but preserves the 4/13 exact-match baseline (`falling`, `original_level2_falling_into_wall`, `original_level5_shadow_into_wall`, `original_level12_xpos_glitch`). `traps` moved from frame 0 `curr_room_modif[0]` to frame 41 `Kid.frame`, confirming the initial room transition setup for that replay. `falling_through_floor_pr274` still diverges at frame 0 `curr_room_modif[17]` expected `6` actual `4`, so the remaining same-room first-draw torch initialization belongs to the next draw-frame alignment slice.

### 2026-04-16 — Phase 15a: seg003 translation + stub wiring

**Mode:** Code | **Outcome:** Complete — awaiting gradle verification on Pi
**Contract changes:** `ExternalStubs.kt` — `doPickup` wired to `Seg006.doPickup()`, `setStartPos` wired to `Seg003.setStartPos()`, `addLife`/`setHealthLife`/`featherFall`/`toggleUpside` implemented, `expired`/`startGame` made safe for headless mode, `drawKidHp` stub added.

Translated all 22 seg003.c functions into `Seg003.kt`: `doStartpos`, `setStartPos`, `findStartLevelDoor`, `posGuards`, `checkCanGuardSeeKid`, `getTileAtKid`, `doMouse`, `redrawAtChar`, `redrawAtChar2`, `checkMirrorImage`, `checkKnock`, `checkMirror`, `jumpThroughMirror`, `bumpIntoOpponent`, `removeFlashIfHurt`, `timers`, and 6 Tier 3 orchestrators deferred (init_game, play_level, play_level_2, draw_level_first, redraw_screen, flash_if_hurt — heavy SDL, needed for game running not trace matching).

Integrated `Seg003.timers()` into `ReplayRunner.writeLayer1Trace()` before each `playFrame()` call, matching C `play_level_2()` order: `guardhp_delta=0` → `hitp_delta=0` → `timers()` → `play_frame()`. Added level-specific exit events (levels 0/6/12) and `expired()` check to `Layer1FrameDriver.playFrame()`. Replaced three `HeadlessFrameLifecycle` shims with proper seg003 translations (`checkCanGuardSeeKid`, `bumpIntoOpponent`, `checkKnock`).

Added `needRedrawBecauseFlipped` to `GameState.kt`.

Code review found and fixed 4 issues: missing `.toInt()` on 3 `yLand[]` → `Char.y` assignments, `unitedWithShadow--` on Short (changed to explicit `.toShort()` pattern), `canGuardSeeKid >= 2` without `.toInt()`, and missing `drawKidHp` call in `jumpThroughMirror`. Validation passes (no compile errors on Windows).

**Verification pending:** `gradle test --no-daemon` and `gradle test layer1ReplayRegression --rerun-tasks --no-daemon` on the Pi. Expected improvements: `original_level5_shadow_into_wall` should no longer crash (doPickup wired), timer-dependent divergences (guard respawn, feather fall, RNG drift from timer events) should resolve or shift later.

## Module 14: Replay Runner

### 2026-04-15 — Step 14b.3: Real-trace regression closure

**Mode:** Code | **Outcome:** Escalated — two targeted fixes applied, replay regression still blocked at the Module 15 boundary
**Contract changes:** None.

Reran `gradle test layer1ReplayRegression --rerun-tasks --no-daemon` against the real Kotlin trace producer. Applied the two targeted fixes allowed by Phase 14b: the trace runner now resets singleton lifecycle state before each manifest replay, and replay trace production enables a C-like current-room buffer mode where in-frame `curr_room_tiles[]`/`curr_room_modif[]` mutations are preserved across same-room `getRoomAddress()` calls and synced back to `level.fg[]`/`level.bg[]` on room changes. The room-buffer behavior is gated to replay production so focused unit tests keep their existing fixture reload semantics.

Focused verification passed with `gradle test --tests com.sdlpop.replay.ReplayRunnerTest --no-daemon`. The full command runs the ordinary Kotlin suite successfully, then fails in the dedicated `layer1ReplayRegression` task. The fixes eliminated the systematic stale `drawn_room=16` first-frame contamination and produced one exact replay match (`original_level12_xpos_glitch`), but the 13-trace workflow still does not close:

- `basic_movement`: frame 270, field `random_seed`, expected `1431214705`, actual `2786909894`, actual trace `SDLPoP-kotlin/build/oracle/layer1-regression/workflow/real-kotlin/basic_movement.trace`.
- `demo_suave_prince_level11`: frame 29, field `Kid.frame`, expected `16`, actual `1`, actual trace `SDLPoP-kotlin/build/oracle/layer1-regression/workflow/real-kotlin/demo_suave_prince_level11.trace`.
- `falling`: frame 26, field `random_seed`, expected `2017826505`, actual `2448654334`, actual trace `SDLPoP-kotlin/build/oracle/layer1-regression/workflow/real-kotlin/falling.trace`.
- `falling_through_floor_pr274`: frame 0, field `curr_room_modif[17]`, expected `6`, actual `4`, actual trace `SDLPoP-kotlin/build/oracle/layer1-regression/workflow/real-kotlin/falling_through_floor_pr274.trace`.
- `grab_bug_pr288`: frame 11, field `curr_room_tiles[2]`, expected `0`, actual `47`, actual trace `SDLPoP-kotlin/build/oracle/layer1-regression/workflow/real-kotlin/grab_bug_pr288.trace`.
- `grab_bug_pr289`: frame 16, field `Kid.frame`, expected `91`, actual `102`, actual trace `SDLPoP-kotlin/build/oracle/layer1-regression/workflow/real-kotlin/grab_bug_pr289.trace`.
- `original_level2_falling_into_wall`: frame 67, field `guardhp_curr`, expected `0`, actual `3`, actual trace `SDLPoP-kotlin/build/oracle/layer1-regression/workflow/real-kotlin/original_level2_falling_into_wall.trace`.
- `original_level5_shadow_into_wall`: frames 0-47 match, then trace generation throws `NotImplementedError: do_pickup (seg003)` while producing frame 48; expected frame count is 175 and actual partial frame count is 48, actual trace `SDLPoP-kotlin/build/oracle/layer1-regression/workflow/real-kotlin/original_level5_shadow_into_wall.trace`.

Per Phase 14b acceptance, no third targeted fix was attempted. The current stop condition is the unimplemented `seg003::do_pickup` path plus remaining lifecycle/game-loop divergences, which likely belongs to the Module 15 game-loop/seg003 boundary rather than more Module 14 replay plumbing.

### 2026-04-15 — Step 14b.2: Minimal lifecycle shim

**Mode:** Code | **Outcome:** Complete — headless non-rendering frame lifecycle shim implemented
**Contract changes:** None.

Extended the Kotlin replay frame-driver boundary to include the deterministic non-rendering lifecycle calls pinned in Step 14b.1: `check_can_guard_see_kid`, `bump_into_opponent`, `check_knock`, `check_sword_vs_sword`, `do_delta_hp`, `check_the_end`, and `show_time`. The Guard subframe now saves through `saveshad()` to match C `play_guard_frame()` rather than the broader `saveshad_and_opp()` helper.

Added `HeadlessFrameLifecycle` in the replay package for the minimal `seg000`/`seg003`/`seg008` behavior needed before trace serialization, including room-link refresh, animated-tile startup for room transitions, loose-floor room-entry handling, HP delta application, guard visibility, bump/knock handling, and timer decrement/text-state semantics. The shim remains headless and does not import SDL, rendering surfaces, audio playback, menus, Android, or full game-loop ownership.

Focused replay tests now pin the expanded call order, Kid subframe bump/knock boundaries, C-equivalent Guard save behavior, and `show_time()` timer decrement/rollover effects. Verification: `gradle test --no-daemon` passed. The exploratory full command `gradle test layer1ReplayRegression --rerun-tasks --no-daemon` still fails and leaves closure to Step 14b.3; remaining divergences are triage-ready, including `basic_movement` frame 0 `curr_room_modif[10]` expected `3` actual `2`, most traces at frame 0 `drawn_room` expected replay-specific rooms but actual `16`, `original_level5_shadow_into_wall` frame 0 `Guard.frame` expected `15` actual `166`, and `demo_suave_prince_level11` frame 29 `Kid.frame` expected `16` actual `1`.

### 2026-04-15 — Step 14b.1: Lifecycle audit and contract pinning

**Mode:** Docs | **Outcome:** Complete — minimum headless frame lifecycle contract pinned
**Contract changes:** DEVPLAN.md, DECISIONS.md — Phase 14b implementation boundary now explicitly includes deterministic timer/frame lifecycle state and excludes SDL/render/audio/menu/Android behavior.

Audited the C validate/replay path against the current Kotlin replay runner. In C, `start_replay()` loads replay data and resets `curr_tick`; replay input is consumed inside `seg006.c::control_kid()` during the Kid subframe; `seg000.c::play_frame()` then completes the non-rendering frame sequence, calls `show_time()`, and writes the trace through `dump_frame_state()`. The reference frame 0 therefore captures post-frame timer state, which explains the systematic one-tick `rem_tick` mismatch in the current Kotlin traces: `ReplayRunner.writeLayer1Trace()` serializes immediately after the narrower `Layer1FrameDriver` without the `show_time()` timer side effect.

The audit also pinned the likely guard-frame surface for Step 14b.2. The current Kotlin driver omits C `check_can_guard_see_kid()` before Kid processing, `bump_into_opponent()` and `check_knock()` inside the Kid subframe, `check_sword_vs_sword()`, `do_delta_hp()`, and `check_the_end()` after sword hurt checks, and it saves the Guard with `saveshad_and_opp()` while C `play_guard_frame()` uses `saveshad()`. Those calls are deterministic lifecycle/game-state behavior from `seg000`/`seg003`, not platform/render/audio work.

No source code was changed in this audit step. Verification was by source inspection only; no Gradle run was needed because the action pinned the contract for the next implementation step.

### 2026-04-15 — Step 14a.4 escalation bypass

**Mode:** Governance | **Outcome:** Bypassed — continue into Phase 14b
**Contract changes:** None.

Human/orchestrator authorization accepted the Step 14a.4 escalation as triage input rather than a stop condition. D-13 is now marked Bypassed, DEVPLAN no longer lists the escalation as an active blocker, and Phase 14b remains the authorized path for the minimal non-rendering timer/game-loop lifecycle reconciliation needed to address the known replay divergences.

### 2026-04-15 — Step 14a.4: Real trace producer workflow

**Mode:** Code | **Outcome:** Escalated — real trace generation wired, regression still diverges after two targeted fix attempts
**Contract changes:** `Layer1RegressionHarness` now defaults to a real `.P1R` replay producer; `layer1ReplayRegression` writes real Kotlin traces under `build/oracle/layer1-regression/workflow/real-kotlin`; README updated to describe the real producer boundary

Implemented replay savestate restoration from embedded `.P1R` quicksave data, including level data, characters, mobs, trobs, collision arrays, timer/control state, RNG state, and current-room buffer loading. Replaced the Phase 13a manifest-copy producer with `ReplayRunner.writeLayer1Trace()`, which restores each replay, installs the replay input hook, drives `Layer1FrameDriver`, and serializes frames with `StateTraceFormat.serializeFrameBytes()`.

Two targeted fixes were applied during validation: `Seg002.gotoOtherRoom()` now guards `Char.room == 0` before Kotlin array indexing, matching the reference build's tolerated offscreen-room path, and savestate restore alignment was corrected by reading the duplicated `prev_collision_row` storage as it appears in the replay savestate. The ordinary unit suite passes under the requested command after isolating test classes from shared singleton state.

The forced verification command `gradle test layer1ReplayRegression --rerun-tasks` still fails in the dedicated regression task. The remaining primary divergence is systematic at frame 0: `rem_tick` is one tick higher in Kotlin than the C reference for 11/13 traces, for example `basic_movement` expected `657` and actual `658`. Two traces first diverge at `Guard.frame`: `falling_through_floor_pr274` expected `171` actual `166`, and `sword_and_level_transition` expected `163` actual `166`. Actual trace artifacts were written to `SDLPoP-kotlin/build/oracle/layer1-regression/workflow/real-kotlin/`.

Per Phase 14a acceptance, no third targeted fix was attempted. Continuing likely requires a human/orchestrator decision on whether Module 14 may expand the narrow frame driver to include timer/game-loop lifecycle behavior from Layer 2 or whether the regression boundary should shift to Module 15.

### 2026-04-15 — Step 14a.3: Layer 1 frame driver

**Mode:** Code | **Outcome:** Complete — translated Layer 1 frame orchestration covered by focused replay tests
**Contract changes:** Added `Layer1FrameDriver` in `com.sdlpop.replay` as the narrow replay-runner frame tick; no SDL, Android, file I/O, rendering, or audio dependency was added to `com.sdlpop.game`

Added `Layer1FrameDriver` with `playFrame()`, `playKidFrame()`, and `playGuardFrame()` orchestration that calls the already translated Layer 1 entry points in SDLPoP frame order: mobs/trobs, skeleton checks, Kid frame processing, Guard frame processing, sword hurt checks, room exit, and guard fallout. The driver lives in the replay package and uses injected `Layer1FrameHooks` for tests, keeping platform/render/audio behavior outside the game package.

Added replay-runner tests that verify the top-level call order, the Kid room-processing pipeline order, and a focused real replay slice where `Seg006.playKid()` consumes installed `.P1R` replay input through `ExternalStubs.doReplayMove()` across two deterministic ticks. Tightened replay test cleanup so shared `GameState` singleton defaults do not leak into unrelated suites.

Verification: `gradle test --tests com.sdlpop.replay.ReplayRunnerTest` passed; full `gradle test` passed in `SDLPoP-kotlin`.

### 2026-04-15 — Step 14a.2: Replay input hooks

**Mode:** Code | **Outcome:** Complete — replay move consumption and compatibility behavior covered by tests
**Contract changes:** `ExternalStubs.doReplayMove` can now be wired to a replay byte consumer; `ReplayRunner.initializeReplayState()` now seeds `savedRandomSeed` as the C replay loader does

Translated the narrow `do_replay_move()` behavior needed by Track A into `ReplayRunner`: an installed replay move hook decodes each packed `.P1R` move byte into signed x/y controls, shift state, and special move bits, applies validate-mode seek/skipping state on tick 0, restores the saved RNG seed, advances `currTick` only while `currentLevel == nextLevel`, handles restart/effect-end special moves, and marks replay completion at the tick limit.

Fixed the seg007 loose-floor RNG compatibility branch for old replay files: when replaying with `gDeprecationNumber < 2`, `looseShake()` now skips the extra DOS-compatibility RNG cycle, matching the C `g_deprecation_number` guard. Added focused tests for move decoding, `ExternalStubs.doReplayMove` tick advancement, validate-mode seek state, end-of-replay handling, shift suppression while dead, and both old/current replay RNG paths.

Verification: `gradle test` passed in `SDLPoP-kotlin`.

### 2026-04-15 — Step 14a.1: Replay manifest and initialization

**Mode:** Code | **Outcome:** Complete — replay source resolution and metadata initialization covered by tests
**Contract changes:** `Layer1ReplayTrace` now carries a `replayFile` source path; `GameState` now carries replay format/version/deprecation metadata needed by replay playback

Mapped each of the 13 Layer 1 regression manifest entries to its source `.P1R` file under `SDLPoP/replays` or `SDLPoP/doc/replays-testcases`, preserving the existing `.trace` manifest and adding source replay validation. Added `ReplayRunner.loadManifestReplay()` to resolve manifest entries through `P1RParser`, plus `ReplayRunner.initializeReplayState()` to seed replay mode, `startLevel`, `randomSeed`, `numReplayTicks`, `currTick`, seek/skipping state, and format/version/deprecation fields on `GameState`.

Added focused tests for manifest-to-replay path resolution, parsing every manifest replay source, and start-level/seed/tick/format/deprecation plumbing into `GameState`. This step intentionally does not decode move bytes or call frame logic; those remain Step 14a.2 and Step 14a.3 respectively.

Verification: `gradle test --tests com.sdlpop.oracle.Layer1RegressionHarnessTest --tests com.sdlpop.replay.ReplayRunnerTest --tests com.sdlpop.replay.P1RParserTest` passed; full `gradle test` passed in `SDLPoP-kotlin`.

### 2026-04-15 — Phase 14a Plan: Kotlin replay playback and trace producer

**Mode:** Discuss | **Outcome:** Phase planned
**Contract changes:** DEVPLAN.md, ARCHITECTURE.md, DECISIONS.md — status propagation and phase plan only; no interface or behavior contract changes

Planned Phase 14a as the Build-regime path from the Phase 13a trace-copy harness to real Kotlin trace generation. The phase will resolve each manifest replay to its `.P1R` input, initialize replay state from `P1RParser`, translate replay move consumption into the existing `ExternalStubs.doReplayMove` hook, add a narrow deterministic Layer 1 frame driver, and plug the resulting producer into `Layer1RegressionHarness`.

Phase 14a is split into four steps:
- **14a.1** Map manifest ids to `.P1R` files, parse replay metadata, seed `GameState` replay fields, and test path/start-level/seed/tick initialization.
- **14a.2** Translate replay input consumption for `doReplayMove`, including per-tick control decoding, replay tick advancement, end handling, skipping/seek state, and deprecation-sensitive behavior.
- **14a.3** Add a minimal Layer 1 frame driver that invokes translated deterministic logic in SDLPoP order while keeping platform, rendering, audio, and broader Layer 2 lifecycle out of scope.
- **14a.4** Replace the copy producer with real Kotlin trace output, update the regression workflow/README boundary, and verify with `gradle test layer1ReplayRegression --rerun-tasks`.

Planning decision recorded in `DECISIONS.md` D-12. No source code changed in this iteration.

## Module 13: Layer 1 Integration Test

### 2026-04-15 — Phase 13a Approval

**Mode:** Review | **Outcome:** Reviewed and approved
**Contract changes:** DEVPLAN.md, DECISIONS.md — approval status only; no interface or behavior contract changes

Human approval recorded for Phase 13a after the completed review and applied should-fix. Module 13 remains closed, and Module 14 remains the next pending phase-plan target for Kotlin replay playback through the translated game loop.

### 2026-04-15 — Phase 13a Complete

**Mode:** Complete | **Outcome:** Phase 13a closed; Module 13 complete
**Contract changes:** DEVPLAN.md, ARCHITECTURE.md — status propagation only; no interface or behavior contract changes

Phase 13a delivered the Layer 1 replay-regression harness: `TRACE_FORMAT.md`-compatible parsing/comparison, `GameState` snapshot serialization to the 310-byte trace-frame layout, a manifest covering all 13 C reference traces, and a Gradle workflow that writes generated Kotlin traces under build output and reports triage-ready divergences.

Learning review: no new DEVPLAN gotchas promoted. The phase had one review should-fix around stable reruns and build-output path normalization, but it was resolved in review without repeated failures or broader process impact.

Contract scan: no game-logic, Layer 1, SDL, Android, or replay-format contract changes across Phase 13a. Module 14 still owns real `.P1R` replay playback through the translated game loop and replacement of the current trace-copy producer hook.

Verification: fresh `gradle test layer1ReplayRegression --rerun-tasks` passed in `SDLPoP-kotlin`.

### 2026-04-15 — Phase 13a Review

**Mode:** Review | **Outcome:** Complete — one should-fix applied
**Contract changes:** None

Reviewed the Phase 13a oracle code against `TRACE_FORMAT.md`, the Layer 1 integration-test contract, and the phase acceptance criteria. The trace parser/comparator and `GameState` serializer cover the 310-byte frame layout, little-endian multi-byte values, and nested character/trob/mob byte fields. The regression manifest enumerates all 13 C reference traces and keeps generated Kotlin traces under Gradle build output. The workflow remains test infrastructure only; Layer 1 game logic has no new SDL, Android, or I/O coupling, and Module 14 still owns real `.P1R` playback through the translated game loop.

Review findings:
- Must fix: none
- Should fix: normalize actual trace paths before enforcing the build-output boundary, and make the manifest-copy test producer replace generated traces so forced/rerun workflow executions are stable
- Optional: none applied

Verification: `gradle test layer1ReplayRegression --rerun-tasks` passed in `SDLPoP-kotlin`.

### 2026-04-15 — Step 13a.3: Regression harness workflow

**Mode:** Code | **Outcome:** Complete — Layer 1 workflow task passes (550 total tests; 1 tagged workflow test)
**Contract changes:** None

Added the manifest-driven Layer 1 replay-regression harness in `com.sdlpop.oracle`. `Layer1RegressionManifest` enumerates all 13 C reference traces, verifies the manifest matches the trace directory, and `Layer1RegressionHarness` writes Kotlin trace artifacts under a caller-provided build-output directory before comparing them with the existing 310-byte state trace oracle. Divergence output now includes replay id, frame index, frame number, field name, byte offset, expected value, actual value, and both trace paths for triage.

Added the `layer1ReplayRegression` Gradle task with `layer1-regression` tagged test coverage. The workflow test currently uses a producer hook that copies the reference trace, proving manifest enumeration, build-output artifact placement, parser/comparator wiring, and match reporting without pretending Module 14 exists. Documented the boundary in `SDLPoP-kotlin/README.md`: Module 14 owns feeding `.P1R` inputs through the translated game loop and replacing the hook with real Kotlin trace output.

Verification: `gradle test layer1ReplayRegression` passed in `SDLPoP-kotlin` (550 tests in the main suite, plus the tagged workflow task).

### 2026-04-15 — Step 13a.2: State snapshot writer

**Mode:** Code | **Outcome:** Complete — 2 new oracle tests pass (546 total)
**Contract changes:** None

Implemented the Kotlin state snapshot writer in `StateTraceFormat`: `serializeFrameBytes` now writes `GameState` into the exact 310-byte trace-frame layout used by the C `dump_frame_state()` instrumentation, including frame number, `Kid`/`Guard`/`Char`, core scalar state, room tile buffers, trobs, mobs, and RNG state. The writer uses little-endian multi-byte encoding and explicit byte truncation/sign preservation for the translated C `byte`, `sbyte`, `word`, `short`, and `dword` fields.

Added focused oracle tests that pin the serialized offsets through the existing field metadata, including unsigned byte truncation, signed byte/short interpretation, unsigned 16-bit words, final mob offsets, and RNG serialization. The tests reset only the trace-owned `GameState` fields they mutate so singleton defaults used by unrelated suites remain intact.

Verification: `gradle test` passed in `SDLPoP-kotlin` (546 tests, 0 failures).

### 2026-04-15 — Step 13a.1: Trace oracle foundation

**Mode:** Code | **Outcome:** Complete — 4 new oracle tests pass (544 total)
**Contract changes:** None

Implemented the Kotlin trace-oracle foundation in `com.sdlpop.oracle`: `StateTraceFormat` now defines the 310-byte frame size, leaf field metadata for every byte range in `TRACE_FORMAT.md`, parsers for byte arrays and trace files, and comparison helpers that report the first divergent frame, byte offset, field name, expected value, and actual value.

The field metadata names nested character, trob, and mob values directly (`Kid.curr_seq`, `trobs[0].type`, `mobs[13].row`) so replay failures can point to a useful state field instead of a raw offset. Tests cover full-layout metadata coverage, little-endian multi-byte reads, signed byte/short interpretation, first-divergence reporting, and parsing the existing `basic_movement.trace` C reference trace.

Verification: `gradle test` passed in `SDLPoP-kotlin` (544 tests, 0 failures).

### 2026-04-15 — Phase 13a Plan: Layer 1 Replay Regression Harness

**Mode:** Discuss | **Outcome:** Phase planned
**Contract changes:** DEVPLAN.md, ARCHITECTURE.md — status propagation and phase plan only; no interface or behavior contract changes

Planned Phase 13a as the Build-regime bridge from focused Layer 1 unit tests to replay-based regression validation. The phase will create Kotlin trace parsing/comparison support, serialize `GameState` snapshots into the 310-byte oracle format, and wire a manifest-driven workflow around the 13 existing SDLPoP reference traces.

Phase 13a is split into three steps:
- **13a.1** Implement the trace oracle foundation: parse 310-byte frames, attach field metadata, compare frames/traces, and report first divergence details.
- **13a.2** Implement the Kotlin state snapshot writer for `Kid`, `Guard`, `Char`, scalar state, room buffers, trobs, mobs, and RNG using the exact `TRACE_FORMAT.md` layout.
- **13a.3** Add the regression manifest/workflow for all 13 reference traces, generated Kotlin trace artifacts under build output, comparison execution, and triage-ready failure reporting while documenting any remaining Module 14 replay-runner boundary.

Acceptance policy: byte-identical trace layout is required, and true game-logic divergences get no more than two targeted fix attempts before escalation with replay, frame, field, and suspected module details. Planning decision recorded in `DECISIONS.md` D-9. No source code changed in this iteration.

### 2026-04-15 — Module 12 Human Approval; Module 13 Unblocked

**Mode:** Governance | **Outcome:** Module 13 ready to plan
**Contract changes:** DEVPLAN.md, ARCHITECTURE.md — status propagation only; no interface or behavior contract changes

Human audit gate for Module 12 is approved. Phase 12a and Phase 12b review outcomes remain accepted with no must-fix or should-fix findings, and the last recorded fresh verification is `gradle test` passing in `SDLPoP-kotlin` with 540 tests and 0 failures on 2026-04-15.

Next autonomous action: phase-plan Module 13. The worker should define the Layer 1 full-regression harness, replay trace generation path, comparison workflow, acceptance criteria, and failure-escalation policy before implementation begins.
