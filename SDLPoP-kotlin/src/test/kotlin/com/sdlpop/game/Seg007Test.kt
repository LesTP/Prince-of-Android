/*
SDLPoP-kotlin — Module 12 Phases 12a.1–12a.2: Seg007 tests.
Tests drawn-room trob coordinate mapping, redraw/wipe bookkeeping,
animated-tile state machines, trob lifecycle, and trigger plumbing.
*/

package com.sdlpop.game

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Seg007Test {
    private val gs = GameState
    private val seg007 = Seg007

    private var lastPlayedSound = -1

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
        gs.currTilepos = 0
        gs.currRoom = 0
        gs.currentLevel = -1
        gs.redrawHeight = 0
        gs.trobsCount = 0
        gs.trob.tilepos = 0
        gs.trob.room = 0
        gs.trob.type = 0
        gs.isGuardNotice = 0
        gs.mobsCount = 0
        gs.Kid = CharType()
        gs.Char = CharType()
        gs.randomSeed = 12345
        gs.custom = CustomOptionsType()
        gs.fixes = FixesOptionsType()
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
        gs.mobs.forEach {
            it.xh = 0; it.y = 0; it.room = 0; it.speed = 0; it.type = 0; it.row = 0
        }
        lastPlayedSound = -1
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
        ExternalStubs.playSound = { id -> lastPlayedSound = id }
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

    // === Phase 12a.2 tests ===

    @Test
    fun addTrobCreatesNewEntry() {
        seg007.addTrob(1, 5, 1)
        assertEquals(1, gs.trobsCount.toInt())
        assertEquals(1, gs.trobs[0].room)
        assertEquals(5, gs.trobs[0].tilepos)
        assertEquals(1, gs.trobs[0].type)
    }

    @Test
    fun addTrobUpdatesExistingEntryInsteadOfDuplicating() {
        seg007.addTrob(1, 5, 1)
        assertEquals(1, gs.trobsCount.toInt())
        // Same room+tilepos, different type
        seg007.addTrob(1, 5, 2)
        assertEquals(1, gs.trobsCount.toInt())
        assertEquals(2, gs.trobs[0].type)
    }

    @Test
    fun addTrobAddsSecondEntryForDifferentPosition() {
        seg007.addTrob(1, 5, 1)
        seg007.addTrob(1, 6, 1)
        assertEquals(2, gs.trobsCount.toInt())
    }

    @Test
    fun findTrobReturnsMinusOneWhenNotFound() {
        gs.trob.room = 99
        gs.trob.tilepos = 99
        assertEquals(-1, seg007.findTrob())
    }

    @Test
    fun animateSpikeProgressesFrames() {
        gs.trob.room = gs.drawnRoom
        gs.trob.tilepos = 5
        gs.trob.type = 1
        gs.currModifier = 0 // start

        // Advance through frames 0->1->2->3->4->0x8F (retract timer)
        seg007.animateSpike()
        assertEquals(1, gs.currModifier)
        seg007.animateSpike()
        assertEquals(2, gs.currModifier)
        seg007.animateSpike()
        assertEquals(3, gs.currModifier)
        seg007.animateSpike()
        assertEquals(4, gs.currModifier)
        seg007.animateSpike()
        assertEquals(0x8F, gs.currModifier) // starts retract at frame 5
    }

    @Test
    fun animateSpikeDisabledSpikeDoesNothing() {
        gs.trob.room = gs.drawnRoom
        gs.trob.tilepos = 5
        gs.trob.type = 1
        gs.currModifier = 0xFF

        seg007.animateSpike()
        assertEquals(0xFF, gs.currModifier)
        assertTrue(gs.trob.type >= 0) // not deleted
    }

    @Test
    fun animateSpikeRetractCountdown() {
        gs.trob.room = gs.drawnRoom
        gs.trob.tilepos = 5
        gs.trob.type = 1
        gs.currModifier = 0x82 // retract timer at 2

        seg007.animateSpike() // 0x82 -> 0x81
        assertEquals(0x81, gs.currModifier)
        seg007.animateSpike() // 0x81 -> 0x80, then (0x80 & 0x7F)==0 → set to 6
        assertEquals(6, gs.currModifier)
    }

    @Test
    fun animateChomperAdvancesFrameAndPlaysSound() {
        gs.trob.room = gs.drawnRoom
        gs.trob.tilepos = 5
        gs.trob.type = 1
        gs.Kid.currRow = 0 // same row
        gs.Kid.alive = 0 // alive
        gs.currModifier = 0 // frame 0

        seg007.animateChomper()
        assertEquals(1, gs.currModifier and 0x7F)

        seg007.animateChomper()
        assertEquals(2, gs.currModifier and 0x7F)
        assertEquals(SoundIds.CHOMPER, lastPlayedSound)
    }

    @Test
    fun animateChomperWrapsAroundChomperSpeed() {
        gs.trob.room = gs.drawnRoom
        gs.trob.tilepos = 5
        gs.trob.type = 1
        gs.Kid.currRow = 0
        gs.Kid.alive = 0
        gs.currModifier = 15 // at chomper_speed (default 15)

        seg007.animateChomper()
        assertEquals(1, gs.currModifier and 0x7F) // wraps to 1
    }

    @Test
    fun animateButtonDecrementsTimerAndRemoves() {
        gs.trob.room = gs.drawnRoom
        gs.trob.tilepos = 5
        gs.trob.type = 1
        // Set up doorlink with timer = 3
        gs.currModifier = 3 // index into doorlinks
        gs.level.doorlinks2[3] = 0x03 // timer = 3

        seg007.animateButton()
        assertEquals(2, gs.level.doorlinks2[3] and 0x1F) // 3→2
        assertTrue(gs.trob.type >= 0) // still active (timer >= 2)

        seg007.animateButton()
        assertEquals(1, gs.level.doorlinks2[3] and 0x1F) // 2→1
        assertEquals(-1, gs.trob.type) // removed (timer < 2)
    }

    @Test
    fun animateEmptyRemovesTrob() {
        gs.trob.room = gs.drawnRoom
        gs.trob.tilepos = 5
        gs.trob.type = 1

        seg007.animateEmpty()
        assertEquals(-1, gs.trob.type)
    }

    @Test
    fun startAnimChomperIgnoresActiveChomper() {
        gs.currRoomModif[5] = 3 // frame 3 — active (between 1 and 5)
        seg007.startAnimChomper(1, 5, 1)
        assertEquals(3, gs.currRoomModif[5]) // unchanged
        assertEquals(0, gs.trobsCount.toInt())
    }

    @Test
    fun startAnimChomperStartsIdleOrPastFrame6() {
        gs.currRoomModif[5] = 0 // idle
        seg007.startAnimChomper(1, 5, 8)
        assertEquals(8, gs.currRoomModif[5])
        assertEquals(1, gs.trobsCount.toInt())
    }

    @Test
    fun startAnimSpikeStartsFromZero() {
        gs.currRoomModif[5] = 0
        seg007.startAnimSpike(1, 5)
        assertEquals(1, gs.trobsCount.toInt())
        assertEquals(SoundIds.SPIKES, lastPlayedSound)
    }

    @Test
    fun startAnimSpikeIgnoresAlreadyActive() {
        gs.currRoomModif[5] = 3 // active positive value
        seg007.startAnimSpike(1, 5)
        assertEquals(0, gs.trobsCount.toInt()) // no new trob
    }

    @Test
    fun isSpikeHarmfulReturnsCorrectValues() {
        gs.currTilepos = 5
        gs.currRoomModif[5] = 0
        assertEquals(0, seg007.isSpikeHarmful()) // no danger at 0

        gs.currRoomModif[5] = 0xFF // -1 signed
        assertEquals(0, seg007.isSpikeHarmful()) // disabled

        gs.currRoomModif[5] = 0x80 // negative, not -1
        assertEquals(1, seg007.isSpikeHarmful()) // harmful (retract phase)

        gs.currRoomModif[5] = 3
        assertEquals(2, seg007.isSpikeHarmful()) // fully extended (1-4)

        gs.currRoomModif[5] = 6
        assertEquals(0, seg007.isSpikeHarmful()) // past danger zone
    }

    @Test
    fun doorlinkAccessors() {
        gs.level.doorlinks1[5] = 0xA3 // bits: 1_01_00011 → next=0, room_hi=01, tile=00011=3
        gs.level.doorlinks2[5] = 0x6A // bits: 011_01010 → room_lo=011, timer=01010=10

        assertEquals(3, seg007.getDoorlinkTile(5))
        assertEquals(10, seg007.getDoorlinkTimer(5))
        assertEquals(0, seg007.getDoorlinkNext(5)) // bit 7 is set → returns 0 (no next)
        // room = (0x60 >> 5) + (0x60 >> 3) = (0x20 >> 5) + (0x60 >> 3) = 1 + 12 = 13
        // Actually: doorlinks1[5] & 0x60 = 0xA3 & 0x60 = 0x20 → >> 5 = 1
        // doorlinks2[5] & 0xE0 = 0x6A & 0xE0 = 0x60 → >> 3 = 12
        assertEquals(13, seg007.getDoorlinkRoom(5))
    }

    @Test
    fun setDoorlinkTimerPreservesHighBits() {
        gs.level.doorlinks2[3] = 0xE5 // 111_00101
        seg007.setDoorlinkTimer(3, 0x0A)
        assertEquals(0xEA, gs.level.doorlinks2[3]) // 111_01010
    }

    @Test
    fun triggerGateOpenerOpensClosedGate() {
        gs.currRoomModif[5] = 0 // fully closed
        val result = seg007.triggerGate(1, 5, Tiles.OPENER)
        assertEquals(1, result) // regular open
        assertEquals(0, gs.currRoomModif[5] and 0x03) // aligned to 4
    }

    @Test
    fun triggerGatePermanentlyOpenGateIgnoresOpener() {
        gs.currRoomModif[5] = 0xFF
        val result = seg007.triggerGate(1, 5, Tiles.OPENER)
        assertEquals(-1, result)
    }

    @Test
    fun triggerGateDebrisForcesPermanentOpen() {
        gs.currRoomModif[5] = 100 // partially open, < 188
        val result = seg007.triggerGate(1, 5, Tiles.DEBRIS)
        assertEquals(2, result) // permanent open
    }

    @Test
    fun triggerGateCloserClosesOpenGate() {
        gs.currRoomModif[5] = 200 // open
        val result = seg007.triggerGate(1, 5, Tiles.CLOSER)
        assertEquals(3, result) // close fast
    }

    @Test
    fun makeLooseFallStartsLooseAnimation() {
        gs.currTilepos = 5
        gs.currRoom = 1
        gs.currRoomTiles[5] = Tiles.LOOSE // not solid (bit 5 clear)
        gs.currRoomModif[5] = 0 // not already falling

        seg007.makeLooseFall(0x80)
        assertEquals(0x80, gs.currRoomModif[5])
        assertEquals(1, gs.trobsCount.toInt())
        assertEquals(0, gs.trobs[0].type) // type 0 = falling
    }

    @Test
    fun makeLooseFallIgnoresSolidLoose() {
        gs.currTilepos = 5
        gs.currRoom = 1
        gs.currRoomTiles[5] = Tiles.LOOSE or 0x20 // solid bit set
        gs.currRoomModif[5] = 0

        seg007.makeLooseFall(0x80)
        assertEquals(0, gs.currRoomModif[5])
        assertEquals(0, gs.trobsCount.toInt())
    }

    @Test
    fun looseMakeShakeStartsShakeOnIdleTile() {
        gs.currTilepos = 5
        gs.currRoom = 1
        gs.currentLevel = 2 // not loose_tiles_level (13)
        gs.currRoomModif[5] = 0

        seg007.looseMakeShake()
        assertEquals(0x80, gs.currRoomModif[5])
        assertEquals(1, gs.trobsCount.toInt())
    }

    @Test
    fun looseMakeShakeNoopOnLooseTilesLevel() {
        gs.currTilepos = 5
        gs.currRoom = 1
        gs.currentLevel = 13 // loose_tiles_level
        gs.currRoomModif[5] = 0

        seg007.looseMakeShake()
        assertEquals(0, gs.currRoomModif[5])
        assertEquals(0, gs.trobsCount.toInt())
    }

    @Test
    fun nextChomperTimingCycles() {
        assertEquals(12, seg007.nextChomperTiming(15))
        assertEquals(9, seg007.nextChomperTiming(12))
        // 9-3=6, 6 < 6 is false, return 6
        assertEquals(6, seg007.nextChomperTiming(9))
        assertEquals(13, seg007.nextChomperTiming(6)) // 6-3=3, 3<6 → 3+10=13
        assertEquals(10, seg007.nextChomperTiming(13)) // 13-3=10, not <6
        assertEquals(7, seg007.nextChomperTiming(10)) // 10-3=7, not <6
        assertEquals(14, seg007.nextChomperTiming(7)) // 7-3=4, 4<6 → 4+10=14
        assertEquals(11, seg007.nextChomperTiming(14)) // 14-3=11
        assertEquals(8, seg007.nextChomperTiming(11)) // 11-3=8
        assertEquals(15, seg007.nextChomperTiming(8)) // 8-3=5, 5<6 → 5+10=15
    }

    @Test
    fun removeLooseSetsEmptyAndReturnsLevelType() {
        gs.currentLevel = 4 // palace level type
        gs.currRoomTiles[5] = Tiles.LOOSE
        val result = seg007.removeLoose(1, 5)
        assertEquals(Tiles.EMPTY, gs.currRoomTiles[5])
        assertEquals(gs.custom.tblLevelType[4], result)
    }

    @Test
    fun addMobAddsEntryAndIncrementsCount() {
        gs.curmob.xh = 12
        gs.curmob.y = 100
        gs.curmob.room = 1
        gs.curmob.speed = 0
        gs.curmob.type = 0
        gs.curmob.row = 1

        seg007.addMob()
        assertEquals(1, gs.mobsCount.toInt())
        assertEquals(12, gs.mobs[0].xh)
        assertEquals(100, gs.mobs[0].y)
        assertEquals(1, gs.mobs[0].room)
    }

    @Test
    fun addMobDoesNotOverflow() {
        gs.mobsCount = 14
        seg007.addMob()
        assertEquals(14, gs.mobsCount.toInt())
    }

    @Test
    fun diedOnButtonChangesOpenerToFloorAndTriggers() {
        gs.currTilepos = 5
        gs.currRoom = 1
        gs.currRoomTiles[5] = Tiles.OPENER
        gs.currRoomModif[5] = 0
        // Set up doorlinks so trigger doesn't crash
        gs.level.doorlinks1[0] = 0x85 // tile=5, next=0 (has bit7 set→no next)
        gs.level.doorlinks2[0] = 0x05 // room bits → small room, timer=5

        seg007.diedOnButton()
        assertEquals(Tiles.FLOOR, gs.currRoomTiles[5])
        assertEquals(0, gs.currRoomModif[5])
    }

    @Test
    fun startLevelDoorSetsModifierAndAddsTrob() {
        seg007.startLevelDoor(1, 5)
        assertEquals(43, gs.currRoomModif[5])
        assertEquals(1, gs.trobsCount.toInt())
        assertEquals(3, gs.trobs[0].type)
    }
}
