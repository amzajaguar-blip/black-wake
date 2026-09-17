import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

# Add throttle to GameState
state_old = """    val playerRoll: Float = 0f,
    val fovOffset: Float = 0f,"""
state_new = """    val playerRoll: Float = 0f,
    val throttle: Float = 0.5f,
    val fovOffset: Float = 0f,"""
code = code.replace(state_old, state_new)

# Add setThrottle method and remove setSilent / setBoost
methods_old = """    fun setBoost(held: Boolean) { isBoostHeld = held }
    fun setSilent(held: Boolean) { isSilentHeld = held }"""
methods_new = """    fun setThrottle(value: Float) { _uiState.update { it.copy(throttle = value.coerceIn(0f, 1f)) } }"""
code = code.replace(methods_old, methods_new)

# Replace boost/silent logic in update()
logic_old = """        val canBoost = state.fuel > 5f && state.drift <= 0f
        val boost = isBoostHeld && canBoost
        val silent = isSilentHeld && !boost

        val canSubmerge = state.oxygen > 0f && !boost
        val newSubmerged = isDiveHeld && canSubmerge"""
logic_new = """        val effectiveThrottle = if (state.fuel > 0f && state.drift <= 0f) state.throttle else 0f
        
        val canSubmerge = state.oxygen > 0f
        val newSubmerged = isDiveHeld && canSubmerge"""
code = code.replace(logic_old, logic_new)

fov_old = """        // FOV Update (Game Feel)
        val targetFov = if (boost) 1.2f else 1.0f"""
fov_new = """        // FOV Update (Game Feel)
        val targetFov = 0.8f + effectiveThrottle * 0.4f // Maps 0.0 -> 0.8, 0.5 -> 1.0, 1.0 -> 1.2"""
code = code.replace(fov_old, fov_new)

fuel_old = """        // Fuel & Detection update
        var newFuel = max(0f, state.fuel - dt * (1.0f + if (boost) 5.6f else 0f))
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
        val speedMultiplier = if (silent) 0.3f else if (!boost) 0.7f else 1.0f
        var stealthMultiplier = speedMultiplier * hidingMultiplier
        if (newSubmerged) {
            stealthMultiplier *= 0.1f // Very hard to detect when submerged
        }
        
        // Add ambient accumulation based on sector threat and if there are pursuers
        val ambientAccumulation = (0.015f * sector.threat) + (state.pursuers.size * 0.02f)
        val boostAccumulation = if (boost) 0.115f else 0f"""
fuel_new = """        // Fuel & Detection update
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
        val boostAccumulation = if (effectiveThrottle > 0.6f) (effectiveThrottle - 0.6f) * 0.3f else 0f"""
code = code.replace(fuel_old, fuel_new)

# Initial State Reset
reset_old = """                playerVelocityX = 0f,
                playerRoll = 0f,
                fovOffset = 0f,"""
reset_new = """                playerVelocityX = 0f,
                playerRoll = 0f,
                throttle = 0.5f,
                fovOffset = 0f,"""
code = code.replace(reset_old, reset_new)

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

