/*
SDLPoP-kotlin, a Kotlin port of SDLPoP (Prince of Persia).
Based on SDLPoP by Dávid Nagy, licensed under GPL v3+.

Module 6, Phase 6a: Core Game Types
Translates struct types from types.h used by Layer 1 game logic.

C-to-Kotlin type mapping:
  byte  (unsigned 8-bit)  → Int (mask with and 0xFF where needed)
  sbyte (signed 8-bit)    → Int (use .toByte().toInt() for sign extension)
  word  (unsigned 16-bit) → Int (mask with and 0xFFFF where needed)
  short (signed 16-bit)   → Short
  dword (unsigned 32-bit) → Long (mask with and 0xFFFFFFFFL where needed)
*/

package com.sdlpop.game

// rect_type: 4 shorts = 8 bytes
// Used by collision detection and rendering clipping
data class RectType(
    var top: Short = 0,
    var left: Short = 0,
    var bottom: Short = 0,
    var right: Short = 0
)

// tile_and_mod: 2 bytes
data class TileAndMod(
    var tiletype: Int = 0,  // byte
    var modifier: Int = 0   // byte
)

// link_type: 4 bytes (packed)
data class LinkType(
    var left: Int = 0,   // byte
    var right: Int = 0,  // byte
    var up: Int = 0,     // byte
    var down: Int = 0    // byte
)

// level_type: 2305 bytes (packed)
// Full level data: tiles, room links, guard positions
data class LevelType(
    val fg: IntArray = IntArray(720),           // byte[720]
    val bg: IntArray = IntArray(720),           // byte[720]
    val doorlinks1: IntArray = IntArray(256),   // byte[256]
    val doorlinks2: IntArray = IntArray(256),   // byte[256]
    val roomlinks: Array<LinkType> = Array(24) { LinkType() },
    var usedRooms: Int = 0,                    // byte (used_rooms)
    val roomxs: IntArray = IntArray(24),        // byte[24]
    val roomys: IntArray = IntArray(24),        // byte[24]
    val fill1: IntArray = IntArray(15),         // byte[15]
    var startRoom: Int = 0,                    // byte (start_room)
    var startPos: Int = 0,                     // byte (start_pos)
    var startDir: Int = 0,                     // sbyte (start_dir) — stored as Int, sign-extend as needed
    val fill2: IntArray = IntArray(4),          // byte[4]
    val guardsTile: IntArray = IntArray(24),    // byte[24]
    val guardsDir: IntArray = IntArray(24),     // byte[24]
    val guardsX: IntArray = IntArray(24),       // byte[24]
    val guardsSeqLo: IntArray = IntArray(24),   // byte[24] (guards_seq_lo)
    val guardsSkill: IntArray = IntArray(24),   // byte[24]
    val guardsSeqHi: IntArray = IntArray(24),   // byte[24] (guards_seq_hi)
    val guardsColor: IntArray = IntArray(24),   // byte[24]
    val fill3: IntArray = IntArray(18)          // byte[18]
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LevelType) return false
        return fg.contentEquals(other.fg) && bg.contentEquals(other.bg) &&
            doorlinks1.contentEquals(other.doorlinks1) && doorlinks2.contentEquals(other.doorlinks2) &&
            roomlinks.contentEquals(other.roomlinks) && usedRooms == other.usedRooms &&
            roomxs.contentEquals(other.roomxs) && roomys.contentEquals(other.roomys) &&
            startRoom == other.startRoom && startPos == other.startPos && startDir == other.startDir &&
            guardsTile.contentEquals(other.guardsTile) && guardsDir.contentEquals(other.guardsDir) &&
            guardsX.contentEquals(other.guardsX) && guardsSeqLo.contentEquals(other.guardsSeqLo) &&
            guardsSkill.contentEquals(other.guardsSkill) && guardsSeqHi.contentEquals(other.guardsSeqHi) &&
            guardsColor.contentEquals(other.guardsColor)
    }

    override fun hashCode(): Int = fg.contentHashCode()

    companion object {
        // C sizeof(level_type) == 2305
        const val SIZE_BYTES = 2305
    }
}

