package com.sdlpop.replay

import com.sdlpop.game.GameState
import com.sdlpop.game.Control
import com.sdlpop.game.ExternalStubs
import com.sdlpop.oracle.Layer1ReplayTrace
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
    }

    fun installReplayMoveHook(replay: ReplayData, state: GameState = GameState) {
        activeReplay = replay
        replayEnded = false
        ExternalStubs.doReplayMove = { doReplayMove(state) }
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
}

data class ReplayMove(
    val x: Int,
    val y: Int,
    val shift: Int,
    val special: Int,
)
