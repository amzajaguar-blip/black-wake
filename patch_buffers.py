import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

# 1. Insert remembers at the top of GameCanvas
old_remembers = """    val wakePath = remember { Path() }
    val beamPath = remember { Path() }
    val boatPath = remember { Path() }
    val cockpitPath = remember { Path() }
    val entityPath = remember { Path() }"""

new_remembers = """    val wakePath = remember { Path() }
    val beamPath = remember { Path() }
    val boatPath = remember { Path() }
    val cockpitPath = remember { Path() }
    val entityPath = remember { Path() }
    
    // Fixed-size primitive array buffers for zero-allocation particle systems
    val rainParticles = remember { FloatArray(150 * 5) }
    val rainInitialized = remember { BooleanArray(1) }
    val sprayParticles = remember { FloatArray(150 * 6) }
    val sprayIndex = remember { IntArray(1) }
    
    if (!rainInitialized[0]) {
        for (i in 0 until 150) {
            val idx = i * 5
            val seed = i * 137.54f
            rainParticles[idx] = (seed * 123.4f) // baseX
            rainParticles[idx+1] = (seed * 456.7f) // baseY
            rainParticles[idx+2] = 0.8f + (seed % 40f) / 100f // speedMod
            rainParticles[idx+3] = 20f + (seed % 40f) // base dropLength
            rainParticles[idx+4] = if (i % 6 == 0) 1f else 0f // isForeground
        }
        rainInitialized[0] = true
    }"""
code = code.replace(old_remembers, new_remembers)

# 2. Replace the old rain system with the buffered rain + spray system
old_rain = """            // Dynamic Weather System (Rain / Sea Spray) - Stateless & Zero Allocation
            // Speed of rain increases drastically when boosting (fovOffset > 1.0)
            val globalRainSpeed = 1500f * state.fovOffset 
            val rainAngleOffset = 40f * state.playerVelocityX // Wind/rain slants based on boat steering
            val tensionRainAlpha = (0.2f + (state.detection * 0.4f)).coerceIn(0f, 1f) // Rain gets thicker under tension
            val rainColor = Color(0xFF6699AA).copy(alpha = tensionRainAlpha)
            val rainColorForeground = Color(0xFFAACCFF).copy(alpha = (tensionRainAlpha * 1.5f).coerceIn(0f, 1f))
            
            for (i in 0..150) {
                // Pseudo-random deterministic values per particle
                val seed = i * 137.54f
                val startX = (seed * 123.4f) % (w * 1.5f) - (w * 0.25f) // Bleed outside edges to handle wind shift
                val baseY = (seed * 456.7f) % h
                val speedMod = 0.8f + (seed % 40f) / 100f // 0.8 to 1.19
                val dropLength = 20f + (seed % 40f) * state.fovOffset
                
                // Add parallax / depth (some drops are thicker and faster)
                val isForeground = (i % 6 == 0)
                val strokeW = if (isForeground) 3f else 1.5f
                val depthSpeed = if (isForeground) 1.6f else 1f
                val activeColor = if (isForeground) rainColorForeground else rainColor
                
                // Use modulo to wrap around screen vertically continuously
                val rawY = baseY + state.runElapsed * globalRainSpeed * speedMod * depthSpeed
                val finalY = rawY % (h * 1.2f) - (h * 0.1f)
                val finalX = startX + (rainAngleOffset * (finalY / h))
                
                drawLine(
                    color = activeColor,
                    start = Offset(finalX, finalY),
                    end = Offset(finalX - rainAngleOffset * 0.3f, finalY + dropLength * depthSpeed),
                    strokeWidth = strokeW
                )
            }"""

new_system = """            // Dynamic Weather System (Rain) - Using Pre-calculated Primitive Buffer
            val globalRainSpeed = 1500f * state.fovOffset 
            val rainAngleOffset = 40f * state.playerVelocityX // Wind/rain slants based on boat steering
            val tensionRainAlpha = (0.2f + (state.detection * 0.4f)).coerceIn(0f, 1f)
            val rainColor = Color(0xFF6699AA).copy(alpha = tensionRainAlpha)
            val rainColorForeground = Color(0xFFAACCFF).copy(alpha = (tensionRainAlpha * 1.5f).coerceIn(0f, 1f))
            
            for (i in 0 until 150) {
                val idx = i * 5
                val startX = rainParticles[idx] % (w * 1.5f) - (w * 0.25f)
                val baseY = rainParticles[idx+1] % h
                val speedMod = rainParticles[idx+2]
                val dropLength = rainParticles[idx+3] * state.fovOffset
                val isForeground = rainParticles[idx+4] > 0.5f
                
                val strokeW = if (isForeground) 3f else 1.5f
                val depthSpeed = if (isForeground) 1.6f else 1f
                val activeColor = if (isForeground) rainColorForeground else rainColor
                
                val rawY = baseY + state.runElapsed * globalRainSpeed * speedMod * depthSpeed
                val finalY = rawY % (h * 1.2f) - (h * 0.1f)
                val finalX = startX + (rainAngleOffset * (finalY / h))
                
                drawLine(
                    color = activeColor,
                    start = Offset(finalX, finalY),
                    end = Offset(finalX - rainAngleOffset * 0.3f, finalY + dropLength * depthSpeed),
                    strokeWidth = strokeW
                )
            }
            
            // Dynamic Sea Spray System - Using Pre-calculated Primitive Buffer
            val dt = state.lastDeltaTime
            val emitCount = if (state.fovOffset > 1.0f) 5 else if (kotlin.math.abs(state.playerVelocityX) > 0.1f) 3 else 1
            
            // Player position from perspective
            val laneWidthBottomSpray = w * 0.4f * state.fovOffset
            val playerCx = w/2 + state.playerX * laneWidthBottomSpray
            val playerY = h * 0.85f
            
            // Emit new spray particles
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
            }
            
            // Update and Draw Spray
            for (i in 0 until 150) {
                val idx = i * 6
                if (sprayParticles[idx+4] > 0f) {
                    sprayParticles[idx] += sprayParticles[idx+2] * dt
                    sprayParticles[idx+1] += sprayParticles[idx+3] * dt
                    sprayParticles[idx+4] -= dt
                    
                    val lifeRatio = (sprayParticles[idx+4] / sprayParticles[idx+5]).coerceIn(0f, 1f)
                    if (lifeRatio > 0f) {
                        val alpha = lifeRatio * 0.5f
                        val radius = 4f + (1f - lifeRatio) * 12f
                        drawCircle(Color.White.copy(alpha = alpha), radius = radius, center = Offset(sprayParticles[idx], sprayParticles[idx+1]))
                    }
                }
            }"""
code = code.replace(old_rain, new_system)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

print("Buffers integrated.")
