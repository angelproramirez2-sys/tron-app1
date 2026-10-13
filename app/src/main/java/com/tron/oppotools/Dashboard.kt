package com.tron.oppotools

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.io.File

data class Snap(
    val pct: Int,
    val charging: Boolean,
    val plugged: String,
    val battTemp: Float,
    val voltage: Int,
    val health: String,
    val tech: String,
    val currentMa: Int,
    val remaining: String,
    val cpuTemp: Float?,
    val storTotal: Long,
    val storFree: Long,
    val ramTotal: Long,
    val ramFree: Long,
    val uptime: String,
    val screen: String
)

private fun gb(b: Long) = String.format("%.1f GB", b / 1e9)

private fun fmtHours(h: Double): String {
    val hh = h.toInt()
    val mm = ((h - hh) * 60).toInt()
    return if (hh >= 24) "${hh / 24} d ${hh % 24} h" else "$hh h $mm min"
}

private fun healthText(h: Int) = when (h) {
    BatteryManager.BATTERY_HEALTH_GOOD -> "Buena"
    BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Sobrecalentada"
    BatteryManager.BATTERY_HEALTH_DEAD -> "Muerta"
    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Sobrevoltaje"
    BatteryManager.BATTERY_HEALTH_COLD -> "Fría"
    else -> "Desconocida"
}

private fun pluggedText(p: Int) = when (p) {
    BatteryManager.BATTERY_PLUGGED_AC -> "Cargador (AC)"
    BatteryManager.BATTERY_PLUGGED_USB -> "USB"
    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Inalámbrico"
    else -> "No conectado"
}

/** Estima el tiempo restante midiendo cuánto baja la batería mientras la app se usa. */
private fun estimateRemaining(ctx: Context, bm: BatteryManager, level: Int, charging: Boolean): String {
    val p = ctx.getSharedPreferences("bat", Context.MODE_PRIVATE)
    val now = System.currentTimeMillis()
    if (charging) {
        p.edit().putInt("lvl", level).putLong("t", now).apply()
        if (level >= 100) return "Carga completa"
        if (Build.VERSION.SDK_INT >= 28) {
            val ms = bm.computeChargeTimeRemaining()
            if (ms > 0) return "Carga completa en " + fmtHours(ms / 3600000.0)
        }
        return "Cargando..."
    }
    val l0 = p.getInt("lvl", -1)
    val t0 = p.getLong("t", 0L)
    if (l0 == -1 || level > l0) {
        p.edit().putInt("lvl", level).putLong("t", now).apply()
        return "Calculando..."
    }
    val drop = l0 - level
    val hours = (now - t0) / 3600000.0
    if (drop < 1 || hours <= 0.0) return "Calculando..."
    val rate = drop / hours
    return "~" + fmtHours(level / rate)
}

private fun readCpuTemp(): Float? {
    return try {
        val zones = File("/sys/class/thermal").listFiles { f -> f.name.startsWith("thermal_zone") }
            ?: return null
        var fallback: Float? = null
        for (z in zones) {
            val raw = File(z, "temp").readText().trim().toFloatOrNull() ?: continue
            val t = if (raw > 1000f) raw / 1000f else raw
            if (t < 5f || t > 120f) continue
            val type = try { File(z, "type").readText().trim().lowercase() } catch (e: Exception) { "" }
            if (type.contains("cpu") || type.contains("soc") || type.contains("tsens")) return t
            if (fallback == null) fallback = t
        }
        fallback
    } catch (e: Exception) {
        null
    }
}

