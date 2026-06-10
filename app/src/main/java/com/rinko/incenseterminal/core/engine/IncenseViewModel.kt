package com.rinko.incenseterminal.core.engine

import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinko.incenseterminal.core.model.BurnPhase
import com.rinko.incenseterminal.core.model.EmberPhase
import com.rinko.incenseterminal.core.model.IncenseConfig
import com.rinko.incenseterminal.core.model.IncenseState
import com.rinko.incenseterminal.core.model.SmokePhase
import com.rinko.incenseterminal.core.renderer.IncenseRenderer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class IncenseViewModel : ViewModel() {

    private val _state = MutableStateFlow(IncenseState())
    val state: StateFlow<IncenseState> = _state.asStateFlow()

    private val smokeAnimator = SmokeAnimator(viewModelScope)
    private val ceremonyPlayer = CeremonyPlayer(viewModelScope)

    private var burnJob: Job? = null
    private var elapsedMs: Long = 0L
    private var config: IncenseConfig = IncenseConfig()

    private val tickIntervalMs = 100L

    private val _renderedIncense = MutableStateFlow(AnnotatedString(""))
    val renderedIncense: StateFlow<AnnotatedString> = _renderedIncense.asStateFlow()

    private var renderJob: Job? = null

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
    }

    fun light(durationSeconds: Int = 25 * 60) {
        config = IncenseConfig(durationSeconds = durationSeconds)
        _state.update {
            it.copy(
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
        _state.value = IncenseState()
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

    fun forceProgress(targetProgress: Float) {
        val current = _state.value
        val burning = current.burnPhase as? BurnPhase.Burning ?: return
        if (targetProgress >= 1f) {
            _state.update { it.copy(burnPhase = BurnPhase.Completed) }
            burnJob?.cancel()
            smokeAnimator.stop()
            ceremonyPlayer.play()
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

    override fun onCleared() {
        super.onCleared()
        burnJob?.cancel()
        smokeAnimator.stop()
        ceremonyPlayer.stop()
        renderJob?.cancel()
    }
}