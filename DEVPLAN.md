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

## Current Status

**Phase:** 0 — Environment Setup (COMPLETE)
**Focus:** Phase 1 — Replay Oracle Spike
**Blocked/Broken:** None

## Phase Summary

### Phase 0: Environment Setup — COMPLETE
One-line: Built SDLPoP on Windows via MSYS2/MinGW, verified gameplay, recorded first replay. See DEVLOG §Phase-0.

### Phase 1: Replay Oracle Spike (next)
See pop_android_port_guide.md §3 Q1 for full spec.

Steps:
1. [ ] Implement `dump_frame_state()` in SDLPoP (~50 lines)
2. [ ] Insert call in `play_frame()` (seg000.c ~line 863)
3. [ ] Run replay twice with `seed=N`, produce two traces
4. [ ] Binary diff traces — confirm determinism
5. [ ] Test with multiple replay types (movement, combat, traps)
6. [ ] Document findings

**Gate:** If non-deterministic, reassess project viability.
