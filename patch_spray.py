import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

old_wake = """                // Player Wake (scia) - Deforms based on steering and roll
                val wakeSpread = 30f + (state.fovOffset - 1.0f) * 150f // 30f normal, 60f boosted
                val wakeDeform = -state.playerVelocityX * 100f // Opposite to steering
                wakePath.reset()
                wakePath.moveTo(playerCx, playerY)
                wakePath.lineTo(playerCx - wakeSpread + wakeDeform, h)
                wakePath.lineTo(playerCx + wakeSpread + wakeDeform, h)
                wakePath.close()
                
                drawPath(wakePath, Color.White.copy(alpha = 0.15f))"""

new_wake = """                // Player Wake (scia) - Deforms based on steering and roll
                val wakeSpread = 30f + (state.fovOffset - 1.0f) * 150f // 30f normal, 60f boosted
                val wakeDeform = -state.playerVelocityX * 150f // Opposite to steering
                val wakeDeformMid = -state.playerVelocityX * 60f
                
                wakePath.reset()
                wakePath.moveTo(playerCx, playerY)
                // Left curve
                wakePath.quadraticBezierTo(
                    playerCx - wakeSpread * 0.5f + wakeDeformMid, playerY + (h - playerY) * 0.5f,
                    playerCx - wakeSpread + wakeDeform, h
                )
                // Base
                wakePath.lineTo(playerCx + wakeSpread + wakeDeform, h)
                // Right curve back
                wakePath.quadraticBezierTo(
                    playerCx + wakeSpread * 0.5f + wakeDeformMid, playerY + (h - playerY) * 0.5f,
                    playerCx, playerY
                )
                wakePath.close()
                
                drawPath(wakePath, Color.White.copy(alpha = 0.15f))"""

code = code.replace(old_wake, new_wake)

old_spray = """            // Emit new spray particles
            for (i in 0 until emitCount) {
                val idx = sprayIndex[0] * 6
                
                val ri = randomIndex[0]
                val r1 = randomTable[ri % 1024]
                val r2 = randomTable[(ri + 1) % 1024]
                val r3 = randomTable[(ri + 2) % 1024]
                val r4 = randomTable[(ri + 3) % 1024]
                val r5 = randomTable[(ri + 4) % 1024]
                randomIndex[0] = (ri + 5) % 1024
                
                sprayParticles[idx] = playerCx + (r1 - 0.5f) * 40f
                sprayParticles[idx+1] = playerY + 20f + (r2 - 0.5f) * 20f
                // Lateral velocity based on boat steering + random scatter
                sprayParticles[idx+2] = -state.playerVelocityX * 300f + (r3 - 0.5f) * 150f
                // Vertical velocity based on boat speed
                val boostSpeed = if (state.fovOffset > 1.0f) 600f else 300f
                sprayParticles[idx+3] = boostSpeed + (r4 * 200f)
                
                val lifeTime = 0.3f + r5 * 0.4f
                sprayParticles[idx+4] = lifeTime // current life
                sprayParticles[idx+5] = lifeTime // max life
                
                sprayIndex[0] = (sprayIndex[0] + 1) % 150
            }"""

new_spray = """            // Emit new spray particles
            for (i in 0 until emitCount) {
                val idx = sprayIndex[0] * 6
                
                val ri = randomIndex[0]
                val r1 = randomTable[ri % 1024]
                val r2 = randomTable[(ri + 1) % 1024]
                val r3 = randomTable[(ri + 2) % 1024]
                val r4 = randomTable[(ri + 3) % 1024]
                val r5 = randomTable[(ri + 4) % 1024]
                randomIndex[0] = (ri + 5) % 1024
                
                // Bias spray origin based on steering (if steering left, spray comes more from the right side)
                val sprayOriginXOffset = (r1 - 0.5f) * 40f + state.playerVelocityX * 25f
                sprayParticles[idx] = playerCx + sprayOriginXOffset
                sprayParticles[idx+1] = playerY + 20f + (r2 - 0.5f) * 20f
                
                // Lateral velocity based on boat steering + roll + random scatter
                // Stronger lateral ejection mapping when steering
                val steerBias = -state.playerVelocityX * 500f // Pushes spray aggressively away from turn
                val rollBias = (state.playerRoll / 15f) * 120f // Tilt adds to spray momentum mapping
                sprayParticles[idx+2] = steerBias + rollBias + (r3 - 0.5f) * 200f
                
                // Vertical velocity based on boat speed
                val boostSpeed = if (state.fovOffset > 1.0f) 800f else 400f
                sprayParticles[idx+3] = boostSpeed + (r4 * 250f)
                
                val lifeTime = 0.3f + r5 * 0.4f
                sprayParticles[idx+4] = lifeTime // current life
                sprayParticles[idx+5] = lifeTime // max life
                
                sprayIndex[0] = (sprayIndex[0] + 1) % 150
            }"""

code = code.replace(old_spray, new_spray)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)
print("Updated successfully.")
