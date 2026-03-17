# DEVPLAN — Prince of Persia Android Port

## Cold Start Summary

**What this is:** Android port of SDLPoP (Prince of Persia), testing autonomous AI porting methodology.

**Key constraints:**
- SDLPoP is ~25,000 lines of C with 344 global variables (tight coupling)
- Replay system (.P1R files) provides deterministic test oracle
- Target language: Kotlin (native Android, validation via replay oracle)

**Gotchas:**
- **SDL2_image DLL Hell (Windows):** SDL2_image pulls ~30 DLLs. Copy all from `/mingw64/bin/` or use `ldd` to find missing ones.
- **MinGW pkg-config:** Install separately: `pacman -S mingw-w64-x86_64-pkgconf`
- **MSYS2 PATH:** Use `PATH='/mingw64/bin:/usr/bin:$PATH'` — make is in /usr/bin, gcc in /mingw64/bin
- **Kotlin integer semantics:** Signed types (Byte/Short/Int) wrap on overflow like C. Conversions are explicit (.toByte(), .toInt()). PoP's 8-bit-era math is mostly add/sub/compare, so semantic mismatches are unlikely, but the replay oracle will catch them immediately if they occur.
- **Replay auto-exit:** Instrumented builds (`DUMP_FRAME_STATE`) auto-exit after replay ends. Essential for consistent trace lengths.
- **Windows fc.exe:** In PowerShell, use `C:\Windows\System32\fc.exe /b` for binary compare — bare `fc` is aliased to `Format-Custom`.

## Current Status

**Phase:** 3 — Test Infrastructure
**Focus:** Step 1 — Inventory Replay Coverage
**Blocked/Broken:** None — replay suite recorded (basic movement, falling, traps, sword + level transition) plus 9 regression replays in doc/replays-testcases/

## Phase Summary

### Phase 0: Environment Setup — COMPLETE
One-line: Built SDLPoP on Windows via MSYS2/MinGW, verified gameplay, recorded first replay. See DEVLOG §Phase-0.

### Phase 1: Replay Oracle Spike — COMPLETE
One-line: Verified replay determinism (FC: no differences). Q1 closed — oracle works. See DEVLOG §Phase-1.

### Phase 2: Target Language Decision — COMPLETE
One-line: Decision: **Kotlin**. Rationale: native Android, predictable integer semantics (signed types wrap on overflow), explicit conversions. Validation deferred to first file translation in Phase 3 — replay oracle will catch semantic mismatches immediately. Q2 closed. See DEVLOG §Phase-2.

### Phase 3: Test Infrastructure (in progress)

**Work Regime:** Build

**Approach:** Hybrid (Option C from planning discussion)
- Build core tools (traces, comparator, Kotlin setup, .P1R parser) now
- Defer game loop runner to first file translation phase
- Rationale: Don't build unused infrastructure; runner designed around actual ported code

**Prerequisites:** ✅ Met
- User recorded replay suite: `basic movement.p1r`, `falling.p1r`, `traps.p1r`, `sword and level transition.p1r`
- Additional regression replays available in `SDLPoP/doc/replays-testcases/` (9 files)

**Deliverables:**
1. Reference state traces from all test replays
2. State trace comparator tool (language-agnostic, human-readable diff)
3. Kotlin project structure with Gradle build
4. .P1R replay file parser
5. Documentation: replay coverage matrix, trace format spec

---

#### Step 1: Inventory Replay Coverage

**Goal:** Document what mechanics each existing replay exercises.

**Actions:**
- List all `.p1r` files in `SDLPoP/replays/` and `SDLPoP/doc/replays-testcases/`
- For user-recorded replays: document intended coverage (from filenames/metadata)
- Create `REPLAY_COVERAGE.md` matrix:
  ```
  | Replay File | Level(s) | Mechanics Covered | Duration (frames) |
  |-------------|----------|-------------------|-------------------|
  | first run.p1r | 1 | basic movement, jump, ... | TBD |
  ```

**Acceptance Criteria:**
- [ ] REPLAY_COVERAGE.md exists
- [ ] All replays in SDLPoP/replays/ documented
- [ ] Coverage gaps identified (if any)

**Files created:**
- `REPLAY_COVERAGE.md`

---

#### Step 2: Generate Reference State Traces

**Goal:** Run all replays through instrumented C build, capture state traces.

**Actions:**
- Verify `DUMP_FRAME_STATE` build still works from Phase 1
- Create batch script to run all replays with same seed
- For each replay:
  ```bash
  prince seed=12345 < "replays/NAME.p1r"
  mv frame_state.bin "traces/reference/NAME.trace"
  ```
- Document trace file format in `TRACE_FORMAT.md`:
  - Binary structure (per-frame layout from Phase 1 DEVLOG)
  - Byte offsets for each field
  - Total size calculation formula

**Acceptance Criteria:**
- [ ] All replays produce `.trace` files in `traces/reference/`
- [ ] TRACE_FORMAT.md documents binary structure
- [ ] Traces are deterministic (re-running same replay produces identical trace)

