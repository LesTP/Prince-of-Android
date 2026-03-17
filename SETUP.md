# Autonomous Loop — Setup & Reference

Covers infrastructure setup, directory layout, loop protocol, and
validation history for the From Idea to Code governance framework.

**Primary runtime:** Claude Code CLI (native Windows).
Docker setup is archived in the appendix — it works but adds unnecessary
complexity now that the CLI is available directly.

---

## One-Time Setup

Complete these steps once on a fresh machine.

### Step 1: Install Claude Code

Install Claude Code CLI via npm (requires Node.js 18+):

```powershell
npm install -g @anthropic-ai/claude-code
```

### Step 2: Log in

```powershell
claude
```

Follow the login prompts. Auth tokens are stored automatically and
refresh on subsequent runs. Exit the session (`/exit` or Ctrl-C).

### Step 3: Create the Devmate rules symlink (for VS Code)

Open PowerShell **as Administrator**:

```powershell
$source  = "C:\Users\myeluashvili\Downloads\MY\ai dev docs\e2e"
$devmate = "C:\Users\myeluashvili\.llms\rules"

mkdir $devmate -Force

Remove-Item "$devmate\CLAUDE.md" -Force -ErrorAction SilentlyContinue
New-Item -ItemType SymbolicLink `
    -Path "$devmate\CLAUDE.md" `
    -Target "$source\CLAUDE_GLOBAL.md"
```

This makes global preferences auto-load in VS Code via Devmate.

### Step 4: Verify

```powershell
# Check Devmate symlink
Get-Item "C:\Users\myeluashvili\.llms\rules\CLAUDE.md" | Select-Object FullName, LinkType, Target

# Check CLI works in headless mode
claude -p "Say hello" --max-turns 1
```

---

## Two Locations

After setup, two locations reference the global config:

```
SOURCE (single source of truth — edit files here)
C:\Users\myeluashvili\Downloads\MY\ai dev docs\e2e\
├── CLAUDE_GLOBAL.md           ← global preferences
├── 03. GOVERNANCE.md          ← process reference
├── CLAUDE_project_template.md ← project CLAUDE.md starter
├── COMMANDS\                  ← slash commands (if using Claude Code commands feature)
│   ├── cold-start.md
│   ├── phase-plan.md
│   └── ...
└── (other templates)


DEVMATE (VS Code — auto-loaded as <devmate_rules>)
C:\Users\myeluashvili\
└── .llms\
    └── rules\
        └── CLAUDE.md  ──symlink──→  e2e\CLAUDE_GLOBAL.md
```

Edit `CLAUDE_GLOBAL.md` once → Devmate sees the change immediately
through the symlink.

---

## Per-Project Setup

For each new project:

```powershell
$project = "C:\Users\myeluashvili\claude-code-workspace\projects\my-new-project"
$source  = "C:\Users\myeluashvili\Downloads\MY\ai dev docs\e2e"

mkdir $project -Force

Copy-Item "$source\CLAUDE_project_template.md" "$project\CLAUDE.md"
Copy-Item "$source\03. GOVERNANCE.md"          "$project\GOVERNANCE.md"

# Edit CLAUDE.md: fill in project name, module list, project-specific notes
# Edit GOVERNANCE.md: customize the Environment section if needed
```

Then run Discovery and Architecture (browser chatbot) to produce
`PROJECT.md` and `ARCHITECTURE.md`, and place them in `$project`.

---

## Project Directory Structure

```
C:\...\projects\my-project\
│
│  # ── Always loaded (Claude reads CLAUDE.md, follows @ references) ──
│
├── CLAUDE.md              ← from CLAUDE_project_template.md
├── PROJECT.md             ← produced during Discovery
├── ARCHITECTURE.md        ← produced during Architecture
├── GOVERNANCE.md          ← copied from source, Environment customized
│
│  # ── Per-module directories (created during Implementation) ──
│
├── module-a/
│   ├── ARCH_module-a.md   ← module contract (from Architecture)
│   ├── DEVPLAN.md         ← roadmap + current status (Claude writes)
│   ├── DEVLOG.md          ← history (Claude writes)
│   ├── DECISIONS.md       ← autonomous decision log (Claude writes)
│   └── src/               ← actual code (Claude writes)
│
└── (more modules as needed)
```

---

## Loop Protocol

The autonomous loop can be driven by a PowerShell script or run
manually one iteration at a time.

1. Invoke Claude Code with a fixed prompt in headless (`-p`) mode
2. Claude reads CLAUDE.md → follows `@` references → reads DEVPLAN
3. Executes one step, updates DEVLOG, updates DEVPLAN status
4. Outputs `LOOP_SIGNAL: CONTINUE | ESCALATE` as final lines
5. Runner parses the signal → loops or stops

