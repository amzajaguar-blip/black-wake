import sys
with open("app/src/main/java/com/blackwake/game/PursuerAI.kt", "r") as f:
    code = f.read()

code = code.replace("state = PursuerState.PURSUIT", "changeState(PursuerState.PURSUIT)")

with open("app/src/main/java/com/blackwake/game/PursuerAI.kt", "w") as f:
    f.write(code)
