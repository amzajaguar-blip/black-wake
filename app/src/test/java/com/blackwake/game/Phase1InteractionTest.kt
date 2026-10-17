package com.blackwake.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

/**
 * Phase 1 cross-system interaction and exploit tests (U6a). Rules already pinned in [GameSimulationTest]
 * are not repeated here; these cover the combinations between SONAR, RAZZO, SPINTA and RIPARA.
 */
class Phase1InteractionTest {
    private val progress = Progress()
    private val rng = Random(1234)

    /** Chapter 02: no scripted tutorial. Regular spawns are pushed far into the future. */
    private fun quietRun(chapter: Int = 1, from: Progress = progress): RunState =
        GameSimulation.newRun(chapter, from).copy(spawnClock = 1_000f)

    private fun step(state: RunState, input: RunInput = RunInput(), from: Progress = progress): StepResult =
        GameSimulation.step(state, 1, from, input, FRAME, rng)

    /** The stepped run with the sea emptied and regular spawns pushed back, to isolate the system under test. */
    private fun StepResult.calmSea(): RunState = run.copy(entities = emptyList(), spawnClock = 1_000f)

    /** Steps (seconds / FRAME).toInt() frames with an empty sea; the run must not end. */
    private fun simulate(start: RunState, seconds: Float, onFrame: (RunState) -> Unit = {}): RunState {
        var run = start
        repeat((seconds / FRAME).toInt()) {
            val result = step(run)
            assertNull("run ended unexpectedly: ${result.outcome?.reason}", result.outcome)
            run = result.calmSea()
            onFrame(run)
        }
        return run
    }

    /** Active repair started at lever 0.2: hull 50, fuel 60, detection 0.3, no spawns, empty sea. */
    private fun repairingRun(pursuers: List<Pursuer> = emptyList()): RunState {
        val start = quietRun().copy(
            hull = 50f, fuel = 60f, throttle = 0.2f, detection = 0.3f, pursuitCooldown = 100f,
            entities = emptyList(), pursuers = pursuers
        )
        val (repairing, _) = GameSimulation.startRepair(start)
        assertTrue("repair must start", repairing.repair.active)
        return repairing
    }

    private fun cruiser(id: Int, y: Float = PursuerAI.CRUISE_Y): Pursuer =
        Pursuer(id = id, type = EnemyBoatType.PATROL, x = 0f, y = y, state = PursuerState.PURSUIT)

    @Test
    fun interactionSonarAndFlareStackDetection() {
        val start = quietRun().copy(detection = 0.1f)
        val (flared, _) = GameSimulation.launchFlare(start)
        val (flareThenSonar, _) = GameSimulation.launchSonar(flared)
        val (pinged, _) = GameSimulation.launchSonar(start)
        val (sonarThenFlare, _) = GameSimulation.launchFlare(pinged)
        for ((label, run) in listOf("flare then sonar" to flareThenSonar, "sonar then flare" to sonarThenFlare)) {
            assertEquals("$label: 0.1 + 0.18 + 0.12", 0.40f, run.detection, 0.001f)
            assertEquals("$label: one flare charge spent", 2, run.flare.charges)
            assertTrue("$label: ping active", run.sonar.active)
        }
    }

    @Test
    fun interactionSonarSpikeSurvivesLowLever() {
        // Lever 0.1 is silent: both twins decay alike, so the ping cost is not erased by the quiet decay.
        val quiet = quietRun().copy(detection = 0.5f, throttle = 0.1f, pursuitCooldown = 100f)
        val (pinged, _) = GameSimulation.launchSonar(quiet)
        val pingedEnd = simulate(pinged, 1f)
        val quietEnd = simulate(quiet, 1f)
        assertTrue("both twins still decaying above 0 (quiet ${quietEnd.detection})", quietEnd.detection > 0f)
        assertEquals("ping spike kept after 1 s at lever 0.1", 0.12f, pingedEnd.detection - quietEnd.detection, 0.01f)
    }

