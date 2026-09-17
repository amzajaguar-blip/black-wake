package com.blackwake.game

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.sin

/**
 * Perspective shared by rendering and touch input. The player's row is z = 0, exactly where
 * [GameSimulation] resolves contacts, so what hits the boat on screen is what hits it in the rules.
 */
object SeaProjection {
    const val HORIZON = 0.30f
    const val PLAYER = 0.80f

    fun laneSpacingNear(width: Float) = width * 0.22f

    fun laneSpacingFar(width: Float) = width * 0.03f

    /** Touch x in pixels to a lane-axis target at the player's row. */
    fun touchToLane(x: Float, width: Float) = ((x - width / 2f) / laneSpacingNear(width)).coerceIn(-1.15f, 1.15f)
}

data class SeaPalette(val sky: Color, val horizon: Color, val seaFar: Color, val seaNear: Color, val lane: Color, val rain: Float)

fun paletteFor(environment: EnvironmentId): SeaPalette = when (environment) {
    EnvironmentId.NIGHT -> SeaPalette(Color(0xFF02070D), Color(0xFF0B2A3A), Color(0xFF06283D), Color(0xFF07456F), Color(0xFF1D4650), 0.25f)
    EnvironmentId.PORT -> SeaPalette(Color(0xFF0A0806), Color(0xFF4A2A10), Color(0xFF0E2A30), Color(0xFF0F3B3A), Color(0xFF5A4020), 0.1f)
    EnvironmentId.STORM -> SeaPalette(Color(0xFF05080B), Color(0xFF1B2A33), Color(0xFF0C1C26), Color(0xFF031A33), Color(0xFF28414D), 1f)
    EnvironmentId.ISLAND -> SeaPalette(Color(0xFF03100C), Color(0xFF14352A), Color(0xFF0B2F2B), Color(0xFF0F4A44), Color(0xFF1F5046), 0.15f)
    EnvironmentId.EXTRACT -> SeaPalette(Color(0xFF02060A), Color(0xFF0B3440), Color(0xFF051E28), Color(0xFF06344A), Color(0xFF2BE7E0), 0.2f)
    EnvironmentId.DAWN -> SeaPalette(Color(0xFF140F30), Color(0xFF6B3A55), Color(0xFF1D2448), Color(0xFF1E3A5F), Color(0xFF6B4E75), 0f)
}

/** Reusable buffers for particles and paths, so the frame loop does not allocate. */
class SeaEffects {
    val rain = FloatArray(RAIN_DROPS * 3)
    val spray = FloatArray(SPRAY_PARTICLES * 5)
    val path = Path()
    private var sprayCursor = 0
    private var lastElapsed = -1f
    private var seed = 0x1234567

    init {
        for (i in 0 until RAIN_DROPS) {
            rain[i * 3] = rand()
            rain[i * 3 + 1] = rand()
            rain[i * 3 + 2] = 0.7f + rand() * 0.6f
        }
    }

    fun rand(): Float {
        var x = seed
        x = x xor (x shl 13)
        x = x xor (x ushr 17)
        x = x xor (x shl 5)
        seed = x
        return (x and 0xFFFF) / 65535f
    }

    /** Advances spray once per simulation step, however many times the frame is redrawn. */
    fun stepSpray(run: RunState, sternX: Float, sternY: Float, unit: Float) {
        val dt = if (lastElapsed < 0f || run.runElapsed < lastElapsed) 0f else run.runElapsed - lastElapsed
        if (run.runElapsed == lastElapsed) return
        lastElapsed = run.runElapsed
        val emit = if (run.dive.submerged) 0 else if (run.speedFactor > 1.1f) 4 else if (abs(run.playerVelocityX) > 0.1f) 3 else 1
        repeat(emit) {
            val i = sprayCursor * 5
            sprayCursor = (sprayCursor + 1) % SPRAY_PARTICLES
            spray[i] = sternX + (rand() - 0.5f) * 30f * unit + run.playerVelocityX * 20f * unit
            spray[i + 1] = sternY + (rand() - 0.5f) * 10f * unit
            spray[i + 2] = (-run.playerVelocityX * 300f + (rand() - 0.5f) * 160f) * unit
            spray[i + 3] = (140f + rand() * 120f) * run.speedFactor * unit
            spray[i + 4] = 0.3f + rand() * 0.4f
        }
        for (p in 0 until SPRAY_PARTICLES) {
            val i = p * 5
            if (spray[i + 4] <= 0f) continue
            spray[i] += spray[i + 2] * dt
            spray[i + 1] += spray[i + 3] * dt
            spray[i + 4] -= dt
        }
    }

