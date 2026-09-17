import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

hud_old = """fun Hud(state: GameState, viewModel: GameViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {"""
hud_new = """fun Hud(state: GameState, viewModel: GameViewModel) {
    val isHullCritical = state.hull < 25f
    val hudPulseAlpha = if (isHullCritical) (kotlin.math.sin(state.runElapsed * 12f) * 0.4f + 0.3f).toFloat() else 0f
    
    Box(modifier = Modifier.fillMaxSize().border(if (isHullCritical) 4.dp else 0.dp, Red.copy(alpha = hudPulseAlpha))) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {"""
code = code.replace(hud_old, hud_new)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

