# ARCHITECTURE — Prince of Persia Android Port

## Status

**Project phase:** Phase 5 — Formal Project Definition (in progress)

Preparation phases 0-3 complete. Q1 (oracle) and Q2 (language) resolved.
Q3 (touch controls) pending Phase 4 prototype.

---

## Component Map

```
┌─────────────────────────────────────────────────────────────┐
│                     Android Application                      │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────────┐  │
│  │  Control      │  │  Platform     │  │  Rendering        │  │
│  │  (touch input)│  │  (Android API)│  │  (Canvas/OpenGL)  │  │
│  │  [NEW]        │  │  [REWRITE]    │  │  [REWRITE]        │  │
│  └──────┬───────┘  └──────┬───────┘  └────────┬──────────┘  │
│         │                 │                    │              │
│         ▼                 ▼                    ▲              │
│  ┌──────────────────────────────────────────────────────┐    │
│  │  Game Loop (seg000, seg001, seg003)                   │    │
│  │  [REFACTOR + TRANSLATE]                               │    │
│  │  Orchestrates: input → game logic → render → repeat   │    │
│  └──────────────────────┬───────────────────────────────┘    │
│                         │                                    │
│                         ▼                                    │
│  ┌──────────────────────────────────────────────────────┐    │
│  │  Game Logic (seg002, seg004, seg005, seg006, seg007)  │    │
│  │  [TRANSLATE C→Kotlin — autonomous]                    │    │
│  │  Zero SDL calls. Pure simulation.                     │    │
│  └──────────────────────┬───────────────────────────────┘    │
│                         │                                    │
│                         ▼                                    │
│  ┌──────────────────────────────────────────────────────┐    │
│  │  State Model (types.h, data.h → Kotlin)               │    │
│  │  344 global variables, char_type, level data           │    │
│  │  [TRANSLATE — mechanical]                              │    │
│  └──────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐                          │
│  │  Replay       │  │  Sequence     │                         │
│  │  (P1R parser) │  │  Table        │                         │
│  │  [DONE]       │  │  [TRANSLATE]  │                         │
│  └──────────────┘  └──────────────┘                          │
└─────────────────────────────────────────────────────────────┘

Test Oracle (external):
  C reference build ──→ state_trace.bin (310 bytes/frame)
  Kotlin port       ──→ state_trace.bin
  compare_traces.py ──→ MATCH / DIVERGENCE at frame N, field F
```

---

## Data Flow

### Per-frame game loop

```
1. Read input      ← Control layer (touch → direction/shift/special)
2. play_frame()    → Game Logic mutates shared state
   ├── play_kid_frame()    seg006 → seg005 → seg004
   ├── play_guard_frame()  seg006 → seg002
   ├── process_trobs()     seg007 (animated tiles)
   └── do_mobs()           seg007 (falling objects)
3. draw_game_frame() → Rendering reads state, draws frame
4. play_next_sound() → Platform plays queued audio
5. Timing            → Platform enforces frame pacing
```

### Shared state (the 344 globals)

All game logic functions read and write the same global variables. There is no encapsulation — this is a direct consequence of the 8-bit assembly origins. Key state objects:

| Variable | Type | Size | Description |
|----------|------|------|-------------|
| `Kid` | char_type | 16 bytes | Player character |
| `Guard` | char_type | 16 bytes | Active enemy |
| `Char` | char_type | 16 bytes | Currently-processed character |
| `level` | level_type | ~2 KB | Full level tile/link data |
| `curr_room_tiles[30]` | byte[] | 30 bytes | Current room tile types |
| `curr_room_modif[30]` | byte[] | 30 bytes | Current room tile modifiers |
| `trobs[30]` | trob_type[] | 90 bytes | Animated tiles (chompers, doors, etc.) |
| `mobs[14]` | mob_type[] | 84 bytes | Falling objects (loose floors) |
| `current_level` | word | 2 bytes | Level number (1-14) |
| `drawn_room` | word | 2 bytes | Currently visible room |
| `rem_min`, `rem_tick` | short, word | 4 bytes | Game timer |
| `random_seed` | dword | 4 bytes | RNG state (deterministic) |

