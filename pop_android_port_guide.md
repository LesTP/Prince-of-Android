# Prince of Persia Android Port — Project Guide

A structured plan for porting SDLPoP (open-source Prince of Persia) to Android,
using the project as a testbed for AI-autonomous porting methodology.

This document guides preliminary research, infrastructure setup, and the creation
of formal PROJECT.md / ARCHITECTURE.md documents. It is a pre-Discovery artifact —
use it to work through open questions, then feed the results into the standard
framework.

---

## 1. Problem Statement

### What we're building

A native Android port of Prince of Persia (1989), based on SDLPoP — the
community's open-source C reimplementation of the DOS original. The port should
be playable on a phone touchscreen with gesture controls.

### Why this project

The primary goal is not the game itself — it's to **test an autonomous AI porting
pipeline**. Specifically:

- Can an AI agent (Claude Code under the From Idea to Code governance framework)
  autonomously translate C game logic to a target language, validated by a
  replay-based test oracle?
- Does the framework's Build/Refine/Explore regime model correctly predict which
  parts of porting work can run autonomously and which require human presence?
- Does the DEVPLAN/DEVLOG documentation structure give the agent enough cold-start
  context to resume effectively after escalations?

The secondary goal is a working, playable game. If the methodology works, the
game is the proof. If it doesn't, we learn where autonomous Build-regime work
breaks down on real legacy code.

### Why SDLPoP specifically

- **Source code available.** Clean C reimplementation based on the DOS disassembly,
  maintained by 32 contributors over 1,000+ commits.
- **Replay system built in.** `.P1R` files record input sequences; deterministic
  RNG via `seed=number`. This is a ready-made test oracle.
- **Bounded scope.** ~25,000 lines of port-worthy code (excluding third-party
  libraries). Small enough for one person + AI.
- **Clear Build/Refine split.** Game logic translation is machine-verifiable
  (Build). Touch control design requires human judgment (Refine). The boundary
  is sharp.
- **Prior art for the porting method.** Christopher Ehrlich's SimCity port
  (C → TypeScript, 4 days, OpenAI Codex) used the same pattern: source code +
  test oracle + autonomous agent loop. SDLPoP has comparable characteristics.

---

## 2. Codebase Research Findings

### Source: SDLPoP decomposition analysis (March 2026)

Method: RepoMapper structural index + manual bash analysis (grep-based platform
coupling, shared state surface, cross-file function calls).

### Codebase profile

| Category | Lines | Files | Notes |
|----------|------:|------:|-------|
| Game logic (seg000–seg008) | ~12,700 | 9 | Core PoP engine from disassembly |
| Platform/SDL layer (seg009) | 4,248 | 1 | Rendering, input, audio, timers |
| SDLPoP extensions | ~7,800 | 7 | Menu, replay, midi, options, etc. |
| Sequence table | 1,228 | 1 | Animation data (mostly static) |
| Third-party libs | ~7,000 | 2 | stb_vorbis, opl3 — don't port |
| Headers | ~3,600 | 5 | Types, globals (344 extern vars), protos |
| **Total (port-worthy)** | **~25,000** | | Excluding third-party |

### Layer structure (empirically derived)

The codebase is not modular in the traditional sense — every game logic file
reads and writes the same 344 global variables. However, it has a usable
**layered** structure based on platform coupling:

**Layer 1 — Pure game logic (zero SDL calls, ~7,200 lines):**
seg002 (guard AI, room transitions), seg004 (collision detection), seg005
(player control, sword fighting), seg006 (character physics, tile queries),
seg007 (animated tiles, traps, triggers, mobs). These files are tightly coupled
to each other through shared globals but collectively form a self-contained
simulation with no platform dependencies.

**Layer 2 — Game loop + level management (~3,300 lines, ~95 SDL calls):**
seg000 (main loop, level loading, save/load, input reading), seg001
(cutscenes, title screen), seg003 (play_level loop, collision orchestration).
These wire Layer 1 together and add SDL-dependent orchestration.

