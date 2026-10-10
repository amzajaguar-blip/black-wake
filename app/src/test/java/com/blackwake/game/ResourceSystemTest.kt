package com.blackwake.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceSystemTest {
    private val eps = 0.0001f

    @Test
    fun fuelExhaustionStartsFiniteDriftWindow() {
        val step = ResourceSystem.stepFuel(
            fuel = 0.2f,
            drift = 0f,
            effectiveThrottle = 1f,
            dt = 1f
        )

        assertEquals(0f, step.fuel, eps)
        assertEquals(3f, step.drift, eps)
        assertTrue(step.exhaustedNow)
    }

    @Test
    fun driftCountsDownAfterFuelIsGone() {
        val step = ResourceSystem.stepFuel(
            fuel = 0f,
            drift = 3f,
            effectiveThrottle = 0f,
            dt = 1f
        )

        assertEquals(0f, step.fuel, eps)
        assertEquals(2f, step.drift, eps)
        assertFalse(step.exhaustedNow)
    }

    @Test
    fun oxygenDepletesAndRegeneratesWithEmergencyTradeoff() {
        assertEquals(75f, ResourceSystem.stepOxygen(100f, submerged = true, emergencyActive = false, dt = 1f), eps)
        assertEquals(85f, ResourceSystem.stepOxygen(100f, submerged = true, emergencyActive = true, dt = 1f), eps)
        assertEquals(65f, ResourceSystem.stepOxygen(50f, submerged = false, emergencyActive = false, dt = 1f), eps)
        assertEquals(95f, ResourceSystem.stepOxygen(50f, submerged = false, emergencyActive = true, dt = 1f), eps)
    }

    @Test
    fun emergencyBatteryDepletionDisablesOverride() {
        val step = ResourceSystem.stepBattery(
            battery = 5f,
            emergencyActive = true,
            dt = 1f
        )

        assertEquals(0f, step.battery, eps)
        assertFalse(step.emergencyActive)
        assertTrue(step.depletedNow)
        assertEquals(10f, step.signedRate, eps)
    }

    @Test
    fun batteryRechargesWhenOverrideIsOff() {
        val step = ResourceSystem.stepBattery(
            battery = 50f,
            emergencyActive = false,
            dt = 2f
        )

        assertEquals(55f, step.battery, eps)
        assertFalse(step.emergencyActive)
        assertFalse(step.depletedNow)
        assertEquals(-2.5f, step.signedRate, eps)
    }
}
