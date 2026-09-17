package com.blackwake.game

import android.content.Context
import androidx.core.content.edit

/** Persists [Progress] in SharedPreferences. Corrupt or missing values fall back to defaults. */
class ProgressStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("black_wake_progress", Context.MODE_PRIVATE)

    fun load(): Progress {
        val defaults = Progress()
        return try {
            val best = prefs.getString(KEY_BEST, null)
                ?.split(',')
                ?.mapNotNull { it.toIntOrNull() }
                .orEmpty()
            Progress(
                intelBank = prefs.getInt(KEY_BANK, 0).coerceAtLeast(0),
                unlockedChapterCount = prefs.getInt(KEY_UNLOCKED, 1).coerceIn(1, CHAPTERS.size),
                bestIntel = List(CHAPTERS.size) { best.getOrNull(it)?.coerceAtLeast(0) ?: 0 },
                modules = Modules(
                    engine = level(KEY_ENGINE),
                    hull = level(KEY_HULL),
                    tank = level(KEY_TANK),
                    radar = level(KEY_RADAR),
                    stealth = level(KEY_STEALTH)
                ),
                boat = prefs.getString(KEY_BOAT, null)
                    ?.let { name -> PlayerBoatType.entries.firstOrNull { it.name == name } }
                    ?: defaults.boat,
                muted = prefs.getBoolean(KEY_MUTED, false)
            )
        } catch (e: ClassCastException) {
            defaults
        }
    }

    fun save(progress: Progress) {
        prefs.edit {
            putInt(KEY_BANK, progress.intelBank)
            putInt(KEY_UNLOCKED, progress.unlockedChapterCount)
            putString(KEY_BEST, progress.bestIntel.joinToString(","))
            putInt(KEY_ENGINE, progress.modules.engine)
            putInt(KEY_HULL, progress.modules.hull)
            putInt(KEY_TANK, progress.modules.tank)
            putInt(KEY_RADAR, progress.modules.radar)
            putInt(KEY_STEALTH, progress.modules.stealth)
            putString(KEY_BOAT, progress.boat.name)
            putBoolean(KEY_MUTED, progress.muted)
        }
    }

    private fun level(key: String): Int = prefs.getInt(key, 0).coerceIn(0, MODULE_MAX_LEVEL)

    private companion object {
        const val KEY_BANK = "intel_bank"
        const val KEY_UNLOCKED = "unlocked_chapters"
        const val KEY_BEST = "best_intel"
        const val KEY_ENGINE = "module_engine"
        const val KEY_HULL = "module_hull"
        const val KEY_TANK = "module_tank"
        const val KEY_RADAR = "module_radar"
        const val KEY_STEALTH = "module_stealth"
        const val KEY_BOAT = "boat"
        const val KEY_MUTED = "muted"
    }
}
