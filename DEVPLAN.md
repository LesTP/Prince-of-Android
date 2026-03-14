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

**Phase:** 2 — Target Language Decision (COMPLETE)
**Focus:** Phase 3 — Test Infrastructure (planning)
**Blocked/Broken:** None

## Phase Summary

### Phase 0: Environment Setup — COMPLETE
One-line: Built SDLPoP on Windows via MSYS2/MinGW, verified gameplay, recorded first replay. See DEVLOG §Phase-0.

### Phase 1: Replay Oracle Spike — COMPLETE
One-line: Verified replay determinism (FC: no differences). Q1 closed — oracle works. See DEVLOG §Phase-1.

### Phase 2: Target Language Decision — COMPLETE
One-line: Decision: **Kotlin**. Rationale: native Android, predictable integer semantics (signed types wrap on overflow), explicit conversions. Validation deferred to first file translation in Phase 3 — replay oracle will catch semantic mismatches immediately. Q2 closed. See DEVLOG §Phase-2.

### Phase 3: Test Infrastructure (next)
See pop_android_port_guide.md §4 Phase 3 for full spec.

**Work Regime:** Build

**Scope:**
1. [ ] State trace comparator tool (structured diff with human-readable output)
2. [ ] Replay runner for Kotlin port (feeds recorded inputs, produces state traces)
3. [ ] Regression suite (replays covering all major mechanics + reference traces)
4. [ ] CI-style runner (optional — runs full suite, reports pass/fail)

**Output:** Working test infrastructure for validating ported code against original.