    @Test
    fun interactionFlareThenQuietPlayerStaysLost() {
        val start = quietRun().copy(detection = 0.1f, throttle = 0f, pursuitCooldown = 100f, pursuers = listOf(cruiser(31, 70f)))
        val (flared, _) = GameSimulation.launchFlare(start)
        assertEquals(PursuerState.SEARCH, flared.pursuers.single().state)
        val hunting = setOf(PursuerState.PURSUIT, PursuerState.INTERCEPT)
        val end = simulate(flared, 3.5f) { run ->
            val p = run.pursuers.single()
            assertFalse("quiet player must not be reacquired (state ${p.state})", p.state in hunting)
        }
        val p = end.pursuers.single()
        assertFalse("not PURSUIT / INTERCEPT after 3.5 s (state ${p.state})", p.state in hunting)
        assertEquals("flare grace spent", 0f, p.flareGrace, 0f)
    }

    @Test
    fun interactionFlareThenBurstEscapesMarkedLane() {
        // Flare first (DC-3 allows it before the burst), then the burst ignites on the first frame at lever 0.9.
        val base = quietRun().copy(
            fuel = 100f, throttle = 0.9f, detection = 0.1f, pursuitCooldown = 100f, pursuers = listOf(cruiser(41, 70f))
        )
        assertTrue("pursuer within reach: Δ 15 ≤ FLARE_REACH", abs(70f - GameSimulation.PLAYER_Y) <= GameSimulation.FLARE_REACH)
        val (flared, _) = GameSimulation.launchFlare(base)
        assertEquals(PursuerState.SEARCH, flared.pursuers.single().state)
        assertEquals(4f, flared.flare.markerTimer, 0.001f)
        assertEquals("flare costs 0.18", 0.18f, flared.detection - base.detection, 0.001f)

        // Twins: both flared; only the burst run is armed. Baseline: no flare, no burst.
        val burstStart = flared
        val controlStart = flared.copy(burst = BurstState(armed = false))
        val baselineStart = base.copy(burst = BurstState(armed = false))
        val frames = (1f / FRAME).toInt()
        assertEquals("simulate(1f) steps 59 frames", 59, frames)
        val burstEnd = simulate(burstStart, 1f)
        val controlEnd = simulate(controlStart, 1f)
        val baselineEnd = simulate(baselineStart, 1f)
        assertTrue(burstEnd.burst.active)
        assertFalse(controlEnd.burst.active)
        assertFalse(baselineEnd.burst.active)
        assertTrue("investigation marker still running", burstEnd.flare.markerTimer > 0f)

        val bp = burstEnd.pursuers.single()
        val cp = controlEnd.pursuers.single()
        assertEquals(PursuerState.SEARCH, bp.state)
        assertEquals(PursuerState.SEARCH, cp.state)

        // Ignition +0.08 on frame 1, then 0.12/s on the other 58 frames; ordinary accumulation matches in all runs.
        val burstCost = 0.08f + 0.12f * (frames - 1) * FRAME
        assertEquals("burst cost over the flared control", burstCost, burstEnd.detection - controlEnd.detection, 0.005f)
        assertEquals("flare + burst cost over the untouched baseline", 0.18f + burstCost, burstEnd.detection - baselineEnd.detection, 0.005f)

        // SEARCH drifts to 110 at 8/s; the burst guarantees 18/s net, so the gap is 10 · 59 · FRAME ≈ 9.83.
        assertTrue("burst opens the gap on the marked lane (gap ${bp.y - cp.y})", bp.y - cp.y >= 9.5f)
        assertEquals("burst push 18/s from y 70", 70f + 18f * frames * FRAME, bp.y, 0.05f)
    }

    @Test
    fun interactionRepairUnderPursuitKeepsThreat() {
        // PATROL attackDelay is 4.5 s and lever 0.2 is not silent: the pursuit keeps going during the repair.
        val repairing = repairingRun(listOf(cruiser(51)))
        val end = simulate(repairing, 1f)
        val p = end.pursuers.single()
        assertTrue("repair must not shake off the pursuer (state ${p.state})", p.state == PursuerState.PURSUIT || p.state == PursuerState.INTERCEPT)
        assertTrue("repair still active", end.repair.active)
        assertEquals("repair grants no invulnerability", 0f, end.invulnerable, 0f)
    }

