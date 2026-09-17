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

    /** Second path so a hull can be built while a wake path is still in hand. */
    val path2 = Path()

    // --- Gradient cache.
    //
    // Brush.verticalGradient/radialGradient allocate, and the sea, the fog, the flare wash, the
    // submerged veil and the damage vignette were each building one on every single frame. They
    // only actually change when their colours or geometry change, so each one is kept until its
    // own inputs move. The colour drivers are quantised by the caller (see [quantize]) so a
    // continuously rising detection level rebuilds a gradient a couple of dozen times over a run
    // instead of sixty times a second.

    private var skySize = 0f
    private var skyTop = Color.Transparent
    private var skyBottom = Color.Transparent
    private var skyCache: Brush? = null

    private var seaTop = 0f
    private var seaBottom = 0f
    private var seaFarColor = Color.Transparent
    private var seaNearColor = Color.Transparent
    private var seaCache: Brush? = null

    private var fogAlpha = -1f
    private var fogTop = 0f
    private var fogEnd = 0f
    private var fogCache: Brush? = null

    private var flareAlpha = -1f
    private var flareRadius = 0f
    private var flareCx = 0f
    private var flareCy = 0f
    private var flareCache: Brush? = null

    private var veilAlpha = -1f
    private var veilEnd = 0f
    private var veilCache: Brush? = null

    private var vignetteAlpha = -1f
    private var vignetteRadius = 0f
    private var vignetteCx = 0f
    private var vignetteCy = 0f
    private var vignetteCache: Brush? = null

    private var hazeTop = 0f
    private var hazeBottom = 0f
    private var hazeColor = Color.Transparent
    private var hazeCache: Brush? = null

    fun sky(endY: Float, top: Color, bottom: Color): Brush {
        val cached = skyCache
        if (cached != null && skySize == endY && skyTop == top && skyBottom == bottom) return cached
        skySize = endY
        skyTop = top
        skyBottom = bottom
        return Brush.verticalGradient(listOf(top, bottom), startY = 0f, endY = endY).also { skyCache = it }
    }

    fun sea(startY: Float, endY: Float, far: Color, near: Color): Brush {
        val cached = seaCache
        if (cached != null && seaTop == startY && seaBottom == endY && seaFarColor == far && seaNearColor == near) return cached
        seaTop = startY
        seaBottom = endY
        seaFarColor = far
        seaNearColor = near
        return Brush.verticalGradient(listOf(far, near), startY = startY, endY = endY).also { seaCache = it }
    }

    /** Light sitting on the horizon: the single cheapest thing that reads as atmosphere. */
    fun haze(startY: Float, endY: Float, color: Color): Brush {
        val cached = hazeCache
        if (cached != null && hazeTop == startY && hazeBottom == endY && hazeColor == color) return cached
        hazeTop = startY
        hazeBottom = endY
        hazeColor = color
        return Brush.verticalGradient(listOf(color, Color.Transparent), startY = startY, endY = endY).also { hazeCache = it }
    }

    fun fog(alpha: Float, startY: Float, endY: Float): Brush {
        val cached = fogCache
        if (cached != null && fogAlpha == alpha && fogTop == startY && fogEnd == endY) return cached
        fogAlpha = alpha
        fogTop = startY
        fogEnd = endY
        return Brush.verticalGradient(
            listOf(Palette.Deep.copy(alpha = 0.95f * alpha), Palette.Deep.copy(alpha = 0.85f * alpha), Color.Transparent),
            startY = startY,
            endY = endY
        ).also { fogCache = it }
    }

    fun flareWash(alpha: Float, cx: Float, cy: Float, radius: Float): Brush {
        val cached = flareCache
        if (cached != null && flareAlpha == alpha && flareCx == cx && flareCy == cy && flareRadius == radius) return cached
        flareAlpha = alpha
        flareCx = cx
        flareCy = cy
        flareRadius = radius
        return Brush.radialGradient(
            listOf(Color(0x66FFE2A0).copy(alpha = alpha), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius
        ).also { flareCache = it }
    }

    fun submergeVeil(alpha: Float, endY: Float): Brush {
        val cached = veilCache
        if (cached != null && veilAlpha == alpha && veilEnd == endY) return cached
        veilAlpha = alpha
        veilEnd = endY
        return Brush.verticalGradient(
            listOf(Color(0xFF020A10).copy(alpha = alpha), Color.Transparent),
            endY = endY
        ).also { veilCache = it }
    }

    fun damageVignette(alpha: Float, cx: Float, cy: Float, radius: Float): Brush {
        val cached = vignetteCache
        if (cached != null && vignetteAlpha == alpha && vignetteCx == cx && vignetteCy == cy && vignetteRadius == radius) return cached
        vignetteAlpha = alpha
        vignetteCx = cx
        vignetteCy = cy
        vignetteRadius = radius
        return Brush.radialGradient(
            listOf(Color.Transparent, Color.Transparent, Palette.Red.copy(alpha = alpha)),
            center = Offset(cx, cy),
            radius = radius
        ).also { vignetteCache = it }
    }

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

    // The raw flare drives the light that must stay smooth; the quantised copies drive anything
    // that ends up baked into a cached gradient, so the cache is not thrown away every frame.
    val flare = (run.flare.timer / GameSimulation.FLARE_SECONDS).coerceIn(0f, 1f)
    val tension = quantize((run.detection / 0.75f).coerceIn(0f, 1f))
    val dread = quantize(((run.detection - 0.75f) / 0.25f).coerceIn(0f, 1f))
    val flareTint = quantize(flare)
    fun tint(c: Color) = lerp(lerp(lerp(c, Palette.Storm, tension * 0.6f), Palette.BlackTide, dread * 0.7f), Color(0xFF553311), flareTint * 0.45f)

    val seaFar = tint(palette.seaFar)
    val seaNear = tint(palette.seaNear)
    val horizonTint = tint(palette.horizon)

    drawRect(fx.sky(horizonY, palette.sky, horizonTint), size = Size(w, horizonY + 1f))
    drawRect(fx.sea(horizonY, h, seaFar, seaNear), topLeft = Offset(0f, horizonY), size = Size(w, h - horizonY))

    // Haze: the horizon glows and the glow falls off into the near water. Reads as air and
    // distance, and gives the far boats something to be silhouetted against.
    val hazeBand = (h - horizonY) * 0.22f
    drawRect(
        fx.haze(horizonY, horizonY + hazeBand, horizonTint.copy(alpha = 0.55f)),
        topLeft = Offset(0f, horizonY),
        size = Size(w, hazeBand)
    )

    val shake = run.cameraShake + (if (run.detection > 0.85f) (run.detection - 0.85f) * 4f else 0f) + (if (run.dive.pressureAlarm) 0.3f else 0f)
    val shakeX = if (shake > 0f) (fx.rand() - 0.5f) * 14f * u * shake else 0f
    val shakeY = if (shake > 0f) (fx.rand() - 0.5f) * 14f * u * shake else 0f
    val playerX = screenX(run.playerX, 1f)

    translate(shakeX, shakeY) {
        drawLine(palette.horizon, Offset(0f, horizonY), Offset(w, horizonY), strokeWidth = 2f * u)

        // Swell lines rushing towards the bow; spacing opens up with perspective. The near ones
        // carry a foam crest above them, which is what makes the swell read as water with a
        // surface rather than as scrolling stripes.
        val travel = run.runElapsed * 0.55f * run.speedFactor
        for (i in 0 until SWELL_LINES) {
            val phase = (travel + i.toFloat() / SWELL_LINES) % 1f
            val s = phase * phase * 1.3f
            val y = screenY(s)
            if (y <= horizonY || y >= h) continue
            val sway = sin(run.runElapsed * 1.7f + i * 1.3f) * 5f * u * s
            drawLine(
                Color.White.copy(alpha = (0.025f + 0.06f * s).coerceAtMost(0.09f)),
                Offset(0f, y + sway),
                Offset(w, y - sway),
                strokeWidth = (1f + s) * u
            )
            if (s > 0.55f) {
                val crest = ((s - 0.55f) / 0.45f).coerceIn(0f, 1f)
                drawLine(
                    Color.White.copy(alpha = 0.06f * crest * (0.6f + 0.4f * palette.rain)),
                    Offset(0f, y + sway - 2.5f * u * s),
                    Offset(w, y - sway - 2.5f * u * s),
                    strokeWidth = 1.2f * u
                )
            }
        }

        // Specular glints on the near water. Positions come from the index and drift with time,
        // so they slide across the swell instead of flickering on and off.
        for (i in 0 until GLINTS) {
            val fi = i * 2.399f
            val gs = 0.42f + (sin(fi) * 0.5f + 0.5f) * 0.58f
            val gy = screenY(gs)
            if (gy <= horizonY + hazeBand * 0.35f || gy >= h) continue
            val drift = ((run.runElapsed * 0.17f + i * 0.137f) % 1f) * 2f - 1f
            val gx = w / 2f + drift * w * 0.62f
            // Length and tilt vary per glint. Uniform horizontal dashes of equal length read as
            // scratches on the screen rather than as light sitting on a moving surface.
            val vary = sin(fi * 3.1f) * 0.5f + 0.5f
            val len = (2.5f + 7f * gs) * (0.5f + vary) * u
            val tilt = sin(fi * 1.7f) * 2.2f * u * gs
            val twinkle = sin(run.runElapsed * 2.3f + fi) * 0.5f + 0.5f
            drawLine(
                Color.White.copy(alpha = (0.05f + 0.07f * gs * twinkle) * (1f - dread * 0.6f)),
                Offset(gx - len, gy - tilt),
                Offset(gx + len, gy + tilt),
                strokeWidth = 1.2f * u
            )
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
        val fog = quantize(huntingFog * (1f - flare))
        if (fog > 0.01f) {
            val fogEnd = screenY(0.45f * fog)
            drawRect(
                fx.fog(fog, horizonY - 20f * u, fogEnd + 30f * u),
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
        drawRect(fx.flareWash(0.35f * quantize(flare), w / 2f, horizonY, w * 0.7f))
    }
    if (submergeFog > 0f) {
        val veil = quantize(submergeFog)
        drawRect(Color(0xFF06283A).copy(alpha = 0.6f * veil))
        drawRect(fx.submergeVeil(0.7f * veil, h * 0.6f))
    }
    val hullRatio = if (run.maxHull > 0f) run.hull / run.maxHull else 1f
    if (hullRatio < 0.4f) {
        val intensity = quantize((0.4f - hullRatio) / 0.4f * blink(run.runElapsed, 5f), 16f)
        drawRect(fx.damageVignette(0.55f * intensity, w / 2f, h / 2f, w * 0.75f))
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
            val faded = if (p.state == PursuerState.LOST_TARGET) 0.4f else 1f

            // Displacement shadow and the wake they drag back towards the horizon.
            drawOval(
                Color(0xFF01070A).copy(alpha = 0.34f * faded),
                topLeft = Offset(-1.3f * r, -1.9f * r),
                size = Size(2.6f * r, 4.0f * r)
            )
            path.reset()
            path.moveTo(-0.45f * r, 0.9f * r)
            path.lineTo(0.45f * r, 0.9f * r)
            path.lineTo(1.5f * r, 4.4f * r)
            path.lineTo(-1.5f * r, 4.4f * r)
            path.close()
            drawPath(path, Color.White.copy(alpha = 0.09f * faded))

            pursuerHull(path, p.type, r)
            drawPath(path, enemyColor(p.type).copy(alpha = faded))

            // Deck shading: one side always darker, which keeps the form solid at small sizes.
            drawRect(
                Color.Black.copy(alpha = 0.18f * faded),
                topLeft = Offset(-1.5f * r, -0.1f * r),
                size = Size(1.5f * r, 1.4f * r)
            )

            // Superstructure and lights, different per class, so the threat is read from the
            // shape and not from the colour alone.
            when (p.type) {
                EnemyBoatType.PATROL -> {
                    drawRoundRect(Color(0xFF232A2E).copy(alpha = faded), topLeft = Offset(-0.42f * r, -0.5f * r), size = Size(0.84f * r, 1.0f * r), cornerRadius = CornerRadius(0.12f * r))
                    drawCircle(Palette.Amber.copy(alpha = 0.85f * faded), 0.22f * r, Offset(0f, -0.75f * r))
                }
                EnemyBoatType.POLICE -> {
                    drawRoundRect(Color(0xFF1B2740).copy(alpha = faded), topLeft = Offset(-0.38f * r, -0.5f * r), size = Size(0.76f * r, 0.9f * r), cornerRadius = CornerRadius(0.12f * r))
                    // Alternating light bar.
                    val blue = (run.runElapsed * 8f).toInt() % 2 == 0
                    drawRect(if (blue) Color(0xFF3060FF) else Palette.Red, topLeft = Offset(-0.45f * r, -0.78f * r), size = Size(0.45f * r, 0.20f * r))
                    drawRect(if (blue) Palette.Red else Color(0xFF3060FF), topLeft = Offset(0f, -0.78f * r), size = Size(0.45f * r, 0.20f * r))
                }
                EnemyBoatType.INTERCEPTOR -> {
                    drawRoundRect(Color(0xFF1E2A1E).copy(alpha = faded), topLeft = Offset(-0.3f * r, -0.9f * r), size = Size(0.6f * r, 1.3f * r), cornerRadius = CornerRadius(0.1f * r))
                    drawCircle(Palette.Cyan.copy(alpha = 0.5f * faded), 0.3f * r, Offset(-0.32f * r, 1.0f * r))
                    drawCircle(Palette.Cyan.copy(alpha = 0.5f * faded), 0.3f * r, Offset(0.32f * r, 1.0f * r))
                }
                EnemyBoatType.HUNTER -> {
                    // Runs dark: faceted deck, no navigation lights at all. You hear it first.
                    path.reset()
                    path.moveTo(0f, -1.6f * r)
                    path.lineTo(0.42f * r, 0.1f * r)
                    path.lineTo(0f, 0.7f * r)
                    path.lineTo(-0.42f * r, 0.1f * r)
                    path.close()
                    drawPath(path, Color(0xFF15181A).copy(alpha = faded))
                }
                EnemyBoatType.ARMORED -> {
                    // Raised bulwark ring and a heavy deck house.
                    drawOval(Color(0xFF6B5C46).copy(alpha = 0.55f * faded), topLeft = Offset(-1.1f * r, -0.8f * r), size = Size(2.2f * r, 2.0f * r), style = Stroke(0.18f * r))
                    drawRoundRect(Color(0xFF2E2A22).copy(alpha = faded), topLeft = Offset(-0.5f * r, -0.4f * r), size = Size(1.0f * r, 1.1f * r), cornerRadius = CornerRadius(0.1f * r))
                    drawCircle(Palette.Amber.copy(alpha = 0.7f * faded), 0.2f * r, Offset(0f, -0.62f * r))
                }
                EnemyBoatType.ELITE -> {
                    // Dorsal fin plus twin searchlights: the apex silhouette.
                    path.reset()
                    path.moveTo(0f, -1.1f * r)
                    path.lineTo(0.22f * r, 0.9f * r)
                    path.lineTo(-0.22f * r, 0.9f * r)
                    path.close()
                    drawPath(path, Color(0xFF3A0A0D).copy(alpha = faded))
                    drawCircle(Palette.Red.copy(alpha = 0.95f * faded), 0.22f * r, Offset(-0.5f * r, -0.5f * r))
                    drawCircle(Palette.Red.copy(alpha = 0.95f * faded), 0.22f * r, Offset(0.5f * r, -0.5f * r))
                }
            }

            // Silhouette edge, and a bow wave when it is actually closing.
            pursuerHull(path, p.type, r)
            drawPath(path, Palette.Red.copy(alpha = 0.85f * faded), style = Stroke(1.5f * u))
            if (p.state == PursuerState.PURSUIT || p.state == PursuerState.INTERCEPT) {
                drawOval(
                    Color.White.copy(alpha = 0.18f * faded),
                    topLeft = Offset(-0.8f * r, -2.4f * r),
                    size = Size(1.6f * r, 1.2f * r),
                    style = Stroke(1.4f * u)
                )
            }
            if (run.sonar.active) drawCircle(Palette.Cyan, r * 2.2f, Offset.Zero, style = Stroke(2f * u))
        }
    }
}

/**
 * Builds the player hull into [into]: a real boat form rather than a triangle. Curved sheer from
 * a fine bow through the shoulders to a square transom, drawn bow-up around the origin so the
 * caller can roll it. [scale] shrinks the same outline for the deck inset and the damage flash,
 * so every layer shares one silhouette.
 */
private fun playerHull(into: Path, u: Float, scale: Float = 1f) {
    val k = u * scale
    into.reset()
    into.moveTo(0f, -44f * k)
    into.quadraticTo(16f * k, -34f * k, 21f * k, -8f * k)
    into.quadraticTo(23f * k, 10f * k, 18f * k, 28f * k)
    into.lineTo(13f * k, 36f * k)
    into.lineTo(-13f * k, 36f * k)
    into.lineTo(-18f * k, 28f * k)
    into.quadraticTo(-23f * k, 10f * k, -21f * k, -8f * k)
    into.quadraticTo(-16f * k, -34f * k, 0f, -44f * k)
    into.close()
}

private fun DrawScope.drawPlayer(run: RunState, boat: BoatSpec, cx: Float, cy: Float, u: Float, h: Float, fx: SeaEffects) {
    val wake = fx.path
    val submerged = run.dive.submerged
    val boosting = run.speedFactor > 1.1f

    // Wake: a fan from the stern that bends away from the turn.
    val spread = (22f + (run.speedFactor - 0.8f) * 60f) * u
    val bend = -run.playerVelocityX * 120f * u
    wake.reset()
    wake.moveTo(cx - 8f * u, cy + 30f * u)
    wake.quadraticTo(cx - spread * 0.5f + bend * 0.4f, cy + (h - cy) * 0.5f, cx - spread + bend, h)
    wake.lineTo(cx + spread + bend, h)
    wake.quadraticTo(cx + spread * 0.5f + bend * 0.4f, cy + (h - cy) * 0.5f, cx + 8f * u, cy + 30f * u)
    wake.close()
    drawPath(wake, Color.White.copy(alpha = if (submerged) 0.04f else 0.13f))

    if (!submerged) {
        // Turbulent core inside the fan, and the two crest lines that make it read as a V.
        wake.reset()
        wake.moveTo(cx - 5f * u, cy + 30f * u)
        wake.quadraticTo(cx - spread * 0.22f + bend * 0.4f, cy + (h - cy) * 0.5f, cx - spread * 0.42f + bend, h)
        wake.lineTo(cx + spread * 0.42f + bend, h)
        wake.quadraticTo(cx + spread * 0.22f + bend * 0.4f, cy + (h - cy) * 0.5f, cx + 5f * u, cy + 30f * u)
        wake.close()
        drawPath(wake, Color.White.copy(alpha = if (boosting) 0.16f else 0.10f))
        drawPath(wake, Color.White.copy(alpha = 0.14f), style = Stroke(1.5f * u))

        // Engine light spilling onto the water astern: a tapered cyan column.
        wake.reset()
        wake.moveTo(cx - 9f * u, cy + 34f * u)
        wake.lineTo(cx + 9f * u, cy + 34f * u)
        wake.lineTo(cx + spread * 0.34f + bend * 0.7f, h)
        wake.lineTo(cx - spread * 0.34f + bend * 0.7f, h)
        wake.close()
        drawPath(wake, Palette.Cyan.copy(alpha = if (boosting) 0.13f else 0.07f))
    }

    translate(cx, cy) {
        rotate(run.playerRoll, pivot = Offset.Zero) {
            val alpha = if (submerged) 0.45f else 1f
            val hull = fx.path2
            val hullColor = Color(boat.hullColor)

            // BACKGROUND — displacement shadow, offset aft so the boat sits in the water.
            drawOval(
                Color(0xFF01070A).copy(alpha = 0.40f * alpha),
                topLeft = Offset(-26f * u, -38f * u),
                size = Size(52f * u, 88f * u)
            )

            // Bow wave: two short crests peeling back from the stem along the hull. Drawn as arcs
            // that start at the stem and sweep aft, never ahead of it — a full ellipse centred on
            // the bow reads as a ring floating over the boat, not as displaced water.
            if (!submerged) {
                val foam = fx.path2
                foam.reset()
                foam.moveTo(-1.5f * u, -41f * u)
                foam.quadraticTo(-12f * u, -34f * u, -17f * u, -16f * u)
                drawPath(foam, Color.White.copy(alpha = 0.30f), style = Stroke(2.2f * u))
                foam.reset()
                foam.moveTo(1.5f * u, -41f * u)
                foam.quadraticTo(12f * u, -34f * u, 17f * u, -16f * u)
                drawPath(foam, Color.White.copy(alpha = 0.30f), style = Stroke(2.2f * u))
            }

            // HULL.
            playerHull(hull, u)
            drawPath(hull, hullColor.copy(alpha = alpha))

            // LIGHT — the lee side darkens as the boat rolls, so the roll is visible on the hull
            // itself and not only in its rotation.
            val lee = (run.playerRoll / 18f).coerceIn(-1f, 1f)
            if (abs(lee) > 0.05f) {
                val side = if (lee > 0f) -1f else 1f
                hull.reset()
                hull.moveTo(0f, -44f * u)
                hull.quadraticTo(side * 18f * u, -30f * u, side * 22f * u, 4f * u)
                hull.quadraticTo(side * 20f * u, 26f * u, side * 13f * u, 36f * u)
                hull.lineTo(0f, 36f * u)
                hull.close()
                drawPath(hull, Color.Black.copy(alpha = 0.22f * abs(lee) * alpha))
            }

            // DETAILS — deck inset, then the superstructure on top of it. The deck has to be
            // clearly lighter than the hull: a dark cabin on a dark hull on dark water collapses
            // into one blob on a phone, which is what the first pass of this did.
            playerHull(hull, u, 0.70f)
            drawPath(hull, Color.White.copy(alpha = 0.15f * alpha))

            // STRUCTURE — cabin block, lighter than the hull so the boat has internal contrast,
            // with a lit top edge.
            drawRoundRect(
                Color(0xFF2E5260).copy(alpha = alpha),
                topLeft = Offset(-9.5f * u, -15f * u),
                size = Size(19f * u, 24f * u),
                cornerRadius = CornerRadius(3f * u)
            )
            drawLine(
                Palette.Cyan.copy(alpha = 0.30f * alpha),
                Offset(-8f * u, -14f * u),
                Offset(8f * u, -14f * u),
                strokeWidth = 1.4f * u
            )

            // Windscreen, raked back.
            hull.reset()
            hull.moveTo(-7f * u, -14f * u)
            hull.lineTo(7f * u, -14f * u)
            hull.lineTo(5f * u, -5f * u)
            hull.lineTo(-5f * u, -5f * u)
            hull.close()
            drawPath(hull, Color(0xFF02080C).copy(alpha = alpha))
            drawLine(
                Color.White.copy(alpha = 0.22f * alpha),
                Offset(-6f * u, -12.5f * u),
                Offset(2f * u, -12.5f * u),
                strokeWidth = 1.2f * u
            )

            // Side strakes: two rubbing strips down the length.
            drawLine(Color.White.copy(alpha = 0.13f * alpha), Offset(-18.5f * u, -6f * u), Offset(-15f * u, 28f * u), strokeWidth = 1.3f * u)
            drawLine(Color.White.copy(alpha = 0.13f * alpha), Offset(18.5f * u, -6f * u), Offset(15f * u, 28f * u), strokeWidth = 1.3f * u)

            // Deck hardware near the bow.
            drawCircle(Color.White.copy(alpha = 0.16f * alpha), 1.6f * u, Offset(0f, -30f * u))
            drawCircle(Color.White.copy(alpha = 0.12f * alpha), 1.4f * u, Offset(-5f * u, -22f * u))
            drawCircle(Color.White.copy(alpha = 0.12f * alpha), 1.4f * u, Offset(5f * u, -22f * u))

            // Navigation lights: red to port, green to starboard. Real naval language, and it
            // tells the player which way the bow is pointing at a glance.
            if (!submerged) {
                drawCircle(Palette.Red.copy(alpha = 0.85f), 2.2f * u, Offset(-20f * u, 0f))
                drawCircle(Palette.Green.copy(alpha = 0.85f), 2.2f * u, Offset(20f * u, 0f))
            }

            // EFFECT — engine glow, brighter and wider on the boost.
            val glow = if (boosting) 1.8f else 1f
            drawCircle(Palette.Cyan.copy(alpha = 0.22f * alpha), radius = 11f * u * glow, center = Offset(-7f * u, 38f * u))
            drawCircle(Palette.Cyan.copy(alpha = 0.22f * alpha), radius = 11f * u * glow, center = Offset(7f * u, 38f * u))
            drawCircle(Palette.Cyan.copy(alpha = 0.60f * alpha), radius = 6f * u * glow, center = Offset(-7f * u, 38f * u))
            drawCircle(Palette.Cyan.copy(alpha = 0.60f * alpha), radius = 6f * u * glow, center = Offset(7f * u, 38f * u))

            // Silhouette edge last, so the boat stays readable against bright water on a phone.
            playerHull(hull, u)
            drawPath(hull, Palette.Cyan.copy(alpha = 0.62f * alpha), style = Stroke(2f * u))

            if (run.invulnerable > 0f) {
                drawPath(hull, Color.White.copy(alpha = blink(run.runElapsed, 30f) * 0.45f))
            }
        }
    }
}

/** How many swell lines and water glints the sea carries. Kept here so the cost is one edit. */
private const val SWELL_LINES = 14
private const val GLINTS = 9

/**
 * Snaps a 0..1 driver onto [steps] levels. Anything that ends up baked into a cached gradient goes
 * through this first: detection and fog climb continuously, and without quantising, every frame
 * would produce a slightly different colour and defeat the cache in [SeaEffects].
 */
private fun quantize(v: Float, steps: Float = 24f): Float = (v * steps).toInt() / steps

/**
 * Builds a pursuer hull into [into], bow-up around the origin, sized from [r].
 *
 * Each class gets its own form on purpose: at these sizes on a phone, shape carries the threat
 * faster than colour does, and colour alone fails for a colour-blind player.
 */
private fun pursuerHull(into: Path, type: EnemyBoatType, r: Float) {
    into.reset()
    when (type) {
        // Beamy workboat: blunt stem, full shoulders, square transom.
        EnemyBoatType.PATROL -> {
            into.moveTo(0f, -1.9f * r)
            into.quadraticTo(0.95f * r, -1.4f * r, 1.02f * r, -0.1f * r)
            into.lineTo(0.88f * r, 1.25f * r)
            into.lineTo(-0.88f * r, 1.25f * r)
            into.lineTo(-1.02f * r, -0.1f * r)
            into.quadraticTo(-0.95f * r, -1.4f * r, 0f, -1.9f * r)
        }
        // Same family as the patrol but finer and faster-looking.
        EnemyBoatType.POLICE -> {
            into.moveTo(0f, -2.1f * r)
            into.quadraticTo(0.78f * r, -1.5f * r, 0.85f * r, -0.1f * r)
            into.lineTo(0.72f * r, 1.2f * r)
            into.lineTo(-0.72f * r, 1.2f * r)
            into.lineTo(-0.85f * r, -0.1f * r)
            into.quadraticTo(-0.78f * r, -1.5f * r, 0f, -2.1f * r)
        }
        // Needle: long, narrow, almost no freeboard.
        EnemyBoatType.INTERCEPTOR -> {
            into.moveTo(0f, -2.9f * r)
            into.quadraticTo(0.58f * r, -1.6f * r, 0.66f * r, 0.2f * r)
            into.lineTo(0.56f * r, 1.3f * r)
            into.lineTo(-0.56f * r, 1.3f * r)
            into.lineTo(-0.66f * r, 0.2f * r)
            into.quadraticTo(-0.58f * r, -1.6f * r, 0f, -2.9f * r)
        }
        // Faceted and chined: hard angles, no curves anywhere.
        EnemyBoatType.HUNTER -> {
            into.moveTo(0f, -2.5f * r)
            into.lineTo(0.5f * r, -1.5f * r)
            into.lineTo(0.82f * r, 0.2f * r)
            into.lineTo(0.62f * r, 1.3f * r)
            into.lineTo(-0.62f * r, 1.3f * r)
            into.lineTo(-0.82f * r, 0.2f * r)
            into.lineTo(-0.5f * r, -1.5f * r)
        }
        // Wide and heavy: it does not turn, it arrives.
        EnemyBoatType.ARMORED -> {
            into.moveTo(0f, -1.5f * r)
            into.quadraticTo(1.3f * r, -1.1f * r, 1.45f * r, 0.1f * r)
            into.lineTo(1.3f * r, 1.6f * r)
            into.lineTo(-1.3f * r, 1.6f * r)
            into.lineTo(-1.45f * r, 0.1f * r)
            into.quadraticTo(-1.3f * r, -1.1f * r, 0f, -1.5f * r)
        }
        // Longest and sharpest, with swept-back quarters.
        EnemyBoatType.ELITE -> {
            into.moveTo(0f, -3.1f * r)
            into.quadraticTo(0.72f * r, -1.7f * r, 0.9f * r, 0.4f * r)
            into.lineTo(1.15f * r, 1.5f * r)
            into.lineTo(0.42f * r, 1.15f * r)
            into.lineTo(-0.42f * r, 1.15f * r)
            into.lineTo(-1.15f * r, 1.5f * r)
            into.lineTo(-0.9f * r, 0.4f * r)
            into.quadraticTo(-0.72f * r, -1.7f * r, 0f, -3.1f * r)
        }
    }
    into.close()
}

private fun enemyColor(type: EnemyBoatType): Color = when (type) {
    EnemyBoatType.PATROL -> Color(0xFF5A6166)
    EnemyBoatType.INTERCEPTOR -> Color(0xFF4F5E4F)
    EnemyBoatType.POLICE -> Color(0xFF22408A)
    EnemyBoatType.HUNTER -> Color(0xFF232628)
    EnemyBoatType.ARMORED -> Color(0xFF5E5040)
    EnemyBoatType.ELITE -> Color(0xFF7A0E12)
}
