package com.sdlpop.assets

/**
 * JVM-testable asset contracts for the SDLPoP DAT/PNG pipeline.
 *
 * This package deliberately stops at bytes and metadata. Android Bitmap creation
 * is a later bridge layer; pure decompression and palette expansion can be
 * validated here without an Android runtime.
 */
enum class AssetLocation {
    DAT,
    DIRECTORY
}

data class DatResourceMetadata(
    val id: Int,
    val offset: Int,
    val size: Int,
    val location: AssetLocation = AssetLocation.DAT
)

data class DatArchiveMetadata(
    val tableOffset: Int,
    val tableSize: Int,
    val resources: List<DatResourceMetadata>
) {
    fun resource(id: Int): DatResourceMetadata? = resources.firstOrNull { it.id == id }
}

data class LoadedAssetResource(
    val id: Int,
    val extension: String,
    val sourceName: String,
    val location: AssetLocation,
    val bytes: ByteArray
) {
    val size: Int get() = bytes.size
}

data class Rgb6(val r: Int, val g: Int, val b: Int) {
    init {
        require(r in 0..0x3F && g in 0..0x3F && b in 0..0x3F) {
            "VGA palette channels are stored as 6-bit values"
        }
    }

    fun toArgb8888(alpha: Int = 0xFF): Int {
        require(alpha in 0..0xFF)
        return (alpha shl 24) or (r shl 18) or (g shl 10) or (b shl 2)
    }
}

data class DatPalette(
    val rowBits: Int,
    val nColors: Int,
    val vga: List<Rgb6>,
    val cga: List<Int>,
    val ega: List<Int>
)

data class SpritePaletteResource(
    val nImages: Int,
    val palette: DatPalette
)

data class ImageDataHeader(
    val height: Int,
    val width: Int,
    val flags: Int
) {
    val depth: Int get() = ((flags shr 12) and 0x07) + 1
    val compressionMethod: Int get() = (flags shr 8) and 0x0F
    val stride: Int get() = (depth * width + 7) / 8
    val compressedPayloadOffset: Int get() = IMAGE_DATA_HEADER_SIZE

    companion object {
        const val IMAGE_DATA_HEADER_SIZE = 6
    }
}

data class PngMetadata(
    val width: Int,
    val height: Int,
    val bitDepth: Int,
    val colorType: Int
)
