/*
SDLPoP-kotlin, a Kotlin port of SDLPoP (Prince of Persia).
Based on SDLPoP by Dávid Nagy, licensed under GPL v3+.

Module 7, Step 7b: Sequence Table Data
Translates seqtbl.c — the animation sequence byte array and offset lookup table.

The sequence table encodes animation sequences as a mini virtual machine.
Each sequence is a stream of bytes: frame IDs (0-228), instruction opcodes
(0xF1-0xFF), and operands. Game logic reads this table via Char.curr_seq
to drive character animation.

Built using helper functions that mirror the C macros (act, jmp, dx, dy, etc.)
for direct source comparison and self-verification.
*/

package com.sdlpop.game

import com.sdlpop.game.SeqtblInstructions.SEQ_ACTION
import com.sdlpop.game.SeqtblInstructions.SEQ_DIE
import com.sdlpop.game.SeqtblInstructions.SEQ_DOWN
import com.sdlpop.game.SeqtblInstructions.SEQ_DX
import com.sdlpop.game.SeqtblInstructions.SEQ_DY
import com.sdlpop.game.SeqtblInstructions.SEQ_END_LEVEL
import com.sdlpop.game.SeqtblInstructions.SEQ_FLIP
import com.sdlpop.game.SeqtblInstructions.SEQ_GET_ITEM
import com.sdlpop.game.SeqtblInstructions.SEQ_JMP
import com.sdlpop.game.SeqtblInstructions.SEQ_JMP_IF_FEATHER
import com.sdlpop.game.SeqtblInstructions.SEQ_KNOCK_DOWN
import com.sdlpop.game.SeqtblInstructions.SEQ_KNOCK_UP
import com.sdlpop.game.SeqtblInstructions.SEQ_SET_FALL
import com.sdlpop.game.SeqtblInstructions.SEQ_SOUND
import com.sdlpop.game.SeqtblInstructions.SEQ_UP
import com.sdlpop.game.SeqtblSounds.SND_DRINK
import com.sdlpop.game.SeqtblSounds.SND_FOOTSTEP
import com.sdlpop.game.SeqtblSounds.SND_LEVEL
import com.sdlpop.game.SeqtblSounds.SND_SILENT
import com.sdlpop.game.Actions.STAND as A_STAND
import com.sdlpop.game.Actions.RUN_JUMP as A_RUN_JUMP
import com.sdlpop.game.Actions.HANG_CLIMB as A_HANG_CLIMB
import com.sdlpop.game.Actions.IN_MIDAIR as A_IN_MIDAIR
import com.sdlpop.game.Actions.IN_FREEFALL as A_IN_FREEFALL
import com.sdlpop.game.Actions.BUMPED as A_BUMPED
import com.sdlpop.game.Actions.HANG_STRAIGHT as A_HANG_STRAIGHT
import com.sdlpop.game.Actions.TURN as A_TURN

object SequenceTable {
    const val SEQTBL_BASE = 0x196E

    // Label offsets (relative to seqtbl start). Absolute address = SEQTBL_BASE + offset.
    // These match the C #define chain in seqtbl.c exactly.
    const val L_running = 0
    const val L_startrun = 5
    const val L_runstt1 = 7
    const val L_runstt4 = 10
    const val L_runcyc1 = 19
    const val L_runcyc7 = 39
    const val L_stand = 50
    const val L_goalertstand = 56
    const val L_alertstand = 58
    const val L_arise = 62
    const val L_guardengarde = 83
    const val L_engarde = 86
    const val L_ready = 100
    const val L_ready_loop = 106
    const val L_stabbed = 110
    const val L_strikeadv = 139
    const val L_strikeret = 153
    const val L_advance = 165
    const val L_fastadvance = 180
    const val L_retreat = 192
    const val L_strike = 206
    const val L_faststrike = 212
    const val L_guy4 = 215
    const val L_guy7 = 220
    const val L_guy8 = 223
    const val L_blockedstrike = 230
    const val L_blocktostrike = 236
    const val L_readyblock = 240
    const val L_blocking = 241
    const val L_striketoblock = 245
    const val L_landengarde = 250
    const val L_bumpengfwd = 256
    const val L_bumpengback = 263
    const val L_flee = 270
    const val L_turnengarde = 277
    const val L_alertturn = 285
    const val L_standjump = 293
    const val L_sjland = 322
    const val L_runjump = 351
    const val L_rjlandrun = 397
    const val L_rdiveroll = 406
    const val L_rdiveroll_crouch = 424
    const val L_sdiveroll = 428
    const val L_crawl = 429
    const val L_crawl_crouch = 443
    const val L_turndraw = 447
    const val L_turn = 459
    const val L_turnrun = 485
    const val L_runturn = 492
    const val L_fightfall = 535
    const val L_efightfall = 563
    const val L_efightfallfwd = 593
    const val L_stepfall = 621
    const val L_fall1 = 630
    const val L_patchfall = 652
    const val L_stepfall2 = 659
    const val L_stepfloat = 664
    const val L_jumpfall = 686
    const val L_rjumpfall = 714
    const val L_jumphangMed = 742
    const val L_jumphangLong = 763
    const val L_jumpbackhang = 790
    const val L_hang = 819
    const val L_hang1 = 822
    const val L_hangstraight = 867
    const val L_hangstraight_loop = 874
    const val L_climbfail = 878
    const val L_climbdown = 894
    const val L_climbup = 918
    const val L_hangdrop = 951
    const val L_hangfall = 968
    const val L_freefall = 987
    const val L_freefall_loop = 989
    const val L_runstop = 993
    const val L_jumpup = 1018
    const val L_highjump = 1039
    const val L_superhijump = 1069
    const val L_fallhang = 1160
    const val L_bump = 1166
    const val L_bumpfall = 1176
    const val L_bumpfloat = 1207
    const val L_hardbump = 1229
    const val L_testfoot = 1259
    const val L_stepback = 1290
    const val L_step14 = 1295
    const val L_step13 = 1326
    const val L_step12 = 1357
    const val L_step11 = 1388
    const val L_step10 = 1417
    const val L_step10a = 1422
    const val L_step9 = 1445
    const val L_step8 = 1451
    const val L_step7 = 1477
    const val L_step6 = 1498
    const val L_step5 = 1519
    const val L_step4 = 1540
    const val L_step3 = 1556
    const val L_step2 = 1572
    const val L_step1 = 1584
    const val L_stoop = 1593
    const val L_stoop_crouch = 1601
    const val L_standup = 1605
    const val L_pickupsword = 1628
    const val L_resheathe = 1644
    const val L_fastsheathe = 1677
    const val L_drinkpotion = 1691
    const val L_softland = 1725
    const val L_softland_crouch = 1736
    const val L_landrun = 1740
    const val L_medland = 1772
    const val L_hardland = 1838
    const val L_hardland_dead = 1847
    const val L_stabkill = 1851
    const val L_dropdead = 1856
    const val L_dropdead_dead = 1868
    const val L_impale = 1872
    const val L_impale_dead = 1879
    const val L_halve = 1883
    const val L_halve_dead = 1887
    const val L_crush = 1891
    const val L_deadfall = 1894
    const val L_deadfall_loop = 1899
    const val L_climbstairs = 1903
    const val L_climbstairs_loop = 1984
    const val L_Vstand = 1988
    const val L_Vraise = 1992
    const val L_Vraise_loop = 2013
    const val L_Vwalk = 2017
    const val L_Vwalk1 = 2019
    const val L_Vwalk2 = 2022
    const val L_Vstop = 2040
    const val L_Vexit = 2047
    const val L_Pstand = 2087
    const val L_Palert = 2091
    const val L_Pstepback = 2106
    const val L_Pstepback_loop = 2122
    const val L_Plie = 2126
    const val L_Pwaiting = 2130
    const val L_Pembrace = 2134
    const val L_Pembrace_loop = 2164
    const val L_Pstroke = 2168
    const val L_Prise = 2172
    const val L_Prise_loop = 2186
    const val L_Pcrouch = 2190
    const val L_Pcrouch_loop = 2254
    const val L_Pslump = 2258
    const val L_Pslump_loop = 2259
    const val L_Mscurry = 2263
    const val L_Mscurry1 = 2265
    const val L_Mstop = 2277
    const val L_Mraise = 2281
    const val L_Mleave = 2285
    const val L_Mclimb = 2304
    const val L_Mclimb_loop = 2306

    // Expected total size of seqtbl array (without USE_TELEPORTS)
    const val SEQTBL_SIZE = 2310

