package com.rinko.incenseterminal.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinko.incenseterminal.core.engine.IncenseViewModel
import com.rinko.incenseterminal.core.model.BurnPhase
import com.rinko.incenseterminal.core.model.IncenseState
import com.rinko.incenseterminal.core.model.formatSeconds
import com.rinko.incenseterminal.ui.theme.IncenseColors
import com.rinko.incenseterminal.ui.theme.MonospaceFamily

@Composable
fun IncenseContent(
    viewModel: IncenseViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val renderedIncense by viewModel.renderedIncense.collectAsState()
    var showConfig by remember { mutableStateOf(false) }
    var dbgClickCount by remember { mutableStateOf(0) }
    var dbgEnabled by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IncenseColors.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                TerminalHeader(state, modifier = Modifier.weight(1f))
                ConfigButton(onClick = { showConfig = true })
            }

            Spacer(modifier = Modifier.height(8.dp))

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val density = LocalDensity.current
                val heightDp = maxHeight

                LaunchedEffect(viewModel.remeasureCount) {
                    if (viewModel.remeasureCount >= 0) {
                        val lineHeightDp = with(density) { 20.sp.toDp() }
                        val maxSticks = if (lineHeightDp > 0.dp)
                            (heightDp / lineHeightDp).toInt() - 4
                        else 0
                        if (maxSticks > 0) {
                            viewModel.onMeasured(maxSticks)
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    IncenseDisplay(renderedIncense)
                    Spacer(modifier = Modifier.height(12.dp))
                    TimerSection(state)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ControlsSection(state, viewModel)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 4.dp, bottom = 4.dp)
                .size(48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    dbgClickCount++
                    if (dbgClickCount >= 10) {
                        dbgEnabled = !dbgEnabled
                        dbgClickCount = 0
                    }
                }
        )

        if (dbgEnabled) {
            DebugOverlay(
                state = state,
                viewModel = viewModel,
                onDismiss = { dbgEnabled = false }
            )
        }
    }

    if (showConfig) {
        ConfigDialog(
            currentSeconds = viewModel.defaultDurationSeconds,
            currentLength = viewModel.defaultLength,
            lengthOptions = viewModel.lengthOptions,
            workloadDefaultSeconds = viewModel.workloadDefaultSeconds,
            isUsingOverride = viewModel.isUsingOverride,
            onDismiss = { showConfig = false },
            onSelectTime = { seconds -> viewModel.setDefaultDuration(seconds) },
            onUseDefault = { viewModel.useWorkloadDefault() },
            onSelectLength = { length -> viewModel.setDefaultLength(length) }
        )
    }
}

@Composable
private fun TerminalHeader(state: IncenseState, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "$ Incense Start",
            fontFamily = MonospaceFamily,
            fontSize = 14.sp,
            color = IncenseColors.Success
        )
        Text(
            text = "Session : #${state.sessionNumber.toString().padStart(3, '0')}",
            fontFamily = MonospaceFamily,
            fontSize = 13.sp,
            color = IncenseColors.PrimaryText
        )
        Text(
            text = "Today   : ${state.todayFocusMinutes}m",
            fontFamily = MonospaceFamily,
            fontSize = 12.sp,
            color = IncenseColors.DimText
        )
        Text(
            text = "Streak  : ${state.streakDays}",
            fontFamily = MonospaceFamily,
            fontSize = 12.sp,
            color = IncenseColors.DimText
        )
    }
}

@Composable
private fun ConfigButton(onClick: () -> Unit) {
    Text(
        text = "< Config >",
        fontFamily = MonospaceFamily,
        fontSize = 14.sp,
        color = IncenseColors.Accent,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    )
}

@Composable
private fun IncenseDisplay(renderedIncense: AnnotatedString) {
    Text(
        text = renderedIncense,
        fontFamily = MonospaceFamily,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        textAlign = TextAlign.Center,
        color = IncenseColors.PrimaryText
    )
}

