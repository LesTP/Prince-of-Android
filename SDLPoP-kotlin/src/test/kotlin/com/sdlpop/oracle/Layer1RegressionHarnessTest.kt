package com.sdlpop.oracle

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Layer1RegressionHarnessTest {

    @Test
    fun `manifest enumerates the 13 reference traces`() {
        val referenceRoot = referenceRoot()
        val replays = Layer1RegressionManifest.fromReferenceRoot(referenceRoot)

        assertEquals(13, replays.size)
        assertEquals("basic_movement", replays.first().id)
        assertEquals("trick_153", replays.last().id)
        replays.forEach { replay ->
            val path = replay.referencePath(referenceRoot)
            assertTrue(Files.isRegularFile(path), "Reference trace exists: $path")
            assertEquals(0, Files.size(path) % StateTraceFormat.FRAME_SIZE)
        }
    }

    @Test
    fun `manifest maps all regression ids to source replays`() {
        val replayRoot = Paths.get(System.getProperty("sdlpop.replayRoot", "../SDLPoP"))
        val replays = Layer1RegressionManifest.fromReplayRoot(replayRoot)

        assertEquals(13, replays.size)
        assertEquals("replays/basic movement.p1r", replays.first().replayFile)
        assertEquals("doc/replays-testcases/trick_153.p1r", replays.last().replayFile)
        replays.forEach { replay ->
            assertTrue(Files.isRegularFile(replay.replayPath(replayRoot)), "Replay source exists for ${replay.id}")
        }
    }

    @Test
    @Tag("layer1-regression")
    fun `workflow writes real Kotlin trace artifacts under build output and compares all manifest traces`() {
        val referenceRoot = referenceRoot()
        val outputRoot = outputRoot().resolve("real-kotlin")
        val harness = Layer1RegressionHarness(referenceRoot, outputRoot)

        val results = harness.run(Layer1RegressionManifest.fromReferenceRoot(referenceRoot))
        val normalizedOutputRoot = outputRoot.toAbsolutePath().normalize()

        assertEquals(13, results.size)
        assertTrue(results.all { it.matched }, results.joinToString("\n") { it.triageReport() })
        results.forEach { result ->
            assertTrue(result.actualPath.startsWith(normalizedOutputRoot))
            assertTrue(Files.isRegularFile(result.actualPath))
        }
    }

    @Test
    fun `divergence report includes replay frame field values and trace paths`() {
        val tempRoot = outputRoot().resolve("divergence-report").also { it.createDirectories() }
        val referenceRoot = tempRoot.resolve("reference").also { it.createDirectories() }
        val actualRoot = tempRoot.resolve("actual")
        val replay = Layer1ReplayTrace("focused", "focused.trace", "focused.p1r")
        val referenceBytes = ByteArray(StateTraceFormat.FRAME_SIZE * 2)
        val actualBytes = referenceBytes.copyOf()
        writeU32(referenceBytes, 0, 0)
        writeU32(referenceBytes, StateTraceFormat.FRAME_SIZE, 1)
        writeU32(actualBytes, 0, 0)
        writeU32(actualBytes, StateTraceFormat.FRAME_SIZE, 1)
        referenceBytes[StateTraceFormat.FRAME_SIZE + 132] = 0xFE.toByte()
        actualBytes[StateTraceFormat.FRAME_SIZE + 132] = 0x02
        Files.write(referenceRoot.resolve(replay.referenceFile), referenceBytes)

        val harness = Layer1RegressionHarness(referenceRoot, actualRoot) { _, outputPath ->
            Files.write(outputPath, actualBytes)
        }

        val result = harness.run(listOf(replay)).single()
        val report = result.triageReport()

        assertFalse(result.matched)
        assertTrue("focused: DIVERGED" in report)
        assertTrue("frame=1" in report)
        assertTrue("frame_number=1" in report)
        assertTrue("field=trobs[0].type" in report)
        assertTrue("byte_offset=132" in report)
        assertTrue("expected=-2" in report)
        assertTrue("actual=2" in report)
        assertTrue("reference=" in report)
        assertTrue("actual_trace=" in report)
    }

    @Test
    fun `runAndRequireMatch fails with triage-ready divergence reports`() {
        val tempRoot = outputRoot().resolve("require-match").also { it.createDirectories() }
        val referenceRoot = tempRoot.resolve("reference").also { it.createDirectories() }
        val actualRoot = tempRoot.resolve("actual")
        val replay = Layer1ReplayTrace("focused", "focused.trace", "focused.p1r")
        val referenceBytes = ByteArray(StateTraceFormat.FRAME_SIZE)
        val actualBytes = referenceBytes.copyOf()
        referenceBytes[68] = 1
        actualBytes[68] = 2
        Files.write(referenceRoot.resolve(replay.referenceFile), referenceBytes)
        val harness = Layer1RegressionHarness(referenceRoot, actualRoot) { _, outputPath ->
            Files.write(outputPath, actualBytes)
        }

        val failure = assertFailsWith<IllegalStateException> {
            harness.runAndRequireMatch(listOf(replay))
        }

        assertTrue("focused: DIVERGED" in failure.message.orEmpty())
        assertTrue("field=curr_room_tiles[0]" in failure.message.orEmpty())
    }

    private fun referenceRoot(): Path =
        Paths.get(System.getProperty("sdlpop.referenceTraceRoot", "../SDLPoP/traces/reference"))

    private fun outputRoot(): Path =
        Paths.get(System.getProperty("sdlpop.kotlinTraceOutput", "build/oracle/layer1-regression"))

    private fun writeU32(bytes: ByteArray, offset: Int, value: Long) {
        bytes[offset] = value.and(0xFF).toByte()
        bytes[offset + 1] = value.shr(8).and(0xFF).toByte()
        bytes[offset + 2] = value.shr(16).and(0xFF).toByte()
        bytes[offset + 3] = value.shr(24).and(0xFF).toByte()
    }
}
