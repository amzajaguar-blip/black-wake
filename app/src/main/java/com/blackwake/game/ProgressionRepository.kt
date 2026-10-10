package com.blackwake.game

import android.content.Context

class ProgressionRepository(context: Context) {
    private val prefs = context.getSharedPreferences("black_wake_progression", Context.MODE_PRIVATE)

    fun load(): GameState {
        val modules = ModulesState(
            hull = prefs.getInt("module_hull", 0).coerceIn(0, 3),
            tank = prefs.getInt("module_tank", 0).coerceIn(0, 3),
            radar = prefs.getInt("module_radar", 0).coerceIn(0, 3),
            stealth = prefs.getInt("module_stealth", 0).coerceIn(0, 3)
        )
        val maxChapters = CHAPTERS.size.coerceAtLeast(1)

        return GameState(
            unlockedChapterCount = prefs.getInt("unlocked_chapters", 1).coerceIn(1, maxChapters),
            intelBank = prefs.getInt("intel_bank", 0).coerceAtLeast(0),
            bestIntel = prefs.getInt("best_intel", 0).coerceAtLeast(0),
            modules = modules
        )
    }

    fun save(state: GameState) {
        prefs.edit()
            .putInt("unlocked_chapters", state.unlockedChapterCount.coerceIn(1, CHAPTERS.size.coerceAtLeast(1)))
            .putInt("intel_bank", state.intelBank.coerceAtLeast(0))
            .putInt("best_intel", state.bestIntel.coerceAtLeast(0))
            .putInt("module_hull", state.modules.hull.coerceIn(0, 3))
            .putInt("module_tank", state.modules.tank.coerceIn(0, 3))
            .putInt("module_radar", state.modules.radar.coerceIn(0, 3))
            .putInt("module_stealth", state.modules.stealth.coerceIn(0, 3))
            .apply()
    }
}
