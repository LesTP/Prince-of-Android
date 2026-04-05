/*
SDLPoP-kotlin, a Kotlin port of SDLPoP (Prince of Persia).
Based on SDLPoP by Dávid Nagy, licensed under GPL v3+.

Module 11: seg002.c → Kotlin
Guard AI, room transitions, sword combat detection, special level events.
52 functions, ~1,237 lines of C.

Phase 11a: Guard init, room management, special events, move helpers (~19 functions).
*/

package com.sdlpop.game

import com.sdlpop.game.Tiles as T
import com.sdlpop.game.Directions as Dir
import com.sdlpop.game.Actions as Act
import com.sdlpop.game.FrameIds as FID
import com.sdlpop.game.SeqIds as Seq
import com.sdlpop.game.SoundIds as Snd
import com.sdlpop.game.CharIds as CID
import com.sdlpop.game.SwordStatus as Sword
import com.sdlpop.game.Control as Ctrl
import com.sdlpop.game.TileGeometry as TG

/**
 * seg002: Guard AI, room transitions, sword combat, special events.
 * All functions operate on [GameState] (aliased as `gs`).
 */
object Seg002 {
    private val gs = GameState
    private val seg006 = Seg006
    private val seg004 = Seg004
    private val seg005 = Seg005
    private val stubs = ExternalStubs

    // ========== Move helpers (seg002:0706 – seg002:0776) ==========

    /** seg002:0706 — Release all controls. */
    fun move0Nothing() {
        gs.controlShift = Ctrl.RELEASED
        gs.controlY = Ctrl.RELEASED
        gs.controlX = Ctrl.RELEASED
        gs.controlShift2 = Ctrl.RELEASED
        gs.controlDown = Ctrl.RELEASED
        gs.controlUp = Ctrl.RELEASED
        gs.controlBackward = Ctrl.RELEASED
        gs.controlForward = Ctrl.RELEASED
    }

    /** seg002:0721 — Move forward. */
    fun move1Forward() {
        gs.controlX = Ctrl.HELD_FORWARD
        gs.controlForward = Ctrl.HELD
    }

    /** seg002:072A — Move backward. */
    fun move2Backward() {
        gs.controlBackward = Ctrl.HELD
        gs.controlX = Ctrl.HELD_BACKWARD
    }

    /** seg002:0735 — Move up. */
    fun move3Up() {
        gs.controlY = Ctrl.HELD_UP
        gs.controlUp = Ctrl.HELD
    }

    /** seg002:073E — Move down. */
    fun move4Down() {
        gs.controlDown = Ctrl.HELD
        gs.controlY = Ctrl.HELD_DOWN
    }

    /** seg002:0749 — Move up+backward. */
    fun moveUpBack() {
        gs.controlUp = Ctrl.HELD
        move2Backward()
    }

    /** seg002:0753 — Move down+backward. */
    fun moveDownBack() {
        gs.controlDown = Ctrl.HELD
        move2Backward()
    }

    /** seg002:075D — Move down+forward. */
    fun moveDownForw() {
        gs.controlDown = Ctrl.HELD
        move1Forward()
    }

    /** seg002:0767 — Shift (attack). */
    fun move6Shift() {
        gs.controlShift = Ctrl.HELD
        gs.controlShift2 = Ctrl.HELD
    }

    /** seg002:0770 — Release shift. */
    fun move7() {
        gs.controlShift = Ctrl.RELEASED
    }

    // ========== Guard init and state management ==========

    /** seg002:0000 — Initialize shadow character from source data. */
    fun doInitShad(source: IntArray, seqIndex: Int) {
        // memcpy(&Char, source, 7) — copy first 7 bytes of char_type fields
        gs.Char.frame = source[0]
        gs.Char.x = source[1]
        gs.Char.y = source[2]
        gs.Char.direction = source[3]
        gs.Char.currCol = source[4]
        gs.Char.currRow = source[5]
        gs.Char.action = source[6]
        seg005.seqtblOffsetChar(seqIndex)
        gs.Char.charid = CID.SHADOW
        gs.demoTime = 0
        gs.guardSkill = 3
        gs.guardhpDelta = 4.toShort()
        gs.guardhpCurr = 4
        gs.guardhpMax = 4
        seg006.saveshad()
    }

