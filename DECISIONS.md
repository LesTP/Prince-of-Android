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
