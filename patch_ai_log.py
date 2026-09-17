import sys

with open("app/src/main/java/com/blackwake/game/PursuerAI.kt", "r") as f:
    code = f.read()

old_package = "package com.blackwake.game\n"
new_package = """package com.blackwake.game

import android.util.Log
"""
code = code.replace(old_package, new_package)

change_state_func = """    var timer: Float = 0f

    private fun changeState(newState: PursuerState) {
        if (this.state != newState) {
            val timestamp = System.currentTimeMillis()
            Log.d("PursuerAI", "State transition: ${this.state} -> $newState at time $timestamp")
            this.state = newState
            this.timer = 0f
        }
    }
"""
code = code.replace("    var timer: Float = 0f\n", change_state_func)

# Replace all state assignments with changeState
code = code.replace("""                    state = PursuerState.DETECTED
                    timer = 0f""", "                    changeState(PursuerState.DETECTED)")
code = code.replace("""                    state = PursuerState.PURSUIT
                    timer = 0f""", "                    changeState(PursuerState.PURSUIT)")
code = code.replace("""                    state = PursuerState.SEARCH
                    timer = 0f""", "                    changeState(PursuerState.SEARCH)")
code = code.replace("""                    state = PursuerState.INTERCEPT""", "                    changeState(PursuerState.INTERCEPT)")
code = code.replace("""                    state = PursuerState.SEARCH_AGAIN
                    timer = 0f""", "                    changeState(PursuerState.SEARCH_AGAIN)")
code = code.replace("""                    state = PursuerState.LOST_TARGET""", "                    changeState(PursuerState.LOST_TARGET)")

data_class = """

data class PursuerSaveState(
    val id: Int,
    val type: String,
    val x: Float,
    val y: Float,
    val velocityX: Float,
    val velocityY: Float,
    val state: String,
    val roll: Float,
    val timer: Float,
    val health: Float,
    val targetX: Float
) {
    fun toCsv(): String {
        return "$id,$type,$x,$y,$velocityX,$velocityY,$state,$roll,$timer,$health,$targetX"
    }

    companion object {
        fun fromCsv(csv: String): PursuerSaveState? {
            val parts = csv.split(",")
            if (parts.size != 11) return null
            return try {
                PursuerSaveState(
                    id = parts[0].toInt(),
                    type = parts[1],
                    x = parts[2].toFloat(),
                    y = parts[3].toFloat(),
                    velocityX = parts[4].toFloat(),
                    velocityY = parts[5].toFloat(),
                    state = parts[6],
                    roll = parts[7].toFloat(),
                    timer = parts[8].toFloat(),
                    health = parts[9].toFloat(),
                    targetX = parts[10].toFloat()
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
"""
code = code + data_class

with open("app/src/main/java/com/blackwake/game/PursuerAI.kt", "w") as f:
    f.write(code)

print("PursuerAI logged and datastructure added")
