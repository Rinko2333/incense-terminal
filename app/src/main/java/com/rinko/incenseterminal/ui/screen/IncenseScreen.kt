package com.rinko.incenseterminal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinko.incenseterminal.core.engine.IncenseViewModel
import com.rinko.incenseterminal.core.model.BurnPhase
import com.rinko.incenseterminal.core.model.IncenseState
import com.rinko.incenseterminal.ui.theme.IncenseColors

@Composable
fun IncenseScreen(
    viewModel: IncenseViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val renderedIncense by viewModel.renderedIncense.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IncenseColors.Background)
            .statusBarsPadding()
    ) {
        StatusSection(
            state = state,
            modifier = Modifier.align(Alignment.TopEnd)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IncenseDisplay(renderedIncense)
            Spacer(modifier = Modifier.height(12.dp))
            TimerSection(state)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ControlsSection(state, viewModel)
            Spacer(modifier = Modifier.height(20.dp))
            DebugSection(viewModel)
        }
    }
}

@Composable
private fun StatusSection(state: IncenseState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = "SESSION #${state.sessionNumber}",
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = IncenseColors.PrimaryText
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "today: ${state.todayFocusMinutes}m  streak: ${state.streakDays}",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = IncenseColors.DimText
        )
    }
}

@Composable
private fun IncenseDisplay(renderedIncense: AnnotatedString) {
    Text(
        text = renderedIncense,
        fontFamily = FontFamily.Monospace,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        textAlign = TextAlign.Center,
        color = IncenseColors.PrimaryText
    )
}

@Composable
private fun TimerSection(state: IncenseState) {
    val remainingText = when (state.burnPhase) {
        is BurnPhase.Idle -> "ready"
        is BurnPhase.Burning -> {
            val sec = state.remainingSeconds
            val min = sec / 60
            val s = sec % 60
            String.format("%d:%02d", min, s)
        }
        is BurnPhase.Paused -> {
            val sec = state.remainingSeconds
            val min = sec / 60
            val s = sec % 60
            String.format("%d:%02d", min, s)
        }
        is BurnPhase.Completed -> "complete"
    }

    val statusLabel = when (state.burnPhase) {
        is BurnPhase.Idle -> ""
        is BurnPhase.Burning -> "remaining"
        is BurnPhase.Paused -> "paused"
        is BurnPhase.Completed -> ""
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (statusLabel.isNotEmpty()) {
            Text(
                text = statusLabel,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = IncenseColors.DimText
            )
        }
        Text(
            text = remainingText,
            fontFamily = FontFamily.Monospace,
            fontSize = 24.sp,
            color = IncenseColors.PrimaryText
        )
    }
}

@Composable
private fun ControlsSection(
    state: IncenseState,
    viewModel: IncenseViewModel
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        when (state.burnPhase) {
            is BurnPhase.Idle -> {
                TerminalButton("start") { viewModel.light(25 * 60) }
                Spacer(modifier = Modifier.width(12.dp))
                TerminalButton("50m") { viewModel.light(50 * 60) }
                Spacer(modifier = Modifier.width(12.dp))
                TerminalButton("90m") { viewModel.light(90 * 60) }
            }
            is BurnPhase.Burning -> {
                TerminalButton("pause") { viewModel.pause() }
                Spacer(modifier = Modifier.width(12.dp))
                TerminalButton("reset") { viewModel.reset() }
            }
            is BurnPhase.Paused -> {
                TerminalButton("resume") { viewModel.resume() }
                Spacer(modifier = Modifier.width(12.dp))
                TerminalButton("reset") { viewModel.reset() }
            }
            is BurnPhase.Completed -> {
                TerminalButton("done") { viewModel.reset() }
            }
        }
    }
}

@Composable
private fun DebugSection(viewModel: IncenseViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "[ debug ]",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = IncenseColors.DimText
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.Center
        ) {
            TerminalButton("10s test") { viewModel.light(10) }
            Spacer(modifier = Modifier.width(8.dp))
            TerminalButton("skip 90%") {
                if (viewModel.state.value.burnPhase is BurnPhase.Burning) {
                    viewModel.forceProgress(0.9f)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            TerminalButton("force done") {
                if (viewModel.state.value.burnPhase is BurnPhase.Burning) {
                    viewModel.forceProgress(1.0f)
                }
            }
        }
    }
}

@Composable
private fun TerminalButton(
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = IncenseColors.Accent
        )
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}