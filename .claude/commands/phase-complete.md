---
name: phase-complete
description: Phase completion checklist — run after review issues are resolved
---

Execute the phase completion protocol:

1. Run phase-level tests and confirm they pass.
2. Read each governance doc and identify needed updates:
   - **DEVPLAN.md**: Update Current Status (Track/Module/Phase/Next). Reduce completed phase to a one-line summary with DEVLOG reference. Deduplicate — keep DEVPLAN minimal. Promote any new Gotchas (from step 3 below).
   - **DEVLOG.md**: Add phase completion entry. If DEVLOG exceeds ~500 lines, archive completed module entries to `DEVLOG_archive.md`.
   - **ARCHITECTURE.md**: Update Implementation Sequence table status. Format: "Phase N complete" after each phase, or "Complete" if this was the module's final phase.
   - **DECISIONS.md**: Close any Open decisions resolved by this phase.
   - **PROJECT.md**: Close any Open risks resolved by this phase.
3. DEVLOG learning review — scan this phase's entries for trial-and-error patterns (multiple attempts to resolve something). Extract prescriptive one-liner summaries and propose additions to DEVPLAN Gotchas.
4. Make all updates identified in steps 2–3.
5. Present summary of everything done and everything needing confirmation.

Under autonomous execution: commit, then exit with ESCALATE. Human audits before next phase begins.
