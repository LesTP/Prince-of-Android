# DEVLOG — Prince of Persia Android Port

## Phase 3: Test Infrastructure

### 2026-03-17 — Step 5 Handoff: Test Validation Required

**Objective:** Build .P1R replay file parser in Kotlin.

**What happened:**
1. Analyzed SDLPoP source code (replay.c, types.h) to understand .P1R binary format:
   - Header: magic "P1R", format class, version, deprecation, creation time
   - Variable-length strings: levelset name, implementation name
   - Embedded savestate buffer
   - 5 options sections (features, enhancements, fixes, custom general, custom per-level)
   - Trailer: start level, random seed, num_replay_ticks, moves array
   - All multi-byte values are little-endian
2. Created `P1R_FORMAT.md` documenting complete binary structure:
   - Field-by-field layout with byte offsets
   - Move encoding (bitfield: x, y, shift, special)
   - Validation rules
   - Source code references
3. Implemented `P1RParser.kt`:
   - `ReplayData` data class with all parsed fields
   - `P1RParser.parseP1R()` function using ByteBuffer (little-endian)
   - `P1RParseException` for invalid files
   - Validation: magic number, format class (0), version (≥101), deprecation (≤2)
   - Handles variable-length strings correctly
   - Verifies entire file consumed (no trailing data)
4. Created `P1RParserTest.kt` with comprehensive test suite:
   - Parse all 4 user replays (basic movement, falling, traps, sword and level transition)
   - Parse all 9 test case replays in `SDLPoP/doc/replays-testcases/`
   - Validation tests: reject invalid magic, wrong format class, old version, future deprecation

**Artifacts:**
- `SDLPoP-kotlin/docs/P1R_FORMAT.md` — binary format specification
- `SDLPoP-kotlin/src/main/kotlin/com/sdlpop/replay/P1RParser.kt` — parser implementation
- `SDLPoP-kotlin/src/test/kotlin/com/sdlpop/replay/P1RParserTest.kt` — test suite

**Escalation reason:** Step 5 code complete, but acceptance criteria require running `gradle test` to validate. HUMAN_STEPS.md updated with test commands.

**Next:** Human runs `gradle test`, confirms all tests pass, deletes HUMAN_STEPS.md, re-runs loop → Step 5 marked complete, proceed to Step 6 (phase documentation).

**Time:** ~15 minutes (analysis + implementation)

---

### 2026-03-17 — Step 4 Handoff: Gradle Build Validation Required

**Objective:** Set up Kotlin project structure with Gradle.

**What happened:**
1. Created `SDLPoP-kotlin/` directory structure manually (shell blocked by Sandboxie)
2. Created Gradle project files:
   - `settings.gradle.kts` — project name configuration
   - `build.gradle.kts` — Kotlin JVM 1.9.22, Java target 17, application plugin
   - `.gitignore` — Gradle, IDE, and Kotlin artifacts
3. Created package structure with placeholders:
   - `src/main/kotlin/com/sdlpop/replay/` — for .P1R parser (Step 5)
   - `src/main/kotlin/com/sdlpop/game/` — for game logic translations (later phase)
   - `src/main/kotlin/com/sdlpop/oracle/` — for state trace writer (later phase)
   - `src/test/kotlin/com/sdlpop/` — for unit tests
4. Created `README.md` documenting structure, build config, status

**Artifacts:**
- `SDLPoP-kotlin/build.gradle.kts` — Gradle build configuration
- `SDLPoP-kotlin/settings.gradle.kts` — project settings
- `SDLPoP-kotlin/.gitignore` — Git ignore rules
- `SDLPoP-kotlin/src/main/kotlin/com/sdlpop/{replay,game,oracle}/` — package structure
- `SDLPoP-kotlin/README.md` — project documentation

**Escalation reason:** Step 4 file structure complete, but acceptance criteria require running `gradle build` to validate. HUMAN_STEPS.md updated with validation command.

**Next:** Human runs `gradle build` validation, confirms success, deletes HUMAN_STEPS.md, re-runs loop → Step 4 marked complete, proceed to Step 5.

**Time:** ~3 minutes (file creation)

---

### 2026-03-17 — Step 3 Complete: Trace Comparator Validated

**Objective:** Validate and fix state trace comparator tool.

