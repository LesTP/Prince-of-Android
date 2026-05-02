/*
SDLPoP-kotlin, a Kotlin port of SDLPoP (Prince of Persia).
Based on SDLPoP by Dávid Nagy, licensed under GPL v3+.

Module 6, Phase 6b: Game Enums
Translates enum constants from types.h used by Layer 1 game logic.

All enums use Int constants (not Kotlin enum classes) to match C semantics:
game logic uses these as raw integers in arithmetic and comparisons.
*/

package com.sdlpop.game

// tiles (0-30) — tile type IDs
object Tiles {
    const val EMPTY = 0
    const val FLOOR = 1
    const val SPIKE = 2
    const val PILLAR = 3
    const val GATE = 4
    const val STUCK = 5
    const val CLOSER = 6           // drop button
    const val DOORTOP_WITH_FLOOR = 7 // tapestry
    const val BIGPILLAR_BOTTOM = 8
    const val BIGPILLAR_TOP = 9
    const val POTION = 10
    const val LOOSE = 11
    const val DOORTOP = 12         // tapestry top
    const val MIRROR = 13
    const val DEBRIS = 14          // broken floor
    const val OPENER = 15          // raise button
    const val LEVEL_DOOR_LEFT = 16 // exit door
    const val LEVEL_DOOR_RIGHT = 17
    const val CHOMPER = 18
    const val TORCH = 19
    const val WALL = 20
    const val SKELETON = 21
    const val SWORD = 22
    const val BALCONY_LEFT = 23
    const val BALCONY_RIGHT = 24
    const val LATTICE_PILLAR = 25
    const val LATTICE_DOWN = 26    // lattice support
    const val LATTICE_SMALL = 27
    const val LATTICE_LEFT = 28
    const val LATTICE_RIGHT = 29
    const val TORCH_WITH_DEBRIS = 30
}

// charids — character type IDs
object CharIds {
    const val KID = 0
    const val SHADOW = 1
    const val GUARD = 2
    const val ID_3 = 3
    const val SKELETON = 4
    const val PRINCESS = 5
    const val VIZIER = 6
    const val MOUSE = 0x18
}

// sword_status
object SwordStatus {
    const val SHEATHED = 0
    const val DRAWN = 2
}

// directions
object Directions {
    const val RIGHT = 0x00
    const val NONE = 0x56
    const val LEFT = -1
}

// actions — action state IDs
object Actions {
    const val STAND = 0
    const val RUN_JUMP = 1
    const val HANG_CLIMB = 2
    const val IN_MIDAIR = 3
    const val IN_FREEFALL = 4
    const val BUMPED = 5
    const val HANG_STRAIGHT = 6
    const val TURN = 7
    const val HURT = 99
}

// frame_flags — frame flag bitmasks
object FrameFlags {
    const val WEIGHT_X = 0x1F
    const val THIN = 0x20
    const val NEEDS_FLOOR = 0x40
    const val EVEN_ODD_PIXEL = 0x80
}

