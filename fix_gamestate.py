import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

old_entities = """    val entities: List<Entity> = emptyList(),
    
    val modules: ModulesState = ModulesState(),"""
    
new_entities = """    val entities: List<Entity> = emptyList(),
    val pursuers: List<Pursuer> = emptyList(),
    
    val modules: ModulesState = ModulesState(),"""

code = code.replace(old_entities, new_entities)

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

print("GameState patched")
