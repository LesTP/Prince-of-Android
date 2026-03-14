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
Commit per step. Decide and log — do not wait for human approval.

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
   - **No active phase:** Run /phase-plan. Create or update DEVPLAN.
     Commit. Exit.
   - **Phase in progress:** Pick the next step from DEVPLAN. Execute
     it (Discuss → Code/Debug → Verify). Update DEVLOG. Commit. Exit.
   - **All steps complete:** Run /phase-review. Log decisions to
     DECISIONS.md. Exit.
   - **Review fixes done:** Run /phase-complete. Full doc update,
     contract propagation, DEVPLAN cleanup. Commit. Exit.
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
