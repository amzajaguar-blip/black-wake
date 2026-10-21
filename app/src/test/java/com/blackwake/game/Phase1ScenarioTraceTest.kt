package com.blackwake.game

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Scripted scenario traces (U7c). Each scenario runs a fixed-seed tool twin and a control twin that differ only
 * in the tool action, prints one `SIMULATION TRACE` row per twin, and asserts the measurable difference that
 * the PHASE_1_DESIGN_SPEC.md acceptance gate names (SPINTA :80, RAZZO :108, RIPARA :136, SONAR :164).
 * Inputs are scripted, not the soak bot. Regular spawns are disabled (spawnClock 1000); hazards are placed
 * by the scenario.
 */
class Phase1ScenarioTraceTest {
    private val progress = Progress()

    /** One frame of scripted input. Tools are applied before the step, in the order flare, sonar, repair. */
    private class Act(
        val lever: Float,
        val flare: Boolean = false,
        val sonar: Boolean = false,
        val repair: Boolean = false,
        val drag: Float? = null,
        val dive: Boolean = false
    )

    private class Trace {
        var damage = 0.0
        var peakDetection = 0f
        var pursuits = 0
        var sonarUses = 0
        var flareUses = 0
        var spintaSeconds = 0.0
        var repairUses = 0
        var wins = 0
        var deaths = 0
        var banked = 0
        var collected = 0
        var fuelUsed = 0.0
        var speedSum = 0.0
        var frames = 0
        var ignitions = 0
        var flareLockBroken = 0
        var flareSoloLuce = 0
        var flareRamCommitted = 0
        var intercepts = 0
        var ramsLanded = 0
        val ignitionToIntercept = ArrayList<Double>()
    }

    /** Result of one twin: the trace, every post-step state (index = frame) and the final state. */
    private class Played(val trace: Trace, val states: List<RunState>, val end: RunState, val outcome: RunOutcome?)

    /** Chapter start with regular spawns disabled and no pursuit spawn unless [pursuitCooldown] allows it. */
    private fun scenarioStart(chapter: Int = 1): RunState =
        GameSimulation.newRun(chapter, progress).copy(spawnClock = 1_000f, pursuitCooldown = 100f)

    private fun patrol(y: Float, state: PursuerState, attackTimer: Float = 0f, stateTimer: Float = 0f): Pursuer =
        Pursuer(id = 900, type = EnemyBoatType.PATROL, x = 0f, y = y, state = state, attackTimer = attackTimer, stateTimer = stateTimer, lockX = 0f)

