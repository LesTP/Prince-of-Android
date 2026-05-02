# PROJECT — Prince of Persia Android Port

## Scope

### What we're building

A native Android port of Prince of Persia (1989), based on SDLPoP — the community's open-source C reimplementation of the DOS original. The game logic (~7,200 lines of pure C) is translated to Kotlin, validated frame-by-frame against the original using a replay-based test oracle. The port runs on Android phones with gesture-based touch controls.

### Primary goal: test autonomous AI porting methodology

Can an AI agent (Claude Code under the From Idea to Code governance framework) autonomously translate C game logic to Kotlin, validated by deterministic replay comparison?

Specific questions being tested:
1. **Autonomy scope:** Does the Build/Refine regime model correctly predict which porting work can run autonomously (game logic translation) and which requires human presence (touch controls, game feel)?
2. **Cold-start resilience:** Does the DEVPLAN/DEVLOG documentation structure give the agent enough context to resume effectively after escalations, across sessions with no shared memory?
3. **Escalation rate:** How many escalations per 1,000 lines of translated code? (Baseline to be established during Layer 1 translation.)

### Secondary goal: working, playable game

If the methodology works, the game is the proof. All 14 levels completable on an Android phone with touch controls.

### Why SDLPoP

- **Source available.** Clean C reimplementation from DOS disassembly, 32 contributors, 1,000+ commits.
- **Built-in test oracle.** `.P1R` replay files + deterministic RNG (`seed=N`) = frame-exact behavioral comparison.
- **Bounded scope.** ~25,000 lines port-worthy (excluding third-party). One person + AI.
- **Sharp Build/Refine boundary.** Game logic = machine-verifiable. Touch controls = human-evaluable. No ambiguity.
- **Prior art.** Ehrlich's SimCity port (C → TypeScript, 4 days, OpenAI Codex) used the same pattern. SDLPoP has comparable characteristics.

---

## Audience

1. **The developer** (methodology tester) — primary. Evaluating whether the governance framework and autonomous loop can handle real legacy code.
2. **Playtesters** — secondary. 3+ people testing the finished port on Android phones for game feel and control usability.
3. **Methodology observers** — tertiary. Anyone interested in AI-assisted porting of legacy C codebases.

