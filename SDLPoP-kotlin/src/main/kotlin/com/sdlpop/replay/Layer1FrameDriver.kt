package com.sdlpop.replay

import com.sdlpop.game.Directions
import com.sdlpop.game.GameState
import com.sdlpop.game.CharIds as CID
import com.sdlpop.game.Seg002
import com.sdlpop.game.Seg004
import com.sdlpop.game.Seg003
import com.sdlpop.game.Seg006
import com.sdlpop.game.Seg007
import com.sdlpop.game.SoundIds as Snd
import com.sdlpop.game.Tiles as T
import com.sdlpop.game.TileGeometry as TG

data class Layer1FrameHooks(
    val doMobs: () -> Unit = { Seg007.doMobs() },
    val processTrobs: () -> Unit = { Seg007.processTrobs() },
    val checkSkel: () -> Unit = { Seg002.checkSkel() },
    val checkCanGuardSeeKid: () -> Unit = { Seg003.checkCanGuardSeeKid() },
    val loadKidAndOpp: () -> Unit = { Seg006.loadkidAndOpp() },
    val loadShadAndOpp: () -> Unit = { Seg006.loadshadAndOpp() },
    val saveKid: () -> Unit = { Seg006.savekid() },
    val saveShad: () -> Unit = { Seg006.saveshad() },
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
    val bumpIntoOpponent: () -> Unit = { Seg003.bumpIntoOpponent() },
    val checkCollisions: () -> Unit = { Seg004.checkCollisions() },
    val checkBumped: () -> Unit = { Seg004.checkBumped() },
    val checkGatePush: () -> Unit = { Seg004.checkGatePush() },
    val checkAction: () -> Unit = { Seg006.checkAction() },
    val checkPress: () -> Unit = { Seg006.checkPress() },
    val checkSpikeBelow: () -> Unit = { Seg006.checkSpikeBelow() },
    val checkSpiked: () -> Unit = { Seg006.checkSpiked() },
    val checkChompedKid: () -> Unit = { Seg004.checkChompedKid() },
    val checkKnock: () -> Unit = { Seg003.checkKnock() },
    val checkGuardBumped: () -> Unit = { Seg004.checkGuardBumped() },
    val checkChompedGuard: () -> Unit = { Seg004.checkChompedGuard() },
    val checkSwordHurting: () -> Unit = { Seg002.checkSwordHurting() },
    val checkSwordHurt: () -> Unit = { Seg002.checkSwordHurt() },
    val checkSwordVsSword: () -> Unit = { HeadlessFrameLifecycle.checkSwordVsSword() },
    val doDeltaHp: () -> Unit = { HeadlessFrameLifecycle.doDeltaHp() },
    val exitRoom: () -> Unit = { Seg002.exitRoom() },
    val checkTheEnd: () -> Unit = { HeadlessFrameLifecycle.checkTheEnd() },
    val checkGuardFallout: () -> Unit = { Seg002.checkGuardFallout() },
    val showTime: () -> Unit = { HeadlessFrameLifecycle.showTime() },
)

private val SOUND_PRIORITY_TABLE = intArrayOf(
    0x14, 0x1E, 0x23, 0x66, 0x32, 0x37, 0x30, 0x30, 0x4B, 0x50,
    0x0D, 0x12, 0x0C, 0x0B, 0x69, 0x6E, 0x73, 0x78, 0x7D, 0x82,
    0x91, 0x96, 0x9B, 0xA0, 0x01, 0x01, 0x01, 0x01, 0x01, 0x13,
    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x00,
    0x01, 0x01, 0x01, 0x01, 0x87, 0x8C, 0x0F, 0x10, 0x15, 0x16,
    0x01, 0x00, 0x01, 0x01, 0x01, 0x01, 0x01, 0x00,
)

