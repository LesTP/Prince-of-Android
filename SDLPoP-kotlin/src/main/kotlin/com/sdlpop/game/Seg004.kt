/*
SDLPoP-kotlin, a Kotlin port of SDLPoP (Prince of Persia).
Based on SDLPoP by Dávid Nagy, licensed under GPL v3+.

Module 9: seg004.c → Kotlin
Collision detection, wall bumping, chomper damage, gate pushing, edge distance.
26 functions, ~621 lines of C.
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
import com.sdlpop.game.EdgeType as ET

/**
 * seg004: Collision detection — wall bumping, chomper damage, gate pushing, edge distance.
 * All functions operate on [GameState] (aliased as `gs`).
 */
object Seg004 {
    private val gs = GameState
    private val seg006 = Seg006
    private val stubs = ExternalStubs

    // --- seg004-local variables (C file-scope statics) ---
    var bumpColLeftOfWall: Int = -1   // sbyte (bump_col_left_of_wall)
    var bumpColRightOfWall: Int = -1  // sbyte (bump_col_right_of_wall)
    var rightCheckedCol: Int = 0      // sbyte (right_checked_col)
    var leftCheckedCol: Int = 0       // sbyte (left_checked_col)
    var collTileLeftXpos: Int = 0     // short (coll_tile_left_xpos)

    // Constant lookup tables — indexed by wall_type return value
    val wallDistFromLeft = intArrayOf(0, 10, 0, -1, 0, 0)
    val wallDistFromRight = intArrayOf(0, 0, 10, 13, 0, 0)

    // seg004:0004
    fun checkCollisions() {
        bumpColLeftOfWall = -1
        bumpColRightOfWall = -1
        if (gs.Char.action == Act.TURN) return
        gs.collisionRow = gs.Char.currRow
        moveCollToPrev()
        gs.prevCollisionRow = gs.collisionRow
        rightCheckedCol = minOf(seg006.getTileDivModM7(gs.charXRightColl.toInt()) + 2, 11)
        leftCheckedCol = seg006.getTileDivModM7(gs.charXLeftColl.toInt()) - 1
        getRowCollisionData(gs.collisionRow, gs.currRowCollRoom, gs.currRowCollFlags)
        getRowCollisionData(gs.collisionRow + 1, gs.belowRowCollRoom, gs.belowRowCollFlags)
        getRowCollisionData(gs.collisionRow - 1, gs.aboveRowCollRoom, gs.aboveRowCollFlags)
        for (column in 9 downTo 0) {
            if (gs.currRowCollRoom[column] >= 0 &&
                gs.prevCollRoom[column] == gs.currRowCollRoom[column]
            ) {
                // char bumps into left of wall
                if ((gs.prevCollFlags[column] and 0x0F) == 0 &&
                    (gs.currRowCollFlags[column] and 0x0F) != 0
                ) {
                    bumpColLeftOfWall = column
                }
                // char bumps into right of wall
                if ((gs.prevCollFlags[column] and 0xF0) == 0 &&
                    (gs.currRowCollFlags[column] and 0xF0) != 0
                ) {
                    bumpColRightOfWall = column
                }
            }
        }
    }

    // seg004:00DF
    fun moveCollToPrev() {
        val rowCollRoomPtr: IntArray
        val rowCollFlagsPtr: IntArray
        if (gs.collisionRow == gs.prevCollisionRow ||
            gs.collisionRow + 3 == gs.prevCollisionRow ||
            gs.collisionRow - 3 == gs.prevCollisionRow
        ) {
            rowCollRoomPtr = gs.currRowCollRoom
            rowCollFlagsPtr = gs.currRowCollFlags
        } else if (
            gs.collisionRow + 1 == gs.prevCollisionRow ||
            gs.collisionRow - 2 == gs.prevCollisionRow
        ) {
            rowCollRoomPtr = gs.aboveRowCollRoom
            rowCollFlagsPtr = gs.aboveRowCollFlags
        } else {
            rowCollRoomPtr = gs.belowRowCollRoom
            rowCollFlagsPtr = gs.belowRowCollFlags
        }
        for (column in 0 until 10) {
            gs.prevCollRoom[column] = rowCollRoomPtr[column]
            gs.prevCollFlags[column] = rowCollFlagsPtr[column]
            gs.belowRowCollRoom[column] = -1
            gs.aboveRowCollRoom[column] = -1
            gs.currRowCollRoom[column] = -1
            if (gs.fixes.fixCollFlags != 0) {
                gs.currRowCollFlags[column] = 0
                gs.belowRowCollFlags[column] = 0
                gs.aboveRowCollFlags[column] = 0
            }
        }
    }

