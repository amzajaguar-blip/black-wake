import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

loop_old = """fun GameLoop(state: GameState, viewModel: GameViewModel) {
    if (state.mode == GameMode.RUNNING) {
        LaunchedEffect(Unit) {
            var lastFrameTime = withFrameNanos { it }
            while (isActive) {
                val frameTime = withFrameNanos { it }
                val delta = (frameTime - lastFrameTime) / 1_000_000_000f
                lastFrameTime = frameTime
                val safeDelta = kotlin.math.min(delta, 0.1f)
                viewModel.updateGame(safeDelta)
            }
        }
    }
}"""
loop_new = """fun GameLoop(state: GameState, viewModel: GameViewModel) {
    if (state.mode == GameMode.RUNNING || state.mode == GameMode.PATCHING) {
        LaunchedEffect(state.mode) {
            var lastFrameTime = withFrameNanos { it }
            while (isActive) {
                val frameTime = withFrameNanos { it }
                val delta = (frameTime - lastFrameTime) / 1_000_000_000f
                lastFrameTime = frameTime
                val safeDelta = kotlin.math.min(delta, 0.1f)
                if (state.mode == GameMode.RUNNING) {
                    viewModel.updateGame(safeDelta)
                } else if (state.mode == GameMode.PATCHING) {
                    viewModel.updatePatching(safeDelta)
                }
            }
        }
    }
}"""

code = code.replace(loop_old, loop_new)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

print("GameScreen loop patched")
