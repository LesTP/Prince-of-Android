# DECISIONS — Prince of Persia Android Port

D-22: Sync Seg008 modifier preprocessing into level buffers
Date: 2026-04-30 | Status: Open
Priority: Important
Decision: `Seg008.loadAlterMod()` writes modifier preprocessing results to both `GameState.currRoomModif[]` and the corresponding `GameState.level.bg[]` slot for the currently loaded room.
Rationale: In C, `curr_room_modif` is a pointer into `level.bg`, so `load_alter_mod()` mutates persistent level data by writing through the current-room pointer. Kotlin models room buffers as copied arrays loaded by `ExternalStubs.loadRoomAddress()`. Without explicit sync, later room reloads would discard gate, loose-floor, potion, wall-connection, fake-wall, and torch preprocessing.
Revisit if: Room buffers are later refactored to be true views into `LevelType.fg/bg`, or if Android rendering owns a separate immutable render-preprocessing cache.

D-21: Phase 16c boundary for seg008 pure render logic
Date: 2026-04-30 | Status: Open
Priority: Important
Decision: Start Phase 16c as a Build-regime translation of the pure `seg008.c` state/render-table preparation slice, split into four steps: render-state scaffold and tile helpers; room/adjacent-tile loading plus modifier preprocessing; object-table and dirty-rect bookkeeping; and pure draw/redraw orchestration behind no-op or test-capturable render-submission hooks.
Rationale: `seg008.c` crosses the rendering Build/Refine boundary. The 30 pure-state functions can be translated and unit-tested without Android Canvas or SDL, but several orchestration functions call render-submission functions that belong to Phase 16d. Keeping those downstream calls behind hooks lets Phase 16c validate traversal, redraw counters, room loading, tile resolution, modifier preprocessing, object ordering, and dirty-rect state without pulling pixel drawing or render-table append semantics into the phase.
Revisit if: A 16c function cannot be made meaningful without translating Phase 16d table append functions, or if render-table models must change in a way that affects the Android backend contract.

D-20: Phase 16b human approval
Date: 2026-04-30 | Status: Closed
Priority: Important
Decision: Mark Phase 16b as reviewed and human-approved. Module 16 stays in progress, and Phase 16c is unblocked for planning.
Rationale: The Phase 16b review is closed, the one should-fix was applied, the phase-complete entry recorded acceptance for the asset loading pipeline, and the latest verification remains `gradle test` passing from the repo root with 586 tests.
Revisit if: Phase 16c planning discovers a missing asset-pipeline contract dependency or a required change to the Phase 16b decode/catalog boundary before render-table work can begin.

D-19: Collapse C metadata/alloc DAT loaders into one Kotlin entry point
Date: 2026-04-30 | Status: Closed
Priority: Nice-to-have
Decision: `AssetRepository` exposes a single `loadFromOpenDatsAlloc()` (and a destination-area variant) instead of mirroring SDLPoP's `load_from_opendats_metadata` / `load_from_opendats_alloc` split. The phase 16b review removed the misnamed delegating `loadFromOpenDatsMetadata` wrapper that returned full bytes despite its name.
Rationale: SDLPoP's metadata function returns a `FILE*` plus locator metadata so callers can stream bytes themselves; `_alloc` then layers byte allocation on top. Kotlin's asset boundary models `LoadedAssetResource` as bytes-already-read because `AssetByteSource` cannot expose a streaming handle for Android `AssetManager` or filesystem-backed test sources. A "metadata only" function on top of that abstraction would have to read or duplicate the bytes anyway, so keeping it as a wrapper added zero value while the name suggested the C contract.
Revisit if: A future caller genuinely needs a streaming or metadata-only DAT lookup (for example, on-demand sprite paging or a per-resource size/checksum query without reading bytes).