    // seg004:0185
    fun getRowCollisionData(row: Int, rowCollRoomPtr: IntArray, rowCollFlagsPtr: IntArray) {
        val room = gs.Char.room
        collTileLeftXpos = gs.xBump[leftCheckedCol + TG.FIRST_ONSCREEN_COLUMN] + TG.TILE_MIDX
        for (column in leftCheckedCol..rightCheckedCol) {
            val leftWallXpos = getLeftWallXpos(room, column, row)
            val rightWallXpos = getRightWallXpos(room, column, row)
            // char bumps into left of wall
            var currFlags = if (leftWallXpos < gs.charXRightColl.toInt()) 0x0F else 0
            // char bumps into right of wall
            currFlags = currFlags or (if (rightWallXpos > gs.charXLeftColl.toInt()) 0xF0 else 0)
            rowCollFlagsPtr[gs.tileCol.toInt()] = currFlags
            rowCollRoomPtr[gs.tileCol.toInt()] = gs.currRoom.toInt()
            collTileLeftXpos += TG.TILE_SIZEX
        }
    }

    // seg004:0226
    fun getLeftWallXpos(room: Int, column: Int, row: Int): Int {
        val type = seg006.wallType(seg006.getTile(room, column, row))
        return if (type != 0) {
            wallDistFromLeft[type] + collTileLeftXpos
        } else {
            0xFF
        }
    }

    // seg004:025F
    fun getRightWallXpos(room: Int, column: Int, row: Int): Int {
        val type = seg006.wallType(seg006.getTile(room, column, row))
        return if (type != 0) {
            collTileLeftXpos - wallDistFromRight[type] + TG.TILE_RIGHTX
        } else {
            0
        }
    }

    // seg004:029D
    fun checkBumped() {
        if (gs.Char.action != Act.HANG_CLIMB &&
            gs.Char.action != Act.HANG_STRAIGHT &&
            // frames 135..149: climb up
            (gs.Char.frame < FID.frame_135_climbing_1 || gs.Char.frame >= 149)
        ) {
            if (gs.fixes.fixTwoCollBug != 0) {
                if (bumpColLeftOfWall >= 0) {
                    checkBumpedLookRight()
                }
                if (bumpColRightOfWall >= 0) {
                    checkBumpedLookLeft()
                }
            } else {
                if (bumpColLeftOfWall >= 0) {
                    checkBumpedLookRight()
                } else if (bumpColRightOfWall >= 0) {
                    checkBumpedLookLeft()
                }
            }
        }
    }

    // seg004:02D2
    fun checkBumpedLookLeft() {
        if ((gs.Char.sword == Sword.DRAWN || gs.Char.direction < Dir.RIGHT) && // looking left
            isObstacleAtCol(bumpColRightOfWall) != 0
        ) {
            if (gs.fixes.enableJumpGrab != 0 && gs.controlShift == Ctrl.HELD) {
                if (seg006.checkGrabRunJump()) {
                    return
                }
                // reset obstacle tile values
                isObstacleAtCol(bumpColRightOfWall)
            }
            bumped(
                (getRightWallXpos(gs.currRoom.toInt(), gs.tileCol.toInt(), gs.tileRow.toInt()) - gs.charXLeftColl.toInt()).toByte().toInt(),
                Dir.RIGHT
            )
        }
    }

    // seg004:030A
    fun checkBumpedLookRight() {
        if ((gs.Char.sword == Sword.DRAWN || gs.Char.direction == Dir.RIGHT) && // looking right
            isObstacleAtCol(bumpColLeftOfWall) != 0
        ) {
            if (gs.fixes.enableJumpGrab != 0 && gs.controlShift == Ctrl.HELD) {
                if (seg006.checkGrabRunJump()) {
                    return
                }
                // reset obstacle tile values
                isObstacleAtCol(bumpColLeftOfWall)
            }
            bumped(
                (getLeftWallXpos(gs.currRoom.toInt(), gs.tileCol.toInt(), gs.tileRow.toInt()) - gs.charXRightColl.toInt()).toByte().toInt(),
                Dir.LEFT
            )
        }
    }

