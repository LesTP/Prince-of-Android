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
| 1 | Grep tool fails on paths with spaces ("PoP port") | Common | 4, 6, 8-12 | Agent falls back to bash grep. Can't fix from docs — tool behavior. Space in project name is the root cause. Pervasive across all Module 8 iterations. |
| 2 | Unnecessary Agent subagent spawning for simple file discovery | Common | 4, 6, 8-12 | Upgraded from "Occasional" — every Module 8 coding iteration spawned Agent(Explore) for file discovery that Glob/bash could handle. Wastes ~$0.10-0.30 and 2-5 turns per iteration. No mitigation applied — low impact per-iteration but cumulative. |
| 3 | File size limit hit on large files (seqtbl.c, types.h, Seg006.kt) | Expected | 5, 8, 12 | "File content exceeds maximum allowed tokens" — agent chunk-reads with offset/limit. Seg006.kt grew to 32,583 tokens (1,993 lines) by iter 12, requiring 6 chunk reads for review. Inherent limitation. |
| 4 | Slash commands invoked via Skill() tool instead of reading .md files | Once | 6 | Fixed in CLAUDE.md: "Do NOT call via Skill tool. Read the .md file and follow instructions." Verified fixed: iters 7-12 all read .md files correctly. |
| 5 | ARCHITECTURE.md status not updated after module completion | Twice | 2, 6 | Fixed in CLAUDE.md: added explicit instruction to update Implementation Sequence table in review-fixes-done step. Verified fixed: iter 7 correctly updated status to "In progress". |
| 6 | Module 6 completed in single iteration (all 4 phases) | Once | 2 | Agent merged all phases into one step. Efficient but skipped per-phase DEVLOG granularity. Acceptable for mechanical translation. |
| 7 | "File has not been read yet" error on DEVPLAN edits | Common | 9, 10, 11, 12 | Agent reads DEVPLAN early, edits other files, then tries to edit DEVPLAN — Edit tool requires a fresh read. Agent recovers (re-reads then edits) but wastes 2 turns each time. Could mitigate by reading DEVPLAN immediately before editing. |
| 8 | Phase 8a absorbed work from later phases (scope creep) | Once | 8 | Plan allocated ~32 functions to 8a; agent translated 45 (including frame loading and physics helpers planned for 8b). Subsequent phases were smaller than planned. Net effect: neutral (same total work, different distribution). |
| 9 | Glob tool also fails on paths with spaces | Common | 8-12 | Same root cause as #1. Glob returns "Directory does not exist" or "No files found" for valid paths. Agent falls back to bash find/ls. |
| 10 | Test bugs outnumber translation bugs | Pattern | 8, 9 | All test failures were in test code (wrong FrameType constructor param order, wrong tile setup location, wrong expected values), not in the C→Kotlin translation. Translation quality is high; test writing is the error-prone step. |
| 11 | Compilation errors self-corrected within iteration | Pattern | 8, 11 | Iter 8: one Short/Int type mismatch. Iter 11: companion object in object, import alias mismatch (SeqIds vs Seq), Short vs Int. All fixed within same iteration, no escalation needed. |

## Framework Observations

### What worked well
- **Cold-start-per-iteration model:** Each fresh invocation correctly re-read CLAUDE.md, followed @references, determined state from DEVPLAN. Zero context confusion across 12 iterations.
- **DEVPLAN as state machine:** "No active phase → plan → exit" / "Phase in progress → pick step → execute → exit" worked exactly as designed. Agent always identified the correct state across 3 modules.
- **Test-before-commit discipline:** Every coding iteration ran `gradle build` + `gradle test` before committing. Zero broken commits across 12 iterations.
- **Plan-then-execute separation:** Planning iterations ($1.00-1.65) cheaper than coding iterations ($2-6.30). The separation forces the agent to think before coding.
- **LOOP_SIGNAL protocol:** Once infrastructure bugs were fixed, signal parsing worked reliably for 10 consecutive iterations (iters 3-12).
- **Phase lifecycle (plan→execute→review→complete):** Module 8 demonstrated the full lifecycle cleanly. Review phase (8e) confirmed zero issues — the phase structure provides a natural quality gate even when autonomous.
- **CLAUDE.md fixes propagate reliably:** Fixes added after Module 7 (slash command reading, ARCHITECTURE.md updates) were correctly followed in all Module 8 iterations. Proves that CLAUDE.md is an effective steering mechanism.
- **Error recovery within iterations:** Compilation errors and test failures were always self-corrected within the same iteration, never requiring escalation or human intervention.

