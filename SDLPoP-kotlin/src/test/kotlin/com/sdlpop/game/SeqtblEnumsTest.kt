package com.sdlpop.game

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Module 7, Step 7a: Verify sequence table enum values match C originals.
 * Spot-checks across all four enum groups.
 */
class SeqtblEnumsTest {

    // === SeqtblInstructions: verify opcode values match C enum ===

    @Test
    fun `SEQ_END_LEVEL is 0xF1`() = assertEquals(0xF1, SeqtblInstructions.SEQ_END_LEVEL)

    @Test
    fun `SEQ_SOUND is 0xF2`() = assertEquals(0xF2, SeqtblInstructions.SEQ_SOUND)

    @Test
    fun `SEQ_ACTION is 0xF9`() = assertEquals(0xF9, SeqtblInstructions.SEQ_ACTION)

    @Test
    fun `SEQ_DX is 0xFB`() = assertEquals(0xFB, SeqtblInstructions.SEQ_DX)

    @Test
    fun `SEQ_JMP is 0xFF`() = assertEquals(0xFF, SeqtblInstructions.SEQ_JMP)

    @Test
    fun `SEQ_FLIP is 0xFE`() = assertEquals(0xFE, SeqtblInstructions.SEQ_FLIP)

    @Test
    fun `SEQ_SET_FALL is 0xF8`() = assertEquals(0xF8, SeqtblInstructions.SEQ_SET_FALL)

    @Test
    fun `SEQ_DIE is 0xF6`() = assertEquals(0xF6, SeqtblInstructions.SEQ_DIE)

    // === SeqtblSounds: verify all 5 entries ===

    @Test
    fun `SND_SILENT is 0`() = assertEquals(0, SeqtblSounds.SND_SILENT)

    @Test
    fun `SND_FOOTSTEP is 1`() = assertEquals(1, SeqtblSounds.SND_FOOTSTEP)

    @Test
    fun `SND_LEVEL is 4`() = assertEquals(4, SeqtblSounds.SND_LEVEL)

    // === FrameIds: spot-check key frame values ===

    @Test
    fun `frame_0 is 0`() = assertEquals(0, FrameIds.frame_0)

    @Test
    fun `frame_15_stand is 15`() = assertEquals(15, FrameIds.frame_15_stand)

    @Test
    fun `frame_106_fall is 106`() = assertEquals(106, FrameIds.frame_106_fall)

    @Test
    fun `frame_109_crouch is 109`() = assertEquals(109, FrameIds.frame_109_crouch)

    @Test
    fun `frame_150_parry is 150`() = assertEquals(150, FrameIds.frame_150_parry)

    @Test
    fun `frame_177_spiked is 177`() = assertEquals(177, FrameIds.frame_177_spiked)

    @Test
    fun `frame_185_dead is 185`() = assertEquals(185, FrameIds.frame_185_dead)

    @Test
    fun `frame_228_exit_stairs_12 is 228`() = assertEquals(228, FrameIds.frame_228_exit_stairs_12)

    @Test
    fun `frame_240_sheathe is 240`() = assertEquals(240, FrameIds.frame_240_sheathe)

    @Test
    fun `frame_67 skips 66 (matches C enum gap)`() = assertEquals(67, FrameIds.frame_67_start_jump_up_1)

    // === SeqIds: spot-check key sequence values ===

    @Test
    fun `seq_1_start_run is 1`() = assertEquals(1, SeqIds.seq_1_start_run)

    @Test
    fun `seq_2_stand is 2`() = assertEquals(2, SeqIds.seq_2_stand)

    @Test
    fun `seq_7_fall is 7`() = assertEquals(7, SeqIds.seq_7_fall)

    @Test
    fun `seq_13_stop_run is 13 (skips 12)`() = assertEquals(13, SeqIds.seq_13_stop_run)

    @Test
    fun `seq_48_super_high_jump is 48`() = assertEquals(48, SeqIds.seq_48_super_high_jump)

    @Test
    fun `seq_75_strike is 75`() = assertEquals(75, SeqIds.seq_75_strike)

    @Test
    fun `seq_84_run is 84`() = assertEquals(84, SeqIds.seq_84_run)

    @Test
    fun `seq_114_mouse_stand is 114`() = assertEquals(114, SeqIds.seq_114_mouse_stand)

    @Test
    fun `seq_teleport is 115`() = assertEquals(115, SeqIds.seq_teleport)
}
