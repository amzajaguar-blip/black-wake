import sys

with open("app/src/main/java/com/blackwake/game/MainActivity.kt", "r") as f:
    code = f.read()

old_content = """class MainActivity : ComponentActivity() {
    private lateinit var viewModel: GameViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        val sp = getSharedPreferences("blackwake_save", Context.MODE_PRIVATE)
        viewModel = GameViewModel(sp)
        
        setContent {
            BlackwakeTheme {
                GameScreen(viewModel)
            }
        }
    }
}"""

new_content = """class MainActivity : ComponentActivity() {
    private lateinit var viewModel: GameViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        val sp = getSharedPreferences("blackwake_save", Context.MODE_PRIVATE)
        viewModel = GameViewModel(sp)
        
        setContent {
            BlackwakeTheme {
                GameScreen(viewModel)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        SynthAudioEngine.start()
    }
    
    override fun onPause() {
        super.onPause()
        SynthAudioEngine.stop()
    }
}"""
code = code.replace(old_content, new_content)
with open("app/src/main/java/com/blackwake/game/MainActivity.kt", "w") as f:
    f.write(code)

print("MainActivity patched")
