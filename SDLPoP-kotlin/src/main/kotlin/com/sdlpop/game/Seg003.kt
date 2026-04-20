/*
SDLPoP-kotlin, a Kotlin port of SDLPoP (Prince of Persia).
Based on SDLPoP by Dávid Nagy, licensed under GPL v3+.

Module 15: seg003.c translation — game loop helpers.
*/

package com.sdlpop.game

import com.sdlpop.game.SoundIds as Snd
import com.sdlpop.game.TileGeometry as TG
import com.sdlpop.game.Tiles as T
import com.sdlpop.game.CharIds as CID
import com.sdlpop.game.Directions as Dir
import com.sdlpop.game.Actions as Act
import com.sdlpop.game.SwordStatus as Sword
import com.sdlpop.game.FrameIds as FID
import com.sdlpop.game.SeqIds as Seq

object Seg003 {
    private val gs = GameState
    private val ext = ExternalStubs
    private val seg006 = Seg006
    private val seg007 = Seg007
    private val seg002 = Seg002

    // data:3D1A
    var distanceMirror: Int = 0 // sbyte

    // seg003:01A3
    fun doStartpos() {
        // Special event: start at checkpoint
        if (gs.currentLevel == gs.custom.checkpointLevel && gs.checkpoint != 0) {
            gs.level.startDir = gs.custom.checkpointRespawnDir
            gs.level.startRoom = gs.custom.checkpointRespawnRoom
            gs.level.startPos = gs.custom.checkpointRespawnTilepos
            // Special event: remove loose floor
            seg006.getTile(
                gs.custom.checkpointClearTileRoom,
                gs.custom.checkpointClearTileCol,
                gs.custom.checkpointClearTileRow
            )
            gs.currRoomTiles[gs.currTilepos] = T.EMPTY
        }
        gs.nextRoom = gs.level.startRoom
        gs.Char.room = gs.level.startRoom
        val x = gs.level.startPos
        gs.Char.currCol = x % TG.SCREEN_TILECOUNTX
        gs.Char.currRow = x / TG.SCREEN_TILECOUNTX
        gs.Char.x = gs.xBump[gs.Char.currCol + TG.FIRST_ONSCREEN_COLUMN] + TG.TILE_SIZEX
        // Start in the opposite direction (and turn into the correct one).
        gs.Char.direction = gs.level.startDir.inv()
        if (gs.seamless == 0) {
            val hp = if (gs.currentLevel != 0) {
                gs.hitpBegLev
            } else {
                // HP on demo level
                gs.custom.demoHitp
            }
            gs.hitpMax = hp
            gs.hitpCurr = hp
        }
        if (gs.custom.tblEntryPose[gs.currentLevel] == 1) {
            // Special event: press button + falling entry
            seg006.getTile(5, 2, 0)
            ext.triggerButton(0, 0, -1)
            ext.seqtblOffsetChar(Seq.seq_7_fall) // fall
        } else if (gs.custom.tblEntryPose[gs.currentLevel] == 2) {
            // Special event: running entry
            ext.seqtblOffsetChar(Seq.seq_84_run) // run
        } else {
            ext.seqtblOffsetChar(Seq.seq_5_turn) // turn
        }
        setStartPos()
    }

    // seg003:028A
    fun setStartPos() {
        gs.Char.y = gs.yLand[gs.Char.currRow + 1].toInt()
        gs.Char.alive = -1
        gs.Char.charid = CID.KID
        gs.isScreaming = 0
        gs.knock = 0
        gs.upsideDown = gs.custom.startUpsideDown // 0
        gs.isFeatherFall = 0
        gs.Char.fallY = 0
        gs.Char.fallX = 0
        gs.offguard = 0
        gs.Char.sword = Sword.SHEATHED
        gs.droppedout = 0
        seg006.playSeq()
        if (gs.currentLevel == gs.custom.fallingEntryLevel && gs.Char.room == gs.custom.fallingEntryRoom) {
            // Special event: level 7 falling entry
            // level 7, room 17: show room below
            seg002.gotoOtherRoom(3)
        }
        seg006.savekid()
    }