// char_type: 16 bytes
// Represents Kid, Guard, Char (currently-processed character), Opp (opponent)
// Field sizes: frame(1) + x(1) + y(1) + direction(1) + curr_col(1) + curr_row(1) +
//              action(1) + fall_x(1) + fall_y(1) + room(1) + repeat(1) + charid(1) +
//              sword(1) + alive(1) + curr_seq(2) = 16 bytes
data class CharType(
    var frame: Int = 0,      // byte
    var x: Int = 0,          // byte
    var y: Int = 0,          // byte
    var direction: Int = 0,  // sbyte
    var currCol: Int = 0,    // sbyte (curr_col)
    var currRow: Int = 0,    // sbyte (curr_row)
    var action: Int = 0,     // byte
    var fallX: Int = 0,      // sbyte (fall_x)
    var fallY: Int = 0,      // sbyte (fall_y) — also used for falling distance check
    var room: Int = 0,       // byte
    var repeat: Int = 0,     // byte
    var charid: Int = 0,     // byte
    var sword: Int = 0,      // byte
    var alive: Int = 0,      // sbyte
    var currSeq: Int = 0     // word (curr_seq) — unsigned 16-bit
) {
    fun copyFrom(other: CharType) {
        frame = other.frame; x = other.x; y = other.y
        direction = other.direction; currCol = other.currCol; currRow = other.currRow
        action = other.action; fallX = other.fallX; fallY = other.fallY
        room = other.room; repeat = other.repeat; charid = other.charid
        sword = other.sword; alive = other.alive; currSeq = other.currSeq
    }

    companion object {
        const val SIZE_BYTES = 16
    }
}

// trob_type: 3 bytes — animated tile state (chompers, doors, etc.)
data class TrobType(
    var tilepos: Int = 0,  // byte
    var room: Int = 0,     // byte
    var type: Int = 0      // sbyte
) {
    companion object {
        const val SIZE_BYTES = 3
    }
}

// mob_type: 6 bytes — movable object state (loose floors)
data class MobType(
    var xh: Int = 0,     // byte
    var y: Int = 0,       // byte
    var room: Int = 0,    // byte
    var speed: Int = 0,   // sbyte
    var type: Int = 0,    // byte
    var row: Int = 0      // byte
) {
    companion object {
        const val SIZE_BYTES = 6
    }
}

// frame_type: 5 bytes — animation frame descriptor
data class FrameType(
    var image: Int = 0,  // byte
    var sword: Int = 0,  // byte — 0x3F: sword image, 0xC0: chtab
    var dx: Int = 0,     // sbyte
    var dy: Int = 0,     // sbyte
    var flags: Int = 0   // byte — 0x1F: weight x, 0x20: thin, 0x40: needs floor, 0x80: even/odd pixel
) {
    companion object {
        const val SIZE_BYTES = 5
    }
}

// auto_move_type: 4 bytes — automatic movement entry (demo moves, shadow AI)
data class AutoMoveType(
    var time: Short = 0,  // short
    var move: Short = 0   // short
)

// sword_table_type: 3 bytes — sword position entry
data class SwordTableType(
    var id: Int = 0,  // byte
    var x: Int = 0,   // sbyte
    var y: Int = 0    // sbyte
)

// objtable_type: used by rendering but referenced by game logic for object management
data class ObjtableType(
    var xh: Int = 0,           // sbyte
    var xl: Int = 0,           // sbyte
    var y: Short = 0,          // short
    var chtabId: Int = 0,      // byte (chtab_id)
    var id: Int = 0,           // byte
    var direction: Int = 0,    // sbyte
    var objType: Int = 0,      // byte (obj_type)
    var clip: RectType = RectType(),
    var tilepos: Int = 0       // byte
)