The state model is the foundation. Every other module depends on it. It must be translated first and verified by the replay oracle.

---

## Layer Contracts

### State Model

**Port strategy:** Translate (mechanical)

**Provides to all layers:**
- Kotlin equivalents of `char_type` (16 bytes), `trob_type` (3 bytes), `mob_type` (6 bytes), `level_type`
- Kotlin object holding all 344 global variables (mutable properties)
- Type aliases: `byte` → `UByte` or `Int`, `word` → `Int`, `sbyte` → `Byte`, `dword` → `Long`

**Constraints:**
- Integer overflow semantics must match C: signed types wrap, conversions are explicit
- Struct field layout must match C for replay oracle comparison (field order, sizes)
- No constructor logic — globals are initialized by level loading, not construction

**Source files:** `types.h`, `data.h`, `data.c`

### Layer 1 — Game Logic

**Port strategy:** Translate C→Kotlin (autonomous, validated by replay oracle)

**Provides:**
- ~180 functions across 5 files (see proto.h: seg002, seg004, seg005, seg006, seg007)
- All character movement, collision, combat, AI, trap, and trigger logic

**Consumes:**
- State model (reads and writes globals directly)
- Control input variables: `control_x` (-1/0/+1), `control_y` (-1/0/+1), `control_shift` (0/1)

**Contract:**
- Zero platform calls. No SDL, no Android, no I/O.
- Given identical state + identical input → produces identical output state (deterministic)
- Validated by: replay oracle (compare `state_trace.bin` frame-by-frame)

**Source files and translation order** (by call-graph dependency):

| Order | File | Lines | Role | Depends on |
|-------|------|------:|------|------------|
| 1 | seg006.c | ~1,800 | Character physics, tile queries, frame loading | State model |
| 2 | seg004.c | ~620 | Collision detection, wall/obstacle checks | seg006 |
| 3 | seg005.c | ~1,600 | Player control, sword fighting, falling | seg006, seg004 |
| 4 | seg002.c | ~1,800 | Guard AI, room transitions, auto-control | seg005, seg006 |
| 5 | seg007.c | ~1,400 | Animated tiles, traps, triggers, mobs | seg006 |

**Escalation triggers:**
- State trace divergence after two different fix attempts
- Ambiguous C semantics (undefined behavior, platform-specific)
- Undiscovered dependency on Layer 2/3/4

### Layer 2 — Game Loop

**Port strategy:** Refactor to remove SDL calls, then translate

**Provides:**
- `play_frame()` — single frame tick (the orchestrator)
- `play_level()` / `init_game()` — level lifecycle
- Level loading, save/load
- Cutscene playback (seg001)
- Timer management

**Consumes:**
- Layer 1 (game logic functions)
- Layer 3 (rendering: `draw_game_frame()`)
- Layer 4 (platform: input, timing, sound)
- State model

**Contract:**
- Calls Layer 1 functions in correct order per frame
- Reads platform input, writes to control variables that Layer 1 reads
- Calls rendering after game logic completes
- Manages level transitions, game-over, win conditions

**Complication:** seg000 is heavily entangled with SDL (~95 calls). Must be manually refactored to separate game logic from platform calls before translation.

**Source files:** `seg000.c` (~2,200 lines), `seg001.c` (~800 lines), `seg003.c` (~500 lines)

### Layer 3 — Rendering

**Port strategy:** Rewrite for Android

**Provides:**
- Room drawing (tiles, backgrounds, overlays)
- Character sprite compositing
- Animation frame rendering
- Screen transitions, flashes

**Consumes:**
- State model (room data, character positions, tile states)
- Asset data (chtab sprite sheets, palette data)

**Contract:**
- Reads game state, produces visual frame
- No game logic side effects (read-only access to state)
- Must handle PoP's specific rendering quirks: object draw order tables, peel system, wipe system

