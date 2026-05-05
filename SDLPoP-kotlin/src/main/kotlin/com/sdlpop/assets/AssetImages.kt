package com.sdlpop.assets

object AssetImages {
    fun decodeDatImage(imageBytes: ByteArray, palette: DatPalette, source: LoadedAssetResource): DatDecodedImage {
        val header = AssetParsers.parseImageDataHeader(imageBytes)
        if (header.height == 0) {
            error("Zero-height DAT images do not decode to a drawable image")
        }

        val decompressed = AssetCodecs.decompressImage(imageBytes)
        val indexes = AssetCodecs.expandTo8Bpp(
            packedData = decompressed,
            width = header.width,
            height = header.height,
            stride = header.stride,
            depth = header.depth
        )
        val pixels = IntArray(header.width * header.height) { index ->
            paletteColorToArgb(palette, indexes[index].toInt() and 0xFF)
        }
        return DatDecodedImage(
            width = header.width,
            height = header.height,
            argbPixels = pixels,
            source = source
        )
    }

    fun loadImage(repository: AssetRepository, resourceId: Int, palette: DatPalette): DecodedAssetImage? {
        val resource = repository.loadFromOpenDatsAlloc(resourceId, "png") ?: return null
        return when (resource.location) {
            AssetLocation.DAT -> decodeDatImage(resource.bytes, palette, resource)
            AssetLocation.DIRECTORY -> {
                val metadata = AssetParsers.parsePngMetadata(resource.bytes)
                PngDecodedImage(
                    width = metadata.width,
                    height = metadata.height,
                    pngBytes = resource.bytes,
                    source = resource
                )
            }
        }
    }

    fun loadSpriteCatalog(
        repository: AssetRepository,
        chtabId: Int,
        paletteResourceId: Int,
        paletteBits: Int = 0
    ): SpriteCatalog? {
        val paletteResource = repository.loadFromOpenDatsAlloc(paletteResourceId, "pal") ?: return null
        val spritePalette = AssetParsers.parseSpritePaletteResource(paletteResource.bytes)
        val palette = if (paletteBits != 0) {
            spritePalette.palette.copy(rowBits = paletteBits)
        } else {
            spritePalette.palette
        }

        val images = List(spritePalette.nImages) { index ->
            loadImage(repository, paletteResourceId + index + 1, palette)
        }
        return SpriteCatalog(
            chtabId = chtabId,
            paletteResourceId = paletteResourceId,
            palette = palette,
            images = images
        )
    }

    private fun paletteColorToArgb(palette: DatPalette, colorIndex: Int): Int {
        val clampedIndex = colorIndex and 0x0F
        return if (clampedIndex == TRANSPARENT_COLOR_INDEX) {
            // Store actual RGB but with alpha=0 so masked blits treat it as transparent.
            // NO_TRANSP blit (source | ALPHA_MASK) will make it opaque with the correct color.
            palette.vga[0].toArgb8888(alpha = 0)
        } else {
            palette.vga[clampedIndex].toArgb8888(alpha = 0xFF)
        }
    }

    private const val TRANSPARENT_COLOR_INDEX = 0
}