    /** seg002:0044 — Set guard hit points from level tables. */
    fun getGuardHp() {
        val hp = gs.custom.extrastrength[gs.guardSkill] + gs.custom.tblGuardHp[gs.currentLevel]
        gs.guardhpDelta = hp.toShort()
        gs.guardhpCurr = hp
        gs.guardhpMax = hp
    }

    /** seg002:0064 — Check for shadow special events, or enter regular guard. */
    fun checkShadow() {
        gs.offguard = 0
        if (gs.currentLevel == 12) {
            if (gs.unitedWithShadow.toInt() == 0 && gs.drawnRoom == 15) {
                gs.Char.room = gs.drawnRoom
                if (seg006.getTile(15, 1, 0) == T.SWORD) {
                    return
                }
                gs.shadowInitialized = 0
                doInitShad(gs.custom.initShad12, 7 /* fall */)
                return
            }
        }
        if (gs.currentLevel == gs.custom.shadowStepLevel) {
            gs.Char.room = gs.drawnRoom
            if (gs.Char.room == gs.custom.shadowStepRoom) {
                if (gs.leveldoorOpen != 0x4D) {
                    stubs.playSound(Snd.PRESENTATION)
                    gs.leveldoorOpen = 0x4D
                }
                doInitShad(gs.custom.initShad6, 2 /* stand */)
                return
            }
        }
        if (gs.currentLevel == gs.custom.shadowStealLevel) {
            gs.Char.room = gs.drawnRoom
            if (gs.Char.room == gs.custom.shadowStealRoom) {
                if (seg006.getTile(gs.custom.shadowStealRoom, 3, 0) != T.POTION) {
                    return
                }
                doInitShad(gs.custom.initShad5, 2 /* stand */)
                return
            }
        }
        enterGuard()
    }

