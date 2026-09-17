import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

bad_lock = "androidx.compose.material.icons.filled.Lock"
good_lock = "androidx.compose.material.icons.Icons.Filled.Lock"

bad_warn = "androidx.compose.material.icons.filled.Warning"
good_warn = "androidx.compose.material.icons.Icons.Filled.Warning"

code = code.replace(bad_lock, good_lock)
code = code.replace(bad_warn, good_warn)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

