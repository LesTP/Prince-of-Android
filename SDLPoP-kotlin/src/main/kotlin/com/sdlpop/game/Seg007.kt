/*
SDLPoP-kotlin, a Kotlin port of SDLPoP (Prince of Persia).
Based on SDLPoP by David Nagy, licensed under GPL v3+.

Module 12: seg007.c -> Kotlin
Traps, triggers, animated tiles, and loose-floor mobs.

Phase 12a.1: Trob loop scaffold, drawn-room helpers, redraw/wipe helpers,
and basic tile animation helper functions.
Phase 12a.2: Animated-tile state machines (torch/potion/sword/chomper/spike/button/empty),
animation starters, trob lifecycle (add/find), doorlink accessors, trigger plumbing,
start_chompers, make_loose_fall, loose_make_shake, do_knock, is_spike_harmful.
*/

package com.sdlpop.game

import com.sdlpop.game.Tiles as T
import com.sdlpop.game.SoundIds as Snd
import com.sdlpop.game.SoundFlags as SF

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

    // seg007:03CF
    fun animateTorch() {
        // Keep animating torches in the rightmost column of the left-side room as well
        if (gs.trob.room == gs.drawnRoom || (gs.trob.room == gs.roomL && (gs.trob.tilepos % 10) == 9)) {
            gs.currModifier = getTorchFrame(gs.currModifier)
            setRedrawAnimRight()
        } else {
            gs.trob.type = -1
        }
    }

    // seg007:03E9
    fun animatePotion() {
        if (gs.trob.type >= 0 && isTrobInDrawnRoom() != 0) {
            val type = gs.currModifier and 0xF8
            gs.currModifier = bubbleNextFrame(gs.currModifier and 0x07) or type
            // FIX_LOOSE_NEXT_TO_POTION: use redrawAtTrob for wider redraw
            if (gs.fixes.fixLooseLeftOfPotion != 0) {
                redrawAtTrob()
            } else {
                setRedrawAnimCurr()
            }
        }
    }

    // seg007:0425
    fun animateSword() {
        if (isTrobInDrawnRoom() != 0) {
            gs.currModifier--
            if (gs.currModifier == 0) {
                gs.currModifier = (Seg002.prandom(255) and 0x3F) + 0x28
            }
            if (gs.fixes.fixLooseLeftOfPotion != 0) {
                redrawAtTrob()
            } else {
                setRedrawAnimCurr()
            }
        }
    }

    // seg007:0448
    fun animateChomper() {
        if (gs.trob.type >= 0) {
            val blood = gs.currModifier and 0x80
            var frame = (gs.currModifier and 0x7F) + 1
            if (frame > gs.custom.chomperSpeed) {
                frame = 1
            }
            gs.currModifier = blood or frame
            if (frame == 2) {
                ext.playSound(Snd.CHOMPER)
            }
            if ((gs.trob.room != gs.drawnRoom || gs.trob.tilepos / 10 != gs.Kid.currRow ||
                        (gs.Kid.alive >= 0 && blood == 0)) && (gs.currModifier and 0x7F) >= 6
            ) {
                gs.trob.type = -1
            }
        }
        if ((gs.currModifier and 0x7F) < 6) {
            redrawAtTrob()
        }
    }

    // seg007:04D3
    fun animateSpike() {
        if (gs.trob.type >= 0) {
            // 0xFF means a disabled spike
            if (gs.currModifier == 0xFF) return
            if (gs.currModifier and 0x80 != 0) {
                gs.currModifier--
                if (gs.currModifier and 0x7F != 0) return
                gs.currModifier = 6
            } else {
                gs.currModifier++
                if (gs.currModifier == 5) {
                    gs.currModifier = 0x8F
                } else if (gs.currModifier == 9) {
                    gs.currModifier = 0
                    gs.trob.type = -1
                }
            }
        }
        redraw21h()
    }

    // seg007:0D3A
    fun animateButton() {
        if (gs.trob.type >= 0) {
            val timer = getDoorlinkTimer(gs.currModifier) - 1
            setDoorlinkTimer(gs.currModifier, timer)
            if (timer < 2) {
                gs.trob.type = -1
                redraw11h()
            }
        }
    }

    // seg007:0D93
    fun animateEmpty() {
        gs.trob.type = -1
        redraw20h()
    }

    // seg007:081E
    fun startAnimTorch(room: Int, tilepos: Int) {
        gs.currRoomModif[tilepos] = Seg002.prandom(8)
        addTrob(room, tilepos, 1)
    }

    // seg007:0847
    fun startAnimPotion(room: Int, tilepos: Int) {
        gs.currRoomModif[tilepos] = gs.currRoomModif[tilepos] and 0xF8
        gs.currRoomModif[tilepos] = gs.currRoomModif[tilepos] or (Seg002.prandom(6) + 1)
        addTrob(room, tilepos, 1)
    }

    // seg007:087C
    fun startAnimSword(room: Int, tilepos: Int) {
        gs.currRoomModif[tilepos] = Seg002.prandom(0xFF) and 0x1F
        addTrob(room, tilepos, 1)
    }

    // seg007:08A7
    fun startAnimChomper(room: Int, tilepos: Int, modifier: Int) {
        val oldModifier = gs.currRoomModif[tilepos]
        if (oldModifier == 0 || oldModifier >= 6) {
            gs.currRoomModif[tilepos] = modifier
            addTrob(room, tilepos, 1)
        }
    }

    // seg007:08E3
    fun startAnimSpike(room: Int, tilepos: Int) {
        val oldModifier = gs.currRoomModif[tilepos].toByte().toInt() // signed interpretation
        if (oldModifier <= 0) {
            if (oldModifier == 0) {
                addTrob(room, tilepos, 1)
                ext.playSound(Snd.SPIKES)
            } else {
                // 0xFF means a disabled spike
                if (oldModifier != 0xFF.toByte().toInt()) {
                    gs.currRoomModif[tilepos] = 0x8F
                }
            }
        }
    }

    // seg007:0F13
    fun startChompers() {
        var timing = 15
        if (gs.Char.currRow.toInt() and 0xFF < 3) {
            ext.getRoomAddress(gs.Char.room)
            var tilepos = gs.tblLine[gs.Char.currRow]
            for (column in 0 until 10) {
                if (getCurrTile(tilepos) == T.CHOMPER) {
                    val modifier = gs.currModifier and 0x7F
                    if (modifier == 0 || modifier >= 6) {
                        startAnimChomper(gs.Char.room, tilepos, timing or (gs.currModifier and 0x80))
                        timing = nextChomperTiming(timing)
                    }
                }
                tilepos++
            }
        }
    }

    // seg007:0F9A
    fun nextChomperTiming(timing: Int): Int {
        // 15,12,9,6,13,10,7,14,11,8,repeat
        var t = timing - 3
        if (t < 6) {
            t += 10
        }
        return t
    }

    // seg007:0A5A
    fun addTrob(room: Int, tilepos: Int, type: Int) {
        if (gs.trobsCount >= GameConstants.TROBS_MAX) {
            return
        }
        gs.trob.room = room
        gs.trob.tilepos = tilepos
        gs.trob.type = type
        val found = findTrob()
        if (found == -1) {
            // add new
            if (gs.trobsCount.toInt() == GameConstants.TROBS_MAX) return
            copyTrob(gs.trobs[gs.trobsCount.toInt()], gs.trob)
            gs.trobsCount++
        } else {
            // change existing
            gs.trobs[found].type = gs.trob.type
        }
    }

    // seg007:0ACA
    fun findTrob(): Int {
        for (index in 0 until gs.trobsCount.toInt()) {
            if (gs.trobs[index].tilepos == gs.trob.tilepos &&
                gs.trobs[index].room == gs.trob.room
            ) return index
        }
        return -1
    }

    // seg007:0BB6
    fun getDoorlinkTimer(index: Int): Int {
        return gs.level.doorlinks2[index] and 0x1F
    }

    // seg007:0BCD
    fun setDoorlinkTimer(index: Int, value: Int): Int {
        gs.level.doorlinks2[index] = (gs.level.doorlinks2[index] and 0xE0) or (value and 0x1F)
        return gs.level.doorlinks2[index]
    }

    // seg007:0BF2
    fun getDoorlinkTile(index: Int): Int {
        return gs.level.doorlinks1[index] and 0x1F
    }

    // seg007:0C09
    fun getDoorlinkNext(index: Int): Int {
        return if (gs.level.doorlinks1[index] and 0x80 != 0) 0 else 1
    }

    // seg007:0C26
    fun getDoorlinkRoom(index: Int): Int {
        return ((gs.level.doorlinks1[index] and 0x60) shr 5) +
                ((gs.level.doorlinks2[index] and 0xE0) shr 3)
    }

    // seg007:0C53
    fun triggerButton(playsound: Int, buttonType: Int, modifier: Int) {
        var bType = buttonType
        var mod = modifier
        getCurrTile(gs.currTilepos)
        if (bType == 0) {
            bType = gs.currTile
        }
        if (mod == -1) {
            mod = gs.currModifier
        }
        val linkTimer = getDoorlinkTimer(mod)
        // is the event jammed?
        if (linkTimer != 0x1F) {
            setDoorlinkTimer(mod, 5)
            if (linkTimer < 2) {
                addTrob(gs.currRoom.toInt(), gs.currTilepos, 1)
                redraw11h()
                gs.isGuardNotice = 1
                if (playsound != 0) {
                    ext.playSound(Snd.BUTTON_PRESSED)
                }
            }
            doTriggerList(mod, bType)
        }
    }

    // seg007:0999
    fun trigger1(targetType: Int, room: Int, tilepos: Int, buttonType: Int): Int {
        var result = -1
        if (targetType == T.GATE) {
            result = triggerGate(room, tilepos, buttonType)
        } else if (targetType == T.LEVEL_DOOR_LEFT) {
            result = if (gs.currRoomModif[tilepos] != 0) -1 else 1
        } else if (gs.custom.allowTriggeringAnyTile != 0) {
            result = 1
        }
        return result
    }

    // seg007:09E5
    fun doTriggerList(index: Int, buttonType: Int) {
        var idx = index
        while (true) {
            val room = getDoorlinkRoom(idx)
            ext.getRoomAddress(room)
            val tilepos = getDoorlinkTile(idx)
            val targetType = gs.currRoomTiles[tilepos] and 0x1F
            val triggerResult = trigger1(targetType, room, tilepos, buttonType)
            if (triggerResult >= 0) {
                addTrob(room, tilepos, triggerResult)
            }
            if (getDoorlinkNext(idx) == 0) break
            idx++
        }
    }

    // seg007:0922 (trigger_gate) — seg007:092C
    fun triggerGate(room: Int, tilepos: Int, buttonType: Int): Int {
        val modifier = gs.currRoomModif[tilepos]
        if (buttonType == T.OPENER) {
            if (modifier == 0xFF) return -1
            if (modifier >= 188) {
                gs.currRoomModif[tilepos] = 238
                return -1
            }
            gs.currRoomModif[tilepos] = (modifier + 3) and 0xFC
            return 1 // regular open
        } else if (buttonType == T.DEBRIS) {
            if (modifier < 188) return 2 // permanent open
            gs.currRoomModif[tilepos] = 0xFF
            return -1
        } else {
            return if (modifier != 0) 3 else -1 // close fast or already closed
        }
    }

    // seg007:0CD9
    fun diedOnButton() {
        var buttonType = getCurrTile(gs.currTilepos)
        val modifier = gs.currModifier
        if (gs.currTile == T.OPENER) {
            gs.currRoomTiles[gs.currTilepos] = T.FLOOR
            gs.currRoomModif[gs.currTilepos] = 0
            buttonType = T.DEBRIS // force permanent open
        } else {
            gs.currRoomTiles[gs.currTilepos] = T.STUCK
        }
        triggerButton(1, buttonType, modifier)
    }

    // seg007:0D72
    fun startLevelDoor(room: Int, tilepos: Int) {
        gs.currRoomModif[tilepos] = 43 // start fully open
        addTrob(room, tilepos, 3)
    }

    // seg007:1556
    fun isSpikeHarmful(): Int {
        val modifier = gs.currRoomModif[gs.currTilepos].toByte().toInt() // signed
        return if (modifier == 0 || modifier == -1) {
            0
        } else if (modifier < 0) {
            1
        } else if (modifier < 5) {
            2
        } else {
            0
        }
    }

    // seg007:0ED5
    fun makeLooseFall(modifier: Int) {
        // is it a "solid" loose floor?
        if ((gs.currRoomTiles[gs.currTilepos] and 0x20) == 0) {
            if (gs.currRoomModif[gs.currTilepos].toByte().toInt() <= 0) {
                gs.currRoomModif[gs.currTilepos] = modifier
                addTrob(gs.currRoom.toInt(), gs.currTilepos, 0)
                redraw20h()
            }
        }
    }

    // seg007:0FB4
    fun looseMakeShake() {
        // don't shake on level 13
        if (gs.currRoomModif[gs.currTilepos] == 0 && gs.currentLevel != gs.custom.looseTilesLevel) {
            gs.currRoomModif[gs.currTilepos] = 0x80
            addTrob(gs.currRoom.toInt(), gs.currTilepos, 1)
        }
    }

    // seg007:0FE0
    fun doKnock(room: Int, tileRow: Int) {
        for (tileCol in 0 until 10) {
            if (Seg006.getTile(room, tileCol, tileRow) == T.LOOSE) {
                looseMakeShake()
            }
        }
    }

    // seg007:0EB8
    fun removeLoose(room: Int, tilepos: Int): Int {
        gs.currRoomTiles[tilepos] = T.EMPTY
        return gs.custom.tblLevelType[gs.currentLevel]
    }

    // seg007:1010
    fun addMob() {
        if (gs.mobsCount >= 14) {
            return
        }
        val mob = gs.mobs[gs.mobsCount.toInt()]
        mob.xh = gs.curmob.xh
        mob.y = gs.curmob.y
        mob.room = gs.curmob.room
        mob.speed = gs.curmob.speed
        mob.type = gs.curmob.type
        mob.row = gs.curmob.row
        gs.mobsCount++
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
}
