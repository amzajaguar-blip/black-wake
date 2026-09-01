import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

old_remembers = """    val sprayParticles = remember { FloatArray(150 * 6) }
    val sprayIndex = remember { IntArray(1) }"""

new_remembers = """    val sprayParticles = remember { FloatArray(150 * 6) }
    val sprayIndex = remember { IntArray(1) }
    val randomTable = remember { FloatArray(1024) { Math.random().toFloat() } }
    val randomIndex = remember { IntArray(1) }"""
code = code.replace(old_remembers, new_remembers)


old_spray = """            // Emit new spray particles
            for (i in 0 until emitCount) {
                val idx = sprayIndex[0] * 6
                val r1 = Math.random().toFloat()
                val r2 = Math.random().toFloat()
                val r3 = Math.random().toFloat()
                
                sprayParticles[idx] = playerCx + (r1 - 0.5f) * 40f
                sprayParticles[idx+1] = playerY + 20f + (r2 - 0.5f) * 20f
                // Lateral velocity based on boat steering + random scatter
                sprayParticles[idx+2] = -state.playerVelocityX * 300f + (r3 - 0.5f) * 150f
                // Vertical velocity based on boat speed
                val boostSpeed = if (state.fovOffset > 1.0f) 600f else 300f
                sprayParticles[idx+3] = boostSpeed + (Math.random().toFloat() * 200f)
                
                val lifeTime = 0.3f + Math.random().toFloat() * 0.4f
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
code = code.replace(old_spray, new_spray)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

print("Random table integrated.")
