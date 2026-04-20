package com.sdlpop.replay

import com.sdlpop.game.CharType
import com.sdlpop.game.Control
import com.sdlpop.game.ExternalStubs
import com.sdlpop.game.GameState
import com.sdlpop.game.Seg003
import com.sdlpop.game.SoundFlags
import com.sdlpop.game.LevelType
import com.sdlpop.game.MobType
import com.sdlpop.game.TrobType
import com.sdlpop.oracle.Layer1ReplayTrace
import com.sdlpop.oracle.StateTraceFormat
import java.nio.file.Files
import java.nio.file.Path

data class ReplayRunInput(
    val manifestEntry: Layer1ReplayTrace,
    val replayPath: Path,
    val replay: ReplayData,
)

object ReplayRunner {
    const val MOVE_RESTART_LEVEL = 1
    const val MOVE_EFFECT_END = 2
    const val REPLAY_SEEK_END = 2

    private var activeReplay: ReplayData? = null
    var replayEnded: Boolean = false
        private set

    fun loadManifestReplay(manifestEntry: Layer1ReplayTrace, replayRoot: Path): ReplayRunInput {
        val replayPath = manifestEntry.replayPath(replayRoot)
        return ReplayRunInput(
            manifestEntry = manifestEntry,
            replayPath = replayPath,
            replay = P1RParser.parseP1R(replayPath),
        )
    }

    fun initializeReplayState(replay: ReplayData, state: GameState = GameState) {
        state.recording = 0
        state.replaying = 1
        state.startLevel = replay.startLevel.toShort()
        state.randomSeed = replay.randomSeed and 0xFFFFFFFFL
        state.savedRandomSeed = replay.randomSeed and 0xFFFFFFFFL
        state.numReplayTicks = replay.numReplayTicks and 0xFFFFFFFFL
        state.currTick = 0
        state.specialMove = 0
        state.skippingReplay = 0
        state.replaySeekTarget = 0
        state.replayFormatClass = replay.formatClass and 0xFFFF
        state.replayVersionNumber = replay.versionNumber and 0xFF
        state.gDeprecationNumber = replay.deprecationNumber
        // Reference build always has digital sound enabled — needed for
        // lastLooseSound tracking which affects RNG consumption in loose_shake.
        state.soundFlags = state.soundFlags or SoundFlags.DIGI
    }