    // seg004:0343
    fun isObstacleAtCol(tileCol: Int): Int {
        var tileRow = gs.Char.currRow
        if (tileRow < 0) {
            tileRow += 3
        }
        if (tileRow >= 3) {
            tileRow -= 3
        }
        seg006.getTile(gs.currRowCollRoom[tileCol], tileCol, tileRow)
        return isObstacle()
    }

    // seg004:037E
    fun isObstacle(): Int {
        if (gs.currTile2 == T.POTION) {
            return 0
        } else if (gs.currTile2 == T.GATE) {
            if (canBumpIntoGate() == 0) return 0
        } else if (gs.currTile2 == T.CHOMPER) {
            // is the chomper closed?
            if (gs.currRoomModif[gs.currTilepos] != 2) return 0
        } else if (
            gs.currTile2 == T.MIRROR &&
            gs.Char.charid == CID.KID &&
            gs.Char.frame >= FID.frame_39_start_run_jump_6 && gs.Char.frame < FID.frame_44_running_jump_5 && // run-jump
            gs.Char.direction < Dir.RIGHT // right-to-left only
        ) {
            gs.currRoomModif[gs.currTilepos] = 0x56 // broken mirror
            gs.jumpedThroughMirror = -1
            return 0
        }
        collTileLeftXpos = xposInDrawnRoom(gs.xBump[gs.tileCol.toInt() + TG.FIRST_ONSCREEN_COLUMN]) + TG.TILE_MIDX
        return 1
    }

    // seg004:0405
    fun xposInDrawnRoom(xpos: Int): Int {
        var result = xpos
        if (gs.currRoom.toInt() != gs.drawnRoom) {
            if (gs.currRoom.toInt() == gs.roomL || gs.currRoom.toInt() == gs.roomBL) {
                result -= TG.TILE_SIZEX * TG.SCREEN_TILECOUNTX
            } else if (gs.currRoom.toInt() == gs.roomR || gs.currRoom.toInt() == gs.roomBR) {
                result += TG.TILE_SIZEX * TG.SCREEN_TILECOUNTX
            }
        }
        return result
    }

    // seg004:0448
    fun bumped(deltaX: Int, pushDirection: Int) {
        // frame 177: spiked
        if (gs.Char.alive < 0 && gs.Char.frame != FID.frame_177_spiked) {
            gs.Char.x += deltaX
            if (pushDirection < Dir.RIGHT) {
                // pushing left
                if (gs.currTile2 == T.WALL) {
                    gs.tileCol = (gs.tileCol - 1).toShort()
                    seg006.getTile(gs.currRoom.toInt(), gs.tileCol.toInt(), gs.tileRow.toInt())
                }
            } else {
                // pushing right
                if (gs.currTile2 == T.DOORTOP ||
                    gs.currTile2 == T.DOORTOP_WITH_FLOOR ||
                    gs.currTile2 == T.WALL
                ) {
                    gs.tileCol = (gs.tileCol + 1).toShort()
                    if (gs.currRoom.toInt() == 0 && gs.tileCol.toInt() == 10) {
                        gs.currRoom = gs.Char.room.toShort()
                        gs.tileCol = 0
                    }
                    seg006.getTile(gs.currRoom.toInt(), gs.tileCol.toInt(), gs.tileRow.toInt())
                }
            }
            if (seg006.tileIsFloor(gs.currTile2) != 0) {
                bumpedFloor(pushDirection)
            } else {
                bumpedFall()
            }
        }
    }

    // seg004:04E4
    fun bumpedFall() {
        val action = gs.Char.action
        gs.Char.x = seg006.charDxForward(-4)
        if (action == Act.IN_FREEFALL) {
            gs.Char.fallX = 0
        } else {
            stubs.seqtblOffsetChar(Seq.seq_45_bumpfall) // fall after bumped
            seg006.playSeq()
        }
        bumpedSound()
    }

