import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

# Fix targetX in Pursuer
old_pursuer = """    var timer: Float = 0f,
    var health: Float = 100f
)"""
new_pursuer = """    var timer: Float = 0f,
    var health: Float = 100f,
    var targetX: Float = 0f
)"""
code = code.replace(old_pursuer, new_pursuer)

# Fix the var state.playerX mess
old_physics = """        var state.playerX = state.playerX + newVelocityX * dt
        if (state.playerX > 1.15f) {
            state.playerX = 1.15f
            if (newVelocityX > 0) newVelocityX = 0f
        } else if (state.playerX < -1.15f) {
            state.playerX = -1.15f
            if (newVelocityX < 0) newVelocityX = 0f
        }"""
        
new_physics = """        var finalPlayerX = state.playerX + newVelocityX * dt
        if (finalPlayerX > 1.15f) {
            finalPlayerX = 1.15f
            if (newVelocityX > 0) newVelocityX = 0f
        } else if (finalPlayerX < -1.15f) {
            finalPlayerX = -1.15f
            if (newVelocityX < 0) newVelocityX = 0f
        }"""
        
code = code.replace(old_physics, new_physics)

# Also fix the copy() at the end
code = code.replace("playerX = state.playerX", "playerX = finalPlayerX")

# Fix newVelX ? Oh, the regex didn't replace that incorrectly because it was just one occurrence in Pursuer code. Let's make sure there are no other state.playerX errors.
code = code.replace("var finalPlayerVelocityX = state.playerVelocityX", "var newVelocityX = state.playerVelocityX") # just in case

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

print("Vars 2 fixed")