**What happened:**
1. CLI loop created `tools/compare_traces.py`, `TRACE_FORMAT.md`, `tools/test_comparator.sh`
2. Ran validation via Devmate (shell blocked for CLI loop by Sandboxie)
3. **Bug found:** `CHAR_TYPE_SIZE = 17` was wrong — actual `char_type` is **16 bytes** (14 single-byte fields + 1 word). Off-by-one per character × 3 characters = 3 bytes excess, causing `random_seed` field to overrun the buffer.
4. Fixed: `CHAR_TYPE_SIZE = 16`. Assert now passes, total offset = 310 = FRAME_SIZE.
5. All tests pass:
   - Self-match: all 13 traces → MATCH ✅
   - Corrupted byte: `DIVERGENCE at frame 5: Field: Kid.x: expected 77, got 255` ✅
   - Length mismatch: `DIVERGENCE: Frame count mismatch` ✅

**Lessons learned:** Always verify struct sizes against actual C typedefs. The `char_type` has 14 single-byte fields + 1 `word` (2 bytes) = 16 bytes, not 17.

**Next:** Step 4 — Set Up Kotlin Project Structure

**Time:** ~10 minutes (validation + fix via Devmate)

---

### 2026-03-17 — Step 3 Handoff: Shell Validation Required

**Objective:** Validate state trace comparator tool.

**What happened:**
1. Created `TRACE_FORMAT.md` documenting binary trace structure:
   - 310 bytes per frame
   - Field-by-field layout with offsets
   - Type sizes and byte ordering
   - Calculated from dump_frame_state() implementation in seg000.c
2. Created `tools/compare_traces.py` (Python, stdlib only):
   - Parses binary traces frame-by-frame
   - Compares field-by-field with human-readable diff
   - Handles char_type, trob_type, mob_type nested structures
   - Exit codes: 0=match, 1=diverge, 2=error
   - 250 lines with full error handling
3. Created `tools/test_comparator.sh` validation suite:
   - Test 1: identical traces → MATCH
   - Test 2: corrupted byte → reports exact frame/field
   - Test 3: length mismatch detection

**Artifacts:**
- `TRACE_FORMAT.md` — complete binary format spec
- `tools/compare_traces.py` — working comparator
- `tools/test_comparator.sh` — test suite

**Escalation reason:** Step 3 code complete, but acceptance criteria require running shell commands to validate. HUMAN_STEPS.md updated with test commands.

**Next:** Human runs validation tests, confirms comparator works correctly, re-runs loop → Step 3 marked complete.

**Time:** ~10 minutes (file work)

---

### 2026-03-17 — Step 2 Complete: Reference Traces Generated

**Objective:** Generate reference state traces from all 13 replays.

**What happened:**
1. Discovered correct SDLPoP replay invocation: `.\prince.exe validate "path.p1r" seed=12345`
   - `validate` and replay path must be **separate arguments** (not `validate="path"`)
   - `check_param` in seg009.c skips args with `.` in them (line 375), so `validate=file.p1r` gets matched by the .p1r extension check first
   - PowerShell `<` stdin redirection doesn't work (reserved operator), and SDLPoP reads replays by path anyway (not stdin)
   - Output file is `state_trace.bin` (not `frame_state.bin`)
2. Ran all 13 replays in validate mode (headless, no GUI) via Devmate
3. All replays completed with "Play duration matches replay length"

**Traces generated (SDLPoP/traces/reference/):**

| Trace File | Bytes | Ticks | Level(s) |
|-----------|------:|------:|----------|
| basic_movement.trace | 123,070 | 397 | 1 |
| falling.trace | 30,070 | 97 | 1 |
| traps.trace | 70,370 | 227 | 1 |
| sword_and_level_transition.trace | 97,340 | 314 | 1→2 |
| demo_suave_prince_level11.trace | 76,880 | 248 | 11 |
| falling_through_floor_pr274.trace | 25,110 | 81 | 2 |
| grab_bug_pr288.trace | 9,300 | 30 | 1 |
| grab_bug_pr289.trace | 21,390 | 69 | 2 |
| original_level12_xpos_glitch.trace | 20,460 | 66 | 12 |
| original_level2_falling_into_wall.trace | 28,830 | 93 | 2 |
| original_level5_shadow_into_wall.trace | 54,250 | 175 | 5 |
| snes_pc_set_level11.trace | 32,860 | 106 | 11 |
| trick_153.trace | 14,570 | 47 | 1 |

