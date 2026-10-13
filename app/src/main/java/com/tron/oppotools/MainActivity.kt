package com.tron.oppotools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

enum class Screen {
    SPLASH, HOME, TOOLS,
    COMPASS, METAL, LEVEL, LIGHT, PROXIMITY, TORCH, SENSORS, CODES
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TronApp() }
    }
}

@Composable
fun TronApp() {
    var screen by remember { mutableStateOf(Screen.SPLASH) }

    BackHandler(enabled = screen != Screen.SPLASH && screen != Screen.HOME) {
        screen = if (screen == Screen.TOOLS) Screen.HOME else Screen.TOOLS
    }

    val toTools = { screen = Screen.TOOLS }

    when (screen) {
        Screen.SPLASH -> CodeRainSplash { screen = Screen.HOME }
        Screen.HOME -> Dashboard(onTools = { screen = Screen.TOOLS })
        Screen.TOOLS -> ToolsMenu(onSelect = { screen = it }, onBack = { screen = Screen.HOME })
        Screen.COMPASS -> CompassScreen(toTools)
        Screen.METAL -> MetalScreen(toTools)
        Screen.LEVEL -> LevelScreen(toTools)
        Screen.LIGHT -> LightScreen(toTools)
        Screen.PROXIMITY -> ProximityScreen(toTools)
        Screen.TORCH -> TorchScreen(toTools)
        Screen.SENSORS -> SensorsScreen(toTools)
        Screen.CODES -> CodesScreen(toTools)
    }
}
