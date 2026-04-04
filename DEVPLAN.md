# DEVPLAN — Prince of Persia Android Port

## Cold Start Summary

**What this is:** Android port of SDLPoP (Prince of Persia), testing autonomous AI porting methodology.

**Key constraints:**
- SDLPoP is ~25,000 lines of C with 344 global variables (tight coupling)
- Replay system (.P1R files) provides deterministic test oracle
- Target language: Kotlin (native Android, validation via replay oracle)

**Environment:** Raspberry Pi 5 (16GB), Incus container `claude-code` (Debian 12 arm64, 12GB RAM, 3 CPUs)
**Project path:** `/home/claude/workspace/PoP port/` (NTFS USB drive, 466GB, 126GB free)
**Shell access:** Full — no Sandboxie restrictions. Claude Code can run `gradle`, `make`, `python3`, `git` directly.

**Gotchas:**
- **Kotlin integer semantics:** Signed types (Byte/Short/Int) wrap on overflow like C. Conversions are explicit (.toByte(), .toInt()). Replay oracle catches mismatches immediately.
- **Replay auto-exit:** Instrumented builds (`DUMP_FRAME_STATE`) auto-exit after replay ends. Essential for consistent trace lengths.
- **C struct sizes:** Always verify struct byte sizes against `typedef` definitions in `types.h`/`data.h`. Don't trust field counts — check each type (`byte`=1, `word`=2, `dword`=4, `short`=2). `char_type` is 16 bytes (not 17), `start_level` is `word` (2 bytes, not 4).
- **Gradle 9.x JUnit:** Add `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` to `build.gradle.kts` or test executor fails to start with "Failed to load JUnit Platform."
- **SDLPoP replay invocation (Linux):** Run from `/tmp/sdlpop/` (binary copied there for execute permission). Use `SDL_VIDEODRIVER=offscreen SDL_AUDIODRIVER=dummy ./prince validate "replays/foo.p1r" seed=12345`. Output: `state_trace.bin`.
- **SDL headless mode:** Pi has no display. Use `SDL_VIDEODRIVER=offscreen` (not `dummy` — dummy hangs). Requires `xvfb` package installed but `offscreen` driver doesn't need it.
- **NTFS execute permissions:** USB drive is NTFS — `chmod +x` is silently ignored. Workaround: copy binaries to `/tmp/sdlpop/` with symlinks to data/replays/doc/SDLPoP.ini.
- **Line endings:** Source files from Windows have CRLF. Run `dos2unix *.c *.h` in SDLPoP/src/ after any file transfer. Grep/ripgrep fail silently on CRLF files.
- **Reference traces:** Regenerated all 13 on ARM64 Pi (2026-04-03). Sizes match expected frame counts. Determinism verified.

## Current Status

**Track:** A — Game Logic Translation (Build regime, autonomous)
**Module:** 7 — Sequence Table (seqtbl.c → Kotlin data) — **IN PROGRESS**
**Phase:** 7a — Planning complete. Ready for step execution.
**Blocked/Broken:** None.

**Migration from Windows (2026-04-03) — COMPLETE.** See DEVLOG §Pi Migration.

**Tracks overview:**
- **Track A (Game Logic):** C→Kotlin translation of ~7,200 lines, validated by replay oracle. **Full shell access on Pi — true autonomous mode.** Next.
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

### Module 6: State Model — COMPLETE
One-line: Translated types.h + data.h → Kotlin (4 files, 27 tests pass). See DEVLOG §Module 6.

### Module 7: Sequence Table — IN PROGRESS

**Regime:** Build (autonomous)
**Source:** `seqtbl.c` (1,228 lines) — pure data, zero game logic
**Depends on:** Module 6 (State Model — types and enums)

**What seqtbl.c is:** A byte array encoding animation sequences as a mini virtual machine. Each sequence is a stream of bytes: frame IDs (0-228), instruction opcodes (0xF1-0xFF for dx, dy, jump, action, sound, etc.), and operands. Game logic reads this table via `Char.curr_seq` pointer to drive character animation.

**Key structures:**
- `seqtbl[]` — ~2,300-byte array of packed sequence data
- `seqtbl_offsets[]` — lookup table mapping `seqids` (1-115) to byte offsets into `seqtbl[]`
- Labels are computed as cumulative offsets from `SEQTBL_BASE` (0x196E)

**Translation approach:** The C file uses macros (`act()`, `jmp()`, `dx()`, `dy()`, `snd()`, `set_fall()`) and computed label offsets to build the byte array at compile time. In Kotlin, we can either:
- (A) Reproduce the macro expansion → build the byte array programmatically with helper functions
- (B) Pre-compute the final byte values and store as a literal array

**Decision: Option A** — helper functions match the C structure, are readable, and are self-verifying (offset mismatches cause test failures).

#### Step breakdown

**Step 7a — Prerequisite enums** (Enums.kt additions):
- `SeqtblInstructions` — 15 opcodes (SEQ_DX=0xFB through SEQ_END_LEVEL=0xF1)
- `SeqtblSounds` — 5 entries (SND_SILENT through SND_LEVEL)
- `FrameIds` — ~230 frame ID constants (frame_0 through frame_228)
- `SeqIds` — ~100 sequence ID constants (seq_1 through seq_114)
- Tests: verify enum values match C originals for spot-checked entries

**Step 7b — Sequence table data** (new file `SequenceTable.kt`):
- `SEQTBL_BASE` constant (0x196E)
- Builder helpers: `act()`, `jmp()`, `dx()`, `dy()`, `snd()`, `setFall()`, `jmpIfFeather()`
- `seqtbl` IntArray built using helpers (matching C byte-for-byte)
- `seqtblOffsets` IntArray (mapping seq IDs to offsets)
- Tests: verify array size, spot-check known offsets, verify specific sequence byte patterns

**Step 7c — Validation and review:**
- Cross-reference: verify `seqtblOffsets` entries match C `seqtbl_offsets[]` values
- Verify total byte count matches C array size
- Build passes (`gradle build`)