    @Test
    fun interactionSonarPingSteersSearchingHunter() {
        // Twins: the hunter sweeps around lockX -1; only the pinged twin has an origin at x 0 to investigate.
        val hunter = Pursuer(id = 61, type = EnemyBoatType.HUNTER, x = -1f, y = 100f, state = PursuerState.SEARCH, lockX = -1f)
        val start = quietRun().copy(detection = 0.1f, throttle = 0f, playerX = 0f, pursuitCooldown = 100f, pursuers = listOf(hunter))
        val (pinged, _) = GameSimulation.launchSonar(start)
        assertEquals(0f, pinged.sonar.pingOriginX, 0f)
        val pingedEnd = simulate(pinged, 0.5f).pursuers.single()
        val controlEnd = simulate(start, 0.5f).pursuers.single()
        val origin = pinged.sonar.pingOriginX
        assertTrue("hunter moves toward the ping origin (x ${pingedEnd.x})", abs(pingedEnd.x - origin) < abs(hunter.x - origin))
        assertTrue(
            "ping pulls closer than the unpinged sweep (pinged ${pingedEnd.x}, control ${controlEnd.x})",
            abs(pingedEnd.x - origin) < abs(controlEnd.x - origin)
        )
        assertEquals(PursuerState.SEARCH, pingedEnd.state)
    }

    @Test
    fun interactionSonarDuringRepairStacksCost() {
        val quiet = repairingRun()
        val (pinged, events) = GameSimulation.launchSonar(quiet)
        assertTrue(events.any { it is GameEvent.Tone && it.freq == 1320f })
        val pingedAfter = step(pinged).run
        val quietAfter = step(quiet).run
        assertTrue("repair stays active after the ping", pingedAfter.repair.active)
        assertTrue(quietAfter.repair.active)
        assertEquals("ping cost stacks on the repair surcharge", 0.12f, pingedAfter.detection - quietAfter.detection, 0.002f)
    }

    @Test
    fun interactionFlareDuringRepairBreaksLockKeepsRepair() {
        val repairing = repairingRun(listOf(cruiser(81, 70f)))
        val (flared, _) = GameSimulation.launchFlare(repairing)
        val p = flared.pursuers.single()
        assertEquals(PursuerState.SEARCH, p.state)
        assertEquals(2f, p.flareGrace, 0.001f)
        assertTrue("flare keeps the repair", flared.repair.active)
        assertEquals("flare costs 0.18 during the repair", 0.18f, flared.detection - repairing.detection, 0.001f)
        val after = step(flared).run
        assertTrue("repair continues after the flare", after.repair.active)
        assertEquals(PursuerState.SEARCH, after.pursuers.single().state)
    }

    @Test
    fun exploitFlareAndSonarSameFrameCrossThreshold() {
        val start = quietRun().copy(detection = 0.45f, pursuitCooldown = 0f, pursuers = emptyList())
        assertEquals("chapter 1 allows one active pursuer", 1, PursuerAI.maxActive(1))
        val (flared, _) = GameSimulation.launchFlare(start)
        val (pinged, _) = GameSimulation.launchSonar(flared)
        assertEquals("0.45 + 0.18 + 0.12", 0.75f, pinged.detection, 0.001f)
        val result = step(pinged)
        val run = result.run
        assertEquals("exactly one pursuer spawned", 1, run.pursuers.size)
        assertEquals("exactly one spawn tone", 1, result.events.count { it is GameEvent.Tone && it.freq == 400f })
    }

    @Test
    fun exploitRepairDoubleTapAndInstantCancel() {
        val start = quietRun().copy(hull = 50f, fuel = 60f, throttle = 0.2f, pursuitCooldown = 100f, entities = emptyList())
        val (first, _) = GameSimulation.startRepair(start)
        val (second, secondEvents) = GameSimulation.startRepair(first)
        assertSame("second tap changes nothing", first, second)
        assertTrue(secondEvents.isEmpty())
        assertEquals("one kit spent", 1, second.repair.kits)
        assertEquals("12 fuel spent once", 48f, second.fuel, 0.001f)

        val cancel = step(second.copy(throttle = 0.9f))
        val cancelled = cancel.run
        assertFalse("lever 0.9 cancels the repair", cancelled.repair.active)
        assertTrue(cancelled.feed.any { it.text == "RIPARAZIONE INTERROTTA // LEVA ALTA" })
        assertEquals("no kit refund", 1, cancelled.repair.kits)
        assertTrue("no fuel refund (fuel ${cancelled.fuel})", cancelled.fuel <= 48f)
        assertTrue(
            "hull gain ${cancelled.hull - start.hull} must not exceed one frame of heal",
            cancelled.hull - start.hull <= 6f * FRAME + 1e-4f
        )
    }

