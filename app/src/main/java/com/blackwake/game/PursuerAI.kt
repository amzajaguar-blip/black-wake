package com.blackwake.game

data class PursuerBehaviorProfile(
    val detectionThreshold: Float,
    val searchThreshold: Float,
    val reactionDelay: Float,
    val searchDuration: Float,
    val reacquireDuration: Float,
    val canIntercept: Boolean,
    val interceptDistance: Float
)

fun behaviorFor(type: EnemyBoatType): PursuerBehaviorProfile = when (type) {
    EnemyBoatType.PATROL -> PursuerBehaviorProfile(0.70f, 0.60f, 0.50f, 3.0f, 2.0f, false, 0f)
    EnemyBoatType.INTERCEPTOR -> PursuerBehaviorProfile(0.65f, 0.58f, 0.35f, 2.5f, 1.5f, true, 18f)
    EnemyBoatType.POLICE -> PursuerBehaviorProfile(0.65f, 0.58f, 0.45f, 3.0f, 2.0f, false, 0f)
    EnemyBoatType.HUNTER -> PursuerBehaviorProfile(0.55f, 0.50f, 0.30f, 4.5f, 2.5f, false, 0f)
    EnemyBoatType.ARMORED -> PursuerBehaviorProfile(0.72f, 0.48f, 0.60f, 4.0f, 2.0f, false, 0f)
    EnemyBoatType.ELITE -> PursuerBehaviorProfile(0.50f, 0.45f, 0.25f, 4.0f, 2.0f, true, 20f)
}

class PursuerAI(
    val type: EnemyBoatType,
    var state: PursuerState = PursuerState.PATROL
) {
    var timer: Float = 0f
    private val profile = behaviorFor(type)

    private fun changeState(newState: PursuerState) {
        if (state != newState) {
            state = newState
            timer = 0f
        }
    }

    fun update(
        dt: Float,
        detectionPercentage: Float,
        distanceY: Float,
        isPlayerSilent: Boolean,
        difficultyMultiplier: Float = 1.0f
    ) {
        timer += dt.coerceAtLeast(0f)
        val difficulty = difficultyMultiplier.coerceAtLeast(0.1f)
        val detectionThreshold = (profile.detectionThreshold / difficulty).coerceIn(0.1f, 1f)
        val searchThreshold = (profile.searchThreshold / difficulty).coerceIn(0.1f, 1f)
        val distance = kotlin.math.abs(distanceY)

        when (state) {
            PursuerState.PATROL -> {
                if (detectionPercentage >= detectionThreshold) {
                    changeState(PursuerState.DETECTED)
                }
            }

            PursuerState.DETECTED -> {
                if (timer >= profile.reactionDelay) {
                    changeState(PursuerState.PURSUIT)
                }
            }

            PursuerState.PURSUIT -> {
                when {
                    isPlayerSilent && distance > 30f && detectionPercentage < searchThreshold ->
                        changeState(PursuerState.SEARCH)

                    profile.canIntercept && distance < profile.interceptDistance ->
                        changeState(PursuerState.INTERCEPT)
                }
            }

            PursuerState.INTERCEPT -> {
                when {
                    isPlayerSilent && distance > 30f && detectionPercentage < searchThreshold ->
                        changeState(PursuerState.SEARCH)

                    !profile.canIntercept || distance >= profile.interceptDistance ->
                        changeState(PursuerState.PURSUIT)
                }
            }

            PursuerState.SEARCH -> {
                if (!isPlayerSilent || detectionPercentage > searchThreshold) {
                    changeState(PursuerState.PURSUIT)
                } else if (timer >= profile.searchDuration) {
                    changeState(PursuerState.SEARCH_AGAIN)
                }
            }

            PursuerState.SEARCH_AGAIN -> {
                if (!isPlayerSilent || detectionPercentage > searchThreshold) {
                    changeState(PursuerState.PURSUIT)
                } else if (timer >= profile.reacquireDuration) {
                    changeState(PursuerState.LOST_TARGET)
                }
            }

            PursuerState.LOST_TARGET -> Unit
        }
    }
}
