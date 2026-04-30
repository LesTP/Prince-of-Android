package com.sdlpop.game

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class Seg008Test {
    private val gs = GameState

    @BeforeTest
    fun resetState() {
        gs.level = LevelType()
        gs.currRoomTiles.fill(0)
        gs.currRoomModif.fill(0)
        for (entry in gs.leftroom) {
            entry.tiletype = 0
            entry.modifier = 0
        }
        gs.currTile = 0
        gs.drawBottomY = 0
        gs.drawMainY = 0
        gs.modifierLeft = 0
        gs.gateTopY = 0
        gs.gateOpenness = 0
        gs.gateBottomY = 0
        gs.custom = CustomOptionsType()
        gs.fixes = FixesOptionsType()
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
}
