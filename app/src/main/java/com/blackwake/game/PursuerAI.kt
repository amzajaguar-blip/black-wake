package com.blackwake.game

import android.util.Log

class PursuerAI(
    val type: EnemyBoatType,
    var state: PursuerState = PursuerState.PATROL
) {
    var timer: Float = 0f

    private fun changeState(newState: PursuerState) {
        if (this.state != newState) {
            val timestamp = System.currentTimeMillis()
            Log.d("PursuerAI", "State transition: ${this.state} -> $newState at time $timestamp")
            this.state = newState
            this.timer = 0f
        }
    }

    fun update(
        dt: Float,
        detectionPercentage: Float,
        distanceY: Float,
        isPlayerSilent: Boolean,
        difficultyMultiplier: Float = 1.0f
    ) {
        timer += dt

        when (state) {
            PursuerState.PATROL -> {
                // Transition from PATROL to PURSUIT based on Detection percentage
                val threshold = (0.7f / difficultyMultiplier).coerceIn(0.1f, 1.0f)
                if (detectionPercentage >= threshold) {
                    changeState(PursuerState.DETECTED)
                }
            }
            PursuerState.DETECTED -> {
                if (timer > 0.5f) {
                    changeState(PursuerState.PURSUIT)
                }
            }
            PursuerState.PURSUIT -> {
                val searchThreshold = (0.6f / difficultyMultiplier).coerceIn(0.1f, 1.0f)
                if (isPlayerSilent && kotlin.math.abs(distanceY) > 30f && detectionPercentage < searchThreshold) {
                    changeState(PursuerState.SEARCH)
                } else if (kotlin.math.abs(distanceY) < 15f && type == EnemyBoatType.INTERCEPTOR) {
                    changeState(PursuerState.INTERCEPT)
                }
            }
            PursuerState.INTERCEPT -> {
                val searchThreshold = (0.6f / difficultyMultiplier).coerceIn(0.1f, 1.0f)
                if (isPlayerSilent && kotlin.math.abs(distanceY) > 30f && detectionPercentage < searchThreshold) {
                    changeState(PursuerState.SEARCH)
                } else if (kotlin.math.abs(distanceY) >= 15f) {
                    changeState(PursuerState.PURSUIT)
                }
            }
            PursuerState.SEARCH -> {
                if (timer > 3f) {
                    changeState(PursuerState.SEARCH_AGAIN)
                }
                val pursuitThreshold = (0.6f / difficultyMultiplier).coerceIn(0.1f, 1.0f)
                if (!isPlayerSilent || detectionPercentage > pursuitThreshold) {
                    changeState(PursuerState.PURSUIT)
                }
            }
            PursuerState.SEARCH_AGAIN -> {
                if (timer > 2f) {
                    changeState(PursuerState.LOST_TARGET)
                }
                val pursuitThreshold = (0.6f / difficultyMultiplier).coerceIn(0.1f, 1.0f)
                if (!isPlayerSilent || detectionPercentage > pursuitThreshold) {
                    changeState(PursuerState.PURSUIT)
                }
            }
            PursuerState.ATTACK -> {
                // Logic for attack state
            }
            PursuerState.LOST_TARGET -> {
                // Terminal state, handled by external cleanup
            }
        }
    }
}


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
