import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

draw_old = """            if (state.mode == GameMode.PATCHING) {
                PatchingMiniGameOverlay(state, viewModel)
            }
        }"""
draw_new = """            if (state.mode == GameMode.PATCHING) {
                PatchingMiniGameOverlay(state, viewModel)
            }
            if (state.showTacticalMap && state.mode != GameMode.PATCHING) {
                TacticalMapOverlay(state)
            }
        }"""
code = code.replace(draw_old, draw_new)

overlay_code = """
@Composable
fun TacticalMapOverlay(state: GameState) {
    val chapter = CHAPTERS[state.currentChapterIndex]
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = false) {} // block clicks behind
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight(0.8f)
                .fillMaxWidth(0.8f)
                .border(2.dp, Cyan)
                .background(Color(0xFF031A33).copy(alpha = 0.9f))
                .padding(24.dp)
        ) {
            Text("MAPPA TATTICA // TRAIETTORIA", color = Cyan, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(8.dp))
            Text(chapter.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(modifier = Modifier.fillMaxSize()) {
                // Draw path line
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
                }
                
                // Draw nodes
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    chapter.sectors.forEachIndexed { index, sector ->
                        val isCurrent = index == state.sectorIndex
                        val isPast = index < state.sectorIndex
                        val nodeColor = if (isCurrent) Amber else if (isPast) Cyan else Color.Gray
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(if (isCurrent) nodeColor.copy(alpha=0.3f) else Color.Transparent, androidx.compose.foundation.shape.CircleShape)
                                    .border(2.dp, nodeColor, androidx.compose.foundation.shape.CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (sector.extract) {
                                    Text("E", color = nodeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                } else if (sector.fork) {
                                    Text("?", color = nodeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Box(modifier = Modifier.size(8.dp).background(nodeColor, androidx.compose.foundation.shape.CircleShape))
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column {
                                Text(
                                    text = sector.title, 
                                    color = if (isCurrent) Color.White else if (isPast) Color.LightGray else Color.Gray,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (sector.extract) "OBJ: ESTRAZIONE" else if (sector.fork) "OBJ: BIVIO TATTICO" else "NAV: TRANSIZIONE",
                                    color = if (isCurrent) Amber else nodeColor.copy(alpha = 0.8f),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
"""

if "fun TacticalMapOverlay" not in code:
    code = code + overlay_code

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

print("TacticalMapOverlay added to GameScreen")
