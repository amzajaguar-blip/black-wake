import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

# 1. Move player coordinate calculation up before entities loop
old_perspective = """                // Perspective projection parameters (affected by FOV)
                val fovScale = state.fovOffset
                val horizonY = h * 0.35f
                val laneWidthBottom = w * 0.4f * fovScale
                val laneWidthTop = w * 0.05f * fovScale
                
                // Draw magnetic lane hints"""
                
new_perspective = """                // Perspective projection parameters (affected by FOV)
                val fovScale = state.fovOffset
                val horizonY = h * 0.35f
                val laneWidthBottom = w * 0.4f * fovScale
                val laneWidthTop = w * 0.05f * fovScale
                
                // Player Coordinates
                val playerY = h * 0.85f
                val playerCx = w/2 + (state.playerX) * laneWidthBottom
                
                // Draw Sonar Ping
                if (state.sonarPingActive) {
                    drawCircle(
                        color = Cyan.copy(alpha = 1f - (state.sonarPingRadius / 1200f).coerceIn(0f, 1f)),
                        radius = state.sonarPingRadius,
                        center = Offset(playerCx, playerY),
                        style = Stroke(width = 4f)
                    )
                }
                
                // Draw magnetic lane hints"""
code = code.replace(old_perspective, new_perspective)

# 2. Add Sonar Highlight to Entities
old_enemy_draw = """                                // Dynamic Enemy Boats based on type
                                drawCircle(Color(0xFF040A0C).copy(alpha = 0.5f), radius + 4f, Offset(cx, cy))"""
new_enemy_draw = """                                // Dynamic Enemy Boats based on type
                                drawCircle(Color(0xFF040A0C).copy(alpha = 0.5f), radius + 4f, Offset(cx, cy))
                                if (state.sonarPingActive && kotlin.math.hypot(cx - playerCx, cy - playerY) < state.sonarPingRadius) {
                                    drawCircle(Cyan, radius * 1.5f, Offset(cx, cy), style = Stroke(width = 2f))
                                    drawLine(Cyan, Offset(cx - radius * 2f, cy), Offset(cx + radius * 2f, cy), strokeWidth = 1f)
                                    drawLine(Cyan, Offset(cx, cy - radius * 2f), Offset(cx, cy + radius * 2f), strokeWidth = 1f)
                                }"""
code = code.replace(old_enemy_draw, new_enemy_draw)

# 3. Add Sonar Highlight to Pursuers
old_pursuer_glow = """                            // Warning glow if intercepting
                            if (p.state == PursuerState.INTERCEPT) {
                                drawCircle(Color.Red.copy(alpha=0.4f), baseRadius * 3f, Offset(0f, 0f))
                            }"""
new_pursuer_glow = """                            // Warning glow if intercepting
                            if (p.state == PursuerState.INTERCEPT) {
                                drawCircle(Color.Red.copy(alpha=0.4f), baseRadius * 3f, Offset(0f, 0f))
                            }
                            
                            // Sonar Highlight
                            if (state.sonarPingActive && kotlin.math.hypot(pCx - playerCx, pY - playerY) < state.sonarPingRadius) {
                                drawCircle(Cyan, baseRadius * 4f, Offset(0f, 0f), style = Stroke(width = 3f))
                                drawCircle(Cyan.copy(alpha = 0.2f), baseRadius * 4f, Offset(0f, 0f))
                            }"""
code = code.replace(old_pursuer_glow, new_pursuer_glow)

# 4. Remove duplicate player coordinate declarations
old_player_coords = """                // Draw player
                val playerY = h * 0.85f
                val targetCxBottom = w/2 + (state.playerX) * laneWidthBottom
                val playerCx = targetCxBottom
                
                // Player Wake"""
new_player_coords = """                // Draw player
                // Player Wake"""
code = code.replace(old_player_coords, new_player_coords)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

print("Sonar Drawing patched")
