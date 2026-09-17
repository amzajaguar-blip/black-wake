import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

hud_old = """                Spacer(modifier = Modifier.height(8.dp))
                Text("SCAFO: ${state.hull.toInt()}", color = if (state.hull > 30) Color.White else Red)
                Text("CARB: ${state.fuel.toInt()}", color = if (state.fuel > 20) Amber else Red)"""

hud_new = """                Spacer(modifier = Modifier.height(8.dp))
                // Oxygen Bar
                Text("O2: ${state.oxygen.toInt()}%", color = if (state.oxygen > 30) Cyan else Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.size(100.dp, 8.dp).background(Color.DarkGray).border(1.dp, Color.Black)) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(state.oxygen / 100f).background(if (state.oxygen > 30) Cyan else Red))
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Hull Bar
                Text("SCAFO: ${state.hull.toInt()}%", color = if (state.hull > 30) Color.Green else Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.size(100.dp, 8.dp).background(Color.DarkGray).border(1.dp, Color.Black)) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(state.hull / 100f).background(if (state.hull > 30) Color.Green else Red))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("CARB: ${state.fuel.toInt()}", color = if (state.fuel > 20) Amber else Red)"""
code = code.replace(hud_old, hud_new)

buttons_old = """                Button(
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
                ) { Text("SIL") }"""

buttons_new = """                Button(
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
                                viewModel.setSilent(true)
                                tryAwaitRelease()
                                viewModel.setSilent(false)
                            }
                        )
                    },
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) { Text("SIL") }"""
code = code.replace(buttons_old, buttons_new)

# Add visual submerged effect (tint the screen blueish when submerged)
water_tint_old = """    val waterTint = androidx.compose.ui.graphics.lerp(tensionColor, BlackTideColor, ((state.detection - 0.75f) / 0.25f).coerceIn(0f, 1f))"""
water_tint_new = """    val waterTint = androidx.compose.ui.graphics.lerp(tensionColor, BlackTideColor, ((state.detection - 0.75f) / 0.25f).coerceIn(0f, 1f))
    
    val submergeFog by animateFloatAsState(
        targetValue = if (state.isSubmerged) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "submergeFog"
    )"""
code = code.replace(water_tint_old, water_tint_new)

player_draw_old = """                // Draw player
                // Player Wake (scia) - Deforms based on steering and roll"""
player_draw_new = """                // Submerge Effect Overlay
                if (submergeFog > 0f) {
                    drawRect(
                        color = Color(0xFF0A2233).copy(alpha = submergeFog * 0.8f),
                        topLeft = Offset(0f, 0f),
                        size = Size(w, h)
                    )
                }

                // Draw player
                // Player Wake (scia) - Deforms based on steering and roll"""
code = code.replace(player_draw_old, player_draw_new)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

print("GameScreen UI patched with Dive logic and Hull bar")