**Validation (tiered — try in order, escalate if insufficient):**

1. **Golden screenshot comparison** (first attempt). Capture reference screenshots from the C build at key frames during replay playback (every Nth frame, plus level transitions, combat, trap activations). Compare against Kotlin renderer output using pixel-diff tooling. Build a visual regression suite (~20–30 key frames across levels). Replays drive both builds identically (same inputs, same state), so frame selection is deterministic.
   - *Catches:* gross rendering errors, wrong sprites, missing tiles, broken draw order
   - *Limitation:* fragile under resolution/scaling/anti-aliasing differences; may need fuzzy matching or cropped region comparison

2. **Render command assertions** (if screenshots prove too fragile). Capture an intermediate "render command list" from both C and Kotlin renderers — what to draw, in what order, at what position, with what sprite ID. Compare command lists like state traces. This validates rendering *intent* independent of pixel output.
   - *Catches:* draw order bugs, wrong sprite selection, incorrect compositing
   - *Requires:* instrumentation of both C and Kotlin renderers to emit command logs

3. **Manual visual inspection** (always required as final pass). Refine regime — human plays through levels, compares against DOS/C original. Required regardless of automated coverage, but automated tiers reduce the surface area the human must inspect.

**Regression safety:** Whichever tier works, wire it into the replay regression suite so the 13 replays protect rendering as well as logic. Without this, rendering changes have no regression net.

**Source files:** `seg008.c` (~2,200 lines), `lighting.c` (~600 lines)

### Layer 4 — Platform

**Port strategy:** Rewrite for Android APIs

**Provides:**
- Display: surface/window management, frame presentation
- Input: keyboard/touch event handling → control variables
- Audio: sound effect playback, music
- Timing: frame pacing, delay functions
- File I/O: level data loading, save files

**Consumes:**
- Android APIs (Activity lifecycle, Canvas/SurfaceView/OpenGL, MediaPlayer, SoundPool)

**Contract (interface defined by proto.h lines 520-660):**
- `init_*` — initialize subsystems
- `process_events` — poll input
- `request_screen_update` — present frame
- `play_sound` / `stop_sounds` — audio
- `idle` / `delay_ticks` — timing

**Source files:** `seg009.c` (~4,250 lines) — replacement target, not translated

### Control Layer (new for Android)

**Port strategy:** New implementation

**Provides:**
- Touch gesture recognition (swipe direction, hold, tap)
- Modifier button (replaces Shift key)
- Game input state: `control_x`, `control_y`, `control_shift`, `control_forward`, `control_backward`

**Consumes:**
- Android MotionEvent stream
- Game mode context (exploration vs. sword fighting — different gesture mappings)

**Contract:**
- Output: same control variable format that Layer 1/2 read from keyboard input
- Must handle: directional swipes, modifier hold + direction combos, sword fighting gestures
- Latency: gesture recognition must complete within 1-2 frames (16-32ms at 60fps)

**Validation:** Refine regime — human playtesting. No automated oracle.

### Extensions (selective)

| File | Lines | Strategy | Priority |
|------|------:|----------|----------|
| seqtbl.c | 1,228 | Translate (pure data) | High — needed for animation |
| replay.c | ~1,500 | Translate (P1R parser done, need replay runner) | High — needed for oracle |
| menu.c | ~3,200 | Rewrite for Android touch UI | Medium |
| midi.c | ~800 | Rewrite for Android audio | Low |
| options.c | ~700 | Translate + adapt for Android prefs | Low |
| screenshot.c | ~700 | Skip (developer tool) | N/A |

---

## Implementation Sequence

### Preparation (complete)

| # | Phase | Status |
|---|-------|--------|
| 0 | Environment Setup | **Complete** |
| 1 | Replay Oracle | **Complete** — deterministic ✅ |
| 2 | Target Language | **Complete** — Kotlin |
| 3 | Test Infrastructure | **Complete** — 13 traces, comparator, P1R parser |
| 4 | Control Prototype | Pending (Refine — parallel track) |
| 5 | Formal Project Def | **In progress** |

