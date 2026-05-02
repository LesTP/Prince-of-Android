/*
SDLPoP-kotlin, a Kotlin port of SDLPoP (Prince of Persia).
Based on SDLPoP by Dávid Nagy, licensed under GPL v3+.

Module 8: seg006.c → Kotlin
Character physics, tile queries, frame loading, state management.
81 functions, ~2,154 lines of C. Most-called file in Layer 1.

Phase 8a: Constants, frame tables, tile/room queries, character state save/load.
Phase 8b-8d: Remaining functions (physics, falling, combat, player/guard control).
*/

package com.sdlpop.game

import com.sdlpop.game.Tiles as T
import com.sdlpop.game.CharIds as CID
import com.sdlpop.game.Directions as Dir
import com.sdlpop.game.Actions as Act
import com.sdlpop.game.FrameFlags as FF
import com.sdlpop.game.FrameIds as FID
import com.sdlpop.game.SeqIds as Seq
import com.sdlpop.game.SoundIds as Snd
import com.sdlpop.game.SeqtblInstructions as SeqI
import com.sdlpop.game.SeqtblSounds as SeqSnd
import com.sdlpop.game.Control as Ctrl
import com.sdlpop.game.TileGeometry as TG
import com.sdlpop.game.Falling as Fall
import com.sdlpop.game.Colors as Col
import com.sdlpop.game.Chtabs as Cht
import com.sdlpop.game.SwordStatus as Sword

/**
 * seg006 — Character physics, tile queries, frame loading, state management.
 *
 * All functions operate on GameState globals, matching the C original's pattern.
 * External dependencies (functions from other segments) are called via ExternalStubs.
 */
object Seg006 {
    private val gs = GameState
    private val ext = ExternalStubs

    // ========== Constants ==========

    const val SEQTBL_BASE = 0x196E

