# DEVLOG — Prince of Persia Android Port

## Module 13: Layer 1 Integration Test

### 2026-04-15 — Phase 13a Plan: Layer 1 Replay Regression Harness

**Mode:** Discuss | **Outcome:** Phase planned
**Contract changes:** DEVPLAN.md, ARCHITECTURE.md — status propagation and phase plan only; no interface or behavior contract changes

Planned Phase 13a as the Build-regime bridge from focused Layer 1 unit tests to replay-based regression validation. The phase will create Kotlin trace parsing/comparison support, serialize `GameState` snapshots into the 310-byte oracle format, and wire a manifest-driven workflow around the 13 existing SDLPoP reference traces.

Phase 13a is split into three steps:
- **13a.1** Implement the trace oracle foundation: parse 310-byte frames, attach field metadata, compare frames/traces, and report first divergence details.
- **13a.2** Implement the Kotlin state snapshot writer for `Kid`, `Guard`, `Char`, scalar state, room buffers, trobs, mobs, and RNG using the exact `TRACE_FORMAT.md` layout.
- **13a.3** Add the regression manifest/workflow for all 13 reference traces, generated Kotlin trace artifacts under build output, comparison execution, and triage-ready failure reporting while documenting any remaining Module 14 replay-runner boundary.

Acceptance policy: byte-identical trace layout is required, and true game-logic divergences get no more than two targeted fix attempts before escalation with replay, frame, field, and suspected module details. Planning decision recorded in `DECISIONS.md` D-9. No source code changed in this iteration.

### 2026-04-15 — Module 12 Human Approval; Module 13 Unblocked

**Mode:** Governance | **Outcome:** Module 13 ready to plan
**Contract changes:** DEVPLAN.md, ARCHITECTURE.md — status propagation only; no interface or behavior contract changes

Human audit gate for Module 12 is approved. Phase 12a and Phase 12b review outcomes remain accepted with no must-fix or should-fix findings, and the last recorded fresh verification is `gradle test` passing in `SDLPoP-kotlin` with 540 tests and 0 failures on 2026-04-15.

Next autonomous action: phase-plan Module 13. The worker should define the Layer 1 full-regression harness, replay trace generation path, comparison workflow, acceptance criteria, and failure-escalation policy before implementation begins.

## Module 12: Layer 1 — seg007 (Traps, triggers, animated tiles)

### 2026-04-15 — Phase 12b Complete

**Mode:** Complete | **Outcome:** Phase 12b closed; Module 12 complete
**Contract changes:** DEVPLAN.md, ARCHITECTURE.md — status propagation and gotcha only; no interface or behavior contract changes

Phase 12b delivered the remaining seg007 loose-floor and mob subsystem: `animateLoose`, loose-floor shaking/fall initiation, falling-object simulation, Kid collision/death handling, room/row transitions, redraw bookkeeping, and mob object-table writes. Three steps across 3 iterations, zero escalations, 21 new focused tests (540 total pass).

Learning review: promoted one gotcha — seg007 tests that consume RNG through loose-floor shaking must restore `GameState.seedWasInit`, because the singleton default is validated by unrelated state-model tests.

Log review: Phase 12b entries show no repeated failures or unresolved tool issues. Two implementation details are already covered by DEVPLAN gotchas: level-backed tile writes for `getTile()`-driven tests, and avoiding broad shared-array resets in `@BeforeTest`.

Contract scan: no interface or behavior contract changes across Phase 12b; only status propagation to DEVPLAN/ARCHITECTURE and the gotcha addition.

### 2026-04-15 — Phase 12b Review

**Mode:** Review | **Outcome:** Complete — no must-fix or should-fix findings
**Contract changes:** None

Reviewed the Phase 12b `Seg007.kt` implementation against the Layer 1 contract and the corresponding `seg007.c` loose-floor/mob source slice. The translated loose-floor animation entry point, falling-object simulation, Kid collision handling, row/room transitions, redraw bookkeeping, and mob object-table writes remain deterministic game logic with no platform, I/O, or Android coupling. The planned omission of unused SDL-only `sub_9A8E` remains valid because no Kotlin call site exists and the function belongs to the renderer blit path.

Review findings:
- Must fix: none
- Should fix: none
- Optional: none applied

Verification: fresh `gradle test` passed in `SDLPoP-kotlin` (540 tests, 0 failures).

### 2026-04-15 — Step 12b.3: Mob draw/object-table bookkeeping

**Mode:** Code | **Outcome:** Complete — 7 new tests pass (540 total)
**Contract changes:** None

Translated the final Phase 12b seg007 draw bookkeeping slice into `Seg007.kt`: `drawMobs`, `drawMob`, and `addMobToObjtable`. The implementation preserves drawn/current, below-room, and above-room visibility rules, marks foreground/redraw slots for the falling loose-floor sprite footprint, and writes mob entries into the existing object table using environment chtab sprite id 10 with `obj_type | 0x80`.

Left unused SDL-only helper `sub_9A8E` unported as planned because there is no live Kotlin reference and it only blits screen rectangles in the C renderer path. Added focused `Seg007Test` coverage for object-table fields, current-room rendering, offscreen skips, room-above/below edge mapping, redraw markers, and `drawMobs` dispatch without mutating stored mob entries.

Verification: `gradle test` passed in `SDLPoP-kotlin` (540 tests, 0 failures).

### 2026-04-15 — Step 12b.2: Falling loose-floor mobs and Kid collision

**Mode:** Code | **Outcome:** Complete — 9 new tests pass (533 total)
**Contract changes:** None

Translated the loose-floor mob simulation tranche into `Seg007.kt`: `doMobs`, `moveMob`, `moveLoose`, `looseLand`, `looseFall`, `redrawAtCurMob`, `mobDownARow`, `checkLooseFallOnKid`, and `fellOnYourHead`. The implementation preserves signed `speed` wrapping and unsigned byte-like `y` wrapping for `mob_type`, handles row and room transitions, keeps chained loose-floor falls in the active mob list, and applies Kid damage/death sequences when a falling floor lands on him.

Added focused `Seg007Test` coverage for mob compaction, room-zero stopping, bottom-row room transitions, landing on buttons/floors/torches, chained loose-floor spawning, redraw markers, and nonfatal/fatal Kid collision behavior. The seg007 tile-write helpers now mirror loose-floor/debris mutations into `level.fg/bg` when a room is known, because `getTile()` reloads room buffers from level data in the Kotlin port.

Verification: `gradle test` passed in `SDLPoP-kotlin` (533 tests, 0 failures).

### 2026-04-15 — Step 12b.1: Loose-floor animation entry point

**Mode:** Code | **Outcome:** Complete — 5 new tests pass (524 total)
**Contract changes:** None

Translated `animate_loose` into `Seg007.kt` with the local loose-floor landing Y table and the `loose_shake` helper it directly requires. The implementation covers shaking-timer progression, level-13 auto-falling loose floors, delayed mob spawning, the `FIX_DROP_2_ROOMS_CLIMBING_LOOSE_TILE` guard, redraw side effects, and byte-masked loose-floor modifiers.

Added focused `Seg007Test` coverage for the 12b.1 behaviors named in the plan. Test setup now restores `GameState.seedWasInit` after each seg007 test because `loose_shake` consumes RNG and the singleton default is validated by `TypesTest`.

Verification: `gradle test` passed in `SDLPoP-kotlin` (524 tests, 0 failures).

### 2026-04-15 — Phase 12b Plan: Loose-floor mobs and remaining seg007 functions

**Mode:** Discuss | **Outcome:** Phase planned

Planned Phase 12b as the final seg007 translation slice: loose-floor fall initiation, falling-object simulation, Kid collision from falling floors, redraw bookkeeping, and mob object-table writes. The phase remains Build regime because behavior is deterministic game logic with focused unit-test coverage ahead of Module 13 replay integration.

Phase 12b is split into three steps:
- **12b.1** Translate `animate_loose` and local loose-floor fall data, covering shaking timers, level-13 auto-falling floors, delayed fall spawning, climbing loose-tile fix behavior, and redraw effects.
- **12b.2** Translate the mob simulation/collision tranche: `do_mobs`, `move_mob`, `move_loose`, `loose_land`, `loose_fall`, `redraw_at_cur_mob`, `mob_down_a_row`, `check_loose_fall_on_kid`, and `fell_on_your_head`.
- **12b.3** Translate mob draw/object-table bookkeeping: `draw_mobs`, `draw_mob`, and `add_mob_to_objtable`; leave unused SDL-only `sub_9A8E` unported unless a live reference appears.

Planning decision recorded in `DECISIONS.md` D-6. No source code changed in this iteration.

### 2026-04-11 — Phase 12a Complete

**Mode:** Complete | **Outcome:** Phase 12a closed; Module 12 continues with Phase 12b
**Contract changes:** DEVPLAN.md, ARCHITECTURE.md — status propagation only; no interface or behavior contract changes

Phase 12a delivered trob pipeline infrastructure, animated-tile state machines (torch, potion, sword, chomper, spike, button, gate, level-door), trigger/doorlink plumbing, and 6 ExternalStubs wire-ups into `Seg007.kt`. Three steps across 4 iterations (25-28), zero escalations, 70 new tests (519 total pass).

Learning review: promoted one gotcha — test `@BeforeTest` methods that globally zero shared arrays (like `soundInterruptible`) corrupt default values validated by other test suites. Fix: only reset entries modified by each test.

Log review: iterations 25-28 show no anomalies — no repeated failures, no wasted turns, no tool issues.

Contract scan: no contract changes across any Phase 12a step.

Remaining seg007 work (loose-floor removal, mob simulation/movement/drawing, object-table writes) deferred to Phase 12b.

### 2026-04-11 — Phase 12a Review

**Mode:** Review | **Outcome:** Complete — no must-fix or should-fix findings
**Contract changes:** None

Reviewed the Phase 12a `Seg007.kt` implementation against the Layer 1 contract and the corresponding `seg007.c` source slice. The translated trob loop, redraw helpers, animated tile state machines, doorlink/button trigger plumbing, seg006-facing trap hooks, gate animation, and level-door animation remain pure game logic with no platform, I/O, or Android coupling. `ExternalStubs` now delegates the completed seg007 trap hooks to real implementations as intended.

Review findings:
- Must fix: none
- Should fix: none
- Optional: none applied

Verification: fresh `gradle test` passed in `SDLPoP-kotlin` (519 tests, 0 failures).

### 2026-04-11 — Step 12a.3: Gate/leveldoor animation, play_door_sound_if_visible

**Mode:** Code | **Outcome:** Complete — 39 new tests pass (519 total)
**Contract changes:** None

