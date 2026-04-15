package com.sdlpop.oracle

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories

data class Layer1ReplayTrace(
    val id: String,
    val referenceFile: String,
) {
    fun referencePath(referenceRoot: Path): Path = referenceRoot.resolve(referenceFile)
}

data class Layer1RegressionResult(
    val replay: Layer1ReplayTrace,
    val referencePath: Path,
    val actualPath: Path,
    val comparison: TraceComparison,
) {
    val matched: Boolean = comparison.matched

    fun triageReport(): String {
        if (matched) {
            return "${replay.id}: MATCH (${referencePath.fileName} == ${actualPath.fileName})"
        }

        val divergence = comparison.divergence
            ?: return "${replay.id}: DIVERGED without detailed trace metadata"

        return buildString {
            append("${replay.id}: DIVERGED")
            append(" frame=${divergence.frameIndex}")
            divergence.frameNumber?.let { append(" frame_number=$it") }
            divergence.fieldName?.let { append(" field=$it") }
            divergence.byteOffsetInFrame?.let { append(" byte_offset=$it") }
            append(" expected=${divergence.expectedValue}")
            append(" actual=${divergence.actualValue}")
            append(" reference=$referencePath")
            append(" actual_trace=$actualPath")
        }
    }
}

class Layer1RegressionHarness(
    private val referenceRoot: Path,
    private val outputRoot: Path,
    private val produceKotlinTrace: (Layer1ReplayTrace, Path) -> Path,
) {
    fun run(replays: List<Layer1ReplayTrace> = Layer1RegressionManifest.replays): List<Layer1RegressionResult> {
        outputRoot.createDirectories()
        return replays.map { replay ->
            val referencePath = replay.referencePath(referenceRoot)
            require(Files.isRegularFile(referencePath)) {
                "Missing reference trace for ${replay.id}: $referencePath"
            }

            val actualPath = produceKotlinTrace(replay, outputRoot.resolve("${replay.id}.trace"))
            require(actualPath.startsWith(outputRoot)) {
                "Kotlin trace for ${replay.id} must be written under build output: $actualPath"
            }
            require(Files.isRegularFile(actualPath)) {
                "Kotlin trace producer did not write ${replay.id}: $actualPath"
            }

            val comparison = StateTraceFormat.compare(
                StateTraceFormat.parse(referencePath),
                StateTraceFormat.parse(actualPath),
            )
            Layer1RegressionResult(replay, referencePath, actualPath, comparison)
        }
    }

    fun runAndRequireMatch(replays: List<Layer1ReplayTrace> = Layer1RegressionManifest.replays) {
        val results = run(replays)
        val failures = results.filterNot { it.matched }
        check(failures.isEmpty()) {
            failures.joinToString(separator = "\n") { it.triageReport() }
        }
    }
}

object Layer1RegressionManifest {
    val replays: List<Layer1ReplayTrace> = listOf(
        "basic_movement",
        "demo_suave_prince_level11",
        "falling",
        "falling_through_floor_pr274",
        "grab_bug_pr288",
        "grab_bug_pr289",
        "original_level12_xpos_glitch",
        "original_level2_falling_into_wall",
        "original_level5_shadow_into_wall",
        "snes_pc_set_level11",
        "sword_and_level_transition",
        "traps",
        "trick_153",
    ).map { id -> Layer1ReplayTrace(id = id, referenceFile = "$id.trace") }

    fun fromReferenceRoot(referenceRoot: Path): List<Layer1ReplayTrace> {
        val missing = replays
            .map { it.referencePath(referenceRoot) }
            .filterNot { Files.isRegularFile(it) }
        require(missing.isEmpty()) {
            "Missing Layer 1 reference traces: ${missing.joinToString()}"
        }

        val unexpected = Files.list(referenceRoot).use { paths ->
            paths
                .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".trace") }
                .map { it.fileName.toString().removeSuffix(".trace") }
                .filter { id -> replays.none { it.id == id } }
                .toList()
        }
        require(unexpected.isEmpty()) {
            "Reference trace manifest must list every .trace file; unexpected: ${unexpected.joinToString()}"
        }

        return replays
    }
}
