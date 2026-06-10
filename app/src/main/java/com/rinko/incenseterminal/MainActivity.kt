package com.rinko.incenseterminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.rinko.incenseterminal.ui.screen.IncenseScreen
import com.rinko.incenseterminal.ui.theme.IncenseTerminalTheme
import com.rinko.incenseterminal.ui.theme.IncenseColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IncenseTerminalTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = IncenseColors.Background
                ) {
                    IncenseScreen()
                }
            }
        }
    }
}