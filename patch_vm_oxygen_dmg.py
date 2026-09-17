import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

hull_dmg_old = """        var newHull = state.hull
        if (state.fuel == 0f && newDrift <= 0f && newInvulnerable <= 0f) {
            newHull = max(0f, newHull - 2.3f * dt)
            if (newHull <= 0f) { finishRun(false, "Il mare entra nello scafo."); return }
        }"""
hull_dmg_new = """        var newHull = state.hull
        if (state.fuel == 0f && newDrift <= 0f && newInvulnerable <= 0f) {
            newHull = max(0f, newHull - 2.3f * dt)
            if (newHull <= 0f) { finishRun(false, "Il mare entra nello scafo."); return }
        }
        if (state.oxygen == 0f && newSubmerged) {
            newHull = max(0f, newHull - 15f * dt) // Severe damage when drowning
            if (newHull <= 0f) { finishRun(false, "Scafo imploso per mancanza d'ossigeno."); return }
            newScreenFlash = 0.3f
            _uiState.update { it.copy(flashColor = androidx.compose.ui.graphics.Color(0x55EF464B)) }
        }"""
code = code.replace(hull_dmg_old, hull_dmg_new)

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

print("GameViewModel patched with oxygen damage logic")
