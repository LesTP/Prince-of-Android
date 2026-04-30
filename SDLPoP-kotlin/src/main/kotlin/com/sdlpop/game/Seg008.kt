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

    var drawTileFloorrightHook: () -> Unit = {}
    var drawTileAnimToprightHook: () -> Unit = {}
    var drawTileRightHook: () -> Unit = {}
    var drawTileAnimRightHook: () -> Unit = {}
    var drawTileBottomHook: (Int) -> Unit = {}
    var drawLooseHook: (Int) -> Unit = {}
    var drawTileBaseHook: () -> Unit = {}
    var drawTileAnimHook: () -> Unit = {}
    var drawTileForeHook: () -> Unit = {}
    var drawTileWipeHook: (Int) -> Unit = {}
    var drawOtherOverlayHook: () -> Unit = {}
    var drawFloorOverlayHook: () -> Unit = {}
    var drawObjtableItemsAtTileHook: (Int) -> Unit = {}
    var drawPeopleHook: () -> Unit = {}

    val colXh = intArrayOf(0, 4, 8, 12, 16, 20, 24, 28, 32, 36)

    fun resetRenderHooks() {
        drawTileFloorrightHook = {}
        drawTileAnimToprightHook = {}
        drawTileRightHook = {}
        drawTileAnimRightHook = {}
        drawTileBottomHook = {}
        drawLooseHook = {}
        drawTileBaseHook = {}
        drawTileAnimHook = {}
        drawTileForeHook = {}
        drawTileWipeHook = {}
        drawOtherOverlayHook = {}
        drawFloorOverlayHook = {}
        drawObjtableItemsAtTileHook = {}
        drawPeopleHook = {}
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
