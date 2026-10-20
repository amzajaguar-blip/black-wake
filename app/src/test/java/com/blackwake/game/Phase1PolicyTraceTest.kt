package com.blackwake.game

import java.util.Locale
import kotlin.math.abs
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Policy trace harness (U7a). Runs the shared soak [Bot] (lanes, dives, flare on INTERCEPT) under four
 * lever/tool policies with the soak's seeds, frame step, [Progress] and scenario loop, and prints one
 * `SIMULATION TRACE` row per (seed, chapter, policy).
 */
class Phase1PolicyTraceTest {
    /**
     * Lever and tool rules, quoted from GATE-P (Posky approved as proposed),
     * GATE-P2 (Posky) and GATE-P3 (Posky).
     *
     * GATE-P: "All new policies share the existing Bot (lanes, dives, flare on INTERCEPT); only the lever
     * and tool use differ."
     */
    private enum class Policy {
        /**
         * GATE-P: "lever always 0.1; never ignites a burst; sonar only when
         * `run.hunted && sonar.cooldown <= 0 && detection < 0.5`; no repair."
         */
        LOW_THROTTLE,

        /**
         * GATE-P: "lever 1.0; when `!burst.active && heatLock == 0 && fuel >= 12 && !submerged &&
         * !burst.armed`, set the lever to 0.8 for exactly one frame, then back to 1.0; never pings; no repair."
         */
        AGGRESSIVE_SPINTA,

        /**
         * GATE-P: "base lever = existing BALANCED hysteresis (0.75, or 0.1 while quiet); burst when
         * `hunted && detection < 0.6 && fuel >= 30`, using the same one-frame 0.8 re-cross; ping when
         * `hunted && cooldown ready && detection < 0.5`; repair (the only policy that repairs): when
         * `hull <= maxHull − 10 && fuel >= 30 && no active pursuer && detection < 0.5`, set the lever to 0.2,
         * call `startRepair`, and hold 0.2 until the repair ends."
         *
         * GATE-P2 (Posky): "(a) once ignited, hold the lever at 1.0 while burst.active, for the full 2.5 s.
         * Then return to the base hysteresis lever. (b) The trigger also requires the AGGRESSIVE guards:
         * !burst.active && heatLock == 0 && fuel >= 12 && !submerged && !burst.armed, plus hunted &&
         * detection < 0.6 && fuel >= 30. Ignite with the same one-frame 0.8 → 1.0 re-cross. (c) The repair
         * rule's 'no active pursuer' means no pursuer in DETECTED, PURSUIT or INTERCEPT. SEARCH,
         * SEARCH_AGAIN and LOST_TARGET are allowed."
         *
         * GATE-P3 (Posky): "Remove `!burst.armed` from the BALANCED_ADAPTIVE trigger. The trigger becomes:
         * !burst.active && heatLock == 0 && fuel >= 12 && !submerged && hunted && detection < 0.6 &&
         * fuel >= 30. Ignition, the full-burst hold and everything else stay as they are." The one-frame
         * 0.8 → 1.0 re-cross: the frame after a 0.8 dip (the lever left in [RunState.throttle] is 0.8) the
         * lever goes to 1.0; otherwise the trigger sets 0.8. GATE-P3 also makes the shared [Bot] treat an
         * active repair as "cannot dive".
         */
        BALANCED_ADAPTIVE,

        /** GATE-P: "STEADY_CRUISE (reference only): lever 0.8 always. It is excluded from the G3 dominance set." */
        STEADY_CRUISE
    }

    private class Action(val lever: Float, val sonar: Boolean, val repair: Boolean)

