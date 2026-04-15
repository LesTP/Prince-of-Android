package com.sdlpop.oracle

import java.nio.file.Files
import java.nio.file.Path

enum class TraceValueType {
    U8,
    S8,
    U16,
    S16,
    U32,
}

data class TraceField(
    val name: String,
    val offset: Int,
    val size: Int,
    private val valueType: TraceValueType,
) {
    fun readValue(frameBytes: ByteArray): Long = when (valueType) {
        TraceValueType.U8 -> frameBytes[offset].toInt().and(0xFF).toLong()
        TraceValueType.S8 -> frameBytes[offset].toInt().toByte().toLong()
        TraceValueType.U16 -> readU16Le(frameBytes, offset).toLong()
        TraceValueType.S16 -> readU16Le(frameBytes, offset).toShort().toLong()
        TraceValueType.U32 -> readU32Le(frameBytes, offset)
    }

    companion object {
        private fun readU16Le(bytes: ByteArray, offset: Int): Int =
            bytes[offset].toInt().and(0xFF) or
                (bytes[offset + 1].toInt().and(0xFF) shl 8)

        private fun readU32Le(bytes: ByteArray, offset: Int): Long =
            bytes[offset].toLong().and(0xFF) or
                (bytes[offset + 1].toLong().and(0xFF) shl 8) or
                (bytes[offset + 2].toLong().and(0xFF) shl 16) or
                (bytes[offset + 3].toLong().and(0xFF) shl 24)
    }
}

data class StateTraceFrame(
    val index: Int,
    val bytes: ByteArray,
) {
    init {
        require(bytes.size == StateTraceFormat.FRAME_SIZE) {
            "Trace frame must be ${StateTraceFormat.FRAME_SIZE} bytes, got ${bytes.size}"
        }
    }

    val frameNumber: Long = StateTraceFormat.fieldByName("frame_number").readValue(bytes)

    fun fieldValue(fieldName: String): Long = StateTraceFormat.fieldByName(fieldName).readValue(bytes)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StateTraceFrame) return false
        return index == other.index && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = 31 * index + bytes.contentHashCode()
}

data class TraceDivergence(
    val frameIndex: Int,
    val frameNumber: Long?,
    val byteOffsetInFrame: Int?,
    val fieldName: String?,
    val expectedValue: String,
    val actualValue: String,
)

data class TraceComparison(
    val matched: Boolean,
    val divergence: TraceDivergence? = null,
) {
    companion object {
        val MATCH = TraceComparison(matched = true)
    }
}

object StateTraceFormat {
    const val FRAME_SIZE = 310

    val fields: List<TraceField> = buildList {
        field("frame_number", 0, 4, TraceValueType.U32)

        charFields("Kid", 4)
        charFields("Guard", 20)
        charFields("Char", 36)

        field("current_level", 52, 2, TraceValueType.U16)
        field("drawn_room", 54, 2, TraceValueType.U16)
        field("rem_min", 56, 2, TraceValueType.S16)
        field("rem_tick", 58, 2, TraceValueType.U16)
        field("hitp_curr", 60, 2, TraceValueType.U16)
        field("hitp_max", 62, 2, TraceValueType.U16)
        field("guardhp_curr", 64, 2, TraceValueType.U16)
        field("guardhp_max", 66, 2, TraceValueType.U16)

        repeat(30) { index ->
            field("curr_room_tiles[$index]", 68 + index, 1, TraceValueType.U8)
        }
        repeat(30) { index ->
            field("curr_room_modif[$index]", 98 + index, 1, TraceValueType.U8)
        }

        field("trobs_count", 128, 2, TraceValueType.S16)
        repeat(30) { index ->
            val offset = 130 + index * 3
            field("trobs[$index].tilepos", offset, 1, TraceValueType.U8)
            field("trobs[$index].room", offset + 1, 1, TraceValueType.U8)
            field("trobs[$index].type", offset + 2, 1, TraceValueType.S8)
        }

        field("mobs_count", 220, 2, TraceValueType.S16)
        repeat(14) { index ->
            val offset = 222 + index * 6
            field("mobs[$index].xh", offset, 1, TraceValueType.U8)
            field("mobs[$index].y", offset + 1, 1, TraceValueType.U8)
            field("mobs[$index].room", offset + 2, 1, TraceValueType.U8)
            field("mobs[$index].speed", offset + 3, 1, TraceValueType.S8)
            field("mobs[$index].type", offset + 4, 1, TraceValueType.U8)
            field("mobs[$index].row", offset + 5, 1, TraceValueType.U8)
        }

        field("random_seed", 306, 4, TraceValueType.U32)
    }

