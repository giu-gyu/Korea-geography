package com.koreageo.quiz.quiz

/** How many wrong guesses before the "힌트" (hint) button appears. */
const val WRONG_GUESSES_BEFORE_HINT = 3

data class GuessState(
    val revealed: Boolean = false,
    val wrongCount: Int = 0,
    val hintShown: Boolean = false,
) {
    val hintAvailable: Boolean get() = wrongCount >= WRONG_GUESSES_BEFORE_HINT
}

sealed class MapLevel {
    /** Stable key used to keep per-level quiz progress separate. */
    abstract val key: String

    data object National : MapLevel() {
        override val key: String = "national"
    }

    data class Province(val code: String, val name: String) : MapLevel() {
        override val key: String = "province:$code"
    }
}

sealed class UiEvent {
    data object WrongAnswer : UiEvent()
    data object AlreadyRevealed : UiEvent()
    data object DrillNotAvailableYet : UiEvent()
}
