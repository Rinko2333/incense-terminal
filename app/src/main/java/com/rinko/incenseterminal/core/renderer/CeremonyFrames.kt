package com.rinko.incenseterminal.core.renderer

data class CeremonyFrame(val lines: List<String>)

object CeremonyFrames {

    val frames = listOf(
        CeremonyFrame(listOf("  *  ")),
        CeremonyFrame(listOf(" * * * ")),
        CeremonyFrame(listOf("* ✦ * ")),
        CeremonyFrame(listOf("focus complete"))
    )

    val summaryLines = listOf(
        "",
        "session complete",
        "",
        "+25m focus",
        "streak +1"
    )

    fun get(index: Int): CeremonyFrame? {
        return frames.getOrNull(index)
    }
}