Translated 4 functions into Seg007.kt completing the animate_tile dispatch for gate and level door tiles:
- `animateDoor` — gate open/close logic with 3 closing modes (normal decrement, regular open→close cycle, permanent open) and fast-close speed tiers (types 3-8 with increasing speeds 20-120)
- `gateStop` — trob removal with visible-gate sound
- `animateLeveldoor` — level door open (with mirror placement on level 4) and close (4 speed tiers), FIX_FEATHER_INTERRUPTED_BY_LEVELDOOR support
- `playDoorSoundIfVisible` — visibility-based gate sound with FIX_GATE_SOUNDS support for drawn room and left-adjacent room column-9 edge cases, plus special level 3 room 2 event

Added helper data arrays: `doorDelta`, `gateCloseSpeeds`, `leveldoorCloseSpeeds`.

Test note: fixed shared-state leak where `@BeforeTest` was filling `soundInterruptible` with zeros, which corrupted the initial default values that `TypesTest` validates. Changed to targeted reset of only the entries modified by tests.

### 2026-04-11 — Step 12a.2: Animated-tile state machines, trob lifecycle, trigger plumbing

**Mode:** Code | **Outcome:** Complete — 31 new tests pass (482 total)
**Contract changes:** None

Translated 30+ functions into Seg007.kt:
- Animated-tile state machines: `animateTorch`, `animatePotion`, `animateSword`, `animateChomper`, `animateSpike`, `animateButton`, `animateEmpty`
- Animation starters: `startAnimTorch`, `startAnimPotion`, `startAnimSword`, `startAnimChomper`, `startAnimSpike`
- Trob lifecycle: `addTrob`, `findTrob`
- Doorlink accessors: `getDoorlinkTimer`, `setDoorlinkTimer`, `getDoorlinkTile`, `getDoorlinkNext`, `getDoorlinkRoom`
- Trigger plumbing: `triggerButton`, `trigger1`, `doTriggerList`, `triggerGate`, `diedOnButton`
- Chomper timing: `startChompers`, `nextChomperTiming`
- Loose floor helpers: `makeLooseFall`, `looseMakeShake`, `doKnock`, `removeLoose`
- Mob lifecycle: `addMob`
- Entry points: `startLevelDoor`, `isSpikeHarmful`

Wired 6 ExternalStubs entries: `startChompers`, `triggerButton`, `makeLooseFall`, `startAnimSpike`, `isSpikePowerful` (→ `isSpikeHarmful`), `diedOnButton`.

Scope note: doorlink accessors and trigger plumbing were pulled into 12a.2 (originally scoped for 12a.3) because they are tightly coupled with `animateButton` and `triggerButton`. Phase 12a.3 is now focused solely on `animateDoor`/`animateLeveldoor` gate animation.

### 2026-04-11 — Step 12a.1: Seg007 scaffold, trob loop, redraw helpers

**Mode:** Code | **Outcome:** Complete — 10 new tests pass (461 total)
**Contract changes:** None

Created `Seg007.kt` with the Phase 12a.1 scaffold and direct translations for the shared trob-processing loop, `animate_tile` dispatch, drawn-room trob coordinate helpers, redraw/wipe array writers, `clear_tile_wipes`, `get_curr_tile`, `bubble_next_frame`, and `get_torch_frame`.

Added `Seg007Test.kt` coverage for current/adjacent-room tile mapping, negative above-room redraw indexing, redraw/wipe bookkeeping, tile type masking, deletion of unsupported trob entries, and bubble frame wraparound. The animated-tile handlers that belong to later steps are present as explicit `NotImplementedError` placeholders, so the scaffold compiles while preserving the planned 12a.2/12a.3 boundaries.

Verification: `gradle test --tests com.sdlpop.game.Seg007Test` passed, then full `gradle test` passed.

### 2026-04-10 — Phase 12a Plan: Trob core, redraw helpers, trap/button animation

**Mode:** Discuss | **Outcome:** Phase planned

Planned Phase 12a around the seg007 slice that is both self-contained and immediately useful to the translated codebase: trob iteration, drawn-room coordinate helpers, redraw/wipe bookkeeping, basic animated-tile state machines, and button/gate trigger plumbing.

Phase 12a is split into three steps:
- **12a.1** Scaffold `Seg007.kt` with trob-loop helpers, room-position math, redraw array writes, and basic helper functions (`get_curr_tile`, `bubble_next_frame`, `get_torch_frame`).
- **12a.2** Translate the first animated-tile tranche plus trob lifecycle (`animate_torch`, `animate_potion`, `animate_sword`, `animate_chomper`, `animate_spike`, `animate_button`, `add_trob`, `find_trob`, starters, `is_spike_harmful`) and wire completed seg007 stubs.
- **12a.3** Translate doorlink/button trigger plumbing and gate animation (`trigger_gate`, `trigger_1`, `do_trigger_list`, `trigger_button`, `animate_door`, `gate_stop`, `play_door_sound_if_visible`, and `animate_leveldoor` if it fits the phase cleanly).

Loose-floor removal, mob spawning/movement, and mob drawing/object-table writes are intentionally deferred to later phases because they form a broader subsystem with heavier state coupling than the first trap/tile slice.

## Module 11: Layer 1 — seg002 (Guard AI, room transitions)

### 2026-04-10 — Phase 11c Complete

**Mode:** Complete | **Outcome:** Module 11 complete; documentation updated

Completed Phase 11c and closed Module 11 after the prior review found no correctness or architecture issues. `Seg002.kt` now covers seg002 sword combat detection, skeleton wake logic, auto-move sequencing, shadow autocontrol, and the associated `ExternalStubs` wire-up needed by downstream modules.

Phase-complete verification attempted a fresh targeted Gradle run with `gradle test --tests com.sdlpop.game.Seg002Test`, but the command failed before task execution in this container with `Failed to load native library 'libnative-platform.so' for Linux aarch64`. Acceptance therefore relies on the reviewed Phase 11c test artifacts already recorded in `DECISIONS.md` D-3.

**Contract changes:** `DEVPLAN.md`, `ARCHITECTURE.md` — status propagation only; no interface or behavior contract changes

Learning review: promote one Phase 11c test setup gotcha into `DEVPLAN.md` — functions that call `getTile()`/`getRoomAddress()` must seed `gs.level.fg[]`/`gs.level.bg[]` in tests, because room buffers are reloaded from level data.

### 2026-04-09 — Phase 11c Step 3: Shadow autocontrol functions (4 functions)

**Mode:** Code | **Outcome:** ✓ All tests pass (20 new, 449 total)

Translated 4 shadow autocontrol functions from seg002.c → Seg002.kt:
- `autocontrolShadowLevel4` (seg002:0FF0): Mirror room approach — shadow walks forward, cleared when x<80.
- `autocontrolShadowLevel5` (seg002:101A): Steal life — waits for door open (modifier≥80), runs shadDrinkMove auto-moves, cleared when x<15.
- `autocontrolShadowLevel6` (seg002:1064): Step level — shift+forward when Kid is in running jump frame 43 with x<128.
- `autocontrolShadowLevel12` (seg002:1082): Final level — complex multi-phase: init shadow from custom data, sword combat (delegates to autocontrolGuardActive or move_down), sword draw approach (charOppDist-based), unite with Kid (flash+addLife+unitedWithShadow=42), follow running Kid.

All level-specific logic uses `CustomOptionsType` fields for room/level configuration. Level 12 shadow uses `charOppDist` for distance-based behavior and `ExternalStubs.addLife` for the unite sequence.

**Contract changes:** None — replaced stub bodies only, no new external deps beyond existing ExternalStubs.addLife.

### 2026-04-07 — Phase 11c Step 2: Skeleton wake + auto moves (2 functions)

**Mode:** Code | **Outcome:** ✓ All tests pass (17 new, 429 total)

Translated 2 functions from seg002.c → Seg002.kt:
- `checkSkel` (seg002:0E7C): Skeleton wake event on level 3. Checks level/room/door/trigger conditions from custom options, erases skeleton tile (→floor), sets up Char as skeleton (charid=4, sword=drawn, 3HP, skill from custom), plays wake-up sequence and sound.
- `doAutoMoves` (seg002:0F3F): Timed auto-move dispatcher for demo/shadow AI. Increments demoTime (capped at 0xFE), advances move index when time threshold met, dispatches via when-switch (moves 0-7 map to move helper functions, -1 = no-op).

Test setup: `checkSkel` tests required placing skeleton tile in `gs.level.fg[]` (not `currRoomTiles[]` directly) because `getTile` calls `getRoomAddress` which reloads tiles from level data.

### 2026-04-05 — Phase 11c Step 1: Sword combat functions (4 functions)

**Mode:** Code | **Outcome:** ✓ All tests pass (26 new)

Translated 4 sword combat functions from seg002.c → Seg002.kt:
- `hurtBySword` (seg002:0BE5): HP deduction, death/knockback branching, gate positioning with `fixOffscreenGuardsDisappearing`, pushed-off-ledge path, skeleton immunity
- `checkSwordHurt` (seg002:0CD4): Routes Guard.action/Kid.action==99 through loadshad/loadkid + hurtBySword + save, sets refrac timer
- `checkSwordHurting` (seg002:0D1A): Skips if Kid on stairs (frames 219-228), checks both sides via loadshadAndOpp/loadkidAndOpp
- `checkHurting` (seg002:0D56): Distance + frame checks, parry detection (frames 150/161), min_hurt_range (8 unarmed, 12 armed), sword moving sound

Extracted `hurtBySwordKnockback` as private helper to eliminate C `goto loc_4276` — shared by not-drawn (instant death) and drawn+dead paths.

Key translation detail: C `(distance = distance_to_edge_weight()) < 4` assigns in condition and reuses in else. Kotlin version pre-computes `distance` with short-circuit to match C semantics (only call when `getTileBehindChar() == 0`).

Test fix: parry tests initially failed because `charOppDist()` returned out-of-range values. Fixed by positioning characters closer (Opp.x = 90, Char.x = 100 → base distance 10 + 13 facing offset = 23, within [0, 29) range).

### 2026-04-05 — Phase 11b Review & Complete

**Mode:** Review | **Outcome:** ✓ Clean — no issues found

Reviewed all 16 Phase 11b functions against C originals. All translations correct:
- Unsigned word comparisons in `autocontrolGuardInactive` use `and 0xFFFF` correctly
- `guardFollowsKidDown` tile row increment matches C `++tile_row` side effect
- `autocontrolShadow` uses `if` (not `else if`) matching C source
- Combat probability functions use correct array indexing via `gs.guardSkill`
- No dead code, unused imports, or architecture drift

No gotchas to promote — Phase 11b was clean (single-step, zero failures).

### 2026-04-05 — Phase 11b Step 1: Autocontrol & guard AI (16 functions)

**Mode:** Code | **Outcome:** ✓ All 386 tests pass (30 new)

