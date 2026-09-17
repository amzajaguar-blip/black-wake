import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

# 1. Add Hull Shake
shake_old = """            // Camera Shake
            val tensionShake = if (state.detection > 0.85f) (state.detection - 0.85f) * 15f else 0f
            val totalShake = state.cameraShake + tensionShake"""
shake_new = """            // Camera Shake
            val tensionShake = if (state.detection > 0.85f) (state.detection - 0.85f) * 15f else 0f
            val hullShake = if (state.hull < 30f) (30f - state.hull) * 0.05f else 0f
            val totalShake = state.cameraShake + tensionShake + hullShake"""
code = code.replace(shake_old, shake_new)

# 2. Add Vignette overlay
flash_old = """        // Screen Flash Effect
        if (state.screenFlash > 0f) {
            Box(modifier = Modifier.fillMaxSize().background(state.flashColor.copy(alpha = state.screenFlash)))
        }
        
        when (state.mode) {"""
flash_new = """        // Screen Flash Effect
        if (state.screenFlash > 0f) {
            Box(modifier = Modifier.fillMaxSize().background(state.flashColor.copy(alpha = state.screenFlash)))
        }
        
        // Hull Damage Vignette (triggered when hull is low)
        if (state.hull < 40f && (state.mode == GameMode.RUNNING || state.mode == GameMode.PAUSED)) {
            val vignetteIntensity = (40f - state.hull) / 40f
            val pulse = (kotlin.math.sin(state.runElapsed * 5f) * 0.5f + 0.5f).toFloat()
            val alpha = vignetteIntensity * (0.4f + 0.6f * pulse)
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Transparent, Red.copy(alpha = alpha)),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width.coerceAtLeast(size.height) * 0.8f
                    )
                )
            }
        }
        
        when (state.mode) {"""
code = code.replace(flash_old, flash_new)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

print("GameScreen patched with damage vignette and shake")
