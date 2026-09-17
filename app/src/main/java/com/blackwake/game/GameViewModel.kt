package com.blackwake.game

import android.app.Application
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Owns the game state and everything with side effects: persistence, audio, music and haptics.
 * Rules live in [GameSimulation]; this class only feeds it input and applies what it returns.
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {
    // Declared before init: Kotlin runs initializers top to bottom.
    private val store = ProgressStore(application)
    private val music = MusicDirector(application)
    private val vibrator: Vibrator? = findVibrator(application)
    private val rng = Random.Default

    private val _uiState = MutableStateFlow(GameState(progress = store.load()))
    val uiState: StateFlow<GameState> = _uiState.asStateFlow()

    private var dragTargetX: Float? = null
    private var diveHeld = false
    private var lastVibrationAt = 0L

    init {
        SynthAudioEngine.muted = _uiState.value.progress.muted
        SynthAudioEngine.start()
        viewModelScope.launch {
            while (isActive) {
                delay(AUDIO_TICK_MS)
                updateAudio(AUDIO_TICK_MS / 1000f)
            }
        }
    }

    // --- Frame loop

    fun frame(dt: Float) {
        val state = _uiState.value
        if (state.mode != GameMode.RUNNING) return
        val result = GameSimulation.step(state.run, state.chapterIndex, state.progress, RunInput(dragTargetX, diveHeld), dt, rng)
        play(result.events)
        val outcome = result.outcome
        if (outcome == null) {
            _uiState.value = state.copy(run = result.run)
        } else {
            finishRun(state, result.run, outcome)
        }
    }

    // --- Navigation

    fun openMenu() = _uiState.update { it.copy(mode = GameMode.MENU, pausePanel = PausePanel.NONE) }

    fun openGarage() = _uiState.update { it.copy(mode = GameMode.GARAGE) }

    fun openBriefing(chapterIndex: Int) = _uiState.update {
        if (chapterIndex in CHAPTERS.indices && chapterIndex < it.progress.unlockedChapterCount) {
            it.copy(mode = GameMode.BRIEFING, chapterIndex = chapterIndex)
        } else it
    }

    fun startRun() {
        releaseControls()
        _uiState.update {
            it.copy(
                mode = GameMode.RUNNING,
                run = GameSimulation.newRun(it.chapterIndex, it.progress),
                outcome = null,
                pausePanel = PausePanel.NONE
            )
        }
    }

    fun openNextChapter() = openBriefing(_uiState.value.chapterIndex + 1)

    fun pause() {
        releaseControls()
        _uiState.update { if (it.mode == GameMode.RUNNING) it.copy(mode = GameMode.PAUSED, pausePanel = PausePanel.NONE) else it }
    }

    fun resume() = _uiState.update { if (it.mode == GameMode.PAUSED) it.copy(mode = GameMode.RUNNING, pausePanel = PausePanel.NONE) else it }

    fun abandonRun() = _uiState.update { if (it.mode == GameMode.PAUSED) it.copy(mode = GameMode.MENU, pausePanel = PausePanel.NONE) else it }

    fun togglePausePanel(panel: PausePanel) = _uiState.update {
        if (it.mode == GameMode.PAUSED) it.copy(pausePanel = if (it.pausePanel == panel) PausePanel.NONE else panel) else it
    }

    /** System back. Returns false when the app should handle it (exit from the menu). */
    fun onBack(): Boolean {
        val state = _uiState.value
        when (state.mode) {
            GameMode.RUNNING -> pause()
            GameMode.PAUSED -> if (state.pausePanel != PausePanel.NONE) togglePausePanel(state.pausePanel) else resume()
            GameMode.BRIEFING, GameMode.GARAGE, GameMode.DEBRIEF -> openMenu()
            GameMode.MENU -> return false
        }
        return true
    }

    fun onAppBackground() {
        Log.i(TAG, "background: pausing mission and audio")
        pause()
        SynthAudioEngine.pause()
        music.pause()
    }

    fun onAppForeground() {
        Log.i(TAG, "foreground: mode=${_uiState.value.mode}")
        SynthAudioEngine.resume()
        music.resume()
    }

    // --- In-mission controls

    fun setDragTarget(x: Float?) {
        dragTargetX = x
    }

    fun setDive(held: Boolean) {
        diveHeld = held
    }

    fun setThrottle(value: Float) = updateRun { GameSimulation.setThrottle(it, value) to emptyList() }

    fun triggerSonar() = updateRun(GameSimulation::triggerSonar)

    fun launchFlare() = updateRun(GameSimulation::launchFlare)

    fun toggleOverride() = updateRun(GameSimulation::toggleOverride)

    fun toggleSeal(id: String) = updateRun { GameSimulation.toggleSeal(it, id) to listOf(GameEvent.Tone(300f, 0.08f, 0.25f)) }

    // --- Garage and settings

    fun buyModule(id: String) {
        val def = MODULES.firstOrNull { it.id == id } ?: return
        val before = _uiState.value.progress
        _uiState.update { state ->
            val progress = state.progress
            val level = progress.modules.level(id)
            val cost = def.costs.getOrNull(level)
            if (cost == null || level >= MODULE_MAX_LEVEL || progress.intelBank < cost) state
            else state.copy(progress = progress.copy(intelBank = progress.intelBank - cost, modules = progress.modules.upgraded(id)))
        }
        val after = _uiState.value.progress
        if (after != before) {
            store.save(after)
            SynthAudioEngine.playTone(990f, 0.12f, 0.3f)
        }
    }

    fun selectBoat(type: PlayerBoatType) = updateProgress { it.copy(boat = type) }

    fun toggleMute() = updateProgress { it.copy(muted = !it.muted) }

    // --- Internals

    private inline fun updateRun(block: (RunState) -> Pair<RunState, List<GameEvent>>) {
        val state = _uiState.value
        if (state.mode != GameMode.RUNNING) return
        val (run, events) = block(state.run)
        _uiState.value = state.copy(run = run)
        play(events)
    }

    private inline fun updateProgress(block: (Progress) -> Progress) {
        _uiState.update { it.copy(progress = block(it.progress)) }
        val progress = _uiState.value.progress
        SynthAudioEngine.muted = progress.muted
        store.save(progress)
    }

    private fun finishRun(state: GameState, run: RunState, outcome: RunOutcome) {
        val before = state.progress
        val unlocked = if (outcome.won) maxOf(before.unlockedChapterCount, minOf(CHAPTERS.size, state.chapterIndex + 2)) else before.unlockedChapterCount
        val progress = before.copy(
            intelBank = before.intelBank + outcome.banked,
            unlockedChapterCount = unlocked,
            bestIntel = before.bestIntel.mapIndexed { i, best -> if (i == state.chapterIndex) maxOf(best, outcome.intel) else best }
        )
        releaseControls()
        _uiState.value = state.copy(
            mode = GameMode.DEBRIEF,
            run = run,
            progress = progress,
            outcome = outcome.copy(unlockedNext = unlocked > before.unlockedChapterCount),
            pausePanel = PausePanel.NONE
        )
        store.save(progress)
        if (outcome.won) {
            viewModelScope.launch {
                for (freq in floatArrayOf(523f, 659f, 784f, 1047f)) {
                    SynthAudioEngine.playTone(freq, 0.16f, 0.3f)
                    delay(140)
                }
            }
        } else {
            SynthAudioEngine.playNoise(0.6f, 0.5f)
            vibrate(listOf(0, 200, 80, 300))
        }
    }

    private fun releaseControls() {
        dragTargetX = null
        diveHeld = false
    }

    private fun updateAudio(dt: Float) {
        val state = _uiState.value
        val run = state.run
        val inMission = state.mode == GameMode.RUNNING
        SynthAudioEngine.muted = state.progress.muted
        SynthAudioEngine.bedEnabled = inMission
        if (inMission) {
            val throttle = if (run.fuel > 0f) run.throttle else 0f
            SynthAudioEngine.engineLevel = throttle
            SynthAudioEngine.boosting = throttle > 0.8f
            SynthAudioEngine.detection = run.detection
            SynthAudioEngine.pursuers = run.activePursuers
            SynthAudioEngine.hullRatio = if (run.maxHull > 0f) run.hull / run.maxHull else 1f
            SynthAudioEngine.oxygenLow = run.dive.submerged && run.dive.oxygen < 30f
            SynthAudioEngine.siren = run.alarm
        } else {
            SynthAudioEngine.siren = false
            SynthAudioEngine.oxygenLow = false
        }
        val chapter = CHAPTERS[state.chapterIndex]
        val sector = chapter.sectors[run.sectorIndex.coerceIn(0, chapter.sectors.lastIndex)]
        val inRun = state.mode == GameMode.RUNNING || state.mode == GameMode.PAUSED
        val mood = if (inRun) MusicDirector.moodFor(run, sector) else MusicDirector.Mood.ROUTE
        music.update(dt, mood, MusicDirector.volumeFor(state.mode), state.progress.muted)
    }

    private fun play(events: List<GameEvent>) {
        for (event in events) {
            when (event) {
                is GameEvent.Tone -> SynthAudioEngine.playTone(event.freq, event.seconds, event.gain)
                is GameEvent.Noise -> SynthAudioEngine.playNoise(event.seconds, event.gain)
                is GameEvent.Vibrate -> vibrate(event.pattern)
            }
        }
    }

    private fun vibrate(pattern: List<Long>) {
        val target = vibrator ?: return
        val now = SystemClock.elapsedRealtime()
        if (now - lastVibrationAt < 80L) return
        lastVibrationAt = now
        try {
            target.vibrate(VibrationEffect.createWaveform(pattern.toLongArray(), -1))
        } catch (e: RuntimeException) {
            // Some devices reject waveforms; haptics are optional.
        }
    }

    private fun findVibrator(app: Application): Vibrator? {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            app.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            app.getSystemService(Vibrator::class.java)
        }
        return vibrator?.takeIf { it.hasVibrator() }
    }

    override fun onCleared() {
        SynthAudioEngine.stop()
        music.release()
        super.onCleared()
    }

    private companion object {
        const val AUDIO_TICK_MS = 50L
        const val TAG = "BlackWake"
    }
}
