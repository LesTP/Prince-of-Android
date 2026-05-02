package com.sdlpop.render

import com.sdlpop.assets.AssetByteSource
import com.sdlpop.assets.AssetRepository
import com.sdlpop.game.GameState
import com.sdlpop.game.LevelType
import com.sdlpop.game.LinkType
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.imageio.ImageIO
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LevelScreenshotTest {
    private val assetsRoot = findAssetsRoot()

    @BeforeTest
    fun resetState() {
        GameState.level = LevelType()
        GameState.currentLevel = -1
        GameState.nextLevel = 0
        GameState.loadedRoom = 0
        GameState.drawnRoom = 0
        GameState.currRoomTiles.fill(0)
        GameState.currRoomModif.fill(0)
        GameState.tableCounts.fill(0)
        GameState.drectsCount = 0
        GameState.peelsCount = 0
        GameState.drawMode = 0
    }

    @Test
    fun `room map follows C screenshot BFS directions`() {
        val level = LevelType()
        level.roomlinks[0] = LinkType(left = 2, right = 3, up = 4, down = 5)
        level.roomlinks[2] = LinkType(right = 6)

        val map = LevelScreenshotGenerator(AssetRepository(FilesystemAssetByteSource(assetsRoot)))
            .buildRoomMap(level, startRoom = 1)
        val placements = map.rooms.associateBy { it.room }

        assertEquals(RoomPlacement(room = 1, x = 1, y = 1), placements[1])
        assertEquals(RoomPlacement(room = 2, x = 0, y = 1), placements[2])
        assertEquals(RoomPlacement(room = 3, x = 2, y = 1), placements[3])
        assertEquals(RoomPlacement(room = 4, x = 1, y = 0), placements[4])
        assertEquals(RoomPlacement(room = 5, x = 1, y = 2), placements[5])
        assertEquals(RoomPlacement(room = 6, x = 3, y = 1), placements[6])
        assertEquals(4, map.width)
        assertEquals(3, map.height)
    }

    @Test
    fun `loads level one resource and writes composite PNG`() {
        val generator = LevelScreenshotGenerator(AssetRepository(FilesystemAssetByteSource(assetsRoot)))

        val screenshot = generator.renderLevel(levelNumber = 1)

        assertTrue(screenshot.roomMap.rooms.isNotEmpty())
        assertEquals(screenshot.roomMap.width * LevelScreenshotGenerator.ROOM_WIDTH, screenshot.image.width)
        assertEquals(screenshot.roomMap.height * LevelScreenshotGenerator.ROOM_HEIGHT, screenshot.image.height)

        val output = Paths.get("build/render/level_01_kotlin.png").toAbsolutePath()
        generator.writePng(screenshot, output)

        assertTrue(Files.isRegularFile(output))
        assertNotNull(ImageIO.read(output.toFile()))
    }

    private fun findAssetsRoot(): Path {
        var current = Paths.get("").toAbsolutePath()
        while (true) {
            val candidate = current.resolve("app/src/main/assets")
            if (Files.isDirectory(candidate)) return candidate
            current = current.parent ?: error("Cannot locate app/src/main/assets")
        }
    }

    private class FilesystemAssetByteSource(private val root: Path) : AssetByteSource {
        override fun readBytes(path: String): ByteArray? {
            val resolved = root.resolve(path)
            return if (Files.isRegularFile(resolved)) Files.readAllBytes(resolved) else null
        }

        override fun isDirectory(path: String): Boolean =
            Files.isDirectory(root.resolve(path))
    }
}
