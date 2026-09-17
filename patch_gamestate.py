import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

state_old = """    val isSubmerged: Boolean = false
    val oxygen: Float = 100f,"""
state_new = """    val isSubmerged: Boolean = false
    val oxygen: Float = 100f,
    val showTacticalMap: Boolean = false,"""
code = code.replace(state_old, state_new)

# if it was without comma
state_old2 = """    val isSubmerged: Boolean = false
    val oxygen: Float = 100f"""
state_new2 = """    val isSubmerged: Boolean = false
    val oxygen: Float = 100f,
    val showTacticalMap: Boolean = false"""
if state_old2 in code:
    code = code.replace(state_old2, state_new2)

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)
