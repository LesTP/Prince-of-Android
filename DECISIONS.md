# DECISIONS — Prince of Persia Android Port

D-1: External dependency handling for seg006
Date: 2026-04-04 | Status: Open
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
Date: 2026-04-10 | Status: Open
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
Date: 2026-04-15 | Status: Open
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
Date: 2026-04-15 | Status: Open
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
Date: 2026-04-15 | Status: Open
Priority: Important
Decision: Start Module 14 with one Build-regime phase that replaces the Phase 13a copy producer with real Kotlin trace generation from `.P1R` replay inputs. Keep the implementation narrowly scoped to replay manifest resolution, replay state initialization, replay move consumption, a minimal deterministic Layer 1 frame driver, and `StateTraceFormat` output.
Rationale: The Module 13 harness already proves trace comparison and artifact workflow. Module 14 should now supply real Kotlin traces without prematurely translating all of Layer 2 or introducing platform/render/audio behavior. A four-step plan keeps replay file plumbing, input decoding, frame orchestration, and workflow replacement independently testable.
Revisit if: Real replay playback cannot progress without translating broader `seg000`/`seg001`/`seg003` lifecycle code that belongs to Module 15, or if the Phase 13a harness producer contract needs to change.
