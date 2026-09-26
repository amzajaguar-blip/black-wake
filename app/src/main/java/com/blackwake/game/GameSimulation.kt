package com.blackwake.game

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sign
import kotlin.random.Random

data class RunInput(
    /** Where the player is dragging the boat, on the -1..1 lane axis; null when not touching the sea. */
    val dragTargetX: Float? = null,
    val diveHeld: Boolean = false
)

/** Side effects requested by the simulation; the ViewModel turns them into sound and haptics. */
sealed interface GameEvent {
    data class Tone(val freq: Float, val seconds: Float, val gain: Float = 0.35f) : GameEvent
    data class Noise(val seconds: Float, val gain: Float = 0.6f) : GameEvent
    data class Vibrate(val pattern: List<Long>) : GameEvent
}

class StepResult(val run: RunState, val events: List<GameEvent>, val outcome: RunOutcome?)

/**
 * The whole mission rule set as pure functions of [RunState]. No Android types, no global state:
 * randomness is injected, side effects are returned as [GameEvent]s.
 */
object GameSimulation {
    const val PLAYER_Y = 85f
    const val PING_RANGE = 110f
    const val PICKUP_HIT = 0.45f
    const val HAZARD_HIT = 0.30f
    const val NEAR_MISS = 0.55f
    const val RAM_HIT = 0.25f
    const val FLARE_SECONDS = 5f
    const val MAX_FEED = 60
    private const val PING_SPEED = 80f
    private const val MESSAGE_SECONDS = 1.8f
    private const val RADIO_SECONDS = 5.5f

    private class TutorialStep(val at: Float, val spawns: List<Pair<EntityType, Int>>, val hint: String)

    private val TUTORIAL = listOf(
        TutorialStep(1.0f, listOf(EntityType.FUEL to 1), "TRASCINA SUL MARE PER CAMBIARE CORSIA"),
        TutorialStep(3.5f, listOf(EntityType.INTEL to 2), "RACCOGLI L'INTEL: SERVE PER L'ESTRAZIONE"),
        TutorialStep(6.0f, listOf(EntityType.WRECK to 1), "RELITTO: SCHIVALO O TIENI PREMUTO IMMERGI"),
        TutorialStep(8.5f, listOf(EntityType.FUEL to 0, EntityType.MINE to 1, EntityType.INTEL to 2), "LEVA ALTA: VELOCE MA VISIBILE"),
        TutorialStep(10.5f, listOf(EntityType.ENEMY to 0, EntityType.ENEMY to 2), "LEVA BASSA: SILENZIO. LA FIRMA CALA")
    )

    fun maxSonarCharges(modules: Modules): Int = 1 + modules.radar

    fun sonarRegenSeconds(modules: Modules): Float = 20f - modules.radar * 3f

    fun bankedIntel(won: Boolean, intel: Int): Int = if (won) intel else (intel * 0.4f).toInt()

    fun newRun(chapterIndex: Int, progress: Progress): RunState {
        val chapter = CHAPTERS[chapterIndex]
        val boat = boatSpec(progress.boat)
        val maxHull = 100f + progress.modules.hull * 7f + boat.hullBonus
        val first = chapter.sectors.first()
        val title = "CAP. ${chapter.code} // ${first.title}"
        return RunState(
            hull = maxHull,
            maxHull = maxHull,
            sonar = SonarState(charges = maxSonarCharges(progress.modules)),
            message = HudMessage(title, MessageTone.INFO, MESSAGE_SECONDS),
            radio = RadioLine(first.radio, RADIO_SECONDS),
            feed = listOf(TerminalMessage(first.radio, MessageTone.INFO, 0f), TerminalMessage(title, MessageTone.INFO, 0f))
        )
    }

    fun setThrottle(run: RunState, value: Float): RunState = run.copy(throttle = value.coerceIn(0f, 1f))

    fun triggerSonar(run: RunState): Pair<RunState, List<GameEvent>> {
        val sonar = run.sonar
        if (sonar.charges <= 0 || sonar.active) return run to emptyList()
        return run.copy(sonar = sonar.copy(charges = sonar.charges - 1, active = true, radius = 0f)) to
            listOf(GameEvent.Tone(1320f, 0.22f, 0.3f))
    }