D-18: Decoded asset image boundary for Android Bitmap creation
Date: 2026-04-30 | Status: Closed
Priority: Important
Decision: Keep DAT decode, palette-to-ARGB conversion, PNG metadata, and chtab sprite catalog loading in the JVM-testable `com.sdlpop.assets` module, and keep actual `android.graphics.Bitmap` creation in the Android app module via `BitmapImageDecoder`.
Rationale: Phase 16b needs objective JVM tests for DAT/PNG dimensions and palette conversion on the Pi, while `BitmapFactory` and `Bitmap.createBitmap()` belong to the Android runtime. Splitting decoded image data from Bitmap instantiation preserves the Android contract without making the core module depend on Android SDK classes or untestable platform APIs.
Revisit if: The renderer needs Android-specific image metadata in the shared asset catalog, or Android-side bitmap allocation must be pooled/cached behind a different lifecycle boundary.

D-17: DAT resource payload boundary for asset codecs
Date: 2026-04-30 | Status: Closed
Priority: Important
Decision: Treat the DAT table `offset` as pointing at a one-byte checksum and expose resource payload bytes starting at `offset + 1` for Kotlin asset parsing and decompression.
Rationale: `seg009.c::load_from_opendats_metadata()` reads the checksum byte first, then `load_from_opendats_alloc()` reads `size` bytes from the following position. Parsing from the raw table offset corrupts image headers, as seen with `GUARD.DAT` resource 751 (`0x66` checksum before the real `height=5,width=6,flags=0xB400` header). Matching the C payload boundary lets the pure decompression tests validate real DAT image resources.
Revisit if: Step 16b.3 implements archive loading against a DAT variant whose resource table size includes the checksum byte differently.

D-16: Module 15 completion and Phase 15b/15c review
Date: 2026-04-20 | Status: Closed
Priority: Important
Decision: Accept Module 15 as complete at 8/13 traces. Phase review found one should-fix (dead HeadlessFrameLifecycle methods superseded by Seg003) — applied. No must-fix issues. Remaining 5 divergences are root-caused and documented: 1 tile modifier init, 1 control dispatch, 2 grab detection (collision bounds edge case), 1 level restart lifecycle (outside headless scope). Module 16 entry criteria documented.
Rationale: The 8 matching traces validate the game logic translation across movement, falling, combat, level transitions, guard AI, traps, RNG determinism, and room transitions. The 5 remaining failures have clear root causes that are either outside headless replay scope (level restart) or will be resolved when Module 16 loads real sprite assets (grab detection). Continuing to debug within the headless shim has diminishing returns.
Revisit if: Module 16 sprite loading does not resolve the grab detection divergences, or if the `demo_suave_prince_level11` control dispatch bug indicates a Layer 1 translation error rather than a demo-level lifecycle issue.

D-15: Phase 14b escalation boundary
Date: 2026-04-15 | Status: Closed (resolved by Module 15)
Priority: Important
Decision: Escalate Phase 14b after the two allowed targeted replay fixes instead of translating additional `seg003` behavior or applying a third divergence fix inside Module 14.
Rationale: Step 14b.3 improved the real trace workflow by resetting singleton state between manifest replays and adding replay-only current-room buffer preservation, but `layer1ReplayRegression` still fails. The clearest hard blocker is `original_level5_shadow_into_wall`, where frames 0-47 match and frame 48 enters `ExternalStubs.doPickup`, an unimplemented `seg003`/Layer 2 path. Additional failures remain in RNG, Kid animation, tile mutation, and guard HP fields. Continuing would expand Module 14 beyond replay plumbing and the pinned non-rendering shim into broader Module 15 game-loop/seg003 behavior.
Revisit if: Human/orchestrator authorizes continuing Module 14 past the two-fix limit, or if Module 15 explicitly absorbs the remaining `seg003` lifecycle and pickup boundary.

