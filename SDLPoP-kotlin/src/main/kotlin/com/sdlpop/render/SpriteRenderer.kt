package com.sdlpop.render

import com.sdlpop.game.Blitters

data class ClipRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    init {
        require(right >= left) { "Clip right must be >= left" }
        require(bottom >= top) { "Clip bottom must be >= top" }
    }
}

class SpriteRenderer(
    val width: Int,
    val height: Int,
    val targetPixels: IntArray = IntArray(width * height),
    private val palette: IntArray = DEFAULT_VGA_PALETTE_ARGB
) {
    init {
        require(width > 0) { "Target width must be positive" }
        require(height > 0) { "Target height must be positive" }
        require(targetPixels.size == width * height) { "Target pixel count must match dimensions" }
        require(palette.size >= 16) { "Palette must provide at least 16 colors" }
    }

    fun clear(color: Int = palette[0]) {
        targetPixels.fill(color)
    }

    fun drawRect(left: Int, top: Int, width: Int, height: Int, colorIndex: Int, clipRect: ClipRect? = null) {
        if (width <= 0 || height <= 0) return

        val color = paletteColor(colorIndex)
        val bounds = clippedBounds(left, top, left + width, top + height, clipRect) ?: return
        for (y in bounds.top until bounds.bottom) {
            val row = y * this.width
            for (x in bounds.left until bounds.right) {
                targetPixels[row + x] = color
            }
        }
    }

    fun drawSprite(
        x: Int,
        y: Int,
        pixels: IntArray,
        spriteWidth: Int,
        spriteHeight: Int,
        blitMode: Int,
        hflip: Boolean = false,
        clipRect: ClipRect? = null
    ) {
        require(spriteWidth > 0) { "Sprite width must be positive" }
        require(spriteHeight > 0) { "Sprite height must be positive" }
        require(pixels.size == spriteWidth * spriteHeight) { "Sprite pixel count must match dimensions" }

        val mode = blitMode and HFLIP_MASK.inv()
        val flip = hflip || (blitMode and HFLIP_MASK) != 0
        val bounds = clippedBounds(x, y, x + spriteWidth, y + spriteHeight, clipRect) ?: return

        for (destY in bounds.top until bounds.bottom) {
            val srcY = destY - y
            val destRow = destY * width
            val srcRow = srcY * spriteWidth
            for (destX in bounds.left until bounds.right) {
                val rawSrcX = destX - x
                val srcX = if (flip) spriteWidth - 1 - rawSrcX else rawSrcX
                val source = pixels[srcRow + srcX]
                val destIndex = destRow + destX
                val dest = targetPixels[destIndex]
                val result = blitPixel(source, dest, mode) ?: continue
                targetPixels[destIndex] = result
            }
        }
    }

    private fun blitPixel(source: Int, dest: Int, mode: Int): Int? =
        when {
            mode == Blitters.NO_TRANSP -> source
            mode == Blitters.OR || mode == Blitters.TRANSP -> {
                if (isTransparent(source)) null else source
            }
            mode == Blitters.XOR -> xorRgb(dest, source)
            mode == Blitters.WHITE -> {
                if (isTransparent(source)) null else paletteColor(WHITE_INDEX)
            }
            mode == Blitters.BLACK -> {
                if (isTransparent(source)) null else paletteColor(BLACK_INDEX)
            }
            isMonoMode(mode) -> {
                if (isTransparent(source)) null else paletteColor(mode and MONO_COLOR_MASK)
            }
            else -> {
                if (isTransparent(source)) null else source
            }
        }

    private fun clippedBounds(left: Int, top: Int, right: Int, bottom: Int, clipRect: ClipRect?): ClipRect? {
        var clippedLeft = left.coerceAtLeast(0)
        var clippedTop = top.coerceAtLeast(0)
        var clippedRight = right.coerceAtMost(width)
        var clippedBottom = bottom.coerceAtMost(height)

        if (clipRect != null) {
            clippedLeft = clippedLeft.coerceAtLeast(clipRect.left)
            clippedTop = clippedTop.coerceAtLeast(clipRect.top)
            clippedRight = clippedRight.coerceAtMost(clipRect.right)
            clippedBottom = clippedBottom.coerceAtMost(clipRect.bottom)
        }

        return if (clippedLeft < clippedRight && clippedTop < clippedBottom) {
            ClipRect(clippedLeft, clippedTop, clippedRight, clippedBottom)
        } else {
            null
        }
    }

    private fun paletteColor(index: Int): Int = palette[index and 0x0F]

    private fun isTransparent(pixel: Int): Boolean = (pixel ushr 24) == 0

    private fun isMonoMode(mode: Int): Boolean = (mode and Blitters.MONO) != 0 || mode >= COLORED_FLAME_BASE

    private fun xorRgb(dest: Int, source: Int): Int = (dest and ALPHA_MASK) or ((dest xor source) and RGB_MASK)

    companion object {
        private const val HFLIP_MASK = 0x80
        private const val MONO_COLOR_MASK = 0x3F
        private const val COLORED_FLAME_BASE = 0x100
        private const val BLACK_INDEX = 0
        private const val WHITE_INDEX = 15
        private const val ALPHA_MASK = -0x1000000
        private const val RGB_MASK = 0x00FFFFFF

        val DEFAULT_VGA_PALETTE_ARGB = intArrayOf(
            rgb6(0x00, 0x00, 0x00),
            rgb6(0x00, 0x00, 0x2A),
            rgb6(0x00, 0x2A, 0x00),
            rgb6(0x00, 0x2A, 0x2A),
            rgb6(0x2A, 0x00, 0x00),
            rgb6(0x2A, 0x00, 0x2A),
            rgb6(0x2A, 0x15, 0x00),
            rgb6(0x2A, 0x2A, 0x2A),
            rgb6(0x15, 0x15, 0x15),
            rgb6(0x15, 0x15, 0x3F),
            rgb6(0x15, 0x3F, 0x15),
            rgb6(0x15, 0x3F, 0x3F),
            rgb6(0x3F, 0x15, 0x15),
            rgb6(0x3F, 0x15, 0x3F),
            rgb6(0x3F, 0x3F, 0x15),
            rgb6(0x3F, 0x3F, 0x3F)
        )

        private fun rgb6(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 18) or (g shl 10) or (b shl 2)
    }
}
