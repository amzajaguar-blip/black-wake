import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

old_garage = """fun GarageScreen(state: GameState, viewModel: GameViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Text("GARAGE", fontSize = 40.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Text("INTEL DISPONIBILE: ${state.intelBank}", color = Cyan)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(16.dp)) {"""

new_garage = """fun GarageScreen(state: GameState, viewModel: GameViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Text("GARAGE & DOCK", fontSize = 40.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Text("INTEL DISPONIBILE: ${state.intelBank}", color = Cyan)
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("SELECT VESSEL", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        // Boat Selector
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val boats = PlayerBoatType.values()
            items(boats.size) { i ->
                val b = boats[i]
                val selected = state.playerBoatType == b
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (selected) Cyan.copy(alpha=0.3f) else Color(0xFF05191F)),
                    border = BorderStroke(1.dp, if (selected) Cyan else Color.Gray),
                    modifier = Modifier.clickable { viewModel.setPlayerBoat(b) }.width(140.dp).height(80.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp).fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(b.name, color = Color.White, fontWeight = FontWeight.Bold)
                        val desc = when(b) {
                            PlayerBoatType.SPEEDBOAT -> "Balanced, Fast"
                            PlayerBoatType.RACING -> "High Speed, Low Armor"
                            PlayerBoatType.PATROL -> "Standard Police"
                            PlayerBoatType.SMUGGLER -> "Heavy cargo"
                            PlayerBoatType.STEALTH -> "Low detection"
                            PlayerBoatType.ARMORED -> "High Hull, Slow"
                        }
                        Text(desc, color = Color.Gray, fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("MODULES", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(16.dp)) {"""

code = code.replace(old_garage, new_garage)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

print("Garage patched")