**Files created:**
- `traces/reference/*.trace` (one per replay)
- `TRACE_FORMAT.md`
- `generate_reference_traces.sh` or `.bat` (automation script)

---

#### Step 3: Build State Trace Comparator

**Goal:** Tool that compares two trace files and reports first divergence with human-readable output.

**Language decision:** Python (stdlib only, portable, good binary/struct support)

**Actions:**
- Create `tools/compare_traces.py`:
  - Parse two binary trace files using TRACE_FORMAT.md spec
  - Compare frame-by-frame
  - On divergence: print frame number, field name, expected vs actual values
  - Exit code: 0 if identical, 1 if divergent
- Handle edge cases:
  - Different trace lengths (report which ended early)
  - File read errors
  - Format validation

**Acceptance Criteria:**
- [ ] `tools/compare_traces.py` exists and runs
- [ ] Comparing identical traces: exits 0, prints "MATCH"
- [ ] Comparing different traces: exits 1, prints frame/field/values
- [ ] Test: comparing `reference/first_run.trace` to itself → MATCH
- [ ] Test: manually corrupt one byte → reports exact divergence

**Files created:**
- `tools/compare_traces.py`
- `tools/test_comparator.sh` (validation script)

---

#### Step 4: Set Up Kotlin Project Structure

**Goal:** Gradle-based Kotlin/JVM project ready for game logic code.

**Actions:**
- Create `SDLPoP-kotlin/` directory (sibling to `SDLPoP/`)
- Initialize Gradle project:
  ```bash
  gradle init --type kotlin-application --dsl kotlin
  ```
- Configure `build.gradle.kts`:
  - Kotlin JVM target: 17 (Android-compatible)
  - Main class: TBD (will be replay runner eventually)
  - Source sets: `src/main/kotlin`, `src/test/kotlin`
- Create placeholder package structure:
  ```
  src/main/kotlin/
    com/sdlpop/
      replay/     # .P1R parser goes here
      game/       # game logic translations go here (later)
      oracle/     # state trace writer goes here (later)
  ```
- Add `.gitignore` for Gradle build artifacts

**Acceptance Criteria:**
- [ ] `SDLPoP-kotlin/` directory exists with valid Gradle project
- [ ] `gradle build` succeeds (even with empty sources)
- [ ] Package structure created
- [ ] README.md in SDLPoP-kotlin/ documenting structure

**Files created:**
- `SDLPoP-kotlin/build.gradle.kts`
- `SDLPoP-kotlin/settings.gradle.kts`
- `SDLPoP-kotlin/src/main/kotlin/com/sdlpop/` (package dirs)
- `SDLPoP-kotlin/.gitignore`
- `SDLPoP-kotlin/README.md`

---

#### Step 5: Build .P1R Replay File Parser

**Goal:** Kotlin code that reads `.p1r` files into data structures.

**Context:** .P1R format is undocumented but used by SDLPoP. Need to reverse-engineer or find spec.

**Actions:**
- Research .P1R format:
  - Check SDLPoP source: `replay.c`, `replay.h` for file I/O code
  - Document format in `SDLPoP-kotlin/docs/P1R_FORMAT.md`
- Implement parser in Kotlin:
  - File: `src/main/kotlin/com/sdlpop/replay/P1RParser.kt`
  - Data class: `ReplayData` (header, input sequence, metadata)
  - Function: `parseP1R(path: Path): ReplayData`
- Write unit tests:
  - Parse `first run.p1r`, verify field values
  - Parse all replays, verify no parse errors

**Acceptance Criteria:**
- [ ] `P1R_FORMAT.md` documents file structure
- [ ] `P1RParser.kt` exists and compiles
- [ ] Unit test: parses `first run.p1r` successfully
- [ ] Unit test: parses all replays in `SDLPoP/replays/` without errors
- [ ] `gradle test` passes

**Files created:**
- `SDLPoP-kotlin/docs/P1R_FORMAT.md`
- `SDLPoP-kotlin/src/main/kotlin/com/sdlpop/replay/P1RParser.kt`
- `SDLPoP-kotlin/src/test/kotlin/com/sdlpop/replay/P1RParserTest.kt`

---

#### Step 6: Document Phase Completion

**Goal:** Update DEVPLAN, DEVLOG, verify all deliverables.

**Actions:**
- Run all acceptance criteria checks
- Update DEVLOG with Phase 3 summary
- Update DEVPLAN: mark Phase 3 complete, add one-line summary
- Verify file inventory matches deliverables list
- Commit: "Phase 3 complete: Test infrastructure"

**Acceptance Criteria:**
- [ ] All prior steps' acceptance criteria pass
- [ ] DEVLOG §Phase-3 entry exists
- [ ] DEVPLAN Phase 3 reduced to one-line + DEVLOG reference
- [ ] Git commit created

---

**Deferred to Later Phase:**
- Game loop replay runner (Kotlin version of SDLPoP's play_frame loop)
  - Rationale: Needs actual game logic code to test against; build alongside first file translation
  - Will be Phase 4 or bundled with first Layer 1 file port
