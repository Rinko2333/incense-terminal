package com.rinko.incenseterminal

import android.app.Application
import android.content.Context
import com.rinko.incenseterminal.core.notification.NotificationHelper
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

    override fun onCreate() {
        super.onCreate()
        restoreWorkload()
        NotificationHelper.ensureChannel(this)
    }

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

    fun getCachedMaxSticks(): Int = prefs.getInt(PREF_MAX_STICKS, 0)

    fun saveMaxSticks(max: Int) {
        prefs.edit().putInt(PREF_MAX_STICKS, max).apply()
    }

    fun clearMaxSticks() {
        prefs.edit().remove(PREF_MAX_STICKS).apply()
    }

    fun saveSessionAnchor(startTimestampMs: Long, totalSeconds: Int, workloadName: String) {
        prefs.edit()
            .putLong(PREF_SESSION_START, startTimestampMs)
            .putInt(PREF_SESSION_TOTAL, totalSeconds)
            .putString(PREF_SESSION_WORKLOAD, workloadName)
            .putBoolean(PREF_SESSION_RUNNING, true)
            .apply()
    }

    fun setSessionPaused(startTimestampMs: Long, totalSeconds: Int, workloadName: String) {
        prefs.edit()
            .putLong(PREF_SESSION_START, startTimestampMs)
            .putInt(PREF_SESSION_TOTAL, totalSeconds)
            .putString(PREF_SESSION_WORKLOAD, workloadName)
            .putBoolean(PREF_SESSION_RUNNING, false)
            .apply()
    }

    fun getSessionAnchor(): SessionAnchor? {
        if (!prefs.getBoolean(PREF_SESSION_RUNNING, false) &&
            !prefs.contains(PREF_SESSION_START)
        ) return null
        val start = prefs.getLong(PREF_SESSION_START, 0L)
        val total = prefs.getInt(PREF_SESSION_TOTAL, 0)
        val name = prefs.getString(PREF_SESSION_WORKLOAD, "") ?: ""
        val running = prefs.getBoolean(PREF_SESSION_RUNNING, false)
        if (start <= 0L || total <= 0) return null
        return SessionAnchor(start, total, name, running)
    }

    fun clearSessionAnchor() {
        prefs.edit()
            .remove(PREF_SESSION_START)
            .remove(PREF_SESSION_TOTAL)
            .remove(PREF_SESSION_WORKLOAD)
            .remove(PREF_SESSION_RUNNING)
            .apply()
    }

    companion object {
        private const val PREF_LAST_WL_ID = "last_workload_id"
        private const val PREF_LAST_LENGTH = "last_length"
        private const val PREF_MAX_STICKS = "max_sticks"
        private const val PREF_SESSION_START = "session_start"
        private const val PREF_SESSION_TOTAL = "session_total"
        private const val PREF_SESSION_WORKLOAD = "session_workload"
        private const val PREF_SESSION_RUNNING = "session_running"
    }
}

data class SessionAnchor(
    val startTimestampMs: Long,
    val totalSeconds: Int,
    val workloadName: String,
    val running: Boolean
)
