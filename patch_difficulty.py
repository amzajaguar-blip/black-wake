import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

old_tutorial = """    val spawnClock: Float = 0f,
    val tutorialStep: Int = 0,
    
    val playerX: Float = 0f,"""
    
new_tutorial = """    val spawnClock: Float = 0f,
    val tutorialStep: Int = 0,
    val difficultyMultiplier: Float = 1.0f,
    
    val playerX: Float = 0f,"""

code = code.replace(old_tutorial, new_tutorial)

# Replace the AI update call
old_ai_update = """            if (p.ai == null) p.ai = PursuerAI(p.type, p.state)
            p.ai!!.update(dt, newDetection, distY, silent)"""

new_ai_update = """            if (p.ai == null) p.ai = PursuerAI(p.type, p.state)
            p.ai!!.update(dt, newDetection, distY, silent, state.difficultyMultiplier)"""

code = code.replace(old_ai_update, new_ai_update)

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

with open("app/src/main/java/com/blackwake/game/PursuerAI.kt", "r") as f:
    ai_code = f.read()

old_ai_func = """    fun update(
        dt: Float,
        detectionPercentage: Float,
        distanceY: Float,
        isPlayerSilent: Boolean
    ) {"""

new_ai_func = """    fun update(
        dt: Float,
        detectionPercentage: Float,
        distanceY: Float,
        isPlayerSilent: Boolean,
        difficultyMultiplier: Float = 1.0f
    ) {"""
ai_code = ai_code.replace(old_ai_func, new_ai_func)

# Replace thresholds
old_detect = """                // Transition from PATROL to PURSUIT based on Detection percentage
                if (detectionPercentage >= 0.7f) {"""
new_detect = """                // Transition from PATROL to PURSUIT based on Detection percentage
                val threshold = (0.7f / difficultyMultiplier).coerceIn(0.1f, 1.0f)
                if (detectionPercentage >= threshold) {"""
ai_code = ai_code.replace(old_detect, new_detect)

old_pursuit_search = """if (isPlayerSilent && kotlin.math.abs(distanceY) > 30f && detectionPercentage < 0.6f) {"""
new_pursuit_search = """val searchThreshold = (0.6f / difficultyMultiplier).coerceIn(0.1f, 1.0f)
                if (isPlayerSilent && kotlin.math.abs(distanceY) > 30f && detectionPercentage < searchThreshold) {"""
ai_code = ai_code.replace(old_pursuit_search, new_pursuit_search)

old_search_pursuit = """if (!isPlayerSilent || detectionPercentage > 0.6f) {"""
new_search_pursuit = """val pursuitThreshold = (0.6f / difficultyMultiplier).coerceIn(0.1f, 1.0f)
                if (!isPlayerSilent || detectionPercentage > pursuitThreshold) {"""
ai_code = ai_code.replace(old_search_pursuit, new_search_pursuit)

with open("app/src/main/java/com/blackwake/game/PursuerAI.kt", "w") as f:
    f.write(ai_code)

print("Difficulty patched in VM and AI")
