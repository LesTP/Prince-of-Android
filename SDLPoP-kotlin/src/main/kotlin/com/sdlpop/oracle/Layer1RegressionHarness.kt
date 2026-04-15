package com.sdlpop.oracle

import com.sdlpop.replay.ReplayRunner
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories

data class Layer1ReplayTrace(
    val id: String,
    val referenceFile: String,
    val replayFile: String,
) {
    fun referencePath(referenceRoot: Path): Path = referenceRoot.resolve(referenceFile)
    fun replayPath(replayRoot: Path): Path = replayRoot.resolve(replayFile)
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
    private val produceKotlinTrace: (Layer1ReplayTrace, Path) -> Path = { replay, outputPath ->
        ReplayRunner.writeLayer1Trace(
            replay,
            Paths.get(System.getProperty("sdlpop.replayRoot", "../SDLPoP")),
            outputPath,
        )
    },
) {
    fun run(replays: List<Layer1ReplayTrace> = Layer1RegressionManifest.replays): List<Layer1RegressionResult> {
        outputRoot.createDirectories()
        val normalizedOutputRoot = outputRoot.toAbsolutePath().normalize()
        return replays.map { replay ->
            val referencePath = replay.referencePath(referenceRoot)
            require(Files.isRegularFile(referencePath)) {
                "Missing reference trace for ${replay.id}: $referencePath"
            }

            val actualPath = produceKotlinTrace(replay, outputRoot.resolve("${replay.id}.trace"))
                .toAbsolutePath()
                .normalize()
            require(actualPath.startsWith(normalizedOutputRoot)) {
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
        Layer1ReplayTrace("basic_movement", "basic_movement.trace", "replays/basic movement.p1r"),
        Layer1ReplayTrace("demo_suave_prince_level11", "demo_suave_prince_level11.trace", "doc/replays-testcases/Demo by Suave Prince level 11.p1r"),
        Layer1ReplayTrace("falling", "falling.trace", "replays/falling.p1r"),
        Layer1ReplayTrace("falling_through_floor_pr274", "falling_through_floor_pr274.trace", "doc/replays-testcases/Falling through floor (PR274).p1r"),
        Layer1ReplayTrace("grab_bug_pr288", "grab_bug_pr288.trace", "doc/replays-testcases/Grab bug (PR288).p1r"),
        Layer1ReplayTrace("grab_bug_pr289", "grab_bug_pr289.trace", "doc/replays-testcases/Grab bug (PR289).p1r"),
        Layer1ReplayTrace("original_level12_xpos_glitch", "original_level12_xpos_glitch.trace", "doc/replays-testcases/Original level 12 xpos glitch.p1r"),
        Layer1ReplayTrace("original_level2_falling_into_wall", "original_level2_falling_into_wall.trace", "doc/replays-testcases/Original level 2 falling into wall.p1r"),
        Layer1ReplayTrace("original_level5_shadow_into_wall", "original_level5_shadow_into_wall.trace", "doc/replays-testcases/Original level 5 shadow into wall.p1r"),
        Layer1ReplayTrace("snes_pc_set_level11", "snes_pc_set_level11.trace", "doc/replays-testcases/SNES-PC-set level 11.p1r"),
        Layer1ReplayTrace("sword_and_level_transition", "sword_and_level_transition.trace", "replays/sword and level transition.p1r"),
        Layer1ReplayTrace("traps", "traps.trace", "replays/traps.p1r"),
        Layer1ReplayTrace("trick_153", "trick_153.trace", "doc/replays-testcases/trick_153.p1r"),
    )

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

    fun fromReplayRoot(replayRoot: Path): List<Layer1ReplayTrace> {
        val missing = replays
            .map { it.replayPath(replayRoot) }
            .filterNot { Files.isRegularFile(it) }
        require(missing.isEmpty()) {
            "Missing Layer 1 replay sources: ${missing.joinToString()}"
        }

        return replays
    }
}
