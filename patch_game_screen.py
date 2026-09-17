import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

game_loop_code = """
@Composable
fun GameLoop(state: GameState, viewModel: GameViewModel) {
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
}
"""

# Append GameLoop to the bottom
code += game_loop_code

# Inject GameLoop into GameScreen
old_screen = """fun GameScreen(viewModel: GameViewModel) {
    val state by viewModel.uiState.collectAsState()
    var showProfiler by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize().background(SeaColor)) {"""

new_screen = """fun GameScreen(viewModel: GameViewModel) {
    val state by viewModel.uiState.collectAsState()
    var showProfiler by remember { mutableStateOf(false) }
    
    GameLoop(state, viewModel)
    
    Box(modifier = Modifier.fillMaxSize().background(SeaColor)) {"""
code = code.replace(old_screen, new_screen)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)
print("GameScreen patched")
