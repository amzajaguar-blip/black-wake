import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

methods = """
    fun toggleDamageControl() {
        _uiState.update { it.copy(showDamageControl = !it.showDamageControl) }
    }
    
    fun toggleCompartmentSeal(id: String) {
        _uiState.update { state ->
            val updated = state.compartments.map { comp ->
                if (comp.id == id) {
                    when (comp.state) {
                        CompartmentState.NORMAL -> comp.copy(state = CompartmentState.SEALED)
                        CompartmentState.BREACHED -> comp.copy(state = CompartmentState.SEALED)
                        CompartmentState.SEALED -> comp.copy(state = CompartmentState.NORMAL) // Or leave sealed if breached? Let's allow unsealing to normal if they want, but if it was breached, unsealing it would re-breach. Let's keep it simple: unsealing goes to NORMAL.
                    }
                } else comp
            }
            state.copy(compartments = updated)
        }
    }
"""

code = code.replace("    fun toggleTacticalMap() {", methods + "\n    fun toggleTacticalMap() {")

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

print("Damage Control methods added")
