package com.blackwake.game

import org.junit.Assert.assertTrue
import org.junit.Test

class EnvironmentRulesTest {

    @Test
    fun portCreatesRestrictedHandlingAndShorterSonar() {
        val night = EnvironmentRules.forEnvironment(EnvironmentId.NIGHT)
        val port = EnvironmentRules.forEnvironment(EnvironmentId.PORT)

        assertTrue(port.sonarRange < night.sonarRange)
        assertTrue(port.laneMagnetMultiplier > night.laneMagnetMultiplier)
        assertTrue(port.detectionMultiplier > night.detectionMultiplier)
    }

    @Test
    fun islandMakesWreckCoverStrategicallyStronger() {
        val night = EnvironmentRules.forEnvironment(EnvironmentId.NIGHT)
        val island = EnvironmentRules.forEnvironment(EnvironmentId.ISLAND)

        assertTrue(island.wreckCoverMultiplier < night.wreckCoverMultiplier)
    }

    @Test
    fun extractionRaisesDetectionPressure() {
        val night = EnvironmentRules.forEnvironment(EnvironmentId.NIGHT)
        val extract = EnvironmentRules.forEnvironment(EnvironmentId.EXTRACT)

        assertTrue(extract.detectionMultiplier > night.detectionMultiplier)
    }
}
