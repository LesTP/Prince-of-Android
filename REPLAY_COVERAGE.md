# Replay Coverage Matrix

## User-Recorded Replays (SDLPoP/replays/)

| Replay File | Level(s) | Mechanics Covered | Duration (frames) | Notes |
|-------------|----------|-------------------|-------------------|-------|
| basic movement.p1r | 1 | Walk, run, turn, standing jump, running jump | TBD | Core movement mechanics |
| falling.p1r | 1 | Falling, ledge grab, climbing up/down, hanging | TBD | Vertical movement and fall physics |
| traps.p1r | 1 | Chompers, spikes, loose floors, gates | TBD | Trap activation and damage |
| sword and level transition.p1r | 1-2 | Sword fighting (advance, retreat, strike, block), guard AI, level transition | TBD | Combat mechanics + level progression |

**User-recorded coverage summary:**
- ✓ Basic movement (walk, run, turn, jump)
- ✓ Vertical movement (fall, grab, climb)
- ✓ Traps (chompers, spikes, loose floors)
- ✓ Combat (sword fighting, guard AI)
- ✓ Level transitions
- ✓ Gate triggers (implied in traps)

**Coverage gaps (user-recorded):**
- Potion pickups (small health, large health, float, flip, hurt, life extension)
- Door opening mechanics
- Multiple guard types (fat guard, skeleton, shadow)
- Special triggers (raise/drop gates, open/close doors)
- Death and respawn
- Save/load game state
- Level-specific mechanics (e.g., mirror room in level 4)

---

## Regression Test Replays (SDLPoP/doc/replays-testcases/)

| Replay File | Level(s) | Mechanics Covered | Duration (frames) | Notes |
|-------------|----------|-------------------|-------------------|-------|
| Demo by Suave Prince level 11.p1r | 11 | Advanced gameplay, late-game mechanics | TBD | Community demo; high-skill play |
| Falling through floor (PR274).p1r | TBD | Edge case: falling through floor bug | TBD | Regression test for PR #274 |
| Grab bug (PR288).p1r | TBD | Edge case: grab mechanic bug | TBD | Regression test for PR #288 |
| Grab bug (PR289).p1r | TBD | Edge case: grab mechanic bug (different case) | TBD | Regression test for PR #289 |
| Original level 12 xpos glitch.p1r | 12 | Edge case: x-position glitch | TBD | Original game behavior preservation |
| Original level 2 falling into wall.p1r | 2 | Edge case: falling into wall | TBD | Original game quirk |
| Original level 5 shadow into wall.p1r | 5 | Shadow mechanics, wall collision edge case | TBD | Original game quirk |
| SNES-PC-set level 11.p1r | 11 | SNES vs PC version differences | TBD | Cross-version compatibility |
| trick_153.p1r | TBD | Advanced trick/glitch | TBD | Community trick showcase |

**Regression replay characteristics:**
- Edge cases and bug reproductions (5 replays)
- Original game quirks preservation (3 replays)
- Advanced/late-game content (2 replays with level 11)
- Cross-version behavior testing (SNES-PC)

---

## Coverage Analysis

### Mechanics Exercised Across All Replays

**Core movement:**
- ✓ Walk, run, turn (basic movement.p1r)
- ✓ Standing jump, running jump (basic movement.p1r)
- ✓ Directional control

**Vertical movement:**
- ✓ Falling (falling.p1r + multiple edge cases)
- ✓ Ledge grab (falling.p1r + grab bugs)
- ✓ Climbing up/down (falling.p1r)
- ✓ Hanging (falling.p1r)

**Combat:**
- ✓ Sword fighting (sword and level transition.p1r)
- ✓ Guard AI (sword and level transition.p1r)
- ✓ Advance, retreat, strike, block (implied in combat replay)

**Traps:**
- ✓ Chompers (traps.p1r)
- ✓ Spikes (traps.p1r)
- ✓ Loose floors (traps.p1r + falling through floor)
- ✓ Gates (traps.p1r)

**Level progression:**
- ✓ Level transitions (sword and level transition.p1r)
- ✓ Multiple levels (2, 5, 11, 12 confirmed)

**Edge cases:**
- ✓ Grab mechanics bugs (2 replays)
- ✓ Falling through floor (1 replay)
- ✓ Wall collision quirks (2 replays)
- ✓ Position glitches (1 replay)

### Known Coverage Gaps

**Not covered by existing replays:**
- Potion pickups (all 6 types)
- Door opening (key usage)
- Fat guard combat
- Skeleton combat
- Shadow combat (distinct from shadow-into-wall replay)
- Death and respawn mechanics
- Timer expiration (60-minute limit)
- Save/load game state
- Mirror room (level 4 special mechanic)
- Pressure plates (open/close doors, raise/drop gates)
- Sword pickup (start of game)

### Recommendations

**High priority additions:**
1. Potion pickup replay (small health, large health at minimum)
2. Door opening replay (key pickup + door interaction)
3. Death and respawn replay
4. Multiple guard types in single replay (fat, skeleton)

**Medium priority:**
1. Pressure plate triggers (separate from loose floors)
2. Timer mechanics (approaching 60-minute limit)
3. Mirror room (level 4)

**Low priority (nice-to-have):**
1. Save/load sequence
2. Float/flip/hurt potions (less common)
3. Life extension potion (rare)

---

## Notes

- **Duration (frames):** Will be populated when reference traces are generated (Step 2)
- **Level detection:** Some regression replays don't indicate level in filename; will be determined from trace analysis or replay metadata
- **Seed consistency:** All trace generation should use same seed (e.g., `seed=12345`) for determinism
- **Replay validation:** Before generating reference traces, verify all replays load and run without errors in instrumented SDLPoP build
