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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
val SeaColor = Color(0xFF041018)

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val state by viewModel.uiState.collectAsState()
    var showProfiler by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize().background(SeaColor)) {
        if (state.mode == GameMode.RUNNING || state.mode == GameMode.PAUSED || state.mode == GameMode.DEBRIEF) {
            GameCanvas(state, viewModel)
            Hud(state, viewModel)
            if (showProfiler) {
                ProfilerDashboard(state)
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
        Text("GARAGE", fontSize = 40.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Text("INTEL DISPONIBILE: ${state.intelBank}", color = Cyan)
        Spacer(modifier = Modifier.height(16.dp))
        
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
fun Hud(state: GameState, viewModel: GameViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                if (state.messageFlash != null) {
                    Text(state.messageFlash, color = if (state.messageColor == "red") Red else if (state.messageColor == "amber") Amber else Cyan, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("SCAFO: ${state.hull.toInt()}", color = if (state.hull > 30) Color.White else Red)
                Text("CARB: ${state.fuel.toInt()}", color = if (state.fuel > 20) Amber else Red)
                Text("INTEL: ${state.intel}", color = Cyan)
                if (state.comboMultiplier > 1) {
                    Text("COMBO x${state.comboMultiplier}", color = Cyan, fontWeight = FontWeight.Bold)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("RILEVAMENTO", color = Color.Gray, fontSize = 12.sp)
                Text("${(state.detection * 100).toInt()}%", color = if (state.detection > 0.8f) Red else Cyan, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Button(onClick = { viewModel.togglePause() }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("PAUSA")
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        
        if (state.forkNotice != null) {
            Text(state.forkNotice, color = Cyan, modifier = Modifier.align(Alignment.CenterHorizontally).background(Color.Black.copy(0.7f)).padding(8.dp))
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    modifier = Modifier.size(64.dp).pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                viewModel.setSilent(true)
                                tryAwaitRelease()
                                viewModel.setSilent(false)
                            }
                        )
                    },
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) { Text("SIL") }
                Button(
                    modifier = Modifier.size(64.dp).pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                viewModel.setBoost(true)
                                tryAwaitRelease()
                                viewModel.setBoost(false)
                            }
                        )
                    },
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = Amber)
                ) { Text("BST", color = Color.Black) }
            }
        }
    }
}

@Composable
fun GameCanvas(state: GameState, viewModel: GameViewModel) {
    // Water effect based on detection (Continuous interpolation)
    val tensionColor = androidx.compose.ui.graphics.lerp(SeaColor, Color(0xFF0F1517), (state.detection / 0.75f).coerceIn(0f, 1f))
    val waterTint = androidx.compose.ui.graphics.lerp(tensionColor, Color(0xFF1A0505), ((state.detection - 0.75f) / 0.25f).coerceIn(0f, 1f))
    
    val wakePath = remember { Path() }
    val beamPath = remember { Path() }
    val boatPath = remember { Path() }
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
            val totalShake = state.cameraShake + tensionShake
            val shakeX = if (totalShake > 0f) (Math.random().toFloat() - 0.5f) * 20f * totalShake else 0f
            val shakeY = if (totalShake > 0f) (Math.random().toFloat() - 0.5f) * 20f * totalShake else 0f
            
            translate(left = shakeX, top = shakeY) {
                
                // Perspective projection parameters (affected by FOV)
                val fovScale = state.fovOffset
                val horizonY = h * 0.35f
                val laneWidthBottom = w * 0.4f * fovScale
                val laneWidthTop = w * 0.05f * fovScale
                
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
                                entityPath.moveTo(cx - radius, cy)
                                entityPath.lineTo(cx - radius*0.5f, cy - radius)
                                entityPath.lineTo(cx + radius*0.8f, cy - radius*0.4f)
                                entityPath.lineTo(cx + radius, cy + radius*0.6f)
                                entityPath.lineTo(cx - radius*0.2f, cy + radius)
                                entityPath.close()
                                drawPath(entityPath, color)
                            }
                            EntityType.ENEMY -> {
                                drawCircle(Color(0xFF040A0C), radius + 3f, Offset(cx, cy))
                                entityPath.moveTo(cx, cy + radius) // nose pointing at player
                                entityPath.lineTo(cx + radius, cy - radius)
                                entityPath.lineTo(cx, cy - radius * 0.5f)
                                entityPath.lineTo(cx - radius, cy - radius)
                                entityPath.close()
                                drawPath(entityPath, color)
                                
                                // Draw light beam for enemy
                                beamPath.reset()
                                beamPath.moveTo(cx, cy + radius*0.5f)
                                beamPath.lineTo(cx - radius * 4f, cy + radius * 8f)
                                beamPath.lineTo(cx + radius * 4f, cy + radius * 8f)
                                beamPath.close()
                                drawPath(beamPath, Red.copy(alpha = 0.25f))
                            }
                            EntityType.FORK -> {
                                drawCircle(color, radius, Offset(cx, cy))
                                drawCircle(color, radius * 1.5f, Offset(cx, cy), style = Stroke(width = 2f))
                            }
                        }
                    }
                }
                
                // Draw player
                val playerY = h * 0.85f
                val targetCxBottom = w/2 + (state.playerX) * laneWidthBottom
                val playerCx = targetCxBottom
                
                // Player Wake (scia) - Deforms based on steering and roll
                val wakeSpread = 30f + (state.fovOffset - 1.0f) * 150f // 30f normal, 60f boosted
                val wakeDeform = -state.playerVelocityX * 100f // Opposite to steering
                wakePath.reset()
                wakePath.moveTo(playerCx, playerY)
                wakePath.lineTo(playerCx - wakeSpread + wakeDeform, h)
                wakePath.lineTo(playerCx + wakeSpread + wakeDeform, h)
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
                
                sprayParticles[idx] = playerCx + (r1 - 0.5f) * 40f
                sprayParticles[idx+1] = playerY + 20f + (r2 - 0.5f) * 20f
                // Lateral velocity based on boat steering + random scatter
                sprayParticles[idx+2] = -state.playerVelocityX * 300f + (r3 - 0.5f) * 150f
                // Vertical velocity based on boat speed
                val boostSpeed = if (state.fovOffset > 1.0f) 600f else 300f
                sprayParticles[idx+3] = boostSpeed + (r4 * 200f)
                
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
