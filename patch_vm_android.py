import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    code = f.read()

# Replace import and class def
code = code.replace("import androidx.lifecycle.ViewModel", "import androidx.lifecycle.AndroidViewModel\nimport android.app.Application\nimport android.content.Context")
code = code.replace("class GameViewModel : ViewModel() {", "class GameViewModel(application: Application) : AndroidViewModel(application) {")

save_load_methods = """
    private val prefs = getApplication<Application>().getSharedPreferences("PursuerStatePrefs", Context.MODE_PRIVATE)

    private fun savePursuers() {
        val pursuers = _uiState.value.pursuers
        val csvList = pursuers.map { p ->
            PursuerSaveState(
                id = p.id,
                type = p.type.name,
                x = p.x,
                y = p.y,
                velocityX = p.velocityX,
                velocityY = p.velocityY,
                state = p.state.name,
                roll = p.roll,
                timer = p.timer,
                health = p.health,
                targetX = p.targetX
            ).toCsv()
        }
        val encoded = csvList.joinToString(";")
        prefs.edit().putString("saved_pursuers", encoded).apply()
    }

    fun loadPursuers() {
        val encoded = prefs.getString("saved_pursuers", "") ?: ""
        if (encoded.isEmpty()) return
        
        val csvList = encoded.split(";")
        val loadedPursuers = csvList.mapNotNull { csv ->
            PursuerSaveState.fromCsv(csv)?.let { saveState ->
                Pursuer(
                    id = saveState.id,
                    type = EnemyBoatType.valueOf(saveState.type),
                    x = saveState.x,
                    y = saveState.y,
                    velocityX = saveState.velocityX,
                    velocityY = saveState.velocityY,
                    state = PursuerState.valueOf(saveState.state),
                    ai = PursuerAI(EnemyBoatType.valueOf(saveState.type), PursuerState.valueOf(saveState.state)).apply { timer = saveState.timer },
                    roll = saveState.roll,
                    timer = saveState.timer,
                    health = saveState.health,
                    targetX = saveState.targetX
                )
            }
        }
        
        _uiState.update { it.copy(pursuers = loadedPursuers) }
    }
    
    fun clearPursuers() {
        prefs.edit().remove("saved_pursuers").apply()
    }
"""

code = code.replace("class GameViewModel(application: Application) : AndroidViewModel(application) {", "class GameViewModel(application: Application) : AndroidViewModel(application) {" + save_load_methods)

# Add loadPursuers/savePursuers to togglePause
toggle_pause = """    fun togglePause() {
        _uiState.update { state ->
            if (state.mode == GameMode.RUNNING) {
                savePursuers()
                state.copy(mode = GameMode.PAUSED)
            } else if (state.mode == GameMode.PAUSED) {
                state.copy(mode = GameMode.RUNNING)
            } else {
                state
            }
        }
    }"""
code = code.replace("""    fun togglePause() {
        _uiState.update { state ->
            if (state.mode == GameMode.RUNNING) {
                state.copy(mode = GameMode.PAUSED)
            } else if (state.mode == GameMode.PAUSED) {
                state.copy(mode = GameMode.RUNNING)
            } else {
                state
            }
        }
    }""", toggle_pause)

# startGame -> clear Pursuers? Wait, startGame is called on new run.
# So we can clear them when we finish run or start game.
code = code.replace("pursuitCooldown = 0f,", "pursuitCooldown = 0f,\n                pursuers = emptyList(),")
start_game_replace = """        clearPursuers()
        _uiState.update { state ->"""
code = code.replace("_uiState.update { state ->", start_game_replace, 1)

# loadPursuers could be used when the app is restarted (viewModel created).
# So put loadPursuers() inside init {} block? But GameState mode will be MENU.
# Actually, the user wants "across game session pauses or app restarts".
# Android apps when paused (e.g. backgrounded) might be killed, so we should save the active pursuers on onCleared?
# Or just whenever we pause or leave.
# A standard way is to save in `onCleared` or whenever we pause.
code = code.replace("""class GameViewModel(application: Application) : AndroidViewModel(application) {""", """class GameViewModel(application: Application) : AndroidViewModel(application) {
    init {
        // Load on startup if we were in the middle of a run. But wait, mode defaults to MENU.
        // We'd have to restore the whole game state for a true session resume, but we'll at least restore pursuers.
        loadPursuers()
    }""")

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.write(code)

print("GameViewModel AndroidViewModel patched")
