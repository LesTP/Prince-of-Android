/*
SDLPoP-kotlin, a Kotlin port of SDLPoP (Prince of Persia).
Based on SDLPoP by Dávid Nagy, licensed under GPL v3+.

Module 10: seg005.c → Kotlin
Player control, movement, falling/landing, sword fighting.
38 functions, ~1,172 lines of C.

Phase 10a: Falling, landing, movement basics, control dispatch (14 functions).
*/

package com.sdlpop.game

import com.sdlpop.game.Tiles as T
import com.sdlpop.game.Directions as Dir
import com.sdlpop.game.Actions as Act
import com.sdlpop.game.FrameIds as FID
import com.sdlpop.game.SeqIds as Seq
import com.sdlpop.game.SoundIds as Snd
import com.sdlpop.game.CharIds as CID
import com.sdlpop.game.SwordStatus as Sword
import com.sdlpop.game.Control as Ctrl
import com.sdlpop.game.TileGeometry as TG
import com.sdlpop.game.EdgeType as ET

/**
 * seg005: Player control — movement, falling/landing, sword fighting.
 * All functions operate on [GameState] (aliased as `gs`).
 */
object Seg005 {
    private val gs = GameState
    private val seg006 = Seg006
    private val seg004 = Seg004
    private val stubs = ExternalStubs

    // ── seqtbl_offset_char / seqtbl_offset_opp ──

    /** seg005:000A — Set Char.curr_seq from sequence table offsets. */
    fun seqtblOffsetChar(seqIndex: Int) {
        gs.Char.currSeq = SequenceTable.seqtblOffsets[seqIndex]
    }

    /** seg005:001D — Set Opp.curr_seq from sequence table offsets. */
    fun seqtblOffsetOpp(seqIndex: Int) {
        gs.Opp.currSeq = SequenceTable.seqtblOffsets[seqIndex]
    }

    // ── do_fall ──

    /** seg005:0030 — Handle falling: scream, check grab, check floor/wall, land or continue falling. */
    fun doFall() {
        if (gs.isScreaming == 0 && gs.Char.fallY >= 31) {
            stubs.playSound(Snd.FALLING)
            gs.isScreaming = 1
        }
        if ((gs.yLand[gs.Char.currRow + 1].toInt() and 0xFFFF) > (gs.Char.y and 0xFFFF)) {
            // Still above landing row
            seg006.checkGrab()

            // FIX_GLIDE_THROUGH_WALL
            if (gs.fixes.fixGlideThroughWall != 0) {
                seg006.determineCol()
                seg006.getTileAtChar()
                if (gs.currTile2 == T.WALL ||
                    ((gs.currTile2 == T.DOORTOP || gs.currTile2 == T.DOORTOP_WITH_FLOOR) &&
                        gs.Char.direction == Dir.LEFT)
                ) {
                    val deltaX = seg006.distanceToEdgeWeight()
                    val deltaXReference = 10
                    if (deltaX >= 8) {
                        val adjusted = -5 + deltaX - deltaXReference
                        gs.Char.x = seg006.charDxForward(adjusted)
                        gs.Char.fallX = 0
                    }
                }
            }
        } else {
            // At or below landing row

            // FIX_JUMP_THROUGH_WALL_ABOVE_GATE
            if (gs.fixes.fixJumpThroughWallAboveGate != 0) {
                if (seg006.getTileAtChar() != T.GATE) {
                    seg006.determineCol()
                }
            }

            if (seg006.getTileAtChar() == T.WALL) {
                seg006.inWall()
            } else if (gs.fixes.fixDropThroughTapestry != 0 &&
                seg006.getTileAtChar() == T.DOORTOP && gs.Char.direction == Dir.LEFT
            ) {
                // FIX_DROP_THROUGH_TAPESTRY
                if (seg006.distanceToEdgeWeight() >= 8) {
                    seg006.inWall()
                }
            }

            if (seg006.tileIsFloor(gs.currTile2) != 0) {
                land()
            } else {
                seg006.incCurrRow()
            }
        }
    }

    // ── land ──