Translated 16 autocontrol/guard AI functions from seg002.c → Seg002.kt:
- **Main dispatch** (1): `autocontrol_opponent` — decrements counters, routes by charid
- **Character-specific dispatchers** (5): `autocontrol_mouse`, `autocontrol_shadow`, `autocontrol_skeleton`, `autocontrol_Jaffar`, `autocontrol_kid`
- **Guard AI core** (4): `autocontrol_guard`, `autocontrol_guard_inactive`, `autocontrol_guard_active`, `autocontrol_guard_kid_far`
- **Combat AI** (4): `autocontrol_guard_kid_in_sight`, `autocontrol_guard_kid_armed`, `guard_advance`, `guard_block`, `guard_strike`
- **Movement** (1): `guard_follows_kid_down`
- **Shadow stubs** (4): `autocontrol_shadow_level4/5/6/12` — empty placeholders for Phase 11c

Key translation details:
- `autocontrol_guard_inactive`: unsigned word comparisons `(word)distance < (word)-8` → `(distance and 0xFFFF) < ((-8) and 0xFFFF)`
- `guard_follows_kid_down`: C `++tile_row` side effect → `gs.tileRow = (gs.tileRow + 1).toShort()` before `getTile` call
- `tileIsFloor` returns `Int` (0/1), not Boolean — used `!= 0` / `== 0` throughout
- `tileRow`/`tileCol`/`currRoom` are `Short` — `.toInt()` for function args, `.toShort()` for assignments

Test fix: `autocontrolGuardActive_canSeeKid2_far_kidFar` initially failed — Opp was positioned behind the guard (distance negative), not in front. Fixed by swapping positions so `charOppDist()` returns positive distance ≥35.

### 2026-04-05 — Phase 11b Plan: Guard AI & autocontrol

**Mode:** Discuss | **Outcome:** Phase planned

Planned Phase 11b as a single step: 16 autocontrol/guard AI functions (~280 lines C). Key complexity in `autocontrol_guard_active` (distance-based behavior tree with 3-state `can_guard_see_kid`) and `guard_follows_kid_down` (tile safety checks with `++tile_row` side effect). Combat probability functions use `custom.advprob/blockprob/strikeprob/restrikeprob/impblockprob` arrays indexed by `guardSkill`. Shadow autocontrol dispatches to level-specific functions deferred to Phase 11c. ~25 tests planned.

### 2026-04-05 — Phase 11a Step 1: Move helpers, guard init/state, room transitions, special events

**Mode:** Code | **Outcome:** ✓ All 356 tests pass (49 new)

Translated 19 functions from seg002.c → Seg002.kt:
- **Move helpers** (10): `move_0_nothing` through `move_7`, `move_up_back`, `move_down_back`, `move_down_forw`
- **Guard init/state** (5): `do_init_shad`, `get_guard_hp`, `check_shadow`, `enter_guard`, `check_guard_fallout`, `leave_guard`
- **Room transitions** (4): `follow_guard`, `exit_room`, `goto_other_room`, `leave_room`
- **Special events** (5): `jaffar_exit`, `level3_set_chkp`, `sword_disappears`, `meet_Jaffar`, `play_mirr_mus`
- **Internal helpers** (2): `load_frame_to_obj` (inlined from seg008), `prandom` (from seg009)

Wired `ExternalStubs.leaveGuard` → `Seg002.leaveGuard()`.

Bug found during testing: `leaveGuard` must mask `currSeq` to byte when writing to `guardsSeqLo`/`guardsSeqHi` — C implicitly truncates byte array stores, Kotlin `IntArray` does not.

### 2026-04-05 — Phase plan for Module 11

<!-- Modules 6-10 complete. Archive entries below this line to DEVLOG_archive.md when DEVLOG exceeds ~500 lines. -->

**Scope:** 1,237 lines C, 52 functions → Seg002.kt. Three phases:
- **11a** — Guard init, room management, special events, move helpers (~29 functions, ~590 lines)
- **11b** — Guard AI & autocontrol (~18 functions, ~350 lines)
- **11c** — Sword combat detection & shadow autocontrol (~15 functions, ~350 lines)

**Key dependencies:** seg002 calls into Seg006 (tile queries, physics), Seg005 (combat), Seg004 (collision). Also needs stubs for seg000/seg003/seg007/seg008 functions. Two ExternalStubs entries already exist (`autocontrolOpponent`, `leaveGuard`) — will be wired to real implementations.

**Notable complexity:** `enter_guard` has a large `FIX_OFFSCREEN_GUARDS_DISAPPEARING` block (~60 lines) that retrieves guards from adjacent rooms. `exit_room` has complex frame-based leave conditions + guard follow logic. `hurt_by_sword` has gate-positioning edge cases. Shadow level 12 autocontrol has unite/fight dual behavior.

---

## Module 10: Layer 1 — seg005 (Player control, sword fighting)

### 2026-04-05 — Module 10 Complete (phase-complete)

**All 38 seg005.c functions translated to Kotlin.** 307 tests pass (75 new), gradle build clean.
3 phases (10a–10c) completed autonomously with zero escalations.

Files produced:
- `Seg005.kt` — 38 game logic functions (player control, movement, falling/landing, jumping/climbing, items, sword fighting)
- `Seg005Test.kt` — 75 unit tests

Key decisions: C goto labels refactored to helper methods (`land` → `landSoftOrActive`/`landFatal`/`landFatalSound`), unsigned word comparisons translated as `(x and 0xFFFF) < (y and 0xFFFF)`, `USE_COPYPROT`/`USE_TELEPORTS` paths skipped (not in reference build), all 18 `#ifdef` fix flags as runtime checks.

Contract changes: ExternalStubs wired — `control`, `drawSword`, `spiked`, `doFall` now point to Seg005 implementations. `doPickup` (seg003) and `leaveGuard` (seg002) stubs added.

Gotcha promoted: C unsigned word comparison pattern `(word)x < (word)y` → `(x and 0xFFFF) < (y and 0xFFFF)`.

---

### 2026-04-05 — Phase 10c: Sword fighting (7 functions)

**Mode:** Code (autonomous)
**Outcome:** Complete — 22 new tests pass (307 total), gradle build clean

**What was done:**
Replaced Phase 10c placeholder functions in `Seg005.kt` with full implementations:
- Sword basics: `backWithSword` (step back), `forwardWithSword` (Kid/Guard variants), `drawSword` (sound, en_garde for guards, FIX_UNINTENDED_SWORD_STRIKE)
- Sword control: `controlWithSword` (main dispatch when sword drawn — swordfight/sheathe/become inactive), `swordfight` (parry release, strike, sheathe, parry/forward/back dispatch)
- Combat: `swordStrike` (strike from eligible frames, Kid/Guard sequences, strike-after-parry), `parry` (opponent frame checks, distance-based guard back, play_seq for Kid vs strike_3)

Wired ExternalStubs: `control` → Seg005.control(), `drawSword` → Seg005.drawSword(), `spiked` → Seg005.spiked(), `doFall` → Seg005.doFall().

22 new tests: backWithSword (2), forwardWithSword (2), drawSword (4 — Kid/Guard/Shadow/fix), controlWithSword (2), swordfight (4), swordStrike (4), parry (4).

All 38 seg005 functions now translated. Module 10 translation complete.

**Design decisions:**
- `controlWithSword` C comment `/*else*/` indicates fall-through from both if branches — translated as sequential blocks after the opponent-in-range check
- `parry` unreachable branch (`charCharid != CID.KID` inside `charCharid == CID.KID` block) preserved to match C source exactly
- `(word)distance < (word)90` and `(word)distance < (word)-4` in controlWithSword use unsigned 16-bit comparison pattern

**Contract changes:** ExternalStubs wired: control, drawSword, spiked, doFall now point to Seg005 implementations instead of throwing NotImplementedError.

---

### 2026-04-04 — Phase 10b: Standing control, climbing, items (17 functions)

**Mode:** Code (autonomous)
**Outcome:** Complete — 17 new tests pass (285 total), gradle build clean

**What was done:**
Replaced Phase 10b placeholder functions in `Seg005.kt` with full implementations:
- Standing: `controlStanding` (sword draw logic, movement dispatch with goto refactoring), `upPressed` (level door entry, jump), `downPressed` (climb down, crouch, position adjust), `goUpLeveldoor`
- Jumping: `standingJump`, `checkJumpUp` (tile-above grab logic), `jumpUpOrGrab`, `grabUpNoFloorBehind`, `jumpUp` (full USE_SUPER_HIGH_JUMP path), `grabUpWithFloorBehind` (FIX_EDGE_DISTANCE_CHECK), `runJump` (edge alignment)
- Hanging: `controlHanging` (wall/doortop detection, super jump), `canClimbUp` (mirror/chomper/gate block), `hangFall`
- Items: `checkGetItem` (potion/sword detection), `getItem` (pickup dispatch), `controlJumpup`

Added `doPickup` (seg003) and `leaveGuard` (seg002) stubs to ExternalStubs.kt.
Added `superJumpTimer`, `superJumpRoom`, `superJumpCol`, `superJumpRow` to GameState.kt.

17 new tests in Seg005Test.kt covering: controlStanding (forward/back/shift-back), standingJump, goUpLeveldoor, controlJumpup (2), downPressed, canClimbUp (normal/closed-gate/mirror), hangFall (no floor), controlHanging (dead falls), grabUpWithFloorBehind, getItem (sword/potion/not-crouching), runJump (not running).

**Design decisions:**
- `controlStanding` C `goto loc_6213` refactored to fall-through control flow
- `(word)distance < (word)-6` in controlStanding translated as `(distance and 0xFFFF) < ((-6) and 0xFFFF)` — unsigned 16-bit comparison
- `USE_COPYPROT` path in `getItem` skipped (not in reference build)
- `USE_TELEPORTS` path in `upPressed` skipped (not in reference build)
- C boolean expression `(curr_tile2 == tiles_10_potion)` in getItem translated as explicit if/else

**Contract changes:** None.

---

### 2026-04-04 — Phase 10a: Falling, landing, movement basics (14 functions)

**Mode:** Code (autonomous)
**Outcome:** Complete — 36 new tests pass (268 total), gradle build clean

**What was done:**
Created `Seg005.kt` with Phase 10a functions (14 of 38):
- Sequence helpers: `seqtblOffsetChar`, `seqtblOffsetOpp`
- Falling/landing: `doFall` (scream, grab check, wall/floor dispatch), `land` (C goto refactored to helper methods: `landSoftOrActive`, `landFatal`, `landFatalSound`), `spiked`
- Control dispatch: `control` (main frame-based dispatch with all fix flag paths)
- Control handlers: `controlCrouched` (level 1 music, stand up, crouch-hop), `controlTurning` (run-after-turn, joystick cleanup), `crouch`
- Movement: `backPressed` (turn or turn-draw-sword), `forwardPressed` (run or safe-step), `controlRunning` (stop/turn/jump/crouch), `safeStep` (careful step to edge), `controlStartrun`

