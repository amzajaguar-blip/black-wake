import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

update_old = """                SynthAudioEngine.hasSiren = newState.pursuers.any { it.state == PursuerState.INTERCEPT || it.state == PursuerState.PURSUIT }
                SynthAudioEngine.isOxygenLow = (newState.oxygen < 20f)
            }
        }
    }"""
update_new = """                SynthAudioEngine.hasSiren = newState.pursuers.any { it.state == PursuerState.INTERCEPT || it.state == PursuerState.PURSUIT }
                SynthAudioEngine.isOxygenLow = (newState.oxygen < 20f)
                SynthAudioEngine.hullIntegrity = newState.hull
            }
        }
    }"""
code = code.replace(update_old, update_new)

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

print("GameViewModel patched with spatial audio update")
