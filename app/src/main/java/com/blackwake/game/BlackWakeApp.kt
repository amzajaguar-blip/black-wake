package com.blackwake.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.isActive

@Composable
fun BlackWakeApp(viewModel: GameViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val running = state.mode == GameMode.RUNNING

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onAppBackground() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onAppForeground() }
    BackHandler(enabled = state.mode != GameMode.MENU) { viewModel.onBack() }
    KeepScreenOn(running)
    if (running) GameLoop(viewModel)

    Box(Modifier.fillMaxSize().background(Palette.Ink)) {
        when (state.mode) {
            GameMode.MENU -> MainMenu(state, viewModel)
            GameMode.BRIEFING -> BriefingScreen(state, viewModel)
            GameMode.GARAGE -> GarageScreen(state, viewModel)
            GameMode.RUNNING, GameMode.PAUSED, GameMode.DEBRIEF -> {
                SeaView(state, viewModel)
                when (state.mode) {
                    GameMode.RUNNING -> Hud(state, viewModel)
                    GameMode.PAUSED -> PauseScreen(state, viewModel)
                    else -> DebriefScreen(state, viewModel)
                }
            }
        }
    }
}

@Composable
private fun GameLoop(viewModel: GameViewModel) {
    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        while (isActive) {
            val now = withFrameNanos { it }
            // Clamp long frames (resume, GC) so contacts are never skipped.
            viewModel.frame(((now - last) / 1_000_000_000f).coerceIn(0f, MAX_STEP_SECONDS))
            last = now
        }
    }
}

private const val MAX_STEP_SECONDS = 0.05f

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
private fun SeaView(state: GameState, viewModel: GameViewModel) {
    val run = state.run
    val chapter = CHAPTERS[state.chapterIndex]
    val sector = chapter.sectors[run.sectorIndex.coerceIn(0, chapter.sectors.lastIndex)]
    val boat = boatSpec(state.progress.boat)
    val effects = remember { SeaEffects() }
    val textMeasurer = rememberTextMeasurer()
    val submergeFog by animateFloatAsState(if (run.dive.submerged) 1f else 0f, tween(300), label = "submerge")
    val huntingFog by animateFloatAsState(if (run.hunted) 1f else 0f, tween(2500), label = "hunted")
    val interactive = state.mode == GameMode.RUNNING

    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(interactive) {
                if (!interactive) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val touch = event.changes.lastOrNull { it.pressed }
                        viewModel.setDragTarget(touch?.let { SeaProjection.touchToLane(it.position.x, size.width.toFloat()) })
                    }
                }
            }
    ) {
        drawSea(run, sector, boat, effects, textMeasurer, submergeFog, huntingFog)
    }
}