The port is not intended for public distribution (SDLPoP's GPL license requires source release; game assets are proprietary to Broderbund/Ubisoft).

---

## Constraints

### Technical

- **Source codebase:** SDLPoP v1.24 RC. ~25,000 lines of C. 344 global variables shared across all game logic files (tight coupling through `data.h`).
- **Target language:** Kotlin (JVM 17, Android-compatible). Decided Phase 2 — native Android, predictable integer overflow semantics, explicit type conversions.
- **Test oracle:** Replay-based deterministic state comparison. Confirmed working in Phase 1 — binary-identical traces across runs with same seed. 13 reference traces covering movement, combat, traps, level transitions, and edge cases.
- **Translation unit:** One C source file at a time. Testing unit: integrated Layer 1 (all files share global state, so individual files aren't independently testable).
- **Platform layer:** Rewrite, not translate. SDL2 calls replaced with Android equivalents (Canvas/OpenGL, touch input, MediaPlayer/SoundPool).

### Environmental

- **Sandboxie:** Corporate sandbox blocks shell execution from Claude CLI (`ConsoleInit` not implemented). Autonomous loop limited to read/write; shell tasks (builds, git, trace generation) require Devmate or manual execution.
- **Chocolatey:** Corporate proxy has limited approved packages. JDK 17 available as `openjdk17`; Gradle requires manual install.
- **Build toolchain:** MSYS2/MinGW for C builds, Gradle 9.4.0 + JDK 17 for Kotlin, Python 3 for trace tools.

### Process

- **Governance:** From Idea to Code framework. Build/Refine/Explore regimes. DEVPLAN/DEVLOG per module.
- **Autonomy:** Continuous autonomy for Build-regime work. Escalate on 3 consecutive failures, regime shifts, scope expansion, contract changes.
- **Commit discipline:** One commit per logical unit. Human confirms phase completion.

---

## Codebase Structure

### Layer model (empirically derived from SDLPoP decomposition analysis)

| Layer | Content | Lines | SDL Calls | Port Strategy |
|-------|---------|------:|----------:|---------------|
| 1 | Pure game logic (seg002, seg004, seg005, seg006, seg007) | ~7,200 | 0 | Autonomous C→Kotlin translation |
| 2 | Game loop + level management (seg000, seg001, seg003) | ~3,300 | ~95 | Manual refactoring to separate SDL, then translate |
| 3 | Rendering (seg008, lighting.c) | ~2,800 | 15+ | Rewrite for Android Canvas/OpenGL |
| 4 | Platform abstraction (seg009) | ~4,250 | 506 | Rewrite for Android APIs |
| 5 | Extensions (menu, replay, midi, options, screenshot, seqtbl) | ~7,800 | varies | Selective: replay + seqtbl translate, menu rewrite, others defer |

### Key architectural fact

All game logic files share mutable state through 344 global variables in `data.h`. Core objects: `Char` (16-byte character struct), `Kid`, `Guard`, `level`, `drawn_room`, `curr_room_tiles`, `current_level`, `rem_min`, `rem_tick`. The game logic layer must be ported as a unit — it cannot be decomposed further.

### Translation sequence (Layer 1)

Ordered by call graph dependencies:

1. State model (`types.h`, `data.h` equivalents) — mechanical, low risk
2. `seg006` — character physics, tile queries (most-called file)
3. `seg004` — collision detection (depends on seg006)
4. `seg005` — player control, sword fighting (depends on seg006, seg004)
5. `seg002` — guard AI, room transitions (depends on seg005, seg006)
6. `seg007` — animated tiles, traps, triggers (depends on seg006)
7. Integration test: full regression suite against combined game logic

---

## Success Criteria

### Methodology (primary)

| Criterion | Measurement | Target |
|-----------|-------------|--------|
| Autonomous translation of Layer 1 | Escalations per 1,000 lines | Establish baseline (no prior data) |
| Cold-start resumption | Agent correctly resumes after each escalation without lost context | 100% (any failure = framework bug) |
| Regime prediction accuracy | Build-regime work succeeds autonomously; Refine-regime work correctly requires human | Qualitative assessment at project end |

### Game (secondary)

| Criterion | Measurement | Target |
|-----------|-------------|--------|
| Completability | All 14 levels completable on Android phone | 14/14 |
| Replay regression | All reference traces matched by Kotlin port | 13/13 traces |
| Control usability | Positive feedback from playtesters | ≥3 playtesters |
| Frame pacing | No perceptible input lag or stutter | Consistent 60fps |

---

## Risk Register

| Risk | Impact | Likelihood | Mitigation | Status |
|------|--------|------------|------------|--------|
| Replay oracle non-deterministic | Project-blocking | ~~Low~~ | ~~Phase 1 spike~~ | **Closed — oracle works (Phase 1)** |
| Target language integer semantics mismatch | Delays, subtle bugs | ~~Medium~~ | ~~Phase 2 spike~~ | **Mitigated — Kotlin chosen, replay oracle validates (Phase 2)** |
| Touch controls don't achieve acceptable feel | Port works but isn't playable | Medium | Phase 4 prototype; Oddmar precedent | **Open — Q3 unresolved** |
| Global state coupling prevents incremental testing | Slows autonomous pipeline | High | Accept unit-of-translation = single file, unit-of-testing = integrated layer | **Closed — Layer 1 translated with 573 tests, 0 escalations (Modules 8-12)** |
| Agent produces wrong code that passes some replays | Silent correctness bugs | Medium | 13-replay regression suite covering 5 mechanics categories | Open |
| Rendering bugs undetected by game logic oracle | Silent visual regressions, no automated regression net | Medium | Tiered validation: golden screenshots → render command assertions → manual inspection (see ARCHITECTURE.md Layer 3 contract) | Open |
| Android frame pacing affects control quality | Controls feel bad despite correct logic | Medium | Use Android frame pacing libraries; test on multiple devices | Open |
| seg000 (main loop) too entangled to port cleanly | Requires refactoring before translation | High | Accept manual refactoring to separate SDL calls from game logic | **Closed — seg000/seg001/seg003 translated in Module 15, headless shim pattern (8/13 traces)** |

---

## Out of Scope

- Multiplayer/co-op
- AI agents playing PoP
- Mod support (SDLPoP has extensive mod support; defer for Android)
- Screenshot/level-map developer tools
- Public distribution (license constraints)

---

## Resolved Blocking Questions

| Question | Resolution | Phase |
|----------|-----------|-------|
| Q1: Does the replay oracle produce deterministic state traces? | **Yes.** Binary-identical traces across runs with same seed. Integer-only game logic is inherently deterministic. | Phase 1 |
| Q2: What is the target language? | **Kotlin.** Native Android, predictable integer semantics (signed types wrap on overflow like C), explicit conversions. Validation deferred to first file translation — oracle catches mismatches immediately. | Phase 2 |
| Q3: Can the modifier-button gesture scheme achieve acceptable game feel? | **Unresolved.** Requires Phase 4 control prototype (Refine regime). | Pending |

---

## Revision Protocol

**Core scope** (changes require pausing implementation):
- Primary goal (methodology test)
- Target language
- Test oracle approach
- Layer model

**Flexible scope** (changes can proceed inline):
- Specific replay coverage
- Kotlin project structure details
- Tool choices (Python vs other for comparator)
- Translation sequence within Layer 1
- Control scheme design (Refine — expected to evolve)

---

## References

- SDLPoP repository: https://github.com/NagyD/SDLPoP
- SDLPoP decomposition analysis: `pop_android_port_guide.md` §2
- SimCity porting precedent: Ehrlich/Codex port (Garry's List, Feb 2026)
- PoP disassembly: https://forum.princed.org/viewtopic.php?f=68&t=3423
- Jordan Mechner's Apple II source: https://github.com/jmechner/Prince-of-Persia-Apple-II
- Oddmar (control scheme precedent): Google Play, gesture split reviews