### Fixed prompt (never changes)
```
Read CLAUDE.md. Determine current state from the project documents.
Execute one iteration of the autonomous loop. Then exit.

Your final output MUST end with exactly two lines:
LOOP_SIGNAL: CONTINUE | ESCALATE
REASON: [one line — what was done or why stopping]
```

### CLI invocation (single iteration)

```powershell
cd "C:\Users\myeluashvili\claude-code-workspace\projects\my-project"

claude -p "Read CLAUDE.md. Determine current state from the project documents. Execute one iteration of the autonomous loop. Then exit. Your final output MUST end with exactly two lines:
LOOP_SIGNAL: CONTINUE | ESCALATE
REASON: [one line — what was done or why stopping]" `
  --max-turns 50 `
  --allowedTools "Bash(*)" "Read(*)" "Write(*)" "Edit(*)"
```

**Note on `Bash(*)`:** This is Claude Code's internal name for the shell
execution tool. On Windows with PowerShell as the default shell, it uses
PowerShell — the name is a misnomer. The GOVERNANCE.md shell section
steers the agent toward PowerShell cmdlets (`Get-ChildItem`,
`Select-String`, `Get-Content`) instead of bash commands.

### Loop runner (multiple iterations)

**`run-loop.ps1`** — invoked with `-ProjectDir` pointing at a project:

```powershell
.\run-loop.ps1 -ProjectDir "C:\...\projects\my-project"
.\run-loop.ps1 -ProjectDir "C:\...\projects\my-project" -MaxIterations 10
```

### Stop conditions
- `LOOP_SIGNAL: ESCALATE` → clean stop (phase complete, hard stop, etc.)
- No `LOOP_SIGNAL` in output → error stop
- Non-zero exit code → error stop
- Max iterations exceeded → safety stop

---

## What Stays the Same vs What Changes

### Set up once, never touch again
- Claude Code CLI install
- Devmate symlink (global CLAUDE.md → source folder)
- Auth tokens (auto-managed by Claude Code)

### Copy once per project
- CLAUDE.md (from CLAUDE_project_template.md — fill in project details)
- GOVERNANCE.md (from source — customize Environment section)

### Produced during Discovery + Architecture
- PROJECT.md
- ARCHITECTURE.md
- ARCH_[module].md files

### Written by Claude during Implementation
- DEVPLAN.md, DEVLOG.md, DECISIONS.md per module
- Source code
- ARCHITECTURE.md status column updates

---

## Validation History

### 2026-03-10: Infrastructure test (loop-test/) — Docker

**Runtime:** Claude Code 2.1.37, Docker on Windows 11 / WSL2

**Single-shot test:** PASS — CLAUDE.md loaded, file created, LOOP_SIGNAL
parsed.

**Counter loop (3 iterations):**
- Iteration 1: 14s — created ITERATION_LOG.md → CONTINUE
- Iteration 2: 29s — read prior state, appended → CONTINUE
- Iteration 3: 13s — recognized 3rd iteration → ESCALATE
- Total: 62s for 3 cold starts including Docker overhead.

### 2026-03-10: PoP port context test — Docker

**Single-shot test:** PASS — loaded CLAUDE.md + 2 `@`-referenced files
(pop_android_port_guide.md, GOVERNANCE.md). Wrote CONTEXT_REPORT.md with
accurate one-sentence summaries of each document.

### 2026-03-11: PoP port 4-step governance test — Docker

**DEVPLAN-driven loop (4 steps, 4 iterations):**
- Iteration 1: 124s — logged context files to DEVLOG.md → CONTINUE
- Iteration 2: 48s — logged session metadata (WSL2, Node v20.20.0) → CONTINUE
- Iteration 3: 42s — logged project structure analysis → CONTINUE
- Iteration 4: 45s — read prior entries, wrote summary → ESCALATE
- Total: 267s (~4.5 min) for 4 cold-start iterations.

**Verified:** DEVPLAN status updated after each step. DEVLOG appended
correctly across 4 separate cold starts. Phase marked COMPLETE. ESCALATE
reason: "human audit before next phase." Git noted as unavailable inside
container (commit skipped, logged).

### 2026-03-17: Native CLI shell test

**Runtime:** Claude Code CLI, Windows 11, PowerShell 7

**Verified:** PowerShell commands execute without hanging:
- `Get-ChildItem` (file listing) — PASS
- `git log` — PASS