    // seg003:02E6
    fun findStartLevelDoor() {
        ext.getRoomAddress(gs.Kid.room)
        for (tilepos in 0 until 30) {
            if ((gs.currRoomTiles[tilepos] and 0x1F) == T.LEVEL_DOOR_LEFT) {
                seg007.startLevelDoor(gs.Kid.room, tilepos)
            }
        }
    }

    // seg003:0576
    fun redrawAtChar() {
        var xColLeft: Int
        var xColRight: Int
        if (gs.Char.sword >= Sword.DRAWN) {
            // If char is holding sword, it makes redraw-area bigger.
            if (gs.Char.direction >= Dir.RIGHT) {
                gs.charColRight = (gs.charColRight + 1).toShort()
                if (gs.charColRight > 9) gs.charColRight = 9
            } else {
                gs.charColLeft = (gs.charColLeft - 1).toShort()
                if (gs.charColLeft < 0) gs.charColLeft = 0
            }
        }
        if (gs.Char.charid == CID.KID) {
            val xTopRow = minOf(gs.charTopRow.toInt(), gs.prevCharTopRow.toInt())
            xColRight = maxOf(gs.charColRight.toInt(), gs.prevCharColRight.toInt())
            xColLeft = minOf(gs.charColLeft.toInt(), gs.prevCharColLeft.toInt())
            for (tileRow in xTopRow..gs.charBottomRow.toInt()) {
                for (tileCol in xColLeft..xColRight) {
                    Seg007.setRedrawFore(seg006.getTilepos(tileCol, tileRow), 1)
                }
            }
            gs.prevCharTopRow = gs.charTopRow
            gs.prevCharColRight = gs.charColRight
            gs.prevCharColLeft = gs.charColLeft
        } else {
            xColRight = gs.charColRight.toInt()
            xColLeft = gs.charColLeft.toInt()
            for (tileRow in gs.charTopRow.toInt()..gs.charBottomRow.toInt()) {
                for (tileCol in xColLeft..xColRight) {
                    Seg007.setRedrawFore(seg006.getTilepos(tileCol, tileRow), 1)
                }
            }
        }
    }

    // seg003:0645
    fun redrawAtChar2() {
        val charAction = gs.Char.action
        val charFrame = gs.Char.frame
        // Determine which redraw function to use
        val useFloorOverlay: Boolean
        // frames 78..80: grab
        if (charFrame < FID.frame_78_jumphang || charFrame >= FID.frame_80_jumphang) {
            // frames 135..149: climb up
            if (charFrame >= FID.frame_137_climbing_3 && charFrame < FID.frame_145_climbing_11) {
                useFloorOverlay = true
            } else {
                // frames 102..106: fall
                if (charAction != Act.HANG_CLIMB && charAction != Act.IN_MIDAIR &&
                    charAction != Act.IN_FREEFALL && charAction != Act.HANG_STRAIGHT &&
                    (charAction != Act.BUMPED || charFrame < FID.frame_102_start_fall_1 || charFrame > FID.frame_106_fall)
                ) {
                    return
                }
                useFloorOverlay = false
            }
        } else {
            useFloorOverlay = false
        }
        for (tileCol in gs.charColRight.toInt() downTo gs.charColLeft.toInt()) {
            if (charAction != 2) {
                if (useFloorOverlay) {
                    Seg007.setRedrawFloorOverlay(seg006.getTilepos(tileCol, gs.charBottomRow.toInt()), 1)
                } else {
                    Seg007.setRedraw2(seg006.getTilepos(tileCol, gs.charBottomRow.toInt()), 1)
                }
            }
            if (gs.charTopRow != gs.charBottomRow) {
                if (useFloorOverlay) {
                    Seg007.setRedrawFloorOverlay(seg006.getTilepos(tileCol, gs.charTopRow.toInt()), 1)
                } else {
                    Seg007.setRedraw2(seg006.getTilepos(tileCol, gs.charTopRow.toInt()), 1)
                }
            }
        }
    }

