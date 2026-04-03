---
name: phase-complete
description: Phase completion checklist — run after review issues are resolved
---

Execute the phase completion protocol:

1. Run phase-level tests and confirm they pass
2. Update DEVLOG with phase completion entry
3. DEVLOG learning review — scan this phase's entries for trial-and-error patterns (multiple attempts to resolve something). Extract prescriptive one-liner summaries and propose additions to DEVPLAN Gotchas.
4. Scan DEVLOG for Contract Changes markers. List affected upstream documents and flag what needs propagation.
5. DEVPLAN cleanup — reduce completed phase to a one-line summary with DEVLOG reference. Deduplicate between DEVPLAN and DEVLOG, keeping DEVPLAN minimal.
6. Update the current module's Status in ARCHITECTURE.md's Implementation Sequence table. Format: "Phase N complete" after each phase, or "Complete" if this was the module's final phase.
7. Commit all documentation updates.

Under autonomous execution: exit with ESCALATE after phase completion. Human audits before next phase begins.
