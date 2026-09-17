import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

old_draw_player = """                // Draw player
                val playerY = h * 0.85f"""
                
new_draw_pursuers = """                // Draw Pursuers
                for (p in state.pursuers) {
                    val pScale = 1f - (p.y / 150f).coerceIn(0f, 1f)
                    val pY = horizonY + (h - horizonY) * (p.y / 100f)
                    val pCxBottom = w/2 + (p.x) * laneWidthBottom
                    val pCxTop = w/2 + (p.x) * laneWidthTop
                    val pCx = pCxTop + (pCxBottom - pCxTop) * (p.y / 100f)
                    
                    translate(left = pCx, top = pY) {
                        rotate(degrees = p.roll) {
                            
                            // Pursuer Wake
                            val pWakeSpread = 30f * pScale
                            val pWakeDeform = -p.velocityX * 50f
                            wakePath.reset()
                            wakePath.moveTo(0f, 0f)
                            wakePath.lineTo(-pWakeSpread + pWakeDeform, h - pY)
                            wakePath.lineTo(pWakeSpread + pWakeDeform, h - pY)
                            wakePath.close()
                            drawPath(wakePath, Color.White.copy(alpha = 0.1f * pScale))
                            
                            // Body
                            boatPath.reset()
                            val baseRadius = 15f * pScale
                            val primaryColor = when (p.type) {
                                EnemyBoatType.PATROL -> Color.Gray
                                EnemyBoatType.INTERCEPTOR -> Color(0xFF556655)
                                EnemyBoatType.POLICE -> Color(0xFF2244AA)
                                EnemyBoatType.HUNTER -> Color(0xFF222222)
                                EnemyBoatType.ARMORED -> Color(0xFF665544)
                                EnemyBoatType.ELITE -> Color(0xFF990000)
                            }
                            
                            when (p.type) {
                                EnemyBoatType.PATROL, EnemyBoatType.POLICE -> {
                                    boatPath.moveTo(0f, -baseRadius * 2f) 
                                    boatPath.lineTo(baseRadius * 0.8f, baseRadius)
                                    boatPath.lineTo(-baseRadius * 0.8f, baseRadius)
                                }
                                EnemyBoatType.INTERCEPTOR, EnemyBoatType.HUNTER -> {
                                    boatPath.moveTo(0f, -baseRadius * 2.5f) 
                                    boatPath.lineTo(baseRadius, baseRadius * 0.5f)
                                    boatPath.lineTo(0f, baseRadius)
                                    boatPath.lineTo(-baseRadius, baseRadius * 0.5f)
                                }
                                EnemyBoatType.ARMORED -> {
                                    boatPath.moveTo(0f, -baseRadius * 1.5f)
                                    boatPath.lineTo(baseRadius * 1.5f, -baseRadius * 0.5f)
                                    boatPath.lineTo(baseRadius * 1.2f, baseRadius * 1.5f)
                                    boatPath.lineTo(-baseRadius * 1.2f, baseRadius * 1.5f)
                                    boatPath.lineTo(-baseRadius * 1.5f, -baseRadius * 0.5f)
                                }
                                EnemyBoatType.ELITE -> {
                                    boatPath.moveTo(0f, -baseRadius * 3f) 
                                    boatPath.lineTo(baseRadius * 1.2f, baseRadius * 1.2f)
                                    boatPath.lineTo(baseRadius * 0.5f, baseRadius * 0.8f)
                                    boatPath.lineTo(-baseRadius * 0.5f, baseRadius * 0.8f)
                                    boatPath.lineTo(-baseRadius * 1.2f, baseRadius * 1.2f)
                                }
                            }
                            boatPath.close()
                            drawPath(boatPath, primaryColor)
                            drawPath(boatPath, Red, style = Stroke(width = 1.5f))
                            
                            // Lights & VFX
                            val beamColor = if (p.type == EnemyBoatType.POLICE) 
                                if ((state.runElapsed * 10f).toInt() % 2 == 0) Color.Blue else Color.Red
                            else Red
                            
                            // Warning glow if intercepting
                            if (p.state == PursuerState.INTERCEPT) {
                                drawCircle(Color.Red.copy(alpha=0.4f), baseRadius * 3f, Offset(0f, 0f))
                            }
                            
                            // Engine Glow
                            drawCircle(Cyan.copy(alpha = 0.8f), baseRadius * 0.5f, Offset(0f, baseRadius * 1.2f))
                        }
                    }
                }
                
                // Draw player
                val playerY = h * 0.85f"""

code = code.replace(old_draw_player, new_draw_pursuers)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

print("Pursuers render logic patched")
