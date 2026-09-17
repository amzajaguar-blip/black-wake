import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

hud_sonar_text = """        }
        
        Text(
            text = "INTEL: ${state.intel}",
            color = Cyan,
            fontSize = 18.sp,"""

new_hud_sonar_text = """        }
        
        // Sonar UI
        val sonarMax = 1 + state.modules.radar
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "SNR",
                color = if (state.sonarCharges > 0) Cyan else Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 0 until sonarMax) {
                    Box(
                        modifier = Modifier
                            .size(width = 16.dp, height = 8.dp)
                            .background(if (i < state.sonarCharges) Cyan else Color.DarkGray)
                            .border(1.dp, Cyan.copy(alpha = 0.5f))
                    )
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = "INTEL: ${state.intel}",
            color = Cyan,
            fontSize = 18.sp,"""

if hud_sonar_text in code:
    code = code.replace(hud_sonar_text, new_hud_sonar_text)
    with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
        f.write(code)
    print("Sonar HUD patched")
else:
    print("Failed to find HUD text")
