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
}