**Layer 3 — Rendering (~2,800 lines, 15+ SDL calls):**
seg008 (room drawing, tile rendering, sprite compositing), lighting.c.

**Layer 4 — Platform abstraction (~4,250 lines, 506 SDL calls):**
seg009. All SDL primitives. This is the replacement target, not a translation
target.

**Layer 5 — SDLPoP extensions (~7,800 lines, variable SDL):**
menu.c, replay.c, midi.c, options.c, screenshot.c, seqtbl.c.

### Key finding: the 344-global coupling

All game logic files share mutable state through data.h. Core objects: `Char`
(active character struct — 15 fields), `Kid`, `Guard` (character instances),
`level` (full level data), `drawn_room`, `curr_room_tiles`, `current_level`,
`rem_min`, `rem_tick`. This means the game logic layer must be ported as a unit,
not decomposed further.

### Key finding: SDL-free game logic core

seg002, seg004, seg005, seg006, seg007 have **zero** SDL calls. This was the
single most valuable finding from the analysis. It means ~7,200 lines of pure C
game logic can be translated without touching the platform layer.

---

## 3. Blocking Open Questions

These must be resolved before committing to the project. Each maps to a
time-boxed spike (Explore regime).

### Q1: Does the replay oracle produce deterministic state traces?

**Why it blocks:** The entire autonomous porting pipeline depends on comparing
game state between the original and the port, frame by frame. If SDLPoP's
replay playback is non-deterministic, the oracle doesn't work.

**Spike to resolve:**
1. Add ~50 lines to SDLPoP: a `dump_frame_state()` function that writes
   Char/Kid/Guard structs + key scalar globals to a binary file each frame.
2. Insert the call in `play_frame()` (seg000.c line 863), after all game logic
   updates and before `draw_game_frame()`.
3. Run a replay twice with the same `seed=N`, produce two state traces.
4. Binary diff the traces. If identical, the oracle works.
5. Repeat with replays exercising: basic movement, sword fighting, traps
   (chompers/spikes/loose floors), level transitions, guard AI.

**State to capture per frame:**
- `Char` struct (15 fields: frame, x, y, direction, curr_col, curr_row,
  action, fall_x, fall_y, room, repeat, charid, sword, alive, curr_seq)
- `Kid` and `Guard` (same struct)
- Scalars: `current_level`, `drawn_room`, `rem_min`, `rem_tick`,
  `hitp_curr`, `hitp_max`, `guardhp_curr`, `guardhp_max`
- Level tile state: `curr_room_tiles[30]`, `curr_room_modif[30]`
- `trobs` and `mobs` arrays (animated tile and falling object state)

**Prerequisite:** SDLPoP must build and run locally. Needs SDL2 dev libraries.

**Expected outcome:** Likely deterministic — PoP's game logic is all integer
math (from 8-bit assembly origins), and the `seed=` flag exists precisely for
reproducible runs. But "likely" isn't "confirmed."

### Q2: What is the target language?

**Why it blocks:** The language choice shapes the entire porting pipeline —
what the agent translates to, what the test harness is written in, how the
platform layer works.

**Options to evaluate:**

| Target | Pros | Cons |
|--------|------|------|
| **C (NDK cross-compile)** | Near-zero translation; SDL2 supports Android | Not a real porting test; minimal autonomy exercise |
| **Kotlin/JVM** | Native Android; good tooling | Integer overflow semantics differ from C; JVM game loop perf |
| **Rust + NDK** | Close to C semantics; safe; good Android NDK story | Harder translation; borrow checker friction |
| **TypeScript (WebView)** | Ehrlich precedent; fast iteration | Extra indirection on Android; number type issues |
| **C → C with SDL2-Android** | Ship fastest; SDL2 has Android backend | Doesn't test the methodology at all |

**Spike to resolve:** This is a design decision, not a technical spike. Evaluate
against the dual goal: (1) does it test the autonomous porting methodology
meaningfully, and (2) can it produce a good Android game? Kotlin is probably the
sweet spot — different enough from C to be a real translation exercise, native
enough on Android to produce a good result.

