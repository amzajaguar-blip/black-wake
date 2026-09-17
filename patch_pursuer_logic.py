import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

# Add pursuers update logic around newEntities logic
old_entities = """        var newEntities = state.entities.toMutableList()
        
        if (newDetection >= 0.91f && newPursuit <= 0f) {"""

new_entities = """        var newEntities = state.entities.toMutableList()
        var newPursuers = state.pursuers.toMutableList()
        
        // --- PURSUER AI UPDATE ---
        val pursuerIter = newPursuers.iterator()
        while(pursuerIter.hasNext()) {
            val p = pursuerIter.next()
            p.timer += dt
            
            // Basic movement forward
            val speedLimit = when (p.type) {
                EnemyBoatType.PATROL -> 45f
                EnemyBoatType.INTERCEPTOR -> 60f
                EnemyBoatType.POLICE -> 55f
                EnemyBoatType.HUNTER -> 65f
                EnemyBoatType.ARMORED -> 40f
                EnemyBoatType.ELITE -> 75f
            }
            
            val predictionFactor = when (p.type) {
                EnemyBoatType.PATROL -> 0.2f
                EnemyBoatType.INTERCEPTOR -> 0.8f
                EnemyBoatType.POLICE -> 0.5f
                EnemyBoatType.ELITE -> 1.2f
                else -> 0.3f
            }
            
            // Distance check & state transitions
            val distY = p.y - 85f // Player is at ~85 (bottom of screen)
            
            if (p.state == PursuerState.SPAWNING) {
                p.velocityY = speedLimit
                if (p.timer > 1.5f) p.state = PursuerState.PURSUIT
            } else if (p.state == PursuerState.PURSUIT || p.state == PursuerState.INTERCEPT) {
                // If silent and far, might lose target
                if (silent && kotlin.math.abs(distY) > 30f && newDetection < 0.6f) {
                    p.state = PursuerState.SEARCH
                    p.timer = 0f
                } else {
                    if (kotlin.math.abs(distY) < 15f && p.type == EnemyBoatType.INTERCEPTOR) {
                        p.state = PursuerState.INTERCEPT
                    } else {
                        p.state = PursuerState.PURSUIT
                    }
                }
            } else if (p.state == PursuerState.SEARCH) {
                if (p.timer > 3f) {
                    p.state = PursuerState.LOST_TARGET
                }
                if (!silent || newDetection > 0.6f) p.state = PursuerState.PURSUIT
            }
            
            if (p.state == PursuerState.LOST_TARGET) {
                p.velocityY = -20f // fall behind
                if (p.y < -50f) pursuerIter.remove()
            } else {
                // Movement logic
                // Y logic: approach player Y=70..85
                val targetY = if (p.state == PursuerState.INTERCEPT) 82f else 70f
                val diffY = targetY - p.y
                p.velocityY = p.velocityY + (diffY * 0.5f - p.velocityY) * dt * 2f
                
                // X logic: predictive tracking
                var predictedX = newPlayerX + newVelX * predictionFactor
                if (p.state == PursuerState.SEARCH) predictedX += kotlin.math.sin(p.timer * 2f) * 0.3f
                
                // Avoid other pursuers
                var separationForce = 0f
                for (other in newPursuers) {
                    if (other.id != p.id) {
                        val dx = p.x - other.x
                        val dy = p.y - other.y
                        val sqDist = dx*dx + dy*dy
                        if (sqDist < 25f && sqDist > 0.1f) {
                            separationForce += (1f / dx) * 0.1f
                        }
                    }
                }
                
                p.targetX = (predictedX + separationForce).coerceIn(-1f, 1f)
                val diffX = p.targetX - p.x
                
                // Acceleration mapping based on type
                val turnRate = when (p.type) {
                    EnemyBoatType.HUNTER -> 5f
                    EnemyBoatType.ARMORED -> 1.5f
                    EnemyBoatType.ELITE -> 6f
                    else -> 3f
                }
                
                p.velocityX = p.velocityX + (diffX * turnRate - p.velocityX) * dt * 5f
                p.x += p.velocityX * dt
                p.y += p.velocityY * dt * 0.3f // apparent speed matching
                p.roll = p.velocityX * 15f
            }
            
            // Collision with player
            if (p.state != PursuerState.LOST_TARGET && p.state != PursuerState.SPAWNING) {
                val dx = kotlin.math.abs(p.x - newPlayerX)
                val dy = kotlin.math.abs(p.y - 85f)
                if (dx < 0.2f && dy < 5f && newInvulnerable <= 0f) {
                    newHull = kotlin.math.max(0f, newHull - 15f)
                    newInvulnerable = 2f
                    newCameraShake = 0.5f
                    SynthAudioEngine.playExplosion()
                    flashMessage("PURSUER COLLISION", "red")
                }
            }
        }
        
        // Spawn Pursuers dynamically
        if (newDetection >= 0.7f && newPursuers.isEmpty() && newPursuit <= 0f) {
            val type = if (newDetection > 0.9f) EnemyBoatType.ELITE else EnemyBoatType.PATROL
            newPursuers.add(Pursuer(entityIdCounter++, type, x = newPlayerX + (Math.random().toFloat() - 0.5f), y = 120f)) // Spawn from bottom (behind)
            flashMessage("INSEGUITORE IN AVVICINAMENTO", "amber")
            SynthAudioEngine.playTone(400f, 0.5f)
            newPursuit = 5f
        }
        
        if (newDetection >= 0.91f && newPursuit <= 0f) {"""

code = code.replace(old_entities, new_entities)

old_update_block = """                comboTimer = newComboTimer,
                entities = newEntities.toList(),
                lastDeltaTime = dt
            )"""
            
new_update_block = """                comboTimer = newComboTimer,
                entities = newEntities.toList(),
                pursuers = newPursuers.toList(),
                lastDeltaTime = dt
            )"""

code = code.replace(old_update_block, new_update_block)

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

print("Pursuer logic patched in GameViewModel")
