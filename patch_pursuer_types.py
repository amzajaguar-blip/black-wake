import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

types_code = """enum class PursuerState {
    SPAWNING, SEARCH, DETECTED, PURSUIT, INTERCEPT, ATTACK, LOST_TARGET
}

data class Pursuer(
    val id: Int,
    val type: EnemyBoatType,
    var x: Float = 0f,
    var y: Float = -20f, // 0 is top of screen, 100 is bottom. Player is at ~85
    var velocityX: Float = 0f,
    var velocityY: Float = 0f,
    var state: PursuerState = PursuerState.SPAWNING,
    var roll: Float = 0f,
    var timer: Float = 0f,
    var health: Float = 100f
)

data class Entity("""

code = code.replace("data class Entity(", types_code)

state_addition = """    val comboTimer: Float = 0f,
    
    val entities: List<Entity> = emptyList(),
    val pursuers: List<Pursuer> = emptyList(),
    
    val isWon: Boolean = false,"""

code = code.replace("""    val comboTimer: Float = 0f,
    
    val entities: List<Entity> = emptyList(),
    
    val isWon: Boolean = false,""", state_addition)

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

print("Pursuer types patched")
