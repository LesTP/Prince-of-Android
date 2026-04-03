---
name: phase-review
description: End-of-phase code review before completion
---

Review all code from the current phase.

Priority #1: Preserve existing functionality
Priority #2: Simplify and reduce code

Check for:
- Dead code or unused imports
- Architecture drift from the spec
- Opportunities to simplify
- Anything that should be split into a separate commit

Organize findings as:
- Must fix (correctness, architecture violations)
- Should fix (simplification, cleanup)
- Optional (style, minor improvements)

Under autonomous execution: proceed with must-fix and should-fix items. Skip optional items. Log decisions to DECISIONS.md. Commit fixes. Exit — next iteration runs phase-complete.
