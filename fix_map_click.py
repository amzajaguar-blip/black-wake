import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

overlay_old = """@Composable
fun TacticalMapOverlay(state: GameState) {"""
overlay_new = """@Composable
fun TacticalMapOverlay(state: GameState, viewModel: GameViewModel) {"""
code = code.replace(overlay_old, overlay_new)

call_old = """            if (state.showTacticalMap && state.mode != GameMode.PATCHING) {
                TacticalMapOverlay(state)
            }"""
call_new = """            if (state.showTacticalMap && state.mode != GameMode.PATCHING) {
                TacticalMapOverlay(state, viewModel)
            }"""
code = code.replace(call_old, call_new)

click_old = """.clickable(enabled = false) {} // block clicks behind"""
click_new = """.clickable { viewModel.toggleTacticalMap() }"""
code = code.replace(click_old, click_new)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

print("Map click to close fixed")
