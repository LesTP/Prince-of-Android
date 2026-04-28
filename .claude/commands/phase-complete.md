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
8. Verify all documents were updated before committing:
   - [ ] DEVPLAN Current Status: Track/Module/Phase/Next updated to reflect new active work
   - [ ] DEVPLAN: completed phase reduced to one-line summary with DEVLOG reference
   - [ ] DEVPLAN Gotchas: any new lessons from step 3 promoted to Gotchas section
   - [ ] DEVLOG: phase completion entry added
   - [ ] ARCHITECTURE.md: Implementation Sequence table status updated
   - [ ] DECISIONS.md: any Open decisions resolved by this phase → Closed
   - [ ] PROJECT.md: any Open risks resolved by this phase → Closed
   If any item was skipped, fix it before committing.

Under autonomous execution: exit with ESCALATE after phase completion. Human audits before next phase begins.
