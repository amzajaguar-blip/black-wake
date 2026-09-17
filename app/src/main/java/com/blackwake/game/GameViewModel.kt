package com.blackwake.game

import androidx.lifecycle.AndroidViewModel
import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import android.os.VibrationEffect
import android.os.Vibrator
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
    MENU, BRIEFING, RUNNING, PAUSED, GARAGE, DEBRIEF, PATCHING
}

enum class EntityType {
    INTEL, FUEL, WRECK, MINE, ENEMY, FORK
}

enum class PlayerBoatType {
    SPEEDBOAT, RACING, PATROL, SMUGGLER, STEALTH, ARMORED
}

enum class EnemyBoatType {
    PATROL, INTERCEPTOR, POLICE, HUNTER, ARMORED, ELITE
}

enum class PursuerState {
    PATROL, SEARCH, DETECTED, PURSUIT, INTERCEPT, ATTACK, LOST_TARGET, SEARCH_AGAIN
}

data class Pursuer(
    val id: Int,
    val type: EnemyBoatType,
    var x: Float = 0f,
    var y: Float = -20f, // 0 is top of screen, 100 is bottom. Player is at ~85
    var velocityX: Float = 0f,
    var velocityY: Float = 0f,
    var state: PursuerState = PursuerState.PATROL,
    var ai: PursuerAI? = null,
    var roll: Float = 0f,
    var timer: Float = 0f,
    var health: Float = 100f,
    var targetX: Float = 0f
)

data class Entity(
    val id: Int,
    val type: EntityType,
    val lane: Int, // 0, 1, 2 (logical, mapping to X: -1, 0, 1)
    val startX: Float,
    var z: Float,
    val value: Int = 0,
    val label: String? = null,
    var resolved: Boolean = false,
    var nearMissed: Boolean = false,
    val enemyType: EnemyBoatType = EnemyBoatType.PATROL
)



enum class CompartmentState { NORMAL, SEALED, BREACHED }
data class Compartment(val id: String, val name: String, val state: CompartmentState = CompartmentState.NORMAL)

data class LeakNode(
    val id: Int,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val fixed: Boolean = false
)

data class TerminalMessage(val text: String, val color: String, val timestamp: Float)