object Layer1FrameDriver {
    fun playFrame(state: GameState = GameState, hooks: Layer1FrameHooks = Layer1FrameHooks()) {
        hooks.doMobs()
        hooks.processTrobs()
        hooks.checkSkel()
        hooks.checkCanGuardSeeKid()
        if (playKidFrame(state, hooks)) return
        playGuardFrame(state, hooks)
        if (state.resurrectTime == 0) {
            hooks.checkSwordHurting()
            hooks.checkSwordHurt()
        }
        hooks.checkSwordVsSword()
        hooks.doDeltaHp()
        hooks.exitRoom()
        hooks.checkTheEnd()
        hooks.checkGuardFallout()
        // Level-specific exit events (from C play_frame lines 955-978)
        if (state.currentLevel == 0) {
            // Special event: level 0 running exit
            if (state.Kid.room == state.custom.demoEndRoom) {
                state.startLevel = (-1).toShort()
                state.needQuotes = 1
                com.sdlpop.game.ExternalStubs.startGame()
            }
        } else if (state.currentLevel == state.custom.fallingExitLevel) {
            // Special event: level 6 falling exit
            if (state.roomleaveResult.toInt() == -2) {
                state.Kid.y = -1
                com.sdlpop.game.ExternalStubs.stopSounds()
                state.nextLevel++
            }
        } else if (state.currentLevel in state.custom.tblSeamlessExit.indices &&
            state.custom.tblSeamlessExit[state.currentLevel] >= 0) {
            // Special event: level 12 running exit
            if (state.Kid.room == state.custom.tblSeamlessExit[state.currentLevel]) {
                state.nextLevel++
                com.sdlpop.game.ExternalStubs.stopSounds()
                state.seamless = 1
            }
        }
        hooks.showTime()
        // expiring doesn't count on Jaffar/princess level
        if (state.currentLevel < 13 && state.remMin.toInt() == 0) {
            com.sdlpop.game.ExternalStubs.expired()
        }
    }

    fun playKidFrame(state: GameState = GameState, hooks: Layer1FrameHooks = Layer1FrameHooks()): Boolean {
        hooks.loadKidAndOpp()
        hooks.loadFramDetCol()
        hooks.checkKilledShadow()
        hooks.playKid()
        if (state.upsideDown != 0 && state.Char.alive >= 0) {
            state.upsideDown = 0
            state.needRedrawBecauseFlipped = 1
        }
        if (state.isRestartLevel != 0) return true

        if (state.Char.room != 0) {
            hooks.playSeq()
            hooks.fallAccel()
            hooks.fallSpeed()
            hooks.loadFrameToObj()
            hooks.loadFramDetCol()
            hooks.setCharCollision()
            hooks.bumpIntoOpponent()
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
            hooks.checkKnock()
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
        hooks.saveShad()
    }
}

object HeadlessFrameLifecycle {
    private val gs = GameState

    fun headlessDrawLevelFirst() {
        gs.nextRoom = gs.Kid.room
        if (gs.nextRoom != 0 && gs.nextRoom != gs.drawnRoom) {
            checkTheEnd()
        } else {
            loadRoomLinks()
        }
        redrawScreen(drawingDifferentRoom = false)
    }

    fun headlessDrawGameFrame() {
        drawGameFrame()
    }

    fun drawGameFrame() {
        if (gs.needFullRedraw != 0) {
            redrawScreen(drawingDifferentRoom = false)
            gs.needFullRedraw = 0
        } else if (gs.differentRoom != 0) {
            gs.drawnRoom = gs.nextRoom
            if (isPalaceLevel()) {
                genPalaceWallColors()
            }
            redrawScreen(drawingDifferentRoom = true)
        } else if (gs.needRedrawBecauseFlipped != 0) {
            gs.needRedrawBecauseFlipped = 0
            redrawScreen(drawingDifferentRoom = false)
        } else {
            gs.drectsCount = 0.toShort()
        }

        playNextSound()
        updateTextTimer()
    }

    fun playNextSound() {
        val nextSound = gs.nextSound.toInt()
        if (nextSound >= 0) {
            val currentSound = gs.currentSound.coerceIn(0, gs.soundInterruptible.lastIndex)
            if (com.sdlpop.game.ExternalStubs.checkSoundPlaying() == 0 ||
                (gs.soundInterruptible[currentSound] != 0 &&
                    soundPriority(nextSound) <= soundPriority(gs.currentSound))
            ) {
                gs.currentSound = nextSound
            }
        }
        gs.nextSound = (-1).toShort()
    }

