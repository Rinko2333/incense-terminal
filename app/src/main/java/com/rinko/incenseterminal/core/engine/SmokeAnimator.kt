package com.rinko.incenseterminal.core.engine

import com.rinko.incenseterminal.core.model.SmokePhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SmokeAnimator(
    private val scope: CoroutineScope,
    private val intervalMs: Long = 500L
) {
    private val _currentPhase = MutableStateFlow(SmokePhase.A)
    val currentPhase: StateFlow<SmokePhase> = _currentPhase.asStateFlow()

    private var job: Job? = null

    fun start() {
        stop()
        job = scope.launch(Dispatchers.Default) {
            while (true) {
                delay(intervalMs)
                _currentPhase.value = _currentPhase.value.next()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _currentPhase.value = SmokePhase.A
    }

    fun pause() {
        job?.cancel()
        job = null
    }

    fun resume() {
        if (job == null || !job!!.isActive) {
            start()
        }
    }
}