package com.sdlpop

import com.sdlpop.assets.AssetByteSource
import com.sdlpop.assets.AssetRepository
import com.sdlpop.render.LevelScreenshotGenerator
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

fun main(args: Array<String>) {
    if (args.firstOrNull() != "render-level-screenshots") {
        usage()
        return
    }

    var assetsRoot: Path? = null
    var outputRoot: Path? = null
    val levels = mutableListOf<Int>()
    var index = 1
    while (index < args.size) {
        when (val arg = args[index]) {
            "--assets" -> assetsRoot = Paths.get(args.getOrElse(++index) { error("Missing --assets value") })
            "--out" -> outputRoot = Paths.get(args.getOrElse(++index) { error("Missing --out value") })
            "--levels" -> {
                val value = args.getOrElse(++index) { error("Missing --levels value") }
                levels += parseLevels(value)
            }
            else -> error("Unknown argument: $arg")
        }
        index++
    }

    val assets = assetsRoot ?: Paths.get("../app/src/main/assets")
    val output = outputRoot ?: Paths.get("build/render")
    val selectedLevels = if (levels.isEmpty()) (1..14).toList() else levels
    val generator = LevelScreenshotGenerator(AssetRepository(FilesystemAssetByteSource(assets)))

    for (level in selectedLevels) {
        val screenshot = generator.renderLevel(level)
        val path = output.resolve("level_%02d_kotlin.png".format(level))
        generator.writePng(screenshot, path)
        println("Wrote ${path.absolutePathString()}")
    }
}

private fun parseLevels(value: String): List<Int> =
    value.split(",").flatMap { part ->
        val trimmed = part.trim()
        if ("-" in trimmed) {
            val start = trimmed.substringBefore("-").toInt()
            val end = trimmed.substringAfter("-").toInt()
            start..end
        } else {
            listOf(trimmed.toInt())
        }
    }.onEach { require(it in 1..14) { "Level must be in 1..14: $it" } }

private fun usage() {
    println("Usage: gradle run --args='render-level-screenshots --assets ../app/src/main/assets --out build/render --levels 1-14'")
}

private class FilesystemAssetByteSource(private val root: Path) : AssetByteSource {
    override fun readBytes(path: String): ByteArray? {
        val resolved = root.resolve(path)
        return if (Files.isRegularFile(resolved)) Files.readAllBytes(resolved) else null
    }

    override fun isDirectory(path: String): Boolean =
        Files.isDirectory(root.resolve(path))
}
