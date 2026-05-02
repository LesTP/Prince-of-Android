package com.sdlpop.game

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

import com.sdlpop.game.Tiles as T
import com.sdlpop.game.Directions as Dir
import com.sdlpop.game.Actions as Act
import com.sdlpop.game.FrameIds as FID
import com.sdlpop.game.SwordStatus as Sword
import com.sdlpop.game.TileGeometry as TG
import com.sdlpop.game.EdgeType as ET
import com.sdlpop.game.Control as Ctrl

class Seg004Test {
    private val gs = GameState
    private val seg004 = Seg004

    @BeforeEach
    fun resetState() {
        // Reset GameState
        gs.Char.room = 1
        gs.Char.currRow = 0
        gs.Char.currCol = 5
        gs.Char.x = 100
        gs.Char.y = 55
        gs.Char.direction = Dir.RIGHT
        gs.Char.action = Act.STAND
        gs.Char.frame = FID.frame_15_stand
        gs.Char.alive = -1
        gs.Char.sword = Sword.SHEATHED
        gs.Char.charid = 0 // kid
        gs.Char.fallX = 0
        gs.Char.fallY = 0
        gs.drawnRoom = 1
        gs.currRoom = 1
        gs.tileCol = 5
        gs.tileRow = 0
        gs.currTile2 = T.FLOOR
        gs.currTilepos = 5
        gs.collisionRow = 0
        gs.prevCollisionRow = 0
        gs.charXLeftColl = 90
        gs.charXRightColl = 110
        gs.charHeight = 50
        gs.isGuardNotice = 0
        gs.jumpedThroughMirror = 0
        gs.edgeType = 0
        gs.controlShift = 0
        gs.infrontx = 6
        gs.roomL = 0
        gs.roomR = 0
        gs.roomBL = 0
        gs.roomBR = 0

        // Reset seg004 locals
        seg004.bumpColLeftOfWall = -1
        seg004.bumpColRightOfWall = -1
        seg004.rightCheckedCol = 0
        seg004.leftCheckedCol = 0
        seg004.collTileLeftXpos = 0

        // Clear collision arrays
        gs.prevCollRoom.fill(-1)
        gs.currRowCollRoom.fill(-1)
        gs.belowRowCollRoom.fill(-1)
        gs.aboveRowCollRoom.fill(-1)
        gs.prevCollFlags.fill(0)
        gs.currRowCollFlags.fill(0)
        gs.belowRowCollFlags.fill(0)
        gs.aboveRowCollFlags.fill(0)

        // Clear room tiles/modifiers to floor
        gs.currRoomTiles.fill(T.FLOOR)
        gs.currRoomModif.fill(0)

        // Initialize level room links
        for (i in gs.level.roomlinks.indices) {
            gs.level.roomlinks[i] = LinkType()
        }

        // Reset fixes to defaults (disabled)
        gs.fixes = FixesOptionsType()
    }

    // --- clearCollRooms ---

    @Test
    fun clearCollRooms_resetsAllArraysAndPrevRow() {
        gs.prevCollRoom[3] = 5
        gs.currRowCollRoom[7] = 2
        gs.belowRowCollRoom[1] = 3
        gs.aboveRowCollRoom[9] = 1
        gs.prevCollisionRow = 2

        seg004.clearCollRooms()

        assertTrue(gs.prevCollRoom.all { it == -1 })
        assertTrue(gs.currRowCollRoom.all { it == -1 })
        assertTrue(gs.belowRowCollRoom.all { it == -1 })
        assertTrue(gs.aboveRowCollRoom.all { it == -1 })
        assertEquals(-1, gs.prevCollisionRow)
    }