The integer overflow question is the specific technical risk: PoP's logic uses
byte/short/word arithmetic where overflow wraps silently. Kotlin's signed byte
overflow is defined (wraps), but mixing signed/unsigned operations needs care.
A small spike: translate seg004 (collision detection, 621 lines, pure game logic)
to the candidate language and run a replay comparison. If it matches, the
language works.

### Q3: Can the modifier-button gesture scheme achieve acceptable game feel?

**Why it blocks:** If touch controls aren't usable, there's no point in the port.

**Not a spike — this is Refine work.** But it needs a minimum viability check
before investing in the full porting pipeline.

**Proposed control model:**
- Directional gestures on the main touch surface (swipe/hold for movement,
  swipe up/down for jump/crouch, diagonals for hop and directional jump)
- One persistent modifier button (translucent, positioned in resting thumb
  zone) that replicates Shift: hold during fall to grab ledge, hold + direction
  for careful step, tap near item to pick up
- Sword fighting mode: left/right for advance/retreat, modifier tap for strike,
  swipe up for block, swipe down to sheathe
- Prior art: Oddmar (hybrid gesture split, same genre, good reviews on Android)

**Minimum viability check:** Build a control prototype *without* the game —
a simple test app that displays the gesture/modifier inputs being recognized,
overlaid on a static PoP screenshot. Playtest the gesture vocabulary for
reachability, timing, and conflict (does swipe-up-to-jump interfere with
the modifier hold?). This can be built in an afternoon and doesn't require
any porting work.

---

## 4. Preparation Sequence

Work through these in order. Each produces artifacts that feed the next.
Everything before Phase 3 is preparation — the autonomous porting pipeline
doesn't start until Phase 3 is set up.

### Phase 0: Environment Setup

**Regime:** Build (mechanical, verifiable)

- [ ] Clone SDLPoP, install SDL2 dev libraries, confirm it builds and runs
- [ ] Record or obtain test replays exercising key mechanics (movement, combat,
      traps, level transitions)
- [ ] Confirm replays play back correctly with `seed=N`

**Output:** Working SDLPoP build with a set of test replays.

### Phase 1: Replay Oracle Spike (resolves Q1)

**Regime:** Explore (time-boxed, output is a decision)

**Time box:** One session.

- [ ] Instrument SDLPoP with `dump_frame_state()` (see Q1 details above)
- [ ] Run replay twice with same seed, binary diff traces
- [ ] Test with multiple replay types (movement, combat, traps, transitions)
- [ ] Document: deterministic? If not, what's the source of non-determinism?

**Output:** Closed decision on oracle viability + instrumentation code if it works.

**Gate:** If the oracle doesn't work, reassess the entire project. Investigate
the non-determinism source and whether it's fixable, or whether a fuzzier
comparison (approximate state matching) could work.

### Phase 2: Target Language Decision (resolves Q2)

**Regime:** Explore (time-boxed, output is a decision)

**Time box:** One session.

- [ ] Choose candidate language (recommendation: Kotlin)
- [ ] Translate seg004 (collision detection, 621 lines) manually or with AI assist
- [ ] Wire it to the replay oracle: feed the same inputs, compare state output
      against the C original's state trace
- [ ] Document: does the translation preserve behavior exactly? What caused
      mismatches (if any)?

**Output:** Closed decision on target language + a validated translation of one
file as proof of concept.

**Gate:** If the translated seg004 can't reproduce the original's state trace,
investigate why. Integer semantics? Missing type? If unfixable in the candidate
language, try another.

### Phase 3: Test Infrastructure

**Regime:** Build

This is the real investment before autonomous work begins. The test
infrastructure is the foundation the entire pipeline runs on.

- [ ] **State trace comparator:** A tool that takes two state trace files and
      reports the first divergence (frame number, which field, expected vs actual).
      Not just binary diff — structured comparison with human-readable output.