    private fun updateTextTimer() {
        if (gs.textTimeRemaining == 1) {
            if (gs.textTimeTotal == 36 || gs.textTimeTotal == 288) {
                gs.startLevel = (-1).toShort()
                gs.needQuotes = 1
                if (gs.recording != 0) {
                    gs.recording = 0
                }
                if (gs.replaying != 0) {
                    gs.replaying = 0
                }
                com.sdlpop.game.ExternalStubs.startGame()
            } else {
                com.sdlpop.game.ExternalStubs.eraseBottomText(1)
            }
        } else if (gs.textTimeRemaining != 0 && gs.textTimeTotal != 1188) {
            gs.textTimeRemaining = (gs.textTimeRemaining - 1) and 0xFFFF
            if (gs.textTimeTotal == 288 && gs.textTimeRemaining < 72) {
                val blinkFrame = gs.textTimeRemaining % 12
                if (blinkFrame > 3) {
                    com.sdlpop.game.ExternalStubs.eraseBottomText(0)
                } else if (blinkFrame == 3) {
                    com.sdlpop.game.ExternalStubs.displayTextBottom("Press Button to Continue")
                    com.sdlpop.game.ExternalStubs.playSound(Snd.BLINK)
                }
            }
        }
    }

    fun checkSwordVsSword() {
        if (gs.Kid.frame == 167 || gs.Guard.frame == 167) {
            com.sdlpop.game.ExternalStubs.playSound(Snd.SWORD_VS_SWORD)
        }
    }

    fun doDeltaHp() {
        if (gs.Opp.charid == CID.SHADOW && gs.currentLevel == 12 && gs.guardhpDelta.toInt() != 0) {
            gs.hitpDelta = gs.guardhpDelta
        }
        gs.hitpCurr = (gs.hitpCurr + gs.hitpDelta.toInt()).coerceIn(0, gs.hitpMax)
        gs.guardhpCurr = (gs.guardhpCurr + gs.guardhpDelta.toInt()).coerceIn(0, gs.guardhpMax)
    }

    fun checkTheEnd() {
        if (gs.nextRoom != 0 && gs.nextRoom != gs.drawnRoom) {
            gs.drawnRoom = gs.nextRoom
            loadRoomLinks()
            gs.differentRoom = 1
            Seg006.loadkid()
            animTileModif()
            Seg007.startChompers()
            checkFallFlo()
            Seg002.checkShadow()
        }
    }

    fun showTime() {
        if (gs.Kid.alive < 0 &&
            !(gs.fixes.enableFreezeTimeDuringEndMusic != 0 && gs.nextLevel != gs.currentLevel) &&
            gs.remMin.toInt() != 0 &&
            (gs.currentLevel < gs.custom.victoryStopsTimeLevel ||
                (gs.currentLevel == gs.custom.victoryStopsTimeLevel && gs.leveldoorOpen == 0)) &&
            gs.currentLevel < 15
        ) {
            gs.remTick = (gs.remTick - 1) and 0xFFFF
            if (gs.remTick == 0) {
                gs.remTick = 719
                gs.remMin = (gs.remMin - 1).toShort()
                val remMin = gs.remMin.toInt()
                if (remMin != 0 && (remMin <= 5 || remMin % 5 == 0)) {
                    gs.isShowTime = 1
                }
            } else if (gs.remMin.toInt() == 1 && gs.remTick % 12 == 0) {
                gs.isShowTime = 1
                gs.textTimeRemaining = 0
            }
        }

        if (gs.isShowTime != 0 && gs.textTimeRemaining == 0) {
            gs.textTimeRemaining = 24
            gs.textTimeTotal = 24
            if (gs.remMin.toInt() == 1) {
                val remSec = (gs.remTick + 1) / 12
                if (remSec == 1) {
                    gs.textTimeRemaining = 12
                    gs.textTimeTotal = 12
                }
            }
            gs.isShowTime = 0
        }
    }

    private fun getTileAtKid(xpos: Int): Int =
        Seg006.getTile(gs.Kid.room, Seg006.getTileDivModM7(xpos), gs.Kid.currRow)

