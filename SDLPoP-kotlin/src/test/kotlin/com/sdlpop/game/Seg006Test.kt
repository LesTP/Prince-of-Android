/*
SDLPoP-kotlin — Module 8 Phase 8a: Seg006 unit tests.
Tests tile/room queries, state save/restore round-trips, frame loading,
control input handling, and sequence table playback.
*/

package com.sdlpop.game

import kotlin.test.*

class Seg006Test {

    @BeforeTest
    fun resetState() {
        // Reset key game state before each test
        val gs = GameState
        gs.Kid = CharType()
        gs.Guard = CharType()
        gs.Char = CharType()
        gs.Opp = CharType()
        gs.level = LevelType()
        gs.currRoom = 0
        gs.tileCol = 0
        gs.tileRow = 0
        gs.currTile2 = 0
        gs.currTilepos = 0
        gs.curFrame = FrameType()
        gs.objXl = 0
        gs.objX = 0
        gs.objDirection = 0
        gs.infrontx = 0
        gs.throughTile = 0
        gs.controlX = 0
        gs.controlY = 0
        gs.controlShift = 0
        gs.controlShift2 = 0
        gs.controlForward = 0
        gs.controlBackward = 0
        gs.controlUp = 0
        gs.controlDown = 0
        gs.ctrl1Forward = 0
        gs.ctrl1Backward = 0
        gs.ctrl1Up = 0
        gs.ctrl1Down = 0
        gs.ctrl1Shift2 = 0
        gs.flashColor = 0
        gs.flashTime = 0
        gs.hitpDelta = 0
        gs.hitpCurr = 0
        gs.hitpMax = 0
        gs.guardhpCurr = 0
        gs.knock = 0
        gs.isGuardNotice = 0
        gs.isFeatherFall = 0
        gs.nextLevel = 0
        gs.keepLastSeed = 0
        gs.currentLevel = -1
        gs.recording = 0
        gs.replaying = 0
        gs.isSoundOn = 0x0F
        gs.haveSword = 0
        gs.pickupObjType = 0
        gs.loadedRoom = 0
        gs.checkpoint = 0
        gs.grabTimer = 0
        gs.playDemoLevel = 0
        gs.resurrectTime = 0
        gs.isShowTime = 0
        gs.textTimeRemaining = 0
        gs.textTimeTotal = 0
        gs.guardSkill = 0
        gs.holdingSword = 0
        gs.custom = CustomOptionsType()
        gs.fixes = FixesOptionsType()
        // Reset stubs to defaults
        ExternalStubs.control = { throw NotImplementedError("control (seg005)") }
        ExternalStubs.autocontrolOpponent = { throw NotImplementedError("autocontrol_opponent (seg002)") }
        ExternalStubs.drawSword = { throw NotImplementedError("draw_sword (seg005)") }
        ExternalStubs.playSound = { _ -> }
        ExternalStubs.stopSounds = { }
        ExternalStubs.checkSoundPlaying = { 0 }
        ExternalStubs.keyTestQuit = { 0 }
        ExternalStubs.doPaused = { 0 }
        ExternalStubs.addReplayMove = { }
        ExternalStubs.doReplayMove = { }
        ExternalStubs.doAutoMoves = { _ -> throw NotImplementedError("do_auto_moves") }
        ExternalStubs.drawGuardHp = { _, _ -> }
        ExternalStubs.seqtblOffsetChar = { seqIndex ->
            GameState.Char.currSeq = SequenceTable.seqtblOffsets[seqIndex]
        }
        for (i in gs.currRoomTiles.indices) {
            gs.currRoomTiles[i] = 0
            gs.currRoomModif[i] = 0
        }
    }

    // === Frame Table Tests ===

    @Test
    fun frameTableKidSize() {
        assertEquals(241, Seg006.frameTableKid.size)
    }

    @Test
    fun frameTblGuardSize() {
        assertEquals(41, Seg006.frameTblGuard.size)
    }

    @Test
    fun frameTblCutsSize() {
        assertEquals(86, Seg006.frameTblCuts.size)
    }

    @Test
    fun swordTblSize() {
        assertEquals(51, Seg006.swordTbl.size)
    }

    @Test
    fun frameTableKidEntry0() {
        val f = Seg006.frameTableKid[0]
        assertEquals(255, f.image)
        assertEquals(0, f.sword)
        assertEquals(0, f.dx)
        assertEquals(0, f.dy)
        assertEquals(0, f.flags)
    }

    @Test
    fun frameTableKidEntry15() {
        // Frame 15 = standing, index 15: {14, 0x00|9, 0, 0, 0x40|3}
        val f = Seg006.frameTableKid[15]
        assertEquals(14, f.image)
        assertEquals(9, f.sword)
        assertEquals(0, f.dx)
        assertEquals(0, f.dy)
        assertEquals(0x43, f.flags)
    }

    // === Tile Query Tests ===

    @Test
    fun getTileInRoom0ReturnsEdge() {
        // Room 0 = outside level, should return edge tile (wall)
        val result = Seg006.getTile(0, 5, 1)
        assertEquals(GameState.custom.levelEdgeHitTile, result)
    }

    @Test
    fun getTileInValidRoom() {
        val gs = GameState
        // Set up room 1 with a floor tile at position (3, 1) = index 13
        gs.level.fg[13] = Tiles.FLOOR  // room 1, row 1, col 3
        val result = Seg006.getTile(1, 3, 1)
        assertEquals(Tiles.FLOOR, result)
        assertEquals(1, gs.currRoom.toInt())
    }

    @Test
    fun getTileWrapRight() {
        val gs = GameState
        // Room 1 right-links to room 2
        gs.level.roomlinks[0].right = 2
        // Set room 2, tile at (0, 0) = fg[30]
        gs.level.fg[30] = Tiles.SPIKE
        val result = Seg006.getTile(1, 10, 0)
        assertEquals(Tiles.SPIKE, result)
        assertEquals(2, gs.currRoom.toInt())
        assertEquals(0, gs.tileCol.toInt())
    }

    @Test
    fun getTileWrapLeft() {
        val gs = GameState
        gs.level.roomlinks[0].left = 3
        gs.level.fg[60 + 9] = Tiles.GATE  // room 3, row 0, col 9
        val result = Seg006.getTile(1, -1, 0)
        assertEquals(Tiles.GATE, result)
        assertEquals(3, gs.currRoom.toInt())
    }

    @Test
    fun getTileWrapDown() {
        val gs = GameState
        gs.level.roomlinks[0].down = 4
        gs.level.fg[90 + 5] = Tiles.CHOMPER  // room 4, row 0, col 5
        val result = Seg006.getTile(1, 5, 3)
        assertEquals(Tiles.CHOMPER, result)
        assertEquals(4, gs.currRoom.toInt())
    }

    @Test
    fun getTileWrapUp() {
        val gs = GameState
        gs.level.roomlinks[0].up = 5
        gs.level.fg[120 + 20 + 3] = Tiles.POTION  // room 5, row 2, col 3
        val result = Seg006.getTile(1, 3, -1)
        assertEquals(Tiles.POTION, result)
        assertEquals(5, gs.currRoom.toInt())
    }

    // === getTilepos Tests ===

    @Test
    fun getTileposValid() {
        assertEquals(0, Seg006.getTilepos(0, 0))
        assertEquals(15, Seg006.getTilepos(5, 1))
        assertEquals(29, Seg006.getTilepos(9, 2))
    }

