import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

# 1. Update GameState
state_old = """    val showDamageControl: Boolean = false,
    val isAudioMuted: Boolean = false,"""
state_new = """    val showDamageControl: Boolean = false,
    val isAudioMuted: Boolean = false,
    val isEmergencyPowerActive: Boolean = false,"""
code = code.replace(state_old, state_new)

# 2. Add toggle method
methods_old = """    fun toggleAudioMute() {
        _uiState.update { state -> 
            val newMuted = !state.isAudioMuted
            SynthAudioEngine.isMuted = newMuted
            state.copy(isAudioMuted = newMuted) 
        }
    }"""
methods_new = """    fun toggleAudioMute() {
        _uiState.update { state -> 
            val newMuted = !state.isAudioMuted
            SynthAudioEngine.isMuted = newMuted
            state.copy(isAudioMuted = newMuted) 
        }
    }
    
    fun toggleEmergencyPower() {
        _uiState.update { it.copy(isEmergencyPowerActive = !it.isEmergencyPowerActive) }
    }"""
code = code.replace(methods_old, methods_new)

# 3. Update triggerSonar
trigger_old = """    fun triggerSonar() {
        _uiState.update { state ->
            if (state.mode == GameMode.RUNNING && state.sonarCharges > 0 && !state.sonarPingActive) {"""
trigger_new = """    fun triggerSonar() {
        _uiState.update { state ->
            if (state.mode == GameMode.RUNNING && state.sonarCharges > 0 && !state.sonarPingActive && !state.isEmergencyPowerActive) {"""
code = code.replace(trigger_old, trigger_new)

# 4. Update Oxygen Logic
o2_old = """        var newOxygen = state.oxygen
        if (newSubmerged) {
            newOxygen = kotlin.math.max(0f, newOxygen - dt * 25f) // Depletes in 4 seconds
        } else {
            newOxygen = kotlin.math.min(100f, newOxygen + dt * 15f) // Regenerates in ~6.6 seconds
        }"""
o2_new = """        var newOxygen = state.oxygen
        val o2DepleteRate = if (state.isEmergencyPowerActive) 15f else 25f
        val o2RegenRate = if (state.isEmergencyPowerActive) 45f else 15f
        if (newSubmerged) {
            newOxygen = kotlin.math.max(0f, newOxygen - dt * o2DepleteRate)
        } else {
            newOxygen = kotlin.math.min(100f, newOxygen + dt * o2RegenRate)
        }"""
code = code.replace(o2_old, o2_new)

# 5. Update Sonar Logic
sonar_old = """        val maxSonarCharges = 1 + state.modules.radar
        if (newSonarCharges < maxSonarCharges) {
            newSonarRegen += dt
            val regenThreshold = 20f - (state.modules.radar * 3f)"""
sonar_new = """        val maxSonarCharges = 1 + state.modules.radar
        if (newSonarCharges < maxSonarCharges && !state.isEmergencyPowerActive) {
            newSonarRegen += dt
            val regenThreshold = 20f - (state.modules.radar * 3f)"""
code = code.replace(sonar_old, sonar_new)

# Also update the returned state copy
state_copy_old = """                screenFlash = newScreenFlash,
                flareActiveTimer = newFlareActiveTimer,"""
state_copy_new = """                screenFlash = newScreenFlash,
                flareActiveTimer = newFlareActiveTimer,"""
# Nothing needed if we don't modify emergency power state inside `update(dt)`.

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

