import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

hud_old = """                Row(verticalAlignment = Alignment.CenterVertically) {
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
                }"""
hud_new = """                Row(verticalAlignment = Alignment.CenterVertically) {
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
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.toggleTacticalMap() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (state.showTacticalMap) Cyan.copy(alpha=0.3f) else Color.Black.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.border(1.dp, Cyan)
                ) {
                    Text(if (state.showTacticalMap) "CHIUDI MAPPA" else "MAPPA TATTICA", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }"""
code = code.replace(hud_old, hud_new)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

print("Map button added to HUD")