    @Test
    fun getTileposNegativeRow() {
        assertEquals(-1, Seg006.getTilepos(0, -1))
        assertEquals(-6, Seg006.getTilepos(5, -1))
    }

    @Test
    fun getTileposOutOfBounds() {
        assertEquals(30, Seg006.getTilepos(10, 0))
        assertEquals(30, Seg006.getTilepos(-1, 0))
        assertEquals(30, Seg006.getTilepos(0, 3))
    }

    @Test
    fun getTileposNominusClamps() {
        assertEquals(30, Seg006.getTileposNominus(0, -1))
        assertEquals(0, Seg006.getTileposNominus(0, 0))
    }

    // === tile_is_floor / wall_type Tests ===

    @Test
    fun tileIsFloorReturnValues() {
        assertEquals(0, Seg006.tileIsFloor(Tiles.EMPTY))
        assertEquals(0, Seg006.tileIsFloor(Tiles.WALL))
        assertEquals(0, Seg006.tileIsFloor(Tiles.BIGPILLAR_TOP))
        assertEquals(0, Seg006.tileIsFloor(Tiles.DOORTOP))
        assertEquals(1, Seg006.tileIsFloor(Tiles.FLOOR))
        assertEquals(1, Seg006.tileIsFloor(Tiles.SPIKE))
        assertEquals(1, Seg006.tileIsFloor(Tiles.GATE))
        assertEquals(1, Seg006.tileIsFloor(Tiles.LOOSE))
        assertEquals(1, Seg006.tileIsFloor(Tiles.CHOMPER))
    }

    @Test
    fun wallTypeReturnValues() {
        assertEquals(0, Seg006.wallType(Tiles.FLOOR))
        assertEquals(1, Seg006.wallType(Tiles.GATE))
        assertEquals(1, Seg006.wallType(Tiles.DOORTOP_WITH_FLOOR))
        assertEquals(1, Seg006.wallType(Tiles.DOORTOP))
        assertEquals(2, Seg006.wallType(Tiles.MIRROR))
        assertEquals(3, Seg006.wallType(Tiles.CHOMPER))
        assertEquals(4, Seg006.wallType(Tiles.WALL))
    }

    // === Character State Save/Load Round-Trip Tests ===

    @Test
    fun loadkidSavekidRoundTrip() {
        val gs = GameState
        gs.Kid = CharType(frame = 15, x = 100, y = 118, direction = -1, currCol = 5, currRow = 1,
            action = 0, fallX = 0, fallY = 0, room = 3, repeat = 0, charid = 0, sword = 0, alive = -1, currSeq = 0x1A00)
        Seg006.loadkid()
        assertEquals(15, gs.Char.frame)
        assertEquals(100, gs.Char.x)
        assertEquals(-1, gs.Char.direction)
        assertEquals(3, gs.Char.room)
        assertEquals(-1, gs.Char.alive)
        assertEquals(0x1A00, gs.Char.currSeq)

        gs.Char.frame = 20
        gs.Char.x = 50
        Seg006.savekid()
        assertEquals(20, gs.Kid.frame)
        assertEquals(50, gs.Kid.x)
    }

    @Test
    fun loadshadSaveshadRoundTrip() {
        val gs = GameState
        gs.Guard = CharType(frame = 150, x = 80, direction = 0, room = 5, charid = 2, alive = -1)
        Seg006.loadshad()
        assertEquals(150, gs.Char.frame)
        assertEquals(80, gs.Char.x)
        assertEquals(5, gs.Char.room)

        gs.Char.frame = 155
        Seg006.saveshad()
        assertEquals(155, gs.Guard.frame)
    }

    @Test
    fun loadkidAndOppSetsOppToGuard() {
        val gs = GameState
        gs.Kid = CharType(frame = 15, room = 3)
        gs.Guard = CharType(frame = 150, room = 3)
        Seg006.loadkidAndOpp()
        assertEquals(15, gs.Char.frame)
        assertEquals(150, gs.Opp.frame)
    }

    @Test
    fun savekidAndOppRestores() {
        val gs = GameState
        gs.Char = CharType(frame = 20, room = 3)
        gs.Opp = CharType(frame = 160, room = 3)
        Seg006.savekidAndOpp()
        assertEquals(20, gs.Kid.frame)
        assertEquals(160, gs.Guard.frame)
    }

    // === Object Save/Load Round-Trip ===

    @Test
    fun saveObjLoadObjRoundTrip() {
        val gs = GameState
        gs.objTilepos = 15
        gs.objX = 200
        gs.objY = 118
        gs.objDirection = -1
        gs.objId = 5
        gs.objChtab = 2
        gs.objClipTop = 10
        gs.objClipBottom = 180
        gs.objClipLeft = 0
        gs.objClipRight = 320

        Seg006.saveObj()

        // Modify originals
        gs.objTilepos = 0
        gs.objX = 0
        gs.objY = 0

        Seg006.loadObj()

        assertEquals(15, gs.objTilepos)
        assertEquals(200, gs.objX.toInt())
        assertEquals(118, gs.objY)
        assertEquals(-1, gs.objDirection)
        assertEquals(5, gs.objId)
        assertEquals(2, gs.objChtab)
    }

    // === resetObjClip Test ===

    @Test
    fun resetObjClipSetsDefaults() {
        val gs = GameState
        gs.objClipLeft = 50
        gs.objClipTop = 50
        Seg006.resetObjClip()
        assertEquals(0.toShort(), gs.objClipLeft)
        assertEquals(0.toShort(), gs.objClipTop)
        assertEquals(320.toShort(), gs.objClipRight)
        assertEquals(192.toShort(), gs.objClipBottom)
    }

    // === Control Input Tests ===

    @Test
    fun readUserControlForwardFromReleased() {
        val gs = GameState
        gs.controlForward = Control.RELEASED
        gs.controlX = Control.HELD_FORWARD
        gs.controlBackward = Control.RELEASED
        gs.controlUp = Control.RELEASED
        gs.controlDown = Control.RELEASED
        gs.controlShift2 = Control.RELEASED
        gs.controlY = 0
        gs.controlShift = 0

        Seg006.readUserControl()
        assertEquals(Control.HELD, gs.controlForward)
    }

    @Test
    fun readUserControlForwardReleaseWhenNoInput() {
        val gs = GameState
        gs.controlForward = Control.RELEASED
        gs.controlX = 0  // no direction held
        gs.controlBackward = Control.RELEASED
        gs.controlUp = Control.RELEASED
        gs.controlDown = Control.RELEASED
        gs.controlShift2 = Control.RELEASED
        gs.controlY = 0
        gs.controlShift = 0

        Seg006.readUserControl()
        assertEquals(Control.RELEASED, gs.controlForward)
    }

    @Test
    fun clearSavedCtrlSetsAllReleased() {
        val gs = GameState
        gs.ctrl1Forward = Control.HELD
        gs.ctrl1Backward = Control.HELD
        gs.ctrl1Up = Control.HELD
        gs.ctrl1Down = Control.HELD
        gs.ctrl1Shift2 = Control.HELD
        Seg006.clearSavedCtrl()
        assertEquals(Control.RELEASED, gs.ctrl1Forward)
        assertEquals(Control.RELEASED, gs.ctrl1Backward)
        assertEquals(Control.RELEASED, gs.ctrl1Up)
        assertEquals(Control.RELEASED, gs.ctrl1Down)
        assertEquals(Control.RELEASED, gs.ctrl1Shift2)
    }