D-14: Phase 14b headless lifecycle boundary
Date: 2026-04-15 | Status: Closed (resolved by Module 15)
Priority: Important
Decision: Implement only the deterministic, non-rendering frame lifecycle needed for replay trace equivalence in Phase 14b: C-equivalent trace timing after `show_time()`, timer state updates, and the missing `seg000`/`seg003` frame calls that mutate game state before the trace snapshot.
Rationale: Step 14b.1 found that the C reference writes `state_trace.bin` from `seg000.c::play_frame()` after `show_time()` runs, while the Kotlin runner currently serializes immediately after the narrower Layer 1 frame driver. This accounts for the systematic frame 0 `rem_tick` mismatch. The two guard-frame first divergences also sit in the gap between the current driver and C's non-rendering frame sequence: `check_can_guard_see_kid`, `bump_into_opponent`, `check_knock`, `check_sword_vs_sword`, `do_delta_hp`, `check_the_end`, and the narrower `saveshad()` guard save path.
Out of scope: SDL event processing, rendering/drawing, audio playback, menus, cutscenes, Android integration, general `seg000`/`seg001`/`seg003` translation, and visual side effects from `show_time()`.
Revisit if: The 13-trace regression still diverges after the pinned lifecycle shim and two targeted fixes, or if a required behavior cannot be isolated from platform/render/audio responsibilities.

D-13: Step 14a.4 escalation boundary
Date: 2026-04-15 | Status: Bypassed
Priority: Important
Decision: Escalate Step 14a.4 instead of attempting a third replay-divergence fix or expanding the frame driver into broader game-loop/timer lifecycle code.
Rationale: Real Kotlin trace production is wired and produces triage-ready artifacts, but `layer1ReplayRegression` still fails after two targeted fixes. The remaining first-frame divergences are systematic timer/frame-lifecycle mismatches (`rem_tick` off by one for 11/13 traces, with two guard-frame divergences), which likely touch the Module 14/15 boundary around non-rendering `seg000`/`seg003` behavior. Phase 14a acceptance explicitly limits true divergence fixing to two targeted attempts before escalation.
Bypass: Human/orchestrator authorization on 2026-04-15 accepted the escalation and directed the loop to continue into Phase 14b, where the missing non-rendering timer/game-loop lifecycle slice is explicitly in scope. The bypass does not close the divergence; it converts the escalation from a stop condition into the motivating input for Step 14b.1.
Revisit if: Phase 14b expands beyond the minimal lifecycle slice, or if the remaining divergence needs reassignment to Module 15.

D-1: External dependency handling for seg006
Date: 2026-04-04 | Status: Closed (all stubs wired by Module 15)
Priority: Important
Decision: Use stub functions (throwing NotImplementedError) for cross-segment calls from seg006. Functions from seg000, seg002, seg003, seg005, seg007, seg008, and replay.c that seg006 calls will be defined as callable references in a stubs file. As each module is translated (Modules 9-12), stubs are replaced with real implementations.
Rationale: seg006 cannot compile or be tested without these ~30 external function references. Interfaces add unnecessary abstraction for code that will be replaced module-by-module. Simple stubs keep the translation mechanical and close to the C structure.
Revisit if: Integration testing (Module 13) reveals that stub boundaries cause behavioral divergence.

D-2: Feature flag translation strategy
Date: 2026-04-04 | Status: Open
Priority: Important
Decision: Translate `#ifdef FIX_*` / `#ifdef USE_*` conditional code as runtime checks against `GameState.fixes` and `GameState.custom` option objects, matching the SDLPoP custom options system. The default/reference build has all fixes enabled.
Rationale: The reference traces were generated with fixes enabled. Runtime checks preserve the ability to toggle fixes (useful for debugging divergences). Compile-time exclusion would lose this flexibility.
Revisit if: Performance profiling shows runtime checks in hot paths are measurable.

D-3: Phase 11c review outcome for Module 11 seg002
Date: 2026-04-10 | Status: Closed
Priority: Important
Decision: Accept Phase 11c (sword combat detection, skeleton wake, auto-move sequencing, and shadow autocontrol) as meeting the Layer 1 contract and phase acceptance criteria. The Kotlin implementations in `Seg002.kt` match the reviewed C control flow for `hurt_by_sword`, `check_sword_hurt`, `check_sword_hurting`, `check_hurting`, `check_skel`, `do_auto_moves`, and the level 4/5/6/12 shadow handlers. No new platform or I/O coupling was introduced.
Rationale: Review compared the translated Kotlin against `SDLPoP/src/seg002.c` and checked existing test artifacts showing `Seg002Test` passed with 142 tests, 0 failures, and 0 errors on 2026-04-09. External wiring for `autocontrolOpponent`, `leaveGuard`, and `doAutoMoves` resolves to `Seg002` instead of placeholder stubs, which is consistent with the module contract.
Revisit if: A fresh Gradle test run in this environment still fails with `Failed to load native library 'libnative-platform.so' for Linux aarch64`, or Module 13 replay integration exposes a behavioral divergence in these paths.