    fun restoreSavestate(replay: ReplayData, state: GameState = GameState) {
        val reader = SavestateReader(replay.savestateBuffer)

        readLevel(reader, state.level)
        state.checkpoint = reader.u16()
        state.upsideDown = reader.u16()
        state.drawnRoom = reader.u16()
        state.currentLevel = reader.u16()
        state.nextLevel = reader.u16()
        state.mobsCount = reader.s16()
        repeat(state.mobs.size) { readMob(reader, state.mobs[it]) }
        state.trobsCount = reader.s16()
        repeat(state.trobs.size) { readTrob(reader, state.trobs[it]) }
        state.leveldoorOpen = reader.u16()

        readChar(reader, state.Kid)
        state.hitpCurr = reader.u16()
        state.hitpMax = reader.u16()
        state.hitpBegLev = reader.u16()
        state.grabTimer = reader.u16()
        state.holdingSword = reader.u16()
        state.unitedWithShadow = reader.s16()
        state.haveSword = reader.u16()
        state.kidSwordStrike = reader.u16()
        state.pickupObjType = reader.s16()
        state.offguard = reader.u16()

        readChar(reader, state.Guard)
        readChar(reader, state.Char)
        readChar(reader, state.Opp)
        state.guardhpCurr = reader.u16()
        state.guardhpMax = reader.u16()
        state.demoIndex = reader.u16()
        state.demoTime = reader.s16()
        state.currGuardColor = reader.u16()
        state.guardNoticeTimer = reader.s16()
        state.guardSkill = reader.u16()
        state.shadowInitialized = reader.u16()
        state.guardRefrac = reader.u16()
        state.justblocked = reader.u16()
        state.droppedout = reader.u16()

        repeat(state.currRowCollRoom.size) { state.currRowCollRoom[it] = reader.s8() }
        repeat(state.currRowCollFlags.size) { state.currRowCollFlags[it] = reader.u8() }
        repeat(state.belowRowCollRoom.size) { state.belowRowCollRoom[it] = reader.s8() }
        repeat(state.belowRowCollFlags.size) { state.belowRowCollFlags[it] = reader.u8() }
        repeat(state.aboveRowCollRoom.size) { state.aboveRowCollRoom[it] = reader.s8() }
        repeat(state.aboveRowCollFlags.size) { state.aboveRowCollFlags[it] = reader.u8() }
        state.prevCollisionRow = reader.s8()

        state.flashColor = reader.u16()
        state.flashTime = reader.u16()
        state.needLevel1Music = reader.u16()
        state.isScreaming = reader.u16()
        state.isFeatherFall = reader.u16()
        state.lastLooseSound = reader.u16()
        state.randomSeed = reader.u32()
        state.remMin = reader.s16()
        state.remTick = reader.u16()

        state.controlX = reader.s8()
        state.controlY = reader.s8()
        state.controlShift = reader.s8()
        state.controlForward = reader.s8()
        state.controlBackward = reader.s8()
        state.controlUp = reader.s8()
        state.controlDown = reader.s8()
        state.controlShift2 = reader.s8()
        state.ctrl1Forward = reader.s8()
        state.ctrl1Backward = reader.s8()
        state.ctrl1Up = reader.s8()
        state.ctrl1Down = reader.s8()
        state.ctrl1Shift2 = reader.s8()
        state.currTick = reader.u32()

        reader.skip(750) // torch_colors[25][30], used only by rendering.
        if (reader.hasRemaining()) state.superJumpFall = reader.u8()
        if (reader.hasRemaining()) state.superJumpTimer = reader.u8()
        if (reader.hasRemaining()) state.superJumpRoom = reader.u8()
        if (reader.hasRemaining()) state.superJumpCol = reader.s8()
        if (reader.hasRemaining()) state.superJumpRow = reader.s8()
        if (reader.remaining() >= 2) state.isGuardNotice = reader.u16()
        if (reader.remaining() >= 2) state.canGuardSeeKid = reader.s16()

        ExternalStubs.getRoomAddress(state.drawnRoom)
        state.currRoom = state.drawnRoom.toShort()
        state.loadedRoom = state.drawnRoom
        state.currTick = 0
    }

    fun installReplayMoveHook(replay: ReplayData, state: GameState = GameState) {
        activeReplay = replay
        replayEnded = false
        ExternalStubs.doReplayMove = { doReplayMove(state) }
    }

    fun writeLayer1Trace(
        manifestEntry: Layer1ReplayTrace,
        replayRoot: Path,
        outputPath: Path,
        state: GameState = GameState,
    ): Path {
        val input = loadManifestReplay(manifestEntry, replayRoot)
        resetTraceRunState(state)
        ExternalStubs.preserveRoomBufferMutations = true
        initializeReplayState(input.replay, state)
        restoreSavestate(input.replay, state)
        HeadlessFrameLifecycle.headlessDrawLevelFirst()
        installReplayMoveHook(input.replay, state)

        Files.createDirectories(outputPath.parent)
        Files.newOutputStream(outputPath).use { output ->
            var frameNumber = 0L
            while (!replayEnded && frameNumber < input.replay.numReplayTicks) {
                // Match C play_level_2() order: reset deltas → timers → play_frame
                state.guardhpDelta = 0
                state.hitpDelta = 0
                Seg003.timers()
                Layer1FrameDriver.playFrame(state)
                output.write(StateTraceFormat.serializeFrameBytes(frameNumber, state))
                frameNumber += 1
            }
        }
        return outputPath
    }

