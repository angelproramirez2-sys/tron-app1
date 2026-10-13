package com.tron.oppotools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Bg = Color(0xFF02070D)
val Cyan = Color(0xFF00E5FF)
val CyanDim = Color(0xFF0B3C4A)
val Orange = Color(0xFFFF7A00)
val Mono = FontFamily.Monospace

@Composable
fun TronBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .drawBehind {
                val step = 40.dp.toPx()
                val line = CyanDim.copy(alpha = 0.45f)
                var x = 0f
                while (x < size.width) {
                    drawLine(line, Offset(x, 0f), Offset(x, size.height), 1f)
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(line, Offset(0f, y), Offset(size.width, y), 1f)
                    y += step
                }
            },
        content = content
    )
}

@Composable
fun TronPanel(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x1A00E5FF), RoundedCornerShape(6.dp))
            .border(1.dp, Cyan, RoundedCornerShape(6.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "// $title",
            color = Cyan,
            fontFamily = Mono,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Column(Modifier.padding(top = 8.dp), content = content)
    }
}

@Composable
fun StatRow(key: String, value: String, valueColor: Color = Color.White) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(key, color = Cyan.copy(alpha = 0.8f), fontFamily = Mono, fontSize = 12.sp)
        Text(
            value,
            color = valueColor,
            fontFamily = Mono,
            fontSize = 12.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
fun TronGauge(
    progress: Float,
    big: String,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = Cyan
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(100.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Butt)
                val inset = 8.dp.toPx()
                val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                drawArc(CyanDim, 135f, 270f, false, Offset(inset, inset), arcSize, style = stroke)
                drawArc(
                    color, 135f, 270f * progress.coerceIn(0f, 1f), false,
                    Offset(inset, inset), arcSize, style = stroke
                )
            }
            Text(big, color = Color.White, fontFamily = Mono, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Text(label, color = Cyan, fontFamily = Mono, fontSize = 11.sp, letterSpacing = 1.sp)
    }
}

@Composable
fun TronButton(text: String, modifier: Modifier = Modifier, color: Color = Cyan, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .border(1.5.dp, color, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 2.sp)
    }
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Text(
                "◀",
                color = Cyan,
                fontSize = 22.sp,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(end = 14.dp, top = 4.dp, bottom = 4.dp)
            )
        }
        Text(
            title,
            color = Cyan,
            fontFamily = Mono,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            letterSpacing = 3.sp
        )
    }
}