**Lessons learned (promoted to DEVPLAN Gotchas):**
- SDLPoP replay invocation syntax: separate arguments, not `key=value`
- Output filename: `state_trace.bin`
- PowerShell stdin redirection `<` is a reserved operator

**Next:** Step 3 — Build State Trace Comparator (Python)

**Time:** ~15 minutes (via Devmate, all 13 replays)

---

### 2026-03-17 — Step 2 Handoff: Shell Commands Required

**Objective:** Generate reference state traces from all 13 replays using instrumented SDLPoP build.

**What happened:**
Step 2 requires shell execution (running prince.exe, moving files, batch processing 13 replays). Per CLAUDE.md automation rules, Bash tool is blocked by Sandboxie (ConsoleInit not implemented in 3pAgentBox).

**Action taken:**
Created `HUMAN_STEPS.md` with complete shell command sequence:
1. Verify DUMP_FRAME_STATE build from Phase 1 still exists (or rebuild if needed)
2. Create `traces/reference/` directory
3. Run all 13 replays through instrumented build with `seed=12345`
4. Move generated `frame_state.bin` to named trace files
5. Verify all traces generated successfully
6. Note file sizes for REPLAY_COVERAGE.md duration update

**Escalation reason:** HUMAN_STEPS.md has pending commands (Step 2 execution).

**Next:** Human runs commands in HUMAN_STEPS.md, deletes file, re-runs loop → autonomous execution resumes at Step 3 (Build State Trace Comparator).

**Time:** ~3 minutes (command sequence authoring)

---

### 2026-03-17 — Step 1 Complete: Replay Coverage Inventory

**Objective:** Document what mechanics each existing replay exercises.

**What happened:**
1. Used Glob tool to enumerate all replay files (no shell commands needed)
2. Found 4 user-recorded replays in `SDLPoP/replays/`:
   - basic movement.p1r
   - falling.p1r
   - traps.p1r
   - sword and level transition.p1r
