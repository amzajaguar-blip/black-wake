package com.blackwake.game

class PursuerAI(
    val type: EnemyBoatType,
    var state: PursuerState = PursuerState.PATROL
) {
    var timer: Float = 0f

    private fun changeState(newState: PursuerState) {
        if (this.state != newState) {
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
            PursuerState.LOST_TARGET -> {
                // Terminal state, handled by external cleanup
            }
        }
    }
}