    /** seg002:0112 — Load guard from level data for current drawn room. */
    fun enterGuard() {
        val roomMinus1 = gs.drawnRoom - 1
        var guardTile = gs.level.guardsTile[roomMinus1]

        // FIX_OFFSCREEN_GUARDS_DISAPPEARING
        if (guardTile >= 30) {
            if (gs.fixes.fixOffscreenGuardsDisappearing == 0) return

            var leftGuardTile: Int = 31
            var rightGuardTile: Int = 31
            if (gs.roomL > 0) leftGuardTile = gs.level.guardsTile[gs.roomL - 1]
            if (gs.roomR > 0) rightGuardTile = gs.level.guardsTile[gs.roomR - 1]

            var otherGuardX: Int
            var otherGuardDir: Int
            var deltaX: Int
            var otherRoomMinus1: Int

            if (rightGuardTile in 0 until 30) {
                otherRoomMinus1 = gs.roomR - 1
                otherGuardX = gs.level.guardsX[otherRoomMinus1]
                otherGuardDir = gs.level.guardsDir[otherRoomMinus1]
                if (otherGuardDir == Dir.RIGHT) otherGuardX -= 9
                if (otherGuardDir == Dir.LEFT) otherGuardX += 1
                if (otherGuardX >= 58 + 4) {
                    // check the left offscreen guard
                    if (leftGuardTile in 0 until 30) {
                        // goto loc_left_guard_tile — fall through to left guard handling below
                    } else {
                        return
                    }
                    // left guard handling (loc_left_guard_tile)
                    otherRoomMinus1 = gs.roomL - 1
                    otherGuardX = gs.level.guardsX[otherRoomMinus1]
                    otherGuardDir = gs.level.guardsDir[otherRoomMinus1]
                    if (otherGuardDir == Dir.RIGHT) otherGuardX -= 9
                    if (otherGuardDir == Dir.LEFT) otherGuardX += 1
                    if (otherGuardX <= 190 - 4) return
                    deltaX = -140
                    guardTile = leftGuardTile
                } else {
                    deltaX = 140
                    guardTile = rightGuardTile
                }
            } else if (leftGuardTile in 0 until 30) {
                // loc_left_guard_tile
                otherRoomMinus1 = gs.roomL - 1
                otherGuardX = gs.level.guardsX[otherRoomMinus1]
                otherGuardDir = gs.level.guardsDir[otherRoomMinus1]
                if (otherGuardDir == Dir.RIGHT) otherGuardX -= 9
                if (otherGuardDir == Dir.LEFT) otherGuardX += 1
                if (otherGuardX <= 190 - 4) return
                deltaX = -140
                guardTile = leftGuardTile
            } else {
                return
            }

            // retrieve guard from adjacent room
            gs.level.guardsX[roomMinus1] = gs.level.guardsX[otherRoomMinus1] + deltaX
            gs.level.guardsColor[roomMinus1] = gs.level.guardsColor[otherRoomMinus1]
            gs.level.guardsDir[roomMinus1] = gs.level.guardsDir[otherRoomMinus1]
            gs.level.guardsSeqHi[roomMinus1] = gs.level.guardsSeqHi[otherRoomMinus1]
            gs.level.guardsSeqLo[roomMinus1] = gs.level.guardsSeqLo[otherRoomMinus1]
            gs.level.guardsSkill[roomMinus1] = gs.level.guardsSkill[otherRoomMinus1]

            gs.level.guardsTile[otherRoomMinus1] = 0xFF
            gs.level.guardsSeqHi[otherRoomMinus1] = 0
        }

        gs.Char.room = gs.drawnRoom
        gs.Char.currRow = guardTile / TG.SCREEN_TILECOUNTX
        gs.Char.y = gs.yLand[gs.Char.currRow + 1].toInt()
        gs.Char.x = gs.level.guardsX[roomMinus1]
        gs.Char.currCol = seg006.getTileDivModM7(gs.Char.x)
        gs.Char.direction = gs.level.guardsDir[roomMinus1]

        // Only regular guards have different colors (VGA only)
        if (gs.graphicsMode == 4 && gs.custom.tblGuardType[gs.currentLevel] == 0) {
            gs.currGuardColor = gs.level.guardsColor[roomMinus1]
        } else {
            gs.currGuardColor = 0
        }

        // REMEMBER_GUARD_HP
        val rememberedHp = (gs.level.guardsColor[roomMinus1] and 0xF0) shr 4

        gs.currGuardColor = gs.currGuardColor and 0x0F

        if (gs.custom.tblGuardType[gs.currentLevel] == 2) {
            gs.Char.charid = CID.SKELETON
        } else {
            gs.Char.charid = CID.GUARD
        }

        val seqHi = gs.level.guardsSeqHi[roomMinus1]
        if (seqHi == 0) {
            if (gs.Char.charid == CID.SKELETON) {
                gs.Char.sword = Sword.DRAWN
                seg005.seqtblOffsetChar(Seq.seq_63_guard_active_after_fall)
            } else {
                gs.Char.sword = Sword.SHEATHED
                seg005.seqtblOffsetChar(Seq.seq_77_guard_stand_inactive)
            }
        } else {
            gs.Char.currSeq = gs.level.guardsSeqLo[roomMinus1] + (seqHi shl 8)
        }

        seg006.playSeq()
        gs.guardSkill = gs.level.guardsSkill[roomMinus1]
        if (gs.guardSkill >= GameConstants.NUM_GUARD_SKILLS) {
            gs.guardSkill = 3
        }

        val frame = gs.Char.frame
        if (frame == FID.frame_185_dead || frame == FID.frame_177_spiked || frame == FID.frame_178_chomped) {
            gs.Char.alive = 1
            stubs.drawGuardHp(0, gs.guardhpCurr)
            gs.guardhpCurr = 0
        } else {
            gs.Char.alive = -1
            gs.justblocked = 0
            gs.guardRefrac = 0
            gs.isGuardNotice = 0
            getGuardHp()
            // REMEMBER_GUARD_HP
            if (gs.fixes.enableRememberGuardHp != 0 && rememberedHp > 0) {
                gs.guardhpDelta = rememberedHp.toShort()
                gs.guardhpCurr = rememberedHp
            }
        }

        gs.Char.fallY = 0
        gs.Char.fallX = 0
        gs.Char.action = Act.RUN_JUMP
        seg006.saveshad()
    }