    /** Pure policy: (RunState, frame-local hysteresis memory) -> lever and tool actions. */
    private fun act(policy: Policy, run: RunState, quiet: Boolean): Action = when (policy) {
        Policy.LOW_THROTTLE -> Action(
            0.1f,
            sonar = run.hunted && run.sonar.cooldown <= 0f && run.detection < 0.5f,
            repair = false
        )
        Policy.AGGRESSIVE_SPINTA -> Action(if (burstGuards(run)) 0.8f else 1f, sonar = false, repair = false)
        Policy.BALANCED_ADAPTIVE -> {
            val sonar = run.hunted && run.sonar.cooldown <= 0f && run.detection < 0.5f
            val repairWanted = run.hull <= run.maxHull - 10f && run.fuel >= 30f &&
                run.pursuers.none {
                    it.state == PursuerState.DETECTED || it.state == PursuerState.PURSUIT ||
                        it.state == PursuerState.INTERCEPT
                } && run.detection < 0.5f
            when {
                run.burst.active -> Action(1f, sonar, repair = false)
                run.repair.active -> Action(0.2f, sonar, repair = false)
                repairWanted -> Action(0.2f, sonar, repair = true)
                !run.burst.active && run.burst.heatLock == 0f && run.fuel >= 12f && !run.dive.submerged &&
                    run.hunted && run.detection < 0.6f && run.fuel >= 30f ->
                    Action(if (run.throttle == 0.8f) 1f else 0.8f, sonar, repair = false)
                else -> Action(if (quiet) 0.1f else 0.75f, sonar, repair = false)
            }
        }
        Policy.STEADY_CRUISE -> Action(0.8f, sonar = false, repair = false)
    }

    private fun burstGuards(run: RunState): Boolean =
        !run.burst.active && run.burst.heatLock == 0f && run.fuel >= 12f && !run.dive.submerged && !run.burst.armed

    private class Trace {
        var runs = 1
        var wins = 0
        var deaths = 0
        var banked = 0
        var collected = 0
        var damage = 0.0
        var peakDetection = 0f
        var pursuits = 0
        var sonarUses = 0
        var flareUses = 0
        var spintaSeconds = 0.0
        var repairUses = 0
        var fuelUsed = 0.0
        var speedSum = 0.0
        var ignitions = 0
        var flareLockBroken = 0
        var flareSoloLuce = 0
        var flareRamCommitted = 0
        var intercepts = 0
        var ramsLanded = 0
        var plannedDiveFailures = 0
        val ignitionToIntercept = ArrayList<Double>()
    }

    @Test
    fun aPolicyTracesReport() {
        val rows = ArrayList<String>(SEEDS.size * 8 * Policy.entries.size)
        for (seed in SEEDS) {
            for (chapter in 0..7) {
                for (policy in Policy.entries) {
                    val trace = scenario(seed, chapter, policy)
                    rows += row(seed, chapter, policy, trace)
                }
            }
        }
        assertEquals(SEEDS.size * 8 * Policy.entries.size, rows.size)
        rows.forEach(::println)
    }

