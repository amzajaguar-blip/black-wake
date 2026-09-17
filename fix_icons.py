import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

if "import androidx.compose.material.icons.filled.Lock" not in code:
    code = code.replace("import androidx.compose.material.icons.filled.Check", "import androidx.compose.material.icons.filled.Check\nimport androidx.compose.material.icons.filled.Lock\nimport androidx.compose.material.icons.filled.Warning")

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