// soundids — sound effect IDs (used by game logic for sound queueing)
object SoundIds {
    const val FELL_TO_DEATH = 0
    const val FALLING = 1
    const val TILE_CRASHING = 2
    const val BUTTON_PRESSED = 3
    const val GATE_CLOSING = 4
    const val GATE_OPENING = 5
    const val GATE_CLOSING_FAST = 6
    const val GATE_STOP = 7
    const val BUMPED = 8
    const val GRAB = 9
    const val SWORD_VS_SWORD = 10
    const val SWORD_MOVING = 11
    const val GUARD_HURT = 12
    const val KID_HURT = 13
    const val LEVELDOOR_CLOSING = 14
    const val LEVELDOOR_SLIDING = 15
    const val MEDIUM_LAND = 16
    const val SOFT_LAND = 17
    const val DRINK = 18
    const val DRAW_SWORD = 19
    const val LOOSE_SHAKE_1 = 20
    const val LOOSE_SHAKE_2 = 21
    const val LOOSE_SHAKE_3 = 22
    const val FOOTSTEP = 23
    const val DEATH_REGULAR = 24
    const val PRESENTATION = 25
    const val EMBRACE = 26
    const val CUTSCENE_2_4_6_12 = 27
    const val DEATH_IN_FIGHT = 28
    const val MEET_JAFFAR = 29
    const val BIG_POTION = 30
    const val SHADOW_MUSIC = 32
    const val SMALL_POTION = 33
    const val CUTSCENE_8_9 = 35
    const val OUT_OF_TIME = 36
    const val VICTORY = 37
    const val BLINK = 38
    const val LOW_WEIGHT = 39
    const val CUTSCENE_12_SHORT_TIME = 40
    const val END_LEVEL_MUSIC = 41
    const val VICTORY_JAFFAR = 43
    const val SKEL_ALIVE = 44
    const val JUMP_THROUGH_MIRROR = 45
    const val CHOMPED = 46
    const val CHOMPER = 47
    const val SPIKED = 48
    const val SPIKES = 49
    const val STORY_2_PRINCESS = 50
    const val PRINCESS_DOOR_OPENING = 51
    const val STORY_4_JAFFAR_LEAVES = 52
    const val STORY_3_JAFFAR_COMES = 53
    const val INTRO_MUSIC = 54
    const val STORY_1_ABSENCE = 55
    const val ENDING_MUSIC = 56
}

// soundflags — sound flag bitmasks
object SoundFlags {
    const val DIGI = 1
    const val MIDI = 2
    const val FLAG_4 = 4
    const val LOOP = 0x80
}

// chtabs — sprite sheet IDs
object Chtabs {
    const val SWORD = 0
    const val FLAMESWORDPOTION = 1
    const val KID = 2
    const val PRINCESSINSTORY = 3
    const val JAFFARINSTORY_PRINCESSINCUTSCENES = 4
    const val GUARD = 5
    const val ENVIRONMENT = 6
    const val ENVIRONMENTWALL = 7
    const val PRINCESSROOM = 8
    const val PRINCESSBED = 9
}

// blitters — drawing modes
object Blitters {
    const val NO_TRANSP = 0
    const val OR = 2
    const val XOR = 3          // used for shadow
    const val WHITE = 8
    const val BLACK = 9
    const val TRANSP = 0x10
    const val MONO = 0x40
    const val MONO_6 = 0x46    // palace wall patterns
    const val MONO_12 = 0x4C   // chomper blood
}

// Control input constants
object Control {
    const val RELEASED = 0
    const val IGNORE = 1
    const val HELD = -1
    const val HELD_LEFT = -1
    const val HELD_RIGHT = 1
    const val HELD_FORWARD = -1
    const val HELD_BACKWARD = 1
    const val HELD_UP = -1
    const val HELD_DOWN = 1
}

// Edge types (for tile edge detection)
object EdgeType {
    const val CLOSER = 0    // closer/sword/potion
    const val WALL = 1      // wall/gate/tapestry/mirror/chomper
    const val FLOOR = 2     // floor (nothing near char)
}

// Tile geometry constants
object TileGeometry {
    const val TILE_SIZEX = 14
    const val TILE_MIDX = 7
    const val TILE_RIGHTX = 13
    const val TILE_SIZEY = 63
    const val SCREENSPACE_X = 58
    const val SCREEN_TILECOUNTX = 10
    const val SCREEN_TILECOUNTY = 3
    const val FIRST_ONSCREEN_COLUMN = 5
}

// Falling constants
object Falling {
    const val SPEED_MAX = 33
    const val SPEED_ACCEL = 3
    const val SPEED_MAX_FEATHER = 4
    const val SPEED_ACCEL_FEATHER = 1
}

// General constants
object GameConstants {
    const val ROOMCOUNT = 24
    const val TROBS_MAX = 30
    const val BASE_FPS = 60
    const val NUM_GUARD_SKILLS = 12
}

