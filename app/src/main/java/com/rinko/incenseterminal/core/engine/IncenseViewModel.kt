package com.rinko.incenseterminal.core.engine

import android.app.Application
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rinko.incenseterminal.IncenseTerminalApp
import com.rinko.incenseterminal.core.model.BurnPhase
import com.rinko.incenseterminal.core.model.EmberPhase
import com.rinko.incenseterminal.core.model.IncenseConfig
import com.rinko.incenseterminal.core.model.IncenseState
import com.rinko.incenseterminal.core.model.SmokePhase
import com.rinko.incenseterminal.core.renderer.IncenseRenderer
import com.rinko.incenseterminal.data.FocusSessionRow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class IncenseViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as IncenseTerminalApp

    private val _state = MutableStateFlow(IncenseState())
    val state: StateFlow<IncenseState> = _state.asStateFlow()

    private val smokeAnimator = SmokeAnimator(viewModelScope)
    private val ceremonyPlayer = CeremonyPlayer(viewModelScope)

    private var burnJob: Job? = null
    private var elapsedMs: Long = 0L
    private var config: IncenseConfig = IncenseConfig()
    private var sessionStartMs: Long = 0L

    private val tickIntervalMs = 100L

    private val _renderedIncense = MutableStateFlow(AnnotatedString(""))
    val renderedIncense: StateFlow<AnnotatedString> = _renderedIncense.asStateFlow()

    private var renderJob: Job? = null

    var defaultDurationSeconds: Int = 25 * 60
        private set

    var defaultLength: Int = 9
        private set

    private var overrideDurationSeconds: Int? = null

    fun setDefaultDuration(seconds: Int) {
        overrideDurationSeconds = seconds
        defaultDurationSeconds = seconds
    }

    fun setDefaultLength(length: Int) {
        defaultLength = length
    }

    init {
        renderJob = viewModelScope.launch {
            _state.collect { state ->
                _renderedIncense.value = IncenseRenderer.render(state)
            }
        }
        viewModelScope.launch {
            smokeAnimator.currentPhase.collect { phase ->
                updateSmokePhase(phase)
            }
        }
        viewModelScope.launch {
            ceremonyPlayer.currentFrame.collect { frame ->
                _state.update { it.copy(ceremonyFrame = frame) }
            }
        }
        viewModelScope.launch {
            app.currentWorkload.collect { wl ->
                if (wl != null) {
                    overrideDurationSeconds = null
                    val base = wl.defaultDurationMinutes * 60
                    defaultDurationSeconds = base
                    _state.update { it.copy(workloadName = wl.name) }
                }
            }
        }
        refreshAggregateState()
    }

    fun light(durationSeconds: Int = effectiveDurationSeconds()) {
        config = IncenseConfig(durationSeconds = durationSeconds, length = defaultLength)
        sessionStartMs = System.currentTimeMillis()
        _state.update { s ->
            s.copy(
                totalSticks = config.totalSticks,
                durationSeconds = durationSeconds,
                burnPhase = BurnPhase.Burning(
                    progress = 0f,
                    emberPhase = EmberPhase.DOT,
                    smokePhase = SmokePhase.A
                )
            )
        }
        elapsedMs = 0L
        smokeAnimator.start()
        startBurnTimer()
    }

    fun pause() {
        val current = _state.value
        val progress = current.progress
        _state.update { it.copy(burnPhase = BurnPhase.Paused(progress = progress)) }
        burnJob?.cancel()
        smokeAnimator.pause()
    }

    fun resume() {
        val current = _state.value
        val paused = current.burnPhase as? BurnPhase.Paused ?: return
        _state.update {
            it.copy(burnPhase = BurnPhase.Burning(
                progress = paused.progress,
                emberPhase = EmberPhase.DOT,
                smokePhase = smokeAnimator.currentPhase.value
            ))
        }
        smokeAnimator.resume()
        startBurnTimer()
    }

    fun reset() {
        burnJob?.cancel()
        smokeAnimator.stop()
        ceremonyPlayer.stop()
        elapsedMs = 0L
        sessionStartMs = 0L
        val currentWl = _state.value.workloadName
        _state.value = IncenseState(
            totalSticks = defaultLength,
            workloadName = currentWl
        )
    }

    private fun startBurnTimer() {
        burnJob?.cancel()
        burnJob = viewModelScope.launch {
            while (true) {
                delay(tickIntervalMs)
                elapsedMs += tickIntervalMs
                val totalDurationMs = config.durationSeconds * 1000L
                val progress = (elapsedMs.toFloat() / totalDurationMs).coerceAtMost(1f)

                val emberPhase = EmberPhase.cycleFrom(EmberPhase.DOT, elapsedMs)

                if (progress >= 1f) {
                    _state.update {
                        it.copy(burnPhase = BurnPhase.Completed)
                    }
                    smokeAnimator.stop()
                    ceremonyPlayer.play()
                    recordSession(completed = true)
                    break
                } else {
                    _state.update { s ->
                        val burning = s.burnPhase as? BurnPhase.Burning ?: return@update s
                        s.copy(
                            burnPhase = burning.copy(
                                progress = progress,
                                emberPhase = emberPhase,
                                smokePhase = smokeAnimator.currentPhase.value
                            )
                        )
                    }
                }
            }
        }
    }

    private fun updateSmokePhase(phase: SmokePhase) {
        _state.update { s ->
            val burning = s.burnPhase as? BurnPhase.Burning ?: return@update s
            s.copy(burnPhase = burning.copy(smokePhase = phase))
        }
    }

    private fun recordSession(completed: Boolean) {
        val currentState = _state.value
        val workloadName = currentState.workloadName.ifEmpty { "focus" }
        val now = System.currentTimeMillis()
        val session = FocusSessionRow(
            workloadName = workloadName,
            durationMinutes = currentState.durationMinutes,
            startTimestamp = sessionStartMs,
            endTimestamp = now,
            completed = completed
        )
        viewModelScope.launch {
            app.sessionRepo.record(session)
            refreshAggregateState()
        }
    }

    private fun refreshAggregateState() {
        viewModelScope.launch {
            val sessionCount = app.sessionRepo.getCompletedSessionCount()
            val todayStart = todayDayStartMs()
            val todayEnd = todayStart + 86_400_000L
            val weekStart = todayStart - 6 * 86_400_000L
            val todayMin = app.sessionRepo.getFocusMinutesForRange(todayStart, todayEnd)
            val weekMin = app.sessionRepo.getFocusMinutesForRange(weekStart, todayEnd)
            val streak = app.sessionRepo.getStreakDays(todayStart)
            _state.update {
                it.copy(
                    sessionNumber = sessionCount + 1,
                    todayFocusMinutes = todayMin,
                    weekFocusMinutes = weekMin,
                    streakDays = streak
                )
            }
        }
    }

    private fun todayDayStartMs(): Long {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun forceProgress(targetProgress: Float) {
        val current = _state.value
        val burning = current.burnPhase as? BurnPhase.Burning ?: return
        if (targetProgress >= 1f) {
            _state.update { it.copy(burnPhase = BurnPhase.Completed) }
            burnJob?.cancel()
            smokeAnimator.stop()
            ceremonyPlayer.play()
            recordSession(completed = true)
            return
        }
        val totalDurationMs = config.durationSeconds * 1000L
        elapsedMs = (targetProgress * totalDurationMs).toLong()
        _state.update {
            it.copy(
                burnPhase = burning.copy(
                    progress = targetProgress,
                    emberPhase = EmberPhase.cycleFrom(EmberPhase.DOT, elapsedMs),
                    smokePhase = smokeAnimator.currentPhase.value
                )
            )
        }
    }

    private fun effectiveDurationSeconds(): Int {
        val current = app.currentWorkload.value
        return overrideDurationSeconds
            ?: (current?.defaultDurationMinutes?.let { it * 60 } ?: defaultDurationSeconds)
    }

    override fun onCleared() {
        super.onCleared()
        burnJob?.cancel()
        smokeAnimator.stop()
        ceremonyPlayer.stop()
        renderJob?.cancel()
    }
}
