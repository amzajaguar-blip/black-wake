import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

hud_old = """                Button(
                    onClick = { viewModel.toggleAudioMute() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.border(1.dp, audioColor)
                ) {
                    Text(if (state.isAudioMuted) "AUDIO: OFF" else "AUDIO: ON", color = audioColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                if (state.comboMultiplier > 1) {"""
hud_new = """                Button(
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
                if (state.comboMultiplier > 1) {"""
code = code.replace(hud_old, hud_new)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

print("Flare UI added")
