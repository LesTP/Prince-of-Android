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

    val colXh = intArrayOf(0, 4, 8, 12, 16, 20, 24, 28, 32, 36)

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
}
