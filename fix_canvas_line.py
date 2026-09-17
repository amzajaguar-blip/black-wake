import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

canvas_old = """                // Draw path line
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lineX = size.width / 4f
                    val stepY = size.height / (chapter.sectors.size.coerceAtLeast(2) - 1)
                    
                    for (i in 0 until chapter.sectors.size - 1) {
                        val color = if (i < state.sectorIndex) Cyan else Color.Gray.copy(alpha = 0.5f)
                        val strokeWidth = if (i < state.sectorIndex) 4.dp.toPx() else 2.dp.toPx()
                        drawLine(
                            color = color,
                            start = Offset(lineX, i * stepY),
                            end = Offset(lineX, (i + 1) * stepY),
                            strokeWidth = strokeWidth
                        )
                    }
                }"""
canvas_new = """                // Draw path line
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lineX = 12.dp.toPx()
                    val startY = 12.dp.toPx()
                    val endY = size.height - 12.dp.toPx()
                    val stepY = (endY - startY) / (chapter.sectors.size.coerceAtLeast(2) - 1)
                    
                    for (i in 0 until chapter.sectors.size - 1) {
                        val color = if (i < state.sectorIndex) Cyan else Color.Gray.copy(alpha = 0.5f)
                        val strokeWidth = if (i < state.sectorIndex) 4.dp.toPx() else 2.dp.toPx()
                        drawLine(
                            color = color,
                            start = Offset(lineX, startY + i * stepY),
                            end = Offset(lineX, startY + (i + 1) * stepY),
                            strokeWidth = strokeWidth
                        )
                    }
                }"""
code = code.replace(canvas_old, canvas_new)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

print("Canvas path line fixed")
