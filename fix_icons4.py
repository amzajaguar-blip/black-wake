import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

bad_icon = """                                androidx.compose.material3.Icon(
                                    imageVector = if (comp.state == CompartmentState.SEALED) androidx.compose.material.icons.Icons.Filled.Lock else androidx.compose.material.icons.Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = color.copy(alpha = flash),
                                    modifier = Modifier.size(24.dp)
                                )"""
good_icon = """                                Text(
                                    text = if (comp.state == CompartmentState.SEALED) "X" else "!",
                                    color = color.copy(alpha = flash),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )"""
code = code.replace(bad_icon, good_icon)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

