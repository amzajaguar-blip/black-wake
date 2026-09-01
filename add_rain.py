import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

target = """                        if (state.invulnerable > 0f) {
                            val alpha = (kotlin.math.sin(state.runElapsed * 20f) * 0.5f + 0.5f).toFloat() * 0.5f
                            drawPath(boatPath, Color.White.copy(alpha=alpha))
                        }
                    }
                }
            }
        }
    }
}"""

replacement = """                        if (state.invulnerable > 0f) {
                            val alpha = (kotlin.math.sin(state.runElapsed * 20f) * 0.5f + 0.5f).toFloat() * 0.5f
                            drawPath(boatPath, Color.White.copy(alpha=alpha))
                        }
                    }
                }
            }
            
            // Dynamic Weather System (Rain / Sea Spray) - Stateless & Zero Allocation
            // Speed of rain increases drastically when boosting (fovOffset > 1.0)
            val globalRainSpeed = 1500f * state.fovOffset 
            val rainAngleOffset = 40f * state.playerVelocityX // Wind/rain slants based on boat steering
            val tensionRainAlpha = (0.2f + (state.detection * 0.4f)).coerceIn(0f, 1f) // Rain gets thicker under tension
            val rainColor = Color(0xFF6699AA).copy(alpha = tensionRainAlpha)
            val rainColorForeground = Color(0xFFAACCFF).copy(alpha = (tensionRainAlpha * 1.5f).coerceIn(0f, 1f))
            
            for (i in 0..150) {
                // Pseudo-random deterministic values per particle
                val seed = i * 137.54f
                val startX = (seed * 123.4f) % (w * 1.5f) - (w * 0.25f) // Bleed outside edges to handle wind shift
                val baseY = (seed * 456.7f) % h
                val speedMod = 0.8f + (seed % 40f) / 100f // 0.8 to 1.19
                val dropLength = 20f + (seed % 40f) * state.fovOffset
                
                // Add parallax / depth (some drops are thicker and faster)
                val isForeground = (i % 6 == 0)
                val strokeW = if (isForeground) 3f else 1.5f
                val depthSpeed = if (isForeground) 1.6f else 1f
                val activeColor = if (isForeground) rainColorForeground else rainColor
                
                // Use modulo to wrap around screen vertically continuously
                val rawY = baseY + state.runElapsed * globalRainSpeed * speedMod * depthSpeed
                val finalY = rawY % (h * 1.2f) - (h * 0.1f)
                val finalX = startX + (rainAngleOffset * (finalY / h))
                
                drawLine(
                    color = activeColor,
                    start = Offset(finalX, finalY),
                    end = Offset(finalX - rainAngleOffset * 0.3f, finalY + dropLength * depthSpeed),
                    strokeWidth = strokeW
                )
            }
        }
    }
}"""

if target in code:
    code = code.replace(target, replacement)
    with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
        f.write(code)
    print("Rain system added successfully.")
else:
    print("Target block not found.")
