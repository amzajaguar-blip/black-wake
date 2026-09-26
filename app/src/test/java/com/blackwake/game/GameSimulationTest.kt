package com.blackwake.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameSimulationTest {
    private val progress = Progress()
    private val rng = Random(1234)

    /** Chapter 02: no scripted tutorial. Regular spawns are pushed far into the future. */
    private fun quietRun(chapter: Int = 1): RunState = GameSimulation.newRun(chapter, progress).copy(spawnClock = 1_000f)

    private fun step(run: RunState, input: RunInput = RunInput(), dt: Float = FRAME, chapter: Int = 1): StepResult =
        GameSimulation.step(run, chapter, progress, input, dt, rng)

    /** Runs whole seconds of simulation; optionally keeps the sea empty to isolate a subsystem. */
    private fun simulate(
        start: RunState,
        seconds: Float,
        input: RunInput = RunInput(),
        chapter: Int = 1,
        emptySea: Boolean = true,
        onFrame: (RunState) -> Unit = {}
    ): RunState {
        var run = start
        repeat((seconds / FRAME).toInt()) {
            val result = step(run, input, FRAME, chapter)
            assertNull("run ended unexpectedly: ${result.outcome?.reason}", result.outcome)
            run = if (emptySea) result.run.copy(entities = emptyList(), spawnClock = 1_000f) else result.run
            onFrame(run)
        }
        return run
    }

    @Test
    fun newRunStartsFromCleanRunState() {
        val run = GameSimulation.newRun(0, progress)
        assertEquals(100f, run.hull, 0f)
        assertEquals(100f, run.dive.oxygen, 0f)
        assertEquals(3, run.flare.charges)
        assertTrue(run.entities.isEmpty() && run.pursuers.isEmpty())
        assertEquals(GameSimulation.maxSonarCharges(progress.modules), run.sonar.charges)
    }

    @Test
    fun oxygenDrainsWhileDivingAndIsWrittenBack() {
        val run = simulate(quietRun(), 1f, RunInput(diveHeld = true))
        assertTrue(run.dive.submerged)
        assertEquals(75f, run.dive.oxygen, 1f)
    }

    @Test
    fun runningOutOfOxygenSurfacesAndLocksUntilReleased() {
        var run = simulate(quietRun(), 5f, RunInput(diveHeld = true))
        assertFalse(run.dive.submerged)
        assertTrue(run.dive.lockedOut)
        assertTrue("oxygen refilled above the dive threshold", run.dive.oxygen >= 10f)
        run = simulate(run, 1f, RunInput(diveHeld = true))
        assertFalse("holding the button must not restart the dive", run.dive.submerged)

        run = step(run, RunInput(diveHeld = false)).run
        assertFalse(run.dive.lockedOut)
        run = step(run, RunInput(diveHeld = true)).run
        assertTrue(run.dive.submerged)
    }

    @Test
    fun longDiveCostsNoHullAndSurfacesOnEmptyOxygen() {
        val start = quietRun().copy(entities = emptyList(), fuel = 100f)
        val run = simulate(start, 6f, RunInput(diveHeld = true))
        assertEquals(run.maxHull, run.hull, 0f)
        assertTrue(run.feed.any { it.text.contains("OSSIGENO ESAURITO") })
        assertFalse(run.dive.submerged)
        assertTrue(run.dive.lockedOut)
    }

    @Test
    fun sonarPingCompletesAndChargeRegenerates() {
        val (pinged, _) = GameSimulation.triggerSonar(quietRun())
        assertTrue(pinged.sonar.active)
        assertEquals(0, pinged.sonar.charges)

        var run = simulate(pinged, 2f)
        assertFalse("ping must finish", run.sonar.active)
        run = simulate(run, GameSimulation.sonarRegenSeconds(progress.modules) + 0.5f)
        assertEquals(1, run.sonar.charges)
    }

    @Test
    fun pursuerSpawnsWhenDetectedAndCanBeShakenOffBySilentRunning() {
        val start = quietRun().copy(detection = 0.75f, throttle = 0f)
        val spawned = step(start, RunInput()).run
        assertEquals(1, spawned.pursuers.size)

        var searched = false
        val end = simulate(spawned, 15f) { run ->
            if (run.pursuers.any { it.state == PursuerState.SEARCH }) searched = true
        }
        assertTrue("silent running should put the pursuer in SEARCH", searched)
        assertTrue("pursuer should give up and leave", end.pursuers.isEmpty())
    }

    @Test
    fun flareDazzlesActivePursuers() {
        val pursuer = Pursuer(id = 99, type = EnemyBoatType.PATROL, x = 0f, y = 70f, state = PursuerState.PURSUIT)
        val (run, _) = GameSimulation.launchFlare(quietRun().copy(pursuers = listOf(pursuer)))
        assertEquals(PursuerState.SEARCH, run.pursuers.single().state)
        assertEquals(2, run.flare.charges)
        assertTrue(run.flare.timer > 0f)
    }

    @Test
    fun ramDamagesTheHullUnlessSubmerged() {
        val ram = Pursuer(id = 5, type = EnemyBoatType.PATROL, x = 0f, y = 84f, state = PursuerState.INTERCEPT, stateTimer = PursuerAI.WIND_UP + 0.1f, lockX = 0f)
        val base = quietRun().copy(pursuers = listOf(ram))

        val hit = step(base).run
        assertEquals(100f - PursuerAI.spec(EnemyBoatType.PATROL).ramDamage, hit.hull, 0.5f)
        assertEquals(PursuerState.PURSUIT, hit.pursuers.single().state)

        val dodged = step(base, RunInput(diveHeld = true)).run
        assertEquals(100f, dodged.hull, 0.01f)
    }

    @Test
    fun hazardDamagesOnceThenInvulnerabilityWindowApplies() {
        val mine = Entity(id = 1, type = EntityType.MINE, lane = 1, x = 0f, z = 1f)
        val first = step(quietRun().copy(entities = listOf(mine))).run
        assertEquals(75f, first.hull, 0.2f)
        assertTrue(first.invulnerable > 0f)
        assertTrue(first.entities.none { it.id == 1 })

        val second = step(first.copy(entities = listOf(mine.copy(id = 2)))).run
        assertEquals(first.hull, second.hull, 0.2f)
    }

    @Test
    fun divingPassesUnderWrecksButNotMines() {
        val wreck = Entity(id = 1, type = EntityType.WRECK, lane = 1, x = 0f, z = 1f)
        val underWreck = step(quietRun().copy(entities = listOf(wreck)), RunInput(diveHeld = true)).run
        assertEquals(100f, underWreck.hull, 0.01f)

        val mine = Entity(id = 2, type = EntityType.MINE, lane = 1, x = 0f, z = 1f)
        val onMine = step(quietRun().copy(entities = listOf(mine)), RunInput(diveHeld = true)).run
        assertTrue(onMine.hull < 100f)
    }

    @Test
    fun visualContactMatchesCollisionPlane() {
        // Entities in the neighbouring lane never hit the player sitting on a lane centre.
        val side = Entity(id = 1, type = EntityType.MINE, lane = 2, x = 1f, z = 1f)
        val run = step(quietRun().copy(entities = listOf(side))).run
        assertEquals(100f, run.hull, 0.01f)
    }

    @Test
    fun hazardHitCostsHullOnceWithNoLingeringDrain() {
        var run = quietRun()
        val wreck = Entity(id = 1, type = EntityType.WRECK, lane = 1, x = run.playerX, z = 1f)
        run = run.copy(entities = listOf(wreck))
        repeat(60) {
            if (run.hull == run.maxHull) run = step(run).run
        }
        assertTrue("wreck must hit the hull", run.hull < run.maxHull)
        val hullAfterHit = run.hull
        val after = simulate(run.copy(entities = emptyList()), 3f)
        assertTrue(after.fuel > 0f)
        assertFalse(after.dive.submerged)
        assertEquals(hullAfterHit, after.hull, 0.01f)
    }

    @Test
    fun throttleTradesSpeedForSignature() {
        val fast = simulate(quietRun().copy(throttle = 1f), 3f)
        val silent = simulate(quietRun().copy(throttle = 0f), 3f)
        assertTrue(fast.sectorProgress > silent.sectorProgress)
        assertTrue(fast.detection > silent.detection)
        assertTrue(fast.fuel < silent.fuel)
    }

    @Test
    fun extractionIsWonOnlyWithRequiredIntel() {
        val chapter = CHAPTERS[0]
        val last = chapter.sectors.lastIndex
        val closing = GameSimulation.newRun(0, progress).copy(
            sectorIndex = last,
            sectorProgress = chapter.sectors[last].duration - 0.001f,
            spawnClock = 1_000f
        )

        val won = step(closing.copy(intel = chapter.intelRequired), chapter = 0).outcome
        assertNotNull(won)
        assertTrue(won!!.won)
        assertEquals(chapter.intelRequired, won.banked)

        val lost = step(closing.copy(intel = chapter.intelRequired - 1), chapter = 0).outcome
        assertNotNull(lost)
        assertFalse(lost!!.won)
    }

    @Test
    fun deathTakesPriorityOverExtraction() {
        val chapter = CHAPTERS[0]
        val last = chapter.sectors.lastIndex
        val mine = Entity(id = 1, type = EntityType.MINE, lane = 1, x = 0f, z = 1f)
        val run = GameSimulation.newRun(0, progress).copy(
            sectorIndex = last,
            sectorProgress = chapter.sectors[last].duration - 0.001f,
            intel = chapter.intelRequired,
            hull = 10f,
            entities = listOf(mine),
            spawnClock = 1_000f
        )
        val outcome = step(run, chapter = 0).outcome
        assertNotNull(outcome)
        assertFalse(outcome!!.won)
    }

    @Test
    fun pursuitWaveAlwaysLeavesAnEscapeLane() {
        val run = step(quietRun().copy(detection = 0.95f)).run
        val wave = run.entities.filter { it.type == EntityType.ENEMY }
        assertEquals(2, wave.size)
        assertEquals(2, wave.map { it.lane }.distinct().size)
    }

    @Test
    fun forkChoiceAppliesItsRewardAndClearsTheOtherRoutes() {
        val forks = listOf(
            Entity(id = 1, type = EntityType.FORK, lane = 0, x = -1f, z = 1f, value = 0),
            Entity(id = 2, type = EntityType.FORK, lane = 1, x = 0f, z = 1f, value = 1),
            Entity(id = 3, type = EntityType.FORK, lane = 2, x = 1f, z = 1f, value = 2)
        )
        val run = step(quietRun().copy(entities = forks)).run
        assertEquals(3, run.intel)
        assertTrue(run.entities.none { it.type == EntityType.FORK })
    }

    @Test
    fun runningDryDrainsTheHull() {
        val run = simulate(quietRun().copy(fuel = 0f), 2f)
        assertTrue(run.hull < 100f)
        assertTrue(run.fuelOutNotified)
    }

    @Test
    fun lowPriorityMessagesDoNotHideDangerMessages() {
        val danger = HudMessage("IMPATTO // MINA", MessageTone.DANGER, 1.5f)
        val nearMiss = Entity(id = 1, type = EntityType.WRECK, lane = 2, x = 1f, z = -0.1f)
        val run = step(quietRun().copy(message = danger, playerX = 0.6f, entities = listOf(nearMiss))).run
        assertEquals("IMPATTO // MINA", run.message?.text)
        assertTrue(run.feed.any { it.text == "SCHIVATA AL LIMITE" })
    }

    @Test
    fun overrideDrainsBatteryAndShutsDownWhenEmpty() {
        val (active, _) = GameSimulation.toggleOverride(quietRun().copy(power = PowerState(battery = 12f)))
        assertTrue(active.power.overrideActive)
        val drained = simulate(active, 2f)
        assertFalse(drained.power.overrideActive)
    }

    @Test
    fun campaignDataIsWellFormed() {
        assertEquals(8, CHAPTERS.size)
        for (chapter in CHAPTERS) {
            assertTrue(chapter.code, chapter.sectors.last().extract)
            assertEquals(chapter.code, 1, chapter.sectors.count { it.extract })
            assertEquals(chapter.code, 1, chapter.sectors.count { it.fork })
            assertTrue(chapter.code, chapter.intelRequired in 1..chapter.intelGoal)
        }
        assertEquals(7, CHAPTERS[0].intelRequired)
        assertEquals(14, CHAPTERS[7].intelRequired)
        assertEquals(PlayerBoatType.entries.size, BOATS.map { it.type }.distinct().size)
    }

    @Test
    fun failedRunsBankFortyPercent() {
        assertEquals(10, GameSimulation.bankedIntel(true, 10))
        assertEquals(4, GameSimulation.bankedIntel(false, 10))
    }

    private companion object {
        const val FRAME = 1f / 60f
    }
}
