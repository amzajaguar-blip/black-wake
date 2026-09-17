import sys
import re

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

# I will find the random spawning line and replace it
old_line = "newEntities.add(Entity(entityIdCounter++, type, lane, xPos, 96f + (Math.random() * 18).toFloat()))"
new_line = """
            val eType = if (type == EntityType.ENEMY) {
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
            
            newEntities.add(Entity(
                id = entityIdCounter++, 
                type = type, 
                lane = lane, 
                startX = xPos, 
                z = 96f + (Math.random() * 18).toFloat(),
                enemyType = eType
            ))"""

if old_line in code:
    code = code.replace(old_line, new_line)
    with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
        f.write(code)
    print("Spawn patched")
else:
    print("Spawn line not found")
