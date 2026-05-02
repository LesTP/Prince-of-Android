/*
SDLPoP-kotlin, a Kotlin port of SDLPoP (Prince of Persia).
Based on SDLPoP by David Nagy, licensed under GPL v3+.

Module 16, Phase 16c: Pure render-state helpers from seg008.c.
*/

package com.sdlpop.game

object Seg008 {
    private val gs: GameState
        get() = GameState
    private val ext: ExternalStubs
        get() = ExternalStubs

    var drawTileFloorrightHook: () -> Unit = ::drawTileFloorright
    var drawTileAnimToprightHook: () -> Unit = ::drawTileAnimTopright
    var drawTileRightHook: () -> Unit = ::drawTileRight
    var drawTileAnimRightHook: () -> Unit = ::drawTileAnimRight
    var drawTileBottomHook: (Int) -> Unit = ::drawTileBottom
    var drawLooseHook: (Int) -> Unit = ::drawLoose
    var drawTileBaseHook: () -> Unit = ::drawTileBase
    var drawTileAnimHook: () -> Unit = ::drawTileAnim
    var drawTileForeHook: () -> Unit = ::drawTileFore
    var drawTileWipeHook: (Int) -> Unit = ::drawTileWipe
    var drawGateBackHook: () -> Unit = ::drawGateBack
    var drawGateForeHook: () -> Unit = ::drawGateFore
    var drawLeveldoorHook: () -> Unit = ::drawLeveldoor
    var wallPatternHook: (Int, Int) -> Unit = { _, _ -> }
    var drawOtherOverlayHook: () -> Unit = ::drawOtherOverlay
    var drawFloorOverlayHook: () -> Unit = ::drawFloorOverlay
    var drawObjtableItemsAtTileHook: (Int) -> Unit = ::drawObjtableItemsAtTile
    var drawPeopleHook: () -> Unit = ::drawPeople
    var drawBackForeHook: (Int, Int) -> Unit = { _, _ -> }
    var drawMidHook: (Int) -> Unit = {}
    var drawWipeHook: (Int) -> Unit = {}

    val colXh = intArrayOf(0, 4, 8, 12, 16, 20, 24, 28, 32, 36)

    fun resetRenderHooks() {
        drawTileFloorrightHook = ::drawTileFloorright
        drawTileAnimToprightHook = ::drawTileAnimTopright
        drawTileRightHook = ::drawTileRight
        drawTileAnimRightHook = ::drawTileAnimRight
        drawTileBottomHook = ::drawTileBottom
        drawLooseHook = ::drawLoose
        drawTileBaseHook = ::drawTileBase
        drawTileAnimHook = ::drawTileAnim
        drawTileForeHook = ::drawTileFore
        drawTileWipeHook = ::drawTileWipe
        drawGateBackHook = ::drawGateBack
        drawGateForeHook = ::drawGateFore
        drawLeveldoorHook = ::drawLeveldoor
        wallPatternHook = { _, _ -> }
        drawOtherOverlayHook = ::drawOtherOverlay
        drawFloorOverlayHook = ::drawFloorOverlay
        drawObjtableItemsAtTileHook = ::drawObjtableItemsAtTile
        drawPeopleHook = ::drawPeople
        drawBackForeHook = { _, _ -> }
        drawMidHook = {}
        drawWipeHook = {}
        addTable = ::addBacktable
    }

    data class Piece(
        val baseId: Int,
        val floorLeft: Int,
        val baseY: Int,
        val rightId: Int,
        val floorRight: Int,
        val rightY: Int,
        val stripeId: Int,
        val toprightId: Int,
        val bottomId: Int,
        val foreId: Int,
        val foreX: Int,
        val foreY: Int,
    )

