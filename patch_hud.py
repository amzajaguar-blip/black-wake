import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

hud_old = """                // Hull Bar
                Text("SCAFO: ${state.hull.toInt()}%", color = if (state.hull > 30) Color.Green else Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.size(100.dp, 8.dp).background(Color.DarkGray).border(1.dp, Color.Black)) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(state.hull / 100f).background(if (state.hull > 30) Color.Green else Red))
                }"""
hud_new = """                // Hull Bar
                val hullCritical = state.hull < 25f
                val hullFlash = if (hullCritical) (kotlin.math.sin(state.runElapsed * 18f) * 0.5f + 0.5f).toFloat() else 1f
                val hullColor = if (state.hull > 30) Color.Green else Red
                Text(if (hullCritical) "SCAFO CRITICO: ${state.hull.toInt()}%" else "SCAFO: ${state.hull.toInt()}%", color = hullColor.copy(alpha = hullFlash), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.size(100.dp, 8.dp).background(Color.DarkGray).border(1.dp, if (hullCritical) Red.copy(alpha = hullFlash) else Color.Black)) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(state.hull / 100f).background(hullColor.copy(alpha = hullFlash)))
                }"""
code = code.replace(hud_old, hud_new)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

print("HUD patched")
