#!/usr/bin/env python3
"""
State Trace Comparator for SDLPoP Replay Oracle

Compares two binary state trace files frame-by-frame and reports the first
divergence with human-readable field-level detail.

Usage:
    python compare_traces.py <reference.trace> <test.trace>

Exit codes:
    0 = traces match exactly
    1 = traces diverge
    2 = error (file not found, invalid format, etc.)
"""

import struct
import sys
from pathlib import Path
from typing import Optional


# Frame structure constants (from TRACE_FORMAT.md)
FRAME_SIZE = 310
CHAR_TYPE_SIZE = 16
TROB_TYPE_SIZE = 3
MOB_TYPE_SIZE = 6
TROBS_MAX = 30
MOBS_MAX = 14


class FrameData:
    """Parsed frame data with field accessors."""

    def __init__(self, raw_bytes: bytes):
        if len(raw_bytes) != FRAME_SIZE:
            raise ValueError(f"Frame must be exactly {FRAME_SIZE} bytes, got {len(raw_bytes)}")
        self.raw = raw_bytes
        self._parse()

    def _parse(self):
        """Parse binary frame data into fields."""
        offset = 0

        # Frame number
        self.frame_number = struct.unpack_from('<I', self.raw, offset)[0]
        offset += 4

        # Characters (Kid, Guard, Char)
        self.kid = self.raw[offset:offset + CHAR_TYPE_SIZE]
        offset += CHAR_TYPE_SIZE

        self.guard = self.raw[offset:offset + CHAR_TYPE_SIZE]
        offset += CHAR_TYPE_SIZE

        self.char = self.raw[offset:offset + CHAR_TYPE_SIZE]
        offset += CHAR_TYPE_SIZE

        # Scalars
        self.current_level = struct.unpack_from('<H', self.raw, offset)[0]
        offset += 2

        self.drawn_room = struct.unpack_from('<H', self.raw, offset)[0]
        offset += 2

        self.rem_min = struct.unpack_from('<h', self.raw, offset)[0]
        offset += 2

        self.rem_tick = struct.unpack_from('<H', self.raw, offset)[0]
        offset += 2

        self.hitp_curr = struct.unpack_from('<H', self.raw, offset)[0]
        offset += 2

        self.hitp_max = struct.unpack_from('<H', self.raw, offset)[0]
        offset += 2

        self.guardhp_curr = struct.unpack_from('<H', self.raw, offset)[0]
        offset += 2

        self.guardhp_max = struct.unpack_from('<H', self.raw, offset)[0]
        offset += 2

        # Room tiles
        self.curr_room_tiles = self.raw[offset:offset + 30]
        offset += 30

        self.curr_room_modif = self.raw[offset:offset + 30]
        offset += 30

        # Trobs
        self.trobs_count = struct.unpack_from('<h', self.raw, offset)[0]
        offset += 2

        self.trobs = self.raw[offset:offset + TROB_TYPE_SIZE * TROBS_MAX]
        offset += TROB_TYPE_SIZE * TROBS_MAX

        # Mobs
        self.mobs_count = struct.unpack_from('<h', self.raw, offset)[0]
        offset += 2

        self.mobs = self.raw[offset:offset + MOB_TYPE_SIZE * MOBS_MAX]
        offset += MOB_TYPE_SIZE * MOBS_MAX

        # RNG state
        self.random_seed = struct.unpack_from('<I', self.raw, offset)[0]
        offset += 4

        assert offset == FRAME_SIZE, f"Parser mismatch: offset={offset}, expected={FRAME_SIZE}"


def parse_char_type(char_bytes: bytes) -> dict:
    """Parse char_type struct into field dict."""
    if len(char_bytes) != CHAR_TYPE_SIZE:
        raise ValueError(f"char_type must be {CHAR_TYPE_SIZE} bytes")

    return {
        'frame': char_bytes[0],
        'x': char_bytes[1],
        'y': char_bytes[2],
        'direction': struct.unpack_from('b', char_bytes, 3)[0],  # signed byte
        'curr_col': struct.unpack_from('b', char_bytes, 4)[0],
        'curr_row': struct.unpack_from('b', char_bytes, 5)[0],
        'action': char_bytes[6],
        'fall_x': struct.unpack_from('b', char_bytes, 7)[0],
        'fall_y': struct.unpack_from('b', char_bytes, 8)[0],
        'room': char_bytes[9],
        'repeat': char_bytes[10],
        'charid': char_bytes[11],
        'sword': char_bytes[12],
        'alive': struct.unpack_from('b', char_bytes, 13)[0],
        'curr_seq': struct.unpack_from('<H', char_bytes, 14)[0],  # word (Uint16) at offset 14
    }


