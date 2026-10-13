package com.tron.oppotools

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.ln
import kotlin.math.sqrt

/* ---------- Utilidades ---------- */

@Composable
fun sensorState(type: Int): Pair<State<FloatArray?>, Boolean> {
    val ctx = LocalContext.current
    val sm = remember { ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensor = remember(type) { sm.getDefaultSensor(type) }
    val state = remember(type) { mutableStateOf<FloatArray?>(null) }
    DisposableEffect(type) {
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                state.value = e.values.clone()
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (sensor != null) sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(l) }
    }
    return state to (sensor != null)
}

@Composable
fun ToolScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    TronBackground {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TopBar(title, onBack)
            content()
        }
    }
}

@Composable
fun NoSensor(name: String) {
    TronPanel("AVISO") {
        Text(
            "Este teléfono no tiene el sensor: $name",
            color = Orange, fontFamily = Mono, fontSize = 13.sp
        )
    }
}

/* ---------- Menú de herramientas ---------- */

private data class Tool(val screen: Screen, val code: String, val name: String)

private val tools = listOf(
    Tool(Screen.COMPASS, "01", "BRÚJULA"),
    Tool(Screen.METAL, "02", "DETECTOR DE METAL"),
    Tool(Screen.LEVEL, "03", "NIVEL"),
    Tool(Screen.LIGHT, "04", "LUXÓMETRO"),
    Tool(Screen.PROXIMITY, "05", "PROXIMIDAD"),
    Tool(Screen.TORCH, "06", "LINTERNA"),
    Tool(Screen.SENSORS, "07", "SENSORES"),
    Tool(Screen.CODES, "08", "CÓDIGOS SECRETOS")
)

