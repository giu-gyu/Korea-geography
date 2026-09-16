package com.koreageo.quiz.quiz

data class GuessState(
    val revealed: Boolean = false,
    val wrongCount: Int = 0,
    val characterCountHintShown: Boolean = false,
    val choseongHintShown: Boolean = false,
)

/**
 * User-configurable hint behavior. A threshold of 0 means that hint is turned off entirely;
 * 1-4 is the number of wrong guesses needed before its button appears.
 */
data class QuizSettings(
    val showLabelsInBrowseMode: Boolean = true,
    val characterCountHintThreshold: Int = 0,
    val choseongHintThreshold: Int = 3,
)

fun GuessState.characterCountHintAvailable(settings: QuizSettings): Boolean =
    settings.characterCountHintThreshold > 0 && wrongCount >= settings.characterCountHintThreshold

fun GuessState.choseongHintAvailable(settings: QuizSettings): Boolean =
    settings.choseongHintThreshold > 0 && wrongCount >= settings.choseongHintThreshold

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
