package com.sdlpop.render

import com.sdlpop.assets.AssetLocation
import com.sdlpop.assets.DatDecodedImage
import com.sdlpop.assets.DatPalette
import com.sdlpop.assets.LoadedAssetResource
import com.sdlpop.assets.Rgb6
import com.sdlpop.assets.SpriteCatalog
import com.sdlpop.game.Blitters
import com.sdlpop.game.Chtabs
import com.sdlpop.game.GameState
import com.sdlpop.game.RectType
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class RenderTableFlusherTest {
    private val gs = GameState

    @BeforeTest
    fun resetState() {
        gs.tableCounts.fill(0)
        gs.drectsCount = 0
        gs.peelsCount = 0
        gs.backtable.forEach {
            it.xh = 0
            it.xl = 0
            it.y = 0
            it.chtabId = 0
            it.id = 0
            it.blit = 0
        }
        gs.foretable.forEach {
            it.xh = 0
            it.xl = 0
            it.y = 0
            it.chtabId = 0
            it.id = 0
            it.blit = 0
        }
        gs.midtable.forEach {
            it.xh = 0
            it.xl = 0
            it.y = 0
            it.chtabId = 0
            it.id = 0
            it.peel = 0
            it.clip = RectType()
            it.blit = 0
        }
        gs.wipetable.forEach {
            it.left = 0
            it.bottom = 0
            it.height = 0
            it.width = 0
            it.color = 0
            it.layer = 0
        }
        gs.drects.forEach {
            it.top = 0
            it.left = 0
            it.bottom = 0
            it.right = 0
        }
    }

    @Test
    fun `flushes tables in C draw order`() {
        val renderer = SpriteRenderer(4, 1)
        renderer.clear(BLUE)
        val flusher = RenderTableFlusher(renderer, singlePixelCatalogs())

        gs.wipetable[0].apply {
            left = 0
            bottom = 1
            height = 1
            width = 4
            color = 1
            layer = 0
        }
        gs.backtable[0].apply {
            chtabId = Chtabs.ENVIRONMENT
            id = 0
            xh = 0
            xl = 0
            y = 0
            blit = Blitters.NO_TRANSP
        }
        gs.midtable[0].apply {
            chtabId = Chtabs.SWORD
            id = 0
            xh = 0
            xl = 1
            y = 0
            blit = Blitters.NO_TRANSP
            clip.left = 0
            clip.top = 0
            clip.right = 4
            clip.bottom = 1
        }
        gs.wipetable[1].apply {
            left = 2
            bottom = 1
            height = 1
            width = 1
            color = 2
            layer = 1
        }
        gs.foretable[0].apply {
            chtabId = Chtabs.GUARD
            id = 0
            xh = 0
            xl = 3
            y = 0
            blit = Blitters.NO_TRANSP
        }
        gs.tableCounts[RenderTableFlusher.WIPETABLE] = 2
        gs.tableCounts[RenderTableFlusher.BACKTABLE] = 1
        gs.tableCounts[RenderTableFlusher.MIDTABLE] = 1
        gs.tableCounts[RenderTableFlusher.FORETABLE] = 1

        flusher.flushTables()

        assertContentEquals(
            intArrayOf(RED, GREEN, SpriteRenderer.DEFAULT_VGA_PALETTE_ARGB[2], WHITE),
            renderer.targetPixels
        )
        assertEquals(5, gs.drectsCount.toInt())
    }

    @Test
    fun `midtable applies clip rect and horizontal flip`() {
        val renderer = SpriteRenderer(4, 2)
        renderer.clear(BLUE)
        val flusher = RenderTableFlusher(renderer, mapOf(Chtabs.SWORD to catalog(Chtabs.SWORD, SPRITE_2X2)))

        gs.midtable[0].apply {
            chtabId = Chtabs.SWORD
            id = 0
            xh = 0
            xl = 2
            y = 0
            blit = Blitters.NO_TRANSP or 0x80
            clip.left = 0
            clip.top = 0
            clip.right = 3
            clip.bottom = 2
        }
        gs.tableCounts[RenderTableFlusher.MIDTABLE] = 1

        flusher.flushTables()

        assertContentEquals(
            intArrayOf(
                GREEN, RED, BLUE, BLUE,
                WHITE, BLUE, BLUE, BLUE
            ),
            renderer.targetPixels
        )
    }

    @Test
    fun `back and fore table ids resolve zero based sprite ids through catalog frame ids`() {
        val renderer = SpriteRenderer(2, 1)
        renderer.clear(BLUE)
        val flusher = RenderTableFlusher(renderer, mapOf(Chtabs.ENVIRONMENT to catalog(Chtabs.ENVIRONMENT, RED_PIXEL, WHITE_PIXEL)))

        gs.backtable[0].apply {
            chtabId = Chtabs.ENVIRONMENT
            id = 1
            xh = 0
            xl = 0
            y = 0
            blit = Blitters.NO_TRANSP
        }
        gs.tableCounts[RenderTableFlusher.BACKTABLE] = 1

        flusher.flushTables()

        assertContentEquals(intArrayOf(WHITE, BLUE), renderer.targetPixels)
    }

    private fun singlePixelCatalogs(): Map<Int, SpriteCatalog> = mapOf(
        Chtabs.ENVIRONMENT to catalog(Chtabs.ENVIRONMENT, RED_PIXEL),
        Chtabs.SWORD to catalog(Chtabs.SWORD, GREEN_PIXEL),
        Chtabs.GUARD to catalog(Chtabs.GUARD, WHITE_PIXEL)
    )

    private fun catalog(chtabId: Int, vararg images: DatDecodedImage): SpriteCatalog =
        SpriteCatalog(
            chtabId = chtabId,
            paletteResourceId = 700 + chtabId,
            palette = TEST_PALETTE,
            images = images.toList()
        )

    companion object {
        private const val RED = -0x010000
        private const val GREEN = -0xff0100
        private const val BLUE = -0xffff01
        private const val WHITE = -0x1

        private val TEST_PALETTE = DatPalette(
            rowBits = 0,
            nColors = 16,
            vga = List(16) { Rgb6(0, 0, 0) },
            cga = emptyList(),
            ega = emptyList()
        )
        private val RED_PIXEL = sprite(1, 1, intArrayOf(RED))
        private val GREEN_PIXEL = sprite(1, 1, intArrayOf(GREEN))
        private val WHITE_PIXEL = sprite(1, 1, intArrayOf(WHITE))
        private val SPRITE_2X2 = sprite(2, 2, intArrayOf(RED, GREEN, BLUE, WHITE))

        private fun sprite(width: Int, height: Int, pixels: IntArray): DatDecodedImage =
            DatDecodedImage(
                width = width,
                height = height,
                argbPixels = pixels,
                source = LoadedAssetResource(
                    id = 0,
                    extension = "dat",
                    sourceName = "test.dat",
                    location = AssetLocation.DAT,
                    bytes = ByteArray(0)
                )
            )
    }
}