    fun resetTraceRunState(state: GameState = GameState) {
        state.Kid = CharType()
        state.Guard = CharType()
        state.Char = CharType()
        state.Opp = CharType()

        state.currentLevel = -1
        state.drawnRoom = 0
        state.loadedRoom = 0
        state.nextRoom = 0
        state.nextLevel = 0
        state.currRoom = 0
        state.differentRoom = 0
        state.roomL = 0
        state.roomR = 0
        state.roomA = 0
        state.roomB = 0
        state.roomBR = 0
        state.roomBL = 0
        state.roomAR = 0
        state.roomAL = 0
        state.currRoomTiles.fill(0)
        state.currRoomModif.fill(0)

        state.hitpCurr = 0
        state.hitpMax = 0
        state.hitpDelta = 0
        state.guardhpCurr = 0
        state.guardhpMax = 0
        state.guardhpDelta = 0
        state.remMin = 0
        state.remTick = 0
        state.canGuardSeeKid = 0
        state.isGuardNotice = 0
        state.guardNoticeTimer = 0
        state.knock = 0
        state.justblocked = 0
        state.offguard = 0
        state.currentSound = 0
        state.nextSound = 0
        state.soundFlags = 0
        state.lastLooseSound = 0
        state.roomleaveResult = 0
        state.exitRoomTimer = 0
        state.isRestartLevel = 0
        state.isShowTime = 0
        state.textTimeRemaining = 0
        state.textTimeTotal = 0
        state.needLevel1Music = 0
        state.isFeatherFall = 0
        state.resurrectTime = 0
        state.seamless = 0
        state.seedWasInit = 0
        state.randomSeed = 0
        state.savedRandomSeed = 0
        state.preservedSeed = 0
        state.keepLastSeed = 0
        state.recording = 0
        state.replaying = 0
        state.numReplayTicks = 0
        state.specialMove = 0
        state.skippingReplay = 0
        state.replaySeekTarget = 0
        state.isValidateMode = 0
        state.currTick = 0

        state.currRowCollRoom.fill(0)
        state.currRowCollFlags.fill(0)
        state.belowRowCollRoom.fill(0)
        state.belowRowCollFlags.fill(0)
        state.aboveRowCollRoom.fill(0)
        state.aboveRowCollFlags.fill(0)
        state.prevCollRoom.fill(0)
        state.prevCollFlags.fill(0)
        state.prevCollisionRow = 0
        state.wipeFrames.fill(0)
        state.wipeHeights.fill(0)
        state.redrawFramesAnim.fill(0)
        state.redrawFrames2.fill(0)
        state.redrawFramesFloorOverlay.fill(0)
        state.redrawFramesFull.fill(0)
        state.redrawFramesFore.fill(0)
        state.tileObjectRedraw.fill(0)
        state.redrawFramesAbove.fill(0)
    }

    fun doReplayMove(state: GameState = GameState) {
        val replay = activeReplay ?: error("No replay installed")

        if (state.currTick == 0L) {
            state.randomSeed = state.savedRandomSeed and 0xFFFFFFFFL
            state.seedWasInit = 1

            if (state.isValidateMode != 0) {
                state.skippingReplay = 1
                state.replaySeekTarget = REPLAY_SEEK_END
            }
        }

        if (state.currTick == state.numReplayTicks) {
            endReplay(state)
            return
        }

        if (state.currentLevel == state.nextLevel) {
            val move = decodeMove(replay.moves[state.currTick.toInt()])
            state.controlX = move.x
            state.controlY = move.y
            state.controlShift =
                if (state.remMin.toInt() != 0 && state.Kid.alive > 6) {
                    Control.RELEASED
                } else if (move.shift != 0) {
                    Control.HELD
                } else {
                    Control.RELEASED
                }

            when (move.special) {
                MOVE_RESTART_LEVEL -> {
                    ExternalStubs.stopSounds()
                    state.isRestartLevel = 1
                }
                MOVE_EFFECT_END -> {
                    ExternalStubs.stopSounds()
                    if (state.needLevel1Music == 2) state.needLevel1Music = 0
                    state.isFeatherFall = 0
                }
            }

            state.currTick = (state.currTick + 1) and 0xFFFFFFFFL
        }
    }

    fun decodeMove(bits: Byte): ReplayMove {
        val value = bits.toInt() and 0xFF
        return ReplayMove(
            x = decodeSigned2(value),
            y = decodeSigned2(value shr 2),
            shift = (value shr 4) and 0x01,
            special = (value shr 5) and 0x07,
        )
    }

    private fun decodeSigned2(value: Int): Int {
        val raw = value and 0x03
        return if (raw >= 2) raw - 4 else raw
    }