fun readSnap(ctx: Context): Snap {
    val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val pct = if (level >= 0 && scale > 0) level * 100 / scale else 0
    val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    val temp = (i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
    val volt = i?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
    val health = healthText(i?.getIntExtra(BatteryManager.EXTRA_HEALTH, 0) ?: 0)
    val plugged = pluggedText(i?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0)
    val tech = i?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "?"
    val cur = Math.abs(bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)) / 1000

    val st = StatFs(Environment.getDataDirectory().path)
    val total = st.blockCountLong * st.blockSizeLong
    val free = st.availableBlocksLong * st.blockSizeLong

    val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val mi = ActivityManager.MemoryInfo()
    am.getMemoryInfo(mi)

    val secs = SystemClock.elapsedRealtime() / 1000
    val up = "${secs / 86400} d ${(secs % 86400) / 3600} h ${(secs % 3600) / 60} min"
    val dm = ctx.resources.displayMetrics

    return Snap(
        pct, charging, plugged, temp, volt, health, tech, cur,
        estimateRemaining(ctx, bm, pct, charging),
        readCpuTemp(), total, free, mi.totalMem, mi.availMem, up,
        "${dm.widthPixels}x${dm.heightPixels} @ ${dm.densityDpi}dpi"
    )
}

@Composable
fun Dashboard(onTools: () -> Unit) {
    val ctx = LocalContext.current
    var s by remember { mutableStateOf(readSnap(ctx)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            s = readSnap(ctx)
        }
    }

    val battColor = when {
        s.pct <= 15 -> Orange
        else -> Cyan
    }
    val tempColor = if (s.battTemp >= 40f) Orange else Cyan
    val storUsed = (s.storTotal - s.storFree).coerceAtLeast(0L)
    val ramUsed = (s.ramTotal - s.ramFree).coerceAtLeast(0L)

    TronBackground {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TopBar("SISTEMA // OPPO A38", null)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TronGauge(s.pct / 100f, "${s.pct}%", "BATERÍA", color = battColor)
                TronGauge(
                    if (s.storTotal > 0) storUsed / s.storTotal.toFloat() else 0f,
                    "${(storUsed * 100 / s.storTotal.coerceAtLeast(1L))}%", "ALMACEN."
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TronGauge(
                    if (s.ramTotal > 0) ramUsed / s.ramTotal.toFloat() else 0f,
                    "${(ramUsed * 100 / s.ramTotal.coerceAtLeast(1L))}%", "RAM"
                )
                TronGauge((s.battTemp / 60f), "${"%.1f".format(s.battTemp)}°", "TEMP", color = tempColor)
            }

            TronPanel("BATERÍA") {
                StatRow("Nivel", "${s.pct}%", battColor)
                StatRow("Estado", if (s.charging) "Cargando" else "Descargando")
                StatRow("Fuente", s.plugged)
                StatRow("Duración estimada", s.remaining, Cyan)
                StatRow("Temperatura", "%.1f °C".format(s.battTemp), tempColor)
                StatRow("Voltaje", "${s.voltage} mV")
                StatRow("Corriente", if (s.currentMa > 0) "${s.currentMa} mA" else "N/D")
                StatRow("Salud", s.health)
                StatRow("Tecnología", s.tech)
            }

            TronPanel("TEMPERATURAS") {
                StatRow("Batería", "%.1f °C".format(s.battTemp), tempColor)
                StatRow(
                    "Procesador / teléfono",
                    s.cpuTemp?.let { "%.1f °C".format(it) } ?: "N/D (Android lo bloquea)",
                    if ((s.cpuTemp ?: 0f) >= 45f) Orange else Color.White
                )
            }

            TronPanel("ALMACENAMIENTO") {
                StatRow("Total", gb(s.storTotal))
                StatRow("Usado", gb(storUsed))
                StatRow("Libre", gb(s.storFree), Cyan)
            }

            TronPanel("MEMORIA RAM") {
                StatRow("Total", gb(s.ramTotal))
                StatRow("Usada", gb(ramUsed))
                StatRow("Libre", gb(s.ramFree), Cyan)
            }

            TronPanel("DISPOSITIVO") {
                StatRow("Modelo", "${Build.MANUFACTURER} ${Build.MODEL}")
                StatRow("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                StatRow("Procesador", Build.HARDWARE)
                StatRow("Núcleos", "${Runtime.getRuntime().availableProcessors()}")
                StatRow("Pantalla", s.screen)
                StatRow("Encendido hace", s.uptime)
            }

            TronButton("HERRAMIENTAS  ▶", Modifier.fillMaxWidth(), onClick = onTools)
        }
    }
}