    fun launchFlare(run: RunState): Pair<RunState, List<GameEvent>> {
        val flare = run.flare
        if (flare.charges <= 0 || flare.timer > 0f || run.dive.submerged) return run to emptyList()
        val dazzled = run.pursuers.count { it.state == PursuerState.PURSUIT || it.state == PursuerState.INTERCEPT || it.state == PursuerState.DETECTED }
        val next = run.copy(
            flare = FlareState(flare.charges - 1, FLARE_SECONDS),
            pursuers = run.pursuers.map(PursuerAI::dazzle),
            screenFlash = 0.5f,
            flashKind = FlashKind.FLARE
        )
        val text = if (dazzled > 0) "RAZZO // INSEGUITORI ACCECATI" else "RAZZO // ACQUA ILLUMINATA"
        return withMessages(next, listOf(text to MessageTone.WARNING)) to listOf(GameEvent.Noise(0.35f, 0.5f))
    }

    fun step(prev: RunState, chapterIndex: Int, progress: Progress, input: RunInput, dt: Float, rng: Random): StepResult {
        val chapter = CHAPTERS[chapterIndex]
        val sector = chapter.sectors[prev.sectorIndex.coerceIn(0, chapter.sectors.lastIndex)]
        val modules = progress.modules
        val boat = boatSpec(progress.boat)
        val events = ArrayList<GameEvent>()
        val said = ArrayList<Pair<String, MessageTone>>()
        fun say(text: String, tone: MessageTone) { said += text to tone }

        val elapsed = prev.runElapsed + dt
        var invulnerable = max(0f, prev.invulnerable - dt)
        var cameraShake = max(0f, prev.cameraShake - dt * 3f)
        var screenFlash = max(0f, prev.screenFlash - dt * 4f)
        var flashKind = if (screenFlash > 0f) prev.flashKind else FlashKind.NONE
        fun flash(kind: FlashKind, amount: Float) { screenFlash = max(screenFlash, amount); flashKind = kind }

        // --- Engine
        val hasFuel = prev.fuel > 0f
        val throttle = if (hasFuel) prev.throttle else 0f
        val boosting = throttle > 0.8f
        val silent = throttle < 0.2f

        // --- Dive
        val prevDive = prev.dive
        val canDive = !prevDive.lockedOut && (prevDive.oxygen >= 15f || (prevDive.submerged && prevDive.oxygen > 0f))
        val wantsDive = input.diveHeld && canDive
        val oxygen = if (wantsDive) max(0f, prevDive.oxygen - dt * 25f)
        else min(100f, prevDive.oxygen + dt * 15f)
        val submerged = wantsDive && oxygen > 0f
        if (submerged && !prevDive.submerged) events += GameEvent.Tone(160f, 0.3f, 0.3f)
        val ranOut = prevDive.submerged && !submerged && input.diveHeld
        if (ranOut) say("OSSIGENO ESAURITO // EMERSIONE", MessageTone.WARNING)
        val lockedOut = input.diveHeld && (prevDive.lockedOut || ranOut)
        val dive = DiveState(submerged = submerged, oxygen = oxygen, lockedOut = lockedOut)

        // --- Speed: throttle, vessel
        val targetSpeed = (0.8f + throttle * 0.4f) * boat.speed
        val speedFactor = prev.speedFactor + (targetSpeed - prev.speedFactor) * min(1f, dt * 5f)

        // --- Sonar
        var charges = prev.sonar.charges
        var regen = prev.sonar.regenTimer
        var pingActive = prev.sonar.active
        var pingRadius = prev.sonar.radius
        if (charges < maxSonarCharges(modules)) {
            regen += dt
            if (regen >= sonarRegenSeconds(modules)) {
                charges++
                regen = 0f
            }
        } else {
            regen = 0f
        }
        if (pingActive) {
            val oldRadius = pingRadius
            pingRadius += dt * PING_SPEED
            if (prev.entities.any { it.type.isHazard && it.z > oldRadius && it.z <= pingRadius }) {
                events += GameEvent.Vibrate(listOf(0, 30))
            }
            if (pingRadius >= PING_RANGE) {
                pingActive = false
                pingRadius = 0f
            }
        }
        val sonar = SonarState(charges, regen, pingActive, pingRadius)

        // --- Detection
        val hiding = prev.entities.any { it.type == EntityType.WRECK && it.z > 0f && it.z < 25f && abs(it.x - prev.playerX) < 0.6f }
        var stealth = (0.3f + throttle * 0.7f) * boat.detection
        if (hiding) stealth *= 0.2f
        if (submerged) stealth *= 0.1f
        val boostAccumulation = if (throttle > 0.6f) (throttle - 0.6f) * 0.3f else 0f
        val accumulation = (0.035f * sector.threat + prev.activePursuers * 0.02f + boostAccumulation) * stealth
        val decay = when {
            submerged -> 0.30f + modules.stealth * 0.05f
            silent -> 0.16f + modules.stealth * 0.04f
            hiding && !boosting -> 0.12f + modules.stealth * 0.02f
            else -> 0.02f
        }
        var detection = (prev.detection + dt * (accumulation - decay)).coerceIn(0f, 1f)

        // --- Fuel
        val boostUse = throttle * throttle * 6.1f * (1f - 0.12f * modules.engine)
        var fuel = max(0f, prev.fuel - dt * (0.5f + boostUse) * boat.fuelUse)

        // --- Continuous hull stress
        var hull = prev.hull
        var deathReason = "Lo scafo non regge. La prova resta sotto la marea."
        if (fuel <= 0f && invulnerable <= 0f) {
            hull -= 2.3f * dt
            deathReason = "Senza carburante il mare entra nello scafo."
        }
        // --- Player movement: magnetic lanes with inertia
        val drag = input.dragTargetX
        val nearestLane = round(prev.playerX).coerceIn(-1f, 1f)
        val inputForce = if (drag != null) (drag - prev.playerX) * 15f else 0f
        val magnet = if (boosting) 3f else if (drag != null) 6f else 12f
        var velocityX = prev.playerVelocityX + (inputForce + (nearestLane - prev.playerX) * magnet - prev.playerVelocityX * 8f) * dt
        if (drag != null && sign(drag - prev.playerX) != sign(velocityX)) velocityX *= 0.5f
        var playerX = prev.playerX + velocityX * dt
        if (playerX > 1.15f) {
            playerX = 1.15f
            if (velocityX > 0f) velocityX = 0f
        } else if (playerX < -1.15f) {
            playerX = -1.15f
            if (velocityX < 0f) velocityX = 0f
        }

        // --- Pursuers
        var nextId = prev.nextId
        var pursuitCooldown = prev.pursuitCooldown - dt
        val ctx = PursuerAI.Context(prev.playerX, prev.playerVelocityX, detection, silent || submerged, dt)
        val pursuers = ArrayList<Pursuer>(prev.pursuers.size + 1)
        for (p in prev.pursuers) {
            var next = PursuerAI.step(p, ctx, prev.pursuers)
            if (next.state == PursuerState.LOST_TARGET && p.state != PursuerState.LOST_TARGET) {
                say("CONTATTO PERSO // ${enemyLabel(p.type)}", MessageTone.INFO)
                detection = max(0f, detection - 0.08f)
            }
            if (next.state == PursuerState.INTERCEPT && p.state != PursuerState.INTERCEPT) {
                say("SPERONAMENTO IN ARRIVO // CAMBIA CORSIA", MessageTone.DANGER)
                events += GameEvent.Tone(520f, 0.25f, 0.4f)
            }
            if (PursuerAI.isGone(next)) continue
            if (PursuerAI.canRam(next) && !submerged && abs(next.y - PLAYER_Y) < 4f && abs(next.x - playerX) < RAM_HIT) {
                if (invulnerable <= 0f) {
                    hull -= max(4f, PursuerAI.spec(next.type).ramDamage - modules.hull * 2f)
                    deathReason = "Speronato da ${enemyLabel(next.type)}. La prova affonda con te."
                    invulnerable = 1.2f
                    cameraShake = max(cameraShake, 0.8f)
                    flash(FlashKind.DAMAGE, 1f)
                    velocityX = (if (playerX >= next.x) 1f else -1f) * 12f
                    events += GameEvent.Noise(0.3f)
                    events += GameEvent.Vibrate(listOf(0, 120, 60, 160))
                    say("SPERONATO // ${enemyLabel(next.type)}", MessageTone.DANGER)
                }
                // Bounce back into pursuit instead of ramming through again.
                next = next.copy(state = PursuerState.PURSUIT, stateTimer = 0f, attackTimer = 0f)
            }
            pursuers += next
        }
        if (detection >= 0.7f && pursuers.count { it.isActive } < PursuerAI.maxActive(chapterIndex) && pursuitCooldown <= 0f) {
            val type = PursuerAI.pickType(chapterIndex, detection, rng)
            val x = (prev.playerX + rng.nextFloat() - 0.5f).coerceIn(-1f, 1f)
            pursuers += Pursuer(nextId++, type, x = x, y = PursuerAI.SPAWN_Y, lockX = x)
            say("INSEGUITORE IN AVVICINAMENTO // ${enemyLabel(type)}", MessageTone.WARNING)
            events += GameEvent.Tone(400f, 0.5f, 0.35f)
            pursuitCooldown = 6f
        }

        // --- Entities: pursuit wave, scripted tutorial, regular spawns, fork
        val entities = ArrayList<Entity>(prev.entities.size + 4)
        entities.addAll(prev.entities)
        var waveCooldown = prev.waveCooldown - dt
        if (detection >= 0.91f && waveCooldown <= 0f) {
            waveCooldown = 8f
            cameraShake = max(cameraShake, 1f)
            say("CONTATTO PIENO // ONDA D'INSEGUIMENTO", MessageTone.DANGER)
            val gap = rng.nextInt(3)
            for (lane in 0..2) {
                if (lane != gap) entities += Entity(nextId++, EntityType.ENEMY, lane, lane - 1f, 66f + lane * 4f)
            }
        }

        var sectorIndex = prev.sectorIndex
        var sectorProgress = prev.sectorProgress + dt * speedFactor
        var sectorElapsed = prev.sectorElapsed + dt
        var spawnClock = prev.spawnClock - dt
        var tutorialStep = prev.tutorialStep
        var forkSpawned = prev.forkSpawned
        var forkNotice = prev.forkNotice
        var radio = prev.radio?.let { if (it.timer > dt) it.copy(timer = it.timer - dt) else null }
        val extraFeed = ArrayList<String>()

        if (chapterIndex == 0 && sectorIndex == 0 && tutorialStep < TUTORIAL.size) {
            val step = TUTORIAL[tutorialStep]
            if (sectorElapsed > step.at) {
                for ((type, lane) in step.spawns) entities += Entity(nextId++, type, lane, lane - 1f, 100f)
                say(step.hint, MessageTone.INFO)
                tutorialStep++
                if (tutorialStep == TUTORIAL.size) spawnClock = 2.5f
            }
        } else if (spawnClock <= 0f) {
            val roll = rng.nextFloat()
            val threatScale = if (detection > 0.65f) 1.5f else 1f
            val intelCut = sector.intelBias * 0.52f
            val fuelCut = intelCut + 0.12f
            val type = when {
                roll < intelCut -> EntityType.INTEL
                roll < fuelCut -> EntityType.FUEL
                roll < fuelCut + sector.threat * 0.32f * threatScale -> if (rng.nextBoolean()) EntityType.ENEMY else EntityType.MINE
                else -> EntityType.WRECK
            }
            val z = 96f + rng.nextFloat() * 18f
            val lane = freeLane(entities, z, rng)
            val enemyType = if (type == EntityType.ENEMY) staticEnemyType(rng) else EnemyBoatType.PATROL
            entities += Entity(nextId++, type, lane, lane - 1f, z, enemyType = enemyType)
            spawnClock = max(0.45f, 1.15f - sector.density * 0.48f + rng.nextFloat() * 0.35f)
        }

        if (sector.fork && !forkSpawned && sectorProgress > 3.2f) {
            forkSpawned = true
            forkNotice = ForkNotice("BIVIO // 01 SICURO · 02 INTEL · 03 VOLATILE", elapsed + 6.2f)
            entities.removeAll { it.type != EntityType.FORK && it.z > 72f && it.z < 96f }
            entities += Entity(nextId++, EntityType.FORK, 0, -1f, 84f, 0, "01 SICURO")
            entities += Entity(nextId++, EntityType.FORK, 1, 0f, 84f, 1, "02 INTEL")
            entities += Entity(nextId++, EntityType.FORK, 2, 1f, 84f, 2, "03 VOLATILE")
            spawnClock = max(spawnClock, 1.5f)
            events += GameEvent.Tone(880f, 0.15f, 0.25f)
        }

        // --- Move entities and resolve contacts
        val entitySpeed = (18f + sector.threat * 4f + (if (detection > 0.65f) 3.5f else 0f)) * speedFactor
        var intel = prev.intel
        var combo = prev.comboCounter
        var comboMultiplier = prev.comboMultiplier
        if (!silent && comboMultiplier > 1) {
            combo = 0
            comboMultiplier = 1
        }
        var forkTaken = false
        val iterator = entities.listIterator()
        while (iterator.hasNext()) {
            val e = iterator.next()
            val z = e.z - dt * entitySpeed
            var moved = e.copy(z = z)
            val lateral = abs(e.x - playerX)
            val atBow = z < 2f && z > -4f

            if (e.type.isHazard && !e.nearMissed && z < 0f && z > -5f && lateral >= HAZARD_HIT && lateral < NEAR_MISS && !submerged) {
                fuel = min(100f, fuel + 2f)
                detection = max(0f, detection - 0.02f)
                say("SCHIVATA AL LIMITE", MessageTone.INFO)
                moved = moved.copy(nearMissed = true)
            }

            var consumed = false
            if (atBow) {
                when (e.type) {
                    EntityType.INTEL -> if (!submerged && lateral < PICKUP_HIT) {
                        consumed = true
                        if (silent) combo++ else {
                            combo = 0
                            comboMultiplier = 1
                        }
                        if (combo >= 3) {
                            comboMultiplier = min(4, comboMultiplier + 1)
                            combo = 0
                            say("COMBO SILENZIOSA x$comboMultiplier", MessageTone.INFO)
                        } else {
                            say(if (comboMultiplier > 1) "INTEL +$comboMultiplier" else "INTEL RECUPERATA", MessageTone.INFO)
                        }
                        intel += comboMultiplier
                        flash(FlashKind.INTEL, 0.5f)
                        events += GameEvent.Tone(880f, 0.1f, 0.35f)
                    }

                    EntityType.FUEL -> if (!submerged && lateral < PICKUP_HIT) {
                        consumed = true
                        fuel = min(100f, fuel + 20f + modules.tank * 4f)
                        say("CARBURANTE RECUPERATO", MessageTone.INFO)
                        flash(FlashKind.FUEL, 0.5f)
                        events += GameEvent.Tone(660f, 0.12f, 0.35f)
                    }

                    // Mines float at depth: diving does not help against them.
                    EntityType.WRECK, EntityType.MINE -> if (lateral < HAZARD_HIT && (e.type == EntityType.MINE || !submerged)) {
                        consumed = true
                        if (invulnerable <= 0f) {
                            combo = 0
                            comboMultiplier = 1
                            val damage = (if (e.type == EntityType.MINE) 25f else 20f) - modules.hull * 3f
                            hull -= damage
                            deathReason = if (e.type == EntityType.MINE) "Una mina apre lo scafo in due." else "Il relitto squarcia lo scafo."
                            invulnerable = 0.86f
                            velocityX = (if (playerX > e.x) 1f else -1f) * 15f
                            cameraShake = max(cameraShake, 1f)
                            flash(FlashKind.DAMAGE, 1f)
                            events += GameEvent.Noise(0.25f)
                            events += GameEvent.Vibrate(listOf(0, 100, 50, 150, 50, 200))
                            say(if (e.type == EntityType.MINE) "IMPATTO // MINA" else "IMPATTO // RELITTO", MessageTone.DANGER)
                        }
                    }

                    EntityType.ENEMY -> if (!submerged && lateral < HAZARD_HIT) {
                        consumed = true
                        detection = min(1f, detection + 0.18f)
                        if (invulnerable <= 0f) {
                            combo = 0
                            comboMultiplier = 1
                            val damage = 14f - modules.hull * 2f
                            hull -= damage
                            deathReason = "Black Tide ha chiuso la rotta."
                            invulnerable = 0.86f
                            velocityX = (if (playerX > e.x) 1f else -1f) * 12f
                            cameraShake = max(cameraShake, 0.8f)
                            flash(FlashKind.DAMAGE, 1f)
                            events += GameEvent.Noise(0.2f)
                            events += GameEvent.Vibrate(listOf(0, 80, 40, 120))
                            say("IMPATTO // BLACK TIDE", MessageTone.DANGER)
                        }
                    }

                    EntityType.FORK -> if (!forkTaken && lateral < PICKUP_HIT) {
                        consumed = true
                        forkTaken = true
                        forkNotice = null
                        when (e.value) {
                            0 -> {
                                detection = max(0f, detection - 0.16f)
                                say("ROTTA SICURA // FIRMA RIDOTTA", MessageTone.INFO)
                            }
                            1 -> {
                                intel += 3
                                detection = min(1f, detection + 0.1f)
                                say("ROTTA INTEL // +3 DATI", MessageTone.WARNING)
                            }
                            else -> {
                                fuel = min(100f, fuel + 28f)
                                detection = min(1f, detection + 0.26f)
                                cameraShake = max(cameraShake, 0.5f)
                                say("ROTTA VOLATILE // TI HANNO VISTO", MessageTone.DANGER)
                            }
                        }
                        events += GameEvent.Tone(990f, 0.14f, 0.3f)
                    }
                }
            }

            when {
                consumed || z < -12f -> iterator.remove()
                else -> iterator.set(moved)
            }
        }
        if (forkTaken) entities.removeAll { it.type == EntityType.FORK }
        if (forkNotice != null && elapsed > forkNotice.expiresAt) forkNotice = null

        var fuelOutNotified = prev.fuelOutNotified
        if (fuel <= 0f && !fuelOutNotified) {
            fuelOutNotified = true
            say("CARBURANTE ESAURITO // DERIVA", MessageTone.DANGER)
        } else if (fuel > 0f) {
            fuelOutNotified = false
        }

        // --- Proximity ping for hazards in the player's lane
        var nearest = 60f
        for (e in entities) if (e.type.isHazard && e.z > 0f && e.z < nearest && abs(e.x - playerX) < 0.6f) nearest = e.z
        var proximityPing = prev.proximityPingTimer
        if (nearest < 60f) {
            proximityPing += dt
            if (proximityPing >= max(0.15f, nearest / 60f * 1.5f)) {
                proximityPing = 0f
                events += GameEvent.Tone(600f + (1f - nearest / 60f) * 800f, 0.05f, 0.12f)
            }
        } else {
            proximityPing = 0f
        }

        // --- Death, then sector completion
        hull = max(0f, hull)
        var outcome: RunOutcome? = null
        if (hull <= 0f) {
            outcome = RunOutcome(false, deathReason, intel, chapter.intelRequired, bankedIntel(false, intel), max(prev.peakDetection, detection), elapsed)
        } else if (sectorProgress >= sector.duration) {
            if (sector.extract) {
                val won = intel >= chapter.intelRequired
                val reason = if (won) "I dati lasciano il mare. Per questa notte, abbastanza."
                else "La trasmissione è incompleta: ${intel}/${chapter.intelRequired} intel. Il segnale si spegne."
                outcome = RunOutcome(won, reason, intel, chapter.intelRequired, bankedIntel(won, intel), max(prev.peakDetection, detection), elapsed)
            } else {
                sectorIndex++
                sectorProgress = 0f
                sectorElapsed = 0f
                forkSpawned = false
                forkNotice = null
                val next = chapter.sectors[sectorIndex]
                say("${next.title} // ${next.tag}", if (next.extract) MessageTone.WARNING else MessageTone.INFO)
                radio = RadioLine(next.radio, RADIO_SECONDS)
                extraFeed += next.radio
                events += GameEvent.Tone(if (next.extract) 740f else 520f, 0.18f, 0.3f)
            }
        }

        val roll = prev.playerRoll + (velocityX * -15f - prev.playerRoll) * min(1f, dt * 10f)
        val base = prev.copy(
            sectorIndex = sectorIndex,
            sectorProgress = sectorProgress,
            sectorElapsed = sectorElapsed,
            runElapsed = elapsed,
            spawnClock = spawnClock,
            tutorialStep = tutorialStep,
            hull = hull,
            fuel = fuel,
            fuelOutNotified = fuelOutNotified,
            intel = intel,
            detection = detection,
            peakDetection = max(prev.peakDetection, detection),
            invulnerable = invulnerable,
            pursuitCooldown = pursuitCooldown,
            waveCooldown = waveCooldown,
            playerX = playerX,
            playerVelocityX = velocityX,
            playerRoll = roll,
            speedFactor = speedFactor,
            dive = dive,
            sonar = sonar,
            flare = prev.flare.copy(timer = max(0f, prev.flare.timer - dt)),
            comboCounter = combo,
            comboMultiplier = comboMultiplier,
            cameraShake = cameraShake,
            screenFlash = screenFlash,
            flashKind = flashKind,
            forkSpawned = forkSpawned,
            forkNotice = forkNotice,
            proximityPingTimer = proximityPing,
            entities = entities.sortedByDescending { it.z },
            pursuers = pursuers,
            nextId = nextId,
            message = prev.message?.let { if (it.timer > dt) it.copy(timer = it.timer - dt) else null },
            radio = radio,
            lastDeltaTime = dt
        )
        val withRadio = if (extraFeed.isEmpty()) base
        else base.copy(feed = (extraFeed.map { TerminalMessage(it, MessageTone.INFO, elapsed) } + base.feed).take(MAX_FEED))
        return StepResult(withMessages(withRadio, said), events, outcome)
    }

