package com.blackwake.game

enum class GameMode {
    MENU, BRIEFING, RUNNING, PAUSED, GARAGE, DEBRIEF
}

enum class EntityType {
    INTEL, FUEL, WRECK, MINE, ENEMY, FORK;

    val isHazard: Boolean get() = this == WRECK || this == MINE || this == ENEMY
}

enum class PlayerBoatType {
    SPEEDBOAT, RACING, PATROL, SMUGGLER, STEALTH, ARMORED
}

enum class EnemyBoatType {
    PATROL, INTERCEPTOR, POLICE, HUNTER, ARMORED, ELITE
}

enum class PursuerState {
    /** Just spotted the player; closing in from behind. */
    DETECTED,
    /** Shadowing the player ahead, tracking their lane. */
    PURSUIT,
    /** Telegraphed ramming run on a locked lane. */
    INTERCEPT,
    /** Lost the trail (player silent or submerged): sweeping and falling back. */
    SEARCH,
    SEARCH_AGAIN,
    /** Gave up; leaves the screen and is removed. */
    LOST_TARGET
}

/** Ordered by priority: a message only replaces the one on screen if its tone is at least as high. */
enum class MessageTone { INFO, WARNING, DANGER }

enum class FlashKind { NONE, INTEL, FUEL, DAMAGE, FLARE }

data class Entity(
    val id: Int,
    val type: EntityType,
    val lane: Int,
    /** Lane centre on the -1..1 axis. */
    val x: Float,
    /** Distance ahead of the player: 100 is the horizon, 0 the player's bow, negative is behind. */
    val z: Float,
    val value: Int = 0,
    val label: String? = null,
    val nearMissed: Boolean = false,
    val enemyType: EnemyBoatType = EnemyBoatType.PATROL
)

data class Pursuer(
    val id: Int,
    val type: EnemyBoatType,
    val x: Float,
    /** Track position: 0 is the horizon, [GameSimulation.PLAYER_Y] the player's row, larger is behind. */
    val y: Float,
    val velocityX: Float = 0f,
    val state: PursuerState = PursuerState.DETECTED,
    val stateTimer: Float = 0f,
    val attackTimer: Float = 0f,
    /** Lane locked for a ramming run, or the last known player position while searching. */
    val lockX: Float = 0f,
    val roll: Float = 0f
) {
    val isActive: Boolean get() = state != PursuerState.LOST_TARGET
}

data class TerminalMessage(val text: String, val tone: MessageTone, val timestamp: Float)

data class HudMessage(val text: String, val tone: MessageTone, val timer: Float)

data class RadioLine(val text: String, val timer: Float)

data class ForkNotice(val text: String, val expiresAt: Float)

data class DiveState(
    val submerged: Boolean = false,
    val oxygen: Float = 100f,
    /** Set when oxygen ran out mid-dive; cleared once the dive control is released. */
    val lockedOut: Boolean = false
)

data class SonarState(
    val charges: Int = 1,
    val regenTimer: Float = 0f,
    val active: Boolean = false,
    /** Ping front, in the same distance units as [Entity.z]. */
    val radius: Float = 0f
)

data class FlareState(
    val charges: Int = 3,
    val timer: Float = 0f
)

/** Everything that belongs to a single mission. A new run always starts from a fresh instance. */
data class RunState(
    val sectorIndex: Int = 0,
    val sectorProgress: Float = 0f,
    val sectorElapsed: Float = 0f,
    val runElapsed: Float = 0f,
    val spawnClock: Float = 0.42f,
    val tutorialStep: Int = 0,

    val hull: Float = 100f,
    val maxHull: Float = 100f,
    val fuel: Float = 100f,
    val fuelOutNotified: Boolean = false,
    val intel: Int = 0,
    val detection: Float = 0f,
    val peakDetection: Float = 0f,
    val invulnerable: Float = 0f,
    val pursuitCooldown: Float = 0f,
    val waveCooldown: Float = 0f,

    val playerX: Float = 0f,
    val playerVelocityX: Float = 0f,
    val playerRoll: Float = 0f,
    val throttle: Float = 0.5f,
    val speedFactor: Float = 1f,

    val dive: DiveState = DiveState(),
    val sonar: SonarState = SonarState(),
    val flare: FlareState = FlareState(),

    val comboCounter: Int = 0,
    val comboMultiplier: Int = 1,

    val cameraShake: Float = 0f,
    val screenFlash: Float = 0f,
    val flashKind: FlashKind = FlashKind.NONE,

    val forkSpawned: Boolean = false,
    val forkNotice: ForkNotice? = null,
    val proximityPingTimer: Float = 0f,

    val entities: List<Entity> = emptyList(),
    val pursuers: List<Pursuer> = emptyList(),
    val nextId: Int = 1,

    val message: HudMessage? = null,
    val radio: RadioLine? = null,
    val feed: List<TerminalMessage> = emptyList(),
    val lastDeltaTime: Float = 0f
) {
    val hunted: Boolean
        get() = pursuers.any { it.state == PursuerState.PURSUIT || it.state == PursuerState.INTERCEPT }

    val alarm: Boolean
        get() = detection >= 0.9f || pursuers.any { it.state == PursuerState.INTERCEPT }

    val activePursuers: Int get() = pursuers.count { it.isActive }
}

data class RunOutcome(
    val won: Boolean,
    val reason: String,
    val intel: Int,
    val intelRequired: Int,
    val banked: Int,
    val peakDetection: Float,
    val seconds: Float,
    val unlockedNext: Boolean = false
)

/** Persistent player progress (saved by [ProgressStore]). */
data class Progress(
    val intelBank: Int = 0,
    val unlockedChapterCount: Int = 1,
    val bestIntel: List<Int> = List(CHAPTERS.size) { 0 },
    val modules: Modules = Modules(),
    val boat: PlayerBoatType = PlayerBoatType.SPEEDBOAT,
    val muted: Boolean = false
)

data class GameState(
    val mode: GameMode = GameMode.MENU,
    val chapterIndex: Int = 0,
    val progress: Progress = Progress(),
    val run: RunState = RunState(),
    val outcome: RunOutcome? = null,
    val pausePanel: PausePanel = PausePanel.NONE
)

enum class PausePanel { NONE, MAP, LOG }
