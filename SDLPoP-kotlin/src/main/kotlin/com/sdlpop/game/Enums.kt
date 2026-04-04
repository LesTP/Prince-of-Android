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
