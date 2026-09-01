import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

# 1. Add new remember paths at the start of GameCanvas
old_paths = """    val wakePath = remember { Path() }
    val beamPath = remember { Path() }"""

new_paths = """    val wakePath = remember { Path() }
    val beamPath = remember { Path() }
    val boatPath = remember { Path() }
    val cockpitPath = remember { Path() }
    val entityPath = remember { Path() }"""
code = code.replace(old_paths, new_paths)

# 2. Add sky and grid before lane hints
old_translate_start = """                        // Perspective projection parameters (affected by FOV)
                val fovScale = state.fovOffset
                val horizonY = h * 0.35f
                val laneWidthBottom = w * 0.4f * fovScale
                val laneWidthTop = w * 0.05f * fovScale
                
                // Draw magnetic lane hints"""

new_translate_start = """                        // Perspective projection parameters (affected by FOV)
                val fovScale = state.fovOffset
                val horizonY = h * 0.35f
                val laneWidthBottom = w * 0.4f * fovScale
                val laneWidthTop = w * 0.05f * fovScale
                
                // Atmosphere & Horizon Sky
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
                
                // Speed lines
                val speedLineOffset = (state.runElapsed * 20f) % 20f
                for (i in 0..5) {
                    val lineY = horizonY + (speedLineOffset + i * 20f) / 100f * (h - horizonY)
                    if (lineY > horizonY && lineY < h) {
                        val alpha = (1f - (lineY - horizonY) / (h - horizonY)) * 0.15f
                        drawLine(Color.White.copy(alpha = alpha), Offset(0f, lineY), Offset(w, lineY), strokeWidth = 1f)
                    }
                }
                
                // Draw magnetic lane hints"""
code = code.replace(old_translate_start, new_translate_start)

# 3. Upgrade Entity Rendering
old_entity_render = """                        // Draw Entity
                        if (entity.type == EntityType.MINE || entity.type == EntityType.ENEMY || entity.type == EntityType.WRECK) {
                            drawCircle(Color(0xFF040A0C), radius + 3f, Offset(cx, cy))
                        }
                        drawCircle(color, radius, Offset(cx, cy))
                        if (entity.type == EntityType.FORK) {
                            drawCircle(color, radius * 1.5f, Offset(cx, cy), style = Stroke(width = 2f))
                        } else if (entity.type == EntityType.ENEMY) {
                            // Draw light beam for enemy
                            beamPath.reset()
                            beamPath.moveTo(cx, cy)
                            beamPath.lineTo(cx - radius * 4f, cy + radius * 8f)
                            beamPath.lineTo(cx + radius * 4f, cy + radius * 8f)
                            beamPath.close()
                            
                            drawPath(beamPath, Red.copy(alpha = 0.2f))
                        }"""

