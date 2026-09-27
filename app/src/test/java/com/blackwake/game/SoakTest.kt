package com.blackwake.game

import java.util.ArrayDeque
import java.util.Locale
import java.util.TreeMap
import kotlin.math.abs
import kotlin.math.round
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SoakTest {
    private enum class Policy { CAUTIOUS, BALANCED, RECKLESS }

    private class Bot {
        private var previousZ = emptyMap<Int, Float>()

        fun choose(run: RunState): Decision {
            val currentLane = round(run.playerX).toInt().coerceIn(-1, 1) + 1
            val diveAvailable = !run.dive.lockedOut &&
                (run.dive.oxygen >= 15f || (run.dive.submerged && run.dive.oxygen > 0f))
            var speed = 25f
            for (entity in run.entities) {
                val oldZ = previousZ[entity.id] ?: continue
                val measured = (oldZ - entity.z) / FRAME
                if (measured > 0f && measured.isFinite()) {
                    speed = measured
                    break
                }
            }
            previousZ = run.entities.associate { it.id to it.z }
            val scores = IntArray(3)
            val blocked = BooleanArray(3)
            val nearBlocked = BooleanArray(3)
            val feasible = HashSet<Int>()
            for (entity in run.entities) {
                if (entity.z <= -5f || entity.z > 35f || entity.lane !in 0..2) continue
                val arrival = (entity.z + 5f) / speed
                val canDiveByArrival = diveAvailable ||
                    (!run.dive.submerged && (!run.dive.lockedOut || arrival > FRAME) &&
                        run.dive.oxygen + 15f * arrival >= 15f)
                val diveFeasible = canDiveByArrival && run.dive.oxygen - 25f * arrival >= 2f
                when (entity.type) {
                    EntityType.MINE -> {
                        blocked[entity.lane] = true
                        if (entity.z <= 20f) nearBlocked[entity.lane] = true
                    }
                    EntityType.WRECK, EntityType.ENEMY -> if (diveFeasible) {
                        feasible += entity.id
                        scores[entity.lane] -= 50
                    } else {
                        blocked[entity.lane] = true
                        if (entity.z <= 20f) nearBlocked[entity.lane] = true
                    }
                    EntityType.INTEL -> scores[entity.lane] += 10
                    EntityType.FUEL -> scores[entity.lane] += if (run.fuel < 40f) 40 else 10
                    else -> Unit
                }
            }
            var best = currentLane
            for (candidate in 0..2) {
                if (!blocked[candidate] && (blocked[best] || scores[candidate] > scores[best])) best = candidate
            }
            // atBow tests post-move z in (-4, 2). At FRAME and speed <= 60,
            // pre-step z > -5 covers its trailing edge; z <= 6 covers its leading edge.
            val closeDiveHazard = run.entities.any {
                abs(it.x - run.playerX) < GameSimulation.HAZARD_HIT && it.z > -5f && it.z <= 6f &&
                    (it.type == EntityType.WRECK || it.type == EntityType.ENEMY)
            }
            val imminentRam = run.pursuers.any {
                PursuerAI.canRam(it) && abs(it.y - GameSimulation.PLAYER_Y) < 8f &&
                    abs(it.x - run.playerX) < GameSimulation.RAM_HIT + 0.2f
            }
            val flare = run.pursuers.any { it.state == PursuerState.INTERCEPT }
            return Decision(best - 1f, diveAvailable && (closeDiveHazard || imminentRam),
                flare, nearBlocked.all { it }, feasible)
        }
    }

    private data class Decision(
        val dragTargetX: Float,
        val diveHeld: Boolean,
        val flare: Boolean,
        val allLanesBlocked: Boolean,
        val feasibleDiveIds: Set<Int>
    )

    private class Metrics {
        var runs = 1
        var wins = 0
        var deaths = 0
        val deathReasons = TreeMap<String, Int>()
        var completedRuns = 0
        var completedSeconds = 0.0
        var hullDamage = 0.0
        var allLanesBlockedFrames = 0
        var forcedHits = 0
        var botErrorHits = 0
        var plannedDiveFailures = 0
        var bandViolations = 0
        var waveEvents = 0
        var blockedWaveGaps = 0
        var freeLaneFallbacks = 0
        var submergedRams = 0
        var pings = 0
        var huntedSeconds = 0.0
        var activeHuntedSeconds = 0.0
        var elapsedSeconds = 0.0
        var pursuersSpawned = 0

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
            allLanesBlockedFrames += other.allLanesBlockedFrames
            forcedHits += other.forcedHits
            botErrorHits += other.botErrorHits
            plannedDiveFailures += other.plannedDiveFailures
            bandViolations += other.bandViolations
            waveEvents += other.waveEvents
            blockedWaveGaps += other.blockedWaveGaps
            freeLaneFallbacks += other.freeLaneFallbacks
            submergedRams += other.submergedRams
            pings += other.pings
            huntedSeconds += other.huntedSeconds
            activeHuntedSeconds += other.activeHuntedSeconds
            elapsedSeconds += other.elapsedSeconds
            pursuersSpawned += other.pursuersSpawned
        }

        fun row(policy: Policy, chapter: Int, minutes: Int): String {
            val reasons = if (deathReasons.isEmpty()) "-" else deathReasons.entries.joinToString(";") {
                "${it.key.replace('|', '/')}=${it.value}"
            }
            val mean = if (completedRuns == 0) 0.0 else completedSeconds / completedRuns
            val damagePerMinute = hullDamage / minutes
            val pingsPerHuntedMinute = if (huntedSeconds == 0.0) 0.0 else pings * 60.0 / huntedSeconds
            val activeFraction = if (huntedSeconds == 0.0) 0.0 else activeHuntedSeconds / huntedSeconds
            val huntedFraction = if (elapsedSeconds == 0.0) 0.0 else huntedSeconds / elapsedSeconds
            return listOf(
                policy.name, chapter.toString(), runs.toString(), wins.toString(), deaths.toString(),
                reasons, decimal(mean), decimal(damagePerMinute), allLanesBlockedFrames.toString(),
                forcedHits.toString(), botErrorHits.toString(), plannedDiveFailures.toString(),
                bandViolations.toString(), waveEvents.toString(), blockedWaveGaps.toString(),
                freeLaneFallbacks.toString(), submergedRams.toString(),
                decimal(pingsPerHuntedMinute), decimal(activeFraction), decimal(huntedFraction),
                pursuersSpawned.toString()
            ).joinToString("|")
        }
    }

    @Test
    fun aSeededSoakReportsBaseline() {
        val rows = ArrayList<String>(24)
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
        println("SOAK label=${SoakAdapter.LABEL} bot=v2 sim-minutes-per-chapter=$SIM_MINUTES seeds=1,2,3,4,5 columns=policy|chapter|runs|wins|deaths|death-reasons|mean-run-seconds|hull-damage-per-minute|all-lanes-blocked-frames|forced-hits|bot-error-hits|planned-dive-failures|band-violation-steps|wave-events|blocked-wave-gaps|freeLane-fallbacks|submerged-rams|pings-per-hunted-minute|ping-active-fraction|hunted-fraction|pursuers-spawned")
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

    @Test
    fun botDivesThroughTheWholeWindow() {
        // The closest existing sector threats to 0.3, 0.6 and 1.0 are
        // 0.30, 0.59 and 0.99. Keep the boat in the wreck's lane to isolate
        // the dive command from the lane-selection policy.
        val cases = listOf(Triple(0, 0, 0.30f), Triple(3, 0, 0.59f), Triple(6, 1, 0.99f))
        for ((chapter, sector, threat) in cases) {
            val progress = Progress()
            val rng = Random(303 + chapter)
            val wreck = Entity(1, EntityType.WRECK, 1, 0f, 20f)
            var run = GameSimulation.newRun(chapter, progress).copy(
                sectorIndex = sector, tutorialStep = 5, spawnClock = 1000f,
                entities = listOf(wreck), nextId = 2
            )
            val bot = Bot()
            var windowFrames = 0
            var submergedFrames = 0
            repeat(120) { frame ->
                val decision = bot.choose(run)
                val inWindow = run.entities.any {
                    it.id == wreck.id && it.z > -5f && it.z <= 6f
                }
                val result = GameSimulation.step(run, chapter, progress,
                    RunInput(dragTargetX = 0f, diveHeld = decision.diveHeld), FRAME, rng)
                val after = SoakAdapter.afterStep(result.run)
                checkState(after) { "dive window threat $threat frame $frame" }
                if (inWindow) {
                    windowFrames++
                    if (after.dive.submerged) submergedFrames++
                    assertTrue("dive released at threat $threat frame $frame", after.dive.submerged)
                }
                run = after
            }
            assertTrue("wreck never entered the window at threat $threat", windowFrames > 0)
            assertEquals("wreck damaged hull at threat $threat", run.maxHull, run.hull, 0f)
            println("DIVE-WINDOW threat=$threat submerged-frames=$submergedFrames window-frames=$windowFrames hits=0")
        }
    }

    private fun scenario(seed: Int, chapter: Int, policy: Policy): Metrics {
        val progress = Progress()
        val rng = Random(seed)
        var bot = Bot()
        val metrics = Metrics()
        var run = GameSimulation.newRun(chapter, progress)
        val blockedHistory = ArrayDeque<Boolean>()
        var blockedInWindow = 0
        var quietThrottle = false
        val frames = SIM_MINUTES * 60 * FRAMES_PER_SECOND
        repeat(frames) { frame ->
            val decision = bot.choose(run)
            blockedHistory.addLast(decision.allLanesBlocked)
            if (decision.allLanesBlocked) {
                blockedInWindow++
                metrics.allLanesBlockedFrames++
            }
            if (blockedHistory.size > FRAMES_PER_SECOND / 2 && blockedHistory.removeFirst()) {
                blockedInWindow--
            }
            val lowThreshold = if (policy == Policy.CAUTIOUS) 0.3f else 0.5f
            val highThreshold = if (policy == Policy.CAUTIOUS) 0.6f else 0.85f
            if (run.detection > highThreshold) quietThrottle = true
            if (run.detection < lowThreshold) quietThrottle = false
            val throttle = when (policy) {
                Policy.RECKLESS -> 1f
                Policy.CAUTIOUS -> if (quietThrottle) 0.1f else 0.5f
                Policy.BALANCED -> if (quietThrottle) 0.1f else 0.75f
            }
            var before = GameSimulation.setThrottle(
                run, throttle
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
            val ramHit = loss > 1f && probableRamTransition(before, after)
            val hitEntity = if (loss > 1f && !ramHit) before.entities.firstOrNull { entity ->
                isHazard(entity.type) && entity.z > -5f && entity.z <= 6f &&
                    abs(entity.x - after.playerX) < GameSimulation.HAZARD_HIT &&
                    after.entities.none { it.id == entity.id }
            } else null
            if (hitEntity != null) {
                if (blockedInWindow > 0) metrics.forcedHits++ else metrics.botErrorHits++
                if ((hitEntity.type == EntityType.WRECK || hitEntity.type == EntityType.ENEMY) &&
                    decision.diveHeld && hitEntity.id in decision.feasibleDiveIds
                ) metrics.plannedDiveFailures++
            }
            if (hasBandViolation(after.entities)) metrics.bandViolations++
            countSpawns(before, after, metrics)
            if (ramHit && after.dive.submerged) {
                metrics.submergedRams++
            }
            metrics.elapsedSeconds += FRAME
            if (before.hunted) {
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
                    blockedHistory.clear()
                    blockedInWindow = 0
                    quietThrottle = false
                }
            } else {
                run = after
            }
        }
        assertEquals("planned dive failure in seed $seed chapter $chapter $policy", 0, metrics.plannedDiveFailures)
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
            run.detection !in 0f..1f || run.fuel !in 0f..100f || run.dive.oxygen !in 0f..100f ||
            run.playerX !in -1.15f..1.15f || run.entities.any { it.z !in -12f..120f }
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

    private fun countSpawns(before: RunState, after: RunState, metrics: Metrics) {
        var waveLanes = 0
        var waveZ = 0f
        var waveCount = 0
        metrics.pursuersSpawned += after.pursuers.count { it.id >= before.nextId }
        for (entity in after.entities) {
            if (entity.id < before.nextId) continue
            if (entity.type == EntityType.ENEMY && entity.z in 65f..75f && entity.lane in 0..2) {
                waveLanes = waveLanes or (1 shl entity.lane)
                waveZ += entity.z
                waveCount++
            }
            if (entity.z in 95f..114f && entity.type != EntityType.FORK) {
                var occupied = 0
                for (other in after.entities) {
                    if (other.id < entity.id && other.lane in 0..2 &&
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
            if (after.entities.any { isHazard(it.type) && it.lane == gap && abs(it.z - center) <= 14f }) {
                metrics.blockedWaveGaps++
            }
        }
    }

    private fun probableRamTransition(before: RunState, after: RunState): Boolean =
        before.pursuers.any { old ->
            PursuerAI.canRam(old) && abs(old.y - GameSimulation.PLAYER_Y) < 8f &&
                abs(old.x - after.playerX) < GameSimulation.RAM_HIT + 0.2f &&
                after.pursuers.any { next ->
                    next.id == old.id && next.state == PursuerState.PURSUIT &&
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
