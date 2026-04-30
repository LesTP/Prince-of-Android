/*
SDLPoP-kotlin, a Kotlin port of SDLPoP (Prince of Persia).
Based on SDLPoP by David Nagy, licensed under GPL v3+.

Module 16, Phase 16c: Pure render-state helpers from seg008.c.
*/

package com.sdlpop.game

object Seg008 {
    private val gs: GameState
        get() = GameState

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
}
