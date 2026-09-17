import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

old_spawn = """                val entity = Entity(
                    id = entityIdCounter++,
                    type = type,
                    lane = lane,
                    startX = 0f,
                    z = 100f,
                    value = value
                )"""

new_spawn = """                val eType = if (type == EntityType.ENEMY) {
                    val r = Math.random()
                    when {
                        r < 0.3 -> EnemyBoatType.PATROL
                        r < 0.6 -> EnemyBoatType.INTERCEPTOR
                        r < 0.8 -> EnemyBoatType.POLICE
                        r < 0.9 -> EnemyBoatType.HUNTER
                        r < 0.95 -> EnemyBoatType.ARMORED
                        else -> EnemyBoatType.ELITE
                    }
                } else EnemyBoatType.PATROL
                
                val entity = Entity(
                    id = entityIdCounter++,
                    type = type,
                    lane = lane,
                    startX = 0f,
                    z = 100f,
                    value = value,
                    enemyType = eType
                )"""

if old_spawn in code:
    code = code.replace(old_spawn, new_spawn)
    with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
        f.write(code)
    print("Spawn logic patched")
else:
    print("Could not find old spawn logic")