    // seg003:0706
    fun checkKnock() {
        val knock = gs.knock.toInt()
        if (knock != 0) {
            seg007.doKnock(gs.Char.room, gs.Char.currRow - if (knock > 0) 1 else 0)
            gs.knock = 0
        }
    }

    // seg003:0913
    fun posGuards() {
        for (room1 in 0 until GameConstants.ROOMCOUNT) {
            val guardTile = gs.level.guardsTile[room1]
            if (guardTile < 30) {
                gs.level.guardsX[room1] = gs.xBump[(guardTile % 10) + TG.FIRST_ONSCREEN_COLUMN] + TG.TILE_SIZEX
                gs.level.guardsSeqHi[room1] = 0
            }
        }
    }

    // seg003:0959
    fun checkCanGuardSeeKid() {
        /*
        Possible results in can_guard_see_kid:
        0: Guard can't see Kid
        1: Guard can see Kid, but won't come
        2: Guard can see Kid, and will come
        */
        val kidFrame = gs.Kid.frame
        if (gs.Guard.charid == CID.MOUSE) {
            gs.canGuardSeeKid = 0
            return
        }
        if ((gs.Guard.charid != CID.SHADOW || gs.currentLevel == 12) &&
            kidFrame != 0 && (kidFrame < FID.frame_219_exit_stairs_3 || kidFrame >= 229) &&
            gs.Guard.direction != Dir.NONE && gs.Kid.alive < 0 && gs.Guard.alive < 0 &&
            gs.Kid.room == gs.Guard.room && gs.Kid.currRow == gs.Guard.currRow
        ) {
            gs.canGuardSeeKid = 2
            var leftPos = gs.xBump[gs.Kid.currCol + TG.FIRST_ONSCREEN_COLUMN] + TG.TILE_MIDX
            if (gs.fixes.fixDoortopDisablingGuard != 0) {
                if (gs.Kid.action == Act.HANG_CLIMB || gs.Kid.action == Act.HANG_STRAIGHT) {
                    leftPos += TG.TILE_SIZEX
                }
            }
            var rightPos = gs.xBump[gs.Guard.currCol + TG.FIRST_ONSCREEN_COLUMN] + TG.TILE_MIDX
            if (leftPos > rightPos) {
                val temp = leftPos
                leftPos = rightPos
                rightPos = temp
            }
            // A chomper is on the left side of a tile, so it doesn't count.
            if (getTileAtKid(leftPos) == T.CHOMPER) {
                leftPos += TG.TILE_SIZEX
            }
            // A gate is on the right side of a tile, so it doesn't count.
            val rightTile = getTileAtKid(rightPos)
            if (rightTile == T.GATE ||
                (gs.fixes.fixDoortopDisablingGuard != 0 &&
                    (rightTile == T.DOORTOP_WITH_FLOOR || rightTile == T.DOORTOP))
            ) {
                rightPos -= TG.TILE_SIZEX
            }
            if (rightPos >= leftPos) {
                var pos = leftPos
                while (pos <= rightPos) {
                    // Can't see through these tiles.
                    if (getTileAtKid(pos) == T.WALL ||
                        gs.currTile2 == T.DOORTOP_WITH_FLOOR ||
                        gs.currTile2 == T.DOORTOP
                    ) {
                        gs.canGuardSeeKid = 0; return
                    }
                    // Can see through these, but won't go through them.
                    if (gs.currTile2 == T.LOOSE ||
                        gs.currTile2 == T.CHOMPER ||
                        (gs.currTile2 == T.GATE && gs.currRoomModif[gs.currTilepos] < 112) ||
                        seg006.tileIsFloor(gs.currTile2) == 0
                    ) {
                        gs.canGuardSeeKid = 1
                    }
                    pos += TG.TILE_SIZEX
                }
            }
        } else {
            gs.canGuardSeeKid = 0
        }
    }

