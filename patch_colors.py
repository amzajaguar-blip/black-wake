import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

# Replace SeaColor
old_colors = """val Cyan = Color(0xFF2BE7E0)
val Amber = Color(0xFFFFB45F)
val Red = Color(0xFFEF464B)
val DarkCyan = Color(0xFF071E25)
val SeaColor = Color(0xFF041018)"""

new_colors = """val Cyan = Color(0xFF2BE7E0)
val Amber = Color(0xFFFFB45F)
val Red = Color(0xFFEF464B)
val DarkCyan = Color(0xFF071E25)
val SeaColor = Color(0xFF07456F) // Deep Blue Ocean
val StormColor = Color(0xFF031A33) // Stormy Blue
val BlackTideColor = Color(0xFF010A14) // Navy Black"""
code = code.replace(old_colors, new_colors)

# Replace sky rendering if any
old_sky = """val skyTint = androidx.compose.ui.graphics.lerp(Color(0xFF02070A), Color(0xFF2A0A0A), (state.detection / 0.8f).coerceIn(0f, 1f))
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Black, skyTint),"""

new_sky = """val skyTint = androidx.compose.ui.graphics.lerp(Color(0xFF041830), Color(0xFF100520), (state.detection / 0.8f).coerceIn(0f, 1f))
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color(0xFF01050A), skyTint),"""
code = code.replace(old_sky, new_sky)

# Replace waterTint
old_waterTint = """val tensionColor = androidx.compose.ui.graphics.lerp(SeaColor, Color(0xFF0F1517), (state.detection / 0.75f).coerceIn(0f, 1f))
    val waterTint = androidx.compose.ui.graphics.lerp(tensionColor, Color(0xFF1A0505), ((state.detection - 0.75f) / 0.25f).coerceIn(0f, 1f))"""

new_waterTint = """val tensionColor = androidx.compose.ui.graphics.lerp(SeaColor, StormColor, (state.detection / 0.75f).coerceIn(0f, 1f))
    val waterTint = androidx.compose.ui.graphics.lerp(tensionColor, BlackTideColor, ((state.detection - 0.75f) / 0.25f).coerceIn(0f, 1f))"""
code = code.replace(old_waterTint, new_waterTint)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

print("GameScreen colors patched")
