package com.sdlpop.replay

import com.sdlpop.game.GameState
import com.sdlpop.oracle.Layer1ReplayTrace
import java.nio.file.Path

data class ReplayRunInput(
    val manifestEntry: Layer1ReplayTrace,
    val replayPath: Path,
    val replay: ReplayData,
)

object ReplayRunner {
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
        state.numReplayTicks = replay.numReplayTicks and 0xFFFFFFFFL
        state.currTick = 0
        state.specialMove = 0
        state.skippingReplay = 0
        state.replaySeekTarget = 0
        state.replayFormatClass = replay.formatClass and 0xFFFF
        state.replayVersionNumber = replay.versionNumber and 0xFF
        state.gDeprecationNumber = replay.deprecationNumber
    }
}
