package com.sdlpop.render

import com.sdlpop.assets.AssetImages
import com.sdlpop.assets.AssetRepository
import com.sdlpop.assets.SpriteCatalog
import com.sdlpop.game.Chtabs
import com.sdlpop.game.Directions
import com.sdlpop.game.ExternalStubs
import com.sdlpop.game.GameState
import com.sdlpop.game.LevelType
import com.sdlpop.game.Seg002
import com.sdlpop.game.Seg008
import com.sdlpop.game.Tiles
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.nio.file.Path
import javax.imageio.ImageIO

class LevelScreenshotGenerator(
    private val repository: AssetRepository,
    private val gs: GameState = GameState
) {
    fun renderLevel(levelNumber: Int): LevelScreenshot {
        require(levelNumber in 0..15) { "Level number must be in 0..15" }

        loadLevel(levelNumber)
        val catalogs = loadDefaultCatalogs(levelNumber)

        // Wire ExternalStubs.getImage so add_backtable/add_foretable/add_midtable
        // can look up image heights for all loaded chtabs (not just KID/GUARD)
        ExternalStubs.getImage = { chtab, imageId ->
            catalogs[chtab]?.dimensionsByFrameId(imageId + 1)
        }

        val map = buildRoomMap(gs.level, startRoom = gs.level.startRoom.takeIf { it in 1..ROOM_COUNT } ?: 1)
        val fullPalette = buildFullPalette(catalogs)
        val image = BufferedImage(
            map.width * ROOM_WIDTH,
            map.height * ROOM_STRIDE_Y,
            BufferedImage.TYPE_INT_ARGB
        )

        for (placement in map.rooms) {
            val roomImage = renderRoom(placement.room, catalogs, fullPalette)
            // Copy only ROOM_STRIDE_Y (189) rows — skip the 11-pixel status bar area
            image.setRGB(
                placement.x * ROOM_WIDTH,
                placement.y * ROOM_STRIDE_Y,
                ROOM_WIDTH,
                ROOM_STRIDE_Y,
                roomImage.targetPixels,
                0,
                ROOM_WIDTH
            )
        }

        return LevelScreenshot(levelNumber = levelNumber, roomMap = map, image = image)
    }

    fun writePng(screenshot: LevelScreenshot, outputPath: Path) {
        outputPath.parent?.toFile()?.mkdirs()
        ImageIO.write(screenshot.image, "png", outputPath.toFile())
    }

    fun loadLevel(levelNumber: Int) {
        val handle = repository.openDat("LEVELS.DAT", optional = true)
            ?: error("Cannot open LEVELS.DAT or LEVELS directory")
        val resource = repository.loadFromOpenDatsAlloc(levelNumber + LEVEL_RESOURCE_BASE, "bin")
            ?: error("Missing level resource ${levelNumber + LEVEL_RESOURCE_BASE}")
        gs.level = parseLevel(resource.bytes)
        repository.closeDat(handle)

        gs.currentLevel = levelNumber
        gs.nextLevel = levelNumber
        gs.drawnRoom = gs.level.startRoom.takeIf { it in 1..ROOM_COUNT } ?: 1
        gs.graphicsMode = GM_MCGA_VGA
        Seg008.alterModsAllrm()
    }

    fun loadDefaultCatalogs(levelNumber: Int): Map<Int, SpriteCatalog> {
        val levelType = gs.custom.tblLevelType.getOrElse(levelNumber) { 0 }
        val environmentDat = if (levelType == 0) "VDUNGEON.DAT" else "VPALACE.DAT"
        val guardType = gs.custom.tblGuardType.getOrElse(levelNumber) { -1 }
        val guardDat = guardDatForType(guardType)
        val guardPaletteDat = if (levelType == 0) "GUARD2.DAT" else "GUARD1.DAT"

        repository.openDat("PRINCE.DAT", optional = true)
        repository.openDat("KID.DAT", optional = true)
        repository.openDat(environmentDat, optional = true)
        if (guardDat != null) {
            repository.openDat(guardDat, optional = true)
            if (guardType == 0) repository.openDat(guardPaletteDat, optional = true)
        }

        val catalogs = mutableMapOf<Int, SpriteCatalog>()
        catalogs.load(Chtabs.SWORD, 700, 1 shl 2)
        catalogs.load(Chtabs.FLAMESWORDPOTION, 150, 1 shl 3)
        catalogs.load(Chtabs.KID, 400, 1 shl 7)
        if (guardDat != null) catalogs.load(Chtabs.GUARD, 750, 1 shl 8)
        catalogs.load(Chtabs.ENVIRONMENT, 200, 1 shl 5)
        catalogs.load(Chtabs.ENVIRONMENTWALL, 360, 1 shl 6)
        return catalogs
    }

    fun buildRoomMap(level: LevelType, startRoom: Int): LevelRoomMap {
        require(startRoom in 1..ROOM_COUNT) { "Start room must be in 1..$ROOM_COUNT" }

        val processed = BooleanArray(ROOM_COUNT + 1)
        val xpos = IntArray(ROOM_COUNT + 1)
        val ypos = IntArray(ROOM_COUNT + 1)
        val queue = ArrayDeque<Int>()
        processed[startRoom] = true
        queue.add(startRoom)

        while (queue.isNotEmpty()) {
            val room = queue.removeFirst()
            val links = level.roomlinks[room - 1]
            val linkedRooms = intArrayOf(links.left, links.right, links.up, links.down)
            for (direction in linkedRooms.indices) {
                val otherRoom = linkedRooms[direction]
                if (otherRoom in 1..ROOM_COUNT && !processed[otherRoom]) {
                    xpos[otherRoom] = xpos[room] + DX[direction]
                    ypos[otherRoom] = ypos[room] + DY[direction]
                    processed[otherRoom] = true
                    queue.add(otherRoom)
                }
            }
        }

        var minX = 0
        var maxX = 0
        var minY = 0
        var maxY = 0
        for (room in 1..ROOM_COUNT) {
            if (xpos[room] < minX) minX = xpos[room]
            if (xpos[room] > maxX) maxX = xpos[room]
            if (ypos[room] < minY) minY = ypos[room]
            if (ypos[room] > maxY) maxY = ypos[room]
        }

        val occupied = mutableMapOf<Pair<Int, Int>, Int>()
        var clashX = minX
        var clashY = maxY + 1
        val placements = mutableListOf<RoomPlacement>()
        for (room in 1..ROOM_COUNT) {
            if (!processed[room]) continue
            while (true) {
                val key = xpos[room] to ypos[room]
                if (occupied.putIfAbsent(key, room) == null) {
                    placements += RoomPlacement(room, xpos[room] - minX, ypos[room] - minY)
                    break
                }
                xpos[room] = clashX++
                ypos[room] = clashY
                if (xpos[room] > maxX) maxX = xpos[room]
                if (ypos[room] > maxY) maxY = ypos[room]
            }
        }

        return LevelRoomMap(
            width = maxX - minX + 1,
            height = maxY - minY + 1,
            rooms = placements
        )
    }

    private fun renderRoom(room: Int, catalogs: Map<Int, SpriteCatalog>, fullPalette: IntArray): SpriteRenderer {
        gs.drawnRoom = room
        gs.Guard.direction = Directions.NONE
        gs.guardhpCurr = 0
        gs.tableCounts.fill(0)
        gs.drectsCount = 0
        gs.peelsCount = 0
        gs.drawMode = 0
        Seg008.loadRoomLinks()

        // Palace levels need wall colors generated per room
        if (gs.custom.tblLevelType.getOrElse(gs.currentLevel) { 0 } != 0) {
            genPalaceWallColors()
        }

        // Load guard from level data (matches C's switch_to_room)
        val roomIdx = room - 1
        // Zero out guardsSeqHi 0xFF — means "no custom sequence" in level data
        if (roomIdx in gs.level.guardsSeqHi.indices && gs.level.guardsSeqHi[roomIdx] == 0xFF) {
            gs.level.guardsSeqHi[roomIdx] = 0
        }
        // Fix guardsX: 0xFF in level data means uninitialized — derive from tile position
        if (roomIdx in gs.level.guardsX.indices && gs.level.guardsX[roomIdx] == 0xFF) {
            val guardTile = gs.level.guardsTile[roomIdx]
            if (guardTile < 30) {
                val col = guardTile % 10
                gs.level.guardsX[roomIdx] = col * 14
            }
        }
        Seg002.checkShadow()

        // Trigger potion bubble state — set modifier bit 0 so bubbles render
        for (tilepos in 0 until 30) {
            val tileType = gs.currRoomTiles[tilepos] and 0x1F
            if (tileType == Tiles.POTION) {
                val modifier = gs.currRoomModif[tilepos]
                if ((modifier and 7) == 0) {
                    gs.currRoomModif[tilepos] = modifier + 1
                }
            }
        }

        // Add guard to objtable before drawRoom so drawObjtableItemsAtTile picks it up
        Seg008.drawGuard()

        Seg008.drawRoom()

        val renderer = SpriteRenderer(ROOM_WIDTH, ROOM_RENDER_HEIGHT, palette = fullPalette)
        renderer.clear()
        RenderTableFlusher(renderer, catalogs, gs).flushTables()
        return renderer
    }

    /**
     * Build a 256-entry VGA palette by placing each chtab's palette at the
     * rows indicated by its [DatPalette.rowBits]. Then apply level_var_palettes
     * (resource 20) override for levels with tblLevelColor != 0.
     */
    private fun buildFullPalette(catalogs: Map<Int, SpriteCatalog>): IntArray {
        val pal = IntArray(256)
        // Start with the 16 standard VGA colors at row 0
        SpriteRenderer.DEFAULT_VGA_PALETTE_ARGB.copyInto(pal)
        // Overlay chtab palettes at their designated rows
        for (catalog in catalogs.values) {
            val rowBits = catalog.palette.rowBits
            for (row in 0 until 16) {
                if ((rowBits and (1 shl row)) != 0) {
                    val base = row * 16
                    val colors = catalog.palette.vga
                    for (i in colors.indices) {
                        if (base + i < 256) {
                            pal[base + i] = colors[i].toArgb8888()
                        }
                    }
                }
            }
        }
        // Apply init_game_main() overrides (C's set_pal calls, run after chtab loading)
        pal[6] = vga6(0x30, 0x26, 0x14)   // palace wall mortar color (lighter tan)
        pal[12] = vga6(0x38, 0x00, 0x0C)  // blood/hurt flash color
        // Apply level_var_palettes (resource 20) override for per-level color variants
        val levelColor = gs.custom.tblLevelColor.getOrElse(gs.currentLevel) { 0 }
        if (levelColor != 0) {
            val levelVarPalettes = repository.loadFromOpenDatsAlloc(20, "bin")
            if (levelVarPalettes != null) {
                val data = levelVarPalettes.bytes
                val levelType = gs.custom.tblLevelType.getOrElse(gs.currentLevel) { 0 }
                val envOffset = 0x30 * (levelColor - 1)
                val wallOffset = envOffset + 0x30 * levelType
                // Set environment palette (0x50-0x5F) from env_pal
                for (i in 0 until 16) {
                    val idx = envOffset + i * 3
                    if (idx + 2 < data.size) {
                        val r = data[idx].toInt() and 0x3F
                        val g = data[idx + 1].toInt() and 0x3F
                        val b = data[idx + 2].toInt() and 0x3F
                        pal[0x50 + i] = (0xFF shl 24) or (r shl 18) or (g shl 10) or (b shl 2)
                    }
                }
                // Set wall palette (0x60-0x6F) from wall_pal
                for (i in 0 until 16) {
                    val idx = wallOffset + i * 3
                    if (idx + 2 < data.size) {
                        val r = data[idx].toInt() and 0x3F
                        val g = data[idx + 1].toInt() and 0x3F
                        val b = data[idx + 2].toInt() and 0x3F
                        pal[0x60 + i] = (0xFF shl 24) or (r shl 18) or (g shl 10) or (b shl 2)
                    }
                }
            }
        }
        return pal
    }

    private fun MutableMap<Int, SpriteCatalog>.load(chtabId: Int, paletteResourceId: Int, paletteBits: Int) {
        AssetImages.loadSpriteCatalog(repository, chtabId, paletteResourceId, paletteBits)?.let {
            put(chtabId, it)
        }
    }

    companion object {
        const val ROOM_WIDTH = 320
        const val ROOM_STRIDE_Y = 189
        const val ROOM_RENDER_HEIGHT = 200
        const val ROOM_OVERLAP_HEIGHT = ROOM_RENDER_HEIGHT - ROOM_STRIDE_Y
        private const val ROOM_COUNT = 24
        private const val LEVEL_RESOURCE_BASE = 2000
        private const val GM_MCGA_VGA = 5
        private val DX = intArrayOf(-1, 1, 0, 0)
        private val DY = intArrayOf(0, 0, -1, 1)

        fun parseLevel(bytes: ByteArray): LevelType {
            require(bytes.size >= LevelType.SIZE_BYTES) {
                "Level resource must contain at least ${LevelType.SIZE_BYTES} bytes"
            }
            val reader = ByteArrayInputStream(bytes)
            val level = LevelType()
            repeat(level.fg.size) { level.fg[it] = reader.u8() }
            repeat(level.bg.size) { level.bg[it] = reader.u8() }
            repeat(level.doorlinks1.size) { level.doorlinks1[it] = reader.u8() }
            repeat(level.doorlinks2.size) { level.doorlinks2[it] = reader.u8() }
            repeat(level.roomlinks.size) {
                level.roomlinks[it].left = reader.u8()
                level.roomlinks[it].right = reader.u8()
                level.roomlinks[it].up = reader.u8()
                level.roomlinks[it].down = reader.u8()
            }
            level.usedRooms = reader.u8()
            repeat(level.roomxs.size) { level.roomxs[it] = reader.u8() }
            repeat(level.roomys.size) { level.roomys[it] = reader.u8() }
            repeat(level.fill1.size) { level.fill1[it] = reader.u8() }
            level.startRoom = reader.u8()
            level.startPos = reader.u8()
            level.startDir = reader.s8()
            repeat(level.fill2.size) { level.fill2[it] = reader.u8() }
            repeat(level.guardsTile.size) { level.guardsTile[it] = reader.u8() }
            repeat(level.guardsDir.size) { level.guardsDir[it] = reader.s8() }
            repeat(level.guardsX.size) { level.guardsX[it] = reader.u8() }
            repeat(level.guardsSeqLo.size) { level.guardsSeqLo[it] = reader.u8() }
            repeat(level.guardsSkill.size) { level.guardsSkill[it] = reader.u8() }
            repeat(level.guardsSeqHi.size) { level.guardsSeqHi[it] = reader.u8() }
            repeat(level.guardsColor.size) { level.guardsColor[it] = reader.u8() }
            repeat(level.fill3.size) { level.fill3[it] = reader.u8() }
            return level
        }

        private fun guardDatForType(guardType: Int): String? =
            when (guardType) {
                0 -> "GUARD.DAT"
                1 -> "FAT.DAT"
                2 -> "SKEL.DAT"
                3 -> "VIZIER.DAT"
                4 -> "SHADOW.DAT"
                else -> null
            }

        /** Convert 6-bit VGA RGB to 8-bit ARGB. */
        private fun vga6(r: Int, g: Int, b: Int): Int =
            (0xFF shl 24) or (r shl 18) or (g shl 10) or (b shl 2)

        private fun ByteArrayInputStream.u8(): Int =
            read().also { require(it >= 0) { "Unexpected end of level resource" } } and 0xFF

        private fun ByteArrayInputStream.s8(): Int = u8().toByte().toInt()
    }

    private fun genPalaceWallColors() {
        val oldRandomSeed = gs.randomSeed
        gs.randomSeed = gs.drawnRoom.toLong()
        Seg002.prandom(1)
        for (row in 0 until 3) {
            for (subrow in 0 until 4) {
                val colorBase = if (subrow % 2 != 0) 0x61 else 0x66
                var previousColor = -1
                for (column in 0..10) {
                    var color: Int
                    do {
                        color = colorBase + Seg002.prandom(3)
                    } while (color == previousColor)
                    gs.palaceWallColors[44 * row + 11 * subrow + column] = color
                    previousColor = color
                }
            }
        }
        gs.randomSeed = oldRandomSeed
    }
}

data class LevelScreenshot(
    val levelNumber: Int,
    val roomMap: LevelRoomMap,
    val image: BufferedImage
)

data class LevelRoomMap(
    val width: Int,
    val height: Int,
    val rooms: List<RoomPlacement>
)

data class RoomPlacement(
    val room: Int,
    val x: Int,
    val y: Int
)
