import sys
import re

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

code = re.sub(r'isDragging = false\s*\}\s*\}\s*fun startGame', 'isDragging = false\n    }\n\n    fun startGame', code)

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

