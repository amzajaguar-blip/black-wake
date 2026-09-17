import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

old_logic = """            // Distance check & state transitions
            val distY = p.y - 85f // Player is at ~85 (bottom of screen)
            
            if (p.state == PursuerState.SPAWNING) {
                p.velocityY = speedLimit
                if (p.timer > 1.5f) p.state = PursuerState.PURSUIT
            } else if (p.state == PursuerState.PURSUIT || p.state == PursuerState.INTERCEPT) {
                // If silent and far, might lose target
                if (silent && kotlin.math.abs(distY) > 30f && newDetection < 0.6f) {
                    p.state = PursuerState.SEARCH
                    p.timer = 0f
                } else {
                    if (kotlin.math.abs(distY) < 15f && p.type == EnemyBoatType.INTERCEPTOR) {
                        p.state = PursuerState.INTERCEPT
                    } else {
                        p.state = PursuerState.PURSUIT
                    }
                }
            } else if (p.state == PursuerState.SEARCH) {
                if (p.timer > 3f) {
                    p.state = PursuerState.LOST_TARGET
                }
                if (!silent || newDetection > 0.6f) p.state = PursuerState.PURSUIT
            }
            
            if (p.state == PursuerState.LOST_TARGET) {"""

new_logic = """            // Distance check & state transitions
            val distY = p.y - 85f // Player is at ~85 (bottom of screen)
            
            if (p.ai == null) p.ai = PursuerAI(p.type, p.state)
            p.ai!!.update(dt, newDetection, distY, silent)
            p.state = p.ai!!.state
            p.timer = p.ai!!.timer
            
            if (p.state == PursuerState.PATROL || p.state == PursuerState.DETECTED) {
                p.velocityY = speedLimit
            } else if (p.state == PursuerState.LOST_TARGET) {"""

code = code.replace(old_logic, new_logic)
code = code.replace("p.state != PursuerState.SPAWNING", "p.state != PursuerState.PATROL && p.state != PursuerState.DETECTED")

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

print("ViewModel Logic Patched")
