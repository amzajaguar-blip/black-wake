import sys
import re

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

# Add to GameState
state_pattern = """    val tutorialStep: Int = 0,
    val difficultyMultiplier: Float = 1.0f,
    
    val playerX: Float = 0f,"""
state_replace = """    val tutorialStep: Int = 0,
    val difficultyMultiplier: Float = 1.0f,
    
    val sonarCharges: Int = 1,
    val sonarRegenTimer: Float = 0f,
    val sonarPingActive: Boolean = false,
    val sonarPingRadius: Float = 0f,
    
    val playerX: Float = 0f,"""
code = code.replace(state_pattern, state_replace)

# Add to startGame
start_pattern = """                tutorialStep = 0,
                playerX = 0f,"""
start_replace = """                tutorialStep = 0,
                sonarCharges = 1 + state.modules.radar,
                sonarRegenTimer = 0f,
                sonarPingActive = false,
                sonarPingRadius = 0f,
                playerX = 0f,"""
code = code.replace(start_pattern, start_replace)

# Add triggerSonar
trigger_func = """    fun triggerSonar() {
        _uiState.update { state ->
            if (state.mode == GameMode.RUNNING && state.sonarCharges > 0 && !state.sonarPingActive) {
                state.copy(
                    sonarCharges = state.sonarCharges - 1,
                    sonarPingActive = true,
                    sonarPingRadius = 0f
                )
            } else state
        }
    }

    fun setBoost"""
code = code.replace("    fun setBoost", trigger_func)

# Add to updateGame
update_pattern = """        val targetFov = if (boost) 1.2f else 1.0f
        val newFov = state.fovOffset + (targetFov - state.fovOffset) * dt * 5f"""
update_replace = """        val targetFov = if (boost) 1.2f else 1.0f
        val newFov = state.fovOffset + (targetFov - state.fovOffset) * dt * 5f
        
        var newSonarCharges = state.sonarCharges
        var newSonarRegen = state.sonarRegenTimer
        var newSonarActive = state.sonarPingActive
        var newSonarRadius = state.sonarPingRadius
        
        val maxSonarCharges = 1 + state.modules.radar
        if (newSonarCharges < maxSonarCharges) {
            newSonarRegen += dt
            val regenThreshold = 20f - (state.modules.radar * 3f)
            if (newSonarRegen >= regenThreshold) {
                newSonarCharges += 1
                newSonarRegen = 0f
            }
        }
        
        if (newSonarActive) {
            newSonarRadius += dt * 350f
            if (newSonarRadius > 250f) {
                newSonarActive = false
                newSonarRadius = 0f
            }
        }"""
code = code.replace(update_pattern, update_replace)

copy_pattern = """                drift = newDrift,
                invulnerable = newInvulnerable,"""
copy_replace = """                drift = newDrift,
                invulnerable = newInvulnerable,
                sonarCharges = newSonarCharges,
                sonarRegenTimer = newSonarRegen,
                sonarPingActive = newSonarActive,
                sonarPingRadius = newSonarRadius,"""
code = code.replace(copy_pattern, copy_replace)

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

print("GameViewModel patched with Sonar Logic")
