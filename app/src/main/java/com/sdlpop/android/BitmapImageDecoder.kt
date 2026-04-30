package com.sdlpop.android

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.sdlpop.assets.DatDecodedImage
import com.sdlpop.assets.DecodedAssetImage
import com.sdlpop.assets.PngDecodedImage

object BitmapImageDecoder {
    fun toBitmap(image: DecodedAssetImage): Bitmap =
        when (image) {
            is DatDecodedImage -> Bitmap.createBitmap(
                image.argbPixels,
                image.width,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            is PngDecodedImage -> BitmapFactory.decodeByteArray(
                image.pngBytes,
                0,
                image.pngBytes.size
            ) ?: error("BitmapFactory could not decode ${image.source.sourceName}")
        }
}