- [ ] **Replay runner for the port:** The equivalent of SDLPoP's replay playback,
      but for the ported code. Feeds recorded inputs into the ported game logic
      and produces a state trace. This is the core of the oracle.
- [ ] **Regression suite:** A set of replays that collectively exercise all major
      game mechanics, with reference state traces generated from the original.
      Replays needed: basic walk/run/turn, running jumps, climbing, falling +
      grab, loose floors, chompers, spikes, sword fighting (multiple guard types),
      level transitions (at least levels 1→2→3), potion pickups, gate triggers.
- [ ] **CI-style runner (optional but valuable):** Script that runs the full
      regression suite and reports pass/fail per replay.

**Output:** Working test infrastructure that can validate any portion of the
ported code against the original.

### Phase 4: Control Prototype (resolves Q3)

**Regime:** Refine (human evaluation required)

Can run in parallel with Phases 1–3.

- [ ] Build a minimal Android test app: static PoP screenshot as background,
      gesture recognizer + modifier button overlaid, visual feedback showing
      which input is being recognized
- [ ] Playtest the gesture vocabulary: reachability, conflict detection,
      modifier hold + directional gesture concurrency
- [ ] Iterate on button size/position, swipe thresholds, dead zones
- [ ] Document the control spec (gesture → game input mapping)

**Output:** Validated control scheme + control spec document.

**Gate:** If the gesture model doesn't feel viable after iteration, consider
alternatives (virtual D-pad + modifier, or scope the project to controller-only).

### Phase 5: Formal Project Definition

**Regime:** Discovery (standard framework Phase 1)

With Q1–Q3 resolved and infrastructure in place, create the formal project
documents:

- [ ] **PROJECT.md** — scope, audience, constraints, success criteria (use the
      Discovery template; most of the content exists in this guide already)
- [ ] **ARCHITECTURE.md** — component map based on the layer structure from
      Section 2, adapted for the target language; implementation sequence;
      interface contracts between layers
- [ ] **ARCH files** — per-layer contracts (game logic layer, platform layer,
      control layer, integration layer)
- [ ] **CLAUDE.md** — project-specific governance for Claude Code sessions

**Output:** Full project documentation stack, ready for Phase 3 (Implementation)
of the framework.

---

## 5. Autonomous Porting Pipeline

This is the methodology being tested. It only begins after Phases 0–5 are
complete.

### What runs autonomously (Build regime)

**Layer 1 translation — pure game logic (~7,200 lines):**

The agent receives: the C source file(s), the target language, the state trace
comparator, and the reference traces. It translates, runs the regression suite,
iterates on failures. Human reviews checkpoints but doesn't steer.

This is the core autonomy test. Expected autonomous unit: one file at a time
(seg002, seg004, seg005, seg006, seg007), validated after each.

However, because all files share global state through data.h, the testing is
integrated — translating seg005 in isolation doesn't produce a testable unit
until the state model and at least the functions it calls from other seg files
exist. The practical sequence is:

1. Port the state model (types.h, data.h equivalents) — mechanical, low risk
2. Port seg006 first (character physics, tile queries — the most-called file)
3. Port seg004 (collision detection — depends on seg006)
4. Port seg005 (player control — depends on seg006, seg004)
5. Port seg002 (guard AI — depends on seg005, seg006)
6. Port seg007 (traps/triggers — depends on seg006)
7. Integration test: run full regression suite against the combined game logic

At each step, the agent runs the available replays and compares against reference
traces. Early files may only be testable through integration with the original C
code for the not-yet-ported files (C-to-target bridge), or testing may wait until
a critical mass of files is ported.

**Escalation triggers:**
- State trace divergence after two different fix attempts
- A decision needed that isn't in the DEVPLAN (architectural choice, ambiguous C semantics)
- Scope expansion (discovering a dependency not mapped in the analysis)
- Any Refine-regime work encountered

**Sequence table (seqtbl.c):** Pure data translation, near-zero risk. Can be
autonomous at any point.

