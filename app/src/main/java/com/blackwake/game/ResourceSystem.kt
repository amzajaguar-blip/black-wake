package com.blackwake.game

import kotlin.math.max
import kotlin.math.min

data class BatteryStep(
    val battery: Float,
    val emergencyActive: Boolean,
    val depletedNow: Boolean,
    val signedRate: Float
)

data class FuelStep(
    val fuel: Float,
    val drift: Float,
    val exhaustedNow: Boolean
)

object ResourceSystem {
    fun stepBattery(battery: Float, emergencyActive: Boolean, dt: Float): BatteryStep {
        val safeDt = max(0f, dt)
        val rate = if (emergencyActive) 10f else -2.5f
        val nextBattery = if (emergencyActive) {
            max(0f, battery - safeDt * rate)
        } else {
            min(100f, battery - safeDt * rate)
        }
        val depletedNow = emergencyActive && battery > 0f && nextBattery <= 0f
        return BatteryStep(
            battery = nextBattery,
            emergencyActive = emergencyActive && nextBattery > 0f,
            depletedNow = depletedNow,
            signedRate = rate
        )
    }

    fun stepOxygen(oxygen: Float, submerged: Boolean, emergencyActive: Boolean, dt: Float): Float {
        val safeDt = max(0f, dt)
        return if (submerged) {
            val depleteRate = if (emergencyActive) 15f else 25f
            max(0f, oxygen - safeDt * depleteRate)
        } else {
            val regenRate = if (emergencyActive) 45f else 15f
            min(100f, oxygen + safeDt * regenRate)
        }
    }

    fun stepFuel(fuel: Float, drift: Float, effectiveThrottle: Float, dt: Float): FuelStep {
        val safeDt = max(0f, dt)
        val throttle = effectiveThrottle.coerceIn(0f, 1f)
        val usageRate = 0.5f + throttle * throttle * 6.1f
        val nextFuel = max(0f, fuel - safeDt * usageRate)
        val exhaustedNow = fuel > 0f && nextFuel <= 0f
        val nextDrift = when {
            exhaustedNow -> 3f
            nextFuel <= 0f -> max(0f, drift - safeDt)
            else -> drift
        }
        return FuelStep(nextFuel, nextDrift, exhaustedNow)
    }
}