Phase 10b/10c functions included as placeholders (empty method bodies).

Created `Seg005Test.kt` with 36 unit tests covering: seqtblOffsetChar/Opp, doFall (scream/no-scream/low-fallY/continues-falling), land (soft/medium/fatal 3-row/guard fatal/shadow soft/start-chompers), spiked (modif set/offscreen fix), control (dead-dying/bumped-release/standing/crouching/fix-move-after-drink), controlCrouched (stand-up/crouch-hop/level1-music), controlTurning (run-after-turn/fix-near-wall/joystick-clear), crouch, backPressed (turn-no-sword/turn-with-sword), forwardPressed (start-run), controlRunning (stop/turn/crouch-while-running), safeStep, controlStartrun.

**Design decisions:**
- `land()` C goto labels (loc_5EE6, loc_5EFD, loc_5F6C, loc_5F75) refactored to helper methods (`landSoftOrActive`, `landFatal`, `landFatalSound`) — cleaner control flow, identical behavior
- `ALLOW_CROUCH_AFTER_CLIMBING` correctly placed as `else if` in control dispatch chain (not separate `if`)
- `FIX_DROP_THROUGH_TAPESTRY` correctly placed as `else if` on wall check in `doFall`
- CharType fields are all `Int` — no `.toByte()` conversions needed (unlike GameState Short fields)

**Contract changes:** None.

---

## Module 9: Layer 1 — seg004 (Collision detection)

### 2026-04-04 — Module 9 Complete (phase-complete)

**All 26 seg004.c functions translated to Kotlin.** 232 tests pass (42 new), gradle build clean.
2 phases (9a–9b) completed autonomously with zero escalations.

Files produced:
- `Seg004.kt` — 26 game logic functions + 2 private helpers (collision detection, wall bumping, chomper damage, gate pushing, edge distance)
- `Seg004Test.kt` — 42 unit tests

Key decisions: `get_edge_distance` goto refactored to extracted helper, `load_frame_to_obj` inlined as private method, all 7 `#ifdef` fix flags as runtime checks.
No contract changes. No trial-and-error patterns — all phases completed in single attempts.

---

### 2026-04-04 — Phase 9b: Review and cleanup

**Mode:** Review (autonomous)
**Outcome:** Complete — no must-fix or should-fix issues found

**Review findings:**
- All 26 C functions present in Kotlin (28 total including 2 private helpers: `getEdgeDistanceFront`, `loadFrameToObj`). Function count verified against C source.
- Integer semantics audit: Short/Int conversions correct — `.toInt()` for arithmetic/indexing, `.toShort()` for assignment. Unsigned word comparison in `bumpedFloor` uses `and 0xFFFF`. Signed byte truncation in `bumped`/`loadFrameToObj` uses `.toByte().toInt()`.
- All 7 `#ifdef` fix paths correctly translated as runtime checks (`!= 0`). `fixCollFlags` (disabled in ref build) added to FixesOptionsType with default 0.
- No dead code, unused imports, TODOs, or NotImplementedError in Seg004.kt.
- No architecture drift: zero platform calls, pure game logic, deterministic.
- Build: `gradle build` clean, `gradle test` 232/232 pass.
- No fixes required. Ready for phase completion.

---

### 2026-04-04 — Phase 9a: Full translation (all 26 functions)

**Mode:** Code (autonomous)
**Outcome:** Complete — 42 new tests pass (232 total), gradle build clean

**What was done:**
Created `Seg004.kt` with all 26 seg004.c functions:
- Collision core: `checkCollisions`, `moveCollToPrev`, `getRowCollisionData`, `clearCollRooms`
- Wall position: `getLeftWallXpos`, `getRightWallXpos`
- Bump detection: `checkBumped`, `checkBumpedLookLeft`, `checkBumpedLookRight`, `isObstacleAtCol`
- Obstacle/position: `isObstacle`, `xposInDrawnRoom`
- Bump response: `bumped`, `bumpedFall`, `bumpedFloor`, `bumpedSound`
- Chomper: `checkChompedKid`, `chomped`, `checkChompedGuard`, `checkChompedHere`
- Gate/guard: `checkGatePush`, `checkGuardBumped`
- Edge/wall distance: `getEdgeDistance`, `distFromWallForward`, `distFromWallBehind`
- Gate helper: `canBumpIntoGate`
- Private helper: `loadFrameToObj` (inlined from seg008 — needed for collision calculations)
- Private helper: `getEdgeDistanceFront` (extracted from C goto-based control flow in `get_edge_distance`)

Added `fixCollFlags` field to `FixesOptionsType` (disabled in reference build, defaults to 0).

5 seg004-local variables as object properties: `bumpColLeftOfWall`, `bumpColRightOfWall`, `rightCheckedCol`, `leftCheckedCol`, `collTileLeftXpos`.
2 constant lookup tables: `wallDistFromLeft`, `wallDistFromRight`.

Created `Seg004Test.kt` with 42 unit tests covering: clearCollRooms (with/without fixCollFlags), moveCollToPrev, wall distance tables, getLeftWallXpos/getRightWallXpos, canBumpIntoGate, isObstacle (potion/gate/chomper/mirror/wall), xposInDrawnRoom, bumpedSound, bumpedFall, bumped (dead/spiked/alive), checkBumped (hang/climb skip), distFromWallForward/Behind, chomped (blood/skeleton/already-chomped), checkGatePush, checkChompedHere, isObstacleAtCol (row wrapping), checkCollisions (turn early return).

