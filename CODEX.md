# Codex Worker Adapter — Prince of Persia Android Port

> **Contract:** Follow `WORKER_SPEC.md` for iteration lifecycle, allowed actions,
> one-action rule, escalation conditions, and output contract. This file covers
> Codex-specific mechanics only.

## Framework
This project follows the From Idea to Code governance framework.

## Required Reading — Every Iteration

You do not have `@`-reference loading. You must explicitly read these files at
the start of every iteration before taking any action.

**CRITICAL: Minimize tool calls.** Each tool call round-trips through the full
context window. Combine reads into as few shell commands as possible.

### Tier 1 — Always (mandatory, every iteration)

Read CODEX.md (this file), WORKER_SPEC.md, and DEVPLAN.md (up to the HISTORY
fence) in a **single command**:

```bash
cat CODEX.md && echo '---SPLIT---' && cat WORKER_SPEC.md && echo '---SPLIT---' && awk '/<!-- HISTORY -->/{exit} {print}' DEVPLAN.md
```

**DEVLOG.md fence:** When reading or writing to DEVLOG.md, stop at the
`<!-- HISTORY` fence. Insert new entries **above** the fence line. Do not read
or patch content below it.

```bash
awk '/<!-- HISTORY -->/{exit} {print}' DEVLOG.md
```

### Tier 2 — Current module (mandatory for step/review/complete actions)

After determining the active module from DEVPLAN's Current Status, read the
relevant section of ARCHITECTURE.md for the layer contract and module
dependencies. Combine with source files in the **same command**.

### Tier 3 — On demand (read only when needed)
- `PROJECT.md` — only during Phase Plan actions
- `ARCHITECTURE.md` — only during Phase Plan or cross-module wiring
- `GOVERNANCE.md` — only if unsure about process

### Read efficiency rules
- **Combine related reads** into one `cat A && echo '---' && cat B` command
- **Never read one file per tool call** when you need multiple files
- **Use `sed -n` ranges** only when you need a specific section, not the whole file
- **Fresh reads before edits** — re-read immediately before editing, not at iteration start

## Load for Current Module
After reading DEVPLAN, determine the active track and module from its Current
Status section. Then read the relevant section of ARCHITECTURE.md for the
layer contract and module dependencies.

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

## Codex-Specific Tool Rules
- **No `@` references.** Read files explicitly using file-read tools or CLI.
  When a file contains `@FILENAME` references, treat them as file paths to read.
- **Command files shared with Claude.** Action procedures live in
  `.claude/commands/*.md`. Read these files and follow their instructions the
  same way Claude does — the content is backend-agnostic.
- **Fresh reads before edits.** Before editing any file (especially DEVPLAN.md),
  read it immediately before the edit — not at the start of the iteration.
- **Shell usage.** Use CLI tools directly for builds, tests, git operations,
  file discovery, and search.
- **Search tool availability.** This loop environment may not have `rg`
  installed. Before using `rg`, check availability with `command -v rg`. If it
  is absent, use portable fallbacks instead: `find` for file discovery,
  `grep -RIn` for text search, and `sed -n` for bounded file reads. Do not
  repeatedly attempt `rg` after it has failed in the same iteration.

## Action Instructions

WORKER_SPEC.md defines four allowed actions. Here is how to execute each one
in Codex. Perform **exactly one** per iteration.

### Phase Plan
**When:** No active phase for the current module.
1. Read `.claude/commands/phase-plan.md` and follow its instructions.
2. Commit with message: `phase-plan: <module>.<phase> — <summary>`.
3. Emit exit signal and stop.

### Step Execution
**When:** A phase is in progress with remaining steps.
1. Pick the next step from DEVPLAN. Do all file read/write work.
2. Run builds, tests, and git operations as needed.
3. Read `.claude/commands/step-done.md` and follow its instructions.
4. Emit exit signal and stop. Do **not** start the next step.

### Phase Review
**When:** All steps in the current phase are complete.
1. Read `.claude/commands/phase-review.md` and follow its instructions.
2. Emit exit signal and stop.

### Phase Complete
**When:** Review is done and fixes (if any) are applied.
1. Read `.claude/commands/phase-complete.md` and follow its instructions.
2. Emit exit signal and stop.

## Output Contract

End every iteration with exactly these four lines — no additional text after:

```
LOOP_SIGNAL: CONTINUE | ESCALATE
REASON: <one-line summary>
ACTION_TYPE: PHASE_PLAN | STEP | REVIEW | COMPLETE
ACTION_ID: <module.phase.step>
```

## Autonomy

When invoked in autonomous mode, execute the action and emit the exit signal
without waiting for human input. In supervised mode, surface proposed changes
for approval before committing.

See WORKER_SPEC.md §8 for full mode definitions.