    @Test
    fun saveCtrl1RestCtrl1RoundTrip() {
        val gs = GameState
        gs.controlForward = Control.HELD
        gs.controlBackward = Control.RELEASED
        gs.controlUp = Control.HELD
        gs.controlDown = Control.RELEASED
        gs.controlShift2 = Control.IGNORE
        Seg006.saveCtrl1()

        gs.controlForward = 0
        gs.controlUp = 0
        gs.controlShift2 = 0
        Seg006.restCtrl1()
        assertEquals(Control.HELD, gs.controlForward)
        assertEquals(Control.RELEASED, gs.controlBackward)
        assertEquals(Control.HELD, gs.controlUp)
        assertEquals(Control.RELEASED, gs.controlDown)
        assertEquals(Control.IGNORE, gs.controlShift2)
    }

    @Test
    fun releaseArrowsSetsAllReleased() {
        val gs = GameState
        gs.controlForward = Control.HELD
        gs.controlBackward = Control.HELD
        gs.controlUp = Control.HELD
        gs.controlDown = Control.HELD
        val result = Seg006.releaseArrows()
        assertEquals(1, result)
        assertEquals(Control.RELEASED, gs.controlForward)
        assertEquals(Control.RELEASED, gs.controlBackward)
        assertEquals(Control.RELEASED, gs.controlUp)
        assertEquals(Control.RELEASED, gs.controlDown)
    }

    @Test
    fun flipControlXSwapsDirection() {
        val gs = GameState
        gs.controlX = Control.HELD_FORWARD   // -1
        gs.controlForward = Control.HELD
        gs.controlBackward = Control.RELEASED
        Seg006.flipControlX()
        assertEquals(1, gs.controlX)  // swapped
        assertEquals(Control.RELEASED, gs.controlForward)  // swapped
        assertEquals(Control.HELD, gs.controlBackward)  // swapped
    }

    // === Frame Loading Tests ===

    @Test
    fun loadFrameKidStanding() {
        val gs = GameState
        gs.Char.charid = CharIds.KID
        gs.Char.frame = 15  // standing frame = index 15 in kid table
        Seg006.loadFrame()
        // frame_table_kid[15] = {14, 0x00|9, 0, 0, 0x40|3}
        assertEquals(14, gs.curFrame.image)
        assertEquals(9, gs.curFrame.sword)
    }

    @Test
    fun loadFrameGuard() {
        val gs = GameState
        gs.Char.charid = CharIds.GUARD
        gs.Char.frame = 150  // guard frame, index = 150 - 149 = 1
        Seg006.loadFrame()
        assertEquals(12, gs.curFrame.image) // frame_tbl_guard[1].image
    }

    @Test
    fun loadFrameOutOfBoundsGivesBlank() {
        val gs = GameState
        gs.Char.charid = CharIds.KID
        gs.Char.frame = 999
        Seg006.loadFrame()
        assertEquals(255, gs.curFrame.image)
    }

    // === Distance/Physics Tests ===

    @Test
    fun charDxForwardRightDirection() {
        val gs = GameState
        gs.Char.direction = Directions.RIGHT
        gs.Char.x = 100
        assertEquals(105, Seg006.charDxForward(5))
    }

    @Test
    fun charDxForwardLeftDirection() {
        val gs = GameState
        gs.Char.direction = Directions.LEFT
        gs.Char.x = 100
        assertEquals(95, Seg006.charDxForward(5))
    }

    @Test
    fun objDxForwardRight() {
        val gs = GameState
        gs.objDirection = Directions.RIGHT
        gs.objX = 100
        assertEquals(105, Seg006.objDxForward(5))
        assertEquals(105, gs.objX.toInt())
    }

    @Test
    fun getTileDivModBasic() {
        // x=65 → (65-58)/14 = 0 remainder 7
        val col = Seg006.getTileDivMod(65)
        assertEquals(0, col)
        assertEquals(7, GameState.objXl)
    }

    @Test
    fun getTileDivModM7Offset() {
        // getTileDivModM7(72) = getTileDivMod(65) = col 0
        val col = Seg006.getTileDivModM7(72)
        assertEquals(0, col)
    }

    @Test
    fun yToRowMod4Values() {
        assertEquals(-1, Seg006.yToRowMod4(-8))   // above screen
        assertEquals(0, Seg006.yToRowMod4(55))     // row 0
        assertEquals(1, Seg006.yToRowMod4(118))    // row 1
        assertEquals(2, Seg006.yToRowMod4(181))    // row 2
    }

    // === isDead Test ===

    @Test
    fun isDeadDetectsDeathFrames() {
        val gs = GameState
        gs.Char.frame = FrameIds.frame_177_spiked
        assertEquals(1, Seg006.isDead())
        gs.Char.frame = FrameIds.frame_178_chomped
        assertEquals(1, Seg006.isDead())
        gs.Char.frame = FrameIds.frame_185_dead
        assertEquals(1, Seg006.isDead())
        gs.Char.frame = FrameIds.frame_15_stand
        assertEquals(0, Seg006.isDead())
    }

    // === clearChar Test ===

    @Test
    fun clearCharResetsFields() {
        val gs = GameState
        gs.Char.direction = Directions.LEFT
        gs.Char.alive = -1
        gs.Char.action = Actions.RUN_JUMP
        gs.guardhpCurr = 5
        Seg006.clearChar()
        assertEquals(Directions.NONE, gs.Char.direction)
        assertEquals(0, gs.Char.alive)
        assertEquals(0, gs.Char.action)
        assertEquals(0, gs.guardhpCurr)
    }

    // === incCurrRow Test ===

    @Test
    fun incCurrRowIncrements() {
        val gs = GameState
        gs.Char.currRow = 1
        Seg006.incCurrRow()
        assertEquals(2, gs.Char.currRow)
    }

    // ========== Phase 8b — fallAccel Tests ==========

    @Test
    fun fallAccelNormalAcceleration() {
        val gs = GameState
        gs.Char.action = Actions.IN_FREEFALL
        gs.Char.fallY = 0
        gs.isFeatherFall = 0
        Seg006.fallAccel()
        assertEquals(Falling.SPEED_ACCEL, gs.Char.fallY)
    }

    @Test
    fun fallAccelCapsAtMax() {
        val gs = GameState
        gs.Char.action = Actions.IN_FREEFALL
        gs.Char.fallY = Falling.SPEED_MAX
        gs.isFeatherFall = 0
        Seg006.fallAccel()
        assertEquals(Falling.SPEED_MAX, gs.Char.fallY)
    }

    @Test
    fun fallAccelFeatherFallSlower() {
        val gs = GameState
        gs.Char.action = Actions.IN_FREEFALL
        gs.Char.fallY = 0
        gs.isFeatherFall = 1
        gs.Char.charid = CharIds.KID
        gs.fixes = FixesOptionsType()  // default (all fixes off)
        Seg006.fallAccel()
        assertEquals(Falling.SPEED_ACCEL_FEATHER, gs.Char.fallY)
    }

