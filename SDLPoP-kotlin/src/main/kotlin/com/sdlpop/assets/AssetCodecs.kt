package com.sdlpop.assets

object AssetCodecs {
    fun calcStride(header: ImageDataHeader): Int =
        calcStride(header.width, header.depth)

    fun calcStride(width: Int, depth: Int): Int {
        require(width >= 0) { "width must be non-negative" }
        require(depth in 1..8) { "depth must be in 1..8" }
        return (depth * width + 7) / 8
    }

    fun decompressImage(imageBytes: ByteArray): ByteArray {
        val header = AssetParsers.parseImageDataHeader(imageBytes)
        val stride = calcStride(header)
        val destLength = stride * header.height
        val payload = imageBytes.copyOfRange(header.compressedPayloadOffset, imageBytes.size)
        return decompressImagePayload(
            payload = payload,
            compressionMethod = header.compressionMethod,
            destLength = destLength,
            stride = stride,
            height = header.height
        )
    }

    fun decompressImagePayload(
        payload: ByteArray,
        compressionMethod: Int,
        destLength: Int,
        stride: Int,
        height: Int
    ): ByteArray {
        require(destLength >= 0) { "destLength must be non-negative" }
        require(stride > 0 || destLength == 0) { "stride must be positive for non-empty images" }
        require(height > 0 || destLength == 0) { "height must be positive for non-empty images" }
        return when (compressionMethod) {
            0 -> payload.copyOf(destLength)
            1 -> decompressRleLr(payload, destLength)
            2 -> decompressRleUd(payload, destLength, stride, height)
            3 -> decompressLzgLr(payload, destLength)
            4 -> decompressLzgUd(payload, destLength, stride, height)
            else -> error("Unsupported image compression method $compressionMethod")
        }
    }

    fun expandTo8Bpp(packedData: ByteArray, width: Int, height: Int, stride: Int, depth: Int): ByteArray {
        require(width >= 0 && height >= 0) { "image dimensions must be non-negative" }
        require(stride >= calcStride(width, depth)) { "stride is too small for width/depth" }
        require(packedData.size >= stride * height) { "packed data is shorter than stride * height" }
        val out = ByteArray(width * height)
        val pixelsPerByte = 8 / depth
        val mask = (1 shl depth) - 1
        var outPos = 0
        for (y in 0 until height) {
            var xPixel = 0
            var inPos = y * stride
            repeat(stride) {
                val value = u8(packedData[inPos])
                var shift = 8
                repeat(pixelsPerByte) {
                    if (xPixel < width) {
                        shift -= depth
                        out[outPos++] = ((value shr shift) and mask).toByte()
                        xPixel++
                    }
                }
                inPos++
            }
        }
        return out
    }

    fun decompressRleLr(source: ByteArray, destLength: Int): ByteArray {
        val destination = ByteArray(destLength)
        var sourcePos = 0
        var destPos = 0
        var remaining = destLength
        while (remaining != 0) {
            var count = source[sourcePos++].toInt()
            if (count >= 0) {
                count++
                while (count != 0 && remaining != 0) {
                    destination[destPos++] = source[sourcePos++]
                    remaining--
                    count--
                }
            } else {
                val value = source[sourcePos++]
                count = -count
                while (count != 0 && remaining != 0) {
                    destination[destPos++] = value
                    remaining--
                    count--
                }
            }
        }
        return destination
    }

    fun decompressRleUd(source: ByteArray, destLength: Int, width: Int, height: Int): ByteArray {
        val destination = ByteArray(destLength)
        var sourcePos = 0
        var destPos = 0
        var remainingHeight = height
        var remaining = destLength
        val destEnd = destLength - 1
        val widthStep = width - 1
        while (remaining != 0) {
            var count = source[sourcePos++].toInt()
            if (count >= 0) {
                count++
                while (count != 0 && remaining != 0) {
                    destination[destPos] = source[sourcePos++]
                    destPos += 1 + widthStep
                    remainingHeight--
                    if (remainingHeight == 0) {
                        destPos -= destEnd
                        remainingHeight = height
                    }
                    remaining--
                    count--
                }
            } else {
                val value = source[sourcePos++]
                count = -count
                while (count != 0 && remaining != 0) {
                    destination[destPos] = value
                    destPos += 1 + widthStep
                    remainingHeight--
                    if (remainingHeight == 0) {
                        destPos -= destEnd
                        remainingHeight = height
                    }
                    remaining--
                    count--
                }
            }
        }
        return destination
    }

    fun decompressLzgLr(source: ByteArray, destLength: Int): ByteArray {
        val destination = ByteArray(destLength)
        val window = ByteArray(LZG_WINDOW_SIZE)
        var windowPos = LZG_WINDOW_SIZE - 0x42
        var sourcePos = 0
        var destPos = 0
        var remaining = destLength
        var mask = 0
        while (remaining != 0) {
            mask = mask ushr 1
            if ((mask and 0xFF00) == 0) {
                mask = u8(source[sourcePos++]) or 0xFF00
            }
            if ((mask and 1) != 0) {
                val value = source[sourcePos++]
                window[windowPos] = value
                destination[destPos++] = value
                windowPos = nextWindowPos(windowPos)
                remaining--
            } else {
                val copyInfo = (u8(source[sourcePos++]) shl 8) or u8(source[sourcePos++])
                var copySource = copyInfo and 0x03FF
                var copyLength = (copyInfo ushr 10) + 3
                while (remaining != 0 && copyLength != 0) {
                    val value = window[copySource]
                    window[windowPos] = value
                    destination[destPos++] = value
                    copySource = nextWindowPos(copySource)
                    windowPos = nextWindowPos(windowPos)
                    remaining--
                    copyLength--
                }
            }
        }
        return destination
    }

    fun decompressLzgUd(source: ByteArray, destLength: Int, stride: Int, height: Int): ByteArray {
        val destination = ByteArray(destLength)
        val window = ByteArray(LZG_WINDOW_SIZE)
        var windowPos = LZG_WINDOW_SIZE - 0x42
        var sourcePos = 0
        var destPos = 0
        var remainingHeight = height
        var remaining = destLength
        val destEnd = destLength - 1
        var mask = 0
        while (remaining != 0) {
            mask = mask ushr 1
            if ((mask and 0xFF00) == 0) {
                mask = u8(source[sourcePos++]) or 0xFF00
            }
            if ((mask and 1) != 0) {
                val value = source[sourcePos++]
                window[windowPos] = value
                destination[destPos] = value
                windowPos = nextWindowPos(windowPos)
                destPos += stride
                remainingHeight--
                if (remainingHeight == 0) {
                    destPos -= destEnd
                    remainingHeight = height
                }
                remaining--
            } else {
                val copyInfo = (u8(source[sourcePos++]) shl 8) or u8(source[sourcePos++])
                var copySource = copyInfo and 0x03FF
                var copyLength = (copyInfo ushr 10) + 3
                while (remaining != 0 && copyLength != 0) {
                    val value = window[copySource]
                    window[windowPos] = value
                    destination[destPos] = value
                    copySource = nextWindowPos(copySource)
                    windowPos = nextWindowPos(windowPos)
                    destPos += stride
                    remainingHeight--
                    if (remainingHeight == 0) {
                        destPos -= destEnd
                        remainingHeight = height
                    }
                    remaining--
                    copyLength--
                }
            }
        }
        return destination
    }

    private fun nextWindowPos(pos: Int): Int =
        (pos + 1) and (LZG_WINDOW_SIZE - 1)

    private fun u8(value: Byte): Int = value.toInt() and 0xFF

    private const val LZG_WINDOW_SIZE = 0x400
}
