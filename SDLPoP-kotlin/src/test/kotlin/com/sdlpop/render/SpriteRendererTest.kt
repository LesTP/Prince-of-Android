package com.sdlpop.render

import com.sdlpop.game.Blitters
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class SpriteRendererTest {
    @Test
    fun `no transparency overwrites every source pixel`() {
        val renderer = SpriteRenderer(3, 2)
        renderer.clear(BLUE)

        renderer.drawSprite(1, 0, intArrayOf(RED, TRANSPARENT, GREEN, WHITE), 2, 2, Blitters.NO_TRANSP)

        assertContentEquals(
            intArrayOf(
                BLUE, RED, TRANSPARENT,
                BLUE, GREEN, WHITE
            ),
            renderer.targetPixels
        )
    }

    @Test
    fun `or and transp skip transparent source pixels`() {
        val orRenderer = SpriteRenderer(2, 2)
        orRenderer.clear(BLUE)
        orRenderer.drawSprite(0, 0, SPRITE_WITH_TRANSPARENCY, 2, 2, Blitters.OR)

        val transpRenderer = SpriteRenderer(2, 2)
        transpRenderer.clear(BLUE)
        transpRenderer.drawSprite(0, 0, SPRITE_WITH_TRANSPARENCY, 2, 2, Blitters.TRANSP)

        val expected = intArrayOf(RED, BLUE, GREEN, WHITE)
        assertContentEquals(expected, orRenderer.targetPixels)
        assertContentEquals(expected, transpRenderer.targetPixels)
    }

    @Test
    fun `xor combines source and destination rgb channels`() {
        val renderer = SpriteRenderer(1, 1)
        renderer.targetPixels[0] = 0xFF123456.toInt()

        renderer.drawSprite(0, 0, intArrayOf(0xFF010204.toInt()), 1, 1, Blitters.XOR)

        assertEquals(0xFF133652.toInt(), renderer.targetPixels[0])
    }

    @Test
    fun `white black and mono draw the source shape using palette colors`() {
        val white = SpriteRenderer(2, 1)
        white.clear(BLUE)
        white.drawSprite(0, 0, intArrayOf(RED, TRANSPARENT), 2, 1, Blitters.WHITE)

        val black = SpriteRenderer(2, 1)
        black.clear(BLUE)
        black.drawSprite(0, 0, intArrayOf(RED, TRANSPARENT), 2, 1, Blitters.BLACK)

        val mono = SpriteRenderer(2, 1)
        mono.clear(BLUE)
        mono.drawSprite(0, 0, intArrayOf(RED, TRANSPARENT), 2, 1, Blitters.MONO_6)

        assertContentEquals(intArrayOf(SpriteRenderer.DEFAULT_VGA_PALETTE_ARGB[15], BLUE), white.targetPixels)
        assertContentEquals(intArrayOf(SpriteRenderer.DEFAULT_VGA_PALETTE_ARGB[0], BLUE), black.targetPixels)
        assertContentEquals(intArrayOf(SpriteRenderer.DEFAULT_VGA_PALETTE_ARGB[6], BLUE), mono.targetPixels)
    }

    @Test
    fun `horizontal flip mirrors sprite pixels`() {
        val renderer = SpriteRenderer(3, 1)
        renderer.clear(BLUE)

        renderer.drawSprite(0, 0, intArrayOf(RED, GREEN, WHITE), 3, 1, Blitters.OR, hflip = true)

        assertContentEquals(intArrayOf(WHITE, GREEN, RED), renderer.targetPixels)
    }

    @Test
    fun `draws only pixels inside target and explicit clip rect`() {
        val renderer = SpriteRenderer(4, 3)
        renderer.clear(BLUE)

        renderer.drawSprite(
            x = -1,
            y = 1,
            pixels = intArrayOf(RED, GREEN, WHITE, RED),
            spriteWidth = 2,
            spriteHeight = 2,
            blitMode = Blitters.NO_TRANSP,
            clipRect = ClipRect(left = 0, top = 0, right = 4, bottom = 2)
        )

        assertContentEquals(
            intArrayOf(
                BLUE, BLUE, BLUE, BLUE,
                GREEN, BLUE, BLUE, BLUE,
                BLUE, BLUE, BLUE, BLUE
            ),
            renderer.targetPixels
        )
    }

    @Test
    fun `draw rect clips to target bounds`() {
        val renderer = SpriteRenderer(3, 2)
        renderer.clear(BLUE)

        renderer.drawRect(left = 1, top = -1, width = 4, height = 2, colorIndex = 12)

        assertContentEquals(
            intArrayOf(
                BLUE, SpriteRenderer.DEFAULT_VGA_PALETTE_ARGB[12], SpriteRenderer.DEFAULT_VGA_PALETTE_ARGB[12],
                BLUE, BLUE, BLUE
            ),
            renderer.targetPixels
        )
    }

    companion object {
        private const val TRANSPARENT = 0x00000000
        private const val RED = -0x010000
        private const val GREEN = -0xff0100
        private const val BLUE = -0xffff01
        private const val WHITE = -0x1
        private val SPRITE_WITH_TRANSPARENCY = intArrayOf(RED, TRANSPARENT, GREEN, WHITE)
    }
}
