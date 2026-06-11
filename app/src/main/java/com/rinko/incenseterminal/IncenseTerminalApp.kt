package com.rinko.incenseterminal

import android.app.Application
import com.rinko.incenseterminal.core.repo.FocusSessionRepository
import com.rinko.incenseterminal.core.repo.WorkloadRepository
import com.rinko.incenseterminal.data.AppDatabase
import com.rinko.incenseterminal.data.WorkloadRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IncenseTerminalApp : Application() {

    val database by lazy { AppDatabase(this) }

    val workloadRepo by lazy { WorkloadRepository(database) }
    val sessionRepo by lazy { FocusSessionRepository(database) }

    private val _currentWorkload = MutableStateFlow<WorkloadRow?>(null)
    val currentWorkload: StateFlow<WorkloadRow?> = _currentWorkload.asStateFlow()

    fun selectWorkload(workload: WorkloadRow) {
        _currentWorkload.value = workload
    }
}
