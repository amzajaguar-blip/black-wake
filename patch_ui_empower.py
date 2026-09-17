import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

sonar_old = """                Row(verticalAlignment = Alignment.CenterVertically) {
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
sonar_new = """                Row(verticalAlignment = Alignment.CenterVertically) {
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
                }"""
code = code.replace(sonar_old, sonar_new)

flare_btn_old = """                Button(
                    onClick = { viewModel.launchFlare() },
                    enabled = state.flareCharges > 0 && !flareActive && !state.isSubmerged,
                    colors = ButtonDefaults.buttonColors(containerColor = if (flareActive) flareColor.copy(alpha=0.3f) else Color.Black.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.border(1.dp, flareColor)
                ) {
                    Text(if (flareActive) "RAZZO ATTIVO" else "RAZZO (${state.flareCharges})", color = flareColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                if (state.comboMultiplier > 1) {"""
flare_btn_new = """                Button(
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
                    Text(if (state.isEmergencyPowerActive) "PWR: O2 BOOST" else "PWR: NORMAL", color = empColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                if (state.comboMultiplier > 1) {"""
code = code.replace(flare_btn_old, flare_btn_new)


with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

