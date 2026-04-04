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
                    gs.Char.x = charDxForward(seqtblReadSigned(gs.Char.currSeq))
                    gs.Char.currSeq++
                }
                SeqI.SEQ_DY -> {
                    gs.Char.y += seqtblReadSigned(gs.Char.currSeq)
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
}
