import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

draw_old = """        if (state.mode == GameMode.RUNNING || state.mode == GameMode.PAUSED || state.mode == GameMode.DEBRIEF) {
            GameCanvas(state, viewModel)
            Hud(state, viewModel)
            if (showProfiler) {
                ProfilerDashboard(state)
            }
        }"""
draw_new = """        if (state.mode == GameMode.RUNNING || state.mode == GameMode.PAUSED || state.mode == GameMode.DEBRIEF || state.mode == GameMode.PATCHING) {
            GameCanvas(state, viewModel)
            Hud(state, viewModel)
            if (showProfiler) {
                ProfilerDashboard(state)
            }
            if (state.mode == GameMode.PATCHING) {
                PatchingMiniGameOverlay(state, viewModel)
            }
        }"""
code = code.replace(draw_old, draw_new)


overlay_composable = """
@Composable
fun PatchingMiniGameOverlay(state: GameState, viewModel: GameViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "EMERGENZA SCAFO - CHIUDI LE FALLE!",
                color = Red,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
        
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val w = maxWidth.value
            val h = maxHeight.value
            state.leakNodes.forEach { node ->
                if (!node.fixed) {
                    Box(
                        modifier = Modifier
                            .offset(x = (node.x * w).dp - 24.dp, y = (node.y * h).dp - 24.dp)
                            .size(48.dp)
                            .background(Red.copy(alpha = 0.8f), androidx.compose.foundation.shape.CircleShape)
                            .border(2.dp, Color.White, androidx.compose.foundation.shape.CircleShape)
                            .androidx.compose.foundation.clickable {
                                viewModel.patchNode(node.id)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("PATCH", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .offset(x = (node.x * w).dp - 24.dp, y = (node.y * h).dp - 24.dp)
                            .size(48.dp)
                            .background(Color.Green.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
                            .border(2.dp, Color.Green, androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material.icons.Icons.Default.let {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                contentDescription = "Fixed",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
"""

if "fun PatchingMiniGameOverlay" not in code:
    code = code + overlay_composable

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

print("GameScreen patched with overlay")