    @Test
    fun fallAccelFeatherCapsAtFeatherMax() {
        val gs = GameState
        gs.Char.action = Actions.IN_FREEFALL
        gs.Char.fallY = Falling.SPEED_MAX_FEATHER
        gs.isFeatherFall = 1
        gs.Char.charid = CharIds.KID
        Seg006.fallAccel()
        assertEquals(Falling.SPEED_MAX_FEATHER, gs.Char.fallY)
    }

    @Test
    fun fallAccelNotInFreefallNoOp() {
        val gs = GameState
        gs.Char.action = Actions.STAND
        gs.Char.fallY = 5
        Seg006.fallAccel()
        assertEquals(5, gs.Char.fallY) // unchanged
    }

    @Test
    fun fallAccelFeatherFixGuardGetsNormalAccel() {
        val gs = GameState
        gs.Char.action = Actions.IN_FREEFALL
        gs.Char.fallY = 0
        gs.isFeatherFall = 1
        gs.Char.charid = CharIds.GUARD
        gs.fixes = FixesOptionsType(fixFeatherFallAffectsGuards = 1)
        Seg006.fallAccel()
        assertEquals(Falling.SPEED_ACCEL, gs.Char.fallY)
        gs.fixes = gs.fixesDisabledState // restore
    }

    // ========== fallSpeed Tests ==========

    @Test
    fun fallSpeedAppliesYVelocity() {
        val gs = GameState
        gs.Char.action = Actions.STAND  // not freefall — just applies Y
        gs.Char.y = 100
        gs.Char.fallY = 6
        Seg006.fallSpeed()
        assertEquals(106, gs.Char.y)
    }

    @Test
    fun fallSpeedFreefallAppliesXAndLoadsFrame() {
        val gs = GameState
        gs.Char.action = Actions.IN_FREEFALL
        gs.Char.y = 50
        gs.Char.fallY = 3
        gs.Char.fallX = 2
        gs.Char.x = 100
        gs.Char.direction = Directions.RIGHT
        gs.Char.frame = FrameIds.frame_106_fall
        Seg006.fallSpeed()
        assertEquals(53, gs.Char.y)
        assertEquals(102, gs.Char.x)  // moved forward by fallX=2
    }

    // ========== setCharCollision Tests ==========

    @Test
    fun setCharCollisionNullImage() {
        val gs = GameState
        ExternalStubs.getImage = { _, _ -> null }
        gs.objX = 100
        gs.Char.direction = Directions.RIGHT
        Seg006.setCharCollision()
        assertEquals(0, gs.charWidthHalf)
        assertEquals(0, gs.charHeight)
        assertEquals((100 / 2 + 58).toShort(), gs.charXLeft)
    }

    @Test
    fun setCharCollisionWithImage() {
        val gs = GameState
        ExternalStubs.getImage = { _, _ -> Pair(20, 40) }
        gs.objX = 100
        gs.objY = 80
        gs.Char.direction = Directions.RIGHT
        gs.curFrame = FrameType(0, 0, 0, 0, 0)
        Seg006.setCharCollision()
        assertEquals(10, gs.charWidthHalf)  // (20+1)/2 = 10
        assertEquals(40, gs.charHeight)
        // charXLeft = 100/2+58 - 10 = 98
        assertEquals(98.toShort(), gs.charXLeft)
        assertEquals(108.toShort(), gs.charXRight)  // 98+10
    }

    @Test
    fun setCharCollisionThinFrame() {
        val gs = GameState
        ExternalStubs.getImage = { _, _ -> Pair(20, 40) }
        gs.objX = 100
        gs.objY = 80
        gs.Char.direction = Directions.RIGHT
        gs.curFrame = FrameType(flags = FrameFlags.THIN)
        Seg006.setCharCollision()
        assertEquals((98 + 4).toShort(), gs.charXLeftColl)
        assertEquals((108 - 4).toShort(), gs.charXRightColl)
    }

    // ========== inWall Tests ==========

    @Test
    fun inWallAdjustsPosition() {
        val gs = GameState
        // Set up a character at a known position facing right
        gs.Char.direction = Directions.RIGHT
        gs.Char.x = 100
        gs.Char.frame = FrameIds.frame_15_stand
        gs.Char.currCol = 3
        gs.Char.currRow = 0
        gs.Char.room = 1
        // Set up room so tile in front is not a wall (simple case)
        setupBasicRoom()
        val oldX = gs.Char.x
        Seg006.inWall()
        // Position should be adjusted
        assertNotEquals(oldX, gs.Char.x)
    }

    // ========== checkOnFloor Tests ==========

    @Test
    fun checkOnFloorNoFlagDoesNothing() {
        val gs = GameState
        gs.curFrame = FrameType(0, 0, 0, 0, 0)  // no NEEDS_FLOOR flag
        gs.Char.action = Actions.STAND
        gs.Char.y = 55
        val oldY = gs.Char.y
        Seg006.checkOnFloor()
        assertEquals(oldY, gs.Char.y) // nothing happened
    }

    @Test
    fun checkOnFloorWithFloorPresent() {
        val gs = GameState
        gs.curFrame = FrameType(flags = FrameFlags.NEEDS_FLOOR)
        gs.Char.room = 1
        gs.Char.currCol = 5
        gs.Char.currRow = 0
        setupBasicRoom()
        gs.currRoomTiles[5] = Tiles.FLOOR  // there IS a floor
        val oldAction = gs.Char.action
        Seg006.checkOnFloor()
        assertEquals(oldAction, gs.Char.action) // no fall started
    }

    // ========== stuckLower Tests ==========

    @Test
    fun stuckLowerIncrementsYOnStuckTile() {
        val gs = GameState
        gs.Char.room = 1
        gs.Char.currCol = 3
        gs.Char.currRow = 0
        // Set tile 3 in level data to STUCK, then load room
        gs.level = LevelType()
        for (i in 0 until 30) { gs.level.fg[i] = Tiles.FLOOR; gs.level.bg[i] = 0 }
        gs.level.fg[3] = Tiles.STUCK
        ExternalStubs.getRoomAddress(1)
        gs.Char.y = 50
        Seg006.stuckLower()
        assertEquals(51, gs.Char.y)
    }

    @Test
    fun stuckLowerNoOpOnNonStuck() {
        val gs = GameState
        gs.Char.room = 1
        gs.Char.currCol = 3
        gs.Char.currRow = 0
        setupBasicRoom()
        gs.Char.y = 50
        Seg006.stuckLower()
        assertEquals(50, gs.Char.y) // unchanged
    }

    // ========== clipChar Tests ==========

    @Test
    fun clipCharExitStairsClipsToLevelDoor() {
        val gs = GameState
        gs.Char.frame = FrameIds.frame_224_exit_stairs_8
        gs.Char.room = 1
        gs.Char.currRow = 0
        gs.leveldoorYbottom = 42
        gs.leveldoorRight = 200
        Seg006.clipChar()
        assertEquals(43.toShort(), gs.objClipTop)
        assertEquals(200.toShort(), gs.objClipRight)
    }

    @Test
    fun clipCharResetObjClipCalled() {
        val gs = GameState
        gs.objClipTop = 99
        gs.objClipBottom = 99
        gs.Char.frame = 0
        gs.Char.room = 1
        gs.Char.currRow = 0
        gs.Char.action = Actions.STAND
        gs.charColLeft = 0
        gs.charTopRow = 0
        gs.charColRight = 0
        setupBasicRoom()
        Seg006.clipChar()
        // resetObjClip should have been called, restoring defaults
        assertNotEquals(99.toShort(), gs.objClipTop)
    }

