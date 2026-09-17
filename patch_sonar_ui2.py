import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

hud_sonar_text = """                Text("SCAFO: ${state.hull.toInt()}", color = if (state.hull > 30) Color.White else Red)
                Text("CARB: ${state.fuel.toInt()}", color = if (state.fuel > 20) Amber else Red)
                Text("INTEL: ${state.intel}", color = Cyan)"""

new_hud_sonar_text = """                Text("SCAFO: ${state.hull.toInt()}", color = if (state.hull > 30) Color.White else Red)
                Text("CARB: ${state.fuel.toInt()}", color = if (state.fuel > 20) Amber else Red)
                Text("INTEL: ${state.intel}", color = Cyan)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
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

if hud_sonar_text in code:
    code = code.replace(hud_sonar_text, new_hud_sonar_text)
    with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
        f.write(code)
    print("Sonar HUD patched")
else:
    print("Failed to find HUD text")
