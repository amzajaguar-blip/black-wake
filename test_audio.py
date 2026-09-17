import sys

with open("app/src/main/java/com/blackwake/game/SynthAudioEngine.kt", "r") as f:
    code = f.read()

# Let's check playExplosion
print("has explosion:", "fun playExplosion()" in code)

