import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

mode_old = "MENU, BRIEFING, RUNNING, PAUSED, GARAGE, DEBRIEF"
mode_new = "MENU, BRIEFING, RUNNING, PAUSED, GARAGE, DEBRIEF, PATCHING"
code = code.replace(mode_old, mode_new)

state_old = """    val flashColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Transparent,"""
state_new = """    val flashColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Transparent,
    
    val hasTriggeredPatching: Boolean = false,
    val leakNodes: List<LeakNode> = emptyList(),"""

if "data class LeakNode" not in code:
    leak_node_code = """
data class LeakNode(
    val id: Int,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val fixed: Boolean = false
)

data class GameState("""
    code = code.replace("data class GameState(", leak_node_code)

code = code.replace(state_old, state_new)

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

print("GameViewModel patched")