    private fun play(
        start: RunState,
        chapter: Int,
        seconds: Float,
        script: (frame: Int, run: RunState) -> Act,
        inject: (frame: Int, run: RunState) -> RunState = { _, run -> run }
    ): Played {
        val rng = Random(SEED)
        val trace = Trace()
        val states = ArrayList<RunState>()
        var run = start
        var outcome: RunOutcome? = null
        var pendingIgnitions = ArrayList<Double>()
        val frames = (seconds / FRAME).toInt()
        for (frame in 0 until frames) {
            val now = frame.toDouble() / FRAMES_PER_SECOND
            run = inject(frame, run)
            val act = script(frame, run)
            var before = GameSimulation.setThrottle(run, act.lever)
            if (act.flare) {
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
            if (act.sonar) {
                val pinged = GameSimulation.launchSonar(before).first
                if (!before.sonar.active && pinged.sonar.active) trace.sonarUses++
                before = pinged
            }
            if (act.repair) {
                val repaired = GameSimulation.startRepair(before).first
                if (!before.repair.active && repaired.repair.active) trace.repairUses++
                before = repaired
            }
            trace.fuelUsed += (run.fuel - before.fuel).coerceAtLeast(0f)
            val result = GameSimulation.step(before, chapter, progress, RunInput(dragTargetX = act.drag, diveHeld = act.dive), FRAME, rng)
            val after = result.run
            checkState(after) { "scenario frame $frame" }
            trace.frames++
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
            // New pursuer = an id absent before the step (scripted pursuers carry ids above nextId).
            trace.pursuits += after.pursuers.count { next -> before.pursuers.none { it.id == next.id } }
            val newIntercepts = after.pursuers.count { next ->
                next.state == PursuerState.INTERCEPT &&
                    before.pursuers.none { it.id == next.id && it.state == PursuerState.INTERCEPT }
            }
            if (newIntercepts > 0) {
                trace.intercepts += newIntercepts
                for (ignitedAt in pendingIgnitions) trace.ignitionToIntercept += now + FRAME - ignitedAt
                pendingIgnitions = ArrayList()
            }
            if (loss > 1f && probableRamTransition(before, after)) trace.ramsLanded++
            states += after
            run = after
            outcome = result.outcome
            if (outcome != null) {
                if (outcome.won) trace.wins++
                if (after.hull <= 0f) trace.deaths++
                trace.banked += outcome.banked
                trace.collected += outcome.intel
                break
            }
        }
        return Played(trace, states, run, outcome)
    }

    /** Same field set as the Phase1PolicyTraceTest row, plus hull_end, kits, fuel_end and pursuers_spawned. */
    private fun row(scenario: String, variant: String, chapter: Int, seconds: Float, p: Played): String {
        val t = p.trace
        val simulated = t.frames.toDouble() / FRAMES_PER_SECOND
        val meanIgnition = if (t.ignitionToIntercept.isEmpty()) "-" else decimal(t.ignitionToIntercept.average())
        return "SIMULATION TRACE scenario=$scenario variant=$variant seed=$SEED chapter=$chapter duration=${decimal(seconds.toDouble())} " +
            "intel=${t.banked} damage=${decimal(t.damage)} peak_detection=${decimal(t.peakDetection.toDouble())} " +
            "pursuits=${t.pursuits} sonar_uses=${t.sonarUses} flare_uses=${t.flareUses} " +
            "spinta_time=${decimal(t.spintaSeconds)} repair_uses=${t.repairUses} " +
            "runs=1 wins=${t.wins} deaths=${t.deaths} intel_collected=${t.collected} " +
            "fuel_used_per_s=${decimal(t.fuelUsed / simulated)} mean_speed=${decimal(t.speedSum / t.frames)} " +
            "ignitions=${t.ignitions} flare_lock_broken=${t.flareLockBroken} flare_solo_luce=${t.flareSoloLuce} " +
            "flare_ram_committed=${t.flareRamCommitted} intercepts=${t.intercepts} rams_landed=${t.ramsLanded} " +
            "mean_s_ignition_to_intercept=$meanIgnition " +
            "hull_end=${decimal(p.end.hull.toDouble())} kits=${p.end.repair.kits} fuel_end=${decimal(p.end.fuel.toDouble())} " +
            "pursuers_spawned=${t.pursuits}"
    }

    private fun printRows(scenario: String, chapter: Int, seconds: Float, tool: Played, control: Played) {
        println(row(scenario, "tool", chapter, seconds, tool))
        println(row(scenario, "control", chapter, seconds, control))
    }

    // --- SPINTA (spec :80)

    /** Burst on a PATROL in PURSUIT at y 70 in the player lane; [lead] seconds before attackDelay. */
    private fun burstTwins(id: String, lead: Float): Pair<Played, Played> {
        val attackDelay = PursuerAI.spec(EnemyBoatType.PATROL).attackDelay
        val start = scenarioStart().copy(
            fuel = 100f, detection = 0.3f, throttle = 0.8f, playerX = 0f,
            burst = BurstState(armed = true),
            pursuers = listOf(patrol(PursuerAI.CRUISE_Y, PursuerState.PURSUIT, attackTimer = attackDelay - lead))
        )
        val tool = play(start, 1, BURST_HOLD, { _, _ -> Act(lever = 1f) })
        val control = play(start, 1, BURST_HOLD, { _, _ -> Act(lever = 0.8f) })
        printRows(id, 1, BURST_HOLD, tool, control)
        return tool to control
    }

    @Test
    fun s1BurstGoodEscapesIntercept() {
        val (tool, control) = burstTwins("S1", 0.6f)
        val interceptDuringBurst = tool.states.any { s -> s.burst.active && s.pursuers.any { it.state == PursuerState.INTERCEPT } }
        assertTrue("tool must ignite once (ignitions ${tool.trace.ignitions})", tool.trace.ignitions == 1)
        assertFalse("tool must see no INTERCEPT entry during the burst (tool intercepts ${tool.trace.intercepts}, control ${control.trace.intercepts})", interceptDuringBurst)
        assertTrue(
            "tool hull must beat control hull (tool ${tool.end.hull}, control ${control.end.hull})",
            tool.end.hull > control.end.hull
        )
    }

    @Test
    fun s1bBurstTooLateStillRammed() {
        val (tool, control) = burstTwins("S1b", 0.2f)
        val startHull = scenarioStart().hull
        assertTrue("tool must ignite once (ignitions ${tool.trace.ignitions})", tool.trace.ignitions == 1)
        assertTrue(
            "a burst 0.2 s before attackDelay must still be rammed (tool hull ${tool.end.hull}, control hull ${control.end.hull}, start $startHull)",
            tool.end.hull < startHull
        )
    }

    @Test
    fun s2BurstBadNearAlarmWithWreck() {
        // Ignition (+0.08) lifts 0.65 over the 0.7 spawn threshold; burst-active blocks the dive, so the wreck hits.
        val seconds = 2.0f
        val start = scenarioStart().copy(
            fuel = 100f, detection = 0.65f, throttle = 0.8f, playerX = 0f, pursuitCooldown = 0f,
            burst = BurstState(armed = true),
            entities = listOf(Entity(id = 800, type = EntityType.WRECK, lane = 1, x = 0f, z = 40f)), nextId = 801
        )
        val tool = play(start, 1, seconds, { _, _ -> Act(lever = 1f, dive = true) })
        val control = play(start, 1, seconds, { _, _ -> Act(lever = 0.8f, dive = true) })
        printRows("S2", 1, seconds, tool, control)
        assertEquals(
            "pursuers spawned (tool ${tool.trace.pursuits}, control ${control.trace.pursuits})",
            1 to 0, tool.trace.pursuits to control.trace.pursuits
        )
        assertTrue(
            "tool hull must be below control hull (tool ${tool.end.hull}, control ${control.end.hull})",
            tool.end.hull < control.end.hull
        )
    }

    // --- RAZZO (spec :108)

    /**
     * Flare at t 0.1 s, then a drag to the adjacent lane centre (+1.0) from t 0.1 to 0.6 s at lever 0.1.
     * Lever 0.1 is silent, so detection decays fast: the control is allowed to drop to SEARCH by lostTrack
     * (hidden and detection < 0.45) after its ram lands (GATE-S3, Posky). The detection at the end of the 2 s
     * grace is printed for both twins and not asserted.
     */
    @Test
    fun s3FlareGoodLaneChangeBreaksChase() {
        val seconds = 4.0f
        val flareFrame = 6
        val dragEnd = 36
        val start = scenarioStart().copy(
            fuel = 100f, detection = 0.70f, throttle = 0.1f, playerX = 0f,
            flare = FlareState(charges = 3),
            pursuers = listOf(patrol(80f, PursuerState.PURSUIT, attackTimer = PursuerAI.spec(EnemyBoatType.PATROL).attackDelay - 1.0f))
        )
        fun script(flare: Boolean) = { frame: Int, _: RunState ->
            Act(lever = 0.1f, flare = flare && frame == flareFrame, drag = if (frame in flareFrame until dragEnd) 1f else null)
        }
        val tool = play(start, 1, seconds, script(true))
        val control = play(start, 1, seconds, script(false))
        printRows("S3", 1, seconds, tool, control)
        val graceEnd = flareFrame + (2f / FRAME).roundToInt() - 1
        println(
            "S3 GRACE-END detection tool=${decimal(tool.states[graceEnd].detection.toDouble())} " +
                "control=${decimal(control.states[graceEnd].detection.toDouble())} t=${decimal(tool.states[graceEnd].runElapsed.toDouble())}"
        )
        val searchFrame = tool.states.indexOfFirst { s -> s.pursuers.any { it.state == PursuerState.SEARCH } }
        assertEquals("flare must break the lock (lock_broken ${tool.trace.flareLockBroken})", 1, tool.trace.flareLockBroken)
        assertTrue(
            "tool must reach SEARCH within 1 frame of the flare (flare frame $flareFrame, SEARCH frame $searchFrame)",
            searchFrame in flareFrame..flareFrame + 1
        )
        assertTrue("tool player must end in the adjacent lane (x ${tool.end.playerX}, control x ${control.end.playerX})", abs(tool.end.playerX - 1f) < 0.1f)
        assertTrue(
            "tool hull must beat control hull (tool ${tool.end.hull}, control ${control.end.hull})",
            tool.end.hull > control.end.hull
        )
    }

    @Test
    fun s4FlareBadFiredInPlaceLoud() {
        val seconds = 1.0f
        val start = scenarioStart().copy(
            fuel = 100f, detection = 0.3f, throttle = 0.8f, playerX = 0f,
            pursuers = listOf(patrol(PursuerAI.CRUISE_Y, PursuerState.INTERCEPT, stateTimer = PursuerAI.WIND_UP))
        )
        val tool = play(start, 1, seconds, { frame, _ -> Act(lever = 0.8f, flare = frame == 0) })
        val control = play(start, 1, seconds, { _, _ -> Act(lever = 0.8f) })
        printRows("S4", 1, seconds, tool, control)
        assertEquals("committed ram reported (ram_committed ${tool.trace.flareRamCommitted})", 1, tool.trace.flareRamCommitted)
        assertTrue(
            "the ram must still land on the tool (tool hull ${tool.end.hull}, control hull ${control.end.hull})",
            tool.end.hull < start.hull
        )
        assertTrue(
            "tool detection must exceed control (tool ${tool.end.detection}, control ${control.end.detection})",
            tool.end.detection > control.end.detection
        )
        assertEquals(
            "flare cost at t ${tool.end.runElapsed} (tool ${tool.end.detection}, control ${control.end.detection})",
            0.18f, tool.end.detection - control.end.detection, 0.02f
        )
    }

    // --- RIPARA (spec :136)

    /**
     * Gauntlet injected at t 6 s in the player lane: MINE z 30, MINE z 50, WRECK z 70 = 25 + 25 + 20 = 70 damage,
     * spaced 20 units so each hit lands after the 0.86 s invulnerability. The brief's "≥ 45" cannot kill a
     * hull-60 boat; 70 is the accepted value (Posky): it kills the unrepaired 60 and spares the repaired 78.
     */
    @Test
    fun s5RepairGoodQuietCorridor() {
        val seconds = 12f
        val gauntletFrame = (6f / FRAME).roundToInt()
        val start = scenarioStart().copy(hull = 60f, fuel = 100f, detection = 0f, throttle = 0.2f, playerX = 0f, nextId = 1000)
        val inject = { frame: Int, run: RunState ->
            if (frame != gauntletFrame) run else run.copy(
                entities = run.entities + listOf(
                    Entity(id = 801, type = EntityType.MINE, lane = 1, x = 0f, z = 30f),
                    Entity(id = 802, type = EntityType.MINE, lane = 1, x = 0f, z = 50f),
                    Entity(id = 803, type = EntityType.WRECK, lane = 1, x = 0f, z = 70f)
                )
            )
        }
        val tool = play(start, 1, seconds, { frame, _ -> Act(lever = 0.2f, repair = frame == 0, drag = 0f) }, inject)
        val control = play(start, 1, seconds, { _, _ -> Act(lever = 0.2f, drag = 0f) }, inject)
        printRows("S5", 1, seconds, tool, control)
        assertEquals("tool repair used once (repair_uses ${tool.trace.repairUses})", 1, tool.trace.repairUses)
        assertEquals(
            "both twins take the full 70 gauntlet damage (tool ${tool.trace.damage}, control ${control.trace.damage})",
            70.0, tool.trace.damage, 0.01
        )
        assertTrue(
            "tool must survive (tool hull ${tool.end.hull}, outcome ${tool.outcome?.reason}; control hull ${control.end.hull})",
            tool.outcome == null && tool.end.hull > 0f
        )
        assertTrue(
            "control must die (control hull ${control.end.hull}, outcome ${control.outcome?.reason}; tool hull ${tool.end.hull})",
            control.outcome != null && !control.outcome.won && control.end.hull <= 0f
        )
    }

    /** A committed PATROL ram (INTERCEPT past the wind-up at y 22) reaches the hit row at about t 1.5 s. */
    @Test
    fun s6RepairBadUnderThreat() {
        val seconds = 2.5f
        val start = scenarioStart().copy(
            hull = 60f, fuel = 100f, detection = 0.1f, throttle = 0.2f, playerX = 0f, nextId = 1000,
            pursuers = listOf(patrol(22f, PursuerState.INTERCEPT, stateTimer = PursuerAI.WIND_UP))
        )
        val tool = play(start, 1, seconds, { frame, _ -> Act(lever = 0.2f, repair = frame == 0) })
        val control = play(start, 1, seconds, { _, _ -> Act(lever = 0.2f) })
        printRows("S6", 1, seconds, tool, control)
        val hitFrame = tool.states.indexOfFirst { it.hull < start.hull + it.repair.healed - 1f }
        println("S6 RAM-LANDS t=${decimal(tool.states[hitFrame].runElapsed.toDouble())} healed=${decimal(tool.end.repair.healed.toDouble())}")
        assertEquals("kit spent (tool kits ${tool.end.repair.kits}, control ${control.end.repair.kits})", 1 to 2, tool.end.repair.kits to control.end.repair.kits)
        assertEquals("repair fuel cost (tool ${tool.end.fuel}, control ${control.end.fuel})", 12f, control.end.fuel - tool.end.fuel, 0.01f)
        assertTrue("healed must be partial (healed ${tool.end.repair.healed})", tool.end.repair.healed > 0f && tool.end.repair.healed < 18f)
        assertFalse("repair must be interrupted", tool.end.repair.active)
        assertTrue(
            "feed must show the impact interrupt (feed ${tool.end.feed.take(5).map { it.text }})",
            tool.end.feed.any { it.text == "RIPARAZIONE INTERROTTA // IMPATTO" }
        )
        assertEquals("both twins are rammed (tool ${tool.trace.ramsLanded}, control ${control.trace.ramsLanded})", 1 to 1, tool.trace.ramsLanded to control.trace.ramsLanded)
        assertTrue(
            "tool detection must exceed control (tool ${tool.end.detection}, control ${control.end.detection})",
            tool.end.detection > control.end.detection
        )
    }

    // --- SONAR (spec :164)

    /** STORM chapter (index 3). The tool's only lane-change trigger is a hazard in its lane in `sonar.contactedEntities`. */
    @Test
    fun s7SonarGoodRevealsHiddenMine() {
        val seconds = 8f
        val chapter = 3
        assertEquals(EnvironmentId.STORM, CHAPTERS[chapter].sectors.first().environment)
        val start = scenarioStart(chapter).copy(
            fuel = 100f, detection = 0f, throttle = 0.5f, playerX = 0f, nextId = 1000,
            entities = listOf(Entity(id = 801, type = EntityType.MINE, lane = 1, x = 0f, z = 90f))
        )
        fun script(ping: Boolean): (Int, RunState) -> Act {
            var dodge = false
            return { frame, run ->
                if (run.sonar.contactedEntities.any { it.type.isHazard && it.lane == 1 }) dodge = true
                Act(lever = 0.5f, sonar = ping && frame == 0, drag = if (dodge) 1f else null)
            }
        }
        val tool = play(start, chapter, seconds, script(true))
        val control = play(start, chapter, seconds, script(false))
        printRows("S7", chapter, seconds, tool, control)
        assertEquals("tool pinged once (sonar_uses ${tool.trace.sonarUses})", 1, tool.trace.sonarUses)
        assertTrue("tool changed lane (x ${tool.end.playerX}, control x ${control.end.playerX})", abs(tool.end.playerX - 1f) < 0.1f)
        assertEquals("tool takes no hit (tool damage ${tool.trace.damage}, control ${control.trace.damage})", 0.0, tool.trace.damage, 0.0)
        assertTrue(
            "control stays in lane and is hit (control hull ${control.end.hull}, tool hull ${tool.end.hull})",
            control.end.hull < tool.end.hull
        )
    }

    @Test
    fun s8SonarBadNearAlarm() {
        val seconds = 1f
        val start = scenarioStart().copy(fuel = 100f, detection = 0.62f, throttle = 0.5f, playerX = 0f, pursuitCooldown = 0f, nextId = 1000)
        val tool = play(start, 1, seconds, { frame, _ -> Act(lever = 0.5f, sonar = frame == 0) })
        val control = play(start, 1, seconds, { _, _ -> Act(lever = 0.5f) })
        printRows("S8", 1, seconds, tool, control)
        assertEquals(
            "pursuers spawned (tool ${tool.trace.pursuits}, control ${control.trace.pursuits})",
            1 to 0, tool.trace.pursuits to control.trace.pursuits
        )
        assertEquals(
            "spawn on the step after the ping (tool detection ${tool.states[0].detection}, control ${control.states[0].detection})",
            1, tool.states[0].pursuers.size
        )
    }

    private companion object {
        const val SEED = 1001
        const val FRAME = 1f / 60f
        const val FRAMES_PER_SECOND = 60
        const val BURST_HOLD = 2.5f

        fun decimal(value: Double): String = String.format(Locale.ROOT, "%.3f", value)
    }
}
