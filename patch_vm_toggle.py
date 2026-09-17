import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

method = """    fun toggleTacticalMap() {
        _uiState.update { it.copy(showTacticalMap = !it.showTacticalMap) }
    }
"""

code = code.replace("    fun patchNode(id: Int) {", method + "\n    fun patchNode(id: Int) {")

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

print("GameViewModel patched with toggleTacticalMap")
