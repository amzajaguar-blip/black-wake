import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

old_copy = """                entities = newEntities.sortedByDescending { it.z },
                intel = newIntel"""
                
new_copy = """                entities = newEntities.sortedByDescending { it.z },
                pursuers = newPursuers.toList(),
                intel = newIntel"""

code = code.replace(old_copy, new_copy)

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

print("Vars 3 fixed")