    /** seg002:0269 — Check if guard has fallen out of the room. */
    fun checkGuardFallout() {
        if (gs.Guard.direction == Dir.NONE || gs.Guard.y < 211) {
            return
        }
        if (gs.Guard.charid == CID.SHADOW) {
            if (gs.Guard.action != Act.IN_FREEFALL) {
                return
            }
            seg006.loadshad()
            seg006.clearChar()
            seg006.saveshad()
        } else if (gs.Guard.charid == CID.SKELETON &&
            gs.level.roomlinks[gs.Guard.room - 1].down == gs.custom.skeletonReappearRoom
        ) {
            gs.Guard.room = gs.level.roomlinks[gs.Guard.room - 1].down
            gs.Guard.x = gs.custom.skeletonReappearX
            gs.Guard.currRow = gs.custom.skeletonReappearRow
            gs.Guard.direction = gs.custom.skeletonReappearDir
            gs.Guard.alive = -1
            leaveGuard()
        } else {
            seg006.onGuardKilled()
            gs.level.guardsTile[gs.drawnRoom - 1] = -1
            gs.Guard.direction = Dir.NONE
            stubs.drawGuardHp(0, gs.guardhpCurr)
            gs.guardhpCurr = 0
        }
    }

    /** seg002:02F5 — Save guard state back to level arrays, remove from room. */
    fun leaveGuard() {
        if (gs.Guard.direction == Dir.NONE || gs.Guard.charid == CID.SHADOW || gs.Guard.charid == CID.MOUSE) {
            return
        }
        val roomMinus1 = gs.Guard.room - 1
        gs.level.guardsTile[roomMinus1] = seg006.getTilepos(0, gs.Guard.currRow)

        gs.level.guardsColor[roomMinus1] = gs.currGuardColor and 0x0F
        // REMEMBER_GUARD_HP
        if (gs.fixes.enableRememberGuardHp != 0 && gs.guardhpCurr < 16) {
            gs.level.guardsColor[roomMinus1] = gs.level.guardsColor[roomMinus1] or (gs.guardhpCurr shl 4)
        }

        gs.level.guardsX[roomMinus1] = gs.Guard.x
        gs.level.guardsDir[roomMinus1] = gs.Guard.direction
        gs.level.guardsSkill[roomMinus1] = gs.guardSkill
        if (gs.Guard.alive < 0) {
            gs.level.guardsSeqHi[roomMinus1] = 0
        } else {
            gs.level.guardsSeqLo[roomMinus1] = gs.Guard.currSeq and 0xFF
            gs.level.guardsSeqHi[roomMinus1] = (gs.Guard.currSeq shr 8) and 0xFF
        }
        gs.Guard.direction = Dir.NONE
        stubs.drawGuardHp(0, gs.guardhpCurr)
        gs.guardhpCurr = 0
    }

    /** seg002:039E — Guard follows kid to another room. */
    fun followGuard() {
        gs.level.guardsTile[gs.Kid.room - 1] = 0xFF
        gs.level.guardsTile[gs.Guard.room - 1] = 0xFF
        seg006.loadshad()
        gotoOtherRoom(gs.roomleaveResult.toInt())
        seg006.saveshad()
    }

