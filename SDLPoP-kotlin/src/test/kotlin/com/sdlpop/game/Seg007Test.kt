/*
SDLPoP-kotlin — Module 12 Phase 12a.1: Seg007 scaffold tests.
Tests drawn-room trob coordinate mapping and redraw/wipe bookkeeping.
*/

package com.sdlpop.game

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class Seg007Test {
    private val gs = GameState
    private val seg007 = Seg007

    @BeforeTest
    fun resetState() {
        gs.level = LevelType()
        gs.drawnRoom = 1
        gs.loadedRoom = 0
        gs.roomL = 2
        gs.roomR = 3
        gs.roomA = 4
        gs.roomB = 5
        gs.roomBL = 6
        gs.roomAL = 7
        gs.currTile = 0
        gs.currModifier = 0
        gs.redrawHeight = 0
        gs.trobsCount = 0
        gs.trob.tilepos = 0
        gs.trob.room = 0
        gs.trob.type = 0
        gs.currRoomTiles.fill(0)
        gs.currRoomModif.fill(0)
        gs.redrawFramesFull.fill(0)
        gs.wipeFrames.fill(0)
        gs.wipeHeights.fill(0)
        gs.redrawFramesAnim.fill(0)
        gs.redrawFramesFore.fill(0)
        gs.redrawFrames2.fill(0)
        gs.redrawFramesFloorOverlay.fill(0)
        gs.tileObjectRedraw.fill(0)
        gs.redrawFramesAbove.fill(0)
        gs.trobs.forEach {
            it.tilepos = 0
            it.room = 0
            it.type = 0
        }
        ExternalStubs.getRoomAddress = { room ->
            gs.loadedRoom = room
            if (room != 0) {
                val base = (room - 1) * 30
                for (i in 0 until 30) {
                    gs.currRoomTiles[i] = gs.level.fg[base + i]
                    gs.currRoomModif[i] = gs.level.bg[base + i]
                }
            }
        }
    }

    @Test
    fun getTrobPosInDrawnRoom_currentRoomReturnsTilepos() {
        gs.trob.room = gs.drawnRoom
        gs.trob.tilepos = 14

        assertEquals(14, seg007.getTrobPosInDrawnRoom())
    }

    @Test
    fun getTrobPosInDrawnRoom_aboveBottomRowMapsNegativeTilepos() {
        gs.trob.room = gs.roomA
        gs.trob.tilepos = 23

        assertEquals(-4, seg007.getTrobPosInDrawnRoom())
    }

    @Test
    fun getTrobRightPosInDrawnRoom_handlesCurrentLeftAndAboveLeftRooms() {
        gs.trob.room = gs.drawnRoom
        gs.trob.tilepos = 8
        assertEquals(9, seg007.getTrobRightPosInDrawnRoom())

        gs.trob.tilepos = 9
        assertEquals(30, seg007.getTrobRightPosInDrawnRoom())

        gs.trob.room = gs.roomL
        gs.trob.tilepos = 19
        assertEquals(10, seg007.getTrobRightPosInDrawnRoom())

        gs.trob.room = gs.roomAL
        gs.trob.tilepos = 29
        assertEquals(-1, seg007.getTrobRightPosInDrawnRoom())
    }

    @Test
    fun getTrobRightAbovePosInDrawnRoom_mapsAboveSlots() {
        gs.trob.room = gs.drawnRoom
        gs.trob.tilepos = 2
        assertEquals(-4, seg007.getTrobRightAbovePosInDrawnRoom())

        gs.trob.tilepos = 12
        assertEquals(3, seg007.getTrobRightAbovePosInDrawnRoom())

        gs.trob.room = gs.roomB
        gs.trob.tilepos = 4
        assertEquals(25, seg007.getTrobRightAbovePosInDrawnRoom())

        gs.trob.room = gs.roomBL
        gs.trob.tilepos = 9
        assertEquals(20, seg007.getTrobRightAbovePosInDrawnRoom())
    }

    @Test
    fun redrawHelpersWriteVisibleAndAboveArrays() {
        seg007.setRedrawAnim(5, 1)
        seg007.setRedraw2(6, 2)
        seg007.setRedrawFloorOverlay(7, 3)
        seg007.setRedrawFull(8, 4)
        seg007.setRedrawFore(9, 5)

        assertEquals(1, gs.redrawFramesAnim[5])
        assertEquals(2, gs.redrawFrames2[6])
        assertEquals(3, gs.redrawFramesFloorOverlay[7])
        assertEquals(4, gs.redrawFramesFull[8])
        assertEquals(5, gs.redrawFramesFore[9])

        seg007.setRedrawAnim(-1, 6)
        assertEquals(6, gs.redrawFramesAbove[0])
        seg007.setRedraw2(-12, 7)
        assertEquals(7, gs.redrawFramesAbove[9])
        seg007.setRedrawFull(30, 8)
        assertEquals(0, gs.redrawFramesFull[29])
    }

    @Test
    fun setWipePreservesMaximumRedrawHeightWhenAlreadyMarked() {
        gs.redrawHeight = 0x11
        seg007.setWipe(3, 1)

        assertEquals(1, gs.wipeFrames[3])
        assertEquals(0x11, gs.wipeHeights[3])

        gs.redrawHeight = 0x05
        seg007.setWipe(3, 2)

        assertEquals(2, gs.wipeFrames[3])
        assertEquals(0x11, gs.wipeHeights[3])
        assertEquals(0x11, gs.redrawHeight.toInt())
    }

    @Test
    fun clearTileWipesClearsAllRedrawArrays() {
        gs.redrawFramesFull[1] = 1
        gs.wipeFrames[2] = 1
        gs.wipeHeights[3] = 1
        gs.redrawFramesAnim[4] = 1
        gs.redrawFramesFore[5] = 1
        gs.redrawFrames2[6] = 1
        gs.redrawFramesFloorOverlay[7] = 1
        gs.tileObjectRedraw[8] = 1
        gs.redrawFramesAbove[9] = 1

        seg007.clearTileWipes()

        assertContentEquals(IntArray(30), gs.redrawFramesFull)
        assertContentEquals(IntArray(30), gs.wipeFrames)
        assertContentEquals(IntArray(30), gs.wipeHeights)
        assertContentEquals(IntArray(30), gs.redrawFramesAnim)
        assertContentEquals(IntArray(30), gs.redrawFramesFore)
        assertContentEquals(IntArray(30), gs.redrawFrames2)
        assertContentEquals(IntArray(30), gs.redrawFramesFloorOverlay)
        assertContentEquals(IntArray(30), gs.tileObjectRedraw)
        assertContentEquals(IntArray(10), gs.redrawFramesAbove)
    }

    @Test
    fun getCurrTileMasksTileTypeAndLoadsModifier() {
        gs.currRoomTiles[12] = Tiles.TORCH or 0xE0
        gs.currRoomModif[12] = 7

        assertEquals(Tiles.TORCH, seg007.getCurrTile(12))
        assertEquals(Tiles.TORCH, gs.currTile)
        assertEquals(7, gs.currModifier)
    }

    @Test
    fun processTrobsDeletesUnknownTileEntries() {
        gs.trobs[0].room = 1
        gs.trobs[0].tilepos = 4
        gs.trobs[0].type = 1
        gs.trobs[1].room = 1
        gs.trobs[1].tilepos = 5
        gs.trobs[1].type = 1
        gs.trobsCount = 2
        gs.level.fg[4] = 31
        gs.level.bg[4] = 10
        gs.level.fg[5] = Tiles.PILLAR
        gs.level.bg[5] = 20

        seg007.processTrobs()

        assertEquals(0, gs.trobsCount.toInt())
    }

    @Test
    fun bubbleNextFrameWrapsAfterSeven() {
        assertEquals(4, seg007.bubbleNextFrame(3))
        assertEquals(1, seg007.bubbleNextFrame(7))
    }
}
