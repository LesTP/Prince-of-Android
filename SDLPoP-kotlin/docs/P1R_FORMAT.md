# .P1R Replay File Format

Binary file format used by SDLPoP for replay files. All multi-byte integers are little-endian.

## File Structure

| Offset | Size | Type | Field | Description |
|--------|------|------|-------|-------------|
| 0 | 3 | char[3] | magic | Magic number: "P1R" |
| 3 | 2 | word (uint16) | format_class | Implementation identifier (0 for SDLPoP) |
| 5 | 1 | byte (uint8) | version_number | Format version (current: 102) |
| 6 | 1 | byte (uint8) | deprecation_number | Deprecation number (current: 2) |
| 7 | 8 | Sint64 (int64) | creation_time | Unix timestamp (seconds since 1970) |
| 15 | 1 | byte (uint8) | levelset_name_len | Length of levelset name (0 for original) |
| 16 | N | char[N] | levelset_name | Levelset name (not null-terminated in file) |
| 16+N | 1 | byte (uint8) | impl_name_len | Length of implementation name |
| 17+N | M | char[M] | implementation_name | Implementation name (e.g., "SDLPoP v1.22") |
| 17+N+M | 4 | dword (uint32) | savestate_size | Size of embedded savestate |
| 21+N+M | S | byte[S] | savestate_buffer | Game state snapshot |
| 21+N+M+S | variable | | options_sections | Options data (see below) |
| ... | 2 | word (uint16) | start_level | Starting level number |
| ... | 4 | dword (uint32) | random_seed | RNG seed for deterministic playback |
| ... | 4 | dword (uint32) | num_replay_ticks | Number of input frames |
| ... | T | byte[T] | moves | Input sequence (T = num_replay_ticks) |

## Options Sections

After the savestate, there are multiple options sections. Each section has:

| Size | Type | Field | Description |
|------|------|-------|-------------|
| 4 | dword (uint32) | section_size | Size of this section's data |
| N | byte[N] | section_data | Binary options data |

Number of sections in current format: 5
- Features (enable_copyprot, enable_quicksave, etc.)
- Enhancements (use_fixes_and_enhancements, crouch_after_climbing, etc.)
- Fixes (30+ bug fix toggles)
- Custom general settings (start_minutes_left, start_hitp, level config)
- Custom per-level settings (level_type, guard_type, guard_hp tables)

## Moves Format

Each move is encoded as a single byte with bitfields:

```
Bit layout (replay_move_type union):
  bits 0-1: x (sbyte, 2 bits)  — horizontal control (-1, 0, +1)
  bits 2-3: y (sbyte, 2 bits)  — vertical control (-1, 0, +1)
  bit  4:   shift (1 bit)      — shift key held (0 or 1)
  bits 5-7: special (3 bits)   — special moves enum (0-7)
```

Special move values:
- 0: no special move
- 1: MOVE_RESTART_LEVEL
- 2: MOVE_EFFECT_END

## Validation Rules

Valid .P1R files must:
1. Start with magic number "P1R"
2. Have format_class matching the implementation (0 for SDLPoP)
3. Have version_number >= REPLAY_FORMAT_MIN_VERSION (101)
4. Have deprecation_number <= REPLAY_FORMAT_DEPRECATION_NUMBER (2)
5. All string lengths must match actual string sizes
6. File size must equal: fixed header + variable strings + savestate + options + fixed trailer + moves

## References

- Source: `SDLPoP/src/replay.c` (functions: `save_recorded_replay`, `load_replay`, `read_replay_header`)
- Constants: lines 30-36 (magic, class, version, deprecation)
- Move encoding: lines 46-54 (replay_move_type union)
- Write order: lines 838-871 (save_recorded_replay function)
- Read order: lines 922-951 (load_replay function)
