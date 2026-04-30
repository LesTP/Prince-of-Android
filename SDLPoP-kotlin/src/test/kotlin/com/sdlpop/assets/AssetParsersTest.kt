package com.sdlpop.assets

import com.sdlpop.game.Chtabs
import com.sdlpop.game.SpriteDimensions
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
    fun `asset repository opens DAT archives and directory-backed assets`() {
        val repository = AssetRepository(FilesystemAssetByteSource(assetsRoot))

        val missing = repository.openDat("KID.DAT", optional = true)
        assertNotNull(missing)
        assertEquals(false, missing.isDatBacked)

        val palette = assertNotNull(repository.loadFromOpenDatsAlloc(400, "pal"))
        assertEquals(AssetLocation.DIRECTORY, palette.location)
        assertEquals("KID/res400.pal", palette.sourceName)
        assertEquals(219, AssetParsers.parseSpritePaletteResource(palette.bytes).nImages)

        repository.closeDat(missing)
        assertNull(repository.loadFromOpenDatsAlloc(400, "pal"))
    }

    @Test
    fun `asset repository loads GUARD DAT bytes before matching PNG directory resources`() {
        val repository = AssetRepository(FilesystemAssetByteSource(assetsRoot))
        val guard = assertNotNull(repository.openDat("GUARD.DAT"))
        assertEquals(true, guard.isDatBacked)

        val datImage = assertNotNull(repository.loadFromOpenDatsMetadata(751, "png"))
        assertEquals(AssetLocation.DAT, datImage.location)
        assertEquals("GUARD.DAT", datImage.sourceName)
        assertEquals(ImageDataHeader(height = 5, width = 6, flags = 0xB400), AssetParsers.parseImageDataHeader(datImage.bytes))

        val secondDatImage = assertNotNull(repository.loadFromOpenDatsAlloc(752, "png"))
        assertEquals(AssetLocation.DAT, secondDatImage.location)
        assertEquals(ImageDataHeader(height = 26, width = 28, flags = 0xB300), AssetParsers.parseImageDataHeader(secondDatImage.bytes))
    }

    @Test
    fun `asset repository falls back to PNG resources in directory-only archives`() {
        val repository = AssetRepository(FilesystemAssetByteSource(assetsRoot))
        repository.openDat("KID.DAT", optional = true)

        val directoryPng = assertNotNull(repository.loadFromOpenDatsAlloc(401, "png"))
        assertEquals(AssetLocation.DIRECTORY, directoryPng.location)
        assertEquals("KID/res401.png", directoryPng.sourceName)
        assertEquals(PngMetadata(width = 12, height = 42, bitDepth = 4, colorType = 3), AssetParsers.parsePngMetadata(directoryPng.bytes))
    }

    @Test
    fun `asset repository copies resource data into caller-owned area`() {
        val repository = AssetRepository(FilesystemAssetByteSource(assetsRoot))
        repository.openDat("GUARD.DAT")

        val destination = ByteArray(8)
        val copied = repository.loadFromOpenDatsToArea(751, destination, "png")

        assertEquals(8, copied)
        assertContentEquals(byteArrayOf(5, 0, 6, 0, 0, 0xB4.toByte(), 0xB7.toByte(), 0), destination)
    }

    @Test
    fun `DAT image decode converts indexed pixels through VGA palette to ARGB`() {
        val repository = AssetRepository(FilesystemAssetByteSource(assetsRoot))
        repository.openDat("GUARD.DAT")
        repository.openDat("GUARD1.DAT")
        val paletteResource = AssetParsers.parseSpritePaletteResource(
            assertNotNull(repository.loadFromOpenDatsAlloc(750, "pal")).bytes
        )

        val image = assertNotNull(AssetImages.loadImage(repository, 751, paletteResource.palette)) as DatDecodedImage

        assertEquals(6, image.width)
        assertEquals(5, image.height)
        assertEquals(30, image.argbPixels.size)
        assertEquals(0x00000000, image.argbPixels[0])
        assertEquals(paletteResource.palette.vga[2].toArgb8888(), image.argbPixels[4])
    }

    @Test
    fun `KID sprite catalog loads PNG dimensions matching headless table`() {
        val repository = AssetRepository(FilesystemAssetByteSource(assetsRoot))
        repository.openDat("KID.DAT", optional = true)

        val catalog = assertNotNull(
            AssetImages.loadSpriteCatalog(
                repository = repository,
                chtabId = Chtabs.KID,
                paletteResourceId = 400,
                paletteBits = 1 shl 1
            )
        )

        assertEquals(219, catalog.imageCount)
        for (frameId in 1..catalog.imageCount) {
            assertEquals(SpriteDimensions.getImageDimensions(Chtabs.KID, frameId), catalog.dimensionsByFrameId(frameId))
        }
    }

    @Test
    fun `GUARD sprite catalog loads DAT dimensions matching headless table`() {
        val repository = AssetRepository(FilesystemAssetByteSource(assetsRoot))
        repository.openDat("GUARD.DAT")
        repository.openDat("GUARD1.DAT")

        val catalog = assertNotNull(
            AssetImages.loadSpriteCatalog(
                repository = repository,
                chtabId = Chtabs.GUARD,
                paletteResourceId = 750,
                paletteBits = 1 shl 1
            )
        )

        assertEquals(34, catalog.imageCount)
        for (frameId in 1..catalog.imageCount) {
            assertEquals(SpriteDimensions.getImageDimensions(Chtabs.GUARD, frameId), catalog.dimensionsByFrameId(frameId))
        }
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

    private class FilesystemAssetByteSource(private val root: Path) : AssetByteSource {
        override fun readBytes(path: String): ByteArray? {
            val resolved = root.resolve(path)
            return if (Files.isRegularFile(resolved)) Files.readAllBytes(resolved) else null
        }

        override fun isDirectory(path: String): Boolean =
            Files.isDirectory(root.resolve(path))
    }
}