    @Test
    fun clearCollRooms_withFixCollFlags_alsoResetsFlags() {
        gs.fixes = FixesOptionsType(fixCollFlags = 1)
        gs.prevCollFlags[0] = 0xFF
        gs.currRowCollFlags[5] = 0x0F
        gs.belowRowCollFlags[3] = 0xF0
        gs.aboveRowCollFlags[8] = 0xFF

        seg004.clearCollRooms()

        assertTrue(gs.prevCollFlags.all { it == 0 })
        assertTrue(gs.currRowCollFlags.all { it == 0 })
        assertTrue(gs.belowRowCollFlags.all { it == 0 })
        assertTrue(gs.aboveRowCollFlags.all { it == 0 })
    }

    @Test
    fun clearCollRooms_withoutFixCollFlags_preservesFlags() {
        gs.fixes = FixesOptionsType(fixCollFlags = 0)
        gs.currRowCollFlags[5] = 0x0F

        seg004.clearCollRooms()

        assertEquals(0x0F, gs.currRowCollFlags[5])
    }

    // --- moveCollToPrev ---

    @Test
    fun moveCollToPrev_sameRow_copiesCurrToPreg() {
        gs.collisionRow = 1
        gs.prevCollisionRow = 1
        gs.currRowCollRoom[3] = 5
        gs.currRowCollFlags[3] = 0x0F

        seg004.moveCollToPrev()

        assertEquals(5, gs.prevCollRoom[3])
        assertEquals(0x0F, gs.prevCollFlags[3])
        assertEquals(-1, gs.currRowCollRoom[3]) // cleared after copy
    }

    // --- wallDistFromLeft / wallDistFromRight ---

    @Test
    fun wallDistTables_correctValues() {
        assertEquals(0, seg004.wallDistFromLeft[0])
        assertEquals(10, seg004.wallDistFromLeft[1])
        assertEquals(0, seg004.wallDistFromLeft[2])
        assertEquals(-1, seg004.wallDistFromLeft[3])

        assertEquals(0, seg004.wallDistFromRight[0])
        assertEquals(0, seg004.wallDistFromRight[1])
        assertEquals(10, seg004.wallDistFromRight[2])
        assertEquals(13, seg004.wallDistFromRight[3])
    }

    // --- getLeftWallXpos / getRightWallXpos ---

    @Test
    fun getLeftWallXpos_noWall_returns0xFF() {
        // Floor tile — wallType returns 0
        setupRoom(1, 5, 0, T.FLOOR)
        seg004.collTileLeftXpos = 79

        val result = seg004.getLeftWallXpos(1, 5, 0)
        assertEquals(0xFF, result)
    }

    @Test
    fun getLeftWallXpos_wall_returnsDistPlusXpos() {
        setupRoom(1, 5, 0, T.WALL)
        seg004.collTileLeftXpos = 79

        val result = seg004.getLeftWallXpos(1, 5, 0)
        // wallType(WALL) = 4, wallDistFromLeft[4] = 0
        // result = 0 + 79 = 79
        assertEquals(79, result)
    }

    @Test
    fun getRightWallXpos_noWall_returns0() {
        setupRoom(1, 5, 0, T.FLOOR)
        seg004.collTileLeftXpos = 79

        val result = seg004.getRightWallXpos(1, 5, 0)
        assertEquals(0, result)
    }

    @Test
    fun getRightWallXpos_wall_returnsXposPlusRightMinusDist() {
        setupRoom(1, 5, 0, T.WALL)
        seg004.collTileLeftXpos = 79

        val result = seg004.getRightWallXpos(1, 5, 0)
        // wallType(WALL) = 4, wallDistFromRight[4] = 0
        assertEquals(79 - 0 + TG.TILE_RIGHTX, result)
    }

    // --- canBumpIntoGate ---

    @Test
    fun canBumpIntoGate_gateOpenEnough_returns1() {
        gs.currTilepos = 5
        gs.currRoomModif[5] = 0 // fully open: (0 >> 2) + 6 = 6
        gs.charHeight = 50
        // 6 < 50 → can bump
        assertEquals(1, seg004.canBumpIntoGate())
    }

    @Test
    fun canBumpIntoGate_charTooShort_returns0() {
        gs.currTilepos = 5
        gs.currRoomModif[5] = 0
        gs.charHeight = 5
        // (0 >> 2) + 6 = 6, 6 < 5 is false
        assertEquals(0, seg004.canBumpIntoGate())
    }

