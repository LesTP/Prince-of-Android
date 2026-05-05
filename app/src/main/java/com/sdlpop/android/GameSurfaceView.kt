package com.sdlpop.android

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.sdlpop.assets.SpriteCatalog
import com.sdlpop.game.Directions
import com.sdlpop.game.GameState
import com.sdlpop.game.Seg008

/**
 * SurfaceView that renders the 320×200 game frame scaled to the device screen.
 *
 * A background render thread draws Level 1's start room using the shared
 * [SpriteRenderer]/[RenderTableFlusher] pipeline via [GameRenderer].
 * The first iteration renders a static room — no game loop tick or input handling.
 */
class GameSurfaceView(
    context: Context,
    private val gs: GameState?,
    private val catalogs: Map<Int, SpriteCatalog>
) : SurfaceView(context), SurfaceHolder.Callback {

    private val gameRenderer = GameRenderer()
    private var renderThread: RenderThread? = null

    private val errorPaint = Paint().apply {
        color = Color.WHITE
        textSize = 28f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        renderThread = RenderThread(holder).also { it.start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Render thread picks up new dimensions on next frame
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        renderThread?.running = false
        renderThread?.join()
        renderThread = null
    }

    /**
     * Compute a destination [RectF] that maintains the 8:5 (320:200) aspect ratio
     * centered within the given surface dimensions.
     */
    private fun computeScaledRect(surfaceWidth: Int, surfaceHeight: Int): RectF {
        val scaleX = surfaceWidth.toFloat() / GameRenderer.WIDTH
        val scaleY = surfaceHeight.toFloat() / GameRenderer.HEIGHT
        val scale = minOf(scaleX, scaleY)
        val scaledW = GameRenderer.WIDTH * scale
        val scaledH = GameRenderer.HEIGHT * scale
        val offsetX = (surfaceWidth - scaledW) / 2f
        val offsetY = (surfaceHeight - scaledH) / 2f
        return RectF(offsetX, offsetY, offsetX + scaledW, offsetY + scaledH)
    }

    private inner class RenderThread(private val holder: SurfaceHolder) : Thread("PoP-Render") {
        @Volatile
        var running = true

        override fun run() {
            // Prepare room state once (static render — no game loop)
            val state = gs
            if (state == null || catalogs.isEmpty()) {
                drawError("Game init failed — check logcat")
                return
            }

            try {
                prepareRoom(state)
                gameRenderer.renderFrame(state, catalogs)
            } catch (e: Exception) {
                Log.e(TAG, "Render failed", e)
                drawError("Render error: ${e.message}")
                return
            }

            // Draw the rendered frame to the surface
            while (running) {
                var canvas: Canvas? = null
                try {
                    canvas = holder.lockCanvas() ?: break
                    canvas.drawColor(Color.BLACK)
                    val rect = computeScaledRect(canvas.width, canvas.height)
                    gameRenderer.drawToCanvas(canvas, rect)
                } finally {
                    canvas?.let { holder.unlockCanvasAndPost(it) }
                }
                // Static scene — no need to loop at high FPS. Draw once, then idle.
                break
            }
        }

        private fun prepareRoom(state: GameState) {
            state.drawnRoom = state.level.startRoom.takeIf { it in 1..24 } ?: 1
            state.Guard.direction = Directions.NONE
            state.guardhpCurr = 0
            state.tableCounts.fill(0)
            state.drectsCount = 0
            state.peelsCount = 0
            state.drawMode = 0
            Seg008.loadRoomLinks()
            Seg008.drawRoom()
        }

        private fun drawError(message: String) {
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas() ?: return
                canvas.drawColor(Color.BLACK)
                canvas.drawText(message, canvas.width / 2f, canvas.height / 2f, errorPaint)
            } finally {
                canvas?.let { holder.unlockCanvasAndPost(it) }
            }
        }
    }

    companion object {
        private const val TAG = "GameSurfaceView"
    }
}