@Composable
fun ToolsMenu(onSelect: (Screen) -> Unit, onBack: () -> Unit) {
    TronBackground {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TopBar("HERRAMIENTAS", onBack)
            tools.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { t ->
                        Column(
                            Modifier
                                .weight(1f)
                                .height(110.dp)
                                .background(Color(0x1A00E5FF), RoundedCornerShape(6.dp))
                                .border(1.5.dp, Cyan, RoundedCornerShape(6.dp))
                                .clickable { onSelect(t.screen) }
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("[${t.code}]", color = Orange, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text(t.name, color = Cyan, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    if (row.size == 1) Box(Modifier.weight(1f))
                }
            }
        }
    }
}

/* ---------- Brújula ---------- */

@Composable
fun CompassScreen(onBack: () -> Unit) {
    val (rot, ok) = sensorState(Sensor.TYPE_ROTATION_VECTOR)
    var az by remember { mutableStateOf(0f) }
    val v = rot.value

    LaunchedEffect(v) {
        if (v != null) {
            val m = FloatArray(9)
            val o = FloatArray(3)
            SensorManager.getRotationMatrixFromVector(m, v.copyOf(minOf(v.size, 4)))
            SensorManager.getOrientation(m, o)
            val target = (Math.toDegrees(o[0].toDouble()).toFloat() + 360f) % 360f
            val d = ((target - az + 540f) % 360f) - 180f
            az = (az + d * 0.25f + 360f) % 360f
        }
    }

    val paint = remember {
        Paint().apply {
            color = android.graphics.Color.rgb(0, 229, 255)
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
    }

    ToolScaffold("BRÚJULA", onBack) {
        if (!ok) {
            NoSensor("brújula (vector de rotación)")
            return@ToolScaffold
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(300.dp)) {
                val c = center
                val r = size.minDimension / 2 - 10.dp.toPx()
                drawCircle(Cyan, r, c, style = Stroke(2.dp.toPx()))
                drawCircle(CyanDim, r * 0.6f, c, style = Stroke(1.dp.toPx()))
                for (i in 0 until 72) {
                    val len = if (i % 6 == 0) 18.dp.toPx() else 8.dp.toPx()
                    rotate(i * 5f - az, c) {
                        drawLine(Cyan, Offset(c.x, c.y - r), Offset(c.x, c.y - r + len), 2f)
                    }
                }
                paint.textSize = 22.dp.toPx()
                val labels = listOf("N", "E", "S", "O")
                for (k in 0..3) {
                    rotate(k * 90f - az, c) {
                        drawIntoCanvas {
                            it.nativeCanvas.drawText(labels[k], c.x, c.y - r + 48.dp.toPx(), paint)
                        }
                    }
                }
                // Aguja fija
                drawLine(Orange, Offset(c.x, c.y - r - 6.dp.toPx()), Offset(c.x, c.y - r * 0.55f), 5f)
                drawCircle(Orange, 5.dp.toPx(), c)
            }
        }
        val dirs = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
        val dir = dirs[((az + 22.5f) / 45f).toInt() % 8]
        TronPanel("RUMBO") {
            StatRow("Grados", "%.0f°".format(az), Orange)
            StatRow("Dirección", dir)
        }
        Text(
            "Mueve el teléfono en forma de 8 para calibrar. Aléjalo de imanes y metales.",
            color = Cyan.copy(alpha = 0.7f), fontFamily = Mono, fontSize = 11.sp
        )
    }
}

/* ---------- Detector de metal ---------- */

@Composable
fun MetalScreen(onBack: () -> Unit) {
    val (mag, ok) = sensorState(Sensor.TYPE_MAGNETIC_FIELD)
    var smooth by remember { mutableStateOf(0f) }
    var base by remember { mutableStateOf<Float?>(null) }
    var sound by remember { mutableStateOf(true) }
    val history = remember { mutableStateListOf<Float>() }
    val v = mag.value

    LaunchedEffect(v) {
        if (v != null) {
            val m = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
            smooth = if (smooth == 0f) m else smooth * 0.7f + m * 0.3f
            if (base == null) base = smooth
            history.add(smooth)
            if (history.size > 120) history.removeAt(0)
        }
    }

    val dev = abs(smooth - (base ?: smooth))
    val devState = rememberUpdatedState(dev)
    val soundState = rememberUpdatedState(sound)
    val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 70) }
    DisposableEffect(Unit) { onDispose { tone.release() } }
    LaunchedEffect(Unit) {
        while (true) {
            val d = devState.value
            if (d > 3f && soundState.value) {
                tone.startTone(ToneGenerator.TONE_PROP_BEEP, 60)
                delay((900L - (d * 20f).toLong()).coerceIn(120L, 900L))
            } else {
                delay(150L)
            }
        }
    }

    val strength = (dev / 50f).coerceIn(0f, 1f)
    val status = when {
        dev > 15f -> "METAL MUY CERCA"
        dev > 3f -> "METAL CERCA"
        else -> "SIN METAL"
    }
    val statusColor = if (dev > 3f) Orange else Cyan

    ToolScaffold("DETECTOR DE METAL", onBack) {
        if (!ok) {
            NoSensor("magnetómetro")
            return@ToolScaffold
        }
        TronPanel("SEÑAL") {
            Text(status, color = statusColor, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Box(
                Modifier
                    .padding(vertical = 10.dp)
                    .fillMaxWidth()
                    .height(14.dp)
                    .background(CyanDim)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(strength)
                        .height(14.dp)
                        .background(statusColor)
                )
            }
            StatRow("Campo magnético", "%.1f µT".format(smooth))
            StatRow("Referencia", "%.1f µT".format(base ?: 0f))
            StatRow("Desviación", "%.1f µT".format(dev), statusColor)
        }
        TronPanel("GRÁFICA") {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                if (history.size > 2) {
                    val lo = history.min()
                    val hi = maxOf(history.max(), lo + 10f)
                    val path = Path()
                    history.forEachIndexed { i, y ->
                        val px = i / 119f * size.width
                        val py = size.height - (y - lo) / (hi - lo) * size.height
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    drawPath(path, Cyan, style = Stroke(2.dp.toPx()))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TronButton("CALIBRAR", Modifier.weight(1f)) { base = smooth }
            TronButton(if (sound) "SONIDO: SÍ" else "SONIDO: NO", Modifier.weight(1f), if (sound) Cyan else Orange) {
                sound = !sound
            }
        }
        Text(
            "Detecta metales con hierro (imanes, llaves, tuberías) a pocos centímetros. " +
                "Pulsa CALIBRAR lejos de metales y luego acerca el teléfono. " +
                "No detecta oro, plata ni aluminio.",
            color = Cyan.copy(alpha = 0.7f), fontFamily = Mono, fontSize = 11.sp
        )
    }
}

/* ---------- Nivel ---------- */

@Composable
fun LevelScreen(onBack: () -> Unit) {
    val (acc, ok) = sensorState(Sensor.TYPE_ACCELEROMETER)
    val v = acc.value
    ToolScaffold("NIVEL", onBack) {
        if (!ok) {
            NoSensor("acelerómetro")
            return@ToolScaffold
        }
        val x = v?.get(0) ?: 0f
        val y = v?.get(1) ?: 0f
        val g = 9.81f
        val bx = (x / g).coerceIn(-1f, 1f)
        val by = (-y / g).coerceIn(-1f, 1f)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(280.dp)) {
                val c = center
                val r = size.minDimension / 2 - 10.dp.toPx()
                drawCircle(Cyan, r, c, style = Stroke(2.dp.toPx()))
                drawCircle(CyanDim, r / 2, c, style = Stroke(1.dp.toPx()))
                drawLine(CyanDim, Offset(c.x - r, c.y), Offset(c.x + r, c.y), 1f)
                drawLine(CyanDim, Offset(c.x, c.y - r), Offset(c.x, c.y + r), 1f)
                val bub = Offset(c.x + bx * (r - 16.dp.toPx()), c.y + by * (r - 16.dp.toPx()))
                val flat = abs(bx) < 0.03f && abs(by) < 0.03f
                drawCircle(if (flat) Cyan else Orange, 16.dp.toPx(), bub)
            }
        }
        val rollDeg = Math.toDegrees(asin(bx.toDouble())).toFloat()
        val pitchDeg = Math.toDegrees(asin(by.toDouble())).toFloat()
        TronPanel("INCLINACIÓN") {
            StatRow("Horizontal", "%.1f°".format(rollDeg))
            StatRow("Vertical", "%.1f°".format(pitchDeg))
        }
        Text("Apoya el teléfono plano sobre la superficie.", color = Cyan.copy(alpha = 0.7f), fontFamily = Mono, fontSize = 11.sp)
    }
}

