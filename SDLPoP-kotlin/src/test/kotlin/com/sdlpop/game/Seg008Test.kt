package com.sdlpop.game

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class Seg008Test {
    private val gs = GameState

    @BeforeTest
    fun resetState() {
        gs.level = LevelType()
        gs.loadedRoom = 0
        gs.drawnRoom = 0
        gs.roomL = 0
        gs.roomR = 0
        gs.roomA = 0
        gs.roomB = 0
        gs.roomAL = 0
        gs.roomAR = 0
        gs.roomBL = 0
        gs.roomBR = 0
        gs.currRoomTiles.fill(0)
        gs.currRoomModif.fill(0)
        for (entry in gs.leftroom) {
            entry.tiletype = 0
            entry.modifier = 0
        }
        for (entry in gs.rowBelowLeft) {
            entry.tiletype = 0
            entry.modifier = 0
        }
        for (room in gs.torchColors) {
            room.fill(0)
        }
        gs.currTile = 0
        gs.currModifier = 0
        gs.drawnCol = 0
        gs.drawnRow = 0
        gs.drawXh = 0
        gs.tileLeft = 0
        gs.drawBottomY = 0
        gs.drawMainY = 0
        gs.modifierLeft = 0
        gs.graphicsMode = 0
        gs.gateTopY = 0
        gs.gateOpenness = 0
        gs.gateBottomY = 0
        gs.custom = CustomOptionsType()
        gs.fixes = FixesOptionsType()
        ExternalStubs.getRoomAddress = { room -> ExternalStubs.loadRoomAddress(room) }
    }

    @Test
    fun calcScreenXCoordScalesDosLogicalXToScreenX() {
        assertEquals(0, Seg008.calcScreenXCoord(0).toInt())
        assertEquals(160, Seg008.calcScreenXCoord(140).toInt())
        assertEquals(320, Seg008.calcScreenXCoord(280).toInt())
        assertEquals((-11).toShort(), Seg008.calcScreenXCoord((-10).toShort()))
    }

    @Test
    fun canSeeBottomleftMatchesVisibleTileSet() {
        for (tile in listOf(Tiles.EMPTY, Tiles.BIGPILLAR_TOP, Tiles.DOORTOP, Tiles.LATTICE_DOWN)) {
            gs.currTile = tile
            assertEquals(1, Seg008.canSeeBottomleft())
        }

        gs.currTile = Tiles.FLOOR
        assertEquals(0, Seg008.canSeeBottomleft())
    }

    @Test
    fun frameHelpersMatchModifierRules() {
        assertEquals(3, Seg008.getSpikeFrame(3))
        assertEquals(5, Seg008.getSpikeFrame(0x83))

        assertEquals(12, Seg008.getLooseFrame(12))
        assertEquals(1, Seg008.getLooseFrame(0x8C))

        gs.custom.looseFloorDelay = 12
        assertEquals(1, Seg008.getLooseFrame(12))
        assertEquals(9, Seg008.getLooseFrame(9))
    }

    @Test
    fun calcGatePosUsesLeftModifierAndDrawY() {
        gs.drawBottomY = 181
        gs.drawMainY = 118
        gs.modifierLeft = 40

        Seg008.calcGatePos()

        assertEquals(119, gs.gateTopY)
        assertEquals(11, gs.gateOpenness)
        assertEquals(107, gs.gateBottomY)
    }

    @Test
    fun getTileToDrawUsesCurrentRoomBuffersAndMasksTileType() {
        gs.currRoomTiles[12] = Tiles.WALL or 0xE0
        gs.currRoomModif[12] = 0x34

        val tile = Seg008.getTileToDraw(room = 1, column = 2, row = 1, tileRoom0 = Tiles.EMPTY)

        assertEquals(Tiles.WALL, tile.tiletype)
        assertEquals(0x34, tile.modifier)
    }

    @Test
    fun getTileToDrawUsesLeftroomForColumnMinusOne() {
        gs.leftroom[1].tiletype = Tiles.PILLAR
        gs.leftroom[1].modifier = 7

        val tile = Seg008.getTileToDraw(room = 1, column = -1, row = 1, tileRoom0 = Tiles.EMPTY)

        assertEquals(Tiles.PILLAR, tile.tiletype)
        assertEquals(7, tile.modifier)
    }

    @Test
    fun getTileToDrawReturnsConfiguredEdgeTileForRoomZero() {
        val tile = Seg008.getTileToDraw(room = 0, column = 4, row = 2, tileRoom0 = Tiles.WALL)

        assertEquals(Tiles.WALL, tile.tiletype)
        assertEquals(0, tile.modifier)
    }

    @Test
    fun getTileToDrawConvertsPressedButtonsUsingDoorlinkTimers() {
        gs.currRoomTiles[0] = Tiles.CLOSER
        gs.currRoomModif[0] = 3
        gs.level.doorlinks2[3] = 2

        var tile = Seg008.getTileToDraw(room = 1, column = 0, row = 0, tileRoom0 = Tiles.EMPTY)
        assertEquals(Tiles.STUCK, tile.tiletype)
        assertEquals(3, tile.modifier)

        gs.currRoomTiles[0] = Tiles.OPENER
        tile = Seg008.getTileToDraw(room = 1, column = 0, row = 0, tileRoom0 = Tiles.EMPTY)
        assertEquals(Tiles.FLOOR, tile.tiletype)
        assertEquals(0, tile.modifier)
    }

    @Test
    fun getTileToDrawAppliesFakeTileRules() {
        assertTileTransform(Tiles.EMPTY, 4, Tiles.FLOOR, 0)
        assertTileTransform(Tiles.EMPTY, 12, Tiles.FLOOR, 1)
        assertTileTransform(Tiles.EMPTY, 13, Tiles.WALL, 0x80)
        assertTileTransform(Tiles.EMPTY, 53, Tiles.WALL, 3)
        assertTileTransform(Tiles.FLOOR, 6, Tiles.EMPTY, 0)
        assertTileTransform(Tiles.FLOOR, 14, Tiles.EMPTY, 1)
        assertTileTransform(Tiles.FLOOR, 51, Tiles.WALL, 1)
        assertTileTransform(Tiles.WALL, 0x40, Tiles.FLOOR, 0)
        assertTileTransform(Tiles.WALL, 0xC0, Tiles.FLOOR, 1)
        assertTileTransform(Tiles.WALL, 0x60, Tiles.EMPTY, 0)
        assertTileTransform(Tiles.WALL, 0xE0, Tiles.EMPTY, 1)
    }

    @Test
    fun getTileToDrawAppliesLooseLeftOfPotionFixWhenEnabled() {
        gs.currRoomTiles[0] = Tiles.LOOSE
        gs.currRoomModif[0] = 0

        var tile = Seg008.getTileToDraw(room = 1, column = 0, row = 0, tileRoom0 = Tiles.EMPTY)
        assertEquals(Tiles.LOOSE, tile.tiletype)

        gs.fixes.fixLooseLeftOfPotion = 1
        tile = Seg008.getTileToDraw(room = 1, column = 0, row = 0, tileRoom0 = Tiles.EMPTY)
        assertEquals(Tiles.FLOOR, tile.tiletype)
    }

    private fun assertTileTransform(inputTile: Int, inputModifier: Int, expectedTile: Int, expectedModifier: Int) {
        gs.currRoomTiles[0] = inputTile
        gs.currRoomModif[0] = inputModifier

        val tile = Seg008.getTileToDraw(room = 1, column = 0, row = 0, tileRoom0 = Tiles.EMPTY)

        assertEquals(expectedTile, tile.tiletype)
        assertEquals(expectedModifier, tile.modifier)
    }

    @Test
    fun loadRoomLinksLoadsDrawnRoomBuffersAndDiagonalNeighbors() {
        gs.drawnRoom = 5
        gs.level.roomlinks[4] = LinkType(left = 4, right = 6, up = 2, down = 8)
        gs.level.roomlinks[1] = LinkType(left = 1, right = 3)
        gs.level.roomlinks[7] = LinkType(left = 7, right = 9)
        seedRoom(5, 0, Tiles.POTION, 2)

        Seg008.loadRoomLinks()

        assertEquals(5, gs.loadedRoom)
        assertEquals(Tiles.POTION, gs.currRoomTiles[0])
        assertEquals(2, gs.currRoomModif[0])
        assertEquals(4, gs.roomL)
        assertEquals(6, gs.roomR)
        assertEquals(2, gs.roomA)
        assertEquals(8, gs.roomB)
        assertEquals(1, gs.roomAL)
        assertEquals(3, gs.roomAR)
        assertEquals(7, gs.roomBL)
        assertEquals(9, gs.roomBR)
    }

    @Test
    fun loadRoomLinksDerivesDiagonalNeighborsThroughSideRoomsWhenAboveBelowMissing() {
        gs.drawnRoom = 5
        gs.level.roomlinks[4] = LinkType(left = 4, right = 6)
        gs.level.roomlinks[3] = LinkType(up = 11, down = 12)
        gs.level.roomlinks[5] = LinkType(up = 13, down = 14)

        Seg008.loadRoomLinks()

        assertEquals(11, gs.roomAL)
        assertEquals(13, gs.roomAR)
        assertEquals(12, gs.roomBL)
        assertEquals(14, gs.roomBR)
    }

    @Test
    fun loadCurrAndLeftTileUsesTopEdgeAndColumnLookup() {
        gs.drawnRoom = 1
        gs.drawnRow = 2
        gs.drawnCol = 0
        gs.custom.drawnTileTopLevelEdge = Tiles.FLOOR
        gs.leftroom[2].tiletype = Tiles.EMPTY
        gs.leftroom[2].modifier = 12
        seedRoom(1, 20, Tiles.WALL, 0x34)
        ExternalStubs.getRoomAddress(1)

        Seg008.loadCurrAndLeftTile()

        assertEquals(Tiles.WALL, gs.currTile)
        assertEquals(0x34, gs.currModifier)
        assertEquals(Tiles.FLOOR, gs.tileLeft)
        assertEquals(1, gs.modifierLeft)
        assertEquals(0, gs.drawXh)
    }

    @Test
    fun loadLeftroomReadsLevelDataAndAppliesLeftLevelEdgeForRoomZero() {
        gs.roomL = 2
        gs.custom.drawnTileLeftLevelEdge = Tiles.PILLAR
        seedRoom(2, 9, Tiles.WALL, 1)
        seedRoom(2, 19, Tiles.EMPTY, 4)
        seedRoom(2, 29, Tiles.FLOOR, 6)

        Seg008.loadLeftroom()

        assertEquals(Tiles.WALL, gs.leftroom[0].tiletype)
        assertEquals(1, gs.leftroom[0].modifier)
        assertEquals(Tiles.FLOOR, gs.leftroom[1].tiletype)
        assertEquals(0, gs.leftroom[1].modifier)
        assertEquals(Tiles.EMPTY, gs.leftroom[2].tiletype)
        assertEquals(0, gs.leftroom[2].modifier)

        gs.roomL = 0
        Seg008.loadLeftroom()
        assertEquals(Tiles.PILLAR, gs.leftroom[0].tiletype)
    }

    @Test
    fun loadRowbelowReadsCurrentBelowAndLeftRoomsThenRestoresDrawnRoom() {
        gs.drawnRoom = 3
        gs.roomL = 2
        gs.drawnRow = 1
        seedRoom(3, 20, Tiles.POTION, 3)
        seedRoom(3, 28, Tiles.FLOOR, 0)
        seedRoom(2, 29, Tiles.WALL, 1)

        Seg008.loadRowbelow()

        assertEquals(3, gs.loadedRoom)
        assertEquals(Tiles.WALL, gs.rowBelowLeft[0].tiletype)
        assertEquals(1, gs.rowBelowLeft[0].modifier)
        assertEquals(Tiles.POTION, gs.rowBelowLeft[1].tiletype)
        assertEquals(3, gs.rowBelowLeft[1].modifier)
        assertEquals(Tiles.FLOOR, gs.rowBelowLeft[9].tiletype)
    }

    @Test
    fun loadRowbelowUsesBelowRoomsAtBottomRow() {
        gs.drawnRoom = 3
        gs.roomB = 8
        gs.roomBL = 7
        gs.drawnRow = 2
        seedRoom(8, 0, Tiles.SPIKE, 4)
        seedRoom(7, 9, Tiles.GATE, 1)

        Seg008.loadRowbelow()

        assertEquals(3, gs.loadedRoom)
        assertEquals(Tiles.GATE, gs.rowBelowLeft[0].tiletype)
        assertEquals(1, gs.rowBelowLeft[0].modifier)
        assertEquals(Tiles.SPIKE, gs.rowBelowLeft[1].tiletype)
        assertEquals(4, gs.rowBelowLeft[1].modifier)
    }

    @Test
    fun alterModsAllrmPreprocessesModifiersAndSyncsLevelBuffers() {
        gs.level.usedRooms = 25
        seedRoom(1, 0, Tiles.GATE, 1)
        seedRoom(1, 1, Tiles.GATE, 2)
        seedRoom(1, 2, Tiles.LOOSE, 9)
        seedRoom(1, 3, Tiles.POTION, 6)
        seedRoom(1, 4, Tiles.TORCH, 7)

        Seg008.alterModsAllrm()

        assertEquals(24, gs.level.usedRooms)
        assertEquals(188, gs.level.bg[0])
        assertEquals(0, gs.level.bg[1])
        assertEquals(0, gs.level.bg[2])
        assertEquals(48, gs.level.bg[3])
        assertEquals(7, gs.torchColors[1][4])
        assertEquals(0, gs.level.bg[4])
    }

    @Test
    fun loadAlterModAddsWallConnectionBitsFromCurrentAndNeighborRooms() {
        gs.level.usedRooms = 2
        gs.level.roomlinks[0] = LinkType(left = 2)
        seedRoom(1, 0, Tiles.WALL, 2)
        seedRoom(1, 1, Tiles.FLOOR, 0)
        seedRoom(2, 9, Tiles.WALL, 0)
        ExternalStubs.getRoomAddress(1)
        gs.roomL = 2
        gs.roomR = 0

        Seg008.loadAlterMod(0)

        assertEquals(0x22, gs.currRoomModif[0])
        assertEquals(0x22, gs.level.bg[0])
    }

    @Test
    fun loadAlterModMarksFakeWallConnectionsForFloorAndEmptyTiles() {
        seedRoom(1, 0, Tiles.WALL, 0)
        seedRoom(1, 1, Tiles.FLOOR, 5)
        seedRoom(1, 2, Tiles.WALL, 0)
        ExternalStubs.getRoomAddress(1)
        gs.roomL = 0
        gs.roomR = 0

        Seg008.loadAlterMod(1)

        assertEquals(53, gs.currRoomModif[1])
        assertEquals(53, gs.level.bg[1])
    }

    @Test
    fun loadAlterModUsesCgaWallModifierRule() {
        seedRoom(1, 4, Tiles.WALL, 0)
        ExternalStubs.getRoomAddress(1)
        gs.graphicsMode = 1

        Seg008.loadAlterMod(4)

        assertEquals(3, gs.currRoomModif[4])
    }

    private fun seedRoom(room: Int, tilepos: Int, tile: Int, modifier: Int) {
        val index = (room - 1) * 30 + tilepos
        gs.level.fg[index] = tile
        gs.level.bg[index] = modifier
    }
}
