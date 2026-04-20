package com.sdlpop.replay

import com.sdlpop.game.Control
import com.sdlpop.game.CharIds
import com.sdlpop.game.CharType
import com.sdlpop.game.Directions
import com.sdlpop.game.ExternalStubs
import com.sdlpop.game.FrameIds
import com.sdlpop.game.GameState
import com.sdlpop.game.Tiles
import com.sdlpop.game.Seg005
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
        GameState.currentLevel = -1
        GameState.nextLevel = 0
        GameState.drawnRoom = 0
        GameState.Kid = CharType()
        GameState.Guard = CharType(direction = Directions.NONE)
        GameState.Char = CharType()
        GameState.Opp = CharType()
        GameState.controlX = 0
        GameState.controlY = 0
        GameState.controlShift = 0
        GameState.remMin = 0
        GameState.Kid.alive = -1
        GameState.isRestartLevel = 0
        GameState.needLevel1Music = 0
        GameState.isFeatherFall = 0
        GameState.remTick = 0
        GameState.hitpDelta = 0
        GameState.guardhpDelta = 0
        GameState.canGuardSeeKid = 0
        GameState.knock = 0
        GameState.isShowTime = 0
        GameState.textTimeRemaining = 0
        GameState.textTimeTotal = 0
        GameState.level.fg.fill(0)
        GameState.level.bg.fill(0)
        GameState.level.roomlinks.forEach {
            it.left = 0
            it.right = 0
            it.up = 0
            it.down = 0
        }
        GameState.trobsCount = 0
        GameState.trobs.forEach {
            it.room = 0
            it.tilepos = 0
            it.type = 0
        }
        stopSoundsCount = 0
        ExternalStubs.preserveRoomBufferMutations = false
        ExternalStubs.control = { Seg005.control() }
        ExternalStubs.stopSounds = { stopSoundsCount += 1 }
        ExternalStubs.doReplayMove = { }
        ExternalStubs.getRoomAddress = { room -> ExternalStubs.loadRoomAddress(room) }
    }

    @AfterTest
    fun restoreStubs() {
        ExternalStubs.control = { Seg005.control() }
        ExternalStubs.preserveRoomBufferMutations = false
        ExternalStubs.stopSounds = { }
        ExternalStubs.doReplayMove = { }
        ExternalStubs.getRoomAddress = { room -> ExternalStubs.loadRoomAddress(room) }
        GameState.Kid = CharType()
        GameState.Guard = CharType()
        GameState.Char = CharType()
        GameState.Opp = CharType()
        GameState.currentLevel = -1
        GameState.drawnRoom = 0
        GameState.loadedRoom = 0
        GameState.nextRoom = 0
        GameState.nextLevel = 0
        GameState.currRoom = 0
        GameState.currRoomTiles.fill(0)
        GameState.currRoomModif.fill(0)
        GameState.hitpCurr = 0
        GameState.remMin = 0
        GameState.recording = 0
        GameState.replaying = 0
        GameState.numReplayTicks = 0
        GameState.currTick = 0
        GameState.controlX = 0
        GameState.controlY = 0
        GameState.controlShift = 0
        GameState.startLevel = (-1).toShort()
        GameState.seedWasInit = 0
        GameState.remTick = 0
        GameState.hitpDelta = 0
        GameState.guardhpDelta = 0
        GameState.canGuardSeeKid = 0
        GameState.knock = 0
        GameState.isShowTime = 0
        GameState.textTimeRemaining = 0
        GameState.textTimeTotal = 0
        GameState.level.fg.fill(0)
        GameState.level.bg.fill(0)
        GameState.level.roomlinks.forEach {
            it.left = 0
            it.right = 0
            it.up = 0
            it.down = 0
        }
        GameState.trobsCount = 0
        GameState.trobs.forEach {
            it.room = 0
            it.tilepos = 0
            it.type = 0
        }
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
    fun `resetTraceRunState clears singleton lifecycle state before each manifest replay`() {
        GameState.drawnRoom = 16
        GameState.loadedRoom = 16
        GameState.nextRoom = 16
        GameState.differentRoom = 1
        GameState.currRoomTiles[0] = 4
        GameState.currRoomModif[0] = 7
        GameState.Kid = CharType(room = 16, frame = 99)
        GameState.Guard = CharType(room = 16, direction = Directions.LEFT)
        GameState.currTick = 42
        GameState.replaying = 1
        GameState.canGuardSeeKid = 2
        GameState.hitpDelta = 1
        GameState.guardhpDelta = (-1).toShort()
        GameState.textTimeRemaining = 24

        ReplayRunner.resetTraceRunState()

        assertEquals(0, GameState.drawnRoom)
        assertEquals(0, GameState.loadedRoom)
        assertEquals(0, GameState.nextRoom)
        assertEquals(0, GameState.differentRoom)
        assertEquals(0, GameState.currRoomTiles[0])
        assertEquals(0, GameState.currRoomModif[0])
        assertEquals(0, GameState.Kid.room)
        assertEquals(0, GameState.Guard.direction)
        assertEquals(0, GameState.currTick)
        assertEquals(0, GameState.replaying)
        assertEquals(0, GameState.canGuardSeeKid.toInt())
        assertEquals(0, GameState.hitpDelta.toInt())
        assertEquals(0, GameState.guardhpDelta.toInt())
        assertEquals(0, GameState.textTimeRemaining)
    }

    @Test
    fun `default getRoomAddress preserves in-frame room mutations across same-room reloads`() {
        GameState.level.fg[0] = 1
        GameState.level.bg[0] = 2
        GameState.level.fg[30] = 3
        GameState.level.bg[30] = 4
        ExternalStubs.preserveRoomBufferMutations = true

        ExternalStubs.getRoomAddress(1)
        GameState.currRoomTiles[0] = 5
        GameState.currRoomModif[0] = 6

        ExternalStubs.getRoomAddress(1)

        assertEquals(5, GameState.currRoomTiles[0])
        assertEquals(6, GameState.currRoomModif[0])

        ExternalStubs.getRoomAddress(2)

        assertEquals(5, GameState.level.fg[0])
        assertEquals(6, GameState.level.bg[0])
        assertEquals(3, GameState.currRoomTiles[0])
        assertEquals(4, GameState.currRoomModif[0])
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
        GameState.currentLevel = 1
        GameState.nextLevel = 1

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
        GameState.currentLevel = 1
        GameState.nextLevel = 1
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

    @Test
    fun `Layer1FrameDriver calls translated frame entry points in SDLPoP order`() {
        val calls = mutableListOf<String>()
        val hooks = recordingHooks(calls).copy(
            playKid = {
                calls += "playKid"
                GameState.Char.room = 0
            },
        )

        Layer1FrameDriver.playFrame(hooks = hooks)

        assertEquals(
            listOf(
                "doMobs",
                "processTrobs",
                "checkSkel",
                "checkCanGuardSeeKid",
                "loadKidAndOpp",
                "loadFramDetCol",
                "checkKilledShadow",
                "playKid",
                "saveKid",
                "checkSwordHurting",
                "checkSwordHurt",
                "checkSwordVsSword",
                "doDeltaHp",
                "exitRoom",
                "checkTheEnd",
                "checkGuardFallout",
                "showTime",
            ),
            calls,
        )
    }

    @Test
    fun `Layer1FrameDriver kid frame runs translated room pipeline before saving Kid`() {
        val calls = mutableListOf<String>()
        val hooks = recordingHooks(calls).copy(
            playKid = {
                calls += "playKid"
                GameState.Char.room = 1
            },
        )

        val restarted = Layer1FrameDriver.playKidFrame(hooks = hooks)

        assertEquals(false, restarted)
        assertEquals(
            listOf(
                "loadKidAndOpp",
                "loadFramDetCol",
                "checkKilledShadow",
                "playKid",
                "playSeq",
                "fallAccel",
                "fallSpeed",
                "loadFrameToObj",
                "loadFramDetCol",
                "setCharCollision",
                "bumpIntoOpponent",
                "checkCollisions",
                "checkBumped",
                "checkGatePush",
                "checkAction",
                "checkPress",
                "checkSpikeBelow",
                "checkSpiked",
                "checkChompedKid",
                "checkKnock",
                "saveKid",
            ),
            calls,
        )
    }

    @Test
    fun `Layer1FrameDriver guard frame saves shadow without overwriting Opp`() {
        val calls = mutableListOf<String>()
        val hooks = recordingHooks(calls).copy(
            playGuard = {
                calls += "playGuard"
                GameState.Char.room = 0
            },
        )
        GameState.Guard.direction = Directions.LEFT

        Layer1FrameDriver.playGuardFrame(hooks = hooks)

        assertTrue("saveShad" in calls)
        assertTrue("saveShadAndOpp" !in calls)
    }

    @Test
    fun `HeadlessFrameLifecycle showTime decrements before trace serialization`() {
        GameState.Kid.alive = -1
        GameState.currentLevel = 1
        GameState.nextLevel = 1
        GameState.remMin = 60
        GameState.remTick = 719

        HeadlessFrameLifecycle.showTime()

        assertEquals(60, GameState.remMin.toInt())
        assertEquals(718, GameState.remTick)
    }

    @Test
    fun `HeadlessFrameLifecycle showTime rolls minute and schedules timer text`() {
        GameState.Kid.alive = -1
        GameState.currentLevel = 1
        GameState.nextLevel = 1
        GameState.remMin = 6
        GameState.remTick = 1

        HeadlessFrameLifecycle.showTime()

        assertEquals(5, GameState.remMin.toInt())
        assertEquals(719, GameState.remTick)
        assertEquals(24, GameState.textTimeRemaining)
        assertEquals(24, GameState.textTimeTotal)
        assertEquals(0, GameState.isShowTime)
    }

    @Test
    fun `HeadlessFrameLifecycle drawLevelFirst initializes starting room animated tiles`() {
        GameState.Kid = CharType(room = 2)
        GameState.Char = CharType(room = 2, currRow = 0)
        GameState.currentLevel = 1
        GameState.drawnRoom = 0
        GameState.nextRoom = 0
        GameState.level.fg[30] = Tiles.POTION
        GameState.level.roomlinks[1].left = 1
        GameState.level.roomlinks[1].right = 3
        GameState.level.roomlinks[1].up = 4
        GameState.level.roomlinks[1].down = 5
        GameState.level.roomlinks[3].left = 6
        GameState.level.roomlinks[3].right = 7
        GameState.level.roomlinks[4].left = 8
        GameState.level.roomlinks[4].right = 9

        HeadlessFrameLifecycle.headlessDrawLevelFirst()

        assertEquals(2, GameState.nextRoom)
        assertEquals(2, GameState.drawnRoom)
        assertEquals(1, GameState.roomL)
        assertEquals(3, GameState.roomR)
        assertEquals(4, GameState.roomA)
        assertEquals(5, GameState.roomB)
        assertEquals(6, GameState.roomAL)
        assertEquals(7, GameState.roomAR)
        assertEquals(8, GameState.roomBL)
        assertEquals(9, GameState.roomBR)
        assertEquals(Tiles.POTION, GameState.currRoomTiles[0])
        assertEquals(1, GameState.trobsCount.toInt())
        assertEquals(2, GameState.trobs[0].room)
        assertEquals(0, GameState.trobs[0].tilepos)
    }

    @Test
    fun `Layer1FrameDriver consumes replay input deterministically on focused kid slice`() {
        val replay = replayWithMoves(55.toByte(), 77.toByte())
        ReplayRunner.initializeReplayState(replay)
        ReplayRunner.installReplayMoveHook(replay)
        GameState.currentLevel = 1
        GameState.nextLevel = 1
        GameState.Kid = CharType(
            frame = FrameIds.frame_15_stand,
            x = 100,
            y = 140,
            direction = Directions.RIGHT,
            room = 0,
            charid = CharIds.KID,
            alive = -1,
        )
        GameState.Guard = CharType(direction = Directions.NONE)
        GameState.hitpCurr = 1

        Layer1FrameDriver.playKidFrame()

        assertEquals(1, GameState.currTick)
        assertEquals(-1, GameState.controlX)
        assertEquals(1, GameState.controlY)
        assertEquals(Control.HELD, GameState.controlShift)

        Layer1FrameDriver.playKidFrame()

        assertEquals(2, GameState.currTick)
        assertEquals(1, GameState.controlX)
        assertEquals(-1, GameState.controlY)
        assertEquals(Control.RELEASED, GameState.controlShift)
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

    private fun recordingHooks(calls: MutableList<String>): Layer1FrameHooks =
        Layer1FrameHooks(
            doMobs = { calls += "doMobs" },
            processTrobs = { calls += "processTrobs" },
            checkSkel = { calls += "checkSkel" },
            checkCanGuardSeeKid = { calls += "checkCanGuardSeeKid" },
            loadKidAndOpp = { calls += "loadKidAndOpp" },
            loadShadAndOpp = { calls += "loadShadAndOpp" },
            saveKid = { calls += "saveKid" },
            saveShad = { calls += "saveShad" },
            saveShadAndOpp = { calls += "saveShadAndOpp" },
            loadFramDetCol = { calls += "loadFramDetCol" },
            checkKilledShadow = { calls += "checkKilledShadow" },
            playKid = { calls += "playKid" },
            playGuard = { calls += "playGuard" },
            playSeq = { calls += "playSeq" },
            fallAccel = { calls += "fallAccel" },
            fallSpeed = { calls += "fallSpeed" },
            loadFrameToObj = { calls += "loadFrameToObj" },
            setCharCollision = { calls += "setCharCollision" },
            bumpIntoOpponent = { calls += "bumpIntoOpponent" },
            checkCollisions = { calls += "checkCollisions" },
            checkBumped = { calls += "checkBumped" },
            checkGatePush = { calls += "checkGatePush" },
            checkAction = { calls += "checkAction" },
            checkPress = { calls += "checkPress" },
            checkSpikeBelow = { calls += "checkSpikeBelow" },
            checkSpiked = { calls += "checkSpiked" },
            checkChompedKid = { calls += "checkChompedKid" },
            checkKnock = { calls += "checkKnock" },
            checkGuardBumped = { calls += "checkGuardBumped" },
            checkChompedGuard = { calls += "checkChompedGuard" },
            checkSwordHurting = { calls += "checkSwordHurting" },
            checkSwordHurt = { calls += "checkSwordHurt" },
            checkSwordVsSword = { calls += "checkSwordVsSword" },
            doDeltaHp = { calls += "doDeltaHp" },
            exitRoom = { calls += "exitRoom" },
            checkTheEnd = { calls += "checkTheEnd" },
            checkGuardFallout = { calls += "checkGuardFallout" },
            showTime = { calls += "showTime" },
        )
}
