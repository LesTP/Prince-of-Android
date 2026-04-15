package com.sdlpop.replay

import com.sdlpop.game.GameState
import com.sdlpop.oracle.Layer1RegressionManifest
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReplayRunnerTest {

    @Test
    fun `loadManifestReplay resolves every Layer 1 manifest source through the parser`() {
        val replayRoot = Paths.get(System.getProperty("sdlpop.replayRoot", "../SDLPoP"))

        Layer1RegressionManifest.fromReplayRoot(replayRoot).forEach { manifestEntry ->
            val input = ReplayRunner.loadManifestReplay(manifestEntry, replayRoot)

            assertEquals(manifestEntry, input.manifestEntry)
            assertEquals(manifestEntry.replayPath(replayRoot), input.replayPath)
            assertTrue(Files.isRegularFile(input.replayPath), "Replay source exists for ${manifestEntry.id}")
            assertTrue(input.replay.numReplayTicks > 0, "Replay has input ticks for ${manifestEntry.id}")
            assertEquals(input.replay.moves.size.toLong(), input.replay.numReplayTicks)
        }
    }

    @Test
    fun `initializeReplayState seeds replay metadata and tick counters`() {
        val replayRoot = Paths.get(System.getProperty("sdlpop.replayRoot", "../SDLPoP"))
        val manifestEntry = Layer1RegressionManifest.replays.first { it.id == "basic_movement" }
        val replay = ReplayRunner.loadManifestReplay(manifestEntry, replayRoot).replay

        GameState.recording = 1
        GameState.replaying = 0
        GameState.startLevel = (-1).toShort()
        GameState.randomSeed = 0
        GameState.numReplayTicks = 0
        GameState.currTick = 99
        GameState.specialMove = 7
        GameState.skippingReplay = 1
        GameState.replaySeekTarget = 3
        GameState.replayFormatClass = -1
        GameState.replayVersionNumber = -1
        GameState.gDeprecationNumber = -1

        ReplayRunner.initializeReplayState(replay)

        assertEquals(0, GameState.recording)
        assertEquals(1, GameState.replaying)
        assertEquals(replay.startLevel.toShort(), GameState.startLevel)
        assertEquals(replay.randomSeed, GameState.randomSeed)
        assertEquals(replay.numReplayTicks, GameState.numReplayTicks)
        assertEquals(0, GameState.currTick)
        assertEquals(0, GameState.specialMove)
        assertEquals(0, GameState.skippingReplay)
        assertEquals(0, GameState.replaySeekTarget)
        assertEquals(replay.formatClass, GameState.replayFormatClass)
        assertEquals(replay.versionNumber, GameState.replayVersionNumber)
        assertEquals(replay.deprecationNumber, GameState.gDeprecationNumber)
    }
}
