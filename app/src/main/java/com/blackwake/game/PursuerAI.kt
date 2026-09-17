package com.blackwake.game

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

/**
 * Black Tide pursuer behaviour. Pure and deterministic: all inputs arrive through [Context].
 *
 * Loop: DETECTED -> PURSUIT (shadow ahead) -> INTERCEPT (wind-up, then ram down the locked lane) -> PURSUIT.
 * Running silent or submerged with low detection drops the pursuer into SEARCH; staying hidden long
 * enough makes it give up (LOST_TARGET). A flare forces SEARCH immediately.
 */
object PursuerAI {
    const val CRUISE_Y = 70f
    const val PASS_Y = 97f
    const val SPAWN_Y = 118f
    const val GONE_Y = 130f
    const val WIND_UP = 0.6f
    const val SHAKE_OFF_DETECTION = 0.45f
    const val REACQUIRE_DETECTION = 0.6f
    const val SEARCH_SECONDS = 3f
    const val SEARCH_AGAIN_SECONDS = 2.5f

    data class Spec(
        val trackRate: Float,
        val prediction: Float,
        val attackDelay: Float,
        val ramDamage: Float,
        val chargeSpeed: Float
    )

    class Context(
        val playerX: Float,
        val playerVelocityX: Float,
        val detection: Float,
        /** Player is running silent or submerged. */
        val hidden: Boolean,
        val dt: Float
    )

    fun spec(type: EnemyBoatType): Spec = when (type) {
        EnemyBoatType.PATROL -> Spec(trackRate = 3f, prediction = 0.2f, attackDelay = 4.5f, ramDamage = 12f, chargeSpeed = 40f)
        EnemyBoatType.POLICE -> Spec(trackRate = 3f, prediction = 0.5f, attackDelay = 4.0f, ramDamage = 12f, chargeSpeed = 45f)
        EnemyBoatType.INTERCEPTOR -> Spec(trackRate = 4f, prediction = 0.8f, attackDelay = 3.0f, ramDamage = 15f, chargeSpeed = 55f)
        EnemyBoatType.HUNTER -> Spec(trackRate = 5f, prediction = 0.6f, attackDelay = 3.0f, ramDamage = 18f, chargeSpeed = 55f)
        EnemyBoatType.ARMORED -> Spec(trackRate = 1.5f, prediction = 0.3f, attackDelay = 5.0f, ramDamage = 22f, chargeSpeed = 35f)
        EnemyBoatType.ELITE -> Spec(trackRate = 6f, prediction = 1.2f, attackDelay = 2.2f, ramDamage = 20f, chargeSpeed = 65f)
    }

    fun pickType(chapterIndex: Int, detection: Float, rng: Random): EnemyBoatType {
        if (detection > 0.9f && chapterIndex >= 3) return EnemyBoatType.ELITE
        val pool = buildList {
            add(EnemyBoatType.PATROL)
            add(EnemyBoatType.POLICE)
            if (chapterIndex >= 1) add(EnemyBoatType.INTERCEPTOR)
            if (chapterIndex >= 3) add(EnemyBoatType.HUNTER)
            if (chapterIndex >= 5) add(EnemyBoatType.ARMORED)
        }
        return pool[rng.nextInt(pool.size)]
    }

    fun maxActive(chapterIndex: Int): Int = 1 + chapterIndex / 3

    /** True while the pursuer is charging through the player's row and can hit them. */
    fun canRam(p: Pursuer): Boolean = p.state == PursuerState.INTERCEPT && p.stateTimer >= WIND_UP

    fun isGone(p: Pursuer): Boolean = p.state == PursuerState.LOST_TARGET && p.y > GONE_Y

    /** Flare glare: every pursuer still on the trail loses it. */
    fun dazzle(p: Pursuer): Pursuer =
        if (p.state == PursuerState.LOST_TARGET || p.state == PursuerState.SEARCH || p.state == PursuerState.SEARCH_AGAIN) p
        else p.copy(state = PursuerState.SEARCH, stateTimer = 0f, attackTimer = 0f, lockX = p.x)

