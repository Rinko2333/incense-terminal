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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class IncenseViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as IncenseTerminalApp

    private val _state = MutableStateFlow(IncenseState())
    val state: StateFlow<IncenseState> = _state.asStateFlow()

    private val smokeAnimator = SmokeAnimator(viewModelScope)
    private val ceremonyPlayer = CeremonyPlayer(viewModelScope)

    private var burnJob: Job? = null
    private val tickIntervalMs = 100L

    private val _renderedIncense = MutableStateFlow(AnnotatedString(""))
    val renderedIncense: StateFlow<AnnotatedString> = _renderedIncense.asStateFlow()

    private var renderJob: Job? = null

    private val _completionEvent = Channel<CompletionInfo>(Channel.BUFFERED)
    val completionEvent = _completionEvent.receiveAsFlow()

    private var config: IncenseConfig = IncenseConfig()
    private var sessionStartTimestampMs: Long = 0L

    var defaultDurationSeconds: Int = 25 * 60
        private set

    var defaultLength: Int = 9
        private set

    private var overrideDurationSeconds: Int? = null

    val isUsingOverride: Boolean get() = overrideDurationSeconds != null

    val currentWorkload = app.currentWorkload

    val workloadDefaultSeconds: Int?
        get() = app.currentWorkload.value?.defaultDurationMinutes?.times(60)

    fun setDefaultDuration(seconds: Int) {
        overrideDurationSeconds = seconds
        defaultDurationSeconds = seconds
    }

    fun useWorkloadDefault() {
        overrideDurationSeconds = null
        defaultDurationSeconds = app.currentWorkload.value?.defaultDurationMinutes?.times(60) ?: (25 * 60)
    }

    fun setDefaultLength(length: Int) {
        defaultLength = length
        app.saveLength(length)
        _state.update {
            if (it.isIdle) it.copy(totalSticks = length)
            else it
        }
    }

    val effectiveMaxSticks: Int
        get() {
            val cached = app.getCachedMaxSticks()
            return if (cached > 0) cached else 24
        }

    val lengthOptions: List<Int>
        get() {
            val max = effectiveMaxSticks
            val min = max / 2
            return (0 until 6).map { i -> min + (max - min) * i / 5 }
        }

    var remeasureCount: Int = 0
        private set

    fun onMeasured(max: Int) {
        app.saveMaxSticks(max)
        remeasureCount = -1
        val minLen = max / 2
        val newLen = if (defaultLength > max || defaultLength < minLen) minLen else defaultLength
        if (newLen != defaultLength) {
            defaultLength = newLen
            app.saveLength(newLen)
        }
        _state.update { if (it.isIdle) it.copy(totalSticks = newLen) else it }
    }

    fun requestRemeasure() {
        app.clearMaxSticks()
        remeasureCount = 0
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
                    val current = _state.value
                    if (current.isIdle) {
                        _state.update { it.copy(workloadName = wl.name) }
                    } else {
                        _state.update { it.copy(workloadName = wl.name) }
                    }
                }
            }
        }
        val cachedMax = app.getCachedMaxSticks()
        if (cachedMax > 0) {
            remeasureCount = -1
        }
        defaultLength = app.restoreLength()
        val effectiveMax = if (cachedMax > 0) cachedMax else 24
        val minLen = effectiveMax / 2
        if (defaultLength > effectiveMax || defaultLength < minLen) {
            defaultLength = minLen
            app.saveLength(minLen)
        }
        _state.update { it.copy(totalSticks = defaultLength) }
        refreshAggregateState()
        restoreSessionIfAny()
    }

    private fun restoreSessionIfAny() {
        val anchor = app.getSessionAnchor() ?: return
        val elapsedSec = ((System.currentTimeMillis() - anchor.startTimestampMs) / 1000L).toInt()
        val totalSec = anchor.totalSeconds
        val workloadName = anchor.workloadName

        if (elapsedSec >= totalSec) {
            sessionStartTimestampMs = anchor.startTimestampMs
            config = IncenseConfig(durationSeconds = totalSec, length = defaultLength)
            _state.update { s ->
                s.copy(
                    totalSticks = config.totalSticks,
                    durationSeconds = totalSec,
                    burnPhase = BurnPhase.Completed,
                    workloadName = workloadName.ifEmpty { s.workloadName }
                )
            }
            recordSessionAndNotify(completed = true, workloadName = workloadName, totalSec = totalSec)
            app.clearSessionAnchor()
            ceremonyPlayer.play()
            return
        }

        if (anchor.running) {
            sessionStartTimestampMs = anchor.startTimestampMs
            config = IncenseConfig(durationSeconds = totalSec, length = defaultLength)
            val progress = (elapsedSec.toFloat() / totalSec).coerceIn(0f, 1f)
            _state.update { s ->
                s.copy(
                    totalSticks = config.totalSticks,
                    durationSeconds = totalSec,
                    workloadName = workloadName.ifEmpty { s.workloadName },
                    burnPhase = BurnPhase.Burning(
                        progress = progress,
                        emberPhase = EmberPhase.cycleFrom(EmberPhase.DOT, elapsedSec * 1000L),
                        smokePhase = smokeAnimator.currentPhase.value
                    )
                )
            }
            smokeAnimator.start()
            startBurnTimer()
        }
    }

    fun light(durationSeconds: Int = effectiveDurationSeconds()) {
        config = IncenseConfig(durationSeconds = durationSeconds, length = defaultLength)
        sessionStartTimestampMs = System.currentTimeMillis()
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
        app.saveSessionAnchor(
            startTimestampMs = sessionStartTimestampMs,
            totalSeconds = durationSeconds,
            workloadName = _state.value.workloadName
        )
        smokeAnimator.start()
        startBurnTimer()
    }

    fun pause() {
        val current = _state.value
        val progress = current.progress
        _state.update { it.copy(burnPhase = BurnPhase.Paused(progress = progress)) }
        burnJob?.cancel()
        smokeAnimator.pause()
        app.setSessionPaused(
            startTimestampMs = sessionStartTimestampMs,
            totalSeconds = config.durationSeconds,
            workloadName = _state.value.workloadName
        )
    }

    fun resume() {
        val current = _state.value
        val paused = current.burnPhase as? BurnPhase.Paused ?: return
        sessionStartTimestampMs = System.currentTimeMillis() -
            (paused.progress * config.durationSeconds * 1000L).toLong()
        _state.update {
            it.copy(burnPhase = BurnPhase.Burning(
                progress = paused.progress,
                emberPhase = EmberPhase.DOT,
                smokePhase = smokeAnimator.currentPhase.value
            ))
        }
        smokeAnimator.resume()
        app.saveSessionAnchor(
            startTimestampMs = sessionStartTimestampMs,
            totalSeconds = config.durationSeconds,
            workloadName = _state.value.workloadName
        )
        startBurnTimer()
    }

    fun reset() {
        burnJob?.cancel()
        smokeAnimator.stop()
        ceremonyPlayer.stop()
        sessionStartTimestampMs = 0L
        app.clearSessionAnchor()
        val current = _state.value
        _state.value = IncenseState(
            totalSticks = defaultLength,
            workloadName = current.workloadName,
            sessionNumber = current.sessionNumber,
            todayFocusMinutes = current.todayFocusMinutes,
            weekFocusMinutes = current.weekFocusMinutes,
            streakDays = current.streakDays
        )
    }

    private fun startBurnTimer() {
        burnJob?.cancel()
        burnJob = viewModelScope.launch {
            while (true) {
                delay(tickIntervalMs)
                val now = System.currentTimeMillis()
                val elapsedMs = (now - sessionStartTimestampMs).coerceAtLeast(0L)
                val totalDurationMs = config.durationSeconds * 1000L
                val progress = (elapsedMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)

                val emberPhase = EmberPhase.cycleFrom(EmberPhase.DOT, elapsedMs)

                if (progress >= 1f) {
                    val workloadName = _state.value.workloadName
                    val totalSec = config.durationSeconds
                    _state.update {
                        it.copy(burnPhase = BurnPhase.Completed)
                    }
                    smokeAnimator.stop()
                    ceremonyPlayer.play()
                    app.clearSessionAnchor()
                    recordSessionAndNotify(completed = true, workloadName = workloadName, totalSec = totalSec)
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

    private fun recordSessionAndNotify(completed: Boolean, workloadName: String, totalSec: Int) {
        val name = workloadName.ifEmpty { "focus" }
        val now = System.currentTimeMillis()
        val session = FocusSessionRow(
            workloadName = name,
            durationMinutes = totalSec / 60,
            startTimestamp = sessionStartTimestampMs,
            endTimestamp = now,
            completed = completed
        )
        viewModelScope.launch {
            app.sessionRepo.record(session)
            refreshAggregateState()
        }
        if (completed) {
            viewModelScope.launch {
                _completionEvent.send(CompletionInfo(name, totalSec / 60))
            }
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
            val workloadName = _state.value.workloadName
            val totalSec = config.durationSeconds
            _state.update { it.copy(burnPhase = BurnPhase.Completed) }
            burnJob?.cancel()
            smokeAnimator.stop()
            ceremonyPlayer.play()
            app.clearSessionAnchor()
            recordSessionAndNotify(completed = true, workloadName = workloadName, totalSec = totalSec)
            return
        }
        val totalDurationMs = config.durationSeconds * 1000L
        val targetElapsedMs = (targetProgress * totalDurationMs).toLong()
        sessionStartTimestampMs = System.currentTimeMillis() - targetElapsedMs
        _state.update {
            it.copy(
                burnPhase = burning.copy(
                    progress = targetProgress,
                    emberPhase = EmberPhase.cycleFrom(EmberPhase.DOT, targetElapsedMs),
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

data class CompletionInfo(
    val workloadName: String,
    val durationMinutes: Int
)
