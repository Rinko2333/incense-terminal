package com.rinko.incenseterminal.core.model

data class IncenseState(
    val burnPhase: BurnPhase = BurnPhase.Idle,
    val totalSticks: Int = 3,
    val durationSeconds: Int = 25 * 60,
    val sessionNumber: Int = 1,
    val todayFocusMinutes: Int = 0,
    val weekFocusMinutes: Int = 0,
    val streakDays: Int = 0,
    val ceremonyFrame: Int = 0
) {
    val progress: Float
        get() = when (burnPhase) {
            is BurnPhase.Idle -> 0f
            is BurnPhase.Burning -> burnPhase.progress
            is BurnPhase.Paused -> burnPhase.progress
            is BurnPhase.Completed -> 1f
        }

    val remainingSticks: Int
        get() = (totalSticks - (progress * totalSticks).toInt()).coerceAtLeast(0)

    val isRunning: Boolean
        get() = burnPhase is BurnPhase.Burning

    val isPaused: Boolean
        get() = burnPhase is BurnPhase.Paused

    val isIdle: Boolean
        get() = burnPhase is BurnPhase.Idle

    val isCompleted: Boolean
        get() = burnPhase is BurnPhase.Completed

    val durationMinutes: Int
        get() = durationSeconds / 60

    val remainingSeconds: Int
        get() = ((1f - progress) * durationSeconds).toInt()
}