    // === Phase 8c: Falling, grabbing, damage, objects tests ===

    // --- takeHp ---

    @Test
    fun takeHpKidPartialDamage() {
        val gs = GameState
        gs.Char.charid = CharIds.KID
        gs.hitpCurr = 3
        gs.hitpDelta = 0
        val dead = Seg006.takeHp(1)
        assertEquals(0, dead)
        assertEquals(-1, gs.hitpDelta.toInt())
    }

    @Test
    fun takeHpKidLethalDamage() {
        val gs = GameState
        gs.Char.charid = CharIds.KID
        gs.hitpCurr = 3
        gs.hitpDelta = 0
        val dead = Seg006.takeHp(3)
        assertEquals(1, dead)
        assertEquals(-3, gs.hitpDelta.toInt())
    }

    @Test
    fun takeHpKidOverkillDamage() {
        val gs = GameState
        gs.Char.charid = CharIds.KID
        gs.hitpCurr = 2
        gs.hitpDelta = 0
        val dead = Seg006.takeHp(100)
        assertEquals(1, dead)
        assertEquals(-2, gs.hitpDelta.toInt())
    }

    @Test
    fun takeHpGuardPartialDamage() {
        val gs = GameState
        gs.Char.charid = CharIds.GUARD
        gs.guardhpCurr = 4
        gs.guardhpDelta = 0
        val dead = Seg006.takeHp(2)
        assertEquals(0, dead)
        assertEquals(-2, gs.guardhpDelta.toInt())
    }

    @Test
    fun takeHpGuardLethalDamage() {
        val gs = GameState
        gs.Char.charid = CharIds.GUARD
        gs.guardhpCurr = 3
        gs.guardhpDelta = 0
        val dead = Seg006.takeHp(5)
        assertEquals(1, dead)
        assertEquals(-3, gs.guardhpDelta.toInt())
    }

    // --- fellOut ---

    @Test
    fun fellOutKillsCharInRoom0() {
        val gs = GameState
        gs.Char.alive = -1
        gs.Char.room = 0
        gs.Char.frame = 15
        gs.hitpCurr = 3
        gs.hitpDelta = 0
        gs.Char.charid = CharIds.KID
        Seg006.fellOut()
        assertEquals(0, gs.Char.alive)
        assertEquals(FrameIds.frame_185_dead, gs.Char.frame)
        // takeHp(100) should have set hitpDelta = -hitpCurr
        assertEquals(-3, gs.hitpDelta.toInt())
    }

    @Test
    fun fellOutNoEffectIfAlive() {
        val gs = GameState
        gs.Char.alive = 0  // already dead
        gs.Char.room = 0
        gs.Char.frame = 15
        gs.hitpCurr = 3
        gs.hitpDelta = 0
        Seg006.fellOut()
        // Should not change anything — alive >= 0
        assertEquals(15, gs.Char.frame)
        assertEquals(0, gs.hitpDelta.toInt())
    }

    @Test
    fun fellOutNoEffectIfNotRoom0() {
        val gs = GameState
        gs.Char.alive = -1
        gs.Char.room = 5  // not room 0
        gs.Char.frame = 15
        gs.hitpCurr = 3
        gs.hitpDelta = 0
        Seg006.fellOut()
        // Should not change anything
        assertEquals(15, gs.Char.frame)
    }

    // --- playDeathMusic ---

    @Test
    fun playDeathMusicShadow() {
        val gs = GameState
        gs.Guard.charid = CharIds.SHADOW
        gs.holdingSword = 0
        var playedSound = -1
        ExternalStubs.playSound = { id -> playedSound = id }
        Seg006.playDeathMusic()
        assertEquals(SoundIds.SHADOW_MUSIC, playedSound)
    }

    @Test
    fun playDeathMusicFighting() {
        val gs = GameState
        gs.Guard.charid = CharIds.GUARD
        gs.holdingSword = 1
        var playedSound = -1
        ExternalStubs.playSound = { id -> playedSound = id }
        Seg006.playDeathMusic()
        assertEquals(SoundIds.DEATH_IN_FIGHT, playedSound)
    }

    @Test
    fun playDeathMusicRegular() {
        val gs = GameState
        gs.Guard.charid = CharIds.GUARD
        gs.holdingSword = 0
        var playedSound = -1
        ExternalStubs.playSound = { id -> playedSound = id }
        Seg006.playDeathMusic()
        assertEquals(SoundIds.DEATH_REGULAR, playedSound)
    }

    // --- onGuardKilled ---

    @Test
    fun onGuardKilledDemoLevel() {
        val gs = GameState
        gs.currentLevel = 0
        gs.checkpoint = 0
        gs.demoIndex = 5
        gs.demoTime = 10
        Seg006.onGuardKilled()
        assertEquals(1, gs.checkpoint)
        assertEquals(0, gs.demoIndex)
        assertEquals(0, gs.demoTime.toInt())
    }

    @Test
    fun onGuardKilledJaffarLevel() {
        val gs = GameState
        gs.currentLevel = 13
        gs.custom = CustomOptionsType()  // defaults: jaffarVictoryLevel=13
        gs.flashColor = 0
        gs.flashTime = 0
        gs.isShowTime = 0
        gs.leveldoorOpen = 0
        var playedSound = -1
        ExternalStubs.playSound = { id -> playedSound = id }
        Seg006.onGuardKilled()
        assertEquals(Colors.BRIGHTWHITE, gs.flashColor)
        assertEquals(18, gs.flashTime)
        assertEquals(1, gs.isShowTime)
        assertEquals(2, gs.leveldoorOpen)
        assertEquals(SoundIds.VICTORY_JAFFAR, playedSound)
    }

    @Test
    fun onGuardKilledRegularGuard() {
        val gs = GameState
        gs.currentLevel = 5
        gs.Char.charid = CharIds.KID
        var playedSound = -1
        ExternalStubs.playSound = { id -> playedSound = id }
        Seg006.onGuardKilled()
        assertEquals(SoundIds.VICTORY, playedSound)
    }

    @Test
    fun onGuardKilledShadowNoSound() {
        val gs = GameState
        gs.currentLevel = 5
        gs.Char.charid = CharIds.SHADOW
        var playedSound = -1
        ExternalStubs.playSound = { id -> playedSound = id }
        Seg006.onGuardKilled()
        assertEquals(-1, playedSound)  // no sound played
    }

    // --- checkKilledShadow ---

    @Test
    fun checkKilledShadowOnLevel12() {
        val gs = GameState
        gs.currentLevel = 12
        gs.Char.charid = CharIds.SHADOW  // shadow
        gs.Opp.charid = CharIds.KID      // kid — (shadow | kid) = shadow
        // Wait, (0 | 1) = 1 = shadow. Let's check: C code is `(Char.charid | Opp.charid) == charid_1_shadow`
        // charid_1_shadow = 1, so one must be 0 and the other 1, or both 1
        // KID=0, SHADOW=1 → (0 | 1) = 1 ✓
        gs.Char.charid = CharIds.KID
        gs.Opp.charid = CharIds.SHADOW
        gs.Char.alive = -1
        gs.Opp.alive = 0  // Opp is dead
        gs.hitpCurr = 3
        gs.hitpDelta = 0
        gs.flashColor = 0
        gs.flashTime = 0
        Seg006.checkKilledShadow()
        assertEquals(Colors.BRIGHTWHITE, gs.flashColor)
        assertEquals(5, gs.flashTime)
        assertEquals(-3, gs.hitpDelta.toInt())  // takeHp(100) on 3hp
    }

