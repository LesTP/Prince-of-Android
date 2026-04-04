# Autonomous Loop Learnings

Observations from running the autonomous development loop on the Prince of Persia Android port. Captures infrastructure bugs, agent behavior patterns, framework observations, cost analysis, and human interventions for post-project writeup.

---

## Infrastructure Bugs

| # | Iter | Issue | Fix | Impact |
|---|------|-------|-----|--------|
| 1 | 1 | Budget cap $1 too low — agent hit cap after planning, before outputting LOOP_SIGNAL | Raised to $10 | Iteration reported as failed despite successful commit (fedbe8e). Required manual DEVLOG entry. |
| 2 | 2 | `jq` not installed — all JSON parsing in run-iteration.sh silently failed | Replaced with `tools/parse_jsonl.py` (python3) | LOOP_SIGNAL not parsed despite being present in output. Iteration falsely reported as NO_SIGNAL error. Summary.log had empty fields. |
| 3 | 2 | `claude -p` text output doesn't include tool-use activity | Added `--output-format stream-json --verbose` | iteration_001.log was essentially empty (just error message). No visibility into what the agent did. |
| 4 | 2 | `parse_jsonl.py` crash on non-dict `tool_use_result` field | Added `isinstance()` guard | Script crashed on first real run; required defensive typing. |
| 5 | 1 | `--max-turns` flag doesn't exist in Claude Code 2.1.84 | Removed from script (was in PS1 original). Use `--max-budget-usd` instead. | Non-issue — caught during script writing, not at runtime. |

## Agent Behavior Patterns

| # | Pattern | Frequency | Iterations | Mitigation |
|---|---------|-----------|------------|------------|
| 1 | Grep tool fails on paths with spaces ("PoP port") | Common | 4, 6 | Agent falls back to bash grep. Can't fix from docs — tool behavior. Space in project name is the root cause. |
| 2 | Unnecessary Agent subagent spawning for simple file discovery | Occasional | 4, 6 | Agent used Agent(Explore) when Glob/Grep would suffice. Wastes turns and context. No mitigation applied — low impact. |
| 3 | File size limit hit on large C files (seqtbl.c, types.h) | Expected | 5 | "File content exceeds maximum allowed tokens" — agent chunk-reads with offset/limit. Adds ~5 extra turns per large file. Inherent limitation. |
| 4 | Slash commands invoked via Skill() tool instead of reading .md files | Once | 6 | Fixed in CLAUDE.md: "Do NOT call via Skill tool. Read the .md file and follow instructions." |
| 5 | ARCHITECTURE.md status not updated after module completion | Twice | 2, 6 | Fixed in CLAUDE.md: added explicit instruction to update Implementation Sequence table in review-fixes-done step. |
| 6 | Module 6 completed in single iteration (all 4 phases) | Once | 2 | Agent merged all phases into one step. Efficient but skipped per-phase DEVLOG granularity. Acceptable for mechanical translation. |

## Framework Observations

### What worked well
- **Cold-start-per-iteration model:** Each fresh invocation correctly re-read CLAUDE.md, followed @references, determined state from DEVPLAN. Zero context confusion across 6 iterations.
- **DEVPLAN as state machine:** "No active phase → plan → exit" / "Phase in progress → pick step → execute → exit" worked exactly as designed. Agent always identified the correct state.
- **Test-before-commit discipline:** Every coding iteration ran `gradle build` + `gradle test` before committing. Zero broken commits.
- **Plan-then-execute separation:** Planning iterations ($1.50) cheaper than coding iterations ($2-3.50). The separation forces the agent to think before coding.
- **LOOP_SIGNAL protocol:** Once infrastructure bugs were fixed, signal parsing worked reliably for 4 consecutive iterations.

