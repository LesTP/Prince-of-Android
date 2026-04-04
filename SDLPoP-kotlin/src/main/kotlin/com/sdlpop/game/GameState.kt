/*
SDLPoP-kotlin, a Kotlin port of SDLPoP (Prince of Persia).
Based on SDLPoP by Dávid Nagy, licensed under GPL v3+.

Module 6, Phase 6a/6c: Global State Object
Translates ~344 global variables from data.h into a Kotlin singleton.
Only variables used by Layer 1 game logic (seg002/004/005/006/007) are included.
SDL/platform types (surfaces, textures, renderers, joystick, font, dialog) are excluded.
*/

package com.sdlpop.game

/**
 * Mutable game state — Kotlin equivalent of data.h global variables.
 *
 * All game logic functions read and write these properties directly,
 * matching the C codebase's global variable pattern. This is intentional —
 * the original game has no encapsulation, and preserving the same access
 * pattern ensures the replay oracle can validate behavior.
 */
object GameState {
    // === Character state ===
    var Kid = CharType()
    var Guard = CharType()
    var Char = CharType()    // Currently-processed character
    var Opp = CharType()     // Opponent of currently-processed character

    // === Level and room state ===
    var level = LevelType()
    var currentLevel: Int = -1     // word (current_level), init -1 like C
    var drawnRoom: Int = 0         // word (drawn_room)
    var loadedRoom: Int = 0        // word (loaded_room)
    var nextRoom: Int = 0          // word (next_room)
    var nextLevel: Int = 0         // word (next_level)
    var currRoom: Short = 0        // short (curr_room)
    var differentRoom: Int = 0     // word (different_room)

    // Room neighbors
    var roomL: Int = 0             // word (room_L)
    var roomR: Int = 0             // word (room_R)
    var roomA: Int = 0             // word (room_A) — above
    var roomB: Int = 0             // word (room_B) — below
    var roomBR: Int = 0            // word (room_BR)
    var roomBL: Int = 0            // word (room_BL)
    var roomAR: Int = 0            // word (room_AR)
    var roomAL: Int = 0            // word (room_AL)

    // Current room tile data (loaded for drawn_room)
    var currRoomTiles = IntArray(30)   // byte* (curr_room_tiles) — tile types
    var currRoomModif = IntArray(30)   // byte* (curr_room_modif) — tile modifiers

    // Tile query temporaries
    var currTile: Int = 0          // byte (curr_tile)
    var currModifier: Int = 0      // byte (curr_modifier)
    var currTile2: Int = 0         // byte (curr_tile2)
    var currTilepos: Int = 0       // byte (curr_tilepos)
    var tileCol: Short = 0         // short (tile_col)
    var tileRow: Short = 0         // short (tile_row)
    var drawXh: Int = 0            // word (draw_xh)

    // Tile lookup arrays
    val leftroom = Array(3) { TileAndMod() }       // leftroom_[3]
    val rowBelowLeft = Array(10) { TileAndMod() }  // row_below_left_[10]

    // === Hit points ===
    var hitpCurr: Int = 0          // word (hitp_curr)
    var hitpMax: Int = 0           // word (hitp_max)
    var hitpDelta: Short = 0       // short (hitp_delta)
    var hitpBegLev: Int = 0        // word (hitp_beg_lev)
    var guardhpCurr: Int = 0       // word (guardhp_curr)
    var guardhpMax: Int = 0        // word (guardhp_max)
    var guardhpDelta: Short = 0    // short (guardhp_delta)

    // === Timer ===
    var remMin: Short = 0          // short (rem_min)
    var remTick: Int = 0           // word (rem_tick)

    // === RNG ===
    var randomSeed: Long = 0       // dword (random_seed)
    var seedWasInit: Int = 0       // word (seed_was_init), init 0