    /**
     * seg005:0090 — Handle landing after a fall.
     * C version uses goto for spike/death paths — refactored to labeled blocks.
     */
    fun land() {
        var seqId: Int
        gs.isScreaming = 0

        // USE_SUPER_HIGH_JUMP
        if (gs.fixes.enableSuperHighJump != 0) {
            gs.superJumpFall = 0
        }

        gs.Char.y = gs.yLand[gs.Char.currRow + 1].toInt()

        // Check if landed on spikes
        val onSpike = seg006.getTileAtChar() == T.SPIKE

        if (!onSpike) {
            // Not on spikes — adjust position if near edge
            if (seg006.tileIsFloor(seg006.getTileInfrontofChar()) == 0 &&
                seg006.distanceToEdgeWeight() < 3
            ) {
                gs.Char.x = seg006.charDxForward(-3)
            }

            // FIX_LAND_AGAINST_GATE_OR_TAPESTRY
            if (gs.fixes.fixLandAgainstGateOrTapestry != 0) {
                seg006.getTileInfrontofChar()
                if (gs.Char.direction == Dir.LEFT && (
                        (gs.currTile2 == T.GATE && seg004.canBumpIntoGate() != 0) ||
                            (gs.currTile2 == T.DOORTOP_WITH_FLOOR)
                        ) && seg006.distanceToEdgeWeight() < 3
                ) {
                    gs.Char.x = seg006.charDxForward(-3)
                }
            }

            stubs.startChompers()
        }

        // Check spike landing (either landed directly on spike, or alive and spikes behind/at)
        var fellOnSpikes = onSpike
        if (!fellOnSpikes && gs.Char.alive < 0) {
            // alive
            fellOnSpikes = (seg006.distanceToEdgeWeight() >= 12 &&
                seg006.getTileBehindChar() == T.SPIKE) ||
                seg006.getTileAtChar() == T.SPIKE
        }

        if (fellOnSpikes) {
            if (stubs.isSpikePowerful() != 0) {
                spiked()
                return
            }
            // FIX_SAFE_LANDING_ON_SPIKES
            if (gs.fixes.fixSafeLandingOnSpikes != 0 &&
                gs.currRoomModif[gs.currTilepos] == 0
            ) {
                spiked()
                return
            }
        }

        if (gs.Char.alive < 0) {
            // alive
            if (gs.Char.fallY < 22) {
                // fell 1 row — soft land
                seqId = landSoftOrActive()
                if (gs.Char.charid == CID.KID) {
                    stubs.playSound(Snd.SOFT_LAND)
                    gs.isGuardNotice = 1
                }
            } else if (gs.Char.fallY < 33) {
                // fell 2 rows
                if (gs.Char.charid == CID.SHADOW) {
                    // shadow: soft land like 1 row
                    seqId = landSoftOrActive()
                    if (gs.Char.charid == CID.KID) {
                        stubs.playSound(Snd.SOFT_LAND)
                        gs.isGuardNotice = 1
                    }
                } else if (gs.Char.charid == CID.GUARD) {
                    // guard: fatal
                    seqId = landFatal()
                } else {
                    // kid (or skeleton)
                    if (seg006.takeHp(1) == 0) {
                        // still alive
                        stubs.playSound(Snd.MEDIUM_LAND)
                        gs.isGuardNotice = 1
                        seqId = Seq.seq_20_medium_land
                    } else {
                        // dead — last HP
                        seqId = landFatalSound()
                    }
                }
            } else {
                // fell 3+ rows — fatal
                seqId = landFatal()
            }
        } else {
            // dead already
            seqId = landFatal()
        }

        seqtblOffsetChar(seqId)
        seg006.playSeq()
        gs.Char.fallY = 0
    }

    /** Helper: soft land or active-after-fall, depending on guard/sword state. */
    private fun landSoftOrActive(): Int {
        return if (gs.Char.charid >= CID.GUARD || gs.Char.sword == Sword.DRAWN) {
            gs.Char.sword = Sword.DRAWN
            Seq.seq_63_guard_active_after_fall
        } else {
            Seq.seq_17_soft_land
        }
    }

