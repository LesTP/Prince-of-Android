# ARCHITECTURE — Prince of Persia Android Port

## Status

**Project phase:** Pre-Discovery (Preparation Sequence)

See `pop_android_port_guide.md` for:
- Codebase analysis (§2)
- Blocking questions (§3)
- Preparation sequence (§4)
- Autonomous porting pipeline design (§5)

## Implementation Sequence

| # | Module | Description | Status |
|---|--------|-------------|--------|
| 0 | Environment Setup | Build SDLPoP, obtain replays | **Phase 0 complete** |
| 1 | Replay Oracle | Instrument state tracing, verify determinism | **Phase 1 complete** |
| 2 | Target Language | Spike: translate seg004 to candidate language | **Phase 2 complete** |
| 3 | Test Infrastructure | State comparator, replay runner, regression suite | **Phase 3 complete** |
| 4 | Control Prototype | Android gesture recognizer + modifier button | Pending |
| 5 | Formal Project Def | PROJECT.md, full ARCHITECTURE.md, ARCH files | Pending |

*Note: Modules 0-5 are preparation phases from pop_android_port_guide.md §4. Full architecture will be defined in Module 5 after blocking questions are resolved.*
