import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

hud_end_old = """        CompassWidget(
            state = state,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
        )
    }
}"""

hud_end_new = """        // Interactive Throttle
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .width(48.dp)
                .height(240.dp)
                .background(Color.Black.copy(alpha = 0.7f))
                .border(2.dp, Cyan)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while(true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.pressed }
                            if (change != null) {
                                change.consume()
                                val yPercent = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                                viewModel.setThrottle(yPercent)
                            }
                        }
                    }
                }
        ) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                Text("MAX", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                Text("MIN", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
            }
            
            // Track
            Box(modifier = Modifier.align(Alignment.Center).width(4.dp).fillMaxHeight(0.8f).background(Color.DarkGray))
            
            // Track Fill
            val trackHeight = 192f // 240 * 0.8
            val fillHeight = trackHeight * state.throttle
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .width(4.dp)
                    .height(fillHeight.dp)
                    .background(if (state.throttle > 0.6f) Amber else Cyan)
            )
            
            // Handle
            val handleY = (1f - state.throttle) * trackHeight
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 24.dp + handleY.dp - 12.dp)
                    .size(width = 36.dp, height = 24.dp)
                    .background(Color(0xFF222222))
                    .border(2.dp, if (state.throttle > 0.6f) Amber else Cyan)
            ) {
                // Lines on handle for grip
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(20.dp, 2.dp).background(Color.Gray))
                    Box(modifier = Modifier.size(20.dp, 2.dp).background(Color.Gray))
                    Box(modifier = Modifier.size(20.dp, 2.dp).background(Color.Gray))
                }
            }
            
            // Throttle text
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(x = 56.dp, y = (-24f - fillHeight + 12f).dp)
                    .background(Color.Black.copy(alpha=0.6f))
                    .border(1.dp, Cyan)
                    .padding(4.dp)
            ) {
                Text("PWR ${(state.throttle * 100).toInt()}%", color = if (state.throttle > 0.6f) Amber else Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        CompassWidget(
            state = state,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
        )
    }
}"""

code = code.replace(hud_end_old, hud_end_new)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