    /** Helper: fatal fall — take 100 HP, return crushed sequence. */
    private fun landFatal(): Int {
        seg006.takeHp(100)
        return landFatalSound()
    }

    /** Helper: play fell-to-death sound, return crushed sequence. */
    private fun landFatalSound(): Int {
        stubs.playSound(Snd.FELL_TO_DEATH)
        return Seq.seq_22_crushed
    }

    // ── spiked ──

    /** seg005:01B7 — Character landed on spikes. */
    fun spiked() {
        gs.currRoomModif[gs.currTilepos] = 0xFF
        gs.Char.y = gs.yLand[gs.Char.currRow + 1].toInt()

        // FIX_OFFSCREEN_GUARDS_DISAPPEARING
        if (gs.fixes.fixOffscreenGuardsDisappearing != 0) {
            var spikeCol = gs.tileCol.toInt()
            if (gs.currRoom.toInt() != gs.Char.room) {
                if (gs.currRoom.toInt() == gs.level.roomlinks[gs.Char.room - 1].right) {
                    spikeCol += 10
                } else if (gs.currRoom.toInt() == gs.level.roomlinks[gs.Char.room - 1].left) {
                    spikeCol -= 10
                }
            }
            gs.Char.x = gs.xBump[spikeCol + TG.FIRST_ONSCREEN_COLUMN] + 10
        } else {
            gs.Char.x = gs.xBump[gs.tileCol.toInt() + TG.FIRST_ONSCREEN_COLUMN] + 10
        }

        gs.Char.x = seg006.charDxForward(8)
        gs.Char.fallY = 0
        stubs.playSound(Snd.SPIKED)
        seg006.takeHp(100)
        seqtblOffsetChar(Seq.seq_51_spiked)
        seg006.playSeq()
    }

    // ── control ──

    /** seg005:0213 — Main control dispatch based on character frame and state. */
    fun control() {
        val charFrame = gs.Char.frame
        if (gs.Char.alive >= 0) {
            // dead
            if (charFrame == FID.frame_15_stand ||
                charFrame == FID.frame_166_stand_inactive ||
                charFrame == FID.frame_158_stand_with_sword ||
                charFrame == FID.frame_171_stand_with_sword
            ) {
                seqtblOffsetChar(Seq.seq_71_dying)
            }
        } else {
            // alive
            val charAction = gs.Char.action
            if (charAction == Act.BUMPED || charAction == Act.IN_FREEFALL) {
                seg006.releaseArrows()
            } else if (gs.Char.sword == Sword.DRAWN) {
                controlWithSword()
            } else if (gs.Char.charid >= CID.GUARD) {
                seg006.controlGuardInactive()
            } else if (charFrame == FID.frame_15_stand ||
                (charFrame >= FID.frame_50_turn && charFrame < 53)
            ) {
                controlStanding()
            } else if (charFrame == FID.frame_48_turn) {
                controlTurning()
            } else if (charFrame < 4) {
                controlStartrun()
            } else if (charFrame >= FID.frame_67_start_jump_up_1 && charFrame < FID.frame_70_jumphang) {
                controlJumpup()
            } else if (charFrame < 15) {
                controlRunning()
            } else if (charFrame >= FID.frame_87_hanging_1 && charFrame < 100) {
                controlHanging()
            } else if (charFrame == FID.frame_109_crouch) {
                controlCrouched()
            } else if (gs.fixes.enableCrouchAfterClimbing != 0 &&
                gs.Char.currSeq >= SequenceTable.seqtblOffsets[Seq.seq_50_crouch] &&
                gs.Char.currSeq < SequenceTable.seqtblOffsets[Seq.seq_49_stand_up_from_crouch]
            ) {
                if (gs.controlForward != Ctrl.IGNORE) gs.controlForward = Ctrl.RELEASED
            }

            // FIX_MOVE_AFTER_DRINK
            if (gs.fixes.fixMoveAfterDrink != 0 &&
                charFrame >= FID.frame_191_drink && charFrame <= FID.frame_205_drink
            ) {
                seg006.releaseArrows()
            }

            // FIX_MOVE_AFTER_SHEATHE
            if (gs.fixes.fixMoveAfterSheathe != 0 &&
                gs.Char.currSeq >= SequenceTable.seqtblOffsets[Seq.seq_92_put_sword_away] &&
                gs.Char.currSeq < SequenceTable.seqtblOffsets[Seq.seq_93_put_sword_away_fast]
            ) {
                seg006.releaseArrows()
            }
        }
    }

