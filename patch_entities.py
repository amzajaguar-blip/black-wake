import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

old_enemy = """                            EntityType.ENEMY -> {
                                drawCircle(Color(0xFF040A0C), radius + 3f, Offset(cx, cy))
                                entityPath.moveTo(cx, cy + radius) // nose pointing at player
                                entityPath.lineTo(cx + radius, cy - radius)
                                entityPath.lineTo(cx, cy - radius * 0.5f)
                                entityPath.lineTo(cx - radius, cy - radius)
                                entityPath.close()
                                drawPath(entityPath, color)
                                
                                // Draw light beam for enemy
                                beamPath.reset()
                                beamPath.moveTo(cx, cy + radius*0.5f)
                                beamPath.lineTo(cx - radius * 4f, cy + radius * 8f)
                                beamPath.lineTo(cx + radius * 4f, cy + radius * 8f)
                                beamPath.close()
                                drawPath(beamPath, Red.copy(alpha = 0.25f))
                            }"""

new_enemy = """                            EntityType.ENEMY -> {
                                // Dynamic Enemy Boats based on type
                                drawCircle(Color(0xFF040A0C).copy(alpha = 0.5f), radius + 4f, Offset(cx, cy))
                                entityPath.reset()
                                
                                val primaryColor = when (entity.enemyType) {
                                    EnemyBoatType.PATROL -> Color.Gray
                                    EnemyBoatType.INTERCEPTOR -> Color(0xFF556655)
                                    EnemyBoatType.POLICE -> Color(0xFF2244AA)
                                    EnemyBoatType.HUNTER -> Color(0xFF222222)
                                    EnemyBoatType.ARMORED -> Color(0xFF665544)
                                    EnemyBoatType.ELITE -> Color(0xFF990000)
                                }
                                
                                when (entity.enemyType) {
                                    EnemyBoatType.PATROL, EnemyBoatType.POLICE -> {
                                        // Slim, fast
                                        entityPath.moveTo(cx, cy + radius) 
                                        entityPath.lineTo(cx + radius * 0.6f, cy - radius)
                                        entityPath.lineTo(cx - radius * 0.6f, cy - radius)
                                    }
                                    EnemyBoatType.INTERCEPTOR, EnemyBoatType.HUNTER -> {
                                        // Angular, aggressive
                                        entityPath.moveTo(cx, cy + radius * 1.2f) 
                                        entityPath.lineTo(cx + radius * 0.8f, cy - radius)
                                        entityPath.lineTo(cx, cy - radius * 0.5f)
                                        entityPath.lineTo(cx - radius * 0.8f, cy - radius)
                                    }
                                    EnemyBoatType.ARMORED -> {
                                        // Blocky, heavy
                                        entityPath.moveTo(cx, cy + radius)
                                        entityPath.lineTo(cx + radius, cy + radius * 0.5f)
                                        entityPath.lineTo(cx + radius * 1.2f, cy - radius)
                                        entityPath.lineTo(cx - radius * 1.2f, cy - radius)
                                        entityPath.lineTo(cx - radius, cy + radius * 0.5f)
                                    }
                                    EnemyBoatType.ELITE -> {
                                        // Futuristic
                                        entityPath.moveTo(cx, cy + radius * 1.5f) 
                                        entityPath.lineTo(cx + radius, cy - radius)
                                        entityPath.lineTo(cx + radius * 0.3f, cy - radius * 0.2f)
                                        entityPath.lineTo(cx - radius * 0.3f, cy - radius * 0.2f)
                                        entityPath.lineTo(cx - radius, cy - radius)
                                    }
                                }
                                entityPath.close()
                                drawPath(entityPath, primaryColor)
                                drawPath(entityPath, color, style = Stroke(width = 1.5f))
                                
                                // Draw light beam for enemy
                                beamPath.reset()
                                beamPath.moveTo(cx, cy + radius*0.5f)
                                beamPath.lineTo(cx - radius * 4f, cy + radius * 8f)
                                beamPath.lineTo(cx + radius * 4f, cy + radius * 8f)
                                beamPath.close()
                                
                                val beamColor = if (entity.enemyType == EnemyBoatType.POLICE) 
                                    if ((state.runElapsed * 10f).toInt() % 2 == 0) Color.Blue else Color.Red
                                else Red
                                drawPath(beamPath, beamColor.copy(alpha = 0.25f))
                            }"""

code = code.replace(old_enemy, new_enemy)

