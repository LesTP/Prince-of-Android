package com.sdlpop.assets

object AssetParsers {
    private val pngSignature = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    )

    fun parseDatArchive(bytes: ByteArray): DatArchiveMetadata {
        require(bytes.size >= 8) { "DAT archive is too small" }
        val tableOffset = readLe32(bytes, 0)
        val tableSize = readLe16(bytes, 4)
        require(tableOffset >= 0 && tableOffset + tableSize <= bytes.size) {
            "DAT table points outside archive"
        }
        require(tableSize >= 2) { "DAT table is too small" }

        val count = readLe16(bytes, tableOffset)
        require(tableSize == 2 + count * 8) {
            "DAT table size does not match resource count"
        }

        val resources = List(count) { index ->
            val pos = tableOffset + 2 + index * 8
            val id = readLe16(bytes, pos)
            val offset = readLe32(bytes, pos + 2)
            val size = readLe16(bytes, pos + 6)
            require(offset >= 0 && offset + 1 + size <= bytes.size) {
                "DAT resource $id points outside archive"
            }
            DatResourceMetadata(id = id, offset = offset, size = size)
        }
        return DatArchiveMetadata(tableOffset, tableSize, resources)
    }

    fun parseSpritePaletteResource(bytes: ByteArray): SpritePaletteResource {
        require(bytes.size >= SPRITE_PALETTE_RESOURCE_SIZE) {
            "Sprite palette resource must be at least $SPRITE_PALETTE_RESOURCE_SIZE bytes"
        }
        val nImages = u8(bytes, 0)
        return SpritePaletteResource(
            nImages = nImages,
            palette = parseDatPalette(bytes, offset = 1)
        )
    }

    fun parseDatPalette(bytes: ByteArray, offset: Int = 0): DatPalette {
        require(offset >= 0 && offset + DAT_PALETTE_SIZE <= bytes.size) {
            "DAT palette points outside byte array"
        }
        val rowBits = readLe16(bytes, offset)
        val nColors = u8(bytes, offset + 2)
        val vgaOffset = offset + 3
        val cgaOffset = vgaOffset + 16 * 3
        val egaOffset = cgaOffset + 16

        val vga = List(16) { index ->
            val pos = vgaOffset + index * 3
            Rgb6(u8(bytes, pos), u8(bytes, pos + 1), u8(bytes, pos + 2))
        }
        val cga = List(16) { index -> u8(bytes, cgaOffset + index) }
        val ega = List(32) { index -> u8(bytes, egaOffset + index) }

        return DatPalette(rowBits, nColors, vga, cga, ega)
    }

    fun parseImageDataHeader(bytes: ByteArray, offset: Int = 0): ImageDataHeader {
        require(offset >= 0 && offset + ImageDataHeader.IMAGE_DATA_HEADER_SIZE <= bytes.size) {
            "Image data header points outside byte array"
        }
        return ImageDataHeader(
            height = readLe16(bytes, offset),
            width = readLe16(bytes, offset + 2),
            flags = readLe16(bytes, offset + 4)
        )
    }

    fun parsePngMetadata(bytes: ByteArray): PngMetadata {
        require(bytes.size >= 33) { "PNG is too small" }
        require(bytes.copyOfRange(0, pngSignature.size).contentEquals(pngSignature)) {
            "PNG signature mismatch"
        }
        val ihdrLength = readBe32(bytes, 8)
        require(ihdrLength == 13) { "PNG IHDR length must be 13" }
        require(String(bytes, 12, 4, Charsets.US_ASCII) == "IHDR") {
            "PNG first chunk is not IHDR"
        }
        return PngMetadata(
            width = readBe32(bytes, 16),
            height = readBe32(bytes, 20),
            bitDepth = u8(bytes, 24),
            colorType = u8(bytes, 25)
        )
    }

    fun readResourceBytes(datBytes: ByteArray, metadata: DatResourceMetadata): ByteArray {
        require(metadata.location == AssetLocation.DAT)
        val payloadOffset = metadata.offset + DAT_RESOURCE_CHECKSUM_SIZE
        return datBytes.copyOfRange(payloadOffset, payloadOffset + metadata.size)
    }

    private fun readLe16(bytes: ByteArray, offset: Int): Int =
        u8(bytes, offset) or (u8(bytes, offset + 1) shl 8)

    private fun readLe32(bytes: ByteArray, offset: Int): Int =
        u8(bytes, offset) or
            (u8(bytes, offset + 1) shl 8) or
            (u8(bytes, offset + 2) shl 16) or
            (u8(bytes, offset + 3) shl 24)

    private fun readBe32(bytes: ByteArray, offset: Int): Int =
        (u8(bytes, offset) shl 24) or
            (u8(bytes, offset + 1) shl 16) or
            (u8(bytes, offset + 2) shl 8) or
            u8(bytes, offset + 3)

    private fun u8(bytes: ByteArray, offset: Int): Int = bytes[offset].toInt() and 0xFF

    private const val DAT_PALETTE_SIZE = 99
    private const val DAT_RESOURCE_CHECKSUM_SIZE = 1
    private const val SPRITE_PALETTE_RESOURCE_SIZE = 100
}
