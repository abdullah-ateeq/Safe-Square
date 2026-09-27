package com.example.safesquare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.safesquare.data.AppSettings
import com.example.safesquare.ui.SafeSquareNavHost
import com.example.safesquare.ui.theme.SafeSquareTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as SafeSquareApplication
        // Warm up sound synthesis while the splash plays.
        app.soundEngine
        setContent {
            val settings by app.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            SafeSquareTheme(themeMode = settings.themeMode, boardTheme = settings.boardTheme) {
                SafeSquareNavHost(app, settings)
            }
        }
    }
}
