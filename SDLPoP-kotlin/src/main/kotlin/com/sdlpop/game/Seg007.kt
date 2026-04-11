/*
SDLPoP-kotlin, a Kotlin port of SDLPoP (Prince of Persia).
Based on SDLPoP by David Nagy, licensed under GPL v3+.

Module 12: seg007.c -> Kotlin
Traps, triggers, animated tiles, and loose-floor mobs.

Phase 12a.1: Trob loop scaffold, drawn-room helpers, redraw/wipe helpers,
and basic tile animation helper functions.
*/

package com.sdlpop.game

import com.sdlpop.game.Tiles as T

/**
 * seg007 — traps, triggers, animated tiles, and loose-floor mobs.
 *
 * All functions operate on GameState globals, matching the C original's shared
 * mutable state. Later Phase 12a steps fill in animated-tile state machines.
 */
object Seg007 {
    private val gs = GameState
    private val ext = ExternalStubs

    // seg007:0000
    fun processTrobs() {
        var needDelete = 0
        if (gs.trobsCount.toInt() == 0) return

        for (index in 0 until gs.trobsCount.toInt()) {
            copyTrob(gs.trob, gs.trobs[index])
            animateTile()
            gs.trobs[index].type = gs.trob.type
            if (gs.trob.type < 0) {
                needDelete = 1
            }
        }

        if (needDelete != 0) {
            var newIndex = 0
            for (index in 0 until gs.trobsCount.toInt()) {
                if (gs.trobs[index].type >= 0) {
                    copyTrob(gs.trobs[newIndex], gs.trobs[index])
                    newIndex++
                }
            }
            gs.trobsCount = newIndex.toShort()
        }
    }

    // seg007:00AF
    fun animateTile() {
        ext.getRoomAddress(gs.trob.room)
        when (getCurrTile(gs.trob.tilepos)) {
            T.TORCH, T.TORCH_WITH_DEBRIS -> animateTorch()
            T.CLOSER, T.OPENER -> animateButton()
            T.SPIKE -> animateSpike()
            T.LOOSE -> animateLoose()
            T.EMPTY -> animateEmpty()
            T.CHOMPER -> animateChomper()
            T.GATE -> animateDoor()
            T.LEVEL_DOOR_LEFT -> animateLeveldoor()
            T.POTION -> animatePotion()
            T.SWORD -> animateSword()
            else -> gs.trob.type = -1
        }
        gs.currRoomModif[gs.trob.tilepos] = gs.currModifier
    }

    // seg007:0166
    fun isTrobInDrawnRoom(): Int {
        return if (gs.trob.room != gs.drawnRoom) {
            gs.trob.type = -1
            0
        } else {
            1
        }
    }

    // seg007:017E
    fun setRedrawAnimRight() {
        setRedrawAnim(getTrobRightPosInDrawnRoom(), 1)
    }

    // seg007:018C
    fun setRedrawAnimCurr() {
        setRedrawAnim(getTrobPosInDrawnRoom(), 1)
    }

    // seg007:019A
    fun redrawAtTrob() {
        gs.redrawHeight = 63
        val tilepos = getTrobPosInDrawnRoom()
        setRedrawFull(tilepos, 1)
        setWipe(tilepos, 1)
    }

    // seg007:01C5
    fun redraw21h() {
        gs.redrawHeight = 0x21
        redrawTileHeight()
    }

    // seg007:01D0
    fun redraw11h() {
        gs.redrawHeight = 0x11
        redrawTileHeight()
    }

    // seg007:01DB
    fun redraw20h() {
        gs.redrawHeight = 0x20
        redrawTileHeight()
    }

    // seg007:01E6
    fun drawTrob() {
        val tilepos = getTrobRightPosInDrawnRoom()
        setRedrawAnim(tilepos, 1)
        setRedrawFore(tilepos, 1)
        setRedrawAnim(getTrobRightAbovePosInDrawnRoom(), 1)
    }

    // seg007:0218
    fun redrawTileHeight() {
        var tilepos = getTrobPosInDrawnRoom()
        setRedrawFull(tilepos, 1)
        setWipe(tilepos, 1)
        tilepos = getTrobRightPosInDrawnRoom()
        setRedrawFull(tilepos, 1)
        setWipe(tilepos, 1)
    }

    // seg007:0258
    fun getTrobPosInDrawnRoom(): Int {
        var tilepos = gs.trob.tilepos
        if (gs.trob.room == gs.roomA) {
            tilepos = if (tilepos >= 20 && tilepos < 30) {
                19 - tilepos
            } else {
                30
            }
        } else if (gs.trob.room != gs.drawnRoom) {
            tilepos = 30
        }
        return tilepos
    }

    // seg007:029D
    fun getTrobRightPosInDrawnRoom(): Int {
        var tilepos = gs.trob.tilepos
        if (gs.trob.room == gs.drawnRoom) {
            tilepos = if (tilepos % 10 != 9) tilepos + 1 else 30
        } else if (gs.trob.room == gs.roomL) {
            tilepos = if (tilepos % 10 == 9) tilepos - 9 else 30
        } else if (gs.trob.room == gs.roomA) {
            tilepos = if (tilepos >= 20 && tilepos < 29) 18 - tilepos else 30
        } else if (gs.trob.room == gs.roomAL && tilepos == 29) {
            tilepos = -1
        } else {
            tilepos = 30
        }
        return tilepos
    }