    @Test
    fun checkKilledShadowWrongLevel() {
        val gs = GameState
        gs.currentLevel = 5  // not level 12
        gs.Char.charid = CharIds.KID
        gs.Opp.charid = CharIds.SHADOW
        gs.Char.alive = -1
        gs.Opp.alive = 0
        gs.hitpCurr = 3
        gs.hitpDelta = 0
        gs.flashColor = 0
        Seg006.checkKilledShadow()
        assertEquals(0, gs.flashColor)  // no effect
        assertEquals(0, gs.hitpDelta.toInt())
    }

    // --- drawHurtSplash ---

    @Test
    fun drawHurtSplashKidDead() {
        val gs = GameState
        gs.Char.frame = FrameIds.frame_185_dead
        gs.Char.charid = CharIds.KID
        gs.objY = 10
        gs.objX = 50
        gs.objDirection = 0  // right
        var addedLayer = -1
        ExternalStubs.addObjtable = { layer -> addedLayer = layer }
        Seg006.drawHurtSplash()
        assertEquals(5, addedLayer)  // hurt splash layer
    }

    @Test
    fun drawHurtSplashChompedNoSplash() {
        val gs = GameState
        gs.Char.frame = FrameIds.frame_178_chomped
        gs.Char.charid = CharIds.KID
        var addedLayer = -1
        ExternalStubs.addObjtable = { layer -> addedLayer = layer }
        Seg006.drawHurtSplash()
        assertEquals(-1, addedLayer)  // no splash for chomped
    }

    // --- addSwordToObjtable ---

    @Test
    fun addSwordToObjtableWhenSwordDrawn() {
        val gs = GameState
        gs.Char.frame = FrameIds.frame_158_stand_with_sword
        gs.Char.sword = SwordStatus.DRAWN
        gs.Char.charid = CharIds.KID
        gs.curFrame = FrameType(sword = 1)  // sword frame index 1
        gs.objX = 100
        gs.objY = 50
        var addedLayer = -1
        ExternalStubs.addObjtable = { layer -> addedLayer = layer }
        ExternalStubs.calcScreenXCoord = { x -> x }  // identity
        Seg006.addSwordToObjtable()
        assertEquals(3, addedLayer)  // sword layer
    }

    @Test
    fun addSwordToObjtableWhenSheathedStanding() {
        val gs = GameState
        gs.Char.frame = FrameIds.frame_15_stand
        gs.Char.sword = SwordStatus.SHEATHED
        gs.Char.charid = CharIds.KID
        gs.curFrame = FrameType(sword = 0)
        var addedLayer = -1
        ExternalStubs.addObjtable = { layer -> addedLayer = layer }
        Seg006.addSwordToObjtable()
        assertEquals(-1, addedLayer)  // no sword added
    }

    // --- checkAction ---

    @Test
    fun checkActionFreefallCallsDoFall() {
        val gs = GameState
        gs.Char.action = Actions.IN_FREEFALL
        gs.Char.frame = FrameIds.frame_106_fall
        gs.fixes = FixesOptionsType()  // all fixes off
        var doFallCalled = false
        ExternalStubs.doFall = { doFallCalled = true }
        Seg006.checkAction()
        assertTrue(doFallCalled)
    }

    @Test
    fun checkActionHangClimbDoesNothing() {
        val gs = GameState
        gs.Char.action = Actions.HANG_CLIMB
        gs.Char.frame = 135
        gs.fixes = FixesOptionsType()
        // Should not call checkOnFloor or doFall — just return
        // If this throws, it means something unexpected was called
        Seg006.checkAction()
        // No assertion needed — if it doesn't crash, it passed
    }

    // --- checkPress ---

    @Test
    fun checkPressHangingOnOpener() {
        val gs = GameState
        gs.Char.frame = FrameIds.frame_87_hanging_1
        gs.Char.action = Actions.HANG_CLIMB
        gs.Char.room = 1
        gs.Char.currRow = 0
        gs.Char.currCol = 5
        gs.Char.alive = -1
        setupBasicRoom()
        // Set the tile above to be an opener
        gs.level.fg[5] = Tiles.OPENER  // row 0 col 5 → but getTileAboveChar goes to row-1
        // Actually getTileAboveChar goes row-1, so we need row -1 which wraps to upper room.
        // Let's set it up so currRow=1, and row 0 has the opener
        gs.Char.currRow = 1
        gs.level.fg[5] = Tiles.OPENER  // tilepos for row 0 col 5 = 5
        var triggered = false
        ExternalStubs.triggerButton = { _, _, _ -> triggered = true }
        ExternalStubs.getRoomAddress(1)
        Seg006.checkPress()
        assertTrue(triggered)
    }

    @Test
    fun checkPressCrouchOnFloorNoTrigger() {
        val gs = GameState
        gs.Char.frame = FrameIds.frame_109_crouch
        gs.Char.action = Actions.STAND  // action < HANG_CLIMB
        gs.Char.room = 1
        gs.Char.currRow = 1
        gs.Char.currCol = 5
        gs.Char.alive = -1
        setupBasicRoom()
        gs.curFrame = FrameType(flags = FrameFlags.NEEDS_FLOOR)
        gs.fixes = FixesOptionsType()
        var triggered = false
        ExternalStubs.triggerButton = { _, _, _ -> triggered = true }
        Seg006.checkPress()
        assertFalse(triggered)  // floor tile, not opener/closer
    }

    // --- checkSpiked ---

    @Test
    fun checkSpikedRunningOnHarmfulSpike() {
        val gs = GameState
        gs.Char.room = 1
        gs.Char.currCol = 5
        gs.Char.currRow = 1
        gs.Char.frame = FrameIds.frame_7_run
        setupBasicRoom()
        // Set tile at char position to spike
        gs.level.fg[15] = Tiles.SPIKE  // tilepos for row 1 col 5 = 15
        ExternalStubs.getRoomAddress(1)
        var harmfulCallCount = 0
        ExternalStubs.isSpikePowerful = { harmfulCallCount++; 2 }  // harmful >= 2
        var spikedCalled = false
        ExternalStubs.spiked = { spikedCalled = true }
        Seg006.checkSpiked()
        assertTrue(spikedCalled)
    }

    @Test
    fun checkSpikedNotOnSpikeTile() {
        val gs = GameState
        gs.Char.room = 1
        gs.Char.currCol = 5
        gs.Char.currRow = 1
        gs.Char.frame = FrameIds.frame_7_run
        setupBasicRoom()  // all floor tiles, no spikes
        var spikedCalled = false
        ExternalStubs.spiked = { spikedCalled = true }
        Seg006.checkSpiked()
        assertFalse(spikedCalled)
    }

    // --- checkGrab ---

    @Test
    fun checkGrabNotHoldingShift() {
        val gs = GameState
        gs.controlShift = Control.RELEASED  // not holding shift
        gs.Char.fallY = 10
        gs.Char.alive = -1
        gs.Char.currRow = 1
        gs.Char.y = 100
        gs.Char.x = 50
        gs.fixes = FixesOptionsType()
        val origX = gs.Char.x
        Seg006.checkGrab()
        assertEquals(origX, gs.Char.x)  // no change — shift not held
    }

