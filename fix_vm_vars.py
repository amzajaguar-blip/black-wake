import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

code = code.replace("newPlayerX", "state.playerX")
code = code.replace("newVelX", "state.playerVelocityX")

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

print("Vars fixed")