    // seg007:032C
    fun getTrobRightAbovePosInDrawnRoom(): Int {
        var tilepos = gs.trob.tilepos
        if (gs.trob.room == gs.drawnRoom) {
            tilepos = if (tilepos % 10 != 9) {
                if (tilepos < 10) -(tilepos + 2) else tilepos - 9
            } else {
                30
            }
        } else if (gs.trob.room == gs.roomL) {
            tilepos = if (tilepos == 9) {
                -1
            } else if (tilepos % 10 == 9) {
                tilepos - 19
            } else {
                30
            }
        } else if (gs.trob.room == gs.roomB) {
            tilepos = if (tilepos < 9) tilepos + 21 else 30
        } else if (gs.trob.room == gs.roomBL && tilepos == 9) {
            tilepos = 20
        } else {
            tilepos = 30
        }
        return tilepos
    }

    // seg007:06AD
    fun bubbleNextFrame(curr: Int): Int {
        var next = curr + 1
        if (next >= 8) next = 1
        return next
    }

    // seg007:06CD
    fun getTorchFrame(curr: Int): Int {
        var next = Seg002.prandom(255)
        if (next != curr) {
            if (next < 9) {
                return next
            } else {
                next = curr
            }
        }
        next++
        if (next >= 9) next = 0
        return next
    }

    // seg007:070A
    fun setRedrawAnim(tilepos: Int, frames: Int) {
        if (tilepos < 30) {
            if (tilepos < 0) {
                gs.redrawFramesAbove[-(tilepos + 1)] = frames
            } else {
                gs.redrawFramesAnim[tilepos] = frames
            }
        }
    }

    // seg007:0738
    fun setRedraw2(tilepos: Int, frames: Int) {
        if (tilepos < 30) {
            if (tilepos < 0) {
                var aboveTilepos = -tilepos - 1
                if (aboveTilepos > 9) aboveTilepos = 9
                gs.redrawFramesAbove[aboveTilepos] = frames
            } else {
                gs.redrawFrames2[tilepos] = frames
            }
        }
    }

    // seg007:0766
    fun setRedrawFloorOverlay(tilepos: Int, frames: Int) {
        if (tilepos < 30) {
            if (tilepos < 0) {
                gs.redrawFramesAbove[-(tilepos + 1)] = frames
            } else {
                gs.redrawFramesFloorOverlay[tilepos] = frames
            }
        }
    }

    // seg007:0794
    fun setRedrawFull(tilepos: Int, frames: Int) {
        if (tilepos < 30) {
            if (tilepos < 0) {
                gs.redrawFramesAbove[-(tilepos + 1)] = frames
            } else {
                gs.redrawFramesFull[tilepos] = frames
            }
        }
    }

    // seg007:07C2
    fun setRedrawFore(tilepos: Int, frames: Int) {
        if (tilepos < 30 && tilepos >= 0) {
            gs.redrawFramesFore[tilepos] = frames
        }
    }

    // seg007:07DF
    fun setWipe(tilepos: Int, frames: Int) {
        if (tilepos < 30 && tilepos >= 0) {
            if (gs.wipeFrames[tilepos] != 0) {
                gs.redrawHeight = maxOf(gs.wipeHeights[tilepos], gs.redrawHeight.toInt()).toShort()
            }
            gs.wipeHeights[tilepos] = gs.redrawHeight.toInt()
            gs.wipeFrames[tilepos] = frames
        }
    }

    // seg007:0B0A
    fun clearTileWipes() {
        gs.redrawFramesFull.fill(0)
        gs.wipeFrames.fill(0)
        gs.wipeHeights.fill(0)
        gs.redrawFramesAnim.fill(0)
        gs.redrawFramesFore.fill(0)
        gs.redrawFrames2.fill(0)
        gs.redrawFramesFloorOverlay.fill(0)
        gs.tileObjectRedraw.fill(0)
        gs.redrawFramesAbove.fill(0)
    }

    // seg007:1041
    fun getCurrTile(tilepos: Int): Int {
        gs.currModifier = gs.currRoomModif[tilepos]
        gs.currTile = gs.currRoomTiles[tilepos] and 0x1F
        return gs.currTile
    }

    private fun copyTrob(target: TrobType, source: TrobType) {
        target.tilepos = source.tilepos
        target.room = source.room
        target.type = source.type
    }

    fun animateTorch() {
        throw NotImplementedError("animate_torch (seg007 Phase 12a.2)")
    }

    fun animatePotion() {
        throw NotImplementedError("animate_potion (seg007 Phase 12a.2)")
    }

    fun animateSword() {
        throw NotImplementedError("animate_sword (seg007 Phase 12a.2)")
    }

    fun animateChomper() {
        throw NotImplementedError("animate_chomper (seg007 Phase 12a.2)")
    }

    fun animateSpike() {
        throw NotImplementedError("animate_spike (seg007 Phase 12a.2)")
    }

    fun animateButton() {
        throw NotImplementedError("animate_button (seg007 Phase 12a.2)")
    }

    fun animateDoor() {
        throw NotImplementedError("animate_door (seg007 Phase 12a.3)")
    }

    fun animateLeveldoor() {
        throw NotImplementedError("animate_leveldoor (seg007 Phase 12a.3)")
    }

    fun animateLoose() {
        throw NotImplementedError("animate_loose (seg007 later phase)")
    }

    fun animateEmpty() {
        throw NotImplementedError("animate_empty (seg007 later phase)")
    }
}