    // === Control inputs ===
    var controlShift: Int = 0      // sbyte (control_shift)
    var controlY: Int = 0          // sbyte (control_y)
    var controlX: Int = 0          // sbyte (control_x)
    var controlShift2: Int = 0     // sbyte (control_shift2)
    var controlForward: Int = 0    // sbyte (control_forward)
    var controlBackward: Int = 0   // sbyte (control_backward)
    var controlUp: Int = 0         // sbyte (control_up)
    var controlDown: Int = 0       // sbyte (control_down)
    var ctrl1Forward: Int = 0      // sbyte (ctrl1_forward)
    var ctrl1Backward: Int = 0     // sbyte (ctrl1_backward)
    var ctrl1Up: Int = 0           // sbyte (ctrl1_up)
    var ctrl1Down: Int = 0         // sbyte (ctrl1_down)
    var ctrl1Shift2: Int = 0       // sbyte (ctrl1_shift2)

    // === Trobs (animated tiles) ===
    val trob = TrobType()          // Current trob being processed
    val trobs = Array(GameConstants.TROBS_MAX) { TrobType() }
    var trobsCount: Short = 0      // short (trobs_count)

    // === Mobs (movable objects — loose floors) ===
    val curmob = MobType()         // Current mob being processed
    val mobs = Array(14) { MobType() }
    var mobsCount: Short = 0       // short (mobs_count)

    // === Guard state ===
    var guardSkill: Int = 0        // word (guard_skill)
    var guardRefrac: Int = 0       // word (guard_refrac)
    var currGuardColor: Int = 0    // word (curr_guard_color)
    var canGuardSeeKid: Short = 0  // short (can_guard_see_kid)
    var isGuardNotice: Int = 0     // word (is_guard_notice)
    var guardNoticeTimer: Short = 0  // short (guard_notice_timer)

    // === Sword/combat state ===
    var holdingSword: Int = 0      // word (holding_sword)
    var haveSword: Int = 0         // word (have_sword)
    var kidSwordStrike: Int = 0    // word (kid_sword_strike)
    var knock: Short = 0           // short (knock)
    var justblocked: Int = 0       // word (justblocked)
    var offguard: Int = 0          // word (offguard)

    // === Sound ===
    var currentSound: Int = 0      // word (current_sound)
    var nextSound: Short = 0       // short (next_sound)
    var isScreaming: Int = 0       // word (is_screaming)
    var lastLooseSound: Int = 0    // word (last_loose_sound)

    // === Character position/collision tracking ===
    var charColRight: Short = 0    // short (char_col_right)
    var charColLeft: Short = 0     // short (char_col_left)
    var charTopRow: Short = 0      // short (char_top_row)
    var charBottomRow: Short = 0   // short (char_bottom_row)
    var prevCharTopRow: Short = 0  // short (prev_char_top_row)
    var prevCharColRight: Short = 0  // short (prev_char_col_right)
    var prevCharColLeft: Short = 0   // short (prev_char_col_left)
    var charWidthHalf: Int = 0     // word (char_width_half)
    var charHeight: Int = 0        // word (char_height)
    var charXLeft: Short = 0       // short (char_x_left)
    var charXLeftColl: Short = 0   // short (char_x_left_coll)
    var charXRightColl: Short = 0  // short (char_x_right_coll)
    var charXRight: Short = 0      // short (char_x_right)
    var charTopY: Short = 0        // short (char_top_y)

    // Collision room arrays
    val prevCollRoom = IntArray(10)        // sbyte[10] (prev_coll_room)
    val currRowCollRoom = IntArray(10)     // sbyte[10] (curr_row_coll_room)
    val belowRowCollRoom = IntArray(10)    // sbyte[10] (below_row_coll_room)
    val aboveRowCollRoom = IntArray(10)    // sbyte[10] (above_row_coll_room)
    val currRowCollFlags = IntArray(10)    // byte[10] (curr_row_coll_flags)
    val aboveRowCollFlags = IntArray(10)   // byte[10] (above_row_coll_flags)
    val belowRowCollFlags = IntArray(10)   // byte[10] (below_row_coll_flags)
    val prevCollFlags = IntArray(10)       // byte[10] (prev_coll_flags)
    var collisionRow: Int = 0              // sbyte (collision_row)
    var prevCollisionRow: Int = 0          // sbyte (prev_collision_row)

