import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

old_briefing = """fun BriefingScreen(state: GameState, viewModel: GameViewModel) {
    val chapter = GAME_CHAPTERS.getOrElse(state.currentChapterIndex) { GAME_CHAPTERS.last() }
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(chapter.title, fontSize = 24.sp, color = Cyan)
        Spacer(modifier = Modifier.height(16.dp))
        Text(chapter.narrative, color = Color.White, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { viewModel.startRun() },
            colors = ButtonDefaults.buttonColors(containerColor = Cyan)
        ) {
            Text("LANCIA MOTORI", color = Color.Black)
        }
    }
}"""

new_briefing = """fun BriefingScreen(state: GameState, viewModel: GameViewModel) {
    val chapter = GAME_CHAPTERS.getOrElse(state.currentChapterIndex) { GAME_CHAPTERS.last() }
    
    // Choose portrait based on chapter index
    val portraitRes = if (state.currentChapterIndex % 2 == 0) R.drawable.character_pilot else R.drawable.character_smuggler
    val portraitName = if (state.currentChapterIndex % 2 == 0) "AGENT 47 // PILOT" else "THE SMUGGLER"
    
    Row(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(chapter.title, fontSize = 24.sp, color = Cyan, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text(chapter.narrative, color = Color.White, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { viewModel.startRun() },
                colors = ButtonDefaults.buttonColors(containerColor = Cyan)
            ) {
                Text("LANCIA MOTORI", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.width(32.dp))
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = portraitRes),
                contentDescription = "Character Portrait",
                modifier = Modifier.size(150.dp).border(2.dp, Cyan)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(portraitName, color = Cyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
    }
}"""

code = code.replace(old_briefing, new_briefing)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

print("Briefing patched")