### Porting (Build regime — autonomous where indicated)

| # | Module | Description | Regime | Depends on | Status |
|---|--------|-------------|--------|------------|--------|
| 6 | State Model | types.h + data.h → Kotlin | Build (autonomous) | — | **Complete** |
| 7 | Sequence Table | seqtbl.c → Kotlin data | Build (autonomous) | State Model | Pending |
| 8 | Layer 1: seg006 | Character physics, tile queries | Build (autonomous) | State Model | Pending |
| 9 | Layer 1: seg004 | Collision detection | Build (autonomous) | seg006 | Pending |
| 10 | Layer 1: seg005 | Player control, sword fighting | Build (autonomous) | seg006, seg004 | Pending |
| 11 | Layer 1: seg002 | Guard AI, room transitions | Build (autonomous) | seg005, seg006 | Pending |
| 12 | Layer 1: seg007 | Traps, triggers, animated tiles | Build (autonomous) | seg006 | Pending |
| 13 | Layer 1 Integration | Full regression suite on combined game logic | Build | Modules 8-12 | Pending |
| 14 | Replay Runner | Kotlin replay playback + state trace writer | Build | Modules 6-13 | Pending |
| 15 | Game Loop | seg000/001/003 refactor + translate | Build (semi-auto) | Modules 6-14 | Pending |
| 16 | Rendering | seg008 + lighting → Android | Build | Game Loop | Pending |
| 17 | Platform | seg009 → Android APIs | Build | — | Pending |
| 18 | Audio | Sound + music → Android | Build | Platform | Pending |
| 19 | Controls | Touch gesture system | Refine | Platform | Pending |
| 20 | Integration | Wire all layers, end-to-end playthrough | Build + Refine | All | Pending |

**Critical path:** 6 → 8 → 9 → 10 → 11 → 12 → 13 → 14 → 15 → 16 → 17 → 20

**Parallel tracks:**
- Module 7 (seqtbl) can run any time after Module 6
- Module 4 (control prototype) can run any time
- Modules 17-18 (platform, audio) can start after Module 6

---

## Tracks

The implementation modules group into three independent tracks based on toolchain and work regime:

### Track A — Game Logic Translation (current environment ✅)

**Regime:** Build (autonomous). **Toolchain:** Gradle + JDK 17 (already installed).
**Environment:** Works within Sandboxie limitations — CLI loop writes Kotlin files, Devmate/manual runs `gradle test` and `compare_traces.py`.

| Module | What | Lines |
|--------|------|------:|
| 6 | State Model (types.h + data.h → Kotlin) | ~500 |
| 7 | Sequence Table (seqtbl.c → Kotlin data) | ~1,200 |
| 8-12 | Layer 1 game logic (seg006 → seg004 → seg005 → seg002 → seg007) | ~7,200 |
| 13 | Layer 1 integration test (full regression suite) | — |
| 14 | Replay runner (Kotlin replay playback + trace writer) | ~500 |

**This is the core methodology test.** ~7,200 lines of autonomous C→Kotlin translation, validated by deterministic replay comparison. **Start here.**

### Track B — Android Platform (requires Android Studio)

**Regime:** Build (semi-autonomous). **Toolchain:** Android Studio + Android SDK + emulator/device.

| Module | What |
|--------|------|
| 15 | Game Loop — refactor seg000/001/003 to remove SDL, translate |
| 16 | Rendering — seg008 + lighting → Android Canvas/OpenGL |
| 17 | Platform — seg009 → Android APIs |
| 18 | Audio — midi.c → Android MediaPlayer/SoundPool |
| 20 | Integration — wire all layers, end-to-end playthrough |

**Starts after Track A** produces enough translated game logic to run. Heavier on human judgment (rendering bugs, frame pacing, audio sync).

