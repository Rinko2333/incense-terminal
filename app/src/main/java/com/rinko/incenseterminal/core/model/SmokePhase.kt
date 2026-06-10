package com.rinko.incenseterminal.core.model

enum class SmokePhase {
    A, B, C;

    fun next(): SmokePhase = when (this) {
        A -> B
        B -> C
        C -> A
    }

    companion object {
        fun cycleFrom(phase: SmokePhase, elapsedMs: Long): SmokePhase {
            val cycles = (elapsedMs / 500L).toInt()
            var current = phase
            repeat(cycles) { current = current.next() }
            return current
        }
    }
}