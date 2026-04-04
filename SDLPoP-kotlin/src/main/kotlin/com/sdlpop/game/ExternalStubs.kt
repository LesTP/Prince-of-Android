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
    // --- seg005 (player control) ---
    var control: () -> Unit = { throw NotImplementedError("control (seg005)") }
    var seqtblOffsetChar: (Int) -> Unit = { seqIndex ->
        GameState.Char.currSeq = SequenceTable.seqtblOffsets[seqIndex]
    }
    var seqtblOffsetOpp: (Int) -> Unit = { seqIndex ->
        GameState.Opp.currSeq = SequenceTable.seqtblOffsets[seqIndex]
    }

    // --- seg007 (traps, triggers, animated tiles) ---
    var startChompers: () -> Unit = { throw NotImplementedError("start_chompers (seg007)") }
    var triggerButton: (Int, Int, Int) -> Unit = { _, _, _ -> throw NotImplementedError("trigger_button (seg007)") }
    var makeLooseFall: (Int) -> Unit = { _ -> throw NotImplementedError("make_loose_fall (seg007)") }
    var startAnimSpike: (Int, Int) -> Unit = { _, _ -> throw NotImplementedError("start_anim_spike (seg007)") }
    var isSpikePowerful: () -> Int = { throw NotImplementedError("is_spike_harmful (seg007)") }

    // --- seg002 (guard AI) ---
    var autocontrolOpponent: () -> Unit = { throw NotImplementedError("autocontrol_opponent (seg002)") }

    // --- seg003 (game loop helpers) ---
    var doFall: () -> Unit = { throw NotImplementedError("do_fall (seg003)") }

    // --- seg005 (sword/combat) ---
    var drawSword: () -> Unit = { throw NotImplementedError("draw_sword (seg005)") }
    var spiked: () -> Unit = { throw NotImplementedError("spiked (seg005)") }

    // --- seg008 (rendering) ---
    var getRoomAddress: (Int) -> Unit = { room ->
        // Inline implementation — same as C's get_room_address
        val gs = GameState
        gs.loadedRoom = room
        if (room != 0) {
            val base = (room - 1) * 30
            for (i in 0 until 30) {
                gs.currRoomTiles[i] = gs.level.fg[base + i]
                gs.currRoomModif[i] = gs.level.bg[base + i]
            }
        }
    }
    var setWipe: (Int, Int) -> Unit = { _, _ -> /* rendering stub — no-op for game logic tests */ }
    var setRedrawFull: (Int, Int) -> Unit = { _, _ -> /* rendering stub — no-op for game logic tests */ }
    var drawGuardHp: (Int, Int) -> Unit = { _, _ -> /* rendering stub */ }
    var eraseBottomText: (Int) -> Unit = { _ -> /* rendering stub */ }
    var addObjtable: (Int) -> Unit = { _ -> /* rendering stub */ }
    var getImage: (Int, Int) -> Pair<Int, Int>? = { _, _ -> null } // returns (width, height) or null
    var calcScreenXCoord: (Short) -> Short = { x -> x } // identity stub

    // --- seg000 (game flow) ---
    var playSound: (Int) -> Unit = { _ -> /* sound stub */ }
    var stopSounds: () -> Unit = { /* sound stub */ }
    var checkSoundPlaying: () -> Int = { 0 }
    var expired: () -> Unit = { throw NotImplementedError("expired (seg000)") }
    var displayTextBottom: (String) -> Unit = { _ -> /* text stub */ }
    var setStartPos: () -> Unit = { throw NotImplementedError("set_start_pos (seg000)") }
    var startGame: () -> Unit = { throw NotImplementedError("start_game (seg000)") }
    var loadGame: () -> Int = { throw NotImplementedError("load_game (seg000)") }
    var keyTestQuit: () -> Int = { 0 }
    var doPaused: () -> Int = { 0 }
    var diedOnButton: () -> Unit = { throw NotImplementedError("died_on_button (seg003)") }

    // --- replay ---
    var addReplayMove: () -> Unit = { /* replay stub */ }
    var doReplayMove: () -> Unit = { /* replay stub */ }
    var doAutoMoves: (Array<AutoMoveType>?) -> Unit = { _ -> throw NotImplementedError("do_auto_moves") }

    // --- seg005 (potion effects) ---
    var addLife: () -> Unit = { throw NotImplementedError("add_life (seg005)") }
    var featherFall: () -> Unit = { throw NotImplementedError("feather_fall (seg005)") }
    var toggleUpside: () -> Unit = { throw NotImplementedError("toggle_upside (seg005)") }
}
