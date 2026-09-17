import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

timer_old = """        val newSubmerged = isDiveHeld && canSubmerge"""
timer_new = """        val newSubmerged = isDiveHeld && canSubmerge
        val newFlareActiveTimer = kotlin.math.max(0f, state.flareActiveTimer - dt)"""

code = code.replace(timer_old, timer_new)

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)
