package com.blackwake.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs
import kotlin.math.pow

enum class GameMode {
    MENU, BRIEFING, RUNNING, PAUSED, GARAGE, DEBRIEF
}

enum class EntityType {
    INTEL, FUEL, WRECK, MINE, ENEMY, FORK
}

data class Entity(
    val id: Int,
    val type: EntityType,
    val lane: Int, // 0, 1, 2 (logical, mapping to X: -1, 0, 1)
    val startX: Float,
    var z: Float,
    val value: Int = 0,
    val label: String? = null,
    var resolved: Boolean = false,
    var nearMissed: Boolean = false
)

data class GameState(
    val mode: GameMode = GameMode.MENU,
    val currentChapterIndex: Int = 0,
    val unlockedChapterCount: Int = 1,
    val sectorIndex: Int = 0,
    val sectorElapsed: Float = 0f,
    val runElapsed: Float = 0f,
    val spawnClock: Float = 0f,
    val tutorialStep: Int = 0,
    
    val playerX: Float = 0f, // Continuous from -1.0 to 1.0
    val playerVelocityX: Float = 0f,
    val playerRoll: Float = 0f,
    val fovOffset: Float = 0f,
    
    val hull: Float = 100f,
    val fuel: Float = 100f,
    val intel: Int = 0,
    val detection: Float = 0.0f,
    val peakDetection: Float = 0.0f,
    val drift: Float = 0f,
    val invulnerable: Float = 0f,
    val pursuitCooldown: Float = 0f,
    
    val comboCounter: Int = 0,
    val comboMultiplier: Int = 1,
    
    val cameraShake: Float = 0f,
    val screenFlash: Float = 0f,
    val flashColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Transparent,
    
    val forkSpawned: Boolean = false,
    val forkNotice: String? = null,
    val forkExpire: Float = 0f,
    val lastDeltaTime: Float = 0f,
    
    val entities: List<Entity> = emptyList(),
    
    val modules: ModulesState = ModulesState(),
    val intelBank: Int = 0,
    val bestIntel: Int = 0,
    
    val messageFlash: String? = null,
    val messageColor: String = "cyan",
    
    val isWon: Boolean = false,
    val debriefReason: String = ""
)

class GameViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GameState())
    val uiState: StateFlow<GameState> = _uiState.asStateFlow()
    
    private var isLooping = false
    private var entityIdCounter = 0
    private var lastTime = System.currentTimeMillis()
    
    var targetPlayerX: Float = 0f // Touch drag target
    private var isDragging = false
    
    private var isBoostHeld = false
    private var isSilentHeld = false
    
    fun setDragTarget(xNorm: Float) {
        // xNorm is 0.0 (left) to 1.0 (right)
        targetPlayerX = (xNorm - 0.5f) * 2f // Map to -1.0 to 1.0
        isDragging = true
    }
    fun clearDrag() {
        isDragging = false
    }

    private fun startLoop() {
        if (isLooping) return
        isLooping = true
        lastTime = System.currentTimeMillis()
        viewModelScope.launch {
            while (isLooping) {
                val now = System.currentTimeMillis()
                val delta = min((now - lastTime) / 1000f, 0.1f) // cap at 100ms for safety
                lastTime = now
                
                updateGame(delta)
                delay(16)
            }
        }
    }

    private fun stopLoop() {
        isLooping = false
    }

    fun startGame() {
        val chapter = CHAPTERS[_uiState.value.currentChapterIndex]
        val sector = chapter.sectors.first()
        _uiState.update { state ->
            state.copy(
                mode = GameMode.RUNNING,
                hull = 100f + (state.modules.hull * 7),
                fuel = 100f,
                intel = 0,
                detection = 0.0f,
                peakDetection = 0.0f,
                sectorIndex = 0,
                sectorElapsed = 0f,
                runElapsed = 0f,
                tutorialStep = 0,
                playerX = 0f,
                playerVelocityX = 0f,
                playerRoll = 0f,
                fovOffset = 0f,
                comboCounter = 0,
                comboMultiplier = 1,
                cameraShake = 0f,
                screenFlash = 0f,
                spawnClock = 0.42f,
                entities = emptyList(),
                forkSpawned = false,
                forkNotice = null,
                drift = 0f,
                invulnerable = 0f,
                pursuitCooldown = 0f,
                messageFlash = "CAP. ${chapter.code} // ${sector.title}",
                messageColor = "cyan"
            )
        }
        startLoop()
    }
    
    fun setBoost(held: Boolean) { isBoostHeld = held }
    fun setSilent(held: Boolean) { isSilentHeld = held }

    fun togglePause() {
        _uiState.update { state ->
            if (state.mode == GameMode.RUNNING) {
                stopLoop()
                state.copy(mode = GameMode.PAUSED)
            } else if (state.mode == GameMode.PAUSED) {
                startLoop()
                state.copy(mode = GameMode.RUNNING)
            } else state
        }
    }

    fun openMenu() { stopLoop(); _uiState.update { it.copy(mode = GameMode.MENU) } }
    fun openBriefing(chapterIndex: Int) { _uiState.update { it.copy(mode = GameMode.BRIEFING, currentChapterIndex = chapterIndex) } }
    fun openGarage() { _uiState.update { it.copy(mode = GameMode.GARAGE) } }
    
    fun buyModule(id: String) {
        _uiState.update { state ->
            val modValue = when(id) {
                "engine" -> state.modules.engine
                "hull" -> state.modules.hull
                "tank" -> state.modules.tank
                "radar" -> state.modules.radar
                "stealth" -> state.modules.stealth
                else -> 0
            }
            if (modValue >= 3) return@update state
            val cost = MODULES.find { it.id == id }?.costs?.get(modValue) ?: return@update state
            if (state.intelBank >= cost) {
                val newModules = state.modules.copy().apply {
                    when(id) {
                        "engine" -> engine++
                        "hull" -> hull++
                        "tank" -> tank++
                        "radar" -> radar++
                        "stealth" -> stealth++
                    }
                }
                state.copy(intelBank = state.intelBank - cost, modules = newModules)
            } else {
                state.copy(messageFlash = "INTEL INSUFFICIENTE", messageColor = "red")
            }
        }
    }

    private fun updateGame(dt: Float) {
        val state = _uiState.value
        if (state.mode != GameMode.RUNNING) return

        val chapter = CHAPTERS[state.currentChapterIndex]
        val sector = chapter.sectors[min(state.sectorIndex, chapter.sectors.size - 1)]

        var newInvulnerable = max(0f, state.invulnerable - dt)
        var newCameraShake = max(0f, state.cameraShake - dt * 3f)
        var newScreenFlash = max(0f, state.screenFlash - dt * 4f)
        
        val canBoost = state.fuel > 5f && state.drift <= 0f
        val boost = isBoostHeld && canBoost
        val silent = isSilentHeld && !boost

        // FOV Update (Game Feel)
        val targetFov = if (boost) 1.2f else 1.0f
        val newFov = state.fovOffset + (targetFov - state.fovOffset) * dt * 5f

        // Fuel & Detection update
        var newFuel = max(0f, state.fuel - dt * (1.0f + if (boost) 5.6f else 0f))
        var newDrift = state.drift
        var newDetection = state.detection

        if (boost) newDetection = min(1f, newDetection + dt * 0.115f)
        else if (silent) newDetection = max(0.0f, newDetection - dt * (0.16f + state.modules.stealth * 0.04f))
        else newDetection = max(0.0f, newDetection - dt * 0.02f)

        if (state.fuel <= 0f) { newDrift = max(0f, newDrift - dt) }
        if (state.fuel == 0f && state.drift <= 0f && newFuel == 0f) {
            if (state.drift == 0f && state.fuel > 0f) {
                newDrift = 3f
                flashMessage("CARBURANTE ESAURITO // DERIVA", "red")
            }
        }
        
        var newHull = state.hull
        if (state.fuel == 0f && newDrift <= 0f && newInvulnerable <= 0f) {
            newHull = max(0f, newHull - 2.3f * dt)
            if (newHull <= 0f) { finishRun(false, "Il mare entra nello scafo."); return }
        }

        var newPeak = max(state.peakDetection, newDetection)
        var newPursuit = state.pursuitCooldown - dt
        var newEntities = state.entities.toMutableList()
        
        if (newDetection >= 0.91f && newPursuit <= 0f) {
            newPursuit = 8f
            flashMessage("CONTATTO PIENO // ONDA D'INSEGUIMENTO", "red")
            newCameraShake = 1f
            for (lane in 0..2) {
                val xPos = (lane - 1).toFloat()
                newEntities.add(Entity(entityIdCounter++, EntityType.ENEMY, lane, xPos, 66f + lane * 4f))
            }
        }

        // Sector progress
        var newRunElapsed = state.runElapsed + dt
        var newSectorElapsed = state.sectorElapsed + dt
        var newSpawnClock = state.spawnClock - dt
        var newForkSpawned = state.forkSpawned
        var newForkNotice = state.forkNotice
        var newTutorialStep = state.tutorialStep

        if (state.currentChapterIndex == 0 && state.sectorIndex == 0 && newTutorialStep < 5) {
            // Scripted invisible tutorial
            if (newSectorElapsed > 1.5f && newTutorialStep == 0) {
                newEntities.add(Entity(entityIdCounter++, EntityType.FUEL, 1, 0f, 100f))
                newTutorialStep = 1
            } else if (newSectorElapsed > 4.5f && newTutorialStep == 1) {
                newEntities.add(Entity(entityIdCounter++, EntityType.INTEL, 2, 1f, 100f))
                newTutorialStep = 2
            } else if (newSectorElapsed > 7.5f && newTutorialStep == 2) {
                newEntities.add(Entity(entityIdCounter++, EntityType.WRECK, 1, 0f, 100f))
                newTutorialStep = 3
            } else if (newSectorElapsed > 11.5f && newTutorialStep == 3) {
                // Require boost to get both
                newEntities.add(Entity(entityIdCounter++, EntityType.FUEL, 0, -1f, 100f))
                newEntities.add(Entity(entityIdCounter++, EntityType.MINE, 1, 0f, 100f))
                newEntities.add(Entity(entityIdCounter++, EntityType.INTEL, 2, 1f, 100f))
                newTutorialStep = 4
            } else if (newSectorElapsed > 15.5f && newTutorialStep == 4) {
                // Introduce enemies and silent mode
                newEntities.add(Entity(entityIdCounter++, EntityType.ENEMY, 0, -1f, 100f))
                newEntities.add(Entity(entityIdCounter++, EntityType.ENEMY, 2, 1f, 100f))
                newTutorialStep = 5
                newSpawnClock = 3.0f // Resume normal spawns
            }
        } else if (newSpawnClock <= 0f) {
            val roll = Math.random().toFloat()
            val threatMulti = if (newDetection > 0.65f) 1.5f else 1f
            val type = when {
                roll < sector.intelBias * 0.52f -> EntityType.INTEL
                roll < sector.intelBias * 0.52f + 0.12f -> EntityType.FUEL
                roll < sector.intelBias * 0.52f + sector.threat * 0.32f * threatMulti -> if (Math.random() > 0.5) EntityType.ENEMY else EntityType.MINE
                else -> EntityType.WRECK
            }
            val lane = (Math.random() * 3).toInt()
            val xPos = (lane - 1).toFloat()
            newEntities.add(Entity(entityIdCounter++, type, lane, xPos, 96f + (Math.random() * 18).toFloat()))
            newSpawnClock = max(0.45f, 1.15f - sector.density * 0.48f + (Math.random() * 0.35).toFloat())
        }

        var newForkExpire = state.forkExpire
        if (sector.fork && !newForkSpawned && newSectorElapsed > 3.2f) {
            newForkSpawned = true
            newForkNotice = "FORCA IN ARRIVO // 01 SICURO · 02 INTEL · 03 VOLATILE"
            newForkExpire = newRunElapsed + 6.2f
            newEntities.add(Entity(entityIdCounter++, EntityType.FORK, 0, -1f, 84f, 0, "01 // SICURO"))
            newEntities.add(Entity(entityIdCounter++, EntityType.FORK, 1, 0f, 84f, 1, "02 // INTEL"))
            newEntities.add(Entity(entityIdCounter++, EntityType.FORK, 2, 1f, 84f, 2, "03 // VOLATILE"))
        }

        var newSectorIndex = state.sectorIndex
        if (newSectorElapsed >= sector.duration) {
            if (sector.extract) {
                val won = state.intel >= kotlin.math.ceil(chapter.intelGoal * 0.7).toInt() && newHull > 0
                finishRun(won, if (won) "I dati lasciano il mare. Per questa notte, abbastanza." else "La trasmissione è incompleta. Il segnale si spegne.")
                return
            }
            newSectorIndex += 1
            newSectorElapsed = 0f
            newForkSpawned = false
            flashMessage("${chapter.sectors[newSectorIndex].title} // ${chapter.sectors[newSectorIndex].tag}", "cyan")
        }

        // PLAYER MOVEMENT PHYSICS (Magnetic Lanes & Inertia)
        // Find nearest lane center (-1f, 0f, 1f)
        val nearestLane = kotlin.math.round(state.playerX).coerceIn(-1f, 1f)
        
        var inputForce = 0f
        if (isDragging) {
            val diff = targetPlayerX - state.playerX
            inputForce = diff * 15f
        }
        
        // Magnetic force to the nearest lane, weakens if dragging strongly or boosting
        val magnetStrength = if (boost) 3f else if (isDragging) 6f else 12f
        val magnetForce = (nearestLane - state.playerX) * magnetStrength
        
        val totalAcceleration = inputForce + magnetForce - state.playerVelocityX * 8f // Damping
        var newVelocityX = state.playerVelocityX + totalAcceleration * dt
        
        // Coyote / Micro-correction responsiveness
        if (isDragging && kotlin.math.sign(targetPlayerX - state.playerX) != kotlin.math.sign(newVelocityX)) {
            newVelocityX *= 0.5f // Quick turnaround
        }
        
        var newPlayerX = state.playerX + newVelocityX * dt
        if (newPlayerX > 1.15f) {
            newPlayerX = 1.15f
            if (newVelocityX > 0) newVelocityX = 0f
        } else if (newPlayerX < -1.15f) {
            newPlayerX = -1.15f
            if (newVelocityX < 0) newVelocityX = 0f
        }
        
        // Boat Roll (Tilt) based on velocity
        val targetRoll = newVelocityX * -15f // Tilt in opposite direction of travel
        val newPlayerRoll = state.playerRoll + (targetRoll - state.playerRoll) * dt * 10f

        // Entities Step & Collision
        val speed = (18f + sector.threat * 4f + (if (newDetection > 0.65f) 3.5f else 0f)) * newFov
        var newIntel = state.intel
        var newComboCounter = state.comboCounter
        var newComboMult = state.comboMultiplier
        
        if (!silent && newComboMult > 1) {
            newComboCounter = 0
            newComboMult = 1
        }
        
        val iter = newEntities.iterator()
        while (iter.hasNext()) {
            val e = iter.next()
            e.z -= dt * speed
            
            // Collision Detection
            val zHit = e.z < 2f && e.z > -4f
            val xHit = abs(e.startX - newPlayerX) < 0.45f
            
            // Near miss detection
            if (!e.resolved && !e.nearMissed && e.z < 0f && e.z > -5f && abs(e.startX - newPlayerX) in 0.45f..0.7f && (e.type == EntityType.MINE || e.type == EntityType.WRECK || e.type == EntityType.ENEMY)) {
                // Near miss bonus!
                newFuel = min(100f, newFuel + 2f)
                newDetection = max(0f, newDetection - 0.02f)
                flashMessage("NEAR MISS", "cyan")
                // Only grant once per entity
                e.nearMissed = true
            }

            if (!e.resolved && zHit && xHit) {
                e.resolved = true
                when(e.type) {
                    EntityType.INTEL -> {
                        if (silent) {
                            newComboCounter++
                        } else {
                            newComboCounter = 0
                            newComboMult = 1
                        }
                        if (newComboCounter >= 3) {
                            newComboMult = min(4, newComboMult + 1)
                            newComboCounter = 0
                            flashMessage("SILENT COMBO x$newComboMult", "cyan")
                        } else {
                            if (silent && newComboMult > 1) {
                                flashMessage("INTEL RECUPERATA +$newComboMult", "cyan")
                            } else {
                                flashMessage("INTEL RECUPERATA", "cyan")
                            }
                        }
                        newIntel += (1 * newComboMult)
                        newScreenFlash = 0.5f
                        _uiState.update { it.copy(flashColor = androidx.compose.ui.graphics.Color(0x332BE7E0)) }
                    }
                    EntityType.FUEL -> {
                        newFuel = min(100f, newFuel + 20f + state.modules.tank * 4f)
                        flashMessage("CARBURANTE RECUPERATO", "amber")
                        newScreenFlash = 0.5f
                        _uiState.update { it.copy(flashColor = androidx.compose.ui.graphics.Color(0x33FFB45F)) }
                    }
                    EntityType.WRECK, EntityType.MINE -> {
                        if (newInvulnerable <= 0f) {
                            newComboCounter = 0
                            newComboMult = 1
                            val dmg = if(e.type == EntityType.MINE) 25f else 20f
                            newHull = max(0f, newHull - (dmg - state.modules.hull * 3f))
                            newInvulnerable = 0.86f
                            
                            // Impact Physics (Knockback)
                            val impactDir = if (newPlayerX > e.startX) 1f else -1f
                            newVelocityX = impactDir * 15f
                            newCameraShake = 1.0f
                            newScreenFlash = 1.0f
                            _uiState.update { it.copy(flashColor = androidx.compose.ui.graphics.Color(0x55EF464B)) }
                            
                            flashMessage(if(e.type == EntityType.MINE) "IMPATTO // Mina." else "IMPATTO // Relitto.", "red")
                        }
                    }
                    EntityType.ENEMY -> {
                        newDetection = min(1f, newDetection + 0.18f)
                        if (newInvulnerable <= 0f) {
                            newComboCounter = 0
                            newComboMult = 1
                            newHull = max(0f, newHull - (14f - state.modules.hull * 2f))
                            newInvulnerable = 0.86f
                            
                            val impactDir = if (newPlayerX > e.startX) 1f else -1f
                            newVelocityX = impactDir * 12f
                            newCameraShake = 0.8f
                            newScreenFlash = 1.0f
                            _uiState.update { it.copy(flashColor = androidx.compose.ui.graphics.Color(0x55EF464B)) }
                            
                            flashMessage("IMPATTO // BLACK TIDE", "red")
                        }
                    }
                    EntityType.FORK -> {
                        newEntities.filter { it.type == EntityType.FORK }.forEach { it.resolved = true }
                        newForkNotice = null
                        if (e.value == 0) {
                            newDetection = max(0.0f, newDetection - 0.16f)
                            flashMessage("ROTTA SICURA", "cyan")
                        } else if (e.value == 1) {
                            newIntel += 3
                            newDetection = min(1f, newDetection + 0.1f)
                            flashMessage("ROUTE INTEL // +3 DATI", "amber")
                        } else {
                            newFuel = min(100f, newFuel + 28f)
                            newDetection = min(1f, newDetection + 0.26f)
                            newCameraShake = 0.5f
                            flashMessage("ROUTE VOLATILE // TI HANNO VISTO", "red")
                        }
                    }
                }
            }
            if (e.z < -12f || e.resolved) {
                iter.remove()
            }
        }
        
        if (newForkNotice != null && newRunElapsed > newForkExpire) {
            newForkNotice = null
        }
        
        if (newHull <= 0f) {
            finishRun(false, "Lo scafo non regge. La prova resta sotto la marea.")
            return
        }

        _uiState.update { s ->
            s.copy(
                fuel = newFuel,
                drift = newDrift,
                detection = newDetection,
                hull = newHull,
                invulnerable = newInvulnerable,
                peakDetection = newPeak,
                pursuitCooldown = newPursuit,
                runElapsed = newRunElapsed,
                sectorElapsed = newSectorElapsed,
                spawnClock = newSpawnClock,
                tutorialStep = newTutorialStep,
                playerX = newPlayerX,
                playerVelocityX = newVelocityX,
                playerRoll = newPlayerRoll,
                fovOffset = newFov,
                comboCounter = newComboCounter,
                comboMultiplier = newComboMult,
                cameraShake = newCameraShake,
                screenFlash = newScreenFlash,
                forkSpawned = newForkSpawned,
                forkNotice = newForkNotice,
                forkExpire = newForkExpire,
                lastDeltaTime = dt,
                sectorIndex = newSectorIndex,
                entities = newEntities.sortedByDescending { it.z },
                intel = newIntel
            )
        }
    }

    private fun finishRun(won: Boolean, reason: String) {
        stopLoop()
        val banked = if (won) _uiState.value.intel else (_uiState.value.intel * 0.4).toInt()
        val nextUnlocked = won && _uiState.value.currentChapterIndex + 1 == _uiState.value.unlockedChapterCount && _uiState.value.currentChapterIndex < CHAPTERS.size - 1
        val newUnlockedCount = if (won) min(CHAPTERS.size, max(_uiState.value.unlockedChapterCount, _uiState.value.currentChapterIndex + 2)) else _uiState.value.unlockedChapterCount
        
        _uiState.update { it.copy(
            mode = GameMode.DEBRIEF,
            isWon = won,
            debriefReason = reason,
            intelBank = it.intelBank + banked,
            bestIntel = max(it.bestIntel, it.intel),
            unlockedChapterCount = newUnlockedCount
        ) }
    }

    private fun flashMessage(msg: String, color: String) {
        _uiState.update { it.copy(messageFlash = msg, messageColor = color) }
        viewModelScope.launch {
            delay(1500)
            _uiState.update { if (it.messageFlash == msg) it.copy(messageFlash = null) else it }
        }
    }
}