**Design decisions:**
- `get_edge_distance` C goto refactored to extracted helper `getEdgeDistanceFront` — cleaner control flow, identical behavior
- `load_frame_to_obj` inlined as private method (seg008 function, but needed for collision; same pattern as seg006's `getRoomAddress`)
- All 7 `#ifdef` fix paths translated as runtime checks: `fixCollFlags` (disabled), `fixTwoCollBug`, `enableJumpGrab`, `fixSkeletonChomperBlood`, `fixOffscreenGuardsDisappearing`, `fixCapedPrinceSlidingThroughGate`, `fixPushGuardIntoWall`
- `FIX_COLL_FLAGS` is disabled in reference build (commented out in config.h) — added to FixesOptionsType with default 0

**Contract changes:** None.

---

## Module 8: Layer 1 — seg006 (Character physics, tile queries)

### 2026-04-04 — Module 8 Complete (phase-complete)

**All 81 seg006.c functions translated to Kotlin.** 190 tests pass, gradle build clean.
5 phases (8a–8e) completed autonomously with zero escalations.

Files produced:
- `Seg006.kt` — 81 game logic functions (tile queries, physics, collision, falling, grabbing, damage, player/guard control)
- `ExternalStubs.kt` — 30 stubs for cross-segment dependencies (seg000/002/003/005/007/008/replay)
- `Seg006Test.kt` — 190 unit tests

Key decisions: external dependency stub pattern, `#ifdef` flags as runtime checks, `getRoomAddress` inlined.
No contract changes. No trial-and-error patterns — all phases completed in single attempts.

---

### 2026-04-04 — Phase 8e: Review and cleanup

**Mode:** Review (autonomous)
**Outcome:** Complete — no must-fix or should-fix issues found

**Review findings:**
- All 81 C functions present in Kotlin (84 total including 3 seqtbl helpers). Function count verified against C source.
- Integer semantics audit: unsigned 16-bit comparison in `checkGrab` correctly masked with `and 0xFFFF`. No `.toShort()` truncation issues in Seg006.kt.
- All `#ifdef` fix paths correctly translated as runtime checks (12 fix flags verified).
- External stubs (ExternalStubs.kt) clean — 30 stubs covering 7 segments. `getRoomAddress` correctly inlined.
- No dead code, unused imports, TODOs, or NotImplementedError in Seg006.kt.
- No architecture drift: zero platform calls, pure game logic, deterministic.
- Build: `gradle build` clean, `gradle test` 190/190 pass.
- No fixes required. Ready for phase completion.

---

### 2026-04-04 — Phase 8d: Player/guard control, integration

**Mode:** Code (autonomous)
**Outcome:** Complete — 24 new tests pass (190 total), gradle build clean, all 81 C functions translated

**What was done:**
Added final 7 functions to `Seg006.kt` (Phase 8d — player/guard control):
- Player control: `playKid` (top-level kid frame handler — fellOut, controlKid, death sequence), `controlKid` (demo vs normal level dispatch, grab timer, replay hooks), `userControl` (direction-aware control dispatch with flipControlX), `doDemo` (demo level AI — checkpoint run, sword fight, auto-moves)
- Guard control: `playGuard` (top-level guard frame handler — mouse/shadow/guard dispatch), `controlGuardInactive` (inactive guard sword draw or flip), `charOppDist` (signed distance between char and opponent)

Added `SDL_SCANCODE_L` and `WITH_CTRL` constants for demo-level key handling in `controlKid`.

All 81 seg006 C functions now present in Kotlin (84 total including 3 seqtbl helpers). Phase 8d complete.

24 new tests: userControl (facing right flips, facing left direct), doDemo (checkpoint/sword/auto-moves), controlKid (kill on no HP, grab timer decrement, demo level dispatch, normal level dispatch, feather fall stop on death), playKid (smoke test, death sequence alive increment), playGuard (mouse autocontrol, dead guard kill, alive guard control, shadow clear), controlGuardInactive (draw sword, flip, wrong frame ignore), charOppDist (different room 999, front facing left/right, opposite facing +13, behind negative).

### 2026-04-04 — Phase 8c, Step 1: Falling, grabbing, damage, objects

**Mode:** Code (autonomous)
**Outcome:** Complete — 29 new tests pass (166 total), gradle build clean

**What was done:**
Added 13 functions to `Seg006.kt` (Phase 8c — falling/grabbing/damage/objects):
- Falling/grabbing: `checkAction` (dispatches to checkGrab/doFall/checkOnFloor by action), `checkGrab` (midair grab with Shift, super high jump support), `checkGrabRunJump` (USE_JUMP_GRAB — grab during jump), `fellOut` (death when falling to room 0)
- Damage/health: `takeHp` (subtract HP from kid or guard), `checkSpiked` (spike damage during run/jump frames), `checkSpikeBelow` (trigger spike animations below character), `playDeathMusic` (select death sound by context), `drawHurtSplash` (render hurt splash effect)
- Objects/items: `checkPress` (button/loose floor activation by standing/hanging), `onGuardKilled` (demo/Jaffar/regular guard death effects), `checkKilledShadow` (level 12 shadow death special event), `addSwordToObjtable` (add sword sprite to render table)

Also added `superJumpFall` variable to `GameState.kt`.

Total: 77 of 81 seg006 functions implemented. Remaining 7 functions for phase 8d (play_kid, control_kid, do_demo, play_guard, user_control, control_guard_inactive, char_opp_dist — already done in 8a: inc_curr_row).

29 new tests covering: takeHp (kid partial/lethal/overkill, guard partial/lethal), fellOut (room 0 death, alive check, room check), playDeathMusic (shadow/fighting/regular), onGuardKilled (demo/Jaffar/regular/shadow), checkKilledShadow (level 12, wrong level), drawHurtSplash (kid dead, chomped skip), addSwordToObjtable (drawn/sheathed), checkAction (freefall/hangclimb), checkPress (hanging opener, floor no-trigger), checkSpiked (harmful/non-spike), checkGrab (no shift, too fast).

**Design decisions:**
- All `#ifdef` fix paths translated as runtime checks (fixGrabFallingSpeed, enableSuperHighJump, enableJumpGrab, fixStandOnThinAir, fixDeadFloatingInAir, fixPressThroughClosedGates, fixInfiniteDownBug, fixChompersNotStarting)
- `check_grab_run_jump` returns Boolean instead of C's int 0/1 — idiomatic Kotlin, callers use it in boolean context
- `draw_hurt_splash` C expression `(Char.charid == charid_0_kid) << 2` translated to explicit if/else for clarity

**Contract changes:** None.

---

### 2026-04-04 — Phase 8b, Step 1: Falling, collision, floor checks, clipping

**Mode:** Code (autonomous)
**Outcome:** Complete — 18 new tests pass (137 total), gradle build clean

**What was done:**
Added 8 functions to `Seg006.kt` (Phase 8b — physics/collision/clipping):
- Falling: `fallAccel` (with feather fall fix), `fallSpeed` (applies Y velocity + X movement in freefall)
- Collision: `setCharCollision` (computes collision bounds from image/frame), `checkOnFloor` (floor check + level 12 special floors + thin air fix)
- Wall/clipping: `inWall` (wall collision adjustment), `clipChar` (character clipping against walls/doors/level door), `stuckLower` (stuck tile Y adjustment)
- Falling entry: `startFall` (determines fall sequence from current frame, handles guard/kid pushed-off-ledge, tapestry fix)

Total: 64 of 81 seg006 functions implemented. Remaining 17 functions for phases 8c-8d.

18 new tests covering: fall acceleration (normal, capped, feather, guard-with-fix), fall speed, collision setup (null image, with image, thin frame), inWall, checkOnFloor (no flag, with floor), stuckLower (stuck tile, non-stuck), clipChar (exit stairs, reset).

**Design decisions:**
- `startFall` included in 8b (not 8c as originally planned) because `checkOnFloor` calls it directly — they are tightly coupled
- `FIX_FEATHER_FALL_AFFECTS_GUARDS` included as runtime check on `gs.fixes.fixFeatherFallAffectsGuards`
- `USE_SUPER_HIGH_JUMP` paths excluded (matching reference build — `enableSuperHighJump` defaults to 0)
- `FIX_RUNNING_JUMP_THROUGH_TAPESTRY` included in `startFall` as runtime check

**Contract changes:** None.

---

### 2026-04-04 — Phase 8a, Step 1: Constants, frame tables, tile/room queries, state management

**Mode:** Code (autonomous)
**Outcome:** Complete — 42 new tests pass (119 total), gradle build clean

**What was done:**
Created `Seg006.kt` (Phase 8a — ~45 functions of 81 total) containing:
- Frame data tables: `frameTableKid` (241 entries), `frameTblGuard` (41), `frameTblCuts` (86), `swordTbl` (51)
- Tile lookup tables: `tileDivTbl` (256), `tileModTbl` (256) with DOS overflow simulation
- Constants: `SEQTBL_BASE` (0x196E)
- Tile/room query functions (16): `getTile`, `findRoomOfTile`, `getTilepos`, `getTileposNominus`, `getTileDivModM7`, `getTileDivMod`, `yToRowMod4`, `getTileAtChar`, `getTileAboveChar`, `getTileBehindChar`, `getTileInfrontofChar`, `getTileInfrontof2Char`, `getTileBehindAboveChar`, `getTileFrontAboveChar`, `tileIsFloor`, `wallType`
- Character state save/load (16): `loadkid`, `savekid`, `loadshad`, `saveshad`, `loadkidAndOpp`, `savekidAndOpp`, `loadshadAndOpp`, `saveshadAndOpp`, `clearChar`, `resetObjClip`, `saveObj`, `loadObj`, `saveCtrl1`, `restCtrl1`, `clearSavedCtrl`, `readUserControl`
- Frame loading (4): `loadFrame`, `getFrameInternal`, `loadFramDetCol`, `determineCol`
- Physics/distance (5): `dxWeight`, `charDxForward`, `objDxForward`, `distanceToEdgeWeight`, `distanceToEdge`
- Play sequence: `playSeq` (full seqtbl instruction interpreter, matching C implementation)
- Additional functions: `flipControlX`, `releaseArrows`, `procGetObject`, `isDead`, `doPickup`, `setObjtileAtChar`, `canGrab`, `canGrabFrontAbove`, `backDeltaX`, `incCurrRow`

Created `ExternalStubs.kt` — stub/interface pattern for functions from other segments (seg000/002/003/005/007/008/replay). Uses `var` function references so real implementations can be wired in when modules are translated. Key stubs include `getRoomAddress` (inlined from seg008) and rendering no-ops.

Added `Colors` object to `Enums.kt` (RED, BRIGHTYELLOW, BRIGHTWHITE).
Added `obj2*` save/restore state variables to `GameState.kt`.

Created `Seg006Test.kt` with 42 unit tests covering:
- Frame table sizes and spot-check entries
- Tile queries: room 0 edge, valid room lookup, wrapping (left/right/up/down)
- getTilepos: valid, negative row, out-of-bounds
- tileIsFloor/wallType return values
- State save/load round-trips: Kid, Guard, Opp, Object
- Control input: readUserControl, flipControlX, releaseArrows, saveCtrl1/restCtrl1
- Frame loading: Kid standing, Guard, out-of-bounds
- Physics: charDxForward, objDxForward, getTileDivMod
- isDead, clearChar, incCurrRow

**Design decisions:**
- FIX_CORNER_GRAB enabled: `findRoomOfTile` checks row<0 before col<0 (matching reference build)
- FIX_SPRITE_XPOS enabled: `xToXhAndXl` uses shift/mask (not conditional logic)
- All `#ifdef` paths from reference build included; runtime checks against `gs.fixes.*`/`gs.custom.*`
- `getRoomAddress` inlined in ExternalStubs rather than stubbed, since it's a simple array copy needed for tile queries to work in tests

**Contract changes:** None.

---

## Module 7: Sequence Table

### 2026-04-04 — Step 7c: Validation and review (phase complete)

**Mode:** Review (autonomous)
**Outcome:** Complete — Module 7 fully validated and closed

**What was done:**
Cross-reference validation of Kotlin sequence table against C source:
- All 108 label offsets verified to match C `#define` chain exactly
- All 115 `seqtblOffsets` entries verified to match C `seqtbl_offsets[]` (same labels, same order)
- Array size confirmed: 2,310 bytes (Mclimb_loop at offset 2306 + 4 bytes)
- No `USE_TELEPORTS` — standard PoP behavior, matching C `#ifdef` exclusion
- All 77 tests pass, `gradle build` clean
- No dead code, no simplification needed — helpers mirror C macros cleanly

**Contract changes:** None.

---

### 2026-04-04 — Step 7b: Sequence table data

**Mode:** Code (autonomous)
**Outcome:** Complete — 20 new tests pass (77 total), gradle build clean

**What was done:**
Created `SequenceTable.kt` containing:
- `SEQTBL_BASE` constant (0x196E) and 108 label offset constants matching C `#define` chain
- Builder helpers (`act()`, `jmp()`, `dx()`, `dy()`, `snd()`, `setFall()`, `jmpIfFeather()`) mirroring C macros
- `seqtbl` IntArray (2,310 bytes) built programmatically, matching C `seqtbl[]` byte-for-byte
- `seqtblOffsets` IntArray (115 entries) mapping sequence IDs to absolute addresses
- Self-verification: `check()` assertion on array size at init time

All sequences translated from seqtbl.c including: running, standing, jumping, climbing, hanging, falling, combat (strike/parry/block), stepping (1-14 pixel steps), crouch, drink potion, sword pickup/sheathe, death sequences, stairs, and NPC sequences (Vizier, Princess, Mouse).

Used standard PoP behavior for `superhijump` (not USE_SUPER_HIGH_JUMP). Teleport sequence excluded (not in base PoP).

Test file: `SequenceTableTest.kt` with 20 tests covering array size, offset lookup validation, byte pattern verification, address range validation, and negative value encoding.

**Contract changes:** None — internal to Module 7.

---

### 2026-04-04 — Step 7a: Prerequisite enums

**Mode:** Code (autonomous)
**Outcome:** Complete — 30 new tests pass (57 total), gradle build clean

**What was done:**
Added four enum objects to `Enums.kt` for sequence table translation:
- `SeqtblInstructions` — 15 opcodes (SEQ_END_LEVEL=0xF1 through SEQ_JMP=0xFF)
- `SeqtblSounds` — 5 sound IDs (SND_SILENT=0 through SND_LEVEL=4)
- `FrameIds` — 170 frame ID constants (frame_0 through frame_240_sheathe, matching C gaps: no 66, 100-101, 120, 176, 184, 189-190, 206, 211-216)
- `SeqIds` — 98 sequence ID constants (seq_1 through seq_teleport=115, including USE_SUPER_HIGH_JUMP and USE_TELEPORTS entries)

All values verified against `types.h` enum definitions. Test file: `SeqtblEnumsTest.kt` with spot-checks across all four groups.

**Contract changes:** None — internal to Module 7.

---

## Module 6: State Model

### 2026-04-04 — Phase 6a+6b+6c+6d: Core types, enums, globals, tests (Loop iteration 2)

**Mode:** Code (autonomous)
**Outcome:** Complete — all 27 tests pass, gradle build clean

**What happened:**
Implemented all four phases of Module 6 (State Model) in a single iteration:

**Phase 6a — Core Game Types** (`Types.kt`):
- Type aliases documented (byte→Int, sbyte→Int, word→Int, short→Short, dword→Long)
- 11 data classes: `CharType` (16 bytes), `TrobType` (3 bytes), `MobType` (6 bytes), `LevelType` (2305 bytes), `FrameType` (5 bytes), `RectType`, `TileAndMod`, `LinkType`, `AutoMoveType`, `SwordTableType`, `ObjtableType`
- `CharType.copyFrom()` utility for Kid↔Char swapping pattern used throughout game logic
- `LevelType` has proper `equals`/`hashCode` for IntArray fields

**Phase 6b — Game Enums** (`Enums.kt`):
- Used Kotlin `object` with `const val` (not enum classes) to match C integer semantics
- Translated: `Tiles` (0-30), `CharIds`, `SwordStatus`, `Directions`, `Actions`, `FrameFlags`, `SoundIds`, `SoundFlags`, `Chtabs`, `Blitters`, `Control`, `EdgeType`, `TileGeometry`, `Falling`, `GameConstants`

**Phase 6c — Global State Object** (`GameState.kt`, `CustomOptions.kt`):
- `GameState` singleton with all ~344 game-logic globals as mutable properties
- Organized by category: character, level/room, hit points, timer, RNG, controls, trobs, mobs, guard, sword/combat, sound, position/collision, falling, frame/animation, rendering, doors, game flow, replay, configuration
- Constant arrays: `yLand`, `xBump`, `tblLine`, `dirFront`, `dirBehind`, `yClip`, `soundInterruptible`, `chtabFlipClip`, `chtabShift`
- `CustomOptionsType` with all default values matching C `custom_defaults` initializer
- `FixesOptionsType` with all 48 bug-fix toggle fields
- SDL/platform types correctly excluded (surfaces, textures, renderers, joystick, font, dialog)

**Phase 6d — Compile & Structural Verification** (`TypesTest.kt`):
- 18 tests: struct size assertions, enum value spot-checks, default value verification, constant array validation, `CharType.copyFrom` test
- Combined with 9 existing P1RParser tests: 27 total, all passing

**Decisions:** None needed — mechanical translation as anticipated.

---



**Mode:** Discuss (autonomous)
**Outcome:** Complete — committed fedbe8e

**What happened:**
Autonomous loop iteration 1 ran /phase-plan for Module 6 (State Model). Agent read CLAUDE.md, PROJECT.md, ARCHITECTURE.md, GOVERNANCE.md, DEVPLAN.md, determined "no active phase," and created a 4-phase breakdown:

- **Phase 6a:** Core Game Types — 11 struct translations (char_type, trob_type, mob_type, level_type, link_type, tile_and_mod, frame_type, auto_move_type, sword_table_type, rect_type) + type aliases
- **Phase 6b:** Game Enums — tiles, charids, actions, directions, sword_status, frame_flags, soundflags, chtabs, blitters
- **Phase 6c:** Global State Object — 344 globals → Kotlin singleton, categorized (oracle-critical, control input, game logic, constant arrays, custom_options_type)
- **Phase 6d:** Compile & Structural Verification — gradle build + unit tests for struct sizes, defaults, enum values

Scope correctly limited to Layer 1 game logic types only — SDL/platform types excluded per ARCHITECTURE.md contract.

**Note:** Iteration hit $1 budget cap after committing but before writing this DEVLOG entry or outputting LOOP_SIGNAL. Budget raised to $10 for subsequent iterations. Logging upgraded to stream-json for full transcript capture.

---

## Autonomous Loop Setup

### 2026-04-03 — Loop Runner and Governance Commands Installed

**Objective:** Set up autonomous loop infrastructure on Pi for Track A execution.

**What happened:**
1. Reviewed all governance/automation docs (AUTOMATION.md, GOVERNANCE.md, autonomous_loop_analysis.md)
2. Evaluated loop runner options: custom bash script vs Ralph Loop plugin
   - Ralph: runs inside one session via stop-hook, persistent context, uses `<promise>` completion
   - Custom: fresh `claude -p` per iteration, matches AUTOMATION.md's cold-start-per-iteration design
   - Decision: custom bash runner — aligns with governance framework's cold-start resilience and auditability model
3. Adapted PowerShell loop runner (run-loop_native.ps1) to bash for Pi: `run-iteration.sh`
4. Installed governance slash commands to `.claude/commands/`:
   - Copied from `~/workspace/e2e/COMMANDS/`: cold-start, integration-check
   - Adapted for autonomous execution: step-done, phase-plan, phase-review, phase-complete
   - Key adaptation: replaced "wait for human" gates with "proceed and log, exit for next iteration"
   - phase-complete retains ESCALATE to preserve human audit at phase boundaries
5. Confirmed git repository already initialized (5 commits, branch master)
6. Updated DEVPLAN: marked git init done, set focus to Module 6 (State Model)
7. Gitignored: autonomous_loop_analysis.md (Windows-era analysis), logs/ directory

**Script features (run-iteration.sh):**
- Single or multi-iteration: `./run-iteration.sh` or `./run-iteration.sh -n 5`
- Auto-numbering from summary.log
- Per-iteration logs: `logs/loop/iteration_NNN.log`
- Summary log: one line per iteration (timestamp, signal, reason)
- Budget cap: $1.00/iteration via `--max-budget-usd`
- Exit codes: 0=CONTINUE, 1=ESCALATE, 2=NO_SIGNAL/ERROR

**Orchestration model:** TG bot session acts as orchestrator — runs iterations via Bash tool, analyzes output, reports to user via Telegram, decides whether to continue.

**Cost estimate per iteration:** ~$0.05–0.14 (cache creation on first, cache reads on subsequent)

**Decisions:**
| # | Decision | Alternatives | Rationale | Confidence |
|---|----------|-------------|-----------|------------|
| 1 | Custom bash runner over Ralph plugin | Ralph (persistent context, stop-hook), standalone bash loop (no orchestrator) | Framework assumes fresh context per iteration for cold-start resilience and auditability. Ralph's persistent context bypasses this. | High |
| 2 | TG bot session as orchestrator | Dumb bash script + TG notifications, separate orchestrator process | User prefers more info and analysis between iterations over speed. Single TG session avoids channel conflicts. | High |
| 3 | Adapt slash commands for autonomous mode | Run without commands (agent follows AUTOMATION.md directly), install commands unmodified | Commands provide structured checklists; autonomous adaptations remove human-wait gates while preserving the process steps. | High |

---

## Pi Migration

### 2026-04-03 — Migration from Windows to Raspberry Pi Complete

**Objective:** Move project from Windows/Sandboxie environment to Raspberry Pi 5 (Incus container) for full shell access and true autonomous operation.

**What happened:**
1. Installed build dependencies: JDK 17.0.18, Gradle 8.12, SDL2 2.26.5 dev libs, GCC 12.2, Python 3.11, xvfb, dos2unix
2. Converted source files from CRLF to LF (dos2unix) — NTFS preserved Windows line endings, causing ripgrep/Grep tool to silently fail on all searches
3. Compiled SDLPoP on ARM64 Linux (clean build, only 1 minor warning in seg000.c)
4. Discovered NTFS mount doesn't support execute permissions (chmod +x silently ignored). Workaround: copy binary to /tmp/sdlpop/ with symlinks to data/replays/doc
5. Tested headless modes: `SDL_VIDEODRIVER=dummy` hangs indefinitely, `xvfb-run` also hangs. **`SDL_VIDEODRIVER=offscreen`** works perfectly
6. Built instrumented binary with `-DDUMP_FRAME_STATE -DUSE_REPLAY` — instrumentation code was already in seg000.c and replay.c from Phase 1
7. Generated all 13 reference traces on ARM64. All replay durations match expected tick counts
8. Verified determinism: two runs of basic_movement.p1r produce byte-identical traces
9. Verified trace comparator: `compare_traces.py` reports MATCH on identical traces
10. Verified Kotlin toolchain: `gradle build` and `gradle test` pass (9/9 P1R parser tests)
11. Updated CLAUDE.md: removed Sandboxie shell constraint, added Pi environment section with NTFS workaround and build commands
12. Updated DEVPLAN.md: marked migration complete, updated gotchas for Pi environment

**Key discoveries (promoted to DEVPLAN Gotchas):**
- `SDL_VIDEODRIVER=offscreen` is the correct headless driver (not `dummy`)
- NTFS silently ignores chmod — must copy binaries to native filesystem for execution
- `dos2unix` required after Windows→Linux file transfer — CRLF breaks ripgrep

**Trace sizes (ARM64, matching DEVLOG Phase 3 Step 2 expectations):**

| Trace | Bytes | Frames |
|-------|------:|-------:|
| basic_movement | 123,070 | 397 |
| falling | 30,070 | 97 |
| traps | 70,370 | 227 |
| sword_and_level_transition | 97,340 | 314 |
| demo_suave_prince_level11 | 76,880 | 248 |
| falling_through_floor_pr274 | 25,110 | 81 |
| grab_bug_pr288 | 9,300 | 30 |
| grab_bug_pr289 | 21,390 | 69 |
| original_level12_xpos_glitch | 20,460 | 66 |
| original_level2_falling_into_wall | 28,830 | 93 |
| original_level5_shadow_into_wall | 54,250 | 175 |
| snes_pc_set_level11 | 32,860 | 106 |
| trick_153 | 14,570 | 47 |

**Remaining:** `git init` + initial commit.

**Time:** ~30 minutes

---

## Phase 3: Test Infrastructure

### 2026-03-18 — Phase 3 Complete

**Objective:** Build test infrastructure for validating Kotlin port against C original.

**Deliverables — all met:**
1. ✅ `REPLAY_COVERAGE.md` — coverage matrix for 13 replays (4 user-recorded, 9 regression)
2. ✅ `SDLPoP/traces/reference/` — 13 reference state traces (~604 KB total)
3. ✅ `TRACE_FORMAT.md` — binary trace format spec (310 bytes/frame)
4. ✅ `tools/compare_traces.py` — frame-by-frame comparator with field-level divergence reporting
5. ✅ `SDLPoP-kotlin/` — Gradle project (Kotlin 1.9.22, JVM 17), `gradle build` + `gradle test` pass
6. ✅ `SDLPoP-kotlin/docs/P1R_FORMAT.md` — .P1R binary format spec
7. ✅ `SDLPoP-kotlin/src/main/kotlin/com/sdlpop/replay/P1RParser.kt` — parser (9/9 tests pass)

**Bugs found and fixed during validation:**
- `CHAR_TYPE_SIZE`: 17→16 (actual C struct is 14 bytes + 1 word = 16)
- `start_level` in P1R parser: `buffer.int` (4 bytes) → `buffer.short` (2 bytes)
- JUnit Platform launcher missing from Gradle 9.x classpath
- `rem_min`, `trobs_count`, `mobs_count` decoded as unsigned, corrected to signed

**Phase review cleanup:**
- Removed unused imports (`BinaryIO`, `Tuple`, `IOException`)
- Removed dead code (3 unreachable `< 0` checks on unsigned values)
- Replaced silent test skips with `assumeTrue()` (5 locations)
- Removed redundant `assertNotNull` calls (3 locations)
- Removed redundant `kotlin("stdlib")` dependency
- Fixed TRACE_FORMAT.md offsets (char_type 17→16 cascaded through all fields)
- Fixed P1R_FORMAT.md start_level (int32→word)

**Environment findings:**
- Sandboxie `3pAgentBox` blocks shell in Claude CLI (ConsoleInit not implemented). Workaround: HUMAN_STEPS.md handoff pattern for shell tasks.
- Chocolatey corporate proxy doesn't have `temurin17` or `gradle`. Use `openjdk17` + manual Gradle install.

**Deferred:**
- Game loop replay runner (Kotlin version of SDLPoP's play_frame loop) — deferred to first file translation phase.

**Time:** ~4 hours across multiple sessions (planning, 6 steps, review)

---

### 2026-03-17 — Step 5 Handoff: Test Validation Required

**Objective:** Build .P1R replay file parser in Kotlin.

**What happened:**
1. Analyzed SDLPoP source code (replay.c, types.h) to understand .P1R binary format:
   - Header: magic "P1R", format class, version, deprecation, creation time
   - Variable-length strings: levelset name, implementation name
   - Embedded savestate buffer
   - 5 options sections (features, enhancements, fixes, custom general, custom per-level)
   - Trailer: start level, random seed, num_replay_ticks, moves array
   - All multi-byte values are little-endian
2. Created `P1R_FORMAT.md` documenting complete binary structure:
   - Field-by-field layout with byte offsets
   - Move encoding (bitfield: x, y, shift, special)
   - Validation rules
   - Source code references
3. Implemented `P1RParser.kt`:
   - `ReplayData` data class with all parsed fields
   - `P1RParser.parseP1R()` function using ByteBuffer (little-endian)
   - `P1RParseException` for invalid files
   - Validation: magic number, format class (0), version (≥101), deprecation (≤2)
   - Handles variable-length strings correctly
   - Verifies entire file consumed (no trailing data)
4. Created `P1RParserTest.kt` with comprehensive test suite:
   - Parse all 4 user replays (basic movement, falling, traps, sword and level transition)
   - Parse all 9 test case replays in `SDLPoP/doc/replays-testcases/`
   - Validation tests: reject invalid magic, wrong format class, old version, future deprecation

**Artifacts:**
- `SDLPoP-kotlin/docs/P1R_FORMAT.md` — binary format specification
- `SDLPoP-kotlin/src/main/kotlin/com/sdlpop/replay/P1RParser.kt` — parser implementation
- `SDLPoP-kotlin/src/test/kotlin/com/sdlpop/replay/P1RParserTest.kt` — test suite

**Escalation reason:** Step 5 code complete, but acceptance criteria require running `gradle test` to validate. HUMAN_STEPS.md updated with test commands.

**Next:** Human runs `gradle test`, confirms all tests pass, deletes HUMAN_STEPS.md, re-runs loop → Step 5 marked complete, proceed to Step 6 (phase documentation).

**Time:** ~15 minutes (analysis + implementation)

---

### 2026-03-17 — Step 4 Handoff: Gradle Build Validation Required

**Objective:** Set up Kotlin project structure with Gradle.

**What happened:**
1. Created `SDLPoP-kotlin/` directory structure manually (shell blocked by Sandboxie)
2. Created Gradle project files:
   - `settings.gradle.kts` — project name configuration
   - `build.gradle.kts` — Kotlin JVM 1.9.22, Java target 17, application plugin
   - `.gitignore` — Gradle, IDE, and Kotlin artifacts
3. Created package structure with placeholders:
   - `src/main/kotlin/com/sdlpop/replay/` — for .P1R parser (Step 5)
   - `src/main/kotlin/com/sdlpop/game/` — for game logic translations (later phase)
   - `src/main/kotlin/com/sdlpop/oracle/` — for state trace writer (later phase)
   - `src/test/kotlin/com/sdlpop/` — for unit tests
4. Created `README.md` documenting structure, build config, status

**Artifacts:**
- `SDLPoP-kotlin/build.gradle.kts` — Gradle build configuration
- `SDLPoP-kotlin/settings.gradle.kts` — project settings
- `SDLPoP-kotlin/.gitignore` — Git ignore rules
- `SDLPoP-kotlin/src/main/kotlin/com/sdlpop/{replay,game,oracle}/` — package structure
- `SDLPoP-kotlin/README.md` — project documentation

**Escalation reason:** Step 4 file structure complete, but acceptance criteria require running `gradle build` to validate. HUMAN_STEPS.md updated with validation command.

**Next:** Human runs `gradle build` validation, confirms success, deletes HUMAN_STEPS.md, re-runs loop → Step 4 marked complete, proceed to Step 5.

**Time:** ~3 minutes (file creation)

---

### 2026-03-17 — Step 3 Complete: Trace Comparator Validated

**Objective:** Validate and fix state trace comparator tool.

**What happened:**
1. CLI loop created `tools/compare_traces.py`, `TRACE_FORMAT.md`, `tools/test_comparator.sh`
2. Ran validation via Devmate (shell blocked for CLI loop by Sandboxie)
3. **Bug found:** `CHAR_TYPE_SIZE = 17` was wrong — actual `char_type` is **16 bytes** (14 single-byte fields + 1 word). Off-by-one per character × 3 characters = 3 bytes excess, causing `random_seed` field to overrun the buffer.
4. Fixed: `CHAR_TYPE_SIZE = 16`. Assert now passes, total offset = 310 = FRAME_SIZE.
5. All tests pass:
   - Self-match: all 13 traces → MATCH ✅
   - Corrupted byte: `DIVERGENCE at frame 5: Field: Kid.x: expected 77, got 255` ✅
   - Length mismatch: `DIVERGENCE: Frame count mismatch` ✅

**Lessons learned:** Always verify struct sizes against actual C typedefs. The `char_type` has 14 single-byte fields + 1 `word` (2 bytes) = 16 bytes, not 17.

**Next:** Step 4 — Set Up Kotlin Project Structure

**Time:** ~10 minutes (validation + fix via Devmate)

---

### 2026-03-17 — Step 3 Handoff: Shell Validation Required

**Objective:** Validate state trace comparator tool.

**What happened:**
1. Created `TRACE_FORMAT.md` documenting binary trace structure:
   - 310 bytes per frame
   - Field-by-field layout with offsets
   - Type sizes and byte ordering
   - Calculated from dump_frame_state() implementation in seg000.c
2. Created `tools/compare_traces.py` (Python, stdlib only):
   - Parses binary traces frame-by-frame
   - Compares field-by-field with human-readable diff
   - Handles char_type, trob_type, mob_type nested structures
   - Exit codes: 0=match, 1=diverge, 2=error
   - 250 lines with full error handling
3. Created `tools/test_comparator.sh` validation suite:
   - Test 1: identical traces → MATCH
   - Test 2: corrupted byte → reports exact frame/field
   - Test 3: length mismatch detection

**Artifacts:**
- `TRACE_FORMAT.md` — complete binary format spec
- `tools/compare_traces.py` — working comparator
- `tools/test_comparator.sh` — test suite

**Escalation reason:** Step 3 code complete, but acceptance criteria require running shell commands to validate. HUMAN_STEPS.md updated with test commands.

**Next:** Human runs validation tests, confirms comparator works correctly, re-runs loop → Step 3 marked complete.

**Time:** ~10 minutes (file work)

---

### 2026-03-17 — Step 2 Complete: Reference Traces Generated

**Objective:** Generate reference state traces from all 13 replays.

**What happened:**
1. Discovered correct SDLPoP replay invocation: `.\prince.exe validate "path.p1r" seed=12345`
   - `validate` and replay path must be **separate arguments** (not `validate="path"`)
   - `check_param` in seg009.c skips args with `.` in them (line 375), so `validate=file.p1r` gets matched by the .p1r extension check first
   - PowerShell `<` stdin redirection doesn't work (reserved operator), and SDLPoP reads replays by path anyway (not stdin)
   - Output file is `state_trace.bin` (not `frame_state.bin`)
2. Ran all 13 replays in validate mode (headless, no GUI) via Devmate
3. All replays completed with "Play duration matches replay length"

**Traces generated (SDLPoP/traces/reference/):**

| Trace File | Bytes | Ticks | Level(s) |
|-----------|------:|------:|----------|
| basic_movement.trace | 123,070 | 397 | 1 |
| falling.trace | 30,070 | 97 | 1 |
| traps.trace | 70,370 | 227 | 1 |
| sword_and_level_transition.trace | 97,340 | 314 | 1→2 |
| demo_suave_prince_level11.trace | 76,880 | 248 | 11 |
| falling_through_floor_pr274.trace | 25,110 | 81 | 2 |
| grab_bug_pr288.trace | 9,300 | 30 | 1 |
| grab_bug_pr289.trace | 21,390 | 69 | 2 |
| original_level12_xpos_glitch.trace | 20,460 | 66 | 12 |
| original_level2_falling_into_wall.trace | 28,830 | 93 | 2 |
| original_level5_shadow_into_wall.trace | 54,250 | 175 | 5 |
| snes_pc_set_level11.trace | 32,860 | 106 | 11 |
| trick_153.trace | 14,570 | 47 | 1 |

**Lessons learned (promoted to DEVPLAN Gotchas):**
- SDLPoP replay invocation syntax: separate arguments, not `key=value`
- Output filename: `state_trace.bin`
- PowerShell stdin redirection `<` is a reserved operator

**Next:** Step 3 — Build State Trace Comparator (Python)

**Time:** ~15 minutes (via Devmate, all 13 replays)

---

### 2026-03-17 — Step 2 Handoff: Shell Commands Required

**Objective:** Generate reference state traces from all 13 replays using instrumented SDLPoP build.

**What happened:**
Step 2 requires shell execution (running prince.exe, moving files, batch processing 13 replays). Per CLAUDE.md automation rules, Bash tool is blocked by Sandboxie (ConsoleInit not implemented in 3pAgentBox).

**Action taken:**
Created `HUMAN_STEPS.md` with complete shell command sequence:
1. Verify DUMP_FRAME_STATE build from Phase 1 still exists (or rebuild if needed)
2. Create `traces/reference/` directory
3. Run all 13 replays through instrumented build with `seed=12345`
4. Move generated `frame_state.bin` to named trace files
5. Verify all traces generated successfully
6. Note file sizes for REPLAY_COVERAGE.md duration update

**Escalation reason:** HUMAN_STEPS.md has pending commands (Step 2 execution).

**Next:** Human runs commands in HUMAN_STEPS.md, deletes file, re-runs loop → autonomous execution resumes at Step 3 (Build State Trace Comparator).

**Time:** ~3 minutes (command sequence authoring)

---

### 2026-03-17 — Step 1 Complete: Replay Coverage Inventory

**Objective:** Document what mechanics each existing replay exercises.

**What happened:**
1. Used Glob tool to enumerate all replay files (no shell commands needed)
2. Found 4 user-recorded replays in `SDLPoP/replays/`:
   - basic movement.p1r
   - falling.p1r
   - traps.p1r
   - sword and level transition.p1r
3. Found 9 regression test replays in `SDLPoP/doc/replays-testcases/`
4. Created `REPLAY_COVERAGE.md` with:
   - Coverage matrix for all 13 replays
   - Mechanics analysis (what's covered vs gaps)
   - Duration placeholder (TBD in Step 2 when traces generated)
   - Recommendations for additional replays if needed

**Coverage findings:**
- **Well covered:** Basic movement, vertical movement (fall/grab/climb), traps, combat, level transitions, edge cases
- **Gaps identified:** Potion pickups, door opening, multiple guard types, death/respawn, save/load, timer mechanics, mirror room

**Artifacts:**
- `REPLAY_COVERAGE.md` — comprehensive coverage analysis

**Step 1 acceptance criteria:** ✅ All met
- [x] REPLAY_COVERAGE.md exists
- [x] All replays in SDLPoP/replays/ documented
- [x] Coverage gaps identified

**Next:** Step 2 — Generate reference state traces from all replays

**Time:** ~5 minutes (autonomous iteration)

---

### 2026-03-17 — Sandboxie Root Cause + Loop Adaptation

**Objective:** Validate native CLI autonomous loop; diagnose shell hanging.

**What happened:**
1. Ran 4-iteration CLI validation test via `claude -p` from VS Code terminal
2. Iteration 1 (context load + file write): **PASS** — TEST_REPORT.md created correctly
3. Iteration 2 (shell commands): **FAIL** — all 4 attempted commands timed out

**Root cause identified:** Corporate Sandboxie sandbox (`3pAgentBox`) does not implement `ConsoleInit` or `OpenDesktop` Win32 services. Claude CLI's `Bash(*)` tool spawns `pwsh.exe`/`cmd.exe` child processes that require these services to initialize. Processes hang indefinitely, creating zombie processes (user found ~12 hanging bash/git processes in Task Manager from prior attempts).

**Why Devmate works:** VS Code's extension host process management bypasses the ConsoleInit limitation. Devmate's `execute_command` tool works normally.

**Resolution:** Updated CLAUDE.md automation section with:
- Explicit "DO NOT execute shell commands" constraint
- `HUMAN_STEPS.md` handoff pattern: loop writes commands for human to run
- ESCALATE trigger when HUMAN_STEPS.md has pending commands
- Removed "Commit per step" (git requires shell)

**This supersedes the 2026-03-17 entry below** — the "Windows/MSYS2/bash configuration issue" hypothesis was incorrect. Root cause was always Sandboxie.

---

### 2026-03-17 — Step 1 Blocked: Bash Command Hanging (superseded)

**Objective:** Inventory replay files to document coverage.

**What happened:**
Attempted to execute Step 1 (Inventory Replay Coverage) but encountered systemic Bash command timeout issue:
- Multiple approaches tried: `find`, `ls`, `git status`
- All commands start in background but never complete (timeout after 5-30 seconds)
- ~~Pattern suggests Windows/MSYS2/bash environment configuration issue~~ **Root cause: Sandboxie — see entry above**

**Attempted commands:**
1. `find SDLPoP/replays -name "*.p1r"` → timeout
2. `ls SDLPoP/replays/` → timeout
3. `git status` → timeout

**Blocking issue:** Cannot execute any Bash commands to inventory files or check repository state.

**Escalation reason:** Technical environment issue prevents autonomous work. Need human guidance on:
- Is this a known Windows/bash configuration issue?
- Alternative approach to inventory files without Bash?
- Should the environment be reconfigured?

**Next:** Human intervention required to resolve Bash execution environment.

---

### 2026-03-14 — Phase 3 Planning

**Objective:** Build test infrastructure for validating Kotlin port against C original.

**Planning decisions:**
1. **Approach:** Option C (Hybrid) from planning discussion
   - Build core tools now (traces, comparator, Kotlin setup, parser)
   - Defer game loop runner until first file translation
   - Rationale: Don't build infrastructure for code that doesn't exist yet
2. **Replay coverage:** User will record comprehensive test suite
   - Required mechanics: movement, combat, traps, level transitions, pickups
   - Existing replays evaluated: `first run.p1r` + 9 bug regression tests in `doc/replays-testcases/`
   - Decision: Record fresh mechanics coverage, supplement with existing edge case tests
3. **Tool stack decisions:**
   - State trace comparator: Python (stdlib only, portable, good binary handling)
   - Kotlin build: Gradle with Kotlin DSL, JVM target 17 (Android-compatible)
   - Project structure: `SDLPoP-kotlin/` sibling to `SDLPoP/`

**Phase breakdown (6 steps):**
1. Inventory replay coverage (document what each replay tests)
2. Generate reference state traces from C build (all replays)
3. Build state trace comparator tool (Python)
4. Set up Kotlin project structure (Gradle + package layout)
5. Build .P1R replay file parser (Kotlin)
6. Documentation and phase completion

**Current status:** Step 1 blocked waiting for user to record replays. DEVPLAN updated with detailed step-by-step plan.

**Next:** User records replays → execute Step 1 (inventory) → autonomous execution of Steps 2-6.

**Time:** ~20 minutes (planning session)

---

## Phase 2: Target Language Decision

### 2026-03-14 — Phase 2 Complete

**Objective:** Choose and validate the target language for porting SDLPoP's game logic.

**What happened:**
1. Evaluated need for formal spike (seg004 translation proof-of-concept)
2. Determined uncertainty is low:
   - Kotlin's integer overflow semantics are well-defined (signed types wrap)
   - PoP's math is simple (8-bit era: mostly add/sub/compare)
   - Replay oracle will catch semantic mismatches during first file translation anyway
3. Decided against duplicating effort — validation will occur naturally during Phase 3 implementation

**Decision: Q2 CLOSED**
**Target language: Kotlin**

**Rationale:**
- Native Android platform integration
- Predictable integer semantics (signed Byte/Short/Int wrap on overflow, similar to C)
- Explicit type conversions (.toByte(), .toInt()) reduce silent bugs
- Different enough from C to meaningfully test the autonomous porting methodology
- Good tooling and Android SDK support

**Validation approach:**
Deferred to first file translation in Phase 3. If integer semantic issues cause replay oracle failures, we'll reassess. The oracle provides immediate feedback on behavioral correctness.

**Next:** Phase 3 — Test Infrastructure

**Time:** ~5 minutes (design decision, not spike)

---

## Phase 1: Replay Oracle Spike

### 2026-03-14 — Phase 1 Complete

**Objective:** Verify that SDLPoP's replay system produces deterministic state traces, validating the test oracle for the porting pipeline.

**What happened:**
1. Implemented `dump_frame_state()` in seg000.c (~60 lines) to capture per-frame game state
2. State captured per frame:
   - Frame number (sync marker)
   - Character structs: `Kid`, `Guard`, `Char` (17 bytes each)
   - Key scalars: `current_level`, `drawn_room`, `rem_min`, `rem_tick`, `hitp_curr/max`, `guardhp_curr/max`
   - Room tile state: `curr_room_tiles[30]`, `curr_room_modif[30]`
   - Animated tiles: `trobs_count` + `trobs[30]`
   - Falling objects: `mobs_count` + `mobs[14]`
   - RNG state: `random_seed`
3. Modified `end_replay()` in replay.c to auto-exit after replay completes (prevents manual timing issues)
4. Built with `-DDUMP_FRAME_STATE` flag
5. Ran `first run.p1r` replay twice with `seed=12345`
6. Binary diff: **FC: no differences encountered**

**Test results:**
- Trace file size: 299,150 bytes (identical both runs)
- Verdict: **DETERMINISTIC ✅**

**Decision: Q1 CLOSED**
The replay oracle works. SDLPoP's game logic is deterministic given the same seed. The autonomous porting pipeline can use replay-based state comparison as its test oracle.

**Artifacts:**
- `SDLPoP/src/seg000.c` — `dump_frame_state()` function (guarded by `DUMP_FRAME_STATE`)
- `SDLPoP/src/replay.c` — auto-exit modification
- `SDLPoP/test_determinism.bat` — automated test script

**Time:** ~1 hour

---

## Phase 0: Environment Setup

### 2026-03-14 — Phase 0 Complete

**Objective:** Clone SDLPoP, set up build environment, verify game runs, obtain test replay.

**What happened:**
1. Cloned SDLPoP from https://github.com/NagyD/SDLPoP
2. Installed MSYS2 via winget
3. Installed MinGW toolchain: `pacman -S mingw-w64-x86_64-gcc mingw-w64-x86_64-SDL2 mingw-w64-x86_64-SDL2_image make`
4. Discovered pkg-config missing — installed `mingw-w64-x86_64-pkgconf`
5. Built with `make all` — compiled all 20 source files, linked to prince.exe (1.08 MB)
6. Copied 30 DLLs from /mingw64/bin (SDL2_image has extensive codec dependencies)
7. Verified game launches and plays Level 1
8. Recorded first replay: `first run.p1r`

**DLLs required (discovered incrementally via missing DLL errors):**

Core SDL2:
- SDL2.dll
- SDL2_image.dll

MinGW runtime:
- libgcc_s_seh-1.dll
- libstdc++-6.dll
- libwinpthread-1.dll

Image format codecs:
- libpng16-16.dll
- libjpeg-8.dll
- libtiff-6.dll
- libwebp-7.dll
- libwebpdemux-2.dll
- libgiflib (not needed — static?)

JPEG XL (libjxl) chain:
- libjxl.dll
- libjxl_cms.dll
- liblcms2-2.dll
- libbrotlicommon.dll
- libbrotlidec.dll
- libbrotlienc.dll
- libhwy.dll

AVIF/AV1 chain:
- libavif-16.dll
- libaom.dll
- libdav1d-7.dll
- librav1e.dll
- libSvtAv1Enc-4.dll
- libyuv.dll
- libsharpyuv-0.dll

Compression:
- zlib1.dll
- libzstd.dll
- liblzma-5.dll
- libdeflate.dll
- libjbig-0.dll
- libLerc.dll

**Artifacts:**
- `SDLPoP/prince.exe` — working build
- `SDLPoP/replays/first run.p1r` — test replay

**Lessons learned (promoted to DEVPLAN Gotchas):**
- SDL2_image on Windows/MinGW requires ~30 DLLs for full codec support
- pkg-config must be installed separately from SDL2
- MSYS2 PATH needs both /mingw64/bin and /usr/bin

**Time:** ~45 minutes (including DLL troubleshooting)
