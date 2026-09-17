import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

old_audio_update = """        // Update Audio Engine
        SynthAudioEngine.engineRpm = if (newFov > 1f) 1f else 0.5f + (kotlin.math.abs(newVelX) * 0.5f)
        SynthAudioEngine.isBoosting = (newFov > 1f)
        SynthAudioEngine.detectionLevel = newDetection"""

new_audio_update = """        // Update Audio Engine
        SynthAudioEngine.engineRpm = if (newFov > 1f) 1f else 0.5f + (kotlin.math.abs(newVelX) * 0.5f)
        SynthAudioEngine.isBoosting = (newFov > 1f)
        SynthAudioEngine.detectionLevel = newDetection
        SynthAudioEngine.pursuerCount = newPursuers.size
        SynthAudioEngine.hasSiren = newPursuers.any { it.type == EnemyBoatType.POLICE }"""

code = code.replace(old_audio_update, new_audio_update)

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

print("GameViewModel audio patched")