### What didn't work
- **Slash commands as Skill invocations:** The governance framework assumes `/phase-plan` etc. are interactive commands. In `claude -p` mode, they're just .md files. The abstraction leaked. (Fixed after iter 6, verified fixed in iters 7-12.)
- **One-step-per-iteration for planning:** AUTOMATION.md says "one step per loop iteration" but the agent interpreted planning as its own iteration. This means Module 7 needed 4 iterations (plan + 3 steps) when 3 would suffice if planning were folded into the first coding step. Trade-off: more iterations = more auditability.
- **DEVLOG not always written:** Iteration 1 hit budget cap before writing DEVLOG. The commit succeeded but the audit trail was incomplete until manually fixed.
- **Phase scoping is approximate:** Phase 8a was planned for ~32 functions but the agent translated 45 (absorbing work from 8b). Phase plans are useful for ordering work, but the agent will naturally adjust scope based on what's logically coherent. Not necessarily bad — just means phase boundaries are soft.
- **Grep/Glob tools unreliable with spaces in paths:** Persistent across iterations 1-12. The agent always recovers via bash, but it wastes 2-5 turns per iteration on failed tool calls before falling back. Fixed by renaming directory to PoP_port (no spaces).
- **Orchestrator session implemented code directly (Modules 9-10):** Without a workspace-level CLAUDE.md, the orchestrator read the project's CLAUDE.md and followed its iteration protocol — implementing code instead of dispatching loops. Burned context 5-10x faster, lost per-iteration cost tracking and log auditability. Fixed by creating /home/claude/workspace/CLAUDE.md with explicit orchestrator role definition.

### Surprises
- **Module 6 done in one iteration:** Agent collapsed all 4 phases (types, enums, globals, tests) into a single iteration — 1,218 lines of Kotlin, 18 tests, $2.99. Much faster than expected.
- **Module 8 Phase 8a was the most expensive single iteration ($6.30):** 45 functions, 42 tests, 862 seconds. The first coding iteration of a large module is disproportionately expensive because the agent reads the entire C source file plus all existing Kotlin files.
- **Review phase found zero issues:** After 4 coding phases and 1,993 lines of Kotlin, the Phase 8e review found nothing to fix. Either the translation quality is genuinely high, or the review is too shallow. The replay oracle (not yet wired up for seg006) will be the true test.
- **Test bugs > translation bugs:** Every test failure across Module 8 was a bug in the test code (wrong constructor param order, wrong expected values, wrong test setup), not in the C→Kotlin translation itself. The agent is better at translation than at writing tests for that translation.
- **Planning got cheaper:** Module 8 planning was $1.00 (iter 7) vs Module 7's $1.65 (iter 3). Possibly due to cache hits or the agent becoming more efficient with the project structure.
- **Grep tool failures on valid files:** Not a CRLF issue (files are clean). The Grep tool has trouble with spaces in paths. Agent's workaround (bash grep) was effective.

### Module 8 Specific Learnings (seg006 — first large game logic file)

**Scale:** 2,154 lines of C → 1,993 lines of Kotlin, 81 functions, 113 new tests, 6 iterations, ~$19.74, ~47 minutes wall clock.

