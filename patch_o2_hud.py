import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

hud_old = """                // Oxygen Bar
                Text("O2: ${state.oxygen.toInt()}%", color = if (state.oxygen > 30) Cyan else Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.size(100.dp, 8.dp).background(Color.DarkGray).border(1.dp, Color.Black)) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(state.oxygen / 100f).background(if (state.oxygen > 30) Cyan else Red))
                }"""
hud_new = """                // Oxygen Bar
                val o2Critical = state.oxygen < 20f
                val o2Flash = if (o2Critical) (kotlin.math.sin(state.runElapsed * 15f) * 0.5f + 0.5f).toFloat() else 1f
                val o2Color = if (state.oxygen > 30) Cyan else Red
                Text("O2: ${state.oxygen.toInt()}%", color = o2Color.copy(alpha = o2Flash), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.size(100.dp, 8.dp).background(Color.DarkGray).border(1.dp, if (o2Critical) Red.copy(alpha = o2Flash) else Color.Black)) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(state.oxygen / 100f).background(o2Color.copy(alpha = o2Flash)))
                }"""
code = code.replace(hud_old, hud_new)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

print("GameScreen O2 flash added")