/* ---------- Luxómetro ---------- */

@Composable
fun LightScreen(onBack: () -> Unit) {
    val (light, ok) = sensorState(Sensor.TYPE_LIGHT)
    ToolScaffold("LUXÓMETRO", onBack) {
        if (!ok) {
            NoSensor("luz ambiental")
            return@ToolScaffold
        }
        val lux = light.value?.get(0) ?: 0f
        val frac = (ln(1f + lux) / ln(10001f)).coerceIn(0f, 1f)
        TronGauge(frac, "%.0f".format(lux), "LUX", Modifier.fillMaxWidth())
        TronPanel("LUZ AMBIENTAL") {
            val level = when {
                lux < 10 -> "Oscuridad"
                lux < 200 -> "Interior tenue"
                lux < 1000 -> "Interior iluminado"
                lux < 10000 -> "Luz fuerte / sombra exterior"
                else -> "Sol directo"
            }
            StatRow("Intensidad", "%.1f lx".format(lux))
            StatRow("Nivel", level, Cyan)
        }
    }
}

/* ---------- Proximidad ---------- */

@Composable
fun ProximityScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val (prox, ok) = sensorState(Sensor.TYPE_PROXIMITY)
    val sm = remember { ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val max = remember { sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.maximumRange ?: 5f }
    ToolScaffold("PROXIMIDAD", onBack) {
        if (!ok) {
            NoSensor("proximidad")
            return@ToolScaffold
        }
        val d = prox.value?.get(0) ?: max
        val near = d < max
        TronPanel("SENSOR") {
            Text(
                if (near) "OBJETO CERCA" else "LIBRE",
                color = if (near) Orange else Cyan, fontFamily = Mono,
                fontWeight = FontWeight.Bold, fontSize = 24.sp
            )
            StatRow("Distancia", "%.1f cm".format(d))
            StatRow("Alcance máximo", "%.1f cm".format(max))
        }
        Text("Tapa con la mano la parte superior de la pantalla para probar.", color = Cyan.copy(alpha = 0.7f), fontFamily = Mono, fontSize = 11.sp)
    }
}

/* ---------- Linterna ---------- */

@Composable
fun TorchScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val cm = remember { ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    val id = remember {
        runCatching {
            cm.cameraIdList.firstOrNull {
                cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }
    var on by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose { runCatching { id?.let { cm.setTorchMode(it, false) } } }
    }
    ToolScaffold("LINTERNA", onBack) {
        if (id == null) {
            NoSensor("flash")
            return@ToolScaffold
        }
        TronButton(
            if (on) "APAGAR" else "ENCENDER",
            Modifier.fillMaxWidth(),
            if (on) Orange else Cyan
        ) {
            on = !on
            runCatching { cm.setTorchMode(id, on) }
        }
    }
}

/* ---------- Lista de sensores ---------- */

@Composable
fun SensorsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val sm = remember { ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val list = remember { sm.getSensorList(Sensor.TYPE_ALL) }
    ToolScaffold("SENSORES (${list.size})", onBack) {
        list.forEach { s ->
            TronPanel(s.stringType.substringAfterLast('.').uppercase()) {
                StatRow("Nombre", s.name)
                StatRow("Fabricante", s.vendor)
                StatRow("Alcance", "%.2f".format(s.maximumRange))
                StatRow("Consumo", "%.2f mA".format(s.power))
            }
        }
    }
}

/* ---------- Códigos secretos ---------- */

private val secretCodes = listOf(
    Triple("*#06#", "IMEI del teléfono", true),
    Triple("*#*#4636#*#*", "Información y uso del teléfono", true),
    Triple("*#*#2664#*#*", "Prueba de pantalla táctil", false),
    Triple("*#*#0*#*#*", "Prueba de pantalla LCD", false),
    Triple("*#800#", "Menú de ingeniería / pruebas (ColorOS)", false),
    Triple("*#899#", "Menú de pruebas (ColorOS)", false),
    Triple("*#6776#", "Versión de software (ColorOS)", false)
)

@Composable
fun CodesScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    ToolScaffold("CÓDIGOS SECRETOS", onBack) {
        Text(
            "Al tocar un código se abre el marcador con el código listo. " +
                "Varios códigos dependen del modelo y la versión de ColorOS, así que algunos pueden no hacer nada en tu A38.",
            color = Cyan.copy(alpha = 0.7f), fontFamily = Mono, fontSize = 11.sp
        )
        secretCodes.forEach { (code, desc, _) ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0x1A00E5FF), RoundedCornerShape(6.dp))
                    .border(1.dp, Cyan, RoundedCornerShape(6.dp))
                    .clickable {
                        runCatching {
                            ctx.startActivity(
                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(code)))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                    .padding(12.dp)
            ) {
                Text(code, color = Orange, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(desc, color = Color.White, fontFamily = Mono, fontSize = 12.sp)
            }
        }
    }
}
