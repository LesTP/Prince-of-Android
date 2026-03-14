# DEVPLAN — Prince of Persia Android Port

## Cold Start Summary

**What this is:** Android port of SDLPoP (Prince of Persia), testing autonomous AI porting methodology.

**Key constraints:**
- SDLPoP is ~25,000 lines of C with 344 global variables (tight coupling)
- Replay system (.P1R files) provides deterministic test oracle
- Target language TBD (likely Kotlin) — requires Phase 2 spike

**Gotchas:**
- **SDL2_image DLL Hell (Windows):** SDL2_image pulls ~30 DLLs. Copy all from `/mingw64/bin/` or use `ldd` to find missing ones.
- **MinGW pkg-config:** Install separately: `pacman -S mingw-w64-x86_64-pkgconf`
- **MSYS2 PATH:** Use `PATH='/mingw64/bin:/usr/bin:$PATH'` — make is in /usr/bin, gcc in /mingw64/bin
- **Replay auto-exit:** Instrumented builds (`DUMP_FRAME_STATE`) auto-exit after replay ends. Essential for consistent trace lengths.
- **Windows fc.exe:** In PowerShell, use `C:\Windows\System32\fc.exe /b` for binary compare — bare `fc` is aliased to `Format-Custom`.

## Current Status

**Phase:** 1 — Replay Oracle Spike (COMPLETE)
**Focus:** Phase 2 — Target Language Decision
**Blocked/Broken:** None

## Phase Summary

### Phase 0: Environment Setup — COMPLETE
One-line: Built SDLPoP on Windows via MSYS2/MinGW, verified gameplay, recorded first replay. See DEVLOG §Phase-0.

### Phase 1: Replay Oracle Spike — COMPLETE
One-line: Verified replay determinism (FC: no differences). Q1 closed — oracle works. See DEVLOG §Phase-1.

### Phase 2: Target Language Decision (next)
See pop_android_port_guide.md §3 Q2 for full spec.

Steps:
1. [ ] Choose candidate language (recommendation: Kotlin)
2. [ ] Translate seg004 (collision detection, 621 lines) as proof-of-concept
3. [ ] Wire to replay oracle and compare state output against C original
4. [ ] Document findings

**Gate:** If behavior doesn't match, investigate integer semantics or try another language.
