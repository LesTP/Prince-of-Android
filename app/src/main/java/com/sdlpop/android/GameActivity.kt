package com.sdlpop.android

import android.app.Activity
import android.os.Bundle
import android.view.Window
import android.view.WindowManager

/**
 * Main game activity. Sets up fullscreen landscape mode and hosts the game SurfaceView.
 * Phase 16a: scaffold only — displays a blank SurfaceView to verify project structure.
 */
class GameActivity : Activity() {

    private lateinit var gameView: GameSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen, no title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // Create the game surface view
        gameView = GameSurfaceView(this)
        setContentView(gameView)
    }
}
