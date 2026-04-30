package com.sdlpop.assets

/**
 * Byte source boundary for Android AssetManager, JVM tests, or any later asset
 * provider. Paths are always slash-separated and relative to app assets root.
 */
interface AssetByteSource {
    fun readBytes(path: String): ByteArray?
    fun isDirectory(path: String): Boolean
}

class AssetRepository(private val source: AssetByteSource) {
    private val openDats = mutableListOf<OpenDat>()

    fun openDat(filename: String, optional: Boolean = false): OpenDat? {
        val datBytes = source.readBytes(filename)
        val archive = datBytes?.let { AssetParsers.parseDatArchive(it) }
        val directory = filename.withoutDatExtension()
        val hasDirectory = source.isDirectory(directory)

        if (datBytes == null && !hasDirectory) {
            if (optional) return null
            error("Cannot find required data file $filename or folder $directory")
        }

        return OpenDat(filename, datBytes, archive, directory, hasDirectory).also {
            openDats.add(0, it)
        }
    }

    fun closeDat(handle: OpenDat) {
        openDats.remove(handle)
    }

    fun loadFromOpenDatsAlloc(resourceId: Int, extension: String): LoadedAssetResource? {
        val normalizedExtension = extension.trimStart('.')
        for (handle in openDats) {
            val datResource = handle.datBytes?.let { datBytes ->
                handle.archive?.resource(resourceId)?.let { metadata ->
                    val bytes = AssetParsers.readResourceBytes(datBytes, metadata)
                    if (normalizedExtension == "png" && metadata.size <= 2) {
                        null
                    } else {
                        LoadedAssetResource(
                            id = resourceId,
                            extension = normalizedExtension,
                            sourceName = handle.filename,
                            location = AssetLocation.DAT,
                            bytes = bytes
                        )
                    }
                }
            }
            if (datResource != null) return datResource

            val directoryResource = if (handle.hasDirectory) {
                val path = "${handle.directory}/res$resourceId.$normalizedExtension"
                source.readBytes(path)?.let { bytes ->
                    LoadedAssetResource(
                        id = resourceId,
                        extension = normalizedExtension,
                        sourceName = path,
                        location = AssetLocation.DIRECTORY,
                        bytes = bytes
                    )
                }
            } else {
                null
            }
            if (directoryResource != null) return directoryResource
        }
        return null
    }

    fun loadFromOpenDatsToArea(resourceId: Int, destination: ByteArray, extension: String): Int {
        val resource = loadFromOpenDatsAlloc(resourceId, extension) ?: return 0
        resource.bytes.copyInto(destination, endIndex = minOf(resource.bytes.size, destination.size))
        return minOf(resource.bytes.size, destination.size)
    }
}

data class OpenDat internal constructor(
    val filename: String,
    internal val datBytes: ByteArray?,
    internal val archive: DatArchiveMetadata?,
    internal val directory: String,
    internal val hasDirectory: Boolean
) {
    val isDatBacked: Boolean get() = datBytes != null
}

private fun String.withoutDatExtension(): String =
    if (length >= 5 && substring(length - 4).equals(".DAT", ignoreCase = true)) {
        substring(0, length - 4)
    } else {
        this
    }
