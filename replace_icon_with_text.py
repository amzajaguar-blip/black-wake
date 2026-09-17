import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

bad_icons_block = """                        androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                contentDescription = "Fixed",
                                tint = Color.White
                            )"""

good_icons_block = """                        Text("✓", color = Color.White, fontSize = 24.sp)"""

code = code.replace(bad_icons_block, good_icons_block)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

