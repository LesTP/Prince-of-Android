package com.sdlpop.android

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.sdlpop.assets.DatDecodedImage
import com.sdlpop.assets.DecodedAssetImage
import com.sdlpop.assets.PngDecodedImage
import com.sdlpop.assets.SpriteCatalog

/**
 * Pre-converts any [PngDecodedImage] entries in a [SpriteCatalog] to [DatDecodedImage]
 * using Android's BitmapFactory. This eliminates the javax.imageio dependency that
 * would crash on Android when [RenderTableFlusher] calls asRenderSprite().
 */
object CatalogPreDecoder {

    private val decodeOptions = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }

    fun preDecodeForAndroid(catalog: SpriteCatalog): SpriteCatalog {
        val converted = catalog.images.map { image ->
            when (image) {
                is PngDecodedImage -> decodePngToArgb(image)
                else -> image // DatDecodedImage or null — already ready
            }
        }
        return catalog.copy(images = converted)
    }

    private fun decodePngToArgb(png: PngDecodedImage): DatDecodedImage {
        val bitmap = BitmapFactory.decodeByteArray(
            png.pngBytes, 0, png.pngBytes.size, decodeOptions
        ) ?: error("BitmapFactory failed to decode sprite from ${png.source.sourceName}")

        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        bitmap.recycle()

        return DatDecodedImage(
            width = w,
            height = h,
            argbPixels = pixels,
            source = png.source
        )
    }
}
