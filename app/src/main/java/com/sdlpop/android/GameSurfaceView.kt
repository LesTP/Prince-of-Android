package com.sdlpop.android

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * SurfaceView that will host the 320×200 game rendering.
 * Phase 16a: scaffold only — draws a black screen with status text to confirm
 * the rendering pipeline is wired up and game logic module is accessible.
 */
class GameSurfaceView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 32f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // Draw a single frame to confirm the surface works
        val canvas = holder.lockCanvas() ?: return
        drawTestFrame(canvas)
        holder.unlockCanvasAndPost(canvas)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Redraw on size change
        val canvas = holder.lockCanvas() ?: return
        drawTestFrame(canvas)
        holder.unlockCanvasAndPost(canvas)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Nothing to clean up yet
    }

    private fun drawTestFrame(canvas: Canvas) {
        // Black background (matching original game)
        canvas.drawColor(Color.BLACK)

        // Status text to confirm everything is wired
        val centerX = width / 2f
        val centerY = height / 2f

        canvas.drawText("Prince of Android", centerX, centerY - 40, textPaint)
        canvas.drawText("Phase 16a — Project scaffold OK", centerX, centerY + 10, textPaint)
        canvas.drawText("Game logic module linked ✓", centerX, centerY + 60, textPaint)
    }
}
