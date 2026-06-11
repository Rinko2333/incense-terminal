package com.rinko.incenseterminal

import android.app.Application
import android.content.Context
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

    private val prefs by lazy {
        getSharedPreferences("incense_prefs", Context.MODE_PRIVATE)
    }

    private val _currentWorkload = MutableStateFlow<WorkloadRow?>(null)
    val currentWorkload: StateFlow<WorkloadRow?> = _currentWorkload.asStateFlow()

    fun selectWorkload(workload: WorkloadRow) {
        _currentWorkload.value = workload
        prefs.edit().putLong(PREF_LAST_WL_ID, workload.id).apply()
    }

    fun restoreWorkload() {
        val lastId = prefs.getLong(PREF_LAST_WL_ID, -1L)
        if (lastId < 0) return
        val wl = workloadRepo.getAll().find { it.id == lastId }
        if (wl != null) {
            _currentWorkload.value = wl
        }
    }

    fun saveLength(length: Int) {
        prefs.edit().putInt(PREF_LAST_LENGTH, length).apply()
    }

    fun restoreLength(): Int {
        return prefs.getInt(PREF_LAST_LENGTH, 9)
    }

    companion object {
        private const val PREF_LAST_WL_ID = "last_workload_id"
        private const val PREF_LAST_LENGTH = "last_length"
    }
}
