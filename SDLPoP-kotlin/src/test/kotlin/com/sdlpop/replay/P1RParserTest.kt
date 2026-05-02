package com.sdlpop.replay

import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class P1RParserTest {

    private val replayBasePath = Paths.get("../SDLPoP/replays")
    private val testReplayPath = Paths.get("../SDLPoP/doc/replays-testcases")

    @Test
    fun `parse basic movement replay successfully`() {
        val replayPath = replayBasePath.resolve("basic movement.p1r")
        assumeTrue(replayPath.toFile().exists(), "Replay file not found: $replayPath")

        val replay = P1RParser.parseP1R(replayPath)

        assertEquals(0, replay.formatClass, "Format class should be 0 for SDLPoP")
        assertTrue(replay.versionNumber >= 101, "Version should be >= 101")
        assertTrue(replay.deprecationNumber <= 2, "Deprecation should be <= 2")
        assertTrue(replay.implementationName.contains("SDLPoP"), "Implementation name should contain 'SDLPoP'")
        assertTrue(replay.numReplayTicks > 0, "Should have at least one input frame")
        assertEquals(replay.moves.size.toLong(), replay.numReplayTicks, "Moves array size should match num_replay_ticks")
        assertEquals(5, replay.optionsSections.size, "Should have 5 options sections")
    }

    @Test
    fun `parse falling replay successfully`() {
        val replayPath = replayBasePath.resolve("falling.p1r")
        assumeTrue(replayPath.toFile().exists(), "Replay file not found: $replayPath")

        val replay = P1RParser.parseP1R(replayPath)

        assertEquals(0, replay.formatClass)
        assertTrue(replay.numReplayTicks > 0)
    }

    @Test
    fun `parse traps replay successfully`() {
        val replayPath = replayBasePath.resolve("traps.p1r")
        assumeTrue(replayPath.toFile().exists(), "Replay file not found: $replayPath")

        val replay = P1RParser.parseP1R(replayPath)

        assertEquals(0, replay.formatClass)
        assertTrue(replay.numReplayTicks > 0)
    }

    @Test
    fun `parse sword and level transition replay successfully`() {
        val replayPath = replayBasePath.resolve("sword and level transition.p1r")
        assumeTrue(replayPath.toFile().exists(), "Replay file not found: $replayPath")

        val replay = P1RParser.parseP1R(replayPath)

        assertEquals(0, replay.formatClass)
        assertTrue(replay.numReplayTicks > 0)
    }

    @Test
    fun `parse all test case replays without errors`() {
        assumeTrue(testReplayPath.toFile().exists(), "Test replays directory not found: $testReplayPath")

        val testReplays = testReplayPath.toFile().listFiles { _, name -> name.endsWith(".p1r") }
        assumeTrue(!testReplays.isNullOrEmpty(), "No .p1r files found in $testReplayPath")

        var successCount = 0
        testReplays.forEach { file ->
            try {
                val replay = P1RParser.parseP1R(file.toPath())
                assertEquals(0, replay.formatClass, "Format class should be 0 for ${file.name}")
                assertTrue(replay.numReplayTicks > 0, "Should have ticks for ${file.name}")
                successCount++
            } catch (e: Exception) {
                throw AssertionError("Failed to parse ${file.name}: ${e.message}", e)
            }
        }

        assertTrue(successCount > 0, "Should have parsed at least one test replay")
        println("Successfully parsed $successCount test replays")
    }

    @Test
    fun `reject invalid magic number`() {
        // Create a temporary file with invalid magic
        val tempFile = kotlin.io.path.createTempFile(suffix = ".p1r")
        try {
            tempFile.toFile().writeBytes(byteArrayOf(0x50, 0x30, 0x52)) // "P0R" instead of "P1R"

            val exception = assertThrows<P1RParseException> {
                P1RParser.parseP1R(tempFile)
            }

            assertTrue(exception.message?.contains("magic number") == true)
        } finally {
            tempFile.toFile().delete()
        }
    }

    @Test
    fun `reject incompatible format class`() {
        val tempFile = kotlin.io.path.createTempFile(suffix = ".p1r")
        try {
            // Valid magic but wrong format class
            val bytes = byteArrayOf(
                0x50, 0x31, 0x52,  // "P1R"
                0x01, 0x00,         // format_class = 1 (should be 0)
            )
            tempFile.toFile().writeBytes(bytes)

            val exception = assertThrows<P1RParseException> {
                P1RParser.parseP1R(tempFile)
            }

            assertTrue(exception.message?.contains("format class") == true)
        } finally {
            tempFile.toFile().delete()
        }
    }

    @Test
    fun `reject old version number`() {
        val tempFile = kotlin.io.path.createTempFile(suffix = ".p1r")
        try {
            val bytes = byteArrayOf(
                0x50, 0x31, 0x52,  // "P1R"
                0x00, 0x00,         // format_class = 0
                100,                // version = 100 (should be >= 101)
            )
            tempFile.toFile().writeBytes(bytes)

            val exception = assertThrows<P1RParseException> {
                P1RParser.parseP1R(tempFile)
            }

            assertTrue(exception.message?.contains("too old") == true)
        } finally {
            tempFile.toFile().delete()
        }
    }

    @Test
    fun `reject future deprecation number`() {
        val tempFile = kotlin.io.path.createTempFile(suffix = ".p1r")
        try {
            val bytes = byteArrayOf(
                0x50, 0x31, 0x52,  // "P1R"
                0x00, 0x00,         // format_class = 0
                102,                // version = 102
                3,                  // deprecation = 3 (should be <= 2)
            )
            tempFile.toFile().writeBytes(bytes)

            val exception = assertThrows<P1RParseException> {
                P1RParser.parseP1R(tempFile)
            }

            assertTrue(exception.message?.contains("too new") == true)
        } finally {
            tempFile.toFile().delete()
        }
    }
}
