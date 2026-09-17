import sys

with open("app/src/main/java/com/blackwake/game/SynthAudioEngine.kt", "r") as f:
    code = f.read()

code = code.replace("private fun playTone", "fun playTone")

with open("app/src/main/java/com/blackwake/game/SynthAudioEngine.kt", "w") as f:
    f.write(code)

print("Audio engine playTone fixed")