    companion object {
        const val RAIN_DROPS = 140
        const val SPRAY_PARTICLES = 120
    }
}

fun DrawScope.drawSea(
    run: RunState,
    sector: Sector,
    boat: BoatSpec,
    fx: SeaEffects,
    textMeasurer: TextMeasurer,
    submergeFog: Float,
    huntingFog: Float
) {
    val w = size.width
    val h = size.height
    val u = h / 360f
    val palette = paletteFor(sector.environment)
    val horizonY = h * SeaProjection.HORIZON
    val playerY = h * SeaProjection.PLAYER
    val near = SeaProjection.laneSpacingNear(w)
    val far = SeaProjection.laneSpacingFar(w)

    fun screenY(s: Float) = horizonY + (playerY - horizonY) * s
    fun screenX(x: Float, s: Float) = w / 2f + x * (far + (near - far) * s)
    fun depth(z: Float) = 1f - z / 100f

    val tension = (run.detection / 0.75f).coerceIn(0f, 1f)
    val dread = ((run.detection - 0.75f) / 0.25f).coerceIn(0f, 1f)
    val flare = (run.flare.timer / GameSimulation.FLARE_SECONDS).coerceIn(0f, 1f)
    fun tint(c: Color) = lerp(lerp(lerp(c, Palette.Storm, tension * 0.6f), Palette.BlackTide, dread * 0.7f), Color(0xFF553311), flare * 0.45f)

    drawRect(Brush.verticalGradient(listOf(palette.sky, tint(palette.horizon)), startY = 0f, endY = horizonY), size = Size(w, horizonY + 1f))
    drawRect(Brush.verticalGradient(listOf(tint(palette.seaFar), tint(palette.seaNear)), startY = horizonY, endY = h), topLeft = Offset(0f, horizonY), size = Size(w, h - horizonY))

    val shake = run.cameraShake + (if (run.detection > 0.85f) (run.detection - 0.85f) * 4f else 0f) + (if (run.dive.pressureAlarm) 0.3f else 0f)
    val shakeX = if (shake > 0f) (fx.rand() - 0.5f) * 14f * u * shake else 0f
    val shakeY = if (shake > 0f) (fx.rand() - 0.5f) * 14f * u * shake else 0f
    val playerX = screenX(run.playerX, 1f)

    translate(shakeX, shakeY) {
        drawLine(palette.horizon, Offset(0f, horizonY), Offset(w, horizonY), strokeWidth = 2f * u)

        // Swell lines rushing towards the bow; spacing opens up with perspective.
        val travel = run.runElapsed * 0.55f * run.speedFactor
        for (i in 0 until 10) {
            val phase = (travel + i / 10f) % 1f
            val s = phase * phase * 1.3f
            val y = screenY(s)
            if (y <= horizonY || y >= h) continue
            val sway = sin(run.runElapsed * 1.7f + i * 1.3f) * 5f * u * s
            drawLine(Color.White.copy(alpha = (0.025f + 0.06f * s).coerceAtMost(0.09f)), Offset(0f, y + sway), Offset(w, y - sway), strokeWidth = (1f + s) * u)
        }

        val bottomScale = (h - horizonY) / (playerY - horizonY)
        for (lane in -1..1) {
            val x = lane.toFloat()
            drawLine(palette.lane.copy(alpha = 0.35f), Offset(screenX(x, 0f), horizonY), Offset(screenX(x, bottomScale), h), strokeWidth = 1.5f * u)
        }

        if (run.sonar.active) {
            val progress = run.sonar.radius / GameSimulation.PING_RANGE
            val reach = playerY - screenY(depth(run.sonar.radius))
            drawOval(
                Palette.Cyan.copy(alpha = (1f - progress) * 0.8f),
                topLeft = Offset(playerX - reach * 2.2f, playerY - reach),
                size = Size(reach * 4.4f, reach * 2f),
                style = Stroke(2.5f * u)
            )
        }

        for (e in run.entities) if (e.z >= 0f && e.z < 100f) drawEntity(e, depth(e.z), screenX(e.x, depth(e.z)), screenY(depth(e.z)), u, run, flare, fx, textMeasurer)

        // Hunted: the far water closes in. Sonar and flares cut through it.
        val fog = huntingFog * (1f - flare)
        if (fog > 0.01f) {
            val fogEnd = screenY(0.45f * fog)
            drawRect(
                Brush.verticalGradient(listOf(Palette.Deep.copy(alpha = 0.95f * fog), Palette.Deep.copy(alpha = 0.85f * fog), Color.Transparent), startY = horizonY - 20f * u, endY = fogEnd + 30f * u),
                topLeft = Offset(0f, horizonY - 20f * u),
                size = Size(w, fogEnd - horizonY + 50f * u)
            )
            if (run.sonar.active) {
                for (e in run.entities) {
                    if (!e.type.isHazard || e.z < 0f || e.z > run.sonar.radius) continue
                    val s = depth(e.z)
                    drawCircle(Palette.Cyan, radius = (8f + 22f * s) * u, center = Offset(screenX(e.x, s), screenY(s)), style = Stroke(2f * u))
                }
            }
        }

        for (p in run.pursuers) if (p.y < GameSimulation.PLAYER_Y) drawPursuer(p, u, screenX(p.x, p.y / GameSimulation.PLAYER_Y), screenY(p.y / GameSimulation.PLAYER_Y), run, flare, fx)

        drawPlayer(run, boat, playerX, playerY, u, h, fx)

        for (e in run.entities) if (e.z < 0f) drawEntity(e, depth(e.z), screenX(e.x, depth(e.z)), screenY(depth(e.z)), u, run, flare, fx, textMeasurer)
        for (p in run.pursuers) if (p.y >= GameSimulation.PLAYER_Y) drawPursuer(p, u, screenX(p.x, p.y / GameSimulation.PLAYER_Y), screenY(p.y / GameSimulation.PLAYER_Y), run, flare, fx)

        fx.stepSpray(run, playerX, playerY + 40f * u, u)
        for (i in 0 until SeaEffects.SPRAY_PARTICLES) {
            val life = fx.spray[i * 5 + 4]
            if (life <= 0f) continue
            val ratio = (life / 0.7f).coerceIn(0f, 1f)
            drawCircle(Color.White.copy(alpha = ratio * 0.45f), radius = (3f + (1f - ratio) * 9f) * u, center = Offset(fx.spray[i * 5], fx.spray[i * 5 + 1]))
        }
    }

    // Rain in screen space, slanted by steering.
    val rainCount = (SeaEffects.RAIN_DROPS * palette.rain).toInt()
    if (rainCount > 0) {
        val slant = 40f * u * run.playerVelocityX
        val rainColor = Color(0xFF9DBBD0).copy(alpha = 0.18f + run.detection * 0.2f)
        for (i in 0 until rainCount) {
            val speed = fx.rain[i * 3 + 2]
            val x = (fx.rain[i * 3] * w * 1.2f - w * 0.1f)
            val y = ((fx.rain[i * 3 + 1] + run.runElapsed * 1.6f * speed) % 1f) * h
            val length = 16f * u * speed * run.speedFactor
            drawLine(rainColor, Offset(x + slant * (y / h), y), Offset(x + slant * (y / h) - slant * 0.2f, y + length), strokeWidth = 1.3f * u)
        }
    }

    if (flare > 0f) {
        drawRect(Brush.radialGradient(listOf(Color(0x66FFE2A0).copy(alpha = 0.35f * flare), Color.Transparent), center = Offset(w / 2f, horizonY), radius = w * 0.7f))
    }
    if (submergeFog > 0f) {
        drawRect(Color(0xFF06283A).copy(alpha = 0.6f * submergeFog))
        drawRect(Brush.verticalGradient(listOf(Color(0xFF020A10).copy(alpha = 0.7f * submergeFog), Color.Transparent), endY = h * 0.6f))
    }
    val hullRatio = if (run.maxHull > 0f) run.hull / run.maxHull else 1f
    if (hullRatio < 0.4f) {
        val intensity = (0.4f - hullRatio) / 0.4f * blink(run.runElapsed, 5f)
        drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Transparent, Palette.Red.copy(alpha = 0.55f * intensity)), center = Offset(w / 2f, h / 2f), radius = w * 0.75f))
    }
    if (run.screenFlash > 0f) {
        val color = when (run.flashKind) {
            FlashKind.INTEL -> Palette.Cyan
            FlashKind.FUEL -> Palette.Amber
            FlashKind.DAMAGE -> Palette.Red
            FlashKind.FLARE -> Color(0xFFFFF3C0)
            FlashKind.NONE -> Color.Transparent
        }
        drawRect(color.copy(alpha = run.screenFlash * 0.28f))
    }
}

