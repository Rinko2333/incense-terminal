package com.rinko.incenseterminal.core.engine

import com.rinko.incenseterminal.core.renderer.CeremonyFrames
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CeremonyPlayer(
    private val scope: CoroutineScope,
    private val frameIntervalMs: Long = 800L
) {
    private val _currentFrame = MutableStateFlow(-1)
    val currentFrame: StateFlow<Int> = _currentFrame.asStateFlow()

    private val _isFinished = MutableStateFlow(false)
    val isFinished: StateFlow<Boolean> = _isFinished.asStateFlow()

    private var job: Job? = null

    fun play() {
        job?.cancel()
        _isFinished.value = false
        _currentFrame.value = 0
        job = scope.launch(Dispatchers.Default) {
            for (i in 0 until CeremonyFrames.frames.size) {
                _currentFrame.value = i
                delay(frameIntervalMs)
            }
            _currentFrame.value = CeremonyFrames.frames.size
            _isFinished.value = true
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _currentFrame.value = -1
        _isFinished.value = false
    }
}