D-4: Phase 12a boundary for Module 12 seg007
Date: 2026-04-10 | Status: Closed
Priority: Important
Decision: Start Module 12 with a phase centered on trob bookkeeping, redraw helpers, and the trap/button entry points already referenced by translated modules (`start_anim_spike`, `trigger_button`, `make_loose_fall`, `start_chompers`, `is_spike_harmful`). Defer loose-floor mob simulation and object-table/drawing code to later phases.
Rationale: `seg007.c` mixes three concerns: animated tile state machines, trigger/gate plumbing, and falling-object simulation. Front-loading the shared trob pipeline and seg006-facing trap hooks reduces active `ExternalStubs` early and creates a tighter first validation slice. The deferred mob code touches room transitions, object tables, and rendering-oriented redraw paths, which is a broader surface better handled after the tile/trigger core is in place.
Revisit if: Translating `animate_leveldoor` or trigger plumbing proves inseparable from the deferred loose-floor/mob functions, or if replay integration shows the trap hooks must land atomically with mob behavior.

D-5: Phase 12a review outcome for Module 12 seg007
Date: 2026-04-11 | Status: Closed
Priority: Important
Decision: Accept Phase 12a (trob core, redraw helpers, trap/button animation, gate animation, and level-door animation) as meeting the Layer 1 contract and phase acceptance criteria. No must-fix or should-fix findings were identified.
Rationale: Review compared the translated Kotlin slice against `SDLPoP/src/seg007.c`, confirmed the completed functions remain deterministic game logic with no platform or I/O coupling, and verified that completed seg007 trap hooks in `ExternalStubs` delegate to real `Seg007` implementations. A fresh `gradle test` run passed with 519 tests and 0 failures on 2026-04-11.
Revisit if: Later seg007 loose-floor/mob phases or Module 13 replay integration expose a behavioral divergence in the Phase 12a paths.

D-6: Phase 12b boundary for Module 12 seg007
Date: 2026-04-15 | Status: Closed
Priority: Important
Decision: Finish Module 12 with a Build-regime phase centered on the deferred loose-floor and falling-object subsystem. Split the work into loose-floor fall initiation, mob physics/collision, and draw/object-table bookkeeping. Leave the unused SDL-only `sub_9A8E` helper unported unless a live reference appears.
Rationale: The remaining `seg007.c` functions form one coupled subsystem around loose floor removal, falling mob state, Kid collision, redraw bookkeeping, and object-table writes. Keeping this as one phase avoids an artificial module split, while the three-step breakdown keeps each iteration testable and keeps rendering-adjacent object-table writes behind the core simulation behavior.
Revisit if: `draw_mob`/`add_mob_to_objtable` require a rendering contract change beyond existing state/object-table bookkeeping, or Module 13 replay integration exposes a need to include the unused SDL blit helper.

D-7: Phase 12b review outcome for Module 12 seg007
Date: 2026-04-15 | Status: Closed
Priority: Important
Decision: Accept Phase 12b (loose-floor fall initiation, falling-object simulation, Kid collision, redraw bookkeeping, and mob object-table writes) as meeting the Layer 1 contract and phase acceptance criteria. No must-fix or should-fix findings were identified.
Rationale: Review compared the translated Kotlin slice against `SDLPoP/src/seg007.c`, confirmed the completed functions remain deterministic game logic with no platform or I/O coupling, and verified that the unused SDL-only `sub_9A8E` helper has no live Kotlin reference. A fresh `gradle test` run passed with 540 tests and 0 failures on 2026-04-15.
Revisit if: Module 13 replay integration exposes a behavioral divergence in the Phase 12b loose-floor, mob, or object-table paths.

