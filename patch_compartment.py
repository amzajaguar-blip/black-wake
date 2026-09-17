import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

comps_classes = """
enum class CompartmentState { NORMAL, SEALED, BREACHED }
data class Compartment(val id: String, val name: String, val state: CompartmentState = CompartmentState.NORMAL)
"""

if "enum class CompartmentState" not in code:
    code = code.replace("data class LeakNode(", comps_classes + "\ndata class LeakNode(")

state_old = """    val showTacticalMap: Boolean = false,"""
state_new = """    val showTacticalMap: Boolean = false,
    val showDamageControl: Boolean = false,
    val compartments: List<Compartment> = listOf(
        Compartment("BOW", "PRUA"),
        Compartment("MID", "CENTRO NAVE"),
        Compartment("AFT", "POPPA"),
        Compartment("ENG", "MOTORI")
    ),"""
code = code.replace(state_old, state_new)

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

print("GameViewModel Compartment state added")
