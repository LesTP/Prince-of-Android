/*
SDLPoP-kotlin, a Kotlin port of SDLPoP (Prince of Persia).
Based on SDLPoP by Dávid Nagy, licensed under GPL v3+.

Module 6, Phase 6c: Custom options and fixes configuration types.
Translated from custom_options_type and fixes_options_type in types.h.
*/

package com.sdlpop.game

/**
 * Game configuration options — level-specific tables, guard skills, special positions.
 * Translated from custom_options_type in types.h.
 */
data class CustomOptionsType(
    var startMinutesLeft: Int = 60,      // word
    var startTicksLeft: Int = 719,       // word
    var startHitp: Int = 3,              // word
    var maxHitpAllowed: Int = 10,        // word
    var savingAllowedFirstLevel: Int = 3,  // word
    var savingAllowedLastLevel: Int = 13,  // word
    var startUpsideDown: Int = 0,        // byte
    var startInBlindMode: Int = 0,       // byte
    var copyprotLevel: Int = 2,          // word
    var drawnTileTopLevelEdge: Int = Tiles.FLOOR,    // byte
    var drawnTileLeftLevelEdge: Int = Tiles.WALL,    // byte
    var levelEdgeHitTile: Int = Tiles.WALL,          // byte
    var allowTriggeringAnyTile: Int = 0,  // byte
    var enableWdaInPalace: Int = 0,       // byte
    // vga_palette: 16 RGB entries — not needed for game logic (rendering only)
    var firstLevel: Int = 1,              // word
    var skipTitle: Int = 0,               // byte
    var shiftLAllowedUntilLevel: Int = 4, // word
    var shiftLReducedMinutes: Int = 15,   // word
    var shiftLReducedTicks: Int = 719,    // word
    var demoHitp: Int = 4,                // word
    var demoEndRoom: Int = 24,            // word
    var introMusicLevel: Int = 1,         // word
    var haveSwordFromLevel: Int = 2,      // word
    var checkpointLevel: Int = 3,         // word
    var checkpointRespawnDir: Int = Directions.LEFT,  // sbyte
    var checkpointRespawnRoom: Int = 2,    // byte
    var checkpointRespawnTilepos: Int = 6, // byte
    var checkpointClearTileRoom: Int = 7,  // byte
    var checkpointClearTileCol: Int = 4,   // byte
    var checkpointClearTileRow: Int = 0,   // byte
    var skeletonLevel: Int = 3,            // word
    var skeletonRoom: Int = 1,             // byte
    var skeletonTriggerColumn1: Int = 2,   // byte
    var skeletonTriggerColumn2: Int = 3,   // byte
    var skeletonColumn: Int = 5,           // byte
    var skeletonRow: Int = 1,              // byte
    var skeletonRequireOpenLevelDoor: Int = 1, // byte
    var skeletonSkill: Int = 2,            // byte
    var skeletonReappearRoom: Int = 3,     // byte
    var skeletonReappearX: Int = 133,      // byte
    var skeletonReappearRow: Int = 1,      // byte
    var skeletonReappearDir: Int = Directions.RIGHT, // byte
    var mirrorLevel: Int = 4,              // word
    var mirrorRoom: Int = 4,               // byte
    var mirrorColumn: Int = 4,             // byte
    var mirrorRow: Int = 0,                // byte
    var mirrorTile: Int = Tiles.MIRROR,    // byte
    var showMirrorImage: Int = 1,          // byte
    var shadowStealLevel: Int = 5,         // byte
    var shadowStealRoom: Int = 24,         // byte
    var shadowStepLevel: Int = 6,          // byte
    var shadowStepRoom: Int = 1,           // byte
    var fallingExitLevel: Int = 6,         // word
    var fallingExitRoom: Int = 1,          // byte
    var fallingEntryLevel: Int = 7,        // word
    var fallingEntryRoom: Int = 17,        // byte
    var mouseLevel: Int = 8,               // word
    var mouseRoom: Int = 16,               // byte
    var mouseDelay: Int = 150,             // word
    var mouseObject: Int = 24,             // byte
    var mouseStartX: Int = 200,            // byte
    var looseTilesLevel: Int = 13,          // word
    var looseTilesRoom1: Int = 23,          // byte
    var looseTilesRoom2: Int = 16,          // byte
    var looseTilesFirstTile: Int = 22,      // byte
    var looseTilesLastTile: Int = 27,       // byte
    var jaffarVictoryLevel: Int = 13,       // word
    var jaffarVictoryFlashTime: Int = 18,   // byte
    var hideLevelNumberFromLevel: Int = 14, // word
    var level13LevelNumber: Int = 12,       // byte
    var victoryStopsTimeLevel: Int = 13,    // word
    var winLevel: Int = 14,                 // word
    var winRoom: Int = 5,                   // byte
    var looseFloorDelay: Int = 11,          // byte

    // Per-level tables (indexed 0-15, level 0 unused in original)
    val tblLevelType: IntArray = intArrayOf(0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 1, 1, 0, 0, 1, 0),
    val tblLevelColor: IntArray = intArrayOf(0, 0, 0, 1, 0, 0, 0, 1, 2, 2, 0, 0, 3, 3, 4, 0),
    val tblGuardType: IntArray = intArrayOf(0, 0, 0, 2, 0, 0, 1, 0, 0, 0, 0, 0, 4, 3, -1, -1),
    val tblGuardHp: IntArray = intArrayOf(4, 3, 3, 3, 3, 4, 5, 4, 4, 5, 5, 5, 4, 6, 0, 0),
    val tblCutscenesByIndex: IntArray = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15),
    val tblEntryPose: IntArray = intArrayOf(0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0),
    val tblSeamlessExit: IntArray = intArrayOf(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 23, -1, -1, -1),

    // Guard skills (indexed 0-11, NUM_GUARD_SKILLS=12)
    val strikeprob: IntArray = intArrayOf(61, 100, 61, 61, 61, 40, 100, 220, 0, 48, 32, 48),
    val restrikeprob: IntArray = intArrayOf(0, 0, 0, 5, 5, 175, 16, 8, 0, 255, 255, 150),
    val blockprob: IntArray = intArrayOf(0, 150, 150, 200, 200, 255, 200, 250, 0, 255, 255, 255),
    val impblockprob: IntArray = intArrayOf(0, 61, 61, 100, 100, 145, 100, 250, 0, 145, 255, 175),
    val advprob: IntArray = intArrayOf(255, 200, 200, 200, 255, 255, 200, 0, 0, 255, 100, 100),
    val refractimer: IntArray = intArrayOf(16, 16, 16, 16, 8, 8, 8, 8, 0, 8, 0, 0),
    val extrastrength: IntArray = intArrayOf(0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0),

    // Shadow starting positions
    val initShad6: IntArray = intArrayOf(0x0F, 0x51, 0x76, 0, 0, 1, 0, 0),
    val initShad5: IntArray = intArrayOf(0x0F, 0x37, 0x37, 0, 0xFF, 0, 0, 0),
    val initShad12: IntArray = intArrayOf(0x0F, 0x51, 0xE8, 0, 0, 0, 0, 0),

    // Automatic moves
    val demoMoves: Array<AutoMoveType> = arrayOf(
        AutoMoveType(0x00, 0), AutoMoveType(0x01, 1), AutoMoveType(0x0D, 0),
        AutoMoveType(0x1E, 1), AutoMoveType(0x25, 5), AutoMoveType(0x2F, 0),
        AutoMoveType(0x30, 1), AutoMoveType(0x41, 0), AutoMoveType(0x49, 2),
        AutoMoveType(0x4B, 0), AutoMoveType(0x63, 2), AutoMoveType(0x64, 0),
        AutoMoveType(0x73, 5), AutoMoveType(0x80, 6), AutoMoveType(0x88, 3),
        AutoMoveType(0x9D, 7), AutoMoveType(0x9E, 0), AutoMoveType(0x9F, 1),
        AutoMoveType(0xAB, 4), AutoMoveType(0xB1, 0), AutoMoveType(0xB2, 1),
        AutoMoveType(0xBC, 0), AutoMoveType(0xC1, 1), AutoMoveType(0xCD, 0),
        AutoMoveType(0xE9.toShort(), (-1).toShort())
    ),
    val shadDrinkMove: Array<AutoMoveType> = arrayOf(
        AutoMoveType(0x00, 0), AutoMoveType(0x01, 1), AutoMoveType(0x0E, 0),
        AutoMoveType(0x12, 6), AutoMoveType(0x1D, 7), AutoMoveType(0x2D, 2),
        AutoMoveType(0x31, 1), AutoMoveType(0xFF.toShort(), (-2).toShort())
    ),

    // Speeds
    var baseSpeed: Int = 5,       // byte
    var fightSpeed: Int = 6,      // byte
    var chomperSpeed: Int = 15,   // byte

    var noMouseInEnding: Int = 0  // byte
)