    /** seg002:03C7 — Handle kid exiting a room, guard follow/leave logic. */
    fun exitRoom() {
        var leave = 0
        if (gs.exitRoomTimer != 0) {
            --gs.exitRoomTimer
            // FIX_HANG_ON_TELEPORT
            if (gs.fixes.fixHangOnTeleport != 0 && gs.Char.y >= 211 && gs.Char.currRow >= 2) {
                // fall through (don't return)
            } else {
                return
            }
        }
        seg006.loadkid()
        loadFrameToObj()
        seg006.setCharCollision()
        gs.roomleaveResult = leaveRoom()
        if (gs.roomleaveResult < 0) {
            return
        }
        seg006.savekid()
        gs.nextRoom = gs.Char.room

        // FIX_DISAPPEARING_GUARD_B
        if (gs.fixes.fixOffscreenGuardsDisappearing != 0 && gs.nextRoom == gs.drawnRoom) return

        // USE_SUPER_HIGH_JUMP
        if (gs.fixes.enableSuperHighJump != 0 && gs.superJumpFall != 0 && gs.nextRoom == gs.drawnRoom) {
            return
        }

        if (gs.Guard.direction == Dir.NONE) return
        if (gs.Guard.alive < 0 && gs.Guard.sword == Sword.DRAWN) {
            val kidRoomM1 = gs.Kid.room - 1
            if (kidRoomM1 in 0..23 &&
                (gs.level.guardsTile[kidRoomM1] >= 30 || gs.level.guardsSeqHi[kidRoomM1] != 0)
            ) {
                if (gs.roomleaveResult.toInt() == 0) {
                    // left
                    if (gs.Guard.x >= 91) {
                        leave = 1
                    } else if (gs.fixes.fixGuardFollowingThroughClosedGates != 0 &&
                        gs.canGuardSeeKid.toInt() != 2 && gs.Kid.sword != Sword.DRAWN
                    ) {
                        leave = 1
                    }
                } else if (gs.roomleaveResult.toInt() == 1) {
                    // right
                    if (gs.Guard.x < 165) {
                        leave = 1
                    } else if (gs.fixes.fixGuardFollowingThroughClosedGates != 0 &&
                        gs.canGuardSeeKid.toInt() != 2 && gs.Kid.sword != Sword.DRAWN
                    ) {
                        leave = 1
                    }
                } else if (gs.roomleaveResult.toInt() == 2) {
                    // up
                    if (gs.Guard.currRow >= 0) leave = 1
                } else {
                    // down
                    if (gs.Guard.currRow < 3) leave = 1
                }
            } else {
                leave = 1
            }
        } else {
            leave = 1
        }
        if (leave != 0) {
            leaveGuard()
        } else {
            followGuard()
        }

        // FIX_DISAPPEARING_GUARD_A
        if (gs.fixes.fixOffscreenGuardsDisappearing != 0 && gs.nextRoom == gs.drawnRoom) {
            gs.drawnRoom = 0
        }
    }

    /** seg002:0486 — Move character to an adjacent room in the given direction (0=left,1=right,2=up,3=down). Returns opposite direction. */
    fun gotoOtherRoom(direction: Int): Int {
        val oppositeDir: Int
        // FIX_ENTERING_GLITCHED_ROOMS
        var otherRoom = if (gs.Char.room == 0 && gs.fixes.fixOffscreenGuardsDisappearing != 0) {
            0
        } else {
            val link = gs.level.roomlinks[gs.Char.room - 1]
            when (direction) {
                0 -> link.left
                1 -> link.right
                2 -> link.up
                3 -> link.down
                else -> 0
            }
        }
        gs.Char.room = otherRoom
        when (direction) {
            0 -> { // left
                gs.Char.x += 140
                oppositeDir = 1
            }
            1 -> { // right
                gs.Char.x -= 140
                oppositeDir = 0
            }
            2 -> { // up
                gs.Char.y += 189
                gs.Char.currRow = seg006.yToRowMod4(gs.Char.y)
                oppositeDir = 3
            }
            else -> { // down
                gs.Char.y -= 189
                gs.Char.currRow = seg006.yToRowMod4(gs.Char.y)
                oppositeDir = 2
            }
        }
        return oppositeDir
    }

    /** seg002:0504 — Determine if character should leave the room. Returns leave direction or -1/-2. */
    fun leaveRoom(): Short {
        val leaveDir: Short
        val chary = gs.Char.y
        val action = gs.Char.action
        val frame = gs.Char.frame

        if (action != Act.BUMPED &&
            action != Act.IN_FREEFALL &&
            action != Act.IN_MIDAIR &&
            chary.toByte().toInt() < 10 && chary.toByte().toInt() > -16
        ) {
            leaveDir = 2 // up
        } else if (chary >= 211) {
            leaveDir = 3 // down
        } else if (
            (frame >= FID.frame_135_climbing_1 && frame < 150) ||
            (frame >= FID.frame_110_stand_up_from_crouch_1 && frame < 120) ||
            (frame >= FID.frame_150_parry && frame < 163 &&
                (frame != FID.frame_157_walk_with_sword || gs.fixes.fixRetreatWithoutLeavingRoom == 0)) ||
            (frame >= FID.frame_166_stand_inactive && frame < 169) ||
            action == Act.TURN
        ) {
            return -1
        } else if (gs.Char.direction != Dir.RIGHT) {
            // looking left
            if (gs.charXLeft <= 54) {
                leaveDir = 0 // left
            } else if (gs.charXLeft >= 198) {
                leaveDir = 1 // right
            } else {
                return -1
            }
        } else {
            // looking right
            seg006.getTile(gs.Char.room, 9, gs.Char.currRow)
            if (gs.currTile2 != T.DOORTOP_WITH_FLOOR &&
                gs.currTile2 != T.DOORTOP &&
                gs.charXRight >= 201
            ) {
                leaveDir = 1 // right
            } else if (gs.charXRight <= 57) {
                leaveDir = 0 // left
            } else {
                return -1
            }
        }

        when (leaveDir.toInt()) {
            0 -> { // left
                playMirrMus()
                level3SetChkp()
                jaffarExit()
            }
            1 -> { // right
                swordDisappears()
                meetJaffar()
            }
            3 -> { // down
                if (gs.currentLevel == gs.custom.fallingExitLevel &&
                    gs.Char.room == gs.custom.fallingExitRoom
                ) {
                    return -2
                }
            }
        }

        gotoOtherRoom(leaveDir.toInt())

        // USE_REPLAY
        if (gs.skippingReplay != 0 && gs.replaySeekTarget == 0 /* replay_seek_0_next_room */) {
            gs.skippingReplay = 0
        }

        return leaveDir
    }

