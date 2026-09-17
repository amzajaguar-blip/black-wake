import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

pursuer_glow_old = """                            // Sonar Highlight
                            if (state.sonarPingActive && kotlin.math.hypot(pCx - playerCx, pY - playerY) < state.sonarPingRadius) {
                                drawCircle(Cyan, baseRadius * 4f, Offset(0f, 0f), style = Stroke(width = 3f))
                                drawCircle(Cyan.copy(alpha = 0.2f), baseRadius * 4f, Offset(0f, 0f))
                            }"""
pursuer_glow_new = """                            // Sonar Highlight
                            if (state.sonarPingActive && kotlin.math.hypot(pCx - playerCx, pY - playerY) < state.sonarPingRadius) {
                                drawCircle(Cyan, baseRadius * 4f, Offset(0f, 0f), style = Stroke(width = 3f))
                                drawCircle(Cyan.copy(alpha = 0.2f), baseRadius * 4f, Offset(0f, 0f))
                            }
                            if (flareIntensity > 0f) {
                                drawCircle(Amber.copy(alpha = flareIntensity * 0.4f), baseRadius * 3.5f, Offset(0f, 0f))
                            }"""
code = code.replace(pursuer_glow_old, pursuer_glow_new)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

