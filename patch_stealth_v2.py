import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

old_detection_logic = "        var newDetection = state.detection\n        if (boost) newDetection = min(1f, newDetection + dt * 0.115f)\n        else if (silent) newDetection = max(0.0f, newDetection - dt * (0.16f + state.modules.stealth * 0.04f))\n        else newDetection = max(0.0f, newDetection - dt * 0.02f)"

new_detection_logic = """        var newDetection = state.detection
        
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
        val stealthMultiplier = speedMultiplier * hidingMultiplier

        // Add ambient accumulation based on sector threat and if there are pursuers
        val ambientAccumulation = (0.015f * sector.threat) + (state.pursuers.size * 0.02f)
        val boostAccumulation = if (boost) 0.115f else 0f
        
        val totalAccumulation = (ambientAccumulation + boostAccumulation) * stealthMultiplier
        newDetection = min(1f, newDetection + dt * totalAccumulation)

        // Apply decay
        val decayRate = if (silent) {
            0.16f + state.modules.stealth * 0.04f
        } else if (isBehindWreck && !boost) {
            0.12f + state.modules.stealth * 0.02f // Fast decay if hiding and not boosting
        } else {
            0.02f
        }
        newDetection = max(0.0f, newDetection - dt * decayRate)"""

if old_detection_logic in code:
    code = code.replace(old_detection_logic, new_detection_logic)
    with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
        f.write(code)
    print("Patched successfully")
else:
    print("Could not find the target string")
