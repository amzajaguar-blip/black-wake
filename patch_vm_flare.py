import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

state_old = """    val isAudioMuted: Boolean = false,"""
state_new = """    val isAudioMuted: Boolean = false,
    val flareCharges: Int = 3,
    val flareActiveTimer: Float = 0f,"""
code = code.replace(state_old, state_new)

methods_old = """    fun toggleAudioMute() {
        _uiState.update { state -> 
            val newMuted = !state.isAudioMuted
            SynthAudioEngine.isMuted = newMuted
            state.copy(isAudioMuted = newMuted) 
        }
    }"""
methods_new = """    fun toggleAudioMute() {
        _uiState.update { state -> 
            val newMuted = !state.isAudioMuted
            SynthAudioEngine.isMuted = newMuted
            state.copy(isAudioMuted = newMuted) 
        }
    }
    
    fun launchFlare() {
        _uiState.update { state ->
            if (state.flareCharges > 0 && state.flareActiveTimer <= 0f && !state.isSubmerged) {
                SynthAudioEngine.playExplosion() // Sound effect for flare
                state.copy(
                    flareCharges = state.flareCharges - 1,
                    flareActiveTimer = 5f, // 5 seconds of visibility
                    screenFlash = 0.5f,
                    flashColor = androidx.compose.ui.graphics.Color(0xAAFFFFAA)
                )
            } else state
        }
    }"""
code = code.replace(methods_old, methods_new)

update_old = """        var newSubmerged = isDiveHeld && canSubmerge"""
update_new = """        var newSubmerged = isDiveHeld && canSubmerge
        var newFlareActiveTimer = max(0f, state.flareActiveTimer - dt)"""
code = code.replace(update_old, update_new)

state_update_old = """                screenFlash = newScreenFlash,"""
state_update_new = """                screenFlash = newScreenFlash,
                flareActiveTimer = newFlareActiveTimer,"""
code = code.replace(state_update_old, state_update_new)

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

