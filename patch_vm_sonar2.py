import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

old_sonar_update = """        if (newSonarActive) {
            newSonarRadius += dt * 350f
            if (newSonarRadius > 250f) {
                newSonarActive = false
                newSonarRadius = 0f
            }
        }"""
new_sonar_update = """        if (newSonarActive) {
            newSonarRadius += dt * 800f // Expansion speed
            if (newSonarRadius > 1200f) { // Cover the screen
                newSonarActive = false
                newSonarRadius = 0f
            }
        }"""
code = code.replace(old_sonar_update, new_sonar_update)

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

print("GameViewModel patched max sonar radius")
