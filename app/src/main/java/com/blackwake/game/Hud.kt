package com.blackwake.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Hud(state: GameState, viewModel: GameViewModel) {
    val run = state.run
    val chapter = CHAPTERS[state.chapterIndex]
    val sector = chapter.sectors[run.sectorIndex.coerceIn(0, chapter.sectors.lastIndex)]

    Box(
        Modifier
            .fillMaxSize()
            .displayCutoutPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        StatusPanel(run, chapter, Modifier.align(Alignment.TopStart))
        MissionPanel(run, chapter, sector, Modifier.align(Alignment.TopCenter).fillMaxWidth(0.36f))
        Row(Modifier.align(Alignment.TopEnd), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SignaturePanel(run)
            TacticalIconButton("II", "Pausa", size = 44.dp) { viewModel.pause() }
        }
        Row(
            Modifier.align(Alignment.BottomStart),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            ThrottleLever(run, onChange = viewModel::setThrottle)
        }
        ActionPad(run, viewModel, Modifier.align(Alignment.BottomEnd))
        if (BuildConfig.DEBUG) {
            val fps = if (run.lastDeltaTime > 0f) (1f / run.lastDeltaTime).toInt() else 0
            Label("FPS $fps · ENT ${run.entities.size}", Palette.Dim, 9.sp, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun StatusPanel(run: RunState, chapter: Chapter, modifier: Modifier) {
    Panel(modifier.width(184.dp)) {
        val hullRatio = if (run.maxHull > 0f) run.hull / run.maxHull else 0f
        val hullCritical = hullRatio < 0.25f
        Gauge(
            "SCAFO", "${run.hull.toInt()}/${run.maxHull.toInt()}", hullRatio,
            if (hullRatio > 0.3f) Palette.Green else Palette.Red, hullCritical, run.runElapsed
        )
        Gauge("CARBURANTE", "${run.fuel.toInt()}%", run.fuel / 100f, if (run.fuel > 20f) Palette.Amber else Palette.Red, run.fuel <= 20f, run.runElapsed)
        val dive = run.dive
        val oxygenText = when {
            dive.pressureAlarm -> "PRESSIONE! ${dive.depth.toInt()}m"
            dive.submerged || dive.depth > 1f -> "${dive.oxygen.toInt()}% · ${dive.depth.toInt()}m"
            else -> "${dive.oxygen.toInt()}%"
        }
        Gauge("OSSIGENO", oxygenText, dive.oxygen / 100f, if (dive.oxygen > 30f) Palette.Cyan else Palette.Red, dive.pressureAlarm || (dive.submerged && dive.oxygen < 30f), run.runElapsed)
        Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            val reached = run.intel >= chapter.intelRequired
            Label("INTEL ${run.intel}/${chapter.intelRequired}", if (reached) Palette.Green else Palette.Cyan, 12.sp)
            if (run.comboMultiplier > 1) Label("COMBO x${run.comboMultiplier}", Palette.Cyan, 10.sp)
        }
    }
}

@Composable
private fun Gauge(label: String, value: String, fraction: Float, color: Color, critical: Boolean, time: Float) {
    val alpha = if (critical) blink(time, 10f) else 1f
    Column(Modifier.padding(bottom = 5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Label(label, color.copy(alpha = alpha), 9.sp)
            Label(value, Palette.Text.copy(alpha = alpha), 9.sp)
        }
        Box(Modifier.padding(top = 2.dp).fillMaxWidth().height(5.dp).background(Palette.Deep)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).background(color.copy(alpha = alpha)))
        }
    }
}

@Composable
private fun MissionPanel(run: RunState, chapter: Chapter, sector: Sector, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Panel(Modifier.fillMaxWidth(), border = if (sector.extract) Palette.Amber.copy(alpha = 0.6f) else Palette.Line) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Label("CAP ${chapter.code} · ${sector.title}", Palette.Text, 10.sp, Modifier.weight(1f, fill = false))
                Label(sector.tag, if (sector.extract) Palette.Amber else Palette.Muted, 9.sp)
            }
            Row(Modifier.fillMaxWidth().padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                chapter.sectors.forEachIndexed { index, s ->
                    val fill = when {
                        index < run.sectorIndex -> 1f
                        index == run.sectorIndex -> (run.sectorProgress / s.duration).coerceIn(0f, 1f)
                        else -> 0f
                    }
                    val color = if (s.extract) Palette.Amber else Palette.Cyan
                    Box(Modifier.weight(1f).height(6.dp).background(Palette.Deep).border(1.dp, color.copy(alpha = 0.3f))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(fill).background(color.copy(alpha = 0.85f)))
                    }
                }
            }
            val radio = run.radio
            if (radio != null) {
                Text(
                    radio.text, color = Palette.Muted, fontSize = 10.sp, fontFamily = Mono, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        val notice = run.forkNotice
        if (notice != null) {
            Label(notice.text, Palette.Amber, 10.sp, Modifier.padding(top = 4.dp).background(Palette.Panel).padding(horizontal = 8.dp, vertical = 3.dp))
        }
        val message = run.message
        if (message != null) {
            val alpha = if (message.tone == MessageTone.DANGER) blink(run.runElapsed, 14f) else 1f
            Text(
                message.text, color = toneColor(message.tone).copy(alpha = alpha), fontSize = 13.sp, fontFamily = Mono,
                fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp).background(Palette.Deep.copy(alpha = 0.6f)).padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun SignaturePanel(run: RunState) {
    Panel(Modifier.width(112.dp), border = if (run.alarm) Palette.Red.copy(alpha = blink(run.runElapsed, 10f)) else Palette.Line) {
        val color = when {
            run.detection >= 0.7f -> Palette.Red
            run.detection >= 0.45f -> Palette.Amber
            else -> Palette.Cyan
        }
        Label("FIRMA", Palette.Muted, 9.sp)
        Text("${(run.detection * 100).toInt()}%", color = color, fontSize = 20.sp, fontFamily = Mono, fontWeight = FontWeight.Bold)
        Box(Modifier.fillMaxWidth().height(4.dp).background(Palette.Deep)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(run.detection).background(color))
        }
        val pursuers = run.activePursuers
        if (pursuers > 0) Label("INSEGUITORI $pursuers", Palette.Red, 9.sp, Modifier.padding(top = 3.dp))
        Radar(run, Modifier.padding(top = 6.dp).size(92.dp))
    }
}

@Composable
private fun Radar(run: RunState, modifier: Modifier) {
    Canvas(modifier.clip(CircleShape).background(Color(0xCC00110C)).border(1.dp, Palette.Cyan.copy(alpha = 0.6f), CircleShape)) {
        val c = Offset(size.width / 2f, size.height * 0.62f)
        val radius = size.width * 0.62f
        drawCircle(Palette.Cyan.copy(alpha = 0.18f), radius * 0.5f, c, style = Stroke(1f))
        drawCircle(Palette.Cyan.copy(alpha = 0.18f), radius, c, style = Stroke(1f))
        val sweep = (run.runElapsed * 2.4f) % 6.2831855f
        drawLine(Palette.Cyan.copy(alpha = 0.6f), c, Offset(c.x + cos(sweep) * radius, c.y + sin(sweep) * radius), 1.5f)
        for (e in run.entities) {
            if (e.z < -5f || e.z > 110f) continue
            val color = when (e.type) {
                EntityType.INTEL -> Palette.Cyan
                EntityType.FUEL -> Palette.Amber
                EntityType.FORK -> Color.White
                else -> Palette.Red
            }
            drawCircle(color, 2.2f * density, Offset(c.x + (e.x - run.playerX) * radius * 0.45f, c.y - e.z / 110f * radius))
        }
        for (p in run.pursuers) {
            if (!p.isActive) continue
            val color = if (p.state == PursuerState.INTERCEPT) Palette.Red else Palette.Red.copy(alpha = 0.7f)
            drawCircle(color, 3.4f * density, Offset(c.x + (p.x - run.playerX) * radius * 0.45f, c.y + (p.y - GameSimulation.PLAYER_Y) / 110f * radius))
        }
        drawCircle(Palette.Green, 2.5f * density, c)
    }
}

@Composable
private fun ThrottleLever(run: RunState, onChange: (Float) -> Unit) {
    val throttle = run.throttle
    val color = when {
        run.fuel <= 0f -> Palette.Dim
        throttle > 0.8f -> Palette.Red
        throttle > 0.6f -> Palette.Amber
        throttle < 0.2f -> Palette.Green
        else -> Palette.Cyan
    }
    val mode = when {
        run.fuel <= 0f -> "DERIVA"
        throttle > 0.8f -> "SPINTA"
        throttle < 0.2f -> "SILENZIO"
        else -> "CROCIERA"
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Label(mode, color, 9.sp)
        Box(
            Modifier
                .padding(top = 3.dp)
                .width(46.dp)
                .height(150.dp)
                .background(Palette.Panel)
                .border(1.dp, color.copy(alpha = 0.7f))
                .semantics { contentDescription = "Leva di potenza" }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        onChange(1f - (down.position.y / size.height).coerceIn(0f, 1f))
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            change.consume()
                            onChange(1f - (change.position.y / size.height).coerceIn(0f, 1f))
                        }
                    }
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val silentY = size.height * 0.8f
                val boostY = size.height * 0.2f
                drawRect(Palette.Green.copy(alpha = 0.10f), topLeft = Offset(0f, silentY), size = androidx.compose.ui.geometry.Size(size.width, size.height - silentY))
                drawRect(Palette.Red.copy(alpha = 0.10f), size = androidx.compose.ui.geometry.Size(size.width, boostY))
                val cx = size.width / 2f
                drawLine(Palette.Dim, Offset(cx, 6f), Offset(cx, size.height - 6f), 3f * density)
                val y = (1f - throttle) * size.height
                drawLine(color, Offset(cx, y), Offset(cx, size.height - 6f), 3f * density)
                drawRect(Palette.PanelSolid, topLeft = Offset(4f * density, y - 9f * density), size = androidx.compose.ui.geometry.Size(size.width - 8f * density, 18f * density))
                drawRect(color, topLeft = Offset(4f * density, y - 9f * density), size = androidx.compose.ui.geometry.Size(size.width - 8f * density, 18f * density), style = Stroke(2f * density))
            }
        }
        Label("${(throttle * 100).toInt()}%", color, 9.sp, Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun ActionPad(run: RunState, viewModel: GameViewModel, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val flare = run.flare
            ActionButton(
                title = "RAZZO",
                detail = if (flare.timer > 0f) "ATTIVO" else "x${flare.charges}",
                color = Palette.Amber,
                enabled = flare.charges > 0 && flare.timer <= 0f && !run.dive.submerged,
                active = flare.timer > 0f,
                onClick = viewModel::launchFlare
            )
            val power = run.power
            ActionButton(
                title = "OVERRIDE",
                detail = "${power.battery.toInt()}%",
                color = Palette.Red,
                enabled = power.overrideActive || power.battery >= 10f,
                active = power.overrideActive,
                fraction = power.battery / 100f,
                onClick = viewModel::toggleOverride
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val sonar = run.sonar
            ActionButton(
                title = "SONAR",
                detail = if (run.power.overrideActive) "AUTO" else "x${sonar.charges}",
                color = Palette.Cyan,
                enabled = sonar.charges > 0 && !sonar.active && !run.power.overrideActive,
                active = sonar.active,
                onClick = viewModel::triggerSonar
            )
            DiveButton(run, viewModel)
        }
    }
}

@Composable
private fun ActionButton(
    title: String,
    detail: String,
    color: Color,
    enabled: Boolean,
    active: Boolean,
    onClick: () -> Unit,
    fraction: Float? = null
) {
    val tint = if (enabled || active) color else Palette.Dim
    Box(
        Modifier
            .size(60.dp)
            .background(if (active) tint.copy(alpha = 0.3f) else Palette.Panel, CircleShape)
            .border(2.dp, tint, CircleShape)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (fraction != null) {
            Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                drawArc(tint.copy(alpha = 0.6f), -90f, 360f * fraction.coerceIn(0f, 1f), useCenter = false, style = Stroke(3f * density))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Label(title, tint, 8.sp)
            Label(detail, Palette.Text.copy(alpha = if (enabled || active) 1f else 0.4f), 10.sp)
        }
    }
}

@Composable
private fun DiveButton(run: RunState, viewModel: GameViewModel) {
    val dive = run.dive
    val canDive = dive.submerged || (dive.oxygen >= 15f && !dive.lockedOut)
    val tint = when {
        dive.submerged -> Palette.Cyan
        canDive -> Palette.Cyan.copy(alpha = 0.85f)
        else -> Palette.Dim
    }
    Box(
        Modifier
            .size(60.dp)
            .background(if (dive.submerged) Palette.Cyan.copy(alpha = 0.35f) else Palette.Panel, CircleShape)
            .border(2.dp, tint, CircleShape)
            .semantics { contentDescription = "Immergi, tieni premuto" }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    viewModel.setDive(true)
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            change.consume()
                            if (!change.pressed) break
                        }
                    } finally {
                        viewModel.setDive(false)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().padding(4.dp)) {
            drawArc(tint.copy(alpha = 0.6f), -90f, 360f * (dive.oxygen / 100f), useCenter = false, style = Stroke(3f * density))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Label("IMMERGI", tint, 8.sp)
            Label(if (dive.submerged) "${dive.oxygen.toInt()}%" else "TIENI", Palette.Text.copy(alpha = if (canDive) 1f else 0.4f), 9.sp)
        }
    }
}

@Composable
fun TacticalIconButton(text: String, description: String, size: Dp, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size)
            .background(Palette.Panel)
            .border(1.dp, Palette.Line)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Palette.Cyan, fontSize = 16.sp, fontFamily = Mono, fontWeight = FontWeight.Bold)
    }
}
