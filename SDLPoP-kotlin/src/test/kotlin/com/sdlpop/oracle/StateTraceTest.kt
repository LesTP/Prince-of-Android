package com.sdlpop.oracle

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

    private fun writeU32(bytes: ByteArray, offset: Int, value: Long) {
        bytes[offset] = value.and(0xFF).toByte()
        bytes[offset + 1] = value.shr(8).and(0xFF).toByte()
        bytes[offset + 2] = value.shr(16).and(0xFF).toByte()
        bytes[offset + 3] = value.shr(24).and(0xFF).toByte()
    }
}