    val tileTable = arrayOf(
        Piece(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
        Piece(41, 1, 0, 42, 1, 2, 145, 0, 43, 0, 0, 0),
        Piece(127, 1, 0, 133, 1, 2, 145, 0, 43, 0, 1, 0),
        Piece(92, 1, 0, 93, 1, 2, 0, 94, 43, 95, 1, 0),
        Piece(46, 1, 0, 47, 1, 2, 0, 48, 43, 49, 3, 0),
        Piece(41, 1, 1, 35, 1, 3, 145, 0, 36, 0, 0, 0),
        Piece(41, 1, 0, 42, 1, 2, 145, 0, 96, 0, 0, 0),
        Piece(46, 1, 0, 0, 0, 2, 0, 0, 43, 49, 3, 0),
        Piece(86, 1, 0, 87, 1, 2, 0, 0, 43, 88, 1, 0),
        Piece(0, 0, 0, 89, 0, 3, 0, 90, 0, 91, 1, 3),
        Piece(41, 1, 0, 42, 1, 2, 145, 0, 43, 12, 2, -3),
        Piece(0, 1, 0, 0, 0, 0, 145, 0, 0, 0, 0, 0),
        Piece(0, 0, 0, 0, 0, 2, 0, 0, 85, 49, 3, 0),
        Piece(75, 1, 0, 42, 1, 2, 0, 0, 43, 77, 0, 0),
        Piece(97, 1, 0, 98, 1, 2, 145, 0, 43, 100, 0, 0),
        Piece(147, 1, 0, 42, 1, 1, 145, 0, 149, 0, 0, 0),
        Piece(41, 1, 0, 37, 0, 0, 0, 38, 43, 0, 0, 0),
        Piece(0, 0, 0, 39, 1, 2, 0, 40, 43, 0, 0, 0),
        Piece(0, 0, 0, 42, 1, 2, 145, 0, 43, 0, 0, 0),
        Piece(41, 1, 0, 42, 1, 2, 0, 0, 43, 0, 0, 0),
        Piece(0, 0, 0, 1, 1, 2, 0, 2, 0, 0, 0, 0),
        Piece(30, 1, 0, 31, 1, 2, 0, 0, 43, 0, 0, 0),
        Piece(41, 1, 0, 42, 1, 2, 145, 0, 43, 0, 0, 0),
        Piece(41, 1, 0, 10, 0, 0, 0, 11, 43, 0, 0, 0),
        Piece(0, 0, 0, 12, 1, 2, 0, 13, 43, 0, 0, 0),
        Piece(92, 1, 0, 42, 1, 2, 145, 0, 43, 95, 1, 0),
        Piece(1, 0, 0, 0, 0, 0, 0, 0, 2, 9, 0, -53),
        Piece(3, 0, -10, 0, 0, 0, 0, 0, 0, 9, 0, -53),
        Piece(4, 0, -10, 0, 0, 0, 0, 0, 0, 9, 0, -53),
        Piece(5, 0, -10, 0, 0, 0, 0, 0, 0, 9, 0, -53),
        Piece(97, 1, 0, 42, 1, 2, 145, 0, 43, 100, 0, 0),
    )

    private val doortopFramTop = intArrayOf(0, 81, 83, 0)
    private val doorFramTop = intArrayOf(60, 61, 62, 63, 64, 65, 66, 67)
    private val doorFramSlice = intArrayOf(67, 59, 58, 57, 56, 55, 54, 53, 52)
    private val bluelineFram1 = intArrayOf(0, 124, 125, 126)
    private val bluelineFramY = intArrayOf(0, -20, -20, 0)
    private val bluelineFram3 = intArrayOf(44, 44, 45, 45)
    private val doortopFramBot = intArrayOf(78, 80, 82, 0)
    private val spikesFramRight = intArrayOf(0, 134, 135, 136, 137, 138, 137, 135, 134, 0)
    private val looseFramRight = intArrayOf(42, 71, 42, 72, 72, 42, 42, 42, 72, 72, 72, 0)
    private val wallFramBottom = intArrayOf(7, 9, 5, 3)
    private val looseFramBottom = intArrayOf(43, 73, 43, 74, 74, 43, 43, 43, 74, 74, 74, 0)
    private val looseFramLeft = intArrayOf(41, 69, 41, 70, 70, 41, 41, 41, 70, 70, 70, 0)
    private val spikesFramLeft = intArrayOf(0, 128, 129, 130, 131, 132, 131, 129, 128, 0)
    private val potionFramBubb = intArrayOf(0, 16, 17, 18, 19, 20, 21, 22)
    private val chomperFram1 = intArrayOf(3, 2, 0, 1, 4, 3, 3, 0)
    private val chomperFramBot = intArrayOf(101, 102, 103, 104, 105, 0)
    private val chomperFramTop = intArrayOf(0, 0, 111, 112, 113, 0)
    private val chomperFramY = intArrayOf(0, 0, 0x25, 0x2F, 0x32)
    private val spikesFramFore = intArrayOf(0, 139, 140, 141, 142, 143, 142, 140, 139, 0)
    private val chomperFramFore = intArrayOf(106, 107, 108, 109, 110, 0)
    private val wallFramMain = intArrayOf(8, 10, 6, 4)
    private val floorLeftOverlay = intArrayOf(32, 151, 151, 150, 150, 151, 32, 32)

    private const val GM_CGA = 1
    private const val GM_HGA_HERC = 2
    private const val GM_MCGA_VGA = 5

    private var addTable: (Int, Int, Int, Int, Int, Int, Int) -> Int = ::addBacktable

    @Suppress("UNUSED_PARAMETER")
    fun addBacktable(chtabId: Int, id: Int, xh: Int, xl: Int, ybottom: Int, blit: Int, peel: Int): Int {
        if (id == 0) return 0
        val index = gs.tableCounts[0].toInt()
        if (index >= 200) return 0
        val item = gs.backtable[index]
        populateBackTable(item, chtabId, id, xh, xl)
        val image = ext.getImage(chtabId, id - 1) ?: return 0
        item.y = (ybottom - image.second + 1).toShort()
        item.blit = blit
        if (gs.drawMode != 0) drawBackForeHook(0, index)
        gs.tableCounts[0] = (index + 1).toShort()
        return 1
    }

    @Suppress("UNUSED_PARAMETER")
    fun addForetable(chtabId: Int, id: Int, xh: Int, xl: Int, ybottom: Int, blit: Int, peel: Int): Int {
        if (id == 0) return 0
        val index = gs.tableCounts[1].toInt()
        if (index >= 200) return 0
        val item = gs.foretable[index]
        populateBackTable(item, chtabId, id, xh, xl)
        val image = ext.getImage(chtabId, id - 1) ?: return 0
        item.y = (ybottom - image.second + 1).toShort()
        item.blit = blit
        if (gs.drawMode != 0) drawBackForeHook(1, index)
        gs.tableCounts[1] = (index + 1).toShort()
        return 1
    }

    fun addMidtable(chtabId: Int, id: Int, xh: Int, xl: Int, ybottom: Int, blit: Int, peel: Int): Int {
        if (id == 0) return 0
        val index = gs.tableCounts[3].toInt()
        if (index >= 50) return 0
        val item = gs.midtable[index]
        item.xh = xh.toByte().toInt()
        item.xl = xl.toByte().toInt()
        item.chtabId = chtabId and 0xFF
        item.id = (id - 1) and 0xFF
        val image = ext.getImage(chtabId, id - 1) ?: return 0
        item.y = (ybottom - image.second + 1).toShort()
        item.blit = blit + if (gs.objDirection == Directions.RIGHT && (gs.chtabFlipClip.getOrNull(chtabId) ?: 0) != 0) 0x80 else 0
        item.peel = peel and 0xFF
        item.clip.left = gs.objClipLeft
        item.clip.right = gs.objClipRight
        item.clip.top = gs.objClipTop
        item.clip.bottom = gs.objClipBottom
        if (gs.drawMode != 0) drawMidHook(index)
        gs.tableCounts[3] = (index + 1).toShort()
        return 1
    }

    fun addWipetable(layer: Int, left: Int, bottom: Int, height: Int, width: Int, color: Int) {
        val index = gs.tableCounts[2].toInt()
        if (index >= 300) return
        val item = gs.wipetable[index]
        item.left = left.toShort()
        item.bottom = (bottom + 1).toShort()
        item.height = height.toByte().toInt()
        item.width = width.toShort()
        item.color = color.toByte().toInt()
        item.layer = layer.toByte().toInt()
        if (gs.drawMode != 0) drawWipeHook(index)
        gs.tableCounts[2] = (index + 1).toShort()
    }

    private fun populateBackTable(item: BackTableType, chtabId: Int, id: Int, xh: Int, xl: Int) {
        item.xh = xh.toByte().toInt()
        item.xl = xl.toByte().toInt()
        item.chtabId = chtabId and 0xFF
        item.id = (id - 1) and 0xFF
    }

    fun drawRoom() {
        loadLeftroom()
        for (row in 2 downTo 0) {
            gs.drawnRow = row.toShort()
            loadRowbelow()
            gs.drawBottomY = (63 * row + 65).toShort()
            gs.drawMainY = (gs.drawBottomY.toInt() - 3).toShort()
            for (column in 0 until 10) {
                gs.drawnCol = column.toShort()
                loadCurrAndLeftTile()
                drawTile()
            }
        }

        val savedRoom = gs.drawnRoom
        gs.drawnRoom = gs.roomA
        loadRoomLinks()
        loadLeftroom()
        gs.drawnRow = 2
        loadRowbelow()
        for (column in 0 until 10) {
            gs.drawnCol = column.toShort()
            loadCurrAndLeftTile()
            gs.drawMainY = (-1).toShort()
            gs.drawBottomY = 2
            drawTileAboveroom()
        }
        gs.drawnRoom = savedRoom
        loadRoomLinks()
    }

    fun drawTile() {
        drawTileFloorrightHook()
        drawTileAnimToprightHook()
        drawTileRightHook()
        drawTileAnimRightHook()
        drawTileBottomHook(0)
        drawLooseHook(0)
        drawTileBaseHook()
        drawTileAnimHook()
        drawTileForeHook()
    }

    fun drawTileAboveroom() {
        drawTileFloorrightHook()
        drawTileAnimToprightHook()
        drawTileRightHook()
        drawTileBottomHook(0)
        drawLooseHook(0)
        drawTileForeHook()
    }

    fun redrawNeeded(tilepos: Int) {
        if (gs.wipeFrames[tilepos] != 0) {
            gs.wipeFrames[tilepos]--
            drawTileWipeHook(gs.wipeHeights[tilepos])
        }
        if (gs.redrawFramesFull[tilepos] != 0) {
            gs.redrawFramesFull[tilepos]--
            drawTile()
        } else if (gs.redrawFramesAnim[tilepos] != 0) {
            gs.redrawFramesAnim[tilepos]--
            drawTileAnimToprightHook()
            drawTileAnimRightHook()
            drawTileAnimHook()
            drawTileForeHook()
            drawTileBottomHook(0)
        }
        if (gs.redrawFrames2[tilepos] != 0) {
            gs.redrawFrames2[tilepos]--
            drawOtherOverlayHook()
        } else if (gs.redrawFramesFloorOverlay[tilepos] != 0) {
            gs.redrawFramesFloorOverlay[tilepos]--
            drawFloorOverlayHook()
        }
        if (gs.tileObjectRedraw[tilepos] != 0) {
            if (gs.tileObjectRedraw[tilepos] == 0xFF) {
                drawObjtableItemsAtTileHook(tilepos - 1)
            }
            drawObjtableItemsAtTileHook(tilepos)
            gs.tileObjectRedraw[tilepos] = 0
        }
        if (gs.redrawFramesFore[tilepos] != 0) {
            gs.redrawFramesFore[tilepos]--
            drawTileForeHook()
        }
    }

    fun redrawNeededAbove(column: Int) {
        if (gs.redrawFramesAbove[column] == 0) return
        gs.redrawFramesAbove[column]--
        if (gs.currTile != Tiles.BIGPILLAR_TOP) {
            drawTileWipeHook(3)
            drawTileFloorrightHook()
        }
        drawTileAnimToprightHook()
        drawTileRightHook()
        drawTileBottomHook(1)
        drawLooseHook(1)
        drawTileForeHook()
    }

    fun drawMoving() {
        Seg007.drawMobs()
        drawPeopleHook()
        redrawNeededTiles()
    }

    fun redrawNeededTiles() {
        loadLeftroom()
        drawObjtableItemsAtTileHook(30)
        for (row in 2 downTo 0) {
            gs.drawnRow = row.toShort()
            loadRowbelow()
            gs.drawBottomY = (63 * row + 65).toShort()
            gs.drawMainY = (gs.drawBottomY.toInt() - 3).toShort()
            for (column in 0 until 10) {
                gs.drawnCol = column.toShort()
                loadCurrAndLeftTile()
                redrawNeeded(gs.tblLine[row] + column)
            }
        }

        val savedRoom = gs.drawnRoom
        gs.drawnRoom = gs.roomA
        loadRoomLinks()
        loadLeftroom()
        gs.drawnRow = 2
        loadRowbelow()
        for (column in 0 until 10) {
            gs.drawnCol = column.toShort()
            loadCurrAndLeftTile()
            gs.drawMainY = (-1).toShort()
            gs.drawBottomY = 2
            redrawNeededAbove(column)
        }
        gs.drawnRoom = savedRoom
        loadRoomLinks()
        drawObjtableItemsAtTileHook(-1)
    }

    fun canSeeBottomleft(): Int {
        return if (
            gs.currTile == Tiles.EMPTY ||
            gs.currTile == Tiles.BIGPILLAR_TOP ||
            gs.currTile == Tiles.DOORTOP ||
            gs.currTile == Tiles.LATTICE_DOWN
        ) 1 else 0
    }

    fun drawTileFloorright() {
        if (canSeeBottomleft() == 0) return
        drawTileTopright()
        if (tileTable[gs.tileLeft].floorRight == 0) return
        addBacktable(
            Chtabs.ENVIRONMENT,
            42,
            gs.drawXh,
            0,
            tileTable[Tiles.FLOOR].rightY + gs.drawMainY.toInt(),
            Blitters.BLACK,
            1,
        )
    }

    fun drawTileTopright() {
        val tiletype = gs.rowBelowLeft[gs.drawnCol.toInt()].tiletype and 0xFF
        val modifier = gs.rowBelowLeft[gs.drawnCol.toInt()].modifier and 0xFF
        when (tiletype) {
            Tiles.DOORTOP_WITH_FLOOR, Tiles.DOORTOP -> {
                if (levelType() == 0) return
                addBacktable(Chtabs.ENVIRONMENT, doortopFramTop[modifier], gs.drawXh, 0, gs.drawBottomY.toInt(), Blitters.OR, 0)
            }
            Tiles.WALL -> {
                addBacktable(Chtabs.ENVIRONMENTWALL, 2, gs.drawXh, 0, gs.drawBottomY.toInt(), Blitters.OR, 0)
            }
            else -> {
                addBacktable(Chtabs.ENVIRONMENT, tileTable[tiletype].toprightId, gs.drawXh, 0, gs.drawBottomY.toInt(), Blitters.OR, 0)
            }
        }
    }

    fun drawTileAnimTopright() {
        if ((gs.currTile == Tiles.EMPTY || gs.currTile == Tiles.BIGPILLAR_TOP || gs.currTile == Tiles.DOORTOP) &&
            gs.rowBelowLeft[gs.drawnCol.toInt()].tiletype == Tiles.GATE
        ) {
            addBacktable(Chtabs.ENVIRONMENT, 68, gs.drawXh, 0, gs.drawBottomY.toInt(), Blitters.MONO, 0)
            val modifier = minOf(gs.rowBelowLeft[gs.drawnCol.toInt()].modifier and 0xFF, 188)
            addBacktable(Chtabs.ENVIRONMENT, doorFramTop[(modifier shr 2) % 8], gs.drawXh, 0, gs.drawBottomY.toInt(), Blitters.OR, 0)
        }
    }

    fun drawTileRight() {
        if (gs.currTile == Tiles.WALL) return
        when (gs.tileLeft) {
            Tiles.EMPTY -> {
                if (gs.modifierLeft > 3) return
                addBacktable(
                    Chtabs.ENVIRONMENT,
                    bluelineFram1[gs.modifierLeft],
                    gs.drawXh,
                    0,
                    bluelineFramY[gs.modifierLeft] + gs.drawMainY.toInt(),
                    Blitters.OR,
                    0,
                )
            }
            Tiles.FLOOR -> {
                addTable(Chtabs.ENVIRONMENT, 42, gs.drawXh, 0, tileTable[gs.tileLeft].rightY + gs.drawMainY.toInt(), Blitters.TRANSP, 0)
                var num = gs.modifierLeft
                if (num > 3) num = 0
                if (num == if (levelType() != 0) 1 else 0) return
                addBacktable(Chtabs.ENVIRONMENT, bluelineFram3[num], gs.drawXh, 0, gs.drawMainY.toInt() - 20, Blitters.NO_TRANSP, 0)
            }
            Tiles.DOORTOP_WITH_FLOOR, Tiles.DOORTOP -> {
                if (levelType() == 0) return
                addBacktable(
                    Chtabs.ENVIRONMENT,
                    doortopFramBot[gs.modifierLeft],
                    gs.drawXh,
                    0,
                    tileTable[gs.tileLeft].rightY + gs.drawMainY.toInt(),
                    Blitters.OR,
                    0,
                )
            }
            Tiles.WALL -> {
                if (levelType() != 0 && (gs.modifierLeft and 0x80) == 0) {
                    addBacktable(Chtabs.ENVIRONMENT, 84, gs.drawXh + 3, 0, gs.drawMainY.toInt() - 27, Blitters.NO_TRANSP, 0)
                }
                addBacktable(
                    Chtabs.ENVIRONMENTWALL,
                    1,
                    gs.drawXh,
                    0,
                    tileTable[gs.tileLeft].rightY + gs.drawMainY.toInt(),
                    Blitters.OR,
                    0,
                )
            }
            else -> {
                var id = tileTable[gs.tileLeft].rightId
                if (id != 0) {
                    val blit = if (gs.tileLeft == Tiles.STUCK) {
                        if (gs.currTile == Tiles.EMPTY || gs.currTile == Tiles.STUCK || Seg006.tileIsFloor(gs.currTile) == 0) {
                            id = 42
                        }
                        Blitters.TRANSP
                    } else {
                        Blitters.OR
                    }
                    addBacktable(Chtabs.ENVIRONMENT, id, gs.drawXh, 0, tileTable[gs.tileLeft].rightY + gs.drawMainY.toInt(), blit, 0)
                }
                if (levelType() != 0) {
                    addBacktable(Chtabs.ENVIRONMENT, tileTable[gs.tileLeft].stripeId, gs.drawXh, 0, gs.drawMainY.toInt() - 27, Blitters.OR, 0)
                }
                if (gs.tileLeft == Tiles.TORCH || gs.tileLeft == Tiles.TORCH_WITH_DEBRIS) {
                    addBacktable(Chtabs.ENVIRONMENT, 146, gs.drawXh, 0, gs.drawBottomY.toInt() - 28, Blitters.NO_TRANSP, 0)
                }
            }
        }
    }

    fun drawTileAnimRight() {
        when (gs.tileLeft) {
            Tiles.SPIKE -> addBacktable(
                Chtabs.ENVIRONMENT,
                spikesFramRight[getSpikeFrame(gs.modifierLeft)],
                gs.drawXh,
                0,
                gs.drawMainY.toInt() - 7,
                Blitters.TRANSP,
                0,
            )
            Tiles.GATE -> drawGateBackHook()
            Tiles.LOOSE -> addBacktable(
                Chtabs.ENVIRONMENT,
                looseFramRight[getLooseFrame(gs.modifierLeft)],
                gs.drawXh,
                0,
                gs.drawBottomY.toInt() - 1,
                Blitters.OR,
                0,
            )
            Tiles.LEVEL_DOOR_LEFT -> drawLeveldoorHook()
            Tiles.TORCH, Tiles.TORCH_WITH_DEBRIS -> {
                if (gs.modifierLeft < 9) {
                    addBacktable(
                        Chtabs.FLAMESWORDPOTION,
                        gs.modifierLeft + 1,
                        gs.drawXh + 1,
                        0,
                        gs.drawMainY.toInt() - 40,
                        torchBlitForLeftTile(),
                        0,
                    )
                }
            }
        }
    }

    fun drawTileBottom(redrawTop: Int) {
        var id = 0
        var blit = Blitters.NO_TRANSP
        var chtabId = Chtabs.ENVIRONMENT
        when (gs.currTile) {
            Tiles.WALL -> {
                if (levelType() == 0 || gs.custom.enableWdaInPalace != 0 || gs.graphicsMode != GM_MCGA_VGA) {
                    id = wallFramBottom[gs.currModifier and 0x7F]
                }
                chtabId = Chtabs.ENVIRONMENTWALL
            }
            Tiles.DOORTOP -> {
                blit = Blitters.OR
                id = tileTable[gs.currTile].bottomId
            }
            else -> id = tileTable[gs.currTile].bottomId
        }
        if (addTable(chtabId, id, gs.drawXh, 0, gs.drawBottomY.toInt(), blit, 0) != 0 && redrawTop != 0) {
            addForetable(chtabId, id, gs.drawXh, 0, gs.drawBottomY.toInt(), blit, 0)
        }
        if (chtabId == Chtabs.ENVIRONMENTWALL && gs.graphicsMode != GM_CGA && gs.graphicsMode != GM_HGA_HERC) {
            wallPatternHook(0, 0)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun drawLoose(redrawTop: Int) {
        if (gs.currTile != Tiles.LOOSE) return
        val id = looseFramBottom[getLooseFrame(gs.currModifier)]
        addBacktable(Chtabs.ENVIRONMENT, id, gs.drawXh, 0, gs.drawBottomY.toInt(), Blitters.NO_TRANSP, 0)
        addForetable(Chtabs.ENVIRONMENT, id, gs.drawXh, 0, gs.drawBottomY.toInt(), Blitters.NO_TRANSP, 0)
    }

    fun drawTileBase() {
        var ybottom = gs.drawMainY.toInt()
        val id = when {
            gs.tileLeft == Tiles.LATTICE_DOWN && gs.currTile == Tiles.DOORTOP -> {
                ybottom += 3
                6
            }
            gs.currTile == Tiles.LOOSE -> looseFramLeft[getLooseFrame(gs.currModifier)]
            gs.currTile == Tiles.OPENER && gs.tileLeft == Tiles.EMPTY && levelType() == 0 -> 148
            else -> tileTable[gs.currTile].baseId
        }
        addTable(Chtabs.ENVIRONMENT, id, gs.drawXh, 0, tileTable[gs.currTile].baseY + ybottom, Blitters.TRANSP, 0)
    }

    fun drawTileAnim() {
        when (gs.currTile) {
            Tiles.SPIKE -> addTable(
                Chtabs.ENVIRONMENT,
                spikesFramLeft[getSpikeFrame(gs.currModifier)],
                gs.drawXh,
                0,
                gs.drawMainY.toInt() - 2,
                Blitters.TRANSP,
                0,
            )
            Tiles.POTION -> drawPotionBubbles()
            Tiles.SWORD -> addMidtable(
                Chtabs.FLAMESWORDPOTION,
                if (gs.currModifier == 1) 11 else 10,
                gs.drawXh,
                0,
                gs.drawMainY.toInt() - 3,
                Blitters.TRANSP,
                if (gs.currModifier == 1) 1 else 0,
            )
            Tiles.CHOMPER -> {
                val chomperNum = chomperFram1[minOf(gs.currModifier and 0x7F, 6)]
                addBacktable(Chtabs.ENVIRONMENT, chomperFramBot[chomperNum], gs.drawXh, 0, gs.drawMainY.toInt(), Blitters.TRANSP, 0)
                if (gs.currModifier and 0x80 != 0) {
                    addBacktable(Chtabs.ENVIRONMENT, chomperNum + 114, gs.drawXh + 1, 4, gs.drawMainY.toInt() - 6, Blitters.MONO_12, 0)
                }
                addBacktable(
                    Chtabs.ENVIRONMENT,
                    chomperFramTop[chomperNum],
                    gs.drawXh,
                    0,
                    gs.drawMainY.toInt() - chomperFramY[chomperNum],
                    Blitters.TRANSP,
                    0,
                )
            }
        }
    }

    fun drawTileFore() {
        if (gs.tileLeft == Tiles.GATE &&
            gs.Kid.currRow == gs.drawnRow.toInt() &&
            gs.Kid.currCol == gs.drawnCol.toInt() - 1 &&
            gs.Kid.room != gs.roomR
        ) {
            drawGateForeHook()
        }
        when (gs.currTile) {
            Tiles.SPIKE -> addForetable(Chtabs.ENVIRONMENT, spikesFramFore[getSpikeFrame(gs.currModifier)], gs.drawXh, 0, gs.drawMainY.toInt() - 2, Blitters.TRANSP, 0)
            Tiles.CHOMPER -> {
                val chomperNum = chomperFram1[minOf(gs.currModifier and 0x7F, 6)]
                addForetable(Chtabs.ENVIRONMENT, chomperFramFore[chomperNum], gs.drawXh, 0, gs.drawMainY.toInt(), Blitters.TRANSP, 0)
                if (gs.currModifier and 0x80 != 0) {
                    addForetable(Chtabs.ENVIRONMENT, chomperNum + 119, gs.drawXh + 1, 4, gs.drawMainY.toInt() - 6, Blitters.MONO_12, 0)
                }
            }
            Tiles.WALL -> {
                if (levelType() == 0 || gs.custom.enableWdaInPalace != 0 || gs.graphicsMode != GM_MCGA_VGA) {
                    addForetable(Chtabs.ENVIRONMENTWALL, wallFramMain[gs.currModifier and 0x7F], gs.drawXh, 0, gs.drawMainY.toInt(), Blitters.NO_TRANSP, 0)
                }
                if (gs.graphicsMode != GM_CGA && gs.graphicsMode != GM_HGA_HERC) {
                    wallPatternHook(1, 1)
                }
            }
            else -> drawDefaultFore()
        }
    }

    fun drawTile2() {
        drawTileRight()
        drawTileAnimRight()
        drawTileBase()
        drawTileAnim()
        drawTileBottom(0)
        drawLoose(0)
    }

    fun drawGateBack() {
        calcGatePos()
        if (gs.gateBottomY + 12 < gs.drawMainY.toInt()) {
            addBacktable(Chtabs.ENVIRONMENT, 50, gs.drawXh, 0, gs.gateBottomY, Blitters.NO_TRANSP, 0)
        } else {
            addBacktable(
                Chtabs.ENVIRONMENT,
                tileTable[Tiles.GATE].rightId,
                gs.drawXh,
                0,
                tileTable[Tiles.GATE].rightY + gs.drawMainY.toInt(),
                Blitters.NO_TRANSP,
                0,
            )
            if (canSeeBottomleft() != 0) drawTileTopright()
            if (gs.fixes.fixGateDrawingBug != 0) drawTileAnimTopright()
            drawTileBottom(0)
            drawLoose(0)
            drawTileBase()
            addBacktable(Chtabs.ENVIRONMENT, 51, gs.drawXh, 0, gs.gateBottomY - 2, Blitters.TRANSP, 0)
        }

        var ybottom = gs.gateBottomY - 12
        if (ybottom < 192) {
            while (ybottom >= 0 && ybottom > 7 && ybottom - 7 > gs.gateTopY) {
                addBacktable(Chtabs.ENVIRONMENT, 52, gs.drawXh, 0, ybottom, Blitters.NO_TRANSP, 0)
                ybottom -= 8
            }
        }
        val gateFrame = (ybottom - gs.gateTopY + 1) and 0xFFFF
        if (gateFrame in 1..8) {
            addBacktable(Chtabs.ENVIRONMENT, doorFramSlice[gateFrame], gs.drawXh, 0, ybottom, Blitters.NO_TRANSP, 0)
        }
    }

    fun drawGateFore() {
        calcGatePos()
        addForetable(Chtabs.ENVIRONMENT, 51, gs.drawXh, 0, gs.gateBottomY - 2, Blitters.TRANSP, 0)
        var ybottom = gs.gateBottomY - 12
        if (ybottom < 192) {
            while (ybottom >= 0 && ybottom > 7 && ybottom - 7 > gs.gateTopY) {
                addForetable(Chtabs.ENVIRONMENT, 52, gs.drawXh, 0, ybottom, Blitters.TRANSP, 0)
                ybottom -= 8
            }
        }
    }

    fun drawLeveldoor() {
        val ybottom = gs.drawMainY.toInt() - 13
        gs.leveldoorRight = (gs.drawXh shl 3) + 48
        if (levelType() != 0) gs.leveldoorRight += 8
        addBacktable(Chtabs.ENVIRONMENT, 99, gs.drawXh + 1, 0, ybottom, Blitters.NO_TRANSP, 0)
        if (gs.modifierLeft != 0) {
            if (gs.level.startRoom != gs.drawnRoom) {
                addBacktable(Chtabs.ENVIRONMENT, 144, gs.drawXh + 1, 0, ybottom - 4, Blitters.NO_TRANSP, 0)
            } else {
                val width = if (levelType() == 0) 39 else 48
                val xLow = if (levelType() == 0) 2 else 0
                addWipetable(0, 8 * (gs.drawXh + 1) + xLow, ybottom - 4, 45, width, 0)
            }
        }
        gs.leveldoorYbottom = ybottom - (gs.modifierLeft and 3) - 48
        var y = ybottom - gs.modifierLeft
        while (true) {
            addBacktable(Chtabs.ENVIRONMENT, 33, gs.drawXh + 1, 0, gs.leveldoorYbottom, Blitters.NO_TRANSP, 0)
            if (y > gs.leveldoorYbottom) {
                gs.leveldoorYbottom += 4
            } else {
                break
            }
        }
        addBacktable(Chtabs.ENVIRONMENT, 34, gs.drawXh + 1, 0, gs.drawMainY.toInt() - 64, Blitters.NO_TRANSP, 0)
    }

    fun drawFloorOverlay() {
        val leftIsTransparent = gs.tileLeft == Tiles.EMPTY ||
            (gs.fixes.fixBigpillarClimb != 0 && gs.tileLeft == Tiles.BIGPILLAR_TOP)
        if (!leftIsTransparent) return

        if (gs.currTile == Tiles.FLOOR ||
            gs.currTile == Tiles.PILLAR ||
            gs.currTile == Tiles.STUCK ||
            gs.currTile == Tiles.TORCH
        ) {
            if (gs.Kid.frame in FrameIds.frame_137_climbing_3..FrameIds.frame_144_climbing_10) {
                addMidtable(
                    Chtabs.ENVIRONMENT,
                    floorLeftOverlay[gs.Kid.frame - FrameIds.frame_137_climbing_3],
                    gs.drawXh,
                    0,
                    if (gs.currTile == Tiles.STUCK) gs.drawMainY.toInt() + 1 else gs.drawMainY.toInt(),
                    Blitters.TRANSP,
                    0,
                )
            }
            addTable = ::addMidtable
            drawTileBottom(0)
            addTable = ::addBacktable
        } else {
            drawOtherOverlay()
        }
    }

    fun drawOtherOverlay() {
        if (gs.tileLeft == Tiles.EMPTY) {
            addTable = ::addMidtable
            drawTile2()
        } else if (gs.currTile != Tiles.EMPTY && gs.drawnCol.toInt() > 0) {
            val tile = TileAndMod()
            if (getTileToDraw(gs.drawnRoom, gs.drawnCol.toInt() - 2, gs.drawnRow.toInt(), tile, Tiles.EMPTY) == Tiles.EMPTY) {
                addTable = ::addMidtable
                drawTile2()
                addTable = ::addBacktable
                drawTile2()
                gs.tileObjectRedraw[gs.tblLine[gs.drawnRow.toInt()] + gs.drawnCol.toInt()] = 0xFF
            }
        }
        addTable = ::addBacktable
    }

    fun drawTileWipe(height: Int) {
        addWipetable(0, gs.drawXh * 8, gs.drawBottomY.toInt(), height, 4 * 8, 0)
    }

    private fun drawPotionBubbles() {
        var potSize = 0
        var color = 12
        when ((gs.currModifier and 0xF8) shr 3) {
            0 -> return
            5, 6 -> color = 9
            3, 4 -> {
                color = 10
                potSize = 1
            }
            2 -> potSize = 1
        }
        val y = gs.drawMainY.toInt() - (potSize shl 2) - 14
        addBacktable(Chtabs.FLAMESWORDPOTION, 23, gs.drawXh + 3, 1, y, Blitters.MONO, 0)
        addForetable(Chtabs.FLAMESWORDPOTION, potionFramBubb[gs.currModifier and 0x7], gs.drawXh + 3, 1, y, color + Blitters.MONO, 0)
    }

    private fun drawDefaultFore() {
        var id = tileTable[gs.currTile].foreId
        if (id == 0) return
        if (gs.currTile == Tiles.POTION) {
            val potionType = (gs.currModifier and 0xF8) shr 3
            if (potionType < 5 && potionType >= 2) id = 13
        }
        val xh = tileTable[gs.currTile].foreX + gs.drawXh
        val ybottom = tileTable[gs.currTile].foreY + gs.drawMainY.toInt()
        if (gs.currTile == Tiles.POTION) {
            if (levelType() != 0) id += 2
            addForetable(Chtabs.FLAMESWORDPOTION, id, xh, 6, ybottom, Blitters.TRANSP, 0)
        } else {
            val blit = if ((gs.currTile == Tiles.PILLAR && levelType() == 0) ||
                (gs.currTile >= Tiles.LATTICE_SMALL && gs.currTile < Tiles.TORCH_WITH_DEBRIS)
            ) {
                Blitters.NO_TRANSP
            } else {
                Blitters.TRANSP
            }
            addForetable(Chtabs.ENVIRONMENT, id, xh, 0, ybottom, blit, 0)
        }
    }

    private fun levelType(): Int {
        return gs.custom.tblLevelType.getOrElse(gs.currentLevel) { 0 }
    }

    private fun torchBlitForLeftTile(): Int {
        if (gs.drawnCol.toInt() == 0) {
            val color = if (gs.roomL in gs.torchColors.indices) {
                gs.torchColors[gs.roomL][gs.drawnRow.toInt() * 10 + 9]
            } else {
                0
            }
            if (color != 0) return Blitters.MONO + (color and 0x3F)
        } else if (gs.drawnRoom in gs.torchColors.indices) {
            val color = gs.torchColors[gs.drawnRoom][gs.drawnRow.toInt() * 10 + gs.drawnCol.toInt() - 1]
            if (color != 0) return Blitters.MONO + (color and 0x3F)
        }
        return Blitters.NO_TRANSP
    }

    fun getSpikeFrame(modifier: Int): Int {
        val mod = modifier and 0xFF
        return if (mod and 0x80 != 0) 5 else mod
    }

    fun getLooseFrame(modifier: Int): Int {
        var mod = modifier and 0xFF
        if (mod and 0x80 != 0 || gs.custom.looseFloorDelay > 11) {
            mod = mod and 0x7F
            if (mod > 10) return 1
        }
        return mod
    }

    fun calcGatePos() {
        gs.gateTopY = gs.drawBottomY.toInt() - 62
        gs.gateOpenness = (minOf(gs.modifierLeft and 0xFF, 188) shr 2) + 1
        gs.gateBottomY = gs.drawMainY.toInt() - gs.gateOpenness
    }

    fun calcScreenXCoord(logicalX: Short): Short {
        return (logicalX.toInt() * 320 / 280).toShort()
    }

    fun addDrect(source: RectType) {
        for (index in 0 until gs.drectsCount.toInt()) {
            val expanded = shrink2Rect(source, deltaX = -1, deltaY = -1)
            if (intersectRect(expanded, gs.drects[index]) != null) {
                unionRectInto(gs.drects[index], gs.drects[index], source)
                return
            }
        }
        val count = gs.drectsCount.toInt()
        if (count >= 30) return
        copyRect(gs.drects[count], source)
        gs.drectsCount = (count + 1).toShort()
    }

    fun sortCurrObjs() {
        var swapped: Int
        val last = gs.nCurrObjs.toInt() - 1
        if (last <= 0) return
        do {
            swapped = 0
            for (index in 0 until last) {
                if (compareCurrObjs(index, index + 1) != 0) {
                    val temp = gs.currObjs[index]
                    gs.currObjs[index] = gs.currObjs[index + 1]
                    gs.currObjs[index + 1] = temp
                    swapped = 1
                }
            }
        } while (swapped != 0)
    }

    fun compareCurrObjs(index1: Int, index2: Int): Int {
        val objIndex1 = gs.currObjs[index1].toInt()
        if (gs.objtable[objIndex1].objType == 1) return 1
        val objIndex2 = gs.currObjs[index2].toInt()
        if (gs.objtable[objIndex2].objType == 1) return 0
        return if (gs.objtable[objIndex1].objType == 0x80 && gs.objtable[objIndex2].objType == 0x80) {
            if (gs.objtable[objIndex1].y < gs.objtable[objIndex2].y) 1 else 0
        } else {
            if (gs.objtable[objIndex1].y > gs.objtable[objIndex2].y) 1 else 0
        }
    }

    fun loadObjFromObjtable(index: Int): Int {
        val currObj = gs.objtable[index]
        gs.objXh = currObj.xh and 0xFF
        gs.objX = currObj.xh.toShort()
        gs.objXl = currObj.xl and 0xFF
        gs.objY = currObj.y.toInt()
        gs.objId = currObj.id and 0xFF
        gs.objChtab = currObj.chtabId and 0xFF
        gs.objDirection = currObj.direction.toByte().toInt()
        gs.objClipTop = currObj.clip.top
        gs.objClipBottom = currObj.clip.bottom
        gs.objClipLeft = currObj.clip.left
        gs.objClipRight = currObj.clip.right
        return currObj.objType and 0xFF
    }

    fun drawObjtableItemsAtTile(tilepos: Int) {
        val wantedTilepos = tilepos and 0xFF
        val objCount = gs.tableCounts[4].toInt()
        if (objCount == 0) return

        gs.nCurrObjs = 0
        for (objIndex in objCount - 1 downTo 0) {
            if ((gs.objtable[objIndex].tilepos and 0xFF) == wantedTilepos) {
                val currIndex = gs.nCurrObjs.toInt()
                gs.currObjs[currIndex] = objIndex.toShort()
                gs.nCurrObjs = (currIndex + 1).toShort()
            }
        }
        if (gs.nCurrObjs.toInt() == 0) return

        sortCurrObjs()
        for (currIndex in 0 until gs.nCurrObjs.toInt()) {
            drawObjtableItem(gs.currObjs[currIndex].toInt())
        }
    }

    fun drawObjtableItem(index: Int) {
        when (loadObjFromObjtable(index)) {
            0, 4 -> {
                if (gs.objId == 0xFF) return
                if (gs.unitedWithShadow.toInt() != 0 && gs.unitedWithShadow.toInt() % 2 == 0) {
                    drawShadowObjtableItem()
                } else {
                    addMidtable(gs.objChtab, gs.objId + 1, gs.objXh, gs.objXl, gs.objY, Blitters.TRANSP, 1)
                }
            }
            2, 3, 5 -> addMidtable(gs.objChtab, gs.objId + 1, gs.objXh, gs.objXl, gs.objY, Blitters.TRANSP, 1)
            1 -> drawShadowObjtableItem()
            0x80 -> {
                gs.objDirection = Directions.LEFT
                addMidtable(gs.objChtab, looseFramLeft[gs.objId], gs.objXh, gs.objXl, gs.objY - 3, Blitters.TRANSP, 1)
                addMidtable(gs.objChtab, looseFramBottom[gs.objId], gs.objXh, gs.objXl, gs.objY, Blitters.NO_TRANSP, 1)
                addMidtable(gs.objChtab, looseFramRight[gs.objId], gs.objX.toInt() + 4, gs.objXl, gs.objY - 1, Blitters.TRANSP, 1)
            }
        }
    }

    private fun drawShadowObjtableItem() {
        if (gs.unitedWithShadow.toInt() == 2) {
            ext.playSound(SoundIds.END_LEVEL_MUSIC)
        }
        addMidtable(gs.objChtab, gs.objId + 1, gs.objXh, gs.objXl, gs.objY, Blitters.OR, 1)
        addMidtable(gs.objChtab, gs.objId + 1, gs.objXh, gs.objXl + 1, gs.objY, Blitters.XOR, 1)
    }

    fun drawPeople() {
        Seg003.checkMirror()
        drawKid()
        drawGuard()
        Seg006.resetObjClip()
        drawHp()
    }

    fun drawKid() {
        if (gs.Kid.room != 0 && gs.Kid.room == gs.drawnRoom) {
            addKidToObjtable()
            if (gs.hitpDelta < 0) Seg006.drawHurtSplash()
            Seg006.addSwordToObjtable()
        }
    }

    fun drawGuard() {
        if (gs.Guard.direction != Directions.NONE && gs.Guard.room == gs.drawnRoom) {
            addGuardToObjtable()
            if (gs.guardhpDelta < 0) Seg006.drawHurtSplash()
            Seg006.addSwordToObjtable()
        }
    }

    private fun drawHp() {
        if (gs.hitpDelta.toInt() != 0) {
            ext.drawKidHp(gs.hitpCurr, gs.hitpMax)
        }
        val blinkState = gs.remTick and 1
        if (gs.hitpCurr == 1 && gs.currentLevel != 15) {
            if (blinkState != 0) ext.drawKidHp(1, 0) else ext.drawKidHp(0, 1)
        }
        if (gs.guardhpDelta.toInt() != 0) {
            ext.drawGuardHp(gs.guardhpCurr, gs.guardhpMax)
        }
        if (gs.guardhpCurr == 1) {
            if (blinkState != 0) ext.drawGuardHp(1, 0) else ext.drawGuardHp(0, 1)
        }
    }

    fun addKidToObjtable() {
        Seg006.loadkid()
        Seg006.loadFramDetCol()
        loadFrameToObj()
        Seg006.stuckLower()
        Seg006.setCharCollision()
        Seg006.setObjtileAtChar()
        Seg003.redrawAtChar()
        Seg003.redrawAtChar2()
        Seg006.clipChar()
        addObjtable(0)
    }

    fun addGuardToObjtable() {
        Seg006.loadshad()
        Seg006.loadFramDetCol()
        loadFrameToObj()
        Seg006.stuckLower()
        Seg006.setCharCollision()
        Seg006.setObjtileAtChar()
        Seg003.redrawAtChar()
        Seg003.redrawAtChar2()
        Seg006.clipChar()
        val objType = if (gs.Char.charid == CharIds.SHADOW) {
            if (gs.currentLevel == gs.custom.mirrorLevel && gs.Char.room == gs.custom.mirrorRoom) {
                gs.objClipLeft = (137 + (gs.custom.mirrorColumn - 4) * 32).toShort()
            }
            1
        } else {
            2
        }
        addObjtable(objType)
    }

    fun addObjtable(objType: Int) {
        val index = gs.tableCounts[4].toInt()
        gs.tableCounts[4] = (index + 1).toShort()
        if (index >= 50) return
        val entry = gs.objtable[index]
        entry.objType = objType and 0xFF
        val (xh, xl) = Seg006.xToXhAndXl(gs.objX.toInt())
        entry.xh = xh.toByte().toInt()
        entry.xl = xl.toByte().toInt()
        entry.y = gs.objY.toShort()
        entry.clip.top = gs.objClipTop
        entry.clip.bottom = gs.objClipBottom
        entry.clip.left = gs.objClipLeft
        entry.clip.right = gs.objClipRight
        entry.chtabId = gs.objChtab and 0xFF
        entry.id = gs.objId and 0xFF
        entry.direction = gs.objDirection.toByte().toInt()
        markObjTileRedraw(index)
    }

    fun markObjTileRedraw(index: Int) {
        gs.objtable[index].tilepos = gs.objTilepos and 0xFF
        if (gs.objTilepos in 0 until 30) {
            gs.tileObjectRedraw[gs.objTilepos] = 1
        }
    }

    fun loadFrameToObj() {
        val chtabBase = Chtabs.KID
        Seg006.resetObjClip()
        Seg006.loadFrame()
        gs.objDirection = gs.Char.direction
        gs.objId = gs.curFrame.image and 0xFF
        gs.objChtab = chtabBase + ((gs.curFrame.sword and 0xFF) shr 6)
        gs.objX = ((Seg006.charDxForward(gs.curFrame.dx) shl 1) - 116).toShort()
        gs.objY = gs.curFrame.dy + gs.Char.y
        if (((gs.curFrame.flags xor gs.objDirection).toByte().toInt()) >= 0) {
            gs.objX = (gs.objX + 1).toShort()
        }
    }

    fun loadRoomLinks() {
        gs.roomBR = 0
        gs.roomBL = 0
        gs.roomAR = 0
        gs.roomAL = 0
        if (gs.drawnRoom != 0) {
            ext.getRoomAddress(gs.drawnRoom)
            val links = gs.level.roomlinks[gs.drawnRoom - 1]
            gs.roomL = links.left
            gs.roomR = links.right
            gs.roomA = links.up
            gs.roomB = links.down
            if (gs.roomA != 0) {
                gs.roomAL = gs.level.roomlinks[gs.roomA - 1].left
                gs.roomAR = gs.level.roomlinks[gs.roomA - 1].right
            } else {
                if (gs.roomL != 0) gs.roomAL = gs.level.roomlinks[gs.roomL - 1].up
                if (gs.roomR != 0) gs.roomAR = gs.level.roomlinks[gs.roomR - 1].up
            }
            if (gs.roomB != 0) {
                gs.roomBL = gs.level.roomlinks[gs.roomB - 1].left
                gs.roomBR = gs.level.roomlinks[gs.roomB - 1].right
            } else {
                if (gs.roomL != 0) gs.roomBL = gs.level.roomlinks[gs.roomL - 1].down
                if (gs.roomR != 0) gs.roomBR = gs.level.roomlinks[gs.roomR - 1].down
            }
        } else {
            gs.roomB = 0
            gs.roomA = 0
            gs.roomR = 0
            gs.roomL = 0
        }
    }

    fun loadCurrAndLeftTile() {
        val edgeTile = if (gs.drawnRow.toInt() == 2) gs.custom.drawnTileTopLevelEdge else Tiles.WALL
        val current = TileAndMod()
        getTileToDraw(gs.drawnRoom, gs.drawnCol.toInt(), gs.drawnRow.toInt(), current, edgeTile)
        gs.currTile = current.tiletype and 0xFF
        gs.currModifier = current.modifier and 0xFF
        val left = TileAndMod()
        getTileToDraw(gs.drawnRoom, gs.drawnCol.toInt() - 1, gs.drawnRow.toInt(), left, edgeTile)
        gs.tileLeft = left.tiletype and 0xFF
        gs.modifierLeft = left.modifier and 0xFF
        gs.drawXh = colXh[gs.drawnCol.toInt()]
    }

    fun loadLeftroom() {
        ext.getRoomAddress(gs.roomL)
        for (row in 0 until 3) {
            getTileToDraw(gs.roomL, 9, row, gs.leftroom[row], gs.custom.drawnTileLeftLevelEdge)
        }
    }

    fun loadRowbelow() {
        val rowBelow: Int
        val room: Int
        val roomLeft: Int
        if (gs.drawnRow.toInt() == 2) {
            room = gs.roomB
            roomLeft = gs.roomBL
            rowBelow = 0
        } else {
            room = gs.drawnRoom
            roomLeft = gs.roomL
            rowBelow = gs.drawnRow.toInt() + 1
        }

        ext.getRoomAddress(room)
        for (column in 1 until 10) {
            getTileToDraw(room, column - 1, rowBelow, gs.rowBelowLeft[column], Tiles.EMPTY)
        }
        ext.getRoomAddress(roomLeft)
        getTileToDraw(roomLeft, 9, rowBelow, gs.rowBelowLeft[0], Tiles.WALL)
        ext.getRoomAddress(gs.drawnRoom)
    }

    fun getTileToDraw(room: Int, column: Int, row: Int, tileRoom0: Int): TileAndMod {
        val result = TileAndMod()
        getTileToDraw(room, column, row, result, tileRoom0)
        return result
    }

    fun getTileToDraw(room: Int, column: Int, row: Int, out: TileAndMod, tileRoom0: Int): Int {
        val tilepos = gs.tblLine[row] + column
        if (column == -1) {
            out.tiletype = gs.leftroom[row].tiletype and 0xFF
            out.modifier = gs.leftroom[row].modifier and 0xFF
        } else if (room != 0) {
            out.tiletype = gs.currRoomTiles[tilepos] and 0x1F
            out.modifier = gs.currRoomModif[tilepos] and 0xFF
        } else {
            out.modifier = 0
            out.tiletype = tileRoom0 and 0xFF
        }

        applyButtonAndFakeTileRules(out)

        if (gs.fixes.fixLooseLeftOfPotion != 0 &&
            out.tiletype == Tiles.LOOSE &&
            out.modifier and 0x7F == 0
        ) {
            out.tiletype = Tiles.FLOOR
        }

        out.tiletype = out.tiletype and 0xFF
        out.modifier = out.modifier and 0xFF
        return out.tiletype
    }

    private fun applyButtonAndFakeTileRules(out: TileAndMod) {
        val tiletype = out.tiletype and 0x1F
        val modifier = out.modifier and 0xFF
        when (tiletype) {
            Tiles.CLOSER -> {
                if (Seg007.getDoorlinkTimer(modifier) > 1) {
                    out.tiletype = Tiles.STUCK
                }
            }
            Tiles.OPENER -> {
                if (Seg007.getDoorlinkTimer(modifier) > 1) {
                    out.modifier = 0
                    out.tiletype = Tiles.FLOOR
                }
            }
            Tiles.EMPTY -> applyFakeEmpty(out, modifier)
            Tiles.FLOOR -> applyFakeFloor(out, modifier)
            Tiles.WALL -> applyFakeWall(out, modifier)
        }
    }

    private fun applyFakeEmpty(out: TileAndMod, modifier: Int) {
        when (modifier) {
            4, 12 -> {
                out.tiletype = Tiles.FLOOR
                out.modifier = if (modifier == 12) 1 else 0
            }
            5, 13 -> {
                out.tiletype = Tiles.WALL
                out.modifier = if (modifier == 13) 0x80 else 0
            }
            50, 51, 52, 53 -> {
                out.tiletype = Tiles.WALL
                out.modifier = modifier - 50
            }
        }
    }

    private fun applyFakeFloor(out: TileAndMod, modifier: Int) {
        when (modifier) {
            6, 14 -> {
                out.tiletype = Tiles.EMPTY
                out.modifier = if (modifier == 14) 1 else 0
            }
            5, 13 -> {
                out.tiletype = Tiles.WALL
                out.modifier = if (modifier == 13) 0x80 else 0
            }
            50, 51, 52, 53 -> {
                out.tiletype = Tiles.WALL
                out.modifier = modifier - 50
            }
        }
    }

    private fun applyFakeWall(out: TileAndMod, modifier: Int) {
        when ((modifier shr 4) and 7) {
            4 -> {
                out.tiletype = Tiles.FLOOR
                out.modifier = modifier shr 7
            }
            6 -> {
                out.tiletype = Tiles.EMPTY
                out.modifier = if (modifier shr 7 != 0) 1 else 0
            }
        }
    }

    fun alterModsAllrm() {
        for (room in 1..24) {
            gs.torchColors[room].fill(0)
        }

        if (gs.level.usedRooms > 24) gs.level.usedRooms = 24

        for (room in 1..gs.level.usedRooms) {
            ext.getRoomAddress(room)
            gs.roomL = gs.level.roomlinks[room - 1].left
            gs.roomR = gs.level.roomlinks[room - 1].right
            for (tilepos in 0 until 30) {
                loadAlterMod(tilepos)
            }
        }
    }

    fun loadAlterMod(tilepos: Int) {
        val tiletype = gs.currRoomTiles[tilepos] and 0x1F
        when (tiletype) {
            Tiles.GATE -> {
                setCurrRoomModifier(tilepos, if ((gs.currRoomModif[tilepos] and 0xFF) == 1) 188 else 0)
            }
            Tiles.LOOSE -> {
                setCurrRoomModifier(tilepos, 0)
            }
            Tiles.POTION -> {
                setCurrRoomModifier(tilepos, (gs.currRoomModif[tilepos] shl 3) and 0xFF)
            }
            Tiles.WALL -> {
                val stored = gs.currRoomModif[tilepos] and 0xFF
                setCurrRoomModifier(tilepos, if (stored == 1) 0x80 else (stored shl 4) and 0xFF)
                applyWallConnectionModifier(tilepos, tiletype)
            }
            Tiles.EMPTY, Tiles.FLOOR -> {
                if ((gs.currRoomModif[tilepos] and 7) == 5) {
                    applyWallConnectionModifier(tilepos, tiletype)
                }
            }
            Tiles.TORCH, Tiles.TORCH_WITH_DEBRIS -> {
                val room = gs.loadedRoom
                if (room in 1..24) {
                    gs.torchColors[room][tilepos] = gs.currRoomModif[tilepos] and 0xFF
                }
                setCurrRoomModifier(tilepos, 0)
            }
        }
    }

    private fun applyWallConnectionModifier(tilepos: Int, tiletype: Int) {
        if (gs.graphicsMode == 1 || gs.graphicsMode == 2) {
            setCurrRoomModifier(tilepos, 3)
            return
        }

        val wallToLeft = if (tilepos % 10 == 0) {
            if (gs.roomL != 0) {
                val index = 30 * (gs.roomL - 1) + tilepos + 9
                isWallConnection(gs.level.fg[index] and 0x1F, gs.level.bg[index] and 0xFF)
            } else {
                true
            }
        } else {
            isWallConnection(gs.currRoomTiles[tilepos - 1] and 0x1F, gs.currRoomModif[tilepos - 1] and 0xFF)
        }

        val wallToRight = if (tilepos % 10 == 9) {
            if (gs.roomR != 0) {
                val index = 30 * (gs.roomR - 1) + tilepos - 9
                isWallConnection(gs.level.fg[index] and 0x1F, gs.level.bg[index] and 0xFF)
            } else {
                true
            }
        } else {
            isWallConnection(gs.currRoomTiles[tilepos + 1] and 0x1F, gs.currRoomModif[tilepos + 1] and 0xFF)
        }

        if (tiletype == Tiles.FLOOR || tiletype == Tiles.EMPTY) {
            when {
                wallToLeft && wallToRight -> setCurrRoomModifier(tilepos, 53)
                wallToLeft -> setCurrRoomModifier(tilepos, 52)
                wallToRight -> setCurrRoomModifier(tilepos, 51)
            }
            return
        }

        val mod = gs.currRoomModif[tilepos] and 0xFF
        when {
            wallToLeft && wallToRight -> setCurrRoomModifier(tilepos, mod or 3)
            wallToLeft -> setCurrRoomModifier(tilepos, mod or 2)
            wallToRight -> setCurrRoomModifier(tilepos, mod or 1)
        }
    }

    private fun isWallConnection(tile: Int, modifier: Int): Boolean {
        return (tile == Tiles.WALL &&
            modifier != 4 && (modifier shr 4) != 4 &&
            modifier != 6 && (modifier shr 4) != 6) ||
            (tile == Tiles.EMPTY && (modifier == 5 || modifier == 13 || modifier in 50..53)) ||
            (tile == Tiles.FLOOR && (modifier == 5 || modifier == 13 || modifier in 50..53))
    }

    private fun setCurrRoomModifier(tilepos: Int, value: Int) {
        val mod = value and 0xFF
        gs.currRoomModif[tilepos] = mod
        if (gs.loadedRoom != 0) {
            gs.level.bg[(gs.loadedRoom - 1) * 30 + tilepos] = mod
        }
    }

    private fun shrink2Rect(source: RectType, deltaX: Int, deltaY: Int): RectType {
        return RectType(
            top = (source.top + deltaY).toShort(),
            left = (source.left + deltaX).toShort(),
            bottom = (source.bottom - deltaY).toShort(),
            right = (source.right - deltaX).toShort(),
        )
    }

    private fun intersectRect(input1: RectType, input2: RectType): RectType? {
        val left = maxOf(input1.left.toInt(), input2.left.toInt())
        val right = minOf(input1.right.toInt(), input2.right.toInt())
        if (left < right) {
            val top = maxOf(input1.top.toInt(), input2.top.toInt())
            val bottom = minOf(input1.bottom.toInt(), input2.bottom.toInt())
            if (top < bottom) {
                return RectType(top.toShort(), left.toShort(), bottom.toShort(), right.toShort())
            }
        }
        return null
    }

    private fun unionRectInto(output: RectType, input1: RectType, input2: RectType) {
        output.top = minOf(input1.top, input2.top)
        output.left = minOf(input1.left, input2.left)
        output.bottom = maxOf(input1.bottom, input2.bottom)
        output.right = maxOf(input1.right, input2.right)
    }

    private fun copyRect(output: RectType, input: RectType) {
        output.top = input.top
        output.left = input.left
        output.bottom = input.bottom
        output.right = input.right
    }
}
