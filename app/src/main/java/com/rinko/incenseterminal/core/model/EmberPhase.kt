package com.rinko.incenseterminal.core.model

enum class EmberPhase(val char: Char) {
    DOT('●'),
    MIDDLE('•'),
    STAR('*');

    fun next(): EmberPhase = when (this) {
        DOT -> MIDDLE
        MIDDLE -> STAR
        STAR -> MIDDLE
    }

    companion object {
        fun cycleFrom(phase: EmberPhase, elapsedMs: Long): EmberPhase {
            val cycles = (elapsedMs / 300L).toInt()
            var current = phase
            repeat(cycles) { current = current.next() }
            return current
        }
    }
}