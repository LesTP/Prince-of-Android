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

## Automation
Running at Continuous autonomy. One step per loop iteration.
Decide and log — do not wait for human approval.

### Shell Constraint
**DO NOT execute shell commands.** The Bash tool will hang indefinitely
(Sandboxie blocks ConsoleInit). Use only Read, Write, and Edit tools.

When a step requires shell commands (builds, git, running executables),
write the commands to `HUMAN_STEPS.md` instead of executing them.
Format:

```
## After Iteration N: [description]
Run these commands, then re-run the loop:
\`\`\`powershell
command 1
command 2
\`\`\`
Expected result: [what to check]
```

### Each Iteration
1. Read this file and follow `@` references to load project documents
2. Read DEVPLAN's Current Status to determine the active track and module
3. Read ARCHITECTURE.md's layer contract for the active module
4. Execute the next action — exactly one of:
   - **No active phase:** Create or update DEVPLAN with step breakdown. Exit.
   - **Phase in progress:** Pick the next step from DEVPLAN. Do all
     file read/write work. If shell commands are needed, write them
     to HUMAN_STEPS.md. Update DEVLOG. Exit.
   - **All steps complete:** Log decisions to DECISIONS.md. Exit.
   - **Review fixes done:** Full doc update, contract propagation,
     DEVPLAN cleanup. Exit.
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
- HUMAN_STEPS.md has pending commands (human must run them first)