    private fun scenario(seed: Int, chapter: Int, policy: Policy): Trace {
        val progress = Progress()
        val rng = Random(seed)
        var bot = Bot()
        val trace = Trace()
        var run = GameSimulation.newRun(chapter, progress)
        var quietThrottle = false
        var pendingIgnitions = ArrayList<Double>()
        val frames = SIM_MINUTES * 60 * FRAMES_PER_SECOND
        repeat(frames) { frame ->
            val now = frame.toDouble() / FRAMES_PER_SECOND
            val decision = bot.choose(run)
            if (run.detection > 0.85f) quietThrottle = true
            if (run.detection < 0.5f) quietThrottle = false
            val action = act(policy, run, quietThrottle)
            var before = GameSimulation.setThrottle(run, action.lever)
            if (decision.flare) {
                val launched = GameSimulation.launchFlare(before).first
                if (launched.flare.charges < before.flare.charges) {
                    trace.flareUses++
                    val text = launched.feed.firstOrNull()?.text.orEmpty()
                    when {
                        "LOCK ROTTO" in text -> trace.flareLockBroken++
                        "SOLO LUCE" in text -> trace.flareSoloLuce++
                        "RAM GIÀ IMPEGNATO" in text -> trace.flareRamCommitted++
                        else -> throw AssertionError("unclassified flare message '$text'")
                    }
                }
                before = launched
            }
            if (action.sonar) {
                val pinged = GameSimulation.launchSonar(before).first
                if (!before.sonar.active && pinged.sonar.active) trace.sonarUses++
                before = pinged
            }
            if (action.repair) {
                val repaired = GameSimulation.startRepair(before).first
                if (!before.repair.active && repaired.repair.active) trace.repairUses++
                before = repaired
            }
            trace.fuelUsed += (run.fuel - before.fuel).coerceAtLeast(0f)
            val result = GameSimulation.step(
                before, chapter, progress,
                RunInput(dragTargetX = decision.dragTargetX, diveHeld = decision.diveHeld), FRAME, rng
            )
            val after = SoakAdapter.afterStep(result.run)
            checkState(after) { "seed $seed chapter $chapter $policy frame $frame" }
            trace.fuelUsed += (before.fuel - after.fuel).coerceAtLeast(0f)
            val loss = (before.hull - after.hull).coerceAtLeast(0f)
            trace.damage += loss
            trace.peakDetection = maxOf(trace.peakDetection, after.detection, before.detection)
            trace.speedSum += after.speedFactor
            if (after.burst.active) trace.spintaSeconds += FRAME
            if (!before.burst.active && after.burst.active) {
                trace.ignitions++
                pendingIgnitions += now
            }
            trace.pursuits += after.pursuers.count { it.id >= before.nextId }
            val newIntercepts = after.pursuers.count { next ->
                next.state == PursuerState.INTERCEPT &&
                    before.pursuers.none { it.id == next.id && it.state == PursuerState.INTERCEPT }
            }
            if (newIntercepts > 0) {
                trace.intercepts += newIntercepts
                val at = now + FRAME
                for (ignitedAt in pendingIgnitions) trace.ignitionToIntercept += at - ignitedAt
                pendingIgnitions = ArrayList()
            }
            val ramHit = loss > 1f && probableRamTransition(before, after)
            if (ramHit) trace.ramsLanded++
            val hitEntity = if (loss > 1f && !ramHit) before.entities.firstOrNull { entity ->
                isHazard(entity.type) && entity.z > -5f && entity.z <= 6f &&
                    abs(entity.x - after.playerX) < GameSimulation.HAZARD_HIT &&
                    after.entities.none { it.id == entity.id }
            } else null
            if (hitEntity != null && (hitEntity.type == EntityType.WRECK || hitEntity.type == EntityType.ENEMY) &&
                decision.diveHeld && hitEntity.id in decision.feasibleDiveIds
            ) trace.plannedDiveFailures++
            val outcome = result.outcome
            if (outcome != null) {
                if (outcome.won) trace.wins++
                if (after.hull <= 0f) trace.deaths++
                trace.banked += outcome.banked
                trace.collected += outcome.intel
                pendingIgnitions = ArrayList()
                if (frame + 1 < frames) {
                    run = GameSimulation.newRun(chapter, progress)
                    bot = Bot()
                    trace.runs++
                    quietThrottle = false
                }
            } else {
                run = after
            }
        }
        assertEquals("planned dive failure in seed $seed chapter $chapter $policy", 0, trace.plannedDiveFailures)
        return trace
    }

    private fun row(seed: Int, chapter: Int, policy: Policy, t: Trace): String {
        val seconds = SIM_MINUTES * 60.0
        val meanIgnition = if (t.ignitionToIntercept.isEmpty()) "-" else decimal(t.ignitionToIntercept.average())
        return "SIMULATION TRACE seed=$seed policy=${policy.name} chapter=$chapter duration=${SIM_MINUTES * 60} " +
            "intel=${t.banked} damage=${decimal(t.damage)} peak_detection=${decimal(t.peakDetection.toDouble())} " +
            "pursuits=${t.pursuits} sonar_uses=${t.sonarUses} flare_uses=${t.flareUses} " +
            "spinta_time=${decimal(t.spintaSeconds)} repair_uses=${t.repairUses} " +
            "runs=${t.runs} wins=${t.wins} deaths=${t.deaths} intel_collected=${t.collected} " +
            "fuel_used_per_s=${decimal(t.fuelUsed / seconds)} mean_speed=${decimal(t.speedSum / (seconds * FRAMES_PER_SECOND))} " +
            "ignitions=${t.ignitions} flare_lock_broken=${t.flareLockBroken} flare_solo_luce=${t.flareSoloLuce} " +
            "flare_ram_committed=${t.flareRamCommitted} intercepts=${t.intercepts} rams_landed=${t.ramsLanded} " +
            "mean_s_ignition_to_intercept=$meanIgnition"
    }

    private companion object {
        const val FRAME = 1f / 60f
        const val FRAMES_PER_SECOND = 60
        const val SIM_MINUTES = 10
        val SEEDS = listOf(1, 2, 3, 4, 5)

        fun decimal(value: Double): String = String.format(Locale.ROOT, "%.3f", value)
    }
}
