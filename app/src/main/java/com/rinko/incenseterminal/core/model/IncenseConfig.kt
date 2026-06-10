package com.rinko.incenseterminal.core.model

data class IncenseConfig(
    val durationSeconds: Int = 25 * 60
) {
    val durationMinutes: Int get() = durationSeconds / 60
    val totalSticks: Int get() = durationMinutes.toSticks()
}

fun Int.toSticks(): Int = when {
    this <= 25 -> 3
    this <= 50 -> 6
    else -> (this / 10).coerceAtLeast(9)
}

fun Int.formatDuration(): String {
    val hours = this / 60
    val minutes = this % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

fun formatSeconds(totalSeconds: Int): String {
    val min = totalSeconds / 60
    val sec = totalSeconds % 60
    return String.format("%d:%02d", min, sec)
}