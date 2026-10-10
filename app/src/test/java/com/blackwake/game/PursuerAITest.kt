package com.blackwake.game

import org.junit.Assert.assertEquals
import org.junit.Test

class PursuerAITest {

    @Test
    fun patrolRequiresDetectionThresholdThenEntersPursuit() {
        val ai = PursuerAI(EnemyBoatType.PATROL)

        ai.update(
            dt = 0.1f,
            detectionPercentage = 0.69f,
            distanceY = 40f,
            isPlayerSilent = false
        )
        assertEquals(PursuerState.PATROL, ai.state)

        ai.update(
            dt = 0.1f,
            detectionPercentage = 0.70f,
            distanceY = 40f,
            isPlayerSilent = false
        )
        assertEquals(PursuerState.DETECTED, ai.state)

        ai.update(
            dt = 0.51f,
            detectionPercentage = 0.70f,
            distanceY = 40f,
            isPlayerSilent = false
        )
        assertEquals(PursuerState.PURSUIT, ai.state)
    }

    @Test
    fun interceptorUsesCloseRangeInterceptState() {
        val ai = PursuerAI(EnemyBoatType.INTERCEPTOR, PursuerState.PURSUIT)

        ai.update(
            dt = 0.1f,
            detectionPercentage = 0.9f,
            distanceY = 10f,
            isPlayerSilent = false
        )

        assertEquals(PursuerState.INTERCEPT, ai.state)
    }

    @Test
    fun silentEscapeProgressesToLostTarget() {
        val ai = PursuerAI(EnemyBoatType.HUNTER, PursuerState.PURSUIT)

        ai.update(
            dt = 0.1f,
            detectionPercentage = 0.1f,
            distanceY = 40f,
            isPlayerSilent = true
        )
        assertEquals(PursuerState.SEARCH, ai.state)

        ai.update(
            dt = 3.1f,
            detectionPercentage = 0.1f,
            distanceY = 40f,
            isPlayerSilent = true
        )
        assertEquals(PursuerState.SEARCH_AGAIN, ai.state)

        ai.update(
            dt = 2.1f,
            detectionPercentage = 0.1f,
            distanceY = 40f,
            isPlayerSilent = true
        )
        assertEquals(PursuerState.LOST_TARGET, ai.state)
    }

    @Test
    fun difficultyMultiplierLowersDetectionThreshold() {
        val ai = PursuerAI(EnemyBoatType.ELITE)

        ai.update(
            dt = 0.1f,
            detectionPercentage = 0.5f,
            distanceY = 40f,
            isPlayerSilent = false,
            difficultyMultiplier = 2f
        )

        assertEquals(PursuerState.DETECTED, ai.state)
    }
}