### What didn't work
- **Slash commands as Skill invocations:** The governance framework assumes `/phase-plan` etc. are interactive commands. In `claude -p` mode, they're just .md files. The abstraction leaked.
- **One-step-per-iteration for planning:** AUTOMATION.md says "one step per loop iteration" but the agent interpreted planning as its own iteration. This means Module 7 needed 4 iterations (plan + 3 steps) when 3 would suffice if planning were folded into the first coding step. Trade-off: more iterations = more auditability.
- **DEVLOG not always written:** Iteration 1 hit budget cap before writing DEVLOG. The commit succeeded but the audit trail was incomplete until manually fixed.

### Surprises
- **Module 6 done in one iteration:** Agent collapsed all 4 phases (types, enums, globals, tests) into a single iteration — 1,218 lines of Kotlin, 18 tests, $2.99. Much faster than expected.
- **Grep tool failures on valid files:** Not a CRLF issue (files are clean). The Grep tool has trouble with spaces in paths. Agent's workaround (bash grep) was effective.
- **Planning iterations are expensive:** $1.50-1.65 just to read docs and write a plan. Cold start (reading 5 project docs) costs ~$0.14 in cache creation, but the planning work itself consumes significant tokens.

## Cost Analysis

### Per iteration
| Iter | Type | Module | Cost | Turns | Duration | Notes |
|------|------|--------|------|-------|----------|-------|
| 1 | Plan | 6 | $1.00+ | ~15 | ~120s | Hit budget cap |
| 2 | Code (all phases) | 6 | $2.99 | 59 | 588s | Full module in one shot |
| 3 | Plan | 7 | $1.65 | 45 | 256s | |
| 4 | Code (step 7a) | 7 | $1.87 | 40 | 326s | Enums |
| 5 | Code (step 7b) | 7 | $3.58 | 43 | 621s | Sequence table data (largest) |
| 6 | Review + complete | 7 | $1.13 | 39 | 144s | Cheapest — mostly doc updates |

### Per module
| Module | Iterations | Total Cost | Lines of Kotlin | Cost/Line |
|--------|-----------|------------|-----------------|-----------|
| 6 (State Model) | 2 | ~$4.00 | ~987 | ~$0.004 |
| 7 (Sequence Table) | 4 | ~$8.23 | ~1,500+ | ~$0.005 |

### By activity type
| Activity | Avg Cost | Avg Turns | Avg Duration |
|----------|----------|-----------|--------------|
| Planning | $1.33 | ~30 | ~188s |
| Coding | $2.81 | 47 | 512s |
| Review/Complete | $1.13 | 39 | 144s |

### Notes
- All costs are Pro subscription (claude.ai login), not API billing. Cost metric tracks internal token usage, actual billing is subscription-based.
- Cache reads reduce cold-start cost on subsequent iterations within the 5-minute cache window.
- Most expensive single iteration: $3.58 (seqtbl.c translation — large data file).

## Human Interventions

| # | Iter | What | Why | Could it be automated? |
|---|------|------|-----|----------------------|
| 1 | 1 | Wrote DEVLOG entry manually | Budget cap cut off agent before DEVLOG write | Yes — higher budget cap fixes this. Fixed. |
| 2 | 1-2 | Raised budget from $1 to $5 to $10 | $1 too low for any real work | Yes — should have been estimated better upfront. Fixed. |
| 3 | 2 | Diagnosed jq missing, rewrote parser in Python | Script silently failed — no error message from jq absence | Partially — could add dependency check at script start. Fixed. |
| 4 | 2 | Fixed ARCHITECTURE.md Module 6 status | Agent didn't update it | Yes — added instruction to CLAUDE.md. Fixed. |
| 5 | 6 | Fixed ARCHITECTURE.md Module 7 status | Same pattern as #4 | Yes — instruction now in CLAUDE.md. Verify next module. |
| 6 | 6 | Fixed slash command invocation pattern | Agent used Skill() instead of reading .md file | Yes — clarified in CLAUDE.md. Verify next iteration. |
| 7 | — | Log review and analysis after each batch | Orchestrator role: read logs, analyze patterns, report via TG | Partially — could be scripted, but human judgment on "is this good work?" adds value. |

---

*Last updated: 2026-04-04, after iteration 6 (Module 7 complete).*