    // seg004:0520
    fun bumpedFloor(pushDirection: Int) {
        if (gs.Char.sword != Sword.DRAWN &&
            ((gs.yLand[gs.Char.currRow + 1].toInt() - gs.Char.y) and 0xFFFF) >= 15
        ) {
            bumpedFall()
        } else {
            gs.Char.y = gs.yLand[gs.Char.currRow + 1].toInt()
            if (gs.Char.fallY >= 22) {
                gs.Char.x = seg006.charDxForward(-5)
            } else {
                gs.Char.fallY = 0
                if (gs.Char.alive != 0) {
                    val seqIndex: Int
                    if (gs.Char.sword == Sword.DRAWN) {
                        if (pushDirection == gs.Char.direction) {
                            stubs.seqtblOffsetChar(Seq.seq_65_bump_forward_with_sword)
                            seg006.playSeq()
                            gs.Char.x = seg006.charDxForward(1)
                            return
                        } else {
                            seqIndex = Seq.seq_64_pushed_back_with_sword
                        }
                    } else {
                        val frame = gs.Char.frame
                        seqIndex = if (frame == 24 || frame == 25 ||
                            (frame in 40 until 43) ||
                            (frame >= FID.frame_102_start_fall_1 && frame < 107)
                        ) {
                            Seq.seq_46_hardbump // bump into wall after run-jump (crouch)
                        } else {
                            Seq.seq_47_bump // bump into wall
                        }
                    }
                    stubs.seqtblOffsetChar(seqIndex)
                    seg006.playSeq()
                    bumpedSound()
                }
            }
        }
    }

    // seg004:05F1
    fun bumpedSound() {
        gs.isGuardNotice = 1
        stubs.playSound(Snd.BUMPED) // touching a wall
    }

    // seg004:0601
    fun clearCollRooms() {
        gs.prevCollRoom.fill(-1)
        gs.currRowCollRoom.fill(-1)
        gs.belowRowCollRoom.fill(-1)
        gs.aboveRowCollRoom.fill(-1)
        if (gs.fixes.fixCollFlags != 0) {
            gs.prevCollFlags.fill(0)
            gs.currRowCollFlags.fill(0)
            gs.belowRowCollFlags.fill(0)
            gs.aboveRowCollFlags.fill(0)
        }
        gs.prevCollisionRow = -1
    }

    // seg004:0657
    fun canBumpIntoGate(): Int {
        return if ((gs.currRoomModif[gs.currTilepos] shr 2) + 6 < gs.charHeight) 1 else 0
    }

    // seg004:067C
    fun getEdgeDistance(): Int {
        var distance: Int
        seg006.determineCol()
        loadFrameToObj()
        seg006.setCharCollision()
        val tiletype = seg006.getTileAtChar()
        if (seg006.wallType(tiletype) != 0) {
            gs.tileCol = gs.Char.currCol.toShort()
            distance = distFromWallForward(tiletype)
            if (distance >= 0) {
                if (distance <= TG.TILE_RIGHTX) {
                    gs.edgeType = ET.WALL
                } else {
                    gs.edgeType = ET.FLOOR
                    distance = 11
                }
            } else {
                return getEdgeDistanceFront()
            }
        } else {
            return getEdgeDistanceFront()
        }
        gs.currTile2 = tiletype
        return distance
    }

    // Helper for the second half of get_edge_distance (the loc_59E8 path)
    private fun getEdgeDistanceFront(): Int {
        var distance: Int
        val tiletype = seg006.getTileInfrontofChar()
        if (tiletype == T.DOORTOP && gs.Char.direction >= Dir.RIGHT) {
            gs.edgeType = ET.CLOSER
            distance = seg006.distanceToEdgeWeight()
        } else {
            if (seg006.wallType(tiletype) != 0) {
                gs.tileCol = gs.infrontx.toShort()
                distance = distFromWallForward(tiletype)
                if (distance >= 0) {
                    if (distance <= TG.TILE_RIGHTX) {
                        gs.edgeType = ET.WALL
                    } else {
                        gs.edgeType = ET.FLOOR
                        distance = 11
                    }
                    gs.currTile2 = tiletype
                    return distance
                }
            }
            if (tiletype == T.LOOSE) {
                gs.edgeType = ET.CLOSER
                distance = seg006.distanceToEdgeWeight()
            } else if (
                tiletype == T.CLOSER ||
                tiletype == T.SWORD ||
                tiletype == T.POTION
            ) {
                distance = seg006.distanceToEdgeWeight()
                if (distance != 0) {
                    gs.edgeType = ET.CLOSER
                } else {
                    gs.edgeType = ET.FLOOR
                    distance = 11
                }
            } else {
                if (seg006.tileIsFloor(tiletype) != 0) {
                    gs.edgeType = ET.FLOOR
                    distance = 11
                } else {
                    gs.edgeType = ET.CLOSER
                    distance = seg006.distanceToEdgeWeight()
                }
            }
        }
        gs.currTile2 = tiletype
        return distance
    }

