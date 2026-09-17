import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

hud_old = """fun Hud(state: GameState, viewModel: GameViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {"""

hud_new = """@Composable
fun CompassWidget(state: GameState, modifier: Modifier = Modifier) {
    // Map player velocity and position to a slight heading rotation
    // Player mostly goes North. Steeling left/right shifts heading slightly.
    val headingOffset = state.playerVelocityX * 15f + state.playerX * 10f
    
    Box(
        modifier = modifier
            .size(80.dp)
            .background(Color(0xFF040A0C).copy(alpha = 0.8f), androidx.compose.foundation.shape.CircleShape)
            .border(2.dp, Cyan.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = size.width / 2f - 4.dp.toPx()
            
            rotate(degrees = -headingOffset, pivot = Offset(cx, cy)) {
                // Draw N, E, S, W markings
                val textRadius = radius * 0.7f
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#2BE7E0") // Cyan
                    textSize = 30f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.MONOSPACE
                    isFakeBoldText = true
                }
                val paintSmall = android.graphics.Paint().apply {
                    color = android.graphics.Color.LTGRAY
                    textSize = 20f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.MONOSPACE
                }
                
                // Draw tick marks
                for (i in 0 until 360 step 15) {
                    val angleRad = Math.toRadians((i - 90).toDouble())
                    val isMajor = i % 90 == 0
                    val startRad = if (isMajor) radius * 0.8f else radius * 0.9f
                    val startX = cx + (Math.cos(angleRad) * startRad).toFloat()
                    val startY = cy + (Math.sin(angleRad) * startRad).toFloat()
                    val endX = cx + (Math.cos(angleRad) * radius).toFloat()
                    val endY = cy + (Math.sin(angleRad) * radius).toFloat()
                    
                    drawLine(
                        color = if (isMajor) Cyan else Color.Gray,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = if (isMajor) 3f else 1f
                    )
                }

                // We use native canvas for text drawing
                drawContext.canvas.nativeCanvas.apply {
                    drawText("N", cx, cy - textRadius + 10f, paint)
                    drawText("S", cx, cy + textRadius + 10f, paintSmall)
                    drawText("E", cx + textRadius, cy + 10f, paintSmall)
                    drawText("W", cx - textRadius, cy + 10f, paintSmall)
                }
            }
            
            // Draw center fixed indicator (the "ship")
            drawLine(
                color = Red,
                start = Offset(cx, cy - radius * 0.5f),
                end = Offset(cx, cy + radius * 0.2f),
                strokeWidth = 3f
            )
            drawLine(
                color = Red,
                start = Offset(cx - radius * 0.2f, cy + radius * 0.2f),
                end = Offset(cx + radius * 0.2f, cy + radius * 0.2f),
                strokeWidth = 3f
            )
        }
    }
}

@Composable
fun Hud(state: GameState, viewModel: GameViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {"""
code = code.replace(hud_old, hud_new)

hud_end_old = """            }
        }
    }
}

@Composable
fun GameCanvas"""

hud_end_new = """            }
        }
    }
        
        CompassWidget(
            state = state,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
        )
    }
}

@Composable
fun GameCanvas"""

code = code.replace(hud_end_old, hud_end_new)

# add nativeCanvas import if needed
if "import androidx.compose.ui.graphics.nativeCanvas" not in code:
    code = code.replace("import androidx.compose.ui.graphics.Color", "import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.graphics.nativeCanvas")


with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

print("GameScreen compass added")
