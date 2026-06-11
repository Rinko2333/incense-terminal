package com.rinko.incenseterminal.core.renderer

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.rinko.incenseterminal.core.model.BurnPhase
import com.rinko.incenseterminal.core.model.EmberPhase
import com.rinko.incenseterminal.core.model.IncenseState
import com.rinko.incenseterminal.ui.theme.IncenseColors

object IncenseRenderer {

    private const val STICK_CHAR = '│'
    private const val PAUSE_CHAR = 'x'
    private const val IDLE_TIP = '·'
    private const val LINE_WIDTH = 7

    fun render(state: IncenseState): AnnotatedString = buildAnnotatedString {
        when (state.burnPhase) {
            is BurnPhase.Idle -> renderIdle(state)
            is BurnPhase.Burning -> renderBurning(state, state.burnPhase)
            is BurnPhase.Paused -> renderPaused(state, state.burnPhase)
            is BurnPhase.Completed -> renderCompleted(state)
        }
    }

    private fun AnnotatedString.Builder.renderIdle(state: IncenseState) {
        repeat(3) {
            withStyle(SpanStyle(color = IncenseColors.Background)) {
                append(centerLine(' '))
                append("\n")
            }
        }
        withStyle(SpanStyle(color = IncenseColors.DimText)) {
            append(centerLine(IDLE_TIP))
            append("\n")
            repeat(state.totalSticks) {
                append(centerLine(STICK_CHAR))
                append("\n")
            }
        }
    }

    private fun AnnotatedString.Builder.renderBurning(
        state: IncenseState,
        phase: BurnPhase.Burning
    ) {
        val burntSticks = state.totalSticks - state.remainingSticks

        repeat(burntSticks) {
            withStyle(SpanStyle(color = IncenseColors.Background)) {
                append(centerLine(' '))
                append("\n")
            }
        }

        val smokeFrame = SmokeFrames.get(phase.smokePhase)
        withStyle(SpanStyle(color = IncenseColors.Smoke)) {
            smokeFrame.lines.forEach { line ->
                append(line)
                append("\n")
            }
        }
        val (emberChar, emberColor) = when (phase.emberPhase) {
            EmberPhase.DOT -> '●' to IncenseColors.Ember
            EmberPhase.MIDDLE -> '•' to IncenseColors.EmberRed
            EmberPhase.STAR -> '*' to IncenseColors.EmberYellow
        }
        withStyle(SpanStyle(color = emberColor)) {
            append(centerLine(emberChar))
            append("\n")
        }
        withStyle(SpanStyle(color = IncenseColors.PrimaryText)) {
            repeat(state.remainingSticks) {
                append(centerLine(STICK_CHAR))
                append("\n")
            }
        }
    }

    private fun AnnotatedString.Builder.renderPaused(
        state: IncenseState,
        phase: BurnPhase.Paused
    ) {
        val burntSticks = state.totalSticks - state.remainingSticks

        repeat(burntSticks) {
            withStyle(SpanStyle(color = IncenseColors.Background)) {
                append(centerLine(' '))
                append("\n")
            }
        }

        withStyle(SpanStyle(color = IncenseColors.DimText)) {
            repeat(3) {
                append(centerLine(' '))
                append("\n")
            }
        }
        withStyle(SpanStyle(color = IncenseColors.Warning)) {
            append(centerLine(PAUSE_CHAR))
            append("\n")
        }
        withStyle(SpanStyle(color = IncenseColors.DimText)) {
            repeat(state.remainingSticks) {
                append(centerLine(STICK_CHAR))
                append("\n")
            }
        }
    }

    private fun AnnotatedString.Builder.renderCompleted(state: IncenseState) {
        val frame = CeremonyFrames.get(state.ceremonyFrame)
        if (frame != null) {
            withStyle(SpanStyle(color = IncenseColors.Success)) {
                frame.lines.forEach { line ->
                    append(centerLinePlain(line))
                    append("\n")
                }
            }
        } else {
            val mins = state.durationMinutes
            val streak = state.streakDays
            withStyle(SpanStyle(color = IncenseColors.Success)) {
                append(centerLinePlain("Complete"))
                append("\n")
            }
            withStyle(SpanStyle(color = IncenseColors.PrimaryText)) {
                append(centerLinePlain("+${mins}m Focus"))
                append("\n")
            }
            withStyle(SpanStyle(color = IncenseColors.Ember)) {
                append(centerLinePlain("Streak $streak"))
                append("\n")
            }
        }
    }

    private fun centerLine(char: Char): String {
        val padding = (LINE_WIDTH - 1) / 2
        return " ".repeat(padding) + char + " ".repeat(LINE_WIDTH - padding - 1)
    }

    private fun centerLinePlain(text: String): String {
        val padding = (LINE_WIDTH - text.length) / 2
        return " ".repeat(padding.coerceAtLeast(0)) + text
    }
}