def compare_char_type(ref: bytes, test: bytes, char_name: str) -> Optional[str]:
    """Compare two char_type structs. Returns error message on divergence, None if match."""
    if ref == test:
        return None

    ref_parsed = parse_char_type(ref)
    test_parsed = parse_char_type(test)

    for field, ref_val in ref_parsed.items():
        test_val = test_parsed[field]
        if ref_val != test_val:
            return f"{char_name}.{field}: expected {ref_val}, got {test_val}"

    # Should never reach here if bytes differ but all fields match
    return f"{char_name}: binary mismatch but parsed fields match (padding?)"


def compare_frames(ref_frame: FrameData, test_frame: FrameData) -> Optional[str]:
    """Compare two frames field-by-field. Returns error message on first divergence, None if match."""

    # Quick path: binary identical
    if ref_frame.raw == test_frame.raw:
        return None

    # Frame number
    if ref_frame.frame_number != test_frame.frame_number:
        return f"frame_number: expected {ref_frame.frame_number}, got {test_frame.frame_number}"

    # Characters
    char_diff = compare_char_type(ref_frame.kid, test_frame.kid, "Kid")
    if char_diff:
        return char_diff

    char_diff = compare_char_type(ref_frame.guard, test_frame.guard, "Guard")
    if char_diff:
        return char_diff

    char_diff = compare_char_type(ref_frame.char, test_frame.char, "Char")
    if char_diff:
        return char_diff

    # Scalars
    scalar_fields = [
        ('current_level', ref_frame.current_level, test_frame.current_level),
        ('drawn_room', ref_frame.drawn_room, test_frame.drawn_room),
        ('rem_min', ref_frame.rem_min, test_frame.rem_min),
        ('rem_tick', ref_frame.rem_tick, test_frame.rem_tick),
        ('hitp_curr', ref_frame.hitp_curr, test_frame.hitp_curr),
        ('hitp_max', ref_frame.hitp_max, test_frame.hitp_max),
        ('guardhp_curr', ref_frame.guardhp_curr, test_frame.guardhp_curr),
        ('guardhp_max', ref_frame.guardhp_max, test_frame.guardhp_max),
    ]

    for name, ref_val, test_val in scalar_fields:
        if ref_val != test_val:
            return f"{name}: expected {ref_val}, got {test_val}"

    # Room tiles
    if ref_frame.curr_room_tiles != test_frame.curr_room_tiles:
        for i in range(30):
            if ref_frame.curr_room_tiles[i] != test_frame.curr_room_tiles[i]:
                return f"curr_room_tiles[{i}]: expected {ref_frame.curr_room_tiles[i]}, got {test_frame.curr_room_tiles[i]}"

    if ref_frame.curr_room_modif != test_frame.curr_room_modif:
        for i in range(30):
            if ref_frame.curr_room_modif[i] != test_frame.curr_room_modif[i]:
                return f"curr_room_modif[{i}]: expected {ref_frame.curr_room_modif[i]}, got {test_frame.curr_room_modif[i]}"

    # Trobs
    if ref_frame.trobs_count != test_frame.trobs_count:
        return f"trobs_count: expected {ref_frame.trobs_count}, got {test_frame.trobs_count}"

    if ref_frame.trobs != test_frame.trobs:
        # Compare active trobs only
        for i in range(min(ref_frame.trobs_count, TROBS_MAX)):
            offset = i * TROB_TYPE_SIZE
            ref_trob = ref_frame.trobs[offset:offset + TROB_TYPE_SIZE]
            test_trob = test_frame.trobs[offset:offset + TROB_TYPE_SIZE]
            if ref_trob != test_trob:
                return f"trobs[{i}]: expected {ref_trob.hex()}, got {test_trob.hex()}"

    # Mobs
    if ref_frame.mobs_count != test_frame.mobs_count:
        return f"mobs_count: expected {ref_frame.mobs_count}, got {test_frame.mobs_count}"

    if ref_frame.mobs != test_frame.mobs:
        for i in range(min(ref_frame.mobs_count, MOBS_MAX)):
            offset = i * MOB_TYPE_SIZE
            ref_mob = ref_frame.mobs[offset:offset + MOB_TYPE_SIZE]
            test_mob = test_frame.mobs[offset:offset + MOB_TYPE_SIZE]
            if ref_mob != test_mob:
                return f"mobs[{i}]: expected {ref_mob.hex()}, got {test_mob.hex()}"

    # RNG state
    if ref_frame.random_seed != test_frame.random_seed:
        return f"random_seed: expected {ref_frame.random_seed}, got {test_frame.random_seed}"

    # Should never reach here if raw bytes differ
    return "Binary mismatch but all parsed fields match (unknown cause)"