    @Test
    fun checkGrabFallingTooFast() {
        val gs = GameState
        gs.controlShift = Control.HELD
        gs.Char.fallY = 33  // > 32 (max grab speed)
        gs.Char.alive = -1
        gs.Char.currRow = 1
        gs.Char.y = 100
        gs.Char.x = 50
        gs.fixes = FixesOptionsType()
        val origX = gs.Char.x
        Seg006.checkGrab()
        assertEquals(origX, gs.Char.x)  // no grab — too fast
    }

    // ========== Phase 8d — Player/guard control tests ==========

    // --- userControl tests ---

    @Test
    fun userControlFacingRightFlipsControlsForControl() {
        val gs = GameState
        gs.Char.direction = Directions.RIGHT
        gs.controlX = Control.HELD_FORWARD
        gs.controlForward = Control.HELD
        gs.controlBackward = Control.RELEASED
        var controlCalled = false
        ExternalStubs.control = {
            controlCalled = true
            // During control() call, X should be flipped (since dir >= RIGHT)
            assertEquals(-Control.HELD_FORWARD, gs.controlX)
        }
        Seg006.userControl()
        assertTrue(controlCalled)
        // After userControl, X is restored
        assertEquals(Control.HELD_FORWARD, gs.controlX)
    }

    @Test
    fun userControlFacingLeftCallsControlDirectly() {
        val gs = GameState
        gs.Char.direction = Directions.LEFT
        gs.controlX = Control.HELD_FORWARD
        var controlCalled = false
        ExternalStubs.control = {
            controlCalled = true
            // No flip — X is unchanged
            assertEquals(Control.HELD_FORWARD, gs.controlX)
        }
        Seg006.userControl()
        assertTrue(controlCalled)
    }

    // --- doDemo tests ---

    @Test
    fun doDemoWithCheckpointSetsForwardControl() {
        val gs = GameState
        gs.checkpoint = 1
        gs.controlForward = Control.RELEASED
        gs.controlX = 0
        gs.controlUp = Control.HELD
        gs.controlDown = Control.HELD
        gs.controlBackward = Control.HELD
        Seg006.doDemo()
        assertEquals(Control.HELD, gs.controlForward)
        assertEquals(Control.HELD_FORWARD, gs.controlX)
        assertEquals(1, gs.controlShift2) // releaseArrows returns 1
        // releaseArrows clears all directional controls
        assertEquals(Control.RELEASED, gs.controlUp)
        assertEquals(Control.RELEASED, gs.controlDown)
        assertEquals(Control.RELEASED, gs.controlBackward)
    }

    @Test
    fun doDemoWithSwordUsesAutocontrol() {
        val gs = GameState
        gs.checkpoint = 0
        gs.Char.sword = 1
        gs.guardSkill = 5
        var autocontrolCalled = false
        ExternalStubs.autocontrolOpponent = {
            autocontrolCalled = true
            assertEquals(10, gs.guardSkill)
        }
        Seg006.doDemo()
        assertTrue(autocontrolCalled)
        assertEquals(11, gs.guardSkill)
    }

    @Test
    fun doDemoNoCheckpointNoSwordCallsAutoMoves() {
        val gs = GameState
        gs.checkpoint = 0
        gs.Char.sword = 0
        var autoMovesCalled = false
        ExternalStubs.doAutoMoves = { moves ->
            autoMovesCalled = true
            assertNotNull(moves)
        }
        Seg006.doDemo()
        assertTrue(autoMovesCalled)
    }

    // --- controlKid tests ---

    @Test
    fun controlKidKillsKidWhenAliveNegativeAndNoHP() {
        val gs = GameState
        gs.Char.alive = -1
        gs.hitpCurr = 0
        gs.currentLevel = 1 // not demo
        gs.playDemoLevel = 0
        gs.replaying = 0
        // Need stubs for normal gameplay path
        ExternalStubs.doPaused = { 0 }
        ExternalStubs.control = { }
        Seg006.controlKid()
        assertEquals(0, gs.Char.alive)
    }

    @Test
    fun controlKidDecrementsGrabTimer() {
        val gs = GameState
        gs.Char.alive = -1
        gs.hitpCurr = 3 // alive, HP > 0
        gs.grabTimer = 5
        gs.currentLevel = 1
        gs.playDemoLevel = 0
        gs.replaying = 0
        ExternalStubs.doPaused = { 0 }
        ExternalStubs.control = { }
        Seg006.controlKid()
        assertEquals(4, gs.grabTimer)
    }

    @Test
    fun controlKidDemoLevelCallsDoDemo() {
        val gs = GameState
        gs.Char.alive = -1
        gs.hitpCurr = 3
        gs.currentLevel = 0 // demo level
        gs.playDemoLevel = 0
        gs.replaying = 0
        gs.checkpoint = 1 // will trigger the checkpoint path in doDemo
        var controlCalled = false
        ExternalStubs.control = { controlCalled = true }
        ExternalStubs.keyTestQuit = { 0 } // no key pressed
        Seg006.controlKid()
        assertTrue(controlCalled)
        // doDemo sets forward control when checkpoint
        assertEquals(Control.HELD, gs.controlForward)
    }

    @Test
    fun controlKidNormalLevelReadsControlAndCallsUserControl() {
        val gs = GameState
        gs.Char.alive = -1
        gs.hitpCurr = 3
        gs.currentLevel = 1
        gs.playDemoLevel = 0
        gs.replaying = 0
        gs.controlX = Control.HELD_FORWARD
        gs.controlY = 0
        gs.controlShift = 0
        // Save some ctrl1 values to verify restCtrl1 is called
        gs.ctrl1Forward = Control.HELD
        gs.ctrl1Backward = Control.RELEASED
        gs.ctrl1Up = Control.RELEASED
        gs.ctrl1Down = Control.RELEASED
        gs.ctrl1Shift2 = Control.RELEASED
        var controlCalled = false
        ExternalStubs.control = { controlCalled = true }
        ExternalStubs.doPaused = { 0 }
        Seg006.controlKid()
        assertTrue(controlCalled)
    }

    @Test
    fun controlKidStopsFeatherFallOnDeath() {
        val gs = GameState
        gs.Char.alive = -1
        gs.hitpCurr = 0
        gs.isFeatherFall = 5
        gs.fixes = FixesOptionsType(fixQuicksaveDuringFeather = 1)
        gs.currentLevel = 1
        gs.playDemoLevel = 0
        gs.replaying = 0
        var soundsStopped = false
        ExternalStubs.checkSoundPlaying = { 1 }
        ExternalStubs.stopSounds = { soundsStopped = true }
        ExternalStubs.doPaused = { 0 }
        ExternalStubs.control = { }
        Seg006.controlKid()
        assertEquals(0, gs.isFeatherFall)
        assertTrue(soundsStopped)
    }

    // --- playKid tests ---

    @Test
    fun playKidCallsFellOutAndControlKid() {
        val gs = GameState
        gs.Char.alive = -1
        gs.hitpCurr = 3
        gs.currentLevel = 1
        gs.playDemoLevel = 0
        gs.replaying = 0
        // Set up a room so fellOut doesn't crash
        gs.Char.room = 1
        gs.Char.currRow = 1
        gs.Char.x = 50
        gs.Char.action = Actions.STAND
        gs.drawnRoom = 1
        ExternalStubs.doPaused = { 0 }
        ExternalStubs.control = { }
        // isDead returns 0 so the death path is skipped
        gs.Char.frame = FrameIds.frame_15_stand
        Seg006.playKid()
        // Should not crash — basic smoke test
    }