    // --- isObstacle ---

    @Test
    fun isObstacle_potion_returns0() {
        gs.currTile2 = T.POTION
        assertEquals(0, seg004.isObstacle())
    }

    @Test
    fun isObstacle_gate_openGate_returns0() {
        gs.currTile2 = T.GATE
        gs.currTilepos = 5
        gs.currRoomModif[5] = 0
        gs.charHeight = 5 // too short to bump
        assertEquals(0, seg004.isObstacle())
    }

    @Test
    fun isObstacle_chomper_notClosed_returns0() {
        gs.currTile2 = T.CHOMPER
        gs.currTilepos = 5
        gs.currRoomModif[5] = 1 // not closed (closed = 2)
        assertEquals(0, seg004.isObstacle())
    }

    @Test
    fun isObstacle_mirror_runJumpLeftToRight_breaksMirror() {
        gs.currTile2 = T.MIRROR
        gs.Char.charid = 0 // kid
        gs.Char.frame = FID.frame_39_start_run_jump_6
        gs.Char.direction = Dir.LEFT // right-to-left
        gs.currTilepos = 5

        assertEquals(0, seg004.isObstacle())
        assertEquals(0x56, gs.currRoomModif[5])
        assertEquals(-1, gs.jumpedThroughMirror.toInt())
    }

    @Test
    fun isObstacle_wall_returns1() {
        gs.currTile2 = T.WALL
        gs.tileCol = 5
        gs.drawnRoom = 1
        gs.currRoom = 1

        assertEquals(1, seg004.isObstacle())
    }

    // --- xposInDrawnRoom ---

    @Test
    fun xposInDrawnRoom_sameRoom_noChange() {
        gs.currRoom = 1
        gs.drawnRoom = 1
        assertEquals(100, seg004.xposInDrawnRoom(100))
    }

    @Test
    fun xposInDrawnRoom_leftRoom_subtractsWidth() {
        gs.currRoom = 2
        gs.drawnRoom = 1
        gs.roomL = 2
        val result = seg004.xposInDrawnRoom(100)
        assertEquals(100 - TG.TILE_SIZEX * TG.SCREEN_TILECOUNTX, result)
    }

    @Test
    fun xposInDrawnRoom_rightRoom_addsWidth() {
        gs.currRoom = 3
        gs.drawnRoom = 1
        gs.roomR = 3
        val result = seg004.xposInDrawnRoom(100)
        assertEquals(100 + TG.TILE_SIZEX * TG.SCREEN_TILECOUNTX, result)
    }

    // --- bumpedSound ---

    @Test
    fun bumpedSound_setsGuardNotice() {
        gs.isGuardNotice = 0
        seg004.bumpedSound()
        assertEquals(1, gs.isGuardNotice)
    }

    // --- bumpedFall ---

    @Test
    fun bumpedFall_inFreefall_clearsX() {
        gs.Char.action = Act.IN_FREEFALL
        gs.Char.fallX = 5
        gs.Char.x = 100
        gs.Char.direction = Dir.RIGHT

        seg004.bumpedFall()

        assertEquals(0, gs.Char.fallX)
        assertEquals(1, gs.isGuardNotice)
    }

    @Test
    fun bumpedFall_notFreefall_setsSeqAndSound() {
        gs.Char.action = Act.RUN_JUMP
        gs.Char.x = 100
        gs.Char.direction = Dir.RIGHT

        seg004.bumpedFall()

        assertEquals(1, gs.isGuardNotice) // bumped_sound was called
    }

    // --- bumped ---

    @Test
    fun bumped_deadChar_noAction() {
        gs.Char.alive = 0 // dead
        gs.Char.x = 100
        seg004.bumped(5, Dir.RIGHT)
        assertEquals(100, gs.Char.x) // x unchanged
    }

