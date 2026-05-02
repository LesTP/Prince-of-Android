package com.sdlpop.render

import com.sdlpop.assets.DatDecodedImage
import com.sdlpop.assets.DecodedAssetImage
import com.sdlpop.assets.PngDecodedImage
import com.sdlpop.assets.SpriteCatalog
import com.sdlpop.game.BackTableType
import com.sdlpop.game.Blitters
import com.sdlpop.game.Chtabs
import com.sdlpop.game.GameState
import com.sdlpop.game.MidtableType
import com.sdlpop.game.RectType
import com.sdlpop.game.Seg008
import com.sdlpop.game.WipetableType
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

class RenderTableFlusher(
    private val renderer: SpriteRenderer,
    private val catalogs: Map<Int, SpriteCatalog>,
    private val gs: GameState = GameState
) {
    fun flushTables() {
        gs.drectsCount = 0
        restorePeels()
        drawWipes(layer = 0)
        drawBackForeTable(whichTable = BACKTABLE)
        drawMidTable()
        drawWipes(layer = 1)
        drawBackForeTable(whichTable = FORETABLE)
    }

    fun drawBackFore(whichTable: Int, index: Int) {
        val table = when (whichTable) {
            BACKTABLE -> gs.backtable
            FORETABLE -> gs.foretable
            else -> error("Unsupported back/fore table id: $whichTable")
        }
        if (index !in table.indices) return
        val entry = table[index]
        val sprite = requireSprite(entry.chtabId, entry.id)
        renderer.drawSprite(
            x = entry.screenX(),
            y = entry.y.toInt(),
            pixels = sprite.argbPixels,
            spriteWidth = sprite.width,
            spriteHeight = sprite.height,
            blitMode = entry.blit
        )
        addDirtyRect(entry.screenX(), entry.y.toInt(), sprite.width, sprite.height)
    }

    fun drawMid(index: Int) {
        if (index !in gs.midtable.indices) return
        val entry = gs.midtable[index]
        val sprite = requireSprite(entry.chtabId, entry.id)
        var x = entry.screenX()
        val y = entry.y.toInt()
        var blit = entry.blit
        val hflip = (blit and HFLIP_MASK) != 0
        if (hflip) {
            blit = blit and HFLIP_MASK.inv()
        }

        val clip = if (gs.chtabFlipClip.getOrNull(entry.chtabId) != 0) {
            if (entry.chtabId != Chtabs.SWORD) {
                x = Seg008.calcScreenXCoord(x.toShort()).toInt()
            }
            entry.clip.toClipRect()
        } else {
            null
        }

        if (hflip) {
            x -= sprite.width
        }

        renderer.drawSprite(
            x = x,
            y = y,
            pixels = sprite.argbPixels,
            spriteWidth = sprite.width,
            spriteHeight = sprite.height,
            blitMode = blit,
            hflip = hflip,
            clipRect = clip
        )
        addDirtyRect(x, y, sprite.width, sprite.height)
    }

    fun drawWipe(index: Int) {
        if (index !in gs.wipetable.indices) return
        val entry = gs.wipetable[index]
        renderer.drawRect(
            left = entry.left.toInt(),
            top = entry.bottom.toInt() - entry.height,
            width = entry.width.toInt(),
            height = entry.height,
            colorIndex = entry.color
        )
        addDirtyRect(
            left = entry.left.toInt(),
            top = entry.bottom.toInt() - entry.height,
            width = entry.width.toInt(),
            height = entry.height
        )
    }

    private fun drawBackForeTable(whichTable: Int) {
        val count = gs.tableCounts[whichTable].toInt()
        for (index in 0 until count) {
            drawBackFore(whichTable, index)
        }
    }

    private fun drawMidTable() {
        val count = gs.tableCounts[MIDTABLE].toInt()
        for (index in 0 until count) {
            drawMid(index)
        }
    }

    private fun drawWipes(layer: Int) {
        val count = gs.tableCounts[WIPETABLE].toInt()
        for (index in 0 until count) {
            if (gs.wipetable[index].layer == layer) {
                drawWipe(index)
            }
        }
    }

    private fun restorePeels() {
        gs.peelsCount = 0
    }

    private fun requireSprite(chtabId: Int, zeroBasedImageId: Int): RenderSprite {
        val image = catalogs[chtabId]?.imageByFrameId(zeroBasedImageId + 1)
            ?: error("Missing sprite chtab=$chtabId image=$zeroBasedImageId")
        return image.asRenderSprite(chtabId, zeroBasedImageId)
    }

    private fun DecodedAssetImage.asRenderSprite(chtabId: Int, zeroBasedImageId: Int): RenderSprite =
        when (this) {
            is DatDecodedImage -> RenderSprite(width, height, argbPixels)
            is PngDecodedImage -> {
                val image = ImageIO.read(ByteArrayInputStream(pngBytes))
                    ?: error("Sprite chtab=$chtabId image=$zeroBasedImageId is not a readable PNG")
                val pixels = IntArray(image.width * image.height)
                image.getRGB(0, 0, image.width, image.height, pixels, 0, image.width)
                RenderSprite(image.width, image.height, pixels)
            }
        }

    private fun BackTableType.screenX(): Int = xh * 8 + xl

    private fun MidtableType.screenX(): Int = xh * 8 + xl

    private fun RectType.toClipRect(): ClipRect =
        ClipRect(left = left.toInt(), top = top.toInt(), right = right.toInt(), bottom = bottom.toInt())

    private fun addDirtyRect(left: Int, top: Int, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val index = gs.drectsCount.toInt()
        if (index >= gs.drects.size) return
        val rect = gs.drects[index]
        rect.left = left.toShort()
        rect.top = top.toShort()
        rect.right = (left + width).toShort()
        rect.bottom = (top + height).toShort()
        gs.drectsCount = (index + 1).toShort()
    }

    private data class RenderSprite(
        val width: Int,
        val height: Int,
        val argbPixels: IntArray
    )

    companion object {
        const val BACKTABLE = 0
        const val FORETABLE = 1
        const val WIPETABLE = 2
        const val MIDTABLE = 3
        private const val HFLIP_MASK = 0x80
    }
}