    private val fieldsByName = fields.associateBy { it.name }

    fun fieldByName(name: String): TraceField =
        fieldsByName[name] ?: error("Unknown trace field: $name")

    fun fieldAtOffset(offset: Int): TraceField {
        require(offset in 0 until FRAME_SIZE) { "Frame offset out of range: $offset" }
        return fields.first { offset >= it.offset && offset < it.offset + it.size }
    }

    fun parse(bytes: ByteArray): List<StateTraceFrame> {
        require(bytes.size % FRAME_SIZE == 0) {
            "Trace size ${bytes.size} is not a multiple of $FRAME_SIZE"
        }
        return bytes.asIterable()
            .chunked(FRAME_SIZE)
            .mapIndexed { index, chunk -> StateTraceFrame(index, chunk.toByteArray()) }
    }

    fun parse(path: Path): List<StateTraceFrame> = parse(Files.readAllBytes(path))

    fun compare(expected: List<StateTraceFrame>, actual: List<StateTraceFrame>): TraceComparison {
        val commonFrames = minOf(expected.size, actual.size)
        for (frameIndex in 0 until commonFrames) {
            val expectedBytes = expected[frameIndex].bytes
            val actualBytes = actual[frameIndex].bytes
            for (offset in 0 until FRAME_SIZE) {
                if (expectedBytes[offset] != actualBytes[offset]) {
                    val field = fieldAtOffset(offset)
                    return TraceComparison(
                        matched = false,
                        divergence = TraceDivergence(
                            frameIndex = frameIndex,
                            frameNumber = expected[frameIndex].frameNumber,
                            byteOffsetInFrame = offset,
                            fieldName = field.name,
                            expectedValue = field.readValue(expectedBytes).toString(),
                            actualValue = field.readValue(actualBytes).toString(),
                        )
                    )
                }
            }
        }

        if (expected.size != actual.size) {
            return TraceComparison(
                matched = false,
                divergence = TraceDivergence(
                    frameIndex = commonFrames,
                    frameNumber = null,
                    byteOffsetInFrame = null,
                    fieldName = "frame_count",
                    expectedValue = expected.size.toString(),
                    actualValue = actual.size.toString(),
                )
            )
        }

        return TraceComparison.MATCH
    }

    fun compare(expected: ByteArray, actual: ByteArray): TraceComparison =
        compare(parse(expected), parse(actual))

    private fun MutableList<TraceField>.charFields(prefix: String, baseOffset: Int) {
        field("$prefix.frame", baseOffset, 1, TraceValueType.U8)
        field("$prefix.x", baseOffset + 1, 1, TraceValueType.U8)
        field("$prefix.y", baseOffset + 2, 1, TraceValueType.U8)
        field("$prefix.direction", baseOffset + 3, 1, TraceValueType.S8)
        field("$prefix.curr_col", baseOffset + 4, 1, TraceValueType.S8)
        field("$prefix.curr_row", baseOffset + 5, 1, TraceValueType.S8)
        field("$prefix.action", baseOffset + 6, 1, TraceValueType.U8)
        field("$prefix.fall_x", baseOffset + 7, 1, TraceValueType.S8)
        field("$prefix.fall_y", baseOffset + 8, 1, TraceValueType.S8)
        field("$prefix.room", baseOffset + 9, 1, TraceValueType.U8)
        field("$prefix.repeat", baseOffset + 10, 1, TraceValueType.U8)
        field("$prefix.charid", baseOffset + 11, 1, TraceValueType.U8)
        field("$prefix.sword", baseOffset + 12, 1, TraceValueType.U8)
        field("$prefix.alive", baseOffset + 13, 1, TraceValueType.S8)
        field("$prefix.curr_seq", baseOffset + 14, 2, TraceValueType.U16)
    }

    private fun MutableList<TraceField>.field(
        name: String,
        offset: Int,
        size: Int,
        valueType: TraceValueType,
    ) {
        add(TraceField(name, offset, size, valueType))
    }
}