    // ========== Frame Tables ==========
    // data:0FE0 — frame_table_kid[] (243 entries)
    val frameTableKid: Array<FrameType> = arrayOf(
        FrameType(255, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(0, 0x00 or 0, 1, 0, 0xC0 or 4),
        FrameType(1, 0x00 or 0, 1, 0, 0x40 or 4),
        FrameType(2, 0x00 or 0, 3, 0, 0x40 or 7),
        FrameType(3, 0x00 or 0, 4, 0, 0x40 or 8),
        FrameType(4, 0x00 or 0, 0, 0, 0xE0 or 6),
        FrameType(5, 0x00 or 0, 0, 0, 0x40 or 9),
        FrameType(6, 0x00 or 0, 0, 0, 0x40 or 10),
        FrameType(7, 0x00 or 0, 0, 0, 0xC0 or 5),
        FrameType(8, 0x00 or 0, 0, 0, 0x40 or 4),
        FrameType(9, 0x00 or 0, 0, 0, 0x40 or 7),
        FrameType(10, 0x00 or 0, 0, 0, 0x40 or 11),
        FrameType(11, 0x00 or 0, 0, 0, 0x40 or 3),
        FrameType(12, 0x00 or 0, 0, 0, 0xC0 or 3),
        FrameType(13, 0x00 or 0, 0, 0, 0x40 or 7),
        FrameType(14, 0x00 or 9, 0, 0, 0x40 or 3),
        FrameType(15, 0x00 or 0, 0, 0, 0xC0 or 3),
        FrameType(16, 0x00 or 0, 0, 0, 0x40 or 4),
        FrameType(17, 0x00 or 0, 0, 0, 0x40 or 6),
        FrameType(18, 0x00 or 0, 0, 0, 0x40 or 8),
        FrameType(19, 0x00 or 0, 0, 0, 0x80 or 9),
        FrameType(20, 0x00 or 0, 0, 0, 0x00 or 11),
        FrameType(21, 0x00 or 0, 0, 0, 0x80 or 11),
        FrameType(22, 0x00 or 0, 0, 0, 0x00 or 17),
        FrameType(23, 0x00 or 0, 0, 0, 0x00 or 7),
        FrameType(24, 0x00 or 0, 0, 0, 0x00 or 5),
        FrameType(25, 0x00 or 0, 0, 0, 0xC0 or 1),
        FrameType(26, 0x00 or 0, 0, 0, 0xC0 or 6),
        FrameType(27, 0x00 or 0, 0, 0, 0x40 or 3),
        FrameType(28, 0x00 or 0, 0, 0, 0x40 or 8),
        FrameType(29, 0x00 or 0, 0, 0, 0x40 or 2),
        FrameType(30, 0x00 or 0, 0, 0, 0x40 or 2),
        FrameType(31, 0x00 or 0, 0, 0, 0xC0 or 2),
        FrameType(32, 0x00 or 0, 0, 0, 0xC0 or 2),
        FrameType(33, 0x00 or 0, 0, 0, 0x40 or 3),
        FrameType(34, 0x00 or 0, 0, 0, 0x40 or 8),
        FrameType(35, 0x00 or 0, 0, 0, 0xC0 or 14),
        FrameType(36, 0x00 or 0, 0, 0, 0xC0 or 1),
        FrameType(37, 0x00 or 0, 0, 0, 0x40 or 5),
        FrameType(38, 0x00 or 0, 0, 0, 0x80 or 14),
        FrameType(39, 0x00 or 0, 0, 0, 0x00 or 11),
        FrameType(40, 0x00 or 0, 0, 0, 0x80 or 11),
        FrameType(41, 0x00 or 0, 0, 0, 0x80 or 10),
        FrameType(42, 0x00 or 0, 0, 0, 0x00 or 1),
        FrameType(43, 0x00 or 0, 0, 0, 0xC0 or 4),
        FrameType(44, 0x00 or 0, 0, 0, 0xC0 or 3),
        FrameType(45, 0x00 or 0, 0, 0, 0xC0 or 3),
        FrameType(46, 0x00 or 0, 0, 0, 0xA0 or 5),
        FrameType(47, 0x00 or 0, 0, 0, 0xA0 or 4),
        FrameType(48, 0x00 or 0, 0, 0, 0x60 or 6),
        FrameType(49, 0x00 or 0, 4, 0, 0x60 or 7),
        FrameType(50, 0x00 or 0, 3, 0, 0x60 or 6),
        FrameType(51, 0x00 or 0, 1, 0, 0x40 or 4),
        FrameType(64, 0x00 or 0, 0, 0, 0xC0 or 2),
        FrameType(65, 0x00 or 0, 0, 0, 0x40 or 1),
        FrameType(66, 0x00 or 0, 0, 0, 0x40 or 2),
        FrameType(67, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(68, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(69, 0x00 or 0, 0, 0, 0x80 or 0),
        FrameType(70, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(71, 0x00 or 0, 0, 0, 0x80 or 0),
        FrameType(72, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(73, 0x00 or 0, 0, 0, 0x80 or 0),
        FrameType(74, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(75, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(76, 0x00 or 0, 0, 0, 0x80 or 0),
        FrameType(255, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(80, 0x00 or 0, -2, 0, 0x40 or 1),
        FrameType(81, 0x00 or 0, -2, 0, 0x40 or 1),
        FrameType(82, 0x00 or 0, -1, 0, 0xC0 or 2),
        FrameType(83, 0x00 or 0, -2, 0, 0x40 or 2),
        FrameType(84, 0x00 or 0, -2, 0, 0x40 or 1),
        FrameType(85, 0x00 or 0, -2, 0, 0x40 or 1),
        FrameType(86, 0x00 or 0, -2, 0, 0x40 or 1),
        FrameType(87, 0x00 or 0, -1, 0, 0x00 or 7),
        FrameType(88, 0x00 or 0, -1, 0, 0x00 or 5),
        FrameType(89, 0x00 or 0, 2, 0, 0x00 or 7),
        FrameType(90, 0x00 or 0, 2, 0, 0x00 or 7),
        FrameType(91, 0x00 or 0, 2, -3, 0x00 or 0),
        FrameType(92, 0x00 or 0, 2, -10, 0x00 or 0),
        FrameType(93, 0x00 or 0, 2, -11, 0x80 or 0),
        FrameType(94, 0x00 or 0, 3, -2, 0x40 or 3),
        FrameType(95, 0x00 or 0, 3, 0, 0xC0 or 3),
        FrameType(96, 0x00 or 0, 3, 0, 0xC0 or 3),
        FrameType(97, 0x00 or 0, 3, 0, 0x60 or 3),
        FrameType(98, 0x00 or 0, 4, 0, 0xE0 or 3),
        FrameType(28, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(99, 0x00 or 0, 7, -14, 0x80 or 0),
        FrameType(100, 0x00 or 0, 7, -12, 0x80 or 0),
        FrameType(101, 0x00 or 0, 4, -12, 0x00 or 0),
        FrameType(102, 0x00 or 0, 3, -10, 0x80 or 0),
        FrameType(103, 0x00 or 0, 2, -10, 0x80 or 0),
        FrameType(104, 0x00 or 0, 1, -10, 0x80 or 0),
        FrameType(105, 0x00 or 0, 0, -11, 0x00 or 0),
        FrameType(106, 0x00 or 0, -1, -12, 0x00 or 0),
        FrameType(107, 0x00 or 0, -1, -14, 0x00 or 0),
        FrameType(108, 0x00 or 0, -1, -14, 0x00 or 0),
        FrameType(109, 0x00 or 0, -1, -15, 0x80 or 0),
        FrameType(110, 0x00 or 0, -1, -15, 0x80 or 0),
        FrameType(111, 0x00 or 0, 0, -15, 0x00 or 0),
        FrameType(255, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(255, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(112, 0x00 or 0, 0, 0, 0xC0 or 6),
        FrameType(113, 0x00 or 0, 0, 0, 0x40 or 6),
        FrameType(114, 0x00 or 0, 0, 0, 0xC0 or 5),
        FrameType(115, 0x00 or 0, 0, 0, 0x40 or 5),
        FrameType(116, 0x00 or 0, 0, 0, 0xC0 or 2),
        FrameType(117, 0x00 or 0, 0, 0, 0xC0 or 4),
        FrameType(118, 0x00 or 0, 0, 0, 0xC0 or 5),
        FrameType(119, 0x00 or 0, 0, 0, 0x40 or 6),
        FrameType(120, 0x00 or 0, 0, 0, 0x40 or 7),
        FrameType(121, 0x00 or 0, 0, 0, 0x40 or 7),
        FrameType(122, 0x00 or 0, 0, 0, 0x40 or 9),
        FrameType(123, 0x00 or 0, 0, 0, 0xC0 or 8),
        FrameType(124, 0x00 or 0, 0, 0, 0xC0 or 9),
        FrameType(125, 0x00 or 0, 0, 0, 0x40 or 9),
        FrameType(126, 0x00 or 0, 0, 0, 0x40 or 5),
        FrameType(127, 0x00 or 0, 2, 0, 0x40 or 5),
        FrameType(128, 0x00 or 0, 2, 0, 0xC0 or 5),
        FrameType(129, 0x00 or 0, 0, 0, 0xC0 or 3),
        FrameType(255, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(133, 0x00 or 0, 0, 0, 0x40 or 3),
        FrameType(134, 0x00 or 0, 0, 0, 0xC0 or 4),
        FrameType(135, 0x00 or 0, 0, 0, 0xC0 or 5),
        FrameType(136, 0x00 or 0, 0, 0, 0x40 or 8),
        FrameType(137, 0x00 or 0, 0, 0, 0x60 or 12),
        FrameType(138, 0x00 or 0, 0, 0, 0xE0 or 15),
        FrameType(139, 0x00 or 0, 0, 0, 0x60 or 3),
        FrameType(140, 0x00 or 0, 0, 0, 0xC0 or 3),
        FrameType(141, 0x00 or 0, 0, 0, 0x40 or 3),
        FrameType(142, 0x00 or 0, 0, 0, 0x40 or 3),
        FrameType(143, 0x00 or 0, 0, 0, 0x40 or 4),
        FrameType(144, 0x00 or 0, 0, 0, 0x40 or 4),
        FrameType(172, 0x00 or 0, 0, 1, 0xC0 or 1),
        FrameType(173, 0x00 or 0, 0, 1, 0xC0 or 7),
        FrameType(145, 0x00 or 0, 0, -12, 0x00 or 1),
        FrameType(146, 0x00 or 0, 0, -21, 0x00 or 0),
        FrameType(147, 0x00 or 0, 1, -26, 0x80 or 0),
        FrameType(148, 0x00 or 0, 4, -32, 0x80 or 0),
        FrameType(149, 0x00 or 0, 6, -36, 0x80 or 1),
        FrameType(150, 0x00 or 0, 7, -41, 0x80 or 2),
        FrameType(151, 0x00 or 0, 2, 17, 0x40 or 2),
        FrameType(152, 0x00 or 0, 4, 9, 0xC0 or 4),
        FrameType(153, 0x00 or 0, 4, 5, 0xC0 or 9),
        FrameType(154, 0x00 or 0, 4, 4, 0xC0 or 8),
        FrameType(155, 0x00 or 0, 5, 0, 0x60 or 9),
        FrameType(156, 0x00 or 0, 5, 0, 0xE0 or 9),
        FrameType(157, 0x00 or 0, 5, 0, 0xE0 or 8),
        FrameType(158, 0x00 or 0, 5, 0, 0x60 or 9),
        FrameType(159, 0x00 or 0, 5, 0, 0x60 or 9),
        FrameType(184, 0x00 or 16, 0, 2, 0x80 or 0),
        FrameType(174, 0x00 or 26, 0, 2, 0x80 or 0),
        FrameType(175, 0x00 or 18, 3, 2, 0x00 or 0),
        FrameType(176, 0x00 or 22, 7, 2, 0xC0 or 4),
        FrameType(177, 0x00 or 21, 10, 2, 0x00 or 0),
        FrameType(178, 0x00 or 23, 7, 2, 0x80 or 0),
        FrameType(179, 0x00 or 25, 4, 2, 0x80 or 0),
        FrameType(180, 0x00 or 24, 0, 2, 0xC0 or 14),
        FrameType(181, 0x00 or 15, 0, 2, 0xC0 or 13),
        FrameType(182, 0x00 or 20, 3, 2, 0x00 or 0),
        FrameType(183, 0x00 or 31, 3, 2, 0x00 or 0),
        FrameType(184, 0x00 or 16, 0, 2, 0x80 or 0),
        FrameType(185, 0x00 or 17, 0, 2, 0x80 or 0),
        FrameType(186, 0x00 or 32, 0, 2, 0x00 or 0),
        FrameType(187, 0x00 or 33, 0, 2, 0x80 or 0),
        FrameType(188, 0x00 or 34, 2, 2, 0xC0 or 3),
        FrameType(14, 0x00 or 0, 0, 0, 0x40 or 3),
        FrameType(189, 0x00 or 19, 7, 2, 0x80 or 0),
        FrameType(190, 0x00 or 14, 1, 2, 0x80 or 0),
        FrameType(191, 0x00 or 27, 0, 2, 0x80 or 0),
        FrameType(181, 0x00 or 15, 0, 2, 0xC0 or 13),
        FrameType(181, 0x00 or 15, 0, 2, 0xC0 or 13),
        FrameType(112, 0x00 or 43, 0, 0, 0xC0 or 6),
        FrameType(113, 0x00 or 44, 0, 0, 0x40 or 6),
        FrameType(114, 0x00 or 45, 0, 0, 0xC0 or 5),
        FrameType(115, 0x00 or 46, 0, 0, 0x40 or 5),
        FrameType(114, 0x00 or 0, 0, 0, 0xC0 or 5),
        FrameType(78, 0x00 or 0, 0, 3, 0x80 or 10),
        FrameType(77, 0x00 or 0, 4, 3, 0x80 or 7),
        FrameType(211, 0x00 or 0, 0, 1, 0x40 or 4),
        FrameType(212, 0x00 or 0, 0, 1, 0x40 or 4),
        FrameType(213, 0x00 or 0, 0, 1, 0x40 or 4),
        FrameType(214, 0x00 or 0, 0, 1, 0x40 or 7),
        FrameType(215, 0x00 or 0, 0, 7, 0x40 or 11),
        FrameType(255, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(79, 0x00 or 0, 4, 7, 0x40 or 9),
        FrameType(130, 0x00 or 0, 0, 0, 0x40 or 4),
        FrameType(131, 0x00 or 0, 0, 0, 0x40 or 4),
        FrameType(132, 0x00 or 0, 0, 2, 0x40 or 4),
        FrameType(255, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(255, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(192, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(193, 0x00 or 0, 0, 1, 0x00 or 0),
        FrameType(194, 0x00 or 0, 0, 0, 0x80 or 0),
        FrameType(195, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(196, 0x00 or 0, -1, 0, 0x00 or 0),
        FrameType(197, 0x00 or 0, -1, 0, 0x00 or 0),
        FrameType(198, 0x00 or 0, -1, 0, 0x00 or 0),
        FrameType(199, 0x00 or 0, -4, 0, 0x00 or 0),
        FrameType(200, 0x00 or 0, -4, 0, 0x80 or 0),
        FrameType(201, 0x00 or 0, -4, 0, 0x00 or 0),
        FrameType(202, 0x00 or 0, -4, 0, 0x00 or 0),
        FrameType(203, 0x00 or 0, -4, 0, 0x00 or 0),
        FrameType(204, 0x00 or 0, -4, 0, 0x00 or 0),
        FrameType(205, 0x00 or 0, -5, 0, 0x00 or 0),
        FrameType(206, 0x00 or 0, -5, 0, 0x00 or 0),
        FrameType(255, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(207, 0x00 or 0, 0, 1, 0x40 or 6),
        FrameType(208, 0x00 or 0, 0, 1, 0xC0 or 6),
        FrameType(209, 0x00 or 0, 0, 1, 0xC0 or 8),
        FrameType(210, 0x00 or 0, 0, 1, 0x40 or 10),
        FrameType(255, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(255, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(255, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(255, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(255, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(255, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(52, 0x00 or 0, 0, 0, 0x80 or 0),
        FrameType(53, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(54, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(55, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(56, 0x00 or 0, 0, 0, 0x80 or 0),
        FrameType(57, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(58, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(59, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(60, 0x00 or 0, 0, 0, 0x80 or 0),
        FrameType(61, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(62, 0x00 or 0, 0, 0, 0x80 or 0),
        FrameType(63, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(160, 0x00 or 35, 1, 1, 0xC0 or 3),
        FrameType(161, 0x00 or 36, 0, 1, 0x40 or 9),
        FrameType(162, 0x00 or 37, 0, 1, 0xC0 or 3),
        FrameType(163, 0x00 or 38, 0, 1, 0x40 or 9),
        FrameType(164, 0x00 or 39, 0, 1, 0xC0 or 3),
        FrameType(165, 0x00 or 40, 1, 1, 0x40 or 9),
        FrameType(166, 0x00 or 41, 1, 1, 0x40 or 3),
        FrameType(167, 0x00 or 42, 1, 1, 0xC0 or 9),
        FrameType(168, 0x00 or 0, 4, 1, 0xC0 or 6),
        FrameType(169, 0x00 or 0, 3, 1, 0xC0 or 10),
        FrameType(170, 0x00 or 0, 1, 1, 0x40 or 3),
        FrameType(171, 0x00 or 0, 1, 1, 0xC0 or 8),
    )

    // data:1496 — frame_tbl_guard[] (41 entries)
    val frameTblGuard: Array<FrameType> = arrayOf(
        FrameType(255, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(12, 0xC0 or 13, 2, 1, 0x00 or 0),
        FrameType(2, 0xC0 or 1, 3, 1, 0x00 or 0),
        FrameType(3, 0xC0 or 2, 4, 1, 0x00 or 0),
        FrameType(4, 0xC0 or 3, 7, 1, 0x40 or 4),
        FrameType(5, 0xC0 or 4, 10, 1, 0x00 or 0),
        FrameType(6, 0xC0 or 5, 7, 1, 0x80 or 0),
        FrameType(7, 0xC0 or 6, 4, 1, 0x80 or 0),
        FrameType(8, 0xC0 or 7, 0, 1, 0x80 or 0),
        FrameType(9, 0xC0 or 8, 0, 1, 0xC0 or 13),
        FrameType(10, 0xC0 or 11, 7, 1, 0x80 or 0),
        FrameType(11, 0xC0 or 12, 3, 1, 0x00 or 0),
        FrameType(12, 0xC0 or 13, 2, 1, 0x00 or 0),
        FrameType(13, 0xC0 or 0, 2, 1, 0x00 or 0),
        FrameType(14, 0xC0 or 28, 0, 1, 0x00 or 0),
        FrameType(15, 0xC0 or 29, 0, 1, 0x80 or 0),
        FrameType(16, 0xC0 or 30, 2, 1, 0xC0 or 3),
        FrameType(17, 0xC0 or 9, -1, 1, 0x40 or 8),
        FrameType(18, 0xC0 or 10, 7, 1, 0x80 or 0),
        FrameType(19, 0xC0 or 14, 3, 1, 0x80 or 0),
        FrameType(9, 0xC0 or 8, 0, 1, 0x80 or 0),
        FrameType(20, 0xC0 or 8, 0, 1, 0xC0 or 13),
        FrameType(21, 0xC0 or 8, 0, 1, 0xC0 or 13),
        FrameType(22, 0xC0 or 47, 0, 0, 0xC0 or 6),
        FrameType(23, 0xC0 or 48, 0, 0, 0x40 or 6),
        FrameType(24, 0xC0 or 49, 0, 0, 0xC0 or 5),
        FrameType(24, 0xC0 or 49, 0, 0, 0xC0 or 5),
        FrameType(24, 0xC0 or 49, 0, 0, 0xC0 or 5),
        FrameType(26, 0xC0 or 0, 0, 3, 0x80 or 10),
        FrameType(27, 0xC0 or 0, 4, 4, 0x80 or 7),
        FrameType(28, 0xC0 or 0, -2, 1, 0x40 or 4),
        FrameType(29, 0xC0 or 0, -2, 1, 0x40 or 4),
        FrameType(30, 0xC0 or 0, -2, 1, 0x40 or 4),
        FrameType(31, 0xC0 or 0, -2, 2, 0x40 or 7),
        FrameType(32, 0xC0 or 0, -2, 2, 0x40 or 10),
        FrameType(255, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(33, 0xC0 or 0, 3, 4, 0xC0 or 9),
        FrameType(255, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(255, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(255, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(255, 0x00 or 0, 0, 0, 0x00 or 0),
    )

    // data:1564 — frame_tbl_cuts[] (87 entries)
    val frameTblCuts: Array<FrameType> = arrayOf(
        FrameType(255, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(15, 0x40 or 0, 0, 0, 0x00 or 0),
        FrameType(1, 0x40 or 0, 0, 0, 0x80 or 0),
        FrameType(2, 0x40 or 0, 0, 0, 0x80 or 0),
        FrameType(3, 0x40 or 0, 0, 0, 0x80 or 0),
        FrameType(4, 0x40 or 0, -1, 0, 0x00 or 0),
        FrameType(5, 0x40 or 0, 2, 0, 0x80 or 0),
        FrameType(6, 0x40 or 0, 2, 0, 0x00 or 0),
        FrameType(7, 0x40 or 0, 0, 0, 0x80 or 0),
        FrameType(8, 0x40 or 0, 1, 0, 0x80 or 0),
        FrameType(255, 0x00 or 0, 0, 0, 0x00 or 0),
        FrameType(0, 0x40 or 0, 0, 0, 0x80 or 0),
        FrameType(9, 0x40 or 0, 0, 0, 0x80 or 0),
        FrameType(10, 0x40 or 0, 0, 0, 0x00 or 0),
        FrameType(11, 0x40 or 0, 0, 0, 0x80 or 0),
        FrameType(12, 0x40 or 0, 0, 0, 0x80 or 0),
        FrameType(13, 0x40 or 0, 0, 0, 0x80 or 0),
        FrameType(14, 0x40 or 0, 0, 0, 0x00 or 0),
        FrameType(16, 0x40 or 0, 0, 0, 0x00 or 0),
        FrameType(0, 0x80 or 0, 0, 0, 0x00 or 0),
        FrameType(2, 0x80 or 0, 0, 0, 0x00 or 0),
        FrameType(3, 0x80 or 0, 0, 0, 0x00 or 0),
        FrameType(4, 0x80 or 0, 0, 0, 0x80 or 0),
        FrameType(5, 0x80 or 0, 0, 0, 0x00 or 0),
        FrameType(6, 0x80 or 0, 0, 0, 0x80 or 0),
        FrameType(7, 0x80 or 0, 0, 0, 0x80 or 0),
        FrameType(8, 0x80 or 0, 0, 0, 0x00 or 0),
        FrameType(9, 0x80 or 0, 0, 0, 0x00 or 0),
        FrameType(10, 0x80 or 0, 0, 0, 0x00 or 0),
        FrameType(11, 0x80 or 0, 0, 0, 0x00 or 0),
        FrameType(12, 0x80 or 0, 0, 0, 0x00 or 0),
        FrameType(13, 0x80 or 0, 0, 0, 0x00 or 0),
        FrameType(14, 0x80 or 0, 0, 0, 0x00 or 0),
        FrameType(15, 0x80 or 0, 0, 0, 0x00 or 0),
        FrameType(16, 0x80 or 0, 0, 0, 0x00 or 0),
        FrameType(17, 0x80 or 0, 0, 0, 0x00 or 0),
        FrameType(18, 0x80 or 0, 0, 0, 0x00 or 0),
        FrameType(19, 0x80 or 0, 0, 0, 0x00 or 0),
        FrameType(20, 0x80 or 0, 0, 0, 0x80 or 0),
        FrameType(21, 0x80 or 0, 0, 0, 0x80 or 0),
        FrameType(22, 0x80 or 0, 1, 0, 0x00 or 0),
        FrameType(23, 0x80 or 0, -1, 0, 0x00 or 0),
        FrameType(24, 0x80 or 0, 2, 0, 0x00 or 0),
        FrameType(25, 0x80 or 0, 1, 0, 0x80 or 0),
        FrameType(26, 0x80 or 0, 0, 0, 0x80 or 0),
        FrameType(27, 0x80 or 0, 0, 0, 0x80 or 0),
        FrameType(28, 0x80 or 0, 0, 0, 0x80 or 0),
        FrameType(29, 0x80 or 0, -1, 0, 0x00 or 0),
        FrameType(0, 0x80 or 0, 0, 0, 0x80 or 0),
        FrameType(1, 0x80 or 0, 0, 0, 0x80 or 0),
        FrameType(2, 0x80 or 0, 0, 0, 0x80 or 0),
        FrameType(3, 0x80 or 0, 0, 0, 0x00 or 0),
        FrameType(4, 0x80 or 0, 0, 0, 0x00 or 0),
        FrameType(5, 0x80 or 0, 0, 0, 0x80 or 0),
        FrameType(6, 0x80 or 0, 0, 0, 0x80 or 0),
        FrameType(7, 0x80 or 0, 0, 0, 0x80 or 0),
        FrameType(8, 0x80 or 0, 0, 0, 0x80 or 0),
        FrameType(9, 0x80 or 0, 0, 0, 0x80 or 0),
        FrameType(10, 0x80 or 0, 0, 0, 0x80 or 0),
        FrameType(11, 0x80 or 0, 0, 0, 0x80 or 0),
        FrameType(12, 0x80 or 0, 0, 0, 0x80 or 0),
        FrameType(13, 0x80 or 0, 0, 0, 0x00 or 0),
        FrameType(14, 0x80 or 0, 0, 0, 0x80 or 0),
        FrameType(15, 0x80 or 0, 0, 0, 0x00 or 0),
        FrameType(16, 0x80 or 0, 0, 0, 0x00 or 0),
        FrameType(17, 0x80 or 0, 0, 0, 0x80 or 0),
        FrameType(18, 0x80 or 0, 0, 0, 0x00 or 0),
        FrameType(19, 0x80 or 0, 3, 0, 0x00 or 0),
        FrameType(20, 0x80 or 0, 3, 0, 0x00 or 0),
        FrameType(21, 0x80 or 0, 3, 0, 0x00 or 0),
        FrameType(22, 0x80 or 0, 2, 0, 0x00 or 0),
        FrameType(23, 0x80 or 0, 3, 0, 0x80 or 0),
        FrameType(24, 0x80 or 0, 5, 0, 0x00 or 0),
        FrameType(25, 0x80 or 0, 5, 0, 0x00 or 0),
        FrameType(26, 0x80 or 0, 1, 0, 0x80 or 0),
        FrameType(27, 0x80 or 0, 2, 0, 0x80 or 0),
        FrameType(28, 0x80 or 0, 2, 0, 0x80 or 0),
        FrameType(29, 0x80 or 0, 1, 0, 0x80 or 0),
        FrameType(30, 0x80 or 0, 1, 0, 0x00 or 0),
        FrameType(31, 0x80 or 0, 2, 0, 0x00 or 0),
        FrameType(32, 0x80 or 0, 3, 0, 0x00 or 0),
        FrameType(33, 0x80 or 0, 3, 0, 0x00 or 0),
        FrameType(34, 0x80 or 0, 0, 0, 0x80 or 0),
        FrameType(35, 0x80 or 0, 2, 0, 0x80 or 0),
        FrameType(36, 0x80 or 0, 2, 0, 0x80 or 0),
        FrameType(37, 0x80 or 0, 1, 0, 0x00 or 0),
    )

    // data:1712 — sword_tbl[] (51 entries)
    val swordTbl: Array<SwordTableType> = arrayOf(
        SwordTableType(255, 0, 0),
        SwordTableType(0, 0, -9),
        SwordTableType(5, -9, -29),
        SwordTableType(1, 7, -25),
        SwordTableType(2, 17, -26),
        SwordTableType(6, 7, -14),
        SwordTableType(7, 0, -5),
        SwordTableType(3, 17, -16),
        SwordTableType(4, 16, -19),
        SwordTableType(30, 12, -9),
        SwordTableType(8, 13, -34),
        SwordTableType(9, 7, -25),
        SwordTableType(10, 10, -16),
        SwordTableType(11, 10, -11),
        SwordTableType(12, 22, -21),
        SwordTableType(13, 28, -23),
        SwordTableType(14, 13, -35),
        SwordTableType(15, 0, -38),
        SwordTableType(16, 0, -29),
        SwordTableType(17, 21, -19),
        SwordTableType(18, 14, -23),
        SwordTableType(19, 21, -22),
        SwordTableType(19, 22, -23),
        SwordTableType(17, 7, -13),
        SwordTableType(17, 15, -18),
        SwordTableType(7, 0, -8),
        SwordTableType(1, 7, -27),
        SwordTableType(28, 14, -28),
        SwordTableType(8, 7, -27),
        SwordTableType(4, 6, -23),
        SwordTableType(4, 9, -21),
        SwordTableType(10, 11, -18),
        SwordTableType(13, 24, -23),
        SwordTableType(13, 19, -23),
        SwordTableType(13, 21, -23),
        SwordTableType(20, 7, -32),
        SwordTableType(21, 14, -32),
        SwordTableType(22, 14, -31),
        SwordTableType(23, 14, -29),
        SwordTableType(24, 28, -28),
        SwordTableType(25, 28, -28),
        SwordTableType(26, 21, -25),
        SwordTableType(27, 14, -22),
        SwordTableType(255, 14, -25),
        SwordTableType(255, 21, -25),
        SwordTableType(29, 0, -16),
        SwordTableType(8, 8, -37),
        SwordTableType(31, 14, -24),
        SwordTableType(32, 14, -24),
        SwordTableType(33, 7, -14),
        SwordTableType(8, 8, -37),
    )

    // ========== Tile/Room Query Functions ==========

    // seg006:0006
    fun getTile(room: Int, col: Int, row: Int): Int {
        gs.currRoom = room.toShort()
        gs.tileCol = col.toShort()
        gs.tileRow = row.toShort()
        gs.currRoom = findRoomOfTile().toShort()
        if (gs.currRoom > 0) {
            ext.getRoomAddress(gs.currRoom.toInt())
            gs.currTilepos = gs.tblLine[gs.tileRow.toInt()] + gs.tileCol.toInt()
            gs.currTile2 = gs.currRoomTiles[gs.currTilepos] and 0x1F
        } else {
            gs.currTile2 = gs.custom.levelEdgeHitTile
        }
        return gs.currTile2
    }

    // seg006:005D
    fun findRoomOfTile(): Int {
        while (true) {
            // FIX_CORNER_GRAB: check tile_row < 0 first
            if (gs.tileRow < 0) {
                gs.tileRow = (gs.tileRow + 3).toShort()
                if (gs.currRoom.toInt() != 0) {
                    gs.currRoom = gs.level.roomlinks[gs.currRoom - 1].up.toShort()
                }
                continue
            }
            if (gs.tileCol < 0) {
                gs.tileCol = (gs.tileCol + 10).toShort()
                if (gs.currRoom.toInt() != 0) {
                    gs.currRoom = gs.level.roomlinks[gs.currRoom - 1].left.toShort()
                }
                continue
            }
            if (gs.tileCol >= 10) {
                gs.tileCol = (gs.tileCol - 10).toShort()
                if (gs.currRoom.toInt() != 0) {
                    gs.currRoom = gs.level.roomlinks[gs.currRoom - 1].right.toShort()
                }
                continue
            }
            if (gs.tileRow >= 3) {
                gs.tileRow = (gs.tileRow - 3).toShort()
                if (gs.currRoom.toInt() != 0) {
                    gs.currRoom = gs.level.roomlinks[gs.currRoom - 1].down.toShort()
                }
                continue
            }
            return gs.currRoom.toInt()
        }
    }

    // seg006:00EC
    fun getTilepos(tileCol: Int, tileRow: Int): Int {
        return if (tileRow < 0) {
            -(tileCol + 1)
        } else if (tileRow >= 3 || tileCol >= 10 || tileCol < 0) {
            30
        } else {
            gs.tblLine[tileRow] + tileCol
        }
    }

    // seg006:0124
    fun getTileposNominus(tileCol: Int, tileRow: Int): Int {
        val tilepos = getTilepos(tileCol, tileRow)
        return if (tilepos < 0) 30 else tilepos
    }

    // seg006:0144
    fun loadFramDetCol() {
        loadFrame()
        determineCol()
    }

    // seg006:014D
    fun determineCol() {
        gs.Char.currCol = getTileDivModM7(dxWeight())
    }

    // seg006:015A
    fun loadFrame() {
        val frame = gs.Char.frame
        var addFrame = 0
        when (gs.Char.charid) {
            CID.KID, CID.MOUSE -> {
                getFrameInternal(frameTableKid, frame, "frame_table_kid", frameTableKid.size)
            }
            CID.GUARD, CID.SKELETON -> {
                if (frame in 102 until 107) addFrame = 70
                getFrameInternal(frameTblGuard, frame + addFrame - 149, "frame_tbl_guard", frameTblGuard.size)
            }
            CID.SHADOW -> {
                if (frame < 150 || frame >= 190) {
                    getFrameInternal(frameTableKid, frame, "frame_table_kid", frameTableKid.size)
                } else {
                    getFrameInternal(frameTblGuard, frame + addFrame - 149, "frame_tbl_guard", frameTblGuard.size)
                }
            }
            CID.PRINCESS, CID.VIZIER -> {
                getFrameInternal(frameTblCuts, frame, "frame_tbl_cuts", frameTblCuts.size)
            }
        }
    }

    fun getFrameInternal(frameTable: Array<FrameType>, frame: Int, @Suppress("UNUSED_PARAMETER") name: String, count: Int) {
        if (frame in 0 until count) {
            gs.curFrame = frameTable[frame].copy()
        } else {
            gs.curFrame = FrameType(255, 0, 0, 0, 0)
        }
    }

    // seg006:01F5
    fun dxWeight(): Int {
        val offset = gs.curFrame.dx - (gs.curFrame.flags and FF.WEIGHT_X)
        return charDxForward(offset)
    }

    // seg006:0213
    fun charDxForward(deltaX: Int): Int {
        var dx = deltaX
        if (gs.Char.direction < Dir.RIGHT) {
            dx = -dx
        }
        return dx + gs.Char.x
    }

    // seg006:0234
    fun objDxForward(deltaX: Int): Int {
        var dx = deltaX
        if (gs.objDirection < Dir.RIGHT) {
            dx = -dx
        }
        gs.objX = (gs.objX + dx).toShort()
        return gs.objX.toInt()
    }

    // seg006:03DE
    fun getTileDivModM7(xpos: Int): Int {
        return getTileDivMod(xpos - 7)
    }

    // seg006:03F0
    fun getTileDivMod(xpos: Int): Int {
        var x = xpos - TG.SCREENSPACE_X
        var xl = x % TG.TILE_SIZEX
        var xh = x / TG.TILE_SIZEX
        if (xl < 0) {
            --xh
            xl += TG.TILE_SIZEX
        }

        // DOS overflow simulation for negative xpos
        if (xpos < 0) {
            val bogus = intArrayOf(
                0x02, 0x00, 0x41, 0x00, 0x80, 0x00, 0xBF, 0x00,
                0xFE, 0x00, 0xFF, 0x01, 0x01, 0xFF, 0xC4, 0xFF,
                0x03, 0x00, 0x42, 0x00, 0x81, 0x00, 0xC0, 0x00,
                0xF8, 0xFF, 0x37, 0x00, 0x76, 0x00, 0xB5, 0x00,
                0xF4, 0x00
            )
            if (bogus.size + xpos >= 0) {
                xh = bogus[bogus.size + xpos]
                xl = tileDivTbl[tileDivTbl.size + xpos]
            }
        }

        // DOS overflow simulation for positive overflow
        if (xpos >= 256) {
            val bogus = intArrayOf(
                0xF4, 0x02, 0x10, 0x1E, 0x2C, 0x3A, 0x48, 0x56,
                0x64, 0x72, 0x80, 0x8E, 0x9C, 0xAA, 0xB8, 0xC6,
                0xD4, 0xE2, 0xF0, 0xFE, 0x00, 0x0A, 0x00, 0xFF,
                0x00, 0x00, 0x00, 0x00, 0x0A, 0x0D, 0x00, 0x00,
                0x00, 0x00
            )
            if (xpos - 256 < bogus.size) {
                xh = tileModTbl[xpos - 256]
                xl = bogus[xpos - 256]
            }
        }

        gs.objXl = xl
        return xh
    }

    // data:22A6
    private val tileDivTbl = intArrayOf(
                                            -5,-5,
        -4,-4,-4,-4,-4,-4,-4,-4,-4,-4,-4,-4,-4,-4,
        -3,-3,-3,-3,-3,-3,-3,-3,-3,-3,-3,-3,-3,-3,
        -2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,
         0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
         1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
         2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
         3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
         4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
         5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
         6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
         7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
         8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
         9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9,
        10,10,10,10,10,10,10,10,10,10,10,10,10,10,
        11,11,11,11,11,11,11,11,11,11,11,11,11,11,
        12,12,12,12,12,12,12,12,12,12,12,12,12,12,
        13,13,13,13,13,13,13,13,13,13,13,13,13,13,
        14,14
    )

    // data:23A6
    private val tileModTbl = intArrayOf(
                                              12, 13,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
        0, 1
    )

    // seg006:0433
    fun yToRowMod4(ypos: Int): Int {
        return (ypos + 60) / TG.TILE_SIZEY % 4 - 1
    }

    // seg006:0950 (tile_is_floor)
    fun tileIsFloor(tiletype: Int): Int {
        return when (tiletype) {
            T.EMPTY, T.BIGPILLAR_TOP, T.DOORTOP, T.WALL,
            T.LATTICE_DOWN, T.LATTICE_SMALL, T.LATTICE_LEFT, T.LATTICE_RIGHT -> 0
            else -> 1
        }
    }

    // seg006:0FC3 (wall_type)
    fun wallType(tiletype: Int): Int {
        return when (tiletype) {
            T.GATE, T.DOORTOP_WITH_FLOOR, T.DOORTOP -> 1
            T.MIRROR -> 2
            T.CHOMPER -> 3
            T.WALL -> 4
            else -> 0
        }
    }

    // seg006:0B0C
    fun getTileInfrontofChar(): Int {
        gs.infrontx = gs.dirFront[gs.Char.direction + 1] + gs.Char.currCol
        return getTile(gs.Char.room, gs.infrontx, gs.Char.currRow)
    }

    // seg006:0B30
    fun getTileInfrontof2Char(): Int {
        val direction = gs.dirFront[gs.Char.direction + 1]
        gs.infrontx = (direction shl 1) + gs.Char.currCol
        return getTile(gs.Char.room, gs.infrontx, gs.Char.currRow)
    }

    // seg006:0B66
    fun getTileBehindChar(): Int {
        return getTile(gs.Char.room, gs.dirBehind[gs.Char.direction + 1] + gs.Char.currCol, gs.Char.currRow)
    }

    // seg006:0707
    fun getTileAtChar(): Int {
        return getTile(gs.Char.room, gs.Char.currCol, gs.Char.currRow)
    }

    // seg006:1005
    fun getTileAboveChar(): Int {
        return getTile(gs.Char.room, gs.Char.currCol, gs.Char.currRow - 1)
    }

    // seg006:1020
    fun getTileBehindAboveChar(): Int {
        return getTile(gs.Char.room, gs.dirBehind[gs.Char.direction + 1] + gs.Char.currCol, gs.Char.currRow - 1)
    }

    // seg006:1049
    fun getTileFrontAboveChar(): Int {
        gs.infrontx = gs.dirFront[gs.Char.direction + 1] + gs.Char.currCol
        return getTile(gs.Char.room, gs.infrontx, gs.Char.currRow - 1)
    }

    // seg006:1072
    fun backDeltaX(deltaX: Int): Int {
        return if (gs.Char.direction < Dir.RIGHT) deltaX else -deltaX
    }

    // seg006:0B8A
    fun distanceToEdgeWeight(): Int {
        return distanceToEdge(dxWeight())
    }

    // seg006:0B94
    fun distanceToEdge(xpos: Int): Int {
        getTileDivModM7(xpos)
        val distance = gs.objXl
        return if (gs.Char.direction == Dir.RIGHT) {
            TG.TILE_RIGHTX - distance
        } else {
            distance
        }
    }

    // ========== Character State Save/Load ==========

    // seg006:044F
    fun loadkid() {
        gs.Char.copyFrom(gs.Kid)
    }

    // seg006:0464
    fun savekid() {
        gs.Kid.copyFrom(gs.Char)
    }

    // seg006:0479
    fun loadshad() {
        gs.Char.copyFrom(gs.Guard)
    }

    // seg006:048E
    fun saveshad() {
        gs.Guard.copyFrom(gs.Char)
    }

    // seg006:04A3
    fun loadkidAndOpp() {
        loadkid()
        gs.Opp.copyFrom(gs.Guard)
    }

    // seg006:04BC
    fun savekidAndOpp() {
        savekid()
        gs.Guard.copyFrom(gs.Opp)
    }

    // seg006:04D5
    fun loadshadAndOpp() {
        loadshad()
        gs.Opp.copyFrom(gs.Kid)
    }

    // seg006:04EE
    fun saveshadAndOpp() {
        saveshad()
        gs.Kid.copyFrom(gs.Opp)
    }

    // seg006:0507
    fun resetObjClip() {
        gs.objClipLeft = 0
        gs.objClipTop = 0
        gs.objClipRight = 320
        gs.objClipBottom = 192
    }

    // seg006:051C
    fun xToXhAndXl(xpos: Int): Pair<Int, Int> {
        // FIX_SPRITE_XPOS enabled
        val xh = xpos shr 3
        val xl = xpos and 7
        return Pair(xh, xl)
    }

    // seg006:1654
    fun saveObj() {
        gs.obj2Tilepos = gs.objTilepos
        gs.obj2X = gs.objX
        gs.obj2Y = gs.objY
        gs.obj2Direction = gs.objDirection
        gs.obj2Id = gs.objId
        gs.obj2Chtab = gs.objChtab
        gs.obj2ClipTop = gs.objClipTop
        gs.obj2ClipBottom = gs.objClipBottom
        gs.obj2ClipLeft = gs.objClipLeft
        gs.obj2ClipRight = gs.objClipRight
    }

    // seg006:1691
    fun loadObj() {
        gs.objTilepos = gs.obj2Tilepos
        gs.objX = gs.obj2X
        gs.objY = gs.obj2Y
        gs.objDirection = gs.obj2Direction
        gs.objId = gs.obj2Id
        gs.objChtab = gs.obj2Chtab
        gs.objClipTop = gs.obj2ClipTop
        gs.objClipBottom = gs.obj2ClipBottom
        gs.objClipLeft = gs.obj2ClipLeft
        gs.objClipRight = gs.obj2ClipRight
    }

    // seg006:0E00
    fun releaseArrows(): Int {
        gs.controlBackward = Ctrl.RELEASED
        gs.controlForward = Ctrl.RELEASED
        gs.controlUp = Ctrl.RELEASED
        gs.controlDown = Ctrl.RELEASED
        return 1
    }

    // seg006:0E12
    fun saveCtrl1() {
        gs.ctrl1Forward = gs.controlForward
        gs.ctrl1Backward = gs.controlBackward
        gs.ctrl1Up = gs.controlUp
        gs.ctrl1Down = gs.controlDown
        gs.ctrl1Shift2 = gs.controlShift2
    }

    // seg006:0E31
    fun restCtrl1() {
        gs.controlForward = gs.ctrl1Forward
        gs.controlBackward = gs.ctrl1Backward
        gs.controlUp = gs.ctrl1Up
        gs.controlDown = gs.ctrl1Down
        gs.controlShift2 = gs.ctrl1Shift2
    }

    // seg006:0E8E
    fun clearSavedCtrl() {
        gs.ctrl1Forward = Ctrl.RELEASED
        gs.ctrl1Backward = Ctrl.RELEASED
        gs.ctrl1Up = Ctrl.RELEASED
        gs.ctrl1Down = Ctrl.RELEASED
        gs.ctrl1Shift2 = Ctrl.RELEASED
    }

    // seg006:0EAF
    fun readUserControl() {
        if (gs.controlForward >= Ctrl.RELEASED) {
            if (gs.controlX == Ctrl.HELD_FORWARD) {
                if (gs.controlForward == Ctrl.RELEASED) {
                    gs.controlForward = Ctrl.HELD
                }
            } else {
                gs.controlForward = Ctrl.RELEASED
            }
        }
        if (gs.controlBackward >= Ctrl.RELEASED) {
            if (gs.controlX == Ctrl.HELD_BACKWARD) {
                if (gs.controlBackward == Ctrl.RELEASED) {
                    gs.controlBackward = Ctrl.HELD
                }
            } else {
                gs.controlBackward = Ctrl.RELEASED
            }
        }
        if (gs.controlUp >= Ctrl.RELEASED) {
            if (gs.controlY == Ctrl.HELD_UP) {
                if (gs.controlUp == Ctrl.RELEASED) {
                    gs.controlUp = Ctrl.HELD
                }
            } else {
                gs.controlUp = Ctrl.RELEASED
            }
        }
        if (gs.controlDown >= Ctrl.RELEASED) {
            if (gs.controlY == Ctrl.HELD_DOWN) {
                if (gs.controlDown == Ctrl.RELEASED) {
                    gs.controlDown = Ctrl.HELD
                }
            } else {
                gs.controlDown = Ctrl.RELEASED
            }
        }
        if (gs.controlShift2 >= Ctrl.RELEASED) {
            if (gs.controlShift == Ctrl.HELD) {
                if (gs.controlShift2 == Ctrl.RELEASED) {
                    gs.controlShift2 = Ctrl.HELD
                }
            } else {
                gs.controlShift2 = Ctrl.RELEASED
            }
        }
    }

    // seg006:1634
    fun clearChar() {
        gs.Char.direction = Dir.NONE
        gs.Char.alive = 0
        gs.Char.action = 0
        ext.drawGuardHp(0, gs.guardhpCurr)
        gs.guardhpCurr = 0
    }

    // seg006:189B
    fun incCurrRow() {
        ++gs.Char.currRow
    }

    // ========== Sequence table access helper ==========

    // Reads a byte from the sequence table at absolute address
    fun seqtblRead(addr: Int): Int {
        val index = addr - SEQTBL_BASE
        return if (index in SequenceTable.seqtbl.indices) {
            SequenceTable.seqtbl[index]
        } else {
            0
        }
    }

    // Reads a signed byte from the sequence table
    fun seqtblReadSigned(addr: Int): Int {
        val v = seqtblRead(addr)
        return if (v > 127) v - 256 else v
    }

    // Reads a little-endian word from the sequence table
    fun seqtblReadWord(addr: Int): Int {
        val lo = seqtblRead(addr)
        val hi = seqtblRead(addr + 1)
        return (hi shl 8) or lo
    }

    // seg006:0254
    fun playSeq() {
        while (true) {
            val command = seqtblRead(gs.Char.currSeq)
            gs.Char.currSeq++
            when (command) {
                SeqI.SEQ_DX -> {
                    gs.Char.x = charDxForward(seqtblReadSigned(gs.Char.currSeq)) and 0xFF
                    gs.Char.currSeq++
                }
                SeqI.SEQ_DY -> {
                    gs.Char.y = (gs.Char.y + seqtblReadSigned(gs.Char.currSeq)) and 0xFF
                    gs.Char.currSeq++
                }
                SeqI.SEQ_FLIP -> {
                    gs.Char.direction = gs.Char.direction.inv()
                }
                SeqI.SEQ_JMP_IF_FEATHER -> {
                    if (gs.isFeatherFall == 0) {
                        gs.Char.currSeq += 2
                    } else {
                        gs.Char.currSeq = seqtblReadWord(gs.Char.currSeq)
                    }
                }
                SeqI.SEQ_JMP -> {
                    gs.Char.currSeq = seqtblReadWord(gs.Char.currSeq)
                }
                SeqI.SEQ_UP -> {
                    --gs.Char.currRow
                    ext.startChompers()
                }
                SeqI.SEQ_DOWN -> {
                    incCurrRow()
                    ext.startChompers()
                }
                SeqI.SEQ_ACTION -> {
                    gs.Char.action = seqtblRead(gs.Char.currSeq)
                    gs.Char.currSeq++
                }
                SeqI.SEQ_SET_FALL -> {
                    gs.Char.fallX = seqtblReadSigned(gs.Char.currSeq)
                    gs.Char.currSeq++
                    gs.Char.fallY = seqtblReadSigned(gs.Char.currSeq)
                    gs.Char.currSeq++
                }
                SeqI.SEQ_KNOCK_UP -> {
                    gs.knock = 1
                }
                SeqI.SEQ_KNOCK_DOWN -> {
                    gs.knock = -1
                }
                SeqI.SEQ_SOUND -> {
                    val whichSound = seqtblRead(gs.Char.currSeq)
                    gs.Char.currSeq++
                    when (whichSound) {
                        SeqSnd.SND_SILENT -> {
                            gs.isGuardNotice = 1
                        }
                        SeqSnd.SND_FOOTSTEP -> {
                            ext.playSound(Snd.FOOTSTEP)
                            gs.isGuardNotice = 1
                        }
                        SeqSnd.SND_BUMP -> {
                            ext.playSound(Snd.BUMPED)
                            gs.isGuardNotice = 1
                        }
                        SeqSnd.SND_DRINK -> {
                            ext.playSound(Snd.DRINK)
                        }
                        SeqSnd.SND_LEVEL -> {
                            // USE_REPLAY: don't do end level music in replays
                            if (gs.recording != 0 || gs.replaying != 0) {
                                // skip
                            } else if (gs.isSoundOn != 0) {
                                if (gs.currentLevel == gs.custom.mirrorLevel) {
                                    ext.playSound(Snd.SHADOW_MUSIC)
                                } else if (gs.currentLevel != 13 && gs.currentLevel != 15) {
                                    ext.playSound(Snd.END_LEVEL_MUSIC)
                                }
                            }
                        }
                    }
                }
                SeqI.SEQ_END_LEVEL -> {
                    ++gs.nextLevel
                    // USE_REPLAY: preserve seed
                    gs.keepLastSeed = 1
                    if (gs.replaying != 0 && gs.skippingReplay != 0) {
                        ext.stopSounds()
                    }
                }
                SeqI.SEQ_GET_ITEM -> {
                    val whichItem = seqtblRead(gs.Char.currSeq)
                    gs.Char.currSeq++
                    if (whichItem == 1) {
                        procGetObject()
                    }
                    // USE_TELEPORTS: teleport() for whichItem==2 — not in base PoP
                }
                SeqI.SEQ_DIE -> {
                    // nop
                }
                else -> {
                    // Default: it's a frame number
                    gs.Char.frame = command
                    return
                }
            }
        }
    }

    // ========== Remaining Phase 8a functions ==========

    // seg006:0DDC
    fun flipControlX() {
        gs.controlX = -gs.controlX
        val temp = gs.controlForward
        gs.controlForward = gs.controlBackward
        gs.controlBackward = temp
    }

    // seg006:1463
    fun procGetObject() {
        if (gs.Char.charid != CID.KID || gs.pickupObjType.toInt() == 0) return
        if (gs.pickupObjType.toInt() == -1) {
            gs.haveSword = -1
            ext.playSound(Snd.VICTORY)
            gs.flashColor = Col.BRIGHTYELLOW
            gs.flashTime = 8
        } else {
            when (gs.pickupObjType.toInt()) {
                1 -> { // health
                    if (gs.hitpCurr != gs.hitpMax) {
                        ext.stopSounds()
                        ext.playSound(Snd.SMALL_POTION)
                        gs.hitpDelta = 1
                        gs.flashColor = Col.RED
                        gs.flashTime = 2
                    }
                }
                2 -> { // life
                    ext.stopSounds()
                    ext.playSound(Snd.BIG_POTION)
                    gs.flashColor = Col.RED
                    gs.flashTime = 4
                    ext.addLife()
                }
                3 -> ext.featherFall()  // feather
                4 -> ext.toggleUpside() // invert
                6 -> { // open
                    getTile(8, 0, 0)
                    ext.triggerButton(0, 0, -1)
                }
                5 -> { // hurt
                    ext.stopSounds()
                    ext.playSound(Snd.KID_HURT)
                    if (gs.currentLevel == 15) {
                        gs.hitpDelta = (-((gs.hitpMax + 1) shr 1)).toShort()
                    } else {
                        gs.hitpDelta = -1
                    }
                }
            }
        }
    }

    // seg006:1599
    fun isDead(): Int {
        return if (gs.Char.frame >= FID.frame_177_spiked &&
            (gs.Char.frame <= FID.frame_178_chomped || gs.Char.frame == FID.frame_185_dead)) 1 else 0
    }

    // seg006:10E6
    fun doPickup(objType: Int) {
        gs.pickupObjType = objType.toShort()
        gs.controlShift2 = Ctrl.IGNORE
        gs.currRoomTiles[gs.currTilepos] = T.FLOOR
        gs.currRoomModif[gs.currTilepos] = 0
        gs.redrawHeight = 35
        ext.setWipe(gs.currTilepos, 1)
        ext.setRedrawFull(gs.currTilepos, 1)
    }

    // seg006:13F3
    fun setObjtileAtChar() {
        val charFrame = gs.Char.frame
        val charAction = gs.Char.action
        if (charAction == Act.RUN_JUMP) {
            gs.tileRow = gs.charBottomRow
            gs.tileCol = gs.charColLeft
        } else {
            gs.tileRow = gs.Char.currRow.toShort()
            gs.tileCol = gs.Char.currCol.toShort()
        }
        if ((charFrame >= FID.frame_135_climbing_1 && charFrame < 149) ||
            charAction == Act.HANG_CLIMB ||
            charAction == Act.IN_MIDAIR ||
            charAction == Act.IN_FREEFALL ||
            charAction == Act.HANG_STRAIGHT
        ) {
            gs.tileCol = (gs.tileCol - 1).toShort()
        }
        gs.objTilepos = getTileposNominus(gs.tileCol.toInt(), gs.tileRow.toInt())
    }

    // seg006:0F55
    fun canGrab(): Int {
        val modifier = gs.currRoomModif[gs.currTilepos]
        if (gs.throughTile == T.WALL) return 0
        if (gs.throughTile == T.DOORTOP && gs.Char.direction >= Dir.RIGHT) return 0
        if (tileIsFloor(gs.throughTile) != 0) return 0
        if (gs.currTile2 == T.LOOSE && modifier != 0 && !(gs.custom.looseFloorDelay > 11)) return 0
        if (gs.currTile2 == T.DOORTOP_WITH_FLOOR && gs.Char.direction < Dir.RIGHT) return 0
        if (tileIsFloor(gs.currTile2) == 0) return 0
        return 1
    }

    // seg006:0ABD
    fun canGrabFrontAbove(): Int {
        gs.throughTile = getTileAboveChar()
        getTileFrontAboveChar()
        return canGrab()
    }

    // ========== Phase 8b — Falling, collision, clipping ==========

    // seg006:055C
    fun fallAccel() {
        if (gs.Char.action == Act.IN_FREEFALL) {
            if (gs.isFeatherFall != 0 &&
                (gs.fixes.fixFeatherFallAffectsGuards == 0 || gs.Char.charid == CID.KID)
            ) {
                gs.Char.fallY += Fall.SPEED_ACCEL_FEATHER
                if (gs.Char.fallY > Fall.SPEED_MAX_FEATHER) gs.Char.fallY = Fall.SPEED_MAX_FEATHER
            } else {
                gs.Char.fallY += Fall.SPEED_ACCEL
                if (gs.Char.fallY > Fall.SPEED_MAX) gs.Char.fallY = Fall.SPEED_MAX
            }
        }
    }

    // seg006:05AE
    fun fallSpeed() {
        gs.Char.y = (gs.Char.y + gs.Char.fallY) and 0xFF
        if (gs.Char.action == Act.IN_FREEFALL) {
            gs.Char.x = charDxForward(gs.Char.fallX) and 0xFF
            loadFramDetCol()
        }
    }

    // seg006:0723
    fun setCharCollision() {
        val image = ext.getImage(gs.objChtab, gs.objId)
        if (image == null) {
            gs.charWidthHalf = 0
            gs.charHeight = 0
        } else {
            gs.charWidthHalf = (image.first + 1) / 2
            gs.charHeight = image.second
        }
        gs.charXLeft = (gs.objX / 2 + 58).toShort()
        if (gs.Char.direction >= Dir.RIGHT) {
            gs.charXLeft = (gs.charXLeft - gs.charWidthHalf).toShort()
        }
        gs.charXLeftColl = gs.charXLeft
        gs.charXRight = (gs.charXLeft + gs.charWidthHalf).toShort()
        gs.charXRightColl = gs.charXRight
        gs.charTopY = (gs.objY - gs.charHeight + 1).toShort()
        if (gs.charTopY >= 192) {
            gs.charTopY = 0
        }
        gs.charTopRow = yToRowMod4(gs.charTopY.toInt()).toShort()
        gs.charBottomRow = yToRowMod4(gs.objY).toShort()
        if (gs.charBottomRow.toInt() == -1) {
            gs.charBottomRow = 3
        }
        gs.charColLeft = maxOf(getTileDivMod(gs.charXLeft.toInt()), 0).toShort()
        gs.charColRight = minOf(getTileDivMod(gs.charXRight.toInt()), 9).toShort()
        if (gs.curFrame.flags and FF.THIN != 0) {
            gs.charXLeftColl = (gs.charXLeftColl + 4).toShort()
            gs.charXRightColl = (gs.charXRightColl - 4).toShort()
        }
    }

    // seg006:0815
    fun checkOnFloor() {
        if (gs.curFrame.flags and FF.NEEDS_FLOOR != 0) {
            if (gs.fixes.fixFallingThroughFloorDuringSwordStrike != 0) {
                if (gs.Char.frame == FID.frame_153_strike_3) return
            }
            if (getTileAtChar() == T.WALL) {
                inWall()
            }
            if (tileIsFloor(gs.currTile2) == 0) {
                // Special event: floors appear (level 12)
                if (gs.currentLevel == 12 &&
                    (gs.unitedWithShadow < 0 ||
                        (gs.fixes.fixHiddenFloorsDuringFlashing != 0 && gs.unitedWithShadow > 0)) &&
                    gs.Char.currRow == 0 &&
                    (gs.Char.room == 2 || (gs.Char.room == 13 && gs.tileCol >= 6))
                ) {
                    gs.currRoomTiles[gs.currTilepos] = T.FLOOR
                    ext.setWipe(gs.currTilepos, 1)
                    ext.setRedrawFull(gs.currTilepos, 1)
                    ++gs.currTilepos
                    ext.setWipe(gs.currTilepos, 1)
                    ext.setRedrawFull(gs.currTilepos, 1)
                } else {
                    if (gs.fixes.fixStandOnThinAir != 0 &&
                        gs.Char.frame >= FID.frame_110_stand_up_from_crouch_1 &&
                        gs.Char.frame <= FID.frame_119_stand_up_from_crouch_10
                    ) {
                        val col = getTileDivModM7(dxWeight() + backDeltaX(2))
                        if (tileIsFloor(getTile(gs.Char.room, col, gs.Char.currRow)) != 0) {
                            return
                        }
                    }
                    startFall()
                }
            }
        }
    }

    // seg006:0ACD
    fun inWall() {
        var deltaX = distanceToEdgeWeight()
        if (deltaX >= 8 || getTileInfrontofChar() == T.WALL) {
            deltaX = 6 - deltaX
        } else {
            deltaX += 4
        }
        gs.Char.x = charDxForward(deltaX)
        loadFramDetCol()
        getTileAtChar()
    }

    // seg006:08B9
    fun startFall() {
        val seqId: Int
        val frame = gs.Char.frame
        gs.Char.sword = Sword.SHEATHED
        incCurrRow()
        ext.startChompers()
        gs.fallFrame = frame
        if (frame == FID.frame_9_run) {
            seqId = Seq.seq_7_fall
        } else if (frame == FID.frame_13_run) {
            seqId = Seq.seq_19_fall
        } else if (frame == FID.frame_26_standing_jump_11) {
            seqId = Seq.seq_18_fall_after_standing_jump
        } else if (frame == FID.frame_44_running_jump_5) {
            seqId = Seq.seq_21_fall_after_running_jump
        } else if (frame >= FID.frame_81_hangdrop_1 && frame < 86) {
            seqId = Seq.seq_19_fall
            gs.Char.x = charDxForward(5)
            loadFramDetCol()
        } else if (frame >= 150 && frame < 180) {
            if (gs.Char.charid == CID.GUARD) {
                if (gs.Char.currRow == 3 && gs.Char.currCol == 10) {
                    clearChar()
                    return
                }
                if (gs.Char.fallX < 0) {
                    seqId = Seq.seq_82_guard_pushed_off_ledge
                    if (gs.Char.direction < Dir.RIGHT && distanceToEdgeWeight() <= 7) {
                        gs.Char.x = charDxForward(-5)
                    }
                } else {
                    gs.droppedout = 0
                    seqId = Seq.seq_83_guard_fall
                }
            } else {
                gs.droppedout = 1
                if (gs.Char.direction < Dir.RIGHT && distanceToEdgeWeight() <= 7) {
                    gs.Char.x = charDxForward(-5)
                }
                seqId = Seq.seq_81_kid_pushed_off_ledge
            }
        } else {
            seqId = Seq.seq_7_fall
        }
        ext.seqtblOffsetChar(seqId)
        playSeq()
        loadFramDetCol()
        if (getTileAtChar() == T.WALL) {
            inWall()
            return
        }
        val tile = getTileInfrontofChar()
        if (tile == T.WALL ||
            (gs.fixes.fixRunningJumpThroughTapestry != 0 && gs.Char.direction == Dir.LEFT &&
                (tile == T.DOORTOP || tile == T.DOORTOP_WITH_FLOOR))
        ) {
            if (gs.fallFrame != 44 || distanceToEdgeWeight() >= 6) {
                gs.Char.x = charDxForward(-1)
            } else {
                ext.seqtblOffsetChar(Seq.seq_104_start_fall_in_front_of_wall)
                playSeq()
            }
            loadFramDetCol()
        }
    }

    // seg006:13E6
    fun stuckLower() {
        if (getTileAtChar() == T.STUCK) {
            ++gs.Char.y
        }
    }

    // ========== Phase 8c: Falling, grabbing, damage, objects ==========

    // seg006:05FC
    fun checkAction() {
        val action = gs.Char.action.toShort()
        val frame = gs.Char.frame.toShort()
        // Prince can grab tiles during a jump if Shift and up arrow, but not forward arrow, keys are pressed.
        if (gs.fixes.enableJumpGrab != 0 && action.toInt() == Act.RUN_JUMP &&
            gs.controlShift == Ctrl.HELD && checkGrabRunJump()) {
            return
        }
        // frame 109: crouching
        if (action.toInt() == Act.HANG_STRAIGHT ||
            action.toInt() == Act.BUMPED
        ) {
            if (frame.toInt() == FID.frame_109_crouch ||
                (gs.fixes.fixStandOnThinAir != 0 &&
                    frame >= FID.frame_110_stand_up_from_crouch_1 && frame <= FID.frame_119_stand_up_from_crouch_10) ||
                (gs.fixes.fixDeadFloatingInAir != 0 &&
                    frame >= FID.frame_177_spiked && frame <= FID.frame_185_dead)
            ) {
                checkOnFloor()
            }
        } else if (action.toInt() == Act.IN_FREEFALL) {
            ext.doFall()
        } else if (action.toInt() == Act.IN_MIDAIR) {
            // frame 102..106: start fall + fall
            if (frame >= FID.frame_102_start_fall_1 && frame < FID.frame_106_fall) {
                checkGrab()
            }
        } else if (action.toInt() != Act.HANG_CLIMB) {
            checkOnFloor()
        }
    }

    // seg006:0658
    fun checkSpiked() {
        val frame = gs.Char.frame.toShort()
        if (getTile(gs.Char.room, gs.Char.currCol, gs.Char.currRow) == T.SPIKE) {
            val harmful = ext.isSpikePowerful().toShort()
            // frames 7..14: running
            // frames 34..39: start run-jump
            // frame 43: land from run-jump
            // frame 26: land from standing jump
            if (
                (harmful >= 2 && ((frame >= FID.frame_7_run && frame < 15) || (frame >= FID.frame_34_start_run_jump_1 && frame < 40))) ||
                ((frame.toInt() == FID.frame_43_running_jump_4 || frame.toInt() == FID.frame_26_standing_jump_11) && harmful != 0.toShort())
            ) {
                ext.spiked()
            }
        }
    }

    // seg006:06BD
    fun takeHp(count: Int): Int {
        var dead = 0
        if (gs.Char.charid == CID.KID) {
            if (count >= gs.hitpCurr) {
                gs.hitpDelta = (-gs.hitpCurr).toShort()
                dead = 1
            } else {
                gs.hitpDelta = (-count).toShort()
            }
        } else {
            if (count >= gs.guardhpCurr) {
                gs.guardhpDelta = (-gs.guardhpCurr).toShort()
                dead = 1
            } else {
                gs.guardhpDelta = (-count).toShort()
            }
        }
        return dead
    }

    // seg006:0941
    fun checkGrab() {
        val maxGrabFallingSpeed = if (gs.fixes.fixGrabFallingSpeed != 0) 30 else 32

        if ((gs.controlShift == Ctrl.HELD ||
            (gs.fixes.enableSuperHighJump != 0 && gs.superJumpFall != 0 && gs.controlY == Ctrl.HELD_UP)) &&
            gs.Char.fallY < maxGrabFallingSpeed &&
            gs.Char.alive < 0 &&
            (gs.yLand[gs.Char.currRow + 1].toInt() and 0xFFFF) <= ((gs.Char.y + 25) and 0xFFFF)
        ) {
            val oldX = gs.Char.x
            val superDeltaX = if (gs.Char.direction == Dir.LEFT) 3 else 4
            gs.Char.x = charDxForward(-8 +
                if (gs.fixes.enableSuperHighJump != 0 && gs.superJumpFall != 0) superDeltaX else 0)
            loadFramDetCol()
            if (canGrabFrontAbove() == 0) {
                gs.Char.x = oldX
            } else {
                gs.Char.x = charDxForward(distanceToEdgeWeight() -
                    if (gs.fixes.enableSuperHighJump != 0 && gs.superJumpFall != 0) superDeltaX else 0)
                gs.Char.y = gs.yLand[gs.Char.currRow + 1].toInt()
                gs.Char.fallY = 0
                ext.seqtblOffsetChar(Seq.seq_15_grab_ledge_midair)
                playSeq()
                gs.grabTimer = 12
                ext.playSound(Snd.GRAB)
                gs.isScreaming = 0
                if (gs.fixes.fixChompersNotStarting != 0) ext.startChompers()
            }
        }
    }

    // USE_JUMP_GRAB
    fun checkGrabRunJump(): Boolean {
        val frame = gs.Char.frame
        val isJump = frame >= FID.frame_22_standing_jump_7 && frame <= FID.frame_23_standing_jump_8
        val isRunningJump = frame >= FID.frame_39_start_run_jump_6 && frame <= FID.frame_41_running_jump_2
        val charRoomM1 = gs.Char.room - 1
        if (gs.Char.action == Act.RUN_JUMP &&
            (isJump || isRunningJump) &&
            gs.controlX == Ctrl.RELEASED && gs.controlY == Ctrl.HELD_UP
        ) {
            if (canGrabFrontAbove() != 0) {
                val grabTile = gs.currTile2
                var grabCol = gs.tileCol.toInt()
                // Prince's and tile rooms can get out of sync at the edge of a room
                if (gs.currRoom.toInt() != gs.Char.room) {
                    val leftRoom = gs.level.roomlinks[charRoomM1].left
                    val rightRoom = gs.level.roomlinks[charRoomM1].right
                    val upRoom = gs.level.roomlinks[charRoomM1].up
                    if (gs.currRoom.toInt() == rightRoom) {
                        grabCol += 10
                    } else if (gs.currRoom.toInt() == leftRoom) {
                        grabCol -= 10
                    } else if (rightRoom != 0 && gs.currRoom.toInt() == gs.level.roomlinks[rightRoom - 1].up) {
                        grabCol += 10
                    } else if (leftRoom != 0 && gs.currRoom.toInt() == gs.level.roomlinks[leftRoom - 1].up) {
                        grabCol -= 10
                    } else if (upRoom != 0 && gs.currRoom.toInt() == gs.level.roomlinks[upRoom - 1].right) {
                        grabCol += 10
                    } else if (upRoom != 0 && gs.currRoom.toInt() == gs.level.roomlinks[upRoom - 1].left) {
                        grabCol -= 10
                    }
                }
                gs.Char.x = gs.xBump[grabCol + TG.FIRST_ONSCREEN_COLUMN] + TG.TILE_MIDX
                gs.Char.x = charDxForward(if (gs.Char.direction == Dir.LEFT) -12 else 2)
                gs.Char.y = gs.yLand[gs.Char.currRow + 1].toInt()
                ext.seqtblOffsetChar(Seq.seq_9_grab_while_jumping)
                playSeq()
                gs.grabTimer = 12
                ext.playSound(Snd.GRAB)
                // check_press() is not going to work on the next frame if Shift is released immediately
                if (grabTile == T.OPENER || grabTile == T.CLOSER) {
                    ext.triggerButton(1, 0, -1)
                } else if (grabTile == T.LOOSE) {
                    gs.isGuardNotice = 1
                    ext.makeLooseFall(1)
                }
                return true
            }
        }
        return false
    }

    // seg006:0BC4
    fun fellOut() {
        if (gs.Char.alive < 0 && gs.Char.room == 0) {
            takeHp(100)
            gs.Char.alive = 0
            ext.eraseBottomText(1)
            gs.Char.frame = FID.frame_185_dead
        }
    }

    // seg006:1199
    fun checkSpikeBelow() {
        val rightCol = getTileDivModM7(gs.charXRight.toInt())
        if (rightCol < 0) return
        var row: Int
        val room = gs.Char.room
        var col = getTileDivModM7(gs.charXLeft.toInt())
        while (col <= rightCol) {
            row = gs.Char.currRow
            var notFinished: Boolean
            do {
                notFinished = false
                if (getTile(room, col, row) == T.SPIKE) {
                    ext.startAnimSpike(gs.currRoom.toInt(), gs.currTilepos)
                } else if (
                    tileIsFloor(gs.currTile2) == 0 &&
                    gs.currRoom.toInt() != 0 &&
                    (if (gs.fixes.fixInfiniteDownBug != 0) (row <= 2) else (room == gs.currRoom.toInt()))
                ) {
                    ++row
                    notFinished = true
                }
            } while (notFinished)
            ++col
        }
    }

    // seg006:10E9
    fun checkPress() {
        val frame = gs.Char.frame
        val action = gs.Char.action
        // frames 87..99: hanging
        // frames 135..140: start climb up
        if ((frame >= FID.frame_87_hanging_1 && frame < 100) || (frame >= FID.frame_135_climbing_1 && frame < FID.frame_141_climbing_7)) {
            // the pressed tile is the one that the char is grabbing
            getTileAboveChar()
        } else if (action == Act.TURN || action == Act.BUMPED || action < Act.HANG_CLIMB) {
            // frame 79: jumping up
            if (frame == FID.frame_79_jumphang && getTileAboveChar() == T.LOOSE) {
                // break a loose floor from above
                ext.makeLooseFall(1)
            } else {
                // the pressed tile is the one that the char is standing on
                if ((gs.curFrame.flags and FF.NEEDS_FLOOR) == 0) return
                if (gs.fixes.fixPressThroughClosedGates != 0) determineCol()
                getTileAtChar()
            }
        } else {
            return
        }
        if (gs.currTile2 == T.OPENER || gs.currTile2 == T.CLOSER) {
            if (gs.Char.alive < 0) {
                ext.triggerButton(1, 0, -1)
            } else {
                ext.diedOnButton()
            }
        } else if (gs.currTile2 == T.LOOSE) {
            gs.isGuardNotice = 1
            ext.makeLooseFall(1)
        }
    }

    // seg006:15B8
    fun playDeathMusic() {
        val soundId: Int
        if (gs.Guard.charid == CID.SHADOW) {
            soundId = Snd.SHADOW_MUSIC // killed by shadow
        } else if (gs.holdingSword != 0) {
            soundId = Snd.DEATH_IN_FIGHT // death in fight
        } else {
            soundId = Snd.DEATH_REGULAR // death not in fight
        }
        ext.playSound(soundId)
    }

    // seg006:15E8
    fun onGuardKilled() {
        if (gs.currentLevel == 0) {
            // demo level: after killing Guard, run out of room
            gs.checkpoint = 1
            gs.demoIndex = 0
            gs.demoTime = 0
        } else if (gs.currentLevel == gs.custom.jaffarVictoryLevel) {
            // Jaffar's level: flash
            gs.flashColor = Col.BRIGHTWHITE
            gs.flashTime = gs.custom.jaffarVictoryFlashTime
            gs.isShowTime = 1
            gs.leveldoorOpen = 2
            ext.playSound(Snd.VICTORY_JAFFAR)
        } else if (gs.Char.charid != CID.SHADOW) {
            ext.playSound(Snd.VICTORY)
        }
    }

    // seg006:16CE
    fun drawHurtSplash() {
        val frame = gs.Char.frame
        if (frame != FID.frame_178_chomped) {
            saveObj()
            gs.objTilepos = -1
            // frame 185: dead
            // frame 106..110: fall + land
            if (frame == FID.frame_185_dead || (frame >= FID.frame_106_fall && frame < 111)) {
                gs.objY += 4
                objDxForward(5)
            } else if (frame == FID.frame_177_spiked) {
                objDxForward(-5)
            } else {
                gs.objY -= (if (gs.Char.charid == CID.KID) 1 else 0).shl(2) + 11
                objDxForward(5)
            }
            if (gs.Char.charid == CID.KID) {
                gs.objChtab = Cht.KID
                gs.objId = 218 // splash!
            } else {
                gs.objChtab = Cht.GUARD
                gs.objId = 1 // splash!
            }
            resetObjClip()
            ext.addObjtable(5) // hurt splash
            loadObj()
        }
    }

    // seg006:175D
    fun checkKilledShadow() {
        // Special event: killed the shadow
        if (gs.currentLevel == 12) {
            if ((gs.Char.charid or gs.Opp.charid) == CID.SHADOW &&
                gs.Char.alive < 0 && gs.Opp.alive >= 0
            ) {
                gs.flashColor = Col.BRIGHTWHITE
                gs.flashTime = 5
                takeHp(100)
            }
        }
    }

    // seg006:1798
    fun addSwordToObjtable() {
        val frame = gs.Char.frame
        if ((frame >= FID.frame_229_found_sword && frame < 238) ||
            gs.Char.sword != Sword.SHEATHED ||
            (gs.Char.charid == CID.GUARD && gs.Char.alive < 0)
        ) {
            val swordFrame = gs.curFrame.sword and 0x3F
            if (swordFrame != 0) {
                gs.objId = swordTbl[swordFrame].id
                if (gs.objId != 0xFF) {
                    gs.objX = ext.calcScreenXCoord(gs.objX)
                    objDxForward(swordTbl[swordFrame].x)
                    gs.objY += swordTbl[swordFrame].y
                    gs.objChtab = Cht.SWORD
                    ext.addObjtable(3) // sword
                }
            }
        }
    }

    // seg006:0E50
    fun clipChar() {
        val frame = gs.Char.frame
        val action = gs.Char.action
        val room = gs.Char.room
        val row = gs.Char.currRow
        resetObjClip()
        // frames 224..228: going up the level door
        if (frame >= FID.frame_224_exit_stairs_8 && frame < 229) {
            gs.objClipTop = (gs.leveldoorYbottom + 1).toShort()
            gs.objClipRight = gs.leveldoorRight.toShort()
        } else {
            if (getTile(room, gs.charColLeft.toInt(), gs.charTopRow.toInt()) == T.WALL ||
                tileIsFloor(gs.currTile2) != 0
            ) {
                if ((action == Act.STAND && (frame == FID.frame_79_jumphang || frame == FID.frame_81_hangdrop_1)) ||
                    getTile(room, gs.charColRight.toInt(), gs.charTopRow.toInt()) == T.WALL ||
                    tileIsFloor(gs.currTile2) != 0
                ) {
                    val clipRow = row + 1
                    val clipY = gs.yClip[clipRow]
                    if (clipRow == 1 ||
                        (clipY < gs.objY && clipY - 15 < gs.charTopY)
                    ) {
                        gs.charTopY = clipY
                        gs.objClipTop = clipY
                    }
                }
            }
            var col = getTileDivMod(gs.charXLeftColl - 4)
            if (getTile(room, col + 1, row) == T.DOORTOP_WITH_FLOOR ||
                gs.currTile2 == T.DOORTOP
            ) {
                gs.objClipRight = ((gs.tileCol.toInt() shl 5) + 32).toShort()
            } else {
                if ((getTile(room, col, row) != T.DOORTOP_WITH_FLOOR &&
                    gs.currTile2 != T.DOORTOP) ||
                    action == Act.IN_MIDAIR ||
                    (action == Act.IN_FREEFALL && frame == FID.frame_106_fall) ||
                    (action == Act.BUMPED && frame == FID.frame_107_fall_land_1) ||
                    (gs.Char.direction < Dir.RIGHT && (
                        action == Act.HANG_CLIMB ||
                        action == Act.HANG_STRAIGHT ||
                        (action == Act.RUN_JUMP &&
                            frame >= FID.frame_137_climbing_3 && frame < FID.frame_140_climbing_6)
                    ))
                ) {
                    col = getTileDivMod(gs.charXRightColl.toInt())
                    if ((getTile(room, col, row) == T.WALL ||
                        (gs.currTile2 == T.MIRROR && gs.Char.direction == Dir.RIGHT)) &&
                        (getTile(room, col, gs.charTopRow.toInt()) == T.WALL ||
                            gs.currTile2 == T.MIRROR) &&
                        room == gs.currRoom.toInt()
                    ) {
                        gs.objClipRight = (gs.tileCol.toInt() shl 5).toShort()
                    }
                } else {
                    gs.objClipRight = ((gs.tileCol.toInt() shl 5) + 32).toShort()
                }
            }
        }
    }

    // ========== Phase 8d — Player/guard control, integration ==========

    // seg006:0DC0
    fun userControl() {
        if (gs.Char.direction >= Dir.RIGHT) {
            flipControlX()
            ext.control()
            flipControlX()
        } else {
            ext.control()
        }
    }

    // seg006:0D49
    fun doDemo() {
        if (gs.checkpoint != 0) {
            gs.controlShift2 = releaseArrows()
            gs.controlForward = Ctrl.HELD
            gs.controlX = Ctrl.HELD_FORWARD
        } else if (gs.Char.sword != 0) {
            gs.guardSkill = 10
            ext.autocontrolOpponent()
            gs.guardSkill = 11
        } else {
            ext.doAutoMoves(gs.custom.demoMoves)
        }
    }

    // seg006:0CD1
    fun controlKid() {
        if (gs.Char.alive < 0 && gs.hitpCurr == 0) {
            gs.Char.alive = 0
            // stop feather fall when kid dies
            if (gs.fixes.fixQuicksaveDuringFeather != 0 && gs.isFeatherFall > 0) {
                gs.isFeatherFall = 0
                if (ext.checkSoundPlaying() != 0) {
                    ext.stopSounds()
                }
            }
        }
        if (gs.grabTimer != 0) {
            --gs.grabTimer
        }
        // USE_REPLAY: replaying check included
        if (gs.currentLevel == 0 && gs.playDemoLevel == 0 && gs.replaying == 0) {
            doDemo()
            ext.control()
            // The player can start a new game or load a saved game during the demo.
            val key = ext.keyTestQuit()
            if (key == (SDL_SCANCODE_L or WITH_CTRL)) { // Ctrl+L
                if (ext.loadGame() != 0) {
                    ext.startGame()
                }
            } else {
                if (key != 0) {
                    gs.startLevel = gs.custom.firstLevel.toShort() // 1
                    ext.startGame()
                }
            }
        } else {
            restCtrl1()
            ext.doPaused()
            // USE_REPLAY
            if (gs.recording != 0) ext.addReplayMove()
            if (gs.replaying != 0) ext.doReplayMove()
            readUserControl()
            userControl()
            saveCtrl1()
        }
    }

    // seg006:0BEE
    fun playKid() {
        fellOut()
        controlKid()
        if (gs.Char.alive >= 0 && isDead() != 0) {
            if (gs.resurrectTime != 0) {
                ext.stopSounds()
                loadkid()
                gs.hitpDelta = gs.hitpMax.toShort()
                ext.seqtblOffsetChar(Seq.seq_2_stand) // stand
                gs.Char.x += 8
                playSeq()
                loadFramDetCol()
                ext.setStartPos()
            }
            if (ext.checkSoundPlaying() != 0 && gs.currentSound != Snd.GATE_OPENING) {
                return
            }
            gs.isShowTime = 0
            if (gs.Char.alive < 0 || gs.Char.alive >= 6) {
                if (gs.Char.alive == 6) {
                    if (gs.isSoundOn != 0 &&
                        gs.currentLevel != 0 && // no death music on demo level
                        gs.currentLevel != 15   // no death music on potions level
                    ) {
                        playDeathMusic()
                    }
                } else {
                    if (gs.Char.alive != 7 || ext.checkSoundPlaying() != 0) return
                    if (gs.remMin.toInt() == 0) {
                        ext.expired()
                    }
                    if (gs.currentLevel != 0 && // no message if died on demo level
                        gs.currentLevel != 15   // no message if died on potions level
                    ) {
                        gs.textTimeRemaining = 288
                        gs.textTimeTotal = 288
                        ext.displayTextBottom("Press Button to Continue")
                    } else {
                        gs.textTimeRemaining = 36
                        gs.textTimeTotal = 36
                    }
                }
            }
            ++gs.Char.alive
        }
    }

    // seg006:0D85
    fun playGuard() {
        if (gs.Char.charid == CID.MOUSE) {
            ext.autocontrolOpponent()
        } else {
            if (gs.Char.alive < 0) {
                if (gs.guardhpCurr == 0) {
                    gs.Char.alive = 0
                    onGuardKilled()
                } else {
                    // goto loc_7A65
                    ext.autocontrolOpponent()
                    ext.control()
                    return
                }
            }
            if (gs.Char.charid == CID.SHADOW) {
                clearChar()
            }
            ext.autocontrolOpponent()
            ext.control()
        }
    }

    // seg006:1827
    fun controlGuardInactive() {
        if (gs.Char.frame == FID.frame_166_stand_inactive && gs.controlDown == Ctrl.HELD) {
            if (gs.controlForward == Ctrl.HELD) {
                ext.drawSword()
            } else {
                gs.controlDown = Ctrl.IGNORE
                ext.seqtblOffsetChar(Seq.seq_80_stand_flipped) // stand flipped
            }
        }
    }

    // seg006:1852
    fun charOppDist(): Int {
        // >0 if Opp is in front of char
        // <0 if Opp is behind char
        if (gs.Char.room != gs.Opp.room) {
            return 999
        }
        var distance: Int = gs.Opp.x - gs.Char.x
        if (gs.Char.direction < Dir.RIGHT) {
            distance = -distance
        }
        if (distance >= 0 && gs.Char.direction != gs.Opp.direction) {
            distance += 13
        }
        return distance
    }

    // SDL scancode constants used by control_kid for demo-level key handling
    const val SDL_SCANCODE_L = 0x0F
    const val WITH_CTRL = 0x8000
}
