import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

import_clickable = "import androidx.compose.foundation.clickable\n"
if "import androidx.compose.foundation.clickable" not in code:
    code = code.replace("import androidx.compose.foundation.background", import_clickable + "import androidx.compose.foundation.background")

bad_icons_block = """                        androidx.compose.material.icons.Icons.Default.let {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Filled.Check,
                                contentDescription = "Fixed",
                                tint = Color.White
                            )
                        }"""
good_icons_block = """                        androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                contentDescription = "Fixed",
                                tint = Color.White
                            )"""
code = code.replace(bad_icons_block, good_icons_block)

code = code.replace(".androidx.compose.foundation.clickable", ".clickable")

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