private fun DrawScope.drawEntity(
    e: Entity, s: Float, cx: Float, cy: Float, u: Float,
    run: RunState, flare: Float, fx: SeaEffects, textMeasurer: TextMeasurer
) {
    val r = (6f + 22f * s) * u
    val path = fx.path
    path.reset()
    when (e.type) {
        EntityType.INTEL -> {
            drawCircle(Palette.Cyan.copy(alpha = 0.18f), r * 1.8f, Offset(cx, cy))
            path.moveTo(cx, cy - r)
            path.lineTo(cx + r * 0.8f, cy)
            path.lineTo(cx, cy + r)
            path.lineTo(cx - r * 0.8f, cy)
            path.close()
            drawPath(path, Palette.Cyan)
            drawPath(path, Color.White, style = Stroke(1.5f * u))
        }

        EntityType.FUEL -> {
            drawCircle(Palette.Amber.copy(alpha = 0.16f), r * 1.8f, Offset(cx, cy))
            drawRoundRect(Palette.Amber, topLeft = Offset(cx - r * 0.7f, cy - r), size = Size(r * 1.4f, r * 2f), cornerRadius = CornerRadius(r * 0.35f))
            drawRect(Color(0xFF3A2410), topLeft = Offset(cx - r * 0.35f, cy - r * 0.5f), size = Size(r * 0.7f, r * 0.25f))
            drawRoundRect(Color.White, topLeft = Offset(cx - r * 0.7f, cy - r), size = Size(r * 1.4f, r * 2f), cornerRadius = CornerRadius(r * 0.35f), style = Stroke(1.5f * u))
        }

        EntityType.MINE -> {
            val pulse = blink(run.runElapsed + e.id, 6f)
            for (k in 0 until 8) {
                val a = k * 0.785f
                drawLine(Color(0xFF2A0C0E), Offset(cx, cy), Offset(cx + kotlin.math.cos(a) * r * 1.25f, cy + sin(a) * r * 1.25f), strokeWidth = 3f * u)
            }
            drawCircle(Color(0xFF1A0A0C), r, Offset(cx, cy))
            drawCircle(Palette.Red.copy(alpha = pulse), r, Offset(cx, cy), style = Stroke(2f * u))
            drawCircle(Palette.Red.copy(alpha = pulse), r * 0.28f, Offset(cx, cy))
        }

        EntityType.WRECK -> {
            if (flare > 0f) drawCircle(Palette.Amber.copy(alpha = flare * 0.2f), r * 2f, Offset(cx, cy))
            path.moveTo(cx - r * 1.2f, cy + r * 0.2f)
            path.lineTo(cx - r * 0.5f, cy - r * 0.9f)
            path.lineTo(cx + r * 0.9f, cy - r * 0.4f)
            path.lineTo(cx + r * 1.2f, cy + r * 0.5f)
            path.lineTo(cx - r * 0.2f, cy + r * 0.8f)
            path.close()
            drawPath(path, Color(0xFF2B3136))
            drawPath(path, Color(0xFF6F7C82), style = Stroke(1.5f * u))
            drawLine(Color(0xFF6F7C82), Offset(cx - r * 0.2f, cy - r * 0.6f), Offset(cx + r * 0.1f, cy - r * 1.6f), strokeWidth = 2f * u)
        }

        EntityType.ENEMY -> {
            if (flare > 0f) drawCircle(Palette.Amber.copy(alpha = flare * 0.35f), r * 2.4f, Offset(cx, cy))
            path.moveTo(cx, cy + r * 1.1f)
            path.lineTo(cx + r * 0.75f, cy - r * 0.9f)
            path.lineTo(cx, cy - r * 0.5f)
            path.lineTo(cx - r * 0.75f, cy - r * 0.9f)
            path.close()
            drawPath(path, enemyColor(e.enemyType))
            drawPath(path, Palette.Red, style = Stroke(1.5f * u))
            // Searchlight towards the player.
            path.reset()
            path.moveTo(cx, cy + r * 0.6f)
            path.lineTo(cx - r * 2.6f, cy + r * 5f)
            path.lineTo(cx + r * 2.6f, cy + r * 5f)
            path.close()
            val beam = if (e.enemyType == EnemyBoatType.POLICE && (run.runElapsed * 8f).toInt() % 2 == 0) Color(0xFF3060FF) else Palette.Red
            drawPath(path, beam.copy(alpha = 0.16f))
        }

        EntityType.FORK -> {
            val color = when (e.value) {
                0 -> Palette.Cyan
                1 -> Palette.Amber
                else -> Palette.Red
            }
            drawCircle(color.copy(alpha = 0.25f), r * 1.6f, Offset(cx, cy))
            drawCircle(color, r * 0.7f, Offset(cx, cy))
            drawCircle(color, r * 1.3f, Offset(cx, cy), style = Stroke(2f * u))
            val label = e.label
            if (label != null && s > 0.15f) {
                val layout = textMeasurer.measure(label, TextStyle(color = color, fontSize = 11.sp, fontFamily = Mono, fontWeight = FontWeight.Bold))
                drawText(layout, topLeft = Offset(cx - layout.size.width / 2f, cy - r * 1.4f - layout.size.height))
            }
        }
    }

    if (e.type.isHazard && run.sonar.active && e.z <= run.sonar.radius) {
        drawCircle(Palette.Cyan, r * 1.5f, Offset(cx, cy), style = Stroke(1.5f * u))
    }
}

