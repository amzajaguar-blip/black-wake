import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

on_cleared = """
    override fun onCleared() {
        super.onCleared()
        if (_uiState.value.mode == GameMode.RUNNING || _uiState.value.mode == GameMode.PAUSED) {
            savePursuers()
        }
    }
"""
code = code.replace("    fun togglePause() {", on_cleared + "    fun togglePause() {")

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

print("onCleared patched")