    @Test
    fun bumped_spikedFrame_noAction() {
        gs.Char.alive = -1
        gs.Char.frame = FID.frame_177_spiked
        gs.Char.x = 100
        seg004.bumped(5, Dir.RIGHT)
        assertEquals(100, gs.Char.x) // x unchanged
    }

    @Test
    fun bumped_alive_movesChar() {
        gs.Char.alive = -1
        gs.Char.frame = FID.frame_15_stand
        gs.Char.x = 100
        gs.Char.y = 55 // matches yLand[1]
        gs.Char.currRow = 0
        gs.Char.direction = Dir.RIGHT
        gs.Char.fallY = 0
        gs.currTile2 = T.FLOOR

        seg004.bumped(5, Dir.RIGHT)

        // x starts at 100 + 5 = 105 from deltaX, then bumpedFloor + playSeq may modify further
        // Key verification: bumpedSound was called (sets isGuardNotice)
        assertEquals(1, gs.isGuardNotice)
        assertNotEquals(100, gs.Char.x) // x was modified
    }

    // --- checkBumped ---

    @Test
    fun checkBumped_hangClimb_noAction() {
        gs.Char.action = Act.HANG_CLIMB
        seg004.bumpColLeftOfWall = 3
        // Should not call check_bumped_look_right
        seg004.checkBumped() // no crash = pass
    }

    @Test
    fun checkBumped_climbingFrame_noAction() {
        gs.Char.action = Act.STAND
        gs.Char.frame = FID.frame_135_climbing_1 // climbing
        seg004.bumpColLeftOfWall = 3
        seg004.checkBumped() // no crash = pass
    }

    // --- distFromWallForward ---

    @Test
    fun distFromWallForward_noWall_returnsNeg1() {
        gs.tileCol = 5
        val result = seg004.distFromWallForward(T.FLOOR)
        assertEquals(-1, result)
    }

    @Test
    fun distFromWallForward_wall_lookingRight() {
        gs.tileCol = 5
        gs.Char.direction = Dir.RIGHT
        gs.charXRightColl = 100

        val result = seg004.distFromWallForward(T.WALL)
        // wallType(WALL) = 4, wallDistFromLeft[4] = 0
        // collTileLeftXpos = xBump[5+5] + 7 = 128+7 = 135
        // result = 0 + 135 - 100 = 35
        assertEquals(35, result)
    }

    @Test
    fun distFromWallForward_gate_cantBump_returnsNeg1() {
        gs.tileCol = 5
        gs.currTilepos = 5
        gs.currRoomModif[5] = 0
        gs.charHeight = 5 // too short
        val result = seg004.distFromWallForward(T.GATE)
        assertEquals(-1, result)
    }

    // --- distFromWallBehind ---

    @Test
    fun distFromWallBehind_noWall_returns99() {
        val result = seg004.distFromWallBehind(T.FLOOR)
        assertEquals(99, result)
    }

    @Test
    fun distFromWallBehind_wall_lookingRight() {
        gs.Char.direction = Dir.RIGHT
        gs.charXLeftColl = 90
        seg004.collTileLeftXpos = 79

        val result = seg004.distFromWallBehind(T.WALL)
        // wallType(WALL) = 1, wallDistFromRight[1] = 0
        // looking right: charXLeftColl - (collTileLeftXpos + TILE_RIGHTX - wallDistFromRight[1])
        // = 90 - (79 + 13 - 0) = 90 - 92 = -2
        assertEquals(-2, result)
    }

    // --- chomped ---

    @Test
    fun chomped_addBlood() {
        gs.currTilepos = 5
        gs.currRoomModif[5] = 2
        gs.Char.frame = FID.frame_15_stand
        gs.Char.room = 1
        gs.currRoom = 1
        gs.Char.currRow = 0
        gs.Char.direction = Dir.RIGHT
        gs.tileCol = 5

        seg004.chomped()

        assertEquals(0x82, gs.currRoomModif[5]) // blood added
    }

