import sys

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "r") as f:
    code = f.read()

ambient_old = """    val tensionColor = androidx.compose.ui.graphics.lerp(baseOcean, StormColor, (state.detection / 0.75f).coerceIn(0f, 1f))
    val waterTint = androidx.compose.ui.graphics.lerp(tensionColor, BlackTideColor, ((state.detection - 0.75f) / 0.25f).coerceIn(0f, 1f))"""
ambient_new = """    val tensionColor = androidx.compose.ui.graphics.lerp(baseOcean, StormColor, (state.detection / 0.75f).coerceIn(0f, 1f))
    val baseWaterTint = androidx.compose.ui.graphics.lerp(tensionColor, BlackTideColor, ((state.detection - 0.75f) / 0.25f).coerceIn(0f, 1f))
    
    val flareIntensity = (state.flareActiveTimer / 5f).coerceIn(0f, 1f)
    val waterTint = androidx.compose.ui.graphics.lerp(baseWaterTint, Color(0xFF553311), flareIntensity * 0.8f)"""
code = code.replace(ambient_old, ambient_new)

fog_old = """                // Hunting Tension Fog: dynamically obscures distant view when actively hunted
                if (huntingFog > 0f) {
                    val fogColor = Color(0xFF040A0C).copy(alpha = huntingFog * 0.95f)
                    val fogBottom = horizonY + (h - horizonY) * 0.55f * huntingFog // Creeps down towards player"""
fog_new = """                // Hunting Tension Fog: dynamically obscures distant view when actively hunted
                val effectiveHuntingFog = huntingFog * (1f - flareIntensity)
                if (effectiveHuntingFog > 0f) {
                    val fogColor = Color(0xFF040A0C).copy(alpha = effectiveHuntingFog * 0.95f)
                    val fogBottom = horizonY + (h - horizonY) * 0.55f * effectiveHuntingFog // Creeps down towards player"""
code = code.replace(fog_old, fog_new)

entity_glow_old = """                                    EnemyBoatType.PATROL -> Color.Gray
                                    EnemyBoatType.INTERCEPTOR -> Color(0xFF556655)
                                    EnemyBoatType.POLICE -> Color(0xFF2244AA)
                                    EnemyBoatType.HUNTER -> Color(0xFF222222)
                                    EnemyBoatType.ARMORED -> Color(0xFF665544)
                                    EnemyBoatType.ELITE -> Color(0xFF990000)
                                }"""
entity_glow_new = """                                    EnemyBoatType.PATROL -> Color.Gray
                                    EnemyBoatType.INTERCEPTOR -> Color(0xFF556655)
                                    EnemyBoatType.POLICE -> Color(0xFF2244AA)
                                    EnemyBoatType.HUNTER -> Color(0xFF222222)
                                    EnemyBoatType.ARMORED -> Color(0xFF665544)
                                    EnemyBoatType.ELITE -> Color(0xFF990000)
                                }
                                
                                if (flareIntensity > 0f) {
                                    drawCircle(Amber.copy(alpha = flareIntensity * 0.4f), radius * 2.5f, Offset(cx, cy))
                                }"""
code = code.replace(entity_glow_old, entity_glow_new)

wreck_old = """                            EntityType.WRECK -> {
                                drawCircle(Color(0xFF040A0C), radius + 3f, Offset(cx, cy))"""
wreck_new = """                            EntityType.WRECK -> {
                                drawCircle(Color(0xFF040A0C), radius + 3f, Offset(cx, cy))
                                if (flareIntensity > 0f) {
                                    drawCircle(Amber.copy(alpha = flareIntensity * 0.2f), radius * 2f, Offset(cx, cy))
                                }"""
code = code.replace(wreck_old, wreck_new)

with open("app/src/main/java/com/blackwake/game/GameScreen.kt", "w") as f:
    f.write(code)