**Layer 4 — Platform abstraction:** This is a *rewrite*, not a translation. The
interface is defined (proto.h lines 520–660), but the implementation is
target-platform-specific (Android rendering, input, audio). This is Build regime
(the interface contract is the spec) but lower autonomy — platform quirks will
cause unexpected escalations.

### What requires human presence (Refine regime)

**Touch controls:** Gesture recognition tuning, modifier button positioning,
swipe threshold calibration, dead zone adjustment. Every iteration needs a
human holding the phone and playing. The control prototype (Phase 4) is the
first Refine cycle; the in-game control integration is the second.

**Game feel verification:** Even if the state traces match perfectly, the
game might not *feel* right — animation timing, screen transitions, audio sync.
These are human-evaluable. Plan a dedicated Refine phase after the core port
is functional.

**Menu/UI adaptation:** SDLPoP's pause menu (3,200 lines) is heavily
SDL-dependent and designed for keyboard/mouse. The Android version needs a
touch-native menu. This is Refine work — design, not translation.

### What's out of scope

- Multiplayer/co-op (Ehrlich's SimCity goal, but not ours)
- AI agents playing PoP (interesting but separate project)
- Mod support (SDLPoP has extensive mod support; defer for Android)
- Screenshot/level-map features (SDLPoP-specific developer tools)

---

## 6. Risk Register

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Replay oracle non-deterministic | Project-blocking | Low | Phase 1 spike; integer-only game logic is favorable |
| Target language integer semantics mismatch | Delays, subtle bugs | Medium | Phase 2 spike with seg004; choose language with C-like overflow |
| Touch controls don't achieve acceptable feel | Port works but isn't playable | Medium | Phase 4 prototype; Oddmar precedent suggests viability |
| Global state coupling prevents incremental testing | Slows autonomous pipeline | High | Accept unit-of-translation = single file, unit-of-testing = integrated layer; build C-to-target bridge for incremental validation |
| Agent produces confidently wrong code that passes some replays | Silent correctness bugs | Medium | Diverse regression suite; replays must cover all mechanics; add replays when bugs are found |
| Android frame pacing affects perceived control quality | Controls feel bad despite correct logic | Medium | Use Android frame pacing libraries; test on multiple devices; address in platform layer implementation |
| SDLPoP seg000 (main loop) is too entangled to port cleanly | Requires refactoring before translation | High | Accept that seg000 needs manual refactoring to separate SDL calls from game logic; this is human-guided Build work, not autonomous |

---

## 7. Success Criteria

### For the methodology test (primary goal)

- The autonomous agent successfully translates Layer 1 game logic with replay
  oracle validation, requiring fewer than N escalations per 1,000 lines
  (establish baseline during Phase 2 spike)
- The DEVPLAN/DEVLOG structure enables cold-start resumption after every
  escalation without loss of context
- The Build/Refine regime boundary prediction is validated: autonomous work
  succeeds for game logic, fails or requires human presence for controls/feel

### For the game (secondary goal)

- All 14 levels are completable on an Android phone using touch controls
- Replay regression suite passes (all reference traces matched)
- Control scheme receives positive feedback from at least 3 playtesters
  (not just the developer)
- Frame pacing is consistent (no perceptible input lag or stutter)

---

## 8. Reference Materials

- **SDLPoP repository:** https://github.com/NagyD/SDLPoP
- **SDLPoP decomposition analysis:** sdlpop_decomposition_analysis.md
- **Touch control research:** Touch-Screen_Control.md
- **SimCity porting precedent:** Ehrlich/Codex port (Garry's List, Feb 2026)
- **PoP disassembly:** https://forum.princed.org/viewtopic.php?f=68&t=3423
- **Jordan Mechner's Apple II source:** https://github.com/jmechner/Prince-of-Persia-Apple-II
- **Oddmar (control scheme precedent):** Google Play, gesture split reviews
- **Dead Cells mobile (control customization precedent):** Playdigious developer writeup
- **Brownfield analysis method:** BROWNFIELD_TEMPLATE_SKELETON.md (revised March 2026)
