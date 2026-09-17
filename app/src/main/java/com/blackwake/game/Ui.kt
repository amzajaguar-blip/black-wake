package com.blackwake.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Palette {
    val Ink = Color(0xFF041018)
    val Deep = Color(0xFF020A10)
    val Panel = Color(0xD9061820)
    val PanelSolid = Color(0xFF071C24)
    val Line = Color(0x552BE7E0)
    val Cyan = Color(0xFF2BE7E0)
    val Amber = Color(0xFFFFB45F)
    val Red = Color(0xFFEF464B)
    val Green = Color(0xFF5BE38C)
    val Text = Color(0xFFE9F8F4)
    val Muted = Color(0xFF8FA7A9)
    val Dim = Color(0xFF3B5157)
    val Storm = Color(0xFF031A33)
    val BlackTide = Color(0xFF010A14)
}

val Mono = FontFamily.Monospace

fun toneColor(tone: MessageTone): Color = when (tone) {
    MessageTone.INFO -> Palette.Cyan
    MessageTone.WARNING -> Palette.Amber
    MessageTone.DANGER -> Palette.Red
}

/** Slow blink for critical readouts, driven by game time so it freezes when paused. */
fun blink(time: Float, speed: Float = 8f): Float = (kotlin.math.sin(time * speed) * 0.35f + 0.65f)

@Composable
fun Label(text: String, color: Color = Palette.Muted, size: TextUnit = 10.sp, modifier: Modifier = Modifier) {
    Text(text, color = color, fontSize = size, fontFamily = Mono, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = modifier)
}

@Composable
fun Panel(modifier: Modifier = Modifier, border: Color = Palette.Line, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .background(Palette.Panel)
            .border(1.dp, border)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        content = content
    )
}

@Composable
fun TacticalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Palette.Cyan,
    filled: Boolean = false,
    enabled: Boolean = true
) {
    val tint = if (enabled) color else Palette.Dim
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .background(if (filled && enabled) tint else Palette.Panel)
            .border(1.dp, tint)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (filled && enabled) Palette.Ink else tint,
            fontSize = 13.sp,
            fontFamily = Mono,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )
    }
}

/** Static nautical-chart backdrop for the menu screens: drawn once, no animation loop. */
@Composable
fun ChartBackdrop(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Palette.Ink)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.radialGradient(listOf(Color(0xFF0A2E3E), Palette.Ink), center = Offset(size.width * 0.72f, size.height * 0.45f), radius = size.maxDimension * 0.7f))
            val step = size.height / 8f
            var x = 0f
            while (x < size.width) {
                drawLine(Palette.Cyan.copy(alpha = 0.05f), Offset(x, 0f), Offset(x, size.height), 1f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(Palette.Cyan.copy(alpha = 0.05f), Offset(0f, y), Offset(size.width, y), 1f)
                y += step
            }
            val center = Offset(size.width * 0.72f, size.height * 0.45f)
            for (i in 1..4) {
                drawCircle(Palette.Cyan.copy(alpha = 0.07f), radius = step * i * 1.3f, center = center, style = Stroke(1.5f))
            }
        }
        content()
    }
}