/**
 * Bug fix options — toggles for various gameplay fixes.
 * Translated from fixes_options_type in types.h.
 * All fields are byte (0=disabled, nonzero=enabled).
 */
data class FixesOptionsType(
    var enableCrouchAfterClimbing: Int = 0,
    var enableFreezeTimeDuringEndMusic: Int = 0,
    var enableRememberGuardHp: Int = 0,
    var fixGateSounds: Int = 0,
    var fixTwoCollBug: Int = 0,
    var fixInfiniteDownBug: Int = 0,
    var fixGateDrawingBug: Int = 0,
    var fixBigpillarClimb: Int = 0,
    var fixJumpDistanceAtEdge: Int = 0,
    var fixEdgeDistanceCheckWhenClimbing: Int = 0,
    var fixPainlessFallOnGuard: Int = 0,
    var fixWallBumpTriggersTileBelow: Int = 0,
    var fixStandOnThinAir: Int = 0,
    var fixPressThroughClosedGates: Int = 0,
    var fixGrabFallingSpeed: Int = 0,
    var fixSkeletonChomperBlood: Int = 0,
    var fixMoveAfterDrink: Int = 0,
    var fixLooseLeftOfPotion: Int = 0,
    var fixGuardFollowingThroughClosedGates: Int = 0,
    var fixSafeLandingOnSpikes: Int = 0,
    var fixGlideThroughWall: Int = 0,
    var fixDropThroughTapestry: Int = 0,
    var fixLandAgainstGateOrTapestry: Int = 0,
    var fixUnintendedSwordStrike: Int = 0,
    var fixRetreatWithoutLeavingRoom: Int = 0,
    var fixRunningJumpThroughTapestry: Int = 0,
    var fixPushGuardIntoWall: Int = 0,
    var fixJumpThroughWallAboveGate: Int = 0,
    var fixChompersNotStarting: Int = 0,
    var fixFeatherInterruptedByLeveldoor: Int = 0,
    var fixOffscreenGuardsDisappearing: Int = 0,
    var fixMoveAfterSheathe: Int = 0,
    var fixHiddenFloorsDuringFlashing: Int = 0,
    var fixHangOnTeleport: Int = 0,
    var fixExitDoor: Int = 0,
    var fixQuicksaveDuringFeather: Int = 0,
    var fixCapedPrinceSlidingThroughGate: Int = 0,
    var fixDoortopDisablingGuard: Int = 0,
    var enableSuperHighJump: Int = 0,
    var fixJumpingOverGuard: Int = 0,
    var fixDrop2RoomsClimbingLooseTile: Int = 0,
    var fixFallingThroughFloorDuringSwordStrike: Int = 0,
    var enableJumpGrab: Int = 0,
    var fixRegisterQuickInput: Int = 0,
    var fixTurnRunningNearWall: Int = 0,
    var fixFeatherFallAffectsGuards: Int = 0,
    var fixOneHpStopsBlinking: Int = 0,
    var fixDeadFloatingInAir: Int = 0,
    var fixCollFlags: Int = 0  // FIX_COLL_FLAGS — disabled in reference build
)