    fun step(p: Pursuer, ctx: Context, all: List<Pursuer>): Pursuer {
        val s = spec(p.type)
        val dt = ctx.dt
        val t = p.stateTimer + dt
        val predictedX = (ctx.playerX + ctx.playerVelocityX * s.prediction).coerceIn(-1f, 1f)
        val lostTrack = ctx.hidden && ctx.detection < SHAKE_OFF_DETECTION
        val reacquired = !ctx.hidden || ctx.detection > REACQUIRE_DETECTION

        return when (p.state) {
            PursuerState.DETECTED -> {
                val moved = p.trackX(predictedX, s.trackRate, dt).copy(y = approach(p.y, 100f, 25f * dt), stateTimer = t)
                if (t > 0.8f) moved.enter(PursuerState.PURSUIT) else moved
            }

            PursuerState.PURSUIT -> {
                val target = (predictedX + separation(p, all)).coerceIn(-1f, 1f)
                val attack = p.attackTimer + dt
                val moved = p.trackX(target, s.trackRate, dt).copy(y = approach(p.y, CRUISE_Y, 30f * dt), attackTimer = attack, stateTimer = t)
                when {
                    lostTrack -> moved.enter(PursuerState.SEARCH).copy(lockX = ctx.playerX, attackTimer = 0f)
                    attack >= s.attackDelay && abs(moved.y - CRUISE_Y) < 6f ->
                        moved.enter(PursuerState.INTERCEPT).copy(lockX = predictedX, attackTimer = 0f)
                    else -> moved
                }
            }

            PursuerState.INTERCEPT -> {
                if (t < WIND_UP) {
                    // Telegraph: slide onto the locked lane and hold, so the player can read the run.
                    p.trackX(p.lockX, s.trackRate * 1.5f, dt).copy(stateTimer = t)
                } else {
                    val moved = p.trackX(p.lockX, s.trackRate, dt).copy(y = approach(p.y, PASS_Y, s.chargeSpeed * dt), stateTimer = t)
                    when {
                        moved.y < PASS_Y -> moved
                        lostTrack -> moved.enter(PursuerState.SEARCH).copy(lockX = ctx.playerX)
                        else -> moved.enter(PursuerState.PURSUIT)
                    }
                }
            }

            PursuerState.SEARCH, PursuerState.SEARCH_AGAIN -> {
                val sweep = sin(t * 2f) * 0.5f
                val moved = p.trackX((p.lockX + sweep).coerceIn(-1f, 1f), 1.5f, dt).copy(y = approach(p.y, 110f, 8f * dt), stateTimer = t)
                when {
                    reacquired -> moved.enter(PursuerState.PURSUIT)
                    p.state == PursuerState.SEARCH && t > SEARCH_SECONDS -> moved.enter(PursuerState.SEARCH_AGAIN)
                    p.state == PursuerState.SEARCH_AGAIN && t > SEARCH_AGAIN_SECONDS -> moved.enter(PursuerState.LOST_TARGET)
                    else -> moved
                }
            }

            PursuerState.LOST_TARGET -> p.copy(y = p.y + 30f * dt, stateTimer = t)
        }
    }

    private fun Pursuer.enter(next: PursuerState): Pursuer = copy(state = next, stateTimer = 0f)

    private fun Pursuer.trackX(target: Float, rate: Float, dt: Float): Pursuer {
        val vx = velocityX + ((target - x) * rate - velocityX) * min(1f, dt * 5f)
        return copy(x = (x + vx * dt).coerceIn(-1.2f, 1.2f), velocityX = vx, roll = vx * 15f)
    }

    private fun approach(value: Float, target: Float, maxStep: Float): Float {
        val d = target - value
        return if (abs(d) <= maxStep) target else value + sign(d) * maxStep
    }

    private fun separation(p: Pursuer, all: List<Pursuer>): Float {
        var force = 0f
        for (o in all) {
            if (o.id == p.id || !o.isActive) continue
            val dx = p.x - o.x
            if (abs(dx) < 0.5f && abs(p.y - o.y) < 12f) {
                force += if (dx > 0f || (dx == 0f && p.id > o.id)) 0.35f else -0.35f
            }
        }
        return force
    }
}