    @Test
    fun chomped_skeleton_noBlood_withFix() {
        gs.fixes = FixesOptionsType(fixSkeletonChomperBlood = 1)
        gs.currTilepos = 5
        gs.currRoomModif[5] = 2
        gs.Char.charid = 4 // skeleton
        gs.Char.frame = FID.frame_15_stand
        gs.Char.room = 1
        gs.currRoom = 1
        gs.Char.currRow = 0
        gs.Char.direction = Dir.RIGHT
        gs.tileCol = 5

        seg004.chomped()

        assertEquals(2, gs.currRoomModif[5]) // no blood
    }

    @Test
    fun chomped_alreadyChompedFrame_noReposition() {
        gs.currTilepos = 5
        gs.currRoomModif[5] = 2
        gs.Char.frame = FID.frame_178_chomped
        gs.Char.room = 1
        gs.currRoom = 1
        gs.Char.x = 100

        seg004.chomped()

        assertEquals(100, gs.Char.x) // position unchanged (except blood)
    }

    // --- checkGatePush ---

    @Test
    fun checkGatePush_standingFrame_checksGate() {
        gs.Char.action = Act.STAND
        gs.Char.frame = FID.frame_15_stand
        gs.Char.room = 1
        // Set tile at char position to gate
        setupRoom(1, 5, 0, T.GATE)
        gs.Char.currRow = 0
        gs.Char.currCol = 5
        gs.Char.x = 100

        // Collision flags: both current and prev must be 0xFF
        gs.currRowCollFlags[5] = 0xFF
        gs.prevCollFlags[5] = 0xFF
        gs.charHeight = 50 // can bump into gate
        gs.currRoomModif[5] = 0

        seg004.checkGatePush()

        // Should have called bumpedSound and modified x
        assertEquals(1, gs.isGuardNotice)
    }

    // --- checkChompedHere ---

    @Test
    fun checkChompedHere_notChomper_returns0() {
        gs.currTile2 = T.FLOOR
        assertEquals(0, seg004.checkChompedHere())
    }

    @Test
    fun checkChompedHere_openChomper_returns0() {
        gs.currTile2 = T.CHOMPER
        gs.currTilepos = 5
        gs.currRoomModif[5] = 1 // not closed
        assertEquals(0, seg004.checkChompedHere())
    }

    // --- isObstacleAtCol ---

    @Test
    fun isObstacleAtCol_negativeCurrRow_wraps() {
        gs.Char.currRow = -1
        gs.currRowCollRoom[5] = 1
        setupRoom(1, 5, 2, T.WALL) // row 2 = currRow(-1) + 3

        val result = seg004.isObstacleAtCol(5)
        assertEquals(1, result)
    }

    @Test
    fun isObstacleAtCol_rowAbove3_wraps() {
        gs.Char.currRow = 3
        gs.currRowCollRoom[5] = 1
        setupRoom(1, 5, 0, T.WALL) // row 0 = currRow(3) - 3

        val result = seg004.isObstacleAtCol(5)
        assertEquals(1, result)
    }

    // --- checkCollisions ---

    @Test
    fun checkCollisions_turnAction_earlyReturn() {
        gs.Char.action = Act.TURN
        seg004.bumpColLeftOfWall = 5 // should be reset
        seg004.bumpColRightOfWall = 3

        seg004.checkCollisions()

        assertEquals(-1, seg004.bumpColLeftOfWall)
        assertEquals(-1, seg004.bumpColRightOfWall)
    }

    // --- Constant verification ---

    @Test
    fun wallDistTables_size() {
        assertEquals(6, seg004.wallDistFromLeft.size)
        assertEquals(6, seg004.wallDistFromRight.size)
    }

    // Helper: set a tile at a specific room/col/row position
    private fun setupRoom(room: Int, col: Int, row: Int, tile: Int) {
        if (room in 1..24 && col in 0..9 && row in 0..2) {
            val base = (room - 1) * 30
            gs.level.fg[base + row * 10 + col] = tile
        }
        // Also load via getRoomAddress so currRoomTiles gets updated
        ExternalStubs.getRoomAddress(room)
    }
}