    /** Appends messages to the log and shows the most urgent one, unless something more urgent is on screen. */
    private fun withMessages(run: RunState, said: List<Pair<String, MessageTone>>): RunState {
        if (said.isEmpty()) return run
        val feed = (said.asReversed().map { TerminalMessage(it.first, it.second, run.runElapsed) } + run.feed).take(MAX_FEED)
        val top = said.last { candidate -> said.none { it.second > candidate.second } }
        val current = run.message
        val message = if (current == null || top.second >= current.tone) HudMessage(top.first, top.second, MESSAGE_SECONDS) else current
        return run.copy(feed = feed, message = message)
    }

    private fun freeLane(entities: List<Entity>, z: Float, rng: Random): Int {
        val start = rng.nextInt(3)
        for (i in 0 until 3) {
            val lane = (start + i) % 3
            if (entities.none { it.lane == lane && abs(it.z - z) < 10f }) return lane
        }
        return start
    }

    private fun staticEnemyType(rng: Random): EnemyBoatType {
        val r = rng.nextFloat()
        return when {
            r < 0.3f -> EnemyBoatType.PATROL
            r < 0.6f -> EnemyBoatType.INTERCEPTOR
            r < 0.8f -> EnemyBoatType.POLICE
            r < 0.9f -> EnemyBoatType.HUNTER
            r < 0.95f -> EnemyBoatType.ARMORED
            else -> EnemyBoatType.ELITE
        }
    }
}
