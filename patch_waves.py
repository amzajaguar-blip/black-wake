import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

# 1. Update wake rendering
old_wake = """                // Player Wake (scia)
                val wakeSpread = 30f + (state.fovOffset - 1.0f) * 150f // 30f normal, 60f boosted
                wakePath.reset()
                wakePath.moveTo(playerCx, playerY)
                wakePath.lineTo(playerCx - wakeSpread - (state.playerVelocityX * 2f), h)
                wakePath.lineTo(playerCx + wakeSpread - (state.playerVelocityX * 2f), h)
                wakePath.close()
                
                drawPath(wakePath, Color.White.copy(alpha = 0.15f))"""

new_wake = """                // Player Wake (scia) - Deforms based on steering and roll
                val wakeSpread = 30f + (state.fovOffset - 1.0f) * 150f // 30f normal, 60f boosted
                val wakeDeform = -state.playerVelocityX * 100f // Opposite to steering
                wakePath.reset()
                wakePath.moveTo(playerCx, playerY)
                wakePath.lineTo(playerCx - wakeSpread + wakeDeform, h)
                wakePath.lineTo(playerCx + wakeSpread + wakeDeform, h)
                wakePath.close()
                
                drawPath(wakePath, Color.White.copy(alpha = 0.15f))
                
                // Procedural Waves (Sinusoidal reflection lines)
                val waveAlpha = 0.03f + (state.detection * 0.07f).coerceIn(0f, 0.1f)
                val waveColor = Color.White.copy(alpha = waveAlpha)
                for (i in 0..4) {
                    val lineY = horizonY + (state.runElapsed * 80f + i * (h - horizonY) / 5) % (h - horizonY)
                    val sinDeform = kotlin.math.sin(state.runElapsed * 3f + i) * 30f
                    if (lineY > horizonY) {
                        drawLine(waveColor, Offset(0f, lineY + sinDeform), Offset(w, lineY - sinDeform), strokeWidth = 2f)
                    }
                }"""
code = code.replace(old_wake, new_wake)

# 2. Add lightning flash to sky
old_sky = """                // Atmosphere & Horizon Sky
                val skyTint = androidx.compose.ui.graphics.lerp(Color(0xFF02070A), Color(0xFF2A0A0A), (state.detection / 0.8f).coerceIn(0f, 1f))
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Black, skyTint),
                        startY = 0f,
                        endY = horizonY
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(w, horizonY)
                )"""

new_sky = """                // Atmosphere & Horizon Sky
                val skyTint = androidx.compose.ui.graphics.lerp(Color(0xFF02070A), Color(0xFF2A0A0A), (state.detection / 0.8f).coerceIn(0f, 1f))
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Black, skyTint),
                        startY = 0f,
                        endY = horizonY
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(w, horizonY)
                )
                
                // Deterministic Lightning during Black Tide
                if (state.detection > 0.85f) {
                    val t = state.runElapsed
                    val lightningAlpha = if (t % 4.3f < 0.1f) 0.4f else if (t % 5.7f < 0.05f) 0.6f else 0f
                    if (lightningAlpha > 0f) {
                        drawRect(Color.White.copy(alpha = lightningAlpha), size = Size(w, h))
                    }
                }"""
code = code.replace(old_sky, new_sky)

# 3. Add Rain Wind Slant Smoothing
# Need to make sure rainAngleOffset doesn't snap instantly but is limited or progressive.
# Actually, state.playerVelocityX is already physically smoothed by the viewmodel's steering acceleration!
# So playerVelocityX is already smooth! We just limit its max effect.

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

print("Waves and lightning integrated.")