### Track C — Touch Controls (requires Android Studio + phone)

**Regime:** Refine (human-driven). **Toolchain:** Android Studio + physical Android device.

| Module | What |
|--------|------|
| 4 | Control prototype — test app with gesture overlay on static screenshot |
| 19 | In-game controls — wire gestures to actual game input |

**Independent of Tracks A and B.** Can start any time. Requires a human holding a phone and judging game feel.

### What's next

**Track A, Module 6 (State Model)** is the immediate next step. It has no dependencies, works in the current environment, and is the foundation for all subsequent game logic translation.

---

## Cross-Cutting Concerns

### Replay Oracle Pipeline

Every autonomous translation module (8-12) follows the same validation loop:
1. Translate C file → Kotlin
2. Build (`gradle build`)
3. Run replay regression suite (all 13 replays)
4. Compare state traces (`compare_traces.py`)
5. On MATCH → proceed to next module
6. On DIVERGENCE → fix, retry (escalate after 2 failures)

### Integer Semantics

C-to-Kotlin integer translation rules:
- `byte` (unsigned 8-bit) → `Int` with `and 0xFF` masking where needed
- `sbyte` (signed 8-bit) → `Byte` (.toByte() wraps on overflow)
- `word` (unsigned 16-bit) → `Int` with `and 0xFFFF` masking
- `short` (signed 16-bit) → `Short`
- `dword` (unsigned 32-bit) → `Long` with `and 0xFFFFFFFF` masking
- Arithmetic overflow: Kotlin signed types wrap like C. Unsigned operations need explicit masking.

The replay oracle catches semantic mismatches immediately — no silent failures.

### Asset Loading

PoP uses custom binary formats for sprites (chtab), levels (DAT), sounds (VOC/WAV), and music. The platform layer must load these from Android assets. File I/O is isolated in seg009 and seg000 — not in game logic.

### Determinism

The game must be deterministic given the same seed and input sequence. This is inherent in the design (integer-only math, seeded RNG). The Kotlin port must preserve this property. Any use of floating point, hash maps with iteration-order dependence, or system clocks in game logic would break determinism.

---

## File Inventory

### Test Infrastructure (Phase 3 — complete)

| File | Purpose |
|------|---------|
| `REPLAY_COVERAGE.md` | Coverage matrix for 13 test replays |
| `TRACE_FORMAT.md` | Binary format spec for state traces (310 bytes/frame) |
| `tools/compare_traces.py` | Frame-by-frame trace comparator |
| `tools/test_comparator.sh` | Comparator validation script |
| `SDLPoP/traces/reference/*.trace` | 13 reference traces from C build |
| `SDLPoP-kotlin/docs/P1R_FORMAT.md` | .P1R replay file format spec |
| `SDLPoP-kotlin/src/.../P1RParser.kt` | Kotlin replay file parser |
| `SDLPoP-kotlin/src/.../P1RParserTest.kt` | Parser test suite (9 tests) |

### Source (SDLPoP — C original)

| File | Layer | Lines | SDL Calls |
|------|-------|------:|----------:|
| seg000.c | 2 | ~2,200 | ~60 |
| seg001.c | 2 | ~800 | ~20 |
| seg002.c | 1 | ~1,800 | 0 |
| seg003.c | 2 | ~500 | ~15 |
| seg004.c | 1 | ~620 | 0 |
| seg005.c | 1 | ~1,600 | 0 |
| seg006.c | 1 | ~1,800 | 0 |
| seg007.c | 1 | ~1,400 | 0 |
| seg008.c | 3 | ~2,200 | 15+ |
| seg009.c | 4 | ~4,250 | 506 |
| lighting.c | 3 | ~600 | ~5 |
| seqtbl.c | ext | 1,228 | 0 |
| menu.c | ext | ~3,200 | varies |
| replay.c | ext | ~1,500 | varies |
| midi.c | ext | ~800 | varies |
| options.c | ext | ~700 | varies |
