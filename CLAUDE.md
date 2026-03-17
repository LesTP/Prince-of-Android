# Prince of Persia Android Port

## Framework
This project follows the From Idea to Code governance framework.

## Always Loaded
- @pop_android_port_guide.md — pre-Discovery research and preparation sequence
- @GOVERNANCE.md — development process reference
- @DEVPLAN.md — current status, phase plan, cold start summary

## Load for Current Module
This project is in pre-Discovery phase. No modules defined yet.
Determine the active module from ARCHITECTURE.md's Implementation Sequence
table — first module without "Complete" status. If ARCHITECTURE.md does
not exist, we are still in Discovery/Architecture phase.

## Available Modules
<!-- Update this list during Architecture phase -->
- (none yet — pre-Discovery)

## Project-Specific Notes
- Source codebase: SDLPoP (open-source C reimplementation of Prince of Persia)
- Target: Android (touch controls)
- Primary goal: test autonomous AI porting methodology
- Secondary goal: working, playable game
- Pre-Discovery guide exists at pop_android_port_guide.md with codebase
  analysis, blocking questions, preparation sequence, and risk register

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
1. Read this file and follow `@` references to load available project documents
2. If ARCHITECTURE.md exists: determine the active module from its
   Implementation Sequence table — first module without "Complete" status.
   Load that module's ARCH_[module].md and DEVPLAN.md.
3. If ARCHITECTURE.md does not exist: we are in pre-Discovery/Discovery phase.
   Work from pop_android_port_guide.md's preparation sequence.
4. Determine current state from DEVPLAN's Current Status section
   (or from pop_android_port_guide.md's checklist if no DEVPLAN exists)
5. Execute the next action — exactly one of:
   - **No active phase:** Create or update DEVPLAN. Exit.
   - **Phase in progress:** Pick the next step from DEVPLAN. Do all
     file read/write work. If shell commands are needed, write them
     to HUMAN_STEPS.md. Update DEVLOG. Exit.
   - **All steps complete:** Log decisions to DECISIONS.md. Exit.
   - **Review fixes done:** Full doc update, contract propagation,
     DEVPLAN cleanup. Exit.
6. Output exit signal as the final two lines:
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