    @Test
    fun exploitSonarSpamSubmergedPaysOnce() {
        val start = quietRun().copy(dive = DiveState(submerged = true, oxygen = 50f), detection = 0.1f)
        val (pinged, _) = GameSimulation.launchSonar(start)
        assertEquals("one ping costs 0.12", 0.12f, pinged.detection - start.detection, 0.001f)
        val (spam, spamEvents) = GameSimulation.launchSonar(pinged)
        assertSame("second ping returns the identical run", pinged, spam)
        assertTrue(spamEvents.isEmpty())
    }

    @Test
    fun flareReachIncludesExactBoundary() {
        val boundaryY = GameSimulation.PLAYER_Y + GameSimulation.FLARE_REACH
        assertEquals(110f, boundaryY, 0f)
        val atBoundary = cruiser(1, boundaryY)
        val beyond = cruiser(2, boundaryY + 0.5f)
        val (flared, _) = GameSimulation.launchFlare(quietRun().copy(pursuers = listOf(atBoundary, beyond)))
        val dazzled = flared.pursuers.first { it.id == 1 }
        assertEquals(PursuerState.SEARCH, dazzled.state)
        assertEquals(2f, dazzled.flareGrace, 0.001f)
        assertEquals("pursuer at 110.5 unaffected", beyond, flared.pursuers.first { it.id == 2 })
    }

    @Test
    fun flareAllOutOfReachIsSoloLuce() {
        val far = cruiser(3, PursuerAI.SPAWN_Y)
        val start = quietRun().copy(pursuers = listOf(far))
        val (flared, _) = GameSimulation.launchFlare(start)
        val message = flared.message
        assertNotNull(message)
        assertEquals("RAZZO // SOLO LUCE", message!!.text)
        assertEquals(2, flared.flare.charges)
        assertEquals(0.18f, flared.detection - start.detection, 0.001f)
        assertEquals("pursuer at Δ 33 unchanged", far, flared.pursuers.single())
    }

    @Test
    fun spintaNaturalFuelOutLeavesOneActiveFrame() {
        // PINNED DEVIATION N6b: the burst-end check reads the previous frame's fuel, so on the frame the tank
        // runs dry the burst is still active; it ends on the next frame with the fuel cause and the overheat.
        // PATROL (fuel use 1.12) ignited at 12 fuel: 0.084 fuel per frame runs dry on frame 143, before the
        // 2.5 s timer (frame 150). A SPEEDBOAT burst burns at most 11.25, so it cannot run dry naturally.
        val patrol = Progress(boat = PlayerBoatType.PATROL)
        assertEquals(1.12f, boatSpec(PlayerBoatType.PATROL).fuelUse, 0f)
        var run = quietRun(from = patrol).copy(fuel = 12f, throttle = 0.9f, pursuitCooldown = 100f, entities = emptyList())
        var frame = 0
        var dryEvents: List<GameEvent> = emptyList()
        while (run.fuel > 0f && frame < 200) {
            val result = step(run, from = patrol)
            assertNull("run ended unexpectedly: ${result.outcome?.reason}", result.outcome)
            run = result.calmSea()
            dryEvents = result.events
            frame++
            assertTrue("burst active through frame $frame", run.burst.active)
        }
        val overheat = GameEvent.Vibrate(listOf(0L, 30L, 80L, 30L))
        assertEquals("tank runs dry on frame 143", 143, frame)
        assertEquals(0f, run.fuel, 0f)
        assertTrue("fuel-out frame: burst still active (N6b)", run.burst.active)
        assertTrue("before the burst timer end", run.burst.timer < GameSimulation.BURST_DURATION_SECONDS)
        assertFalse(run.feed.any { it.text.startsWith("SPINTA") })
        assertFalse(dryEvents.contains(overheat))

        val end = step(run, from = patrol)
        assertNull(end.outcome)
        val ended = end.run
        assertFalse("burst ends on frame 144", ended.burst.active)
        assertTrue(ended.burst.heatLock > 0f)
        assertEquals(1, ended.feed.count { it.text == "SPINTA INTERROTTA // CARBURANTE" })
        assertEquals("one overheat haptic", 1, end.events.count { it == overheat })
    }

    private companion object {
        const val FRAME = 1f / 60f
    }
}
