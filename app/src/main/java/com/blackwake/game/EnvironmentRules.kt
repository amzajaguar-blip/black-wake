package com.blackwake.game

data class EnvironmentProfile(
    val sonarRange: Float,
    val laneMagnetMultiplier: Float,
    val detectionMultiplier: Float,
    val wreckCoverMultiplier: Float
)

object EnvironmentRules {
    fun forEnvironment(environment: EnvironmentId): EnvironmentProfile = when (environment) {
        EnvironmentId.NIGHT -> EnvironmentProfile(
            sonarRange = 1200f,
            laneMagnetMultiplier = 1.0f,
            detectionMultiplier = 1.0f,
            wreckCoverMultiplier = 0.20f
        )
        EnvironmentId.PORT -> EnvironmentProfile(
            sonarRange = 850f,
            laneMagnetMultiplier = 1.35f,
            detectionMultiplier = 1.10f,
            wreckCoverMultiplier = 0.15f
        )
        EnvironmentId.ISLAND -> EnvironmentProfile(
            sonarRange = 1000f,
            laneMagnetMultiplier = 0.85f,
            detectionMultiplier = 0.95f,
            wreckCoverMultiplier = 0.08f
        )
        EnvironmentId.EXTRACT -> EnvironmentProfile(
            sonarRange = 1200f,
            laneMagnetMultiplier = 1.0f,
            detectionMultiplier = 1.35f,
            wreckCoverMultiplier = 0.20f
        )
        EnvironmentId.STORM -> EnvironmentProfile(
            sonarRange = 750f,
            laneMagnetMultiplier = 0.65f,
            detectionMultiplier = 0.80f,
            wreckCoverMultiplier = 0.15f
        )
        EnvironmentId.DAWN -> EnvironmentProfile(
            sonarRange = 1200f,
            laneMagnetMultiplier = 1.0f,
            detectionMultiplier = 1.20f,
            wreckCoverMultiplier = 0.20f
        )
    }
}
