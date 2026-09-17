import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

code = code.replace("androidx.compose.material.icons.Icons.Default.Check", "androidx.compose.material.icons.Icons.Filled.Check")

if "import androidx.compose.material.icons.filled.Check" not in code:
    code = code.replace("import androidx.compose.material.icons.Icons", "import androidx.compose.material.icons.Icons\nimport androidx.compose.material.icons.filled.Check")

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

print("Icons import fixed")