private fun DrawScope.drawPursuer(p: Pursuer, u: Float, cx: Float, cy: Float, run: RunState, flare: Float, fx: SeaEffects) {
    val s = p.y / GameSimulation.PLAYER_Y
    val r = (8f + 14f * s) * u
    val winding = p.state == PursuerState.INTERCEPT && p.stateTimer < PursuerAI.WIND_UP
    translate(cx, cy) {
        rotate(p.roll, pivot = Offset.Zero) {
            if (p.state == PursuerState.INTERCEPT) {
                val alpha = if (winding) blink(run.runElapsed, 30f) * 0.5f else 0.35f
                drawCircle(Palette.Red.copy(alpha = alpha), r * 2.6f, Offset.Zero)
            }
            if (flare > 0f) drawCircle(Palette.Amber.copy(alpha = flare * 0.35f), r * 3f, Offset.Zero)
            val path = fx.path
            path.reset()
            when (p.type) {
                EnemyBoatType.PATROL, EnemyBoatType.POLICE -> {
                    path.moveTo(0f, -r * 2f)
                    path.lineTo(r * 0.8f, r)
                    path.lineTo(-r * 0.8f, r)
                }
                EnemyBoatType.INTERCEPTOR, EnemyBoatType.HUNTER -> {
                    path.moveTo(0f, -r * 2.5f)
                    path.lineTo(r, r * 0.5f)
                    path.lineTo(0f, r)
                    path.lineTo(-r, r * 0.5f)
                }
                EnemyBoatType.ARMORED -> {
                    path.moveTo(0f, -r * 1.5f)
                    path.lineTo(r * 1.5f, -r * 0.5f)
                    path.lineTo(r * 1.2f, r * 1.5f)
                    path.lineTo(-r * 1.2f, r * 1.5f)
                    path.lineTo(-r * 1.5f, -r * 0.5f)
                }
                EnemyBoatType.ELITE -> {
                    path.moveTo(0f, -r * 3f)
                    path.lineTo(r * 1.2f, r * 1.2f)
                    path.lineTo(r * 0.5f, r * 0.8f)
                    path.lineTo(-r * 0.5f, r * 0.8f)
                    path.lineTo(-r * 1.2f, r * 1.2f)
                }
            }
            path.close()
            val faded = if (p.state == PursuerState.LOST_TARGET) 0.4f else 1f
            drawPath(path, enemyColor(p.type).copy(alpha = faded))
            drawPath(path, Palette.Red.copy(alpha = faded), style = Stroke(1.5f * u))
            val light = if (p.type == EnemyBoatType.POLICE && (run.runElapsed * 8f).toInt() % 2 == 0) Color(0xFF3060FF) else Palette.Red
            drawCircle(light.copy(alpha = 0.9f * faded), r * 0.3f, Offset(0f, -r * 0.4f))
            if (run.sonar.active) drawCircle(Palette.Cyan, r * 2.2f, Offset.Zero, style = Stroke(2f * u))
        }
    }
}