data class GameState(
    val mode: GameMode = GameMode.MENU,
    val playerBoatType: PlayerBoatType = PlayerBoatType.SPEEDBOAT,
    val currentChapterIndex: Int = 0,
    val unlockedChapterCount: Int = 1,
    val sectorIndex: Int = 0,
    val sectorElapsed: Float = 0f,
    val runElapsed: Float = 0f,
    val spawnClock: Float = 0f,
    val tutorialStep: Int = 0,
    val difficultyMultiplier: Float = 1.0f,
    
    val sonarCharges: Int = 1,
    val sonarRegenTimer: Float = 0f,
    val sonarPingActive: Boolean = false,
    val sonarPingRadius: Float = 0f,
    
    val isSubmerged: Boolean = false,
    val oxygen: Float = 100f,
    val showTacticalMap: Boolean = false,
    val showEventLog: Boolean = false,
    val showDamageControl: Boolean = false,
    val isAudioMuted: Boolean = false,
    val isEmergencyPowerActive: Boolean = false,
    val battery: Float = 100f,
    val flareCharges: Int = 3,
    val seabedDepth: Float = 50f,
    val submergedTimer: Float = 0f,
    val playerDepth: Float = 0f,
    val pressureEventActive: Boolean = false,
    val flareActiveTimer: Float = 0f,
    val batteryHistory: List<Float> = listOf(),
    val batteryDrainRate: Float = 0f,
    val compartments: List<Compartment> = listOf(
        Compartment("BOW", "PRUA"),
        Compartment("MID", "CENTRO NAVE"),
        Compartment("AFT", "POPPA"),
        Compartment("ENG", "MOTORI")
    ),
    
    val playerX: Float = 0f, // Continuous from -1.0 to 1.0
    val playerVelocityX: Float = 0f,
    val playerRoll: Float = 0f,
    val throttle: Float = 0.5f,
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
    
    val hasTriggeredPatching: Boolean = false,
    val leakNodes: List<LeakNode> = emptyList(),
    
    val forkSpawned: Boolean = false,
    val forkNotice: String? = null,
    val forkExpire: Float = 0f,
    val proximityPingTimer: Float = 0f,
    val lastDeltaTime: Float = 0f,
    
    val entities: List<Entity> = emptyList(),
    val pursuers: List<Pursuer> = emptyList(),
    
    val modules: ModulesState = ModulesState(),
    val intelBank: Int = 0,
    val bestIntel: Int = 0,
    
    val messageFlash: String? = null,
    val messageColor: String = "cyan",
    val terminalFeed: List<TerminalMessage> = emptyList(),
    val isGlobalEmergency: Boolean = false,
    
    val isWon: Boolean = false,
    val debriefReason: String = ""
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    init {
        SynthAudioEngine.start()
        // Load on startup if we were in the middle of a run. But wait, mode defaults to MENU.
        // We'd have to restore the whole game state for a true session resume, but we'll at least restore pursuers.
        loadPursuers()
    }
    private val prefs = getApplication<Application>().getSharedPreferences("PursuerStatePrefs", Context.MODE_PRIVATE)

    private fun savePursuers() {
        val pursuers = _uiState.value.pursuers
        val csvList = pursuers.map { p ->
            PursuerSaveState(
                id = p.id,
                type = p.type.name,
                x = p.x,
                y = p.y,
                velocityX = p.velocityX,
                velocityY = p.velocityY,
                state = p.state.name,
                roll = p.roll,
                timer = p.timer,
                health = p.health,
                targetX = p.targetX
            ).toCsv()
        }
        val encoded = csvList.joinToString(";")
        prefs.edit().putString("saved_pursuers", encoded).apply()
    }

    fun loadPursuers() {
        val encoded = prefs.getString("saved_pursuers", "") ?: ""
        if (encoded.isEmpty()) return
        
        val csvList = encoded.split(";")
        val loadedPursuers = csvList.mapNotNull { csv ->
            PursuerSaveState.fromCsv(csv)?.let { saveState ->
                Pursuer(
                    id = saveState.id,
                    type = EnemyBoatType.valueOf(saveState.type),
                    x = saveState.x,
                    y = saveState.y,
                    velocityX = saveState.velocityX,
                    velocityY = saveState.velocityY,
                    state = PursuerState.valueOf(saveState.state),
                    ai = PursuerAI(EnemyBoatType.valueOf(saveState.type), PursuerState.valueOf(saveState.state)).apply { timer = saveState.timer },
                    roll = saveState.roll,
                    timer = saveState.timer,
                    health = saveState.health,
                    targetX = saveState.targetX
                )
            }
        }
        
        _uiState.update { it.copy(pursuers = loadedPursuers) }
    }
    
    fun clearPursuers() {
        prefs.edit().remove("saved_pursuers").apply()
    }

    private val _uiState = MutableStateFlow(GameState())
    val uiState: StateFlow<GameState> = _uiState.asStateFlow()
    
    
    private var entityIdCounter = 0
    private var lastTime = System.currentTimeMillis()
    
    var targetPlayerX: Float = 0f // Touch drag target
    private var isDragging = false
    
    private var isBoostHeld = false
    private var isSilentHeld = false
    private var isDiveHeld = false

    fun setDive(held: Boolean) { isDiveHeld = held }
    
    fun setDragTarget(xNorm: Float) {
        // xNorm is 0.0 (left) to 1.0 (right)
        targetPlayerX = (xNorm - 0.5f) * 2f // Map to -1.0 to 1.0
        isDragging = true
    }
    fun clearDrag() {
        isDragging = false
    }

    fun startGame() {
        val chapter = CHAPTERS[_uiState.value.currentChapterIndex]
        val sector = chapter.sectors.first()
                clearPursuers()
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
                sonarCharges = 1 + state.modules.radar,
                sonarRegenTimer = 0f,
                sonarPingActive = false,
                sonarPingRadius = 0f,
                playerX = 0f,
                playerVelocityX = 0f,
                playerRoll = 0f,
                throttle = 0.5f,
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
                pursuers = emptyList(),
                messageFlash = "CAP. ${chapter.code} // ${sector.title}",
                messageColor = "cyan"
            )
        }
        
    }
    
    fun triggerSonar() {
        _uiState.update { state ->
            if (state.mode == GameMode.RUNNING && state.sonarCharges > 0 && !state.sonarPingActive && !state.isEmergencyPowerActive) {
                state.copy(
                    sonarCharges = state.sonarCharges - 1,
                    sonarPingActive = true,
                    sonarPingRadius = 0f
                )
            } else state
        }
    }

    fun setThrottle(value: Float) { _uiState.update { it.copy(throttle = value.coerceIn(0f, 1f)) } }


    override fun onCleared() {
        SynthAudioEngine.stop()
        super.onCleared()
        if (_uiState.value.mode == GameMode.RUNNING || _uiState.value.mode == GameMode.PAUSED) {
            savePursuers()
        }
    }
    fun togglePause() {
        _uiState.update { state ->
            if (state.mode == GameMode.RUNNING) {
                
                state.copy(mode = GameMode.PAUSED)
            } else if (state.mode == GameMode.PAUSED) {
                
                state.copy(mode = GameMode.RUNNING)
            } else state
        }
    }

    fun openMenu() { ; _uiState.update { it.copy(mode = GameMode.MENU) } }
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



    fun toggleDamageControl() {
        _uiState.update { it.copy(showDamageControl = !it.showDamageControl) }
    }
    
    fun toggleAudioMute() {
        _uiState.update { state -> 
            val newMuted = !state.isAudioMuted
            SynthAudioEngine.isMuted = newMuted
            state.copy(isAudioMuted = newMuted) 
        }
    }
    
        fun toggleGlobalEmergency() {
        _uiState.update { state -> 
            val newEmergency = !state.isGlobalEmergency
            SynthAudioEngine.hasSiren = newEmergency
            state.copy(isGlobalEmergency = newEmergency) 
        }
    }

    fun toggleEmergencyPower() {
        _uiState.update { 
            if (!it.isEmergencyPowerActive && it.battery <= 0f) {
                it // Cannot turn on if battery is 0
            } else {
                it.copy(isEmergencyPowerActive = !it.isEmergencyPowerActive)
            }
        }
    }
    
    fun launchFlare() {
        _uiState.update { state ->
            if (state.flareCharges > 0 && state.flareActiveTimer <= 0f && !state.isSubmerged) {
                SynthAudioEngine.playExplosion() // Sound effect for flare
                state.copy(
                    flareCharges = state.flareCharges - 1,
                    flareActiveTimer = 5f, // 5 seconds of visibility
                    screenFlash = 0.5f,
                    flashColor = androidx.compose.ui.graphics.Color(0xAAFFFFAA)
                )
            } else state
        }
    }
    
    fun toggleCompartmentSeal(id: String) {
        _uiState.update { state ->
            val updated = state.compartments.map { comp ->
                if (comp.id == id) {
                    when (comp.state) {
                        CompartmentState.NORMAL -> comp.copy(state = CompartmentState.SEALED)
                        CompartmentState.BREACHED -> comp.copy(state = CompartmentState.SEALED)
                        CompartmentState.SEALED -> comp.copy(state = CompartmentState.NORMAL) // Or leave sealed if breached? Let's allow unsealing to normal if they want, but if it was breached, unsealing it would re-breach. Let's keep it simple: unsealing goes to NORMAL.
                    }
                } else comp
            }
            state.copy(compartments = updated)
        }
    }

    
    fun toggleEventLog() {
        _uiState.update { it.copy(showEventLog = !it.showEventLog) }
    }

    fun toggleTacticalMap() {
        _uiState.update { it.copy(showTacticalMap = !it.showTacticalMap) }
    }

    fun patchNode(id: Int) {
        _uiState.update { state ->
            if (state.mode != GameMode.PATCHING) return@update state
            val newNodes = state.leakNodes.map { if (it.id == id) it.copy(fixed = true) else it }
            SynthAudioEngine.playTone(1200f, 0.1f)
            
            var newMode = state.mode
            var newHull = state.hull
            if (newNodes.all { it.fixed }) {
                newMode = GameMode.RUNNING
                newHull = kotlin.math.min(100f, state.hull + 20f)
                SynthAudioEngine.playTone(880f, 0.4f)
            }
            
            state.copy(leakNodes = newNodes, mode = newMode, hull = newHull)
        }
    }
    
    fun updatePatching(dt: Float) {
        _uiState.update { state ->
            if (state.mode != GameMode.PATCHING) return@update state
            
            val newNodes = state.leakNodes.map { node ->
                if (node.fixed) {
                     node
                } else {
                     var nx = node.x + node.vx * dt
                     var ny = node.y + node.vy * dt
                     var nvx = node.vx
                     var nvy = node.vy
                     if (nx < 0.1f || nx > 0.9f) nvx = -nvx
                     if (ny < 0.1f || ny > 0.9f) nvy = -nvy
                     nx = nx.coerceIn(0.1f, 0.9f)
                     ny = ny.coerceIn(0.1f, 0.9f)
                     node.copy(x = nx, y = ny, vx = nvx, vy = nvy)
                }
            }
            
            state.copy(leakNodes = newNodes)
        }
    }

    fun updateGame(dt: Float) {
        val state = _uiState.value
        if (state.mode != GameMode.RUNNING) return

        val chapter = CHAPTERS[state.currentChapterIndex]
        val sector = chapter.sectors[min(state.sectorIndex, chapter.sectors.size - 1)]

        var newInvulnerable = max(0f, state.invulnerable - dt)
        var newCameraShake = max(0f, state.cameraShake - dt * 3f)
        var newScreenFlash = max(0f, state.screenFlash - dt * 4f)
        
        val effectiveThrottle = if (state.fuel > 0f && state.drift <= 0f) state.throttle else 0f
        val boost = effectiveThrottle > 0.8f
        val silent = effectiveThrottle < 0.2f
        val canSubmerge = state.oxygen > 0f
        val wasO2Critical = state.oxygen < 20f
        val newSubmerged = isDiveHeld && canSubmerge
        val newFlareActiveTimer = kotlin.math.max(0f, state.flareActiveTimer - dt)
        
        var newOxygen = state.oxygen
        var newBattery = state.battery
        var newEmergencyPowerActive = state.isEmergencyPowerActive
        val drainRate = if (newEmergencyPowerActive) 10f else -2.5f
        if (newEmergencyPowerActive) {
            newBattery = kotlin.math.max(0f, newBattery - dt * drainRate) // deplete battery faster (10 seconds)
            if (newBattery == 0f) {
                newEmergencyPowerActive = false
                newCameraShake = 0.5f
                triggerVibration(longArrayOf(0, 100, 50, 100))
                flashMessage("OVERRIDE FALLITO // BATTERIA ESAURITA", "red")
            }
        } else {
            newBattery = kotlin.math.min(100f, newBattery - dt * drainRate) // recharge in 40 seconds
        }
        
        // Update history periodically
        val newHistory = if (state.runElapsed % 0.5f < dt) {
            (state.batteryHistory + newBattery).takeLast(60)
        } else {
            state.batteryHistory
        }

        val o2DepleteRate = if (newEmergencyPowerActive) 15f else 25f
        val o2RegenRate = if (newEmergencyPowerActive) 45f else 15f
        
        var newSubmergedTimer = state.submergedTimer
        var newPressureEventActive = false
        if (newSubmerged) {
            newOxygen = kotlin.math.max(0f, newOxygen - dt * o2DepleteRate)
            newSubmergedTimer += dt
        } else {
            newOxygen = kotlin.math.min(100f, newOxygen + dt * o2RegenRate)
            newSubmergedTimer = kotlin.math.max(0f, newSubmergedTimer - dt * 2f)
        }

        // FOV Update (Game Feel)
        val targetFov = 0.8f + effectiveThrottle * 0.4f
        val newFov = state.fovOffset + (targetFov - state.fovOffset) * dt * 5f
        
        // Calculate seabed depth based on runElapsed to simulate terrain
        val baseDepth = 80f
        val depthVariation = kotlin.math.sin(state.runElapsed * 0.4f) * 30f + kotlin.math.cos(state.runElapsed * 0.15f) * 20f
        val newSeabedDepth = kotlin.math.max(10f, baseDepth + depthVariation)
        
        val targetDepth = if (newSubmerged) kotlin.math.min(newSeabedDepth, 15f + newSubmergedTimer * 12f) else 0f
        val newPlayerDepth = state.playerDepth + (targetDepth - state.playerDepth) * dt * 1.5f
        
        val safeDepthThreshold = 55f
        if (newPlayerDepth > safeDepthThreshold) {
            newPressureEventActive = true
            // Dynamic stress-induced vibration based on how deep past safe limit
            val depthStress = (newPlayerDepth - safeDepthThreshold) / 25f
            newCameraShake = kotlin.math.max(newCameraShake, kotlin.math.min(1f, depthStress * 0.4f))
        }
        
        var newSonarCharges = state.sonarCharges
        var newSonarRegen = state.sonarRegenTimer
        var newSonarActive = state.sonarPingActive
        var newSonarRadius = state.sonarPingRadius
        
        val maxSonarCharges = 1 + state.modules.radar
        if (newSonarCharges < maxSonarCharges) {
            val regenBoost = if (newEmergencyPowerActive) 6f else 1f
            newSonarRegen += dt * regenBoost
            val regenThreshold = 20f - (state.modules.radar * 3f)
            if (newSonarRegen >= regenThreshold) {
                newSonarCharges += 1
                newSonarRegen = 0f
            }
        }
        
        // Auto-ping if emergency power is active and we have charges
        if (newEmergencyPowerActive && newSonarCharges > 0 && !newSonarActive) {
            newSonarCharges -= 1
            newSonarActive = true
            newSonarRadius = 0f
        }
        
        var newObstacleDetected = false
        if (newSonarActive) {
            val oldRadius = newSonarRadius
            newSonarRadius += dt * 800f // Expansion speed
            
            // Sonar detect new entities
            for (e in state.entities) {
                if (e.z > 0f && e.type != EntityType.FUEL && e.type != EntityType.INTEL) {
                    // Check if radius just passed this entity
                    val dist = e.z * 10f // approx distance mapping
                    if (oldRadius < dist && newSonarRadius >= dist) {
                        newObstacleDetected = true
                    }
                }
            }
            if (newObstacleDetected) {
                triggerVibration(longArrayOf(0, 30)) // Short pulse for obstacle
            }

            if (newSonarRadius > 1200f) { // Cover the screen
                newSonarActive = false
                newSonarRadius = 0f
            }
        }

        // Fuel & Detection update
        val fuelUsageRate = 0.5f + effectiveThrottle * effectiveThrottle * 6.1f
        var newFuel = max(0f, state.fuel - dt * fuelUsageRate)
        var newDrift = state.drift
        var newDetection = state.detection
        
        // Stealth Mechanic Check: Hiding behind obstacles
        var isBehindWreck = false
        for (e in state.entities) {
            // If the obstacle is a WRECK, ahead of us but close, and we are in the same lane (x position match)
            if (e.type == EntityType.WRECK && e.z > 0f && e.z < 25f && kotlin.math.abs(e.startX - state.playerX) < 0.6f) {
                isBehindWreck = true
                break
            }
        }
        
        val hidingMultiplier = if (isBehindWreck) 0.2f else 1.0f
        val speedMultiplier = 0.3f + effectiveThrottle * 0.7f
        var stealthMultiplier = speedMultiplier * hidingMultiplier
        if (newSubmerged) {
            stealthMultiplier *= 0.1f // Very hard to detect when submerged
        }

        // Add ambient accumulation based on sector threat and if there are pursuers
        val ambientAccumulation = (0.015f * sector.threat) + (state.pursuers.size * 0.02f)
        val boostAccumulation = if (effectiveThrottle > 0.6f) (effectiveThrottle - 0.6f) * 0.3f else 0f
        
        val totalAccumulation = (ambientAccumulation + boostAccumulation) * stealthMultiplier
        newDetection = min(1f, newDetection + dt * totalAccumulation)

        // Apply decay
        val decayRate = if (newSubmerged) {
            0.30f + state.modules.stealth * 0.05f // Extreme decay when submerged
        } else if (silent) {
            0.16f + state.modules.stealth * 0.04f
        } else if (isBehindWreck && !boost) {
            0.12f + state.modules.stealth * 0.02f // Fast decay if hiding and not boosting
        } else {
            0.02f
        }
        newDetection = max(0.0f, newDetection - dt * decayRate)

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
        if (state.oxygen == 0f && newSubmerged) {
            newHull = max(0f, newHull - 15f * dt) // Severe damage when drowning
            if (newHull <= 0f) { finishRun(false, "Scafo imploso per mancanza d'ossigeno."); return }
            newScreenFlash = 0.3f
            _uiState.update { it.copy(flashColor = androidx.compose.ui.graphics.Color(0x55EF464B)) }
        }
        if (newPressureEventActive) {
            newHull = max(0f, newHull - 10f * dt) // Pressure event damage
            if (newHull <= 0f) { finishRun(false, "Scafo collassato sotto pressione estrema."); return }
            if (state.runElapsed % 1f < dt) SynthAudioEngine.playTone(200f, 0.2f) // Deep creaking sound
        }
        
        var newCompartments = state.compartments
        val breachedCount = newCompartments.count { it.state == CompartmentState.BREACHED }
        if (breachedCount > 0) {
            newHull = max(0f, newHull - (breachedCount * 2.5f) * dt)
            if (state.runElapsed % 2f < dt) SynthAudioEngine.playTone(400f, 0.1f) // Warning beep for flooding
        }
        

        var newPeak = max(state.peakDetection, newDetection)
        var newPursuit = state.pursuitCooldown - dt
        var newEntities = state.entities.toMutableList()
        var newPursuers = state.pursuers.toMutableList()
        
        // --- PURSUER AI UPDATE ---
        val pursuerIter = newPursuers.iterator()
        while(pursuerIter.hasNext()) {
            val p = pursuerIter.next()
            p.timer += dt
            
            // Basic movement forward
            val speedLimit = when (p.type) {
                EnemyBoatType.PATROL -> 45f
                EnemyBoatType.INTERCEPTOR -> 60f
                EnemyBoatType.POLICE -> 55f
                EnemyBoatType.HUNTER -> 65f
                EnemyBoatType.ARMORED -> 40f
                EnemyBoatType.ELITE -> 75f
            }
            
            val predictionFactor = when (p.type) {
                EnemyBoatType.PATROL -> 0.2f
                EnemyBoatType.INTERCEPTOR -> 0.8f
                EnemyBoatType.POLICE -> 0.5f
                EnemyBoatType.ELITE -> 1.2f
                else -> 0.3f
            }
            
            // Distance check & state transitions
            val distY = p.y - 85f // Player is at ~85 (bottom of screen)
            
            if (p.ai == null) p.ai = PursuerAI(p.type, p.state)
            p.ai!!.update(dt, newDetection, distY, silent, state.difficultyMultiplier)
            p.state = p.ai!!.state
            p.timer = p.ai!!.timer
            
            if (p.state == PursuerState.PATROL || p.state == PursuerState.DETECTED) {
                p.velocityY = speedLimit
            } else if (p.state == PursuerState.LOST_TARGET) {
                p.velocityY = -20f // fall behind
                if (p.y < -50f) pursuerIter.remove()
            } else {
                // Movement logic
                // Y logic: approach player Y=70..85
                val targetY = if (p.state == PursuerState.INTERCEPT) 82f else 70f
                val diffY = targetY - p.y
                p.velocityY = p.velocityY + (diffY * 0.5f - p.velocityY) * dt * 2f
                
                // X logic: predictive tracking
                var predictedX = state.playerX + state.playerVelocityX * predictionFactor
                if (p.state == PursuerState.SEARCH) predictedX += kotlin.math.sin(p.timer * 2f) * 0.3f
                
                // Avoid other pursuers
                var separationForce = 0f
                for (other in newPursuers) {
                    if (other.id != p.id) {
                        val dx = p.x - other.x
                        val dy = p.y - other.y
                        val sqDist = dx*dx + dy*dy
                        if (sqDist < 25f && sqDist > 0.1f) {
                            separationForce += (1f / dx) * 0.1f
                        }
                    }
                }
                
                p.targetX = (predictedX + separationForce).coerceIn(-1f, 1f)
                val diffX = p.targetX - p.x
                
                // Acceleration mapping based on type
                val turnRate = when (p.type) {
                    EnemyBoatType.HUNTER -> 5f
                    EnemyBoatType.ARMORED -> 1.5f
                    EnemyBoatType.ELITE -> 6f
                    else -> 3f
                }
                
                p.velocityX = p.velocityX + (diffX * turnRate - p.velocityX) * dt * 5f
                p.x += p.velocityX * dt
                p.y += p.velocityY * dt * 0.3f // apparent speed matching
                p.roll = p.velocityX * 15f
            }
            
            // Collision with player
            if (p.state != PursuerState.LOST_TARGET && p.state != PursuerState.PATROL && p.state != PursuerState.DETECTED) {
                val dx = kotlin.math.abs(p.x - state.playerX)
                val dy = kotlin.math.abs(p.y - 85f)
                if (dx < 0.2f && dy < 5f && newInvulnerable <= 0f) {
                    newHull = kotlin.math.max(0f, newHull - 15f)
                    newInvulnerable = 2f
                    val normalComps = newCompartments.filter { it.state == CompartmentState.NORMAL }
                    if (normalComps.isNotEmpty()) {
                        val toBreach = normalComps.random()
                        newCompartments = newCompartments.map { if (it.id == toBreach.id) it.copy(state = CompartmentState.BREACHED) else it }
                        flashMessage("ALLAGAMENTO: ${toBreach.name}", "red")
                    }
                    newCameraShake = 0.5f
                    SynthAudioEngine.playExplosion()
                    flashMessage("PURSUER COLLISION", "red")
                }
            }
        }
        
        // Spawn Pursuers dynamically
        if (newDetection >= 0.7f && newPursuers.isEmpty() && newPursuit <= 0f) {
            val type = if (newDetection > 0.9f) EnemyBoatType.ELITE else EnemyBoatType.PATROL
            newPursuers.add(Pursuer(entityIdCounter++, type, x = state.playerX + (Math.random().toFloat() - 0.5f), y = 120f)) // Spawn from bottom (behind)
            flashMessage("INSEGUITORE IN AVVICINAMENTO", "amber")
            SynthAudioEngine.playTone(400f, 0.5f)
            newPursuit = 5f
        }
        
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
            
            val eType = if (type == EntityType.ENEMY) {
                val r = Math.random()
                when {
                    r < 0.3 -> EnemyBoatType.PATROL
                    r < 0.6 -> EnemyBoatType.INTERCEPTOR
                    r < 0.8 -> EnemyBoatType.POLICE
                    r < 0.9 -> EnemyBoatType.HUNTER
                    r < 0.95 -> EnemyBoatType.ARMORED
                    else -> EnemyBoatType.ELITE
                }
            } else EnemyBoatType.PATROL
            
            newEntities.add(Entity(
                id = entityIdCounter++, 
                type = type, 
                lane = lane, 
                startX = xPos, 
                z = 96f + (Math.random() * 18).toFloat(),
                enemyType = eType
            ))
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
        
        var finalPlayerX = state.playerX + newVelocityX * dt
        if (finalPlayerX > 1.15f) {
            finalPlayerX = 1.15f
            if (newVelocityX > 0) newVelocityX = 0f
        } else if (finalPlayerX < -1.15f) {
            finalPlayerX = -1.15f
            if (newVelocityX < 0) newVelocityX = 0f
        }
        
        // Boat Roll (Tilt) based on velocity
        val targetRoll = newVelocityX * -15f // Tilt in opposite direction of travel
        val newPlayerRoll = state.playerRoll + (targetRoll - state.playerRoll) * dt * 10f

        // Entities Step & Collision
        val speed = (18f + sector.threat * 4f + (if (newDetection > 0.65f) 3.5f else 0f)) * newFov * (if (newEmergencyPowerActive) 1.5f else 1.0f)
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
            val xHit = abs(e.startX - state.playerX) < 0.45f
            
            // Near miss detection
            if (!e.resolved && !e.nearMissed && e.z < 0f && e.z > -5f && abs(e.startX - state.playerX) in 0.45f..0.7f && (e.type == EntityType.MINE || e.type == EntityType.WRECK || e.type == EntityType.ENEMY)) {
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
                            val actualDmg = (dmg - state.modules.hull * 3f)
                            newHull = max(0f, newHull - actualDmg)
                            newInvulnerable = 0.86f
                            triggerVibration(longArrayOf(0, 100, 50, 150, 50, 200)) // Heavy damage shake
                            if (actualDmg > 10f) {
                                val normalComps = newCompartments.filter { it.state == CompartmentState.NORMAL }
                                if (normalComps.isNotEmpty()) {
                                    val toBreach = normalComps.random()
                                    newCompartments = newCompartments.map { if (it.id == toBreach.id) it.copy(state = CompartmentState.BREACHED) else it }
                                    flashMessage("FALLA: ${toBreach.name}", "red")
                                }
                            }
                            
                            // Impact Physics (Knockback)
                            val impactDir = if (state.playerX > e.startX) 1f else -1f
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
                            val actualDmg = (14f - state.modules.hull * 2f)
                            newHull = max(0f, newHull - actualDmg)
                            newInvulnerable = 0.86f
                            triggerVibration(longArrayOf(0, 80, 40, 120)) // Medium enemy hit
                            if (actualDmg > 10f && java.util.Random().nextBoolean()) {
                                val normalComps = newCompartments.filter { it.state == CompartmentState.NORMAL }
                                if (normalComps.isNotEmpty()) {
                                    val toBreach = normalComps.random()
                                    newCompartments = newCompartments.map { if (it.id == toBreach.id) it.copy(state = CompartmentState.BREACHED) else it }
                                    flashMessage("ALLAGAMENTO: ${toBreach.name}", "red")
                                }
                            }
                            
                            val impactDir = if (state.playerX > e.startX) 1f else -1f
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
        
        // --- Proximity Audio Ping (Sonar/Situational Awareness) ---
        var minObjectDist = 100f
        for (e in newEntities) {
            if (!e.resolved && (e.type == EntityType.MINE || e.type == EntityType.WRECK || e.type == EntityType.ENEMY)) {
                if (e.z > 0f && e.z < minObjectDist) {
                    minObjectDist = e.z
                }
            }
        }
        for (p in newPursuers) {
            if (p.state != PursuerState.LOST_TARGET) {
                val dy = kotlin.math.abs(p.y - 85f)
                if (dy < minObjectDist) {
                    minObjectDist = dy
                }
            }
        }
        
        var newProximityPingTimer = state.proximityPingTimer
        if (minObjectDist < 60f) {
            val pingInterval = kotlin.math.max(0.15f, (minObjectDist / 60f) * 1.5f)
            newProximityPingTimer += dt
            if (newProximityPingTimer >= pingInterval) {
                newProximityPingTimer = 0f
                val freq = 600f + (1f - (minObjectDist / 60f)) * 800f
                SynthAudioEngine.playTone(freq, 0.08f)
            }
        } else {
            newProximityPingTimer = 0f
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
                compartments = newCompartments,
                invulnerable = newInvulnerable,
                peakDetection = newPeak,
                pursuitCooldown = newPursuit,
                runElapsed = newRunElapsed,
                sectorElapsed = newSectorElapsed,
                spawnClock = newSpawnClock,
                tutorialStep = newTutorialStep,
                playerX = finalPlayerX,
                playerVelocityX = newVelocityX,
                playerRoll = newPlayerRoll,
                fovOffset = newFov,
                comboCounter = newComboCounter,
                comboMultiplier = newComboMult,
                cameraShake = newCameraShake,
                screenFlash = newScreenFlash,
                flareActiveTimer = newFlareActiveTimer,
                isEmergencyPowerActive = newEmergencyPowerActive,
                battery = newBattery,
                batteryHistory = newHistory,
                batteryDrainRate = drainRate,
                seabedDepth = newSeabedDepth,
                submergedTimer = newSubmergedTimer,
                playerDepth = newPlayerDepth,
                pressureEventActive = newPressureEventActive,
                forkSpawned = newForkSpawned,
                forkNotice = newForkNotice,
                forkExpire = newForkExpire,
                lastDeltaTime = dt,
                sectorIndex = newSectorIndex,
                entities = newEntities.sortedByDescending { it.z },
                pursuers = newPursuers.toList(),
                intel = newIntel,
                proximityPingTimer = newProximityPingTimer
            )
        }
    }

    private fun finishRun(won: Boolean, reason: String) {
        
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
        _uiState.update { 
            it.copy(
                messageFlash = msg, 
                messageColor = color,
                terminalFeed = (listOf(TerminalMessage(msg, color, it.runElapsed)) + it.terminalFeed).take(200)
            ) 
        }
        viewModelScope.launch {
            delay(1500)
            _uiState.update { if (it.messageFlash == msg) it.copy(messageFlash = null) else it }
        }
    }
    
    private fun triggerVibration(pattern: LongArray) {
        try {
            val vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            // Ignore if vibration is not available
        }
    }
    fun setPlayerBoat(b: PlayerBoatType) {
        _uiState.update { it.copy(playerBoatType = b) }
    }
}