def compare_traces(ref_path: Path, test_path: Path) -> int:
    """
    Compare two trace files.

    Returns:
        0 if traces match
        1 if traces diverge
        2 on error
    """

    # Validate files exist
    if not ref_path.exists():
        print(f"ERROR: Reference file not found: {ref_path}", file=sys.stderr)
        return 2

    if not test_path.exists():
        print(f"ERROR: Test file not found: {test_path}", file=sys.stderr)
        return 2

    # Check file sizes
    ref_size = ref_path.stat().st_size
    test_size = test_path.stat().st_size

    if ref_size % FRAME_SIZE != 0:
        print(f"ERROR: Reference file size ({ref_size}) is not a multiple of frame size ({FRAME_SIZE})", file=sys.stderr)
        return 2

    if test_size % FRAME_SIZE != 0:
        print(f"ERROR: Test file size ({test_size}) is not a multiple of frame size ({FRAME_SIZE})", file=sys.stderr)
        return 2

    ref_frames = ref_size // FRAME_SIZE
    test_frames = test_size // FRAME_SIZE

    print(f"Comparing traces:")
    print(f"  Reference: {ref_path} ({ref_frames} frames)")
    print(f"  Test:      {test_path} ({test_frames} frames)")
    print()

    # Length mismatch is immediate divergence
    if ref_frames != test_frames:
        print(f"DIVERGENCE: Frame count mismatch")
        print(f"  Reference: {ref_frames} frames")
        print(f"  Test:      {test_frames} frames")
        return 1

    # Compare frame by frame
    try:
        with open(ref_path, 'rb') as ref_file, open(test_path, 'rb') as test_file:
            for frame_idx in range(ref_frames):
                ref_raw = ref_file.read(FRAME_SIZE)
                test_raw = test_file.read(FRAME_SIZE)

                if len(ref_raw) != FRAME_SIZE or len(test_raw) != FRAME_SIZE:
                    print(f"ERROR: Unexpected EOF at frame {frame_idx}", file=sys.stderr)
                    return 2

                # Quick binary comparison first
                if ref_raw == test_raw:
                    continue

                # Parse and find exact field divergence
                try:
                    ref_frame = FrameData(ref_raw)
                    test_frame = FrameData(test_raw)

                    divergence = compare_frames(ref_frame, test_frame)
                    if divergence:
                        print(f"DIVERGENCE at frame {frame_idx}:")
                        print(f"  Field: {divergence}")
                        return 1

                except Exception as e:
                    print(f"ERROR: Failed to parse frame {frame_idx}: {e}", file=sys.stderr)
                    return 2

        # All frames match
        print(f"MATCH: All {ref_frames} frames identical")
        return 0

    except Exception as e:
        print(f"ERROR: {e}", file=sys.stderr)
        return 2


def main():
    if len(sys.argv) != 3:
        print("Usage: compare_traces.py <reference.trace> <test.trace>", file=sys.stderr)
        print("\nCompares two SDLPoP state trace files frame-by-frame.", file=sys.stderr)
        print("Exit code: 0=match, 1=diverge, 2=error", file=sys.stderr)
        return 2

    ref_path = Path(sys.argv[1])
    test_path = Path(sys.argv[2])

    return compare_traces(ref_path, test_path)


if __name__ == '__main__':
    sys.exit(main())