    internal fun loadRoomLinks() {
        gs.roomBR = 0
        gs.roomBL = 0
        gs.roomAR = 0
        gs.roomAL = 0
        if (gs.drawnRoom != 0) {
            com.sdlpop.game.ExternalStubs.getRoomAddress(gs.drawnRoom)
            val links = gs.level.roomlinks[gs.drawnRoom - 1]
            gs.roomL = links.left
            gs.roomR = links.right
            gs.roomA = links.up
            gs.roomB = links.down
            if (gs.roomA != 0) {
                gs.roomAL = gs.level.roomlinks[gs.roomA - 1].left
                gs.roomAR = gs.level.roomlinks[gs.roomA - 1].right
            } else {
                if (gs.roomL != 0) gs.roomAL = gs.level.roomlinks[gs.roomL - 1].up
                if (gs.roomR != 0) gs.roomAR = gs.level.roomlinks[gs.roomR - 1].up
            }
            if (gs.roomB != 0) {
                gs.roomBL = gs.level.roomlinks[gs.roomB - 1].left
                gs.roomBR = gs.level.roomlinks[gs.roomB - 1].right
            } else {
                if (gs.roomL != 0) gs.roomBL = gs.level.roomlinks[gs.roomL - 1].down
                if (gs.roomR != 0) gs.roomBR = gs.level.roomlinks[gs.roomR - 1].down
            }
        } else {
            gs.roomB = 0
            gs.roomA = 0
            gs.roomR = 0
            gs.roomL = 0
        }
    }

    private fun redrawScreen(drawingDifferentRoom: Boolean) {
        if (drawingDifferentRoom) {
            gs.drectsCount = 0.toShort()
        }
        gs.differentRoom = 0
        loadRoomLinks()
        gs.exitRoomTimer = 2
    }

    private fun isPalaceLevel(): Boolean =
        gs.currentLevel in gs.custom.tblLevelType.indices &&
            gs.custom.tblLevelType[gs.currentLevel] != 0

    private fun genPalaceWallColors() {
        val oldRandomSeed = gs.randomSeed
        gs.randomSeed = gs.drawnRoom.toLong()
        Seg002.prandom(1)
        for (row in 0 until 3) {
            for (subrow in 0 until 4) {
                val colorBase = if (subrow % 2 != 0) 0x61 else 0x66
                var previousColor = -1
                for (column in 0..10) {
                    var color: Int
                    do {
                        color = colorBase + Seg002.prandom(3)
                    } while (color == previousColor)
                    gs.palaceWallColors[44 * row + 11 * subrow + column] = color
                    previousColor = color
                }
            }
        }
        gs.randomSeed = oldRandomSeed
    }

    private fun soundPriority(soundId: Int): Int =
        SOUND_PRIORITY_TABLE.getOrElse(soundId) { Int.MAX_VALUE }

    private fun checkFallFlo() {
        if (gs.currentLevel == gs.custom.looseTilesLevel &&
            (gs.drawnRoom == gs.custom.looseTilesRoom1 || gs.drawnRoom == gs.custom.looseTilesRoom2)
        ) {
            gs.currRoom = gs.roomA.toShort()
            com.sdlpop.game.ExternalStubs.getRoomAddress(gs.currRoom.toInt())
            for (tilepos in gs.custom.looseTilesFirstTile..gs.custom.looseTilesLastTile) {
                gs.currTilepos = tilepos
                Seg007.makeLooseFall(-(Seg002.prandom(0xFF) and 0x0F))
            }
        }
    }

    private fun animTileModif() {
        for (tilepos in 0 until 30) {
            when (Seg007.getCurrTile(tilepos)) {
                T.POTION -> Seg007.startAnimPotion(gs.drawnRoom, tilepos)
                T.TORCH,
                T.TORCH_WITH_DEBRIS -> Seg007.startAnimTorch(gs.drawnRoom, tilepos)
                T.SWORD -> Seg007.startAnimSword(gs.drawnRoom, tilepos)
            }
        }

        for (row in 0..2) {
            when (Seg006.getTile(gs.roomL, 9, row)) {
                T.TORCH,
                T.TORCH_WITH_DEBRIS -> Seg007.startAnimTorch(gs.roomL, row * 10 + 9)
            }
        }
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
