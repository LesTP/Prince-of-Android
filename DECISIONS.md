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
