import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

trigger_old = """        if (newHull <= 0f) {
            finishRun(false, "Lo scafo non regge. La prova resta sotto la marea.")
            return
        }
        
        _uiState.update { s ->
            s.copy(
                fuel = newFuel,"""
trigger_new = """        if (newHull <= 0f) {
            finishRun(false, "Lo scafo non regge. La prova resta sotto la marea.")
            return
        }
        
        var finalMode = state.mode
        var finalHasTriggered = state.hasTriggeredPatching
        var finalLeakNodes = state.leakNodes
        
        if (newHull < 30f && !state.hasTriggeredPatching && newHull > 0f) {
            finalMode = GameMode.PATCHING
            finalHasTriggered = true
            val r = java.util.Random()
            finalLeakNodes = (1..5).map { i ->
                LeakNode(
                    id = i,
                    x = 0.2f + r.nextFloat() * 0.6f,
                    y = 0.2f + r.nextFloat() * 0.6f,
                    vx = (r.nextFloat() - 0.5f) * 0.6f,
                    vy = (r.nextFloat() - 0.5f) * 0.6f
                )
            }
            SynthAudioEngine.playTone(800f, 0.5f)
        }
        
        _uiState.update { s ->
            s.copy(
                mode = finalMode,
                hasTriggeredPatching = finalHasTriggered,
                leakNodes = finalLeakNodes,
                fuel = newFuel,"""
code = code.replace(trigger_old, trigger_new)

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

print("GameViewModel minigame trigger patched")