@Composable
private fun TimerSection(state: IncenseState) {
    val remainingText = when (state.burnPhase) {
        is BurnPhase.Idle -> "Ready"
        is BurnPhase.Burning, is BurnPhase.Paused -> formatSeconds(state.remainingSeconds)
        is BurnPhase.Completed -> "Complete"
    }

    val statusLabel = when (state.burnPhase) {
        is BurnPhase.Idle, is BurnPhase.Completed -> "OK"
        is BurnPhase.Burning -> "Remaining"
        is BurnPhase.Paused -> "Paused"
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (statusLabel.isNotEmpty()) {
            Text(
                text = statusLabel,
                fontFamily = MonospaceFamily,
                fontSize = 10.sp,
                color = IncenseColors.DimText
            )
        }
        Text(
            text = remainingText,
            fontFamily = MonospaceFamily,
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
    when (state.burnPhase) {
        is BurnPhase.Idle -> {
            AnsiButton("Start") { viewModel.light() }
        }
        is BurnPhase.Burning -> {
            Row(horizontalArrangement = Arrangement.Center) {
                AnsiButton("Pause") { viewModel.pause() }
                Spacer(modifier = Modifier.width(24.dp))
                AnsiButton("Reset") { viewModel.reset() }
            }
        }
        is BurnPhase.Paused -> {
            Row(horizontalArrangement = Arrangement.Center) {
                AnsiButton("Resume") { viewModel.resume() }
                Spacer(modifier = Modifier.width(24.dp))
                AnsiButton("Reset") { viewModel.reset() }
            }
        }
        is BurnPhase.Completed -> {
            AnsiButton("Done") { viewModel.reset() }
        }
    }
}

@Composable
private fun AnsiButton(label: String, onClick: () -> Unit) {
    Text(
        text = "░░░ $label ░░░",
        fontFamily = MonospaceFamily,
        fontSize = 16.sp,
        color = IncenseColors.Accent,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp)
    )
}

@Composable
private fun DebugOverlay(
    state: IncenseState,
    viewModel: IncenseViewModel,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(0.dp),
            color = IncenseColors.Background,
            border = BorderStroke(1.dp, IncenseColors.DimText)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "[ debug menu ]",
                    fontFamily = MonospaceFamily,
                    fontSize = 12.sp,
                    color = IncenseColors.DimText
                )
                Spacer(modifier = Modifier.height(12.dp))

                DebugMenuItem("10s test") { viewModel.light(10); onDismiss() }

                val isBurning = state.burnPhase is BurnPhase.Burning
                if (isBurning) {
                    DebugMenuItem("skip 90%") { viewModel.forceProgress(0.9f); onDismiss() }
                    DebugMenuItem("force done") { viewModel.forceProgress(1.0f); onDismiss() }
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "  (start burning first)",
                        fontFamily = MonospaceFamily,
                        fontSize = 11.sp,
                        color = IncenseColors.Warning
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                DebugMenuItem("remeasure") { viewModel.requestRemeasure(); onDismiss() }

                Spacer(modifier = Modifier.height(8.dp))
                DebugMenuItem(">>> close <<<") { onDismiss() }
            }
        }
    }
}

@Composable
private fun DebugMenuItem(label: String, onClick: () -> Unit) {
    Text(
        text = "  $label",
        fontFamily = MonospaceFamily,
        fontSize = 12.sp,
        color = IncenseColors.PrimaryText,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .fillMaxWidth()
    )
}

private enum class ConfigPage { MAIN, TIME, LENGTH }

@Composable
private fun ConfigDialog(
    currentSeconds: Int,
    currentLength: Int,
    lengthOptions: List<Int>,
    workloadDefaultSeconds: Int?,
    isUsingOverride: Boolean,
    onDismiss: () -> Unit,
    onSelectTime: (Int) -> Unit,
    onUseDefault: () -> Unit,
    onSelectLength: (Int) -> Unit
) {
    var page by remember { mutableStateOf(ConfigPage.MAIN) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(0.dp),
            color = IncenseColors.Background,
            border = BorderStroke(1.dp, IncenseColors.Accent)
        ) {
            when (page) {
                ConfigPage.MAIN -> ConfigMainPage(
                    onDismiss = onDismiss,
                    onTime = { page = ConfigPage.TIME },
                    onLength = { page = ConfigPage.LENGTH }
                )
                ConfigPage.TIME -> ConfigTimePage(
                    currentSeconds = currentSeconds,
                    workloadDefaultSeconds = workloadDefaultSeconds,
                    isUsingOverride = isUsingOverride,
                    onBack = { page = ConfigPage.MAIN },
                    onSelect = onSelectTime,
                    onUseDefault = onUseDefault
                )
                ConfigPage.LENGTH -> ConfigLengthPage(
                    currentLength = currentLength,
                    options = lengthOptions,
                    onBack = { page = ConfigPage.MAIN },
                    onSelect = onSelectLength
                )
            }
        }
    }
}

