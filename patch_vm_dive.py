import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

# Add to GameState
state_pattern = """    val sonarPingActive: Boolean = false,
    val sonarPingRadius: Float = 0f,"""
state_replace = """    val sonarPingActive: Boolean = false,
    val sonarPingRadius: Float = 0f,
    
    val isSubmerged: Boolean = false,
    val oxygen: Float = 100f,"""
code = code.replace(state_pattern, state_replace)

# Add isDiveHeld and setDive
dive_func = """    private var isBoostHeld = false
    private var isSilentHeld = false
    private var isDiveHeld = false

    fun setDive(held: Boolean) { isDiveHeld = held }"""
code = code.replace("""    private var isBoostHeld = false
    private var isSilentHeld = false""", dive_func)

# Add logic to updateGame
update_pattern = """        val silent = isSilentHeld && !boost

        // FOV Update (Game Feel)"""
update_replace = """        val silent = isSilentHeld && !boost
        
        val canSubmerge = state.oxygen > 0f && !boost
        val newSubmerged = isDiveHeld && canSubmerge
        
        var newOxygen = state.oxygen
        if (newSubmerged) {
            newOxygen = kotlin.math.max(0f, newOxygen - dt * 25f) // Depletes in 4 seconds
        } else {
            newOxygen = kotlin.math.min(100f, newOxygen + dt * 15f) // Regenerates in ~6.6 seconds
        }

        // FOV Update (Game Feel)"""
code = code.replace(update_pattern, update_replace)

stealth_pattern = """        val stealthMultiplier = speedMultiplier * hidingMultiplier"""
stealth_replace = """        var stealthMultiplier = speedMultiplier * hidingMultiplier
        if (newSubmerged) {
            stealthMultiplier *= 0.1f // Very hard to detect when submerged
        }"""
code = code.replace(stealth_pattern, stealth_replace)

decay_pattern = """        val decayRate = if (silent) {
            0.16f + state.modules.stealth * 0.04f
        } else if (isBehindWreck && !boost) {
            0.12f + state.modules.stealth * 0.02f // Fast decay if hiding and not boosting
        } else {"""
decay_replace = """        val decayRate = if (newSubmerged) {
            0.30f + state.modules.stealth * 0.05f // Extreme decay when submerged
        } else if (silent) {
            0.16f + state.modules.stealth * 0.04f
        } else if (isBehindWreck && !boost) {
            0.12f + state.modules.stealth * 0.02f // Fast decay if hiding and not boosting
        } else {"""
code = code.replace(decay_pattern, decay_replace)

# Copy to new state
copy_pattern = """                sonarPingActive = newSonarActive,
                sonarPingRadius = newSonarRadius,"""
copy_replace = """                sonarPingActive = newSonarActive,
                sonarPingRadius = newSonarRadius,
                isSubmerged = newSubmerged,
                oxygen = newOxygen,"""
code = code.replace(copy_pattern, copy_replace)

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

print("GameViewModel patched with Dive Logic")
