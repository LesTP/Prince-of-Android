package com.sdlpop.oracle

import com.sdlpop.game.CharType
import com.sdlpop.game.GameState
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StateTraceTest {

    @Test
    fun `trace metadata covers the full 310 byte frame layout`() {
        assertEquals(310, StateTraceFormat.FRAME_SIZE)
        assertEquals("frame_number", StateTraceFormat.fieldAtOffset(0).name)
        assertEquals("Kid.frame", StateTraceFormat.fieldAtOffset(4).name)
        assertEquals("Kid.curr_seq", StateTraceFormat.fieldAtOffset(18).name)
        assertEquals("curr_room_tiles[0]", StateTraceFormat.fieldAtOffset(68).name)
        assertEquals("curr_room_modif[29]", StateTraceFormat.fieldAtOffset(127).name)
        assertEquals("trobs[0].type", StateTraceFormat.fieldAtOffset(132).name)
        assertEquals("mobs[13].row", StateTraceFormat.fieldAtOffset(305).name)
        assertEquals("random_seed", StateTraceFormat.fieldAtOffset(309).name)

        val coveredOffsets = BooleanArray(StateTraceFormat.FRAME_SIZE)
        StateTraceFormat.fields.forEach { field ->
            for (offset in field.offset until field.offset + field.size) {
                assertFalse(coveredOffsets[offset], "Offset $offset is covered more than once")
                coveredOffsets[offset] = true
            }
        }
        assertTrue(coveredOffsets.all { it }, "Every frame offset must have field metadata")
    }

    @Test
    fun `parse constructed frame with little endian and signed values`() {
        val bytes = ByteArray(StateTraceFormat.FRAME_SIZE)
        writeU32(bytes, 0, 0x89ABCDEFL)
        bytes[7] = 0xFF.toByte()      // Kid.direction = -1
        bytes[8] = 0xFE.toByte()      // Kid.curr_col = -2
        bytes[18] = 0x34
        bytes[19] = 0x12              // Kid.curr_seq = 0x1234
        bytes[56] = 0xFF.toByte()
        bytes[57] = 0xFF.toByte()     // rem_min = -1
        bytes[132] = 0x80.toByte()    // trobs[0].type = -128
        bytes[225] = 0xF6.toByte()    // mobs[0].speed = -10
        writeU32(bytes, 306, 0xFFFFFFFFL)

        val frame = StateTraceFormat.parse(bytes).single()

        assertEquals(0x89ABCDEFL, frame.frameNumber)
        assertEquals(-1, frame.fieldValue("Kid.direction"))
        assertEquals(-2, frame.fieldValue("Kid.curr_col"))
        assertEquals(0x1234, frame.fieldValue("Kid.curr_seq"))
        assertEquals(-1, frame.fieldValue("rem_min"))
        assertEquals(-128, frame.fieldValue("trobs[0].type"))
        assertEquals(-10, frame.fieldValue("mobs[0].speed"))
        assertEquals(0xFFFFFFFFL, frame.fieldValue("random_seed"))
    }

    @Test
    fun `compare reports first divergent frame field and values`() {
        val expected = ByteArray(StateTraceFormat.FRAME_SIZE * 2)
        val actual = expected.copyOf()
        writeU32(expected, 0, 0)
        writeU32(expected, StateTraceFormat.FRAME_SIZE, 1)
        writeU32(actual, 0, 0)
        writeU32(actual, StateTraceFormat.FRAME_SIZE, 1)

        expected[StateTraceFormat.FRAME_SIZE + 132] = 0xFE.toByte()
        actual[StateTraceFormat.FRAME_SIZE + 132] = 0x02

        val comparison = StateTraceFormat.compare(expected, actual)
        val divergence = comparison.divergence

        assertFalse(comparison.matched)
        assertEquals(1, divergence?.frameIndex)
        assertEquals(1, divergence?.frameNumber)
        assertEquals(132, divergence?.byteOffsetInFrame)
        assertEquals("trobs[0].type", divergence?.fieldName)
        assertEquals("-2", divergence?.expectedValue)
        assertEquals("2", divergence?.actualValue)
    }

    @Test
    fun `parse existing C reference trace`() {
        val tracePath = Paths.get("../SDLPoP/traces/reference/basic_movement.trace")
        assumeTrue(tracePath.toFile().exists(), "Reference trace not found: $tracePath")

        val frames = StateTraceFormat.parse(tracePath)

        assertEquals(397, frames.size)
        assertEquals(0, frames.first().index)
        assertEquals(0, frames.first().frameNumber)
        assertEquals(396, frames.last().index)
        assertEquals(396, frames.last().frameNumber)
        assertTrue(StateTraceFormat.compare(frames, frames).matched)
    }

    @Test
    fun `serialize GameState writes the pinned 310 byte trace layout`() = withTraceStateReset {
        val gs = GameState
        gs.Kid = CharType(
            frame = 0x101,
            x = 0x82,
            y = 0x7F,
            direction = -1,
            currCol = -2,
            currRow = 2,
            action = 0x123,
            fallX = -128,
            fallY = 127,
            room = 24,
            repeat = 0xFF,
            charid = 0,
            sword = 2,
            alive = -5,
            currSeq = 0x1234,
        )
        gs.Guard = CharType(frame = 9, x = 10, y = 11, direction = 0, currSeq = 0xCAFE)
        gs.Char = CharType(frame = 12, x = 13, y = 14, direction = -1, currSeq = 0xBEEF)
        gs.currentLevel = -1
        gs.drawnRoom = 0x1234
        gs.remMin = (-2).toShort()
        gs.remTick = 0xFEDC
        gs.hitpCurr = 3
        gs.hitpMax = 4
        gs.guardhpCurr = 5
        gs.guardhpMax = 6
        gs.currRoomTiles[0] = 0x121
        gs.currRoomTiles[29] = 0xFE
        gs.currRoomModif[0] = 0x80
        gs.currRoomModif[29] = 0x1FF
        gs.trobsCount = (-3).toShort()
        gs.trobs[0].tilepos = 0x100
        gs.trobs[0].room = 7
        gs.trobs[0].type = -1
        gs.trobs[29].tilepos = 29
        gs.trobs[29].room = 24
        gs.trobs[29].type = -128
        gs.mobsCount = 14
        gs.mobs[0].xh = 0xAB
        gs.mobs[0].y = 0xCD
        gs.mobs[0].room = 24
        gs.mobs[0].speed = -10
        gs.mobs[0].type = 0x123
        gs.mobs[0].row = 2
        gs.mobs[13].xh = 13
        gs.mobs[13].y = 14
        gs.mobs[13].room = 15
        gs.mobs[13].speed = -128
        gs.mobs[13].type = 16
        gs.mobs[13].row = 17
        gs.randomSeed = 0x89ABCDEFL

        val bytes = StateTraceFormat.serializeFrameBytes(0x76543210L)
        val frame = StateTraceFormat.parse(bytes).single()

        assertEquals(StateTraceFormat.FRAME_SIZE, bytes.size)
        assertEquals(0x76543210L, frame.frameNumber)
        assertEquals(0x01, frame.fieldValue("Kid.frame"))
        assertEquals(0x82, frame.fieldValue("Kid.x"))
        assertEquals(-1, frame.fieldValue("Kid.direction"))
        assertEquals(-2, frame.fieldValue("Kid.curr_col"))
        assertEquals(-128, frame.fieldValue("Kid.fall_x"))
        assertEquals(127, frame.fieldValue("Kid.fall_y"))
        assertEquals(0x23, frame.fieldValue("Kid.action"))
        assertEquals(-5, frame.fieldValue("Kid.alive"))
        assertEquals(0x1234, frame.fieldValue("Kid.curr_seq"))
        assertEquals(0xCAFE, frame.fieldValue("Guard.curr_seq"))
        assertEquals(0xBEEF, frame.fieldValue("Char.curr_seq"))
        assertEquals(0xFFFF, frame.fieldValue("current_level"))
        assertEquals(0x1234, frame.fieldValue("drawn_room"))
        assertEquals(-2, frame.fieldValue("rem_min"))
        assertEquals(0xFEDC, frame.fieldValue("rem_tick"))
        assertEquals(3, frame.fieldValue("hitp_curr"))
        assertEquals(6, frame.fieldValue("guardhp_max"))
        assertEquals(0x21, frame.fieldValue("curr_room_tiles[0]"))
        assertEquals(0xFE, frame.fieldValue("curr_room_tiles[29]"))
        assertEquals(0x80, frame.fieldValue("curr_room_modif[0]"))
        assertEquals(0xFF, frame.fieldValue("curr_room_modif[29]"))
        assertEquals(-3, frame.fieldValue("trobs_count"))
        assertEquals(0, frame.fieldValue("trobs[0].tilepos"))
        assertEquals(-1, frame.fieldValue("trobs[0].type"))
        assertEquals(-128, frame.fieldValue("trobs[29].type"))
        assertEquals(14, frame.fieldValue("mobs_count"))
        assertEquals(-10, frame.fieldValue("mobs[0].speed"))
        assertEquals(0x23, frame.fieldValue("mobs[0].type"))
        assertEquals(-128, frame.fieldValue("mobs[13].speed"))
        assertEquals(17, frame.fieldValue("mobs[13].row"))
        assertEquals(0x89ABCDEFL, frame.fieldValue("random_seed"))
    }

    @Test
    fun `serializeFrame wraps bytes in StateTraceFrame`() = withTraceStateReset {
        val frame = StateTraceFormat.serializeFrame(7)

        assertEquals(7, frame.index)
        assertEquals(7, frame.frameNumber)
        assertEquals(StateTraceFormat.FRAME_SIZE, frame.bytes.size)
    }

    private fun writeU32(bytes: ByteArray, offset: Int, value: Long) {
        bytes[offset] = value.and(0xFF).toByte()
        bytes[offset + 1] = value.shr(8).and(0xFF).toByte()
        bytes[offset + 2] = value.shr(16).and(0xFF).toByte()
        bytes[offset + 3] = value.shr(24).and(0xFF).toByte()
    }

    private fun withTraceStateReset(block: () -> Unit) {
        resetTraceState()
        try {
            block()
        } finally {
            resetTraceState()
        }
    }

    private fun resetTraceState() {
        val gs = GameState
        gs.Kid = CharType()
        gs.Guard = CharType()
        gs.Char = CharType()
        gs.currentLevel = -1
        gs.drawnRoom = 0
        gs.remMin = 0
        gs.remTick = 0
        gs.hitpCurr = 0
        gs.hitpMax = 0
        gs.guardhpCurr = 0
        gs.guardhpMax = 0
        gs.currRoomTiles.fill(0)
        gs.currRoomModif.fill(0)
        gs.trobsCount = 0
        gs.trobs.forEach {
            it.tilepos = 0
            it.room = 0
            it.type = 0
        }
        gs.mobsCount = 0
        gs.mobs.forEach {
            it.xh = 0
            it.y = 0
            it.room = 0
            it.speed = 0
            it.type = 0
            it.row = 0
        }
        gs.randomSeed = 0
    }
}
