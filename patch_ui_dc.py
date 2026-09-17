import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

hud_old = """                Button(
                    onClick = { viewModel.toggleTacticalMap() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (state.showTacticalMap) Cyan.copy(alpha=0.3f) else Color.Black.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.border(1.dp, Cyan)
                ) {
                    Text(if (state.showTacticalMap) "CHIUDI MAPPA" else "MAPPA TATTICA", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }"""
hud_new = """                Button(
                    onClick = { viewModel.toggleTacticalMap() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (state.showTacticalMap) Cyan.copy(alpha=0.3f) else Color.Black.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.border(1.dp, Cyan)
                ) {
                    Text(if (state.showTacticalMap) "CHIUDI MAPPA" else "MAPPA TATTICA", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                }"""
code = code.replace(hud_old, hud_new)

overlay_code = """
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
                                androidx.compose.material3.Icon(
                                    imageVector = if (comp.state == CompartmentState.SEALED) androidx.compose.material.icons.Icons.Default.Lock else androidx.compose.material.icons.Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = color.copy(alpha = flash),
                                    modifier = Modifier.size(24.dp)
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
"""

if "fun DamageControlOverlay" not in code:
    code = code + overlay_code

draw_old = """            if (state.showTacticalMap && state.mode != GameMode.PATCHING) {
                TacticalMapOverlay(state, viewModel)
            }
        }"""
draw_new = """            if (state.showTacticalMap && state.mode != GameMode.PATCHING) {
                TacticalMapOverlay(state, viewModel)
            }
            if (state.showDamageControl && state.mode != GameMode.PATCHING) {
                DamageControlOverlay(state, viewModel)
            }
        }"""
code = code.replace(draw_old, draw_new)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

print("Damage Control Overlay UI added")
