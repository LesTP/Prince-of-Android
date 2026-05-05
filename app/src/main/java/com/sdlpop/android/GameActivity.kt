package com.sdlpop.android

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import com.sdlpop.assets.AssetImages
import com.sdlpop.assets.AssetRepository
import com.sdlpop.assets.SpriteCatalog
import com.sdlpop.game.Chtabs
import com.sdlpop.game.ExternalStubs
import com.sdlpop.game.GameState
import com.sdlpop.game.Seg008
import com.sdlpop.render.LevelScreenshotGenerator

/**
 * Main game activity. Sets up fullscreen landscape mode, loads Level 1 assets,
 * and hands the rendering pipeline to [GameSurfaceView].
 */
class GameActivity : Activity() {

    private lateinit var gameView: GameSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen, no title bar, extend behind all system bars
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        // Display cutout mode must be set before setContentView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        try {
            val catalogs = initializeGameState()
            gameView = GameSurfaceView(this, GameState, catalogs)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize game", e)
            gameView = GameSurfaceView(this, null, emptyMap())
        }
        setContentView(gameView)

        // Immersive sticky mode — hide both status bar and navigation bar
        // Must be called after setContentView so the decor view exists
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
    }

    private fun initializeGameState(): Map<Int, SpriteCatalog> {
        val repository = AssetRepository(AndroidAssetSource(assets))
        val gs = GameState

        // Load level 1
        val levelHandle = repository.openDat("LEVELS.DAT", optional = true)
            ?: error("Cannot open LEVELS.DAT")
        val levelResource = repository.loadFromOpenDatsAlloc(LEVEL_RESOURCE_BASE + 1, "bin")
            ?: error("Missing level 1 resource")
        gs.level = LevelScreenshotGenerator.parseLevel(levelResource.bytes)
        repository.closeDat(levelHandle)

        gs.currentLevel = 1
        gs.nextLevel = 1
        gs.drawnRoom = gs.level.startRoom.takeIf { it in 1..24 } ?: 1
        gs.graphicsMode = GM_MCGA_VGA
        Seg008.alterModsAllrm()

        // Open sprite DAT files
        val levelType = gs.custom.tblLevelType.getOrElse(1) { 0 }
        val environmentDat = if (levelType == 0) "VDUNGEON.DAT" else "VPALACE.DAT"
        repository.openDat("PRINCE.DAT", optional = true)
        repository.openDat("KID.DAT", optional = true)
        repository.openDat(environmentDat, optional = true)
        repository.openDat("GUARD.DAT", optional = true)

        // Load sprite catalogs (same order as LevelScreenshotGenerator)
        val catalogs = mutableMapOf<Int, SpriteCatalog>()
        fun load(chtabId: Int, paletteResourceId: Int, paletteBits: Int) {
            AssetImages.loadSpriteCatalog(repository, chtabId, paletteResourceId, paletteBits)?.let {
                catalogs[chtabId] = CatalogPreDecoder.preDecodeForAndroid(it)
            }
        }
        load(Chtabs.SWORD, 700, 1 shl 2)
        load(Chtabs.FLAMESWORDPOTION, 150, 1 shl 3)
        load(Chtabs.KID, 400, 1 shl 7)
        load(Chtabs.GUARD, 750, 1 shl 8)
        load(Chtabs.ENVIRONMENT, 200, 1 shl 5)
        load(Chtabs.ENVIRONMENTWALL, 360, 1 shl 6)

        // Wire ExternalStubs.getImage to use real catalog dimensions
        ExternalStubs.getImage = { chtab, imageId ->
            catalogs[chtab]?.dimensionsByFrameId(imageId + 1)
        }

        return catalogs
    }

    companion object {
        private const val TAG = "GameActivity"
        private const val LEVEL_RESOURCE_BASE = 2000
        private const val GM_MCGA_VGA = 5
    }
}
