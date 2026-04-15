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
Phase 12a.3: Gate/leveldoor animation (animate_door, gate_stop, animate_leveldoor,
play_door_sound_if_visible) — completes the animate_tile dispatch for gate and level door tiles.
Phase 12b.1: Loose-floor animation entry point (animate_loose, loose_shake).
Phase 12b.3: Loose-floor mob draw/object-table bookkeeping.
*/

package com.sdlpop.game

import com.sdlpop.game.Tiles as T
import com.sdlpop.game.SoundIds as Snd
import com.sdlpop.game.SoundFlags as SF
import com.sdlpop.game.FrameIds as FID
import com.sdlpop.game.Actions as Act
import com.sdlpop.game.SeqIds as Seq
import com.sdlpop.game.Chtabs as Chtab

/**
 * seg007 — traps, triggers, animated tiles, and loose-floor mobs.
 *
 * All functions operate on GameState globals, matching the C original's shared
 * mutable state. Later Phase 12a steps fill in animated-tile state machines.
 */
object Seg007 {
    private val gs = GameState
    private val ext = ExternalStubs
    private var curmobIndex = 0
    private var currTileTemp = 0

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
                gs.currRoomModif[gs.currTilepos] = modifier and 0xFF
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
        writeRoomTile(room, tilepos, T.EMPTY)
        return gs.custom.tblLevelType[gs.currentLevel]
    }

    private fun writeRoomTile(room: Int, tilepos: Int, tile: Int) {
        gs.currRoomTiles[tilepos] = tile
        if (room > 0) {
            gs.level.fg[(room - 1) * 30 + tilepos] = tile
        }
    }

    private fun writeRoomModifier(room: Int, tilepos: Int, modifier: Int) {
        gs.currRoomModif[tilepos] = modifier
        if (room > 0) {
            gs.level.bg[(room - 1) * 30 + tilepos] = modifier
        }
    }

    // seg007:1010
    fun addMob() {
        if (gs.mobsCount >= 14) {
            return
        }
        copyMob(gs.mobs[gs.mobsCount.toInt()], gs.curmob)
        gs.mobsCount++
    }

    private fun copyMob(dst: MobType, src: MobType) {
        dst.xh = src.xh
        dst.y = src.y
        dst.room = src.room
        dst.speed = src.speed
        dst.type = src.type
        dst.row = src.row
    }

    // seg007:1063
    fun doMobs() {
        val nMobs = gs.mobsCount.toInt()
        curmobIndex = 0
        while (curmobIndex < nMobs) {
            copyMob(gs.curmob, gs.mobs[curmobIndex])
            moveMob()
            checkLooseFallOnKid()
            copyMob(gs.mobs[curmobIndex], gs.curmob)
            curmobIndex++
        }

        var newIndex = 0
        for (index in 0 until gs.mobsCount.toInt()) {
            if (gs.mobs[index].speed != -1) {
                copyMob(gs.mobs[newIndex], gs.mobs[index])
                newIndex++
            }
        }
        gs.mobsCount = newIndex.toShort()
    }

    // seg007:110F
    fun moveMob() {
        if (gs.curmob.type == 0) {
            moveLoose()
        }
        if (gs.curmob.speed <= 0) {
            gs.curmob.speed = (gs.curmob.speed + 1).toByte().toInt()
        }
    }

    // data:227A
    private val ySomething = intArrayOf(-1, 62, 125, 188, 25)

    // seg007:1126
    fun moveLoose() {
        if (gs.curmob.speed < 0) return
        if (gs.curmob.speed < 29) {
            gs.curmob.speed = (gs.curmob.speed + 3).toByte().toInt()
        }
        gs.curmob.y = (gs.curmob.y + gs.curmob.speed) and 0xFF
        if (gs.curmob.room == 0) {
            if (gs.curmob.y < 210) {
                return
            } else {
                gs.curmob.speed = -2
                return
            }
        }
        if (gs.curmob.y < 226 && ySomething[gs.curmob.row + 1] <= gs.curmob.y) {
            currTileTemp = Seg006.getTile(gs.curmob.room, gs.curmob.xh shr 2, gs.curmob.row)
            if (currTileTemp == T.LOOSE) {
                looseFall()
            }
            if (currTileTemp == T.EMPTY || currTileTemp == T.LOOSE) {
                mobDownARow()
                return
            }
            ext.playSound(Snd.TILE_CRASHING)
            doKnock(gs.curmob.room, gs.curmob.row)
            gs.curmob.y = ySomething[gs.curmob.row + 1] and 0xFF
            gs.curmob.speed = -2
            looseLand()
        }
    }

    // seg007:11E8
    fun looseLand() {
        var buttonType = 0
        var tiletype = Seg006.getTile(gs.curmob.room, gs.curmob.xh shr 2, gs.curmob.row)
        when (tiletype) {
            T.OPENER -> {
                writeRoomTile(gs.currRoom.toInt(), gs.currTilepos, T.DEBRIS)
                buttonType = T.DEBRIS
                triggerButton(1, buttonType, -1)
                tiletype = Seg006.getTile(gs.curmob.room, gs.curmob.xh shr 2, gs.curmob.row)
                landLooseOnTile(tiletype)
            }
            T.CLOSER -> {
                triggerButton(1, buttonType, -1)
                tiletype = Seg006.getTile(gs.curmob.room, gs.curmob.xh shr 2, gs.curmob.row)
                landLooseOnTile(tiletype)
            }
            else -> landLooseOnTile(tiletype)
        }
    }

    private fun landLooseOnTile(tiletype: Int) {
        when (tiletype) {
            T.FLOOR, T.SPIKE, T.POTION, T.TORCH, T.TORCH_WITH_DEBRIS -> {
                val debrisTile = if (tiletype == T.TORCH || tiletype == T.TORCH_WITH_DEBRIS) {
                    T.TORCH_WITH_DEBRIS
                } else {
                    T.DEBRIS
                }
                writeRoomTile(gs.currRoom.toInt(), gs.currTilepos, debrisTile)
                redrawAtCurMob()
                if (gs.tileCol.toInt() != 0) {
                    setRedrawFull(gs.currTilepos - 1, 1)
                }
            }
        }
    }

    // seg007:12CB
    fun looseFall() {
        writeRoomModifier(gs.currRoom.toInt(), gs.currTilepos, removeLoose(gs.currRoom.toInt(), gs.currTilepos))
        gs.curmob.speed = (gs.curmob.speed shr 1).toByte().toInt()
        copyMob(gs.mobs[curmobIndex], gs.curmob)
        gs.curmob.y = (gs.curmob.y + 6) and 0xFF
        mobDownARow()
        addMob()
        copyMob(gs.curmob, gs.mobs[curmobIndex])
        redrawAtCurMob()
    }

    // seg007:132C
    fun redrawAtCurMob() {
        if (gs.curmob.room == gs.drawnRoom) {
            gs.redrawHeight = 0x20
            setRedrawFull(gs.currTilepos, 1)
            setWipe(gs.currTilepos, 1)
            if ((gs.currTilepos % 10) + 1 < 10) {
                setRedrawFull(gs.currTilepos + 1, 1)
                setWipe(gs.currTilepos + 1, 1)
            }
        }
    }

    // seg007:1387
    fun mobDownARow() {
        gs.curmob.row++
        if (gs.curmob.row >= 3) {
            gs.curmob.y = (gs.curmob.y - 192) and 0xFF
            gs.curmob.row = 0
            gs.curmob.room = if (gs.curmob.room != 0) {
                gs.level.roomlinks[gs.curmob.room - 1].down
            } else {
                0
            }
        }
    }

    // seg007:13AE
    fun drawMobs() {
        for (index in 0 until gs.mobsCount.toInt()) {
            copyMob(gs.curmob, gs.mobs[index])
            drawMob()
        }
    }

    // seg007:13E5
    fun drawMob() {
        var ypos = gs.curmob.y
        if (gs.curmob.room == gs.drawnRoom) {
            if (gs.curmob.y >= 210) return
        } else if (gs.curmob.room == gs.roomB) {
            if (kotlin.math.abs(ypos.toByte().toInt()) >= 18) return
            gs.curmob.y = (gs.curmob.y + 192) and 0xFF
            ypos = gs.curmob.y
        } else if (gs.curmob.room == gs.roomA) {
            if (gs.curmob.y < 174) return
            ypos = gs.curmob.y - 189
        } else {
            return
        }

        var tileCol = gs.curmob.xh shr 2
        val tileRow = Seg006.yToRowMod4(ypos)
        gs.objTilepos = Seg006.getTileposNominus(tileCol, tileRow)
        tileCol++
        var tilepos = Seg006.getTilepos(tileCol, tileRow)
        setRedraw2(tilepos, 1)
        setRedrawFore(tilepos, 1)

        val topRow = Seg006.yToRowMod4(ypos - 18)
        if (topRow != tileRow) {
            tilepos = Seg006.getTilepos(tileCol, topRow)
            setRedraw2(tilepos, 1)
            setRedrawFore(tilepos, 1)
        }

        addMobToObjtable(ypos)
    }

    // seg007:14DE
    fun addMobToObjtable(ypos: Int) {
        val index = gs.tableCounts[4].toInt()
        gs.tableCounts[4] = (index + 1).toShort()
        if (index !in gs.objtable.indices) return

        val currObj = gs.objtable[index]
        currObj.objType = gs.curmob.type or 0x80
        currObj.xh = gs.curmob.xh
        currObj.xl = 0
        currObj.y = ypos.toShort()
        currObj.chtabId = Chtab.ENVIRONMENT
        currObj.id = 10
        currObj.clip.top = 0.toShort()
        currObj.clip.left = 0.toShort()
        currObj.clip.right = 40.toShort()
        markObjTileRedraw(index)
    }

    private fun markObjTileRedraw(index: Int) {
        gs.objtable[index].tilepos = gs.objTilepos
        if (gs.objTilepos < 30) {
            gs.tileObjectRedraw[gs.objTilepos] = 1
        }
    }

    // seg007:1591
    fun checkLooseFallOnKid() {
        Seg006.loadkid()
        if (
            gs.Char.room == gs.curmob.room &&
            gs.Char.currCol == (gs.curmob.xh shr 2) &&
            gs.curmob.y < gs.Char.y &&
            gs.Char.y - 30 < gs.curmob.y
        ) {
            fellOnYourHead()
            Seg006.savekid()
        }
    }

    // seg007:15D3
    fun fellOnYourHead() {
        val frame = gs.Char.frame
        val action = gs.Char.action
        if (
            (gs.currentLevel == gs.custom.looseTilesLevel || (frame < FID.frame_5_start_run || frame >= 15)) &&
            (action < Act.HANG_CLIMB || action == Act.TURN)
        ) {
            gs.Char.y = gs.yLand[gs.Char.currRow + 1].toInt()
            if (Seg006.takeHp(1) != 0) {
                Seg005.seqtblOffsetChar(Seq.seq_22_crushed)
                if (frame == FID.frame_177_spiked) {
                    gs.Char.x = Seg006.charDxForward(-12)
                }
            } else {
                if (frame != FID.frame_109_crouch) {
                    if (Seg006.getTileBehindChar() == 0) {
                        gs.Char.x = Seg006.charDxForward(-2)
                    }
                    Seg005.seqtblOffsetChar(Seq.seq_52_loose_floor_fell_on_kid)
                }
            }
        }
    }

    // data:2284
    private val yLooseLand = intArrayOf(2, 65, 128, 191, 254)

    // data:2734
    private val looseSound = intArrayOf(0, 1, 1, 1, 0, 1, 0, 0, 1, 0, 0, 0)

    // data:27C0
    private val doorDelta = intArrayOf(-1, 4, 4)
    // data:27BC
    private val gateCloseSpeeds = intArrayOf(0, 0, 0, 20, 40, 60, 80, 100, 120)
    // data:27B8
    private val leveldoorCloseSpeeds = intArrayOf(0, 5, 17, 99, 0)

    // seg007:0522
    fun animateDoor() {
        /*
        Possible values of anim_type:
        0: closing
        1: open
        2: permanent open
        3,4,5,6,7,8: fast closing with speeds 20,40,60,80,100,120 /4 pixel/frame
        */
        var animType = gs.trob.type
        if (animType >= 0) {
            if (animType >= 3) {
                // closing fast
                if (animType < 8) {
                    animType++
                    gs.trob.type = animType
                }
                val newMod = gs.currModifier - gateCloseSpeeds[animType]
                gs.currModifier = newMod
                if (newMod < 0) {
                    gs.currModifier = 0
                    gs.trob.type = -1
                    ext.playSound(Snd.GATE_CLOSING_FAST)
                }
            } else {
                if (gs.currModifier != 0xFF) {
                    // 0xFF means permanently open.
                    gs.currModifier += doorDelta[animType]
                    if (animType == 0) {
                        // closing
                        if (gs.currModifier != 0) {
                            if (gs.currModifier < 188) {
                                if ((gs.currModifier and 3) == 3) {
                                    playDoorSoundIfVisible(Snd.GATE_CLOSING)
                                }
                            }
                        } else {
                            gateStop()
                        }
                    } else {
                        // opening
                        if (gs.currModifier < 188) {
                            if ((gs.currModifier and 7) == 0) {
                                ext.playSound(Snd.GATE_OPENING)
                            }
                        } else {
                            // stop
                            if (animType < 2) {
                                // after regular open
                                gs.currModifier = 238
                                gs.trob.type = 0 // closing
                                ext.playSound(Snd.GATE_STOP)
                            } else {
                                // after permanent open
                                gs.currModifier = 0xFF // keep open
                                gateStop()
                            }
                        }
                    }
                } else {
                    gateStop()
                }
            }
        }
        drawTrob()
    }

    // seg007:05E3
    fun gateStop() {
        gs.trob.type = -1
        playDoorSoundIfVisible(Snd.GATE_STOP)
    }

    // seg007:05F1
    fun animateLeveldoor() {
        /*
        Possible values of trob_type:
        0: open
        1: open (with button)
        2: open
        3,4,5,6: fast closing with speeds 0,5,17,99 pixel/frame
        */
        val trobType = gs.trob.type
        if (gs.trob.type >= 0) {
            if (trobType >= 3) {
                // closing
                gs.trob.type++
                gs.currModifier -= leveldoorCloseSpeeds[gs.trob.type - 3]
                if (gs.currModifier.toByte().toInt() < 0) {
                    gs.currModifier = 0
                    gs.trob.type = -1
                    ext.playSound(Snd.LEVELDOOR_CLOSING)
                } else {
                    if (gs.trob.type == 4 &&
                        (gs.soundFlags and SF.DIGI) != 0
                    ) {
                        gs.soundInterruptible[Snd.LEVELDOOR_SLIDING] = 1
                        ext.playSound(Snd.LEVELDOOR_SLIDING)
                    }
                }
            } else {
                // opening
                gs.currModifier++
                if (gs.currModifier >= 43) {
                    gs.trob.type = -1
                    if (!(gs.fixes.fixFeatherInterruptedByLeveldoor != 0 && gs.isFeatherFall != 0)) {
                        ext.stopSounds()
                    }
                    if (gs.leveldoorOpen == 0 || gs.leveldoorOpen == 2) {
                        gs.leveldoorOpen = 1
                        if (gs.currentLevel == gs.custom.mirrorLevel) {
                            // Special event: place mirror
                            Seg006.getTile(gs.custom.mirrorRoom, gs.custom.mirrorColumn, gs.custom.mirrorRow)
                            gs.currRoomTiles[gs.currTilepos] = gs.custom.mirrorTile
                        }
                    }
                } else {
                    gs.soundInterruptible[Snd.LEVELDOOR_SLIDING] = 0
                    ext.playSound(Snd.LEVELDOOR_SLIDING)
                }
            }
        }
        setRedrawAnimRight()
    }

    // seg007:1669
    fun playDoorSoundIfVisible(soundId: Int) {
        val tilepos = gs.trob.tilepos
        val gateRoom = gs.trob.room
        var hasSound = 0

        val hasSoundCondition = if (gs.fixes.fixGateSounds != 0) {
            (gateRoom == gs.roomL && tilepos % 10 == 9) ||
                    (gateRoom == gs.drawnRoom && tilepos % 10 != 9)
        } else {
            if (gateRoom == gs.roomL) tilepos % 10 == 9
            else (gateRoom == gs.drawnRoom && tilepos % 10 != 9)
        }

        // Special event: sound of closing gates
        if ((gs.currentLevel == 3 && gateRoom == 2) || hasSoundCondition) {
            hasSound = 1
        }
        if (hasSound != 0) {
            ext.playSound(soundId)
        }
    }

    // seg007:0D9D
    fun animateLoose() {
        val animType = gs.trob.type
        if (animType >= 0) {
            gs.currModifier = (gs.currModifier + 1) and 0xFF
            if ((gs.currModifier and 0x80) != 0) {
                // Just shaking. Level 13 uses this path for auto-falling floors.
                if (gs.currentLevel == gs.custom.looseTilesLevel) return
                if (gs.currModifier >= 0x84) {
                    gs.currModifier = 0
                    gs.trob.type = -1
                }
                looseShake(if (gs.currModifier == 0) 1 else 0)
            } else {
                // Something is on the floor; spawn a falling mob once the delay expires.
                if (gs.currModifier >= gs.custom.looseFloorDelay) {
                    val room = gs.trob.room
                    val tilepos = gs.trob.tilepos
                    val skipDropForClimbingKid =
                        gs.fixes.fixDrop2RoomsClimbingLooseTile != 0 &&
                            room == gs.level.roomlinks[gs.Kid.room - 1].up &&
                            tilepos / 10 == 2 &&
                            gs.Kid.currRow == 0 &&
                            gs.Kid.currCol == tilepos % 10 &&
                            gs.Kid.frame >= FID.frame_135_climbing_1 &&
                            gs.Kid.frame < FID.frame_141_climbing_7

                    if (skipDropForClimbingKid) {
                        looseShake(0)
                    } else {
                        gs.currModifier = removeLoose(room, tilepos)
                        gs.trob.type = -1
                        val row = tilepos / 10
                        gs.curmob.xh = (tilepos % 10) shl 2
                        gs.curmob.y = yLooseLand[row + 1]
                        gs.curmob.room = room
                        gs.curmob.speed = 0
                        gs.curmob.type = 0
                        gs.curmob.row = row
                        addMob()
                    }
                } else {
                    looseShake(0)
                }
            }
        }
        redraw20h()
    }

    // seg007:0E55
    fun looseShake(arg0: Int) {
        val modifier = gs.currModifier and 0x7F
        if (arg0 != 0 || (modifier < looseSound.size && looseSound[modifier] != 0)) {
            var soundId: Int
            do {
                soundId = Seg002.prandom(2) + Snd.LOOSE_SHAKE_1
            } while (soundId == gs.lastLooseSound)

            if (!(gs.replaying != 0 && gs.gDeprecationNumber < 2)) {
                Seg002.prandom(2) // DOS compatibility: waste one RNG cycle.
            }

            if ((gs.soundFlags and SF.DIGI) != 0) {
                gs.lastLooseSound = soundId
            }
            ext.playSound(soundId)
        }
    }
}
