# DEVPLAN — Prince of Persia Android Port

## Cold Start Summary

**What this is:** Android port of SDLPoP (Prince of Persia), testing autonomous AI porting methodology.

**Key constraints:**
- SDLPoP is ~25,000 lines of C with 344 global variables (tight coupling)
- Replay system (.P1R files) provides deterministic test oracle
- Target language: Kotlin (native Android, validation via replay oracle)

**Gotchas:**
- **Sandboxie blocks shell in CLI loop:** Corporate Sandboxie (`3pAgentBox`) doesn't implement `ConsoleInit`/`OpenDesktop`. Claude CLI's `Bash(*)` tool hangs when spawning `pwsh.exe`/`cmd.exe`. **Workaround:** CLI loop handles read/write only; shell tasks (builds, git, traces) run manually or via Devmate. Devmate's `execute_command` works because VS Code's extension host bypasses the limitation.
- **SDLPoP replay invocation:** Use `.\prince.exe validate "path\to\replay.p1r" seed=12345` — `validate` and the replay path must be **separate arguments**. `validate="path"` syntax fails because the `.p1r` extension check in seg000.c fires first and treats the whole string as a filename. PowerShell's `<` stdin redirection also doesn't work (reserved operator). Output trace file is `state_trace.bin` (not `frame_state.bin`).
- **SDL2_image DLL Hell (Windows):** SDL2_image pulls ~30 DLLs. Copy all from `/mingw64/bin/` or use `ldd` to find missing ones.
- **MinGW pkg-config:** Install separately: `pacman -S mingw-w64-x86_64-pkgconf`
- **MSYS2 PATH:** Use `PATH='/mingw64/bin:/usr/bin:$PATH'` — make is in /usr/bin, gcc in /mingw64/bin
- **Kotlin integer semantics:** Signed types (Byte/Short/Int) wrap on overflow like C. Conversions are explicit (.toByte(), .toInt()). PoP's 8-bit-era math is mostly add/sub/compare, so semantic mismatches are unlikely, but the replay oracle will catch them immediately if they occur.
- **Replay auto-exit:** Instrumented builds (`DUMP_FRAME_STATE`) auto-exit after replay ends. Essential for consistent trace lengths.
- **Windows fc.exe:** In PowerShell, use `C:\Windows\System32\fc.exe /b` for binary compare — bare `fc` is aliased to `Format-Custom`.
- **C struct sizes:** Always verify struct byte sizes against `typedef` definitions in `types.h`/`data.h`. Don't trust field counts — check each type (`byte`=1, `word`=2, `dword`=4, `short`=2). `char_type` is 16 bytes (not 17), `start_level` is `word` (2 bytes, not 4).
- **Gradle 9.x JUnit:** Add `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` to `build.gradle.kts` or test executor fails to start with "Failed to load JUnit Platform."
- **Chocolatey corporate proxy:** Use `choco search jdk --limit-output` to find approved packages. `temurin17` unavailable — use `openjdk17`. Gradle not in approved list — manual install from zip to `C:\tools\gradle-9.4.0\`.

## Current Status

**Track:** A — Game Logic Translation (Build regime, autonomous)
**Focus:** Not started — next step is State Model (types.h + data.h → Kotlin)
**Blocked/Broken:** None — Phase 5 (formal docs) in progress, Track A can begin immediately after.

**Tracks overview:**
- **Track A (Game Logic):** C→Kotlin translation of ~7,200 lines, validated by replay oracle. Runs within current environment (file read/write + manual shell). **Next.**
- **Track B (Android Platform):** Rendering, platform, audio, game loop. Requires Android Studio. **After Track A.**
- **Track C (Touch Controls):** Gesture prototype + playtesting. Requires Android Studio + phone. **Parallel, any time.**

## Phase Summary

### Phase 0: Environment Setup — COMPLETE
One-line: Built SDLPoP on Windows via MSYS2/MinGW, verified gameplay, recorded first replay. See DEVLOG §Phase-0.

### Phase 1: Replay Oracle Spike — COMPLETE
One-line: Verified replay determinism (FC: no differences). Q1 closed — oracle works. See DEVLOG §Phase-1.

### Phase 2: Target Language Decision — COMPLETE
One-line: Decision: **Kotlin**. Rationale: native Android, predictable integer semantics (signed types wrap on overflow), explicit conversions. Validation deferred to first file translation in Phase 3 — replay oracle will catch semantic mismatches immediately. Q2 closed. See DEVLOG §Phase-2.

### Phase 3: Test Infrastructure — COMPLETE
One-line: Built replay oracle toolchain: 13 reference traces, Python comparator, Kotlin P1R parser (Gradle, 9/9 tests pass). See DEVLOG §Phase-3.

**Deferred:** Game loop replay runner — build alongside first file translation.
