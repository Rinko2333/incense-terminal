package com.rinko.incenseterminal.core.renderer

data class CeremonyFrame(val lines: List<String>)

object CeremonyFrames {
    val frames = listOf(
        CeremonyFrame(listOf("██████████")),
        CeremonyFrame(listOf("▓▓▓▓▓▓▓▓▓▓")),
        CeremonyFrame(listOf("▒▒▒▒▒▒▒▒▒▒")),
        CeremonyFrame(listOf("░░░░░░░░░░")),
        CeremonyFrame(listOf("          ")),
        CeremonyFrame(listOf("Focus Complete"))
    )

    fun get(index: Int): CeremonyFrame? {
        return frames.getOrNull(index)
    }
}