new_entity_render = """                        // Draw Entity
                        entityPath.reset()
                        when (entity.type) {
                            EntityType.INTEL -> {
                                entityPath.moveTo(cx, cy - radius)
                                entityPath.lineTo(cx + radius, cy)
                                entityPath.lineTo(cx, cy + radius)
                                entityPath.lineTo(cx - radius, cy)
                                entityPath.close()
                                drawPath(entityPath, color)
                                drawPath(entityPath, Color.White, style = Stroke(width = 2f))
                            }
                            EntityType.FUEL -> {
                                drawRoundRect(color, topLeft = Offset(cx - radius*0.8f, cy - radius), size = Size(radius*1.6f, radius*2f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius*0.4f))
                                drawRoundRect(Color.White, topLeft = Offset(cx - radius*0.8f, cy - radius), size = Size(radius*1.6f, radius*2f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius*0.4f), style = Stroke(width = 2f))
                            }
                            EntityType.MINE -> {
                                drawCircle(Color(0xFF040A0C), radius + 3f, Offset(cx, cy))
                                drawCircle(color, radius, Offset(cx, cy))
                                drawCircle(Color.Black, radius * 0.4f, Offset(cx, cy))
                                drawLine(Color.Black, Offset(cx - radius, cy), Offset(cx + radius, cy), strokeWidth = 3f)
                                drawLine(Color.Black, Offset(cx, cy - radius), Offset(cx, cy + radius), strokeWidth = 3f)
                            }
                            EntityType.WRECK -> {
                                drawCircle(Color(0xFF040A0C), radius + 3f, Offset(cx, cy))
                                entityPath.moveTo(cx - radius, cy)
                                entityPath.lineTo(cx - radius*0.5f, cy - radius)
                                entityPath.lineTo(cx + radius*0.8f, cy - radius*0.4f)
                                entityPath.lineTo(cx + radius, cy + radius*0.6f)
                                entityPath.lineTo(cx - radius*0.2f, cy + radius)
                                entityPath.close()
                                drawPath(entityPath, color)
                            }
                            EntityType.ENEMY -> {
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
                            }
                            EntityType.FORK -> {
                                drawCircle(color, radius, Offset(cx, cy))
                                drawCircle(color, radius * 1.5f, Offset(cx, cy), style = Stroke(width = 2f))
                            }
                        }"""
code = code.replace(old_entity_render, new_entity_render)

# 4. Upgrade Boat Rendering
old_boat_render = """                        // Hull
                        drawRect(Color(0xFF132F38), topLeft = Offset(-25f, -40f), size = Size(50f, 80f))
                        // Engine / details
                        drawRect(Cyan, topLeft = Offset(-10f, 20f), size = Size(20f, 15f))
                        
                        if (state.invulnerable > 0f) {
                            val alpha = (kotlin.math.sin(state.runElapsed * 20f) * 0.5f + 0.5f).toFloat() * 0.5f
                            drawRect(Color.White.copy(alpha=alpha), topLeft = Offset(-25f, -40f), size = Size(50f, 80f))
                        }"""

new_boat_render = """                        // Hull - Sleek stealth shape
                        boatPath.reset()
                        boatPath.moveTo(0f, -45f) // Bow (Nose)
                        boatPath.lineTo(22f, 15f) // Starboard (Right)
                        boatPath.lineTo(16f, 40f) // Right Stern
                        boatPath.lineTo(-16f, 40f) // Left Stern
                        boatPath.lineTo(-22f, 15f) // Port (Left)
                        boatPath.close()
                        
                        drawPath(boatPath, Color(0xFF132F38))
                        drawPath(boatPath, Color(0xFF234B58), style = Stroke(width = 2f)) // Edge highlight
                        
                        // Cockpit glass
                        cockpitPath.reset()
                        cockpitPath.moveTo(0f, -15f)
                        cockpitPath.lineTo(12f, 10f)
                        cockpitPath.lineTo(-12f, 10f)
                        cockpitPath.close()
                        drawPath(cockpitPath, Color.Black)
                        
                        // Engine exhausts / details
                        drawRect(Cyan, topLeft = Offset(-12f, 40f), size = Size(8f, 10f))
                        drawRect(Cyan, topLeft = Offset(4f, 40f), size = Size(8f, 10f))
                        
                        // Engine Glow based on boost
                        val glowScale = if (state.fovOffset > 1.0f) 2f else 1f
                        drawCircle(Cyan.copy(alpha = 0.6f), radius = 10f * glowScale, center = Offset(-8f, 48f))
                        drawCircle(Cyan.copy(alpha = 0.6f), radius = 10f * glowScale, center = Offset(8f, 48f))
                        
                        if (state.invulnerable > 0f) {
                            val alpha = (kotlin.math.sin(state.runElapsed * 20f) * 0.5f + 0.5f).toFloat() * 0.5f
                            drawPath(boatPath, Color.White.copy(alpha=alpha))
                        }"""
code = code.replace(old_boat_render, new_boat_render)


with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

print("Upgrade complete.")