    private fun endReplay(state: GameState) {
        replayEnded = true
        state.replaying = 0
        state.skippingReplay = 0
    }

    private fun readLevel(reader: SavestateReader, level: LevelType) {
        repeat(level.fg.size) { level.fg[it] = reader.u8() }
        repeat(level.bg.size) { level.bg[it] = reader.u8() }
        repeat(level.doorlinks1.size) { level.doorlinks1[it] = reader.u8() }
        repeat(level.doorlinks2.size) { level.doorlinks2[it] = reader.u8() }
        repeat(level.roomlinks.size) {
            level.roomlinks[it].left = reader.u8()
            level.roomlinks[it].right = reader.u8()
            level.roomlinks[it].up = reader.u8()
            level.roomlinks[it].down = reader.u8()
        }
        level.usedRooms = reader.u8()
        repeat(level.roomxs.size) { level.roomxs[it] = reader.u8() }
        repeat(level.roomys.size) { level.roomys[it] = reader.u8() }
        repeat(level.fill1.size) { level.fill1[it] = reader.u8() }
        level.startRoom = reader.u8()
        level.startPos = reader.u8()
        level.startDir = reader.s8()
        repeat(level.fill2.size) { level.fill2[it] = reader.u8() }
        repeat(level.guardsTile.size) { level.guardsTile[it] = reader.u8() }
        repeat(level.guardsDir.size) { level.guardsDir[it] = reader.u8() }
        repeat(level.guardsX.size) { level.guardsX[it] = reader.u8() }
        repeat(level.guardsSeqLo.size) { level.guardsSeqLo[it] = reader.u8() }
        repeat(level.guardsSkill.size) { level.guardsSkill[it] = reader.u8() }
        repeat(level.guardsSeqHi.size) { level.guardsSeqHi[it] = reader.u8() }
        repeat(level.guardsColor.size) { level.guardsColor[it] = reader.u8() }
        repeat(level.fill3.size) { level.fill3[it] = reader.u8() }
    }

    private fun readChar(reader: SavestateReader, char: CharType) {
        char.frame = reader.u8()
        char.x = reader.u8()
        char.y = reader.u8()
        char.direction = reader.s8()
        char.currCol = reader.s8()
        char.currRow = reader.s8()
        char.action = reader.u8()
        char.fallX = reader.s8()
        char.fallY = reader.s8()
        char.room = reader.u8()
        char.repeat = reader.u8()
        char.charid = reader.u8()
        char.sword = reader.u8()
        char.alive = reader.s8()
        char.currSeq = reader.u16()
    }

    private fun readTrob(reader: SavestateReader, trob: TrobType) {
        trob.tilepos = reader.u8()
        trob.room = reader.u8()
        trob.type = reader.s8()
    }

    private fun readMob(reader: SavestateReader, mob: MobType) {
        mob.xh = reader.u8()
        mob.y = reader.u8()
        mob.room = reader.u8()
        mob.speed = reader.s8()
        mob.type = reader.u8()
        mob.row = reader.u8()
    }
}

data class ReplayMove(
    val x: Int,
    val y: Int,
    val shift: Int,
    val special: Int,
)

private class SavestateReader(private val bytes: ByteArray) {
    private var offset: Int = 0

    fun hasRemaining(): Boolean = offset < bytes.size

    fun remaining(): Int = bytes.size - offset

    fun u8(): Int = bytes[offset++].toInt() and 0xFF

    fun s8(): Int = bytes[offset++].toInt().toByte().toInt()

    fun u16(): Int {
        val value = (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8)
        offset += 2
        return value
    }

    fun s16(): Short = u16().toShort()

    fun u32(): Long {
        val value = (bytes[offset].toLong() and 0xFF) or
            ((bytes[offset + 1].toLong() and 0xFF) shl 8) or
            ((bytes[offset + 2].toLong() and 0xFF) shl 16) or
            ((bytes[offset + 3].toLong() and 0xFF) shl 24)
        offset += 4
        return value and 0xFFFFFFFFL
    }

    fun skip(count: Int) {
        offset = minOf(bytes.size, offset + count)
    }
}
