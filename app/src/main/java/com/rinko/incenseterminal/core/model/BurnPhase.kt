package com.rinko.incenseterminal.core.model

sealed class BurnPhase {
    data object Idle : BurnPhase()
    data class Burning(
        val progress: Float,
        val emberPhase: EmberPhase = EmberPhase.DOT,
        val smokePhase: SmokePhase = SmokePhase.A
    ) : BurnPhase()
    data class Paused(val progress: Float) : BurnPhase()
    data object Completed : BurnPhase()
}