package com.sdlpop.game

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class Seg008Test {
    private val gs = GameState

    @BeforeTest
    fun resetState() {
        gs.level = LevelType()
        gs.currentLevel = -1
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
        gs.drawMode = 0
        gs.remMin = 0
        gs.remTick = 0
        gs.isShowTime = 0
        gs.textTimeRemaining = 0
        gs.textTimeTotal = 0
        gs.nextLevel = 0
        gs.seamless = 0
        gs.leveldoorOpen = 0
        gs.randomSeed = 0
        gs.seedWasInit = 1
        gs.palaceWallColors.fill(0)
        gs.gateTopY = 0
        gs.gateOpenness = 0
        gs.gateBottomY = 0
        gs.hitpDelta = 0
        gs.guardhpDelta = 0
        gs.unitedWithShadow = 0
        gs.objXh = 0
        gs.objXl = 0
        gs.objY = 0
        gs.objChtab = 0
        gs.objId = 0
        gs.objTilepos = 0
        gs.objX = 0
        gs.objDirection = 0
        gs.objClipLeft = 0
        gs.objClipTop = 0
        gs.objClipRight = 0
        gs.objClipBottom = 0
        gs.backtable.forEach {
            it.xh = 0
            it.xl = 0
            it.y = 0
            it.chtabId = 0
            it.id = 0
            it.blit = 0
        }
        gs.foretable.forEach {
            it.xh = 0
            it.xl = 0
            it.y = 0
            it.chtabId = 0
            it.id = 0
            it.blit = 0
        }
        gs.midtable.forEach {
            it.xh = 0
            it.xl = 0
            it.y = 0
            it.chtabId = 0
            it.id = 0
            it.peel = 0
            it.clip = RectType()
            it.blit = 0
        }
        gs.wipetable.forEach {
            it.left = 0
            it.bottom = 0
            it.height = 0
            it.width = 0
            it.color = 0
            it.layer = 0
        }
        gs.nCurrObjs = 0
        gs.currObjs.fill(0)
        gs.objtable.forEach {
            it.xh = 0
            it.xl = 0
            it.y = 0
            it.chtabId = 0
            it.id = 0
            it.direction = 0
            it.objType = 0
            it.clip = RectType()
            it.tilepos = 0
        }
        gs.tableCounts.fill(0)
        gs.tileObjectRedraw.fill(0)
        gs.wipeFrames.fill(0)
        gs.wipeHeights.fill(0)
        gs.redrawFramesAnim.fill(0)
        gs.redrawFrames2.fill(0)
        gs.redrawFramesFloorOverlay.fill(0)
        gs.redrawFramesFull.fill(0)
        gs.redrawFramesFore.fill(0)
        gs.redrawFramesAbove.fill(0)
        gs.drects.forEach {
            it.top = 0
            it.left = 0
            it.bottom = 0
            it.right = 0
        }
        gs.drectsCount = 0
        gs.Kid = CharType()
        gs.Guard = CharType()
        gs.Char = CharType()
        gs.curFrame = FrameType()
        gs.custom = CustomOptionsType()
        gs.fixes = FixesOptionsType()
        ExternalStubs.getRoomAddress = { room -> ExternalStubs.loadRoomAddress(room) }
        ExternalStubs.getImage = { _, _ -> null }
        ExternalStubs.addObjtable = { objType -> Seg008.addObjtable(objType) }
        ExternalStubs.playSound = { _ -> }
        ExternalStubs.displayTextBottom = { _ -> }
        Seg008.resetRenderHooks()
    }

    @Test
    fun addBacktableMapsFieldsAndComputesTopYFromImageHeight() {
        ExternalStubs.getImage = { chtab, imageId ->
            assertEquals(Chtabs.ENVIRONMENT, chtab)
            assertEquals(41, imageId)
            24 to 17
        }

        val result = Seg008.addBacktable(Chtabs.ENVIRONMENT, 42, 260, 255, 100, Blitters.BLACK, 1)

        assertEquals(1, result)
        assertEquals(1, gs.tableCounts[0].toInt())
        val entry = gs.backtable[0]
        assertEquals(4, entry.xh)
        assertEquals(-1, entry.xl)
        assertEquals(Chtabs.ENVIRONMENT, entry.chtabId)
        assertEquals(41, entry.id)
        assertEquals(84, entry.y.toInt())
        assertEquals(Blitters.BLACK, entry.blit)
    }

    @Test
    fun addForetableUsesForetableCountAndDefersImmediateDrawWhenDrawModeIsActive() {
        ExternalStubs.getImage = { _, _ -> 16 to 8 }
        gs.drawMode = 1
        var drawnTable = -1
        var drawnIndex = -1
        Seg008.drawBackForeHook = { table, index ->
            drawnTable = table
            drawnIndex = index
        }

        val result = Seg008.addForetable(Chtabs.ENVIRONMENT, 6, 3, 4, 50, Blitters.TRANSP, 0)

        assertEquals(1, result)
        assertEquals(1, gs.tableCounts[1].toInt())
        assertEquals(1, drawnTable)
        assertEquals(0, drawnIndex)
        assertEquals(43, gs.foretable[0].y.toInt())
    }

    @Test
    fun appendHelpersRejectZeroIdMissingImagesAndFullTablesWithoutChangingCounts() {
        ExternalStubs.getImage = { _, _ -> null }

        assertEquals(0, Seg008.addBacktable(Chtabs.ENVIRONMENT, 0, 1, 2, 3, 4, 0))
        assertEquals(0, gs.tableCounts[0].toInt())

        assertEquals(0, Seg008.addBacktable(Chtabs.ENVIRONMENT, 2, 1, 2, 3, 4, 0))
        assertEquals(0, gs.tableCounts[0].toInt())

        gs.tableCounts[0] = 200
        ExternalStubs.getImage = { _, _ -> 1 to 1 }
        assertEquals(0, Seg008.addBacktable(Chtabs.ENVIRONMENT, 2, 1, 2, 3, 4, 0))
        assertEquals(200, gs.tableCounts[0].toInt())
    }

    @Test
    fun addMidtableCapturesClipFieldsPeelAndRightFacingFlipBlit() {
        ExternalStubs.getImage = { _, _ -> 11 to 9 }
        gs.objDirection = Directions.RIGHT
        gs.objClipLeft = 10
        gs.objClipRight = 90
        gs.objClipTop = 20
        gs.objClipBottom = 80
        gs.drawMode = 1
        var drawnIndex = -1
        Seg008.drawMidHook = { index -> drawnIndex = index }

        val result = Seg008.addMidtable(Chtabs.KID, 7, 1, 2, 40, Blitters.TRANSP, 1)

        assertEquals(1, result)
        assertEquals(1, gs.tableCounts[3].toInt())
        assertEquals(0, drawnIndex)
        val entry = gs.midtable[0]
        assertEquals(1, entry.xh)
        assertEquals(2, entry.xl)
        assertEquals(32, entry.y.toInt())
        assertEquals(Chtabs.KID, entry.chtabId)
        assertEquals(6, entry.id)
        assertEquals(1, entry.peel)
        assertEquals(Blitters.TRANSP + 0x80, entry.blit)
        assertEquals(10, entry.clip.left.toInt())
        assertEquals(90, entry.clip.right.toInt())
        assertEquals(20, entry.clip.top.toInt())
        assertEquals(80, entry.clip.bottom.toInt())
    }

    @Test
    fun addWipetableMapsFieldsBottomPlusOneAndTableLimit() {
        gs.drawMode = 1
        var drawnIndex = -1
        Seg008.drawWipeHook = { index -> drawnIndex = index }

        Seg008.addWipetable(layer = -1, left = 12, bottom = 34, height = 260, width = 56, color = 255)

        assertEquals(1, gs.tableCounts[2].toInt())
        assertEquals(0, drawnIndex)
        val entry = gs.wipetable[0]
        assertEquals(12, entry.left.toInt())
        assertEquals(35, entry.bottom.toInt())
        assertEquals(4, entry.height)
        assertEquals(56, entry.width.toInt())
        assertEquals(-1, entry.color)
        assertEquals(-1, entry.layer)

        gs.tableCounts[2] = 300
        Seg008.addWipetable(layer = 0, left = 1, bottom = 2, height = 3, width = 4, color = 5)
        assertEquals(300, gs.tableCounts[2].toInt())
    }

    @Test
    fun calcScreenXCoordScalesDosLogicalXToScreenX() {
        assertEquals(0, Seg008.calcScreenXCoord(0).toInt())
        assertEquals(160, Seg008.calcScreenXCoord(140).toInt())
        assertEquals(320, Seg008.calcScreenXCoord(280).toInt())
        assertEquals((-11).toShort(), Seg008.calcScreenXCoord((-10).toShort()))
    }

    @Test
    fun showTimeDecrementsTimerAndDisplaysMinuteSecondAndExpiredText() {
        val messages = mutableListOf<String>()
        ExternalStubs.displayTextBottom = { text -> messages += text }
        gs.Kid.alive = -1
        gs.currentLevel = 1
        gs.remMin = 6
        gs.remTick = 1

        Seg008.showTime()

        assertEquals(5, gs.remMin.toInt())
        assertEquals(719, gs.remTick.toInt())
        assertEquals(listOf("5 MINUTES LEFT"), messages)
        assertEquals(24, gs.textTimeRemaining)
        assertEquals(0, gs.isShowTime)

        messages.clear()
        gs.remMin = 1
        gs.remTick = 13
        gs.textTimeRemaining = 0
        Seg008.showTime()
        assertEquals(listOf("1 SECOND LEFT"), messages)
        assertEquals(12, gs.textTimeRemaining)

        messages.clear()
        gs.remMin = 0
        gs.isShowTime = 1
        gs.textTimeRemaining = 0
        Seg008.showTime()
        assertEquals(listOf("TIME HAS EXPIRED!"), messages)
    }

    @Test
    fun showLevelDisplaysConfiguredLevelTextAndResetsSeamlessFlag() {
        val messages = mutableListOf<String>()
        ExternalStubs.displayTextBottom = { text -> messages += text }
        gs.currentLevel = 13
        gs.seamless = 0

        Seg008.showLevel()

        assertEquals(listOf("LEVEL 12"), messages)
        assertEquals(24, gs.textTimeRemaining)
        assertEquals(24, gs.textTimeTotal)
        assertEquals(1, gs.isShowTime)

        messages.clear()
        gs.currentLevel = 2
        gs.seamless = 1
        Seg008.showLevel()
        assertEquals(emptyList(), messages)
        assertEquals(0, gs.seamless)
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

    @Test
    fun addObjtableStoresObjectTemporariesAndMarksTileRedraw() {
        gs.objX = 267
        gs.objY = 144
        gs.objChtab = Chtabs.KID
        gs.objId = 14
        gs.objDirection = Directions.LEFT
        gs.objTilepos = 12
        gs.objClipTop = 3
        gs.objClipLeft = 4
        gs.objClipBottom = 150
        gs.objClipRight = 260

        Seg008.addObjtable(2)

        assertEquals(1, gs.tableCounts[4].toInt())
        assertEquals(2, gs.objtable[0].objType)
        assertEquals(33, gs.objtable[0].xh)
        assertEquals(3, gs.objtable[0].xl)
        assertEquals(144, gs.objtable[0].y.toInt())
        assertEquals(Chtabs.KID, gs.objtable[0].chtabId)
        assertEquals(14, gs.objtable[0].id)
        assertEquals(Directions.LEFT, gs.objtable[0].direction)
        assertEquals(3, gs.objtable[0].clip.top.toInt())
        assertEquals(4, gs.objtable[0].clip.left.toInt())
        assertEquals(150, gs.objtable[0].clip.bottom.toInt())
        assertEquals(260, gs.objtable[0].clip.right.toInt())
        assertEquals(12, gs.objtable[0].tilepos)
        assertEquals(1, gs.tileObjectRedraw[12])
    }

    @Test
    fun loadObjFromObjtableRestoresObjectTemporaries() {
        gs.objtable[3].xh = (-2).toByte().toInt()
        gs.objtable[3].xl = 7
        gs.objtable[3].y = 118
        gs.objtable[3].chtabId = Chtabs.GUARD
        gs.objtable[3].id = 12
        gs.objtable[3].direction = Directions.LEFT
        gs.objtable[3].objType = 0x80
        gs.objtable[3].clip = RectType(1, 2, 3, 4)

        val type = Seg008.loadObjFromObjtable(3)

        assertEquals(0x80, type)
        assertEquals(254, gs.objXh)
        assertEquals((-2).toShort(), gs.objX)
        assertEquals(7, gs.objXl)
        assertEquals(118, gs.objY)
        assertEquals(Chtabs.GUARD, gs.objChtab)
        assertEquals(12, gs.objId)
        assertEquals(Directions.LEFT, gs.objDirection)
        assertEquals(1, gs.objClipTop.toInt())
        assertEquals(4, gs.objClipRight.toInt())
    }

    @Test
    fun sortCurrObjsMatchesCObjectDrawOrderRules() {
        gs.nCurrObjs = 4
        gs.currObjs[0] = 0
        gs.currObjs[1] = 1
        gs.currObjs[2] = 2
        gs.currObjs[3] = 3
        gs.objtable[0].objType = 2
        gs.objtable[0].y = 90
        gs.objtable[1].objType = 2
        gs.objtable[1].y = 120
        gs.objtable[2].objType = 1
        gs.objtable[2].y = 40
        gs.objtable[3].objType = 0x80
        gs.objtable[3].y = 50

        Seg008.sortCurrObjs()

        assertEquals(3, gs.currObjs[0].toInt())
        assertEquals(0, gs.currObjs[1].toInt())
        assertEquals(1, gs.currObjs[2].toInt())
        assertEquals(2, gs.currObjs[3].toInt())
    }

    @Test
    fun sortCurrObjsUsesAscendingYForLooseFloors() {
        gs.nCurrObjs = 2
        gs.currObjs[0] = 0
        gs.currObjs[1] = 1
        gs.objtable[0].objType = 0x80
        gs.objtable[0].y = 120
        gs.objtable[1].objType = 0x80
        gs.objtable[1].y = 80

        Seg008.sortCurrObjs()

        assertEquals(0, gs.currObjs[0].toInt())
        assertEquals(1, gs.currObjs[1].toInt())
    }

    @Test
    fun loadFrameToObjComputesStandingKidObjectCoordinates() {
        gs.Char.charid = CharIds.KID
        gs.Char.frame = FrameIds.frame_15_stand
        gs.Char.direction = Directions.RIGHT
        gs.Char.x = 140
        gs.Char.y = 118

        Seg008.loadFrameToObj()

        assertEquals(14, gs.objId)
        assertEquals(Chtabs.KID, gs.objChtab)
        assertEquals(165, gs.objX.toInt())
        assertEquals(118, gs.objY)
        assertEquals(Directions.RIGHT, gs.objDirection)
    }

    @Test
    fun addKidAndGuardPopulateObjectTableWhenInDrawnRoom() {
        ExternalStubs.getImage = { _, _ -> 20 to 60 }
        gs.drawnRoom = 1
        gs.Kid = CharType(
            charid = CharIds.KID,
            room = 1,
            x = 140,
            y = 118,
            frame = FrameIds.frame_15_stand,
            direction = Directions.RIGHT,
            currCol = 4,
            currRow = 1,
            action = Actions.STAND,
            alive = 0,
        )
        gs.Guard = CharType(
            charid = CharIds.GUARD,
            room = 1,
            x = 170,
            y = 118,
            frame = 150,
            direction = Directions.LEFT,
            currCol = 5,
            currRow = 1,
            action = Actions.STAND,
            alive = 0,
        )

        Seg008.addKidToObjtable()
        Seg008.addGuardToObjtable()

        assertEquals(2, gs.tableCounts[4].toInt())
        assertEquals(0, gs.objtable[0].objType)
        assertEquals(2, gs.objtable[1].objType)
        assertEquals(Chtabs.KID, gs.objtable[0].chtabId)
        assertEquals(Chtabs.GUARD, gs.objtable[1].chtabId)
    }

    @Test
    fun addDrectMergesExpandedIntersectingRectsAndAppendsSeparateRects() {
        Seg008.addDrect(RectType(top = 10, left = 10, bottom = 20, right = 20))
        Seg008.addDrect(RectType(top = 20, left = 20, bottom = 30, right = 30))
        Seg008.addDrect(RectType(top = 50, left = 50, bottom = 60, right = 60))

        assertEquals(2, gs.drectsCount.toInt())
        assertEquals(10, gs.drects[0].top.toInt())
        assertEquals(10, gs.drects[0].left.toInt())
        assertEquals(30, gs.drects[0].bottom.toInt())
        assertEquals(30, gs.drects[0].right.toInt())
        assertEquals(50, gs.drects[1].top.toInt())
        assertEquals(50, gs.drects[1].left.toInt())
        assertEquals(60, gs.drects[1].bottom.toInt())
        assertEquals(60, gs.drects[1].right.toInt())
    }

    @Test
    fun drawTileAndAboveRoomUseCSubmissionOrder() {
        val calls = mutableListOf<String>()
        installTileCallRecorder(calls)

        Seg008.drawTile()
        assertEquals(
            listOf("floorright", "anim_topright", "right", "anim_right", "bottom:0", "loose:0", "base", "anim", "fore"),
            calls,
        )

        calls.clear()
        Seg008.drawTileAboveroom()
        assertEquals(
            listOf("floorright", "anim_topright", "right", "bottom:0", "loose:0", "fore"),
            calls,
        )
    }

    @Test
    fun redrawNeededDecrementsCountersAndClearsTileObjectRedraw() {
        val calls = mutableListOf<String>()
        installTileCallRecorder(calls)
        gs.wipeFrames[4] = 1
        gs.wipeHeights[4] = 7
        gs.redrawFramesAnim[4] = 1
        gs.redrawFrames2[4] = 1
        gs.redrawFramesFore[4] = 1
        gs.tileObjectRedraw[4] = 0xFF

        Seg008.redrawNeeded(4)

        assertEquals(
            listOf(
                "wipe:7",
                "anim_topright",
                "anim_right",
                "anim",
                "fore",
                "bottom:0",
                "other_overlay",
                "obj_at:3",
                "obj_at:4",
                "fore",
            ),
            calls,
        )
        assertEquals(0, gs.wipeFrames[4])
        assertEquals(0, gs.redrawFramesAnim[4])
        assertEquals(0, gs.redrawFrames2[4])
        assertEquals(0, gs.redrawFramesFore[4])
        assertEquals(0, gs.tileObjectRedraw[4])
    }

    @Test
    fun redrawNeededPrefersFullRedrawAndFloorOverlayFallback() {
        val calls = mutableListOf<String>()
        installTileCallRecorder(calls)
        gs.redrawFramesFull[6] = 1
        gs.redrawFramesAnim[6] = 1
        gs.redrawFramesFloorOverlay[6] = 1

        Seg008.redrawNeeded(6)

        assertEquals(
            listOf(
                "floorright",
                "anim_topright",
                "right",
                "anim_right",
                "bottom:0",
                "loose:0",
                "base",
                "anim",
                "fore",
                "floor_overlay",
            ),
            calls,
        )
        assertEquals(0, gs.redrawFramesFull[6])
        assertEquals(1, gs.redrawFramesAnim[6])
        assertEquals(0, gs.redrawFramesFloorOverlay[6])
    }

    @Test
    fun redrawNeededAboveMatchesAboveRoomOrderAndBigPillarFix() {
        val calls = mutableListOf<String>()
        installTileCallRecorder(calls)
        gs.redrawFramesAbove[2] = 1
        gs.currTile = Tiles.FLOOR

        Seg008.redrawNeededAbove(2)

        assertEquals(
            listOf("wipe:3", "floorright", "anim_topright", "right", "bottom:1", "loose:1", "fore"),
            calls,
        )
        assertEquals(0, gs.redrawFramesAbove[2])

        calls.clear()
        gs.redrawFramesAbove[2] = 1
        gs.currTile = Tiles.BIGPILLAR_TOP
        Seg008.redrawNeededAbove(2)
        assertEquals(listOf("anim_topright", "right", "bottom:1", "loose:1", "fore"), calls)
    }

    @Test
    fun tileFloorRightSubmitsToprightAndFloorMaskWhenBottomLeftIsVisible() {
        installFixedImageSize()
        gs.currTile = Tiles.EMPTY
        gs.tileLeft = Tiles.FLOOR
        gs.drawXh = 8
        gs.drawMainY = 62
        gs.drawBottomY = 65
        gs.drawnCol = 2
        gs.rowBelowLeft[2].tiletype = Tiles.WALL

        Seg008.drawTileFloorright()

        assertEquals(2, gs.tableCounts[0].toInt())
        assertBackEntry(0, Chtabs.ENVIRONMENTWALL, 1, 8, 65, Blitters.OR)
        assertBackEntry(1, Chtabs.ENVIRONMENT, 41, 8, 64, Blitters.BLACK)
    }

    @Test
    fun tileRightHandlesFloorStripeAndWallSideSubmissions() {
        installFixedImageSize()
        gs.currTile = Tiles.FLOOR
        gs.tileLeft = Tiles.FLOOR
        gs.modifierLeft = 2
        gs.drawXh = 12
        gs.drawMainY = 62
        gs.currentLevel = 1

        Seg008.drawTileRight()

        assertEquals(2, gs.tableCounts[0].toInt())
        assertBackEntry(0, Chtabs.ENVIRONMENT, 41, 12, 64, Blitters.TRANSP)
        assertBackEntry(1, Chtabs.ENVIRONMENT, 44, 12, 42, Blitters.NO_TRANSP)

        gs.tableCounts.fill(0)
        gs.tileLeft = Tiles.WALL
        gs.modifierLeft = 0
        gs.currentLevel = 4
        Seg008.drawTileRight()

        assertEquals(2, gs.tableCounts[0].toInt())
        assertBackEntry(0, Chtabs.ENVIRONMENT, 83, 15, 35, Blitters.NO_TRANSP)
        assertBackEntry(1, Chtabs.ENVIRONMENTWALL, 0, 12, 64, Blitters.OR)
    }

    @Test
    fun animatedRightSubmitsSpikesLooseTorchAndDelegatesStructures() {
        installFixedImageSize()
        val calls = mutableListOf<String>()
        Seg008.drawGateBackHook = { calls += "gate" }
        Seg008.drawLeveldoorHook = { calls += "leveldoor" }
        gs.drawXh = 4
        gs.drawMainY = 60
        gs.drawBottomY = 65

        gs.tileLeft = Tiles.SPIKE
        gs.modifierLeft = 3
        Seg008.drawTileAnimRight()
        assertBackEntry(0, Chtabs.ENVIRONMENT, 135, 4, 53, Blitters.TRANSP)

        gs.tileLeft = Tiles.LOOSE
        gs.modifierLeft = 4
        Seg008.drawTileAnimRight()
        assertBackEntry(1, Chtabs.ENVIRONMENT, 71, 4, 64, Blitters.OR)

        gs.tileLeft = Tiles.TORCH
        gs.modifierLeft = 8
        Seg008.drawTileAnimRight()
        assertBackEntry(2, Chtabs.FLAMESWORDPOTION, 8, 5, 20, Blitters.NO_TRANSP)

        gs.tileLeft = Tiles.GATE
        Seg008.drawTileAnimRight()
        gs.tileLeft = Tiles.LEVEL_DOOR_LEFT
        Seg008.drawTileAnimRight()
        assertEquals(listOf("gate", "leveldoor"), calls)
    }

    @Test
    fun tileBottomLooseBaseAnimForeAndWipePopulateExpectedTables() {
        installFixedImageSize()
        gs.drawXh = 16
        gs.drawMainY = 62
        gs.drawBottomY = 65
        gs.currTile = Tiles.LOOSE
        gs.currModifier = 3

        Seg008.drawTileBottom(1)
        Seg008.drawLoose(0)
        Seg008.drawTileBase()
        Seg008.drawTileWipe(5)

        assertEquals(2, gs.tableCounts[0].toInt())
        assertEquals(1, gs.tableCounts[1].toInt())
        assertEquals(1, gs.tableCounts[2].toInt())
        assertBackEntry(0, Chtabs.ENVIRONMENT, 73, 16, 65, Blitters.NO_TRANSP)
        assertForeEntry(0, Chtabs.ENVIRONMENT, 73, 16, 65, Blitters.NO_TRANSP)
        assertBackEntry(1, Chtabs.ENVIRONMENT, 69, 16, 62, Blitters.TRANSP)
        assertEquals(128, gs.wipetable[0].left.toInt())
        assertEquals(66, gs.wipetable[0].bottom.toInt())
        assertEquals(5, gs.wipetable[0].height)
        assertEquals(32, gs.wipetable[0].width.toInt())
    }

    @Test
    fun tileAnimAndForeSubmitPotionSwordChomperAndWallCommands() {
        installFixedImageSize()
        gs.drawXh = 20
        gs.drawMainY = 62
        gs.drawBottomY = 65

        gs.currTile = Tiles.POTION
        gs.currModifier = (2 shl 3) or 6
        Seg008.drawTileAnim()
        Seg008.drawTileFore()
        assertEquals(1, gs.tableCounts[0].toInt())
        assertEquals(2, gs.tableCounts[1].toInt())
        assertBackEntry(0, Chtabs.FLAMESWORDPOTION, 22, 23, 44, Blitters.MONO)
        assertForeEntry(0, Chtabs.FLAMESWORDPOTION, 20, 23, 44, 12 + Blitters.MONO)
        assertForeEntry(1, Chtabs.FLAMESWORDPOTION, 12, 22, 59, Blitters.TRANSP)

        gs.tableCounts.fill(0)
        gs.currTile = Tiles.SWORD
        gs.currModifier = 1
        Seg008.drawTileAnim()
        assertEquals(1, gs.tableCounts[3].toInt())
        assertEquals(10, gs.midtable[0].id)
        assertEquals(1, gs.midtable[0].peel)

        gs.tableCounts.fill(0)
        gs.currTile = Tiles.CHOMPER
        gs.currModifier = 0x83
        Seg008.drawTileAnim()
        Seg008.drawTileFore()
        assertEquals(2, gs.tableCounts[0].toInt())
        assertEquals(2, gs.tableCounts[1].toInt())
        assertBackEntry(1, Chtabs.ENVIRONMENT, 114, 21, 56, Blitters.MONO_12)
        assertForeEntry(1, Chtabs.ENVIRONMENT, 119, 21, 56, Blitters.MONO_12)

        gs.tableCounts.fill(0)
        val wallCalls = mutableListOf<String>()
        Seg008.wallPatternHook = { which, table -> wallCalls += "$which:$table" }
        gs.currTile = Tiles.WALL
        gs.currModifier = 2
        gs.graphicsMode = 5
        gs.currentLevel = 1
        Seg008.drawTileBottom(0)
        Seg008.drawTileFore()
        assertBackEntry(0, Chtabs.ENVIRONMENTWALL, 4, 20, 65, Blitters.NO_TRANSP)
        assertForeEntry(0, Chtabs.ENVIRONMENTWALL, 5, 20, 62, Blitters.NO_TRANSP)
        assertEquals(listOf("0:0", "1:1"), wallCalls)
    }

    @Test
    fun wallPatternPopulatesDungeonWallTablesDeterministicallyAndRestoresPrng() {
        installFixedImageSize()
        gs.currentLevel = 1
        gs.drawnRoom = 3
        gs.drawnRow = 1
        gs.drawnCol = 4
        gs.drawXh = 16
        gs.drawBottomY = 128
        gs.currModifier = 3
        gs.randomSeed = 98765
        val originalSeed = gs.randomSeed

        Seg008.wallPattern(1, 0)

        assertEquals(originalSeed, gs.randomSeed)
        assertEquals(true, gs.tableCounts[0].toInt() >= 2)
        val firstRun = (0 until gs.tableCounts[0].toInt()).map { index ->
            val entry = gs.backtable[index]
            listOf(entry.chtabId, entry.id, entry.xh, entry.xl, entry.y.toInt(), entry.blit)
        }

        gs.tableCounts.fill(0)
        gs.backtable.forEach {
            it.chtabId = 0
            it.id = 0
            it.xh = 0
            it.xl = 0
            it.y = 0
            it.blit = 0
        }
        gs.randomSeed = 24680
        Seg008.wallPattern(1, 0)
        val secondRun = (0 until gs.tableCounts[0].toInt()).map { index ->
            val entry = gs.backtable[index]
            listOf(entry.chtabId, entry.id, entry.xh, entry.xl, entry.y.toInt(), entry.blit)
        }

        assertEquals(firstRun, secondRun)
    }

    @Test
    fun wallPatternPopulatesPalaceWipesAndForetableForVgaPalaceLevels() {
        installFixedImageSize()
        gs.currentLevel = 4
        gs.graphicsMode = 5
        gs.drawnRow = 1
        gs.drawnCol = 2
        gs.drawXh = 12
        gs.drawMainY = 70
        gs.drawBottomY = 100
        gs.palaceWallColors[44 + 2] = 31
        gs.palaceWallColors[44 + 11 + 2] = 32
        gs.palaceWallColors[44 + 12 + 2] = 33
        gs.palaceWallColors[44 + 22 + 2] = 34
        gs.palaceWallColors[44 + 23 + 2] = 35
        gs.palaceWallColors[44 + 33 + 2] = 36

        Seg008.wallPattern(1, 1)

        assertEquals(6, gs.tableCounts[2].toInt())
        assertEquals(5, gs.tableCounts[1].toInt())
        assertEquals(96, gs.wipetable[0].left.toInt())
        assertEquals(31, gs.wipetable[0].color)
        assertEquals(1, gs.wipetable[0].layer)
        assertEquals(101, gs.wipetable[5].bottom.toInt())
        assertEquals(36, gs.wipetable[5].color)
        assertEquals(Chtabs.ENVIRONMENTWALL, gs.foretable[0].chtabId)
        assertEquals(Blitters.MONO_6, gs.foretable[0].blit)
    }

    @Test
    fun drawMarksSubmitExpectedWallDecals() {
        installFixedImageSize()
        gs.drawXh = 8
        gs.drawBottomY = 100

        Seg008.drawLeftMark(3, 2, 1)
        Seg008.drawRightMark(2, 9)

        assertEquals(2, gs.tableCounts[0].toInt())
        assertBackEntry(0, Chtabs.ENVIRONMENTWALL, 14, 9, 80, Blitters.TRANSP)
        assertEquals(8, gs.backtable[0].xl)
        assertBackEntry(1, Chtabs.ENVIRONMENTWALL, 15, 9, 69, Blitters.TRANSP)
        assertEquals(6, gs.backtable[1].xl)
    }

    @Test
    fun gateBackAndForeSubmitDoorSlicesFromModifierOpenness() {
        installFixedImageSize()
        gs.drawXh = 8
        gs.drawMainY = 62
        gs.drawBottomY = 65
        gs.modifierLeft = 40
        gs.currTile = Tiles.WALL

        Seg008.drawGateBack()

        assertEquals(8, gs.tableCounts[0].toInt())
        assertBackEntry(0, Chtabs.ENVIRONMENT, 46, 8, 64, Blitters.NO_TRANSP)
        assertBackEntry(2, Chtabs.ENVIRONMENT, 50, 8, 49, Blitters.TRANSP)
        assertBackEntry(7, Chtabs.ENVIRONMENT, 54, 8, 7, Blitters.NO_TRANSP)

        Seg008.drawGateFore()

        assertEquals(5, gs.tableCounts[1].toInt())
        assertForeEntry(0, Chtabs.ENVIRONMENT, 50, 8, 49, Blitters.TRANSP)
        assertForeEntry(4, Chtabs.ENVIRONMENT, 51, 8, 15, Blitters.TRANSP)
    }

    @Test
    fun leveldoorSubmitsStairsWipeSlidingSegmentsAndDoorTop() {
        installFixedImageSize()
        gs.drawXh = 4
        gs.drawMainY = 100
        gs.drawnRoom = 3
        gs.level.startRoom = 3
        gs.modifierLeft = 7

        Seg008.drawLeveldoor()

        assertEquals(14, gs.tableCounts[0].toInt())
        assertEquals(1, gs.tableCounts[2].toInt())
        assertEquals(80, gs.leveldoorRight)
        assertBackEntry(0, Chtabs.ENVIRONMENT, 98, 5, 87, Blitters.NO_TRANSP)
        assertBackEntry(13, Chtabs.ENVIRONMENT, 33, 5, 36, Blitters.NO_TRANSP)
        assertEquals(42, gs.wipetable[0].left.toInt())
        assertEquals(84, gs.wipetable[0].bottom.toInt())
        assertEquals(45, gs.wipetable[0].height)
        assertEquals(39, gs.wipetable[0].width.toInt())
    }

    @Test
    fun floorOverlaySwitchesBottomSubmissionToMidtableForClimbingKid() {
        installFixedImageSize()
        gs.tileLeft = Tiles.EMPTY
        gs.currTile = Tiles.FLOOR
        gs.Kid.frame = FrameIds.frame_137_climbing_3
        gs.drawXh = 12
        gs.drawMainY = 62
        gs.drawBottomY = 65

        Seg008.drawFloorOverlay()

        assertEquals(2, gs.tableCounts[3].toInt())
        assertEquals(Chtabs.ENVIRONMENT, gs.midtable[0].chtabId)
        assertEquals(31, gs.midtable[0].id)
        assertEquals(53, gs.midtable[0].y.toInt())
        assertEquals(42, gs.midtable[1].id)
        assertEquals(56, gs.midtable[1].y.toInt())
        assertEquals(0, gs.tableCounts[0].toInt())
    }

    @Test
    fun otherOverlayDrawsThroughMidtableAndBacktableWhenTileTwoLeftIsEmpty() {
        installFixedImageSize()
        gs.drawnRoom = 1
        gs.drawnRow = 0
        gs.drawnCol = 2
        gs.drawXh = 8
        gs.drawMainY = 62
        gs.drawBottomY = 65
        gs.currTile = Tiles.FLOOR
        gs.tileLeft = Tiles.FLOOR
        seedRoom(1, 0, Tiles.EMPTY, 0)

        Seg008.drawOtherOverlay()

        assertEquals(true, gs.tableCounts[3].toInt() > 0)
        assertEquals(true, gs.tableCounts[0].toInt() > 0)
        assertEquals(0xFF, gs.tileObjectRedraw[2])
    }

    @Test
    fun objectFlushSortsVisibleObjectsAndDrawsMatchingTileOnly() {
        installFixedImageSize()
        putObj(index = 0, tilepos = 5, objType = 2, id = 4, y = 10, xh = 1)
        putObj(index = 1, tilepos = 5, objType = 2, id = 8, y = 30, xh = 2)
        putObj(index = 2, tilepos = 6, objType = 2, id = 12, y = 50, xh = 3)
        gs.tableCounts[4] = 3

        Seg008.drawObjtableItemsAtTile(5)

        assertEquals(2, gs.tableCounts[3].toInt())
        assertEquals(4, gs.midtable[0].id)
        assertEquals(8, gs.midtable[1].id)
        assertEquals(1, gs.midtable[0].xh)
        assertEquals(2, gs.midtable[1].xh)
    }

    @Test
    fun objectItemDrawsShadowBlendAndLooseFloorPieces() {
        installFixedImageSize()
        val sounds = mutableListOf<Int>()
        ExternalStubs.playSound = { sound -> sounds += sound }
        putObj(index = 0, tilepos = 255, objType = 1, id = 6, y = 40, xh = 4, direction = Directions.LEFT)
        gs.tableCounts[4] = 1
        gs.unitedWithShadow = 2

        Seg008.drawObjtableItemsAtTile(-1)

        assertEquals(listOf(SoundIds.END_LEVEL_MUSIC), sounds)
        assertEquals(2, gs.tableCounts[3].toInt())
        assertEquals(Blitters.OR, gs.midtable[0].blit)
        assertEquals(Blitters.XOR, gs.midtable[1].blit)

        gs.tableCounts.fill(0)
        putObj(index = 0, tilepos = 4, objType = 0x80, id = 3, y = 70, xh = 6)
        gs.tableCounts[4] = 1
        Seg008.drawObjtableItemsAtTile(4)

        assertEquals(3, gs.tableCounts[3].toInt())
        assertEquals(69, gs.midtable[0].id)
        assertEquals(73, gs.midtable[1].id)
        assertEquals(71, gs.midtable[2].id)
        assertEquals(10, gs.midtable[2].xh)
    }

    @Test
    fun drawRoomTraversesCurrentRoomThenAboveRoomAndRestoresDrawnRoom() {
        val calls = mutableListOf<String>()
        Seg008.drawTileBaseHook = {
            calls += "base:${gs.drawnRoom}:${gs.drawnRow}:${gs.drawnCol}:${gs.drawBottomY}:${gs.currTile}"
        }
        Seg008.drawTileForeHook = {
            calls += "fore:${gs.drawnRoom}:${gs.drawnRow}:${gs.drawnCol}:${gs.drawBottomY}:${gs.currTile}"
        }
        gs.drawnRoom = 1
        gs.level.roomlinks[0] = LinkType(up = 2)
        seedRoom(1, 20, Tiles.FLOOR, 0)
        seedRoom(1, 0, Tiles.WALL, 0)
        seedRoom(1, 9, Tiles.WALL, 0)
        seedRoom(2, 20, Tiles.POTION, 1)

        Seg008.loadRoomLinks()
        Seg008.drawRoom()

        assertEquals(1, gs.drawnRoom)
        assertEquals(1, gs.loadedRoom)
        assertEquals("base:1:2:0:191:1", calls.first())
        assertEquals("base:1:0:9:65:20", calls.filter { it.startsWith("base:") }.last())
        assertEquals("fore:2:2:0:2:10", calls.takeLast(10).first())
    }

    @Test
    fun redrawNeededTilesTraversesRoomsAndSentinelObjectTiles() {
        val calls = mutableListOf<String>()
        Seg008.drawObjtableItemsAtTileHook = { tilepos -> calls += "obj:$tilepos" }
        Seg008.drawTileWipeHook = { height -> calls += "wipe:${gs.drawnRoom}:${gs.drawnRow}:${gs.drawnCol}:$height" }
        gs.drawnRoom = 1
        gs.level.roomlinks[0] = LinkType(up = 2)
        seedRoom(1, 20, Tiles.FLOOR, 0)
        seedRoom(2, 20, Tiles.POTION, 0)
        gs.wipeFrames[20] = 1
        gs.wipeHeights[20] = 5
        gs.redrawFramesAbove[0] = 1

        Seg008.loadRoomLinks()
        Seg008.redrawNeededTiles()

        assertEquals("obj:30", calls.first())
        assertEquals("obj:-1", calls.last())
        assertEquals(1, gs.drawnRoom)
        assertEquals(1, gs.loadedRoom)
        assertEquals(0, gs.wipeFrames[20])
        assertEquals(0, gs.redrawFramesAbove[0])
        assertEquals(true, calls.contains("wipe:1:2:0:5"))
        assertEquals(true, calls.contains("wipe:2:2:0:3"))
    }

    @Test
    fun drawMovingAddsMobsPeopleAndThenProcessesRedraws() {
        val calls = mutableListOf<String>()
        Seg008.drawPeopleHook = { calls += "people" }
        Seg008.drawObjtableItemsAtTileHook = { tilepos -> calls += "obj:$tilepos" }
        Seg008.drawTileWipeHook = { height -> calls += "wipe:$height" }
        gs.drawnRoom = 1
        seedRoom(1, 0, Tiles.FLOOR, 0)
        gs.mobsCount = 1
        gs.mobs[0] = MobType(xh = 16, y = 80, room = 1, type = 0, row = 1)
        gs.wipeFrames[0] = 1
        gs.wipeHeights[0] = 4

        Seg008.loadRoomLinks()
        Seg008.drawMoving()

        assertEquals("people", calls.first())
        assertEquals(true, gs.tableCounts[4].toInt() > 0)
        assertEquals(true, calls.contains("wipe:4"))
        assertEquals("obj:-1", calls.last())
    }

    private fun installTileCallRecorder(calls: MutableList<String>) {
        Seg008.drawTileFloorrightHook = { calls += "floorright" }
        Seg008.drawTileAnimToprightHook = { calls += "anim_topright" }
        Seg008.drawTileRightHook = { calls += "right" }
        Seg008.drawTileAnimRightHook = { calls += "anim_right" }
        Seg008.drawTileBottomHook = { redrawTop -> calls += "bottom:$redrawTop" }
        Seg008.drawLooseHook = { redrawTop -> calls += "loose:$redrawTop" }
        Seg008.drawTileBaseHook = { calls += "base" }
        Seg008.drawTileAnimHook = { calls += "anim" }
        Seg008.drawTileForeHook = { calls += "fore" }
        Seg008.drawTileWipeHook = { height -> calls += "wipe:$height" }
        Seg008.drawOtherOverlayHook = { calls += "other_overlay" }
        Seg008.drawFloorOverlayHook = { calls += "floor_overlay" }
        Seg008.drawObjtableItemsAtTileHook = { tilepos -> calls += "obj_at:$tilepos" }
    }

    private fun installFixedImageSize() {
        ExternalStubs.getImage = { _, _ -> 16 to 10 }
    }

    private fun assertBackEntry(index: Int, chtab: Int, id: Int, xh: Int, ybottom: Int, blit: Int) {
        val entry = gs.backtable[index]
        assertEquals(chtab, entry.chtabId)
        assertEquals(id, entry.id)
        assertEquals(xh, entry.xh)
        assertEquals(ybottom - 9, entry.y.toInt())
        assertEquals(blit, entry.blit)
    }

    private fun assertForeEntry(index: Int, chtab: Int, id: Int, xh: Int, ybottom: Int, blit: Int) {
        val entry = gs.foretable[index]
        assertEquals(chtab, entry.chtabId)
        assertEquals(id, entry.id)
        assertEquals(xh, entry.xh)
        assertEquals(ybottom - 9, entry.y.toInt())
        assertEquals(blit, entry.blit)
    }

    private fun seedRoom(room: Int, tilepos: Int, tile: Int, modifier: Int) {
        val index = (room - 1) * 30 + tilepos
        gs.level.fg[index] = tile
        gs.level.bg[index] = modifier
    }

    private fun putObj(
        index: Int,
        tilepos: Int,
        objType: Int,
        id: Int,
        y: Int,
        xh: Int,
        direction: Int = Directions.RIGHT,
    ) {
        val entry = gs.objtable[index]
        entry.tilepos = tilepos
        entry.objType = objType
        entry.id = id
        entry.y = y.toShort()
        entry.xh = xh
        entry.xl = 0
        entry.chtabId = Chtabs.GUARD
        entry.direction = direction
        entry.clip = RectType(top = 1.toShort(), left = 2.toShort(), bottom = 3.toShort(), right = 4.toShort())
    }
}