**External dependency stubs pattern worked well.** Agent created `ExternalStubs.kt` with function references that throw `NotImplementedError` for functions from other segments (seg000, seg002, seg003, seg005, seg007, seg008). This kept seg006 compilable and testable in isolation. When those modules are translated, stubs get replaced with real implementations. Clean architectural decision logged to DECISIONS.md.

**Chunk reading strategy for large files.** seg006.c exceeded the 10K token read limit. The agent consistently used offset/limit reads (200-300 lines per chunk, 8-9 reads total). This is mechanical but reliable — adds ~5 turns per large file read. The Kotlin output (Seg006.kt) also exceeded the limit by the review phase, requiring 6 chunk reads.

**Feature flag translation.** SDLPoP has ~12 `#ifdef` bug-fix flags. The agent translated these as runtime checks against `GameState.custom`/`GameState.fixes` — correct approach, matching the reference build configuration. No ambiguity about which code paths to include.

**Test quality vs translation quality.** The agent's C→Kotlin translation had zero logical bugs caught by testing (all failures were test-code bugs). Specific test errors:
- Iter 8: Frame table size expectations wrong (241 vs 243), frame data values wrong (image field index confusion)
- Iter 9: FrameType constructor parameter order wrong in test (passed THIN flag to `sword` field instead of `flags` field), tile setup location wrong (`currRoomTiles` vs `level.fg` — `getRoomAddress` reloads from level data)
- Both are cases where the test didn't match the data structure, not where the translation was wrong.

### Module 9-10 Specific Learnings (orchestrator anti-pattern)

**Modules 9-10 implemented directly by orchestrator — anti-pattern discovered.** The Telegram orchestrator session implemented seg004 (26 functions) and seg005 (38 functions) directly instead of dispatching via `run-iteration.sh`. This burned orchestrator context rapidly — the session hit context limits after ~2 modules, requiring compaction. The orchestrator's job is to dispatch loops, analyze logs, and report — not to write Kotlin.

**Root cause:** User said "do one loop" and the orchestrator interpreted this as "implement the next step" rather than "run `./run-iteration.sh`". The CLAUDE.md in the project directory (which the orchestrator reads) has iteration protocol instructions that look actionable, so the orchestrator followed them directly.

**Fix:** Created `/home/claude/workspace/CLAUDE.md` (parent directory) defining the orchestrator role explicitly. The orchestrator session launches from `/home/claude/workspace/`, reads this CLAUDE.md, and knows to dispatch — not implement. The project's own `PoP_port/CLAUDE.md` is for inner loop sessions only.