    // === Falling ===
    var fallFrame: Int = 0         // byte (fall_frame)
    var throughTile: Int = 0       // byte (through_tile)
    var infrontx: Int = 0          // sbyte (infrontx)

    // === Frame/animation ===
    var curFrame = FrameType()     // frame_type (cur_frame)
    var seamless: Int = 0          // word (seamless)

    // === Room rendering/redraw state ===
    val wipeFrames = IntArray(30)              // byte[30] (wipe_frames)
    val wipeHeights = IntArray(30)             // sbyte[30] (wipe_heights)
    val redrawFramesAnim = IntArray(30)        // byte[30] (redraw_frames_anim)
    val redrawFrames2 = IntArray(30)           // byte[30] (redraw_frames2)
    val redrawFramesFloorOverlay = IntArray(30) // byte[30] (redraw_frames_floor_overlay)
    val redrawFramesFull = IntArray(30)        // byte[30] (redraw_frames_full)
    val redrawFramesFore = IntArray(30)        // byte[30] (redraw_frames_fore)
    val tileObjectRedraw = IntArray(30)        // byte[30] (tile_object_redraw)
    val redrawFramesAbove = IntArray(10)       // byte[10] (redraw_frames_above)
    var needFullRedraw: Int = 0                // word (need_full_redraw)
    var redrawHeight: Short = 0                // short (redraw_height)
    var needDrects: Int = 0                    // word (need_drects)

    // === Object table ===
    var nCurrObjs: Short = 0                   // short (n_curr_objs)
    val objtable = Array(50) { ObjtableType() }
    val currObjs = ShortArray(50)              // short[50] (curr_objs)

    // Object temporaries
    var objXh: Int = 0             // byte (obj_xh)
    var objXl: Int = 0             // byte (obj_xl)
    var objY: Int = 0              // byte (obj_y)
    var objChtab: Int = 0          // byte (obj_chtab)
    var objId: Int = 0             // byte (obj_id)
    var objTilepos: Int = 0        // byte (obj_tilepos)
    var objX: Short = 0            // short (obj_x)
    var objDirection: Int = 0      // sbyte (obj_direction)
    var objClipLeft: Short = 0     // short (obj_clip_left)
    var objClipTop: Short = 0      // short (obj_clip_top)
    var objClipRight: Short = 0    // short (obj_clip_right)
    var objClipBottom: Short = 0   // short (obj_clip_bottom)

    // === Door/exit state ===
    var leveldoorRight: Int = 0    // word (leveldoor_right)
    var leveldoorYbottom: Int = 0  // word (leveldoor_ybottom)
    var leveldoorOpen: Int = 0     // word (leveldoor_open)

    // Door link pointers (indices into level doorlinks arrays)
    var doorlink1Ad: Int = 0       // byte* → index
    var doorlink2Ad: Int = 0       // byte* → index

    // === Game flow ===
    var isShowTime: Int = 0        // word (is_show_time)
    var checkpoint: Int = 0        // word (checkpoint)
    var upsideDown: Int = 0        // word (upside_down)
    var resurrectTime: Int = 0     // word (resurrect_time)
    var dontResetTime: Int = 0     // word (dont_reset_time)
    var textTimeRemaining: Int = 0 // word (text_time_remaining)
    var textTimeTotal: Int = 0     // word (text_time_total)
    var needLevel1Music: Int = 0   // word (need_level1_music)
    var startLevel: Short = -1     // short (start_level), init -1
    var isCutscene: Int = 0        // word (is_cutscene)
    var isEndingSequence: Boolean = false  // bool (is_ending_sequence)

