import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

new_func = """    fun setPlayerBoat(b: PlayerBoatType) {
        _uiState.update { it.copy(playerBoatType = b) }
    }
}
"""

if "} // end of viewmodel" in code:
    print("Found end")
code = code.rsplit('}', 1)[0] + new_func

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

print("GameViewModel patched with setPlayerBoat")