    // ========== Special event functions ==========

    /** seg002:0643 — Jaffar exit: trigger button when level door is open. */
    fun jaffarExit() {
        if (gs.leveldoorOpen == 2) {
            seg006.getTile(24, 0, 0)
            stubs.triggerButton(0, 0, -1)
        }
    }

    /** seg002:0665 — Set checkpoint on level 3. */
    fun level3SetChkp() {
        if (gs.currentLevel == gs.custom.checkpointLevel && gs.Char.room == 7) {
            gs.checkpoint = 1
            gs.hitpBegLev = gs.hitpMax
        }
    }

    /** seg002:0680 — Sword disappears on level 12. */
    fun swordDisappears() {
        if (gs.currentLevel == 12 && gs.Char.room == 18) {
            seg006.getTile(15, 1, 0)
            gs.currRoomTiles[gs.currTilepos] = T.FLOOR
            gs.currRoomModif[gs.currTilepos] = 0
        }
    }

    /** seg002:06AE — Meet Jaffar: play music on level 13. */
    fun meetJaffar() {
        if (gs.currentLevel == 13 && gs.leveldoorOpen == 0 && gs.Char.room == 3) {
            stubs.playSound(Snd.MEET_JAFFAR)
            gs.guardNoticeTimer = 28
        }
    }

    /** seg002:06D3 — Play mirror music on level 4. */
    fun playMirrMus() {
        if (gs.leveldoorOpen != 0 &&
            gs.leveldoorOpen != 0x4D &&
            gs.currentLevel == gs.custom.mirrorLevel &&
            gs.Char.currRow == gs.custom.mirrorRow &&
            gs.Char.room == 11
        ) {
            stubs.playSound(Snd.PRESENTATION)
            gs.leveldoorOpen = 0x4D
        }
    }

    // ========== Internal helpers ==========

    /** load_frame_to_obj — inlined from seg008, same as Seg004's version. */
    private fun loadFrameToObj() {
        seg006.resetObjClip()
        seg006.loadFrame()
        gs.objDirection = gs.Char.direction
        gs.objId = gs.curFrame.image
        gs.objChtab = 2 + (gs.curFrame.sword shr 6)
        gs.objX = ((seg006.charDxForward(gs.curFrame.dx) shl 1) - 116).toShort()
        gs.objY = gs.curFrame.dy + gs.Char.y
        if (((gs.curFrame.flags xor gs.objDirection).toByte().toInt()) >= 0) {
            gs.objX = (gs.objX + 1).toShort()
        }
    }

    /** prandom — pseudo-random number generator (from seg009). */
    fun prandom(max: Int): Int {
        if (gs.seedWasInit == 0) {
            gs.randomSeed = System.currentTimeMillis() and 0xFFFFFFFFL
            gs.seedWasInit = 1
        }
        gs.randomSeed = (gs.randomSeed * 214013 + 2531011) and 0xFFFFFFFFL
        return ((gs.randomSeed shr 16) % (max + 1)).toInt()
    }
}