    // ── control_crouched ──

    /** seg005:02EB — Control while crouched: level 1 music, stand up, crouch-hop. */
    fun controlCrouched() {
        if (gs.needLevel1Music != 0 && gs.currentLevel == gs.custom.introMusicLevel) {
            if (stubs.checkSoundPlaying() == 0) {
                if (gs.needLevel1Music == 1) {
                    stubs.playSound(Snd.PRESENTATION)
                    gs.needLevel1Music = 2
                } else {
                    // During replays, crouch immobilization gets cancelled in do_replay_move()
                    // USE_REPLAY: skip recording/replaying logic (stub-only)
                    gs.needLevel1Music = 0
                }
            }
        } else {
            gs.needLevel1Music = 0
            if (gs.controlShift2 == Ctrl.HELD && checkGetItem()) return
            if (gs.controlY != Ctrl.HELD_DOWN) {
                seqtblOffsetChar(Seq.seq_49_stand_up_from_crouch)
            } else {
                if (gs.controlForward == Ctrl.HELD) {
                    gs.controlForward = Ctrl.IGNORE
                    seqtblOffsetChar(Seq.seq_79_crouch_hop)
                }
            }
        }
    }

    // ── control_turning ──

    /** seg005:058F — Control during turn animation. */
    fun controlTurning() {
        if (gs.controlShift >= Ctrl.RELEASED && gs.controlX == Ctrl.HELD_FORWARD && gs.controlY >= Ctrl.RELEASED) {
            // FIX_TURN_RUN_NEAR_WALL
            if (gs.fixes.fixTurnRunningNearWall != 0) {
                val distance = seg004.getEdgeDistance()
                if (gs.edgeType == ET.WALL && gs.currTile2 != T.CHOMPER && distance < 8) {
                    gs.controlForward = Ctrl.HELD
                } else {
                    seqtblOffsetChar(Seq.seq_43_start_run_after_turn)
                }
            } else {
                seqtblOffsetChar(Seq.seq_43_start_run_after_turn)
            }
        }

        // Joystick: clear controls to prevent unintended actions after turn
        if (gs.isJoystMode != 0) {
            if (gs.controlUp == Ctrl.HELD && gs.controlY >= Ctrl.RELEASED) {
                gs.controlUp = Ctrl.RELEASED
            }
            if (gs.controlDown == Ctrl.HELD && gs.controlY <= Ctrl.RELEASED) {
                gs.controlDown = Ctrl.RELEASED
            }
            if (gs.controlBackward == Ctrl.HELD && gs.controlX == Ctrl.RELEASED) {
                gs.controlBackward = Ctrl.RELEASED
            }
        }
    }

    // ── crouch ──

    /** seg005:05AD — Start crouch sequence. */
    fun crouch() {
        seqtblOffsetChar(Seq.seq_50_crouch)
        gs.controlDown = seg006.releaseArrows()
    }

    // ── back_pressed ──

    /** seg005:05BE — Handle backward press: turn or turn-draw-sword. */
    fun backPressed() {
        val seqId: Int
        gs.controlBackward = seg006.releaseArrows()
        if (gs.haveSword == 0 ||
            gs.canGuardSeeKid.toInt() < 2 ||
            seg006.charOppDist() > 0 ||
            seg006.distanceToEdgeWeight() < 2
        ) {
            seqId = Seq.seq_5_turn
        } else {
            gs.Char.sword = Sword.DRAWN
            gs.offguard = 0
            seqId = Seq.seq_89_turn_draw_sword
        }
        seqtblOffsetChar(seqId)
    }

    // ── forward_pressed ──

