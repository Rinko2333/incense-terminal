package com.rinko.incenseterminal.core.renderer

import com.rinko.incenseterminal.core.model.SmokePhase

data class SmokeFrame(val lines: List<String>)

object SmokeFrames {

    val A = SmokeFrame(
        listOf(
            "    (  ",
            "   ) ) ",
            "  ( (  ",
            "    )  "
        )
    )

    val B = SmokeFrame(
        listOf(
            "   )   ",
            "  ( (  ",
            "   ) ) ",
            "   (   "
        )
    )

    val C = SmokeFrame(
        listOf(
            "  ( (  ",
            "    )  ",
            "   (   ",
            "  ( (  "
        )
    )

    fun get(phase: SmokePhase): SmokeFrame = when (phase) {
        SmokePhase.A -> A
        SmokePhase.B -> B
        SmokePhase.C -> C
    }
}