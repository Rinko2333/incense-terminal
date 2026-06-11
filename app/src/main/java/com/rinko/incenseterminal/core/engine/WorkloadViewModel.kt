package com.rinko.incenseterminal.core.engine

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rinko.incenseterminal.IncenseTerminalApp
import com.rinko.incenseterminal.data.WorkloadRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

class WorkloadViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as IncenseTerminalApp

    private val _workloads = MutableStateFlow<List<WorkloadRow>>(emptyList())
    val workloads: StateFlow<List<WorkloadRow>> = _workloads.asStateFlow()

    private val _todayCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val todayCounts: StateFlow<Map<String, Int>> = _todayCounts.asStateFlow()

    val currentWorkload: StateFlow<WorkloadRow?> = app.currentWorkload

    init {
        refreshWorkloads()
        app.restoreWorkload()
        refreshTodayCounts()
    }

    private fun refreshWorkloads() {
        _workloads.value = app.workloadRepo.getAll()
    }

    private fun refreshTodayCounts() {
        val todayStart = todayDayStartMs()
        _todayCounts.value = app.sessionRepo.getTodayCountsPerWorkload(todayStart)
    }

    fun addWorkload(name: String, defaultDurationMinutes: Int) {
        viewModelScope.launch {
            app.workloadRepo.insert(name, defaultDurationMinutes)
            refreshWorkloads()
        }
    }

    fun deleteWorkload(workload: WorkloadRow) {
        viewModelScope.launch {
            app.workloadRepo.delete(workload.id)
            refreshWorkloads()
            if (currentWorkload.value?.id == workload.id) {
                val first = _workloads.value.firstOrNull()
                if (first != null) app.selectWorkload(first)
            }
        }
    }

    fun updateWorkload(workload: WorkloadRow, name: String, durationMinutes: Int) {
        viewModelScope.launch {
            app.workloadRepo.update(workload.id, name, durationMinutes)
            refreshWorkloads()
            if (currentWorkload.value?.id == workload.id) {
                val updated = _workloads.value.find { it.id == workload.id }
                if (updated != null) app.selectWorkload(updated)
            }
        }
    }

    fun selectWorkload(workload: WorkloadRow) {
        app.selectWorkload(workload)
    }

    private fun todayDayStartMs(): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
