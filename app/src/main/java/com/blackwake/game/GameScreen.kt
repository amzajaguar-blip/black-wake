package com.blackwake.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.focusable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.sin

val Cyan = Color(0xFF2BE7E0)
val Amber = Color(0xFFFFB45F)
val Red = Color(0xFFEF464B)
val DarkCyan = Color(0xFF071E25)
val SeaColor = Color(0xFF07456F) // Deep Blue Ocean
val StormColor = Color(0xFF031A33) // Stormy Blue
val BlackTideColor = Color(0xFF010A14) // Navy Black

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val state by viewModel.uiState.collectAsState()
    var showProfiler by remember { mutableStateOf(false) }
    
    GameLoop(state, viewModel)
    
    Box(modifier = Modifier.fillMaxSize().background(SeaColor)) {
        if (state.mode == GameMode.RUNNING || state.mode == GameMode.PAUSED || state.mode == GameMode.DEBRIEF || state.mode == GameMode.PATCHING) {
            GameCanvas(state, viewModel)
            Hud(state, viewModel)
            if (showProfiler) {
                ProfilerDashboard(state)
            }
            if (state.mode == GameMode.PATCHING) {
                PatchingMiniGameOverlay(state, viewModel)
            }
            if (state.showTacticalMap && state.mode != GameMode.PATCHING) {
                TacticalMapOverlay(state, viewModel)
            }
            if (state.showDamageControl && state.mode != GameMode.PATCHING) {
                DamageControlOverlay(state, viewModel)
            }
        }
        
        Button(
            onClick = { showProfiler = !showProfiler },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.5f)),
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 80.dp, end = 16.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Text("PROFILER", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        }
        
        // Screen Flash Effect
        if (state.screenFlash > 0f) {
            Box(modifier = Modifier.fillMaxSize().background(state.flashColor.copy(alpha = state.screenFlash)))
        }
        
        // Hull Damage Vignette (triggered when hull is low)
        if (state.hull < 40f && (state.mode == GameMode.RUNNING || state.mode == GameMode.PAUSED)) {
            val vignetteIntensity = (40f - state.hull) / 40f
            val pulse = (kotlin.math.sin(state.runElapsed * 5f) * 0.5f + 0.5f).toFloat()
            val alpha = vignetteIntensity * (0.4f + 0.6f * pulse)
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Transparent, Red.copy(alpha = alpha)),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width.coerceAtLeast(size.height) * 0.8f
                    )
                )
            }
        }
        
        when (state.mode) {
            GameMode.MENU -> MainMenu(state, viewModel)
            GameMode.BRIEFING -> BriefingScreen(state, viewModel)
            GameMode.GARAGE -> GarageScreen(state, viewModel)
            GameMode.PAUSED -> PauseScreen(viewModel)
            GameMode.DEBRIEF -> DebriefScreen(state, viewModel)
            else -> {}
        }
    }
}

@Composable
fun MainMenu(state: GameState, viewModel: GameViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("MAREA NERA", fontSize = 48.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Text("OPERAZIONE 07.14N", color = Amber, fontSize = 14.sp, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(CHAPTERS) { index, chapter ->
                val isUnlocked = index < state.unlockedChapterCount
                Card(
                    modifier = Modifier.clickable(enabled = isUnlocked) { viewModel.openBriefing(index) },
                    colors = CardDefaults.cardColors(containerColor = if (isUnlocked) Color(0xFF05191F) else Color(0xFF020A0E)),
                    border = BorderStroke(1.dp, if (isUnlocked) Cyan.copy(alpha=0.3f) else Color.DarkGray)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(chapter.code, color = Cyan, fontSize = 12.sp)
                        Text(chapter.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { viewModel.openGarage() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF081D22))) {
                Text("GARAGE (${state.intelBank} INTEL)", color = Cyan)
            }
        }
    }
}

@Composable
fun BriefingScreen(state: GameState, viewModel: GameViewModel) {
    val chapter = CHAPTERS[state.currentChapterIndex]
    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Text("CAPITOLO ${chapter.code}", color = Cyan, letterSpacing = 2.sp)
        Text(chapter.title, fontSize = 40.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Text(chapter.subtitle, color = Amber, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(24.dp))
        
        chapter.briefing.forEach {
            Text(it, color = Color(0xFFCCdedb), modifier = Modifier.padding(bottom = 8.dp))
        }
        Spacer(modifier = Modifier.weight(1f))
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { viewModel.startGame() }, colors = ButtonDefaults.buttonColors(containerColor = Cyan)) {
                Text("INIZIA MISSIONE", color = Color.Black)
            }
            Button(onClick = { viewModel.openMenu() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)) {
                Text("INDIETRO", color = Color.White)
            }
        }
    }
}

@Composable
fun GarageScreen(state: GameState, viewModel: GameViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Text("GARAGE & DOCK", fontSize = 40.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Text("INTEL DISPONIBILE: ${state.intelBank}", color = Cyan)
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("SELECT VESSEL", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        // Boat Selector
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val boats = PlayerBoatType.values()
            items(boats.size) { i ->
                val b = boats[i]
                val selected = state.playerBoatType == b
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (selected) Cyan.copy(alpha=0.3f) else Color(0xFF05191F)),
                    border = BorderStroke(1.dp, if (selected) Cyan else Color.Gray),
                    modifier = Modifier.clickable { viewModel.setPlayerBoat(b) }.width(140.dp).height(80.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp).fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(b.name, color = Color.White, fontWeight = FontWeight.Bold)
                        val desc = when(b) {
                            PlayerBoatType.SPEEDBOAT -> "Balanced, Fast"
                            PlayerBoatType.RACING -> "High Speed, Low Armor"
                            PlayerBoatType.PATROL -> "Standard Police"
                            PlayerBoatType.SMUGGLER -> "Heavy cargo"
                            PlayerBoatType.STEALTH -> "Low detection"
                            PlayerBoatType.ARMORED -> "High Hull, Slow"
                        }
                        Text(desc, color = Color.Gray, fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("MODULES", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            itemsIndexed(MODULES) { _, mod ->
                val level = when(mod.id) {
                    "engine" -> state.modules.engine
                    "hull" -> state.modules.hull
                    "tank" -> state.modules.tank
                    "radar" -> state.modules.radar
                    "stealth" -> state.modules.stealth
                    else -> 0
                }
                val cost = if (level < 3) mod.costs[level] else 0
                val canAfford = state.intelBank >= cost && level < 3
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF05191F)),
                    border = BorderStroke(1.dp, Color.DarkGray)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(mod.title, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(mod.tag, color = Amber, fontSize = 12.sp)
                            Text("Lv $level/3", color = Cyan, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { viewModel.buyModule(mod.id) },
                            enabled = canAfford,
                            colors = ButtonDefaults.buttonColors(containerColor = if (canAfford) Cyan else Color.Gray)
                        ) {
                            Text(if (level < 3) "$cost INTEL" else "MAX", color = Color.Black)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = { viewModel.openMenu() }) { Text("INDIETRO") }
    }
}

@Composable
fun PauseScreen(viewModel: GameViewModel) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.8f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SOSPENSIONE", color = Color.White, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { viewModel.togglePause() }, colors = ButtonDefaults.buttonColors(containerColor = Cyan)) {
                Text("RIPRENDI", color = Color.Black)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.openMenu() }) {
                Text("ABBANDONA")
            }
        }
    }
}

@Composable
fun DebriefScreen(state: GameState, viewModel: GameViewModel) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.8f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.background(Color(0xFF040E13)).border(1.dp, if (state.isWon) Cyan else Red).padding(32.dp)) {
            Text(if (state.isWon) "MISSIONE COMPIUTA" else "MISSIONE FALLITA", color = if (state.isWon) Cyan else Red, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text(state.debriefReason, color = Color.White)
            Spacer(modifier = Modifier.height(24.dp))
            Row {
                Button(onClick = { viewModel.openMenu() }) { Text("RITORNA AL MENU") }
            }
        }
    }
}

@Composable
fun CompassWidget(state: GameState, modifier: Modifier = Modifier) {
    // Map player velocity and position to a slight heading rotation
    // Player mostly goes North. Steeling left/right shifts heading slightly.
    val headingOffset = state.playerVelocityX * 15f + state.playerX * 10f
    
    Box(
        modifier = modifier
            .size(80.dp)
            .background(Color(0xFF040A0C).copy(alpha = 0.8f), androidx.compose.foundation.shape.CircleShape)
            .border(2.dp, Cyan.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = size.width / 2f - 4.dp.toPx()
            
            rotate(degrees = -headingOffset, pivot = Offset(cx, cy)) {
                // Draw N, E, S, W markings
                val textRadius = radius * 0.7f
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#2BE7E0") // Cyan
                    textSize = 30f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.MONOSPACE
                    isFakeBoldText = true
                }
                val paintSmall = android.graphics.Paint().apply {
                    color = android.graphics.Color.LTGRAY
                    textSize = 20f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.MONOSPACE
                }
                
                // Draw tick marks
                for (i in 0 until 360 step 15) {
                    val angleRad = Math.toRadians((i - 90).toDouble())
                    val isMajor = i % 90 == 0
                    val startRad = if (isMajor) radius * 0.8f else radius * 0.9f
                    val startX = cx + (Math.cos(angleRad) * startRad).toFloat()
                    val startY = cy + (Math.sin(angleRad) * startRad).toFloat()
                    val endX = cx + (Math.cos(angleRad) * radius).toFloat()
                    val endY = cy + (Math.sin(angleRad) * radius).toFloat()
                    
                    drawLine(
                        color = if (isMajor) Cyan else Color.Gray,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = if (isMajor) 3f else 1f
                    )
                }

                // We use native canvas for text drawing
                drawContext.canvas.nativeCanvas.apply {
                    drawText("N", cx, cy - textRadius + 10f, paint)
                    drawText("S", cx, cy + textRadius + 10f, paintSmall)
                    drawText("E", cx + textRadius, cy + 10f, paintSmall)
                    drawText("W", cx - textRadius, cy + 10f, paintSmall)
                }
            }
            
            // Draw center fixed indicator (the "ship")
            drawLine(
                color = Red,
                start = Offset(cx, cy - radius * 0.5f),
                end = Offset(cx, cy + radius * 0.2f),
                strokeWidth = 3f
            )
            drawLine(
                color = Red,
                start = Offset(cx - radius * 0.2f, cy + radius * 0.2f),
                end = Offset(cx + radius * 0.2f, cy + radius * 0.2f),
                strokeWidth = 3f
            )
        }
    }
}