    // seg003:0A99
    fun getTileAtKid(xpos: Int): Int {
        return seg006.getTile(gs.Kid.room, seg006.getTileDivModM7(xpos), gs.Kid.currRow)
    }

    // seg003:0ABA
    fun doMouse() {
        seg006.loadkid()
        gs.Char.charid = gs.custom.mouseObject
        gs.Char.x = gs.custom.mouseStartX
        gs.Char.currRow = 0
        gs.Char.y = gs.yLand[gs.Char.currRow + 1].toInt()
        gs.Char.alive = -1
        gs.Char.direction = Dir.LEFT
        gs.guardhpCurr = 1
        ext.seqtblOffsetChar(Seq.seq_105_mouse_forward) // mouse forward
        seg006.playSeq()
        seg006.saveshad()
    }

    // seg003:085B
    fun checkMirrorImage() {
        val xpos = gs.xBump[gs.Char.currCol + TG.FIRST_ONSCREEN_COLUMN] + 10
        var distance = seg006.distanceToEdgeWeight()
        if (gs.Char.direction >= Dir.RIGHT) {
            distance = distance.inv() + TG.TILE_SIZEX
        }
        distanceMirror = (distance - 2).toByte().toInt() // sbyte
        gs.Char.x = (xpos shl 1) - gs.Char.x
        gs.Char.direction = gs.Char.direction.inv()
    }

    // seg003:0798
    fun checkMirror() {
        if (gs.jumpedThroughMirror.toInt() == -1) {
            jumpThroughMirror()
        } else {
            if (seg006.getTileAtChar() == T.MIRROR) {
                seg006.loadkid()
                seg006.loadFrame()
                checkMirrorImage()
                if (distanceMirror >= 0 && gs.custom.showMirrorImage != 0 && gs.Char.room == gs.drawnRoom) {
                    ext.addObjtable(4) // mirror image
                }
            }
        }
    }

    // seg003:080A
    fun jumpThroughMirror() {
        seg006.loadkid()
        seg006.loadFrame()
        checkMirrorImage()
        gs.jumpedThroughMirror = 0.toShort()
        gs.Char.charid = CID.SHADOW
        ext.playSound(Snd.JUMP_THROUGH_MIRROR) // jump through mirror
        seg006.saveshad()
        gs.guardhpCurr = gs.hitpMax
        gs.guardhpMax = gs.hitpMax
        gs.hitpCurr = 1
        ext.drawKidHp(1, gs.hitpMax)
        ext.drawGuardHp(gs.guardhpCurr, gs.guardhpMax)
    }

    // seg003:08AA
    fun bumpIntoOpponent() {
        // This is called from play_kid_frame, so char=Kid, Opp=Guard
        if (gs.canGuardSeeKid.toInt() >= 2 &&
            gs.Char.sword == Sword.SHEATHED && // Kid must not be in fighting pose
            gs.Opp.sword != Sword.SHEATHED && // but Guard must
            gs.Opp.action < 2 &&
            gs.Char.direction != gs.Opp.direction // must be facing toward each other
        ) {
            val distance = seg006.charOppDist()
            if (Math.abs(distance) <= 15) {
                if (gs.fixes.fixPainlessFallOnGuard != 0) {
                    if (gs.Char.fallY >= 33) return // don't bump; dead
                    else if (gs.Char.fallY >= 22) { // medium land
                        seg006.takeHp(1)
                        ext.playSound(Snd.MEDIUM_LAND)
                    }
                }
                if (gs.fixes.fixJumpingOverGuard != 0) {
                    if ((gs.Char.direction == Dir.RIGHT && gs.Char.x > gs.Opp.x) ||
                        (gs.Char.direction == Dir.LEFT && gs.Char.x < gs.Opp.x)
                    ) {
                        gs.Char.x = gs.Opp.x
                    }
                }
                gs.Char.y = gs.yLand[gs.Char.currRow + 1].toInt()
                gs.Char.fallY = 0
                ext.seqtblOffsetChar(Seq.seq_47_bump)
                seg006.playSeq()
            }
        }
    }