    // === Miscellaneous game logic state ===
    var flashColor: Int = 0        // word (flash_color)
    var flashTime: Int = 0         // word (flash_time)
    var roomleaveResult: Short = 0 // short (roomleave_result)
    var isFeatherFall: Int = 0     // word (is_feather_fall)
    var unitedWithShadow: Short = 0  // short (united_with_shadow)
    var shadowInitialized: Int = 0   // word (shadow_initialized)
    var jumpedThroughMirror: Short = 0  // short (jumped_through_mirror)
    var droppedout: Int = 0        // word (droppedout)
    var exitRoomTimer: Int = 0     // word (exit_room_timer)
    var pickupObjType: Short = 0   // short (pickup_obj_type)
    var grabTimer: Int = 0         // word (grab_timer)
    var edgeType: Int = 0          // byte (edge_type)
    var isBlindMode: Int = 0       // word (is_blind_mode)
    var isPaused: Int = 0          // word (is_paused)
    var isRestartLevel: Int = 0    // word (is_restart_level)
    var demoMode: Int = 0          // word (demo_mode), init 0
    var demoIndex: Int = 0         // word (demo_index)
    var demoTime: Short = 0        // short (demo_time)
    var playDemoLevel: Int = 0     // int (play_demo_level)
    var needQuotes: Int = 0        // word (need_quotes)
    var drawMode: Int = 0          // word (draw_mode)
    var graphicsMode: Int = 0      // byte (graphics_mode), init 0
    var soundFlags: Int = 0        // byte (sound_flags), init 0
    var superJumpFall: Int = 0     // byte (super_jump_fall), init 0
    var isKeyboardMode: Int = 0    // word (is_keyboard_mode), init 0
    var isJoystMode: Int = 0       // word (is_joyst_mode)
    var soundMode: Int = 0         // byte (sound_mode), init 0
    var isSoundOn: Int = 0x0F      // byte (is_sound_on), init 0x0F
    var cheatsEnabled: Int = 0     // word (cheats_enabled), init 0

    // === Replay state ===
    var recording: Int = 0         // byte
    var replaying: Int = 0         // byte
    var numReplayTicks: Long = 0   // dword (num_replay_ticks)
    var specialMove: Int = 0       // byte (special_move)
    var savedRandomSeed: Long = 0  // dword (saved_random_seed)
    var preservedSeed: Long = 0    // dword (preserved_seed)
    var keepLastSeed: Int = 0      // sbyte (keep_last_seed)
    var skippingReplay: Int = 0    // byte (skipping_replay)
    var replaySeekTarget: Int = 0  // byte (replay_seek_target)
    var isValidateMode: Int = 0    // byte (is_validate_mode)
    var currTick: Long = 0         // dword (curr_tick)

    // === Configuration ===
    var useFixesAndEnhancements: Int = 0  // byte
    var useCustomOptions: Int = 0         // byte (use_custom_options)
    val fixesDisabledState = FixesOptionsType()
    val fixesSaved = FixesOptionsType()
    var fixes = fixesDisabledState         // pointer equivalent
    val customDefaults = CustomOptionsType()
    val customSaved = CustomOptionsType()
    var custom = customDefaults            // pointer equivalent

    // === Object save/restore (obj2_*) — used by save_obj/load_obj ===
    var obj2Tilepos: Int = 0       // byte (obj2_tilepos)
    var obj2X: Short = 0           // word (obj2_x) — stored as Short matching obj_x
    var obj2Y: Int = 0             // byte (obj2_y)
    var obj2Direction: Int = 0     // sbyte (obj2_direction)
    var obj2Id: Int = 0            // byte (obj2_id)
    var obj2Chtab: Int = 0         // byte (obj2_chtab)
    var obj2ClipTop: Short = 0     // short (obj2_clip_top)
    var obj2ClipBottom: Short = 0  // short (obj2_clip_bottom)
    var obj2ClipLeft: Short = 0    // short (obj2_clip_left)
    var obj2ClipRight: Short = 0   // short (obj2_clip_right)