    // seg004:076B
    fun checkChompedKid() {
        val tileRow = gs.Char.currRow
        for (tileCol in 0 until 10) {
            if (gs.currRowCollFlags[tileCol] == 0xFF &&
                seg006.getTile(gs.currRowCollRoom[tileCol], tileCol, tileRow) == T.CHOMPER &&
                (gs.currRoomModif[gs.currTilepos] and 0x7F) == 2 // closed chomper
            ) {
                chomped()
            }
        }
    }

    // seg004:07BF
    fun chomped() {
        if (!(gs.fixes.fixSkeletonChomperBlood != 0 && gs.Char.charid == CID.SKELETON)) {
            gs.currRoomModif[gs.currTilepos] = gs.currRoomModif[gs.currTilepos] or 0x80 // put blood
        }
        if (gs.Char.frame != FID.frame_178_chomped && gs.Char.room == gs.currRoom.toInt()) {
            if (gs.fixes.fixOffscreenGuardsDisappearing != 0) {
                var chomperCol = gs.tileCol.toInt()
                if (gs.currRoom.toInt() != gs.Char.room) {
                    if (gs.currRoom.toInt() == gs.level.roomlinks[gs.Char.room - 1].right) {
                        chomperCol += TG.SCREEN_TILECOUNTX
                    } else if (gs.currRoom.toInt() == gs.level.roomlinks[gs.Char.room - 1].left) {
                        chomperCol -= TG.SCREEN_TILECOUNTX
                    }
                }
                gs.Char.x = gs.xBump[chomperCol + TG.FIRST_ONSCREEN_COLUMN] + TG.TILE_MIDX
            } else {
                gs.Char.x = gs.xBump[gs.tileCol.toInt() + TG.FIRST_ONSCREEN_COLUMN] + TG.TILE_MIDX
            }
            gs.Char.x = seg006.charDxForward(7 - if (gs.Char.direction == 0) 1 else 0)
            gs.Char.y = gs.yLand[gs.Char.currRow + 1].toInt()
            seg006.takeHp(100)
            stubs.playSound(Snd.CHOMPED) // something chomped
            stubs.seqtblOffsetChar(Seq.seq_54_chomped) // chomped
            seg006.playSeq()
        }
    }

    // seg004:0833
    fun checkGatePush() {
        // Closing gate pushes Kid
        val frame = gs.Char.frame
        if (gs.Char.action == Act.TURN ||
            frame == FID.frame_15_stand || // stand
            (frame >= FID.frame_108_fall_land_2 && frame < 111) // crouch
        ) {
            seg006.getTileAtChar()
            val origCol = gs.tileCol.toInt()
            val origRoom = gs.currRoom.toInt()
            if ((gs.currTile2 == T.GATE ||
                seg006.getTile(gs.currRoom.toInt(), gs.tileCol.toInt() - 1, gs.tileRow.toInt()) == T.GATE) &&
                (gs.currRowCollFlags[gs.tileCol.toInt()] and gs.prevCollFlags[gs.tileCol.toInt()]) == 0xFF &&
                canBumpIntoGate() != 0
            ) {
                bumpedSound()
                if (gs.fixes.fixCapedPrinceSlidingThroughGate != 0) {
                    // If get_tile() changed curr_room to the left neighbor
                    if (gs.currRoom.toInt() == gs.level.roomlinks[origRoom - 1].left) {
                        gs.tileCol = (gs.tileCol - 10).toShort()
                        gs.currRoom = origRoom.toShort()
                    }
                }
                // push Kid left if origCol <= tileCol, push right if origCol > tileCol
                gs.Char.x += 5 - (if (origCol <= gs.tileCol.toInt()) 10 else 0)
            }
        }
    }

