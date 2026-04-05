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

    // ══════════════════════════════════════════════════════════
    // Phase 10b: Standing control, climbing, items
    // ══════════════════════════════════════════════════════════

    // ── control_standing ──

    /** seg005:0358 — Control while standing: sword draw, movement, jump, crouch. */
    fun controlStanding() {
        if (gs.controlShift2 == Ctrl.HELD && gs.controlShift == Ctrl.HELD && checkGetItem()) {
            return
        }
        if (gs.Char.charid != CID.KID && gs.controlDown == Ctrl.HELD && gs.controlForward == Ctrl.HELD) {
            drawSword()
            return
        }
        if (gs.haveSword != 0) {
            if (gs.offguard != 0 && gs.controlShift >= Ctrl.RELEASED) {
                // goto loc_6213 — fall through to forward check below
            } else {
                if (gs.canGuardSeeKid.toInt() >= 2) {
                    val distance = seg006.charOppDist()
                    if (distance >= -10 && distance < 90) {
                        gs.holdingSword = 1
                        if ((distance and 0xFFFF) < ((-6) and 0xFFFF)) {
                            // (word)distance < (word)-6 — unsigned comparison
                            if (gs.Opp.charid == CID.SHADOW &&
                                (gs.Opp.action == Act.IN_MIDAIR ||
                                    (gs.Opp.frame >= FID.frame_107_fall_land_1 && gs.Opp.frame < 118))
                            ) {
                                gs.offguard = 0
                            } else {
                                drawSword()
                                return
                            }
                        } else {
                            backPressed()
                            return
                        }
                    }
                } else {
                    gs.offguard = 0
                }
            }
        }
        // loc_6213 path and normal standing controls
        if (gs.controlShift == Ctrl.HELD) {
            if (gs.controlBackward == Ctrl.HELD) {
                backPressed()
            } else if (gs.controlUp == Ctrl.HELD) {
                upPressed()
            } else if (gs.controlDown == Ctrl.HELD) {
                downPressed()
            } else if (gs.controlX == Ctrl.HELD_FORWARD && gs.controlForward == Ctrl.HELD) {
                safeStep()
            }
        } else if (gs.controlForward == Ctrl.HELD) {
            if (gs.isKeyboardMode != 0 && gs.controlUp == Ctrl.HELD) {
                standingJump()
            } else {
                forwardPressed()
            }
        } else if (gs.controlBackward == Ctrl.HELD) {
            backPressed()
        } else if (gs.controlUp == Ctrl.HELD) {
            if (gs.isKeyboardMode != 0 && gs.controlForward == Ctrl.HELD) {
                standingJump()
            } else {
                upPressed()
            }
        } else if (gs.controlDown == Ctrl.HELD) {
            downPressed()
        } else if (gs.controlX == Ctrl.HELD_FORWARD) {
            forwardPressed()
        }
    }

    // ── up_pressed ──

    /** seg005:0482 — Up pressed: enter level door or jump up. */
    fun upPressed() {
        var leveldoorTilepos = -1
        if (seg006.getTileAtChar() == T.LEVEL_DOOR_LEFT) leveldoorTilepos = gs.currTilepos
        else if (seg006.getTileBehindChar() == T.LEVEL_DOOR_LEFT) leveldoorTilepos = gs.currTilepos
        else if (seg006.getTileInfrontofChar() == T.LEVEL_DOOR_LEFT) leveldoorTilepos = gs.currTilepos

        if (leveldoorTilepos != -1 &&
            gs.level.startRoom != gs.drawnRoom &&
            (if (gs.fixes.fixExitDoor != 0)
                gs.currRoomModif[leveldoorTilepos] >= 42
            else
                gs.leveldoorOpen != 0)
        ) {
            goUpLeveldoor()
            return
        }

        // USE_TELEPORTS: skipped (not in reference build)

        // Jump up
        if (gs.controlX == Ctrl.HELD_FORWARD) {
            standingJump()
        } else {
            checkJumpUp()
        }
    }

    // ── down_pressed ──

    /** seg005:04C7 — Down pressed: climb down, crouch, or adjust position. */
    fun downPressed() {
        gs.controlDown = Ctrl.IGNORE
        if (seg006.tileIsFloor(seg006.getTileInfrontofChar()) == 0 &&
            seg006.distanceToEdgeWeight() < 3
        ) {
            gs.Char.x = seg006.charDxForward(5)
            seg006.loadFramDetCol()
        } else {
            if (seg006.tileIsFloor(seg006.getTileBehindChar()) == 0 &&
                seg006.distanceToEdgeWeight() >= 8
            ) {
                gs.throughTile = seg006.getTileBehindChar()
                seg006.getTileAtChar()
                if (seg006.canGrab() != 0 &&
                    // ALLOW_CROUCH_AFTER_CLIMBING
                    !(gs.fixes.enableCrouchAfterClimbing != 0 && gs.controlForward == Ctrl.HELD) &&
                    (gs.Char.direction >= Dir.RIGHT ||
                        seg006.getTileAtChar() != T.GATE ||
                        gs.currRoomModif[gs.currTilepos] shr 2 >= 6)
                ) {
                    gs.Char.x = seg006.charDxForward(seg006.distanceToEdgeWeight() - 9)
                    seqtblOffsetChar(Seq.seq_68_climb_down)
                } else {
                    crouch()
                }
            } else {
                crouch()
            }
        }
    }

    // ── go_up_leveldoor ──

    /** seg005:0574 — Position char and start level door sequence. */
    fun goUpLeveldoor() {
        gs.Char.x = gs.xBump[gs.tileCol.toInt() + TG.FIRST_ONSCREEN_COLUMN] + 10
        gs.Char.direction = Dir.LEFT
        seqtblOffsetChar(Seq.seq_70_go_up_on_level_door)
    }

    // ── standing_jump ──

    /** seg005:0825 — Start standing jump. */
    fun standingJump() {
        gs.controlUp = Ctrl.IGNORE
        gs.controlForward = Ctrl.IGNORE
        seqtblOffsetChar(Seq.seq_3_standing_jump)
    }

    // ── check_jump_up ──

    /** seg005:0836 — Check tiles above and decide jump/grab direction. */
    fun checkJumpUp() {
        gs.controlUp = seg006.releaseArrows()
        gs.throughTile = seg006.getTileAboveChar()
        seg006.getTileFrontAboveChar()
        if (seg006.canGrab() != 0) {
            grabUpWithFloorBehind()
        } else {
            gs.throughTile = seg006.getTileBehindAboveChar()
            seg006.getTileAboveChar()
            if (seg006.canGrab() != 0) {
                jumpUpOrGrab()
            } else {
                jumpUp()
            }
        }
    }

    // ── jump_up_or_grab ──

    /** seg005:087B — Jump up or grab depending on distance and floor behind. */
    fun jumpUpOrGrab() {
        val distance = seg006.distanceToEdgeWeight()
        if (distance < 6) {
            jumpUp()
        } else if (seg006.tileIsFloor(seg006.getTileBehindChar()) == 0) {
            grabUpNoFloorBehind()
        } else {
            gs.Char.x = seg006.charDxForward(distance - TG.TILE_SIZEX)
            seg006.loadFramDetCol()
            grabUpWithFloorBehind()
        }
    }

    // ── grab_up_no_floor_behind ──

    /** seg005:08C7 — Grab up when no floor behind. */
    fun grabUpNoFloorBehind() {
        seg006.getTileAboveChar()
        gs.Char.x = seg006.charDxForward(seg006.distanceToEdgeWeight() - 10)
        seqtblOffsetChar(Seq.seq_16_jump_up_and_grab)
    }

    // ── jump_up ──

    /** seg005:08E6 — Jump straight up, checking ceiling. */
    fun jumpUp() {
        gs.controlUp = seg006.releaseArrows()
        val distance = seg004.getEdgeDistance()
        if (distance < 4 && gs.edgeType == ET.WALL) {
            gs.Char.x = seg006.charDxForward(distance - 3)
        }
        // FIX_JUMP_DISTANCE_AT_EDGE
        if (gs.fixes.fixJumpDistanceAtEdge != 0 && distance == 3 && gs.edgeType == ET.CLOSER) {
            gs.Char.x = seg006.charDxForward(-1)
        }

        // USE_SUPER_HIGH_JUMP path
        if (gs.fixes.enableSuperHighJump != 0) {
            val deltaX: Int = if (gs.isFeatherFall != 0 &&
                seg006.tileIsFloor(seg006.getTileAboveChar()) == 0 &&
                gs.currTile2 != T.WALL
            ) {
                if (gs.Char.direction == Dir.LEFT) 1 else 3
            } else {
                0
            }
            val charCol = seg006.getTileDivMod(seg006.backDeltaX(deltaX) + seg006.dxWeight() - 6)
            seg006.getTile(gs.Char.room, charCol, gs.Char.currRow - 1)
            if (gs.currTile2 != T.WALL && seg006.tileIsFloor(gs.currTile2) == 0) {
                if (gs.fixes.enableSuperHighJump != 0 && gs.isFeatherFall != 0) {
                    if (gs.currRoom.toInt() == 0 && gs.Char.currRow == 0) {
                        seqtblOffsetChar(Seq.seq_14_jump_up_into_ceiling)
                    } else {
                        seg006.getTile(gs.Char.room, charCol, gs.Char.currRow - 2)
                        var isTopFloor = seg006.tileIsFloor(gs.currTile2) != 0 || gs.currTile2 == T.WALL
                        if (isTopFloor && gs.currTile2 == T.LOOSE &&
                            (gs.currRoomTiles[gs.currTilepos] and 0x20) == 0
                        ) {
                            isTopFloor = false
                        }
                        gs.superJumpTimer = if (isTopFloor) 22 else 24
                        gs.superJumpRoom = gs.currRoom.toInt()
                        gs.superJumpCol = gs.tileCol.toInt()
                        gs.superJumpRow = gs.tileRow.toInt()
                        seqtblOffsetChar(Seq.seq_48_super_high_jump)
                    }
                } else {
                    seqtblOffsetChar(Seq.seq_28_jump_up_with_nothing_above)
                }
            } else {
                seqtblOffsetChar(Seq.seq_14_jump_up_into_ceiling)
            }
        } else {
            // Standard path (no USE_SUPER_HIGH_JUMP)
            seg006.getTile(
                gs.Char.room,
                seg006.getTileDivMod(seg006.backDeltaX(0) + seg006.dxWeight() - 6),
                gs.Char.currRow - 1
            )
            if (gs.currTile2 != T.WALL && seg006.tileIsFloor(gs.currTile2) == 0) {
                seqtblOffsetChar(Seq.seq_28_jump_up_with_nothing_above)
            } else {
                seqtblOffsetChar(Seq.seq_14_jump_up_into_ceiling)
            }
        }
    }

    // ── grab_up_with_floor_behind ──

    /** seg005:0AA8 — Grab up when there is floor behind. */
    fun grabUpWithFloorBehind() {
        val distance = seg006.distanceToEdgeWeight()
        val edgeDistance = seg004.getEdgeDistance()

        val jumpStraight = if (gs.fixes.fixEdgeDistanceCheckWhenClimbing != 0) {
            // FIX_EDGE_DISTANCE_CHECK_WHEN_CLIMBING
            distance < 4 && gs.edgeType != ET.WALL
        } else {
            distance < 4 && edgeDistance < 4 && gs.edgeType != ET.WALL
        }

        if (jumpStraight) {
            gs.Char.x = seg006.charDxForward(distance)
            seqtblOffsetChar(Seq.seq_8_jump_up_and_grab_straight)
        } else {
            gs.Char.x = seg006.charDxForward(distance - 4)
            seqtblOffsetChar(Seq.seq_24_jump_up_and_grab_forward)
        }
    }

    // ── run_jump ──

    /** seg005:0AF7 — Running jump: align to edge and jump. */
    fun runJump() {
        if (gs.Char.frame >= FID.frame_7_run) {
            val xpos = seg006.charDxForward(4)
            var col = seg006.getTileDivModM7(xpos)
            var posAdjustment: Int
            for (tilesForward in 0 until 2) {
                col += gs.dirFront[gs.Char.direction + 1]
                seg006.getTile(gs.Char.room, col, gs.Char.currRow)
                if (gs.currTile2 == T.SPIKE || seg006.tileIsFloor(gs.currTile2) == 0) {
                    posAdjustment = seg006.distanceToEdge(xpos) + TG.TILE_SIZEX * tilesForward - TG.TILE_SIZEX
                    if ((posAdjustment and 0xFFFF) < ((-8) and 0xFFFF) || posAdjustment >= 2) {
                        if (posAdjustment < 128) return
                        posAdjustment = -3
                    }
                    gs.Char.x = seg006.charDxForward(posAdjustment + 4)
                    break
                }
            }
            gs.controlUp = seg006.releaseArrows()
            seqtblOffsetChar(Seq.seq_4_run_jump)
        }
    }

    // ── control_hanging ──

    /** seg005:0968 — Control while hanging: climb up, hang against wall, or fall. */
    fun controlHanging() {
        if (gs.Char.alive < 0) {
            if (gs.grabTimer == 0 && gs.controlY == Ctrl.HELD) {
                canClimbUp()
            } else if (gs.controlShift == Ctrl.HELD ||
                (gs.fixes.enableSuperHighJump != 0 && gs.superJumpFall != 0 && gs.controlY == Ctrl.HELD)
            ) {
                if (gs.Char.action != Act.HANG_STRAIGHT &&
                    (seg006.getTileAtChar() == T.WALL ||
                        (gs.Char.direction == Dir.LEFT &&
                            (gs.currTile2 == T.DOORTOP_WITH_FLOOR || gs.currTile2 == T.DOORTOP)))
                ) {
                    if (gs.grabTimer == 0) {
                        stubs.playSound(Snd.BUMPED)
                    }
                    seqtblOffsetChar(Seq.seq_25_hang_against_wall)
                } else {
                    if (seg006.tileIsFloor(seg006.getTileAboveChar()) == 0) {
                        hangFall()
                    }
                }
            } else {
                hangFall()
            }
        } else {
            hangFall()
        }
    }

    // ── can_climb_up ──

    /** seg005:09DF — Check if can climb up: mirror/chomper/closed gate block. */
    fun canClimbUp() {
        var seqId = Seq.seq_10_climb_up
        gs.controlUp = seg006.releaseArrows()
        gs.controlShift2 = gs.controlUp
        if (gs.fixes.enableSuperHighJump != 0) {
            gs.superJumpFall = 0
        }
        seg006.getTileAboveChar()
        if (((gs.currTile2 == T.MIRROR || gs.currTile2 == T.CHOMPER) &&
                gs.Char.direction == Dir.RIGHT) ||
            (gs.currTile2 == T.GATE && gs.Char.direction != Dir.RIGHT &&
                gs.currRoomModif[gs.currTilepos] shr 2 < 6)
        ) {
            seqId = Seq.seq_73_climb_up_to_closed_gate
        }
        seqtblOffsetChar(seqId)
    }

    // ── hang_fall ──

    /** seg005:0A46 — Release ledge and fall or land. */
    fun hangFall() {
        gs.controlDown = seg006.releaseArrows()
        if (gs.fixes.enableSuperHighJump != 0) {
            gs.superJumpFall = 0
        }
        if (seg006.tileIsFloor(seg006.getTileBehindChar()) == 0 &&
            seg006.tileIsFloor(seg006.getTileAtChar()) == 0
        ) {
            seqtblOffsetChar(Seq.seq_23_release_ledge_and_fall)
        } else {
            if (seg006.getTileAtChar() == T.WALL ||
                (gs.Char.direction < Dir.RIGHT &&
                    (gs.currTile2 == T.DOORTOP_WITH_FLOOR || gs.currTile2 == T.DOORTOP))
            ) {
                gs.Char.x = seg006.charDxForward(-7)
            }
            seqtblOffsetChar(Seq.seq_11_release_ledge_and_land)
        }
    }

    // ── check_get_item ──

    /** seg005:06F0 — Check if there's a potion or sword to pick up. Returns true if item found. */
    fun checkGetItem(): Boolean {
        if (seg006.getTileAtChar() == T.POTION || gs.currTile2 == T.SWORD) {
            if (seg006.tileIsFloor(seg006.getTileBehindChar()) == 0) {
                return false
            }
            gs.Char.x = seg006.charDxForward(-14)
            seg006.loadFramDetCol()
        }
        if (seg006.getTileInfrontofChar() == T.POTION || gs.currTile2 == T.SWORD) {
            getItem()
            return true
        }
        return false
    }

    // ── get_item ──

    /** seg005:073E — Pick up potion or sword. */
    fun getItem() {
        if (gs.Char.frame != FID.frame_109_crouch) {
            val distance = seg004.getEdgeDistance()
            if (gs.edgeType != ET.FLOOR) {
                gs.Char.x = seg006.charDxForward(distance)
            }
            if (gs.Char.direction >= Dir.RIGHT) {
                // C: char_dx_forward((curr_tile2 == tiles_10_potion) - 2)
                // (curr_tile2 == tiles_10_potion) evaluates to 1 or 0 in C
                val potionAdj = if (gs.currTile2 == T.POTION) 1 else 0
                gs.Char.x = seg006.charDxForward(potionAdj - 2)
            }
            crouch()
        } else if (gs.currTile2 == T.SWORD) {
            stubs.doPickup(-1)
            seqtblOffsetChar(Seq.seq_91_get_sword)
        } else {
            // potion
            stubs.doPickup(gs.currRoomModif[gs.currTilepos] shr 3)
            seqtblOffsetChar(Seq.seq_78_drink)
            // USE_COPYPROT: skipped (not in reference build)
        }
    }

    // ── control_jumpup ──

    /** seg005:0812 — Control during jump-up start: allow standing jump. */
    fun controlJumpup() {
        if (gs.controlX == Ctrl.HELD_FORWARD || gs.controlForward == Ctrl.HELD) {
            standingJump()
        }
    }

    // ══════════════════════════════════════════════════════════
    // Phase 10c: Sword fighting
    // ══════════════════════════════════════════════════════════

    // ── back_with_sword ──

    /** seg005:0BB5 — Step back with sword drawn. */
    fun backWithSword() {
        val frame = gs.Char.frame
        if (frame == FID.frame_158_stand_with_sword || frame == FID.frame_170_stand_with_sword || frame == FID.frame_171_stand_with_sword) {
            gs.controlBackward = Ctrl.IGNORE
            seqtblOffsetChar(Seq.seq_57_back_with_sword)
        }
    }

    // ── forward_with_sword ──

    /** seg005:0BE3 — Step forward with sword drawn. */
    fun forwardWithSword() {
        val frame = gs.Char.frame
        if (frame == FID.frame_158_stand_with_sword || frame == FID.frame_170_stand_with_sword || frame == FID.frame_171_stand_with_sword) {
            gs.controlForward = Ctrl.IGNORE
            if (gs.Char.charid != CID.KID) {
                seqtblOffsetChar(Seq.seq_56_guard_forward_with_sword)
            } else {
                seqtblOffsetChar(Seq.seq_86_forward_with_sword)
            }
        }
    }

    // ── draw_sword ──

    /** seg005:0C1D — Draw sword (Kid plays sound, Guard uses en_garde). */
    fun drawSword() {
        var seqId = Seq.seq_55_draw_sword
        gs.controlForward = seg006.releaseArrows()
        gs.controlShift2 = gs.controlForward
        // FIX_UNINTENDED_SWORD_STRIKE
        if (gs.fixes.fixUnintendedSwordStrike != 0) {
            gs.ctrl1Shift2 = Ctrl.IGNORE
        }
        if (gs.Char.charid == CID.KID) {
            stubs.playSound(Snd.DRAW_SWORD)
            gs.offguard = 0
        } else if (gs.Char.charid != CID.SHADOW) {
            seqId = Seq.seq_90_en_garde
        }
        gs.Char.sword = Sword.DRAWN
        seqtblOffsetChar(seqId)
    }

    // ── control_with_sword ──

    /** seg005:0C67 — Control dispatch when sword is drawn. */
    fun controlWithSword() {
        if (gs.Char.action < Act.HANG_CLIMB) {
            if (seg006.getTileAtChar() == T.LOOSE || gs.canGuardSeeKid.toInt() >= 2) {
                val distance = seg006.charOppDist()
                if ((distance and 0xFFFF) < (90 and 0xFFFF)) {
                    swordfight()
                    return
                } else if (distance < 0) {
                    if ((distance and 0xFFFF) < ((-4) and 0xFFFF)) {
                        seqtblOffsetChar(Seq.seq_60_turn_with_sword)
                        return
                    } else {
                        swordfight()
                        return
                    }
                }
            }
            // No opponent in range — sheathe or become inactive
            if (gs.Char.charid == CID.KID && gs.Char.alive < 0) {
                gs.holdingSword = 0
            }
            if (gs.Char.charid < CID.GUARD) {
                if (gs.Char.frame == FID.frame_171_stand_with_sword) {
                    gs.Char.sword = Sword.SHEATHED
                    seqtblOffsetChar(Seq.seq_92_put_sword_away)
                }
            } else {
                swordfight()
            }
        }
    }

    // ── swordfight ──

    /** seg005:0CDB — Main sword fighting control dispatch. */
    fun swordfight() {
        val frame = gs.Char.frame
        val charid = gs.Char.charid
        // frame 161: parry — if shift released, back with sword
        if (frame == FID.frame_161_parry && gs.controlShift2 >= Ctrl.RELEASED) {
            seqtblOffsetChar(Seq.seq_57_back_with_sword)
            return
        } else if (gs.controlShift2 == Ctrl.HELD) {
            if (charid == CID.KID) {
                gs.kidSwordStrike = 15
            }
            swordStrike()
            if (gs.controlShift2 == Ctrl.IGNORE) return
        }
        if (gs.controlDown == Ctrl.HELD) {
            if (frame == FID.frame_158_stand_with_sword || frame == FID.frame_170_stand_with_sword || frame == FID.frame_171_stand_with_sword) {
                gs.controlDown = Ctrl.IGNORE
                gs.Char.sword = Sword.SHEATHED
                val seqId: Int
                if (charid == CID.KID) {
                    gs.offguard = 1
                    gs.guardRefrac = 9
                    gs.holdingSword = 0
                    seqId = Seq.seq_93_put_sword_away_fast
                } else if (charid == CID.SHADOW) {
                    seqId = Seq.seq_92_put_sword_away
                } else {
                    seqId = Seq.seq_87_guard_become_inactive
                }
                seqtblOffsetChar(seqId)
            }
        } else if (gs.controlUp == Ctrl.HELD) {
            parry()
        } else if (gs.controlForward == Ctrl.HELD) {
            forwardWithSword()
        } else if (gs.controlBackward == Ctrl.HELD) {
            backWithSword()
        }
    }

    // ── sword_strike ──

    /** seg005:0DB0 — Attempt sword strike from eligible frames. */
    fun swordStrike() {
        val frame = gs.Char.frame
        val seqId: Int
        if (frame == FID.frame_157_walk_with_sword ||
            frame == FID.frame_158_stand_with_sword ||
            frame == FID.frame_170_stand_with_sword ||
            frame == FID.frame_171_stand_with_sword ||
            frame == FID.frame_165_walk_with_sword
        ) {
            seqId = if (gs.Char.charid == CID.KID) {
                Seq.seq_75_strike
            } else {
                Seq.seq_58_guard_strike
            }
        } else if (frame == FID.frame_150_parry || frame == FID.frame_161_parry) {
            seqId = Seq.seq_66_strike_after_parry
        } else {
            return
        }
        gs.controlShift2 = Ctrl.IGNORE
        seqtblOffsetChar(seqId)
    }

    // ── parry ──

    /** seg005:0E0F — Parry with sword: check opponent frame, play parry sequence. */
    fun parry() {
        val charFrame = gs.Char.frame
        val oppFrame = gs.Opp.frame
        val charCharid = gs.Char.charid
        var seqId = Seq.seq_62_parry
        var doPlaySeq = false
        if (charFrame == FID.frame_158_stand_with_sword ||
            charFrame == FID.frame_170_stand_with_sword ||
            charFrame == FID.frame_171_stand_with_sword ||
            charFrame == FID.frame_168_back ||
            charFrame == FID.frame_165_walk_with_sword
        ) {
            if (seg006.charOppDist() >= 32 && charCharid != CID.KID) {
                backWithSword()
                return
            } else if (charCharid == CID.KID) {
                if (oppFrame == FID.frame_168_back) return
                if (oppFrame != FID.frame_151_strike_1 &&
                    oppFrame != FID.frame_152_strike_2 &&
                    oppFrame != FID.frame_162_block_to_strike
                ) {
                    if (oppFrame == FID.frame_153_strike_3) {
                        doPlaySeq = true
                    } else if (charCharid != CID.KID) {
                        // Note: this branch is unreachable since we're inside charCharid == KID,
                        // but matches the C source structure exactly
                        backWithSword()
                        return
                    }
                }
            } else {
                if (oppFrame != FID.frame_152_strike_2) return
            }
        } else {
            if (charFrame != FID.frame_167_blocked) return
            seqId = Seq.seq_61_parry_after_strike
        }
        gs.controlUp = Ctrl.IGNORE
        seqtblOffsetChar(seqId)
        if (doPlaySeq) {
            seg006.playSeq()
        }
    }
}
