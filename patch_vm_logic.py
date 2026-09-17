import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

logic_old = """        if (state.oxygen == 0f && newSubmerged) {
            newHull = max(0f, newHull - 15f * dt) // Severe damage when drowning
            if (newHull <= 0f) { finishRun(false, "Scafo imploso per mancanza d'ossigeno."); return }
            newScreenFlash = 0.3f
            _uiState.update { it.copy(flashColor = androidx.compose.ui.graphics.Color(0x55EF464B)) }
        }"""
        
logic_new = """        if (state.oxygen == 0f && newSubmerged) {
            newHull = max(0f, newHull - 15f * dt) // Severe damage when drowning
            if (newHull <= 0f) { finishRun(false, "Scafo imploso per mancanza d'ossigeno."); return }
            newScreenFlash = 0.3f
            _uiState.update { it.copy(flashColor = androidx.compose.ui.graphics.Color(0x55EF464B)) }
        }
        
        var newCompartments = state.compartments
        val breachedCount = newCompartments.count { it.state == CompartmentState.BREACHED }
        if (breachedCount > 0) {
            newHull = max(0f, newHull - (breachedCount * 2.5f) * dt)
            if (state.runElapsed % 2f < dt) SynthAudioEngine.playTone(400f, 0.1f) // Warning beep for flooding
        }
        """
code = code.replace(logic_old, logic_new)


# Apply breach on collision
collision_logic_old = """                            newHull = max(0f, newHull - (dmg - state.modules.hull * 3f))
                            newInvulnerable = 0.86f"""
collision_logic_new = """                            val actualDmg = (dmg - state.modules.hull * 3f)
                            newHull = max(0f, newHull - actualDmg)
                            newInvulnerable = 0.86f
                            if (actualDmg > 10f) {
                                val normalComps = newCompartments.filter { it.state == CompartmentState.NORMAL }
                                if (normalComps.isNotEmpty()) {
                                    val toBreach = normalComps.random()
                                    newCompartments = newCompartments.map { if (it.id == toBreach.id) it.copy(state = CompartmentState.BREACHED) else it }
                                    flashMessage("FALLA: ${toBreach.name}", "red")
                                }
                            }"""
code = code.replace(collision_logic_old, collision_logic_new)

enemy_collision_old = """                            newHull = max(0f, newHull - (14f - state.modules.hull * 2f))
                            newInvulnerable = 0.86f"""
enemy_collision_new = """                            val actualDmg = (14f - state.modules.hull * 2f)
                            newHull = max(0f, newHull - actualDmg)
                            newInvulnerable = 0.86f
                            if (actualDmg > 10f && java.util.Random().nextBoolean()) {
                                val normalComps = newCompartments.filter { it.state == CompartmentState.NORMAL }
                                if (normalComps.isNotEmpty()) {
                                    val toBreach = normalComps.random()
                                    newCompartments = newCompartments.map { if (it.id == toBreach.id) it.copy(state = CompartmentState.BREACHED) else it }
                                    flashMessage("ALLAGAMENTO: ${toBreach.name}", "red")
                                }
                            }"""
code = code.replace(enemy_collision_old, enemy_collision_new)

pursuer_collision_old = """                    newHull = kotlin.math.max(0f, newHull - 15f)
                    newInvulnerable = 2f"""
pursuer_collision_new = """                    newHull = kotlin.math.max(0f, newHull - 15f)
                    newInvulnerable = 2f
                    val normalComps = newCompartments.filter { it.state == CompartmentState.NORMAL }
                    if (normalComps.isNotEmpty()) {
                        val toBreach = normalComps.random()
                        newCompartments = newCompartments.map { if (it.id == toBreach.id) it.copy(state = CompartmentState.BREACHED) else it }
                        flashMessage("ALLAGAMENTO: ${toBreach.name}", "red")
                    }"""
code = code.replace(pursuer_collision_old, pursuer_collision_new)


state_update_old = """                hull = newHull,
                invulnerable = newInvulnerable,"""
state_update_new = """                hull = newHull,
                compartments = newCompartments,
                invulnerable = newInvulnerable,"""
code = code.replace(state_update_old, state_update_new)


with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

print("Compartment game logic added")