old_player = """                    rotate(degrees = state.playerRoll) {
                        // Hull - Sleek stealth shape
                        boatPath.reset()
                        boatPath.moveTo(0f, -45f) // Bow (Nose)
                        boatPath.lineTo(22f, 15f) // Starboard (Right)
                        boatPath.lineTo(16f, 40f) // Right Stern
                        boatPath.lineTo(-16f, 40f) // Left Stern
                        boatPath.lineTo(-22f, 15f) // Port (Left)
                        boatPath.close()
                        
                        drawPath(boatPath, Color(0xFF101820))
                        drawPath(boatPath, Cyan, style = Stroke(width = 2f))
                        
                        // Cockpit
                        cockpitPath.reset()
                        cockpitPath.moveTo(0f, -10f)
                        cockpitPath.lineTo(12f, 15f)
                        cockpitPath.lineTo(-12f, 15f)
                        cockpitPath.close()
                        
                        drawPath(cockpitPath, Color.Black)
                        drawPath(cockpitPath, Cyan.copy(alpha=0.5f), style = Stroke(width = 1f))
                        
                        // Engine glow
                        val glowScale = 1.0f + (state.fovOffset - 1.0f) * 2f
                        drawCircle(Cyan.copy(alpha = 0.6f), radius = 10f * glowScale, center = Offset(-8f, 48f))
                        drawCircle(Cyan.copy(alpha = 0.6f), radius = 10f * glowScale, center = Offset(8f, 48f))
                        
                        if (state.invulnerable > 0f) {
                            val alpha = (kotlin.math.sin(state.runElapsed * 20f) * 0.5f + 0.5f).toFloat() * 0.5f
                            drawPath(boatPath, Color.White.copy(alpha=alpha))
                        }
                    }"""

new_player = """                    rotate(degrees = state.playerRoll) {
                        boatPath.reset()
                        cockpitPath.reset()
                        var hullColor = Color(0xFF101820)
                        var accentColor = Cyan
                        
                        when (state.playerBoatType) {
                            PlayerBoatType.SPEEDBOAT -> {
                                hullColor = Color(0xFFE0E0E0)
                                accentColor = Color(0xFFFF5500)
                                boatPath.moveTo(0f, -40f) 
                                boatPath.lineTo(18f, 10f) 
                                boatPath.lineTo(14f, 35f) 
                                boatPath.lineTo(-14f, 35f) 
                                boatPath.lineTo(-18f, 10f) 
                                boatPath.close()
                                
                                cockpitPath.moveTo(0f, -5f)
                                cockpitPath.lineTo(10f, 15f)
                                cockpitPath.lineTo(-10f, 15f)
                                cockpitPath.close()
                            }
                            PlayerBoatType.RACING -> {
                                hullColor = Color(0xFFDD1111)
                                accentColor = Color.White
                                boatPath.moveTo(0f, -50f) 
                                boatPath.lineTo(15f, 0f) 
                                boatPath.lineTo(20f, 40f) 
                                boatPath.lineTo(-20f, 40f) 
                                boatPath.lineTo(-15f, 0f) 
                                boatPath.close()
                                
                                cockpitPath.moveTo(0f, -15f)
                                cockpitPath.lineTo(8f, 10f)
                                cockpitPath.lineTo(-8f, 10f)
                                cockpitPath.close()
                            }
                            PlayerBoatType.STEALTH -> {
                                hullColor = Color(0xFF101820)
                                accentColor = Cyan
                                boatPath.moveTo(0f, -45f)
                                boatPath.lineTo(22f, 15f)
                                boatPath.lineTo(16f, 40f)
                                boatPath.lineTo(-16f, 40f)
                                boatPath.lineTo(-22f, 15f)
                                boatPath.close()
                                
                                cockpitPath.moveTo(0f, -10f)
                                cockpitPath.lineTo(12f, 15f)
                                cockpitPath.lineTo(-12f, 15f)
                                cockpitPath.close()
                            }
                            else -> {
                                hullColor = Color(0xFF445544)
                                accentColor = Color(0xFFAAAABB)
                                boatPath.moveTo(0f, -35f)
                                boatPath.lineTo(25f, 15f)
                                boatPath.lineTo(25f, 45f)
                                boatPath.lineTo(-25f, 45f)
                                boatPath.lineTo(-25f, 15f)
                                boatPath.close()
                                
                                cockpitPath.moveTo(-15f, 0f)
                                cockpitPath.lineTo(15f, 0f)
                                cockpitPath.lineTo(15f, 20f)
                                cockpitPath.lineTo(-15f, 20f)
                                cockpitPath.close()
                            }
                        }
                        
                        drawPath(boatPath, hullColor)
                        drawPath(boatPath, accentColor, style = Stroke(width = 2f))
                        
                        drawPath(cockpitPath, Color.Black)
                        drawPath(cockpitPath, accentColor.copy(alpha=0.5f), style = Stroke(width = 1f))
                        
                        // Engine glow
                        val glowScale = 1.0f + (state.fovOffset - 1.0f) * 2f
                        drawCircle(accentColor.copy(alpha = 0.6f), radius = 10f * glowScale, center = Offset(-8f, 48f))
                        drawCircle(accentColor.copy(alpha = 0.6f), radius = 10f * glowScale, center = Offset(8f, 48f))
                        
                        if (state.invulnerable > 0f) {
                            val alpha = (kotlin.math.sin(state.runElapsed * 20f) * 0.5f + 0.5f).toFloat() * 0.5f
                            drawPath(boatPath, Color.White.copy(alpha=alpha))
                        }
                    }"""
code = code.replace(old_player, new_player)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

print("Entities patched")
