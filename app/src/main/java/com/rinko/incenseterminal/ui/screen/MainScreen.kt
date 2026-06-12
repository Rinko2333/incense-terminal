package com.rinko.incenseterminal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rinko.incenseterminal.ui.theme.IncenseColors
import com.rinko.incenseterminal.ui.theme.MonospaceFamily

enum class Screen { HOME, WORKLOAD, HISTORY }

@Composable
fun MainScreen() {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IncenseColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        NavBar(currentScreen) { currentScreen = it }
        HorizontalDivider(color = IncenseColors.DimText.copy(alpha = 0.3f), thickness = 0.5.dp)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (currentScreen) {
                Screen.HOME -> IncenseContent(onNavigateToWorkload = { currentScreen = Screen.WORKLOAD })
                Screen.WORKLOAD -> WorkloadScreen(onSelectDone = { currentScreen = Screen.HOME })
                Screen.HISTORY -> HistoryScreen()
            }
        }
    }
}

@Composable
private fun NavBar(current: Screen, onSelect: (Screen) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        NavItem("~ /", current == Screen.HOME) { onSelect(Screen.HOME) }
        Spacer(modifier = Modifier.width(14.dp))
        NavItem("# workloads /", current == Screen.WORKLOAD) { onSelect(Screen.WORKLOAD) }
        Spacer(modifier = Modifier.width(14.dp))
        NavItem("# history /", current == Screen.HISTORY) { onSelect(Screen.HISTORY) }
    }
}

@Composable
private fun NavItem(label: String, active: Boolean, onClick: () -> Unit) {
    Text(
        text = if (active) "[ $label ]" else "  $label",
        fontFamily = MonospaceFamily,
        fontSize = 14.sp,
        color = if (active) IncenseColors.Accent else IncenseColors.DimText,
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 4.dp)
    )
}
