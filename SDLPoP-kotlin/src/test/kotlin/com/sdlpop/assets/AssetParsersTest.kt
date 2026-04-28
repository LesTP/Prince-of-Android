package com.sdlpop.assets

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AssetParsersTest {
    private val assetsRoot = findAssetsRoot()

    @Test
    fun `KID palette resource exposes sprite count and VGA colors`() {
        val resource = AssetParsers.parseSpritePaletteResource(readAsset("KID/res400.pal"))

        assertEquals(219, resource.nImages)
        assertEquals(0, resource.palette.rowBits)
        assertEquals(16, resource.palette.nColors)
        assertEquals(Rgb6(0, 0, 0), resource.palette.vga[0])
        assertEquals(Rgb6(0x2E, 0x24, 0), resource.palette.vga[1])
    }

    @Test
    fun `packaged PNG dimensions match headless KID and GUARD fixture dimensions`() {
        val kidStand = AssetParsers.parsePngMetadata(readAsset("KID/res401.png"))
        val kidLast = AssetParsers.parsePngMetadata(readAsset("KID/res619.png"))
        val guardFirst = AssetParsers.parsePngMetadata(readAsset("GUARD/res751.png"))
        val dungeonFloor = AssetParsers.parsePngMetadata(readAsset("VDUNGEON/res242.png"))

        assertEquals(PngMetadata(width = 12, height = 42, bitDepth = 4, colorType = 3), kidStand)
        assertEquals(PngMetadata(width = 28, height = 26, bitDepth = 4, colorType = 3), kidLast)
        assertEquals(PngMetadata(width = 6, height = 5, bitDepth = 4, colorType = 3), guardFirst)
        assertEquals(PngMetadata(width = 26, height = 15, bitDepth = 4, colorType = 3), dungeonFloor)
    }

    @Test
    fun `GUARD DAT table exposes compressed image headers`() {
        val datBytes = readAsset("GUARD.DAT")
        val archive = AssetParsers.parseDatArchive(datBytes)

        assertEquals(6676, archive.tableOffset)
        assertEquals(274, archive.tableSize)
        assertEquals(34, archive.resources.size)

        val first = assertNotNull(archive.resource(751))
        assertEquals(DatResourceMetadata(id = 751, offset = 6, size = 20), first)

        val imageBytes = AssetParsers.readResourceBytes(datBytes, first)
        val header = AssetParsers.parseImageDataHeader(imageBytes)
        assertEquals(ImageDataHeader(height = 5, width = 6, flags = 0), header)
        assertEquals(1, header.depth)
        assertEquals(0, header.compressionMethod)
        assertEquals(1, header.stride)
        assertEquals(6, header.compressedPayloadOffset)
        assertEquals(14, imageBytes.size - header.compressedPayloadOffset)
    }

    private fun readAsset(relativePath: String): ByteArray =
        Files.readAllBytes(assetsRoot.resolve(relativePath))

    private fun findAssetsRoot(): Path {
        var current = Paths.get("").toAbsolutePath()
        while (true) {
            val candidate = current.resolve("app/src/main/assets")
            if (Files.isDirectory(candidate)) return candidate
            current = current.parent ?: error("Cannot locate app/src/main/assets")
        }
    }
}
