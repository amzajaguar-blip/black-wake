import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

old_enum = """enum class PursuerState {
    SPAWNING, SEARCH, DETECTED, PURSUIT, INTERCEPT, ATTACK, LOST_TARGET
}"""
new_enum = """enum class PursuerState {
    PATROL, SEARCH, DETECTED, PURSUIT, INTERCEPT, ATTACK, LOST_TARGET, SEARCH_AGAIN
}"""
code = code.replace(old_enum, new_enum)

old_pursuer = """    var velocityY: Float = 0f,
    var state: PursuerState = PursuerState.SPAWNING,"""
new_pursuer = """    var velocityY: Float = 0f,
    var state: PursuerState = PursuerState.PATROL,
    var ai: PursuerAI? = null,"""
code = code.replace(old_pursuer, new_pursuer)

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

print("Enum Patched")
