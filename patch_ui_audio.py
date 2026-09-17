import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

hud_old = """                Button(
                    onClick = { viewModel.toggleDamageControl() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (state.showDamageControl) dcColor.copy(alpha=0.3f) else Color.Black.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.border(1.dp, dcColor.copy(alpha=dcFlash))
                ) {
                    Text(if (state.showDamageControl) "CHIUDI PANN." else "CONTROLLO DANNI", color = dcColor.copy(alpha=dcFlash), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }"""
hud_new = """                Button(
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
                }"""
code = code.replace(hud_old, hud_new)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

