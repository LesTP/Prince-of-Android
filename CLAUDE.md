# Claude Worker Adapter — Prince of Persia Android Port

> **Contract:** Follow `WORKER_SPEC.md` for iteration lifecycle, allowed actions,
> one-action rule, escalation conditions, and output contract. This file covers
> Claude-specific mechanics only.

## Framework
This project follows the From Idea to Code governance framework.

## Always Loaded
- @PROJECT.md — scope, constraints, success criteria
- @ARCHITECTURE.md — component map, layer contracts, implementation sequence
- @GOVERNANCE.md — development process reference
- @DEVPLAN.md — current status, cold start summary, gotchas
- @WORKER_SPEC.md — backend-agnostic worker contract

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

## Claude-Specific Tool Rules
- **File discovery:** Use `bash find` or `bash ls` for listing files. Do NOT spawn Agent(Explore) subagents for simple file discovery.
- **Search in this project:** Use `bash grep` instead of the Grep tool, and `bash find` instead of the Glob tool. The built-in tools have issues with this project's directory paths.
- **Edit tool requires fresh reads:** Before editing any file (especially DEVPLAN.md), read it immediately before the edit — not at the start of the iteration.

## Claude-Specific Runner Info
**Runner:** `run-iteration.sh` — runs `claude -p` per iteration, logs to `logs/loop/`.

**Slash commands:** Project commands in `.claude/commands/` — these are NOT
Skill-tool skills. To use them, read the `.md` file and follow its instructions.
Do NOT call them via the Skill tool.

| Action (from WORKER_SPEC) | Claude command file |
|---------------------------|---------------------|
| Phase Plan | `.claude/commands/phase-plan.md` |
| Step Execution | `.claude/commands/step-done.md` (after completing the step) |
| Phase Review | `.claude/commands/phase-review.md` |
| Phase Complete | `.claude/commands/phase-complete.md` |

## Autonomy
This project supports autonomous execution. When invoked with
`autonomous: true` in the prompt, commands auto-proceed and the agent follows
`WORKER_SPEC.md`. Otherwise, commands pause for human approval.

See WORKER_SPEC.md §8 for mode definitions (autonomous vs. supervised).