@Composable
fun Hud(state: GameState, viewModel: GameViewModel) {
    val isHullCritical = state.hull < 25f
    val hudPulseAlpha = if (isHullCritical) (kotlin.math.sin(state.runElapsed * 12f) * 0.4f + 0.3f).toFloat() else 0f
    
    Box(modifier = Modifier.fillMaxSize().border(if (isHullCritical) 4.dp else 0.dp, Red.copy(alpha = hudPulseAlpha))) {
        if (state.isGlobalEmergency) {
            val emgPulse = (kotlin.math.sin(state.runElapsed * 8f) * 0.15f + 0.1f).toFloat()
            Box(modifier = Modifier.fillMaxSize().background(Red.copy(alpha = emgPulse)))
        }
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                if (state.messageFlash != null) {
                    val isCritical = state.messageColor == "red"
                    val flashAlpha = if (isCritical) (kotlin.math.sin(state.runElapsed * 20f) * 0.5f + 0.5f).toFloat() else 1f
                    val mColor = if (state.messageColor == "red") Red else if (state.messageColor == "amber") Amber else Cyan
                    Text(state.messageFlash, color = mColor.copy(alpha = flashAlpha), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                // Terminal Feed
                Box(modifier = Modifier.width(200.dp).height(60.dp).background(Color.Black.copy(alpha=0.6f)).border(1.dp, Cyan.copy(alpha=0.2f)).padding(4.dp)) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
                        state.terminalFeed.take(6).reversed().forEach { msg ->
                            val isCritical = msg.color == "red"
                            val flashAlpha = if (isCritical) (kotlin.math.sin(state.runElapsed * 15f) * 0.3f + 0.7f).toFloat() else 1f
                            val tColor = when (msg.color) { "red" -> Red.copy(alpha = flashAlpha); "amber" -> Amber; else -> Cyan }
                            Text("> ${msg.text}", color = tColor, fontSize = 8.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Oxygen Bar
                val o2Critical = state.oxygen < 20f
                val o2Flash = if (o2Critical) (kotlin.math.sin(state.runElapsed * 15f) * 0.5f + 0.5f).toFloat() else 1f
                val o2Color = if (state.oxygen > 30) Cyan else Red
                Text("O2: ${state.oxygen.toInt()}%", color = o2Color.copy(alpha = o2Flash), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.size(100.dp, 8.dp).background(Color.DarkGray).border(1.dp, if (o2Critical) Red.copy(alpha = o2Flash) else Color.Black)) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(state.oxygen / 100f).background(o2Color.copy(alpha = o2Flash)))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("CARB: ${state.fuel.toInt()}", color = if (state.fuel > 20) Amber else Red)
                Text("INTEL: ${state.intel}", color = Cyan)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.isEmergencyPowerActive) {
                        Text("SNR: OFFLINE", color = Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("SNR:", color = if (state.sonarCharges > 0) Cyan else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        for (i in 0 until (1 + state.modules.radar)) {
                            Box(
                                modifier = Modifier
                                    .size(width = 12.dp, height = 6.dp)
                                    .background(if (i < state.sonarCharges) Cyan else Color.DarkGray)
                                    .border(1.dp, Cyan.copy(alpha = 0.5f))
                            )
                            Spacer(Modifier.width(2.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.toggleTacticalMap() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (state.showTacticalMap) Cyan.copy(alpha=0.3f) else Color.Black.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.border(1.dp, Cyan)
                ) {
                    Text(if (state.showTacticalMap) "CHIUDI MAPPA" else "MAPPA TATTICA", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.toggleEventLog() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (state.showEventLog) Cyan.copy(alpha=0.3f) else Color.Black.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.border(1.dp, Cyan)
                ) {
                    Text(if (state.showEventLog) "CHIUDI ARCHIVIO" else "LOG DI BORDO", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                val hasBreach = state.compartments.any { it.state == CompartmentState.BREACHED }
                val dcColor = if (hasBreach) Red else Amber
                val dcFlash = if (hasBreach) (kotlin.math.sin(state.runElapsed * 10f) * 0.5f + 0.5f).toFloat() else 1f
                Button(
                    onClick = { viewModel.toggleDamageControl() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (state.showDamageControl) dcColor.copy(alpha=0.3f) else Color.Black.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.border(1.dp, dcColor.copy(alpha=dcFlash))
                ) {
                    Text(if (state.showDamageControl) "CHIUDI PANN." else "CONTROLLO DANNI", color = dcColor.copy(alpha=dcFlash), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                val audioColor = if (state.isAudioMuted) Color.Gray else Cyan
                Button(
                    onClick = { viewModel.toggleAudioMute() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.border(1.dp, audioColor)
                ) {
                    Text(if (state.isAudioMuted) "AUDIO: OFF" else "AUDIO: ON", color = audioColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                val flareActive = state.flareActiveTimer > 0f
                val flareColor = if (flareActive) Color(0xFFFFFF99) else if (state.flareCharges > 0) Amber else Color.Gray
                Button(
                    onClick = { viewModel.launchFlare() },
                    enabled = state.flareCharges > 0 && !flareActive && !state.isSubmerged,
                    colors = ButtonDefaults.buttonColors(containerColor = if (flareActive) flareColor.copy(alpha=0.3f) else Color.Black.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.border(1.dp, flareColor)
                ) {
                    Text(if (flareActive) "RAZZO ATTIVO" else "RAZZO (${state.flareCharges})", color = flareColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                val empColor = if (state.isEmergencyPowerActive) Red else Amber
                Button(
                    onClick = { viewModel.toggleEmergencyPower() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (state.isEmergencyPowerActive) Red.copy(alpha=0.3f) else Color.Black.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.border(1.dp, empColor)
                ) {
                    Text(if (state.isEmergencyPowerActive) "PWR: OVERRIDE" else "PWR: NORMAL", color = empColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                val batColor = if (state.isEmergencyPowerActive) Red else Cyan
                val voltage = 21.0f + (state.battery / 100f) * 7.8f
                
                Row(modifier = Modifier.width(100.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("PWR CORE", color = batColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(String.format(java.util.Locale.US, "%.1fV", voltage), color = batColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.height(2.dp))
                // Linear Voltage Gauge
                Box(
                    modifier = Modifier
                        .size(100.dp, 10.dp)
                        .background(Color.Black.copy(alpha=0.8f))
                        .border(1.dp, batColor.copy(alpha=0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(state.battery / 100f)
                            .background(batColor.copy(alpha = 0.6f))
                    )
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val tickCount = 4
                        val step = size.width / tickCount
                        for (i in 1 until tickCount) {
                            drawLine(
                                color = batColor.copy(alpha = 0.8f),
                                start = Offset(i * step, 0f),
                                end = Offset(i * step, size.height),
                                strokeWidth = 1f
                            )
                        }
                    }
                }
                
                // Power Consumption Rate Visualization
                if (state.batteryHistory.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(if (state.batteryDrainRate > 0) "CONSUMO IN CORSO" else "RICARICA IN CORSO", color = batColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.size(100.dp, 30.dp).background(Color.Black.copy(alpha=0.5f)).border(1.dp, batColor.copy(alpha=0.5f))) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val maxPoints = 60
                            val stepX = w / (maxPoints - 1).toFloat()
                            val path = androidx.compose.ui.graphics.Path()
                            
                            val historySize = state.batteryHistory.size
                            val padCount = maxPoints - historySize
                            
                            for (i in 0 until maxPoints) {
                                val x = i * stepX
                                val y: Float
                                if (i < padCount) {
                                    y = h - (state.batteryHistory.first() / 100f) * h
                                } else {
                                    y = h - (state.batteryHistory[i - padCount] / 100f) * h
                                }
                                
                                if (i == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }
                            }
                            drawPath(path, batColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                // Global Emergency
                val emgColor = if (state.isGlobalEmergency) Red else Amber
                val emgFlash = if (state.isGlobalEmergency) (kotlin.math.sin(state.runElapsed * 15f) * 0.5f + 0.5f).toFloat() else 1f
                Button(
                    onClick = { viewModel.toggleGlobalEmergency() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (state.isGlobalEmergency) Red.copy(alpha=0.3f) else Color.Black.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.border(1.dp, emgColor.copy(alpha=emgFlash))
                ) {
                    Text("ALLARME", color = emgColor.copy(alpha=emgFlash), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                if (state.comboMultiplier > 1) {
                    Text("COMBO x${state.comboMultiplier}", color = Cyan, fontWeight = FontWeight.Bold)
                }
            }
            Row(verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("RILEVAMENTO", color = Color.Gray, fontSize = 12.sp)
                Text("${(state.detection * 100).toInt()}%", color = if (state.detection > 0.8f) Red else Cyan, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                
                if (state.pursuers.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("INSEGUITORI: ${state.pursuers.size}", color = Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    state.pursuers.forEach { p ->
                        val dist = kotlin.math.abs(p.y - 85f).toInt()
                        val stateName = when (p.state) {
                            PursuerState.SEARCH -> "SRCH"
                            PursuerState.INTERCEPT -> "INTC"
                            PursuerState.LOST_TARGET -> "LOST"
                            else -> "PRST"
                        }
                        Text("${p.type.name} [$stateName] ${dist}m", color = Red, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
                
                VisualRadar(state)
                MiniNavMap(state)
                
                Button(onClick = { viewModel.togglePause() }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("PAUSA")
                }
            }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        
        if (state.forkNotice != null) {
            Text(state.forkNotice, color = Cyan, modifier = Modifier.align(Alignment.CenterHorizontally).background(Color.Black.copy(0.7f)).padding(8.dp))
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                val hullCritical = state.hull < 25f
                val hullFlash = if (hullCritical) (kotlin.math.sin(state.runElapsed * 18f) * 0.5f + 0.5f).toFloat() else 1f
                val hullColor = if (state.hull > 30) Color.Green else Red
                Text(
                    text = if (hullCritical) "INTEGRITA' SCAFO - CRITICO" else "INTEGRITA' SCAFO", 
                    color = hullColor.copy(alpha = hullFlash), 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.Bold, 
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                val maxHull = 100f + (state.modules.hull * 7f)
                val hullRatio = (state.hull / maxHull).coerceIn(0f, 1f)
                Box(modifier = Modifier.width(220.dp).height(18.dp).background(Color(0xFF222222)).border(2.dp, if (hullCritical) Red.copy(alpha = hullFlash) else Color.Black)) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(hullRatio).background(hullColor.copy(alpha = hullFlash)))
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (state.pressureEventActive) {
                    Text("ALLARME: PRESSIONE ECCESSIVA", color = Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    modifier = Modifier.size(64.dp).pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                viewModel.setDive(true)
                                tryAwaitRelease()
                                viewModel.setDive(false)
                            }
                        )
                    },
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = if (state.isSubmerged) Cyan else Color.DarkGray)
                ) { Text("DIV", color = if (state.isSubmerged) Color.Black else Color.White) }
                Button(
                    modifier = Modifier.size(64.dp).pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                viewModel.triggerSonar()
                            }
                        )
                    },
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = if (state.sonarCharges > 0) Cyan else Color.DarkGray)
                ) { Text("SNR", color = Color.Black) }
            }
        }
    }
        
        // Interactive Throttle
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .width(48.dp)
                .height(240.dp)
                .background(Color.Black.copy(alpha = 0.7f))
                .border(2.dp, Cyan)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while(true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.pressed }
                            if (change != null) {
                                change.consume()
                                val yPercent = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                                viewModel.setThrottle(yPercent)
                            }
                        }
                    }
                }
        ) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                Text("MAX", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                Text("MIN", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
            }
            
            // Track
            Box(modifier = Modifier.align(Alignment.Center).width(4.dp).fillMaxHeight(0.8f).background(Color.DarkGray))
            
            // Track Fill
            val trackHeight = 192f // 240 * 0.8
            val fillHeight = trackHeight * state.throttle
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .width(4.dp)
                    .height(fillHeight.dp)
                    .background(if (state.throttle > 0.6f) Amber else Cyan)
            )
            
            // Handle
            val handleY = (1f - state.throttle) * trackHeight
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 24.dp + handleY.dp - 12.dp)
                    .size(width = 36.dp, height = 24.dp)
                    .background(Color(0xFF222222))
                    .border(2.dp, if (state.throttle > 0.6f) Amber else Cyan)
            ) {
                // Lines on handle for grip
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(20.dp, 2.dp).background(Color.Gray))
                    Box(modifier = Modifier.size(20.dp, 2.dp).background(Color.Gray))
                    Box(modifier = Modifier.size(20.dp, 2.dp).background(Color.Gray))
                }
            }
            
            // Throttle text
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(x = 56.dp, y = (-24f - fillHeight + 12f).dp)
                    .background(Color.Black.copy(alpha=0.6f))
                    .border(1.dp, Cyan)
                    .padding(4.dp)
            ) {
                Text("PWR ${(state.throttle * 100).toInt()}%", color = if (state.throttle > 0.6f) Amber else Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        RadarWidget(
            state = state,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
        )
        
        VerticalDepthGauge(
            state = state,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        )
    }
}

@Composable
fun GameCanvas(state: GameState, viewModel: GameViewModel) {
    // Water effect based on detection (Continuous interpolation)
    // Biome colors
    val baseOcean = when (state.currentChapterIndex % 3) {
        0 -> SeaColor // Deep Blue Ocean
        1 -> Color(0xFF0F3B3A) // Swamp/Greenish Open Water
        else -> Color(0xFF140F30) // Night Ocean (Purple-ish Navy)
    }
    
    val tensionColor = androidx.compose.ui.graphics.lerp(baseOcean, StormColor, (state.detection / 0.75f).coerceIn(0f, 1f))
    val baseWaterTint = androidx.compose.ui.graphics.lerp(tensionColor, BlackTideColor, ((state.detection - 0.75f) / 0.25f).coerceIn(0f, 1f))
    
    val flareIntensity = (state.flareActiveTimer / 5f).coerceIn(0f, 1f)
    val waterTint = androidx.compose.ui.graphics.lerp(baseWaterTint, Color(0xFF553311), flareIntensity * 0.8f)
    
    val submergeFog by animateFloatAsState(
        targetValue = if (state.isSubmerged) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "submergeFog"
    )
    
    val wakePath = remember { Path() }
    val beamPath = remember { Path() }
    val boatPath = remember { Path() }
    
    val isHunted = state.pursuers.any { it.state == PursuerState.PURSUIT || it.state == PursuerState.INTERCEPT || it.state == PursuerState.ATTACK }
    val huntingFog by animateFloatAsState(
        targetValue = if (isHunted) 1f else 0f,
        animationSpec = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
        label = "huntingFog"
    )
    val cockpitPath = remember { Path() }
    val entityPath = remember { Path() }
    
    // Fixed-size primitive array buffers for zero-allocation particle systems
    val rainParticles = remember { FloatArray(150 * 5) }
    val rainInitialized = remember { BooleanArray(1) }
    val sprayParticles = remember { FloatArray(150 * 6) }
    val sprayIndex = remember { IntArray(1) }
    val randomTable = remember { FloatArray(1024) { Math.random().toFloat() } }
    val randomIndex = remember { IntArray(1) }
    
    if (!rainInitialized[0]) {
        for (i in 0 until 150) {
            val idx = i * 5
            val seed = i * 137.54f
            rainParticles[idx] = (seed * 123.4f) // baseX
            rainParticles[idx+1] = (seed * 456.7f) // baseY
            rainParticles[idx+2] = 0.8f + (seed % 40f) / 100f // speedMod
            rainParticles[idx+3] = 20f + (seed % 40f) // base dropLength
            rainParticles[idx+4] = if (i % 6 == 0) 1f else 0f // isForeground
        }
        rainInitialized[0] = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(waterTint)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.lastOrNull { it.pressed && !it.isConsumed }
                        if (change != null) {
                            val xNorm = (change.position.x / size.width).coerceIn(0f, 1f)
                            viewModel.setDragTarget(xNorm)
                        } else if (event.changes.none { it.pressed && !it.isConsumed }) {
                            viewModel.clearDrag()
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            
            // Camera Shake
            val tensionShake = if (state.detection > 0.85f) (state.detection - 0.85f) * 15f else 0f
            val hullShake = if (state.hull < 30f) (30f - state.hull) * 0.05f else 0f
            val totalShake = state.cameraShake + tensionShake + hullShake
            val shakeX = if (totalShake > 0f) (Math.random().toFloat() - 0.5f) * 20f * totalShake else 0f
            val shakeY = if (totalShake > 0f) (Math.random().toFloat() - 0.5f) * 20f * totalShake else 0f
            
            translate(left = shakeX, top = shakeY) {
                
                // Perspective projection parameters (affected by FOV)
                val fovScale = state.fovOffset
                val horizonY = h * 0.35f
                val laneWidthBottom = w * 0.4f * fovScale
                val laneWidthTop = w * 0.05f * fovScale
                
                // Player Coordinates
                val playerY = h * 0.85f
                val playerCx = w/2 + (state.playerX) * laneWidthBottom
                
                // Draw Sonar Ping
                if (state.sonarPingActive) {
                    drawCircle(
                        color = Cyan.copy(alpha = 1f - (state.sonarPingRadius / 1200f).coerceIn(0f, 1f)),
                        radius = state.sonarPingRadius,
                        center = Offset(playerCx, playerY),
                        style = Stroke(width = 4f)
                    )
                }
                
                // Draw magnetic lane hints
                for (i in -1..1) {
                    val cxBottom = w/2 + i * laneWidthBottom
                    val cxTop = w/2 + i * laneWidthTop
                    drawLine(Color(0xFF1D4650).copy(alpha = 0.5f), Offset(cxBottom, h), Offset(cxTop, horizonY), strokeWidth = 1f)
                }
                
                // Draw entities
                for (i in state.entities.indices) {
                    val entity = state.entities[i]
                    if (entity.z > -10f && entity.z < 100f) {
                        val scale = 1f - (entity.z / 100f)
                        val cy = horizonY + (h - horizonY) * scale
                        val cxBottom = w/2 + (entity.startX) * laneWidthBottom
                        val cxTop = w/2 + (entity.startX) * laneWidthTop
                        val cx = cxTop + (cxBottom - cxTop) * scale
                        
                        val color = when(entity.type) {
                            EntityType.INTEL -> Cyan
                            EntityType.FUEL -> Amber
                            EntityType.MINE -> Red
                            EntityType.ENEMY -> Red
                            EntityType.WRECK -> Color.DarkGray
                            EntityType.FORK -> if (entity.value == 0) Cyan else if (entity.value == 1) Amber else Red
                        }
                        
                        val radius = 24f * scale + 6f
                        
                        // Draw Entity
                        entityPath.reset()
                        when (entity.type) {
                            EntityType.INTEL -> {
                                entityPath.moveTo(cx, cy - radius)
                                entityPath.lineTo(cx + radius, cy)
                                entityPath.lineTo(cx, cy + radius)
                                entityPath.lineTo(cx - radius, cy)
                                entityPath.close()
                                drawPath(entityPath, color)
                                drawPath(entityPath, Color.White, style = Stroke(width = 2f))
                            }
                            EntityType.FUEL -> {
                                drawRoundRect(color, topLeft = Offset(cx - radius*0.8f, cy - radius), size = Size(radius*1.6f, radius*2f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius*0.4f))
                                drawRoundRect(Color.White, topLeft = Offset(cx - radius*0.8f, cy - radius), size = Size(radius*1.6f, radius*2f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius*0.4f), style = Stroke(width = 2f))
                            }
                            EntityType.MINE -> {
                                drawCircle(Color(0xFF040A0C), radius + 3f, Offset(cx, cy))
                                drawCircle(color, radius, Offset(cx, cy))
                                drawCircle(Color.Black, radius * 0.4f, Offset(cx, cy))
                                drawLine(Color.Black, Offset(cx - radius, cy), Offset(cx + radius, cy), strokeWidth = 3f)
                                drawLine(Color.Black, Offset(cx, cy - radius), Offset(cx, cy + radius), strokeWidth = 3f)
                            }
                            EntityType.WRECK -> {
                                drawCircle(Color(0xFF040A0C), radius + 3f, Offset(cx, cy))
                                if (flareIntensity > 0f) {
                                    drawCircle(Amber.copy(alpha = flareIntensity * 0.2f), radius * 2f, Offset(cx, cy))
                                }
                                entityPath.moveTo(cx - radius, cy)
                                entityPath.lineTo(cx - radius*0.5f, cy - radius)
                                entityPath.lineTo(cx + radius*0.8f, cy - radius*0.4f)
                                entityPath.lineTo(cx + radius, cy + radius*0.6f)
                                entityPath.lineTo(cx - radius*0.2f, cy + radius)
                                entityPath.close()
                                drawPath(entityPath, color)
                            }
                            EntityType.ENEMY -> {
                                // Dynamic Enemy Boats based on type
                                drawCircle(Color(0xFF040A0C).copy(alpha = 0.5f), radius + 4f, Offset(cx, cy))
                                if (state.sonarPingActive && kotlin.math.hypot(cx - playerCx, cy - playerY) < state.sonarPingRadius) {
                                    drawCircle(Cyan, radius * 1.5f, Offset(cx, cy), style = Stroke(width = 2f))
                                    drawLine(Cyan, Offset(cx - radius * 2f, cy), Offset(cx + radius * 2f, cy), strokeWidth = 1f)
                                    drawLine(Cyan, Offset(cx, cy - radius * 2f), Offset(cx, cy + radius * 2f), strokeWidth = 1f)
                                }
                                entityPath.reset()
                                
                                val primaryColor = when (entity.enemyType) {
                                    EnemyBoatType.PATROL -> Color.Gray
                                    EnemyBoatType.INTERCEPTOR -> Color(0xFF556655)
                                    EnemyBoatType.POLICE -> Color(0xFF2244AA)
                                    EnemyBoatType.HUNTER -> Color(0xFF222222)
                                    EnemyBoatType.ARMORED -> Color(0xFF665544)
                                    EnemyBoatType.ELITE -> Color(0xFF990000)
                                }
                                
                                if (flareIntensity > 0f) {
                                    drawCircle(Amber.copy(alpha = flareIntensity * 0.4f), radius * 2.5f, Offset(cx, cy))
                                }
                                
                                when (entity.enemyType) {
                                    EnemyBoatType.PATROL, EnemyBoatType.POLICE -> {
                                        // Slim, fast
                                        entityPath.moveTo(cx, cy + radius) 
                                        entityPath.lineTo(cx + radius * 0.6f, cy - radius)
                                        entityPath.lineTo(cx - radius * 0.6f, cy - radius)
                                    }
                                    EnemyBoatType.INTERCEPTOR, EnemyBoatType.HUNTER -> {
                                        // Angular, aggressive
                                        entityPath.moveTo(cx, cy + radius * 1.2f) 
                                        entityPath.lineTo(cx + radius * 0.8f, cy - radius)
                                        entityPath.lineTo(cx, cy - radius * 0.5f)
                                        entityPath.lineTo(cx - radius * 0.8f, cy - radius)
                                    }
                                    EnemyBoatType.ARMORED -> {
                                        // Blocky, heavy
                                        entityPath.moveTo(cx, cy + radius)
                                        entityPath.lineTo(cx + radius, cy + radius * 0.5f)
                                        entityPath.lineTo(cx + radius * 1.2f, cy - radius)
                                        entityPath.lineTo(cx - radius * 1.2f, cy - radius)
                                        entityPath.lineTo(cx - radius, cy + radius * 0.5f)
                                    }
                                    EnemyBoatType.ELITE -> {
                                        // Futuristic
                                        entityPath.moveTo(cx, cy + radius * 1.5f) 
                                        entityPath.lineTo(cx + radius, cy - radius)
                                        entityPath.lineTo(cx + radius * 0.3f, cy - radius * 0.2f)
                                        entityPath.lineTo(cx - radius * 0.3f, cy - radius * 0.2f)
                                        entityPath.lineTo(cx - radius, cy - radius)
                                    }
                                }
                                entityPath.close()
                                drawPath(entityPath, primaryColor)
                                drawPath(entityPath, color, style = Stroke(width = 1.5f))
                                
                                // Draw light beam for enemy
                                beamPath.reset()
                                beamPath.moveTo(cx, cy + radius*0.5f)
                                beamPath.lineTo(cx - radius * 4f, cy + radius * 8f)
                                beamPath.lineTo(cx + radius * 4f, cy + radius * 8f)
                                beamPath.close()
                                
                                val beamColor = if (entity.enemyType == EnemyBoatType.POLICE) 
                                    if ((state.runElapsed * 10f).toInt() % 2 == 0) Color.Blue else Color.Red
                                else Red
                                drawPath(beamPath, beamColor.copy(alpha = 0.25f))
                            }
                            EntityType.FORK -> {
                                drawCircle(color, radius, Offset(cx, cy))
                                drawCircle(color, radius * 1.5f, Offset(cx, cy), style = Stroke(width = 2f))
                            }
                        }
                    }
                }
                
                // Draw Pursuers
                for (p in state.pursuers) {
                    val pScale = 1f - (p.y / 150f).coerceIn(0f, 1f)
                    val pY = horizonY + (h - horizonY) * (p.y / 100f)
                    val pCxBottom = w/2 + (p.x) * laneWidthBottom
                    val pCxTop = w/2 + (p.x) * laneWidthTop
                    val pCx = pCxTop + (pCxBottom - pCxTop) * (p.y / 100f)
                    
                    translate(left = pCx, top = pY) {
                        rotate(degrees = p.roll) {
                            
                            // Pursuer Wake
                            val pWakeSpread = 30f * pScale
                            val pWakeDeform = -p.velocityX * 50f
                            wakePath.reset()
                            wakePath.moveTo(0f, 0f)
                            wakePath.lineTo(-pWakeSpread + pWakeDeform, h - pY)
                            wakePath.lineTo(pWakeSpread + pWakeDeform, h - pY)
                            wakePath.close()
                            drawPath(wakePath, Color.White.copy(alpha = 0.1f * pScale))
                            
                            // Body
                            boatPath.reset()
                            val baseRadius = 15f * pScale
                            val primaryColor = when (p.type) {
                                EnemyBoatType.PATROL -> Color.Gray
                                EnemyBoatType.INTERCEPTOR -> Color(0xFF556655)
                                EnemyBoatType.POLICE -> Color(0xFF2244AA)
                                EnemyBoatType.HUNTER -> Color(0xFF222222)
                                EnemyBoatType.ARMORED -> Color(0xFF665544)
                                EnemyBoatType.ELITE -> Color(0xFF990000)
                            }
                            
                            when (p.type) {
                                EnemyBoatType.PATROL, EnemyBoatType.POLICE -> {
                                    boatPath.moveTo(0f, -baseRadius * 2f) 
                                    boatPath.lineTo(baseRadius * 0.8f, baseRadius)
                                    boatPath.lineTo(-baseRadius * 0.8f, baseRadius)
                                }
                                EnemyBoatType.INTERCEPTOR, EnemyBoatType.HUNTER -> {
                                    boatPath.moveTo(0f, -baseRadius * 2.5f) 
                                    boatPath.lineTo(baseRadius, baseRadius * 0.5f)
                                    boatPath.lineTo(0f, baseRadius)
                                    boatPath.lineTo(-baseRadius, baseRadius * 0.5f)
                                }
                                EnemyBoatType.ARMORED -> {
                                    boatPath.moveTo(0f, -baseRadius * 1.5f)
                                    boatPath.lineTo(baseRadius * 1.5f, -baseRadius * 0.5f)
                                    boatPath.lineTo(baseRadius * 1.2f, baseRadius * 1.5f)
                                    boatPath.lineTo(-baseRadius * 1.2f, baseRadius * 1.5f)
                                    boatPath.lineTo(-baseRadius * 1.5f, -baseRadius * 0.5f)
                                }
                                EnemyBoatType.ELITE -> {
                                    boatPath.moveTo(0f, -baseRadius * 3f) 
                                    boatPath.lineTo(baseRadius * 1.2f, baseRadius * 1.2f)
                                    boatPath.lineTo(baseRadius * 0.5f, baseRadius * 0.8f)
                                    boatPath.lineTo(-baseRadius * 0.5f, baseRadius * 0.8f)
                                    boatPath.lineTo(-baseRadius * 1.2f, baseRadius * 1.2f)
                                }
                            }
                            boatPath.close()
                            drawPath(boatPath, primaryColor)
                            drawPath(boatPath, Red, style = Stroke(width = 1.5f))
                            
                            // Lights & VFX
                            val beamColor = if (p.type == EnemyBoatType.POLICE) 
                                if ((state.runElapsed * 10f).toInt() % 2 == 0) Color.Blue else Color.Red
                            else Red
                            
                            // Warning glow if intercepting
                            if (p.state == PursuerState.INTERCEPT) {
                                drawCircle(Color.Red.copy(alpha=0.4f), baseRadius * 3f, Offset(0f, 0f))
                            }
                            
                            // Sonar Highlight
                            if (state.sonarPingActive && kotlin.math.hypot(pCx - playerCx, pY - playerY) < state.sonarPingRadius) {
                                drawCircle(Cyan, baseRadius * 4f, Offset(0f, 0f), style = Stroke(width = 3f))
                                drawCircle(Cyan.copy(alpha = 0.2f), baseRadius * 4f, Offset(0f, 0f))
                            }
                            if (flareIntensity > 0f) {
                                drawCircle(Amber.copy(alpha = flareIntensity * 0.4f), baseRadius * 3.5f, Offset(0f, 0f))
                            }
                            
                            // Engine Glow
                            drawCircle(Cyan.copy(alpha = 0.8f), baseRadius * 0.5f, Offset(0f, baseRadius * 1.2f))
                        }
                    }
                }
                
                // Hunting Tension Fog: dynamically obscures distant view when actively hunted
                val effectiveHuntingFog = huntingFog * (1f - flareIntensity)
                if (effectiveHuntingFog > 0f) {
                    val fogColor = Color(0xFF040A0C).copy(alpha = effectiveHuntingFog * 0.95f)
                    val fogBottom = horizonY + (h - horizonY) * 0.55f * effectiveHuntingFog // Creeps down towards player
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(fogColor, fogColor, Color.Transparent),
                            startY = horizonY - 100f,
                            endY = fogBottom
                        ),
                        topLeft = Offset(0f, 0f),
                        size = Size(w, fogBottom)
                    )
                }
                
                // Submerge Effect Overlay
                if (submergeFog > 0f) {
                    drawRect(
                        color = Color(0xFF0A2233).copy(alpha = submergeFog * 0.8f),
                        topLeft = Offset(0f, 0f),
                        size = Size(w, h)
                    )
                }

                // Draw player
                // Player Wake (scia) - Deforms based on steering and roll
                val wakeSpread = 30f + (state.fovOffset - 1.0f) * 150f // 30f normal, 60f boosted
                val wakeDeform = -state.playerVelocityX * 150f // Opposite to steering
                val wakeDeformMid = -state.playerVelocityX * 60f
                
                wakePath.reset()
                wakePath.moveTo(playerCx, playerY)
                // Left curve
                wakePath.quadraticBezierTo(
                    playerCx - wakeSpread * 0.5f + wakeDeformMid, playerY + (h - playerY) * 0.5f,
                    playerCx - wakeSpread + wakeDeform, h
                )
                // Base
                wakePath.lineTo(playerCx + wakeSpread + wakeDeform, h)
                // Right curve back
                wakePath.quadraticBezierTo(
                    playerCx + wakeSpread * 0.5f + wakeDeformMid, playerY + (h - playerY) * 0.5f,
                    playerCx, playerY
                )
                wakePath.close()
                
                drawPath(wakePath, Color.White.copy(alpha = 0.15f))
                
                // Procedural Waves (Sinusoidal reflection lines)
                val waveAlpha = 0.03f + (state.detection * 0.07f).coerceIn(0f, 0.1f)
                val waveColor = Color.White.copy(alpha = waveAlpha)
                for (i in 0..4) {
                    val lineY = horizonY + (state.runElapsed * 80f + i * (h - horizonY) / 5) % (h - horizonY)
                    val sinDeform = kotlin.math.sin(state.runElapsed * 3f + i) * 30f
                    if (lineY > horizonY) {
                        drawLine(waveColor, Offset(0f, lineY + sinDeform), Offset(w, lineY - sinDeform), strokeWidth = 2f)
                    }
                }
                
                // Draw Boat with Roll (Tilt)
                translate(left = playerCx, top = playerY) {
                    rotate(degrees = state.playerRoll) {
                        // Hull - Sleek stealth shape
                        boatPath.reset()
                        boatPath.moveTo(0f, -45f) // Bow (Nose)
                        boatPath.lineTo(22f, 15f) // Starboard (Right)
                        boatPath.lineTo(16f, 40f) // Right Stern
                        boatPath.lineTo(-16f, 40f) // Left Stern
                        boatPath.lineTo(-22f, 15f) // Port (Left)
                        boatPath.close()
                        
                        drawPath(boatPath, Color(0xFF132F38))
                        drawPath(boatPath, Color(0xFF234B58), style = Stroke(width = 2f)) // Edge highlight
                        
                        // Cockpit glass
                        cockpitPath.reset()
                        cockpitPath.moveTo(0f, -15f)
                        cockpitPath.lineTo(12f, 10f)
                        cockpitPath.lineTo(-12f, 10f)
                        cockpitPath.close()
                        drawPath(cockpitPath, Color.Black)
                        
                        // Engine exhausts / details
                        drawRect(Cyan, topLeft = Offset(-12f, 40f), size = Size(8f, 10f))
                        drawRect(Cyan, topLeft = Offset(4f, 40f), size = Size(8f, 10f))
                        
                        // Engine Glow based on boost
                        val glowScale = if (state.fovOffset > 1.0f) 2f else 1f
                        drawCircle(Cyan.copy(alpha = 0.6f), radius = 10f * glowScale, center = Offset(-8f, 48f))
                        drawCircle(Cyan.copy(alpha = 0.6f), radius = 10f * glowScale, center = Offset(8f, 48f))
                        
                        if (state.invulnerable > 0f) {
                            val alpha = (kotlin.math.sin(state.runElapsed * 20f) * 0.5f + 0.5f).toFloat() * 0.5f
                            drawPath(boatPath, Color.White.copy(alpha=alpha))
                        }
                    }
                }
            }
            
            // Dynamic Weather System (Rain) - Using Pre-calculated Primitive Buffer
            val globalRainSpeed = 1500f * state.fovOffset 
            val rainAngleOffset = 40f * state.playerVelocityX // Wind/rain slants based on boat steering
            val tensionRainAlpha = (0.2f + (state.detection * 0.4f)).coerceIn(0f, 1f)
            val rainColor = Color(0xFF6699AA).copy(alpha = tensionRainAlpha)
            val rainColorForeground = Color(0xFFAACCFF).copy(alpha = (tensionRainAlpha * 1.5f).coerceIn(0f, 1f))
            
            for (i in 0 until 150) {
                val idx = i * 5
                val startX = rainParticles[idx] % (w * 1.5f) - (w * 0.25f)
                val baseY = rainParticles[idx+1] % h
                val speedMod = rainParticles[idx+2]
                val dropLength = rainParticles[idx+3] * state.fovOffset
                val isForeground = rainParticles[idx+4] > 0.5f
                
                val strokeW = if (isForeground) 3f else 1.5f
                val depthSpeed = if (isForeground) 1.6f else 1f
                val activeColor = if (isForeground) rainColorForeground else rainColor
                
                val rawY = baseY + state.runElapsed * globalRainSpeed * speedMod * depthSpeed
                val finalY = rawY % (h * 1.2f) - (h * 0.1f)
                val finalX = startX + (rainAngleOffset * (finalY / h))
                
                drawLine(
                    color = activeColor,
                    start = Offset(finalX, finalY),
                    end = Offset(finalX - rainAngleOffset * 0.3f, finalY + dropLength * depthSpeed),
                    strokeWidth = strokeW
                )
            }
            
            // Dynamic Sea Spray System - Using Pre-calculated Primitive Buffer
            val dt = state.lastDeltaTime
            val emitCount = if (state.fovOffset > 1.0f) 5 else if (kotlin.math.abs(state.playerVelocityX) > 0.1f) 3 else 1
            
            // Player position from perspective
            val laneWidthBottomSpray = w * 0.4f * state.fovOffset
            val playerCx = w/2 + state.playerX * laneWidthBottomSpray
            val playerY = h * 0.85f
            
            // Emit new spray particles
            for (i in 0 until emitCount) {
                val idx = sprayIndex[0] * 6
                
                val ri = randomIndex[0]
                val r1 = randomTable[ri % 1024]
                val r2 = randomTable[(ri + 1) % 1024]
                val r3 = randomTable[(ri + 2) % 1024]
                val r4 = randomTable[(ri + 3) % 1024]
                val r5 = randomTable[(ri + 4) % 1024]
                randomIndex[0] = (ri + 5) % 1024
                
                // Bias spray origin based on steering (if steering left, spray comes more from the right side)
                val sprayOriginXOffset = (r1 - 0.5f) * 40f + state.playerVelocityX * 25f
                sprayParticles[idx] = playerCx + sprayOriginXOffset
                sprayParticles[idx+1] = playerY + 20f + (r2 - 0.5f) * 20f
                
                // Lateral velocity based on boat steering + roll + random scatter
                // Stronger lateral ejection mapping when steering
                val steerBias = -state.playerVelocityX * 500f // Pushes spray aggressively away from turn
                val rollBias = (state.playerRoll / 15f) * 120f // Tilt adds to spray momentum mapping
                sprayParticles[idx+2] = steerBias + rollBias + (r3 - 0.5f) * 200f
                
                // Vertical velocity based on boat speed
                val boostSpeed = if (state.fovOffset > 1.0f) 800f else 400f
                sprayParticles[idx+3] = boostSpeed + (r4 * 250f)
                
                val lifeTime = 0.3f + r5 * 0.4f
                sprayParticles[idx+4] = lifeTime // current life
                sprayParticles[idx+5] = lifeTime // max life
                
                sprayIndex[0] = (sprayIndex[0] + 1) % 150
            }
            
            // Update and Draw Spray
            for (i in 0 until 150) {
                val idx = i * 6
                if (sprayParticles[idx+4] > 0f) {
                    sprayParticles[idx] += sprayParticles[idx+2] * dt
                    sprayParticles[idx+1] += sprayParticles[idx+3] * dt
                    sprayParticles[idx+4] -= dt
                    
                    val lifeRatio = (sprayParticles[idx+4] / sprayParticles[idx+5]).coerceIn(0f, 1f)
                    if (lifeRatio > 0f) {
                        val alpha = lifeRatio * 0.5f
                        val radius = 4f + (1f - lifeRatio) * 12f
                        drawCircle(Color.White.copy(alpha = alpha), radius = radius, center = Offset(sprayParticles[idx], sprayParticles[idx+1]))
                    }
                }
            }
        }
    }
}

@Composable
fun ProfilerDashboard(state: GameState) {
    val fps = if (state.lastDeltaTime > 0f) (1f / state.lastDeltaTime).toInt() else 0
    val lane = kotlin.math.round(state.playerX).toInt()
    
    val runtime = Runtime.getRuntime()
    val usedMemMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    val maxMemMb = runtime.maxMemory() / (1024 * 1024)
    
    // Fixed array for frame history
    val historySize = 60
    val frameHistory = remember { FloatArray(historySize) }
    val historyIndex = remember { mutableIntStateOf(0) }
    
    // Zero-allocation path for graph
    val graphPath = remember { Path() }
    
    // Update history tracking
    LaunchedEffect(state.runElapsed) {
        if (state.lastDeltaTime > 0f) {
            frameHistory[historyIndex.intValue % historySize] = state.lastDeltaTime
            historyIndex.intValue++
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp, start = 16.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.85f))
                .border(1.dp, Color.Green.copy(alpha = 0.3f))
                .padding(12.dp)
                .width(220.dp)
        ) {
            Text("PERFORMANCE PROFILER", color = Color.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("FPS: $fps (dt: ${String.format("%.3f", state.lastDeltaTime)}s)", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("MEM: $usedMemMb MB / $maxMemMb MB", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("ENTITIES: ${state.entities.size}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            
            Spacer(Modifier.height(12.dp))
            Text("FRAME TIME (TARGET: 16ms)", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(4.dp))
            
            Canvas(modifier = Modifier.fillMaxWidth().height(50.dp).background(Color(0xFF0F1517))) {
                val w = size.width
                val h = size.height
                val stepX = w / (historySize - 1).toFloat()
                val maxDt = 0.05f // scale up to 50ms
                
                // Draw Target Line (16.6ms)
                val y16 = h - (0.0166f / maxDt) * h
                drawLine(Color.Green.copy(alpha=0.5f), Offset(0f, y16), Offset(w, y16), strokeWidth = 1f)
                
                graphPath.reset()
                for (i in 0 until historySize) {
                    val idx = (historyIndex.intValue + i) % historySize
                    val dt = frameHistory[idx]
                    val x = i * stepX
                    val y = h - (dt / maxDt).coerceIn(0f, 1f) * h
                    
                    if (i == 0) {
                        graphPath.moveTo(x, y)
                    } else {
                        graphPath.lineTo(x, y)
                    }
                }
                drawPath(graphPath, Color.Cyan, style = Stroke(width = 2f))
            }
            
            Spacer(Modifier.height(12.dp))
            Text("PHYSICS STATE", color = Color.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("POS X: ${String.format("%.3f", state.playerX)} [Lane $lane]", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("VEL X: ${String.format("%.3f", state.playerVelocityX)}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("ROLL : ${String.format("%.3f", state.playerRoll)}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("FOV  : ${String.format("%.3f", state.fovOffset)}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun GameLoop(state: GameState, viewModel: GameViewModel) {
    if (state.mode == GameMode.RUNNING || state.mode == GameMode.PATCHING) {
        LaunchedEffect(state.mode) {
            var lastFrameTime = withFrameNanos { it }
            while (isActive) {
                val frameTime = withFrameNanos { it }
                val delta = (frameTime - lastFrameTime) / 1_000_000_000f
                lastFrameTime = frameTime
                val safeDelta = kotlin.math.min(delta, 0.1f)
                if (state.mode == GameMode.RUNNING) {
                    viewModel.updateGame(safeDelta)
                } else if (state.mode == GameMode.PATCHING) {
                    viewModel.updatePatching(safeDelta)
                }
            }
        }
    }
}

@Composable
fun PatchingMiniGameOverlay(state: GameState, viewModel: GameViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "EMERGENZA SCAFO - CHIUDI LE FALLE!",
                color = Red,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
        
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val w = maxWidth.value
            val h = maxHeight.value
            state.leakNodes.forEach { node ->
                if (!node.fixed) {
                    Box(
                        modifier = Modifier
                            .offset(x = (node.x * w).dp - 24.dp, y = (node.y * h).dp - 24.dp)
                            .size(48.dp)
                            .background(Red.copy(alpha = 0.8f), androidx.compose.foundation.shape.CircleShape)
                            .border(2.dp, Color.White, androidx.compose.foundation.shape.CircleShape)
                            .clickable {
                                viewModel.patchNode(node.id)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("PATCH", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .offset(x = (node.x * w).dp - 24.dp, y = (node.y * h).dp - 24.dp)
                            .size(48.dp)
                            .background(Color.Green.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
                            .border(2.dp, Color.Green, androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", color = Color.White, fontSize = 24.sp)
                    }
                }
            }
        }
    }
}


@Composable
fun VisualRadar(state: GameState) {
    Box(
        modifier = Modifier
            .padding(top = 8.dp)
            .size(120.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(Color(0xFF001100).copy(alpha = 0.8f))
            .border(2.dp, Cyan, androidx.compose.foundation.shape.CircleShape)
    ) {
        val sweepAngle = (state.runElapsed * 150f) % 360f
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2
            
            // Draw grid
            drawCircle(Cyan.copy(alpha = 0.3f), radius = radius * 0.5f, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
            drawCircle(Cyan.copy(alpha = 0.3f), radius = radius, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
            drawLine(Cyan.copy(alpha = 0.3f), Offset(center.x, 0f), Offset(center.x, size.height))
            drawLine(Cyan.copy(alpha = 0.3f), Offset(0f, center.y), Offset(size.width, center.y))
            
            // Draw Sweep
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(Color.Transparent, Cyan.copy(alpha = 0.6f)),
                    center = center
                ),
                startAngle = sweepAngle - 90f,
                sweepAngle = 90f,
                useCenter = true,
                topLeft = Offset(0f, 0f),
                size = size
            )
            drawLine(
                color = Cyan,
                start = center,
                end = Offset(
                    center.x + kotlin.math.cos(Math.toRadians(sweepAngle.toDouble())).toFloat() * radius,
                    center.y + kotlin.math.sin(Math.toRadians(sweepAngle.toDouble())).toFloat() * radius
                ),
                strokeWidth = 2f
            )
            
            fun drawBlip(mapX: Float, mapY: Float, color: Color, sizeDp: Float) {
                val blipX = center.x + mapX * radius
                val blipY = center.y + mapY * radius
                
                // Only draw if inside circle
                if (mapX*mapX + mapY*mapY <= 1f) {
                    val angleToBlip = Math.toDegrees(kotlin.math.atan2(mapY.toDouble(), mapX.toDouble())).toFloat()
                    val normBlip = if (angleToBlip < 0) angleToBlip + 360f else angleToBlip
                    val angleDiff = (sweepAngle - normBlip + 360f) % 360f
                    
                    val blipAlpha = if (angleDiff < 120f) 1f - (angleDiff / 120f) else 0.15f
                    drawCircle(color.copy(alpha = blipAlpha), radius = sizeDp.dp.toPx(), center = Offset(blipX, blipY))
                }
            }
            
            // Draw Blips (Entities)
            state.entities.forEach { e ->
                if (!e.resolved && e.z > 0 && e.z < 100f) {
                    val mapX = (e.startX - state.playerX) / 2f
                    val mapY = -(e.z / 100f)
                    val color = when(e.type) {
                        EntityType.ENEMY, EntityType.MINE -> Red
                        EntityType.FUEL, EntityType.INTEL -> Amber
                        else -> Color.Gray
                    }
                    drawBlip(mapX, mapY, color, 3f)
                }
            }
            
            // Draw Pursuers
            state.pursuers.forEach { p ->
                val mapX = (p.x - state.playerX) / 2f
                val mapY = (p.y - 85f) / 100f
                drawBlip(mapX, mapY, Red, 4f)
            }
            
            // Fake Noise Blips
            for (i in 0..3) {
                val noiseTime = state.runElapsed * 0.2f + i * 13.37f
                val mapX = kotlin.math.sin(noiseTime * 2.1f).toFloat() * 0.8f
                val mapY = kotlin.math.cos(noiseTime * 1.7f).toFloat() * 0.8f
                drawBlip(mapX, mapY, Color.Green, 2f)
            }
            
            // Player
            drawCircle(Cyan, radius = 2.dp.toPx(), center = center)
        }
    }
}


@Composable
fun MiniNavMap(state: GameState) {
    val chapter = CHAPTERS.getOrNull(state.currentChapterIndex) ?: return
    val currentSector = chapter.sectors.getOrNull(state.sectorIndex) ?: chapter.sectors.last()
    val distanceLeft = kotlin.math.max(0f, currentSector.duration - state.sectorElapsed)
    
    Box(
        modifier = Modifier
            .padding(top = 16.dp)
            .width(120.dp)
            .height(160.dp)
            .background(Color(0xFF031A33).copy(alpha = 0.6f))
            .border(1.dp, Cyan.copy(alpha=0.3f))
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("ROTTA", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text("DST: ${String.format("%.1f", distanceLeft)}m", color = Amber, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val paddingX = 8.dp.toPx()
                    val startY = 8.dp.toPx()
                    val endY = size.height - 8.dp.toPx()
                    val stepY = if (chapter.sectors.size > 1) (endY - startY) / (chapter.sectors.size - 1) else 0f
                    
                    // Draw base line
                    for (i in 0 until chapter.sectors.size - 1) {
                        val color = if (i < state.sectorIndex) Cyan else Color.DarkGray
                        drawLine(
                            color = color,
                            start = Offset(paddingX, startY + i * stepY),
                            end = Offset(paddingX, startY + (i + 1) * stepY),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                    
                    // Draw active progress line
                    if (state.sectorIndex < chapter.sectors.size - 1) {
                        val progress = (state.sectorElapsed / currentSector.duration).coerceIn(0f, 1f)
                        val currentStepY = startY + state.sectorIndex * stepY
                        val nextStepY = startY + (state.sectorIndex + 1) * stepY
                        drawLine(
                            color = Amber,
                            start = Offset(paddingX, currentStepY),
                            end = Offset(paddingX, currentStepY + (nextStepY - currentStepY) * progress),
                            strokeWidth = 2.dp.toPx()
                        )
                        
                        // Player indicator
                        val pulse = (kotlin.math.sin(state.runElapsed * 10f) * 2f).dp.toPx()
                        drawCircle(
                            color = Cyan,
                            radius = 3.dp.toPx() + pulse,
                            center = Offset(paddingX, currentStepY + (nextStepY - currentStepY) * progress)
                        )
                    } else if (state.sectorIndex == chapter.sectors.size - 1) {
                        val progress = (state.sectorElapsed / currentSector.duration).coerceIn(0f, 1f)
                        val currentStepY = startY + state.sectorIndex * stepY
                        val pulse = (kotlin.math.sin(state.runElapsed * 10f) * 2f).dp.toPx()
                        val downward = progress * 10.dp.toPx()
                        drawLine(
                            color = Amber,
                            start = Offset(paddingX, currentStepY),
                            end = Offset(paddingX, currentStepY + downward),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawCircle(
                            color = Cyan,
                            radius = 3.dp.toPx() + pulse,
                            center = Offset(paddingX, currentStepY + downward)
                        )
                    }
                    
                    // Draw nodes
                    chapter.sectors.forEachIndexed { index, sector ->
                        val isCurrent = index == state.sectorIndex
                        val isPast = index < state.sectorIndex
                        val nodeColor = if (isCurrent) Amber else if (isPast) Cyan else Color.Gray
                        
                        drawCircle(
                            color = nodeColor,
                            radius = if (isCurrent) 4.dp.toPx() else 3.dp.toPx(),
                            center = Offset(paddingX, startY + index * stepY)
                        )
                        
                        val textPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor(if (isCurrent) "#FFB45F" else if (isPast) "#2BE7E0" else "#888888")
                            textSize = 24f
                            typeface = android.graphics.Typeface.MONOSPACE
                        }
                        
                        drawContext.canvas.nativeCanvas.drawText(
                            sector.tag.take(6),
                            paddingX + 12.dp.toPx(),
                            startY + index * stepY + 8f,
                            textPaint
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TacticalMapOverlay(state: GameState, viewModel: GameViewModel) {
    val chapter = CHAPTERS[state.currentChapterIndex]
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { viewModel.toggleTacticalMap() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight(0.8f)
                .fillMaxWidth(0.8f)
                .border(2.dp, Cyan)
                .background(Color(0xFF031A33).copy(alpha = 0.9f))
                .padding(24.dp)
        ) {
            Text("MAPPA TATTICA // TRAIETTORIA", color = Cyan, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(8.dp))
            Text(chapter.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(modifier = Modifier.fillMaxSize()) {
                // Draw path line
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lineX = 12.dp.toPx()
                    val startY = 12.dp.toPx()
                    val endY = size.height - 12.dp.toPx()
                    val stepY = (endY - startY) / (chapter.sectors.size.coerceAtLeast(2) - 1)
                    
                    for (i in 0 until chapter.sectors.size - 1) {
                        val color = if (i < state.sectorIndex) Cyan else Color.Gray.copy(alpha = 0.5f)
                        val strokeWidth = if (i < state.sectorIndex) 4.dp.toPx() else 2.dp.toPx()
                        drawLine(
                            color = color,
                            start = Offset(lineX, startY + i * stepY),
                            end = Offset(lineX, startY + (i + 1) * stepY),
                            strokeWidth = strokeWidth
                        )
                    }
                }
                
                // Draw nodes
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    chapter.sectors.forEachIndexed { index, sector ->
                        val isCurrent = index == state.sectorIndex
                        val isPast = index < state.sectorIndex
                        val nodeColor = if (isCurrent) Amber else if (isPast) Cyan else Color.Gray
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(if (isCurrent) nodeColor.copy(alpha=0.3f) else Color.Transparent, androidx.compose.foundation.shape.CircleShape)
                                    .border(2.dp, nodeColor, androidx.compose.foundation.shape.CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (sector.extract) {
                                    Text("E", color = nodeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                } else if (sector.fork) {
                                    Text("?", color = nodeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Box(modifier = Modifier.size(8.dp).background(nodeColor, androidx.compose.foundation.shape.CircleShape))
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column {
                                Text(
                                    text = sector.title, 
                                    color = if (isCurrent) Color.White else if (isPast) Color.LightGray else Color.Gray,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (sector.extract) "OBJ: ESTRAZIONE" else if (sector.fork) "OBJ: BIVIO TATTICO" else "NAV: TRANSIZIONE",
                                    color = if (isCurrent) Amber else nodeColor.copy(alpha = 0.8f),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DamageControlOverlay(state: GameState, viewModel: GameViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = true) { viewModel.toggleDamageControl() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight(0.7f)
                .fillMaxWidth(0.85f)
                .border(2.dp, Amber)
                .background(Color(0xFF031A33).copy(alpha = 0.95f))
                .clickable(enabled = false) {} // block clicks
                .padding(24.dp)
        ) {
            Text("CONTROLLO DANNI // PARATIE STAGNE", color = Amber, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Sigilla manualmente i compartimenti compromessi per fermare l'allagamento. Attenzione: sigillare paratie intatte riduce l'efficienza.", color = Color.White, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(24.dp))
            
            // Draw Submarine layout
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                state.compartments.forEach { comp ->
                    val color = when (comp.state) {
                        CompartmentState.NORMAL -> Color.Green
                        CompartmentState.SEALED -> Amber
                        CompartmentState.BREACHED -> Red
                    }
                    val flash = if (comp.state == CompartmentState.BREACHED) (kotlin.math.sin(state.runElapsed * 15f) * 0.5f + 0.5f).toFloat() else 1f
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .height(140.dp)
                                .width(60.dp)
                                .background(color.copy(alpha = 0.2f * flash))
                                .border(2.dp, color.copy(alpha = flash))
                                .clickable { viewModel.toggleCompartmentSeal(comp.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(comp.id, color = Color.White, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (comp.state == CompartmentState.SEALED) "X" else "!",
                                    color = color.copy(alpha = flash),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(comp.name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(comp.state.name, color = color, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun RadarWidget(state: GameState, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val pulsePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = modifier
            .size(100.dp)
            .background(Color(0xFF040A0C).copy(alpha = 0.8f), androidx.compose.foundation.shape.CircleShape)
            .border(2.dp, Cyan.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = size.width / 2f - 2.dp.toPx()

            // Draw radar grid
            drawCircle(color = Cyan.copy(alpha = 0.2f), radius = radius, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
            drawCircle(color = Cyan.copy(alpha = 0.15f), radius = radius * 0.66f, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
            drawCircle(color = Cyan.copy(alpha = 0.1f), radius = radius * 0.33f, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
            drawLine(color = Cyan.copy(alpha = 0.15f), start = Offset(cx, 0f), end = Offset(cx, size.height))
            drawLine(color = Cyan.copy(alpha = 0.15f), start = Offset(0f, cy), end = Offset(size.width, cy))

            // Draw expanding pulse
            val pulseRadius = pulsePhase * radius
            val pulseAlpha = 1f - pulsePhase
            drawCircle(
                color = Cyan.copy(alpha = pulseAlpha * 0.6f),
                radius = pulseRadius,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Cyan.copy(alpha = pulseAlpha * 0.15f),
                radius = pulseRadius
            )

            // Calculate rotation for relative heading
            val headingOffset = state.playerVelocityX * 15f
            
            rotate(degrees = -headingOffset, pivot = Offset(cx, cy)) {
                val maxDist = 120f
                for (e in state.entities) {
                    if (e.resolved) continue
                    val dx = e.startX - state.playerX
                    val dy = e.z // Distance ahead
                    
                    val plotX = cx + (dx / 2.0f) * radius
                    val plotY = cy - (dy / maxDist) * radius
                    
                    val distToCenterSq = (plotX - cx) * (plotX - cx) + (plotY - cy) * (plotY - cy)
                    if (distToCenterSq <= radius * radius) {
                        val dotColor = when(e.type) {
                            EntityType.MINE -> Red
                            EntityType.WRECK -> Amber
                            EntityType.FUEL -> Color.Green
                            EntityType.INTEL -> Cyan
                            EntityType.FORK -> Color.White
                            EntityType.ENEMY -> Red
                        }
                        
                        val distToCenter = kotlin.math.sqrt(distToCenterSq)
                        val distRatio = distToCenter / radius
                        val blipIntensity = if (pulsePhase > distRatio && pulsePhase < distRatio + 0.2f) {
                            1f - ((pulsePhase - distRatio) / 0.2f)
                        } else {
                            0.2f
                        }
                        
                        drawCircle(color = dotColor.copy(alpha = 0.5f + 0.5f * blipIntensity), radius = 3.dp.toPx(), center = Offset(plotX, plotY))
                        if (blipIntensity > 0.1f) {
                            drawCircle(color = dotColor.copy(alpha = blipIntensity * 0.5f), radius = 6.dp.toPx(), center = Offset(plotX, plotY))
                        }
                    }
                }
                
                for (p in state.pursuers) {
                    val dx = p.x - state.playerX
                    val dy = 85f - p.y // 85 is player Y, so this is distance ahead
                    
                    val plotX = cx + (dx / 2.0f) * radius
                    val plotY = cy - (dy / maxDist) * radius
                    
                    val distToCenterSq = (plotX - cx) * (plotX - cx) + (plotY - cy) * (plotY - cy)
                    if (distToCenterSq <= radius * radius) {
                        val distToCenter = kotlin.math.sqrt(distToCenterSq)
                        val distRatio = distToCenter / radius
                        val blipIntensity = if (pulsePhase > distRatio && pulsePhase < distRatio + 0.2f) {
                            1f - ((pulsePhase - distRatio) / 0.2f)
                        } else {
                            0.2f
                        }
                        
                        drawCircle(color = Red.copy(alpha = 0.5f + 0.5f * blipIntensity), radius = 4.dp.toPx(), center = Offset(plotX, plotY))
                        if (blipIntensity > 0.1f) {
                            drawCircle(color = Red.copy(alpha = blipIntensity * 0.5f), radius = 8.dp.toPx(), center = Offset(plotX, plotY))
                        }
                    }
                }
                
                drawPath(
                    path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(cx, cy - 6.dp.toPx())
                        lineTo(cx + 4.dp.toPx(), cy + 4.dp.toPx())
                        lineTo(cx - 4.dp.toPx(), cy + 4.dp.toPx())
                        close()
                    },
                    color = Color.Green
                )
            }
        }
    }
}

@Composable
fun VerticalDepthGauge(state: GameState, modifier: Modifier = Modifier) {
    val maxDepth = 150f
    val currentDepth = state.playerDepth
    val seabedDepth = state.seabedDepth
    
    val playerPercent = (currentDepth / maxDepth).coerceIn(0f, 1f)
    val seabedPercent = (seabedDepth / maxDepth).coerceIn(0f, 1f)
    
    val safeDepthThreshold = 55f
    val safePercent = (safeDepthThreshold / maxDepth).coerceIn(0f, 1f)
    val isCritical = currentDepth > safeDepthThreshold
    
    val flashAlpha = if (isCritical) (kotlin.math.sin(state.runElapsed * 20f) * 0.5f + 0.5f).toFloat() else 1f
    val borderColor = if (isCritical) Red.copy(alpha = flashAlpha) else Cyan.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .width(40.dp)
            .height(200.dp)
            .background(Color(0xFF040A0C).copy(alpha = 0.8f), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .border(2.dp, borderColor, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw safe threshold zone
            val safeY = size.height * safePercent
            drawRect(
                color = Red.copy(alpha = 0.15f),
                topLeft = Offset(0f, safeY),
                size = androidx.compose.ui.geometry.Size(size.width, size.height - safeY)
            )
            drawLine(
                color = Red.copy(alpha = 0.8f),
                start = Offset(0f, safeY),
                end = Offset(size.width, safeY),
                strokeWidth = 2f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )

            // Draw scale lines
            val ticks = 10
            for (i in 0..ticks) {
                val y = size.height * (i.toFloat() / ticks)
                val isMajor = i % 2 == 0
                drawLine(
                    color = Cyan.copy(alpha = if(isMajor) 0.5f else 0.2f),
                    start = Offset(if(isMajor) 0f else 8f, y),
                    end = Offset(if(isMajor) 12f else 12f, y),
                    strokeWidth = if(isMajor) 2f else 1f
                )
            }
            
            // Draw seabed area (solid rect from seabed depth to bottom)
            val seabedY = size.height * seabedPercent
            drawRect(
                color = Amber.copy(alpha = 0.3f),
                topLeft = Offset(0f, seabedY),
                size = androidx.compose.ui.geometry.Size(size.width, size.height - seabedY)
            )
            // Seabed line
            drawLine(
                color = Amber.copy(alpha = 0.8f),
                start = Offset(0f, seabedY),
                end = Offset(size.width, seabedY),
                strokeWidth = 2f
            )
            
            // Draw player indicator
            val playerY = size.height * playerPercent
            
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width - 12f, playerY)
                lineTo(size.width, playerY - 6f)
                lineTo(size.width, playerY + 6f)
                close()
            }
            drawPath(path, color = Cyan)
            
            // Player horizontal line
            drawLine(
                color = Cyan,
                start = Offset(4f, playerY),
                end = Offset(size.width, playerY),
                strokeWidth = 2f
            )
        }
        
        // Depth Text at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 26.dp)
                .background(Color.Black.copy(alpha=0.6f))
                .padding(2.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${currentDepth.toInt()}m",
                    color = if (isCritical) Red.copy(alpha = flashAlpha) else Cyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                // Real-time hydrostatic pressure (approx 1 atm = 101 kPa, +10 kPa per meter)
                val pressureKpa = 101 + (currentDepth * 10.05f).toInt()
                Text(
                    text = "${pressureKpa}kPa",
                    color = if (isCritical) Red.copy(alpha = flashAlpha) else Color.Gray,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        
        if (isCritical) {
            Text(
                text = "CRIT",
                color = Red.copy(alpha = flashAlpha),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-16).dp)
            )
        }
        // Seabed Text at top of seabed
        val seabedOffset = (seabedPercent * 200) - 100 // Map 0..1 to -100..100
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = seabedOffset.dp)
                .padding(2.dp)
        ) {
            Text(
                text = "FONDO",
                color = Amber.copy(alpha = 0.8f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun EventLogOverlay(state: GameState, viewModel: GameViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = true) { viewModel.toggleEventLog() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight(0.8f)
                .fillMaxWidth(0.6f)
                .border(2.dp, Cyan)
                .background(Color(0xFF031A33).copy(alpha = 0.95f))
                .padding(24.dp)
                .clickable(enabled = false) {}
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("ARCHIVIO EVENTI DI BORDO", color = Cyan, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("${state.terminalFeed.size} VOCI", color = Cyan.copy(alpha=0.5f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Cyan.copy(alpha=0.5f)))
            Spacer(modifier = Modifier.height(16.dp))
            
            androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.terminalFeed.size) { index ->
                    val msg = state.terminalFeed[index]
                    val isCritical = msg.color == "red"
                    val flashAlpha = if (isCritical) (kotlin.math.sin(state.runElapsed * 10f) * 0.2f + 0.8f).toFloat() else 1f
                    val tColor = when (msg.color) { "red" -> Red.copy(alpha = flashAlpha); "amber" -> Amber; else -> Cyan }
                    
                    val mins = (msg.timestamp / 60).toInt()
                    val secs = (msg.timestamp % 60).toInt()
                    val timeStr = String.format("T+%02d:%02d", mins, secs)
                    
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                        Text(timeStr, color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(60.dp))
                        Text(msg.text, color = tColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