Prior Docker/MSYS2 bash hanging issue (2026-03-17 DEVLOG) does not
reproduce with native CLI. The `Bash(*)` tool permission uses the
system's default shell (PowerShell 7).

**Pending:** Full loop iteration test via native CLI (planned as next
validation step).

---

## Appendix: Docker Setup (Archived)

The Docker-based setup was validated and works. It is no longer the
primary method but is preserved here for reference or as a fallback.

### Why Docker was used initially
- Consistent Linux environment for Claude Code
- Isolated permissions (no host filesystem access outside bind mounts)
- Reproducible across machines

### Why native CLI replaced it
- Claude Code CLI is now available directly on Windows
- Docker added container startup overhead (~5s per iteration)
- Linux-in-Docker caused shell mismatches (bash commands hanging on
  Windows paths/MSYS2 interactions)
- Simpler setup: no Dockerfile, no bind mounts, no symlinks to
  .claude-config

### Docker build

```powershell
cd C:\Users\myeluashvili\claude-code-workspace
docker build -t claude-code .
```

The Dockerfile installs Claude Code via npm on a Node 20 base image.
The entrypoint symlinks `/root/.claude.json` into the persistent config
directory so settings survive between container runs.

### Docker directory structure

```
C:\Users\myeluashvili\claude-code-workspace\
├── .claude-config\
│   ├── CLAUDE.md      ──symlink──→  e2e\CLAUDE_GLOBAL.md
│   ├── commands\      ──symlink──→  e2e\COMMANDS\
│   ├── .credentials.json            (auto-managed by Claude Code)
│   └── settings.json                (auto-managed by Claude Code)
├── Dockerfile
├── run-claude-code.ps1              (interactive launcher)
└── projects\
    └── (your projects here)
```

### Docker mount mapping

```
Host (Windows)                                     Container (Linux)
───────────────────────────────────────────────────────────────────────
.claude-config/                              → /root/.claude/
  ├── CLAUDE.md (symlink → CLAUDE_GLOBAL)        global preferences
  ├── commands/ (symlink → COMMANDS/)            slash commands
  ├── .credentials.json                          auth tokens
  └── settings.json                              tool config

projects/my-project/                         → /workspace/
  ├── CLAUDE.md                                  project context
  ├── PROJECT.md                                 scope + constraints
  ├── ARCHITECTURE.md                            component map
  ├── GOVERNANCE.md                              process rules
  └── module-a/                                  module files + code
```

### Docker invocation

```powershell
# Interactive
cd C:\Users\myeluashvili\claude-code-workspace
.\run-claude-code.ps1

# Autonomous loop
.\run-loop.ps1 -ProjectDir "C:\...\projects\my-project"
.\run-loop.ps1 -ProjectDir "C:\...\projects\my-project" -MaxIterations 10
```

### Docker flags

| Flag | Why |
|------|-----|
| `--network host` | Docker bridge NAT blocks HTTPS to Anthropic API |
| `--allowedTools "Bash(*)" "Read(*)" "Write(*)" "Edit(*)"` | Grants permissions. `--dangerously-skip-permissions` blocked for root |
| `--max-turns N` | Safety cap on tool-use cycles per invocation |
| `-w /workspace` | Sets working directory to mounted project |
| `claude` (explicit) | Image entrypoint is `exec "$@"` — no default CMD |

### What does NOT work in Docker

| Approach | Error |
|----------|-------|
| `--dangerously-skip-permissions` | Blocked: "cannot be used with root/sudo privileges" |
| `--permission-mode bypassPermissions` | Same block (treated identically) |
| `--user 1000:1000` (non-root) | `/root` inaccessible; entrypoint.sh fails |
| Default Docker networking (bridge) | API calls time out (DNS resolves, TCP blocked) |
| Passing image name without `claude` command | Entrypoint is `exec "$@"` — no default CMD |
| `claude-auth` named Docker volume | Works for auth but doesn't contain CLAUDE.md or commands/ — use .claude-config/ bind mount instead |

### Symlinks for Docker

Required to share global config between source, Docker container, and
VS Code Devmate. See the original SETUP.md in git history for full
symlink creation commands (Step 4 of original One-Time Setup).

**Fallback** if Docker doesn't follow symlinks:

```powershell
$source = "C:\Users\myeluashvili\Downloads\MY\ai dev docs\e2e"
$config = "C:\Users\myeluashvili\claude-code-workspace\.claude-config"
Copy-Item "$source\CLAUDE_GLOBAL.md" "$config\CLAUDE.md" -Force
Copy-Item "$source\COMMANDS\*" "$config\commands\" -Force
```