    // seg003:0B1A
    fun removeFlashIfHurt() {
        if (gs.flashTime != 0) {
            gs.flashTime--
        } else {
            if (gs.hitpDelta >= 0) return
        }
        // remove_flash() — rendering only, no-op in headless
    }

    // seg003:0735
    fun timers() {
        if (gs.unitedWithShadow > 0) {
            gs.unitedWithShadow = (gs.unitedWithShadow - 1).toShort()
            if (gs.unitedWithShadow.toInt() == 0) {
                gs.unitedWithShadow = (gs.unitedWithShadow - 1).toShort()
            }
        }
        if (gs.guardNoticeTimer > 0) {
            gs.guardNoticeTimer = (gs.guardNoticeTimer - 1).toShort()
        }
        if (gs.resurrectTime > 0) {
            gs.resurrectTime--
        }

        if (gs.fixes.fixQuicksaveDuringFeather != 0) {
            if (gs.isFeatherFall > 0) {
                gs.isFeatherFall--
                if (gs.isFeatherFall == 0) {
                    if (ext.checkSoundPlaying() != 0) {
                        ext.stopSounds()
                    }
                }
            }
        } else {
            if (gs.isFeatherFall != 0) gs.isFeatherFall++

            if (gs.isFeatherFall != 0 && (ext.checkSoundPlaying() == 0 || gs.isFeatherFall > 225)) {
                // during replays, feather effect gets cancelled in do_replay_move()
                if (gs.replaying == 0) {
                    gs.isFeatherFall = 0
                }
            }
        }

        // Special event: mouse
        if (gs.currentLevel == gs.custom.mouseLevel && gs.Char.room == gs.custom.mouseRoom && gs.leveldoorOpen != 0) {
            gs.leveldoorOpen++
            // time before mouse comes: 150/12=12.5 seconds
            if (gs.leveldoorOpen == gs.custom.mouseDelay) {
                doMouse()
            }
        }

        if (gs.fixes.enableSuperHighJump != 0 && gs.superJumpTimer > 0) {
            gs.superJumpTimer--
            if (gs.superJumpTimer == 0 && gs.Kid.frame == FID.frame_79_jumphang) {
                if (seg006.getTile(gs.superJumpRoom, gs.superJumpCol, gs.superJumpRow) == T.LOOSE &&
                    (gs.currRoomTiles[gs.currTilepos] and 0x20) == 0
                ) {
                    ext.makeLooseFall(1) // knocks the true loose tile above
                    seg007.doKnock(gs.superJumpRoom, gs.superJumpRow) // shakes the rest of loose tiles
                } else if (gs.currTile2 == T.WALL || seg006.tileIsFloor(gs.currTile2) != 0) {
                    if (gs.superJumpRow < 2) {
                        gs.Kid.currRow = gs.superJumpRow + 1
                        gs.Kid.y = gs.yLand[gs.superJumpRow + 2] + 10
                    }
                    seg007.doKnock(gs.superJumpRoom, gs.superJumpRow) // shakes loose tiles in the row
                } else if (seg006.tileIsFloor(gs.currTile2) == 0) {
                    if (gs.superJumpRow == 2) {
                        gs.Kid.room = gs.level.roomlinks[gs.Kid.room - 1].up
                    }
                    if (gs.Kid.room != 0) { // there is a room above
                        gs.Kid.currRow = gs.superJumpRow + 1
                        gs.Kid.y = gs.yLand[gs.superJumpRow + 2] - 10
                        gs.Kid.fallX = 0
                        gs.Kid.fallY = 0
                        gs.superJumpFall = 1
                        // gives kid an ability to grab the above front tile
                        // seqtbl_offset_kid_char(seq_19_fall)
                        gs.Kid.currSeq = SequenceTable.seqtblOffsets[Seq.seq_19_fall]
                        seg006.playSeq()
                    }
                }
            }
        }
    }
}