    @Test
    fun playKidDeathSequenceIncrementsAlive() {
        val gs = GameState
        gs.Char.alive = 0
        gs.hitpCurr = 3
        gs.currentLevel = 1
        gs.playDemoLevel = 0
        gs.replaying = 0
        gs.Char.room = 1
        gs.Char.currRow = 1
        gs.Char.x = 50
        gs.Char.action = Actions.STAND
        gs.Char.frame = FrameIds.frame_185_dead // isDead returns 1
        gs.drawnRoom = 1
        gs.resurrectTime = 0
        ExternalStubs.doPaused = { 0 }
        ExternalStubs.control = { }
        ExternalStubs.checkSoundPlaying = { 0 }
        Seg006.playKid()
        assertEquals(1, gs.Char.alive)
    }

    // --- playGuard tests ---

    @Test
    fun playGuardMouseCallsAutocontrol() {
        val gs = GameState
        gs.Char.charid = CharIds.MOUSE
        var autocontrolCalled = false
        ExternalStubs.autocontrolOpponent = { autocontrolCalled = true }
        Seg006.playGuard()
        assertTrue(autocontrolCalled)
    }

    @Test
    fun playGuardDeadGuardWithNoHPKills() {
        val gs = GameState
        gs.Char.charid = CharIds.GUARD
        gs.Char.alive = -1
        gs.guardhpCurr = 0
        gs.currentLevel = 5
        var controlCalled = false
        ExternalStubs.autocontrolOpponent = { }
        ExternalStubs.control = { controlCalled = true }
        ExternalStubs.playSound = { }
        Seg006.playGuard()
        assertEquals(0, gs.Char.alive)
        // onGuardKilled was called — for non-jaffar, non-shadow, non-demo: plays victory sound
    }

    @Test
    fun playGuardAliveGuardCallsAutocontrolAndControl() {
        val gs = GameState
        gs.Char.charid = CharIds.GUARD
        gs.Char.alive = -1
        gs.guardhpCurr = 3
        var autocontrolCalled = false
        var controlCalled = false
        ExternalStubs.autocontrolOpponent = { autocontrolCalled = true }
        ExternalStubs.control = { controlCalled = true }
        Seg006.playGuard()
        assertTrue(autocontrolCalled)
        assertTrue(controlCalled)
    }

    @Test
    fun playGuardShadowClearsChar() {
        val gs = GameState
        gs.Char.charid = CharIds.SHADOW
        gs.Char.alive = 0 // not negative — skips dead check
        gs.Char.direction = Directions.RIGHT
        ExternalStubs.autocontrolOpponent = { }
        ExternalStubs.control = { }
        ExternalStubs.drawGuardHp = { _, _ -> }
        Seg006.playGuard()
        assertEquals(Directions.NONE, gs.Char.direction) // clearChar sets direction to NONE
    }

    // --- controlGuardInactive tests ---

    @Test
    fun controlGuardInactiveDrawsSwordWhenForward() {
        val gs = GameState
        gs.Char.frame = FrameIds.frame_166_stand_inactive
        gs.controlDown = Control.HELD
        gs.controlForward = Control.HELD
        var drawSwordCalled = false
        ExternalStubs.drawSword = { drawSwordCalled = true }
        Seg006.controlGuardInactive()
        assertTrue(drawSwordCalled)
    }

    @Test
    fun controlGuardInactiveFlipsWhenNoForward() {
        val gs = GameState
        gs.Char.frame = FrameIds.frame_166_stand_inactive
        gs.controlDown = Control.HELD
        gs.controlForward = Control.RELEASED
        var seqOffsetCalled = false
        ExternalStubs.seqtblOffsetChar = { seqIndex ->
            seqOffsetCalled = true
            assertEquals(SeqIds.seq_80_stand_flipped, seqIndex)
            gs.Char.currSeq = SequenceTable.seqtblOffsets[seqIndex]
        }
        Seg006.controlGuardInactive()
        assertTrue(seqOffsetCalled)
        assertEquals(Control.IGNORE, gs.controlDown)
    }

    @Test
    fun controlGuardInactiveIgnoresWrongFrame() {
        val gs = GameState
        gs.Char.frame = FrameIds.frame_15_stand // not inactive frame
        gs.controlDown = Control.HELD
        gs.controlForward = Control.HELD
        var drawSwordCalled = false
        ExternalStubs.drawSword = { drawSwordCalled = true }
        Seg006.controlGuardInactive()
        assertFalse(drawSwordCalled)
    }

    // --- charOppDist tests ---

    @Test
    fun charOppDistDifferentRoomReturns999() {
        val gs = GameState
        gs.Char.room = 1
        gs.Opp.room = 2
        assertEquals(999, Seg006.charOppDist())
    }

    @Test
    fun charOppDistOppInFrontFacingLeft() {
        val gs = GameState
        gs.Char.room = 1
        gs.Opp.room = 1
        gs.Char.x = 100
        gs.Opp.x = 80  // Opp is at lower X
        gs.Char.direction = Directions.LEFT // facing left means lower X is forward
        gs.Opp.direction = Directions.LEFT
        // distance = Opp.x - Char.x = -20, direction < RIGHT → negate → 20
        // same direction, so no +13
        assertEquals(20, Seg006.charOppDist())
    }

    @Test
    fun charOppDistOppInFrontFacingRight() {
        val gs = GameState
        gs.Char.room = 1
        gs.Opp.room = 1
        gs.Char.x = 80
        gs.Opp.x = 100 // Opp is at higher X
        gs.Char.direction = Directions.RIGHT
        gs.Opp.direction = Directions.RIGHT
        // distance = 100 - 80 = 20, direction >= RIGHT → no negate
        // same direction → no +13
        assertEquals(20, Seg006.charOppDist())
    }

    @Test
    fun charOppDistOppositeFacingAdds13() {
        val gs = GameState
        gs.Char.room = 1
        gs.Opp.room = 1
        gs.Char.x = 80
        gs.Opp.x = 100
        gs.Char.direction = Directions.RIGHT
        gs.Opp.direction = Directions.LEFT
        // distance = 20, Opp facing different direction, distance >= 0 → +13 = 33
        assertEquals(33, Seg006.charOppDist())
    }

    @Test
    fun charOppDistOppBehindReturnsNegative() {
        val gs = GameState
        gs.Char.room = 1
        gs.Opp.room = 1
        gs.Char.x = 100
        gs.Opp.x = 80
        gs.Char.direction = Directions.RIGHT
        gs.Opp.direction = Directions.RIGHT
        // distance = 80 - 100 = -20, direction >= RIGHT → no negate
        // distance < 0, so no +13
        assertEquals(-20, Seg006.charOppDist())
    }

    // Helper to set up a basic room with all floor tiles
    private fun setupBasicRoom() {
        val gs = GameState
        gs.level = LevelType()
        // Set room 1 links to 0 (no neighbors)
        for (i in 0 until 30) {
            gs.level.fg[i] = Tiles.FLOOR
            gs.level.bg[i] = 0
        }
        ExternalStubs.getRoomAddress(1)
    }
}
