package com.sdlpop.replay

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path

/**
 * Data class representing a parsed .P1R replay file.
 */
data class ReplayData(
    val formatClass: Int,
    val versionNumber: Int,
    val deprecationNumber: Int,
    val creationTime: Long,
    val levelsetName: String,
    val implementationName: String,
    val savestateSize: Long,
    val savestateBuffer: ByteArray,
    val optionsSections: List<ByteArray>,
    val startLevel: Int,
    val randomSeed: Long,
    val numReplayTicks: Long,
    val moves: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReplayData

        if (formatClass != other.formatClass) return false
        if (versionNumber != other.versionNumber) return false
        if (deprecationNumber != other.deprecationNumber) return false
        if (creationTime != other.creationTime) return false
        if (levelsetName != other.levelsetName) return false
        if (implementationName != other.implementationName) return false
        if (savestateSize != other.savestateSize) return false
        if (!savestateBuffer.contentEquals(other.savestateBuffer)) return false
        if (optionsSections.size != other.optionsSections.size) return false
        if (!optionsSections.zip(other.optionsSections).all { (a, b) -> a.contentEquals(b) }) return false
        if (startLevel != other.startLevel) return false
        if (randomSeed != other.randomSeed) return false
        if (numReplayTicks != other.numReplayTicks) return false
        if (!moves.contentEquals(other.moves)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = formatClass
        result = 31 * result + versionNumber
        result = 31 * result + deprecationNumber
        result = 31 * result + creationTime.hashCode()
        result = 31 * result + levelsetName.hashCode()
        result = 31 * result + implementationName.hashCode()
        result = 31 * result + savestateSize.hashCode()
        result = 31 * result + savestateBuffer.contentHashCode()
        result = 31 * result + optionsSections.hashCode()
        result = 31 * result + startLevel
        result = 31 * result + randomSeed.hashCode()
        result = 31 * result + numReplayTicks.hashCode()
        result = 31 * result + moves.contentHashCode()
        return result
    }
}

/**
 * Exception thrown when a .P1R file is invalid or cannot be parsed.
 */
class P1RParseException(message: String) : Exception(message)

/**
 * Parser for SDLPoP .P1R replay files.
 */
object P1RParser {
    private const val MAGIC_NUMBER = "P1R"
    private const val EXPECTED_FORMAT_CLASS = 0
    private const val MIN_VERSION = 101
    private const val MAX_DEPRECATION = 2
    private const val NUM_OPTIONS_SECTIONS = 5

    /**
     * Parses a .P1R replay file from the given path.
     *
     * @param path Path to the .P1R file
     * @return Parsed replay data
     * @throws P1RParseException if the file is invalid or incompatible
     * @throws IOException if the file cannot be read
     */
    fun parseP1R(path: Path): ReplayData {
        val bytes = Files.readAllBytes(path)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        try {
            // Read magic number
            val magic = ByteArray(3)
            buffer.get(magic)
            if (String(magic, Charsets.US_ASCII) != MAGIC_NUMBER) {
                throw P1RParseException("Invalid magic number: expected '$MAGIC_NUMBER', got '${String(magic, Charsets.US_ASCII)}'")
            }

            // Read format class (word = uint16)
            val formatClass = buffer.short.toInt() and 0xFFFF
            if (formatClass != EXPECTED_FORMAT_CLASS) {
                throw P1RParseException("Incompatible format class: expected $EXPECTED_FORMAT_CLASS, got $formatClass")
            }

            // Read version and deprecation numbers
            val versionNumber = buffer.get().toInt() and 0xFF
            if (versionNumber < MIN_VERSION) {
                throw P1RParseException("Replay format too old: minimum version $MIN_VERSION, got $versionNumber")
            }

            val deprecationNumber = buffer.get().toInt() and 0xFF
            if (deprecationNumber > MAX_DEPRECATION) {
                throw P1RParseException("Replay format too new: max deprecation $MAX_DEPRECATION, got $deprecationNumber")
            }

            // Read creation time (Sint64 = int64)
            val creationTime = buffer.long

            // Read levelset name
            val levelsetNameLen = buffer.get().toInt() and 0xFF
            val levelsetName = if (levelsetNameLen > 0) {
                val nameBytes = ByteArray(levelsetNameLen)
                buffer.get(nameBytes)
                String(nameBytes, Charsets.US_ASCII)
            } else {
                ""
            }

            // Read implementation name
            val implNameLen = buffer.get().toInt() and 0xFF
            val implNameBytes = ByteArray(implNameLen)
            buffer.get(implNameBytes)
            val implementationName = String(implNameBytes, Charsets.US_ASCII)

            // Read savestate
            val savestateSize = buffer.int.toLong() and 0xFFFFFFFF
            val savestateBuffer = ByteArray(savestateSize.toInt())
            buffer.get(savestateBuffer)

            // Read options sections
            val optionsSections = mutableListOf<ByteArray>()
            repeat(NUM_OPTIONS_SECTIONS) {
                val sectionSize = buffer.int.toLong() and 0xFFFFFFFF
                val sectionData = ByteArray(sectionSize.toInt())
                buffer.get(sectionData)
                optionsSections.add(sectionData)
            }

            // Read start level (word = uint16, per replay_format.txt)
            val startLevel = buffer.short.toInt() and 0xFFFF

            // Read random seed (dword = uint32)
            val randomSeed = buffer.int.toLong() and 0xFFFFFFFF

            // Read num replay ticks (dword = uint32)
            val numReplayTicks = buffer.int.toLong() and 0xFFFFFFFF

            // Read moves array
            val moves = ByteArray(numReplayTicks.toInt())
            buffer.get(moves)

            // Verify we consumed the entire file
            if (buffer.hasRemaining()) {
                throw P1RParseException("Unexpected data at end of file: ${buffer.remaining()} bytes remaining")
            }

            return ReplayData(
                formatClass = formatClass,
                versionNumber = versionNumber,
                deprecationNumber = deprecationNumber,
                creationTime = creationTime,
                levelsetName = levelsetName,
                implementationName = implementationName,
                savestateSize = savestateSize,
                savestateBuffer = savestateBuffer,
                optionsSections = optionsSections,
                startLevel = startLevel,
                randomSeed = randomSeed,
                numReplayTicks = numReplayTicks,
                moves = moves
            )

        } catch (e: P1RParseException) {
            throw e
        } catch (e: Exception) {
            throw P1RParseException("Parse error: ${e.message}")
        }
    }
}
