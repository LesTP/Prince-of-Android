package com.sdlpop.game

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Module 6 structural verification: struct sizes, enum values, and default values.
 * These tests ensure the Kotlin translation matches the C original's layout.
 */
class TypesTest {

    // === Struct size assertions (match C sizeof) ===

    @Test
    fun `char_type is 16 bytes`() {
        // frame(1) + x(1) + y(1) + direction(1) + curr_col(1) + curr_row(1) +
        // action(1) + fall_x(1) + fall_y(1) + room(1) + repeat(1) + charid(1) +
        // sword(1) + alive(1) + curr_seq(2) = 16
        assertEquals(16, CharType.SIZE_BYTES)
    }

    @Test
    fun `trob_type is 3 bytes`() {
        // tilepos(1) + room(1) + type(1) = 3
        assertEquals(3, TrobType.SIZE_BYTES)
    }

    @Test
    fun `mob_type is 6 bytes`() {
        // xh(1) + y(1) + room(1) + speed(1) + type(1) + row(1) = 6
        assertEquals(6, MobType.SIZE_BYTES)
    }

    @Test
    fun `frame_type is 5 bytes`() {
        // image(1) + sword(1) + dx(1) + dy(1) + flags(1) = 5
        assertEquals(5, FrameType.SIZE_BYTES)
    }

    @Test
    fun `level_type is 2305 bytes`() {
        // fg(720) + bg(720) + doorlinks1(256) + doorlinks2(256) +
        // roomlinks(24*4=96) + used_rooms(1) + roomxs(24) + roomys(24) +
        // fill_1(15) + start_room(1) + start_pos(1) + start_dir(1) +
        // fill_2(4) + guards_tile(24) + guards_dir(24) + guards_x(24) +
        // guards_seq_lo(24) + guards_skill(24) + guards_seq_hi(24) +
        // guards_color(24) + fill_3(18) = 2305
        assertEquals(2305, LevelType.SIZE_BYTES)
    }

    // === Enum value spot-checks (match C) ===

    @Test
    fun `tile enum values match C`() {
        assertEquals(0, Tiles.EMPTY)
        assertEquals(1, Tiles.FLOOR)
        assertEquals(11, Tiles.LOOSE)
        assertEquals(18, Tiles.CHOMPER)
        assertEquals(20, Tiles.WALL)
        assertEquals(30, Tiles.TORCH_WITH_DEBRIS)
    }

    @Test
    fun `direction enum values match C`() {
        assertEquals(0x00, Directions.RIGHT)
        assertEquals(0x56, Directions.NONE)
        assertEquals(-1, Directions.LEFT)
    }

    @Test
    fun `action enum values match C`() {
        assertEquals(0, Actions.STAND)
        assertEquals(4, Actions.IN_FREEFALL)
        assertEquals(99, Actions.HURT)
    }

    @Test
    fun `charid enum values match C`() {
        assertEquals(0, CharIds.KID)
        assertEquals(2, CharIds.GUARD)
        assertEquals(4, CharIds.SKELETON)
        assertEquals(0x18, CharIds.MOUSE)
    }

    @Test
    fun `frame flag bitmasks match C`() {
        assertEquals(0x1F, FrameFlags.WEIGHT_X)
        assertEquals(0x20, FrameFlags.THIN)
        assertEquals(0x40, FrameFlags.NEEDS_FLOOR)
        assertEquals(0x80, FrameFlags.EVEN_ODD_PIXEL)
    }

    @Test
    fun `blitter values match C`() {
        assertEquals(0, Blitters.NO_TRANSP)
        assertEquals(3, Blitters.XOR)
        assertEquals(0x10, Blitters.TRANSP)
        assertEquals(0x40, Blitters.MONO)
    }

    // === Default value checks (match C initializers in data.h) ===

    @Test
    fun `custom_defaults match C`() {
        val cd = CustomOptionsType()
        assertEquals(60, cd.startMinutesLeft)
        assertEquals(719, cd.startTicksLeft)
        assertEquals(3, cd.startHitp)
        assertEquals(10, cd.maxHitpAllowed)
        assertEquals(Tiles.FLOOR, cd.drawnTileTopLevelEdge)
        assertEquals(Tiles.WALL, cd.drawnTileLeftLevelEdge)
        assertEquals(Tiles.WALL, cd.levelEdgeHitTile)
        assertEquals(11, cd.looseFloorDelay)
        assertEquals(5, cd.baseSpeed)
        assertEquals(6, cd.fightSpeed)
        assertEquals(15, cd.chomperSpeed)
    }

    @Test
    fun `guard HP table matches C`() {
        val cd = CustomOptionsType()
        val expected = intArrayOf(4, 3, 3, 3, 3, 4, 5, 4, 4, 5, 5, 5, 4, 6, 0, 0)
        assertEquals(expected.toList(), cd.tblGuardHp.toList())
    }

    @Test
    fun `guard skill tables match C`() {
        val cd = CustomOptionsType()
        assertEquals(61, cd.strikeprob[0])
        assertEquals(100, cd.strikeprob[1])
        assertEquals(220, cd.strikeprob[7])
        assertEquals(255, cd.blockprob[5])
        assertEquals(16, cd.refractimer[0])
        assertEquals(0, cd.refractimer[10])
        assertEquals(1, cd.extrastrength[4])
    }

    @Test
    fun `constant arrays match C`() {
        // y_land
        assertEquals(listOf<Short>(-8, 55, 118, 181, 244), GameState.yLand.toList())

        // tbl_line
        assertEquals(listOf(0, 10, 20), GameState.tblLine.toList())

        // dir_front / dir_behind
        assertEquals(listOf(-1, 1), GameState.dirFront.toList())
        assertEquals(listOf(1, -1), GameState.dirBehind.toList())

        // x_bump: spot check first few and last
        assertEquals(-12, GameState.xBump[0])
        assertEquals(58, GameState.xBump[5])
        assertEquals(254, GameState.xBump[19])
    }

    @Test
    fun `sound_interruptible matches C`() {
        // fell_to_death = not interruptible
        assertEquals(0, GameState.soundInterruptible[0])
        // falling = interruptible
        assertEquals(1, GameState.soundInterruptible[1])
        // gate_closing_fast = not interruptible
        assertEquals(0, GameState.soundInterruptible[6])
        // chomper = interruptible
        assertEquals(1, GameState.soundInterruptible[47])
        // 58 entries total (indices 0-57)
        assertEquals(58, GameState.soundInterruptible.size)
    }

    @Test
    fun `GameState initial values match C`() {
        assertEquals(-1, GameState.currentLevel)
        assertEquals(0, GameState.seedWasInit)
        assertEquals(0x0F, GameState.isSoundOn)
        assertEquals((-1).toShort(), GameState.startLevel)
    }

    // === CharType.copyFrom test ===

    @Test
    fun `CharType copyFrom copies all fields`() {
        val src = CharType(
            frame = 15, x = 100, y = 80, direction = -1, currCol = 3, currRow = 1,
            action = 1, fallX = 2, fallY = 5, room = 7, repeat = 0, charid = 0,
            sword = 2, alive = 1, currSeq = 42
        )
        val dst = CharType()
        dst.copyFrom(src)
        assertEquals(src, dst)
    }
}
