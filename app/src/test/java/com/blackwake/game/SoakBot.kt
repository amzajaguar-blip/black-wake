package com.blackwake.game

import kotlin.math.abs
import kotlin.math.round

/** Same frame step as [SoakTest]; file-private so it does not leak into the package. */
private const val FRAME = 1f / 60f

internal class Bot {
    private var previousZ = emptyMap<Int, Float>()

    fun choose(run: RunState): Decision {
        val currentLane = round(run.playerX).toInt().coerceIn(-1, 1) + 1
        val diveAvailable = !run.dive.lockedOut &&
            (run.dive.oxygen >= 15f || (run.dive.submerged && run.dive.oxygen > 0f)) && run.burst.diveLock <= 0f && !run.burst.active
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
        val canDive = diveAvailable && (closeDiveHazard || imminentRam) && run.burst.diveLock <= 0f
        return Decision(best - 1f, canDive, flare, nearBlocked.all { it }, feasible)
    }
}

internal data class Decision(
    val dragTargetX: Float,
    val diveHeld: Boolean,
    val flare: Boolean,
    val allLanesBlocked: Boolean,
    val feasibleDiveIds: Set<Int>
)

internal inline fun checkState(run: RunState, context: () -> String) {
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

internal fun isHazard(type: EntityType): Boolean =
    type == EntityType.MINE || type == EntityType.WRECK || type == EntityType.ENEMY

internal fun probableRamTransition(before: RunState, after: RunState): Boolean =
    before.pursuers.any { old ->
        PursuerAI.canRam(old) && abs(old.y - GameSimulation.PLAYER_Y) < 8f &&
            abs(old.x - after.playerX) < GameSimulation.RAM_HIT + 0.2f &&
            after.pursuers.any { next ->
                next.id == old.id && next.state == PursuerState.PURSUIT &&
                    abs(next.y - old.y) < 2f
            }
    }