    // === Rendering table counts ===
    val tableCounts = ShortArray(5)       // short[5] (table_counts)
    // backtable_count = tableCounts[0], foretable_count = tableCounts[1],
    // wipetable_count = tableCounts[2], midtable_count = tableCounts[3],
    // objtable_count = tableCounts[4]
    var drectsCount: Short = 0            // short (drects_count)
    var peelsCount: Short = 0             // short (peels_count)

    // === Constant arrays (read-only game data) ===

    // Y-positions for floor rows (-1=above screen, 0-2=visible rows, 3=below screen)
    val yLand = shortArrayOf(-8, 55, 118, 181, 244)

    // X-positions for tile columns (includes 5 off-screen left and 5 off-screen right)
    val xBump = intArrayOf(
        -12, 2, 16, 30, 44, 58, 72, 86, 100, 114,
        128, 142, 156, 170, 184, 198, 212, 226, 240, 254
    )

    // Tile row start indices
    val tblLine = intArrayOf(0, 10, 20)

    // Direction lookup: dir_front[0]=left(-1), dir_front[1]=right(1)
    // direction==0 means right, direction==-1 (0xFF) means left
    // dir_front maps direction index to front direction offset
    val dirFront = intArrayOf(-1, 1)
    val dirBehind = intArrayOf(1, -1)

    // Y clipping boundaries for rows
    val yClip = shortArrayOf(-60, 3, 66, 129, 192)

    // Sound interruptibility flags (indexed by sound ID)
    val soundInterruptible = intArrayOf(
        0, // sound_0_fell_to_death
        1, // sound_1_falling
        1, // sound_2_tile_crashing
        1, // sound_3_button_pressed
        1, // sound_4_gate_closing
        1, // sound_5_gate_opening
        0, // sound_6_gate_closing_fast
        1, // sound_7_gate_stop
        1, // sound_8_bumped
        1, // sound_9_grab
        1, // sound_10_sword_vs_sword
        1, // sound_11_sword_moving
        1, // sound_12_guard_hurt
        1, // sound_13_kid_hurt
        0, // sound_14_leveldoor_closing
        0, // sound_15_leveldoor_sliding
        1, // sound_16_medium_land
        1, // sound_17_soft_land
        0, // sound_18_drink
        1, // sound_19_draw_sword
        1, // sound_20_loose_shake_1
        1, // sound_21_loose_shake_2
        1, // sound_22_loose_shake_3
        1, // sound_23_footstep
        0, // sound_24_death_regular
        0, // sound_25_presentation
        0, // sound_26_embrace
        0, // sound_27_cutscene_2_4_6_12
        0, // sound_28_death_in_fight
        1, // sound_29_meet_Jaffar
        0, // sound_30_big_potion
        0, // sound_31
        0, // sound_32_shadow_music
        0, // sound_33_small_potion
        0, // sound_34
        0, // sound_35_cutscene_8_9
        0, // sound_36_out_of_time
        0, // sound_37_victory
        0, // sound_38_blink
        0, // sound_39_low_weight
        0, // sound_40_cutscene_12_short_time
        0, // sound_41_end_level_music
        0, // sound_42
        0, // sound_43_victory_Jaffar
        0, // sound_44_skel_alive
        0, // sound_45_jump_through_mirror
        0, // sound_46_chomped
        1, // sound_47_chomper
        0, // sound_48_spiked
        0, // sound_49_spikes
        0, // sound_50_story_2_princess
        0, // sound_51_princess_door_opening
        0, // sound_52_story_4_Jaffar_leaves
        0, // sound_53_story_3_Jaffar_comes
        0, // sound_54_intro_music
        0, // sound_55_story_1_absence
        0, // sound_56_ending_music
        0  // (sentinel)
    )

    // Chtab flip/shift tables (rendering helpers, but referenced by game logic)
    val chtabFlipClip = intArrayOf(1, 0, 1, 1, 1, 1, 0, 0, 0, 0)
    val chtabShift = intArrayOf(0, 1, 0, 0, 0, 0, 1, 1, 1, 0)

    // Palace wall colors: 44 entries × 3 bytes (RGB)
    val palaceWallColors = IntArray(44 * 3)
}
