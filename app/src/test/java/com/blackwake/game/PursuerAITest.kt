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
            dt = 4.6f,
            detectionPercentage = 0.1f,
            distanceY = 40f,
            isPlayerSilent = true
        )
        assertEquals(PursuerState.SEARCH_AGAIN, ai.state)

        ai.update(
            dt = 2.6f,
            detectionPercentage = 0.1f,
            distanceY = 40f,
            isPlayerSilent = true
        )
        assertEquals(PursuerState.LOST_TARGET, ai.state)
    }

    @Test
    fun hunterDetectsEarlierThanPatrol() {
        val patrol = PursuerAI(EnemyBoatType.PATROL)
        val hunter = PursuerAI(EnemyBoatType.HUNTER)

        patrol.update(0.1f, 0.56f, 40f, false)
        hunter.update(0.1f, 0.56f, 40f, false)

        assertEquals(PursuerState.PATROL, patrol.state)
        assertEquals(PursuerState.DETECTED, hunter.state)
    }

    @Test
    fun eliteCanInterceptAtCloseRange() {
        val elite = PursuerAI(EnemyBoatType.ELITE, PursuerState.PURSUIT)

        elite.update(0.1f, 0.9f, 15f, false)

        assertEquals(PursuerState.INTERCEPT, elite.state)
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