private fun DrawScope.drawPlayer(run: RunState, boat: BoatSpec, cx: Float, cy: Float, u: Float, h: Float, fx: SeaEffects) {
    val path = fx.path
    val submerged = run.dive.submerged

    // Wake: a fan from the stern that bends away from the turn.
    val spread = (22f + (run.speedFactor - 0.8f) * 60f) * u
    val bend = -run.playerVelocityX * 120f * u
    path.reset()
    path.moveTo(cx - 8f * u, cy + 30f * u)
    path.quadraticTo(cx - spread * 0.5f + bend * 0.4f, cy + (h - cy) * 0.5f, cx - spread + bend, h)
    path.lineTo(cx + spread + bend, h)
    path.quadraticTo(cx + spread * 0.5f + bend * 0.4f, cy + (h - cy) * 0.5f, cx + 8f * u, cy + 30f * u)
    path.close()
    drawPath(path, Color.White.copy(alpha = if (submerged) 0.04f else 0.13f))

    translate(cx, cy) {
        rotate(run.playerRoll, pivot = Offset.Zero) {
            val alpha = if (submerged) 0.45f else 1f
            path.reset()
            path.moveTo(0f, -40f * u)
            path.lineTo(22f * u, 8f * u)
            path.lineTo(16f * u, 34f * u)
            path.lineTo(-16f * u, 34f * u)
            path.lineTo(-22f * u, 8f * u)
            path.close()
            drawPath(path, Color(boat.hullColor).copy(alpha = alpha))
            drawPath(path, Palette.Cyan.copy(alpha = 0.55f * alpha), style = Stroke(2f * u))

            path.reset()
            path.moveTo(0f, -14f * u)
            path.lineTo(10f * u, 8f * u)
            path.lineTo(-10f * u, 8f * u)
            path.close()
            drawPath(path, Color.Black.copy(alpha = alpha))

            val glow = if (run.speedFactor > 1.1f) 1.8f else 1f
            drawCircle(Palette.Cyan.copy(alpha = 0.55f * alpha), radius = 7f * u * glow, center = Offset(-7f * u, 38f * u))
            drawCircle(Palette.Cyan.copy(alpha = 0.55f * alpha), radius = 7f * u * glow, center = Offset(7f * u, 38f * u))

            if (run.invulnerable > 0f) {
                path.reset()
                path.moveTo(0f, -40f * u)
                path.lineTo(22f * u, 8f * u)
                path.lineTo(16f * u, 34f * u)
                path.lineTo(-16f * u, 34f * u)
                path.lineTo(-22f * u, 8f * u)
                path.close()
                drawPath(path, Color.White.copy(alpha = blink(run.runElapsed, 30f) * 0.45f))
            }
        }
    }
}

private fun enemyColor(type: EnemyBoatType): Color = when (type) {
    EnemyBoatType.PATROL -> Color(0xFF5A6166)
    EnemyBoatType.INTERCEPTOR -> Color(0xFF4F5E4F)
    EnemyBoatType.POLICE -> Color(0xFF22408A)
    EnemyBoatType.HUNTER -> Color(0xFF232628)
    EnemyBoatType.ARMORED -> Color(0xFF5E5040)
    EnemyBoatType.ELITE -> Color(0xFF7A0E12)
}
