import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

new_methods = """
    fun patchNode(id: Int) {
        _uiState.update { state ->
            if (state.mode != GameMode.PATCHING) return@update state
            val newNodes = state.leakNodes.map { if (it.id == id) it.copy(fixed = true) else it }
            SynthAudioEngine.playTone(1200f, 0.1f)
            
            var newMode = state.mode
            var newHull = state.hull
            if (newNodes.all { it.fixed }) {
                newMode = GameMode.RUNNING
                newHull = kotlin.math.min(100f, state.hull + 20f)
                SynthAudioEngine.playTone(880f, 0.4f)
            }
            
            state.copy(leakNodes = newNodes, mode = newMode, hull = newHull)
        }
    }
    
    fun updatePatching(dt: Float) {
        _uiState.update { state ->
            if (state.mode != GameMode.PATCHING) return@update state
            
            val newNodes = state.leakNodes.map { node ->
                if (node.fixed) {
                     node
                } else {
                     var nx = node.x + node.vx * dt
                     var ny = node.y + node.vy * dt
                     var nvx = node.vx
                     var nvy = node.vy
                     if (nx < 0.1f || nx > 0.9f) nvx = -nvx
                     if (ny < 0.1f || ny > 0.9f) nvy = -nvy
                     nx = nx.coerceIn(0.1f, 0.9f)
                     ny = ny.coerceIn(0.1f, 0.9f)
                     node.copy(x = nx, y = ny, vx = nvx, vy = nvy)
                }
            }
            
            state.copy(leakNodes = newNodes)
        }
    }
"""

if "fun patchNode" not in code:
    code = code.replace("    fun updateGame(dt: Float) {", new_methods + "\n    fun updateGame(dt: Float) {")

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

print("GameViewModel updatePatching added")
