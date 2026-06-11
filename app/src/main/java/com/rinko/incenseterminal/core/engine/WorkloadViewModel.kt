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

class WorkloadViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as IncenseTerminalApp

    private val _workloads = MutableStateFlow<List<WorkloadRow>>(emptyList())
    val workloads: StateFlow<List<WorkloadRow>> = _workloads.asStateFlow()

    val currentWorkload: StateFlow<WorkloadRow?> = app.currentWorkload

    init {
        refreshWorkloads()
    }

    private fun refreshWorkloads() {
        _workloads.value = app.workloadRepo.getAll()
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
                app.selectWorkload(_workloads.value.firstOrNull() ?: return@launch)
            }
        }
    }

    fun selectWorkload(workload: WorkloadRow) {
        app.selectWorkload(workload)
    }
}
