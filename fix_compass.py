import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

code = code.replace("@Composable\n@Composable\nfun CompassWidget", "@Composable\nfun CompassWidget")

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

print("Fixed annotation")
