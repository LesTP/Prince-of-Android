package com.sdlpop.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SequenceTableTest {

    @Test
    fun `seqtbl array has correct size`() {
        assertEquals(SequenceTable.SEQTBL_SIZE, SequenceTable.seqtbl.size,
            "seqtbl should be ${SequenceTable.SEQTBL_SIZE} bytes")
    }

    @Test
    fun `seqtblOffsets array has correct size`() {
        assertEquals(115, SequenceTable.seqtblOffsets.size,
            "seqtblOffsets should have 115 entries (indices 0-114)")
    }

    @Test
    fun `seqtblOffsets index 0 is zero`() {
        assertEquals(0x0000, SequenceTable.seqtblOffsets[0])
    }

    @Test
    fun `seqtblOffsets spot check - startrun`() {
        // Index 1 = startrun = SEQTBL_BASE + 5 = 0x1973
        assertEquals(0x1973, SequenceTable.seqtblOffsets[SeqIds.seq_1_start_run])
    }

    @Test
    fun `seqtblOffsets spot check - stand`() {
        // Index 2 = stand = SEQTBL_BASE + 50 = 0x19A0
        assertEquals(0x19A0, SequenceTable.seqtblOffsets[SeqIds.seq_2_stand])
    }

    @Test
    fun `seqtblOffsets spot check - standjump`() {
        // Index 3 = standjump = SEQTBL_BASE + 293 = 0x1A93
        assertEquals(0x1A93, SequenceTable.seqtblOffsets[SeqIds.seq_3_standing_jump])
    }

    @Test
    fun `seqtblOffsets spot check - engarde`() {
        // Index 55 = engarde = SEQTBL_BASE + 86 = 0x19C4
        assertEquals(0x19C4, SequenceTable.seqtblOffsets[SeqIds.seq_55_draw_sword])
    }

    @Test
    fun `seqtblOffsets spot check - climbstairs`() {
        // Index 70 = climbstairs = SEQTBL_BASE + 1903 = 0x20DD
        assertEquals(0x20DD, SequenceTable.seqtblOffsets[SeqIds.seq_70_go_up_on_level_door])
    }

    @Test
    fun `seqtblOffsets spot check - Pstand`() {
        // Index 94 = Pstand = SEQTBL_BASE + 2087 = 0x2195
        assertEquals(0x2195, SequenceTable.seqtblOffsets[SeqIds.seq_94_princess_stand_PV1])
    }

    @Test
    fun `seqtblOffsets spot check - mouse stand`() {
        // Index 114 = Mraise = SEQTBL_BASE + 2281 = 0x2257
        assertEquals(0x2257, SequenceTable.seqtblOffsets[SeqIds.seq_114_mouse_stand])
    }

    @Test
    fun `running sequence starts with act then jmp`() {
        // running: act(actions_1_run_jump), jmp(runcyc1)
        val t = SequenceTable.seqtbl
        assertEquals(SeqtblInstructions.SEQ_ACTION, t[0])
        assertEquals(Actions.RUN_JUMP, t[1])
        assertEquals(SeqtblInstructions.SEQ_JMP, t[2])
        // jmp target = runcyc1 = SEQTBL_BASE + 19 = 0x1981
        assertEquals(0x81, t[3]) // low byte
        assertEquals(0x19, t[4]) // high byte
    }

    @Test
    fun `stand sequence at correct offset`() {
        // stand (offset 50): act(actions_0_stand), frame_15_stand, jmp(stand)
        val t = SequenceTable.seqtbl
        val off = SequenceTable.L_stand
        assertEquals(SeqtblInstructions.SEQ_ACTION, t[off])
        assertEquals(Actions.STAND, t[off + 1])
        assertEquals(FrameIds.frame_15_stand, t[off + 2])
        assertEquals(SeqtblInstructions.SEQ_JMP, t[off + 3])
    }

    @Test
    fun `freefall sequence at correct offset`() {
        // freefall (offset 987): act(actions_4_in_freefall), frame_106_fall, jmp(freefall_loop)
        val t = SequenceTable.seqtbl
        val off = SequenceTable.L_freefall
        assertEquals(SeqtblInstructions.SEQ_ACTION, t[off])
        assertEquals(Actions.IN_FREEFALL, t[off + 1])
        // freefall_loop (offset 989)
        assertEquals(FrameIds.frame_106_fall, t[off + 2])
        assertEquals(SeqtblInstructions.SEQ_JMP, t[off + 3])
    }

    @Test
    fun `Vstand uses alt2frame value 54`() {
        // Vstand (offset 1988): 54, jmp(Vstand)
        val t = SequenceTable.seqtbl
        val off = SequenceTable.L_Vstand
        assertEquals(54, t[off]) // alt2frame_54_Vstand
        assertEquals(SeqtblInstructions.SEQ_JMP, t[off + 1])
    }

    @Test
    fun `last bytes are Mclimb_loop jmp`() {
        // Last 4 bytes: frame_188_mouse_stand, SEQ_JMP, lo, hi
        val t = SequenceTable.seqtbl
        val off = SequenceTable.L_Mclimb_loop
        assertEquals(FrameIds.frame_188_mouse_stand, t[off])
        assertEquals(SeqtblInstructions.SEQ_JMP, t[off + 1])
        // jmp target = Mclimb_loop = SEQTBL_BASE + 2306 = 0x2270
        assertEquals(0x70, t[off + 2])
        assertEquals(0x22, t[off + 3])
        assertEquals(off + 4, t.size, "Mclimb_loop should be the last sequence")
    }

    @Test
    fun `all seqtbl values are in byte range`() {
        for ((i, v) in SequenceTable.seqtbl.withIndex()) {
            assertTrue(v in 0..255, "seqtbl[$i] = $v is outside byte range 0-255")
        }
    }

    @Test
    fun `all seqtblOffsets are valid addresses`() {
        for ((i, v) in SequenceTable.seqtblOffsets.withIndex()) {
            if (i == 0) {
                assertEquals(0, v, "Index 0 should be 0")
                continue
            }
            assertTrue(v >= SequenceTable.SEQTBL_BASE,
                "seqtblOffsets[$i] = $v should be >= SEQTBL_BASE")
            assertTrue(v < SequenceTable.SEQTBL_BASE + SequenceTable.SEQTBL_SIZE,
                "seqtblOffsets[$i] = $v should be < SEQTBL_BASE + SEQTBL_SIZE")
        }
    }

    @Test
    fun `stepfall has jmp_if_feather instruction`() {
        // stepfall (offset 621): act(in_midair), dx(1), dy(3), jmp_if_feather(stepfloat)
        val t = SequenceTable.seqtbl
        val off = SequenceTable.L_stepfall
        assertEquals(SeqtblInstructions.SEQ_ACTION, t[off])
        assertEquals(Actions.IN_MIDAIR, t[off + 1])
        assertEquals(SeqtblInstructions.SEQ_DX, t[off + 2])
        assertEquals(1, t[off + 3])
        assertEquals(SeqtblInstructions.SEQ_DY, t[off + 4])
        assertEquals(3, t[off + 5])
        assertEquals(SeqtblInstructions.SEQ_JMP_IF_FEATHER, t[off + 6])
        // Target = stepfloat = SEQTBL_BASE + 664 = 0x1C06
        assertEquals(0x06, t[off + 7])
        assertEquals(0x1C, t[off + 8])
    }

    @Test
    fun `climbup has SEQ_UP instruction`() {
        // Find SEQ_UP in the climbup sequence
        val t = SequenceTable.seqtbl
        val off = SequenceTable.L_climbup
        val end = off + 33 // climbup is 33 bytes
        var found = false
        for (i in off until end) {
            if (t[i] == SeqtblInstructions.SEQ_UP) {
                found = true
                break
            }
        }
        assertTrue(found, "climbup sequence should contain SEQ_UP")
    }

    @Test
    fun `negative dx values are unsigned byte encoded`() {
        // flee (offset 270): act(turn), dx(-8), jmp(turn)
        // dx(-8) = SEQ_DX, 248 (0xF8 = -8 as unsigned byte)
        val t = SequenceTable.seqtbl
        val off = SequenceTable.L_flee
        assertEquals(SeqtblInstructions.SEQ_ACTION, t[off])
        assertEquals(Actions.TURN, t[off + 1])
        assertEquals(SeqtblInstructions.SEQ_DX, t[off + 2])
        assertEquals((-8) and 0xFF, t[off + 3]) // 248
    }
}
