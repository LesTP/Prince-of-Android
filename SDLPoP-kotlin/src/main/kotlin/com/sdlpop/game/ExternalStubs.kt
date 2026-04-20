/*
SDLPoP-kotlin, a Kotlin port of SDLPoP (Prince of Persia).
Based on SDLPoP by Dávid Nagy, licensed under GPL v3+.

Module 8: External dependency stubs.
Functions from other segments (seg000, seg002, seg003, seg005, seg007, seg008, replay)
that seg006 calls. These throw NotImplementedError until their modules are translated.
The stub pattern allows seg006 to compile and be unit-tested in isolation.
*/

package com.sdlpop.game

/**
 * External function stubs — callable references that seg006 uses.
 * Each is a `var` so the real implementation can be wired in when its module is translated.
 */
object ExternalStubs {
    var preserveRoomBufferMutations: Boolean = false

    // --- seg005 (player control) ---
    var control: () -> Unit = { Seg005.control() }
    var seqtblOffsetChar: (Int) -> Unit = { seqIndex ->
        GameState.Char.currSeq = SequenceTable.seqtblOffsets[seqIndex]
    }
    var seqtblOffsetOpp: (Int) -> Unit = { seqIndex ->
        GameState.Opp.currSeq = SequenceTable.seqtblOffsets[seqIndex]
    }

    // --- seg007 (traps, triggers, animated tiles) ---
    var startChompers: () -> Unit = { Seg007.startChompers() }
    var triggerButton: (Int, Int, Int) -> Unit = { playsound, buttonType, modifier -> Seg007.triggerButton(playsound, buttonType, modifier) }
    var makeLooseFall: (Int) -> Unit = { modifier -> Seg007.makeLooseFall(modifier) }
    var startAnimSpike: (Int, Int) -> Unit = { room, tilepos -> Seg007.startAnimSpike(room, tilepos) }
    var isSpikePowerful: () -> Int = { Seg007.isSpikeHarmful() }

    // --- seg002 (guard AI) ---
    var autocontrolOpponent: () -> Unit = { Seg002.autocontrolOpponent() }

    // --- seg002 (guard AI) ---
    var leaveGuard: () -> Unit = { Seg002.leaveGuard() }

    // --- seg003 (game loop helpers) ---
    var doFall: () -> Unit = { Seg005.doFall() }
    var doPickup: (Int) -> Unit = { objType -> Seg006.doPickup(objType) }

    // --- seg005 (sword/combat) ---
    var drawSword: () -> Unit = { Seg005.drawSword() }
    var spiked: () -> Unit = { Seg005.spiked() }

    // --- seg008 (rendering) ---
    var getRoomAddress: (Int) -> Unit = { room -> loadRoomAddress(room) }
    var setWipe: (Int, Int) -> Unit = { _, _ -> /* rendering stub — no-op for game logic tests */ }
    var setRedrawFull: (Int, Int) -> Unit = { _, _ -> /* rendering stub — no-op for game logic tests */ }
    var drawGuardHp: (Int, Int) -> Unit = { _, _ -> /* rendering stub */ }
    var drawKidHp: (Int, Int) -> Unit = { _, _ -> /* rendering stub */ }
    var eraseBottomText: (Int) -> Unit = { _ -> /* rendering stub */ }
    var addObjtable: (Int) -> Unit = { _ -> /* rendering stub */ }
    var getImage: (Int, Int) -> Pair<Int, Int>? = { _, _ -> null } // returns (width, height) or null
    var calcScreenXCoord: (Short) -> Short = { x -> x } // identity stub

    // --- seg000 (game flow) ---
    var playSound: (Int) -> Unit = { _ -> /* sound stub */ }
    var stopSounds: () -> Unit = { /* sound stub */ }
    var checkSoundPlaying: () -> Int = { 0 }
    var expired: () -> Unit = {
        // Minimal headless expired: set restart state, call startGame stub
        val gs = GameState
        if (gs.demoMode == 0) {
            // skip rendering (free_surface, clear_screen, load_intro) in headless mode
        }
        gs.startLevel = (-1).toShort()
        startGame()
    }
    var displayTextBottom: (String) -> Unit = { _ -> /* text stub */ }
    var setStartPos: () -> Unit = { Seg003.setStartPos() }
    var startGame: () -> Unit = { /* stub — in headless replay, game restart is a no-op */ }
    var loadGame: () -> Int = { throw NotImplementedError("load_game (seg000)") }
    var keyTestQuit: () -> Int = { 0 }
    var doPaused: () -> Int = { 0 }
    var diedOnButton: () -> Unit = { Seg007.diedOnButton() }

    // --- replay ---
    var addReplayMove: () -> Unit = { /* replay stub */ }
    var doReplayMove: () -> Unit = { /* replay stub */ }
    var doAutoMoves: (Array<AutoMoveType>?) -> Unit = { moves ->
        requireNotNull(moves) { "do_auto_moves requires a non-null move table" }
        Seg002.doAutoMoves(moves)
    }

    // --- seg000 (potion effects) ---
    var addLife: () -> Unit = {
        val gs = GameState
        var hpmax = gs.hitpMax
        hpmax++
        if (hpmax > gs.custom.maxHitpAllowed) hpmax = gs.custom.maxHitpAllowed
        gs.hitpMax = hpmax
        setHealthLife()
    }
    var setHealthLife: () -> Unit = {
        val gs = GameState
        gs.hitpDelta = (gs.hitpMax - gs.hitpCurr).toShort()
    }
    var featherFall: () -> Unit = {
        val gs = GameState
        if (gs.fixes.fixQuicksaveDuringFeather != 0) {
            // FEATHER_FALL_LENGTH (18.75) * ticks_per_sec (12 at DOS timing) = 225
            gs.isFeatherFall = 225
        } else {
            gs.isFeatherFall = 1
        }
        gs.flashColor = 2 // green
        gs.flashTime = 3
        stopSounds()
        playSound(SoundIds.LOW_WEIGHT)
    }
    var toggleUpside: () -> Unit = {
        val gs = GameState
        gs.upsideDown = gs.upsideDown.inv()
        gs.needRedrawBecauseFlipped = 1
    }

    fun loadRoomAddress(room: Int) {
        val gs = GameState
        if (preserveRoomBufferMutations && gs.loadedRoom == room) return

        if (preserveRoomBufferMutations) syncLoadedRoom()
        gs.loadedRoom = room
        if (room != 0) {
            val base = (room - 1) * 30
            for (i in 0 until 30) {
                gs.currRoomTiles[i] = gs.level.fg[base + i]
                gs.currRoomModif[i] = gs.level.bg[base + i]
            }
        }
    }

    fun syncLoadedRoom() {
        val gs = GameState
        val room = gs.loadedRoom
        if (room != 0) {
            val base = (room - 1) * 30
            for (i in 0 until 30) {
                gs.level.fg[base + i] = gs.currRoomTiles[i] and 0xFF
                gs.level.bg[base + i] = gs.currRoomModif[i] and 0xFF
            }
        }
    }
}
