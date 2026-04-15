package com.sdlpop.replay

import com.sdlpop.game.Directions
import com.sdlpop.game.GameState
import com.sdlpop.game.Seg002
import com.sdlpop.game.Seg004
import com.sdlpop.game.Seg006
import com.sdlpop.game.Seg007

data class Layer1FrameHooks(
    val doMobs: () -> Unit = { Seg007.doMobs() },
    val processTrobs: () -> Unit = { Seg007.processTrobs() },
    val checkSkel: () -> Unit = { Seg002.checkSkel() },
    val loadKidAndOpp: () -> Unit = { Seg006.loadkidAndOpp() },
    val loadShadAndOpp: () -> Unit = { Seg006.loadshadAndOpp() },
    val saveKid: () -> Unit = { Seg006.savekid() },
    val saveShadAndOpp: () -> Unit = { Seg006.saveshadAndOpp() },
    val loadFramDetCol: () -> Unit = { Seg006.loadFramDetCol() },
    val checkKilledShadow: () -> Unit = { Seg006.checkKilledShadow() },
    val playKid: () -> Unit = { Seg006.playKid() },
    val playGuard: () -> Unit = { Seg006.playGuard() },
    val playSeq: () -> Unit = { Seg006.playSeq() },
    val fallAccel: () -> Unit = { Seg006.fallAccel() },
    val fallSpeed: () -> Unit = { Seg006.fallSpeed() },
    val loadFrameToObj: () -> Unit = { loadFrameToObjForLayer1() },
    val setCharCollision: () -> Unit = { Seg006.setCharCollision() },
    val checkCollisions: () -> Unit = { Seg004.checkCollisions() },
    val checkBumped: () -> Unit = { Seg004.checkBumped() },
    val checkGatePush: () -> Unit = { Seg004.checkGatePush() },
    val checkAction: () -> Unit = { Seg006.checkAction() },
    val checkPress: () -> Unit = { Seg006.checkPress() },
    val checkSpikeBelow: () -> Unit = { Seg006.checkSpikeBelow() },
    val checkSpiked: () -> Unit = { Seg006.checkSpiked() },
    val checkChompedKid: () -> Unit = { Seg004.checkChompedKid() },
    val checkGuardBumped: () -> Unit = { Seg004.checkGuardBumped() },
    val checkChompedGuard: () -> Unit = { Seg004.checkChompedGuard() },
    val checkSwordHurting: () -> Unit = { Seg002.checkSwordHurting() },
    val checkSwordHurt: () -> Unit = { Seg002.checkSwordHurt() },
    val exitRoom: () -> Unit = { Seg002.exitRoom() },
    val checkGuardFallout: () -> Unit = { Seg002.checkGuardFallout() },
)

object Layer1FrameDriver {
    fun playFrame(state: GameState = GameState, hooks: Layer1FrameHooks = Layer1FrameHooks()) {
        hooks.doMobs()
        hooks.processTrobs()
        hooks.checkSkel()
        if (playKidFrame(state, hooks)) return
        playGuardFrame(state, hooks)
        if (state.resurrectTime == 0) {
            hooks.checkSwordHurting()
            hooks.checkSwordHurt()
        }
        hooks.exitRoom()
        hooks.checkGuardFallout()
    }

    fun playKidFrame(state: GameState = GameState, hooks: Layer1FrameHooks = Layer1FrameHooks()): Boolean {
        hooks.loadKidAndOpp()
        hooks.loadFramDetCol()
        hooks.checkKilledShadow()
        hooks.playKid()
        if (state.upsideDown != 0 && state.Char.alive >= 0) {
            state.upsideDown = 0
            state.needFullRedraw = 1
        }
        if (state.isRestartLevel != 0) return true

        if (state.Char.room != 0) {
            hooks.playSeq()
            hooks.fallAccel()
            hooks.fallSpeed()
            hooks.loadFrameToObj()
            hooks.loadFramDetCol()
            hooks.setCharCollision()
            hooks.checkCollisions()
            hooks.checkBumped()
            hooks.checkGatePush()
            hooks.checkAction()
            hooks.checkPress()
            hooks.checkSpikeBelow()
            if (state.resurrectTime == 0) {
                hooks.checkSpiked()
                hooks.checkChompedKid()
            }
        }

        hooks.saveKid()
        return false
    }

    fun playGuardFrame(state: GameState = GameState, hooks: Layer1FrameHooks = Layer1FrameHooks()) {
        if (state.Guard.direction == Directions.NONE) return

        hooks.loadShadAndOpp()
        hooks.loadFramDetCol()
        hooks.checkKilledShadow()
        hooks.playGuard()
        if (state.Char.room == state.drawnRoom) {
            hooks.playSeq()
            if (state.Char.x >= 44 && state.Char.x < 211) {
                hooks.fallAccel()
                hooks.fallSpeed()
                hooks.loadFrameToObj()
                hooks.loadFramDetCol()
                hooks.setCharCollision()
                hooks.checkGuardBumped()
                hooks.checkAction()
                hooks.checkPress()
                hooks.checkSpikeBelow()
                hooks.checkSpiked()
                hooks.checkChompedGuard()
            }
        }
        hooks.saveShadAndOpp()
    }
}

private fun loadFrameToObjForLayer1(state: GameState = GameState) {
    Seg006.resetObjClip()
    Seg006.loadFrame()
    state.objDirection = state.Char.direction
    state.objId = state.curFrame.image
    state.objChtab = 2 + (state.curFrame.sword shr 6)
    state.objX = ((Seg006.charDxForward(state.curFrame.dx) shl 1) - 116).toShort()
    state.objY = state.curFrame.dy + state.Char.y
    if (((state.curFrame.flags xor state.objDirection).toByte().toInt()) >= 0) {
        state.objX = (state.objX + 1).toShort()
    }
}
