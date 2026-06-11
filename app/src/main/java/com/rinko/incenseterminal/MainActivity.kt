package com.rinko.incenseterminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rinko.incenseterminal.ui.screen.MainScreen
import com.rinko.incenseterminal.ui.theme.IncenseTerminalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IncenseTerminalTheme {
                MainScreen()
            }
        }
    }
}
