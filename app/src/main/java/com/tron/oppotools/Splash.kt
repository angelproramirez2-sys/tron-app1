package com.tron.oppotools

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val SPLASH_MS = 3400L

@Composable
fun CodeRainSplash(onDone: () -> Unit) {
    var time by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        val start = withFrameMillisCompat()
        while (true) {
            time = withFrameMillisCompat() - start
            if (time >= SPLASH_MS) {
                onDone()
                break
            }
        }
    }

    val paint = remember {
        Paint().apply {
            color = android.graphics.Color.rgb(0, 229, 255)
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }
    }
    val chars = "01ABCDEF<>/{}[]#\$%&*+=;:"
    val cellPx = with(LocalDensity.current) { 18.dp.toPx() }

    Box(Modifier.fillMaxSize().background(Bg)) {
        Canvas(Modifier.fillMaxSize()) {
            paint.textSize = cellPx
            val cols = (size.width / cellPx).toInt()
            val rows = (size.height / cellPx).toInt()
            if (rows > 0) {
                drawIntoCanvas { canvas ->
                    for (c in 0 until cols) {
                        val speed = 6f + (c * 37 % 7)
                        val offset = (c * 53 % rows)
                        val tail = 12 + (c * 11 % 10)
                        val head = (time / 1000f * speed + offset) % (rows + tail)
                        for (r in 0 until rows) {
                            val d = head - r
                            if (d >= 0f && d < tail) {
                                paint.alpha = if (d < 1f) 255 else (255 * (1f - d / tail)).toInt()
                                val idx = ((c * 31L + r * 17L + time / 120) % chars.length).toInt()
                                canvas.nativeCanvas.drawText(
                                    chars[idx].toString(), c * cellPx, (r + 1) * cellPx, paint
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(
            Modifier
                .align(Alignment.Center)
                .background(Bg.copy(alpha = 0.85f))
                .padding(horizontal = 28.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "TRON TOOLS",
                color = Cyan,
                fontFamily = Mono,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                letterSpacing = 6.sp
            )
            Text(
                "OPPO A38 // INICIANDO SISTEMA",
                color = Cyan.copy(alpha = 0.8f),
                fontFamily = Mono,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
            )
            Box(
                Modifier
                    .fillMaxWidth(0.6f)
                    .height(4.dp)
                    .background(Color(0xFF0B3C4A))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth((time / SPLASH_MS.toFloat()).coerceIn(0f, 1f))
                        .height(4.dp)
                        .background(Cyan)
                )
            }
        }
    }
}

private suspend fun withFrameMillisCompat(): Long =
    androidx.compose.runtime.withFrameMillis { it }
