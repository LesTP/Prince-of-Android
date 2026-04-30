package com.sdlpop.android

import android.content.res.AssetManager
import com.sdlpop.assets.AssetByteSource

class AndroidAssetSource(private val assets: AssetManager) : AssetByteSource {
    override fun readBytes(path: String): ByteArray? =
        runCatching { assets.open(path).use { it.readBytes() } }.getOrNull()

    override fun isDirectory(path: String): Boolean =
        runCatching { assets.list(path)?.isNotEmpty() == true }.getOrDefault(false)
}