    // seg004:08C3
    fun checkGuardBumped() {
        if (gs.Char.action == Act.RUN_JUMP &&
            gs.Char.alive < 0 &&
            gs.Char.sword >= Sword.DRAWN
        ) {
            if (
                (gs.fixes.fixPushGuardIntoWall != 0 && seg006.getTileBehindChar() == T.WALL) ||
                seg006.getTileAtChar() == T.WALL ||
                gs.currTile2 == T.DOORTOP_WITH_FLOOR ||
                (gs.currTile2 == T.GATE && canBumpIntoGate() != 0) ||
                (gs.Char.direction >= Dir.RIGHT && (
                    seg006.getTile(gs.currRoom.toInt(), gs.tileCol.toInt() - 1, gs.tileRow.toInt()) == T.DOORTOP_WITH_FLOOR ||
                    (gs.currTile2 == T.GATE && canBumpIntoGate() != 0)
                ))
            ) {
                loadFrameToObj()
                seg006.setCharCollision()
                if (isObstacle() != 0) {
                    val deltaX = distFromWallBehind(gs.currTile2)
                    if (deltaX < 0 && deltaX > -13) {
                        gs.Char.x = seg006.charDxForward(-deltaX)
                        stubs.seqtblOffsetChar(Seq.seq_65_bump_forward_with_sword)
                        seg006.playSeq()
                        seg006.loadFramDetCol()
                    }
                }
            }
        }
    }

    // seg004:0989
    fun checkChompedGuard() {
        seg006.getTileAtChar()
        if (checkChompedHere() == 0) {
            seg006.getTile(gs.currRoom.toInt(), gs.tileCol.toInt() + 1, gs.tileRow.toInt())
            checkChompedHere()
        }
    }

    // seg004:09B0
    fun checkChompedHere(): Int {
        if (gs.currTile2 == T.CHOMPER &&
            (gs.currRoomModif[gs.currTilepos] and 0x7F) == 2
        ) {
            collTileLeftXpos = gs.xBump[gs.tileCol.toInt() + TG.FIRST_ONSCREEN_COLUMN] + TG.TILE_MIDX
            if (getLeftWallXpos(gs.currRoom.toInt(), gs.tileCol.toInt(), gs.tileRow.toInt()) < gs.charXRightColl.toInt() &&
                getRightWallXpos(gs.currRoom.toInt(), gs.tileCol.toInt(), gs.tileRow.toInt()) > gs.charXLeftColl.toInt()
            ) {
                chomped()
                return 1
            } else {
                return 0
            }
        } else {
            return 0
        }
    }

    // seg004:0A10
    fun distFromWallForward(tiletype: Int): Int {
        if (tiletype == T.GATE && canBumpIntoGate() == 0) {
            return -1
        }
        collTileLeftXpos = gs.xBump[gs.tileCol.toInt() + TG.FIRST_ONSCREEN_COLUMN] + TG.TILE_MIDX
        val type = seg006.wallType(tiletype)
        if (type == 0) return -1
        return if (gs.Char.direction < Dir.RIGHT) {
            // looking left
            gs.charXLeftColl.toInt() - (collTileLeftXpos + TG.TILE_RIGHTX - wallDistFromRight[type])
        } else {
            // looking right
            wallDistFromLeft[type] + collTileLeftXpos - gs.charXRightColl.toInt()
        }
    }

    // seg004:0A7B
    fun distFromWallBehind(tiletype: Int): Int {
        val type = seg006.wallType(tiletype)
        if (type == 0) {
            return 99
        }
        return if (gs.Char.direction >= Dir.RIGHT) {
            // looking right
            gs.charXLeftColl.toInt() - (collTileLeftXpos + TG.TILE_RIGHTX - wallDistFromRight[type])
        } else {
            // looking left
            wallDistFromLeft[type] + collTileLeftXpos - gs.charXRightColl.toInt()
        }
    }

    // --- load_frame_to_obj (from seg008) — inlined here for seg004 collision use ---
    private fun loadFrameToObj() {
        seg006.resetObjClip()
        seg006.loadFrame()
        gs.objDirection = gs.Char.direction
        gs.objId = gs.curFrame.image
        gs.objChtab = 2 + (gs.curFrame.sword shr 6) // chtab_base = id_chtab_2_kid
        gs.objX = ((seg006.charDxForward(gs.curFrame.dx) shl 1) - 116).toShort()
        gs.objY = gs.curFrame.dy + gs.Char.y
        // (sbyte)(cur_frame.flags ^ obj_direction) >= 0 means same sign bit
        if (((gs.curFrame.flags xor gs.objDirection).toByte().toInt()) >= 0) {
            gs.objX = (gs.objX + 1).toShort()
        }
    }
}
