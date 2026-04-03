---
name: step-done
description: Step completion — log, commit, prep for next step
---

Current step is complete. Tests have already passed.

Parse $ARGUMENTS for --amend or --commit (default is --commit).

1. Present a summary of changes made in this step
2. Update DEVLOG with a structured entry:
   - Header: `### Step [N]: [short title]`
   - Structured fields: Mode, Outcome, Contract changes
   - Followed by prose: what was done, decisions, issues
3. If this step modified a shared contract, list affected documents in the Contract changes field
4. If --commit: create a new commit with a descriptive message
   If --amend: amend the previous commit, updating its message to
   include this step's changes
5. Update DEVPLAN Current Status to reflect new state
6. Briefly state what the next step is according to the DEVPLAN

Under autonomous execution: proceed to exit after this. The next iteration picks up the next step.