    /** seg005:060F — Handle forward press: run or safe step near wall. */
    fun forwardPressed() {
        val distance: Int = seg004.getEdgeDistance()

        // ALLOW_CROUCH_AFTER_CLIMBING
        if (gs.fixes.enableCrouchAfterClimbing != 0 && gs.controlDown == Ctrl.HELD) {
            downPressed()
            gs.controlForward = Ctrl.RELEASED
            return
        }

        if (gs.edgeType == ET.WALL && gs.currTile2 != T.CHOMPER && distance < 8) {
            if (gs.controlForward == Ctrl.HELD) {
                safeStep()
            }
        } else {
            seqtblOffsetChar(Seq.seq_1_start_run)
        }
    }

    // ── control_running ──

    /** seg005:0649 — Control while running: stop, turn, jump, crouch. */
    fun controlRunning() {
        if (gs.controlX == Ctrl.RELEASED && (gs.Char.frame == FID.frame_7_run || gs.Char.frame == FID.frame_11_run)) {
            gs.controlForward = seg006.releaseArrows()
            seqtblOffsetChar(Seq.seq_13_stop_run)
        } else if (gs.controlX == Ctrl.HELD_BACKWARD) {
            gs.controlBackward = seg006.releaseArrows()
            seqtblOffsetChar(Seq.seq_6_run_turn)
        } else if (gs.controlY == Ctrl.HELD_UP && gs.controlUp == Ctrl.HELD) {
            runJump()
        } else if (gs.controlDown == Ctrl.HELD) {
            gs.controlDown = Ctrl.IGNORE
            seqtblOffsetChar(Seq.seq_26_crouch_while_running)
        }
    }

    // ── safe_step ──

    /** seg005:06A8 — Careful step toward edge. */
    fun safeStep() {
        gs.controlShift2 = Ctrl.IGNORE
        gs.controlForward = Ctrl.IGNORE
        val distance = seg004.getEdgeDistance()
        if (distance != 0) {
            gs.Char.repeat = 1
            seqtblOffsetChar(distance + 28) // 29..42: safe step to edge
        } else if (gs.edgeType != ET.WALL && gs.Char.repeat != 0) {
            gs.Char.repeat = 0
            seqtblOffsetChar(Seq.seq_44_step_on_edge)
        } else {
            seqtblOffsetChar(Seq.seq_39_safe_step_11)
        }
    }

    // ── control_startrun ──

    /** seg005:07FF — Control at start of run: allow standing jump. */
    fun controlStartrun() {
        if (gs.controlY == Ctrl.HELD_UP && gs.controlX == Ctrl.HELD_FORWARD) {
            standingJump()
        }
    }

    // ── Stub references for Phase 10b/10c functions ──
    // These will be filled in during subsequent phases.

    /** Placeholder — Phase 10b */
    fun controlStanding() { /* Phase 10b */ }
    fun upPressed() { /* Phase 10b */ }
    fun downPressed() { /* Phase 10b */ }
    fun standingJump() { /* Phase 10b */ }
    fun checkJumpUp() { /* Phase 10b */ }
    fun jumpUpOrGrab() { /* Phase 10b */ }
    fun grabUpNoFloorBehind() { /* Phase 10b */ }
    fun jumpUp() { /* Phase 10b */ }
    fun grabUpWithFloorBehind() { /* Phase 10b */ }
    fun runJump() { /* Phase 10b */ }
    fun controlHanging() { /* Phase 10b */ }
    fun canClimbUp() { /* Phase 10b */ }
    fun hangFall() { /* Phase 10b */ }
    fun goUpLeveldoor() { /* Phase 10b */ }
    fun checkGetItem(): Boolean = false // Phase 10b
    fun getItem() { /* Phase 10b */ }
    fun controlJumpup() { /* Phase 10b */ }

    /** Placeholder — Phase 10c */
    fun drawSword() { /* Phase 10c */ }
    fun controlWithSword() { /* Phase 10c */ }
    fun swordfight() { /* Phase 10c */ }
    fun swordStrike() { /* Phase 10c */ }
    fun parry() { /* Phase 10c */ }
    fun backWithSword() { /* Phase 10c */ }
    fun forwardWithSword() { /* Phase 10c */ }
}
