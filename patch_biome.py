import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

old_tint = """    val tensionColor = androidx.compose.ui.graphics.lerp(SeaColor, StormColor, (state.detection / 0.75f).coerceIn(0f, 1f))
    val waterTint = androidx.compose.ui.graphics.lerp(tensionColor, BlackTideColor, ((state.detection - 0.75f) / 0.25f).coerceIn(0f, 1f))"""

new_tint = """    // Biome colors
    val baseOcean = when (state.currentChapterIndex % 3) {
        0 -> SeaColor // Deep Blue Ocean
        1 -> Color(0xFF0F3B3A) // Swamp/Greenish Open Water
        else -> Color(0xFF140F30) // Night Ocean (Purple-ish Navy)
    }
    
    val tensionColor = androidx.compose.ui.graphics.lerp(baseOcean, StormColor, (state.detection / 0.75f).coerceIn(0f, 1f))
    val waterTint = androidx.compose.ui.graphics.lerp(tensionColor, BlackTideColor, ((state.detection - 0.75f) / 0.25f).coerceIn(0f, 1f))"""
code = code.replace(old_tint, new_tint)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)
print("Biome patched")
