import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

code = code.replace(".androidx.compose.foundation.clickable", ".clickable")
code = code.replace("androidx.compose.material.icons.Icons.Default.let {", "")
code = code.replace("androidx.compose.material.icons.Icons.Filled.Check", "androidx.compose.material.icons.Icons.Default.Check")
code = code.replace("                        }", "", 1) # This might remove the wrong bracket! Let's be careful.