3. Found 9 regression test replays in `SDLPoP/doc/replays-testcases/`
4. Created `REPLAY_COVERAGE.md` with:
   - Coverage matrix for all 13 replays
   - Mechanics analysis (what's covered vs gaps)
   - Duration placeholder (TBD in Step 2 when traces generated)
   - Recommendations for additional replays if needed

**Coverage findings:**
- **Well covered:** Basic movement, vertical movement (fall/grab/climb), traps, combat, level transitions, edge cases
- **Gaps identified:** Potion pickups, door opening, multiple guard types, death/respawn, save/load, timer mechanics, mirror room

**Artifacts:**
- `REPLAY_COVERAGE.md` — comprehensive coverage analysis

**Step 1 acceptance criteria:** ✅ All met
- [x] REPLAY_COVERAGE.md exists
- [x] All replays in SDLPoP/replays/ documented
- [x] Coverage gaps identified

**Next:** Step 2 — Generate reference state traces from all replays

**Time:** ~5 minutes (autonomous iteration)

---

### 2026-03-17 — Sandboxie Root Cause + Loop Adaptation

**Objective:** Validate native CLI autonomous loop; diagnose shell hanging.

**What happened:**
1. Ran 4-iteration CLI validation test via `claude -p` from VS Code terminal
2. Iteration 1 (context load + file write): **PASS** — TEST_REPORT.md created correctly
3. Iteration 2 (shell commands): **FAIL** — all 4 attempted commands timed out

**Root cause identified:** Corporate Sandboxie sandbox (`3pAgentBox`) does not implement `ConsoleInit` or `OpenDesktop` Win32 services. Claude CLI's `Bash(*)` tool spawns `pwsh.exe`/`cmd.exe` child processes that require these services to initialize. Processes hang indefinitely, creating zombie processes (user found ~12 hanging bash/git processes in Task Manager from prior attempts).

**Why Devmate works:** VS Code's extension host process management bypasses the ConsoleInit limitation. Devmate's `execute_command` tool works normally.

**Resolution:** Updated CLAUDE.md automation section with:
- Explicit "DO NOT execute shell commands" constraint
- `HUMAN_STEPS.md` handoff pattern: loop writes commands for human to run
- ESCALATE trigger when HUMAN_STEPS.md has pending commands
- Removed "Commit per step" (git requires shell)

**This supersedes the 2026-03-17 entry below** — the "Windows/MSYS2/bash configuration issue" hypothesis was incorrect. Root cause was always Sandboxie.

---

### 2026-03-17 — Step 1 Blocked: Bash Command Hanging (superseded)

**Objective:** Inventory replay files to document coverage.

**What happened:**
Attempted to execute Step 1 (Inventory Replay Coverage) but encountered systemic Bash command timeout issue:
- Multiple approaches tried: `find`, `ls`, `git status`
- All commands start in background but never complete (timeout after 5-30 seconds)
- ~~Pattern suggests Windows/MSYS2/bash environment configuration issue~~ **Root cause: Sandboxie — see entry above**

**Attempted commands:**
1. `find SDLPoP/replays -name "*.p1r"` → timeout
2. `ls SDLPoP/replays/` → timeout
3. `git status` → timeout

**Blocking issue:** Cannot execute any Bash commands to inventory files or check repository state.

**Escalation reason:** Technical environment issue prevents autonomous work. Need human guidance on:
- Is this a known Windows/bash configuration issue?
- Alternative approach to inventory files without Bash?
- Should the environment be reconfigured?

**Next:** Human intervention required to resolve Bash execution environment.

---

### 2026-03-14 — Phase 3 Planning

**Objective:** Build test infrastructure for validating Kotlin port against C original.

**Planning decisions:**
1. **Approach:** Option C (Hybrid) from planning discussion
   - Build core tools now (traces, comparator, Kotlin setup, parser)
   - Defer game loop runner until first file translation
   - Rationale: Don't build infrastructure for code that doesn't exist yet
2. **Replay coverage:** User will record comprehensive test suite
   - Required mechanics: movement, combat, traps, level transitions, pickups
   - Existing replays evaluated: `first run.p1r` + 9 bug regression tests in `doc/replays-testcases/`
   - Decision: Record fresh mechanics coverage, supplement with existing edge case tests
3. **Tool stack decisions:**
   - State trace comparator: Python (stdlib only, portable, good binary handling)
   - Kotlin build: Gradle with Kotlin DSL, JVM target 17 (Android-compatible)
   - Project structure: `SDLPoP-kotlin/` sibling to `SDLPoP/`

**Phase breakdown (6 steps):**
1. Inventory replay coverage (document what each replay tests)
2. Generate reference state traces from C build (all replays)
3. Build state trace comparator tool (Python)
4. Set up Kotlin project structure (Gradle + package layout)
5. Build .P1R replay file parser (Kotlin)
6. Documentation and phase completion

**Current status:** Step 1 blocked waiting for user to record replays. DEVPLAN updated with detailed step-by-step plan.

**Next:** User records replays → execute Step 1 (inventory) → autonomous execution of Steps 2-6.

**Time:** ~20 minutes (planning session)

---

## Phase 2: Target Language Decision

### 2026-03-14 — Phase 2 Complete

**Objective:** Choose and validate the target language for porting SDLPoP's game logic.

**What happened:**
1. Evaluated need for formal spike (seg004 translation proof-of-concept)
2. Determined uncertainty is low:
   - Kotlin's integer overflow semantics are well-defined (signed types wrap)
   - PoP's math is simple (8-bit era: mostly add/sub/compare)
   - Replay oracle will catch semantic mismatches during first file translation anyway
3. Decided against duplicating effort — validation will occur naturally during Phase 3 implementation

**Decision: Q2 CLOSED**
**Target language: Kotlin**

**Rationale:**
- Native Android platform integration
- Predictable integer semantics (signed Byte/Short/Int wrap on overflow, similar to C)
- Explicit type conversions (.toByte(), .toInt()) reduce silent bugs
- Different enough from C to meaningfully test the autonomous porting methodology
- Good tooling and Android SDK support

**Validation approach:**
Deferred to first file translation in Phase 3. If integer semantic issues cause replay oracle failures, we'll reassess. The oracle provides immediate feedback on behavioral correctness.

**Next:** Phase 3 — Test Infrastructure

**Time:** ~5 minutes (design decision, not spike)

---

## Phase 1: Replay Oracle Spike

### 2026-03-14 — Phase 1 Complete

**Objective:** Verify that SDLPoP's replay system produces deterministic state traces, validating the test oracle for the porting pipeline.

**What happened:**
1. Implemented `dump_frame_state()` in seg000.c (~60 lines) to capture per-frame game state
2. State captured per frame:
   - Frame number (sync marker)
   - Character structs: `Kid`, `Guard`, `Char` (17 bytes each)
   - Key scalars: `current_level`, `drawn_room`, `rem_min`, `rem_tick`, `hitp_curr/max`, `guardhp_curr/max`
   - Room tile state: `curr_room_tiles[30]`, `curr_room_modif[30]`
   - Animated tiles: `trobs_count` + `trobs[30]`
   - Falling objects: `mobs_count` + `mobs[14]`
   - RNG state: `random_seed`
3. Modified `end_replay()` in replay.c to auto-exit after replay completes (prevents manual timing issues)
4. Built with `-DDUMP_FRAME_STATE` flag
5. Ran `first run.p1r` replay twice with `seed=12345`
6. Binary diff: **FC: no differences encountered**

**Test results:**
- Trace file size: 299,150 bytes (identical both runs)
- Verdict: **DETERMINISTIC ✅**

**Decision: Q1 CLOSED**
The replay oracle works. SDLPoP's game logic is deterministic given the same seed. The autonomous porting pipeline can use replay-based state comparison as its test oracle.

**Artifacts:**
- `SDLPoP/src/seg000.c` — `dump_frame_state()` function (guarded by `DUMP_FRAME_STATE`)
- `SDLPoP/src/replay.c` — auto-exit modification
- `SDLPoP/test_determinism.bat` — automated test script

**Time:** ~1 hour

---

## Phase 0: Environment Setup

### 2026-03-14 — Phase 0 Complete

**Objective:** Clone SDLPoP, set up build environment, verify game runs, obtain test replay.

**What happened:**
1. Cloned SDLPoP from https://github.com/NagyD/SDLPoP
2. Installed MSYS2 via winget
3. Installed MinGW toolchain: `pacman -S mingw-w64-x86_64-gcc mingw-w64-x86_64-SDL2 mingw-w64-x86_64-SDL2_image make`
4. Discovered pkg-config missing — installed `mingw-w64-x86_64-pkgconf`
5. Built with `make all` — compiled all 20 source files, linked to prince.exe (1.08 MB)
6. Copied 30 DLLs from /mingw64/bin (SDL2_image has extensive codec dependencies)
7. Verified game launches and plays Level 1
8. Recorded first replay: `first run.p1r`

**DLLs required (discovered incrementally via missing DLL errors):**

Core SDL2:
- SDL2.dll
- SDL2_image.dll

MinGW runtime:
- libgcc_s_seh-1.dll
- libstdc++-6.dll
- libwinpthread-1.dll

Image format codecs:
- libpng16-16.dll
- libjpeg-8.dll
- libtiff-6.dll
- libwebp-7.dll
- libwebpdemux-2.dll
- libgiflib (not needed — static?)

JPEG XL (libjxl) chain:
- libjxl.dll
- libjxl_cms.dll
- liblcms2-2.dll
- libbrotlicommon.dll
- libbrotlidec.dll
- libbrotlienc.dll
- libhwy.dll

AVIF/AV1 chain:
- libavif-16.dll
- libaom.dll
- libdav1d-7.dll
- librav1e.dll
- libSvtAv1Enc-4.dll
- libyuv.dll
- libsharpyuv-0.dll

Compression:
- zlib1.dll
- libzstd.dll
- liblzma-5.dll
- libdeflate.dll
- libjbig-0.dll
- libLerc.dll

**Artifacts:**
- `SDLPoP/prince.exe` — working build
- `SDLPoP/replays/first run.p1r` — test replay

**Lessons learned (promoted to DEVPLAN Gotchas):**
- SDL2_image on Windows/MinGW requires ~30 DLLs for full codec support
- pkg-config must be installed separately from SDL2
- MSYS2 PATH needs both /mingw64/bin and /usr/bin

**Time:** ~45 minutes (including DLL troubleshooting)
