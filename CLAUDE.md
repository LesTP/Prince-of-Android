# Prince of Persia Android Port

## Framework
This project follows the From Idea to Code governance framework.

## Always Loaded
- @PROJECT.md — scope, constraints, success criteria
- @ARCHITECTURE.md — component map, layer contracts, implementation sequence
- @GOVERNANCE.md — development process reference
- @DEVPLAN.md — current status, cold start summary, gotchas

## Load for Current Module
Determine the active track and module from DEVPLAN's Current Status section.
For layer contracts and module dependencies, see ARCHITECTURE.md.

## Available Modules
**Track A — Game Logic Translation (current):**
- Module 6: State Model (types.h + data.h → Kotlin)
- Module 7: Sequence Table (seqtbl.c → Kotlin)
- Modules 8-12: Layer 1 game logic (seg006 → seg004 → seg005 → seg002 → seg007)
- Module 13: Layer 1 integration test
- Module 14: Replay runner

**Track B — Android Platform (after Track A):**
- Modules 15-18: Game loop, rendering, platform, audio

**Track C — Touch Controls (parallel, any time):**
- Module 4: Control prototype
- Module 19: In-game controls

## Project-Specific Notes
- Source codebase: SDLPoP (open-source C reimplementation of Prince of Persia)
- Target: Kotlin (decided Phase 2)
- Primary goal: test autonomous AI porting methodology
- Secondary goal: working, playable Android game
- Test oracle: replay-based deterministic state comparison (13 reference traces, validated Phase 1+3)

## Environment (Raspberry Pi 5)
- **Host:** Raspberry Pi 5 (16GB), Incus container `claude-code` (Debian 12 arm64, 12GB RAM, 3 CPUs)
- **Project path:** `/home/claude/workspace/PoP port/`
- **Storage:** Project on USB drive (NTFS, 466GB, mounted at /home/claude/workspace). OS on SD card (29GB).
- **Shell:** Full bash access — no Sandboxie restrictions. Can run `gradle`, `make`, `python3`, `git` directly.

### NTFS Workaround
The USB drive (NTFS) does not support execute permissions. To run compiled binaries:
1. Copy binary to `/tmp/sdlpop/`: `cp SDLPoP/prince /tmp/sdlpop/prince && chmod +x /tmp/sdlpop/prince`
2. Symlinks for data: `data/`, `replays/`, `doc/`, `SDLPoP.ini` → original locations
3. Run from `/tmp/sdlpop/`: `cd /tmp/sdlpop && SDL_VIDEODRIVER=offscreen SDL_AUDIODRIVER=dummy ./prince validate "replays/foo.p1r" seed=12345`

### Trace Generation
```bash
cd /tmp/sdlpop
SDL_VIDEODRIVER=offscreen SDL_AUDIODRIVER=dummy ./prince validate "replays/foo.p1r" seed=12345
# Output: state_trace.bin (310 bytes/frame)
```
Requires instrumented build: `make CPPFLAGS="-Wall -D_GNU_SOURCE=1 -DDUMP_FRAME_STATE -DUSE_REPLAY"` in SDLPoP/src/

### Build Commands
- **C (SDLPoP):** `cd SDLPoP/src && make -j3` (or with CPPFLAGS above for instrumented build)
- **Kotlin:** `cd SDLPoP-kotlin && gradle build` / `gradle test`
- **Trace comparison:** `python3 tools/compare_traces.py ref.trace test.trace`

## Automation
Running at Continuous autonomy. One step per loop iteration.
Decide and log — do not wait for human approval.

**Runner:** `run-iteration.sh` — runs `claude -p` per iteration, logs to `logs/loop/`.
**Orchestrator:** TG bot session invokes the runner, analyzes output, reports via Telegram.
**Slash commands:** Available in `.claude/commands/` — adapted for autonomous execution (human-wait gates removed). Use them as appropriate for the current loop state.

### Each Iteration
1. Read this file and follow `@` references to load project documents
2. Read DEVPLAN's Current Status to determine the active track and module
3. Read ARCHITECTURE.md's layer contract for the active module
4. Execute the next action — exactly one of:
   - **No active phase:** Run /phase-plan. Update DEVPLAN with step breakdown. Commit. Exit.
   - **Phase in progress:** Pick the next step from DEVPLAN. Do all
     file read/write work. Run shell commands directly (builds, git, tests). Run /step-done. Exit.
   - **All steps complete:** Run /phase-review. Log decisions to DECISIONS.md. Exit.
   - **Review fixes done:** Run /phase-complete. Full doc update, contract propagation,
     DEVPLAN cleanup. Commit. Exit.
5. Output exit signal as the final two lines:
   ```
   LOOP_SIGNAL: CONTINUE
   REASON: [one line — what was done]
   ```

### Hard Stops (exit with ESCALATE)
- 3 consecutive failures on the same problem
- Work regime shifts to Refine or Explore
- Scope needs to expand beyond the defined phase
- Contract change would affect other modules
- Phase completion (human audits before next phase)
- All modules complete