D-8: Module 12 human approval and Module 13 start gate
Date: 2026-04-15 | Status: Closed
Priority: Important
Decision: Accept Module 12 as human-approved and unblock Module 13 planning.
Rationale: Module 12 has completed both planned phases, both reviews found no must-fix or should-fix issues, and the latest recorded fresh Kotlin verification passed with 540 tests and 0 failures. The previous loop escalation was solely the explicit human-audit gate before Module 13, which is now cleared.
Revisit if: Module 13 phase planning finds a missing Layer 1 contract dependency that should have been completed in Module 12.

D-9: Phase 13a boundary for Layer 1 replay regression harness
Date: 2026-04-15 | Status: Closed
Priority: Important
Decision: Start Module 13 with one Build-regime phase that establishes the Kotlin trace oracle foundation, writes 310-byte `GameState` snapshots, and wires the 13-reference-trace regression workflow. Keep full replay playback/game-loop translation outside this phase except for documenting the Module 14 boundary.
Rationale: The translated Layer 1 files now compile and have focused unit tests, but replay-level validation needs a deterministic harness before game-loop work begins. Splitting trace comparison, snapshot serialization, and regression orchestration into three steps creates testable progress without expanding scope into Layer 2 or Android platform behavior.
Revisit if: Building the regression workflow proves impossible without translating `seg000` frame orchestration or replay playback semantics that belong to Module 14.

D-10: Phase 13a review outcome for Layer 1 replay regression harness
Date: 2026-04-15 | Status: Closed
Priority: Important
Decision: Accept Phase 13a as meeting the trace-oracle and regression-harness contract after applying a review fix that normalizes build-output path checks and makes the manifest-copy workflow rerunnable with replacement of generated trace files.
Rationale: Review confirmed `StateTrace.kt` follows the 310-byte `TRACE_FORMAT.md` layout, little-endian multi-byte encoding, and nested `char_type`/`trob_type`/`mob_type` byte ordering; `Layer1RegressionManifest` enumerates all 13 reference traces; generated traces stay under build output; divergence reports include replay, frame, field, expected value, and actual value; and Module 14 remains responsible for real replay playback. A fresh forced verification run passed with `gradle test layer1ReplayRegression --rerun-tasks`.
Revisit if: Module 14 replay playback needs a different trace producer contract than `(Layer1ReplayTrace, Path) -> Path`.

D-11: Phase 13a human approval
Date: 2026-04-15 | Status: Closed
Priority: Important
Decision: Mark Phase 13a as reviewed and human-approved. Module 13 stays complete, and Module 14 remains unblocked for phase planning.
Rationale: The review outcome is closed, the one should-fix was applied, and the recorded forced verification passed with `gradle test layer1ReplayRegression --rerun-tasks`.
Revisit if: Module 14 replay playback discovers that the Phase 13a harness contract must change before real Kotlin trace production can replace the copy producer.

D-12: Phase 14a boundary for Kotlin replay runner
Date: 2026-04-15 | Status: Closed
Priority: Important
Decision: Start Module 14 with one Build-regime phase that replaces the Phase 13a copy producer with real Kotlin trace generation from `.P1R` replay inputs. Keep the implementation narrowly scoped to replay manifest resolution, replay state initialization, replay move consumption, a minimal deterministic Layer 1 frame driver, and `StateTraceFormat` output.
Rationale: The Module 13 harness already proves trace comparison and artifact workflow. Module 14 should now supply real Kotlin traces without prematurely translating all of Layer 2 or introducing platform/render/audio behavior. A four-step plan keeps replay file plumbing, input decoding, frame orchestration, and workflow replacement independently testable.
Revisit if: Real replay playback cannot progress without translating broader `seg000`/`seg001`/`seg003` lifecycle code that belongs to Module 15, or if the Phase 13a harness producer contract needs to change.
