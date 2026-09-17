import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

state_old = """    val showDamageControl: Boolean = false,"""
state_new = """    val showDamageControl: Boolean = false,
    val isAudioMuted: Boolean = false,"""
code = code.replace(state_old, state_new)

methods_old = """    fun toggleDamageControl() {
        _uiState.update { it.copy(showDamageControl = !it.showDamageControl) }
    }"""
methods_new = """    fun toggleDamageControl() {
        _uiState.update { it.copy(showDamageControl = !it.showDamageControl) }
    }
    
    fun toggleAudioMute() {
        _uiState.update { state -> 
            val newMuted = !state.isAudioMuted
            SynthAudioEngine.isMuted = newMuted
            state.copy(isAudioMuted = newMuted) 
        }
    }"""
code = code.replace(methods_old, methods_new)

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

