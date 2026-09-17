import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

# Make updateGame public
code = code.replace("private fun updateGame(dt: Float)", "fun updateGame(dt: Float)")

# Remove old loop
old_loop = """
    private var isLooping = false
    private var lastTime = 0L

    private fun startLoop() {
        if (isLooping) return
        isLooping = true
        lastTime = System.currentTimeMillis()
        viewModelScope.launch {
            while (isLooping) {
                val now = System.currentTimeMillis()
                val delta = min((now - lastTime) / 1000f, 0.1f) // cap at 100ms for safety
                lastTime = now
                
                updateGame(delta)
                delay(16)
            }
        }
    }

    private fun stopLoop() {
        isLooping = false
    }"""
code = code.replace(old_loop, "")

# Replace startLoop() calls with nothing
code = code.replace("startLoop()", "")
code = code.replace("stopLoop()", "")
code = code.replace("private var isLooping = false", "")
code = code.replace("private var lastTime = 0L", "")

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

print("GameViewModel patched")