@Composable
private fun ConfigMainPage(
    onDismiss: () -> Unit,
    onTime: () -> Unit,
    onLength: () -> Unit
) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "=== Config ===",
            fontFamily = MonospaceFamily,
            fontSize = 14.sp,
            color = IncenseColors.Accent
        )
        Spacer(modifier = Modifier.height(16.dp))

        ConfigMenuItem("Time") { onTime() }
        ConfigMenuItem("Length") { onLength() }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "[ cancel ]",
            fontFamily = MonospaceFamily,
            fontSize = 11.sp,
            color = IncenseColors.DimText,
            modifier = Modifier
                .clickable(onClick = onDismiss)
                .padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun ConfigTimePage(
    currentSeconds: Int,
    workloadDefaultSeconds: Int?,
    isUsingOverride: Boolean,
    onBack: () -> Unit,
    onSelect: (Int) -> Unit,
    onUseDefault: () -> Unit
) {
    val defaultSentinel = -1
    var selected by remember { mutableStateOf(if (isUsingOverride) currentSeconds else defaultSentinel) }
    var isCustom by remember { mutableStateOf(false) }
    var customInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "=== Default Time ===",
            fontFamily = MonospaceFamily,
            fontSize = 14.sp,
            color = IncenseColors.Accent
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (workloadDefaultSeconds != null) {
            val isDefaultSelected = selected == defaultSentinel
            Text(
                text = if (isDefaultSelected) " > #DEFAULT (${workloadDefaultSeconds / 60}m) < " else "   #DEFAULT (${workloadDefaultSeconds / 60}m)   ",
                fontFamily = MonospaceFamily,
                fontSize = 13.sp,
                color = if (isDefaultSelected) IncenseColors.Ember else IncenseColors.PrimaryText,
                modifier = Modifier
                    .clickable {
                        selected = defaultSentinel
                        isCustom = false
                        customInput = ""
                        onUseDefault()
                    }
                    .padding(vertical = 6.dp)
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        val options = listOf(15 * 60 to "15m", 30 * 60 to "30m", 45 * 60 to "45m", 60 * 60 to "60m")
        options.forEach { (sec, label) ->
            if (!isCustom) {
                val isSelected = sec == selected
                Text(
                    text = if (isSelected) " > $label < " else "   $label   ",
                    fontFamily = MonospaceFamily,
                    fontSize = 13.sp,
                    color = if (isSelected) IncenseColors.Ember else IncenseColors.PrimaryText,
                    modifier = Modifier
                        .clickable {
                            selected = sec
                            onSelect(sec)
                        }
                        .padding(vertical = 6.dp)
                        .fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (!isCustom) {
            Text(
                text = "[ Custom ]",
                fontFamily = MonospaceFamily,
                fontSize = 13.sp,
                color = IncenseColors.PrimaryText,
                modifier = Modifier
                    .clickable { isCustom = true }
                    .padding(vertical = 6.dp)
                    .fillMaxWidth()
            )
        } else {
            CustomInputRow(
                value = customInput,
                onValueChange = { customInput = it }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "[ save ]",
                fontFamily = MonospaceFamily,
                fontSize = 12.sp,
                color = IncenseColors.Success,
                modifier = Modifier
                    .clickable {
                        val mins = customInput.toIntOrNull()
                        if (mins != null && mins > 0) {
                            onSelect(mins * 60)
                            onBack()
                        }
                    }
                    .padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isCustom) "[ cancel ]" else "[ back ]",
            fontFamily = MonospaceFamily,
            fontSize = 11.sp,
            color = IncenseColors.DimText,
            modifier = Modifier
                .clickable {
                    if (isCustom) {
                        isCustom = false
                        customInput = ""
                    } else {
                        onBack()
                    }
                }
                .padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun CustomInputRow(value: String, onValueChange: (String) -> Unit) {
    val textColor = IncenseColors.Ember
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Text(
            text = ">[",
            fontFamily = MonospaceFamily,
            fontSize = 13.sp,
            color = textColor
        )
        BasicTextField(
            value = value,
            onValueChange = { newValue ->
                if (newValue.all { c -> c.isDigit() } && newValue.length <= 4) {
                    onValueChange(newValue)
                }
            },
            modifier = Modifier.width(60.dp),
            textStyle = TextStyle(
                color = textColor,
                fontFamily = MonospaceFamily,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            ),
            singleLine = true,
            cursorBrush = SolidColor(textColor)
        )
        Text(
            text = if (value.isNotEmpty()) " minutes]<" else "     minutes]<",
            fontFamily = MonospaceFamily,
            fontSize = 13.sp,
            color = textColor
        )
    }
}

@Composable
private fun ConfigLengthPage(
    currentLength: Int,
    options: List<Int>,
    onBack: () -> Unit,
    onSelect: (Int) -> Unit
) {
    var selected by remember { mutableStateOf(currentLength) }

    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "=== Length ===",
            fontFamily = MonospaceFamily,
            fontSize = 14.sp,
            color = IncenseColors.Accent
        )
        Spacer(modifier = Modifier.height(16.dp))

        options.forEach { len ->
            val isSelected = len == selected
            Text(
                text = if (isSelected) " > $len < " else "   $len   ",
                fontFamily = MonospaceFamily,
                fontSize = 13.sp,
                color = if (isSelected) IncenseColors.Ember else IncenseColors.PrimaryText,
                modifier = Modifier
                    .clickable {
                        selected = len
                        onSelect(len)
                    }
                    .padding(vertical = 6.dp)
                    .fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "[ back ]",
            fontFamily = MonospaceFamily,
            fontSize = 11.sp,
            color = IncenseColors.DimText,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun ConfigMenuItem(label: String, onClick: () -> Unit) {
    Text(
        text = "  $label",
        fontFamily = MonospaceFamily,
        fontSize = 13.sp,
        color = IncenseColors.PrimaryText,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
            .fillMaxWidth()
    )
}
