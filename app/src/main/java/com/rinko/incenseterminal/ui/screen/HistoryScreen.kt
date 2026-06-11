package com.rinko.incenseterminal.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinko.incenseterminal.core.engine.HistoryViewModel
import com.rinko.incenseterminal.data.HeatmapDay
import com.rinko.incenseterminal.ui.theme.IncenseColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val CELL_SIZE_DP = 36
private const val CELL_PADDING_DP = 2
private const val ROW_HORIZONTAL_PADDING_DP = 8
private val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel()) {
    val viewData by viewModel.viewData.collectAsState()
    val viewMonth by viewModel.viewMonth.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()

    val monthBlocks = remember(viewData, viewMonth) {
        buildMonthBlocks(viewMonth, viewData)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = ROW_HORIZONTAL_PADDING_DP.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$ burn log",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = IncenseColors.Success
                )
                Spacer(modifier = Modifier.width(8.dp))
                ArrowButton("<") { viewModel.stepMonth(-1) }
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = viewModel.monthLabel(viewMonth),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = IncenseColors.PrimaryText
                )
                Spacer(modifier = Modifier.width(2.dp))
                ArrowButton(">") { viewModel.stepMonth(1) }
            }

            Spacer(modifier = Modifier.weight(1f))

            HeatmapLegend()
        }

        Spacer(modifier = Modifier.height(8.dp))

        DayOfWeekHeader()

        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            items(monthBlocks) { block ->
                MonthBlock(
                    block = block,
                    onCellClick = { ms -> viewModel.selectDay(ms) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (selectedDay != null) {
            DayDetailCard(
                dayData = selectedDay!!,
                onDismiss = { viewModel.clearSelection() }
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DayOfWeekHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        DAY_LABELS.forEach { label ->
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = IncenseColors.DimText,
                textAlign = TextAlign.Center,
                modifier = Modifier.size(CELL_SIZE_DP.dp)
            )
        }
    }
}

@Composable
private fun ArrowButton(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        color = IncenseColors.Accent,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun MonthBlock(
    block: MonthBlock,
    onCellClick: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = block.label,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = IncenseColors.DimText,
            modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
        )
        block.weeks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                week.forEach { cell ->
                    HeatmapCellView(
                        cell = cell,
                        onClick = { cell?.dayStartMs?.let(onCellClick) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatmapCellView(
    cell: HeatmapCell?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(CELL_SIZE_DP.dp)
            .padding(CELL_PADDING_DP.dp)
            .let {
                if (cell != null && !cell.isFuture) {
                    it.clickable(onClick = onClick)
                } else it
            },
        contentAlignment = Alignment.Center
    ) {
        if (cell == null) {
            Spacer(modifier = Modifier.size(CELL_SIZE_DP.dp))
        } else {
            val ch = heatChar(cell.totalMinutes)
            val color = heatColor(cell.totalMinutes, cell.isFuture)
            Text(
                text = ch,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HeatmapLegend() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "less", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = IncenseColors.DimText)
        Spacer(modifier = Modifier.width(2.dp))
        listOf("□", "░", "▒", "▓", "█").forEach { ch ->
            Text(text = ch, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = IncenseColors.PrimaryText)
            Spacer(modifier = Modifier.width(1.dp))
        }
        Text(text = "more", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = IncenseColors.DimText)
    }
}

@Composable
private fun DayDetailCard(
    dayData: HistoryViewModel.SelectedDay,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
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
            border = BorderStroke(1.dp, IncenseColors.Accent)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                dateFmt.timeZone = TimeZone.getTimeZone("UTC")
                val dateStr = dateFmt.format(Date(dayData.dayStartMs))

                Text(
                    text = "== $dateStr ==",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = IncenseColors.Accent
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (dayData.summaries.isEmpty()) {
                    Text(
                        text = "no sessions",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = IncenseColors.DimText
                    )
                } else {
                    val totalMin = dayData.summaries.sumOf { it.totalMinutes }
                    Text(
                        text = "${dayData.sessions.size} sessions, ${totalMin}m total",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = IncenseColors.DimText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    dayData.summaries.forEach { summary ->
                        val name = summary.workloadName.replace("_", " ")
                        Text(
                            text = "$name  ${summary.totalMinutes}m",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = IncenseColors.PrimaryText
                        )
                    }
                }
            }
        }
    }
}

data class HeatmapCell(
    val dayStartMs: Long,
    val totalMinutes: Int,
    val isFuture: Boolean
)

data class MonthBlock(
    val label: String,
    val weeks: List<List<HeatmapCell?>>
)

private fun buildMonthBlocks(viewMonthStartMs: Long, data: List<HeatmapDay>): List<MonthBlock> {
    val map = data.associateBy { it.dayStartMs }
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

    val today = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val blocks = mutableListOf<MonthBlock>()
    for (offset in -1..1) {
        val c = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = viewMonthStartMs
            add(Calendar.MONTH, offset)
        }
        val monthStart = c.clone() as Calendar
        monthStart.set(Calendar.DAY_OF_MONTH, 1)
        monthStart.set(Calendar.HOUR_OF_DAY, 0)
        monthStart.set(Calendar.MINUTE, 0)
        monthStart.set(Calendar.SECOND, 0)
        monthStart.set(Calendar.MILLISECOND, 0)
        val monthStartMs = monthStart.timeInMillis

        val label = monthLabel(monthStart)

        val firstDow = monthStart.get(Calendar.DAY_OF_WEEK)
        val mondayIndex = ((firstDow - Calendar.MONDAY) + 7) % 7

        val daysInMonth = monthStart.getActualMaximum(Calendar.DAY_OF_MONTH)
        val totalCells = mondayIndex + daysInMonth
        val weekCount = (totalCells + 6) / 7

        val cells = mutableListOf<HeatmapCell?>()
        repeat(mondayIndex) { cells.add(null) }
        for (d in 1..daysInMonth) {
            val dayCal = monthStart.clone() as Calendar
            dayCal.set(Calendar.DAY_OF_MONTH, d)
            val dayMs = dayCal.timeInMillis
            val entry = map[dayMs]
            cells.add(
                HeatmapCell(
                    dayStartMs = dayMs,
                    totalMinutes = entry?.totalMinutes ?: 0,
                    isFuture = dayMs > today
                )
            )
        }
        while (cells.size < weekCount * 7) cells.add(null)
        val weeks = cells.chunked(7)

        blocks.add(MonthBlock(label = label, weeks = weeks))
        cal.timeInMillis = monthStartMs
    }
    return blocks
}

private fun monthLabel(cal: Calendar): String {
    val fmt = SimpleDateFormat("MMM yyyy", Locale.US)
    fmt.timeZone = TimeZone.getTimeZone("UTC")
    return fmt.format(cal.time)
}

private fun heatChar(totalMinutes: Int): String = when {
    totalMinutes == 0 -> "□"
    totalMinutes <= 30 -> "░"
    totalMinutes <= 60 -> "▒"
    totalMinutes <= 120 -> "▓"
    else -> "█"
}

private fun heatColor(totalMinutes: Int, isFuture: Boolean) = when {
    isFuture -> IncenseColors.DimText.copy(alpha = 0.4f)
    totalMinutes == 0 -> IncenseColors.DimText
    totalMinutes <= 30 -> IncenseColors.Accent
    totalMinutes <= 60 -> IncenseColors.Success
    totalMinutes <= 120 -> IncenseColors.Ember
    else -> IncenseColors.EmberRed
}