// Sequence table instruction opcodes (0xF1-0xFF range)
object SeqtblInstructions {
    const val SEQ_END_LEVEL = 0xF1
    const val SEQ_SOUND = 0xF2
    const val SEQ_GET_ITEM = 0xF3
    const val SEQ_KNOCK_DOWN = 0xF4
    const val SEQ_KNOCK_UP = 0xF5
    const val SEQ_DIE = 0xF6
    const val SEQ_JMP_IF_FEATHER = 0xF7
    const val SEQ_SET_FALL = 0xF8
    const val SEQ_ACTION = 0xF9
    const val SEQ_DY = 0xFA
    const val SEQ_DX = 0xFB
    const val SEQ_DOWN = 0xFC
    const val SEQ_UP = 0xFD
    const val SEQ_FLIP = 0xFE
    const val SEQ_JMP = 0xFF
}

// Sequence table sound IDs (used by snd() instruction)
object SeqtblSounds {
    const val SND_SILENT = 0
    const val SND_FOOTSTEP = 1
    const val SND_BUMP = 2
    const val SND_DRINK = 3
    const val SND_LEVEL = 4
}

// Frame IDs — animation frame numbers (from types.h enum frameids)
object FrameIds {
    const val frame_0 = 0
    const val frame_1_start_run = 1
    const val frame_2_start_run = 2
    const val frame_3_start_run = 3
    const val frame_4_start_run = 4
    const val frame_5_start_run = 5
    const val frame_6_start_run = 6
    const val frame_7_run = 7
    const val frame_8_run = 8
    const val frame_9_run = 9
    const val frame_10_run = 10
    const val frame_11_run = 11
    const val frame_12_run = 12
    const val frame_13_run = 13
    const val frame_14_run = 14
    const val frame_15_stand = 15
    const val frame_16_standing_jump_1 = 16
    const val frame_17_standing_jump_2 = 17
    const val frame_18_standing_jump_3 = 18
    const val frame_19_standing_jump_4 = 19
    const val frame_20_standing_jump_5 = 20
    const val frame_21_standing_jump_6 = 21
    const val frame_22_standing_jump_7 = 22
    const val frame_23_standing_jump_8 = 23
    const val frame_24_standing_jump_9 = 24
    const val frame_25_standing_jump_10 = 25
    const val frame_26_standing_jump_11 = 26
    const val frame_27_standing_jump_12 = 27
    const val frame_28_standing_jump_13 = 28
    const val frame_29_standing_jump_14 = 29
    const val frame_30_standing_jump_15 = 30
    const val frame_31_standing_jump_16 = 31
    const val frame_32_standing_jump_17 = 32
    const val frame_33_standing_jump_18 = 33
    const val frame_34_start_run_jump_1 = 34
    const val frame_35_start_run_jump_2 = 35
    const val frame_36_start_run_jump_3 = 36
    const val frame_37_start_run_jump_4 = 37
    const val frame_38_start_run_jump_5 = 38
    const val frame_39_start_run_jump_6 = 39
    const val frame_40_running_jump_1 = 40
    const val frame_41_running_jump_2 = 41
    const val frame_42_running_jump_3 = 42
    const val frame_43_running_jump_4 = 43
    const val frame_44_running_jump_5 = 44
    const val frame_45_turn = 45
    const val frame_46_turn = 46
    const val frame_47_turn = 47
    const val frame_48_turn = 48
    const val frame_49_turn = 49
    const val frame_50_turn = 50
    const val frame_51_turn = 51
    const val frame_52_turn = 52
    const val frame_53_runturn = 53
    const val frame_54_runturn = 54
    const val frame_55_runturn = 55
    const val frame_56_runturn = 56
    const val frame_57_runturn = 57
    const val frame_58_runturn = 58
    const val frame_59_runturn = 59
    const val frame_60_runturn = 60
    const val frame_61_runturn = 61
    const val frame_62_runturn = 62
    const val frame_63_runturn = 63
    const val frame_64_runturn = 64
    const val frame_65_runturn = 65
    const val frame_67_start_jump_up_1 = 67
    const val frame_68_start_jump_up_2 = 68
    const val frame_69_start_jump_up_3 = 69
    const val frame_70_jumphang = 70
    const val frame_71_jumphang = 71
    const val frame_72_jumphang = 72
    const val frame_73_jumphang = 73
    const val frame_74_jumphang = 74
    const val frame_75_jumphang = 75
    const val frame_76_jumphang = 76
    const val frame_77_jumphang = 77
    const val frame_78_jumphang = 78
    const val frame_79_jumphang = 79
    const val frame_80_jumphang = 80
    const val frame_81_hangdrop_1 = 81
    const val frame_82_hangdrop_2 = 82
    const val frame_83_hangdrop_3 = 83
    const val frame_84_hangdrop_4 = 84
    const val frame_85_hangdrop_5 = 85
    const val frame_86_test_foot = 86
    const val frame_87_hanging_1 = 87
    const val frame_88_hanging_2 = 88
    const val frame_89_hanging_3 = 89
    const val frame_90_hanging_4 = 90
    const val frame_91_hanging_5 = 91
    const val frame_92_hanging_6 = 92
    const val frame_93_hanging_7 = 93
    const val frame_94_hanging_8 = 94
    const val frame_95_hanging_9 = 95
    const val frame_96_hanging_10 = 96
    const val frame_97_hanging_11 = 97
    const val frame_98_hanging_12 = 98
    const val frame_99_hanging_13 = 99
    const val frame_102_start_fall_1 = 102
    const val frame_103_start_fall_2 = 103
    const val frame_104_start_fall_3 = 104
    const val frame_105_start_fall_4 = 105
    const val frame_106_fall = 106
    const val frame_107_fall_land_1 = 107
    const val frame_108_fall_land_2 = 108
    const val frame_109_crouch = 109
    const val frame_110_stand_up_from_crouch_1 = 110
    const val frame_111_stand_up_from_crouch_2 = 111
    const val frame_112_stand_up_from_crouch_3 = 112
    const val frame_113_stand_up_from_crouch_4 = 113
    const val frame_114_stand_up_from_crouch_5 = 114
    const val frame_115_stand_up_from_crouch_6 = 115
    const val frame_116_stand_up_from_crouch_7 = 116
    const val frame_117_stand_up_from_crouch_8 = 117
    const val frame_118_stand_up_from_crouch_9 = 118
    const val frame_119_stand_up_from_crouch_10 = 119
    const val frame_121_stepping_1 = 121
    const val frame_122_stepping_2 = 122
    const val frame_123_stepping_3 = 123
    const val frame_124_stepping_4 = 124
    const val frame_125_stepping_5 = 125
    const val frame_126_stepping_6 = 126
    const val frame_127_stepping_7 = 127
    const val frame_128_stepping_8 = 128
    const val frame_129_stepping_9 = 129
    const val frame_130_stepping_10 = 130
    const val frame_131_stepping_11 = 131
    const val frame_132_stepping_12 = 132
    const val frame_133_sheathe = 133
    const val frame_134_sheathe = 134
    const val frame_135_climbing_1 = 135
    const val frame_136_climbing_2 = 136
    const val frame_137_climbing_3 = 137
    const val frame_138_climbing_4 = 138
    const val frame_139_climbing_5 = 139
    const val frame_140_climbing_6 = 140
    const val frame_141_climbing_7 = 141
    const val frame_142_climbing_8 = 142
    const val frame_143_climbing_9 = 143
    const val frame_144_climbing_10 = 144
    const val frame_145_climbing_11 = 145
    const val frame_146_climbing_12 = 146
    const val frame_147_climbing_13 = 147
    const val frame_148_climbing_14 = 148
    const val frame_149_climbing_15 = 149
    const val frame_150_parry = 150
    const val frame_151_strike_1 = 151
    const val frame_152_strike_2 = 152
    const val frame_153_strike_3 = 153
    const val frame_154_poking = 154
    const val frame_155_guy_7 = 155
    const val frame_156_guy_8 = 156
    const val frame_157_walk_with_sword = 157
    const val frame_158_stand_with_sword = 158
    const val frame_159_fighting = 159
    const val frame_160_fighting = 160
    const val frame_161_parry = 161
    const val frame_162_block_to_strike = 162
    const val frame_163_fighting = 163
    const val frame_164_fighting = 164
    const val frame_165_walk_with_sword = 165
    const val frame_166_stand_inactive = 166
    const val frame_167_blocked = 167
    const val frame_168_back = 168
    const val frame_169_begin_block = 169
    const val frame_170_stand_with_sword = 170
    const val frame_171_stand_with_sword = 171
    const val frame_172_jumpfall_2 = 172
    const val frame_173_jumpfall_3 = 173
    const val frame_174_jumpfall_4 = 174
    const val frame_175_jumpfall_5 = 175
    const val frame_177_spiked = 177
    const val frame_178_chomped = 178
    const val frame_179_collapse_1 = 179
    const val frame_180_collapse_2 = 180
    const val frame_181_collapse_3 = 181
    const val frame_182_collapse_4 = 182
    const val frame_183_collapse_5 = 183
    const val frame_185_dead = 185
    const val frame_186_mouse_1 = 186
    const val frame_187_mouse_2 = 187
    const val frame_188_mouse_stand = 188
    const val frame_191_drink = 191
    const val frame_192_drink = 192
    const val frame_193_drink = 193
    const val frame_194_drink = 194
    const val frame_195_drink = 195
    const val frame_196_drink = 196
    const val frame_197_drink = 197
    const val frame_198_drink = 198
    const val frame_199_drink = 199
    const val frame_200_drink = 200
    const val frame_201_drink = 201
    const val frame_202_drink = 202
    const val frame_203_drink = 203
    const val frame_204_drink = 204
    const val frame_205_drink = 205
    const val frame_207_draw_1 = 207
    const val frame_208_draw_2 = 208
    const val frame_209_draw_3 = 209
    const val frame_210_draw_4 = 210
    const val frame_217_exit_stairs_1 = 217
    const val frame_218_exit_stairs_2 = 218
    const val frame_219_exit_stairs_3 = 219
    const val frame_220_exit_stairs_4 = 220
    const val frame_221_exit_stairs_5 = 221
    const val frame_222_exit_stairs_6 = 222
    const val frame_223_exit_stairs_7 = 223
    const val frame_224_exit_stairs_8 = 224
    const val frame_225_exit_stairs_9 = 225
    const val frame_226_exit_stairs_10 = 226
    const val frame_227_exit_stairs_11 = 227
    const val frame_228_exit_stairs_12 = 228
    const val frame_229_found_sword = 229
    const val frame_230_sheathe = 230
    const val frame_231_sheathe = 231
    const val frame_232_sheathe = 232
    const val frame_233_sheathe = 233
    const val frame_234_sheathe = 234
    const val frame_235_sheathe = 235
    const val frame_236_sheathe = 236
    const val frame_237_sheathe = 237
    const val frame_238_sheathe = 238
    const val frame_239_sheathe = 239
    const val frame_240_sheathe = 240
}

