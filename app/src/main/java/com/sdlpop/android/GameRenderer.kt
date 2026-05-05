package com.sdlpop.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.sdlpop.assets.SpriteCatalog
import com.sdlpop.game.GameState
import com.sdlpop.render.RenderTableFlusher
import com.sdlpop.render.SpriteRenderer

/**
 * Bridges the shared [SpriteRenderer]/[RenderTableFlusher] pipeline to Android's Canvas.
 *
 * Owns a 320×200 software pixel buffer and a reusable [Bitmap]. Each call to
 * [renderFrame] fills the buffer via the game's table flusher; [drawToCanvas]
 * copies the result to the device screen at the correct scale.
 */
class GameRenderer {

    private val renderer = SpriteRenderer(WIDTH, HEIGHT)
    private val frameBitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)

    private val scalePaint = Paint().apply {
        isFilterBitmap = false  // nearest-neighbor for crisp pixel art
        isAntiAlias = false
    }

    fun renderFrame(gs: GameState, catalogs: Map<Int, SpriteCatalog>) {
        renderer.clear()
        RenderTableFlusher(renderer, catalogs, gs).flushTables()
        frameBitmap.setPixels(renderer.targetPixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT)
    }

    fun drawToCanvas(canvas: Canvas, destRect: RectF) {
        canvas.drawBitmap(frameBitmap, null, destRect, scalePaint)
    }

    companion object {
        const val WIDTH = 320
        const val HEIGHT = 200
    }
}
