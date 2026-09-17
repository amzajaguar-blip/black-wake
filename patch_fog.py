import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

# 1. Add huntingFog state calculation
old_setup = """    val beamPath = remember { Path() }
    val boatPath = remember { Path() }"""
new_setup = """    val beamPath = remember { Path() }
    val boatPath = remember { Path() }
    
    val isHunted = state.pursuers.any { it.state == PursuerState.PURSUIT || it.state == PursuerState.INTERCEPT || it.state == PursuerState.ATTACK }
    val huntingFog by animateFloatAsState(
        targetValue = if (isHunted) 1f else 0f,
        animationSpec = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
        label = "huntingFog"
    )"""
code = code.replace(old_setup, new_setup)

# 2. Add fog drawing before player drawing
old_player_draw = """                }
                
                // Draw player
                val playerY = h * 0.85f"""
new_player_draw = """                }
                
                // Hunting Tension Fog: dynamically obscures distant view when actively hunted
                if (huntingFog > 0f) {
                    val fogColor = Color(0xFF040A0C).copy(alpha = huntingFog * 0.95f)
                    val fogBottom = horizonY + (h - horizonY) * 0.55f * huntingFog // Creeps down towards player
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(fogColor, fogColor, Color.Transparent),
                            startY = horizonY - 100f,
                            endY = fogBottom
                        ),
                        topLeft = Offset(0f, 0f),
                        size = Size(w, fogBottom)
                    )
                }
                
                // Draw player
                val playerY = h * 0.85f"""
code = code.replace(old_player_draw, new_player_draw)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

print("Fog logic injected")