// Color IDs (from types.h enum colors)
object Colors {
    const val RED = 4
    const val BRIGHTYELLOW = 14
    const val BRIGHTWHITE = 15
}

// Sequence IDs — animation sequence identifiers (from types.h enum seqids)
object SeqIds {
    const val seq_1_start_run = 1
    const val seq_2_stand = 2
    const val seq_3_standing_jump = 3
    const val seq_4_run_jump = 4
    const val seq_5_turn = 5
    const val seq_6_run_turn = 6
    const val seq_7_fall = 7
    const val seq_8_jump_up_and_grab_straight = 8
    const val seq_9_grab_while_jumping = 9
    const val seq_10_climb_up = 10
    const val seq_11_release_ledge_and_land = 11
    const val seq_13_stop_run = 13
    const val seq_14_jump_up_into_ceiling = 14
    const val seq_15_grab_ledge_midair = 15
    const val seq_16_jump_up_and_grab = 16
    const val seq_17_soft_land = 17
    const val seq_18_fall_after_standing_jump = 18
    const val seq_19_fall = 19
    const val seq_20_medium_land = 20
    const val seq_21_fall_after_running_jump = 21
    const val seq_22_crushed = 22
    const val seq_23_release_ledge_and_fall = 23
    const val seq_24_jump_up_and_grab_forward = 24
    const val seq_25_hang_against_wall = 25
    const val seq_26_crouch_while_running = 26
    const val seq_28_jump_up_with_nothing_above = 28
    const val seq_29_safe_step_1 = 29
    const val seq_30_safe_step_2 = 30
    const val seq_31_safe_step_3 = 31
    const val seq_32_safe_step_4 = 32
    const val seq_33_safe_step_5 = 33
    const val seq_34_safe_step_6 = 34
    const val seq_35_safe_step_7 = 35
    const val seq_36_safe_step_8 = 36
    const val seq_37_safe_step_9 = 37
    const val seq_38_safe_step_10 = 38
    const val seq_39_safe_step_11 = 39
    const val seq_40_safe_step_12 = 40
    const val seq_41_safe_step_13 = 41
    const val seq_42_safe_step_14 = 42
    const val seq_43_start_run_after_turn = 43
    const val seq_44_step_on_edge = 44
    const val seq_45_bumpfall = 45
    const val seq_46_hardbump = 46
    const val seq_47_bump = 47
    const val seq_48_super_high_jump = 48
    const val seq_49_stand_up_from_crouch = 49
    const val seq_50_crouch = 50
    const val seq_51_spiked = 51
    const val seq_52_loose_floor_fell_on_kid = 52
    const val seq_54_chomped = 54
    const val seq_55_draw_sword = 55
    const val seq_56_guard_forward_with_sword = 56
    const val seq_57_back_with_sword = 57
    const val seq_58_guard_strike = 58
    const val seq_60_turn_with_sword = 60
    const val seq_61_parry_after_strike = 61
    const val seq_62_parry = 62
    const val seq_63_guard_active_after_fall = 63
    const val seq_64_pushed_back_with_sword = 64
    const val seq_65_bump_forward_with_sword = 65
    const val seq_66_strike_after_parry = 66
    const val seq_68_climb_down = 68
    const val seq_69_attack_was_parried = 69
    const val seq_70_go_up_on_level_door = 70
    const val seq_71_dying = 71
    const val seq_73_climb_up_to_closed_gate = 73
    const val seq_74_hit_by_sword = 74
    const val seq_75_strike = 75
    const val seq_77_guard_stand_inactive = 77
    const val seq_78_drink = 78
    const val seq_79_crouch_hop = 79
    const val seq_80_stand_flipped = 80
    const val seq_81_kid_pushed_off_ledge = 81
    const val seq_82_guard_pushed_off_ledge = 82
    const val seq_83_guard_fall = 83
    const val seq_84_run = 84
    const val seq_85_stabbed_to_death = 85
    const val seq_86_forward_with_sword = 86
    const val seq_87_guard_become_inactive = 87
    const val seq_88_skel_wake_up = 88
    const val seq_89_turn_draw_sword = 89
    const val seq_90_en_garde = 90
    const val seq_91_get_sword = 91
    const val seq_92_put_sword_away = 92
    const val seq_93_put_sword_away_fast = 93
    const val seq_94_princess_stand_PV1 = 94
    const val seq_95_Jaffar_stand_PV1 = 95
    const val seq_101_mouse_stands_up = 101
    const val seq_103_princess_lying_PV2 = 103
    const val seq_104_start_fall_in_front_of_wall = 104
    const val seq_105_mouse_forward = 105
    const val seq_106_mouse = 106
    const val seq_107_mouse_stand_up_and_go = 107
    const val seq_108_princess_turn_and_hug = 108
    const val seq_109_princess_stand_PV2 = 109
    const val seq_110_princess_crouching_PV2 = 110
    const val seq_111_princess_stand_up_PV2 = 111
    const val seq_112_princess_crouch_down_PV2 = 112
    const val seq_114_mouse_stand = 114
    const val seq_teleport = 115
}
