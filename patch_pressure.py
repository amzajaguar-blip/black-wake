import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

old_code = """        // Depth Text at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 20.dp)
                .background(Color.Black.copy(alpha=0.6f))
                .padding(2.dp)
        ) {
            Text(
                text = "${currentDepth.toInt()}m",
                color = if (isCritical) Red.copy(alpha = flashAlpha) else Cyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        
        if (isCritical) {
            Text(
                text = "CRIT",
                color = Red.copy(alpha = flashAlpha),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-16).dp)
            )
        }"""

new_code = """        // Depth Text at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 26.dp)
                .background(Color.Black.copy(alpha=0.6f))
                .padding(2.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${currentDepth.toInt()}m",
                    color = if (isCritical) Red.copy(alpha = flashAlpha) else Cyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                // Real-time hydrostatic pressure (approx 1 atm = 101 kPa, +10 kPa per meter)
                val pressureKpa = 101 + (currentDepth * 10.05f).toInt()
                Text(
                    text = "${pressureKpa}kPa",
                    color = if (isCritical) Red.copy(alpha = flashAlpha) else Color.Gray,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        
        if (isCritical) {
            Text(
                text = "CRIT",
                color = Red.copy(alpha = flashAlpha),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-16).dp)
            )
        }"""

code = code.replace(old_code, new_code)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)
