import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

# 1. Add showProfiler state
old_screen = """fun GameScreen(viewModel: GameViewModel) {
    val state by viewModel.uiState.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize().background(SeaColor)) {"""

new_screen = """fun GameScreen(viewModel: GameViewModel) {
    val state by viewModel.uiState.collectAsState()
    var showProfiler by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize().background(SeaColor)) {"""
code = code.replace(old_screen, new_screen)

# 2. Replace DebugOverlay call and add button
old_overlay = """            Hud(state, viewModel)
            DebugOverlay(state)
        }"""

new_overlay = """            Hud(state, viewModel)
            if (showProfiler) {
                ProfilerDashboard(state)
            }
        }
        
        Button(
            onClick = { showProfiler = !showProfiler },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.5f)),
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 80.dp, end = 16.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Text("PROFILER", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        }"""
code = code.replace(old_overlay, new_overlay)

# 3. Replace DebugOverlay definition with ProfilerDashboard
old_func = """@Composable
fun DebugOverlay(state: GameState) {
    val fps = if (state.lastDeltaTime > 0f) (1f / state.lastDeltaTime).toInt() else 0
    val lane = kotlin.math.round(state.playerX).toInt()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp, start = 16.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(8.dp)
        ) {
            Text("DEBUG OVERLAY", color = Color.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("FPS: $fps (dt: ${String.format("%.3f", state.lastDeltaTime)})", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("LANE: $lane", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("POS X: ${String.format("%.3f", state.playerX)}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("VEL X: ${String.format("%.3f", state.playerVelocityX)}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("ROLL : ${String.format("%.3f", state.playerRoll)}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("ENTITIES: ${state.entities.size}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}"""

new_func = """@Composable
fun ProfilerDashboard(state: GameState) {
    val fps = if (state.lastDeltaTime > 0f) (1f / state.lastDeltaTime).toInt() else 0
    val lane = kotlin.math.round(state.playerX).toInt()
    
    val runtime = Runtime.getRuntime()
    val usedMemMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    val maxMemMb = runtime.maxMemory() / (1024 * 1024)
    
    // Fixed array for frame history
    val historySize = 60
    val frameHistory = remember { FloatArray(historySize) }
    val historyIndex = remember { mutableIntStateOf(0) }
    
    // Zero-allocation path for graph
    val graphPath = remember { Path() }
    
    // Update history tracking
    LaunchedEffect(state.runElapsed) {
        if (state.lastDeltaTime > 0f) {
            frameHistory[historyIndex.intValue % historySize] = state.lastDeltaTime
            historyIndex.intValue++
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp, start = 16.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.85f))
                .border(1.dp, Color.Green.copy(alpha = 0.3f))
                .padding(12.dp)
                .width(220.dp)
        ) {
            Text("PERFORMANCE PROFILER", color = Color.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("FPS: $fps (dt: ${String.format("%.3f", state.lastDeltaTime)}s)", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("MEM: $usedMemMb MB / $maxMemMb MB", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("ENTITIES: ${state.entities.size}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            
            Spacer(Modifier.height(12.dp))
            Text("FRAME TIME (TARGET: 16ms)", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(4.dp))
            
            Canvas(modifier = Modifier.fillMaxWidth().height(50.dp).background(Color(0xFF0F1517))) {
                val w = size.width
                val h = size.height
                val stepX = w / (historySize - 1).toFloat()
                val maxDt = 0.05f // scale up to 50ms
                
                // Draw Target Line (16.6ms)
                val y16 = h - (0.0166f / maxDt) * h
                drawLine(Color.Green.copy(alpha=0.5f), Offset(0f, y16), Offset(w, y16), strokeWidth = 1f)
                
                graphPath.reset()
                for (i in 0 until historySize) {
                    val idx = (historyIndex.intValue + i) % historySize
                    val dt = frameHistory[idx]
                    val x = i * stepX
                    val y = h - (dt / maxDt).coerceIn(0f, 1f) * h
                    
                    if (i == 0) {
                        graphPath.moveTo(x, y)
                    } else {
                        graphPath.lineTo(x, y)
                    }
                }
                drawPath(graphPath, Color.Cyan, style = Stroke(width = 2f))
            }
            
            Spacer(Modifier.height(12.dp))
            Text("PHYSICS STATE", color = Color.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("POS X: ${String.format("%.3f", state.playerX)} [Lane $lane]", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("VEL X: ${String.format("%.3f", state.playerVelocityX)}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("ROLL : ${String.format("%.3f", state.playerRoll)}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("FOV  : ${String.format("%.3f", state.fovOffset)}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}"""
code = code.replace(old_func, new_func)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

print("Profiler added successfully.")
