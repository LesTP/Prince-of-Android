package com.sdlpop.assets

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertContentEquals
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
        assertEquals(ImageDataHeader(height = 5, width = 6, flags = 0xB400), header)
        assertEquals(4, header.depth)
        assertEquals(4, header.compressionMethod)
        assertEquals(3, header.stride)
        assertEquals(6, header.compressedPayloadOffset)
        assertEquals(14, imageBytes.size - header.compressedPayloadOffset)
    }

    @Test
    fun `RLE left-to-right copy and repeat packets match C decoder`() {
        val source = byteArrayOf(
            1, 0x12, 0x34,
            0xFE.toByte(), 0xAA.toByte(),
            0, 0x55
        )

        assertContentEquals(
            byteArrayOf(0x12, 0x34, 0xAA.toByte(), 0xAA.toByte(), 0x55),
            AssetCodecs.decompressRleLr(source, destLength = 5)
        )
    }

    @Test
    fun `DAT image decompression supports raw RLE and LZG resource fixtures`() {
        assertDecodedFixture(
            resourceId = 776,
            expectedHeader = ImageDataHeader(height = 1, width = 1, flags = 0),
            expectedDecompressedPrefix = listOf(0),
            expectedPixelsPrefix = listOf(0),
            expectedDecompressedSum = 0,
            expectedPixelSum = 0
        )
        assertDecodedFixture(
            resourceId = 768,
            expectedHeader = ImageDataHeader(height = 42, width = 27, flags = 0xB200),
            expectedDecompressedPrefix = listOf(0, 0, 0, 0, 0, 14, 51, 51, 51, 224, 0, 0, 0, 0, 0, 0),
            expectedPixelsPrefix = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 14, 3, 3, 3, 3, 3, 3, 14, 0, 0, 0, 0, 0),
            expectedDecompressedSum = 27802,
            expectedPixelSum = 3259
        )
        assertDecodedFixture(
            resourceId = 752,
            expectedHeader = ImageDataHeader(height = 26, width = 28, flags = 0xB300),
            expectedDecompressedPrefix = listOf(0, 0, 0, 0, 0, 0, 0, 17, 0, 0, 0, 0, 0, 0, 0, 0),
            expectedPixelsPrefix = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0),
            expectedDecompressedSum = 11019,
            expectedPixelSum = 1299
        )
        assertDecodedFixture(
            resourceId = 751,
            expectedHeader = ImageDataHeader(height = 5, width = 6, flags = 0xB400),
            expectedDecompressedPrefix = listOf(0, 0, 33, 0, 33, 17, 33, 17, 17, 0, 33, 17, 0, 0, 33),
            expectedPixelsPrefix = listOf(0, 0, 0, 0, 2, 1, 0, 0, 2, 1, 1, 1, 2, 1, 1, 1, 1, 1, 0, 0, 2, 1, 1, 1),
            expectedDecompressedSum = 233,
            expectedPixelSum = 23
        )
    }

    @Test
    fun `packed pixel expansion handles 1 2 and 4 bpp rows`() {
        assertContentEquals(
            byteArrayOf(1, 0, 1, 0, 0, 1, 0, 1),
            AssetCodecs.expandTo8Bpp(byteArrayOf(0b10100101.toByte()), width = 8, height = 1, stride = 1, depth = 1)
        )
        assertContentEquals(
            byteArrayOf(3, 0, 2, 1),
            AssetCodecs.expandTo8Bpp(byteArrayOf(0b11001001.toByte()), width = 4, height = 1, stride = 1, depth = 2)
        )
        assertContentEquals(
            byteArrayOf(10, 11, 12),
            AssetCodecs.expandTo8Bpp(byteArrayOf(0xAB.toByte(), 0xC0.toByte()), width = 3, height = 1, stride = 2, depth = 4)
        )
    }

    private fun readAsset(relativePath: String): ByteArray =
        Files.readAllBytes(assetsRoot.resolve(relativePath))

    private fun assertDecodedFixture(
        resourceId: Int,
        expectedHeader: ImageDataHeader,
        expectedDecompressedPrefix: List<Int>,
        expectedPixelsPrefix: List<Int>,
        expectedDecompressedSum: Int,
        expectedPixelSum: Int
    ) {
        val datBytes = readAsset("GUARD.DAT")
        val archive = AssetParsers.parseDatArchive(datBytes)
        val imageBytes = AssetParsers.readResourceBytes(datBytes, assertNotNull(archive.resource(resourceId)))
        val header = AssetParsers.parseImageDataHeader(imageBytes)

        assertEquals(expectedHeader, header)

        val decompressed = AssetCodecs.decompressImage(imageBytes)
        assertEquals(header.stride * header.height, decompressed.size)
        assertEquals(expectedDecompressedPrefix, decompressed.take(expectedDecompressedPrefix.size).map { it.toInt() and 0xFF })
        assertEquals(expectedDecompressedSum, decompressed.sumOf { it.toInt() and 0xFF })

        val pixels = AssetCodecs.expandTo8Bpp(decompressed, header.width, header.height, header.stride, header.depth)
        assertEquals(header.width * header.height, pixels.size)
        assertEquals(expectedPixelsPrefix, pixels.take(expectedPixelsPrefix.size).map { it.toInt() and 0xFF })
        assertEquals(expectedPixelSum, pixels.sumOf { it.toInt() and 0xFF })
    }

    private fun findAssetsRoot(): Path {
        var current = Paths.get("").toAbsolutePath()
        while (true) {
            val candidate = current.resolve("app/src/main/assets")
            if (Files.isDirectory(candidate)) return candidate
            current = current.parent ?: error("Cannot locate app/src/main/assets")
        }
    }
}
