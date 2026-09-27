package com.blackwake.game

import java.util.Locale
import java.util.TreeMap
import kotlin.math.abs
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SoakTest {
    private enum class Policy { CAUTIOUS, RECKLESS }

    private class Bot {
        // Keep the commanded lane as memory; the cross-version observation surface
        // deliberately does not expose the boat's lateral position to the bot.
        private var lane = 1

        fun choose(run: RunState): Decision {
            val canDive = run.dive.oxygen >= 15f && !run.dive.lockedOut
            val scores = IntArray(3)
            val unsafe = BooleanArray(3)
            for (entity in run.entities) {
                if (entity.z < 0f || entity.z > 35f || entity.lane !in 0..2) continue
                when (entity.type) {
                    EntityType.MINE -> { unsafe[entity.lane] = true; scores[entity.lane] -= 100 }
                    EntityType.WRECK, EntityType.ENEMY -> if (!canDive) {
                        unsafe[entity.lane] = true
                        scores[entity.lane] -= 100
                    }
                    EntityType.INTEL, EntityType.FUEL -> scores[entity.lane] += 1
                    else -> Unit
                }
            }
            val noSafeLane = unsafe.all { it }
            var best = lane
            for (candidate in 0..2) {
                if (scores[candidate] > scores[best]) best = candidate
            }
            lane = best
            val closeDiveHazard = run.entities.any {
                it.lane == lane && it.z >= 0f && it.z <= 6f &&
                    (it.type == EntityType.WRECK || it.type == EntityType.ENEMY)
            }
            val imminentRam = run.pursuers.any {
                PursuerAI.canRam(it) && abs(it.y - GameSimulation.PLAYER_Y) <= 8f
            }
            val flare = run.pursuers.any { it.state == PursuerState.INTERCEPT }
            return Decision(lane - 1f, (canDive && closeDiveHazard) || imminentRam, flare, noSafeLane)
        }
    }

    private data class Decision(
        val dragTargetX: Float,
        val diveHeld: Boolean,
        val flare: Boolean,
        val noSafeLane: Boolean
    )

    private class Metrics {
        var runs = 1
        var wins = 0
        var deaths = 0
        val deathReasons = TreeMap<String, Int>()
        var completedRuns = 0
        var completedSeconds = 0.0
        var hullDamage = 0.0
        var forcedHits = 0
        var bandViolations = 0
        var waveEvents = 0
        var blockedWaveGaps = 0
        var freeLaneFallbacks = 0
        var submergedRams = 0
        var pings = 0
        var huntedSeconds = 0.0
        var activeHuntedSeconds = 0.0

        fun add(other: Metrics) {
            runs += other.runs
            wins += other.wins
            deaths += other.deaths
            for ((reason, count) in other.deathReasons) {
                deathReasons[reason] = (deathReasons[reason] ?: 0) + count
            }
            completedRuns += other.completedRuns
            completedSeconds += other.completedSeconds
            hullDamage += other.hullDamage
            forcedHits += other.forcedHits
            bandViolations += other.bandViolations
            waveEvents += other.waveEvents
            blockedWaveGaps += other.blockedWaveGaps
            freeLaneFallbacks += other.freeLaneFallbacks
            submergedRams += other.submergedRams
            pings += other.pings
            huntedSeconds += other.huntedSeconds
            activeHuntedSeconds += other.activeHuntedSeconds
        }

        fun row(policy: Policy, chapter: Int, minutes: Int): String {
            val reasons = if (deathReasons.isEmpty()) "-" else deathReasons.entries.joinToString(";") {
                "${it.key.replace('|', '/')}=${it.value}"
            }
            val mean = if (completedRuns == 0) 0.0 else completedSeconds / completedRuns
            val damagePerMinute = hullDamage / minutes
            val pingsPerHuntedMinute = if (huntedSeconds == 0.0) 0.0 else pings * 60.0 / huntedSeconds
            val activeFraction = if (huntedSeconds == 0.0) 0.0 else activeHuntedSeconds / huntedSeconds
            return listOf(
                policy.name, chapter.toString(), runs.toString(), wins.toString(), deaths.toString(),
                reasons, decimal(mean), decimal(damagePerMinute), forcedHits.toString(),
                bandViolations.toString(), waveEvents.toString(), blockedWaveGaps.toString(),
                freeLaneFallbacks.toString(), submergedRams.toString(),
                decimal(pingsPerHuntedMinute), decimal(activeFraction)
            ).joinToString("|")
        }
    }

    @Test
    fun aSeededSoakReportsBaseline() {
        val rows = ArrayList<String>(16)
        var firstScenarioRow = ""
        for (policy in Policy.entries) {
            for (chapter in 0..7) {
                val total = Metrics().apply { runs = 0 }
                for (seed in SEEDS) {
                    val one = scenario(seed, chapter, policy)
                    if (seed == 1 && chapter == 0 && policy == Policy.CAUTIOUS) {
                        firstScenarioRow = one.row(policy, chapter, SIM_MINUTES)
                    }
                    total.add(one)
                }
                rows += total.row(policy, chapter, SIM_MINUTES * SEEDS.size)
            }
        }
        assertEquals(
            "seed 1 / chapter 0 / CAUTIOUS must produce identical metrics",
            firstScenarioRow,
            scenario(1, 0, Policy.CAUTIOUS).row(Policy.CAUTIOUS, 0, SIM_MINUTES)
        )
        println("SOAK label=${SoakAdapter.LABEL} sim-minutes-per-chapter=$SIM_MINUTES seeds=1,2,3,4,5 columns=policy|chapter|runs|wins|deaths|death-reasons|mean-run-seconds|hull-damage-per-minute|forced-hits|band-violation-steps|wave-events|blocked-wave-gaps|freeLane-fallbacks|submerged-rams|pings-per-hunted-minute|ping-active-fraction")
        rows.forEach(::println)
    }

    @Test
    fun bDiveChainingProbe() {
        val progress = Progress()
        val rng = Random(101)
        var run = GameSimulation.setThrottle(GameSimulation.newRun(1, progress), 0.1f)
        var burst = 0
        var steady = 0
        repeat(70 * FRAMES_PER_SECOND) { frame ->
            val held = !run.dive.lockedOut && (run.dive.submerged || run.dive.oxygen >= 15f)
            val result = GameSimulation.step(run.copy(entities = emptyList()), 1, progress, RunInput(diveHeld = held), FRAME, rng)
            val after = SoakAdapter.afterStep(result.run)
            checkState(after) { "dive frame $frame" }
            run = after.copy(entities = emptyList())
            if (frame < 5 * FRAMES_PER_SECOND && run.dive.submerged) burst++
            if (frame >= 10 * FRAMES_PER_SECOND && run.dive.submerged) steady++
        }
        val burstFraction = burst / (5.0 * FRAMES_PER_SECOND)
        val steadyFraction = steady / (60.0 * FRAMES_PER_SECOND)
        println("DIVE burst-0-5=${decimal(burstFraction)} steady-10-70=${decimal(steadyFraction)}")
        assertTrue("steady submerged fraction $steadyFraction exceeds 0.395", steadyFraction <= 0.375 + 0.02)
    }

    @Test
    fun cSonarCadenceProbe() {
        for (radar in listOf(0, 3)) {
            val progress = Progress(modules = Modules(radar = radar))
            val rng = Random(202 + radar)
            val pursuer = Pursuer(
                id = 99, type = EnemyBoatType.PATROL, x = 0f,
                y = GameSimulation.PLAYER_Y - 15f, state = PursuerState.PURSUIT
            )
            var run = GameSimulation.setThrottle(GameSimulation.newRun(1, progress), 0.1f)
            var pings = 0
            var active = 0
            repeat(60 * FRAMES_PER_SECOND) { frame ->
                val pinned = run.copy(entities = emptyList(), pursuers = listOf(pursuer))
                val result = GameSimulation.step(pinned, 1, progress, RunInput(), FRAME, rng)
                val after = SoakAdapter.afterStep(result.run)
                checkState(after) { "sonar radar $radar frame $frame" }
                run = after.copy(entities = emptyList())
                if (!pinned.sonar.active && run.sonar.active) pings++
                if (run.sonar.active) active++
            }
            println("SONAR radar=$radar pings-per-hunted-minute=${decimal(pings.toDouble())} ping-active-fraction=${decimal(active / (60.0 * FRAMES_PER_SECOND))}")
        }
    }

    private fun scenario(seed: Int, chapter: Int, policy: Policy): Metrics {
        val progress = Progress()
        val rng = Random(seed)
        var bot = Bot()
        val metrics = Metrics()
        var run = GameSimulation.newRun(chapter, progress)
        var noSafeFrames = 0
        val frames = SIM_MINUTES * 60 * FRAMES_PER_SECOND
        repeat(frames) { frame ->
            val decision = bot.choose(run)
            noSafeFrames = if (decision.noSafeLane) noSafeFrames + 1 else 0
            var before = GameSimulation.setThrottle(
                run,
                if (policy == Policy.RECKLESS) 1f else if (run.detection > 0.6f) 0.1f else 0.5f
            )
            if (decision.flare) before = GameSimulation.launchFlare(before).first
            val result = GameSimulation.step(
                before, chapter, progress,
                RunInput(dragTargetX = decision.dragTargetX, diveHeld = decision.diveHeld), FRAME, rng
            )
            val after = SoakAdapter.afterStep(result.run)
            checkState(after) { "seed $seed chapter $chapter $policy frame $frame" }
            val loss = (before.hull - after.hull).coerceAtLeast(0f)
            metrics.hullDamage += loss
            if (loss > 1f && noSafeFrames >= 30) metrics.forcedHits++
            if (hasBandViolation(after.entities)) metrics.bandViolations++
            countSpawns(before.entities, after.entities, metrics)
            if (loss > 1f && after.dive.submerged && probableRamTransition(before, after, decision.dragTargetX)) {
                metrics.submergedRams++
            }
            val hunted = before.pursuers.any { it.state == PursuerState.PURSUIT || it.state == PursuerState.INTERCEPT }
            if (hunted) {
                metrics.huntedSeconds += FRAME
                if (after.sonar.active) metrics.activeHuntedSeconds += FRAME
                if (!before.sonar.active && after.sonar.active) metrics.pings++
            }
            val outcome = result.outcome
            if (outcome != null) {
                metrics.completedRuns++
                metrics.completedSeconds += after.runElapsed
                if (outcome.won) metrics.wins++
                if (after.hull <= 0f) {
                    metrics.deaths++
                    metrics.deathReasons[outcome.reason] = (metrics.deathReasons[outcome.reason] ?: 0) + 1
                }
                if (frame + 1 < frames) {
                    run = GameSimulation.newRun(chapter, progress)
                    bot = Bot()
                    metrics.runs++
                    noSafeFrames = 0
                }
            } else {
                run = after
            }
        }
        return metrics
    }

    private inline fun checkState(run: RunState, context: () -> String) {
        if (!run.hull.isFinite() || !run.maxHull.isFinite() || !run.detection.isFinite() ||
            !run.fuel.isFinite() || !run.dive.oxygen.isFinite() || !run.playerX.isFinite() ||
            run.entities.any { !it.z.isFinite() }
        ) {
            throw AssertionError("non-finite state at ${context()}")
        }
        if (run.maxHull < 0f || run.hull < 0f || run.hull > run.maxHull ||
            run.detection !in 0f..1f || run.fuel !in 0f..100f || run.dive.oxygen !in 0f..100f
        ) {
            throw AssertionError("state outside allowed range at ${context()}")
        }
    }

    private fun hasBandViolation(entities: List<Entity>): Boolean {
        for (anchor in entities) {
            if (!isHazard(anchor.type) || anchor.z !in 0f..120f) continue
            var lanes = 0
            for (entity in entities) {
                if (isHazard(entity.type) && entity.lane in 0..2 &&
                    entity.z >= anchor.z && entity.z <= 120f && entity.z - anchor.z <= 28f
                ) lanes = lanes or (1 shl entity.lane)
                if (lanes == 7) return true
            }
        }
        return false
    }

    private fun countSpawns(before: List<Entity>, after: List<Entity>, metrics: Metrics) {
        var waveLanes = 0
        var waveZ = 0f
        var waveCount = 0
        for (entity in after) {
            if (!isNew(entity, before)) continue
            if (entity.type == EntityType.ENEMY && entity.z in 65f..75f && entity.lane in 0..2) {
                waveLanes = waveLanes or (1 shl entity.lane)
                waveZ += entity.z
                waveCount++
            }
            if (entity.z in 95f..114f && entity.type != EntityType.FORK) {
                var occupied = 0
                for (other in after) {
                    if (other !== entity && !isNew(other, before) && other.lane in 0..2 &&
                        abs(other.z - entity.z) < 10f
                    ) occupied = occupied or (1 shl other.lane)
                }
                if (occupied == 7) metrics.freeLaneFallbacks++
            }
        }
        if (waveCount == 2 && Integer.bitCount(waveLanes) == 2) {
            metrics.waveEvents++
            val gap = (0..2).first { waveLanes and (1 shl it) == 0 }
            val center = waveZ / 2f
            if (after.any { isHazard(it.type) && it.lane == gap && abs(it.z - center) <= 14f }) {
                metrics.blockedWaveGaps++
            }
        }
    }

    private fun isNew(entity: Entity, before: List<Entity>): Boolean = before.none {
        it.type == entity.type && it.lane == entity.lane && it.x == entity.x &&
            it.z >= entity.z && it.z - entity.z < 2f
    }

    private fun probableRamTransition(before: RunState, after: RunState, commandedX: Float): Boolean =
        before.pursuers.any { old ->
            PursuerAI.canRam(old) && abs(old.y - GameSimulation.PLAYER_Y) < 6f &&
                abs(old.x - commandedX) < GameSimulation.HAZARD_HIT &&
                after.pursuers.any { next ->
                    next.state == PursuerState.PURSUIT && abs(next.x - old.x) < 2f &&
                        abs(next.y - old.y) < 2f
                }
        }

    private fun isHazard(type: EntityType): Boolean =
        type == EntityType.MINE || type == EntityType.WRECK || type == EntityType.ENEMY

    private companion object {
        const val FRAME = 1f / 60f
        const val FRAMES_PER_SECOND = 60
        const val SIM_MINUTES = 10
        val SEEDS = listOf(1, 2, 3, 4, 5)

        fun decimal(value: Double): String = String.format(Locale.ROOT, "%.3f", value)
    }
}
