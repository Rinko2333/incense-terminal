package com.rinko.incenseterminal.core.engine

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rinko.incenseterminal.IncenseTerminalApp
import com.rinko.incenseterminal.data.DailySummary
import com.rinko.incenseterminal.data.FocusSessionRow
import com.rinko.incenseterminal.data.HeatmapDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as IncenseTerminalApp

    private val _heatmapData = MutableStateFlow<List<HeatmapDay>>(emptyList())
    val heatmapData: StateFlow<List<HeatmapDay>> = _heatmapData.asStateFlow()

    private val _selectedDay = MutableStateFlow<SelectedDay?>(null)
    val selectedDay: StateFlow<SelectedDay?> = _selectedDay.asStateFlow()

    private val _viewMonth = MutableStateFlow(todayMonthStart())
    val viewMonth: StateFlow<Long> = _viewMonth.asStateFlow()

    private val _viewData = MutableStateFlow<List<HeatmapDay>>(emptyList())
    val viewData: StateFlow<List<HeatmapDay>> = _viewData.asStateFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        _heatmapData.value = app.sessionRepo.getHeatmapData()
        rebuildView()
    }

    fun stepMonth(delta: Int) {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = _viewMonth.value
        }
        cal.add(Calendar.MONTH, delta)
        _viewMonth.value = monthStartOf(cal)
        rebuildView()
    }

    private fun rebuildView() {
        val monthMs = _viewMonth.value
        val fromMs = monthMs - ONE_MONTH_MS
        val toMs = monthMs + 2 * ONE_MONTH_MS
        val all = _heatmapData.value
        val inRange = all.filter { it.dayStartMs in fromMs until toMs }
        _viewData.value = inRange
        if (_selectedDay.value != null) {
            val sd = _selectedDay.value!!
            if (sd.dayStartMs !in fromMs until toMs) {
                _selectedDay.value = null
            }
        }
    }

    fun selectDay(dayStartMs: Long) {
        viewModelScope.launch {
            val dayEndMs = dayStartMs + 86_400_000L
            val summary = app.sessionRepo.getDailySummary(dayStartMs, dayEndMs)
            val sessions = app.sessionRepo.getSessionsForDay(dayStartMs, dayEndMs)
            _selectedDay.value = SelectedDay(
                dayStartMs = dayStartMs,
                summaries = summary,
                sessions = sessions
            )
        }
    }

    fun clearSelection() {
        _selectedDay.value = null
    }

    fun monthLabel(monthMs: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date(monthMs))
    }

    private fun monthStartOf(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun todayMonthStart(): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        return monthStartOf(cal)
    }

    data class SelectedDay(
        val dayStartMs: Long,
        val summaries: List<DailySummary>,
        val sessions: List<FocusSessionRow>
    )

    companion object {
        const val ONE_MONTH_MS = 31L * 86_400_000L
    }
}
