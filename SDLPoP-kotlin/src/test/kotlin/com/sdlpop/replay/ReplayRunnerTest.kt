package com.sdlpop.replay

import com.sdlpop.game.Control
import com.sdlpop.game.ExternalStubs
import com.sdlpop.game.GameState
import com.sdlpop.oracle.Layer1RegressionManifest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReplayRunnerTest {
    private var stopSoundsCount = 0

    @BeforeTest
    fun resetState() {
        GameState.recording = 0
        GameState.replaying = 0
        GameState.startLevel = 0
        GameState.randomSeed = 0
        GameState.savedRandomSeed = 0
        GameState.numReplayTicks = 0
        GameState.currTick = 0
        GameState.specialMove = 0
        GameState.skippingReplay = 0
        GameState.replaySeekTarget = 0
        GameState.replayFormatClass = 0
        GameState.replayVersionNumber = 0
        GameState.gDeprecationNumber = 0
        GameState.seedWasInit = 0
        GameState.isValidateMode = 0
        GameState.currentLevel = 1
        GameState.nextLevel = 1
        GameState.controlX = 0
        GameState.controlY = 0
        GameState.controlShift = 0
        GameState.remMin = 0
        GameState.Kid.alive = -1
        GameState.isRestartLevel = 0
        GameState.needLevel1Music = 0
        GameState.isFeatherFall = 0
        stopSoundsCount = 0
        ExternalStubs.stopSounds = { stopSoundsCount += 1 }
        ExternalStubs.doReplayMove = { }
    }

    @AfterTest
    fun restoreStubs() {
        ExternalStubs.stopSounds = { }
        ExternalStubs.doReplayMove = { }
        GameState.seedWasInit = 0
    }

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
        GameState.savedRandomSeed = 0
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
        assertEquals(replay.randomSeed, GameState.savedRandomSeed)
        assertEquals(replay.numReplayTicks, GameState.numReplayTicks)
        assertEquals(0, GameState.currTick)
        assertEquals(0, GameState.specialMove)
        assertEquals(0, GameState.skippingReplay)
        assertEquals(0, GameState.replaySeekTarget)
        assertEquals(replay.formatClass, GameState.replayFormatClass)
        assertEquals(replay.versionNumber, GameState.replayVersionNumber)
        assertEquals(replay.deprecationNumber, GameState.gDeprecationNumber)
    }

    @Test
    fun `decodeMove unpacks signed direction shift and special bits`() {
        val move = ReplayRunner.decodeMove(55.toByte())

        assertEquals(-1, move.x)
        assertEquals(1, move.y)
        assertEquals(1, move.shift)
        assertEquals(ReplayRunner.MOVE_RESTART_LEVEL, move.special)
    }

    @Test
    fun `ExternalStubs doReplayMove consumes replay bytes and advances ticks`() {
        val replay = replayWithMoves(55.toByte(), 77.toByte())
        ReplayRunner.initializeReplayState(replay)
        ReplayRunner.installReplayMoveHook(replay)

        ExternalStubs.doReplayMove()

        assertEquals(replay.randomSeed, GameState.randomSeed)
        assertEquals(1, GameState.seedWasInit)
        assertEquals(-1, GameState.controlX)
        assertEquals(1, GameState.controlY)
        assertEquals(Control.HELD, GameState.controlShift)
        assertEquals(1, GameState.currTick)
        assertEquals(1, GameState.isRestartLevel)
        assertEquals(1, stopSoundsCount)

        GameState.needLevel1Music = 2
        GameState.isFeatherFall = 1

        ExternalStubs.doReplayMove()

        assertEquals(1, GameState.controlX)
        assertEquals(-1, GameState.controlY)
        assertEquals(Control.RELEASED, GameState.controlShift)
        assertEquals(2, GameState.currTick)
        assertEquals(0, GameState.needLevel1Music)
        assertEquals(0, GameState.isFeatherFall)
        assertEquals(2, stopSoundsCount)
    }

    @Test
    fun `doReplayMove ignores shift while dead and applies validate seek state`() {
        val replay = replayWithMoves(16.toByte())
        ReplayRunner.initializeReplayState(replay)
        ReplayRunner.installReplayMoveHook(replay)
        GameState.isValidateMode = 1
        GameState.remMin = 1
        GameState.Kid.alive = 7

        ExternalStubs.doReplayMove()

        assertEquals(1, GameState.skippingReplay)
        assertEquals(ReplayRunner.REPLAY_SEEK_END, GameState.replaySeekTarget)
        assertEquals(Control.RELEASED, GameState.controlShift)
        assertEquals(1, GameState.currTick)
    }

    @Test
    fun `doReplayMove ends replay at tick limit without consuming another move`() {
        val replay = replayWithMoves(0.toByte())
        ReplayRunner.initializeReplayState(replay)
        ReplayRunner.installReplayMoveHook(replay)
        GameState.currTick = replay.numReplayTicks
        GameState.replaying = 1
        GameState.skippingReplay = 1

        ExternalStubs.doReplayMove()

        assertEquals(true, ReplayRunner.replayEnded)
        assertEquals(0, GameState.replaying)
        assertEquals(0, GameState.skippingReplay)
        assertEquals(replay.numReplayTicks, GameState.currTick)
    }

    private fun replayWithMoves(vararg moves: Byte): ReplayData =
        ReplayData(
            formatClass = 0,
            versionNumber = 101,
            deprecationNumber = 2,
            creationTime = 0,
            levelsetName = "",
            implementationName = "test",
            savestateSize = 0,
            savestateBuffer = ByteArray(0),
            optionsSections = List(5) { ByteArray(0) },
            startLevel = 1,
            randomSeed = 0x12345678,
            numReplayTicks = moves.size.toLong(),
            moves = byteArrayOf(*moves),
        )
}