**Impact:** Modules 9-10 translation quality was fine (zero test failures in final code, 117 new tests all pass), but cost/auditability was lost:
- No per-iteration cost tracking (orchestrator session doesn't log costs per step)
- No iteration transcripts in `logs/loop/`
- Context burned 5-10x faster than dispatched iterations
- Orchestrator couldn't handle other requests (status checks, log reviews) while implementing

**New learnings from Modules 9-10 translation:**
- **CharType fields are all `Int`** — no `.toByte()` needed (unlike GameState Short fields). Caused multiple compile errors in seg005 Phase 10a. Added to DEVPLAN gotchas.
- **C goto refactoring patterns:** `land()` gotos → helper methods, `controlStanding` goto → fall-through. Clean patterns, no semantic issues.
- **C unsigned word comparisons:** `(word)x < (word)y` translates to `(x and 0xFFFF) < (y and 0xFFFF)`. Recurring in distance checks. Added to DEVPLAN gotchas.
- **ExternalStubs wire-up works at scale:** 4 stubs wired in Phase 10c (control, drawSword, spiked, doFall). Pattern scales cleanly.
- **`else if` chain correctness matters:** Two bugs from placing `if` where `else if` was needed in control dispatch chains. C source structure must be followed exactly for dispatch chains.

**Compilation errors were type-system issues, not logic errors.** Iter 8: Short negation type mismatch. Iter 11: `companion object` in `object` (Kotlin-specific), SeqIds vs Seq import alias, Short vs Int for Char.x. All caught by the compiler, fixed in same iteration. The Kotlin type system is doing its job as a safety net.

**Phase-complete not yet run.** Iteration 12 ran phase-review (8e) but not phase-complete. The next iteration (13) will run phase-complete to close Module 8 and update docs. This is a governance lifecycle detail — review and completion are separate steps by design.

**Module 8 validates the autonomous methodology at scale.** This is the first "real" game logic file (character physics, tile queries, player/guard control). Modules 6-7 were data definitions and tables. Module 8 has branching logic, state mutation, complex control flow, and cross-function dependencies. The fact that it completed with zero escalations and zero human interventions is a strong signal that the methodology works for Build-regime translation tasks.

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
| 7 | Plan | 8 | $1.00 | 23 | 272s | Cheapest plan — efficient cold start |
| 8 | Code (phase 8a) | 8 | $6.30 | 83 | 862s | **Most expensive** — 45 functions, 42 tests |
| 9 | Code (phase 8b) | 8 | $3.09 | 66 | 542s | 8 functions, 18 tests |
| 10 | Code (phase 8c) | 8 | $3.89 | 79 | 623s | 13 functions, 29 tests |
| 11 | Code (phase 8d) | 8 | $3.87 | 87 | 611s | Final 7 functions, 24 tests. Compilation errors self-fixed. |
| 12 | Review (phase 8e) | 8 | $1.58 | 40 | 171s | Zero issues found |
| 13 | Complete | 10 | $0.80 | 21 | 162s | Phase-complete via loop — first iteration after Modules 9-10 done by orchestrator |

### Per module
| Module | Iterations | Total Cost | Lines of Kotlin | Tests | Cost/Line |
|--------|-----------|------------|-----------------|-------|-----------|
| 6 (State Model) | 2 | ~$4.00 | ~987 | 27 | ~$0.004 |
| 7 (Sequence Table) | 4 | ~$8.23 | ~1,500+ | 50 | ~$0.005 |
| 8 (seg006) | 6 | ~$19.74 | ~1,993 | 113 | ~$0.010 |
| 9 (seg004) | 0* | N/A | ~350 | 42 | N/A |
| 10 (seg005) | 1** | ~$0.80 | ~600 | 75 | N/A |

\* Module 9 implemented directly by orchestrator session (anti-pattern — see below).
\*\* Module 10 phases 10a-10c implemented by orchestrator; only phase-complete ran via loop (iter 13).

### By activity type (updated through iter 12)
| Activity | Avg Cost | Avg Turns | Avg Duration |
|----------|----------|-----------|--------------|
| Planning | $1.22 | ~28 | ~216s |
| Coding | $3.64 | 67 | 596s |
| Review/Complete | $1.36 | 40 | 158s |

### Cost scaling observation
Module 8 cost/line ($0.010) is 2x Module 7 ($0.005) despite similar translation work. Key drivers:
- **Cold-start overhead per phase:** Each iteration re-reads CLAUDE.md + DEVPLAN + source files. More phases = more cold starts.
- **File size tax:** seg006.c (2,154 lines) exceeds the 10K token read limit, requiring chunk reads. seqtbl.c (1,228 lines) fit in fewer reads.
- **Grep/Glob fallback overhead:** ~2-5 wasted turns per iteration on failed tool calls before bash fallback.
- **Agent subagent spawning:** ~$0.10-0.30 per iteration on unnecessary Explore agents.

Estimated overhead per iteration: ~$0.40-0.70 in tool failures + subagent waste. Over 6 iterations that's ~$2.50-4.00 — roughly 15-20% of total module cost.

### Notes
- All costs are Pro subscription (claude.ai login), not API billing. Cost metric tracks internal token usage, actual billing is subscription-based.
- Cache reads reduce cold-start cost on subsequent iterations within the 5-minute cache window.
- Most expensive single iteration: $6.30 (Phase 8a — 45 functions, first coding pass on seg006).
- Total loop cost through Module 10: ~$32.80 across 13 iterations. Modules 9-10 code cost not tracked (orchestrator session).

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
| 8 | 7-12 | None required | Module 8 completed with zero human interventions during execution | N/A — validates that CLAUDE.md fixes from Module 7 were effective. |
| 9 | — | Orchestrator implemented Modules 9-10 directly | No workspace-level CLAUDE.md to define orchestrator role | Yes — created /home/claude/workspace/CLAUDE.md with orchestrator role. See anti-pattern writeup above. |
| 10 | 13 | None required | Phase-complete ran cleanly via loop | N/A — first iteration after orchestrator fix. |

## Efficiency Analysis (after iteration 12)

### Token overhead sources — loop iterations
| Source | Turns wasted/iter | Est. cost/iter | Fix |
|--------|-------------------|----------------|-----|
| Grep/Glob failures on paths with spaces | 2-5 | ~$0.10-0.20 | Rename directory (remove space) |
| Agent(Explore) for simple file discovery | 2-5 | ~$0.10-0.30 | CLAUDE.md instruction: use bash find/ls |
| "File has not been read yet" on DEVPLAN edits | 2 | ~$0.05 | CLAUDE.md instruction: read immediately before editing |
| Cold-start doc re-reading | 5-10 | ~$0.14 | Inherent to design — acceptable |

Total estimated waste: ~$0.40-0.70 per iteration, ~15-20% of module cost.

### Token overhead sources — orchestrator session
| Source | Tokens consumed | Fix |
|--------|----------------|-----|
| @references auto-load (ARCHITECTURE.md, PROJECT.md, GOVERNANCE.md) | ~15K+ per system turn | These load because CLAUDE.md has @references and the orchestrator runs from the project directory. Options: (a) run orchestrator from parent dir, (b) create lighter CLAUDE_ORCH.md, (c) accept the cost. |
| Full .txt log reads for review | ~5K-15K per review | Log digest script — extract only errors, test failures, commits. ~50 lines instead of 1,761. |
| Compaction summary | ~5K+ | Unavoidable for context continuity, but leaner orchestrator context = less to summarize. |
| Verbose TG analysis messages | ~1K per batch | Acceptable — human value outweighs token cost. |

### Implemented fixes
- [x] Rename directory: "PoP port" → "PoP_port" (eliminates Grep/Glob failures) — done 2026-04-04
- [x] CLAUDE.md hints for loop efficiency (no Agent(Explore), bash for spaces, read-before-edit) — done 2026-04-04
- [ ] Log digest script (tools/digest_logs.py)
- [x] Workspace-level CLAUDE.md defining orchestrator role — done 2026-04-05. Prevents orchestrator from implementing code directly.
- [x] Log digest script (`tools/digest_logs.py`) — done 2026-04-07
- [x] Codex backend support in `run-iteration.sh` — done 2026-04-10
- [x] `parse_codex_jsonl.py` for Codex transcript parsing — done 2026-04-11
- [x] WORKER_SPEC.md (backend-agnostic contract) — done 2026-04-15
- [ ] Replay oracle wiring for seg006+ (needed for ground-truth validation beyond unit tests)

---

## Update: Iterations 14-51 (Modules 11-14)

*Added 2026-04-16. Covers the completion of Layer 1 translation and the replay runner.*

### Additional Agent Behavior Patterns

| # | Pattern | Frequency | Iterations | Mitigation |
|---|---------|-----------|------------|------------|
| 12 | Kotlin Short/Int type confusion in translated code | Common | 14-28 | Added to DEVPLAN gotchas: CharType fields are Int, not Short. Agent still hits this occasionally. |
| 13 | Codex backend produces correct code but less verbose DEVLOG entries | Pattern | 23-28 | Codex iterations (cost=n/a) work functionally but governance doc updates are terser than Claude iterations. Acceptable trade-off. |
| 14 | Replay regression tests catch frame-0 divergences | Blocker | 47-51 | Layer 1 replay oracle exposed game-loop initialization differences. Blocked Module 14 until Layer 2 (seg000/seg003) is translated. Correct escalation behavior. |

### Additional Cost Analysis

#### Iterations 14-51

| Iter | Type | Module | Cost | Backend | Notes |
|------|------|--------|------|---------|-------|
| 14 | Plan | 11 (seg002) | $1.16 | claude | Guard AI planning |
| 15-22 | Mixed | 11 (seg002) | ~$15 | claude | 73 functions, guard AI + combat |
| 23-28 | Mixed | 12 (seg007) | n/a | codex | Trob, loose floors — first Codex module |
| 29-38 | Mixed | 13 (integration) | ~$5 | claude | Replay oracle pipeline |
| 39-45 | Mixed | 14 (replay runner) | ~$8 | mixed | P1R parser, trace comparison |
| 46-48 | Step | 14 | n/a | codex | Regression harness |
| 49-51 | Review+ | 14 | — | mixed | ESCALATE on replay divergence — correct |

**Total cost (all 51 iterations):** ~$60.81 (Claude iterations only; Codex cost not metered)

#### Per-module totals (updated)

| Module | Iterations | Total Cost | Lines | Tests | Notes |
|--------|-----------|------------|-------|-------|-------|
| 6 (State Model) | 2 | ~$4.00 | ~987 | 27 | |
| 7 (Seq Table) | 4 | ~$8.23 | ~1,500 | 50 | |
| 8 (seg006) | 6 | ~$19.74 | ~1,993 | 113 | |
| 9 (seg004) | 0* | N/A | ~350 | 42 | Orchestrator anti-pattern |
| 10 (seg005) | 1** | ~$0.80 | ~600 | 75 | |
| 11 (seg002) | ~9 | ~$16 | ~2,000+ | ~80 | Guard AI, combat |
| 12 (seg007) | ~6 | n/a | ~800+ | ~50 | First Codex-only module |
| 13 (integration) | ~10 | ~$5 | ~500 | ~60 | Replay oracle pipeline |
| 14 (replay runner) | ~7 | ~$8 | ~400 | ~40 | 1/13 traces match |

#### By activity type (updated through iter 51)

| Activity | Avg Cost (Claude) | Avg Duration |
|----------|-------------------|-------------|
| Planning | ~$1.10 | ~180s |
| Coding | ~$3.50 | ~500s |
| Review/Complete | ~$1.20 | ~150s |

### Additional Human Interventions

| # | Iter | What | Why | Could it be automated? |
|---|------|------|-----|----------------------|
| 11 | 23+ | Switched to Codex backend for Module 12 | Cost experiment — Codex iterations are unmetered | N/A — human decision about cost strategy |
| 12 | 47-51 | Module 14 replay regression blocked | Frame-0 divergences require Layer 2 game loop translation | No — genuine dependency; correct escalation |

### Framework Observations (Modules 11-14)

#### What worked
- **Codex backend validated:** Module 12 (seg007) completed entirely on Codex with the same governance framework. Proves backend-agnostic design works in practice.
- **ExternalStubs pattern scaled to 5 modules:** seg002, seg004, seg005, seg006, seg007 all use the stub pattern for cross-segment dependencies. Wire-up happens naturally as modules complete.
- **Replay oracle as acceptance gate:** The oracle correctly identified when game-loop initialization was missing, producing genuine escalations (not false positives). This validates the test strategy.

#### What didn't work
- **Replay oracle blocked further progress:** 12/13 traces fail due to Layer 2 game-loop behavior (frame-0 initialization). Unit tests pass, but end-to-end validation is gated on Layer 2 translation. This is an architectural dependency, not a framework failure.

---

*Last updated: 2026-04-16, after iteration 51 (Module 14 complete, Module 15 in progress).*