    // The sequence table byte array, built to match C seqtbl[] byte-for-byte
    val seqtbl: IntArray

    // Sequence ID to absolute address lookup table (matches C seqtbl_offsets[])
    val seqtblOffsets: IntArray

    init {
        val b = mutableListOf<Int>()

        // Builder helpers matching C macros
        fun act(action: Int) { b.add(SEQ_ACTION); b.add(action) }
        fun jmp(labelOffset: Int) {
            val a = SEQTBL_BASE + labelOffset
            b.add(SEQ_JMP); b.add(a and 0xFF); b.add((a shr 8) and 0xFF)
        }
        fun jmpIfFeather(labelOffset: Int) {
            val a = SEQTBL_BASE + labelOffset
            b.add(SEQ_JMP_IF_FEATHER); b.add(a and 0xFF); b.add((a shr 8) and 0xFF)
        }
        fun dx(amount: Int) { b.add(SEQ_DX); b.add(amount and 0xFF) }
        fun dy(amount: Int) { b.add(SEQ_DY); b.add(amount and 0xFF) }
        fun snd(sound: Int) { b.add(SEQ_SOUND); b.add(sound) }
        fun setFall(x: Int, y: Int) { b.add(SEQ_SET_FALL); b.add(x and 0xFF); b.add(y and 0xFF) }
        fun f(vararg frames: Int) { for (fr in frames) b.add(fr) }
        fun op(opcode: Int) { b.add(opcode) }

        // Frame ID shortcuts (using FrameIds constants)
        val F = FrameIds

        // ---- running (offset 0) ----
        act(A_RUN_JUMP); jmp(L_runcyc1)

        // ---- startrun (offset 5) ----
        act(A_RUN_JUMP)
        // runstt1 (offset 7)
        f(F.frame_1_start_run, F.frame_2_start_run, F.frame_3_start_run)
        // runstt4 (offset 10)
        f(F.frame_4_start_run)
        dx(8); f(F.frame_5_start_run)
        dx(3); f(F.frame_6_start_run)
        // runcyc1 (offset 19)
        dx(3); f(F.frame_7_run)
        dx(5); f(F.frame_8_run)
        dx(1); snd(SND_FOOTSTEP); f(F.frame_9_run)
        dx(2); f(F.frame_10_run)
        dx(4); f(F.frame_11_run)
        dx(5); f(F.frame_12_run)
        // runcyc7 (offset 39)
        dx(2); snd(SND_FOOTSTEP); f(F.frame_13_run)
        dx(3); f(F.frame_14_run)
        dx(4); jmp(L_runcyc1)

        // ---- stand (offset 50) ----
        act(A_STAND); f(F.frame_15_stand)
        jmp(L_stand)

        // ---- goalertstand (offset 56) ----
        act(A_RUN_JUMP)
        // alertstand (offset 58)
        f(F.frame_166_stand_inactive)
        jmp(L_alertstand)

        // ---- arise (offset 62) ----
        act(A_BUMPED); dx(10); f(F.frame_177_spiked)
        f(F.frame_177_spiked)
        dx(-7); dy(-2); f(F.frame_178_chomped)
        dx(5); dy(2); f(F.frame_166_stand_inactive)
        dx(-1); jmp(L_ready)

        // ---- guardengarde (offset 83) ----
        jmp(L_ready)

        // ---- engarde (offset 86) ----
        act(A_RUN_JUMP)
        dx(2); f(F.frame_207_draw_1)
        f(F.frame_208_draw_2)
        dx(2); f(F.frame_209_draw_3)
        dx(2); f(F.frame_210_draw_4)
        // ready (offset 100)
        dx(3); act(A_RUN_JUMP); snd(SND_SILENT); f(F.frame_158_stand_with_sword)
        f(F.frame_170_stand_with_sword)
        // ready_loop (offset 106)
        f(F.frame_171_stand_with_sword)
        jmp(L_ready_loop)

        // ---- stabbed (offset 110) ----
        act(A_BUMPED); setFall(-1, 0); f(F.frame_172_jumpfall_2)
        dx(-1); dy(1); f(F.frame_173_jumpfall_3)
        dx(-1); f(F.frame_174_jumpfall_4)
        dx(-1); dy(2) // frame 175 commented out in Apple II source
        dx(-2); dy(1)
        dx(-5); dy(-4)
        jmp(L_guy8)

        // ---- strikeadv (offset 139) ----
        act(A_RUN_JUMP); setFall(1, 0); f(F.frame_155_guy_7)
        dx(2); f(F.frame_165_walk_with_sword)
        dx(-2); jmp(L_ready)

        // ---- strikeret (offset 153) ----
        act(A_RUN_JUMP); setFall(-1, 0); f(F.frame_155_guy_7)
        f(F.frame_156_guy_8)
        f(F.frame_157_walk_with_sword)
        f(F.frame_158_stand_with_sword)
        jmp(L_retreat)

        // ---- advance (offset 165) ----
        act(A_RUN_JUMP); setFall(1, 0)
        dx(2); f(F.frame_163_fighting)
        dx(4); f(F.frame_164_fighting)
        f(F.frame_165_walk_with_sword)
        jmp(L_ready)

        // ---- fastadvance (offset 180) ----
        act(A_RUN_JUMP); setFall(1, 0); dx(6); f(F.frame_164_fighting)
        f(F.frame_165_walk_with_sword)
        jmp(L_ready)

        // ---- retreat (offset 192) ----
        act(A_RUN_JUMP); setFall(-1, 0); dx(-3); f(F.frame_160_fighting)
        dx(-2); f(F.frame_157_walk_with_sword)
        jmp(L_ready)

        // ---- strike (offset 206) ----
        act(A_RUN_JUMP); setFall(-1, 0); f(F.frame_168_back)
        // faststrike (offset 212)
        act(A_RUN_JUMP); f(F.frame_151_strike_1)
        // guy4 (offset 215)
        act(A_RUN_JUMP); f(F.frame_152_strike_2)
        f(F.frame_153_strike_3)
        f(F.frame_154_poking)
        // guy7 (offset 220)
        act(A_BUMPED); f(F.frame_155_guy_7)
        // guy8 (offset 223)
        act(A_RUN_JUMP); f(F.frame_156_guy_8)
        f(F.frame_157_walk_with_sword)
        jmp(L_ready)

        // ---- blockedstrike (offset 230) ----
        act(A_RUN_JUMP); f(F.frame_167_blocked)
        jmp(L_guy7)

        // ---- blocktostrike (offset 236) ----
        f(F.frame_162_block_to_strike)
        jmp(L_guy4)

        // ---- readyblock (offset 240) ----
        f(F.frame_169_begin_block)
        // blocking (offset 241)
        f(F.frame_150_parry)
        jmp(L_ready)

        // ---- striketoblock (offset 245) ----
        f(F.frame_159_fighting)
        f(F.frame_160_fighting)
        jmp(L_blocking)

        // ---- landengarde (offset 250) ----
        act(A_RUN_JUMP); op(SEQ_KNOCK_DOWN); jmp(L_ready)

        // ---- bumpengfwd (offset 256) ----
        act(A_BUMPED); dx(-8); jmp(L_ready)

        // ---- bumpengback (offset 263) ----
        act(A_BUMPED); f(F.frame_160_fighting)
        f(F.frame_157_walk_with_sword)
        jmp(L_ready)

        // ---- flee (offset 270) ----
        act(A_TURN); dx(-8); jmp(L_turn)

        // ---- turnengarde (offset 277) ----
        act(A_BUMPED); op(SEQ_FLIP); dx(5); jmp(L_retreat)

        // ---- alertturn (offset 285) ----
        act(A_BUMPED); op(SEQ_FLIP); dx(18); jmp(L_goalertstand)

        // ---- standjump (offset 293) ----
        act(A_RUN_JUMP); f(F.frame_16_standing_jump_1)
        f(F.frame_17_standing_jump_2)
        dx(2); f(F.frame_18_standing_jump_3)
        dx(2); f(F.frame_19_standing_jump_4)
        dx(2); f(F.frame_20_standing_jump_5)
        dx(2); f(F.frame_21_standing_jump_6)
        dx(2); f(F.frame_22_standing_jump_7)
        dx(7); f(F.frame_23_standing_jump_8)
        dx(9); f(F.frame_24_standing_jump_9)
        dx(5); dy(-6)
        // sjland (offset 322)
        f(F.frame_25_standing_jump_10)
        dx(1); dy(6); f(F.frame_26_standing_jump_11)
        dx(4); op(SEQ_KNOCK_DOWN); snd(SND_FOOTSTEP); f(F.frame_27_standing_jump_12)
        dx(-3); f(F.frame_28_standing_jump_13)
        dx(5); f(F.frame_29_standing_jump_14)
        snd(SND_FOOTSTEP); f(F.frame_30_standing_jump_15)
        f(F.frame_31_standing_jump_16)
        f(F.frame_32_standing_jump_17)
        f(F.frame_33_standing_jump_18)
        dx(1); jmp(L_stand)

        // ---- runjump (offset 351) ----
        act(A_RUN_JUMP); snd(SND_FOOTSTEP); f(F.frame_34_start_run_jump_1)
        dx(5); f(F.frame_35_start_run_jump_2)
        dx(6); f(F.frame_36_start_run_jump_3)
        dx(3); f(F.frame_37_start_run_jump_4)
        dx(5); snd(SND_FOOTSTEP); f(F.frame_38_start_run_jump_5)
        dx(7); f(F.frame_39_start_run_jump_6)
        dx(12); dy(-3); f(F.frame_40_running_jump_1)
        dx(8); dy(-9); f(F.frame_41_running_jump_2)
        dx(8); dy(-2); f(F.frame_42_running_jump_3)
        dx(4); dy(11); f(F.frame_43_running_jump_4)
        dx(4); dy(3)
        // rjlandrun (offset 397)
        f(F.frame_44_running_jump_5)
        dx(5); op(SEQ_KNOCK_DOWN); snd(SND_FOOTSTEP); jmp(L_runcyc1)

        // ---- rdiveroll (offset 406) ----
        act(A_RUN_JUMP); dx(1); f(F.frame_107_fall_land_1)
        dx(2); dx(2); f(F.frame_108_fall_land_2)
        dx(2); f(F.frame_109_crouch)
        dx(2); f(F.frame_109_crouch)
        // rdiveroll_crouch (offset 424)
        dx(2); f(F.frame_109_crouch)
        jmp(L_rdiveroll_crouch)

        // ---- sdiveroll (offset 428) ----
        f(0x00) // not implemented

        // ---- crawl (offset 429) ----
        act(A_RUN_JUMP); dx(1); f(F.frame_110_stand_up_from_crouch_1)
        f(F.frame_111_stand_up_from_crouch_2)
        dx(2); f(F.frame_112_stand_up_from_crouch_3)
        dx(2); f(F.frame_108_fall_land_2)
        // crawl_crouch (offset 443)
        dx(2); f(F.frame_109_crouch)
        jmp(L_crawl_crouch)

        // ---- turndraw (offset 447) ----
        act(A_TURN); op(SEQ_FLIP); dx(6); f(F.frame_45_turn)
        dx(1); f(F.frame_46_turn)
        jmp(L_engarde)

        // ---- turn (offset 459) ----
        act(A_TURN); op(SEQ_FLIP); dx(6); f(F.frame_45_turn)
        dx(1); f(F.frame_46_turn)
        dx(2); f(F.frame_47_turn)
        dx(-1); f(F.frame_48_turn)
        dx(1); f(F.frame_49_turn)
        dx(-2); f(F.frame_50_turn)
        f(F.frame_51_turn)
        f(F.frame_52_turn)
        jmp(L_stand)

        // ---- turnrun (offset 485) ----
        act(A_RUN_JUMP); dx(-1); jmp(L_runstt1)

        // ---- runturn (offset 492) ----
        act(A_RUN_JUMP); dx(1); f(F.frame_53_runturn)
        dx(1); snd(SND_FOOTSTEP); f(F.frame_54_runturn)
        dx(8); f(F.frame_55_runturn)
        snd(SND_FOOTSTEP); f(F.frame_56_runturn)
        dx(7); f(F.frame_57_runturn)
        dx(3); f(F.frame_58_runturn)
        dx(1); f(F.frame_59_runturn)
        f(F.frame_60_runturn)
        dx(2); f(F.frame_61_runturn)
        dx(-1); f(F.frame_62_runturn)
        f(F.frame_63_runturn)
        f(F.frame_64_runturn)
        dx(-1); f(F.frame_65_runturn)
        dx(-14); op(SEQ_FLIP); jmp(L_runcyc7)

        // ---- fightfall (offset 535) ----
        act(A_IN_MIDAIR); dy(-1); f(F.frame_102_start_fall_1)
        dx(-2); dy(6); f(F.frame_103_start_fall_2)
        dx(-2); dy(9); f(F.frame_104_start_fall_3)
        dx(-1); dy(12); f(F.frame_105_start_fall_4)
        dx(-3); setFall(0, 15); jmp(L_freefall)

        // ---- efightfall (offset 563) ----
        act(A_IN_MIDAIR); dy(-1); dx(-2); f(F.frame_102_start_fall_1)
        dx(-3); dy(6); f(F.frame_103_start_fall_2)
        dx(-3); dy(9); f(F.frame_104_start_fall_3)
        dx(-2); dy(12); f(F.frame_105_start_fall_4)
        dx(-3); setFall(0, 15); jmp(L_freefall)

        // ---- efightfallfwd (offset 593) ----
        act(A_IN_MIDAIR); dx(1); dy(-1); f(F.frame_102_start_fall_1)
        dx(2); dy(6); f(F.frame_103_start_fall_2)
        dx(-1); dy(9); f(F.frame_104_start_fall_3)
        dy(12); f(F.frame_105_start_fall_4)
        dx(-2); setFall(1, 15); jmp(L_freefall)

        // ---- stepfall (offset 621) ----
        act(A_IN_MIDAIR); dx(1); dy(3); jmpIfFeather(L_stepfloat)
        // fall1 (offset 630)
        f(F.frame_102_start_fall_1)
        dx(2); dy(6); f(F.frame_103_start_fall_2)
        dx(-1); dy(9); f(F.frame_104_start_fall_3)
        dy(12); f(F.frame_105_start_fall_4)
        dx(-2); setFall(1, 15); jmp(L_freefall)

        // ---- patchfall (offset 652) ----
        dx(-1); dy(-3); jmp(L_fall1)

        // ---- stepfall2 (offset 659) ----
        dx(1); jmp(L_stepfall)

        // ---- stepfloat (offset 664) ----
        f(F.frame_102_start_fall_1)
        dx(2); dy(3); f(F.frame_103_start_fall_2)
        dx(-1); dy(4); f(F.frame_104_start_fall_3)
        dy(5); f(F.frame_105_start_fall_4)
        dx(-2); setFall(1, 6); jmp(L_freefall)

        // ---- jumpfall (offset 686) ----
        act(A_IN_MIDAIR); dx(1); dy(3); f(F.frame_102_start_fall_1)
        dx(2); dy(6); f(F.frame_103_start_fall_2)
        dx(1); dy(9); f(F.frame_104_start_fall_3)
        dx(2); dy(12); f(F.frame_105_start_fall_4)
        setFall(2, 15); jmp(L_freefall)

        // ---- rjumpfall (offset 714) ----
        act(A_IN_MIDAIR); dx(1); dy(3); f(F.frame_102_start_fall_1)
        dx(3); dy(6); f(F.frame_103_start_fall_2)
        dx(2); dy(9); f(F.frame_104_start_fall_3)
        dx(3); dy(12); f(F.frame_105_start_fall_4)
        setFall(3, 15); jmp(L_freefall)

        // ---- jumphangMed (offset 742) ----
        act(A_RUN_JUMP)
        f(F.frame_67_start_jump_up_1, F.frame_68_start_jump_up_2, F.frame_69_start_jump_up_3,
          F.frame_70_jumphang, F.frame_71_jumphang, F.frame_72_jumphang, F.frame_73_jumphang,
          F.frame_74_jumphang, F.frame_75_jumphang, F.frame_76_jumphang, F.frame_77_jumphang)
        act(A_HANG_CLIMB)
        f(F.frame_78_jumphang, F.frame_79_jumphang, F.frame_80_jumphang)
        jmp(L_hang)

        // ---- jumphangLong (offset 763) ----
        act(A_RUN_JUMP)
        f(F.frame_67_start_jump_up_1, F.frame_68_start_jump_up_2, F.frame_69_start_jump_up_3,
          F.frame_70_jumphang, F.frame_71_jumphang, F.frame_72_jumphang, F.frame_73_jumphang,
          F.frame_74_jumphang, F.frame_75_jumphang, F.frame_76_jumphang, F.frame_77_jumphang)
        act(A_HANG_CLIMB); dx(1); f(F.frame_78_jumphang)
        dx(2); f(F.frame_79_jumphang)
        dx(1); f(F.frame_80_jumphang)
        jmp(L_hang)

        // ---- jumpbackhang (offset 790) ----
        act(A_RUN_JUMP)
        f(F.frame_67_start_jump_up_1, F.frame_68_start_jump_up_2, F.frame_69_start_jump_up_3,
          F.frame_70_jumphang, F.frame_71_jumphang, F.frame_72_jumphang, F.frame_73_jumphang,
          F.frame_74_jumphang, F.frame_75_jumphang, F.frame_76_jumphang)
        dx(-1); f(F.frame_77_jumphang)
        act(A_HANG_CLIMB); dx(-2); f(F.frame_78_jumphang)
        dx(-1); f(F.frame_79_jumphang)
        dx(-1); f(F.frame_80_jumphang)
        jmp(L_hang)

        // ---- hang (offset 819) ----
        act(A_HANG_CLIMB); f(F.frame_91_hanging_5)
        // hang1 (offset 822)
        f(F.frame_90_hanging_4, F.frame_89_hanging_3, F.frame_88_hanging_2,
          F.frame_87_hanging_1, F.frame_87_hanging_1, F.frame_87_hanging_1, F.frame_88_hanging_2,
          F.frame_89_hanging_3, F.frame_90_hanging_4, F.frame_91_hanging_5, F.frame_92_hanging_6,
          F.frame_93_hanging_7, F.frame_94_hanging_8, F.frame_95_hanging_9, F.frame_96_hanging_10,
          F.frame_97_hanging_11, F.frame_98_hanging_12, F.frame_99_hanging_13, F.frame_97_hanging_11,
          F.frame_96_hanging_10, F.frame_95_hanging_9, F.frame_94_hanging_8, F.frame_93_hanging_7,
          F.frame_92_hanging_6, F.frame_91_hanging_5, F.frame_90_hanging_4, F.frame_89_hanging_3,
          F.frame_88_hanging_2, F.frame_87_hanging_1, F.frame_88_hanging_2, F.frame_89_hanging_3,
          F.frame_90_hanging_4, F.frame_91_hanging_5, F.frame_92_hanging_6, F.frame_93_hanging_7,
          F.frame_94_hanging_8, F.frame_95_hanging_9, F.frame_96_hanging_10, F.frame_95_hanging_9,
          F.frame_94_hanging_8, F.frame_93_hanging_7, F.frame_92_hanging_6)
        jmp(L_hangdrop)

        // ---- hangstraight (offset 867) ----
        act(A_HANG_STRAIGHT); f(F.frame_92_hanging_6)
        f(F.frame_93_hanging_7, F.frame_93_hanging_7, F.frame_92_hanging_6, F.frame_92_hanging_6)
        // hangstraight_loop (offset 874)
        f(F.frame_91_hanging_5)
        jmp(L_hangstraight_loop)

        // ---- climbfail (offset 878) ----
        f(F.frame_135_climbing_1, F.frame_136_climbing_2, F.frame_137_climbing_3, F.frame_137_climbing_3,
          F.frame_138_climbing_4, F.frame_138_climbing_4, F.frame_138_climbing_4, F.frame_138_climbing_4,
          F.frame_137_climbing_3, F.frame_136_climbing_2, F.frame_135_climbing_1)
        dx(-7); jmp(L_hangdrop)

        // ---- climbdown (offset 894) ----
        act(A_RUN_JUMP); f(F.frame_148_climbing_14)
        f(F.frame_145_climbing_11, F.frame_144_climbing_10, F.frame_143_climbing_9,
          F.frame_142_climbing_8, F.frame_141_climbing_7)
        dx(-5); dy(63); op(SEQ_DOWN); act(A_IN_MIDAIR); f(F.frame_140_climbing_6)
        f(F.frame_138_climbing_4, F.frame_136_climbing_2)
        f(F.frame_91_hanging_5)
        act(A_HANG_CLIMB); jmp(L_hang1)

        // ---- climbup (offset 918) ----
        act(A_RUN_JUMP); f(F.frame_135_climbing_1)
        f(F.frame_136_climbing_2, F.frame_137_climbing_3, F.frame_138_climbing_4,
          F.frame_139_climbing_5, F.frame_140_climbing_6)
        dx(5); dy(-63); op(SEQ_UP); f(F.frame_141_climbing_7)
        f(F.frame_142_climbing_8, F.frame_143_climbing_9, F.frame_144_climbing_10, F.frame_145_climbing_11,
          F.frame_146_climbing_12, F.frame_147_climbing_13, F.frame_148_climbing_14)
        act(A_BUMPED) // to clear flags
        f(F.frame_149_climbing_15)
        act(A_RUN_JUMP); f(F.frame_118_stand_up_from_crouch_9, F.frame_119_stand_up_from_crouch_10)
        dx(1); jmp(L_stand)

        // ---- hangdrop (offset 951) ----
        f(F.frame_81_hangdrop_1, F.frame_82_hangdrop_2)
        act(A_BUMPED); f(F.frame_83_hangdrop_3)
        act(A_RUN_JUMP); op(SEQ_KNOCK_DOWN); snd(SND_SILENT)
        f(F.frame_84_hangdrop_4, F.frame_85_hangdrop_5)
        dx(3); jmp(L_stand)

        // ---- hangfall (offset 968) ----
        act(A_IN_MIDAIR); f(F.frame_81_hangdrop_1)
        dy(6); f(F.frame_81_hangdrop_1)
        dy(9); f(F.frame_81_hangdrop_1)
        dy(12); dx(2); setFall(0, 12); jmp(L_freefall)

        // ---- freefall (offset 987) ----
        act(A_IN_FREEFALL)
        // freefall_loop (offset 989)
        f(F.frame_106_fall)
        jmp(L_freefall_loop)

        // ---- runstop (offset 993) ----
        act(A_RUN_JUMP); f(F.frame_53_runturn)
        dx(2); snd(SND_FOOTSTEP); f(F.frame_54_runturn)
        dx(7); f(F.frame_55_runturn)
        snd(SND_FOOTSTEP); f(F.frame_56_runturn)
        dx(2); f(F.frame_49_turn)
        dx(-2); f(F.frame_50_turn)
        f(F.frame_51_turn, F.frame_52_turn)
        jmp(L_stand)

        // ---- jumpup (offset 1018) ----
        act(A_RUN_JUMP)
        f(F.frame_67_start_jump_up_1, F.frame_68_start_jump_up_2, F.frame_69_start_jump_up_3,
          F.frame_70_jumphang, F.frame_71_jumphang, F.frame_72_jumphang, F.frame_73_jumphang,
          F.frame_74_jumphang, F.frame_75_jumphang, F.frame_76_jumphang, F.frame_77_jumphang,
          F.frame_78_jumphang)
        act(A_STAND); op(SEQ_KNOCK_UP); f(F.frame_79_jumphang)
        jmp(L_hangdrop)

        // ---- highjump (offset 1039) ----
        act(A_RUN_JUMP)
        f(F.frame_67_start_jump_up_1, F.frame_68_start_jump_up_2, F.frame_69_start_jump_up_3,
          F.frame_70_jumphang, F.frame_71_jumphang, F.frame_72_jumphang, F.frame_73_jumphang,
          F.frame_74_jumphang, F.frame_75_jumphang, F.frame_76_jumphang, F.frame_77_jumphang,
          F.frame_78_jumphang, F.frame_79_jumphang)
        dy(-4); f(F.frame_79_jumphang)
        dy(-2); f(F.frame_79_jumphang)
        f(F.frame_79_jumphang)
        dy(2); f(F.frame_79_jumphang)
        dy(4); jmp(L_hangdrop)

        // ---- superhijump (offset 1069) ---- (non-USE_SUPER_HIGH_JUMP branch)
        f(F.frame_67_start_jump_up_1, F.frame_68_start_jump_up_2, F.frame_69_start_jump_up_3,
          F.frame_70_jumphang, F.frame_71_jumphang, F.frame_72_jumphang, F.frame_73_jumphang,
          F.frame_74_jumphang, F.frame_75_jumphang, F.frame_76_jumphang)
        dy(-1); f(F.frame_77_jumphang)
        dy(-3); f(F.frame_78_jumphang)
        dy(-4); f(F.frame_79_jumphang)
        dy(-10); f(F.frame_79_jumphang)
        // #else branch (standard PoP behavior)
        dy(-9); f(F.frame_79_jumphang)
        dy(-8); f(F.frame_79_jumphang)
        dy(-7); f(F.frame_79_jumphang)
        dy(-6); f(F.frame_79_jumphang)
        dy(-5); f(F.frame_79_jumphang)
        // end #else
        dy(-4); f(F.frame_79_jumphang)
        dy(-3); f(F.frame_79_jumphang)
        dy(-2); f(F.frame_79_jumphang)
        dy(-2); f(F.frame_79_jumphang)
        dy(-1); f(F.frame_79_jumphang)
        dy(-1); f(F.frame_79_jumphang)
        dy(-1); f(F.frame_79_jumphang)
        f(F.frame_79_jumphang, F.frame_79_jumphang, F.frame_79_jumphang)
        dy(1); f(F.frame_79_jumphang)
        dy(1); f(F.frame_79_jumphang)
        dy(2); f(F.frame_79_jumphang)
        dy(2); f(F.frame_79_jumphang)
        dy(3); f(F.frame_79_jumphang)
        dy(4); f(F.frame_79_jumphang)
        dy(5); f(F.frame_79_jumphang)
        dy(6); f(F.frame_79_jumphang)
        setFall(0, 6); jmp(L_freefall)

        // ---- fallhang (offset 1160) ----
        act(A_IN_MIDAIR); f(F.frame_80_jumphang)
        jmp(L_hang)

        // ---- bump (offset 1166) ----
        act(A_BUMPED); dx(-4); f(F.frame_50_turn)
        f(F.frame_51_turn, F.frame_52_turn)
        jmp(L_stand)

        // ---- bumpfall (offset 1176) ----
        act(A_BUMPED); dx(1); dy(3); jmpIfFeather(L_bumpfloat)
        f(F.frame_102_start_fall_1)
        dx(2); dy(6); f(F.frame_103_start_fall_2)
        dx(-1); dy(9); f(F.frame_104_start_fall_3)
        dy(12); f(F.frame_105_start_fall_4)
        dx(-2); setFall(0, 15); jmp(L_freefall)

        // ---- bumpfloat (offset 1207) ----
        f(F.frame_102_start_fall_1)
        dx(2); dy(3); f(F.frame_103_start_fall_2)
        dx(-1); dy(4); f(F.frame_104_start_fall_3)
        dy(5); f(F.frame_105_start_fall_4)
        dx(-2); setFall(0, 6); jmp(L_freefall)

        // ---- hardbump (offset 1229) ----
        act(A_BUMPED); dx(-1); dy(-4); f(F.frame_102_start_fall_1)
        dx(-1); dy(3); dx(-3); dy(1); op(SEQ_KNOCK_DOWN)
        dx(1); snd(SND_FOOTSTEP); f(F.frame_107_fall_land_1)
        dx(2); f(F.frame_108_fall_land_2)
        snd(SND_FOOTSTEP); f(F.frame_109_crouch)
        jmp(L_standup)

        // ---- testfoot (offset 1259) ----
        f(F.frame_121_stepping_1)
        dx(1); f(F.frame_122_stepping_2)
        f(F.frame_123_stepping_3)
        dx(2); f(F.frame_124_stepping_4)
        dx(4); f(F.frame_125_stepping_5)
        dx(3); f(F.frame_126_stepping_6)
        dx(-4); f(F.frame_86_test_foot)
        snd(SND_FOOTSTEP); op(SEQ_KNOCK_DOWN); dx(-4); f(F.frame_116_stand_up_from_crouch_7)
        dx(-2); f(F.frame_117_stand_up_from_crouch_8)
        f(F.frame_118_stand_up_from_crouch_9)
        f(F.frame_119_stand_up_from_crouch_10)
        jmp(L_stand)

        // ---- stepback (offset 1290) ----
        dx(-5); jmp(L_stand)

        // ---- step14 (offset 1295) ----
        act(A_RUN_JUMP); f(F.frame_121_stepping_1)
        dx(1); f(F.frame_122_stepping_2)
        dx(1); f(F.frame_123_stepping_3)
        dx(3); f(F.frame_124_stepping_4)
        dx(4); f(F.frame_125_stepping_5)
        dx(3); f(F.frame_126_stepping_6)
        dx(-1); dx(3); f(F.frame_127_stepping_7)
        f(F.frame_128_stepping_8, F.frame_129_stepping_9, F.frame_130_stepping_10,
          F.frame_131_stepping_11, F.frame_132_stepping_12)
        jmp(L_stand)

        // ---- step13 (offset 1326) ----
        act(A_RUN_JUMP); f(F.frame_121_stepping_1)
        dx(1); f(F.frame_122_stepping_2)
        dx(1); f(F.frame_123_stepping_3)
        dx(3); f(F.frame_124_stepping_4)
        dx(4); f(F.frame_125_stepping_5)
        dx(3); f(F.frame_126_stepping_6)
        dx(-1); dx(2); f(F.frame_127_stepping_7)
        f(F.frame_128_stepping_8, F.frame_129_stepping_9, F.frame_130_stepping_10,
          F.frame_131_stepping_11, F.frame_132_stepping_12)
        jmp(L_stand)

        // ---- step12 (offset 1357) ----
        act(A_RUN_JUMP); f(F.frame_121_stepping_1)
        dx(1); f(F.frame_122_stepping_2)
        dx(1); f(F.frame_123_stepping_3)
        dx(3); f(F.frame_124_stepping_4)
        dx(4); f(F.frame_125_stepping_5)
        dx(3); f(F.frame_126_stepping_6)
        dx(-1); dx(1); f(F.frame_127_stepping_7)
        f(F.frame_128_stepping_8, F.frame_129_stepping_9, F.frame_130_stepping_10,
          F.frame_131_stepping_11, F.frame_132_stepping_12)
        jmp(L_stand)

        // ---- step11 (offset 1388) ----
        act(A_RUN_JUMP); f(F.frame_121_stepping_1)
        dx(1); f(F.frame_122_stepping_2)
        dx(1); f(F.frame_123_stepping_3)
        dx(3); f(F.frame_124_stepping_4)
        dx(4); f(F.frame_125_stepping_5)
        dx(3); f(F.frame_126_stepping_6)
        dx(-1); f(F.frame_127_stepping_7)
        f(F.frame_128_stepping_8, F.frame_129_stepping_9, F.frame_130_stepping_10,
          F.frame_131_stepping_11, F.frame_132_stepping_12)
        jmp(L_stand)

        // ---- step10 (offset 1417) ----
        act(A_RUN_JUMP); f(F.frame_121_stepping_1)
        // step10a (offset 1422)
        dx(1); f(F.frame_122_stepping_2)
        dx(1); f(F.frame_123_stepping_3)
        dx(3); f(F.frame_124_stepping_4)
        dx(4); f(F.frame_125_stepping_5)
        dx(3); f(F.frame_126_stepping_6)
        dx(-2); f(F.frame_128_stepping_8)
        f(F.frame_129_stepping_9, F.frame_130_stepping_10, F.frame_131_stepping_11, F.frame_132_stepping_12)
        jmp(L_stand)

        // ---- step9 (offset 1445) ----
        act(A_RUN_JUMP); f(F.frame_121_stepping_1)
        jmp(L_step10a)

        // ---- step8 (offset 1451) ----
        act(A_RUN_JUMP); f(F.frame_121_stepping_1)
        dx(1); f(F.frame_122_stepping_2)
        dx(1); f(F.frame_123_stepping_3)
        dx(3); f(F.frame_124_stepping_4)
        dx(4); f(F.frame_125_stepping_5)
        dx(-1); f(F.frame_127_stepping_7)
        f(F.frame_128_stepping_8, F.frame_129_stepping_9, F.frame_130_stepping_10,
          F.frame_131_stepping_11, F.frame_132_stepping_12)
        jmp(L_stand)

        // ---- step7 (offset 1477) ----
        act(A_RUN_JUMP); f(F.frame_121_stepping_1)
        dx(1); f(F.frame_122_stepping_2)
        dx(1); f(F.frame_123_stepping_3)
        dx(3); f(F.frame_124_stepping_4)
        dx(2); f(F.frame_129_stepping_9)
        f(F.frame_130_stepping_10, F.frame_131_stepping_11, F.frame_132_stepping_12)
        jmp(L_stand)

        // ---- step6 (offset 1498) ----
        act(A_RUN_JUMP); f(F.frame_121_stepping_1)
        dx(1); f(F.frame_122_stepping_2)
        dx(1); f(F.frame_123_stepping_3)
        dx(2); f(F.frame_124_stepping_4)
        dx(2); f(F.frame_129_stepping_9)
        f(F.frame_130_stepping_10, F.frame_131_stepping_11, F.frame_132_stepping_12)
        jmp(L_stand)

        // ---- step5 (offset 1519) ----
        act(A_RUN_JUMP); f(F.frame_121_stepping_1)
        dx(1); f(F.frame_122_stepping_2)
        dx(1); f(F.frame_123_stepping_3)
        dx(2); f(F.frame_124_stepping_4)
        dx(1); f(F.frame_129_stepping_9)
        f(F.frame_130_stepping_10, F.frame_131_stepping_11, F.frame_132_stepping_12)
        jmp(L_stand)

        // ---- step4 (offset 1540) ----
        act(A_RUN_JUMP); f(F.frame_121_stepping_1)
        dx(1); f(F.frame_122_stepping_2)
        dx(1); f(F.frame_123_stepping_3)
        dx(2); f(F.frame_131_stepping_11)
        f(F.frame_132_stepping_12)
        jmp(L_stand)

        // ---- step3 (offset 1556) ----
        act(A_RUN_JUMP); f(F.frame_121_stepping_1)
        dx(1); f(F.frame_122_stepping_2)
        dx(1); f(F.frame_123_stepping_3)
        dx(1); f(F.frame_131_stepping_11)
        f(F.frame_132_stepping_12)
        jmp(L_stand)

        // ---- step2 (offset 1572) ----
        act(A_RUN_JUMP); f(F.frame_121_stepping_1)
        dx(1); f(F.frame_122_stepping_2)
        dx(1); f(F.frame_132_stepping_12)
        jmp(L_stand)

        // ---- step1 (offset 1584) ----
        act(A_RUN_JUMP); f(F.frame_121_stepping_1)
        dx(1); f(F.frame_132_stepping_12)
        jmp(L_stand)

        // ---- stoop (offset 1593) ----
        act(A_RUN_JUMP); dx(1); f(F.frame_107_fall_land_1)
        dx(2); f(F.frame_108_fall_land_2)
        // stoop_crouch (offset 1601)
        f(F.frame_109_crouch)
        jmp(L_stoop_crouch)

        // ---- standup (offset 1605) ----
        act(A_BUMPED); dx(1); f(F.frame_110_stand_up_from_crouch_1)
        f(F.frame_111_stand_up_from_crouch_2)
        dx(2); f(F.frame_112_stand_up_from_crouch_3)
        f(F.frame_113_stand_up_from_crouch_4)
        dx(1); f(F.frame_114_stand_up_from_crouch_5)
        f(F.frame_115_stand_up_from_crouch_6, F.frame_116_stand_up_from_crouch_7)
        dx(-4); f(F.frame_117_stand_up_from_crouch_8)
        f(F.frame_118_stand_up_from_crouch_9, F.frame_119_stand_up_from_crouch_10)
        jmp(L_stand)

        // ---- pickupsword (offset 1628) ----
        act(A_RUN_JUMP); op(SEQ_GET_ITEM); f(1); f(F.frame_229_found_sword)
        f(F.frame_229_found_sword, F.frame_229_found_sword, F.frame_229_found_sword,
          F.frame_229_found_sword, F.frame_229_found_sword)
        f(F.frame_230_sheathe, F.frame_231_sheathe, F.frame_232_sheathe)
        jmp(L_resheathe)

        // ---- resheathe (offset 1644) ----
        act(A_RUN_JUMP); dx(-5); f(F.frame_233_sheathe)
        f(F.frame_234_sheathe, F.frame_235_sheathe, F.frame_236_sheathe, F.frame_237_sheathe,
          F.frame_238_sheathe, F.frame_239_sheathe, F.frame_240_sheathe, F.frame_133_sheathe,
          F.frame_133_sheathe, F.frame_134_sheathe, F.frame_134_sheathe, F.frame_134_sheathe)
        f(F.frame_48_turn)
        dx(1); f(F.frame_49_turn)
        dx(-2); act(A_BUMPED); f(F.frame_50_turn)
        act(A_RUN_JUMP); f(F.frame_51_turn)
        f(F.frame_52_turn)
        jmp(L_stand)

        // ---- fastsheathe (offset 1677) ----
        act(A_RUN_JUMP); dx(-5); f(F.frame_234_sheathe)
        f(F.frame_236_sheathe, F.frame_238_sheathe, F.frame_240_sheathe, F.frame_134_sheathe)
        dx(-1); jmp(L_stand)

        // ---- drinkpotion (offset 1691) ----
        act(A_RUN_JUMP); dx(4); f(F.frame_191_drink)
        f(F.frame_192_drink, F.frame_193_drink, F.frame_194_drink, F.frame_195_drink,
          F.frame_196_drink, F.frame_197_drink)
        snd(SND_DRINK)
        f(F.frame_198_drink, F.frame_199_drink, F.frame_200_drink, F.frame_201_drink,
          F.frame_202_drink, F.frame_203_drink, F.frame_204_drink, F.frame_205_drink,
          F.frame_205_drink, F.frame_205_drink)
        op(SEQ_GET_ITEM); f(1); f(F.frame_205_drink)
        f(F.frame_205_drink, F.frame_201_drink, F.frame_198_drink)
        dx(-4); jmp(L_stand)

        // ---- softland (offset 1725) ----
        act(A_BUMPED); op(SEQ_KNOCK_DOWN); dx(1); f(F.frame_107_fall_land_1)
        dx(2); f(F.frame_108_fall_land_2)
        // softland_crouch (offset 1736)
        act(A_RUN_JUMP); f(F.frame_109_crouch)
        jmp(L_softland_crouch)

        // ---- landrun (offset 1740) ----
        act(A_RUN_JUMP); dy(-2); dx(1); f(F.frame_107_fall_land_1)
        dx(2); f(F.frame_108_fall_land_2)
        f(F.frame_109_crouch)
        dx(1); f(F.frame_110_stand_up_from_crouch_1)
        f(F.frame_111_stand_up_from_crouch_2)
        dx(2); f(F.frame_112_stand_up_from_crouch_3)
        f(F.frame_113_stand_up_from_crouch_4)
        dx(1); dy(1); f(F.frame_114_stand_up_from_crouch_5)
        dy(1); f(F.frame_115_stand_up_from_crouch_6)
        dx(-2); jmp(L_runstt4)

        // ---- medland (offset 1772) ----
        act(A_BUMPED); op(SEQ_KNOCK_DOWN); dy(-2); dx(1); dx(2); f(F.frame_108_fall_land_2)
        f(F.frame_109_crouch, F.frame_109_crouch, F.frame_109_crouch, F.frame_109_crouch,
          F.frame_109_crouch, F.frame_109_crouch, F.frame_109_crouch, F.frame_109_crouch,
          F.frame_109_crouch, F.frame_109_crouch, F.frame_109_crouch, F.frame_109_crouch,
          F.frame_109_crouch, F.frame_109_crouch, F.frame_109_crouch, F.frame_109_crouch,
          F.frame_109_crouch, F.frame_109_crouch, F.frame_109_crouch, F.frame_109_crouch,
          F.frame_109_crouch, F.frame_109_crouch, F.frame_109_crouch, F.frame_109_crouch,
          F.frame_109_crouch, F.frame_109_crouch, F.frame_109_crouch, F.frame_109_crouch,
          F.frame_109_crouch)
        dx(1); f(F.frame_110_stand_up_from_crouch_1)
        f(F.frame_110_stand_up_from_crouch_1, F.frame_110_stand_up_from_crouch_1,
          F.frame_111_stand_up_from_crouch_2)
        dx(2); f(F.frame_112_stand_up_from_crouch_3)
        f(F.frame_113_stand_up_from_crouch_4)
        dx(1); dy(1); f(F.frame_114_stand_up_from_crouch_5)
        dy(1); f(F.frame_115_stand_up_from_crouch_6)
        f(F.frame_116_stand_up_from_crouch_7)
        dx(-4); f(F.frame_117_stand_up_from_crouch_8)
        f(F.frame_118_stand_up_from_crouch_9, F.frame_119_stand_up_from_crouch_10)
        jmp(L_stand)

        // ---- hardland (offset 1838) ----
        act(A_BUMPED); op(SEQ_KNOCK_DOWN); dy(-2); dx(3); f(F.frame_185_dead)
        op(SEQ_DIE)
        // hardland_dead (offset 1847)
        f(F.frame_185_dead)
        jmp(L_hardland_dead)

        // ---- stabkill (offset 1851) ----
        act(A_BUMPED); jmp(L_dropdead)

        // ---- dropdead (offset 1856) ----
        act(A_RUN_JUMP); op(SEQ_DIE); f(F.frame_179_collapse_1)
        f(F.frame_180_collapse_2, F.frame_181_collapse_3, F.frame_182_collapse_4)
        dx(1); f(F.frame_183_collapse_5)
        // dropdead_dead (offset 1868)
        dx(-4); f(F.frame_185_dead)
        jmp(L_dropdead_dead)

        // ---- impale (offset 1872) ----
        act(A_RUN_JUMP); op(SEQ_KNOCK_DOWN); dx(4); f(F.frame_177_spiked)
        op(SEQ_DIE)
        // impale_dead (offset 1879)
        f(F.frame_177_spiked)
        jmp(L_impale_dead)

        // ---- halve (offset 1883) ----
        act(A_RUN_JUMP); f(F.frame_178_chomped)
        op(SEQ_DIE)
        // halve_dead (offset 1887)
        f(F.frame_178_chomped)
        jmp(L_halve_dead)

        // ---- crush (offset 1891) ----
        jmp(L_medland)

        // ---- deadfall (offset 1894) ----
        setFall(0, 0); act(A_IN_FREEFALL)
        // deadfall_loop (offset 1899)
        f(F.frame_185_dead)
        jmp(L_deadfall_loop)

        // ---- climbstairs (offset 1903) ----
        act(A_BUMPED)
        dx(-5); dy(-1); snd(SND_FOOTSTEP); f(F.frame_217_exit_stairs_1)
        f(F.frame_218_exit_stairs_2, F.frame_219_exit_stairs_3)
        dx(1); f(F.frame_220_exit_stairs_4)
        dx(-4); dy(-3); snd(SND_FOOTSTEP); f(F.frame_221_exit_stairs_5)
        dx(-4); dy(-2); f(F.frame_222_exit_stairs_6)
        dx(-2); dy(-3); f(F.frame_223_exit_stairs_7)
        dx(-3); dy(-8); snd(SND_LEVEL); snd(SND_FOOTSTEP); f(F.frame_224_exit_stairs_8)
        dx(-1); dy(-1); f(F.frame_225_exit_stairs_9)
        dx(-3); dy(-4); f(F.frame_226_exit_stairs_10)
        dx(-1); dy(-5); snd(SND_FOOTSTEP); f(F.frame_227_exit_stairs_11)
        dx(-2); dy(-1); f(F.frame_228_exit_stairs_12)
        f(F.frame_0)
        snd(SND_FOOTSTEP); f(F.frame_0, F.frame_0, F.frame_0)
        snd(SND_FOOTSTEP); f(F.frame_0, F.frame_0, F.frame_0)
        snd(SND_FOOTSTEP); f(F.frame_0, F.frame_0, F.frame_0)
        snd(SND_FOOTSTEP); op(SEQ_END_LEVEL)
        // climbstairs_loop (offset 1984)
        f(F.frame_0)
        jmp(L_climbstairs_loop)

        // ---- Vstand (offset 1988) ----
        f(54) // alt2frame_54_Vstand
        jmp(L_Vstand)

        // ---- Vraise (offset 1992) ----
        f(85, 67, 67, 67)
        f(67, 67, 67, 67)
        f(67, 67, 67, 68)
        f(69, 70, 71, 72)
        f(73, 74, 75, 83)
        // Vraise_loop (offset 2013) — wait, let me recount
        // Vraise starts at 1992. 21 bytes of frame data, then Vraise_loop at 2013
        f(84)
        // Vraise_loop (offset 2013)
        f(76)
        jmp(L_Vraise_loop)

        // ---- Vwalk (offset 2017) ----
        dx(1)
        // Vwalk1 (offset 2019)
        f(48)
        // Vwalk2 (offset 2022) — wait, let me check: Vwalk1 at 2019, data = 48(1), dx(2)(2) = 3 bytes → 2022 ✓
        dx(2)
        // Vwalk2 (offset 2022)
        f(49)
        dx(6); f(50)
        dx(1); f(51)
        dx(-1); f(52)
        dx(1); f(53)
        dx(1); jmp(L_Vwalk1)

        // ---- Vstop (offset 2040) ----
        dx(1); f(55)
        f(56)
        jmp(L_Vstand)

        // ---- Vexit (offset 2047) ----
        f(77, 78, 79, 80, 81, 82)
        dx(1); f(54)
        f(54, 54, 54, 54)
        f(54, 57, 58, 59)
        f(60, 61)
        dx(2); f(62)
        dx(-1); f(63)
        dx(-3); f(64)
        f(65)
        dx(-1); f(66)
        op(SEQ_FLIP); dx(16); dx(3); jmp(L_Vwalk2)

        // ---- Pstand (offset 2087) ----
        f(11)
        jmp(L_Pstand)

        // ---- Palert (offset 2091) ----
        f(2, 3, 4, 5, 6, 7, 8, 9)
        op(SEQ_FLIP); dx(8); f(11)
        jmp(L_Pstand)

        // ---- Pstepback (offset 2106) ----
        op(SEQ_FLIP); dx(11); f(12)
        dx(1); f(13)
        dx(1); f(14)
        dx(3); f(15)
        dx(1); f(16)
        // Pstepback_loop (offset 2122)
        f(17)
        jmp(L_Pstepback_loop)

        // ---- Plie (offset 2126) ----
        f(19)
        jmp(L_Plie)

        // ---- Pwaiting (offset 2130) ----
        f(20)
        jmp(L_Pwaiting)

        // ---- Pembrace (offset 2134) ----
        f(21)
        dx(1); f(22)
        f(23, 24)
        dx(1); f(25)
        dx(-3); f(26)
        dx(-2); f(27)
        dx(-4); f(28)
        dx(-3); f(29)
        dx(-2); f(30)
        dx(-3); f(31)
        dx(-1); f(32)
        // Pembrace_loop (offset 2164)
        f(33)
        jmp(L_Pembrace_loop)

        // ---- Pstroke (offset 2168) ----
        f(37)
        jmp(L_Pstroke)

        // ---- Prise (offset 2172) ----
        f(37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47)
        op(SEQ_FLIP); dx(12)
        // Prise_loop (offset 2186)
        f(11)
        jmp(L_Prise_loop)

        // ---- Pcrouch (offset 2190) ----
        f(11, 11)
        op(SEQ_FLIP); dx(13); f(47)
        f(46, 45, 44, 43, 42, 41, 40, 39, 38, 37)
        f(36, 36, 36)
        f(35, 35, 35)
        f(34, 34, 34, 34, 34, 34, 34)
        f(35, 35)
        f(36, 36, 36)
        f(35, 35, 35)
        f(34, 34, 34, 34, 34, 34, 34)
        f(35, 35)
        f(36, 36, 36)
        f(35, 35, 35)
        f(34, 34, 34, 34, 34, 34, 34, 34, 34)
        f(35, 35, 35)
        // Pcrouch_loop (offset 2254)
        f(36)
        jmp(L_Pcrouch_loop)

        // ---- Pslump (offset 2258) ----
        f(1)
        // Pslump_loop (offset 2259)
        f(18)
        jmp(L_Pslump_loop)

        // ---- Mscurry (offset 2263) ----
        act(A_RUN_JUMP)
        // Mscurry1 (offset 2265)
        f(F.frame_186_mouse_1)
        dx(5); f(F.frame_186_mouse_1)
        dx(3); f(F.frame_187_mouse_2)
        dx(4); jmp(L_Mscurry1)

        // ---- Mstop (offset 2277) ----
        f(F.frame_186_mouse_1)
        jmp(L_Mstop)

        // ---- Mraise (offset 2281) ----
        f(F.frame_188_mouse_stand)
        jmp(L_Mraise)

        // ---- Mleave (offset 2285) ----
        act(A_STAND); f(F.frame_186_mouse_1)
        f(F.frame_186_mouse_1, F.frame_186_mouse_1, F.frame_188_mouse_stand, F.frame_188_mouse_stand,
          F.frame_188_mouse_stand, F.frame_188_mouse_stand, F.frame_188_mouse_stand, F.frame_188_mouse_stand,
          F.frame_188_mouse_stand, F.frame_188_mouse_stand)
        op(SEQ_FLIP); dx(8); jmp(L_Mscurry1)

        // ---- Mclimb (offset 2304) ----
        f(F.frame_186_mouse_1, F.frame_186_mouse_1)
        // Mclimb_loop (offset 2306)
        f(F.frame_188_mouse_stand)
        jmp(L_Mclimb_loop)

        // NOTE: USE_TELEPORTS sequences not included (not in base PoP)

        seqtbl = b.toIntArray()
        check(seqtbl.size == SEQTBL_SIZE) {
            "seqtbl size mismatch: expected $SEQTBL_SIZE, got ${seqtbl.size}"
        }

        // Sequence ID to absolute address lookup table
        // Index = sequence ID, value = absolute address (SEQTBL_BASE + label offset)
        fun addr(offset: Int) = SEQTBL_BASE + offset
        seqtblOffsets = intArrayOf(
            0x0000,                     // 0: unused
            addr(L_startrun),           // 1: seq_1_start_run
            addr(L_stand),              // 2: seq_2_stand
            addr(L_standjump),          // 3: seq_3_standing_jump
            addr(L_runjump),            // 4: seq_4_run_jump
            addr(L_turn),              // 5: seq_5_turn
            addr(L_runturn),           // 6: seq_6_run_turn
            addr(L_stepfall),          // 7: seq_7_fall
            addr(L_jumphangMed),       // 8: seq_8_jump_up_and_grab_straight
            addr(L_hang),              // 9: seq_9_grab_while_jumping
            addr(L_climbup),           // 10: seq_10_climb_up
            addr(L_hangdrop),          // 11: seq_11_release_ledge_and_land
            addr(L_freefall),          // 12: (freefall)
            addr(L_runstop),           // 13: seq_13_stop_run
            addr(L_jumpup),            // 14: seq_14_jump_up_into_ceiling
            addr(L_fallhang),          // 15: seq_15_grab_ledge_midair
            addr(L_jumpbackhang),      // 16: seq_16_jump_up_and_grab
            addr(L_softland),          // 17: seq_17_soft_land
            addr(L_jumpfall),          // 18: seq_18_fall_after_standing_jump
            addr(L_stepfall2),         // 19: seq_19_fall
            addr(L_medland),           // 20: seq_20_medium_land
            addr(L_rjumpfall),         // 21: seq_21_fall_after_running_jump
            addr(L_hardland),          // 22: seq_22_crushed
            addr(L_hangfall),          // 23: seq_23_release_ledge_and_fall
            addr(L_jumphangLong),      // 24: seq_24_jump_up_and_grab_forward
            addr(L_hangstraight),      // 25: seq_25_hang_against_wall
            addr(L_rdiveroll),         // 26: seq_26_crouch_while_running
            addr(L_sdiveroll),         // 27: (sdiveroll)
            addr(L_highjump),          // 28: seq_28_jump_up_with_nothing_above
            addr(L_step1),             // 29: seq_29_safe_step_1
            addr(L_step2),             // 30: seq_30_safe_step_2
            addr(L_step3),             // 31: seq_31_safe_step_3
            addr(L_step4),             // 32: seq_32_safe_step_4
            addr(L_step5),             // 33: seq_33_safe_step_5
            addr(L_step6),             // 34: seq_34_safe_step_6
            addr(L_step7),             // 35: seq_35_safe_step_7
            addr(L_step8),             // 36: seq_36_safe_step_8
            addr(L_step9),             // 37: seq_37_safe_step_9
            addr(L_step10),            // 38: seq_38_safe_step_10
            addr(L_step11),            // 39: seq_39_safe_step_11
            addr(L_step12),            // 40: seq_40_safe_step_12
            addr(L_step13),            // 41: seq_41_safe_step_13
            addr(L_step14),            // 42: seq_42_safe_step_14
            addr(L_turnrun),           // 43: seq_43_start_run_after_turn
            addr(L_testfoot),          // 44: seq_44_step_on_edge
            addr(L_bumpfall),          // 45: seq_45_bumpfall
            addr(L_hardbump),          // 46: seq_46_hardbump
            addr(L_bump),              // 47: seq_47_bump
            addr(L_superhijump),       // 48: seq_48_super_high_jump
            addr(L_standup),           // 49: seq_49_stand_up_from_crouch
            addr(L_stoop),             // 50: seq_50_crouch
            addr(L_impale),            // 51: seq_51_spiked
            addr(L_crush),             // 52: seq_52_loose_floor_fell_on_kid
            addr(L_deadfall),          // 53: (deadfall)
            addr(L_halve),             // 54: seq_54_chomped
            addr(L_engarde),           // 55: seq_55_draw_sword
            addr(L_advance),           // 56: seq_56_guard_forward_with_sword
            addr(L_retreat),           // 57: seq_57_back_with_sword
            addr(L_strike),            // 58: seq_58_guard_strike
            addr(L_flee),              // 59: (flee)
            addr(L_turnengarde),       // 60: seq_60_turn_with_sword
            addr(L_striketoblock),     // 61: seq_61_parry_after_strike
            addr(L_readyblock),        // 62: seq_62_parry
            addr(L_landengarde),       // 63: seq_63_guard_active_after_fall
            addr(L_bumpengfwd),        // 64: seq_64_pushed_back_with_sword
            addr(L_bumpengback),       // 65: seq_65_bump_forward_with_sword
            addr(L_blocktostrike),     // 66: seq_66_strike_after_parry
            addr(L_strikeadv),         // 67: (strikeadv)
            addr(L_climbdown),         // 68: seq_68_climb_down
            addr(L_blockedstrike),     // 69: seq_69_attack_was_parried
            addr(L_climbstairs),       // 70: seq_70_go_up_on_level_door
            addr(L_dropdead),          // 71: seq_71_dying
            addr(L_stepback),          // 72: (stepback)
            addr(L_climbfail),         // 73: seq_73_climb_up_to_closed_gate
            addr(L_stabbed),           // 74: seq_74_hit_by_sword
            addr(L_faststrike),        // 75: seq_75_strike
            addr(L_strikeret),         // 76: (strikeret)
            addr(L_alertstand),        // 77: seq_77_guard_stand_inactive
            addr(L_drinkpotion),       // 78: seq_78_drink
            addr(L_crawl),             // 79: seq_79_crouch_hop
            addr(L_alertturn),         // 80: seq_80_stand_flipped
            addr(L_fightfall),         // 81: seq_81_kid_pushed_off_ledge
            addr(L_efightfall),        // 82: seq_82_guard_pushed_off_ledge
            addr(L_efightfallfwd),     // 83: seq_83_guard_fall
            addr(L_running),           // 84: seq_84_run
            addr(L_stabkill),          // 85: seq_85_stabbed_to_death
            addr(L_fastadvance),       // 86: seq_86_forward_with_sword
            addr(L_goalertstand),      // 87: seq_87_guard_become_inactive
            addr(L_arise),             // 88: seq_88_skel_wake_up
            addr(L_turndraw),          // 89: seq_89_turn_draw_sword
            addr(L_guardengarde),      // 90: seq_90_en_garde
            addr(L_pickupsword),       // 91: seq_91_get_sword
            addr(L_resheathe),         // 92: seq_92_put_sword_away
            addr(L_fastsheathe),       // 93: seq_93_put_sword_away_fast
            addr(L_Pstand),            // 94: seq_94_princess_stand_PV1
            addr(L_Vstand),            // 95: seq_95_Jaffar_stand_PV1
            addr(L_Vwalk),             // 96: (Vwalk)
            addr(L_Vstop),             // 97: (Vstop)
            addr(L_Palert),            // 98: (Palert)
            addr(L_Pstepback),         // 99: (Pstepback)
            addr(L_Vexit),             // 100: (Vexit)
            addr(L_Mclimb),            // 101: seq_101_mouse_stands_up
            addr(L_Vraise),            // 102: (Vraise)
            addr(L_Plie),              // 103: seq_103_princess_lying_PV2
            addr(L_patchfall),         // 104: seq_104_start_fall_in_front_of_wall
            addr(L_Mscurry),           // 105: seq_105_mouse_forward
            addr(L_Mstop),             // 106: seq_106_mouse
            addr(L_Mleave),            // 107: seq_107_mouse_stand_up_and_go
            addr(L_Pembrace),          // 108: seq_108_princess_turn_and_hug
            addr(L_Pwaiting),          // 109: seq_109_princess_stand_PV2
            addr(L_Pstroke),           // 110: seq_110_princess_crouching_PV2
            addr(L_Prise),             // 111: seq_111_princess_stand_up_PV2
            addr(L_Pcrouch),           // 112: seq_112_princess_crouch_down_PV2
            addr(L_Pslump),            // 113: (Pslump)
            addr(L_Mraise),            // 114: seq_114_mouse_stand
        )
    }
}
